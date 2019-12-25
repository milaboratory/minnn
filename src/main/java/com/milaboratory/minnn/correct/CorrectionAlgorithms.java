/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
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
package com.milaboratory.minnn.correct;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.clustering.Clustering;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NSequenceWithQualityBuilder;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceQuality;
import com.milaboratory.minnn.io.MifWriter;
import com.milaboratory.minnn.outputconverter.MatchedGroup;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.Match;
import com.milaboratory.minnn.pattern.MatchedGroupEdge;
import com.milaboratory.minnn.util.ConsensusLetter;
import com.milaboratory.util.SmartProgressReporter;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class CorrectionAlgorithms {
    private final BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory;
    private final WildcardClusteringStrategy wildcardClusteringStrategy;
    private final boolean averageBarcodeLengthRequired;
    private final int maxUniqueBarcodes;
    private final int minCount;
    private final boolean filterByCount;

    public CorrectionAlgorithms(
            BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory, int maxUniqueBarcodes, int minCount,
            float wildcardsCollapsingMergeThreshold) {
        this.barcodeClusteringStrategyFactory = barcodeClusteringStrategyFactory;
        this.wildcardClusteringStrategy = new WildcardClusteringStrategy(wildcardsCollapsingMergeThreshold);
        this.averageBarcodeLengthRequired = barcodeClusteringStrategyFactory.averageBarcodeLengthRequired();
        this.maxUniqueBarcodes = maxUniqueBarcodes;
        this.minCount = minCount;
        this.filterByCount = (maxUniqueBarcodes > 0) || (minCount > 1);
    }

    /**
     * Perform wildcards collapsing, barcodes correction, filtering by barcodes count, and prepare data that will be
     * used for correction.
     *
     * @param preprocessorPort          results of quality preprocessing:
     *                                  barcodes from clusters with calculated quality
     * @param keyGroups                 group names in which we will correct barcodes
     * @param orderedPortIndex          index for ordered output port, used in secondary barcodes correction
     *                                  for parallel correction of multiple primary barcode clusters
     * @return                          prepared correction data
     */
    public CorrectionData prepareCorrectionData(
            OutputPort<CorrectionQualityPreprocessingResult> preprocessorPort, LinkedHashSet<String> keyGroups,
            long orderedPortIndex) {
        CorrectionData correctionData = new CorrectionData(keyGroups, orderedPortIndex, filterByCount);
        boolean reportProgress = true;
        for (CorrectionQualityPreprocessingResult inputData : CUtils.it(preprocessorPort)) {
            correctionData.parsedReadsCount += inputData.clusterSize;
            // don't report progress inside each thread if this is correction with primary and secondary groups
            reportProgress &= (inputData.primaryGroups == null);
            for (Map.Entry<String, CorrectionGroupData> groupData : correctionData.keyGroupsData.entrySet()) {
                String groupName = groupData.getKey();
                CorrectionGroupData correctionGroupData = groupData.getValue();
                NSequenceWithQuality seqWithQuality = inputData.groupValues.get(groupName);
                NucleotideSequence seq = seqWithQuality.getSequence();

                SequenceWithWildcardsCount currentCounter = new SequenceWithWildcardsCount(seqWithQuality);
                currentCounter.count = inputData.clusterSize;
                correctionGroupData.wildcardCounters.add(currentCounter);
                correctionData.totalWildcardsCount += currentCounter.wildcardsCount * inputData.clusterSize;
                correctionData.totalNucleotidesCount += seq.size() * inputData.clusterSize;

                // counting raw barcode sequences if filtering by count is enabled
                if (filterByCount) {
                    Map<NucleotideSequence, SequenceCounter> rawBarcodeCounters =
                            correctionGroupData.notCorrectedBarcodeCounters;
                    rawBarcodeCounters.putIfAbsent(seq, new SequenceCounter(seq));
                    rawBarcodeCounters.get(seq).count += inputData.clusterSize;
                }

                if (averageBarcodeLengthRequired)
                    correctionGroupData.lengthSum += seq.size() * inputData.clusterSize;
            }
        }

        // clustering by wildcards; filling sequenceCounters and originalSequencesWithWildcards for final clustering
        for (HashMap.Entry<String, CorrectionGroupData> entry : correctionData.keyGroupsData.entrySet()) {
            String groupName = entry.getKey();
            CorrectionGroupData groupData = entry.getValue();
            Clustering<SequenceWithWildcardsCount, SequenceWithQualityForClustering> clustering = new Clustering<>(
                    groupData.wildcardCounters, new SequenceCounterExtractor<>(), wildcardClusteringStrategy);
            if (reportProgress)
                SmartProgressReporter.startProgressReport("Clustering barcodes by wildcards in group "
                                + groupName, clustering, System.err);
            clustering.performClustering().forEach(cluster -> {
                if (cluster.size() > 0) {
                    List<NSequenceWithQuality> originalSequencesWithQuality = new ArrayList<>();
                    Set<NucleotideSequence> originalSequences = new HashSet<>();
                    SequenceWithQualityAndCount head = cluster.getHead();
                    AtomicLong totalCount = new AtomicLong(head.count);
                    originalSequencesWithQuality.add(head.seq);
                    originalSequences.add(head.seq.getSequence());
                    cluster.processAllChildren(childCluster -> {
                        SequenceWithQualityAndCount child = childCluster.getHead();
                        totalCount.addAndGet(child.count);
                        originalSequencesWithQuality.add(child.seq);
                        originalSequences.add(child.seq.getSequence());
                        return true;
                    });
                    NSequenceWithQuality consensusSequence = mergeSequences(originalSequencesWithQuality);
                    groupData.originalSequencesWithWildcards.put(consensusSequence.getSequence(), originalSequences);
                    SequenceWithQualityAndCount currentCounter = new SequenceWithQualityAndCount(consensusSequence);
                    currentCounter.count = totalCount.get();
                    groupData.sequenceCounters.add(currentCounter);
                } else
                    groupData.sequenceCounters.add(cluster.getHead());
            });
            groupData.wildcardCounters = null;
        }

        // final clustering and filling correction map
        for (HashMap.Entry<String, CorrectionGroupData> entry : correctionData.keyGroupsData.entrySet()) {
            String groupName = entry.getKey();
            CorrectionGroupData groupData = entry.getValue();
            Clustering<SequenceWithQualityAndCount, SequenceWithQualityForClustering> clustering = new Clustering<>(
                    groupData.sequenceCounters, new SequenceCounterExtractor<>(),
                    barcodeClusteringStrategyFactory.createStrategy(
                            (float)(groupData.lengthSum) / correctionData.parsedReadsCount));
            if (reportProgress)
                SmartProgressReporter.startProgressReport("Clustering barcodes in group " + groupName,
                        clustering, System.err);
            clustering.performClustering().forEach(cluster -> {
                NSequenceWithQuality headSequence = cluster.getHead().seq;
                Set<NucleotideSequence> headOriginalSequences = groupData.originalSequencesWithWildcards
                        .get(headSequence.getSequence());
                if (headOriginalSequences != null)
                    headOriginalSequences.forEach(seq -> groupData.correctionMap.put(seq, headSequence));
                cluster.processAllChildren(child -> {
                    NucleotideSequence childSequence = child.getHead().seq.getSequence();
                    Set<NucleotideSequence> childOriginalSequences = groupData.originalSequencesWithWildcards
                            .get(childSequence);
                    if (childOriginalSequences != null)
                        childOriginalSequences.forEach(seq -> groupData.correctionMap.put(seq, headSequence));
                    else
                        groupData.correctionMap.put(childSequence, headSequence);
                    return true;
                });
            });
            groupData.sequenceCounters = null;
            groupData.originalSequencesWithWildcards = null;
        }

        if (filterByCount) {
            if (reportProgress)
                System.err.println("Filtering corrected barcodes by count...");
            // calculating which barcodes must be included or excluded, saving results to correctionData
            filterByCount(correctionData);
            correctionData.keyGroupsData.values().forEach(groupData -> groupData.notCorrectedBarcodeCounters = null);
        }

        return correctionData;
    }

    /**
     * Perform correction using prepared data, then write corrected reads and reads that were filtered out.
     *
     * @param correctionData            all prepared data that is needed for correction and writing
     * @param rawReadsPort              port (MIF reader) with raw parsed reads to correct barcodes in them
     * @param writer                    MIF writer for corrected reads
     * @param excludedBarcodesWriter    MIF writer for reads that were filtered out by barcodes count
     * @return                          correction stats: number of corrected reads and filtered out reads
     */
    public CorrectionStats correctAndWrite(
            CorrectionData correctionData, OutputPort<ParsedRead> rawReadsPort,
            MifWriter writer, MifWriter excludedBarcodesWriter) {
        long correctedReads = 0;
        long excludedReads = 0;

        for (int i = 0; i < correctionData.parsedReadsCount; i++) {
            ParsedRead parsedRead = rawReadsPort.take();
            if (parsedRead == null)
                throw new IllegalStateException("pass2Reader returned less reads than pass1Reader!");
            CorrectBarcodesResult correctBarcodesResult = correctBarcodes(parsedRead, correctionData);
            if (correctBarcodesResult.corrected)
                correctedReads++;
            if (correctBarcodesResult.excluded) {
                if (excludedBarcodesWriter != null)
                    excludedBarcodesWriter.write(correctBarcodesResult.parsedRead);
                excludedReads++;
            } else
                writer.write(correctBarcodesResult.parsedRead);
        }
        return new CorrectionStats(correctedReads, excludedReads,
                correctionData.totalWildcardsCount, correctionData.totalNucleotidesCount);
    }

    public static OutputPort<CorrectionQualityPreprocessingResult> getPreprocessingResultOutputPort(
            OutputPort<ParsedRead> inputPort, LinkedHashSet<String> keyGroups, LinkedHashSet<String> primaryGroups) {
        return new OutputPort<CorrectionQualityPreprocessingResult>() {
            LinkedHashMap<String, NucleotideSequence> previousGroups = null;
            LinkedHashMap<String, NucleotideSequence> previousPrimaryGroups = null;
            Map<String, long[]> currentClusterSumQualities = new HashMap<>();
            int currentCounter = 0;
            boolean finished = false;

            @Override
            public CorrectionQualityPreprocessingResult take() {
                if (finished)
                    return null;
                CorrectionQualityPreprocessingResult preparedResult = null;
                while (preparedResult == null) {
                    ParsedRead parsedRead = inputPort.take();
                    if (parsedRead != null) {
                        LinkedHashMap<String, NucleotideSequence> currentGroups = new LinkedHashMap<>();
                        Map<String, SequenceQuality> currentQualities = new HashMap<>();
                        for (String keyGroup : keyGroups) {
                            NSequenceWithQuality groupValue = parsedRead.getGroupValue(keyGroup);
                            currentGroups.put(keyGroup, groupValue.getSequence());
                            currentQualities.put(keyGroup, groupValue.getQuality());
                        }
                        LinkedHashMap<String, NucleotideSequence> currentPrimaryGroups;
                        if (primaryGroups.size() > 0) {
                            currentPrimaryGroups = new LinkedHashMap<>();
                            for (String primaryGroup : primaryGroups)
                                currentPrimaryGroups.put(primaryGroup,
                                        parsedRead.getGroupValue(primaryGroup).getSequence());
                        } else
                            currentPrimaryGroups = null;
                        if (!Objects.equals(currentPrimaryGroups, previousPrimaryGroups)
                                || !currentGroups.equals(previousGroups)) {
                            if (previousGroups != null) {
                                preparedResult = new CorrectionQualityPreprocessingResult(previousGroups,
                                        currentClusterSumQualities, currentCounter, previousPrimaryGroups);
                                currentClusterSumQualities = new HashMap<>();
                                currentCounter = 0;
                            }
                            previousGroups = currentGroups;
                            previousPrimaryGroups = currentPrimaryGroups;
                        }
                        for (String keyGroup : keyGroups) {
                            SequenceQuality currentGroupQuality = currentQualities.get(keyGroup);
                            currentClusterSumQualities.putIfAbsent(keyGroup, new long[currentGroupQuality.size()]);
                            long[] currentGroupSumQualities = currentClusterSumQualities.get(keyGroup);
                            for (int i = 0; i < currentGroupQuality.size(); i++)
                                currentGroupSumQualities[i] += currentGroupQuality.value(i);
                        }
                        currentCounter++;
                    } else {
                        finished = true;
                        if (previousGroups != null)
                            return new CorrectionQualityPreprocessingResult(previousGroups,
                                    currentClusterSumQualities, currentCounter, previousPrimaryGroups);
                        else
                            return null;
                    }
                }
                return preparedResult;
            }
        };
    }

    public static OutputPort<CorrectionData> performSecondaryBarcodesCorrection(
            OutputPort<CorrectionQualityPreprocessingResult> preprocessorPort,
            CorrectionAlgorithms correctionAlgorithmsInstance, LinkedHashSet<String> keyGroups, int threads) {
        OutputPort<PrimaryBarcodeCluster> clusterOutputPort;
        AtomicLong orderedPortIndex = new AtomicLong(0);

        clusterOutputPort = new OutputPort<PrimaryBarcodeCluster>() {
            Map<String, NucleotideSequence> previousGroups = null;
            PrimaryBarcodeCluster currentCluster = new PrimaryBarcodeCluster(new ArrayList<>(),
                    orderedPortIndex.getAndIncrement());
            boolean finished = false;

            @Override
            public synchronized PrimaryBarcodeCluster take() {
                if (finished)
                    return null;
                PrimaryBarcodeCluster preparedCluster = null;
                while (preparedCluster == null) {
                    CorrectionQualityPreprocessingResult preprocessingResult = preprocessorPort.take();
                    if (preprocessingResult != null) {
                        Map<String, NucleotideSequence> currentGroups = preprocessingResult.primaryGroups;
                        if (!currentGroups.equals(previousGroups)) {
                            if (previousGroups != null) {
                                preparedCluster = currentCluster;
                                currentCluster = new PrimaryBarcodeCluster(new ArrayList<>(),
                                        orderedPortIndex.getAndIncrement());
                            }
                            previousGroups = currentGroups;
                        }
                        currentCluster.preprocessingResults.add(preprocessingResult);
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

        OutputPort<CorrectionData> correctionDataUnorderedPort = new ParallelProcessor<>(clusterOutputPort,
                new PrimaryBarcodeClustersProcessor(correctionAlgorithmsInstance, keyGroups), threads);
        return new OrderedOutputPort<>(correctionDataUnorderedPort, data -> data.orderedPortIndex);
    }

    private static NSequenceWithQuality mergeSequences(List<NSequenceWithQuality> originalSequences) {
        if (originalSequences.size() < 2)
            throw new IllegalStateException("Trying to merge less than 2 sequences: " + originalSequences);
        int sequenceLength = originalSequences.get(0).size();
        ConsensusLetter[] consensusLetters = new ConsensusLetter[sequenceLength];
        for (int position = 0; position < sequenceLength; position++) {
            List<NSequenceWithQuality> currentPositionLetters = new ArrayList<>();
            for (NSequenceWithQuality originalSequence : originalSequences) {
                if (originalSequence.size() != sequenceLength)
                    throw new IllegalStateException("Trying to merge sequences of different sizes: "
                            + originalSequences);
                currentPositionLetters.add(originalSequence.getRange(position, position + 1));
            }
            consensusLetters[position] = new ConsensusLetter(currentPositionLetters);
        }
        NSequenceWithQualityBuilder builder = new NSequenceWithQualityBuilder();
        for (int position = 0; position < sequenceLength; position++)
            builder.append(consensusLetters[position].getConsensusLetter());
        return builder.createAndDestroy();
    }

    /**
     * Filter barcodes by count and fill sets of included barcodes (that were not filtered out) for each group.
     *
     * @param correctionData        data structures for each group: each contains sequence counters and stats
     *                              for the group and empty set of included barcodes that will be filled
     *                              in this function
     */
    private void filterByCount(CorrectionData correctionData) {
        // counting corrected barcodes by not corrected barcodes counts
        for (CorrectionGroupData groupData : correctionData.keyGroupsData.values()) {
            Map<NucleotideSequence, SequenceCounter> correctedCounters = new HashMap<>();
            for (Map.Entry<NucleotideSequence, SequenceCounter> barcodeValueEntry
                    : groupData.notCorrectedBarcodeCounters.entrySet()) {
                NucleotideSequence oldValue = barcodeValueEntry.getKey();
                long oldCount = barcodeValueEntry.getValue().count;
                NSequenceWithQuality correctedOldValue = groupData.correctionMap.get(oldValue);
                NucleotideSequence newValue = (correctedOldValue == null) ? oldValue : correctedOldValue.getSequence();
                SequenceCounter correctedSequenceCounter = correctedCounters.get(newValue);
                if (correctedSequenceCounter == null) {
                    SequenceCounter newCounter = new SequenceCounter(newValue);
                    newCounter.count = oldCount;
                    correctedCounters.put(newValue, newCounter);
                } else
                    correctedSequenceCounter.count += oldCount;
            }
            // filtering by count
            int maxUniqueBarcodesLimit = (maxUniqueBarcodes == 0) ? Integer.MAX_VALUE : maxUniqueBarcodes;
            new TreeSet<>(correctedCounters.values()).stream()
                    .limit(maxUniqueBarcodesLimit).filter(counter -> counter.count >= minCount)
                    .map(counter -> counter.seq).forEach(groupData.includedBarcodes::add);
        }
    }

    /**
     * Correct barcodes in the parsed read.
     *
     * @param parsedRead        original parsed read
     * @param correctionData    data structures for each group: each contains sequence counters, stats, correction map
     *                          and included barcodes set (if filtering by count is enabled) for the group
     * @return                  parsed read with corrected barcodes, number of corrected barcodes and excluded flag
     *                          (which is true if any of barcodes in this parsed read was filtered out by count)
     */
    CorrectBarcodesResult correctBarcodes(ParsedRead parsedRead, CorrectionData correctionData) {
        Map<String, NSequenceWithQuality> correctedGroups = new HashMap<>();
        boolean isCorrection = false;
        boolean excluded = false;
        for (Map.Entry<String, CorrectionGroupData> groupData : correctionData.keyGroupsData.entrySet()) {
            String groupName = groupData.getKey();
            CorrectionGroupData correctionGroupData = groupData.getValue();
            MatchedGroup matchedGroup = parsedRead.getGroupByName(groupName);
            NSequenceWithQuality oldValue = matchedGroup.getValue();
            NSequenceWithQuality correctValue = correctionGroupData.correctionMap.get(oldValue.getSequence());
            if (correctValue == null)
                correctValue = oldValue;
            isCorrection |= !correctValue.getSequence().equals(oldValue.getSequence());
            correctedGroups.put(groupName, correctValue);
            if (filterByCount)
                excluded |= !correctionGroupData.includedBarcodes.contains(correctValue.getSequence());
        }

        ArrayList<MatchedGroupEdge> newGroupEdges;
        if (!isCorrection)
            newGroupEdges = parsedRead.getMatchedGroupEdges();
        else {
            newGroupEdges = new ArrayList<>();
            Set<String> keyGroups = correctionData.keyGroupsData.keySet();
            for (MatchedGroupEdge matchedGroupEdge : parsedRead.getMatchedGroupEdges()) {
                String currentGroupName = matchedGroupEdge.getGroupEdge().getGroupName();
                if (!keyGroups.contains(currentGroupName))
                    newGroupEdges.add(matchedGroupEdge);
                else
                    newGroupEdges.add(new MatchedGroupEdge(matchedGroupEdge.getTarget(),
                            matchedGroupEdge.getTargetId(), matchedGroupEdge.getGroupEdge(),
                            correctedGroups.get(currentGroupName)));
            }
        }

        Set<String> defaultGroups = parsedRead.getDefaultGroupNames();
        int numberOfTargets = defaultGroups.size();
        Match newMatch = new Match(numberOfTargets, parsedRead.getBestMatchScore(), newGroupEdges);
        if (newMatch.getGroups().stream().map(MatchedGroup::getGroupName)
                .filter(defaultGroups::contains).count() != numberOfTargets)
            throw new IllegalStateException("Missing default groups in new Match: expected " + defaultGroups
                    + ", got " + newMatch.getGroups().stream().map(MatchedGroup::getGroupName)
                    .filter(defaultGroups::contains).collect(Collectors.toList()));
        return new CorrectBarcodesResult(new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(),
                parsedRead.getRawNumberOfTargetsOverride(), newMatch, 0), isCorrection,
                excluded);
    }

    static class CorrectBarcodesResult {
        final ParsedRead parsedRead;
        final boolean corrected;
        final boolean excluded;

        CorrectBarcodesResult(ParsedRead parsedRead, boolean corrected, boolean excluded) {
            this.parsedRead = parsedRead;
            this.corrected = corrected;
            this.excluded = excluded;
        }
    }

    private static class PrimaryBarcodeClustersProcessor implements Processor<PrimaryBarcodeCluster, CorrectionData> {
        final CorrectionAlgorithms correctionAlgorithmsInstance;
        final LinkedHashSet<String> keyGroups;

        PrimaryBarcodeClustersProcessor(
                CorrectionAlgorithms correctionAlgorithmsInstance, LinkedHashSet<String> keyGroups) {
            this.correctionAlgorithmsInstance = correctionAlgorithmsInstance;
            this.keyGroups = keyGroups;
        }

        @Override
        public CorrectionData process(PrimaryBarcodeCluster primaryBarcodeCluster) {
            OutputPort<CorrectionQualityPreprocessingResult> preprocessingResultsPort
                    = new OutputPort<CorrectionQualityPreprocessingResult>() {
                int resultIndex = 0;

                @Override
                public CorrectionQualityPreprocessingResult take() {
                    List<CorrectionQualityPreprocessingResult> results = primaryBarcodeCluster.preprocessingResults;
                    if (resultIndex == results.size())
                        return null;
                    return results.get(resultIndex++);
                }
            };
            return correctionAlgorithmsInstance.prepareCorrectionData(preprocessingResultsPort, keyGroups,
                    primaryBarcodeCluster.orderedPortIndex);
        }
    }
}
