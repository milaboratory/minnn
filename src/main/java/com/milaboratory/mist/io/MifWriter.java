package com.milaboratory.mist.io;

import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.util.CanReportProgress;

import java.io.*;

import static java.lang.Double.NaN;

public final class MifWriter implements AutoCloseable, CanReportProgress {
    private static final int DEFAULT_BUFFER_SIZE = 1 << 20;
    private final PrimitivO output;
    private boolean finished = false;
    private long estimatedNumberOfReads = -1;
    private long writtenReads = 0;

    public MifWriter(OutputStream outputStream, MifHeader mifHeader) {
        output = new PrimitivO(outputStream);
        writeHeader(mifHeader);
    }

    public MifWriter(String file, MifHeader mifHeader) throws IOException {
        output = new PrimitivO(new BufferedOutputStream(new FileOutputStream(file), DEFAULT_BUFFER_SIZE));
        writeHeader(mifHeader);
    }

    public MifWriter(String file, MifHeader mifHeader, int bufferSize) throws IOException {
        output = new PrimitivO(new BufferedOutputStream(new FileOutputStream(file), bufferSize));
        writeHeader(mifHeader);
    }

    private void writeHeader(MifHeader mifHeader) {
        output.writeInt(mifHeader.getNumberOfReads());
        output.writeInt(mifHeader.getCorrectedGroups().size());
        for (String correctedGroup : mifHeader.getCorrectedGroups())
            output.writeObject(correctedGroup);
        output.writeBoolean(mifHeader.isSorted());
        output.writeInt(mifHeader.getGroupEdges().size());
        for (GroupEdge groupEdge : mifHeader.getGroupEdges()) {
            output.writeObject(groupEdge);
            output.putKnownObject(groupEdge);
        }
    }

    public void write(ParsedRead parsedRead) {
        output.writeObject(parsedRead);
        writtenReads++;
    }

    @Override
    public void close() {
        output.writeObject(null);
        output.close();
        finished = true;
    }

    public void setEstimatedNumberOfReads(long estimatedNumberOfReads) {
        this.estimatedNumberOfReads = estimatedNumberOfReads;
    }

    @Override
    public double getProgress() {
        if (estimatedNumberOfReads == -1)
            return (writtenReads == 0) ? 0 : NaN;
        else
            return Math.min(1, (double)writtenReads / estimatedNumberOfReads);
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
