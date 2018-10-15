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

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.alignment.LinearGapAlignmentScoring;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.outputconverter.MatchedGroup;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.minnn.pattern.Match;
import com.milaboratory.minnn.pattern.MatchedGroupEdge;
import com.milaboratory.util.SmartProgressReporter;
import gnu.trove.map.hash.TLongLongHashMap;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.core.alignment.BandedLinearAligner.alignLocalGlobal;
import static com.milaboratory.core.sequence.quality.QualityTrimmer.trim;
import static com.milaboratory.minnn.cli.CliUtils.floatFormat;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.io.ConsensusIO.OriginalReadStatus.*;
import static com.milaboratory.minnn.pattern.PatternUtils.invertCoordinate;
import static com.milaboratory.minnn.util.SequencesCache.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class ConsensusIO {
    private static final double OVERFLOW_PROTECTION_MIN = 1E-100D;
    private static final double OVERFLOW_PROTECTION_MAX = 1E100D;
    private static final NucleotideSequence[] consensusMajorBases = new NucleotideSequence[] {
            sequencesCache.get(new NucleotideSequence("A")), sequencesCache.get(new NucleotideSequence("T")),
            sequencesCache.get(new NucleotideSequence("G")), sequencesCache.get(new NucleotideSequence("C")) };
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
    private final String originalReadStatsFileName;
    private final boolean toSeparateGroups;
    private final long inputReadsLimit;
    private final int maxWarnings;
    private final int threads;
    private final PrintStream debugOutputStream;
    private final byte debugQualityThreshold;
    private final AtomicLong totalReads = new AtomicLong(0);
    private final ConcurrentHashMap<Long, OriginalReadData> originalReadsData;
    private final TLongLongHashMap consensusFinalIds;
    private final AtomicLong consensusCurrentTempId = new AtomicLong(0);
    private long consensusReads = 0;
    private int warningsDisplayed = 0;
    private LinkedHashSet<String> defaultGroups;
    private Set<String> groupSet;
    private int numberOfTargets;

    public ConsensusIO(List<String> groupList, String inputFileName, String outputFileName, int alignerWidth,
                       int matchScore, int mismatchScore, int gapScore, long scoreThreshold,
                       float skippedFractionToRepeat, int maxConsensusesPerCluster, int readsMinGoodSeqLength,
                       float readsAvgQualityThreshold, int readsTrimWindowSize, int minGoodSeqLength,
                       float avgQualityThreshold, int trimWindowSize, String originalReadStatsFileName,
                       boolean toSeparateGroups, long inputReadsLimit, int maxWarnings, int threads,
                       String debugOutputFileName, byte debugQualityThreshold) {
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
        this.toSeparateGroups = toSeparateGroups;
        this.minGoodSeqLength = minGoodSeqLength;
        this.avgQualityThreshold = avgQualityThreshold;
        this.trimWindowSize = trimWindowSize;
        this.originalReadStatsFileName = originalReadStatsFileName;
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
        this.originalReadsData = (originalReadStatsFileName == null) ? null : new ConcurrentHashMap<>();
        this.consensusFinalIds = (originalReadStatsFileName == null) ? null : new TLongLongHashMap();
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long originalNumberOfReads;
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
                        ParsedRead parsedRead = ((inputReadsLimit == 0) || (totalReads.get() < inputReadsLimit))
                                ? reader.take() : null;
                        if (parsedRead != null) {
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
                            currentCluster.data.add(new DataFromParsedRead(parsedRead));
                            if (originalReadsData != null)
                                originalReadsData.put(parsedRead.getOriginalRead().getId(), new OriginalReadData());
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
                    if (consensus.isConsensus) {
                        if (consensusFinalIds != null)
                            consensusFinalIds.put(consensus.tempId, consensusReads);
                        consensusReads++;
                        if (toSeparateGroups)
                            consensus.getReadsWithConsensuses().forEach(writer::write);
                        else
                            writer.write(consensus.toParsedRead());
                    }
                    if (debugOutputStream != null)
                        consensus.debugData.writeDebugData(clusterIndex++, i);
                }
            }
            reader.close();
            originalNumberOfReads = reader.getOriginalNumberOfReads();
            writer.setOriginalNumberOfReads(originalNumberOfReads);
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        if (originalReadStatsFileName != null) {
            System.err.println("Writing file with stats for original reads...");
            try (PrintStream originalReadsDataWriter = new PrintStream(
                    new FileOutputStream(originalReadStatsFileName))) {
                StringBuilder header = new StringBuilder("read.id consensus.id status consensus.best.id reads.num");
                for (String groupName : defaultGroups) {
                    header.append(' ').append(groupName).append(".consensus.seq ");
                    header.append(groupName).append(".consensus.qual");
                }
                originalReadsDataWriter.println(header);

                for (long readId = 0; readId < originalNumberOfReads; readId++) {
                    OriginalReadData currentReadData = originalReadsData.get(readId);
                    OriginalReadStatus status = (currentReadData == null) ? NOT_MATCHED : currentReadData.status;
                    Consensus consensus = (status == USED_IN_CONSENSUS) ? currentReadData.consensus : null;

                    StringBuilder line = new StringBuilder();
                    line.append(readId).append(' ');
                    if (consensus == null)
                        line.append("-1 ");
                    else {
                        long finalId = Objects.requireNonNull(consensusFinalIds).get(consensus.tempId);
                        if (finalId == -1)
                            throw new IllegalStateException("Consensus finalId == -1 for tempId " + consensus.tempId);
                        line.append(finalId).append(' ');
                    }
                    line.append(status.name()).append(' ');
                    line.append((consensus == null) ? -1 : consensus.sequences[0].getOriginalReadId()).append(' ');
                    line.append((consensus == null) ? 0 : consensus.consensusReadsNum);
                    for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                        if (consensus == null)
                            line.append(" - -");
                        else {
                            SequenceWithAttributes currentSeq = consensus.sequences[targetIndex];
                            line.append(' ').append(currentSeq.getSeq());
                            line.append(' ').append(currentSeq.getQual());
                        }
                    }
                    originalReadsDataWriter.println(line);
                }
            } catch (IOException e) {
                throw exitWithError(e.getMessage());
            }
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
        ArrayList<GroupEdge> groupEdges = mifHeader.getGroupEdges();
        numberOfTargets = mifHeader.getNumberOfTargets();
        defaultGroups = IntStream.rangeClosed(1, numberOfTargets).mapToObj(i -> "R" + i)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        MifHeader newHeader;
        if (toSeparateGroups) {
            Set<String> defaultSeparateGroups = IntStream.rangeClosed(1, numberOfTargets)
                    .mapToObj(i -> "CR" + i).collect(Collectors.toSet());
            if (((groupSet != null) && (groupSet.stream().anyMatch(defaultSeparateGroups::contains)))
                    || (groupEdges.stream().map(GroupEdge::getGroupName).anyMatch(defaultSeparateGroups::contains)))
                throw exitWithError("Groups CR1, CR2 etc must not be used in --groups flag and input file if "
                        + "--consensuses-to-separate-groups flag is specified!");
            defaultSeparateGroups.stream().sorted().forEachOrdered(name -> {
                groupEdges.add(new GroupEdge(name, true));
                groupEdges.add(new GroupEdge(name, false));
            });
        }
        newHeader = new MifHeader(numberOfTargets, mifHeader.getCorrectedGroups(), false, groupEdges);
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), newHeader)
                : new MifWriter(outputFileName, newHeader);
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

    enum OriginalReadStatus {
        NOT_MATCHED, READ_DISCARDED_TRIM, CONSENSUS_DISCARDED_TRIM, NOT_USED_IN_CONSENSUS, USED_IN_CONSENSUS
    }

    private class OriginalReadData {
        OriginalReadStatus status = NOT_USED_IN_CONSENSUS;
        Consensus consensus = null;
    }

    private enum SpecialSequences {
        NULL_SEQ, EMPTY_SEQ
    }

    private class SequenceWithAttributes {
        private final NucleotideSequence seq;
        private final SequenceQuality qual;
        private final long originalReadId;

        SequenceWithAttributes(NucleotideSequence seq, SequenceQuality qual, long originalReadId) {
            this.seq = seq;
            this.qual = qual;
            this.originalReadId = originalReadId;
        }

        SequenceWithAttributes(SpecialSequences type, long originalReadId) {
            if (type == SpecialSequences.NULL_SEQ) {
                this.seq = null;
                this.qual = null;
            } else if (type == SpecialSequences.EMPTY_SEQ) {
                this.seq = NucleotideSequence.EMPTY;
                this.qual = SequenceQuality.EMPTY;
            } else throw new IllegalArgumentException("Unknown special sequence type: " + type);
            this.originalReadId = originalReadId;
        }

        NucleotideSequence getSeq() {
            return seq;
        }

        SequenceQuality getQual() {
            return qual;
        }

        long getOriginalReadId() {
            return originalReadId;
        }

        private SequenceWithAttributes resultWithCachedValues(int from, int to) {
            NucleotideSequence seqPart = (seq == null) ? null : seq.getRange(from, to);
            SequenceQuality qualPart = (qual == null) ? null : qual.getRange(from, to);
            NucleotideSequence cachedSeq = sequencesCache.get(seqPart);
            if (cachedSeq == null)
                cachedSeq = seqPart;
            SequenceQuality cachedQual = ((qualPart != null) && (qualPart.size() == 1))
                    ? qualityCache.get(qualPart.value(0)) : qualPart;
            return new SequenceWithAttributes(cachedSeq, cachedQual, originalReadId);
        }

        SequenceWithAttributes letterAt(int position) {
            if ((seq == null) || (position < 0) || (position >= seq.size()))
                return new SequenceWithAttributes(SpecialSequences.NULL_SEQ, originalReadId);
            return resultWithCachedValues(position, position + 1);
        }

        SequenceWithAttributes getSubSequence(int from, int to) {
            if (seq == null)
                throw new IllegalStateException("getSubSequence() called for null sequence! Read id: "
                        + originalReadId);
            if ((from < 0) || (to > seq.size()) || (to - from < 1))
                throw new IndexOutOfBoundsException("seq.size(): " + seq.size() + ", from: " + from + ", to: " + to);
            return resultWithCachedValues(from, to);
        }

        NSequenceWithQuality toNSequenceWithQuality() {
            return new NSequenceWithQuality(seq, qual);
        }

        long calculateSumQuality() {
            if ((qual == null) || isEmpty())
                return 0;
            long sum = 0;
            for (byte quality : qual.asArray())
                sum += quality;
            return sum;
        }

        byte calculateMinQuality() {
            if ((qual == null) || isEmpty())
                return 0;
            byte minQuality = DEFAULT_MAX_QUALITY;
            for (byte quality : qual.asArray())
                if (quality < minQuality)
                    minQuality = quality;
            return minQuality;
        }

        boolean isNull() {
            return seq == null;
        }

        boolean isEmpty() {
            return NucleotideSequence.EMPTY.equals(seq);
        }

        int size() {
            return (seq == null) ? 0 : seq.size();
        }

        @Override
        public String toString() {
            return "SequenceWithAttributes{" + "seq=" + seq + ", qual=" + qual
                    + ", originalReadId=" + originalReadId + '}';
        }
    }

    private class Barcode {
        final String groupName;
        final SequenceWithAttributes value;

        Barcode(String groupName, SequenceWithAttributes value) {
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
        final SequenceWithAttributes[] sequences;
        final TargetBarcodes[] barcodes;
        final long originalReadId;
        final LinkedHashMap<String, SequenceWithAttributes> otherGroups = toSeparateGroups ? new LinkedHashMap<>()
                : null;

        DataFromParsedRead(ParsedRead parsedRead) {
            originalReadId = parsedRead.getOriginalRead().getId();
            List<MatchedGroup> parsedReadGroups = parsedRead.getGroups();
            List<MatchedGroup> extractedGroups = parsedReadGroups.stream()
                    .filter(g -> defaultGroups.contains(g.getGroupName())).collect(Collectors.toList());
            if (extractedGroups.size() != numberOfTargets)
                throw new IllegalArgumentException("Wrong number of target groups in ParsedRead: expected "
                        + numberOfTargets + ", target groups in ParsedRead: " + parsedRead.getGroups().stream()
                        .map(MatchedGroup::getGroupName).filter(defaultGroups::contains).collect(Collectors.toList()));
            sequences = new SequenceWithAttributes[numberOfTargets];
            extractedGroups.forEach(group ->
                    sequences[getTargetIndex(group.getTargetId(), parsedRead.isReverseMatch())] =
                            new SequenceWithAttributes(group.getValue().getSequence(), group.getValue().getQuality(),
                                    originalReadId));
            barcodes = IntStream.range(0, numberOfTargets).mapToObj(i -> new TargetBarcodes(new ArrayList<>()))
                    .toArray(TargetBarcodes[]::new);
            parsedReadGroups.forEach(group -> {
                SequenceWithAttributes sequenceWithAttributes = new SequenceWithAttributes(
                        group.getValue().getSequence(), group.getValue().getQuality(), originalReadId);
                if (groupSet.contains(group.getGroupName())) {
                    int targetIndex = getTargetIndex(group.getTargetId(), parsedRead.isReverseMatch());
                    ArrayList<Barcode> currentTargetList = barcodes[targetIndex].targetBarcodes;
                    currentTargetList.add(new Barcode(group.getGroupName(), sequenceWithAttributes));
                } else if (toSeparateGroups && !defaultGroups.contains(group.getGroupName()))
                    otherGroups.put(group.getGroupName(), sequenceWithAttributes);
            });
        }

        DataFromParsedRead(SequenceWithAttributes[] sequences, TargetBarcodes[] barcodes, long originalReadId) {
            this.sequences = sequences;
            this.barcodes = barcodes;
            this.originalReadId = originalReadId;
        }

        private int getTargetIndex(byte targetId, boolean isReverseMatch) {
            int index = targetId - 1;
            if (isReverseMatch) {
                if (index == 0)
                    index = 1;
                else if (index == 1)
                    index = 0;
            }
            return index;
        }
    }

    private class Consensus {
        final SequenceWithAttributes[] sequences;
        final TargetBarcodes[] barcodes;
        final int consensusReadsNum;
        final ArrayList<DataFromParsedRead> savedOriginalSequences = toSeparateGroups ? new ArrayList<>() : null;
        final ConsensusDebugData debugData;
        final boolean isConsensus;
        final long tempId;

        Consensus(SequenceWithAttributes[] sequences, TargetBarcodes[] barcodes, int consensusReadsNum,
                  ConsensusDebugData debugData, long tempId) {
            this.sequences = sequences;
            this.barcodes = barcodes;
            this.consensusReadsNum = consensusReadsNum;
            this.debugData = debugData;
            this.isConsensus = true;
            this.tempId = tempId;
        }

        Consensus(ConsensusDebugData debugData) {
            this.sequences = null;
            this.barcodes = null;
            this.consensusReadsNum = 0;
            this.debugData = debugData;
            this.isConsensus = false;
            this.tempId = -1;
        }

        ParsedRead toParsedRead() {
            if (!isConsensus || (sequences == null) || (barcodes == null))
                throw exitWithError("toParsedRead() called for null consensus!");
            SequenceRead originalRead;
            SingleRead[] reads = new SingleRead[numberOfTargets];
            ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
            for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
                SequenceWithAttributes currentSequence = sequences[targetId - 1];
                TargetBarcodes targetBarcodes = barcodes[targetId - 1];
                reads[targetId - 1] = new SingleReadImpl(currentSequence.getOriginalReadId(),
                        currentSequence.toNSequenceWithQuality(), "Consensus");
                addReadGroupEdges(matchedGroupEdges, targetId, currentSequence.toNSequenceWithQuality());
                for (Barcode barcode : targetBarcodes.targetBarcodes)
                    addGroupEdges(matchedGroupEdges, targetId, barcode.groupName,
                            currentSequence.toNSequenceWithQuality(), barcode.value.toNSequenceWithQuality());
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

        List<ParsedRead> getReadsWithConsensuses() {
            if (!isConsensus || (sequences == null) || (barcodes == null))
                throw exitWithError("getReadsWithConsensuses() called for null consensus!");
            if (!toSeparateGroups || (savedOriginalSequences == null))
                throw exitWithError("getReadsWithConsensuses() called when toSeparateGroups flag is not set!");
            else {
                List<ParsedRead> generatedReads = new ArrayList<>();
                for (DataFromParsedRead currentOriginalData : savedOriginalSequences) {
                    ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
                    SingleRead[] reads = new SingleRead[numberOfTargets];
                    for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
                        SequenceWithAttributes currentOriginalSequence = currentOriginalData.sequences[targetId - 1];
                        SequenceWithAttributes currentConsensusSequence = sequences[targetId - 1];
                        TargetBarcodes targetBarcodes = barcodes[targetId - 1];
                        reads[targetId - 1] = new SingleReadImpl(currentOriginalSequence.getOriginalReadId(),
                                currentOriginalSequence.toNSequenceWithQuality(), "");
                        addReadGroupEdges(matchedGroupEdges, targetId,
                                currentOriginalSequence.toNSequenceWithQuality());
                        addGroupEdges(matchedGroupEdges, targetId, "CR" + targetId,
                                currentOriginalSequence.toNSequenceWithQuality(),
                                currentConsensusSequence.toNSequenceWithQuality());
                        for (Barcode barcode : targetBarcodes.targetBarcodes)
                            addGroupEdges(matchedGroupEdges, targetId, barcode.groupName,
                                    currentOriginalSequence.toNSequenceWithQuality(),
                                    barcode.value.toNSequenceWithQuality());
                        for (HashMap.Entry<String, SequenceWithAttributes> entry
                                : currentOriginalData.otherGroups.entrySet())
                            addGroupEdges(matchedGroupEdges, targetId, entry.getKey(),
                                    currentOriginalSequence.toNSequenceWithQuality(),
                                    entry.getValue().toNSequenceWithQuality());
                    }

                    SequenceRead originalRead;
                    if (numberOfTargets == 1)
                        originalRead = reads[0];
                    else if (numberOfTargets == 2)
                        originalRead = new PairedRead(reads);
                    else
                        originalRead = new MultiRead(reads);

                    Match bestMatch = new Match(numberOfTargets, 0, matchedGroupEdges);
                    generatedReads.add(new ParsedRead(originalRead, false, bestMatch, consensusReadsNum));
                }
                return generatedReads;
            }
        }

        private void addReadGroupEdges(ArrayList<MatchedGroupEdge> matchedGroupEdges, byte targetId,
                                       NSequenceWithQuality seq) {
            matchedGroupEdges.add(new MatchedGroupEdge(seq, targetId,
                    new GroupEdge("R" + targetId, true), 0));
            matchedGroupEdges.add(new MatchedGroupEdge(null, targetId,
                    new GroupEdge("R" + targetId, false), seq.size()));
        }

        private void addGroupEdges(ArrayList<MatchedGroupEdge> matchedGroupEdges, byte targetId, String groupName,
                                   NSequenceWithQuality target, NSequenceWithQuality value) {
            matchedGroupEdges.add(new MatchedGroupEdge(target, targetId, new GroupEdge(groupName, true), value));
            matchedGroupEdges.add(new MatchedGroupEdge(null, targetId,
                    new GroupEdge(groupName, false), null));
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

        long size() {
            return consensuses.stream().filter(c -> c.isConsensus).count();
        }
    }

    private class ConsensusDebugData {
        // outer list - targetIndex, second - sequenceIndex, inner - positionIndex
        List<ArrayList<ArrayList<SequenceWithAttributes>>> data = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> new ArrayList<ArrayList<SequenceWithAttributes>>()).collect(Collectors.toList());

        void writeDebugData(int clusterIndex, int consensusIndex) {
            debugOutputStream.println("\nclusterIndex: " + clusterIndex + ", consensusIndex: " + consensusIndex);
            for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                debugOutputStream.println("targetIndex: " + targetIndex);
                ArrayList<ArrayList<SequenceWithAttributes>> targetData = data.get(targetIndex);
                for (ArrayList<SequenceWithAttributes> sequenceData : targetData) {
                    StringBuilder sequenceString = new StringBuilder();
                    for (SequenceWithAttributes currentLetter : sequenceData) {
                        if (currentLetter.isNull())
                            sequenceString.append(".");
                        else if (currentLetter.isEmpty())
                            sequenceString.append("-");
                        else {
                            if (currentLetter.getQual().value(0) < debugQualityThreshold)
                                sequenceString.append(Character.toLowerCase(currentLetter.getSeq()
                                        .symbolAt(0)));
                            else
                                sequenceString.append(Character.toUpperCase(currentLetter.getSeq()
                                        .symbolAt(0)));
                        }
                    }
                    if (sequenceData.size() > 0)
                        sequenceString.append(" - originalReadId: ").append(sequenceData.get(0).getOriginalReadId());
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
                    long sumQuality = Arrays.stream(data.get(i).sequences)
                            .mapToLong(SequenceWithAttributes::calculateSumQuality).sum();
                    if (sumQuality > bestSumQuality) {
                        bestSumQuality = sumQuality;
                        bestDataIndex = i;
                    }
                }
                DataFromParsedRead bestData = data.get(bestDataIndex);
                HashSet<Integer> filteredOutReads = new HashSet<>();
                ArrayList<AlignedSubsequences> subsequencesList = getAlignedSubsequencesList(data, filteredOutReads,
                        bestData.sequences, bestDataIndex);
                Consensus stage1Consensus = generateConsensus(subsequencesList, bestData.sequences, bestData.barcodes,
                        false);

                if (!stage1Consensus.isConsensus) {
                    displayWarning("WARNING: consensus assembled from " + (data.size() - filteredOutReads.size())
                            + " reads discarded on stage 1 after quality trimming! Barcode values: "
                            + formatBarcodeValues(bestData.barcodes) + ", best read id: " + bestData.originalReadId);
                    if (debugOutputStream != null)
                        calculatedConsensuses.consensuses.add(stage1Consensus);
                } else {
                    // stage 2: align to consensus from stage 1
                    subsequencesList = getAlignedSubsequencesList(trimBadQualityTails(data), filteredOutReads,
                            stage1Consensus.sequences, -1);
                    if (subsequencesList.size() > 0) {
                        Consensus stage2Consensus = generateConsensus(subsequencesList,
                                Objects.requireNonNull(stage1Consensus.sequences),
                                Objects.requireNonNull(stage1Consensus.barcodes), true);
                        if (!stage2Consensus.isConsensus) {
                            displayWarning("WARNING: consensus assembled from " + (data.size()
                                    - filteredOutReads.size()) + " reads discarded on stage 2 after "
                                    + "quality trimming! Barcode values: " + formatBarcodeValues(bestData.barcodes)
                                    + ", best read id: " + bestData.originalReadId);
                            if (debugOutputStream != null)
                                calculatedConsensuses.consensuses.add(stage2Consensus);
                        } else {
                            if (toSeparateGroups)
                                for (int i = 0; i < data.size(); i++) {
                                    if (!filteredOutReads.contains(i))
                                        stage2Consensus.savedOriginalSequences.add(data.get(i));
                                }
                            calculatedConsensuses.consensuses.add(stage2Consensus);
                        }
                    }
                }

                if ((filteredOutReads.size() < data.size())
                        && (float)filteredOutReads.size() / cluster.data.size() >= skippedFractionToRepeat) {
                    if (calculatedConsensuses.size() < maxConsensusesPerCluster) {
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
                builder.append(barcode.groupName).append('=').append(barcode.value.getSeq().toString());
            }
            return builder.toString();
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
                SequenceWithAttributes[] sequences = dataFromParsedRead.sequences;
                SequenceWithAttributes[] processedSequences = new SequenceWithAttributes[numberOfTargets];
                boolean allSequencesAreGood = true;
                for (int i = 0; i < numberOfTargets; i++) {
                    SequenceWithAttributes sequence = sequences[i];
                    int trimResultLeft = trim(sequence.getQual(), 0, sequence.size(), 1,
                            true, readsAvgQualityThreshold, readsTrimWindowSize);
                    if (trimResultLeft < -1) {
                        allSequencesAreGood = false;
                        break;
                    }
                    int trimResultRight = trim(sequence.getQual(), 0, sequence.size(), -1,
                            true, readsAvgQualityThreshold, readsTrimWindowSize);
                    if (trimResultRight < 0)
                        throw new IllegalStateException("Unexpected negative trimming result");
                    else if (trimResultRight - trimResultLeft - 1 < readsMinGoodSeqLength) {
                        allSequencesAreGood = false;
                        break;
                    } else
                        processedSequences[i] = sequence.getSubSequence(trimResultLeft + 1, trimResultRight);
                }

                if (allSequencesAreGood)
                    processedData.add(new DataFromParsedRead(processedSequences, dataFromParsedRead.barcodes,
                            dataFromParsedRead.originalReadId));
                else {
                    processedData.add(null);
                    if (originalReadsData != null)
                        originalReadsData.get(dataFromParsedRead.originalReadId).status = READ_DISCARDED_TRIM;
                }
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
                HashSet<Integer> filteredOutReads, SequenceWithAttributes[] bestSequences, int bestSeqIndex) {
            ArrayList<AlignedSubsequences> subsequencesList = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                if (i != bestSeqIndex) {
                    if (!filteredOutReads.contains(i) && (data.get(i) != null)) {
                        DataFromParsedRead currentData = data.get(i);
                        int sumScore = 0;
                        ArrayList<Alignment<NucleotideSequence>> alignments = new ArrayList<>();
                        for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                            SequenceWithAttributes currentSequence = currentData.sequences[targetIndex];
                            Alignment<NucleotideSequence> alignment = alignLocalGlobal(scoring,
                                    bestSequences[targetIndex].toNSequenceWithQuality(),
                                    currentSequence.toNSequenceWithQuality(), alignerWidth);
                            alignments.add(alignment);
                            sumScore += alignment.getScore();
                        }
                        if (sumScore < scoreThreshold)
                            filteredOutReads.add(i);
                        else {
                            AlignedSubsequences currentSubsequences = new AlignedSubsequences(bestSequences,
                                    currentData.originalReadId);
                            for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                                SequenceWithAttributes currentSequence = currentData.sequences[targetIndex];
                                SequenceWithAttributes alignedBestSequence = bestSequences[targetIndex];
                                int previousSeqPosition = -1;
                                for (int position = 0; position < alignedBestSequence.size(); position++) {
                                    Alignment<NucleotideSequence> alignment = alignments.get(targetIndex);
                                    int seqPosition = alignment.convertToSeq2Position(position);
                                    if (previousSeqPosition < 0) {
                                        if (seqPosition < 0)
                                            currentSubsequences.set(targetIndex, position,
                                                    new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                                            currentSequence.getOriginalReadId()));
                                        else
                                            currentSubsequences.set(targetIndex, position,
                                                    currentSequence.getSubSequence(0, seqPosition + 1));
                                        previousSeqPosition = seqPosition;
                                    } else {
                                        if (seqPosition < 0)
                                            currentSubsequences.set(targetIndex, position,
                                                    new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                                            currentSequence.getOriginalReadId()));
                                        else {
                                            if (previousSeqPosition == currentSequence.size() - 1)
                                                currentSubsequences.set(targetIndex, position,
                                                        new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                                                currentSequence.getOriginalReadId()));
                                            else
                                                currentSubsequences.set(targetIndex, position,
                                                        currentSequence.getSubSequence(previousSeqPosition + 1,
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
                    AlignedSubsequences currentSubsequences = new AlignedSubsequences(bestSequences,
                            data.get(i).originalReadId);
                    for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                        SequenceWithAttributes currentSequence = bestSequences[targetIndex];
                        for (int position = 0; position < currentSequence.size(); position++)
                            currentSubsequences.set(targetIndex, position, currentSequence.letterAt(position));
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
         * @param stage2            true if this is 2nd stage (best sequences are consensuses from stage1),
         *                          or false if this is 1nd stage
         * @return                  consensus: array of sequences (1 sequence for 1 target) and consensus score
         */
        private Consensus generateConsensus(ArrayList<AlignedSubsequences> subsequencesList,
                                            SequenceWithAttributes[] bestSequences, TargetBarcodes[] barcodes,
                                            boolean stage2) {
            ConsensusDebugData debugData = (debugOutputStream == null) ? null : new ConsensusDebugData();
            int consensusReadsNum = subsequencesList.size();
            long bestSeqReadId = bestSequences[0].getOriginalReadId();
            SequenceWithAttributes[] sequences = new SequenceWithAttributes[numberOfTargets];
            List<LettersWithPositions> lettersList = IntStream.range(0, consensusReadsNum)
                    .mapToObj(i -> new LettersWithPositions()).collect(Collectors.toList());
            TargetBarcodes[] consensusBarcodes = IntStream.range(0, numberOfTargets)
                    .mapToObj(i -> new TargetBarcodes(new ArrayList<>())).toArray(TargetBarcodes[]::new);
            for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                ArrayList<ArrayList<SequenceWithAttributes>> debugDataForThisTarget = (debugData == null) ? null
                        : debugData.data.get(targetIndex);
                consensusBarcodes[targetIndex].targetBarcodes.addAll(barcodes[targetIndex].targetBarcodes);
                List<ArrayList<SequenceWithAttributes>> lettersMatrixList = IntStream.range(0, consensusReadsNum)
                        .mapToObj(i -> new ArrayList<SequenceWithAttributes>()).collect(Collectors.toList());
                for (int position = 0; position < bestSequences[targetIndex].size(); position++) {
                    ArrayList<SequenceWithAttributes> currentPositionSequences = new ArrayList<>();
                    int bestQualityIndex = -1;
                    byte bestQuality = -1;
                    for (int i = 0; i < consensusReadsNum; i++) {
                        AlignedSubsequences currentSubsequences = subsequencesList.get(i);
                        SequenceWithAttributes currentSequence = currentSubsequences.get(targetIndex, position);
                        currentPositionSequences.add(currentSequence);
                        if (!currentSequence.isNull()) {
                            byte currentQuality = currentSequence.calculateMinQuality();
                            if (currentQuality > bestQuality) {
                                bestQuality = currentQuality;
                                bestQualityIndex = i;
                            }
                        }
                    }

                    LettersMatrix lettersMatrix;
                    if (bestQualityIndex == -1) {
                        // in this case lettersMatrix is empty and getRowLength() will return 0
                        lettersMatrix = new LettersMatrix();
                    } else {
                        lettersMatrix = new LettersMatrix(currentPositionSequences.get(bestQualityIndex),
                                bestQualityIndex);
                        for (int i = 0; i < currentPositionSequences.size(); i++) {
                            if (i != bestQualityIndex) {
                                SequenceWithAttributes currentSeq = currentPositionSequences.get(i);
                                if (!currentSeq.isNull())
                                    lettersMatrix.add(currentSeq, alignLocalGlobal(scoring,
                                            currentPositionSequences.get(bestQualityIndex).toNSequenceWithQuality(),
                                            currentPositionSequences.get(i).toNSequenceWithQuality(), alignerWidth));
                                else
                                    lettersMatrix.addNull(currentSeq.getOriginalReadId());
                            }
                        }
                    }
                    for (int sequenceIndex = 0; sequenceIndex < consensusReadsNum; sequenceIndex++) {
                        ArrayList<SequenceWithAttributes> currentLettersRow = lettersMatrixList.get(sequenceIndex);
                        for (int letterIndex = 0; letterIndex < lettersMatrix.getRowLength(); letterIndex++)
                            addLetterToRow(currentLettersRow,
                                    lettersMatrix.getLetterByCoordinate(sequenceIndex, letterIndex));
                    }
                }

                // moving letters from lists to LettersWithPositions objects
                for (int sequenceIndex = 0; sequenceIndex < consensusReadsNum; sequenceIndex++) {
                    ArrayList<SequenceWithAttributes> currentLettersRow = lettersMatrixList.get(sequenceIndex);
                    LettersWithPositions currentLettersWithPositions = lettersList.get(sequenceIndex);
                    currentLettersWithPositions.set(targetIndex, currentLettersRow);
                    if (debugDataForThisTarget != null)
                        debugDataForThisTarget.add(currentLettersRow);
                }

                // choosing letters for consensus and calculating quality
                int fullRowLength = lettersMatrixList.get(0).size();
                ArrayList<SequenceWithAttributes> consensusLetters = getLettersWithQuality(lettersList, fullRowLength,
                        targetIndex);

                // consensus sequence assembling and quality trimming
                if (consensusLetters.size() == 0) {
                    storeOriginalReadsData(subsequencesList, CONSENSUS_DISCARDED_TRIM, null);
                    return new Consensus(debugData);
                }
                NSequenceWithQuality consensusRawSequence = NSequenceWithQuality.EMPTY;
                for (SequenceWithAttributes consensusLetter : consensusLetters)
                    consensusRawSequence = consensusRawSequence.concatenate(consensusLetter.toNSequenceWithQuality());
                SequenceWithAttributes consensusSequence = new SequenceWithAttributes(
                        consensusRawSequence.getSequence(), consensusRawSequence.getQuality(), bestSeqReadId);

                int trimResultLeft = trim(consensusSequence.getQual(), 0, consensusSequence.size(),
                        1, true, avgQualityThreshold, trimWindowSize);
                if (trimResultLeft < -1) {
                    storeOriginalReadsData(subsequencesList, CONSENSUS_DISCARDED_TRIM, null);
                    return new Consensus(debugData);
                }
                int trimResultRight = trim(consensusSequence.getQual(), 0, consensusSequence.size(),
                        -1, true, avgQualityThreshold, trimWindowSize);
                if (trimResultRight < 0)
                    throw new IllegalStateException("Unexpected negative trimming result");
                else if (trimResultRight - trimResultLeft - 1 < minGoodSeqLength) {
                    storeOriginalReadsData(subsequencesList, CONSENSUS_DISCARDED_TRIM, null);
                    return new Consensus(debugData);
                }
                consensusSequence = consensusSequence.getSubSequence(trimResultLeft + 1, trimResultRight);
                sequences[targetIndex] = consensusSequence;
            }

            Consensus consensus = new Consensus(sequences, consensusBarcodes, consensusReadsNum, debugData,
                    consensusCurrentTempId.getAndIncrement());
            if (stage2)
                storeOriginalReadsData(subsequencesList, USED_IN_CONSENSUS, consensus);
            return consensus;
        }

        private void storeOriginalReadsData(ArrayList<AlignedSubsequences> subsequencesList,
                                            OriginalReadStatus status, Consensus consensus) {
            if (originalReadsData != null)
                subsequencesList.forEach(alignedSubsequences -> {
                    OriginalReadData originalReadData = originalReadsData.get(alignedSubsequences.originalReadId);
                    originalReadData.status = status;
                    originalReadData.consensus = consensus;
                });
        }

        /**
         * Choose letters for consensus and calculate their quality.
         *
         * @param lettersList       each element of this list is LettersWithPositions structure that allows to get
         *                          letter by targetIndex and position; one element of lettersList is for one original
         *                          (possibly multi-target) base read for this consensus
         * @param fullRowLength     row length of aligned sequences matrix for current targetIndex
         * @param targetIndex       current targetIndex
         * @return                  list of calculated consensus letters with calculated qualities
         */
        private ArrayList<SequenceWithAttributes> getLettersWithQuality(List<LettersWithPositions> lettersList,
                                                                        int fullRowLength, int targetIndex) {
            ArrayList<SequenceWithAttributes> consensusLetters = new ArrayList<>();

            for (int position = 0; position < fullRowLength; position++) {
                ArrayList<SequenceWithAttributes> baseLetters = new ArrayList<>();
                // loop by source reads for this consensus
                for (LettersWithPositions currentLettersWithPositions : lettersList) {
                    SequenceWithAttributes currentLetter = currentLettersWithPositions.get(targetIndex, position);
                    if (currentLetter.isEmpty())
                        baseLetters.add(currentLetter);
                    else if (!currentLetter.isNull()) {
                        NucleotideSequence letterWithoutQuality = currentLetter.getSeq();
                        if (letterWithoutQuality.containsWildcards()) {
                            Wildcard wildcard = wildcards.get(letterWithoutQuality);
                            for (int i = 0; i < wildcard.basicSize(); i++) {
                                NucleotideSequence currentBasicLetter = wildcardCodeToSequence
                                        .get(wildcard.getMatchingCode(i));
                                baseLetters.add(new SequenceWithAttributes(currentBasicLetter, qualityCache
                                        .get((byte)(currentLetter.getQual().value(0) / wildcard.basicSize())),
                                        currentLetter.getOriginalReadId()));
                            }
                        } else
                            baseLetters.add(currentLetter);
                    }
                }

                if (baseLetters.size() > 0) {
                    SequenceWithAttributes consensusLetter = calculateConsensusLetter(baseLetters);
                    if (!consensusLetter.isEmpty())
                        consensusLetters.add(consensusLetter);
                }
            }

            return consensusLetters;
        }

        /**
         * Calculate consensus letter from list of base letters.
         *
         * @param baseLetters       base letters; allowed values A, T, G, C and EMPTY_SEQ (deletion)
         * @return                  calculated consensus letter: letter with quality or EMPTY_SEQ for deletion
         */
        private SequenceWithAttributes calculateConsensusLetter(List<SequenceWithAttributes> baseLetters) {
            if (baseLetters.size() == 1)
                return baseLetters.get(0);
            Map<NucleotideSequence, Integer> letterCounts = Arrays.stream(consensusMajorBases)
                    .collect(Collectors.toMap(majorBase -> majorBase, majorBase -> (int)(baseLetters.stream()
                            .map(SequenceWithAttributes::getSeq).filter(majorBase::equals).count())));
            int deletionsCount = (int)(baseLetters.stream().filter(SequenceWithAttributes::isEmpty).count());
            if (letterCounts.values().stream().allMatch(count -> count <= deletionsCount))
                return new SequenceWithAttributes(SpecialSequences.EMPTY_SEQ, -1);
            final double gamma = 1.0 / (consensusMajorBases.length - 1);

            NucleotideSequence bestMajorBase = null;
            double bestQuality = -1;
            for (NucleotideSequence majorBase : consensusMajorBases) {
                double product = Math.pow(gamma, -letterCounts.get(majorBase));
                for (SequenceWithAttributes currentLetter : baseLetters)
                    if (!currentLetter.isEmpty()) {
                        double errorProbability = Math.pow(10.0, -currentLetter.getQual().value(0) / 10.0);
                        if (currentLetter.getSeq().equals(majorBase))
                            product *= (1 - errorProbability) / Math.max(OVERFLOW_PROTECTION_MIN, errorProbability);
                        else
                            product *= errorProbability / Math.max(OVERFLOW_PROTECTION_MIN,
                                    1 - gamma * errorProbability);
                        product = Math.min(product, OVERFLOW_PROTECTION_MAX);
                    }

                double majorErrorProbability = 1.0 / (1 + product);
                double quality = -10 * Math.log10(majorErrorProbability);
                if (quality > bestQuality) {
                    bestMajorBase = majorBase;
                    bestQuality = quality;
                }
            }

            return new SequenceWithAttributes(Objects.requireNonNull(bestMajorBase),
                    qualityCache.get((byte)Math.min(DEFAULT_MAX_QUALITY, bestQuality)), -1);
        }

        /**
         * Add new letter to the end of letters row with properly handling of trailing nulls: replace them with
         * deletions because there must be no nulls in the middle of sequence.
         *
         * @param currentLettersRow     current row of letters that will be modified
         * @param newLetter             new letter (can be NULL_SEQ or deletion) that is inserted to the end of row
         */
        private void addLetterToRow(ArrayList<SequenceWithAttributes> currentLettersRow,
                                    SequenceWithAttributes newLetter) {
            if (!newLetter.isNull()) {
                // letters from the same row must have the same read id
                long currentReadId = newLetter.getOriginalReadId();
                int firstNullPosition = -1;
                for (int currentPosition = currentLettersRow.size() - 1; currentPosition >= 0; currentPosition--) {
                    if (currentLettersRow.get(currentPosition).isNull())
                        firstNullPosition = currentPosition;
                    else
                        break;
                }
                if (firstNullPosition > 0)
                    for (int currentPosition = firstNullPosition; currentPosition < currentLettersRow.size();
                         currentPosition++)
                        currentLettersRow.set(currentPosition, new SequenceWithAttributes(SpecialSequences.EMPTY_SEQ,
                                currentReadId));
            }
            currentLettersRow.add(newLetter);
        }

        private class AlignedSubsequences {
            private final int[] indexes = new int[numberOfTargets];
            private final SequenceWithAttributes[] sequences;
            private final long originalReadId;

            AlignedSubsequences(SequenceWithAttributes[] bestSequences, long originalReadId) {
                int currentIndex = 0;
                for (int i = 0; i < numberOfTargets; i++) {
                    indexes[i] = currentIndex;
                    currentIndex += bestSequences[i].size();
                }
                sequences = new SequenceWithAttributes[currentIndex];
                this.originalReadId = originalReadId;
            }

            void set(int targetIndex, int position, SequenceWithAttributes value) {
                sequences[index(targetIndex, position)] = value;
            }

            SequenceWithAttributes get(int targetIndex, int position) {
                return sequences[index(targetIndex, position)];
            }

            private int index(int targetIndex, int position) {
                return indexes[targetIndex] + position;
            }
        }

        private class LettersWithPositions {
            private HashMap<Integer, ArrayList<SequenceWithAttributes>> targetSequences = new HashMap<>();

            void set(int targetIndex, ArrayList<SequenceWithAttributes> values) {
                for (SequenceWithAttributes value : values)
                    if (!value.isNull() && !value.isEmpty() && (value.size() != 1))
                        throw new IllegalArgumentException("Trying to write sequence " + value
                                + " to LettersWithPositions");
                if (targetSequences.containsKey(targetIndex))
                    throw new IllegalStateException("Trying to write key " + targetIndex + " to targetSequences "
                            + "when it already exists: targetSequences=" + targetSequences);
                targetSequences.put(targetIndex, values);
            }

            SequenceWithAttributes get(int targetIndex, int position) {
                return targetSequences.get(targetIndex).get(position);
            }
        }

        private class LettersMatrix {
            // column numbers in the matrix corresponding to base sequence letters; last value is row length
            private final int[] baseLettersCoordinates;
            private final int baseSequenceRealIndex;
            private final ArrayList<ArrayList<Integer>> positionsCache = new ArrayList<>();
            private final ArrayList<SequenceWithAttributes> sequences = new ArrayList<>();
            private final boolean nullBaseSequence;

            LettersMatrix() {
                nullBaseSequence = true;
                baseLettersCoordinates = null;
                this.baseSequenceRealIndex = 0;
            }

            LettersMatrix(SequenceWithAttributes baseSequence, int baseSequenceRealIndex) {
                if (baseSequence.isNull())
                    throw new IllegalArgumentException("Base sequence is null in LetterMatrix constructor; "
                            + "baseSequence = " + baseSequence + ", baseSequenceRealIndex = " + baseSequenceRealIndex);
                nullBaseSequence = false;
                baseLettersCoordinates = IntStream.rangeClosed(0, baseSequence.size()).toArray();
                sequences.add(baseSequence);
                this.baseSequenceRealIndex = baseSequenceRealIndex;
            }

            int getRowLength() {
                return nullBaseSequence ? 0 : baseLettersCoordinates[baseLettersCoordinates.length - 1];
            }

            void add(SequenceWithAttributes sequence, Alignment<NucleotideSequence> alignment) {
                if (nullBaseSequence)
                    throw new IllegalStateException("add(" + sequence + ", " + alignment + " called for LettersMatrix "
                            + "with null base sequence!");
                int stage = 0;  // 0 - before base start, 1 - inside alignment range, 2 - after base end
                int leftTailLength = 0;
                int rightTailLength = 0;
                int currentPartLength = 1;
                sequences.add(sequence);
                ArrayList<Integer> currentPositions = new ArrayList<>();
                SequenceWithAttributes baseSequence = sequences.get(0);
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

            void addNull(long originalReadId) {
                sequences.add(new SequenceWithAttributes(SpecialSequences.NULL_SEQ, originalReadId));
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

            SequenceWithAttributes getLetterByCoordinate(int sequenceRealIndex, int coordinate) {
                if (nullBaseSequence)
                    throw new IllegalStateException("getLetterByCoordinate(" + sequenceRealIndex + ", " + coordinate
                            + " called for LettersMatrix with null base sequence!");
                if (sequenceRealIndex == baseSequenceRealIndex) {
                    for (int i = 0; i < baseLettersCoordinates.length - 1; i++) {
                        int currentCoordinate = baseLettersCoordinates[i];
                        if (currentCoordinate == coordinate)
                            return sequences.get(0).letterAt(i);
                        else if (currentCoordinate > coordinate)
                            return new SequenceWithAttributes(SpecialSequences.EMPTY_SEQ,
                                    sequences.get(0).getOriginalReadId());
                    }
                    return new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                            sequences.get(0).getOriginalReadId());
                } else {
                    int sequenceIndex = (sequenceRealIndex > baseSequenceRealIndex) ? sequenceRealIndex
                            : sequenceRealIndex + 1;
                    SequenceWithAttributes sequence = sequences.get(sequenceIndex);
                    if (sequence.isNull())
                        return sequence;
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
                                return sequence.letterAt(seqPosition);
                            else if (currentBaseCoordinate > coordinate) {
                                if (seqPosition == 0)
                                    return new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                            sequence.getOriginalReadId());
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
                            return new SequenceWithAttributes(SpecialSequences.EMPTY_SEQ,
                                    sequence.getOriginalReadId());
                        } else if (coordinate < seqStartCoordinate)
                            return new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                    sequence.getOriginalReadId());
                        else
                            return sequence.letterAt(coordinate - seqStartCoordinate);
                    } else {
                        int currentPartStart = seqPosition - currentPartLength;
                        int wantedSeqPosition = currentPartStart + coordinate - baseLettersCoordinates[basePosition];
                        if (wantedSeqPosition >= seqPosition) {
                            /* if nucleotide not found and this is not last position in sequence, this is a deletion,
                               otherwise we are on the right from all sequence and return NULL_SEQ */
                            if (seqPosition < sequence.size())
                                return new SequenceWithAttributes(SpecialSequences.EMPTY_SEQ,
                                        sequence.getOriginalReadId());
                            else
                                return new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                        sequence.getOriginalReadId());
                        } else
                            return sequence.letterAt(wantedSeqPosition);
                    }
                }
            }
        }
    }
}
