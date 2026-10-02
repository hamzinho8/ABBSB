package com.hamza.blackberrybridge.audio;

import java.io.ByteArrayInputStream;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import net.rim.device.api.media.control.AudioPathControl;

/**
 * Lecteur audio temps réel haute fidélité (Zero-Lag HD) pour BlackBerry Curve 9300 (OS 5.0 à 7.1).
 *
 * AMÉLIORATIONS MAJEURES DE LA QUALITÉ SONORE :
 * 1. Support Haute Définition (HD) : Détection automatique de la fréquence (8000 Hz, 16000 Hz HD Voice, 22050 Hz).
 * 2. Élimination des micro-coupures et bruits de saccade (Handoff Ping-Pong) :
 *    Pré-initialisation du prochain bloc pour enchaîner sans interruption du DAC audio.
 * 3. Filtre Anti-Saturation acoustique :
 *    Atténuation de dynamique (-1.5 dB) sur les échantillons PCM 16-bit pour empêcher
 *    la saturation du petit haut-parleur physique du Curve 9300.
 * 4. Anti-Latence Stricte (Drop-if-Lagging) :
 *    Maintien du direct absolu sans accumulation de mémoire tampon.
 */
public class ZeroLagAudioPlayer implements Runnable, PlayerListener {

    private static ZeroLagAudioPlayer instance;

    // Tampon atomique à emplacement unique (Single-slot buffer)
    private volatile byte[] pendingChunk = null;
    private final Object bufferLock = new Object();

    // Contrôle d'exécution du Thread ouvrier
    private volatile boolean isRunning = false;
    private Thread workerThread;

    // Player multimédia JSR-135 actif
    private Player activePlayer;
    private final Object playerLock = new Object();
    private volatile boolean chunkFinished = false;

    // Configuration et routage matériel
    private int volume = 100; // 0 à 100
    private int audioRoute = AudioPathControl.AUDIO_PATH_HANDSFREE; // Haut-parleur par défaut
    private int detectedSampleRate = 16000; // 16 kHz par défaut pour une clarté optimale

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
     * Paquet d'amorce : "AUDIO_START" ou "AUDIO_START|<sampleRate>|..."
     */
    public synchronized void onAudioStart() {
        start();
    }

    /**
     * Paquets de flux continu : "AUDIO_CHUNK|<base64_wav>\n"
     */
    public void onAudioChunk(String base64Wav) {
        if (base64Wav == null || base64Wav.length() == 0) {
            return;
        }

        if (!isRunning) {
            start();
        }

        try {
            // Décodage Base64 pur Java rapide sans dépendance externe
            byte[] wavBytes = FastBase64.decode(base64Wav);
            if (wavBytes != null && wavBytes.length >= 44) {

                // 1. Analyse du format WAV pour optimiser le rendu (Fréquence d'échantillonnage)
                detectFormat(wavBytes);

                // 2. Traitement acoustique anti-saturation pour haut-parleur Curve 9300
                applyAcousticMastering(wavBytes);

                // 3. Politique Anti-Latence (Drop-if-Lagging)
                synchronized (bufferLock) {
                    chunksReceived++;
                    if (pendingChunk != null) {
                        chunksDropped++; // Ancien bloc non lu écrasé immédiatement
                    }
                    pendingChunk = wavBytes;
                    bufferLock.notify(); // Réveil immédiat du thread de lecture
                }
            }
        } catch (Throwable t) {
            System.out.println("[ZeroLagAudio] Erreur réception: " + t.getMessage());
        }
    }

    /**
     * Paquet de fin : "AUDIO_STOP\n"
     */
    public synchronized void onAudioStop() {
        stop();
    }

    // =========================================================================
    // Analyse et Traitement Acoustique Haute Qualité
    // =========================================================================

    /**
     * Détecte automatiquement la fréquence d'échantillonnage dans l'en-tête WAV (octets 24-27).
     */
    private void detectFormat(byte[] wavBytes) {
        if (wavBytes.length >= 28) {
            int rate = (wavBytes[24] & 0xFF) |
                      ((wavBytes[25] & 0xFF) << 8) |
                      ((wavBytes[26] & 0xFF) << 16) |
                      ((wavBytes[27] & 0xFF) << 24);
            if (rate > 0 && rate != detectedSampleRate) {
                detectedSampleRate = rate;
                System.out.println("[ZeroLagAudio] Fréquence détectée: " + detectedSampleRate + " Hz");
            }
        }
    }

