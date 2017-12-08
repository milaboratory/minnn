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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.milaboratory.mist.io.MifReader.detectReadsNumber;
import static com.milaboratory.mist.io.MistDataFormat.*;
import static com.milaboratory.mist.io.ReadsNumber.*;
import static com.milaboratory.mist.output_converter.GroupUtils.*;
import static com.milaboratory.mist.util.SystemUtils.exitWithError;

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
    private final ReadsNumber readsNumber;
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
        if (inputFormat == FASTQ) {
            if (inputFileNames.size() <= 1)
                this.readsNumber = SINGLE;
            else if (inputFileNames.size() == 2)
                this.readsNumber = PAIRED;
            else
                this.readsNumber = MULTI;
        } else if (inputFormat == MIF)
            this.readsNumber = detectReadsNumber();
        else
            throw new IllegalStateException("Unknown input format: " + inputFormat);
        this.testIOSpeed = testIOSpeed;
    }

    public void processReadsParallel() {
        long startTime = System.currentTimeMillis();
        List<SequenceReaderCloseable<? extends SequenceRead>> readers = new ArrayList<>();
        SequenceWriter writer;
        try {
            readers.add(createReader(false));
            if (!orientedReads && (readsNumber != SINGLE))
                readers.add(createReader(true));
            writer = createWriter();
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }
        List<OutputPort<? extends SequenceRead>> readerPorts = new ArrayList<>(readers);
        List<CanReportProgress> progress = readers.stream().map(r -> (CanReportProgress)r).collect(Collectors.toList());
        SmartProgressReporter.startProgressReport("Parsing", progress.get(0));
        if (progress.size() == 2)
            SmartProgressReporter.startProgressReport("Parsing with swapped reads", progress.get(1));
        List<OutputPort<? extends SequenceRead>> bufferedReaderPorts = readerPorts.stream()
                .map(rp -> CUtils.buffered(rp, 16)).collect(Collectors.toList());
        List<OrderedOutputPort<ParsedRead>> parsedReads = new ArrayList<>();
        for (int i = 0; i < readers.size(); i++) {
            OutputPort<? extends SequenceRead> inputReads = bufferedReaderPorts.get(i);
            boolean reverseMatch = (i == 1);
            OutputPort<ProcessorInput> processorInputs = () -> {
                SequenceRead read = inputReads.take();
                return (read == null) ? null : new ProcessorInput(read, reverseMatch);
            };
            OutputPort<ParsedRead> parsedReadsPort = new ParallelProcessor<>(processorInputs,
                    testIOSpeed ? new TestIOSpeedProcessor() : new ReadParserProcessor(), threads);
            OrderedOutputPort<ParsedRead> orderedReadsPort = new OrderedOutputPort<>(parsedReadsPort,
                    object -> object.getOriginalRead().getId());
            parsedReads.add(orderedReadsPort);
        }
        OutputPort<ParsedRead> bestMatchPort = () -> {
            ParsedRead bestRead = null;
            for (OrderedOutputPort<ParsedRead> parsedRead : parsedReads) {
                if (bestRead == null) {
                    bestRead = parsedRead.take();
                    if (bestRead == null)
                        return null;
                } else {
                    ParsedRead currentRead = parsedRead.take();
                    if (currentRead.getBestMatchScore() > currentRead.getBestMatchScore())
                        bestRead = currentRead;
                }
            }
            return bestRead;
        };

        long totalReads = 0;
        long matchedReads = 0;
        for (ParsedRead parsedRead : CUtils.it(bestMatchPort)) {
            SequenceRead parsedSequenceRead = parsedRead.getParsedRead();
            totalReads++;
            if (parsedSequenceRead != null) {
                writer.write(parsedSequenceRead);
                matchedReads++;
            }
        }
        writer.close();

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println(String.format("\nProcessing time: %02d min, %02d sec",
                TimeUnit.MILLISECONDS.toMinutes(elapsedTime), TimeUnit.MILLISECONDS.toSeconds(elapsedTime)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime))));
        System.out.println(String.format("Matched reads: %.1f%%\n",
                totalReads == 0 ? 0.0 : matchedReads * 100.0 / totalReads));
    }

    private SequenceReaderCloseable<? extends SequenceRead> createReader(boolean swapped) throws IOException {
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
                        if (swapped)
                            return new PairedFastqReader(inputFileNames.get(1), inputFileNames.get(0),
                                    true);
                        else
                            return new PairedFastqReader(inputFileNames.get(0), inputFileNames.get(1),
                                    true);
                    default:
                        List<SingleFastqReader> readers = new ArrayList<>();
                        if (swapped) {
                            for (int i = 0; i < inputFileNames.size(); i++) {
                                if (i < inputFileNames.size() - 2)
                                    readers.add(new SingleFastqReader(inputFileNames.get(i), true));
                                else if (i == inputFileNames.size() - 2)
                                    readers.add(new SingleFastqReader(inputFileNames.get(i + 1), true));
                                else
                                    readers.add(new SingleFastqReader(inputFileNames.get(i - 1), true));
                            }
                        } else
                            for (String fileName : inputFileNames)
                                readers.add(new SingleFastqReader(fileName, true));
                        return new MultiReader(readers.toArray(new SingleFastqReader[readers.size()]));
                }
            case MIF:
                if (inputFileNames.size() == 0)
                    return new MifReader(System.in);
                else
                    return new MifReader(inputFileNames.get(0), swapped);
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
        final boolean reverseMatch;

        ProcessorInput(SequenceRead read, boolean reverseMatch) {
            this.read = read;
            this.reverseMatch = reverseMatch;
        }
    }

    private class ReadParserProcessor implements Processor<ProcessorInput, ParsedRead> {
        @Override
        public ParsedRead process(ProcessorInput input) {
            MultiNSequenceWithQualityImpl target = new MultiNSequenceWithQualityImpl(StreamSupport.stream(
                    input.read.spliterator(), false).map(SingleRead::getData).toArray(NSequenceWithQuality[]::new));
            MatchingResult result = pattern.match(target);
            Match bestMatch = result.getBestMatch(fairSorting);
            if (bestMatch == null)
                return new ParsedRead(input.read);
            else {
                int numberOfReads = target.numberOfSequences();
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
                    if (copyOldComments)
                        comments.append(input.read.getRead(i).getDescription());
                    if ((comments.length() != 0) && (input.reverseMatch
                            || (groupsNotInsideMainDescription.length() != 0)))
                        comments.append("~");
                    if (input.reverseMatch)
                        comments.append("||~");
                    comments.append(groupsNotInsideMainDescription);
                    if ((comments.length() != 0) && ((groupsInsideMainDescription.length() != 0)))
                        comments.append("~");
                    comments.append(groupsInsideMainDescription);
                    if ((comments.length() != 0) && (notMatchedGroupsDescription.length() != 0))
                        comments.append("~");
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
                return new ParsedRead(input.read, parsedRead, getGroupsFromMatch(bestMatch), input.reverseMatch,
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
