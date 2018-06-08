package com.milaboratory.mist.io;

import cc.redberry.pipe.*;
import cc.redberry.pipe.blocks.Merger;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.Chunk;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.io.sequence.fasta.*;
import com.milaboratory.core.io.sequence.fastq.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.outputconverter.*;
import com.milaboratory.mist.pattern.*;
import com.milaboratory.util.CanReportProgress;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import static com.milaboratory.core.io.sequence.SequenceReadUtil.setReadId;
import static com.milaboratory.mist.io.MistDataFormat.*;
import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class ReadProcessor {
    private final List<String> inputFileNames;
    private final String outputFileName;
    private final Pattern pattern;
    private final boolean orientedReads;
    private final boolean fairSorting;
    private final long inputReadsLimit;
    private final int threads;
    private final MistDataFormat inputFormat;
    private int numberOfReads;

    public ReadProcessor(List<String> inputFileNames, String outputFileName, Pattern pattern,
                         boolean orientedReads, boolean fairSorting, long inputReadsLimit, int threads,
                         MistDataFormat inputFormat) {
        if ((inputFormat == MIF) && (inputFileNames.size() > 1))
            throw exitWithError("Mif data format uses single file; specified " + inputFileNames.size()
                    + " input files!");
        this.inputFileNames = inputFileNames;
        this.outputFileName = outputFileName;
        this.pattern = pattern;
        this.orientedReads = orientedReads;
        this.fairSorting = fairSorting;
        this.inputReadsLimit = inputReadsLimit;
        this.threads = threads;
        this.inputFormat = inputFormat;
    }

    public void processReadsParallel() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        long matchedReads = 0;
        try (OutputPortCloseable<SequenceRead> reader = (OutputPortCloseable<SequenceRead>)createReader();
             MifWriter writer = createWriter()) {
            CanReportProgress progress = (CanReportProgress)reader;
            SmartProgressReporter.startProgressReport("Parsing", progress, System.err);
            Merger<Chunk<SequenceRead>> bufferedReaderPort = CUtils.buffered(CUtils.chunked(reader,
                    4 * 64), 4 * 16);
            OutputPort<Chunk<ParsedRead>> parsedReadsPort = new ParallelProcessor<>(bufferedReaderPort,
                    CUtils.chunked(new ReadParserProcessor(orientedReads)), threads);
            OrderedOutputPort<ParsedRead> orderedReadsPort = new OrderedOutputPort<>(CUtils.unchunked(parsedReadsPort),
                    read -> read.getOriginalRead().getId());
            for (ParsedRead parsedRead : CUtils.it(orderedReadsPort)) {
                if (parsedRead.getBestMatch() != null) {
                    writer.write(parsedRead);
                    matchedReads++;
                }
                if (++totalReads == inputReadsLimit)
                    break;
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println(String.format("Matched reads: %.1f%%\n",
                totalReads == 0 ? 0.0 : matchedReads * 100.0 / totalReads));
    }

    private OutputPortCloseable<? extends SequenceRead> createReader() throws IOException {
        OutputPortCloseable<? extends SequenceRead> reader;
        switch (inputFormat) {
            case FASTQ:
                switch (inputFileNames.size()) {
                    case 0:
                        numberOfReads = 1;
                        reader = new SingleFastqReader(System.in);
                        break;
                    case 1:
                        numberOfReads = 1;
                        String[] s = inputFileNames.get(0).split("\\.");
                        if (s[s.length - 1].equals("fasta") || s[s.length - 1].equals("fa")
                                || ((s.length > 2) && s[s.length - 1].equals("gz")
                                    && (s[s.length - 2].equals("fasta") || s[s.length - 2].equals("fa"))))
                            reader = new FastaSequenceReaderWrapper(new FastaReader<>(
                                    inputFileNames.get(0), NucleotideSequence.ALPHABET));
                        else
                            reader = new SingleFastqReader(inputFileNames.get(0));
                        break;
                    case 2:
                        numberOfReads = 2;
                        reader = new PairedFastqReader(inputFileNames.get(0), inputFileNames.get(1));
                        break;
                    default:
                        numberOfReads = inputFileNames.size();
                        SingleFastqReader readers[] = new SingleFastqReader[inputFileNames.size()];
                        for (int i = 0; i < inputFileNames.size(); i++)
                            readers[i] = new SingleFastqReader(inputFileNames.get(i));
                        reader = new MultiReader(readers);
                }
                break;
            case MIF:
                MifReader mifReader = (inputFileNames.size() == 0) ? new MifReader(System.in)
                        : new MifReader(inputFileNames.get(0));
                if (inputReadsLimit > 0)
                    mifReader.setParsedReadsLimit(inputReadsLimit);
                numberOfReads = mifReader.getNumberOfReads();
                reader = new MifSequenceReader(mifReader);
                break;
            default:
                throw new IllegalStateException("Unknown input format: " + inputFormat);
        }
        int readsInPattern = pattern instanceof SinglePattern ? 1
                : ((MultipleReadsOperator)pattern).getNumberOfPatterns();
        if (numberOfReads != readsInPattern)
            throw exitWithError("Mismatched number of patterns (" + readsInPattern + ") and reads ("
                    + numberOfReads + ")!");
        return reader;
    }

    private MifWriter createWriter() throws IOException {
        MifHeader mifHeader = new MifHeader(numberOfReads, new ArrayList<>(), false, pattern.getGroupEdges());
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), mifHeader)
                : new MifWriter(outputFileName, mifHeader);
    }

    private class MifSequenceReader implements OutputPortCloseable<SequenceRead>, CanReportProgress {
        private final MifReader mifReader;
        private AtomicLong index = new AtomicLong(0);

        MifSequenceReader(MifReader mifReader) {
            this.mifReader = mifReader;
        }

        @Override
        public void close() {
            mifReader.close();
        }

        @Override
        public SequenceRead take() {
            ParsedRead parsedRead = mifReader.take();
            return (parsedRead == null) ? null : setReadId(index.getAndIncrement(), parsedRead.getOriginalRead());
        }

        @Override
        public double getProgress() {
            return mifReader.getProgress();
        }

        @Override
        public boolean isFinished() {
            return mifReader.isFinished();
        }
    }

    private class ReadParserProcessor implements Processor<SequenceRead, ParsedRead> {
        private final boolean orientedReads;

        public ReadParserProcessor(boolean orientedReads) {
            this.orientedReads = orientedReads;
        }

        @Override
        public ParsedRead process(SequenceRead input) {
            Match bestMatch = null;
            boolean reverseMatch = false;
            if (orientedReads) {
                MultiNSequenceWithQualityImpl target = new MultiNSequenceWithQualityImpl(StreamSupport.stream(
                        input.spliterator(), false).map(SingleRead::getData)
                        .toArray(NSequenceWithQuality[]::new));
                bestMatch = pattern.match(target).getBestMatch(fairSorting);
            } else {
                NSequenceWithQuality[] sequences = StreamSupport.stream(input.spliterator(), false)
                        .map(SingleRead::getData).toArray(NSequenceWithQuality[]::new);
                int numberOfReads = sequences.length;
                if (numberOfReads == 1)
                    bestMatch = pattern.match(sequences[0]).getBestMatch(fairSorting);
                else {
                    NSequenceWithQuality[] sequencesWithSwap = sequences.clone();
                    sequencesWithSwap[0] = sequences[1];
                    sequencesWithSwap[1] = sequences[0];
                    MultiNSequenceWithQualityImpl notSwappedTarget = new MultiNSequenceWithQualityImpl(sequences);
                    MultiNSequenceWithQualityImpl swappedTarget = new MultiNSequenceWithQualityImpl(sequencesWithSwap);
                    Match notSwappedMatch = pattern.match(notSwappedTarget).getBestMatch(fairSorting);
                    Match swappedMatch = pattern.match(swappedTarget).getBestMatch(fairSorting);
                    if (notSwappedMatch == null) {
                        if (swappedMatch != null) {
                            bestMatch = swappedMatch;
                            reverseMatch = true;
                        }
                    } else {
                        if (swappedMatch != null) {
                            if (swappedMatch.getScore() > notSwappedMatch.getScore()) {
                                bestMatch = swappedMatch;
                                reverseMatch = true;
                            } else
                                bestMatch = notSwappedMatch;
                        } else
                            bestMatch = notSwappedMatch;
                    }
                }
            }

            if (bestMatch != null)
                bestMatch.assembleGroups();
            return new ParsedRead(input, reverseMatch, bestMatch, 0);
        }
    }
}
