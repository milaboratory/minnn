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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class MifToFastqIO {
    private final String inputFileName;
    private final List<String> outputFileNames;
    private final String[] groupNames;
    private final boolean noDefaultGroups;
    private final boolean copyOldComments;

    public MifToFastqIO(String inputFileName, List<String> outputFileNames, List<String> groupNames,
                        boolean noDefaultGroups, boolean copyOldComments) {
        this.inputFileName = inputFileName;
        this.outputFileNames = outputFileNames;
        this.groupNames = groupNames.toArray(new String[groupNames.size()]);
        this.noDefaultGroups = noDefaultGroups;
        this.copyOldComments = copyOldComments;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        try (MifReader reader = createReader();
             SequenceWriter writer = createWriter(reader.getNumberOfReads())) {
            SmartProgressReporter.startProgressReport("Processing", reader);
            OutputPortCloseable<SequenceRead> sequenceReads = new SequenceReadOutputPort(reader);
            for (SequenceRead sequenceRead : CUtils.it(sequenceReads)) {
                totalReads++;
                writer.write(sequenceRead);
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.out.println("Processed " + totalReads + " reads\n");
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private SequenceWriter createWriter(int numberOfReads) throws IOException {
        LinkedHashSet<String> outputGroupNames = noDefaultGroups ? new LinkedHashSet<>() : IntStream.rangeClosed(1,
                numberOfReads).mapToObj(n -> "R" + n).collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> overrideGroupsList = noDefaultGroups ? new ArrayList<>() : Arrays.stream(groupNames)
                .filter(outputGroupNames::contains).collect(Collectors.toList());
        if (overrideGroupsList.size() > 0)
            System.out.println("Warning! Overriding default group names: " + overrideGroupsList);
        outputGroupNames.addAll(Arrays.asList(groupNames));
        int outputFilesNum = (outputFileNames.size() == 0) ? 1 : outputFileNames.size();
        if (outputGroupNames.size() != outputFilesNum)
            throw exitWithError("Mismatched number of output files (" + outputFilesNum + ") and output "
                    + "reads (" + outputGroupNames.size() + ")! Group names for output reads: " + outputGroupNames);

        switch (outputFileNames.size()) {
            case 0:
                return new SingleFastqWriter(System.out);
            case 1:
                return new SingleFastqWriter(outputFileNames.get(0));
            case 2:
                return new PairedFastqWriter(outputFileNames.get(0), outputFileNames.get(1));
            default:
                return new MultiFastqWriter(outputFileNames.toArray(new String[outputFileNames.size()]));
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
                return parsedRead.toSequenceRead(copyOldComments, noDefaultGroups, groupEdges, groupNames);
        }

        @Override
        public void close() {
            reader.close();
        }
    }
}
