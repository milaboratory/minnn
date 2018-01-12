package com.milaboratory.mist.io;

import cc.redberry.pipe.OutputPortCloseable;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.util.CanReportProgress;
import com.milaboratory.util.CountingInputStream;

import java.io.*;

import static java.lang.Double.NaN;

final class MifReader implements OutputPortCloseable<ParsedRead>, CanReportProgress {
    private final PrimitivI input;
    private final CountingInputStream countingInputStream;
    private final long size;
    private boolean finished = false;

    MifReader(InputStream stream) {
        input = new PrimitivI(this.countingInputStream = new CountingInputStream(stream));
        initKnownReferences();
        size = 0;
    }

    MifReader(String fileName) throws IOException {
        File file = new File(fileName);
        input = new PrimitivI(this.countingInputStream = new CountingInputStream(new FileInputStream(file)));
        initKnownReferences();
        size = file.length();
    }

    private void initKnownReferences() {
        int groupEdgesNum = input.readInt();
        for (int i = 0; i < groupEdgesNum; i++)
            input.putKnownReference(input.readObject(GroupEdge.class));
    }

    @Override
    public void close() {
        input.close();
        finished = true;
    }

    @Override
    public double getProgress() {
        if (size == 0)
            return NaN;
        return (1.0 * countingInputStream.getBytesRead()) / size;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public ParsedRead take() {
        ParsedRead parsedRead = input.readObject(ParsedRead.class);
        if (parsedRead == null)
            finished = true;
        return parsedRead;
    }
}
