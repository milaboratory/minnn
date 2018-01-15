package com.milaboratory.mist.outputconverter;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.SerializersManager;
import com.milaboratory.util.ObjectSerializer;

import java.io.*;
import java.util.*;

public final class ParsedReadObjectSerializer implements ObjectSerializer<ParsedRead> {
    private final List<GroupEdge> groupEdges;
    private final SerializersManager serializersManager = new SerializersManager();

    public ParsedReadObjectSerializer(List<GroupEdge> groupEdges) {
        this.groupEdges = groupEdges;
    }

    @Override
    public void write(Collection<ParsedRead> data, OutputStream stream) {
        final PrimitivO out = new PrimitivO(new DataOutputStream(stream), serializersManager);
        for (GroupEdge groupEdge : groupEdges) {
            out.writeObject(groupEdge);
            out.putKnownReference(groupEdge);
        }
        for (ParsedRead parsedRead : data)
            out.writeObject(parsedRead);
    }

    @Override
    public OutputPort<ParsedRead> read(InputStream stream) {
        final PrimitivI in = new PrimitivI(new DataInputStream(stream), serializersManager);
        for (int i = 0; i < groupEdges.size(); i++) {
            GroupEdge groupEdge = in.readObject(GroupEdge.class);
            in.putKnownReference(groupEdge);
        }
        return () -> in.readObject(ParsedRead.class);
    }
}
