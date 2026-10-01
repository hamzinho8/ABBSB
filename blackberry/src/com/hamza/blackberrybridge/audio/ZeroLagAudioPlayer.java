package com.hamza.blackberrybridge.audio;

import java.io.ByteArrayInputStream;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import net.rim.device.api.media.control.AudioPathControl;

/**
 * Lecteur audio temps réel anti-latence (Zero-Lag) pour BlackBerry Curve 9300 (OS 5.0 à 7.1).
 *
 * Spécifications du flux :
 * - Paquet d'amorce : "AUDIO_START\n"
 * - Paquets continus : "AUDIO_CHUNK|<base64_wav>\n" envoyés toutes les 250 ms
 * - Paquet de fin : "AUDIO_STOP\n"
 * - Format : WAV RIFF/PCM 8000 Hz, 16-bit Mono (4044 octets décodés = 250 ms)
 *
 * Architecture Anti-Latence (Drop-if-Lagging) :
 * - Utilisation d'un tampon atomique unique (Single-slot Buffer) au lieu d'une file FIFO.
 * - Si un nouveau paquet AUDIO_CHUNK arrive alors qu'un paquet précédent est en cours
 *   ou en attente, l'ancien est IMMÉDIATEMENT écrasé/rejeté.
 * - Élimine définitivement le retard cumulé de 10 secondes et maintient un flux 100% temps réel.
 * - Fermeture immédiate du Player précédent avant instanciation du nouveau pour libérer le DSP.
 */
public class ZeroLagAudioPlayer implements Runnable, PlayerListener {

    private static ZeroLagAudioPlayer instance;

    // Tampon atomique à emplacement unique (Single-slot buffer)
    // Empêche strictement toute accumulation de paquets
    private volatile byte[] pendingChunk = null;
    private final Object bufferLock = new Object();

    // Contrôle d'exécution du Thread ouvrier
    private volatile boolean isRunning = false;
    private Thread workerThread;

    // Player multimédia actif
    private Player activePlayer;
    private final Object playerLock = new Object();
    private volatile boolean chunkFinished = false;

    // Configuration et routage matériel
    private int volume = 100; // Volume maximal par défaut
    private int audioRoute = AudioPathControl.AUDIO_PATH_HANDSFREE; // Haut-parleur par défaut

    // Statistiques de performance
    private int chunksReceived = 0;
    private int chunksPlayed = 0;
    private int chunksDropped = 0;

    /**
     * Singleton d'accès pour le lecteur audio.
     */
    public static synchronized ZeroLagAudioPlayer getInstance() {
        if (instance == null) {
            instance = new ZeroLagAudioPlayer();
        }
        return instance;
    }

    public ZeroLagAudioPlayer() {
        this.volume = 100;
        this.audioRoute = AudioPathControl.AUDIO_PATH_HANDSFREE;
    }

    // =========================================================================
    // Réception du Protocole Bluetooth SPP / RFCOMM
    // =========================================================================

    /**
     * 1. Paquet d'amorce : "AUDIO_START\n"
     * Démarre le thread de lecture temps réel et réinitialise les compteurs.
     */
    public synchronized void onAudioStart() {
        start();
    }

    /**
     * 2. Paquets de flux continu : "AUDIO_CHUNK|<base64_wav>\n" (toutes les 250 ms)
     * Décode la chaîne Base64 et applique la politique stricte Drop-if-Lagging.
     */
    public void onAudioChunk(String base64Wav) {
        if (base64Wav == null || base64Wav.length() == 0) {
            return;
        }

        // Auto-démarrage si AUDIO_START n'a pas été envoyé avant
        if (!isRunning) {
            start();
        }

        try {
            // Décodage Base64 pur Java rapide sans dépendance externe
            byte[] wavBytes = FastBase64.decode(base64Wav);
            if (wavBytes != null && wavBytes.length >= 44) {
                synchronized (bufferLock) {
                    chunksReceived++;
                    // ANTI-LATENCE : Si un bloc était déjà en attente, il est jeté immédiatement !
                    if (pendingChunk != null) {
                        chunksDropped++;
                    }
                    // On ne conserve QUE le bloc le plus récent
                    pendingChunk = wavBytes;
                    bufferLock.notify(); // Réveil immédiat du thread ouvrier
                }
            }
        } catch (Throwable t) {
            System.out.println("[ZeroLagAudio] Erreur réception: " + t.getMessage());
        }
    }

