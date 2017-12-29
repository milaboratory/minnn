package com.milaboratory.mist.outputconverter;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.SerializersManager;
import com.milaboratory.util.ObjectSerializer;

import java.io.*;
import java.util.Collection;

public final class ParsedReadObjectSerializer implements ObjectSerializer<ParsedRead> {
    private static final SerializersManager serializersManager = new SerializersManager();

    @Override
    public void write(Collection<ParsedRead> data, OutputStream stream) {
        final PrimitivO out = new PrimitivO(new DataOutputStream(stream), serializersManager);
        for (ParsedRead parsedRead : data)
            out.writeObject(parsedRead);
    }

    @Override
    public OutputPort<ParsedRead> read(InputStream stream) {
        final PrimitivI in = new PrimitivI(new DataInputStream(stream), serializersManager);
        return () -> in.readObject(ParsedRead.class);
    }
}
