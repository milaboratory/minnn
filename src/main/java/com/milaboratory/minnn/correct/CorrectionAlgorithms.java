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
import com.milaboratory.core.clustering.Clustering;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.minnn.io.MifReader;
import com.milaboratory.minnn.io.MifWriter;
import com.milaboratory.minnn.outputconverter.MatchedGroup;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.Match;
import com.milaboratory.minnn.pattern.MatchedGroupEdge;
import com.milaboratory.util.SmartProgressReporter;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.util.SystemUtils.*;

public final class CorrectionAlgorithms {
    private CorrectionAlgorithms() {}

    public static CorrectionStats fullFileCorrect(
            MifReader pass1Reader, MifReader pass2Reader, MifWriter writer, MifWriter excludedBarcodesWriter,
            long inputReadsLimit, BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory,
            LinkedHashSet<String> keyGroups, int maxUniqueBarcodes, int minCount) {
        boolean filterByCount = (maxUniqueBarcodes > 0) || (minCount > 1);
        Set<GroupData> groupsData = keyGroups.stream().map(groupName -> new GroupData(groupName, filterByCount,
                barcodeClusteringStrategyFactory.averageBarcodeLengthRequired())).collect(Collectors.toSet());
        long totalReads = 0;
        long correctedReads = 0;
        long excludedReads = 0;

        // 1st pass: counting barcodes and initializing clustering strategy
        SmartProgressReporter.startProgressReport("Counting sequences", pass1Reader, System.err);
        for (ParsedRead parsedRead : CUtils.it(pass1Reader)) {
            // get sequences from parsed read and fill sequence maps
            for (GroupData groupData : groupsData)
                groupData.processSequence(parsedRead.getGroupValue(groupData.groupName));
            if (++totalReads == inputReadsLimit)
                break;
        }

        // clustering and filling barcode correction maps inside groupsData
        performClustering(groupsData, barcodeClusteringStrategyFactory, true);

        if (filterByCount) {
            System.err.println("Filtering corrected barcodes by count...");
            // calculating which barcodes must be included or excluded, saving results to groupsData
            filterByCount(groupsData, maxUniqueBarcodes, minCount);
        }

        // 2nd pass: correcting barcodes
        totalReads = 0;
        SmartProgressReporter.startProgressReport("Correcting barcodes", pass2Reader, System.err);
        for (ParsedRead parsedRead : CUtils.it(pass2Reader)) {
            CorrectBarcodesResult correctBarcodesResult = correctBarcodes(parsedRead, groupsData);
            correctedReads += correctBarcodesResult.numCorrectedBarcodes;
            if (correctBarcodesResult.excluded) {
                if (excludedBarcodesWriter != null)
                    excludedBarcodesWriter.write(correctBarcodesResult.parsedRead);
                excludedReads++;
            } else
                writer.write(correctBarcodesResult.parsedRead);
            if (++totalReads == inputReadsLimit)
                break;
        }
        return new CorrectionStats(totalReads, correctedReads, excludedReads);
    }

