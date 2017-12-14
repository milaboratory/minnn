package com.milaboratory.mist.io;

import cc.redberry.pipe.*;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.io.sequence.fasta.*;
import com.milaboratory.core.io.sequence.fastq.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.output_converter.*;
import com.milaboratory.mist.pattern.*;
import com.milaboratory.util.CanReportProgress;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.milaboratory.mist.io.MistDataFormat.*;
import static com.milaboratory.mist.output_converter.GroupUtils.*;
import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class ReadProcessor {
    private final List<String> inputFileNames;
    private final List<String> outputFileNames;
    private final Pattern pattern;
    private final boolean orientedReads;
    private final boolean fairSorting;
    private final int firstReadNumber;
    private final int threads;
    private final boolean copyOldComments;
    private final MistDataFormat inputFormat;
    private final MistDataFormat outputFormat;
    private final boolean testIOSpeed;

    public ReadProcessor(List<String> inputFileNames, List<String> outputFileNames, Pattern pattern,
            boolean orientedReads, boolean fairSorting, int firstReadNumber, int threads, boolean copyOldComments,
            MistDataFormat inputFormat, MistDataFormat outputFormat, boolean testIOSpeed) {
        if ((inputFormat == MIF) && (inputFileNames.size() > 1))
            throw exitWithError("Mif data format uses single file; specified " + inputFileNames.size()
                    + " input files!");
        if ((outputFormat == MIF) && (outputFileNames.size() > 1))
            throw exitWithError("Mif data format uses single file; specified " + outputFileNames.size()
                    + " output files!");
        if (((inputFileNames.size() > 1) || (outputFileNames.size() > 1))
                && (inputFileNames.size() != outputFileNames.size()))
            throw exitWithError("Not equal numbers of input and output file names!");
        if (pattern instanceof SinglePattern && (inputFileNames.size() > 1))
            throw exitWithError("Trying to use pattern for single read with multiple reads!");
        if (pattern instanceof MultipleReadsOperator
                && (inputFileNames.size() != ((MultipleReadsOperator)pattern).getNumberOfPatterns()))
            throw exitWithError("Mismatched number of patterns ("
                    + ((MultipleReadsOperator)pattern).getNumberOfPatterns() + ") and reads (" + inputFileNames.size()
                    + ")!");
        this.inputFileNames = inputFileNames;
        this.outputFileNames = outputFileNames;
        this.pattern = pattern;
        this.orientedReads = orientedReads;
        this.fairSorting = fairSorting;
        this.firstReadNumber = firstReadNumber;
        this.threads = threads;
        this.copyOldComments = copyOldComments;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.testIOSpeed = testIOSpeed;
    }

    public void processReadsParallel() {
        long startTime = System.currentTimeMillis();
        SequenceReaderCloseable<? extends SequenceRead> reader;
        SequenceWriter writer;
        try {
            reader = createReader();
            writer = createWriter();
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }
        OutputPort<? extends SequenceRead> readerPort = reader;
        CanReportProgress progress = (CanReportProgress)reader;
        SmartProgressReporter.startProgressReport("Parsing", progress);
        OutputPort<? extends SequenceRead> bufferedReaderPort = CUtils.buffered(readerPort, 2048);

        OutputPort<ProcessorInput> processorInputs = () -> {
            SequenceRead read = bufferedReaderPort.take();
            return (read == null) ? null : new ProcessorInput(read, orientedReads);
        };
        OutputPort<ParsedRead> parsedReadsPort = new ParallelProcessor<>(processorInputs,
                testIOSpeed ? new TestIOSpeedProcessor() : new ReadParserProcessor(), threads);
        OrderedOutputPort<ParsedRead> orderedReadsPort = new OrderedOutputPort<>(parsedReadsPort,
                object -> object.getOriginalRead().getId());

        long totalReads = 0;
        long matchedReads = 0;
        for (ParsedRead parsedRead : CUtils.it(orderedReadsPort)) {
            SequenceRead parsedSequenceRead = parsedRead.getParsedRead();
            totalReads++;
            if (parsedSequenceRead != null) {
                writer.write(parsedSequenceRead);
                matchedReads++;
            }
        }
        writer.close();

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.out.println(String.format("Matched reads: %.1f%%\n",
                totalReads == 0 ? 0.0 : matchedReads * 100.0 / totalReads));
    }

    private SequenceReaderCloseable<? extends SequenceRead> createReader() throws IOException {
        switch (inputFormat) {
            case FASTQ:
                switch (inputFileNames.size()) {
                    case 0:
                        return new SingleFastqReader(System.in);
                    case 1:
                        String[] s = inputFileNames.get(0).split("\\.");
                        if (s[s.length - 1].equals("fasta") || s[s.length - 1].equals("fa"))
                            return new FastaSequenceReaderWrapper(new FastaReader<>(inputFileNames.get(0),
                                    NucleotideSequence.ALPHABET), true);
                        else
                            return new SingleFastqReader(inputFileNames.get(0), true);
                    case 2:
                        return new PairedFastqReader(inputFileNames.get(0), inputFileNames.get(1),
                                true);
                    default:
                        SingleFastqReader readers[] = new SingleFastqReader[inputFileNames.size()];
                        for (int i = 0; i < inputFileNames.size(); i++)
                            readers[i] = new SingleFastqReader(inputFileNames.get(i), true);
                        return new MultiReader(readers);
                }
            case MIF:
                if (inputFileNames.size() == 0)
                    return new MifReader(System.in);
                else
                    return new MifReader(inputFileNames.get(0));
            default:
                throw new IllegalStateException("Unknown input format: " + inputFormat);
        }
    }

    private SequenceWriter createWriter() throws IOException {
        switch (outputFormat) {
            case FASTQ:
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
            case MIF:
                if (outputFileNames.size() == 0)
                    return new MifWriter(System.out);
                else
                    return new MifWriter(outputFileNames.get(0));
            default:
                throw new IllegalStateException("Unknown output format: " + outputFormat);
        }
    }

    private class ProcessorInput {
        final SequenceRead read;
        final boolean orientedReads;

        ProcessorInput(SequenceRead read, boolean orientedReads) {
            this.read = read;
            this.orientedReads = orientedReads;
        }
    }

    private class ReadParserProcessor implements Processor<ProcessorInput, ParsedRead> {
        @Override
        public ParsedRead process(ProcessorInput input) {
            Match bestMatch = null;
            int numberOfReads;
            boolean reverseMatch = false;
            if (input.orientedReads) {
                MultiNSequenceWithQualityImpl target = new MultiNSequenceWithQualityImpl(StreamSupport.stream(
                        input.read.spliterator(), false).map(SingleRead::getData)
                        .toArray(NSequenceWithQuality[]::new));
                bestMatch = pattern.match(target).getBestMatch(fairSorting);
                numberOfReads = target.numberOfSequences();
            } else {
                NSequenceWithQuality[] sequences = StreamSupport.stream(input.read.spliterator(), false)
                        .map(SingleRead::getData).toArray(NSequenceWithQuality[]::new);
                numberOfReads = sequences.length;
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

            if (bestMatch == null)
                return new ParsedRead(input.read);
            else {
                SingleRead[] reads = new SingleReadImpl[numberOfReads];
                for (int i = 0; i < numberOfReads; i++) {
                    String mainGroupName = "R" + (firstReadNumber + i);
                    ArrayList<MatchedGroup> currentGroups = getGroupsFromMatch(bestMatch, i);
                    MatchedRange mainGroup = currentGroups.stream().filter(g -> g.getGroupName().equals(mainGroupName))
                            .map(g -> (MatchedRange)g).findFirst().orElse(bestMatch.getMatchedRange(i));
                    ArrayList<MatchedGroup> groupsInsideMain = getGroupsInsideMain(currentGroups, mainGroup.getRange(),
                            true).stream().filter(g -> !g.getGroupName().equals(mainGroupName))
                            .collect(Collectors.toCollection(ArrayList::new));
                    ArrayList<MatchedGroup> groupsNotInsideMain = getGroupsInsideMain(currentGroups,
                            mainGroup.getRange(), false);
                    String groupsInsideMainDescription = groupsToReadDescription(groupsInsideMain, mainGroupName,
                            true);
                    String groupsNotInsideMainDescription = groupsToReadDescription(groupsNotInsideMain, mainGroupName,
                            false);
                    String notMatchedGroupsDescription = descriptionForNotMatchedGroups(pattern, i, currentGroups);

                    StringBuilder comments = new StringBuilder();
                    String nextSeparator = "~";
                    if (copyOldComments)
                        comments.append(input.read.getRead(i).getDescription());
                    if ((comments.length() != 0) && (reverseMatch || (groupsNotInsideMainDescription.length() != 0))) {
                        comments.append(nextSeparator);
                        nextSeparator = "|";
                    }
                    if (reverseMatch) {
                        comments.append("||~");
                        nextSeparator = "";
                    }
                    comments.append(groupsNotInsideMainDescription);
                    if (groupsInsideMainDescription.length() != 0) {
                        comments.append(nextSeparator);
                        nextSeparator = "|";
                    }
                    comments.append(groupsInsideMainDescription);
                    if (notMatchedGroupsDescription.length() != 0)
                        comments.append(nextSeparator);
                    comments.append(notMatchedGroupsDescription);

                    reads[i] = new SingleReadImpl(0, mainGroup.getValue(), comments.toString());
                }

                SequenceRead parsedRead;
                switch (numberOfReads) {
                    case 1:
                        parsedRead = reads[0];
                        break;
                    case 2:
                        parsedRead = new PairedRead(reads[0], reads[1]);
                        break;
                    default:
                        parsedRead = new MultiRead(reads);
                }
                return new ParsedRead(input.read, parsedRead, getGroupsFromMatch(bestMatch), reverseMatch,
                        bestMatch.getScore());
            }
        }
    }

    private class TestIOSpeedProcessor implements Processor<ProcessorInput, ParsedRead> {
        @Override
        public ParsedRead process(ProcessorInput input) {
            return new ParsedRead(input.read, input.read, new ArrayList<>(), false, 0);
        }
    }
}
