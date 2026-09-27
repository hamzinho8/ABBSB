package com.hamza.blackberrybridge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.media.control.AudioPathControl;

/**
 * AudioQueueWorker pour BlackBerry Curve 9300 (OS 5.0 à 7.1).
 * 
 * Découplage complet du thread de réception Bluetooth et de la lecture audio J2ME MMAPI :
 * 1. File d'attente FIFO (Vector de byte[]) circulaire bornée à 10 paquets max (400 ms).
 *    Si la file dépasse 10 paquets, les plus anciens sont immédiatement purgés pour
 *    éliminer toute latence/décalage et éviter la saturation mémoire (RAM).
 * 2. Réception en moins de 1 ms sur le thread Bluetooth sans aucun blocage I/O.
 * 3. Thread de lecture dédié en tâche de fond qui accumule 3 blocs (120 ms = 1920 octets)
 *    et les joue avec en-tête WAV 44 octets via J2ME Manager.createPlayer("audio/x-wav").
 * 4. Contrôle du volume local et bascule écouteur combiné (HANDSET) / haut-parleur (HANDSFREE).
 * 5. Nettoyage et libération immédiate des ressources Player dès réception de CALL_END ou VOICE_STOP.
 */
public class AudioQueueWorker implements Runnable {
    private static AudioQueueWorker instance;

    // File d'attente circulaire de trames PCM
    private final Vector queue = new Vector();
    private static final int MAX_QUEUE_SIZE = 10; // Max 10 trames (400 ms) pour éviter tout lag et préserver la RAM
    private static final int BATCH_BLOCKS = 3;    // Accumulation de 3 blocs (120 ms) pour fluidité optimale J2ME

    private Thread workerThread;
    private volatile boolean running = false;
    private volatile boolean isPlaying = false;

    // Paramètres audio
    private int sampleRate = 8000;
    private int channels = 1;
    private int bitsPerSample = 16;
    private int volume = 100; // 0 - 100%
    private int audioPath = AudioPathControl.AUDIO_PATH_HANDSET; // Écouteur par défaut

    // Player J2ME actif
    private Player currentPlayer;
    private final Object playerLock = new Object();
    private boolean playerCompleted = false;

    // Compteur de paquets reçus
    private long totalPacketsReceived = 0;
    private AudioStatusListener statusListener;

    public interface AudioStatusListener {
        void onAudioPacketReceived(long packetCount, boolean isSpeaker);
    }

    public static synchronized AudioQueueWorker getInstance() {
        if (instance == null) {
            instance = new AudioQueueWorker();
        }
        return instance;
    }

    private AudioQueueWorker() {
    }

    public void setAudioStatusListener(AudioStatusListener listener) {
        this.statusListener = listener;
    }

    /**
     * Démarre le traitement audio et le thread d'arrière-plan.
     */
    public synchronized void startAudioStream(int rate, int ch, int bits) {
        this.sampleRate = (rate > 0) ? rate : 8000;
        this.channels = (ch > 0) ? ch : 1;
        this.bitsPerSample = (bits > 0) ? bits : 16;

        if (running && workerThread != null && workerThread.isAlive()) {
            LogManager.log("AUDIO_QUEUE", "Worker already running, resetting queue");
            synchronized (queue) {
                queue.removeAllElements();
                queue.notifyAll();
            }
            return;
        }

        running = true;
        totalPacketsReceived = 0;
        synchronized (queue) {
            queue.removeAllElements();
        }

        workerThread = new Thread(this);
        workerThread.setPriority(Thread.MAX_PRIORITY - 1); // Priorité élevée pour audio fluide sans affamer l'UI
        workerThread.start();

        LogManager.log("AUDIO_QUEUE", "AudioQueueWorker started (8000Hz 16-bit Mono, max 10 packets queue)");
    }

    /**
     * Ajout non-bloquant depuis le thread Bluetooth (exécuté en < 1 ms).
     */
    public void enqueueBase64(String base64Data) {
        if (!running || base64Data == null || base64Data.length() == 0) return;

        byte[] pcmData = decodeBase64(base64Data);
        if (pcmData != null && pcmData.length > 0) {
            enqueue(pcmData);
        }
    }

