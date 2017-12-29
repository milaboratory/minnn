package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPortCloseable;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.outputconverter.ParsedReadObjectSerializer;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.mist.pattern.MatchedGroupEdge;
import com.milaboratory.util.SmartProgressReporter;
import com.milaboratory.util.Sorter;
import com.milaboratory.util.TempFileManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class SorterIO {
    private final String inputFileName;
    private final String outputFileName;
    private final List<String> sortGroupNames;
    private final int chunkSize;
    private final File tmpFile;

    public SorterIO(String inputFileName, String outputFileName, List<String> sortGroupNames, int chunkSize) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.sortGroupNames = sortGroupNames;
        this.chunkSize = chunkSize;
        this.tmpFile = TempFileManager.getTempFile();
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        MifReader reader;
        OutputPortCloseable<ParsedRead> sorted;
        try {
            if (inputFileName == null)
                reader = new MifReader(System.in);
            else
                reader = new MifReader(inputFileName);
            SmartProgressReporter.startProgressReport("Sorting", reader);
            sorted = Sorter.sort(reader, new ParsedReadComparator(), chunkSize, new ParsedReadObjectSerializer(),
                    tmpFile);
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        MifWriter writer = null;
        long totalReads = 0;
        for (ParsedRead parsedRead : CUtils.it(sorted)) {
            totalReads++;
            if (writer == null) {
                ArrayList<GroupEdge> groupEdges = parsedRead.getBestMatch().getMatchedGroupEdges().stream()
                        .map(MatchedGroupEdge::getGroupEdge).collect(Collectors.toCollection(ArrayList::new));
                writer = createWriter(groupEdges);
            }
            writer.write(parsedRead);
        }
        if (writer == null)
            writer = createWriter(new ArrayList<>());
        reader.close();
        writer.close();

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.out.println("Sorted " + totalReads + " reads\n");
    }

    private MifWriter createWriter(ArrayList<GroupEdge> groupEdges) {
        if (outputFileName == null)
            return new MifWriter(System.out, groupEdges);
        else
            try {
                return new MifWriter(outputFileName, groupEdges);
            } catch (IOException e) {
                throw exitWithError(e.getMessage());
            }
    }

    private class ParsedReadComparator implements Comparator<ParsedRead> {
        @Override
        public int compare(ParsedRead parsedRead1, ParsedRead parsedRead2) {
            for (String groupName : sortGroupNames) {
                NSequenceWithQuality read1Value = parsedRead1.getBestMatch().getGroupValue(groupName);
                NSequenceWithQuality read2Value = parsedRead2.getBestMatch().getGroupValue(groupName);
                if ((read1Value == null) && (read2Value != null))
                    return -1;
                else if ((read1Value != null) && (read2Value == null))
                    return 1;
                else if (read1Value != null) {
                    int compareValue = read1Value.getSequence().compareTo(read2Value.getSequence());
                    if (compareValue != 0)
                        return compareValue;
                }
            }
            return 0;
        }
    }
}
