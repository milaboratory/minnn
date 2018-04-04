package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.alignment.LinearGapAlignmentScoring;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.core.alignment.BandedLinearAligner.alignLocalGlobal;
import static com.milaboratory.mist.cli.Defaults.*;
import static com.milaboratory.mist.pattern.PatternUtils.invertCoordinate;
import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class ConsensusIO {
    private final String inputFileName;
    private final String outputFileName;
    private final int alignerWidth;
    private final int matchScore;
    private final int mismatchScore;
    private final int gapScore;
    private final long penaltyThreshold;
    private final int threads;
    private final AtomicLong totalReads = new AtomicLong(0);
    private final AtomicLong consensusReads = new AtomicLong(0);
    private Set<String> groupList;

    public ConsensusIO(List<String> groupList, String inputFileName, String outputFileName, int alignerWidth,
                       int matchScore, int mismatchScore, int gapScore, long penaltyThreshold, int threads) {
        this.groupList = (groupList == null) ? null : new LinkedHashSet<>(groupList);
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.alignerWidth = alignerWidth;
        this.matchScore = matchScore;
        this.mismatchScore = mismatchScore;
        this.gapScore = gapScore;
        this.penaltyThreshold = penaltyThreshold;
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
        private final LinearGapAlignmentScoring<NucleotideSequence> scoring = new LinearGapAlignmentScoring<>(
                NucleotideSequence.ALPHABET, matchScore, mismatchScore, gapScore);

        @Override
        public CalculatedConsensuses process(Dataset dataset) {
            CalculatedConsensuses calculatedConsensuses = new CalculatedConsensuses(dataset.orderedPortIndex);
            ArrayList<DataFromParsedRead> data = dataset.data;
            ArrayList<ArrayList<NSequenceWithQuality>> subsequences = new ArrayList<>();
            ArrayList<HashMap<NucleotideSequence, Integer>> lettersInPositions = new ArrayList<>();

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
            for (int i = 0; i < data.size(); i++) {
                if (i != bestSeqIndex) {
                    for (int targetIndex = 0; targetIndex < bestSequences.length; targetIndex++) {
                        NSequenceWithQuality currentSequence = data.get(i).sequences[targetIndex];
                        Alignment<NucleotideSequence> alignment = alignLocalGlobal(scoring,
                                bestSequences[targetIndex].getSequence(), currentSequence.getSequence(), alignerWidth);

                    }
                } else {

                }
            }

            return calculatedConsensuses;
        }

        private long calculateSumQuality(NSequenceWithQuality seq) {
            long sum = 0;
            for (byte quality : seq.getQuality().asArray())
                sum += quality;
            return sum;
        }

        private byte calculateMinQuality(NSequenceWithQuality seq) {
            byte minQuality = DEFAULT_GOOD_QUALITY;
            for (byte quality : seq.getQuality().asArray())
                if (quality < minQuality)
                    minQuality = quality;
            return minQuality;
        }

        private Consensus generateConsensus(ArrayList<AlignedSubsequences> subsequencesList,
                                            NSequenceWithQuality[] bestSequences) {
            int numTargets = bestSequences.length;
            ArrayList<LettersWithPositions> lettersList = new ArrayList<>();
            for (int targetIndex = 0; targetIndex < numTargets; targetIndex++) {
                for (int position = 0; position < bestSequences[targetIndex].size(); position++) {
                    ArrayList<NSequenceWithQuality> currentPositionSequences = new ArrayList<>();
                    int bestQualityIndex = -1;
                    byte bestQuality = 0;
                    for (int i = 0; i < subsequencesList.size(); i++) {
                        AlignedSubsequences currentSubsequences = subsequencesList.get(i);
                        NSequenceWithQuality currentSequence = currentSubsequences.get(targetIndex, position);
                        currentPositionSequences.add(currentSequence);
                        byte currentQuality = (currentSequence == NSequenceWithQuality.EMPTY)
                                ? 0 : calculateMinQuality(currentSequence);
                        if (currentQuality > bestQuality) {
                            bestQuality = currentQuality;
                            bestQualityIndex = i;
                        }
                    }
                    if (bestQualityIndex == -1)
                        throw new IllegalStateException("bestQualityIndex not found in " + subsequencesList);
                    LettersMatrix lettersMatrix = new LettersMatrix(currentPositionSequences.get(bestQualityIndex),
                            bestQualityIndex);
                    for (int i = 0; i < currentPositionSequences.size(); i++) {
                        if (i != bestQualityIndex) {
                            NSequenceWithQuality currentSeq = currentPositionSequences.get(i);
                            if (currentSeq != NSequenceWithQuality.EMPTY)
                                lettersMatrix.add(currentSeq, alignLocalGlobal(scoring,
                                    currentPositionSequences.get(bestQualityIndex).getSequence(),
                                    currentPositionSequences.get(i).getSequence(), alignerWidth));
                            else
                                lettersMatrix.addEmpty();
                        }
                    }


                }
            }
        }

        private abstract class MultiTargetArray {
            protected final int[] indexes;
            protected NSequenceWithQuality[] sequences = null;

            MultiTargetArray(int numTargets) {
                indexes = new int[numTargets];
            }

            protected int index(int targetIndex, int position) {
                return indexes[targetIndex] + position;
            }
        }

        private class AlignedSubsequences extends MultiTargetArray {
            AlignedSubsequences(NSequenceWithQuality[] bestSequences) {
                super(bestSequences.length);
                int currentIndex = 0;
                for (int i = 0; i < bestSequences.length; i++) {
                    indexes[i] = currentIndex;
                    currentIndex += bestSequences[i].size();
                }
                sequences = new NSequenceWithQuality[currentIndex];
            }

            void set(int targetIndex, int position, NSequenceWithQuality value) {
                sequences[index(targetIndex, position)] = value;
            }

            NSequenceWithQuality get(int targetIndex, int position) {
                NSequenceWithQuality value = sequences[index(targetIndex, position)];
                if (value == null)
                    throw new IllegalStateException("Subsequence with targetIndex " + targetIndex + " and position "
                            + position + " is not initialized!");
                else
                    return value;
            }
        }

        private class LettersWithPositions extends MultiTargetArray {
            private List<Boolean> initializedReads;
            private HashMap<Integer, ArrayList<NSequenceWithQuality>> tempValues = new HashMap<>();
            private boolean initialized = false;

            LettersWithPositions(int numTargets) {
                super(numTargets);
                initializedReads = Collections.nCopies(numTargets, false);
            }

            void set(int targetIndex, ArrayList<NSequenceWithQuality> values) {
                if (initialized)
                    throw new IllegalStateException("LettersWithPositions already initialized, but set(" + targetIndex
                            + ", " + values + ") was called!");
                if (initializedReads.get(targetIndex))
                    throw new IllegalStateException("Trying to initialize letters for targetIndex " + targetIndex
                            + " with values " + values + " while there are already stored values "
                            + tempValues.get(targetIndex));
                for (NSequenceWithQuality value : values)
                    if ((value != NSequenceWithQuality.EMPTY) && (value.size() != 1))
                        throw new IllegalArgumentException("Trying to write sequence " + value
                                + " to LettersWithPositions");
                tempValues.put(targetIndex, values);
                initializedReads.set(targetIndex, true);
                if (!initializedReads.contains(false)) {
                    int currentIndex = 0;
                    for (int i = 0; i < indexes.length; i++) {
                        indexes[i] = currentIndex;
                        currentIndex += tempValues.get(i).size();
                    }
                    sequences = new NSequenceWithQuality[currentIndex];
                    for (int i = 0; i < indexes.length; i++) {
                        ArrayList<NSequenceWithQuality> currentValues = tempValues.get(i);
                        for (int j = 0; j < currentValues.size(); j++)
                            sequences[index(i, j)] = currentValues.get(j);
                    }
                    tempValues = null;
                    initializedReads = null;
                    initialized = true;
                }
            }

            NSequenceWithQuality get(int targetIndex, int position) {
                if (!initialized)
                    throw new IllegalStateException("LettersWithPositions was not initialized, but get(" + targetIndex
                            + ", " + position + ") was called! Current sequences array: " + Arrays.toString(sequences));
                NSequenceWithQuality letter = sequences[index(targetIndex, position)];
                if (letter == null)
                    throw new IllegalStateException("Letter with targetIndex " + targetIndex + " and position "
                            + position + " is not initialized!");
                else
                    return letter;
            }

            byte getDeletionQuality(int targetIndex, int position) {
                if (get(targetIndex, position) != NSequenceWithQuality.EMPTY)
                    throw new IllegalArgumentException("getDeletionQuality() called for sequence "
                            + get(targetIndex, position));
                int deletionIndex = index(targetIndex, position);
                int startIndex = indexes[indexes.length - 1];
                int endIndex = sequences.length;    // exclusive
                for (int i = 1; i < indexes.length; i++)
                    if (indexes[i] > deletionIndex) {
                        startIndex = indexes[i - 1];
                        endIndex = indexes[i];
                    }
                NSequenceWithQuality foundPreviousSeq = null;
                NSequenceWithQuality foundNextSeq = null;
                int currentPreviousIndex = deletionIndex - 1;
                int currentNextIndex = deletionIndex + 1;
                while (currentPreviousIndex >= startIndex) {
                    NSequenceWithQuality currentSeq = sequences[currentPreviousIndex];
                    if (currentSeq != NSequenceWithQuality.EMPTY) {
                        foundPreviousSeq = currentSeq;
                        break;
                    }
                    currentPreviousIndex--;
                }
                while (currentNextIndex < endIndex) {
                    NSequenceWithQuality currentSeq = sequences[currentNextIndex];
                    if (currentSeq != NSequenceWithQuality.EMPTY) {
                        foundNextSeq = currentSeq;
                        break;
                    }
                    currentNextIndex++;
                }
                if ((foundPreviousSeq != null) && (foundNextSeq != null))
                    return (byte)((calculateMinQuality(foundPreviousSeq) + calculateMinQuality(foundNextSeq)) / 2);
                else if (foundPreviousSeq != null)
                    return calculateMinQuality(foundPreviousSeq);
                else if (foundNextSeq != null)
                    return calculateMinQuality(foundNextSeq);
                else
                    throw new IllegalStateException("Sequence with indexes from " + startIndex + " to " + endIndex
                            + " is empty!");
            }
        }

        private class LettersMatrix {
            // column numbers in the matrix corresponding to base sequence letters; last value is row length
            private final int[] baseLettersCoordinates;
            private final int baseSequenceRealIndex;
            private final ArrayList<ArrayList<Integer>> positionsCache = new ArrayList<>();
            private final ArrayList<NSequenceWithQuality> sequences = new ArrayList<>();

            LettersMatrix(NSequenceWithQuality baseSequence, int baseSequenceRealIndex) {
                baseLettersCoordinates = IntStream.rangeClosed(0, baseSequence.size()).toArray();
                sequences.add(baseSequence);
                this.baseSequenceRealIndex = baseSequenceRealIndex;
            }

            int getRowLength() {
                return baseLettersCoordinates[baseLettersCoordinates.length - 1];
            }

            void add(NSequenceWithQuality sequence, Alignment<NucleotideSequence> alignment) {
                int stage = 0;  // 0 - before base start, 1 - inside alignment range, 2 - after base end
                int leftTailLength = 0;
                int rightTailLength = 0;
                int currentPartLength = 1;
                sequences.add(sequence);
                ArrayList<Integer> currentPositions = new ArrayList<>();
                NSequenceWithQuality baseSequence = sequences.get(0);
                for (int i = 0; i < sequence.size(); i++) {
                    int baseSequencePosition = alignment.convertToSeq1Position(i);
                    if (baseSequencePosition == -1) {
                        if (stage == 0) {
                            currentPositions.add(-1);
                            leftTailLength++;
                        } else {
                            currentPositions.add(baseSequence.size());
                            stage = 2;
                            rightTailLength++;
                        }
                    } else {
                        if (stage == 2)
                            throw new IllegalArgumentException("3 or more contiguous parts of seq2 are out of range "
                                    + "in seq1; seq1: " + baseSequence + ", seq2: " + sequence + ", alignment: "
                                    + alignment);
                        else if (stage == 0) {
                            currentPositions.add((baseSequencePosition > 0) ? baseSequencePosition
                                    : invertCoordinate(baseSequencePosition));
                            stage = 1;
                            extend(0, leftTailLength);
                        } else {
                            int currentCoordinate = (baseSequencePosition > 0) ? baseSequencePosition
                                    : invertCoordinate(baseSequencePosition);
                            int previousCoordinate = currentPositions.get(i - 1);
                            currentPositions.add(currentCoordinate);
                            if (currentCoordinate == previousCoordinate)
                                currentPartLength++;
                            else {
                                if (currentPartLength > 1) {
                                    if (currentCoordinate - previousCoordinate != 1)
                                        throw new IllegalStateException("Something is wrong with alignment: seq1: "
                                                + baseSequence + ", seq2: " + sequence + ", alignment: " + alignment
                                                + ", currentCoordinate: " + currentCoordinate + ", previousCoordinate:"
                                                + previousCoordinate + ", currentPartLength: " + currentPartLength);
                                    extend(currentCoordinate, currentPartLength);
                                }
                                currentPartLength = 1;
                            }
                        }
                    }
                }
                extend(baseLettersCoordinates.length - 1, rightTailLength);
                positionsCache.add(currentPositions);
            }

            void addEmpty() {
                sequences.add(NSequenceWithQuality.EMPTY);
                positionsCache.add(new ArrayList<>());
            }

            /**
             * Extend matrix to fit longer subsequence into it.
             *
             * @param arrayIndex 0 means that we put subsequence before 1st base letter,
             *                   (baseLettersCoordinates.length - 1) means that we put it after last letter
             * @param newLength length of subsequence that we put to the gap specified by arrayIndex
             */
            private void extend(int arrayIndex, int newLength) {
                int currentLength = (arrayIndex == 0) ? baseLettersCoordinates[0]
                        : baseLettersCoordinates[arrayIndex] - baseLettersCoordinates[arrayIndex - 1] - 1;
                if (newLength > currentLength) {
                    int diff = newLength - currentLength;
                    for (int i = arrayIndex; i < baseLettersCoordinates.length; i++)
                        baseLettersCoordinates[i] += diff;
                }
            }

            NSequenceWithQuality getLetterByCoordinate(int sequenceRealIndex, int coordinate) {
                if (sequenceRealIndex == baseSequenceRealIndex) {
                    for (int i = 0; i < baseLettersCoordinates.length - 1; i++) {
                        int currentCoordinate = baseLettersCoordinates[i];
                        if (currentCoordinate == coordinate)
                            return letterAt(sequences.get(0), i);
                        else if (currentCoordinate > coordinate)
                            return NSequenceWithQuality.EMPTY;
                    }
                    return NSequenceWithQuality.EMPTY;
                } else {
                    int sequenceIndex = (sequenceRealIndex > baseSequenceRealIndex) ? sequenceRealIndex
                            : sequenceRealIndex + 1;
                    NSequenceWithQuality sequence = sequences.get(sequenceIndex);
                    if (sequence.size() == 0)
                        return NSequenceWithQuality.EMPTY;
                    ArrayList<Integer> positions = positionsCache.get(sequenceIndex - 1);
                    int basePosition = -1;
                    int currentBasePosition = -1;
                    int currentPartLength = 1;
                    int seqPosition;
                    for (seqPosition = 0; seqPosition < sequence.size(); seqPosition++) {
                        currentBasePosition = positions.get(seqPosition);
                        if (currentBasePosition > -1) {
                            int currentBaseCoordinate = baseLettersCoordinates[currentBasePosition];
                            if (currentBaseCoordinate == coordinate)
                                return letterAt(sequence, seqPosition);
                            else if (currentBaseCoordinate > coordinate)
                                break;
                            else if (currentBasePosition == basePosition)
                                currentPartLength++;
                            else
                                currentPartLength = 1;
                        }
                        basePosition = currentBasePosition;
                    }
                    if (currentBasePosition == -1)
                        throw new IllegalStateException("LettersMatrix error in sequence: " + sequence
                                + "; sequenceIndex: " + sequenceIndex + ", coordinate: " + coordinate);
                    if (basePosition == -1) {
                        int seqStartCoordinate = baseLettersCoordinates[0] - seqPosition;
                        if (coordinate >= baseLettersCoordinates[0])
                            throw new IllegalStateException("Wrong base position found: sequence: " + sequence
                                    + ", sequenceIndex: " + sequenceIndex + ", coordinate: " + coordinate
                                    + ", basePosition: -1, baseLettersCoordinates[0]: " + baseLettersCoordinates[0]
                                    + ", seqPosition: " + seqPosition);
                        else if (coordinate < seqStartCoordinate)
                            return NSequenceWithQuality.EMPTY;
                        else
                            return letterAt(sequence, coordinate - seqStartCoordinate);
                    } else {
                        int currentPartStart = seqPosition - currentPartLength;
                        int wantedSeqPosition = currentPartStart + coordinate - baseLettersCoordinates[basePosition];
                        if (wantedSeqPosition >= seqPosition)
                            return NSequenceWithQuality.EMPTY;
                        else
                            return letterAt(sequence, wantedSeqPosition);
                    }
                }
            }

            private NSequenceWithQuality letterAt(NSequenceWithQuality seq, int position) {
                return new NSequenceWithQuality(new NucleotideSequence(new char[] { seq.getSequence()
                        .symbolAt(position) }), seq.getQuality().value(position));
            }
        }
    }
}