    /**
     * Ajoute un paquet de données PCM décodées dans la file d'attente.
     */
    public void enqueue(byte[] pcmData) {
        if (!running || pcmData == null || pcmData.length == 0) return;

        synchronized (queue) {
            // Sécurité mémoire Curve 9300 : si la file dépasse 10 paquets, jeter les plus anciens
            while (queue.size() >= MAX_QUEUE_SIZE) {
                queue.removeElementAt(0);
            }
            queue.addElement(pcmData);
            totalPacketsReceived++;
            queue.notify();
        }

        // Notification de réception pour mise à jour de l'indicateur d'écran
        if (statusListener != null && (totalPacketsReceived % 5 == 1)) {
            final long count = totalPacketsReceived;
            final boolean spk = isSpeakerOn();
            try {
                statusListener.onAudioPacketReceived(count, spk);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Boucle principale du thread d'arrière-plan.
     */
    public void run() {
        ByteArrayOutputStream batchBuffer = new ByteArrayOutputStream(2048);

        while (running) {
            try {
                batchBuffer.reset();
                int collectedBlocks = 0;

                // 1. Extraire jusqu'à BATCH_BLOCKS (120ms) ou attendre
                synchronized (queue) {
                    while (queue.isEmpty() && running) {
                        queue.wait(200);
                    }
                    if (!running) break;

                    while (!queue.isEmpty() && collectedBlocks < BATCH_BLOCKS) {
                        byte[] chunk = (byte[]) queue.elementAt(0);
                        queue.removeElementAt(0);
                        batchBuffer.write(chunk, 0, chunk.length);
                        collectedBlocks++;
                    }
                }

                if (!running) break;

                // 2. Jouer le bloc accumulé s'il contient des données
                byte[] pcmBatch = batchBuffer.toByteArray();
                if (pcmBatch != null && pcmBatch.length > 0) {
                    playPcmBatch(pcmBatch);
                }

            } catch (InterruptedException ie) {
                break;
            } catch (Throwable t) {
                LogManager.error("AUDIO_QUEUE", "Playback error in worker loop: " + t.getMessage());
            }
        }

        // Nettoyage final
        stopCurrentPlayer();
        synchronized (queue) {
            queue.removeAllElements();
        }
        isPlaying = false;
        LogManager.log("AUDIO_QUEUE", "AudioQueueWorker thread terminated cleanly");
    }

    /**
     * Joue un lot de données PCM en y adjoignant un en-tête WAV standard de 44 octets.
     */
    private void playPcmBatch(byte[] pcmData) {
        if (!running || pcmData == null || pcmData.length == 0) return;

        Player p = null;
        try {
            int totalBytes = pcmData.length;
            byte[] wavHeader = createWavHeader(sampleRate, channels, bitsPerSample, totalBytes);
            byte[] fullWav = new byte[44 + totalBytes];
            System.arraycopy(wavHeader, 0, fullWav, 0, 44);
            System.arraycopy(pcmData, 0, fullWav, 44, totalBytes);

            ByteArrayInputStream bais = new ByteArrayInputStream(fullWav);
            p = Manager.createPlayer(bais, "audio/x-wav");
            p.realize();
            p.prefetch();

            // Volume Control
            VolumeControl vc = (VolumeControl) p.getControl("VolumeControl");
            if (vc != null) {
                vc.setLevel(volume);
            }

            // Audio Path Control (Écouteur vs Haut-parleur)
            AudioPathControl apc = (AudioPathControl) p.getControl("AudioPathControl");
            if (apc != null) {
                try {
                    apc.setAudioPath(audioPath);
                } catch (Throwable ignored) {}
            }

            // Gestion de l'achèvement par PlayerListener
            playerCompleted = false;
            p.addPlayerListener(new PlayerListener() {
                public void playerUpdate(Player player, String event, Object eventData) {
                    if (PlayerListener.END_OF_MEDIA.equals(event) || 
                        PlayerListener.STOPPED_AT_TIME.equals(event) || 
                        PlayerListener.ERROR.equals(event)) {
                        synchronized (playerLock) {
                            playerCompleted = true;
                            playerLock.notifyAll();
                        }
                    }
                }
            });

            synchronized (this) {
                if (!running) {
                    closePlayer(p);
                    return;
                }
                currentPlayer = p;
                isPlaying = true;
            }

            p.start();

            // Durée théorique en millisecondes : (totalBytes / (8000 * 2)) * 1000
            int durationMs = (totalBytes * 1000) / (sampleRate * channels * (bitsPerSample / 8));
            int waitTimeout = durationMs + 40;

            synchronized (playerLock) {
                if (!playerCompleted && running) {
                    playerLock.wait(waitTimeout);
                }
            }

        } catch (Throwable t) {
            LogManager.error("AUDIO_QUEUE", "Error playing PCM batch: " + t.getMessage());
        } finally {
            closePlayer(p);
            synchronized (this) {
                if (currentPlayer == p) {
                    currentPlayer = null;
                }
            }
        }
    }

    private void closePlayer(Player p) {
        if (p == null) return;
        try {
            if (p.getState() == Player.STARTED) {
                p.stop();
            }
        } catch (Throwable ignored) {}
        try {
            p.deallocate();
        } catch (Throwable ignored) {}
        try {
            p.close();
        } catch (Throwable ignored) {}
    }

    private void stopCurrentPlayer() {
        synchronized (this) {
            if (currentPlayer != null) {
                closePlayer(currentPlayer);
                currentPlayer = null;
            }
            synchronized (playerLock) {
                playerCompleted = true;
                playerLock.notifyAll();
            }
        }
    }

    /**
     * Arrête immédiatement la lecture et libère la totalité des ressources mémoire.
     */
    public synchronized void stopAudioStream() {
        running = false;
        isPlaying = false;

        // Réveiller le thread de traitement pour sortie immédiate
        synchronized (queue) {
            queue.removeAllElements();
            queue.notifyAll();
        }

        stopCurrentPlayer();

        if (workerThread != null) {
            try {
                workerThread.interrupt();
            } catch (Throwable ignored) {}
            workerThread = null;
        }

        LogManager.log("AUDIO_QUEUE", "AudioQueueWorker stopped and resources released");
    }

    /**
     * Génère un en-tête WAV 44 octets standard pour format PCM linéaire.
     */
    public static byte[] createWavHeader(int sampleRate, int channels, int bitsPerSample, int pcmDataLen) {
        byte[] header = new byte[44];
        int totalDataLen = pcmDataLen + 36;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0; // SubChunk1Size (16 pour PCM)
        header[20] = 1; header[21] = 0; // AudioFormat (1 = PCM)
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) blockAlign; header[33] = 0;
        header[34] = (byte) bitsPerSample; header[35] = 0;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (pcmDataLen & 0xff);
        header[41] = (byte) ((pcmDataLen >> 8) & 0xff);
        header[42] = (byte) ((pcmDataLen >> 16) & 0xff);
        header[43] = (byte) ((pcmDataLen >> 24) & 0xff);
        return header;
    }

    /**
     * Décodeur Base64 ultra-rapide avec tolérance aux sauts de ligne.
     */
    public static byte[] decodeBase64(String s) {
        if (s == null || s.length() == 0) return new byte[0];
        try {
            byte[] rimResult = Base64InputStream.decode(s);
            if (rimResult != null && rimResult.length > 0) {
                return rimResult;
            }
        } catch (Throwable ignored) {}

        // Décodeur manuel CLDC 1.1 en secours
        return manualBase64Decode(s);
    }

    private static byte[] manualBase64Decode(String s) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(s.length() * 3 / 4);
        int b1, b2, b3, b4;
        int i = 0;
        int len = s.length();

        while (i < len) {
            char c1 = s.charAt(i++);
            while (i < len && c1 <= ' ') c1 = s.charAt(i++);
            if (i > len || c1 <= ' ') break;

            char c2 = (i < len) ? s.charAt(i++) : ' ';
            while (i < len && c2 <= ' ') c2 = s.charAt(i++);

            char c3 = (i < len) ? s.charAt(i++) : ' ';
            while (i < len && c3 <= ' ') c3 = s.charAt(i++);

            char c4 = (i < len) ? s.charAt(i++) : ' ';
            while (i < len && c4 <= ' ') c4 = s.charAt(i++);

            b1 = decodeChar(c1);
            b2 = decodeChar(c2);
            b3 = decodeChar(c3);
            b4 = decodeChar(c4);

            if (b1 != -1 && b2 != -1) {
                bos.write((b1 << 2) | (b2 >> 4));
                if (b3 != -1) {
                    bos.write(((b2 & 0x0f) << 4) | (b3 >> 2));
                    if (b4 != -1) {
                        bos.write(((b3 & 0x03) << 6) | b4);
                    }
                }
            }
        }
        return bos.toByteArray();
    }

    private static int decodeChar(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+') return 62;
        if (c == '/') return 63;
        return -1;
    }

    // =========================================================================
    // Contrôle du Volume et du Routage Audio (Combiné / Haut-parleur)
    // =========================================================================

    public synchronized void setVolume(int vol) {
        this.volume = Math.max(0, Math.min(100, vol));
        if (currentPlayer != null) {
            try {
                VolumeControl vc = (VolumeControl) currentPlayer.getControl("VolumeControl");
                if (vc != null) vc.setLevel(this.volume);
            } catch (Throwable ignored) {}
        }
    }

    public synchronized void adjustVolume(int delta) {
        setVolume(this.volume + delta);
    }

    public int getVolume() {
        return volume;
    }

    public synchronized boolean toggleAudioPath() {
        if (audioPath == AudioPathControl.AUDIO_PATH_HANDSET) {
            setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE);
            return true;
        } else {
            setAudioPath(AudioPathControl.AUDIO_PATH_HANDSET);
            return false;
        }
    }

    public synchronized void setAudioPath(int path) {
        this.audioPath = path;
        if (currentPlayer != null) {
            try {
                AudioPathControl apc = (AudioPathControl) currentPlayer.getControl("AudioPathControl");
                if (apc != null) apc.setAudioPath(audioPath);
            } catch (Throwable ignored) {}
        }
    }

    public int getAudioPath() {
        return audioPath;
    }

    public boolean isSpeakerOn() {
        return audioPath == AudioPathControl.AUDIO_PATH_HANDSFREE;
    }

    public boolean isStreaming() {
        return running;
    }

    public int getQueueSize() {
        synchronized (queue) {
            return queue.size();
        }
    }
}
