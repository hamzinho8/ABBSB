package com.hamza.blackberrybridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.RecordControl;
import javax.microedition.media.control.VolumeControl;
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.io.Base64OutputStream;
import net.rim.device.api.media.control.AudioPathControl;

/**
 * Module de streaming audio bidirectionnel temps réel pour BlackBerry Curve 9300.
 * Volet 1 :
 * - Réception des trames PCM 8000Hz 16-bit Mono (320 octets = 20ms) via "VOICE_TX|<base64>"
 *   et lecture continue via un Player javax.microedition.media sans latence ni grésillement.
 * - Capture du microphone BlackBerry via Manager.createPlayer("capture://audio?encoding=pcm...")
 *   et transmission au smartphone Android sous la forme "VOICE_RX|<base64>".
 * - Routage dynamique entre l'écouteur interne (EARPIECE / HANDSET) et le haut-parleur (SPEAKERPHONE).
 */
public class CallAudioPlayerRecorder {
    private SmartBridgeApp app;
    private boolean isStreaming = false;
    
    // Composants de lecture (Playback)
    private Player playbackPlayer;
    private PcmAudioStream playbackStream;
    private int audioPath = AudioPathControl.AUDIO_PATH_HANDSET; // Écouteur par défaut
    private int volume = 85; // 0 - 100
    
    // Composants d'enregistrement (Microphone capture)
    private Player recordPlayer;
    private RecordControl recordControl;
    private MicPcmSender micSender;
    
    public CallAudioPlayerRecorder(SmartBridgeApp app) {
        this.app = app;
    }
    
    /**
     * Démarre le pont audio bidirectionnel (lecture et capture micro).
     */
    public synchronized void startVoiceBridge() {
        if (isStreaming) {
            LogManager.log("AUDIO_BRIDGE", "Voice bridge already running");
            return;
        }
        isStreaming = true;
        LogManager.log("AUDIO_BRIDGE", "Starting bidirectional voice bridge (PCM 8000Hz 16-bit Mono)...");
        
        // 1. Initialiser le flux et le lecteur audio pour la réception Android -> BlackBerry
        initPlayback();
        
        // 2. Initialiser la capture microphone BlackBerry -> Android
        initCapture();
    }
    
    private void initPlayback() {
        try {
            stopPlayback();
            playbackStream = new PcmAudioStream();
            playbackPlayer = Manager.createPlayer(playbackStream, "audio/x-wav");
            playbackPlayer.realize();
            playbackPlayer.prefetch();
            
            // Configuration du volume
            VolumeControl vc = (VolumeControl) playbackPlayer.getControl("VolumeControl");
            if (vc != null) {
                vc.setLevel(volume);
            }
            
            // Configuration du routage audio (Écouteur vs Haut-parleur)
            applyAudioPath();
            
            playbackPlayer.start();
            LogManager.log("AUDIO_BRIDGE", "Playback player started successfully");
        } catch (Throwable t) {
            LogManager.error("AUDIO_BRIDGE", "Error initializing playback: " + t.getMessage());
        }
    }
    
    private void initCapture() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    stopCapture();
                    // Initialisation de la capture microphone CLDC/MIDP JSR-135
                    recordPlayer = Manager.createPlayer("capture://audio?encoding=pcm&rate=8000&bits=16&channels=1");
                    recordPlayer.realize();
                    recordControl = (RecordControl) recordPlayer.getControl("RecordControl");
                    
