package com.milaboratory.mist.io;

import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.primitivio.PrimitivO;

import java.io.*;
import java.util.ArrayList;

public final class MifWriter implements AutoCloseable {
    private final PrimitivO output;

    public MifWriter(OutputStream outputStream, ArrayList<GroupEdge> groupEdges) {
        output = new PrimitivO(outputStream);
        initKnownReferences(groupEdges);
    }

    public MifWriter(String file, ArrayList<GroupEdge> groupEdges) throws IOException {
        output = new PrimitivO(new FileOutputStream(file));
        initKnownReferences(groupEdges);
    }

    private void initKnownReferences(ArrayList<GroupEdge> groupEdges) {
        output.writeInt(groupEdges.size());
        for (GroupEdge groupEdge : groupEdges) {
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
