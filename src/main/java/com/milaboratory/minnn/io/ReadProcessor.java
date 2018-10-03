/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact MiLaboratory LLC, which owns exclusive
 * rights for distribution of this program for commercial purposes, using the
 * following email address: licensing@milaboratory.com.
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.minnn.io;

import cc.redberry.pipe.*;
import cc.redberry.pipe.blocks.Merger;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.Chunk;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.io.sequence.fasta.*;
import com.milaboratory.core.io.sequence.fastq.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.cli.DescriptionGroups;
import com.milaboratory.minnn.outputconverter.*;
import com.milaboratory.minnn.pattern.*;
import com.milaboratory.util.CanReportProgress;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static com.milaboratory.minnn.cli.CliUtils.floatFormat;
import static com.milaboratory.minnn.io.MinnnDataFormat.*;
import static com.milaboratory.minnn.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class ReadProcessor {
    private final List<String> inputFileNames;
    private final String outputFileName;
    private final Pattern pattern;
    private final boolean orientedReads;
    private final boolean fairSorting;
    private final long inputReadsLimit;
    private final int threads;
    private final MinnnDataFormat inputFormat;
    private final DescriptionGroups descriptionGroups;
    private int numberOfTargets;

    public ReadProcessor(List<String> inputFileNames, String outputFileName, Pattern pattern,
                         boolean orientedReads, boolean fairSorting, long inputReadsLimit, int threads,
                         MinnnDataFormat inputFormat, DescriptionGroups descriptionGroups) {
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
        this.descriptionGroups = descriptionGroups;
    }

    public void processReadsParallel() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        long matchedReads = 0;
        try (IndexedSequenceReader<?> reader = createReader();
             MifWriter writer = createWriter()) {
            SmartProgressReporter.startProgressReport("Parsing", reader, System.err);
            Merger<Chunk<IndexedSequenceRead>> bufferedReaderPort = CUtils.buffered(CUtils.chunked(reader,
                    4 * 64), 4 * 16);
            OutputPort<Chunk<ParsedRead>> parsedReadsPort = new ParallelProcessor<>(bufferedReaderPort,
                    CUtils.chunked(new ReadParserProcessor(orientedReads)), threads);
            OrderedOutputPort<ParsedRead> orderedReadsPort = new OrderedOutputPort<>(CUtils.unchunked(parsedReadsPort),
                    ParsedRead::getOutputPortId);
            for (ParsedRead parsedRead : CUtils.it(orderedReadsPort)) {
                if (parsedRead.getBestMatch() != null) {
                    writer.write(parsedRead);
                    matchedReads++;
                }
                if (++totalReads == inputReadsLimit)
                    break;
            }
            reader.close();
            long originalNumberOfReads = (inputFormat == MIF) ? reader.getOriginalNumberOfReads() : totalReads;
            writer.setOriginalNumberOfReads(originalNumberOfReads);
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        float percent = (totalReads == 0) ? 0 : (float)matchedReads / totalReads * 100;
        System.err.println("Matched reads: " + floatFormat.format(percent) + "%\n");
    }

    private IndexedSequenceReader createReader() throws IOException {
        IndexedSequenceReader reader;
        switch (inputFormat) {
            case FASTQ:
                switch (inputFileNames.size()) {
                    case 0:
                        numberOfTargets = 1;
                        reader = new IndexedSequenceReader<>(new SingleFastqReader(System.in), t -> t);
                        break;
                    case 1:
                        numberOfTargets = 1;
                        String[] s = inputFileNames.get(0).split("\\.");
                        if (s[s.length - 1].equals("fasta") || s[s.length - 1].equals("fa")
                                || ((s.length > 2) && s[s.length - 1].equals("gz")
                                    && (s[s.length - 2].equals("fasta") || s[s.length - 2].equals("fa"))))
                            reader = new IndexedSequenceReader<>(new FastaSequenceReaderWrapper(new FastaReader<>(
                                    inputFileNames.get(0), NucleotideSequence.ALPHABET)), t -> t);
                        else
                            reader = new IndexedSequenceReader<>(new SingleFastqReader(inputFileNames.get(0)), t -> t);
                        break;
                    case 2:
                        numberOfTargets = 2;
                        reader = new IndexedSequenceReader<>(new PairedFastqReader(inputFileNames.get(0),
                                inputFileNames.get(1)), t -> t);
                        break;
                    default:
                        numberOfTargets = inputFileNames.size();
                        SingleFastqReader readers[] = new SingleFastqReader[inputFileNames.size()];
                        for (int i = 0; i < inputFileNames.size(); i++)
                            readers[i] = new SingleFastqReader(inputFileNames.get(i));
                        reader = new IndexedSequenceReader<>(new MultiReader(readers), t -> t);
                }
                break;
            case MIF:
                MifReader mifReader = (inputFileNames.size() == 0) ? new MifReader(System.in)
                        : new MifReader(inputFileNames.get(0));
                if (inputReadsLimit > 0)
                    mifReader.setParsedReadsLimit(inputReadsLimit);
                numberOfTargets = mifReader.getNumberOfTargets();
                reader = new IndexedSequenceReader<>(mifReader, ParsedRead::getOriginalRead);
                break;
            default:
                throw new IllegalStateException("Unknown input format: " + inputFormat);
        }
        int targetsInPattern = pattern instanceof SinglePattern ? 1
                : ((MultipleReadsOperator)pattern).getNumberOfPatterns();
        if (numberOfTargets != targetsInPattern)
            throw exitWithError("Mismatched number of patterns (" + targetsInPattern + ") and target reads ("
                    + numberOfTargets + ")!");
        return reader;
    }

    private MifWriter createWriter() throws IOException {
        MifHeader mifHeader = new MifHeader(numberOfTargets, new ArrayList<>(), false, pattern.getGroupEdges());
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), mifHeader)
                : new MifWriter(outputFileName, mifHeader);
    }

    private class IndexedSequenceRead {
        final SequenceRead sequenceRead;
        final long index;

        IndexedSequenceRead(SequenceRead sequenceRead, long index) {
            this.sequenceRead = sequenceRead;
            this.index = index;
        }
    }

    private class IndexedSequenceReader<T> implements OutputPortCloseable<IndexedSequenceRead>, CanReportProgress {
        private final OutputPortCloseable<? extends T> innerReader;
        private final Function<T, SequenceRead> toSequenceRead;
        private final CanReportProgress progress;
        private AtomicLong index = new AtomicLong(0);

        IndexedSequenceReader(OutputPortCloseable<? extends T> innerReader, Function<T, SequenceRead> toSequenceRead) {
            this.innerReader = innerReader;
            this.toSequenceRead = toSequenceRead;
            this.progress = innerReader instanceof CanReportProgress ? (CanReportProgress)innerReader : null;
        }

        @Override
        public synchronized void close() {
            innerReader.close();
        }

        @Override
        public IndexedSequenceRead take() {
            T t = innerReader.take();
            if (t == null)
                return null;
            else
                return new IndexedSequenceRead(toSequenceRead.apply(t), index.getAndIncrement());
        }

        @Override
        public double getProgress() {
            if (progress != null)
                return progress.getProgress();
            else
                return Double.NaN;
        }

        @Override
        public synchronized boolean isFinished() {
            return (progress != null) && progress.isFinished();
        }

        long getOriginalNumberOfReads() {
            if (inputFormat == FASTQ)
                throw new IllegalStateException("getOriginalNumberOfReads() must be used only for MIF input!");
            else
                return ((MifReader)innerReader).getOriginalNumberOfReads();
        }
    }

    private class ReadParserProcessor implements Processor<IndexedSequenceRead, ParsedRead> {
        private final boolean orientedReads;

        ReadParserProcessor(boolean orientedReads) {
            this.orientedReads = orientedReads;
        }

        @Override
        public ParsedRead process(IndexedSequenceRead input) {
            Match bestMatch = null;
            boolean reverseMatch = false;
            if (orientedReads) {
                MultiNSequenceWithQualityImpl target = new MultiNSequenceWithQualityImpl(StreamSupport.stream(
                        input.sequenceRead.spliterator(), false).map(SingleRead::getData)
                        .toArray(NSequenceWithQuality[]::new));
                bestMatch = pattern.match(target).getBestMatch(fairSorting);
            } else {
                NSequenceWithQuality[] sequences = StreamSupport.stream(input.sequenceRead.spliterator(), false)
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

            return new ParsedRead(input.sequenceRead, reverseMatch, (bestMatch == null) ? null
                    : descriptionGroups.addDescriptionGroups(bestMatch, input.sequenceRead),
                    0, input.index);
        }
    }
}
