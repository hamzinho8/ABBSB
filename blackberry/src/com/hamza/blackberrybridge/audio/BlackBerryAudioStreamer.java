package com.hamza.blackberrybridge.audio;

import java.io.ByteArrayInputStream;
import java.util.Vector;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

public class BlackBerryAudioStreamer implements Runnable {
    private static BlackBerryAudioStreamer instance;
    private final Vector chunkQueue = new Vector();
    private boolean isRunning = false;
    private boolean hasBeeped = false;
    private Thread workerThread;
    private int volume = 100;

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
        if (!isRunning || base64Data == null || base64Data.length() == 0) return;
        
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
                        bais.reset();
                        player = Manager.createPlayer(bais, "audio/wav");
                    }

                    player.realize();
                    player.prefetch();

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
}
