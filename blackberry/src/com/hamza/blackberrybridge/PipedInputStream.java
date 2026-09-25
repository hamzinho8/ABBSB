package com.hamza.blackberrybridge;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implémentation CLDC 1.1 / Java ME de PipedInputStream pour BlackBerry OS 5.0 / 6.0.
 * Fournit l'extrémité de lecture d'un tube circulaire (16 Ko ou 32 Ko) pour le Player J2ME.
 * 
 * Optimisations temps réel pour la voix sur BlackBerry Curve 9300 :
 * - Fournit des trames de silence en cas de retard réseau pour éviter le décrochage du Player.
 * - Gestion circulaire évitant les blocages du thread Bluetooth d'écriture.
 */
public class PipedInputStream extends InputStream {
    boolean connected = false;
    private boolean closedByReader = false;
    private boolean closedByWriter = false;
    private byte[] buffer;
    private int in = -1;
    private int out = 0;

    public PipedInputStream() {
        this(16384);
    }

    public PipedInputStream(int pipeSize) {
        if (pipeSize <= 0) pipeSize = 16384;
        this.buffer = new byte[pipeSize];
    }

    public PipedInputStream(PipedOutputStream src) throws IOException {
        this(src, 16384);
    }

    public PipedInputStream(PipedOutputStream src, int pipeSize) throws IOException {
        this(pipeSize);
        if (src != null) {
            src.connect(this);
        }
    }

    public synchronized void connect(PipedOutputStream src) throws IOException {
        if (src != null) src.connect(this);
    }

    protected synchronized void receive(int b) throws IOException {
        checkStateForReceive();
        while (in == out) {
            try {
                wait(20);
                if (in == out) {
                    out = (out + 1) % buffer.length;
                    break;
                }
            } catch (InterruptedException e) {
                throw new IOException("Interrupted");
            }
        }
        if (in < 0) {
            in = 0;
            out = 0;
        }
        buffer[in++] = (byte)(b & 0xFF);
        if (in >= buffer.length) in = 0;
        notifyAll();
    }

    synchronized void receive(byte[] b, int off, int len) throws IOException {
        checkStateForReceive();
        for (int i = 0; i < len; i++) {
            receive(b[off + i]);
        }
    }

    private void checkStateForReceive() throws IOException {
        if (!connected) throw new IOException("Pipe not connected");
        if (closedByWriter || closedByReader) throw new IOException("Pipe closed");
    }

    synchronized void receivedLast() {
        closedByWriter = true;
        notifyAll();
    }

    synchronized void flushPipe() {
        notifyAll();
    }

    public synchronized int read() throws IOException {
        if (!connected) throw new IOException("Pipe not connected");
        if (closedByReader) throw new IOException("Pipe closed");
        
        while (in < 0) {
            if (closedByWriter) return -1;
            try {
                wait(20);
                if (in < 0 && !closedByWriter) {
                    return 0; // Trame de silence pour éviter l'arrêt du Player
                }
            } catch (InterruptedException e) {
                return 0;
            }
        }

        int val = buffer[out++] & 0xFF;
        if (out >= buffer.length) out = 0;
        if (in == out) in = -1; // Buffer vide
        notifyAll();
        return val;
    }

    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (b == null) throw new NullPointerException();
        if (len == 0) return 0;
        if (!connected) throw new IOException("Pipe not connected");
        if (closedByReader) throw new IOException("Pipe closed");

        while (in < 0) {
            if (closedByWriter) return -1;
            try {
                wait(20);
                if (in < 0 && !closedByWriter) {
                    // Trame de silence
                    for (int i = 0; i < len; i++) b[off + i] = 0;
                    return len;
                }
            } catch (InterruptedException e) {
                for (int i = 0; i < len; i++) b[off + i] = 0;
                return len;
            }
        }

        int bytesRead = 0;
        while (len > 0 && in >= 0) {
            b[off++] = buffer[out++];
            if (out >= buffer.length) out = 0;
            bytesRead++;
            len--;
            if (in == out) in = -1;
        }
        notifyAll();
        return bytesRead;
    }

    public synchronized int available() throws IOException {
        if (in < 0) return 0;
        if (in == out) return buffer.length;
        if (in > out) return in - out;
        return buffer.length - out + in;
    }

    public synchronized void close() throws IOException {
        closedByReader = true;
        closedByWriter = true;
        notifyAll();
    }
}
