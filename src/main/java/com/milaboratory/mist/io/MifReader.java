package com.milaboratory.mist.io;

import cc.redberry.pipe.OutputPortCloseable;
import com.milaboratory.core.io.CompressionType;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.util.CanReportProgress;
import com.milaboratory.util.CountingInputStream;

import java.io.*;
import java.util.ArrayList;

import static java.lang.Double.NaN;

public final class MifReader implements OutputPortCloseable<ParsedRead>, CanReportProgress {
    private static final int DEFAULT_BUFFER_SIZE = 1048576;
    private final PrimitivI input;
    private final CountingInputStream countingInputStream;
    private final long size;
    private boolean finished = false;
    private int numberOfReads;
    private ArrayList<GroupEdge> groupEdges = new ArrayList<>();

    public MifReader(InputStream stream) {
        input = new PrimitivI(this.countingInputStream = new CountingInputStream(stream));
        readHeader();
        size = -1;
    }

    public MifReader(String fileName) throws IOException {
        File file = new File(fileName);
        CompressionType ct = CompressionType.detectCompressionType(file);
        this.countingInputStream = new CountingInputStream(new FileInputStream(file));
        if (ct == CompressionType.None) {
            input = new PrimitivI(new BufferedInputStream(this.countingInputStream, DEFAULT_BUFFER_SIZE));
            size = file.length();
        } else {
            input = new PrimitivI(ct.createInputStream(this.countingInputStream, DEFAULT_BUFFER_SIZE));
            size = -1;
        }
        readHeader();
    }

    private void readHeader() {
        numberOfReads = input.readInt();
        int groupEdgesNum = input.readInt();
        for (int i = 0; i < groupEdgesNum; i++) {
            GroupEdge groupEdge = input.readObject(GroupEdge.class);
            input.putKnownReference(groupEdge);
            groupEdges.add(groupEdge);
        }
    }

    @Override
    public void close() {
        input.close();
        finished = true;
    }

    @Override
    public double getProgress() {
        if (size == -1)
            return NaN;
        return (1.0 * countingInputStream.getBytesRead()) / size;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public synchronized ParsedRead take() {
        if (finished)
            return null;
        ParsedRead parsedRead = input.readObject(ParsedRead.class);
        if (parsedRead == null)
            finished = true;
        return parsedRead;
    }

    public int getNumberOfReads() {
        return numberOfReads;
    }

    public ArrayList<GroupEdge> getGroupEdges() {
        return new ArrayList<>(groupEdges);
    }

    public MifHeader getHeader() {
        return new MifHeader(numberOfReads, groupEdges);
    }
}
