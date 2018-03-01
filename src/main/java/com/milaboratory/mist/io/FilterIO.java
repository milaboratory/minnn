package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.Merger;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.Chunk;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.readfilter.ReadFilter;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class FilterIO {
    private final ReadFilter readFilter;
    private final String inputFileName;
    private final String outputFileName;
    private final int threads;

    public FilterIO(ReadFilter readFilter, String inputFileName, String outputFileName, int threads) {
        this.readFilter = readFilter;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.threads = threads;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        long matchedReads = 0;
        try (MifReader reader = createReader();
             MifWriter writer = createWriter(reader.getHeader())) {
            SmartProgressReporter.startProgressReport("Filtering reads", reader, System.err);
            Merger<Chunk<ParsedRead>> bufferedReaderPort = CUtils.buffered(CUtils.chunked(
                    new NumberedParsedReadsPort(reader), 4 * 64), 4 * 16);
            OutputPort<Chunk<ParsedRead>> correctedReadsPort = new ParallelProcessor<>(bufferedReaderPort,
                    CUtils.chunked(new FilterProcessor()), threads);
            OrderedOutputPort<ParsedRead> orderedReadsPort = new OrderedOutputPort<>(
                    CUtils.unchunked(correctedReadsPort), read -> read.getOriginalRead().getId());
            for (ParsedRead parsedRead : CUtils.it(orderedReadsPort)) {
                totalReads++;
                if (parsedRead.getBestMatch() != null) {
                    writer.write(parsedRead);
                    matchedReads++;
                }
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads, matched " + matchedReads + " reads\n");
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private MifWriter createWriter(MifHeader mifHeader) throws IOException {
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), mifHeader)
                : new MifWriter(outputFileName, mifHeader);
    }

    private class FilterProcessor implements Processor<ParsedRead, ParsedRead> {
        @Override
        public ParsedRead process(ParsedRead parsedRead) {
            return readFilter.filter(parsedRead);
        }
    }
}