    /**
     * Traitement acoustique préventif :
     * Le haut-parleur interne du Curve 9300 sature et grésille fortement si l'audio numérique
     * dépasse 0 dBFS. On applique un headroom doux (-1.5 dB, 85%) pour garder un son rond,
     * net et chaleureux sans aucune distorsion harmonique.
     */
    private void applyAcousticMastering(byte[] wavBytes) {
        if (wavBytes == null || wavBytes.length < 48) return;

        // Les données PCM 16-bit débutent à l'octet 44
        for (int i = 44; i + 1 < wavBytes.length; i += 2) {
            int low = wavBytes[i] & 0xFF;
            int high = wavBytes[i + 1];
            int sample = (short)((high << 8) | low);

            // Atténuation douce à 85% de la crête pour éviter l'écrêtage analogique
            sample = (sample * 85) / 100;

            wavBytes[i] = (byte)(sample & 0xFF);
            wavBytes[i + 1] = (byte)((sample >> 8) & 0xFF);
        }
    }

    // =========================================================================
    // Démarrage / Arrêt du Moteur
    // =========================================================================

    public synchronized void start() {
        if (isRunning) return;

        isRunning = true;
        chunksReceived = 0;
        chunksPlayed = 0;
        chunksDropped = 0;

        synchronized (bufferLock) {
            pendingChunk = null;
        }

        workerThread = new Thread(this, "ZeroLagAudio-HD-Worker");
        workerThread.setPriority(Thread.MAX_PRIORITY);
        workerThread.start();
        System.out.println("[ZeroLagAudio] Moteur HD démarré. Priorité Maximale.");
    }

    public synchronized void stop() {
        isRunning = false;

        synchronized (bufferLock) {
            pendingChunk = null;
            bufferLock.notifyAll();
        }

        closeActivePlayer();
        workerThread = null;
        System.out.println("[ZeroLagAudio] Moteur arrêté. Joués: " + chunksPlayed + " | Jetés: " + chunksDropped);
    }

    public boolean isRunning() {
        return isRunning;
    }

    // =========================================================================
    // Boucle de Lecture Continue Sans Latence (Worker Thread)
    // =========================================================================

    public void run() {
        while (isRunning) {
            byte[] chunkToPlay = null;

            synchronized (bufferLock) {
                while (pendingChunk == null && isRunning) {
                    try {
                        bufferLock.wait(250);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                if (!isRunning) break;

                chunkToPlay = pendingChunk;
                pendingChunk = null;
            }

            if (chunkToPlay != null && chunkToPlay.length >= 44) {
                playSingleChunk(chunkToPlay);
            }
        }

        closeActivePlayer();
    }

    /**
     * Joue un bloc audio avec configuration matérielle et enchaînement optimisé.
     */
    private void playSingleChunk(byte[] wavBytes) {
        Player player = null;
        ByteArrayInputStream bais = null;

        try {
            // Création du Player J2ME
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

            if (player == null) return;

            // Préparation du nouveau player avant d'éteindre l'ancien (réduit le délai inter-blocs)
            player.realize();
            player.prefetch();
            applyAudioRouting(player);
            applyVolumeControl(player);

            player.addPlayerListener(this);

            // Handoff sans interruption : on ferme le précédent juste avant le start
            closeActivePlayer();

            synchronized (playerLock) {
                activePlayer = player;
                chunkFinished = false;
            }

            player.start();

            // Calcul de la durée théorique du bloc (ex: 250 ms)
            // PCM 16-bit Mono = (bytes - 44) / (sampleRate * 2) en secondes
            int pcmBytes = wavBytes.length - 44;
            int bytesPerSecond = (detectedSampleRate > 0 ? detectedSampleRate : 16000) * 2;
            int nominalDurationMs = (pcmBytes > 0 && bytesPerSecond > 0) ? (pcmBytes * 1000 / bytesPerSecond) : 250;
            if (nominalDurationMs <= 0 || nominalDurationMs > 1000) {
                nominalDurationMs = 250;
            }

            // Surveillance temporelle active avec passage direct au bloc suivant si un bloc frais attend
            long startTime = System.currentTimeMillis();
            int maxDuration = nominalDurationMs + 10;
            int switchThreshold = Math.max(50, nominalDurationMs - 30);

            while (isRunning && !chunkFinished) {
                long elapsed = System.currentTimeMillis() - startTime;

                if (elapsed >= maxDuration || player.getState() != Player.STARTED) {
                    break;
                }

                // Si un NOUVEAU paquet attend et qu'on approche de la fin, on enchaîne immédiatement
                if (pendingChunk != null && elapsed >= switchThreshold) {
                    break;
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                    break;
                }
            }

            chunksPlayed++;

        } catch (Throwable t) {
            System.out.println("[ZeroLagAudio] Erreur lecture: " + t.getMessage());
        } finally {
            if (bais != null) {
                try { bais.close(); } catch (Throwable ignored) {}
            }
        }
    }

    /**
     * Fermeture propre du Player actif.
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
                vc.setLevel(volume);
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

    public void playerUpdate(Player player, String event, Object eventData) {
        if (PlayerListener.END_OF_MEDIA.equals(event) || PlayerListener.STOPPED.equals(event) || PlayerListener.ERROR.equals(event)) {
            chunkFinished = true;
        }
    }

    // =========================================================================
    // Méthodes de configuration
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

    public int getDetectedSampleRate() {
        return detectedSampleRate;
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
