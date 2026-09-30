package com.hamza.blackberrybridge.audio;

import java.io.ByteArrayInputStream;
import java.util.Vector;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import net.rim.device.api.media.control.AudioPathControl;

/**
 * Récepteur et lecteur audio streaming optimisé pour BlackBerry Curve 9300 (OS 5.0 à 7.1 / 6.0).
 *
 * Spécifications du flux :
 * - Format : RIFF WAV 8000 Hz, 16-bit PCM, Mono
 * - Bloc : 8044 octets (44 octets en-tête RIFF/WAVE + 8000 octets PCM = 500 ms)
 * - Encodage réseau : Base64 sans retour à la ligne
 *
 * Fonctionnalités implémentées :
 * 1. Décodage Base64 pur Java rapide et sans allocation excessive.
 * 2. Gestionnaire J2ME JSR-135 avec Player javax.microedition.media.Manager.
 * 3. Contrôle du volume à 100% (VolumeControl) et routage matériel vers Haut-parleur / Écouteur (AudioPathControl).
 * 4. Double tampon (Double Buffering) et thread ouvrier dédié à priorité maximale pour enchaînement sans micro-coupures.
 * 5. Fermeture et libération propre des ressources matérielles sur réception de "AUDIO_STOP".
 */
public class AudioStreamReceiver implements Runnable, PlayerListener {

    private static AudioStreamReceiver instance;

    // File d'attente / Double tampon des blocs WAV décodés
    private final Vector bufferQueue = new Vector();
    private static final int MAX_BUFFER_CAPACITY = 4; // Max 2 secondes d'audio pour éviter tout décalage
    private static final int INITIAL_PREBUFFER_CHUNKS = 1; // Tampon minimal pour fluidifier l'enchaînement

    // Contrôle d'exécution du Thread ouvrier
    private volatile boolean isRunning = false;
    private Thread workerThread;
    private final Object stateLock = new Object();

    // Player multimédia JSR-135 en cours de lecture
    private Player currentPlayer;
    private final Object playerLock = new Object();
    private volatile boolean currentChunkFinished = false;

    // Configuration audio
    private int volumeLevel = 100; // 0 à 100
    private int targetAudioRoute = AudioPathControl.AUDIO_PATH_HANDSFREE; // Haut-parleur par défaut
    private int totalChunksReceived = 0;
    private int totalChunksPlayed = 0;

    /**
     * Accès Singleton pour le récepteur audio.
     */
    public static synchronized AudioStreamReceiver getInstance() {
        if (instance == null) {
            instance = new AudioStreamReceiver();
        }
        return instance;
    }

    public AudioStreamReceiver() {
        this.targetAudioRoute = AudioPathControl.AUDIO_PATH_HANDSFREE;
        this.volumeLevel = 100;
    }

    // =========================================================================
    // Protocole Réseau Bluetooth SPP / RFCOMM
    // =========================================================================

    /**
     * 1. Paquet d'initialisation : "AUDIO_START\n"
     * Démarre le moteur audio et initialise le thread de double-tampon.
     */
    public void onAudioStart() {
        start();
    }

    /**
     * 2. Paquets de flux continu : "AUDIO_CHUNK|<base64_wav>\n"
     * Reçoit la chaîne Base64, la décode et l'ajoute au double-tampon.
     */
    public void onAudioChunk(String base64Data) {
        if (base64Data == null || base64Data.length() == 0) {
            return;
        }

        // Auto-démarrage si le flux commence sans AUDIO_START préalable
        if (!isRunning) {
            start();
        }

        try {
            // Décodage Base64 standard ultra-rapide sans dépendance externe
            byte[] wavBytes = FastBase64.decode(base64Data);
            if (wavBytes != null && wavBytes.length >= 44) {
                enqueueWavChunk(wavBytes);
            }
        } catch (Throwable t) {
            System.out.println("[AudioStreamReceiver] Erreur décodage: " + t.getMessage());
        }
    }

    /**
     * 3. Paquet de fin : "AUDIO_STOP\n"
     * Arrête immédiatement le flux et libère toutes les ressources matérielles.
     */
    public void onAudioStop() {
        stop();
    }

    // =========================================================================
    // Cycle de vie du moteur audio
    // =========================================================================

    /**
     * Démarre la réception et la lecture séquentielle continue.
     */
    public synchronized void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        totalChunksReceived = 0;
        totalChunksPlayed = 0;

        synchronized (bufferQueue) {
            bufferQueue.removeAllElements();
        }

