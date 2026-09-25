package com.hamza.blackberrybridge;

import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.RecordControl;
import net.rim.device.api.media.control.AudioPathControl;

/**
 * Module de streaming audio bidirectionnel temps réel pour BlackBerry Curve 9300.
 * 
 * Volet 1 :
 * - Réception et lecture des trames audio de l'interlocuteur :
 *   Délégation complète à StreamingAudioPlayer (PipedOutputStream / PipedInputStream 32 Ko,
 *   en-tête WAV 44 octets unique, instance unique Player J2ME).
 * - Capture du microphone BlackBerry via Manager.createPlayer("capture://audio?encoding=pcm...")
 *   et transmission au smartphone Android sous la forme "VOICE_RX|<base64>".
 * - Routage dynamique entre l'écouteur interne (HANDSET) et le haut-parleur (HANDSFREE).
 */
public class CallAudioPlayerRecorder {
    private SmartBridgeApp app;
    private volatile boolean isStreaming = false;
    
    // Composants d'enregistrement (Microphone capture)
    private Player recordPlayer;
    private RecordControl recordControl;
    private MicPcmSender micSender;
    
    // Table d'encodage Base64 rapide
    private static final char[] BASE64_ENCODE_CHARS = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    
    public CallAudioPlayerRecorder(SmartBridgeApp app) {
        this.app = app;
    }
    
    /**
     * Démarre le pont audio bidirectionnel (lecture StreamingAudioPlayer et capture micro).
     */
    public synchronized void startVoiceBridge() {
        if (isStreaming) {
            LogManager.log("AUDIO_BRIDGE", "Voice bridge already active");
            return;
        }
        isStreaming = true;
        LogManager.log("AUDIO_BRIDGE", "Starting bidirectional voice bridge (PCM 8000Hz 16-bit Mono)...");
        
        // 1. Initialiser et démarrer le lecteur audio streaming (PipedStream + WAV header 44 octets)
        StreamingAudioPlayer.getInstance().startAudioStream(8000, 1, 16);
        
        // 2. Initialiser la capture microphone BlackBerry -> Android
        initCapture();
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
                        micSender = new MicPcmSender(app, CallAudioPlayerRecorder.this);
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
     * Transmet la trame audio "VOICE_TX|<base64>" au StreamingAudioPlayer singleton.
     */
    public void playVoicePacket(String base64Data) {
        StreamingAudioPlayer.getInstance().writeChunk(base64Data);
    }
    
    /**
     * Arrête immédiatement le pont audio (capture micro et lecture).
     */
    public synchronized void stopVoiceBridge() {
        if (!isStreaming) return;
        isStreaming = false;
        LogManager.log("AUDIO_BRIDGE", "Stopping bidirectional voice bridge immediately...");
        
        stopCapture();
        StreamingAudioPlayer.getInstance().stopAudioStream();
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
        return StreamingAudioPlayer.getInstance().toggleAudioPath();
    }
    
    public synchronized void setAudioPath(int path) {
        StreamingAudioPlayer.getInstance().setAudioPath(path);
    }
    
    public int getAudioPath() {
        return StreamingAudioPlayer.getInstance().getAudioPath();
    }
    
    public boolean isSpeakerOn() {
        return StreamingAudioPlayer.getInstance().isSpeakerOn();
    }
    
    public int getVolume() {
        return StreamingAudioPlayer.getInstance().getVolume();
    }
    
    public synchronized void setVolume(int vol) {
        StreamingAudioPlayer.getInstance().setVolume(vol);
    }
    
    public void adjustVolume(int delta) {
        StreamingAudioPlayer.getInstance().adjustVolume(delta);
    }
    
    public boolean isStreaming() {
        return isStreaming || StreamingAudioPlayer.getInstance().isPlaying();
    }
    
    // =========================================================================
    // Capture Microphone & Envoi Paquets VOICE_RX (MicPcmSender)
    // =========================================================================
    private static class MicPcmSender extends OutputStream {
        private SmartBridgeApp app;
        private CallAudioPlayerRecorder parent;
        private final byte[] chunk = new byte[320]; // 20ms à 8000Hz 16-bit Mono
        private final char[] base64Chars = new char[440];
        private int count = 0;
        private volatile boolean active = true;

        public MicPcmSender(SmartBridgeApp app, CallAudioPlayerRecorder parent) {
            this.app = app;
            this.parent = parent;
        }

        public synchronized void write(int b) {
            if (!active || !parent.isStreaming()) return;
            chunk[count++] = (byte) b;
            if (count >= 320) flushChunk();
        }

        public synchronized void write(byte[] b, int off, int len) {
            if (!active || !parent.isStreaming() || b == null) return;
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
            if (!active || !parent.isStreaming()) {
                count = 0;
                return;
            }
            try {
                // Encodage Base64 direct sans allouer d'objets intermédiaires
                int charCount = encodeBase64Fast(chunk, 320, base64Chars);
                String base64Str = new String(base64Chars, 0, charCount);
                app.getConnectionManager().sendData("VOICE_RX|" + base64Str + "\n");
            } catch (Throwable t) {
                LogManager.error("AUDIO_MIC", "Error sending VOICE_RX: " + t.getMessage());
            }
            count = 0;
        }

        private int encodeBase64Fast(byte[] in, int len, char[] outBuf) {
            int outIdx = 0;
            int end = len - (len % 3);
            for (int i = 0; i < end; i += 3) {
                int b1 = in[i] & 0xFF;
                int b2 = in[i + 1] & 0xFF;
                int b3 = in[i + 2] & 0xFF;
                outBuf[outIdx++] = BASE64_ENCODE_CHARS[b1 >>> 2];
                outBuf[outIdx++] = BASE64_ENCODE_CHARS[((b1 & 0x03) << 4) | (b2 >>> 4)];
                outBuf[outIdx++] = BASE64_ENCODE_CHARS[((b2 & 0x0F) << 2) | (b3 >>> 6)];
                outBuf[outIdx++] = BASE64_ENCODE_CHARS[b3 & 0x3F];
            }
            if (len % 3 == 1) {
                int b1 = in[end] & 0xFF;
                outBuf[outIdx++] = BASE64_ENCODE_CHARS[b1 >>> 2];
                outBuf[outIdx++] = BASE64_ENCODE_CHARS[(b1 & 0x03) << 4];
                outBuf[outIdx++] = '=';
                outBuf[outIdx++] = '=';
            } else if (len % 3 == 2) {
                int b1 = in[end] & 0xFF;
                int b2 = in[end + 1] & 0xFF;
                outBuf[outIdx++] = BASE64_ENCODE_CHARS[b1 >>> 2];
                outBuf[outIdx++] = BASE64_ENCODE_CHARS[((b1 & 0x03) << 4) | (b2 >>> 4)];
                outBuf[outIdx++] = BASE64_ENCODE_CHARS[(b2 & 0x0F) << 2];
                outBuf[outIdx++] = '=';
            }
            return outIdx;
        }

        public synchronized void close() {
            active = false;
            count = 0;
        }
    }
}
