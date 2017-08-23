package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.io.sequence.fasta.*;
import com.milaboratory.core.io.sequence.fastq.*;
import com.milaboratory.core.sequence.MultiNSequenceWithQualityImpl;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mist.output_converter.*;
import com.milaboratory.mist.pattern.*;
import com.milaboratory.util.CanReportProgress;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    private final boolean addOldComment;

    public ReadProcessor(List<String> inputFileNames, List<String> outputFileNames, Pattern pattern,
            boolean orientedReads, boolean fairSorting, int firstReadNumber, int threads, boolean addOldComment) {
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
        this.addOldComment = addOldComment;
    }

    public void processReadsParallel() {
        List<SequenceReaderCloseable<? extends SequenceRead>> readers = new ArrayList<>();
        SequenceWriter writer;
        try {
            readers.add(createReader(false));
            if (!orientedReads && (inputFileNames.size() >= 2))
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
            OutputPort<ProcessorInput> processorInputs = () -> new ProcessorInput(inputReads.take(), reverseMatch);
            OutputPort<ParsedRead> parsedReadsPort = new ParallelProcessor<>(processorInputs, new ReadParserProcessor(),
                    threads);
            OrderedOutputPort<ParsedRead> orderedReadsPort = new OrderedOutputPort<>(parsedReadsPort,
                    object -> object.getOriginalRead().getId());
            parsedReads.add(orderedReadsPort);
        }
        OutputPort<ParsedRead> bestMatchPort = () -> {
            ParsedRead bestRead = null;
            for (OrderedOutputPort<ParsedRead> parsedRead : parsedReads) {
                if (bestRead == null)
                    bestRead = parsedRead.take();
                else {
                    ParsedRead currentRead = parsedRead.take();
                    if (currentRead.getBestMatchScore() > currentRead.getBestMatchScore())
                        bestRead = currentRead;
                }
            }
            if (bestRead == null)
                throw new NullPointerException();
            else
                return bestRead;
        };
        for (ParsedRead parsedRead : CUtils.it(bestMatchPort))
            writer.write(parsedRead.getParsedRead());
        writer.close();
    }

    private SequenceReaderCloseable<? extends SequenceRead> createReader(boolean swapped) throws IOException {
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
                    return new PairedFastqReader(inputFileNames.get(1), inputFileNames.get(0), true);
                else
                    return new PairedFastqReader(inputFileNames.get(0), inputFileNames.get(1), true);
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
    }

    private SequenceWriter createWriter() throws IOException {
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
                return new ParsedRead(input.read, null, new ArrayList<>());
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
                    String description = addOldComment ? input.read.getRead(i).getDescription() + "~" : "";
                    if (input.reverseMatch)
                        description += "|~";
                    description += groupsToReadDescription(groupsNotInsideMain, mainGroupName, false)
                            + (((groupsNotInsideMain.size() == 0) || (groupsInsideMain.size() == 0)) ? "" : '~')
                            + groupsToReadDescription(groupsInsideMain, mainGroupName, true);
                    reads[i] = new SingleReadImpl(0, mainGroup.getValue(), description);
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
}