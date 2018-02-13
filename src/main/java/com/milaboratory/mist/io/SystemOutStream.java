package com.milaboratory.mist.io;

import java.io.IOException;
import java.io.OutputStream;

public class SystemOutStream extends OutputStream {
    private boolean closed = false;

    @Override
    public void write(int i) {
        if (!closed)
            System.out.write(i);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        if (!closed)
            System.out.write(bytes);
    }

    @Override
    public void write(byte[] bytes, int i, int i1) {
        if (!closed)
            System.out.write(bytes, i, i1);
    }

    @Override
    public void flush() {
        if (!closed)
            System.out.flush();
    }

    @Override
    public void close() throws IOException {
        // avoid closing System.out
        super.close();
        closed = true;
    }
}
