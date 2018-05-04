package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPortCloseable;
import com.milaboratory.core.io.CompressionType;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.outputconverter.ParsedReadObjectSerializer;
import com.milaboratory.util.SmartProgressReporter;
import com.milaboratory.util.Sorter;
import com.milaboratory.util.TempFileManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.milaboratory.mist.cli.Defaults.DEFAULT_SORT_CHUNK_SIZE;
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
        this.chunkSize = (chunkSize == -1) ? estimateChunkSize() : chunkSize;
        this.tmpFile = TempFileManager.getTempFile();
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        try (MifReader reader = createReader();
             MifWriter writer = createWriter(reader.getHeader())) {
            SmartProgressReporter.startProgressReport("Reading", reader, System.err);
            SmartProgressReporter.startProgressReport("Writing", writer, System.err);
            if (!reader.isCorrected())
                System.err.println("WARNING: sorting not corrected MIF file!");
            OutputPortCloseable<ParsedRead> sorted = Sorter.sort(reader, new ParsedReadComparator(), chunkSize,
                    new ParsedReadObjectSerializer(reader.getGroupEdges()), tmpFile);
            for (ParsedRead parsedRead : CUtils.it(sorted)) {
                totalReads++;
                if (totalReads == 1)
                    writer.setEstimatedNumberOfReads(reader.getEstimatedNumberOfReads());
                writer.write(parsedRead);
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Sorted " + totalReads + " reads\n");
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private MifWriter createWriter(MifHeader inputHeader) throws IOException {
        MifHeader outputHeader = new MifHeader(inputHeader.getNumberOfReads(), inputHeader.isCorrected(), true,
                inputHeader.getGroupEdges());
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                : new MifWriter(outputFileName, outputHeader);
    }

    private int estimateChunkSize() {
        if (inputFileName == null)
            return DEFAULT_SORT_CHUNK_SIZE;
        else {
            // heuristic to auto-determine chunk size by input file size
            File inputFile = new File(inputFileName);
            CompressionType ct = CompressionType.detectCompressionType(inputFile);
            int averageBytesPerParsedRead = (ct == CompressionType.None) ? 50 : 15;
            return (int)Math.min(Math.max(16384, inputFile.length() / averageBytesPerParsedRead / 8), 1048576);
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
