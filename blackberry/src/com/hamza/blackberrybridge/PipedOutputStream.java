package com.hamza.blackberrybridge;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Implémentation CLDC 1.1 / Java ME de PipedOutputStream pour BlackBerry OS 5.0 / 6.0.
 * Fournit l'extrémité d'écriture d'un tube (pipe) d'octets connecté à un PipedInputStream.
 */
public class PipedOutputStream extends OutputStream {
    private PipedInputStream sink;
    private boolean closed = false;

    public PipedOutputStream() {}

    public PipedOutputStream(PipedInputStream snk) throws IOException {
        connect(snk);
    }

    public synchronized void connect(PipedInputStream snk) throws IOException {
        if (snk == null) throw new NullPointerException("Sink cannot be null");
        if (sink != null || snk.connected) throw new IOException("Pipe already connected");
        sink = snk;
        snk.connected = true;
    }

    public void write(int b) throws IOException {
        if (sink == null) throw new IOException("Pipe not connected");
        if (closed) throw new IOException("Pipe closed");
        sink.receive(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (sink == null) throw new IOException("Pipe not connected");
        if (closed) throw new IOException("Pipe closed");
        sink.receive(b, off, len);
    }

    public void flush() throws IOException {
        if (sink != null) {
            sink.flushPipe();
        }
    }

    public synchronized void close() throws IOException {
        closed = true;
        if (sink != null) {
            sink.receivedLast();
        }
    }
}
