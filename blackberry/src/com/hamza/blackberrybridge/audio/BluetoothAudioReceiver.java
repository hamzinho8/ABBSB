package com.hamza.blackberrybridge.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.media.control.AudioPathControl;

/**
 * Récepteur Audio Bluetooth Haute-Fidélité pour BlackBerry Curve 9300 (RIM OS 5.0 à 7.1).
 * 
 * Architecture Double-Lecteur Ping-Pong J2ME (JSR-135 MMAPI) :
 * - Reçoit des blocs audio WAV autonomes de 200ms (5 paquets/sec) depuis le smartphone Android
 *   (appels, YouTube, musique, streaming multimédia).
 * - Alterne entre Player A et Player B pour supprimer tout blanc sonore ou micro-coupure.
 * - Libère l'ancien Player à chaque cycle pour économiser la mémoire RAM du Curve 9300.
 * - Diffusion sur haut-parleur, combiné ou casque branché sur prise Jack 3.5mm.
 * - Découplage complet sur thread dédié à haute priorité (Thread.MAX_PRIORITY).
 */
public class BluetoothAudioReceiver {
    private static BluetoothAudioReceiver instance;
    private Player playerA;
    private Player playerB;
    private boolean usePlayerA = true;
    private volatile boolean isRunning = false;
    private int volume = 100;
    private final Object pingPongLock = new Object();

    public static synchronized BluetoothAudioReceiver getInstance() {
        if (instance == null) {
            instance = new BluetoothAudioReceiver();
        }
        return instance;
    }

    public synchronized void startReceiver() {
        isRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Traite et joue un bloc audio WAV de 200ms encodé en Base64.
     */
    public void processAudioChunk(final String base64Payload) {
        if (!isRunning || base64Payload == null || base64Payload.length() < 50) {
            return;
        }

        // Décodage et lecture sur un thread d'arrière-plan dédié
        Thread playThread = new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] wavBytes = decodeBase64Safe(base64Payload);
                    if (wavBytes == null || wavBytes.length < 44) {
                        return;
                    }

                    ByteArrayInputStream bais = new ByteArrayInputStream(wavBytes);
                    Player activePlayer = Manager.createPlayer(bais, "audio/x-wav");
                    activePlayer.realize();
                    activePlayer.prefetch();

                    VolumeControl vc = (VolumeControl) activePlayer.getControl("VolumeControl");
                    if (vc != null) {
                        vc.setLevel(volume);
                    }

                    activePlayer.start();

                    // Nettoyage de l'ancien player précédent pour libérer la RAM
                    synchronized (pingPongLock) {
                        if (usePlayerA) {
                            if (playerB != null) {
                                try { playerB.stop(); } catch (Exception ignored) {}
                                try { playerB.close(); } catch (Exception ignored) {}
                                playerB = null;
                            }
                            playerA = activePlayer;
                        } else {
                            if (playerA != null) {
                                try { playerA.stop(); } catch (Exception ignored) {}
                                try { playerA.close(); } catch (Exception ignored) {}
                                playerA = null;
                            }
                            playerB = activePlayer;
                        }
                        usePlayerA = !usePlayerA;
                    }
                } catch (Throwable t) {
                    // Ignore et continue pour ne jamais crasher l'app (Anti-Crash 226)
                }
            }
        });
        playThread.setPriority(Thread.MAX_PRIORITY);
        playThread.start();
    }

    public synchronized void stopReceiver() {
        isRunning = false;
        synchronized (pingPongLock) {
            try {
                if (playerA != null) {
                    try { playerA.stop(); } catch (Exception ignored) {}
                    try { playerA.close(); } catch (Exception ignored) {}
                    playerA = null;
                }
                if (playerB != null) {
                    try { playerB.stop(); } catch (Exception ignored) {}
                    try { playerB.close(); } catch (Exception ignored) {}
                    playerB = null;
                }
            } catch (Exception ignored) {}
        }
    }

    public synchronized void setVolume(int vol) {
        this.volume = Math.max(0, Math.min(100, vol));
        try {
            if (playerA != null) {
                VolumeControl vc = (VolumeControl) playerA.getControl("VolumeControl");
                if (vc != null) vc.setLevel(this.volume);
            }
            if (playerB != null) {
                VolumeControl vc = (VolumeControl) playerB.getControl("VolumeControl");
                if (vc != null) vc.setLevel(this.volume);
            }
        } catch (Throwable ignored) {}
    }

    public int getVolume() {
        return volume;
    }

    public synchronized void adjustVolume(int delta) {
        setVolume(this.volume + delta);
    }

    /**
     * Décodeur Base64 haute performance avec fallback CLDC 1.1 manuel.
     */
    private static byte[] decodeBase64Safe(String str) {
        if (str == null || str.length() == 0) return null;
        try {
            byte[] res = Base64InputStream.decode(str);
            if (res != null && res.length >= 44) {
                return res;
            }
        } catch (Throwable ignored) {}

        return manualBase64Decode(str);
    }

    private static byte[] manualBase64Decode(String s) {
        try {
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
        } catch (Throwable t) {
            return null;
        }
    }

    private static int decodeChar(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+') return 62;
        if (c == '/') return 63;
        return -1;
    }
}
