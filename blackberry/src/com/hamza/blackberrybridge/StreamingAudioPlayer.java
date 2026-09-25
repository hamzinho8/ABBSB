package com.hamza.blackberrybridge;

import java.io.IOException;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.media.control.AudioPathControl;

/**
 * Lecteur audio streaming temps réel pour BlackBerry Curve 9300 (OS 5.0 / 6.0).
 * Architecture basée sur PipedOutputStream couplé à PipedInputStream (16 Ko ou 32 Ko)
 * avec injection unique au démarrage d'un en-tête WAV 44 octets (taille de flux infinie).
 * 
 * Avantages décisifs :
 * - Un seul Player J2ME (Manager.createPlayer) instancié pour toute la durée de l'appel.
 * - Élimine les instanciations répétées et les fuites mémoire responsables de l'absence de son sur BBOS 6.
 * - Prise en charge native des paquets :
 *   * "VOICE_START|8000|1|16" -> démarrage instantané du Player
 *   * "VOICE_TX|<base64>"      -> écriture directe du chunk PCM dans le tube audio
 *   * "VOICE_STOP" / "CALL_END" -> arrêt et libération propre des ressources
 * - Bascule dynamique Écouteur combiné (HANDSET) / Haut-parleur (HANDSFREE) via AudioPathControl.
 */
public class StreamingAudioPlayer {
    private static StreamingAudioPlayer instance;
    
    private Player player;
    private PipedOutputStream audioOut;
    private PipedInputStream audioIn;
    private volatile boolean isPlaying = false;
    
    private int audioPath = AudioPathControl.AUDIO_PATH_HANDSET; // Écouteur par défaut
    private int volume = 90; // 0 - 100%

    public static synchronized StreamingAudioPlayer getInstance() {
        if (instance == null) {
            instance = new StreamingAudioPlayer();
        }
        return instance;
    }

    public synchronized void startAudioStream() {
        startAudioStream(8000, 1, 16);
    }

    public synchronized void startAudioStream(final int sampleRate, final int channels, final int bitsPerSample) {
        if (isPlaying) {
            LogManager.log("STREAM_PLAYER", "Audio stream already active");
            return;
        }
        
        LogManager.log("STREAM_PLAYER", "Starting audio stream: " + sampleRate + "Hz, " + channels + "ch, " + bitsPerSample + "bit");
        
        try {
            stopAudioStreamInternal();
            
            // 1. Initialisation des flux pipes avec buffer circulaire de 32 Ko (~2 secondes à 8000Hz 16-bit Mono)
            audioOut = new PipedOutputStream();
            audioIn = new PipedInputStream(audioOut, 32768);

            // 2. Écriture unique de l'en-tête WAV 44 octets avec taille de données quasi-infinie (0x7FFFFFFF)
            byte[] wavHeader = createWavHeader(sampleRate, channels, bitsPerSample);
            audioOut.write(wavHeader);
            audioOut.flush();

            // 3. Création unique du Player J2ME avec le type MIME audio/x-wav
            player = Manager.createPlayer(audioIn, "audio/x-wav");
            player.realize();
            player.prefetch();

            // 4. Configuration du volume
            VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");
            if (vc != null) {
                vc.setLevel(volume);
            }

            // 5. Configuration du routage audio (Écouteur vs Haut-parleur)
            applyAudioPath();

            // 6. Démarrage de la lecture
            player.start();
            isPlaying = true;
            LogManager.log("STREAM_PLAYER", "Streaming audio player started successfully");
        } catch (Throwable t) {
            LogManager.error("STREAM_PLAYER", "Error starting audio stream: " + t.getMessage());
            stopAudioStreamInternal();
        }
    }

    /**
     * Écrit un paquet audio Base64 entrant ("VOICE_TX|<base64>") directement dans le pipe audio.
     */
    public void writeChunk(String base64Data) {
        if (!isPlaying || audioOut == null || base64Data == null || base64Data.length() == 0) return;
        
        try {
            byte[] pcmData = Base64InputStream.decode(base64Data);
            if (pcmData != null && pcmData.length > 0) {
                synchronized (this) {
                    if (isPlaying && audioOut != null) {
                        audioOut.write(pcmData);
                        audioOut.flush();
                    }
                }
            }
        } catch (Throwable t) {
            // Buffer temporairement saturé ou liaison interrompue
        }
    }

