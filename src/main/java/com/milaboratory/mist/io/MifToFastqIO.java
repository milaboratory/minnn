package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPortCloseable;
import com.milaboratory.core.io.sequence.SequenceRead;
import com.milaboratory.core.io.sequence.SequenceWriter;
import com.milaboratory.core.io.sequence.fastq.MultiFastqWriter;
import com.milaboratory.core.io.sequence.fastq.PairedFastqWriter;
import com.milaboratory.core.io.sequence.fastq.SingleFastqWriter;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class MifToFastqIO {
    private final String inputFileName;
    private final String[] outputGroupNames;
    private final String[] outputFileNames;
    private final boolean copyOriginalHeaders;

    public MifToFastqIO(String inputFileName, LinkedHashMap<String, String> outputGroups, boolean copyOriginalHeaders) {
        this.inputFileName = inputFileName;
        this.outputGroupNames = new String[outputGroups.size()];
        this.outputFileNames = new String[outputGroups.size()];
        int index = 0;
        for (HashMap.Entry<String, String> entry : outputGroups.entrySet()) {
            outputGroupNames[index] = entry.getKey();
            outputFileNames[index] = entry.getValue();
            index++;
        }
        this.copyOriginalHeaders = copyOriginalHeaders;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        try (MifReader reader = createReader();
             SequenceWriter writer = createWriter()) {
            SmartProgressReporter.startProgressReport("Processing", reader, System.err);
            OutputPortCloseable<SequenceRead> sequenceReads = new SequenceReadOutputPort(reader);
            for (SequenceRead sequenceRead : CUtils.it(sequenceReads)) {
                totalReads++;
                writer.write(sequenceRead);
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads\n");
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private SequenceWriter createWriter() throws IOException {
        switch (outputFileNames.length) {
            case 1:
                return new SingleFastqWriter(outputFileNames[0]);
            case 2:
                return new PairedFastqWriter(outputFileNames[0], outputFileNames[1]);
            default:
                return new MultiFastqWriter(outputFileNames);
        }
    }

    private class SequenceReadOutputPort implements OutputPortCloseable<SequenceRead> {
        private final MifReader reader;
        private final ArrayList<GroupEdge> groupEdges;

        SequenceReadOutputPort(MifReader reader) {
            this.reader = reader;
            this.groupEdges = reader.getGroupEdges();
        }

        @Override
        public SequenceRead take() {
            ParsedRead parsedRead = reader.take();
            if (parsedRead == null)
                return null;
            else
                return parsedRead.toSequenceRead(copyOriginalHeaders, groupEdges, outputGroupNames);
        }

        @Override
        public void close() {
            reader.close();
        }
    }
}
