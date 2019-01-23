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
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.core.clustering.Cluster;
import com.milaboratory.core.clustering.Clustering;
import com.milaboratory.core.clustering.ClusteringStrategy;
import com.milaboratory.core.clustering.SequenceExtractor;
import com.milaboratory.core.mutations.Mutations;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.Wildcard;
import com.milaboratory.core.tree.NeighborhoodIterator;
import com.milaboratory.core.tree.TreeSearchParameters;
import com.milaboratory.minnn.outputconverter.MatchedGroup;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.Match;
import com.milaboratory.minnn.pattern.MatchedGroupEdge;
import com.milaboratory.minnn.pattern.MatchedItem;
import com.milaboratory.minnn.stat.MutationProbability;
import com.milaboratory.minnn.stat.SimpleMutationProbability;
import com.milaboratory.util.SmartProgressReporter;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.cli.CliUtils.floatFormat;
import static com.milaboratory.minnn.util.SequencesCache.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class CorrectBarcodesIO {
    private final PipelineConfiguration pipelineConfiguration;
    private final String inputFileName;
    private final String outputFileName;
    private final int mismatches;
    private final int indels;
    private final int totalErrors;
    private final float threshold;
    private final List<String> groupNames;
    private final int maxClusterDepth;
    private final MutationProbability mutationProbability;
    private final int minCount;
    private final String excludedBarcodesOutputFileName;
    private final long inputReadsLimit;
    private final boolean suppressWarnings;
    private Set<String> defaultGroups;
    private LinkedHashSet<String> keyGroups;
    private Map<String, HashMap<NucleotideSequence, NucleotideSequence>> sequenceCorrectionMaps;
    private Map<String, TObjectLongHashMap<NucleotideSequence>> correctedBarcodeCounters;
    private int numberOfTargets;
    private AtomicLong corrected = new AtomicLong(0);

    public CorrectBarcodesIO(PipelineConfiguration pipelineConfiguration, String inputFileName, String outputFileName,
                             int mismatches, int indels, int totalErrors, float threshold, List<String> groupNames,
                             int maxClusterDepth, float singleSubstitutionProbability, float singleIndelProbability,
                             int minCount, String excludedBarcodesOutputFileName, long inputReadsLimit,
                             boolean suppressWarnings) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.mismatches = mismatches;
        this.indels = indels;
        this.totalErrors = totalErrors;
        this.threshold = threshold;
        this.groupNames = groupNames;
        this.maxClusterDepth = maxClusterDepth;
        this.mutationProbability = new SimpleMutationProbability(singleSubstitutionProbability, singleIndelProbability);
        this.minCount = minCount;
        this.excludedBarcodesOutputFileName = excludedBarcodesOutputFileName;
        this.inputReadsLimit = inputReadsLimit;
        this.suppressWarnings = suppressWarnings;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        long excludedReads = 0;
        try (MifReader initialReader = new MifReader(inputFileName);
             MifReader barcodeCountReader = (minCount > 0) ? new MifReader(inputFileName) : null;
             MifReader finalReader = new MifReader(inputFileName);
             MifWriter writer = Objects.requireNonNull(createWriter(initialReader.getHeader(), false));
             MifWriter excludedBarcodesWriter = createWriter(initialReader.getHeader(), true)) {
            if (inputReadsLimit > 0) {
                initialReader.setParsedReadsLimit(inputReadsLimit);
                finalReader.setParsedReadsLimit(inputReadsLimit);
            }
            SmartProgressReporter.startProgressReport("Counting sequences", initialReader, System.err);
            defaultGroups = IntStream.rangeClosed(1, initialReader.getNumberOfTargets())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());
            if (groupNames.stream().anyMatch(defaultGroups::contains))
                throw exitWithError("Default groups R1, R2, etc should not be specified for correction!");
            keyGroups = new LinkedHashSet<>(groupNames);
            if (!suppressWarnings && initialReader.isSorted())
                System.err.println("WARNING: correcting sorted MIF file; output file will be unsorted!");
            List<String> correctedAgainGroups = keyGroups.stream().filter(gn -> initialReader.getCorrectedGroups()
                    .stream().anyMatch(gn::equals)).collect(Collectors.toList());
            if (!suppressWarnings && (correctedAgainGroups.size() != 0))
                System.err.println("WARNING: group(s) " + correctedAgainGroups + " already corrected and will be " +
                        "corrected again!");
            Map<String, HashMap<NucleotideSequence, SequenceCounter>> sequenceMaps = keyGroups.stream()
                    .collect(Collectors.toMap(groupName -> groupName, groupName -> new HashMap<>()));
            numberOfTargets = initialReader.getNumberOfTargets();
            for (ParsedRead parsedRead : CUtils.it(initialReader)) {
                for (Map.Entry<String, HashMap<NucleotideSequence, SequenceCounter>> entry : sequenceMaps.entrySet()) {
                    NucleotideSequence groupValue = parsedRead.getGroupValue(entry.getKey()).getSequence();
                    SequenceCounter counter = entry.getValue().get(groupValue);
                    if (counter == null)
                        entry.getValue().put(groupValue, new SequenceCounter(groupValue));
                    else
                        counter.count++;
                }
                if (++totalReads == inputReadsLimit)
                    break;
            }
            totalReads = 0;

            // sorting nucleotide sequences by count in each group and performing clustering
            SequenceCounterExtractor sequenceCounterExtractor = new SequenceCounterExtractor();
            BarcodeClusteringStrategy barcodeClusteringStrategy = new BarcodeClusteringStrategy();
            sequenceCorrectionMaps = new HashMap<>();
            for (Map.Entry<String, HashMap<NucleotideSequence, SequenceCounter>> entry : sequenceMaps.entrySet()) {
                TreeSet<SequenceCounter> sortedSequences = new TreeSet<>(entry.getValue().values());
                Clustering<SequenceCounter, NucleotideSequence> clustering = new Clustering<>(sortedSequences,
                        sequenceCounterExtractor, barcodeClusteringStrategy);
                SmartProgressReporter.startProgressReport("Clustering barcodes in group " + entry.getKey(),
                        clustering, System.err);
                HashMap<NucleotideSequence, NucleotideSequence> currentCorrectionMap = new HashMap<>();
                clustering.performClustering().forEach(cluster -> {
                    NucleotideSequence headSequence = cluster.getHead().multiSequence.getBestSequence();
                    cluster.processAllChildren(child -> {
                        child.getHead().multiSequence.sequences.keySet().forEach(seq -> currentCorrectionMap.put(seq,
                                headSequence));
                        return true;
                    });
                });
                sequenceCorrectionMaps.put(entry.getKey(), currentCorrectionMap);
            }

            // intermediate pass, only if filtering by count is required: calculating counts for corrected barcodes
            if (minCount > 0) {
                correctedBarcodeCounters = new HashMap<>();
                SmartProgressReporter.startProgressReport("Counting barcodes", barcodeCountReader, System.err);
                for (ParsedRead parsedRead : CUtils.it(barcodeCountReader))
                    updateBarcodeCounters(parsedRead);
            }

            // final pass: correcting barcodes
            SmartProgressReporter.startProgressReport("Correcting barcodes", finalReader, System.err);
            for (ParsedRead parsedRead : CUtils.it(finalReader)) {
                CorrectBarcodesResult correctBarcodesResult = correctBarcodes(parsedRead);
                if (correctBarcodesResult.excluded) {
                    if (excludedBarcodesWriter != null)
                        excludedBarcodesWriter.write(correctBarcodesResult.parsedRead);
                    excludedReads++;
                } else
                    writer.write(correctBarcodesResult.parsedRead);
                if (++totalReads == inputReadsLimit)
                    break;
            }
            finalReader.close();
            writer.setOriginalNumberOfReads(finalReader.getOriginalNumberOfReads());
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads");
        float percent = (totalReads == 0) ? 0 : (float)corrected.get() / totalReads * 100;
        System.err.println("Reads with corrected barcodes: " + corrected
                + " (" + floatFormat.format(percent) + "%)");
        if (excludedReads > 0)
            System.err.println("Reads excluded by too low barcode count: " + excludedReads
                    + " (" + floatFormat.format((float)excludedReads / totalReads * 100) + "%)\n");
        else
            System.err.println();
    }

    private MifWriter createWriter(MifHeader inputHeader, boolean excludedBarcodes) throws IOException {
        LinkedHashSet<String> allCorrectedGroups = new LinkedHashSet<>(inputHeader.getCorrectedGroups());
        allCorrectedGroups.addAll(groupNames);
        MifHeader outputHeader = new MifHeader(pipelineConfiguration, inputHeader.getNumberOfTargets(),
                new ArrayList<>(allCorrectedGroups), false, inputHeader.getGroupEdges());
        if (excludedBarcodes)
            return (excludedBarcodesOutputFileName == null) ? null
                    : new MifWriter(excludedBarcodesOutputFileName, outputHeader);
        else
            return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                    : new MifWriter(outputFileName, outputHeader);
    }

    private void updateBarcodeCounters(ParsedRead parsedRead) {
        for (Map.Entry<String, MatchedGroup> entry : extractMatchedGroups(parsedRead).entrySet()) {
            String groupName = entry.getKey();
            correctedBarcodeCounters.computeIfAbsent(groupName, n -> new TObjectLongHashMap<>());
            TObjectLongHashMap<NucleotideSequence> currentGroupBarcodes = correctedBarcodeCounters.get(groupName);
            MatchedGroup matchedGroup = entry.getValue();
            NucleotideSequence oldValue = matchedGroup.getValue().getSequence();
            NucleotideSequence correctValue = sequenceCorrectionMaps.get(groupName).get(oldValue);
            if (correctValue == null)
                correctValue = oldValue;
            currentGroupBarcodes.adjustOrPutValue(correctValue, 1, 0);
        }
    }

    private CorrectBarcodesResult correctBarcodes(ParsedRead parsedRead) {
        HashMap<Byte, ArrayList<CorrectedGroup>> correctedGroups = new HashMap<>();
        boolean isCorrection = false;
        boolean excluded = false;
        for (Map.Entry<String, MatchedGroup> entry : extractMatchedGroups(parsedRead).entrySet()) {
            String groupName = entry.getKey();
            MatchedGroup matchedGroup = entry.getValue();
            byte targetId = matchedGroup.getTargetId();
            NucleotideSequence oldValue = matchedGroup.getValue().getSequence();
            NucleotideSequence correctValue = sequenceCorrectionMaps.get(groupName).get(oldValue);
            if (correctValue == null)
                correctValue = oldValue;
            isCorrection |= !correctValue.equals(oldValue);
            correctedGroups.computeIfAbsent(targetId, id -> new ArrayList<>());
            correctedGroups.get(targetId).add(new CorrectedGroup(groupName, correctValue));
            if (minCount > 0)
                if (correctedBarcodeCounters.get(groupName).get(correctValue) < minCount)
                    excluded = true;
        }

        ArrayList<MatchedGroupEdge> newGroupEdges;
        if (!isCorrection)
            newGroupEdges = parsedRead.getMatchedGroupEdges();
        else {
            newGroupEdges = new ArrayList<>();
            for (byte targetId : parsedRead.getGroups().stream().map(MatchedItem::getTargetId)
                    .collect(Collectors.toCollection(LinkedHashSet::new))) {
                ArrayList<CorrectedGroup> currentCorrectedGroups = correctedGroups.get(targetId);
                if (currentCorrectedGroups == null)
                    parsedRead.getMatchedGroupEdges().stream()
                            .filter(mge -> mge.getTargetId() == targetId).forEach(newGroupEdges::add);
                else {
                    Map<String, CorrectedGroup> currentCorrectedGroupsMap = currentCorrectedGroups.stream()
                            .collect(Collectors.toMap(cg -> cg.groupName, cg -> cg));
                    for (MatchedGroupEdge matchedGroupEdge : parsedRead.getMatchedGroupEdges().stream()
                            .filter(mge -> mge.getTargetId() == targetId).collect(Collectors.toList())) {
                        String currentGroupName = matchedGroupEdge.getGroupEdge().getGroupName();
                        if (!keyGroups.contains(currentGroupName))
                            newGroupEdges.add(matchedGroupEdge);
                        else {
                            CorrectedGroup currentCorrectedGroup = currentCorrectedGroupsMap.get(currentGroupName);
                            newGroupEdges.add(new MatchedGroupEdge(matchedGroupEdge.getTarget(),
                                    matchedGroupEdge.getTargetId(), matchedGroupEdge.getGroupEdge(),
                                    new NSequenceWithQuality(currentCorrectedGroup.correctedValue)));
                        }
                    }
                }
            }
            corrected.getAndIncrement();
        }

        Match newMatch = new Match(numberOfTargets, parsedRead.getBestMatchScore(), newGroupEdges);
        if (newMatch.getGroups().stream().map(MatchedGroup::getGroupName)
                .filter(defaultGroups::contains).count() != numberOfTargets)
            throw new IllegalStateException("Missing default groups in new Match: expected " + defaultGroups
                    + ", got " + newMatch.getGroups().stream().map(MatchedGroup::getGroupName)
                    .filter(defaultGroups::contains).collect(Collectors.toList()));
        return new CorrectBarcodesResult(new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(),
                newMatch, 0), excluded);
    }

    /**
     * Extract from parsed read all matched groups that must be corrected and map them to their group names.
     *
     * @param parsedRead    parsed read
     * @return              map keys: group names,
     *                      map values: MatchedGroup structures containing nucleotide sequence and targetId
     */
    private Map<String, MatchedGroup> extractMatchedGroups(ParsedRead parsedRead) {
        return parsedRead.getGroups().stream().filter(group -> keyGroups.contains(group.getGroupName()))
                .collect(Collectors.toMap(MatchedGroup::getGroupName, group -> group));
    }

    private static class CorrectBarcodesResult {
        final ParsedRead parsedRead;
        final boolean excluded;

        CorrectBarcodesResult(ParsedRead parsedRead, boolean excluded) {
            this.parsedRead = parsedRead;
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

    private static class MultiSequence {
        final HashMap<NucleotideSequence, Long> sequences = new HashMap<>();

        MultiSequence(NucleotideSequence seq) {
            long variability = 1;
            for (int i = 0; i < seq.size(); i++)
                variability *= charToWildcard.get(seq.symbolAt(i)).basicSize();
            sequences.put(seq, variability);
        }

        /**
         * @return sequence with smallest variability by wildcards
         */
        NucleotideSequence getBestSequence() {
            Map.Entry<NucleotideSequence, Long> bestEntry = null;
            for (Map.Entry<NucleotideSequence, Long> entry : sequences.entrySet())
                if ((bestEntry == null) || (bestEntry.getValue() > entry.getValue()))
                    bestEntry = entry;
            return Objects.requireNonNull(bestEntry).getKey();
        }

        private boolean equalByWildcards(NucleotideSequence seq1, NucleotideSequence seq2) {
            if (seq1.size() != seq2.size())
                return false;
            for (int i = 0; i < seq1.size(); i++) {
                Wildcard wildcard1 = charToWildcard.get(seq1.symbolAt(i));
                Wildcard wildcard2 = charToWildcard.get(seq2.symbolAt(i));
                if ((wildcard1.getBasicMask() & wildcard2.getBasicMask()) == 0)
                    return false;
            }
            return true;
        }

        /**
         * Important! Call to equals() will perform mutual merge if MultiSequences are equal by wildcards!
         *
         * @param o     other MultiSequence
         * @return      true if MultiSequences are equal by wildcards
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MultiSequence that = (MultiSequence)o;
            if (sequences.keySet().parallelStream().allMatch(seq1 -> that.sequences.keySet().stream()
                    .allMatch(seq2 -> equalByWildcards(seq1, seq2)))) {
                sequences.putAll(that.sequences);
                that.sequences.putAll(sequences);
                return true;
            } else
                return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static class SequenceCounter implements Comparable<SequenceCounter> {
        final MultiSequence multiSequence;
        long count;

        SequenceCounter(NucleotideSequence sequence) {
            multiSequence = new MultiSequence(sequence);
            count = 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SequenceCounter that = (SequenceCounter)o;
            // MultiSequence objects will perform mutual merge if equal
            return multiSequence.equals(that.multiSequence);
        }

        @Override
        public int hashCode() {
            return 0;
        }

        // compareTo is reversed to start from bigger counts
        @Override
        public int compareTo(SequenceCounter other) {
            int comparisonResult = -Long.compare(count, other.count);
            // disable equal counts because they lead to objects loss
            return (comparisonResult == 0) ? 1 : comparisonResult;
        }
    }

    private static class SequenceCounterExtractor implements SequenceExtractor<SequenceCounter, NucleotideSequence> {
        @Override
        public NucleotideSequence getSequence(SequenceCounter sequenceCounter) {
            return sequenceCounter.multiSequence.getBestSequence();
        }
    }

    private class BarcodeClusteringStrategy implements ClusteringStrategy<SequenceCounter, NucleotideSequence> {
        private final TreeSearchParameters treeSearchParameters = new TreeSearchParameters(mismatches, indels, indels,
                totalErrors);

        @Override
        public boolean canAddToCluster(Cluster<SequenceCounter> cluster, SequenceCounter minorSequenceCounter,
                                       NeighborhoodIterator<NucleotideSequence, SequenceCounter[]> iterator) {
            Mutations<NucleotideSequence> currentMutations = iterator.getCurrentMutations();
            long majorClusterCount = cluster.getHead().count;
            long minorClusterCount = minorSequenceCounter.count;
            float expected = majorClusterCount;
            for (int mutationCode : currentMutations.getRAWMutations())
                expected *= mutationProbability.mutationProbability(mutationCode);
            return (minorClusterCount <= expected) && ((float)minorClusterCount / majorClusterCount < threshold);
        }

        @Override
        public TreeSearchParameters getSearchParameters() {
            return treeSearchParameters;
        }

        @Override
        public int getMaxClusterDepth() {
            return maxClusterDepth;
        }

        @Override
        public int compare(SequenceCounter c1, SequenceCounter c2) {
            return Long.compare(c1.count, c2.count);
        }
    }
}