                    if (recordControl != null) {
                        micSender = new MicPcmSender(app);
                        recordControl.setRecordStream(micSender);
                        recordControl.startRecord();
                        recordPlayer.start();
                        LogManager.log("AUDIO_BRIDGE", "Microphone capture active (320 bytes / 20ms chunks)");
                    } else {
                        LogManager.error("AUDIO_BRIDGE", "RecordControl unavailable on this hardware");
                    }
                } catch (Throwable t) {
                    LogManager.error("AUDIO_BRIDGE", "Capture init error: " + t.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Traite un paquet audio entrant reçu du smartphone Android : "VOICE_TX|<base64>"
     */
    public void playVoicePacket(String base64Data) {
        if (!isStreaming || base64Data == null || base64Data.length() == 0) return;
        try {
            byte[] pcmChunk = Base64InputStream.decode(base64Data);
            if (pcmChunk != null && pcmChunk.length > 0 && playbackStream != null) {
                playbackStream.writePcm(pcmChunk, 0, pcmChunk.length);
            }
        } catch (IOException e) {
            LogManager.error("AUDIO_BRIDGE", "Base64 decode error: " + e.getMessage());
        }
    }
    
    /**
     * Arrête proprement le pont audio et libère les ressources matérielles RIM.
     */
    public synchronized void stopVoiceBridge() {
        if (!isStreaming) return;
        isStreaming = false;
        LogManager.log("AUDIO_BRIDGE", "Stopping bidirectional voice bridge...");
        
        stopCapture();
        stopPlayback();
    }
    
    private void stopPlayback() {
        try {
            if (playbackStream != null) {
                playbackStream.close();
                playbackStream = null;
            }
        } catch (Throwable ignored) {}
        
        try {
            if (playbackPlayer != null) {
                if (playbackPlayer.getState() == Player.STARTED) {
                    playbackPlayer.stop();
                }
                playbackPlayer.deallocate();
                playbackPlayer.close();
                playbackPlayer = null;
            }
        } catch (Throwable ignored) {}
    }
    
    private void stopCapture() {
        try {
            if (recordControl != null) {
                recordControl.stopRecord();
                recordControl = null;
            }
        } catch (Throwable ignored) {}
        
        try {
            if (recordPlayer != null) {
                if (recordPlayer.getState() == Player.STARTED) {
                    recordPlayer.stop();
                }
                recordPlayer.deallocate();
                recordPlayer.close();
                recordPlayer = null;
            }
        } catch (Throwable ignored) {}
        
        if (micSender != null) {
            micSender.close();
            micSender = null;
        }
    }
    
    /**
     * Bascule la sortie locale entre le combiné (écouteur) et le haut-parleur.
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
            if (playbackPlayer != null) {
                AudioPathControl apc = (AudioPathControl) playbackPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
                if (apc != null && apc.canSwitchToPath(audioPath)) {
                    apc.setAudioPath(audioPath);
                    LogManager.log("AUDIO_BRIDGE", "AudioPath switched to: " + (audioPath == AudioPathControl.AUDIO_PATH_HANDSFREE ? "SPEAKERPHONE" : "EARPIECE/HANDSET"));
                }
            }
        } catch (Throwable t) {
            LogManager.error("AUDIO_BRIDGE", "AudioPathControl error: " + t.getMessage());
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
            if (playbackPlayer != null) {
                VolumeControl vc = (VolumeControl) playbackPlayer.getControl("VolumeControl");
                if (vc != null) vc.setLevel(volume);
            }
        } catch (Throwable ignored) {}
    }
    
    public void adjustVolume(int delta) {
        setVolume(this.volume + delta);
    }
    
    public boolean isStreaming() {
        return isStreaming;
    }
    
    // =========================================================================
    // Flux de lecture WAV avec Ring Buffer (PcmAudioStream)
    // =========================================================================
    private static class PcmAudioStream extends InputStream {
        private byte[] header = new byte[44];
        private int headerPos = 0;
        private byte[] ringBuffer = new byte[8192];
        private int head = 0;
        private int tail = 0;
        private boolean closed = false;

        public PcmAudioStream() {
            buildWavHeader(header, 8000, 1, 16);
        }

        private static void buildWavHeader(byte[] h, int rate, int channels, int bits) {
            h[0] = 'R'; h[1] = 'I'; h[2] = 'F'; h[3] = 'F';
            h[4] = (byte)0xFF; h[5] = (byte)0xFF; h[6] = (byte)0x7F; h[7] = 0x7F; // ~2 Go streaming
            h[8] = 'W'; h[9] = 'A'; h[10] = 'V'; h[11] = 'E';
            h[12] = 'f'; h[13] = 'm'; h[14] = 't'; h[15] = ' ';
            h[16] = 16; h[17] = 0; h[18] = 0; h[19] = 0; // Subchunk1Size
            h[20] = 1; h[21] = 0; // AudioFormat (1 = PCM)
            h[22] = (byte)channels; h[23] = 0;
            h[24] = (byte)(rate & 0xff); h[25] = (byte)((rate >> 8) & 0xff);
            h[26] = (byte)((rate >> 16) & 0xff); h[27] = (byte)((rate >> 24) & 0xff);
            int byteRate = rate * channels * (bits / 8);
            h[28] = (byte)(byteRate & 0xff); h[29] = (byte)((byteRate >> 8) & 0xff);
            h[30] = (byte)((byteRate >> 16) & 0xff); h[31] = (byte)((byteRate >> 24) & 0xff);
            h[32] = (byte)(channels * (bits / 8)); h[33] = 0; // BlockAlign
            h[34] = (byte)bits; h[35] = 0;
            h[36] = 'd'; h[37] = 'a'; h[38] = 't'; h[39] = 'a';
            h[40] = (byte)0xFF; h[41] = (byte)0xFF; h[42] = (byte)0x7F; h[43] = 0x7F;
        }

        public synchronized int read() throws IOException {
            if (headerPos < 44) {
                return header[headerPos++] & 0xFF;
            }
            while (head == tail && !closed) {
                try {
                    wait(20);
                    if (head == tail) return 0; // Trame de silence pour éviter le grésillement
                } catch (InterruptedException e) {
                    return 0;
                }
            }
            if (closed && head == tail) return -1;
            int val = ringBuffer[tail] & 0xFF;
            tail = (tail + 1) % ringBuffer.length;
            return val;
        }

        public synchronized int read(byte[] b, int off, int len) throws IOException {
            if (b == null) throw new NullPointerException();
            if (len == 0) return 0;
            int readBytes = 0;
            if (headerPos < 44) {
                int toCopy = Math.min(len, 44 - headerPos);
                System.arraycopy(header, headerPos, b, off, toCopy);
                headerPos += toCopy;
                off += toCopy;
                len -= toCopy;
                readBytes += toCopy;
                if (len == 0) return readBytes;
            }
            while (head == tail && !closed) {
                try {
                    wait(20);
                    if (head == tail) {
                        for (int i = 0; i < len; i++) b[off + i] = 0;
                        return readBytes + len;
                    }
                } catch (InterruptedException e) {
                    return readBytes;
                }
            }
            if (closed && head == tail) return readBytes > 0 ? readBytes : -1;
            while (len > 0 && head != tail) {
                b[off++] = ringBuffer[tail];
                tail = (tail + 1) % ringBuffer.length;
                len--;
                readBytes++;
            }
            return readBytes;
        }

        public synchronized void writePcm(byte[] data, int off, int len) {
            if (closed || data == null) return;
            for (int i = 0; i < len; i++) {
                int nextHead = (head + 1) % ringBuffer.length;
                if (nextHead != tail) {
                    ringBuffer[head] = data[off + i];
                    head = nextHead;
                }
            }
            notifyAll();
        }

        public synchronized void close() {
            closed = true;
            notifyAll();
        }
    }
    
    // =========================================================================
    // Capture Microphone & Envoi Paquets VOICE_RX (MicPcmSender)
    // =========================================================================
    private static class MicPcmSender extends OutputStream {
        private SmartBridgeApp app;
        private byte[] chunk = new byte[320]; // 20ms à 8000Hz 16-bit Mono
        private int count = 0;
        private boolean active = true;

        public MicPcmSender(SmartBridgeApp app) {
            this.app = app;
        }

        public synchronized void write(int b) {
            if (!active) return;
            chunk[count++] = (byte) b;
            if (count >= 320) flushChunk();
        }

        public synchronized void write(byte[] b, int off, int len) {
            if (!active || b == null) return;
            while (len > 0) {
                int toCopy = Math.min(len, 320 - count);
                System.arraycopy(b, off, chunk, count, toCopy);
                count += toCopy;
                off += toCopy;
                len -= toCopy;
                if (count >= 320) flushChunk();
            }
        }

        private void flushChunk() {
            if (!active) return;
            try {
                String base64 = Base64OutputStream.encodeAsString(chunk, 0, 320, false, false);
                app.getConnectionManager().sendData("VOICE_RX|" + base64 + "\n");
            } catch (Throwable t) {
                LogManager.error("AUDIO_MIC", "Error encoding VOICE_RX: " + t.getMessage());
            }
            count = 0;
        }

        public synchronized void close() {
            active = false;
            count = 0;
        }
    }
}