    /**
     * 3. Paquet de fin : "AUDIO_STOP\n"
     * Libère toutes les ressources matérielles et coupe la lecture instantanément.
     */
    public synchronized void onAudioStop() {
        stop();
    }

    // =========================================================================
    // Démarrage / Arrêt du Moteur
    // =========================================================================

    /**
     * Initialise et démarre le thread haute priorité.
     */
    public synchronized void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        chunksReceived = 0;
        chunksPlayed = 0;
        chunksDropped = 0;

        synchronized (bufferLock) {
            pendingChunk = null;
        }

        workerThread = new Thread(this, "ZeroLagAudio-Worker");
        // Priorité maximale pour garantir l'exécution temps réel sans saccades J2ME
        workerThread.setPriority(Thread.MAX_PRIORITY);
        workerThread.start();
        System.out.println("[ZeroLagAudio] Démarrage flux temps réel 250ms.");
    }

    /**
     * Arrête le lecteur et libère la mémoire ainsi que le processeur audio.
     */
    public synchronized void stop() {
        isRunning = false;

        synchronized (bufferLock) {
            pendingChunk = null;
            bufferLock.notifyAll();
        }

        // Fermeture immédiate du Player J2ME actif
        closeActivePlayer();

        workerThread = null;
        System.out.println("[ZeroLagAudio] Arrêt. Reçus=" + chunksReceived + ", Joués=" + chunksPlayed + ", Jetés=" + chunksDropped);
    }

    public boolean isRunning() {
        return isRunning;
    }

    // =========================================================================
    // Boucle de Lecture Haute Performance (Worker Thread)
    // =========================================================================

    public void run() {
        while (isRunning) {
            byte[] chunkToPlay = null;

            // Récupération atomique du bloc le plus récent
            synchronized (bufferLock) {
                while (pendingChunk == null && isRunning) {
                    try {
                        bufferLock.wait(250);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                if (!isRunning) {
                    break;
                }

                // Prélèvement du bloc frais
                chunkToPlay = pendingChunk;
                pendingChunk = null; // Emplacement libéré
            }

            if (chunkToPlay != null && chunkToPlay.length >= 44) {
                playSingleChunk(chunkToPlay);
            }
        }

        closeActivePlayer();
    }

    /**
     * Lecture d'un bloc de 250 ms avec routage audio, volume 100% et commutation sans latence.
     */
    private void playSingleChunk(byte[] wavBytes) {
        Player player = null;
        ByteArrayInputStream bais = null;

        try {
            // 1. Fermer scrupuleusement l'ancien Player avant d'en ouvrir un nouveau
            // Cela libère la mémoire native et le codec matériel du Curve 9300
            closeActivePlayer();

            // 2. Création du nouveau Player via javax.microedition.media.Manager
            bais = new ByteArrayInputStream(wavBytes);
            try {
                player = Manager.createPlayer(bais, "audio/x-wav");
            } catch (Exception ex1) {
                bais.reset();
                try {
                    player = Manager.createPlayer(bais, "audio/wav");
                } catch (Exception ex2) {
                    bais.reset();
                    player = Manager.createPlayer(bais, null);
                }
            }

            if (player == null) {
                return;
            }

            synchronized (playerLock) {
                activePlayer = player;
                chunkFinished = false;
            }

            player.addPlayerListener(this);

            // 3. Initialisation matérielle immédiate
            player.realize();
            player.prefetch();

            // 4. Configuration du routage audio (Haut-parleur externe ou casque)
            applyAudioRouting(player);

            // 5. Configuration du VolumeControl à 100% (Volume maximum)
            applyVolumeControl(player);

            // 6. Démarrage de la lecture
            player.start();

            // 7. Surveillance active de la durée de lecture (250 ms nominal)
            // Si un nouveau paquet arrive et qu'on approche de 250 ms, on passe directement au suivant !
            long startTime = System.currentTimeMillis();
            while (isRunning && !chunkFinished) {
                long elapsed = System.currentTimeMillis() - startTime;

                // Si le bloc est fini ou a dépassé 260 ms, on sort
                if (elapsed >= 260 || player.getState() != Player.STARTED) {
                    break;
                }

                // Si un NOUVEAU paquet est arrivé dans le tampon et qu'on a déjà joué au moins 210 ms,
                // on coupe pour éviter tout décalage temporel
                if (pendingChunk != null && elapsed >= 210) {
                    break;
                }

                try {
                    Thread.sleep(12);
                } catch (InterruptedException ie) {
                    break;
                }
            }

            chunksPlayed++;

        } catch (Throwable t) {
            System.out.println("[ZeroLagAudio] Erreur lecture: " + t.getMessage());
        } finally {
            // Nettoyage immédiat des flux mémoire
            if (bais != null) {
                try { bais.close(); } catch (Throwable ignored) {}
            }
            closeActivePlayer();
        }
    }

    /**
     * Fermeture propre du Player J2ME.
     */
    private void closeActivePlayer() {
        synchronized (playerLock) {
            if (activePlayer != null) {
                try {
                    activePlayer.removePlayerListener(this);
                    if (activePlayer.getState() == Player.STARTED) {
                        activePlayer.stop();
                    }
                    activePlayer.deallocate();
                    activePlayer.close();
                } catch (Throwable ignored) {}
                activePlayer = null;
            }
            chunkFinished = true;
        }
    }

    /**
     * Configure le volume maximal à 100% via VolumeControl.
     */
    private void applyVolumeControl(Player player) {
        if (player == null) return;
        try {
            VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");
            if (vc == null) {
                vc = (VolumeControl) player.getControl("javax.microedition.media.control.VolumeControl");
            }
            if (vc != null) {
                vc.setLevel(volume); // 100%
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Achemine le son vers le haut-parleur principal ou le casque 3.5mm du Curve 9300.
     */
    private void applyAudioRouting(Player player) {
        if (player == null) return;
        try {
            AudioPathControl apc = (AudioPathControl) player.getControl("net.rim.device.api.media.control.AudioPathControl");
            if (apc == null) {
                apc = (AudioPathControl) player.getControl("AudioPathControl");
            }

            if (apc != null) {
                // Prise Jack 3.5mm prioritaire si branchée
                if (apc.canSwitchToPath(AudioPathControl.AUDIO_PATH_HEADSET)) {
                    apc.setAudioPath(AudioPathControl.AUDIO_PATH_HEADSET);
                } else if (apc.canSwitchToPath(audioRoute)) {
                    apc.setAudioPath(audioRoute);
                } else if (apc.canSwitchToPath(AudioPathControl.AUDIO_PATH_HANDSFREE)) {
                    apc.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE);
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Écouteur JSR-135 PlayerListener.
     */
    public void playerUpdate(Player player, String event, Object eventData) {
        if (PlayerListener.END_OF_MEDIA.equals(event) || PlayerListener.STOPPED.equals(event) || PlayerListener.ERROR.equals(event)) {
            chunkFinished = true;
        }
    }

    // =========================================================================
    // Méthodes Publiques Utilitaires
    // =========================================================================

    public synchronized void setVolume(int level) {
        this.volume = Math.max(0, Math.min(100, level));
        synchronized (playerLock) {
            if (activePlayer != null) {
                applyVolumeControl(activePlayer);
            }
        }
    }

    public int getVolume() {
        return volume;
    }

    public synchronized void setAudioRoute(int route) {
        this.audioRoute = route;
        synchronized (playerLock) {
            if (activePlayer != null) {
                applyAudioRouting(activePlayer);
            }
        }
    }

    public synchronized void setSpeakerphone(boolean speakerOn) {
        setAudioRoute(speakerOn ? AudioPathControl.AUDIO_PATH_HANDSFREE : AudioPathControl.AUDIO_PATH_HANDSET);
    }

    public boolean isSpeakerphoneOn() {
        return audioRoute == AudioPathControl.AUDIO_PATH_HANDSFREE;
    }

    public int getChunksReceived() {
        return chunksReceived;
    }

    public int getChunksPlayed() {
        return chunksPlayed;
    }

    public int getChunksDropped() {
        return chunksDropped;
    }
}
