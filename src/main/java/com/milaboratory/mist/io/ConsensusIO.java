package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mist.outputconverter.MatchedGroup;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.mist.pattern.Match;
import com.milaboratory.mist.pattern.MatchedGroupEdge;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class ConsensusIO {
    private final String inputFileName;
    private final String outputFileName;
    private final int alignerWidth;
    private final int threads;
    private final AtomicLong totalReads = new AtomicLong(0);
    private final AtomicLong consensusReads = new AtomicLong(0);
    private Set<String> groupList;

    public ConsensusIO(List<String> groupList, String inputFileName, String outputFileName, int alignerWidth,
                       int threads) {
        this.groupList = (groupList == null) ? null : new LinkedHashSet<>(groupList);
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.alignerWidth = alignerWidth;
        this.threads = threads;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        try (MifReader reader = createReader();
             MifWriter writer = createWriter(reader.getHeader())) {
            SmartProgressReporter.startProgressReport("Calculating consensus", reader, System.err);
            if (!reader.isCorrected())
                System.err.println("WARNING: calculating consensus for not corrected MIF file!");
            if (!reader.isSorted())
                System.err.println("WARNING: calculating consensus for not sorted MIF file; result will be wrong!");
            Set<String> defaultGroups = IntStream.rangeClosed(1, reader.getNumberOfReads())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());

            OutputPort<Dataset> datasetOutputPort = new OutputPort<Dataset>() {
                LinkedHashMap<String, NucleotideSequence> previousGroups = null;
                Dataset currentDataset = new Dataset(0);
                int orderedPortIndex = 0;
                boolean finished = false;

                @Override
                public synchronized Dataset take() {
                    if (finished)
                        return null;
                    Dataset preparedDataset = null;
                    while (preparedDataset == null) {
                        ParsedRead parsedRead = reader.take();
                        if (parsedRead != null) {
                            Set<String> allGroups = parsedRead.getGroups().stream().map(MatchedGroup::getGroupName)
                                    .filter(groupName -> !defaultGroups.contains(groupName))
                                    .collect(Collectors.toSet());
                            if (groupList != null) {
                                for (String groupName : groupList)
                                    if (!allGroups.contains(groupName))
                                        throw exitWithError("Group " + groupName + " not found in the input!");
                            } else
                                groupList = allGroups;
                            LinkedHashMap<String, NucleotideSequence> currentGroups = parsedRead.getGroups().stream()
                                    .filter(g -> groupList.contains(g.getGroupName()))
                                    .collect(LinkedHashMap::new, (m, g) -> m.put(g.getGroupName(),
                                            g.getValue().getSequence()), Map::putAll);
                            if (!currentGroups.equals(previousGroups)) {
                                if (previousGroups != null) {
                                    preparedDataset = currentDataset;
                                    currentDataset = new Dataset(++orderedPortIndex);
                                }
                                previousGroups = currentGroups;
                            }
                            currentDataset.data.add(new DataFromParsedRead(parsedRead, defaultGroups));
                            totalReads.getAndIncrement();
                        } else {
                            finished = true;
                            if (previousGroups != null)
                                return currentDataset;
                            else
                                return null;
                        }
                    }
                    return preparedDataset;
                }
            };

            OutputPort<CalculatedConsensuses> calculatedConsensusesPort = new ParallelProcessor<>(datasetOutputPort,
                    new DatasetProcessor(), threads);
            OrderedOutputPort<CalculatedConsensuses> orderedConsensusesPort = new OrderedOutputPort<>(
                    calculatedConsensusesPort, cc -> cc.orderedPortIndex);
            for (CalculatedConsensuses calculatedConsensuses : CUtils.it(orderedConsensusesPort))
                for (Consensus consensus : calculatedConsensuses.consensuses)
                    writer.write(consensus.toParsedRead(consensusReads.getAndIncrement()));
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads\n");
        System.err.println("Calculated " + consensusReads + " consensuses\n");
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private MifWriter createWriter(MifHeader mifHeader) throws IOException {
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), mifHeader)
                : new MifWriter(outputFileName, mifHeader);
    }

    private class DataFromParsedRead {
        final NSequenceWithQuality[] sequences;

        DataFromParsedRead(ParsedRead parsedRead, Set<String> defaultGroups) {
            List<MatchedGroup> extractedGroups = parsedRead.getGroups().stream()
                    .filter(g -> defaultGroups.contains(g.getGroupName())).collect(Collectors.toList());
            sequences = new NSequenceWithQuality[extractedGroups.size()];
            extractedGroups.forEach(g -> sequences[g.getTargetId() - 1] = g.getValue());
        }
    }

    private class Consensus {
        final NSequenceWithQuality[] sequences;
        final long score;

        Consensus(NSequenceWithQuality[] sequences, long score) {
            this.sequences = sequences;
            this.score = score;
        }

        ParsedRead toParsedRead(long readId) {
            SequenceRead originalRead;
            SingleRead[] reads = new SingleRead[sequences.length];
            ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
            for (byte targetId = 1; targetId <= sequences.length; targetId++) {
                NSequenceWithQuality currentSequence = sequences[targetId - 1];
                reads[targetId - 1] = new SingleReadImpl(readId, currentSequence, "Consensus");
                matchedGroupEdges.add(new MatchedGroupEdge(currentSequence, targetId,
                        new GroupEdge("R" + targetId, true), 0));
                matchedGroupEdges.add(new MatchedGroupEdge(currentSequence, targetId,
                        new GroupEdge("R" + targetId, false), currentSequence.size()));
            }
            if (sequences.length == 1)
                originalRead = reads[0];
            else if (sequences.length == 2)
                originalRead = new PairedRead(reads);
            else
                originalRead = new MultiRead(reads);

            Match bestMatch = new Match(sequences.length, score, matchedGroupEdges);
            return new ParsedRead(originalRead, false, bestMatch);
        }
    }

    private class Dataset {
        final ArrayList<DataFromParsedRead> data = new ArrayList<>();
        final long orderedPortIndex;

        Dataset(long orderedPortIndex) {
            this.orderedPortIndex = orderedPortIndex;
        }
    }

    private class CalculatedConsensuses {
        final ArrayList<Consensus> consensuses = new ArrayList<>();
        final long orderedPortIndex;

        CalculatedConsensuses(long orderedPortIndex) {
            this.orderedPortIndex = orderedPortIndex;
        }
    }

    private class DatasetProcessor implements Processor<Dataset, CalculatedConsensuses> {
        @Override
        public CalculatedConsensuses process(Dataset dataset) {
            CalculatedConsensuses calculatedConsensuses = new CalculatedConsensuses(dataset.orderedPortIndex);
            ArrayList<DataFromParsedRead> data = dataset.data;
            HashMap<Integer, HashMap<NucleotideSequence, Integer>> subsequences;
            HashMap<Integer, HashMap<NucleotideSequence, Integer>> lettersInPositions;

            // stage 1: align to best quality
            long bestSumQuality = 0;
            int bestSeqIndex = 0;
            for (int i = 0; i < data.size(); i++) {
                long sumQuality = Arrays.stream(data.get(i).sequences).mapToLong(this::calculateSumQuality).sum();
                if (sumQuality > bestSumQuality) {
                    bestSumQuality = sumQuality;
                    bestSeqIndex = i;
                }
            }
            NSequenceWithQuality[] bestSequences = data.get(bestSeqIndex).sequences;


            return calculatedConsensuses;
        }

        private long calculateSumQuality(NSequenceWithQuality seq) {
            long sum = 0;
            for (byte quality : seq.getQuality().asArray())
                sum += quality;
            return sum;
        }
    }
}