    /**
     * Écrit des échantillons PCM bruts directement dans le pipe audio.
     */
    public synchronized void writeChunk(byte[] pcmData, int offset, int length) {
        if (!isPlaying || audioOut == null || pcmData == null || length <= 0) return;
        try {
            audioOut.write(pcmData, offset, length);
            audioOut.flush();
        } catch (Throwable t) {}
    }

    public synchronized void stopAudioStream() {
        if (!isPlaying) return;
        LogManager.log("STREAM_PLAYER", "Stopping audio stream...");
        stopAudioStreamInternal();
    }

    private synchronized void stopAudioStreamInternal() {
        isPlaying = false;
        
        try {
            if (player != null) {
                try {
                    if (player.getState() == Player.STARTED) {
                        player.stop();
                    }
                } catch (Throwable ignored) {}
                try { player.deallocate(); } catch (Throwable ignored) {}
                try { player.close(); } catch (Throwable ignored) {}
                player = null;
            }
        } catch (Throwable ignored) {}

        try {
            if (audioOut != null) {
                try { audioOut.close(); } catch (Throwable ignored) {}
                audioOut = null;
            }
        } catch (Throwable ignored) {}

        try {
            if (audioIn != null) {
                try { audioIn.close(); } catch (Throwable ignored) {}
                audioIn = null;
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Bascule la sortie locale entre combiné (Écouteur interne) et Haut-parleur externe.
     */
    public synchronized boolean toggleAudioPath() {
        if (audioPath == AudioPathControl.AUDIO_PATH_HANDSFREE) {
            setAudioPath(AudioPathControl.AUDIO_PATH_HANDSET);
            return false; // Écouteur
        } else {
            setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE);
            return true; // Haut-parleur
        }
    }

    public synchronized void setAudioPath(int path) {
        this.audioPath = path;
        applyAudioPath();
    }

    private void applyAudioPath() {
        try {
            if (player != null) {
                AudioPathControl apc = (AudioPathControl) player.getControl("net.rim.device.api.media.control.AudioPathControl");
                if (apc != null && apc.canSwitchToPath(audioPath)) {
                    apc.setAudioPath(audioPath);
                    LogManager.log("STREAM_PLAYER", "Audio path switched to: " + (audioPath == AudioPathControl.AUDIO_PATH_HANDSFREE ? "SPEAKERPHONE" : "EARPIECE/HANDSET"));
                }
            }
        } catch (Throwable t) {
            LogManager.error("STREAM_PLAYER", "AudioPathControl error: " + t.getMessage());
        }
    }

    public int getAudioPath() {
        return audioPath;
    }

    public boolean isSpeakerOn() {
        return audioPath == AudioPathControl.AUDIO_PATH_HANDSFREE;
    }

    public int getVolume() {
        return volume;
    }

    public synchronized void setVolume(int vol) {
        if (vol < 0) vol = 0;
        if (vol > 100) vol = 100;
        this.volume = vol;
        try {
            if (player != null) {
                VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");
                if (vc != null) vc.setLevel(volume);
            }
        } catch (Throwable ignored) {}
    }

    public void adjustVolume(int delta) {
        setVolume(this.volume + delta);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Génère un en-tête WAV standard 44 octets avec une taille quasi-infinie
     * pour permettre le streaming temps réel ininterrompu.
     */
    private byte[] createWavHeader(int sampleRate, int channels, int bitsPerSample) {
        byte[] header = new byte[44];
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        // Taille chunk RIFF quasi-infinie (~2 Go)
        header[4] = (byte) 0x24; header[5] = (byte) 0xFF; header[6] = (byte) 0xFF; header[7] = (byte) 0x7F;
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0; // SubChunk1Size (16 pour PCM standard)
        header[20] = 1; header[21] = 0; // AudioFormat (1 = PCM linéaire)
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
        // Taille de données audio quasi-infinie
        header[40] = (byte) 0x00; header[41] = (byte) 0xFF; header[42] = (byte) 0xFF; header[43] = (byte) 0x7F;
        return header;
    }
}
