package com.milaboratory.mist.io;

import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.primitivio.PrimitivO;

import java.io.*;

public final class MifWriter implements AutoCloseable {
    private static final int DEFAULT_BUFFER_SIZE = 1 << 22;
    private final PrimitivO output;

    public MifWriter(OutputStream outputStream, MifHeader mifHeader) {
        output = new PrimitivO(outputStream);
        writeHeader(mifHeader);
    }

    public MifWriter(String file, MifHeader mifHeader) throws IOException {
        output = new PrimitivO(new BufferedOutputStream(new FileOutputStream(file), DEFAULT_BUFFER_SIZE));
        writeHeader(mifHeader);
    }

    private void writeHeader(MifHeader mifHeader) {
        output.writeInt(mifHeader.getNumberOfReads());
        output.writeInt(mifHeader.getGroupEdges().size());
        for (GroupEdge groupEdge : mifHeader.getGroupEdges()) {
            output.writeObject(groupEdge);
            output.putKnownReference(groupEdge);
        }
    }

    public void write(ParsedRead parsedRead) {
        output.writeObject(parsedRead);
    }

    @Override
    public void close() {
        output.writeObject(null);
        output.close();
    }
}