    public static CorrectionStats sortedClustersCorrect(
            MifReader reader, MifWriter writer, MifWriter excludedBarcodesWriter, long inputReadsLimit,
            BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory,
            LinkedHashSet<String> primaryGroups, LinkedHashSet<String> keyGroups,
            int maxUniqueBarcodes, int minCount) {
        AtomicLong totalReads = new AtomicLong(0);
        long correctedReads = 0;
        long excludedReads = 0;

        SmartProgressReporter.startProgressReport("Counting sequences", reader, System.err);
        OutputPort<List<ParsedRead>> clustersOutputPort = new OutputPort<List<ParsedRead>>() {
            LinkedHashMap<String, NucleotideSequence> previousGroups = null;
            List<ParsedRead> currentCluster = new ArrayList<>();
            boolean finished = false;

            @Override
            public List<ParsedRead> take() {
                if (finished)
                    return null;
                List<ParsedRead> preparedCluster = null;
                while (preparedCluster == null) {
                    ParsedRead parsedRead = ((inputReadsLimit == 0) || (totalReads.get() < inputReadsLimit))
                            ? reader.take() : null;
                    if (parsedRead != null) {
                        LinkedHashMap<String, NucleotideSequence> currentGroups = extractPrimaryBarcodes(parsedRead,
                                primaryGroups);
                        if (!currentGroups.equals(previousGroups)) {
                            if (previousGroups != null) {
                                preparedCluster = currentCluster;
                                currentCluster = new ArrayList<>();
                            }
                            previousGroups = currentGroups;
                        }
                        currentCluster.add(parsedRead);
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

        boolean correctionStarted = false;
        for (List<ParsedRead> cluster : CUtils.it(clustersOutputPort)) {
            if (!correctionStarted) {
                SmartProgressReporter.startProgressReport("Correcting barcodes", writer, System.err);
                correctionStarted = true;
            }
            ClusterStats stats = processCluster(cluster, writer, excludedBarcodesWriter,
                    barcodeClusteringStrategyFactory, keyGroups, maxUniqueBarcodes, minCount);
            correctedReads += stats.correctedReads;
            excludedReads += stats.excludedReads;
        }

        return new CorrectionStats(totalReads.get(), correctedReads, excludedReads);
    }

    public static CorrectionStats unsortedClustersCorrect(
            MifReader reader, MifWriter writer, MifWriter excludedBarcodesWriter, long inputReadsLimit,
            BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory,
            LinkedHashSet<String> primaryGroups, LinkedHashSet<String> keyGroups,
            int maxUniqueBarcodes, int minCount) {
        // keys: primary barcodes values; values: all reads that have this combination of barcodes values
        HashMap<LinkedHashMap<String, NucleotideSequence>, List<ParsedRead>> allClusters = new HashMap<>();
        long totalReads = 0;
        long correctedReads = 0;
        long excludedReads = 0;

        // reading the entire input file into memory and clustering by primary barcodes values
        SmartProgressReporter.startProgressReport("Reading input file into memory", reader, System.err);
        for (ParsedRead parsedRead : CUtils.it(reader)) {
            LinkedHashMap<String, NucleotideSequence> primaryBarcodes = extractPrimaryBarcodes(parsedRead,
                    primaryGroups);
            allClusters.putIfAbsent(primaryBarcodes, new ArrayList<>());
            allClusters.get(primaryBarcodes).add(parsedRead);
            if (++totalReads == inputReadsLimit)
                break;
        }

        boolean correctionStarted = false;
        for (List<ParsedRead> cluster : allClusters.values()) {
            if (!correctionStarted) {
                SmartProgressReporter.startProgressReport("Correcting barcodes", writer, System.err);
                correctionStarted = true;
            }
            ClusterStats stats = processCluster(cluster, writer, excludedBarcodesWriter,
                    barcodeClusteringStrategyFactory, keyGroups, maxUniqueBarcodes, minCount);
            correctedReads += stats.correctedReads;
            excludedReads += stats.excludedReads;
        }

        return new CorrectionStats(totalReads, correctedReads, excludedReads);
    }

    /**
     * Extract values of primary barcodes from parsed read.
     *
     * @param parsedRead        parsed read
     * @param primaryGroups     names of primary groups
     * @return                  keys: names of primary groups; values: values (sequences) of primary groups
     */
    private static LinkedHashMap<String, NucleotideSequence> extractPrimaryBarcodes(
            ParsedRead parsedRead, LinkedHashSet<String> primaryGroups) {
        Set<String> allGroups = parsedRead.getNotDefaultGroups().stream().map(MatchedGroup::getGroupName)
                .collect(Collectors.toSet());
        for (String groupName : primaryGroups)
            if (!allGroups.contains(groupName))
                throw exitWithError("Group " + groupName + " not found in the input!");
        return parsedRead.getGroups().stream().filter(g -> primaryGroups.contains(g.getGroupName()))
                .collect(LinkedHashMap::new, (m, g) -> m.put(g.getGroupName(), g.getValue().getSequence()),
                        Map::putAll);
    }

    /**
     * Sort nucleotide sequences by count in each group and perform clustering.
     *
     * @param groupsData                        data structures for each group: each contains sequence counters
     *                                          and stats for the group and empty correction map that will be filled
     *                                          in this function
     * @param barcodeClusteringStrategyFactory  clustering strategy factory that can calculate clustering strategy
     *                                          parameters from collected stats for the group
     * @param reportProgress                    report clustering progress; must be used only with full file correction
     */
    private static void performClustering(
            Set<GroupData> groupsData, BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory,
            boolean reportProgress) {
        SequenceCounterExtractor sequenceCounterExtractor = new SequenceCounterExtractor();
        for (GroupData groupData : groupsData) {
            if (groupData.parsedReadsCount > 0) {
                Clustering<SequenceCounter, SequenceWithQualityForClustering> clustering = new Clustering<>(
                        groupData.getSequenceCounters(), sequenceCounterExtractor,
                        barcodeClusteringStrategyFactory.createStrategy(
                                (float)(groupData.lengthSum) / groupData.parsedReadsCount),
                        MutationGuideForClustering.INSTANCE);
                if (reportProgress)
                    SmartProgressReporter.startProgressReport("Clustering barcodes in group "
                                    + groupData.groupName, clustering, System.err);
                clustering.performClustering().forEach(cluster -> {
                    NSequenceWithQuality headSequence = cluster.getHead().getSequence();
                    cluster.processAllChildren(child -> {
                        child.getHead().getSequences().forEach(seq ->
                                groupData.correctionMap.put(seq.getSequence(), headSequence.getSequence()));
                        return true;
                    });
                });
            }
        }
    }

    /**
     * Filter barcodes by count and fill sets of included barcodes (that were not filtered out) for each group.
     *
     * @param groupsData            data structures for each group: each contains sequence counters and stats
     *                              for the group and empty set of included barcodes that will be filled
     *                              in this function
     * @param maxUniqueBarcodes     maximal number of included unique barcodes for each group
     * @param minCount              minimal count of unique barcode, barcodes with lower counts will not be included
     */
    private static void filterByCount(Set<GroupData> groupsData, int maxUniqueBarcodes, int minCount) {
        // counting corrected barcodes by not corrected barcodes counts
        for (GroupData groupData : groupsData) {
            Map<NucleotideSequence, RawSequenceCounter> correctedCounters = new HashMap<>();
            for (Map.Entry<NucleotideSequence, RawSequenceCounter> barcodeValueEntry
                    : groupData.notCorrectedBarcodeCounters.entrySet()) {
                NucleotideSequence oldValue = barcodeValueEntry.getKey();
                long oldCount = barcodeValueEntry.getValue().count;
                NucleotideSequence newValue = groupData.correctionMap.get(oldValue);
                if (newValue == null)
                    newValue = oldValue;
                RawSequenceCounter correctedSequenceCounter = correctedCounters.get(newValue);
                if (correctedSequenceCounter == null) {
                    RawSequenceCounter newCounter = new RawSequenceCounter(newValue);
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
     * @param parsedRead    original parsed read
     * @param groupsData    data structures for each group: each contains sequence counters, stats, correction map
     *                      and included barcodes set (if filtering by count is enabled) for the group
     * @return              parsed read with corrected barcodes, number of corrected barcodes and excluded flag
     *                      (which is true if any of barcodes in this parsed read was filtered out by count)
     */
    private static CorrectBarcodesResult correctBarcodes(ParsedRead parsedRead, Set<GroupData> groupsData) {
        ArrayList<CorrectedGroup> correctedGroups = new ArrayList<>();
        boolean isCorrection = false;
        int numCorrectedBarcodes = 0;
        boolean excluded = false;
        for (GroupData groupData : groupsData) {
            MatchedGroup matchedGroup = parsedRead.getGroupByName(groupData.groupName);
            NucleotideSequence oldValue = matchedGroup.getValue().getSequence();
            NucleotideSequence correctValue = groupData.correctionMap.get(oldValue);
            if (correctValue == null)
                correctValue = oldValue;
            isCorrection |= !correctValue.equals(oldValue);
            correctedGroups.add(new CorrectedGroup(groupData.groupName, correctValue));
            if (groupData.filterByCount)
                excluded |= !groupData.includedBarcodes.contains(correctValue);
        }

        ArrayList<MatchedGroupEdge> newGroupEdges;
        if (!isCorrection)
            newGroupEdges = parsedRead.getMatchedGroupEdges();
        else {
            newGroupEdges = new ArrayList<>();
            Set<String> keyGroups = groupsData.stream().map(data -> data.groupName).collect(Collectors.toSet());
            Map<String, CorrectedGroup> correctedGroupsMap = correctedGroups.stream()
                    .collect(Collectors.toMap(cg -> cg.groupName, cg -> cg));
            for (MatchedGroupEdge matchedGroupEdge : parsedRead.getMatchedGroupEdges()) {
                String currentGroupName = matchedGroupEdge.getGroupEdge().getGroupName();
                if (!keyGroups.contains(currentGroupName))
                    newGroupEdges.add(matchedGroupEdge);
                else {
                    CorrectedGroup correctedGroup = correctedGroupsMap.get(currentGroupName);
                    newGroupEdges.add(new MatchedGroupEdge(matchedGroupEdge.getTarget(),
                            matchedGroupEdge.getTargetId(), matchedGroupEdge.getGroupEdge(),
                            new NSequenceWithQuality(correctedGroup.correctedValue)));
                }
            }
            numCorrectedBarcodes++;
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
                parsedRead.getRawNumberOfTargetsOverride(), newMatch, 0), numCorrectedBarcodes,
                excluded);
    }

    /**
     * Correct barcodes in cluster (for correction with primary barcodes) and write corrected cluster to output file.
     *
     * @param cluster                           cluster: list of parsed reads with the same primary barcodes
     * @param writer                            MifWriter for output file
     * @param excludedBarcodesWriter            MifWriter for excluded barcodes output file
     * @param barcodeClusteringStrategyFactory  clustering strategy factory that can calculate clustering strategy
     *                                          parameters from collected stats for the group
     * @param keyGroups                         group names in which we will correct barcodes
     * @param maxUniqueBarcodes                 maximal number of included unique barcodes for each group,
     *                                          for filtering corrected barcodes by count
     * @param minCount                          minimal count of unique barcode, barcodes with lower counts will not
     *                                          be included
     * @return                                  stats for cluster: number of corrected reads and number
     *                                          of excluded reads
     */
    private static ClusterStats processCluster(
            List<ParsedRead> cluster, MifWriter writer, MifWriter excludedBarcodesWriter,
            BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory, LinkedHashSet<String> keyGroups,
            int maxUniqueBarcodes, int minCount) {
        boolean filterByCount = (maxUniqueBarcodes > 0) || (minCount > 1);
        Set<GroupData> groupsData = keyGroups.stream().map(groupName -> new GroupData(groupName, filterByCount,
                barcodeClusteringStrategyFactory.averageBarcodeLengthRequired())).collect(Collectors.toSet());
        long correctedReads = 0;
        long excludedReads = 0;

        // counting barcodes and calculating statistics for clustering strategy factory
        cluster.forEach(parsedRead -> groupsData.forEach(groupData ->
                groupData.processSequence(parsedRead.getGroupValue(groupData.groupName))));

        // clustering and filling barcode correction maps inside groupsData
        performClustering(groupsData, barcodeClusteringStrategyFactory, false);

        // calculating which barcodes must be included or excluded, saving results to groupsData
        if (filterByCount)
            filterByCount(groupsData, maxUniqueBarcodes, minCount);

        for (ParsedRead parsedRead : cluster) {
            CorrectBarcodesResult correctBarcodesResult = correctBarcodes(parsedRead, groupsData);
            correctedReads += correctBarcodesResult.numCorrectedBarcodes;
            if (correctBarcodesResult.excluded) {
                if (excludedBarcodesWriter != null)
                    excludedBarcodesWriter.write(correctBarcodesResult.parsedRead);
                excludedReads++;
            } else
                writer.write(correctBarcodesResult.parsedRead);
        }

        return new ClusterStats(correctedReads, excludedReads);
    }

    private static class GroupData {
        final String groupName;
        final boolean filterByCount;
        final boolean averageBarcodeLengthRequired;
        final List<SequenceCounter> sequenceCounters = new ArrayList<>();
        final Map<NucleotideSequence, RawSequenceCounter> notCorrectedBarcodeCounters;
        // keys: not corrected sequences, values: corrected sequences
        final Map<NucleotideSequence, NucleotideSequence> correctionMap = new HashMap<>();
        // barcodes that are not filtered out if filtering by count is enabled
        final Set<NucleotideSequence> includedBarcodes;
        long lengthSum = 0;
        long parsedReadsCount = 0;

        GroupData(String groupName, boolean filterByCount, boolean averageBarcodeLengthRequired) {
            this.groupName = groupName;
            this.filterByCount = filterByCount;
            this.averageBarcodeLengthRequired = averageBarcodeLengthRequired;
            this.notCorrectedBarcodeCounters = filterByCount ? new HashMap<>() : null;
            this.includedBarcodes = filterByCount ? new HashSet<>() : null;
        }

        void processSequence(NSequenceWithQuality seqWithQuality) {
            // try to add the sequence to any of the existing counters or create new counter
            boolean matchingCounterFound = false;
            for (SequenceCounter counter : sequenceCounters)
                if (counter.add(seqWithQuality)) {
                    matchingCounterFound = true;
                    break;
                }
            if (!matchingCounterFound)
                sequenceCounters.add(new SequenceCounter(seqWithQuality));

            // counting raw barcode sequences if filtering by count is enabled
            if (filterByCount) {
                NucleotideSequence seq = seqWithQuality.getSequence();
                notCorrectedBarcodeCounters.putIfAbsent(seq, new RawSequenceCounter(seq));
                notCorrectedBarcodeCounters.get(seq).count++;
            }

            if (averageBarcodeLengthRequired)
                lengthSum += seqWithQuality.size();

            parsedReadsCount++;
        }

        List<SequenceCounter> getSequenceCounters() {
            return sequenceCounters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if ((o == null) || (getClass() != o.getClass())) return false;
            GroupData groupData = (GroupData)o;
            return Objects.equals(groupName, groupData.groupName);
        }

        @Override
        public int hashCode() {
            return groupName.hashCode();
        }
    }

    private static class ClusterStats {
        final long correctedReads;
        final long excludedReads;

        ClusterStats(long correctedReads, long excludedReads) {
            this.correctedReads = correctedReads;
            this.excludedReads = excludedReads;
        }
    }

    private static class CorrectBarcodesResult {
        final ParsedRead parsedRead;
        final int numCorrectedBarcodes;
        final boolean excluded;

        CorrectBarcodesResult(ParsedRead parsedRead, int numCorrectedBarcodes, boolean excluded) {
            this.parsedRead = parsedRead;
            this.numCorrectedBarcodes = numCorrectedBarcodes;
            this.excluded = excluded;
        }
    }

    private static class CorrectedGroup {
        final String groupName;
        final NucleotideSequence correctedValue;

        CorrectedGroup(String groupName, NucleotideSequence correctedValue) {
            this.groupName = groupName;
            this.correctedValue = correctedValue;
        }
    }
}
