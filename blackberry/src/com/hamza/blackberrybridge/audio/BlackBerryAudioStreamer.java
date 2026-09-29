package com.hamza.blackberrybridge.audio;

import java.io.ByteArrayInputStream;
import java.util.Vector;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import net.rim.device.api.media.control.AudioPathControl;

public class BlackBerryAudioStreamer implements Runnable {
    private static BlackBerryAudioStreamer instance;
    private final Vector chunkQueue = new Vector();
    private boolean isRunning = false;
    private boolean hasBeeped = false;
    private Thread workerThread;
    private int volume = 100;
    private int forcedAudioPath = -1; // -1 = auto (headset if plugged, else handsfree)

    public static synchronized BlackBerryAudioStreamer getInstance() {
        if (instance == null) {
            instance = new BlackBerryAudioStreamer();
        }
        return instance;
    }

    public synchronized void startAudio() {
        if (isRunning) return;
        isRunning = true;
        hasBeeped = false;
        synchronized (chunkQueue) {
            chunkQueue.removeAllElements();
        }
        workerThread = new Thread(this);
        workerThread.setPriority(Thread.MAX_PRIORITY);
        workerThread.start();
    }

    public void enqueueChunk(String base64Data) {
        if (base64Data == null || base64Data.length() == 0) return;
        
        // Auto-démarrage si le flux commence sans commande AUDIO_START préalable
        if (!isRunning) {
            startAudio();
        }
        
        byte[] wavBytes = FastBase64.decode(base64Data);
        if (wavBytes != null && wavBytes.length > 44) {
            synchronized (chunkQueue) {
                if (chunkQueue.size() > 4) {
                    chunkQueue.removeElementAt(0); // Évite le retard audio
                }
                chunkQueue.addElement(wavBytes);
                chunkQueue.notify();
            }
        }
    }

    public void run() {
        while (isRunning) {
            byte[] wavBytes = null;
            synchronized (chunkQueue) {
                while (chunkQueue.isEmpty() && isRunning) {
                    try {
                        chunkQueue.wait(500);
                    } catch (InterruptedException e) {}
                }
                if (!isRunning) break;
                if (!chunkQueue.isEmpty()) {
                    wavBytes = (byte[]) chunkQueue.firstElement();
                    chunkQueue.removeElementAt(0);
                }
            }

            if (wavBytes != null) {
                // Émettre un bip court de confirmation au premier paquet reçu pour valider le matériel audio
                if (!hasBeeped) {
                    hasBeeped = true;
                    try {
                        Manager.playTone(69, 100, 100); // Note La4 (440Hz), 100ms, volume 100%
                    } catch (Exception e) {}
                }

                Player player = null;
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(wavBytes);
                    try {
                        player = Manager.createPlayer(bais, "audio/x-wav");
                    } catch (Exception ex) {
                        ByteArrayInputStream baisFallback = new ByteArrayInputStream(wavBytes);
                        player = Manager.createPlayer(baisFallback, "audio/wav");
                    }

                    player.realize();
                    player.prefetch();

                    // Routage intelligent du son vers le casque ou le haut-parleur
                    try {
                        AudioPathControl apc = (AudioPathControl) player.getControl("net.rim.device.api.media.control.AudioPathControl");
                        if (apc == null) {
                            apc = (AudioPathControl) player.getControl("AudioPathControl");
                        }
                        if (apc != null) {
                            if (forcedAudioPath != -1 && apc.canSwitchToPath(forcedAudioPath)) {
                                apc.setAudioPath(forcedAudioPath);
                            } else if (apc.canSwitchToPath(AudioPathControl.AUDIO_PATH_HEADSET)) {
                                apc.setAudioPath(AudioPathControl.AUDIO_PATH_HEADSET); // Prise Jack 3.5mm
                            } else if (apc.canSwitchToPath(AudioPathControl.AUDIO_PATH_HANDSFREE)) {
                                apc.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSFREE); // Haut-parleur externe
                            }
                        }
                    } catch (Throwable ignored) {}

                    VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");
                    if (vc != null) {
                        vc.setLevel(volume);
                    }

                    player.start();

                    long start = System.currentTimeMillis();
                    while (player.getState() == Player.STARTED && (System.currentTimeMillis() - start) < 550) {
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                } catch (Throwable t) {
                    System.out.println("[AudioStreamer] Erreur: " + t.getMessage());
                } finally {
                    if (player != null) {
                        try {
                            player.stop();
                        } catch (Exception e) {}
                        try {
                            player.close();
                        } catch (Exception e) {}
                        player = null;
                    }
                }
            }
        }
    }

    public synchronized void stopAudio() {
        isRunning = false;
        synchronized (chunkQueue) {
            chunkQueue.removeAllElements();
            chunkQueue.notifyAll();
        }
        if (workerThread != null) {
            try {
                workerThread.interrupt();
            } catch (Exception e) {}
            workerThread = null;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public synchronized void setVolume(int vol) {
        this.volume = Math.max(0, Math.min(100, vol));
    }

    public int getVolume() {
        return volume;
    }

    public synchronized void setForcedAudioPath(int path) {
        this.forcedAudioPath = path;
    }

    public synchronized void toggleSpeakerHandset() {
        if (forcedAudioPath == AudioPathControl.AUDIO_PATH_HANDSFREE) {
            forcedAudioPath = AudioPathControl.AUDIO_PATH_HANDSET;
        } else {
            forcedAudioPath = AudioPathControl.AUDIO_PATH_HANDSFREE;
        }
    }
}
