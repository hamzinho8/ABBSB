package com.hamza.blackberrybridge.audio;

import java.io.ByteArrayInputStream;
import java.util.Vector;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

/**
 * Lecteur audio séquentiel unique sur Thread ouvrier dédié pour BlackBerry Curve 9300.
 * Conçu pour lire les flux WAV autonomes (500 ms / 200 ms, 8000 Hz, 16-bit Mono).
 * 
 * Évite les conflits d'allocation matérielle audio et le crash JVM (226) en :
 * - Utilisant une file d'attente FIFO (Vector) avec purge automatique en cas de retard (> 4 blocs).
 * - Garantissant qu'un seul Player J2ME est actif à la fois.
 * - Attendant la fin de la lecture du bloc avant de fermer proprement les ressources.
 */
public class BlackBerryAudioStreamer implements Runnable {
    private static BlackBerryAudioStreamer instance;
    private final Vector chunkQueue = new Vector();
    private boolean isRunning = false;
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
                // Si la file dépasse 4 blocs (2 secondes de retard à 500ms), purge des plus anciens
                if (chunkQueue.size() > 4) {
                    chunkQueue.removeElementAt(0);
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
                Player player = null;
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(wavBytes);
                    player = Manager.createPlayer(bais, "audio/x-wav");
                    player.realize();
                    player.prefetch();
                    
                    VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");
                    if (vc != null) {
                        vc.setLevel(volume);
                    }
                    
                    player.start();
                    
                    // On attend la fin de lecture du bloc (500 ms) avant de libérer le matériel
                    // Cela évite les conflits d'allocation audio et l'erreur JVM 226
                    while (player != null && player.getState() == Player.STARTED && isRunning) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                } catch (Throwable t) {
                    // Ignore et continue pour ne jamais crasher l'app
                } finally {
                    if (player != null) {
                        try { player.stop(); } catch (Exception e) {}
                        try { player.close(); } catch (Exception e) {}
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