        workerThread = new Thread(this, "AudioStreamReceiver-Worker");
        // Priorité maximale pour éliminer toute saccade audio lors du traitement J2ME
        workerThread.setPriority(Thread.MAX_PRIORITY);
        workerThread.start();
        System.out.println("[AudioStreamReceiver] Moteur audio démarré (WAV 8000Hz 16-bit Mono).");
    }

    /**
     * Arrête la lecture et libère le lecteur audio J2ME.
     */
    public synchronized void stop() {
        isRunning = false;

        // Vider la file de tampons
        synchronized (bufferQueue) {
            bufferQueue.removeAllElements();
            bufferQueue.notifyAll();
        }

        // Interrompre le Player en cours
        closeCurrentPlayer();

        // Réveiller l'attente du worker
        synchronized (stateLock) {
            stateLock.notifyAll();
        }

        workerThread = null;
        System.out.println("[AudioStreamReceiver] Moteur audio arrêté proprement. Paquets joués: " + totalChunksPlayed);
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Ajoute un bloc de données audio WAV au double-tampon avec politique anti-latence.
     */
    private void enqueueWavChunk(byte[] wavBytes) {
        synchronized (bufferQueue) {
            totalChunksReceived++;

            // Politique anti-accumulation : si plus de 4 blocs (2 secondes de retard)
            // on élimine le plus ancien pour conserver le temps réel
            if (bufferQueue.size() >= MAX_BUFFER_CAPACITY) {
                bufferQueue.removeElementAt(0);
            }

            bufferQueue.addElement(wavBytes);
            bufferQueue.notify(); // Réveille le thread de lecture
        }
    }

    // =========================================================================
    // Boucle de lecture séquentielle / Double Tampon (Worker Thread)
    // =========================================================================

    public void run() {
        while (isRunning) {
            byte[] currentChunk = null;

            // 1. Récupération du prochain bloc dans le double-tampon
            synchronized (bufferQueue) {
                while (bufferQueue.isEmpty() && isRunning) {
                    try {
                        bufferQueue.wait(500);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }

                if (!isRunning) {
                    break;
                }

                if (!bufferQueue.isEmpty()) {
                    currentChunk = (byte[]) bufferQueue.firstElement();
                    bufferQueue.removeElementAt(0);
                }
            }

            // 2. Lecture du bloc WAV de 500 ms
            if (currentChunk != null && currentChunk.length >= 44) {
                playWavChunk(currentChunk);
            }
        }

        // Nettoyage final à la fin de la boucle
        closeCurrentPlayer();
    }

    /**
     * Instancie et joue un bloc WAV 500 ms avec configuration du volume et de l'AudioPath.
     */
    private void playWavChunk(byte[] wavBytes) {
        Player player = null;
        ByteArrayInputStream bais = null;

        try {
            bais = new ByteArrayInputStream(wavBytes);

            // Création du Player JSR-135 avec le type MIME audio/x-wav (ou fallback audio/wav)
            try {
                player = Manager.createPlayer(bais, "audio/x-wav");
            } catch (Exception e1) {
                try {
                    bais.reset();
                    player = Manager.createPlayer(bais, "audio/wav");
                } catch (Exception e2) {
                    bais.reset();
                    player = Manager.createPlayer(bais, null);
                }
            }

            if (player == null) {
                return;
            }

            synchronized (playerLock) {
                currentPlayer = player;
                currentChunkFinished = false;
            }

            // Enregistrement de l'écouteur d'événements pour détecter END_OF_MEDIA
            player.addPlayerListener(this);

            // Phase 1 : Realize & Prefetch (optimisation du pipeline audio matériel)
            player.realize();
            player.prefetch();

            // Phase 2 : Routage matériel du son vers l'écouteur, haut-parleur ou jack 3.5mm
            applyAudioRouting(player);

            // Phase 3 : Réglage du volume sonore au maximum (VolumeControl)
            applyVolumeLevel(player);

            // Phase 4 : Démarrage de la lecture
            player.start();

            // Phase 5 : Attente synchrone du bloc (500 ms nominal, timeout de sécurité à 550 ms)
            long startTime = System.currentTimeMillis();
            while (isRunning && !currentChunkFinished) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= 520 || player.getState() != Player.STARTED) {
                    break;
                }
                try {
                    Thread.sleep(15);
                } catch (InterruptedException ie) {
                    break;
                }
            }

            totalChunksPlayed++;

        } catch (Throwable t) {
            System.out.println("[AudioStreamReceiver] Erreur lecture chunk: " + t.getMessage());
        } finally {
            // Libération immédiate du player pour éviter tout verrouillage du DSP Curve 9300
            if (player != null) {
                try {
                    player.removePlayerListener(this);
                    if (player.getState() == Player.STARTED) {
                        player.stop();
                    }
                    player.deallocate();
                    player.close();
                } catch (Throwable ignored) {}
            }
            if (bais != null) {
                try {
                    bais.close();
                } catch (Throwable ignored) {}
            }
            synchronized (playerLock) {
                if (currentPlayer == player) {
                    currentPlayer = null;
                }
            }
        }
    }

    /**
     * Application du routage audio matériel (AudioPathControl) sur BlackBerry Curve 9300.
     */
    private void applyAudioRouting(Player player) {
        if (player == null) return;
        try {
            AudioPathControl apc = (AudioPathControl) player.getControl("net.rim.device.api.media.control.AudioPathControl");
            if (apc == null) {
                apc = (AudioPathControl) player.getControl("AudioPathControl");
            }

            if (apc != null) {
                // Casque filaire 3.5 mm prioritaire si branché
                if (apc.canSwitchToPath(AudioPathControl.AUDIO_PATH_HEADSET)) {
                    apc.setAudioPath(AudioPathControl.AUDIO_PATH_HEADSET);
                } else if (apc.canSwitchToPath(targetAudioRoute)) {
                    apc.setAudioPath(targetAudioRoute);
                } else if (apc.canSwitchToPath(AudioPathControl.AUDIO_PATH_HANDSFREE)) {
                    apc.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE);
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Réglage du volume sonore via VolumeControl.
     */
    private void applyVolumeLevel(Player player) {
        if (player == null) return;
        try {
            VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");
            if (vc == null) {
                vc = (VolumeControl) player.getControl("javax.microedition.media.control.VolumeControl");
            }
            if (vc != null) {
                vc.setLevel(volumeLevel);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Écouteur PlayerListener pour détecter la fin de lecture du bloc 500 ms.
     */
    public void playerUpdate(Player player, String event, Object eventData) {
        if (PlayerListener.END_OF_MEDIA.equals(event) || PlayerListener.STOPPED.equals(event) || PlayerListener.ERROR.equals(event)) {
            currentChunkFinished = true;
        }
    }

    /**
     * Ferme proprement le Player en cours.
     */
    private void closeCurrentPlayer() {
        synchronized (playerLock) {
            if (currentPlayer != null) {
                try {
                    currentPlayer.removePlayerListener(this);
                    if (currentPlayer.getState() == Player.STARTED) {
                        currentPlayer.stop();
                    }
                    currentPlayer.deallocate();
                    currentPlayer.close();
                } catch (Throwable ignored) {}
                currentPlayer = null;
            }
            currentChunkFinished = true;
        }
    }

    // =========================================================================
    // Méthodes de configuration externe
    // =========================================================================

    /**
     * Règle le volume sonore (0 à 100).
     */
    public synchronized void setVolume(int level) {
        this.volumeLevel = Math.max(0, Math.min(100, level));
        synchronized (playerLock) {
            if (currentPlayer != null) {
                applyVolumeLevel(currentPlayer);
            }
        }
    }

    public int getVolume() {
        return volumeLevel;
    }

    /**
     * Configure le routage vers l'écouteur ou le haut-parleur.
     * @param route AudioPathControl.AUDIO_PATH_HANDSFREE ou AUDIO_PATH_HANDSET
     */
    public synchronized void setAudioRoute(int route) {
        this.targetAudioRoute = route;
        synchronized (playerLock) {
            if (currentPlayer != null) {
                applyAudioRouting(currentPlayer);
            }
        }
    }

    /**
     * Bascule entre Haut-parleur et Écouteur combiné.
     */
    public synchronized void toggleAudioRoute() {
        if (targetAudioRoute == AudioPathControl.AUDIO_PATH_HANDSFREE) {
            targetAudioRoute = AudioPathControl.AUDIO_PATH_HANDSET;
        } else {
            targetAudioRoute = AudioPathControl.AUDIO_PATH_HANDSFREE;
        }
        setAudioRoute(targetAudioRoute);
    }

    /**
     * Active ou désactive le haut-parleur externe.
     */
    public synchronized void setSpeakerphone(boolean speakerOn) {
        setAudioRoute(speakerOn ? AudioPathControl.AUDIO_PATH_HANDSFREE : AudioPathControl.AUDIO_PATH_HANDSET);
    }

    public boolean isSpeakerphoneOn() {
        return targetAudioRoute == AudioPathControl.AUDIO_PATH_HANDSFREE;
    }

    public int getTotalChunksReceived() {
        return totalChunksReceived;
    }

    public int getTotalChunksPlayed() {
        return totalChunksPlayed;
    }

    public int getQueuedChunksCount() {
        synchronized (bufferQueue) {
            return bufferQueue.size();
        }
    }
}
