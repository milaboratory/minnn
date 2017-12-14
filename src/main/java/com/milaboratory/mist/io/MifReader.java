package com.milaboratory.mist.io;

import com.milaboratory.core.io.sequence.SequenceRead;
import com.milaboratory.core.io.sequence.SequenceReaderCloseable;

import java.io.IOException;
import java.io.InputStream;

final class MifReader implements SequenceReaderCloseable<SequenceRead> {
    MifReader(InputStream stream) throws IOException {

    }

    MifReader(String file) throws IOException {

    }

    @Override
    public void close() {

    }

    @Override
    public long getNumberOfReads() {
        return 0;
    }

    @Override
    public SequenceRead take() {
        return null;
    }
}
