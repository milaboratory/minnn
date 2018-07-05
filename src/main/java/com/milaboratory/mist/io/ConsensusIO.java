package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.alignment.LinearGapAlignmentScoring;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.outputconverter.MatchedGroup;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.mist.pattern.Match;
import com.milaboratory.mist.pattern.MatchedGroupEdge;
import com.milaboratory.util.SmartProgressReporter;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.core.alignment.BandedLinearAligner.alignLocalGlobal;
import static com.milaboratory.core.sequence.SequenceQuality.MAX_QUALITY_VALUE;
import static com.milaboratory.core.sequence.quality.QualityTrimmer.trim;
import static com.milaboratory.mist.cli.CliUtils.floatFormat;
import static com.milaboratory.mist.cli.Defaults.*;
import static com.milaboratory.mist.pattern.PatternUtils.invertCoordinate;
import static com.milaboratory.mist.util.SequencesCache.*;
import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class ConsensusIO {
    private final String inputFileName;
    private final String outputFileName;
    private final int alignerWidth;
    private final int matchScore;
    private final int mismatchScore;
    private final int gapScore;
    private final long scoreThreshold;
    private final float skippedFractionToRepeat;
    private final int maxConsensusesPerCluster;
    private final int readsMinGoodSeqLength;
    private final float readsAvgQualityThreshold;
    private final int readsTrimWindowSize;
    private final int minGoodSeqLength;
    private final float avgQualityThreshold;
    private final int trimWindowSize;
    private final long inputReadsLimit;
    private final int maxWarnings;
    private final int threads;
    private final PrintStream debugOutputStream;
    private final byte debugQualityThreshold;
    private final AtomicLong totalReads = new AtomicLong(0);
    private long consensusReads = 0;
    private int warningsDisplayed = 0;
    private Set<String> groupSet;
    private int numberOfTargets;

    public ConsensusIO(List<String> groupList, String inputFileName, String outputFileName, int alignerWidth,
                       int matchScore, int mismatchScore, int gapScore, long scoreThreshold,
                       float skippedFractionToRepeat, int maxConsensusesPerCluster, int readsMinGoodSeqLength,
                       float readsAvgQualityThreshold, int readsTrimWindowSize, int minGoodSeqLength,
                       float avgQualityThreshold, int trimWindowSize, long inputReadsLimit, int maxWarnings,
                       int threads, String debugOutputFileName, byte debugQualityThreshold) {
        this.groupSet = (groupList == null) ? null : new LinkedHashSet<>(groupList);
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.alignerWidth = alignerWidth;
        this.matchScore = matchScore;
        this.mismatchScore = mismatchScore;
        this.gapScore = gapScore;
        this.scoreThreshold = scoreThreshold;
        this.skippedFractionToRepeat = skippedFractionToRepeat;
        this.maxConsensusesPerCluster = maxConsensusesPerCluster;
        this.readsMinGoodSeqLength = readsMinGoodSeqLength;
        this.readsAvgQualityThreshold = readsAvgQualityThreshold;
        this.readsTrimWindowSize = readsTrimWindowSize;
        this.minGoodSeqLength = minGoodSeqLength;
        this.avgQualityThreshold = avgQualityThreshold;
        this.trimWindowSize = trimWindowSize;
        this.inputReadsLimit = inputReadsLimit;
        this.maxWarnings = maxWarnings;
        this.threads = threads;
        try {
            debugOutputStream = (debugOutputFileName == null) ? null
                    : new PrintStream(new FileOutputStream(debugOutputFileName));
        } catch (IOException e) {
            throw exitWithError(e.toString());
        }
        this.debugQualityThreshold = debugQualityThreshold;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        try (MifReader reader = createReader();
             MifWriter writer = createWriter(reader.getHeader())) {
            if (inputReadsLimit > 0)
                reader.setParsedReadsLimit(inputReadsLimit);
            SmartProgressReporter.startProgressReport("Calculating consensuses", reader, System.err);
            if (groupSet == null) {
                if (reader.getCorrectedGroups().size() == 0)
                    displayWarning("WARNING: calculating consensus for not corrected MIF file!");
            } else {
                List<String> notCorrectedGroups = groupSet.stream().filter(gn -> reader.getCorrectedGroups().stream()
                        .noneMatch(gn::equals)).collect(Collectors.toList());
                if (notCorrectedGroups.size() != 0)
                    displayWarning("WARNING: group(s) " + notCorrectedGroups + " not corrected, but used in " +
                            "consensus calculation!");
            }
            if (!reader.isSorted())
                displayWarning("WARNING: calculating consensus for not sorted MIF file; result will be wrong!");
            numberOfTargets = reader.getNumberOfReads();
            Set<String> defaultGroups = IntStream.rangeClosed(1, numberOfTargets)
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());

            OutputPort<Cluster> clusterOutputPort = new OutputPort<Cluster>() {
                LinkedHashMap<String, NucleotideSequence> previousGroups = null;
                Cluster currentCluster = new Cluster(0);
                int orderedPortIndex = 0;
                boolean finished = false;

                @Override
                public synchronized Cluster take() {
                    if (finished)
                        return null;
                    Cluster preparedCluster = null;
                    while (preparedCluster == null) {
                        ParsedRead parsedRead = reader.take();
                        if ((parsedRead != null) && ((inputReadsLimit == 0) || (totalReads.get() < inputReadsLimit))) {
                            Set<String> allGroups = parsedRead.getGroups().stream().map(MatchedGroup::getGroupName)
                                    .filter(groupName -> !defaultGroups.contains(groupName))
                                    .collect(Collectors.toSet());
                            if (groupSet != null) {
                                for (String groupName : groupSet)
                                    if (!allGroups.contains(groupName))
                                        throw exitWithError("Group " + groupName + " not found in the input!");
                            } else
                                groupSet = allGroups;
                            LinkedHashMap<String, NucleotideSequence> currentGroups = parsedRead.getGroups().stream()
                                    .filter(g -> groupSet.contains(g.getGroupName()))
                                    .collect(LinkedHashMap::new, (m, g) -> m.put(g.getGroupName(),
                                            g.getValue().getSequence()), Map::putAll);
                            if (!currentGroups.equals(previousGroups)) {
                                if (previousGroups != null) {
                                    preparedCluster = currentCluster;
                                    currentCluster = new Cluster(++orderedPortIndex);
                                }
                                previousGroups = currentGroups;
                            }
                            currentCluster.data.add(new DataFromParsedRead(parsedRead, defaultGroups));
                            totalReads.getAndIncrement();
                        } else {
                            finished = true;
                            if (previousGroups != null)
                                return currentCluster;
                            else
                                return null;
                        }
                    }
                    return preparedCluster;
                }
            };

            OutputPort<CalculatedConsensuses> calculatedConsensusesPort = new ParallelProcessor<>(clusterOutputPort,
                    new ClusterProcessor(), threads);
            OrderedOutputPort<CalculatedConsensuses> orderedConsensusesPort = new OrderedOutputPort<>(
                    calculatedConsensusesPort, cc -> cc.orderedPortIndex);
            int clusterIndex = 0;
            for (CalculatedConsensuses calculatedConsensuses : CUtils.it(orderedConsensusesPort)) {
                for (int i = 0; i < calculatedConsensuses.consensuses.size(); i++) {
                    Consensus consensus = calculatedConsensuses.consensuses.get(i);
                    if (consensus.isConsensus)
                        writer.write(consensus.toParsedRead(consensusReads++));
                    if (debugOutputStream != null)
                        consensus.debugData.writeDebugData(clusterIndex++, i);
                }
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads\n");
        System.err.println("Calculated " + consensusReads + " consensuses\n");
        if (consensusReads > 0)
            System.err.println("Average reads per consensus: " + floatFormat.format((float)totalReads.get()
                    / consensusReads) + "\n");
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private MifWriter createWriter(MifHeader mifHeader) throws IOException {
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), mifHeader)
                : new MifWriter(outputFileName, mifHeader);
    }

    private synchronized void displayWarning(String text) {
        if (maxWarnings == -1)
            System.err.println(text);
        else if ((maxWarnings > 0) && (warningsDisplayed < maxWarnings)) {
            System.err.println(text);
            warningsDisplayed++;
            if (warningsDisplayed == maxWarnings)
                System.err.println("Warnings limit reached!");
        }
    }

    private class Barcode {
        final String groupName;
        final NSequenceWithQuality value;

        Barcode(String groupName, NSequenceWithQuality value) {
            this.groupName = groupName;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Barcode{" + "groupName='" + groupName + '\'' + ", value=" + value + '}';
        }
    }

    private class TargetBarcodes {
        final ArrayList<Barcode> targetBarcodes;

        TargetBarcodes(ArrayList<Barcode> targetBarcodes) {
            this.targetBarcodes = targetBarcodes;
        }

        @Override
        public String toString() {
            return "TargetBarcodes{" + "targetBarcodes=" + targetBarcodes + '}';
        }
    }

    private class DataFromParsedRead {
        final NSequenceWithQuality[] sequences;
        final TargetBarcodes[] barcodes;

        DataFromParsedRead(ParsedRead parsedRead, Set<String> defaultGroups) {
            List<MatchedGroup> parsedReadGroups = parsedRead.getGroups();
            List<MatchedGroup> extractedGroups = parsedReadGroups.stream()
                    .filter(g -> defaultGroups.contains(g.getGroupName())).collect(Collectors.toList());
            if (extractedGroups.size() != numberOfTargets)
                throw new IllegalArgumentException("Wrong number of target groups in ParsedRead: expected "
                        + numberOfTargets + ", target groups in ParsedRead: " + parsedRead.getGroups().stream()
                        .map(MatchedGroup::getGroupName).filter(defaultGroups::contains).collect(Collectors.toList()));
            sequences = new NSequenceWithQuality[numberOfTargets];
            extractedGroups.forEach(g -> sequences[g.getTargetId() - 1] = g.getValue());
            barcodes = IntStream.range(0, numberOfTargets).mapToObj(i -> new TargetBarcodes(new ArrayList<>()))
                    .toArray(TargetBarcodes[]::new);
            parsedReadGroups.stream().filter(g -> groupSet.contains(g.getGroupName())).forEachOrdered(group -> {
                int targetIndex = group.getTargetId() - 1;
                ArrayList<Barcode> currentTargetList = barcodes[targetIndex].targetBarcodes;
                currentTargetList.add(new Barcode(group.getGroupName(), group.getValue()));
            });
        }

        DataFromParsedRead(NSequenceWithQuality[] sequences, TargetBarcodes[] barcodes) {
            this.sequences = sequences;
            this.barcodes = barcodes;
        }
    }

    private class Consensus {
        final NSequenceWithQuality[] sequences;
        final TargetBarcodes[] barcodes;
        final int consensusReadsNum;
        final ConsensusDebugData debugData;
        final boolean isConsensus;

        Consensus(NSequenceWithQuality[] sequences, TargetBarcodes[] barcodes, int consensusReadsNum,
                  ConsensusDebugData debugData) {
            this.sequences = sequences;
            this.barcodes = barcodes;
            this.consensusReadsNum = consensusReadsNum;
            this.debugData = debugData;
            this.isConsensus = true;
        }

        Consensus(ConsensusDebugData debugData) {
            this.sequences = null;
            this.barcodes = null;
            this.consensusReadsNum = 0;
            this.debugData = debugData;
            this.isConsensus = false;
        }

        ParsedRead toParsedRead(long readId) {
            if (!isConsensus || (sequences == null) || (barcodes == null))
                throw exitWithError("toParsedRead() called for null consensus!");
            SequenceRead originalRead;
            SingleRead[] reads = new SingleRead[numberOfTargets];
            ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
            for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
                NSequenceWithQuality currentSequence = sequences[targetId - 1];
                TargetBarcodes targetBarcodes = barcodes[targetId - 1];
                reads[targetId - 1] = new SingleReadImpl(readId, currentSequence, "Consensus");
                matchedGroupEdges.add(new MatchedGroupEdge(currentSequence, targetId,
                        new GroupEdge("R" + targetId, true), 0));
                matchedGroupEdges.add(new MatchedGroupEdge(null, targetId,
                        new GroupEdge("R" + targetId, false), currentSequence.size()));
                for (Barcode barcode : targetBarcodes.targetBarcodes) {
                    matchedGroupEdges.add(new MatchedGroupEdge(currentSequence, targetId,
                            new GroupEdge(barcode.groupName, true), barcode.value));
                    matchedGroupEdges.add(new MatchedGroupEdge(null, targetId,
                            new GroupEdge(barcode.groupName, false), null));
                }
            }
            if (numberOfTargets == 1)
                originalRead = reads[0];
            else if (numberOfTargets == 2)
                originalRead = new PairedRead(reads);
            else
                originalRead = new MultiRead(reads);

            Match bestMatch = new Match(numberOfTargets, 0, matchedGroupEdges);
            return new ParsedRead(originalRead, false, bestMatch, consensusReadsNum);
        }
    }

    private class Cluster {
        final ArrayList<DataFromParsedRead> data = new ArrayList<>();
        final long orderedPortIndex;

        Cluster(long orderedPortIndex) {
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

    private class ConsensusDebugData {
        // outer list - targetIndex, second - sequenceIndex, inner - positionIndex
        ArrayList<ArrayList<ArrayList<NSequenceWithQuality>>> data = new ArrayList<>();

        void writeDebugData(int clusterIndex, int consensusIndex) {
            debugOutputStream.println("\nclusterIndex: " + clusterIndex + ", consensusIndex: " + consensusIndex);
            for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                debugOutputStream.println("targetIndex: " + targetIndex);
                ArrayList<ArrayList<NSequenceWithQuality>> targetData = data.get(targetIndex);
                for (ArrayList<NSequenceWithQuality> sequenceData : targetData) {
                    StringBuilder sequenceString = new StringBuilder();
                    for (NSequenceWithQuality currentLetter : sequenceData) {
                        if (currentLetter == null)
                            sequenceString.append(".");
                        else if (currentLetter == NSequenceWithQuality.EMPTY)
                            sequenceString.append("-");
                        else {
                            if (currentLetter.getQuality().value(0) < debugQualityThreshold)
                                sequenceString.append(Character.toLowerCase(currentLetter.getSequence()
                                        .symbolAt(0)));
                            else
                                sequenceString.append(Character.toUpperCase(currentLetter.getSequence()
                                        .symbolAt(0)));
                        }
                    }
                    debugOutputStream.println(sequenceString.toString());
                }
            }
        }
    }

    private class ClusterProcessor implements Processor<Cluster, CalculatedConsensuses> {
        private final LinearGapAlignmentScoring<NucleotideSequence> scoring = new LinearGapAlignmentScoring<>(
                NucleotideSequence.ALPHABET, matchScore, mismatchScore, gapScore);

        @Override
        public CalculatedConsensuses process(Cluster cluster) {
            CalculatedConsensuses calculatedConsensuses = new CalculatedConsensuses(cluster.orderedPortIndex);
            List<DataFromParsedRead> data = cluster.data;

            while (data.size() > 0) {
                // stage 1: align to best quality
                long bestSumQuality = 0;
                int bestDataIndex = 0;
                for (int i = 0; i < data.size(); i++) {
                    long sumQuality = Arrays.stream(data.get(i).sequences).mapToLong(this::calculateSumQuality).sum();
                    if (sumQuality > bestSumQuality) {
                        bestSumQuality = sumQuality;
                        bestDataIndex = i;
                    }
                }
                DataFromParsedRead bestData = data.get(bestDataIndex);
                HashSet<Integer> filteredOutReads = new HashSet<>();
                ArrayList<AlignedSubsequences> subsequencesList = getAlignedSubsequencesList(data, filteredOutReads,
                        bestData.sequences, bestDataIndex);
                Consensus stage1Consensus = generateConsensus(subsequencesList, bestData.sequences, bestData.barcodes);

                if (!stage1Consensus.isConsensus) {
                    displayWarning("WARNING: consensus assembled from " + (data.size() - filteredOutReads.size())
                            + " reads discarded on stage 1 after quality trimming! Barcode values: "
                            + formatBarcodeValues(bestData.barcodes));
                    if (debugOutputStream != null)
                        calculatedConsensuses.consensuses.add(stage1Consensus);
                } else {
                    // stage 2: align to consensus from stage 1
                    subsequencesList = getAlignedSubsequencesList(trimBadQualityTails(data), filteredOutReads,
                            stage1Consensus.sequences, -1);
                    if (subsequencesList.size() > 0) {
                        Consensus stage2Consensus = generateConsensus(subsequencesList, stage1Consensus.sequences,
                                stage1Consensus.barcodes);
                        if (!stage2Consensus.isConsensus) {
                            displayWarning("WARNING: consensus assembled from " + (data.size()
                                    - filteredOutReads.size()) + " reads discarded on stage 2 after "
                                    + "quality trimming! Barcode values: " + formatBarcodeValues(bestData.barcodes));
                            if (debugOutputStream != null)
                                calculatedConsensuses.consensuses.add(stage2Consensus);
                        } else
                            calculatedConsensuses.consensuses.add(stage2Consensus);
                    }
                }

                if ((filteredOutReads.size() < data.size())
                        && (float)filteredOutReads.size() / cluster.data.size() >= skippedFractionToRepeat) {
                    if (calculatedConsensuses.consensuses.size() < maxConsensusesPerCluster) {
                        ArrayList<DataFromParsedRead> remainingData = new ArrayList<>();
                        for (int i = 0; i < data.size(); i++)
                            if (filteredOutReads.contains(i))
                                remainingData.add(data.get(i));
                        data = remainingData;
                    } else {
                        displayWarning("WARNING: max consensuses per cluster exceeded; not processed "
                                + filteredOutReads.size() + " reads from cluster of " + cluster.data.size()
                                + " reads! Barcode values: " + formatBarcodeValues(bestData.barcodes));
                        data = new ArrayList<>();
                    }
                } else
                    data = new ArrayList<>();
            }

            return calculatedConsensuses;
        }

        private String formatBarcodeValues(TargetBarcodes[] targetBarcodes) {
            ArrayList<Barcode> barcodes = new ArrayList<>();
            Arrays.stream(targetBarcodes).forEach(tb -> barcodes.addAll(tb.targetBarcodes));
            barcodes.sort(Comparator.comparing(b -> b.groupName));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < barcodes.size(); i++) {
                Barcode barcode = barcodes.get(i);
                if (i > 0)
                    builder.append(", ");
                builder.append(barcode.groupName).append('=').append(barcode.value.getSequence().toString());
            }
            return builder.toString();
        }

        private long calculateSumQuality(NSequenceWithQuality seq) {
            if ((seq == null) || (seq == NSequenceWithQuality.EMPTY))
                return 0;
            long sum = 0;
            for (byte quality : seq.getQuality().asArray())
                sum += quality;
            return sum;
        }

        private byte calculateMinQuality(NSequenceWithQuality seq) {
            if ((seq == null) || (seq == NSequenceWithQuality.EMPTY))
                return 0;
            byte minQuality = DEFAULT_GOOD_QUALITY;
            for (byte quality : seq.getQuality().asArray())
                if (quality < minQuality)
                    minQuality = quality;
            return minQuality;
        }

        private NSequenceWithQuality getCachedValues(SequenceWithQuality<NucleotideSequence> input) {
            NucleotideSequence sequence = sequencesCache.get(input.getSequence());
            return new NSequenceWithQuality((sequence == null) ? input.getSequence() : sequence,
                    (input.size() == 1) ? qualityCache.get(input.getQuality().value(0)) : input.getQuality());
        }

        private NSequenceWithQuality letterAt(NSequenceWithQuality seq, int position) {
            if ((seq == null) || (position < 0) || (position >= seq.size()))
                return null;
            return getCachedValues(seq.getSubSequence(position, position + 1));
        }

        private NSequenceWithQuality getSubSequence(NSequenceWithQuality seq, int from, int to) {
            if ((from < 0) || (to > seq.size()) || (to - from < 1))
                throw new IndexOutOfBoundsException("seq.size(): " + seq.size() + ", from: " + from + ", to: " + to);
            return getCachedValues(seq.getSubSequence(from, to));
        }

        /**
         * Trim bad quality tails and filter out entirely bad sequences from data.
         *
         * @param data  data from cluster of parsed reads with same barcodes
         * @return      trimmed and filtered data
         */
        private List<DataFromParsedRead> trimBadQualityTails(List<DataFromParsedRead> data) {
            List<DataFromParsedRead> processedData = new ArrayList<>();
            for (DataFromParsedRead dataFromParsedRead : data) {
                NSequenceWithQuality[] sequences = dataFromParsedRead.sequences;
                NSequenceWithQuality[] processedSequences = new NSequenceWithQuality[numberOfTargets];
                TargetBarcodes[] barcodes = dataFromParsedRead.barcodes;
                TargetBarcodes[] processedBarcodes = IntStream.range(0, numberOfTargets)
                        .mapToObj(i -> new TargetBarcodes(new ArrayList<>())).toArray(TargetBarcodes[]::new);
                boolean allSequencesAreGood = true;
                for (int i = 0; i < numberOfTargets; i++) {
                    NSequenceWithQuality sequence = sequences[i];
                    TargetBarcodes targetBarcodes = barcodes[i];
                    int trimResultLeft = trim(sequence.getQuality(), 0, sequence.size(), 1,
                            true, readsAvgQualityThreshold, readsTrimWindowSize);
                    if (trimResultLeft < -1) {
                        allSequencesAreGood = false;
                        break;
                    }
                    int trimResultRight = trim(sequence.getQuality(), 0, sequence.size(), -1,
                            true, readsAvgQualityThreshold, readsTrimWindowSize);
                    if (trimResultRight < 0)
                        throw new IllegalStateException("Unexpected negative trimming result");
                    else if (trimResultRight - trimResultLeft - 1 < readsMinGoodSeqLength) {
                        allSequencesAreGood = false;
                        break;
                    } else {
                        processedSequences[i] = getSubSequence(sequence, trimResultLeft + 1, trimResultRight);
                        processedBarcodes[i].targetBarcodes.addAll(targetBarcodes.targetBarcodes);
                    }
                }

                if (allSequencesAreGood)
                    processedData.add(new DataFromParsedRead(processedSequences, processedBarcodes));
                else
                    processedData.add(null);
            }

            return processedData;
        }

        /**
         * Align sequences and generate list of AlignedSubsequences objects that is needed for generateConsensus().
         *
         * @param data              data from cluster of parsed reads with same barcodes
         * @param filteredOutReads  mutable set of filtered out reads: this function will add to this set
         *                          indexes of all reads that didn't fit score threshold
         * @param bestSequences     best array of sequences: 1 sequence in array corresponding to 1 target
         * @param bestSeqIndex      index of best sequences in cluster; or -1 if they are not from cluster
         * @return                  list of aligned subsequences for generateConsensus() function
         */
        private ArrayList<AlignedSubsequences> getAlignedSubsequencesList(List<DataFromParsedRead> data,
                HashSet<Integer> filteredOutReads, NSequenceWithQuality[] bestSequences, int bestSeqIndex) {
            ArrayList<AlignedSubsequences> subsequencesList = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                if (i != bestSeqIndex) {
                    if (!filteredOutReads.contains(i) && (data.get(i) != null)) {
                        int sumScore = 0;
                        ArrayList<Alignment<NucleotideSequence>> alignments = new ArrayList<>();
                        for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                            NSequenceWithQuality currentSequence = data.get(i).sequences[targetIndex];
                            Alignment<NucleotideSequence> alignment = alignLocalGlobal(scoring,
                                    bestSequences[targetIndex], currentSequence, alignerWidth);
                            alignments.add(alignment);
                            sumScore += alignment.getScore();
                        }
                        if (sumScore < scoreThreshold)
                            filteredOutReads.add(i);
                        else {
                            AlignedSubsequences currentSubsequences = new AlignedSubsequences(bestSequences);
                            for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                                NSequenceWithQuality currentSequence = data.get(i).sequences[targetIndex];
                                NSequenceWithQuality alignedBestSequence = bestSequences[targetIndex];
                                int previousSeqPosition = -1;
                                for (int position = 0; position < alignedBestSequence.size(); position++) {
                                    Alignment<NucleotideSequence> alignment = alignments.get(targetIndex);
                                    int seqPosition = alignment.convertToSeq2Position(position);
                                    if (previousSeqPosition < 0) {
                                        if (seqPosition < 0)
                                            currentSubsequences.set(targetIndex, position, null);
                                        else
                                            currentSubsequences.set(targetIndex, position, getSubSequence(
                                                    currentSequence, 0, seqPosition + 1));
                                        previousSeqPosition = seqPosition;
                                    } else {
                                        if (seqPosition < 0)
                                            currentSubsequences.set(targetIndex, position, null);
                                        else {
                                            if (previousSeqPosition == currentSequence.size() - 1)
                                                currentSubsequences.set(targetIndex, position, null);
                                            else
                                                currentSubsequences.set(targetIndex, position,
                                                        getSubSequence(currentSequence, previousSeqPosition + 1,
                                                                Math.min(currentSequence.size(), seqPosition + 1)));
                                            previousSeqPosition = seqPosition;
                                        }
                                    }
                                }
                            }
                            subsequencesList.add(currentSubsequences);
                        }
                    }
                } else {
                    AlignedSubsequences currentSubsequences = new AlignedSubsequences(bestSequences);
                    for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                        NSequenceWithQuality currentSequence = bestSequences[targetIndex];
                        for (int position = 0; position < currentSequence.size(); position++)
                            currentSubsequences.set(targetIndex, position, letterAt(currentSequence, position));
                    }
                    subsequencesList.add(currentSubsequences);
                }
            }

            return subsequencesList;
        }

        /**
         * Generate consensus from prepared aligned subsequences list.
         *
         * @param subsequencesList  1 element of this list corresponding to 1 read; AlignedSubsequences structure
         *                          contains sequences from cluster splitted by coordinates that came from alignment
         *                          of sequences from this array to sequences from best array
         * @param bestSequences     best array of sequences: 1 sequence in array corresponding to 1 target
         * @param barcodes          barcodes from best sequences
         * @return                  consensus: array of sequences (1 sequence for 1 target) and consensus score
         */
        private Consensus generateConsensus(ArrayList<AlignedSubsequences> subsequencesList,
                                            NSequenceWithQuality[] bestSequences, TargetBarcodes[] barcodes) {
            ConsensusDebugData debugData = (debugOutputStream == null) ? null : new ConsensusDebugData();
            int consensusReadsNum = subsequencesList.size();
            NSequenceWithQuality[] sequences = new NSequenceWithQuality[numberOfTargets];
            List<LettersWithPositions> lettersList = IntStream.range(0, consensusReadsNum)
                    .mapToObj(i -> new LettersWithPositions()).collect(Collectors.toList());
            TargetBarcodes[] consensusBarcodes = IntStream.range(0, numberOfTargets)
                    .mapToObj(i -> new TargetBarcodes(new ArrayList<>())).toArray(TargetBarcodes[]::new);
            for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                ArrayList<ArrayList<NSequenceWithQuality>> debugDataForThisTarget;
                if (debugData == null)
                    debugDataForThisTarget = null;
                else {
                    debugDataForThisTarget = new ArrayList<>();
                    debugData.data.add(debugDataForThisTarget);
                }
                consensusBarcodes[targetIndex].targetBarcodes.addAll(barcodes[targetIndex].targetBarcodes);
                List<ArrayList<NSequenceWithQuality>> lettersMatrixList = IntStream.range(0, consensusReadsNum)
                        .mapToObj(i -> new ArrayList<NSequenceWithQuality>()).collect(Collectors.toList());
                for (int position = 0; position < bestSequences[targetIndex].size(); position++) {
                    ArrayList<NSequenceWithQuality> currentPositionSequences = new ArrayList<>();
                    int bestQualityIndex = -1;
                    byte bestQuality = -1;
                    for (int i = 0; i < consensusReadsNum; i++) {
                        AlignedSubsequences currentSubsequences = subsequencesList.get(i);
                        NSequenceWithQuality currentSequence = currentSubsequences.get(targetIndex, position);
                        currentPositionSequences.add(currentSequence);
                        if (currentSequence != null) {
                            byte currentQuality = calculateMinQuality(currentSequence);
                            if (currentQuality > bestQuality) {
                                bestQuality = currentQuality;
                                bestQualityIndex = i;
                            }
                        }
                    }

                    LettersMatrix lettersMatrix;
                    if (bestQualityIndex == -1) {
                        // in this case lettersMatrix is a column of nulls and getRowLength() will return 0
                        lettersMatrix = new LettersMatrix(null, 0);
                    } else {
                        lettersMatrix = new LettersMatrix(currentPositionSequences.get(bestQualityIndex),
                                bestQualityIndex);
                        for (int i = 0; i < currentPositionSequences.size(); i++) {
                            if (i != bestQualityIndex) {
                                NSequenceWithQuality currentSeq = currentPositionSequences.get(i);
                                if (currentSeq != null)
                                    lettersMatrix.add(currentSeq, alignLocalGlobal(scoring,
                                            currentPositionSequences.get(bestQualityIndex),
                                            currentPositionSequences.get(i), alignerWidth));
                                else
                                    lettersMatrix.addNull();
                            }
                        }
                    }
                    for (int sequenceIndex = 0; sequenceIndex < consensusReadsNum; sequenceIndex++) {
                        ArrayList<NSequenceWithQuality> currentLettersRow = lettersMatrixList.get(sequenceIndex);
                        for (int letterIndex = 0; letterIndex < lettersMatrix.getRowLength(); letterIndex++)
                            addLetterToRow(currentLettersRow,
                                    lettersMatrix.getLetterByCoordinate(sequenceIndex, letterIndex));
                    }
                }

                // moving letters from lists to LettersWithPositions objects
                for (int sequenceIndex = 0; sequenceIndex < consensusReadsNum; sequenceIndex++) {
                    ArrayList<NSequenceWithQuality> currentLettersRow = lettersMatrixList.get(sequenceIndex);
                    LettersWithPositions currentLettersWithPositions = lettersList.get(sequenceIndex);
                    currentLettersWithPositions.set(targetIndex, currentLettersRow);
                    if (debugDataForThisTarget != null)
                        debugDataForThisTarget.add(currentLettersRow);
                }

                ArrayList<NSequenceWithQuality> consensusLetters = new ArrayList<>();
                int fullRowLength = lettersMatrixList.get(0).size();
                for (int position = 0; position < fullRowLength; position++) {
                    // calculating quality sums for letters and deletions
                    HashMap<NucleotideSequence, Long> currentPositionQualitySums = new HashMap<>();
                    for (LettersWithPositions currentLettersWithPositions : lettersList) {
                        NSequenceWithQuality currentLetter = currentLettersWithPositions.get(targetIndex, position);
                        if (currentLetter == NSequenceWithQuality.EMPTY) {
                            currentPositionQualitySums.putIfAbsent(NucleotideSequence.EMPTY, 0L);
                            currentPositionQualitySums.put(NucleotideSequence.EMPTY,
                                    currentPositionQualitySums.get(NucleotideSequence.EMPTY)
                                            + currentLettersWithPositions.getDeletionQuality(targetIndex, position));
                        } else if (currentLetter != null) {
                            NucleotideSequence letterWithoutQuality = currentLetter.getSequence();
                            if (letterWithoutQuality.containsWildcards()) {
                                Wildcard wildcard = wildcards.get(letterWithoutQuality);
                                for (int i = 0; i < wildcard.basicSize(); i++) {
                                    NucleotideSequence currentBasicLetter = wildcardCodeToSequence
                                            .get(wildcard.getMatchingCode(i));
                                    currentPositionQualitySums.putIfAbsent(currentBasicLetter, 0L);
                                    currentPositionQualitySums.put(currentBasicLetter,
                                            currentPositionQualitySums.get(currentBasicLetter)
                                                    + currentLetter.getQuality().value(0)
                                                    / wildcard.basicSize());
                                }
                            } else {
                                currentPositionQualitySums.putIfAbsent(letterWithoutQuality, 0L);
                                currentPositionQualitySums.put(letterWithoutQuality,
                                        currentPositionQualitySums.get(letterWithoutQuality)
                                                + currentLetter.getQuality().value(0));
                            }
                        }
                    }

                    // choosing consensus letter and calculating consensus letter quality
                    long bestSum = 0;
                    long totalSum = 0;
                    NucleotideSequence consensusLetter = NucleotideSequence.EMPTY;
                    for (HashMap.Entry<NucleotideSequence, Long> entry : currentPositionQualitySums.entrySet()) {
                        totalSum += entry.getValue();
                        if (entry.getValue() > bestSum) {
                            bestSum = entry.getValue();
                            consensusLetter = entry.getKey();
                        }
                    }
                    if (consensusLetter != NucleotideSequence.EMPTY) {
                        float p = 1 - (float)bestSum / totalSum;
                        long phredQuality = (bestSum == totalSum) ? bestSum
                                : Math.min((long)(-10 * Math.log10(p)), bestSum);
                        consensusLetters.add(new NSequenceWithQuality(consensusLetter,
                                qualityCache.get((byte)Math.min(MAX_QUALITY_VALUE, phredQuality))));
                    }
                }

                // consensus sequence assembling and quality trimming
                NSequenceWithQuality consensusSequence = NSequenceWithQuality.EMPTY;
                for (NSequenceWithQuality consensusLetter : consensusLetters)
                    consensusSequence = consensusSequence.concatenate(consensusLetter);

                int trimResultLeft = trim(consensusSequence.getQuality(), 0, consensusSequence.size(),
                        1, true, avgQualityThreshold, trimWindowSize);
                if (trimResultLeft < -1)
                    return new Consensus(debugData);
                int trimResultRight = trim(consensusSequence.getQuality(), 0, consensusSequence.size(),
                        -1, true, avgQualityThreshold, trimWindowSize);
                if (trimResultRight < 0)
                    throw new IllegalStateException("Unexpected negative trimming result");
                else if (trimResultRight - trimResultLeft - 1 < minGoodSeqLength)
                    return new Consensus(debugData);
                consensusSequence = getSubSequence(consensusSequence, trimResultLeft + 1, trimResultRight);
                sequences[targetIndex] = consensusSequence;
            }

            return new Consensus(sequences, consensusBarcodes, consensusReadsNum, debugData);
        }

        /**
         * Add new letter to the end of letters row with properly handling of trailing nulls: replace them with
         * deletions because there must be no nulls in the middle of sequence.
         *
         * @param currentLettersRow     current row of letters that will be modified
         * @param newLetter             new letter (can be null or deletion) that is inserted to the end of row
         */
        private void addLetterToRow(ArrayList<NSequenceWithQuality> currentLettersRow, NSequenceWithQuality newLetter) {
            if (newLetter != null) {
                int firstNullPosition = -1;
                for (int currentPosition = currentLettersRow.size() - 1; currentPosition >= 0; currentPosition--) {
                    if (currentLettersRow.get(currentPosition) == null)
                        firstNullPosition = currentPosition;
                    else
                        break;
                }
                if (firstNullPosition > 0)
                    for (int currentPosition = firstNullPosition; currentPosition < currentLettersRow.size();
                         currentPosition++)
                        currentLettersRow.set(currentPosition, NSequenceWithQuality.EMPTY);
            }
            currentLettersRow.add(newLetter);
        }

        private class AlignedSubsequences {
            private final int[] indexes = new int[numberOfTargets];
            private final NSequenceWithQuality[] sequences;

            AlignedSubsequences(NSequenceWithQuality[] bestSequences) {
                int currentIndex = 0;
                for (int i = 0; i < numberOfTargets; i++) {
                    indexes[i] = currentIndex;
                    currentIndex += bestSequences[i].size();
                }
                sequences = new NSequenceWithQuality[currentIndex];
            }

            void set(int targetIndex, int position, NSequenceWithQuality value) {
                sequences[index(targetIndex, position)] = value;
            }

            NSequenceWithQuality get(int targetIndex, int position) {
                return sequences[index(targetIndex, position)];
            }

            private int index(int targetIndex, int position) {
                return indexes[targetIndex] + position;
            }
        }

        private class LettersWithPositions {
            private HashMap<Integer, ArrayList<NSequenceWithQuality>> targetSequences = new HashMap<>();

            void set(int targetIndex, ArrayList<NSequenceWithQuality> values) {
                for (NSequenceWithQuality value : values)
                    if ((value != null) && (value != NSequenceWithQuality.EMPTY) && (value.size() != 1))
                        throw new IllegalArgumentException("Trying to write sequence " + value
                                + " to LettersWithPositions");
                if (targetSequences.containsKey(targetIndex))
                    throw new IllegalStateException("Trying to write key " + targetIndex + " to targetSequences "
                            + "when it already exists: targetSequences=" + targetSequences);
                targetSequences.put(targetIndex, values);
            }

            NSequenceWithQuality get(int targetIndex, int position) {
                return targetSequences.get(targetIndex).get(position);
            }

            byte getDeletionQuality(int targetIndex, int position) {
                if (get(targetIndex, position) != NSequenceWithQuality.EMPTY)
                    throw new IllegalArgumentException("getDeletionQuality() called for sequence "
                            + get(targetIndex, position));
                ArrayList<NSequenceWithQuality> currentLetters = targetSequences.get(targetIndex);

                NSequenceWithQuality foundPreviousSeq = null;
                NSequenceWithQuality foundNextSeq = null;
                int currentPreviousIndex = position - 1;
                int currentNextIndex = position + 1;
                while (currentPreviousIndex >= 0) {
                    NSequenceWithQuality currentSeq = currentLetters.get(currentPreviousIndex);
                    if (currentSeq == null)
                        break;
                    if (currentSeq != NSequenceWithQuality.EMPTY) {
                        foundPreviousSeq = currentSeq;
                        break;
                    }
                    currentPreviousIndex--;
                }
                while (currentNextIndex < currentLetters.size()) {
                    NSequenceWithQuality currentSeq = currentLetters.get(currentNextIndex);
                    if (currentSeq == null)
                        break;
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
                    throw new IllegalStateException("Found empty sequence with targetIndex " + targetIndex + ": "
                            + currentLetters);
            }
        }

        private class LettersMatrix {
            // column numbers in the matrix corresponding to base sequence letters; last value is row length
            private final int[] baseLettersCoordinates;
            private final int baseSequenceRealIndex;
            private final ArrayList<ArrayList<Integer>> positionsCache = new ArrayList<>();
            private final ArrayList<NSequenceWithQuality> sequences = new ArrayList<>();
            private final boolean nullBaseSequence;

            LettersMatrix(NSequenceWithQuality baseSequence, int baseSequenceRealIndex) {
                if (baseSequence == null) {
                    baseLettersCoordinates = null;
                    nullBaseSequence = true;
                } else {
                    baseLettersCoordinates = IntStream.rangeClosed(0, baseSequence.size()).toArray();
                    nullBaseSequence = false;
                }
                sequences.add(baseSequence);
                this.baseSequenceRealIndex = baseSequenceRealIndex;
            }

            int getRowLength() {
                return nullBaseSequence ? 0 : baseLettersCoordinates[baseLettersCoordinates.length - 1];
            }

            void add(NSequenceWithQuality sequence, Alignment<NucleotideSequence> alignment) {
                if (nullBaseSequence)
                    throw new IllegalStateException("add(" + sequence + ", " + alignment + " called for LettersMatrix "
                            + "with null base sequence!");
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
                            currentPositions.add((baseSequencePosition >= 0) ? baseSequencePosition
                                    : invertCoordinate(baseSequencePosition));
                            stage = 1;
                            extend(0, leftTailLength);
                        } else {
                            int currentCoordinate = (baseSequencePosition >= 0) ? baseSequencePosition
                                    : invertCoordinate(baseSequencePosition);
                            int previousCoordinate = currentPositions.get(i - 1);
                            currentPositions.add(currentCoordinate);
                            if (currentCoordinate == previousCoordinate)
                                currentPartLength++;
                            else {
                                if (currentPartLength > 1)
                                    extend(currentCoordinate, currentPartLength);
                                currentPartLength = Math.max(1, currentCoordinate - previousCoordinate);
                            }
                        }
                    }
                }
                extend(baseLettersCoordinates.length - 1, rightTailLength);
                positionsCache.add(currentPositions);
            }

            void addNull() {
                sequences.add(null);
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
                if (nullBaseSequence)
                    throw new IllegalStateException("getLetterByCoordinate(" + sequenceRealIndex + ", " + coordinate
                            + " called for LettersMatrix with null base sequence!");
                if (sequenceRealIndex == baseSequenceRealIndex) {
                    for (int i = 0; i < baseLettersCoordinates.length - 1; i++) {
                        int currentCoordinate = baseLettersCoordinates[i];
                        if (currentCoordinate == coordinate)
                            return letterAt(sequences.get(0), i);
                        else if (currentCoordinate > coordinate)
                            return NSequenceWithQuality.EMPTY;
                    }
                    return null;
                } else {
                    int sequenceIndex = (sequenceRealIndex > baseSequenceRealIndex) ? sequenceRealIndex
                            : sequenceRealIndex + 1;
                    NSequenceWithQuality sequence = sequences.get(sequenceIndex);
                    if (sequence == null)
                        return null;
                    /* get positions in current sequence relative to base sequence;
                       sequenceIndex - 1 because there is no base sequence as 1st element */
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
                            else if (currentBaseCoordinate > coordinate) {
                                if (seqPosition == 0)
                                    return null;
                                break;
                            } else if (currentBasePosition == basePosition)
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
                        if (coordinate >= baseLettersCoordinates[0]) {
                            // there are deletions on base sequence start positions, and we pick one of them
                            return NSequenceWithQuality.EMPTY;
                        } else if (coordinate < seqStartCoordinate)
                            return null;
                        else
                            return letterAt(sequence, coordinate - seqStartCoordinate);
                    } else {
                        int currentPartStart = seqPosition - currentPartLength;
                        int wantedSeqPosition = currentPartStart + coordinate - baseLettersCoordinates[basePosition];
                        if (wantedSeqPosition >= seqPosition) {
                            /* if nucleotide not found and this is not last position in sequence, this is a deletion,
                               otherwise we are on the right from all sequence and return null */
                            if (seqPosition < sequence.size())
                                return NSequenceWithQuality.EMPTY;
                            else
                                return null;
                        } else
                            return letterAt(sequence, wantedSeqPosition);
                    }
                }
            }
        }
    }
}
