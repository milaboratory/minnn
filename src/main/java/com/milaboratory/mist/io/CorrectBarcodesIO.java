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
package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import com.milaboratory.core.clustering.Cluster;
import com.milaboratory.core.clustering.Clustering;
import com.milaboratory.core.clustering.ClusteringStrategy;
import com.milaboratory.core.clustering.SequenceExtractor;
import com.milaboratory.core.mutations.Mutations;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.tree.NeighborhoodIterator;
import com.milaboratory.core.tree.TreeSearchParameters;
import com.milaboratory.mist.outputconverter.MatchedGroup;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.Match;
import com.milaboratory.mist.pattern.MatchedGroupEdge;
import com.milaboratory.mist.pattern.MatchedItem;
import com.milaboratory.mist.stat.MutationProbability;
import com.milaboratory.mist.stat.SimpleMutationProbability;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.mist.cli.CliUtils.floatFormat;
import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class CorrectBarcodesIO {
    private final String inputFileName;
    private final String outputFileName;
    private final int mismatches;
    private final int indels;
    private final int totalErrors;
    private final float threshold;
    private final List<String> groupNames;
    private final int maxClusterDepth;
    private final MutationProbability mutationProbability;
    private final long inputReadsLimit;
    private final boolean suppressWarnings;
    private Set<String> defaultGroups;
    private LinkedHashSet<String> keyGroups;
    private Map<String, HashMap<NucleotideSequence, NucleotideSequence>> sequenceCorrectionMaps;
    private int numberOfReads;
    private AtomicLong corrected = new AtomicLong(0);

    public CorrectBarcodesIO(String inputFileName, String outputFileName, int mismatches, int indels,
                             int totalErrors, float threshold, List<String> groupNames, int maxClusterDepth,
                             float singleSubstitutionProbability, float singleIndelProbability, long inputReadsLimit,
                             boolean suppressWarnings) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.mismatches = mismatches;
        this.indels = indels;
        this.totalErrors = totalErrors;
        this.threshold = threshold;
        this.groupNames = groupNames;
        this.maxClusterDepth = maxClusterDepth;
        this.mutationProbability = new SimpleMutationProbability(singleSubstitutionProbability, singleIndelProbability);
        this.inputReadsLimit = inputReadsLimit;
        this.suppressWarnings = suppressWarnings;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        try (MifReader pass1Reader = new MifReader(inputFileName);
             MifReader pass2Reader = new MifReader(inputFileName);
             MifWriter writer = createWriter(pass1Reader.getHeader())) {
            if (inputReadsLimit > 0) {
                pass1Reader.setParsedReadsLimit(inputReadsLimit);
                pass2Reader.setParsedReadsLimit(inputReadsLimit);
            }
            SmartProgressReporter.startProgressReport("Counting sequences", pass1Reader, System.err);
            defaultGroups = IntStream.rangeClosed(1, pass1Reader.getNumberOfReads())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());
            if (groupNames.stream().anyMatch(defaultGroups::contains))
                throw exitWithError("Default groups R1, R2, etc should not be specified for correction!");
            keyGroups = new LinkedHashSet<>(groupNames);
            if (!suppressWarnings && pass1Reader.isSorted())
                System.err.println("WARNING: correcting sorted MIF file; output file will be unsorted!");
            List<String> correctedAgainGroups = keyGroups.stream().filter(gn -> pass1Reader.getCorrectedGroups()
                    .stream().anyMatch(gn::equals)).collect(Collectors.toList());
            if (!suppressWarnings && (correctedAgainGroups.size() != 0))
                System.err.println("WARNING: group(s) " + correctedAgainGroups + " already corrected and will be " +
                        "corrected again!");
            Map<String, HashMap<NucleotideSequence, SequenceCounter>> sequenceMaps = keyGroups.stream()
                    .collect(Collectors.toMap(groupName -> groupName, groupName -> new HashMap<>()));
            numberOfReads = pass1Reader.getNumberOfReads();
            for (ParsedRead parsedRead : CUtils.it(pass1Reader)) {
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
                    NucleotideSequence headSequence = cluster.getHead().sequence;
                    cluster.processAllChildren((child) -> {
                        currentCorrectionMap.put(child.getHead().sequence, headSequence);
                        return true;
                    });
                });
                sequenceCorrectionMaps.put(entry.getKey(), currentCorrectionMap);
            }

            // second pass: correcting barcodes
            SmartProgressReporter.startProgressReport("Correcting barcodes", pass2Reader, System.err);
            for (ParsedRead parsedRead : CUtils.it(pass2Reader)) {
                writer.write(correctBarcodes(parsedRead));
                if (++totalReads == inputReadsLimit)
                    break;
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads");
        float percent = (totalReads == 0) ? 0 : (float)corrected.get() / totalReads * 100;
        System.err.println("Reads with corrected barcodes: " + corrected + " (" + floatFormat.format(percent) + "%)\n");
    }

    private MifWriter createWriter(MifHeader inputHeader) throws IOException {
        LinkedHashSet<String> allCorrectedGroups = new LinkedHashSet<>(inputHeader.getCorrectedGroups());
        allCorrectedGroups.addAll(groupNames);
        MifHeader outputHeader = new MifHeader(inputHeader.getNumberOfReads(), new ArrayList<>(allCorrectedGroups),
                false, inputHeader.getGroupEdges());
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                : new MifWriter(outputFileName, outputHeader);
    }

    private ParsedRead correctBarcodes(ParsedRead parsedRead) {
        Map<String, MatchedGroup> matchedGroups = parsedRead.getGroups().stream()
                .filter(group -> keyGroups.contains(group.getGroupName()))
                .collect(Collectors.toMap(MatchedGroup::getGroupName, group -> group));
        HashMap<Byte, ArrayList<CorrectedGroup>> correctedGroups = new HashMap<>();
        boolean isCorrection = false;
        for (Map.Entry<String, MatchedGroup> entry : matchedGroups.entrySet()) {
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

        Match newMatch = new Match(numberOfReads, parsedRead.getBestMatchScore(), newGroupEdges);
        if (newMatch.getGroups().stream().map(MatchedGroup::getGroupName)
                .filter(defaultGroups::contains).count() != numberOfReads)
            throw new IllegalStateException("Missing default groups in new Match: expected " + defaultGroups
                    + ", got " + newMatch.getGroups().stream().map(MatchedGroup::getGroupName)
                    .filter(defaultGroups::contains).collect(Collectors.toList()));
        return new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(), newMatch, 0);
    }

    private static class CorrectedGroup {
        final String groupName;
        final NucleotideSequence correctedValue;

        CorrectedGroup(String groupName, NucleotideSequence correctedValue) {
            this.groupName = groupName;
            this.correctedValue = correctedValue;
        }
    }

    private static class SequenceCounter implements Comparable<SequenceCounter> {
        final NucleotideSequence sequence;
        long count;

        SequenceCounter(NucleotideSequence sequence) {
            this.sequence = sequence;
            count = 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SequenceCounter that = (SequenceCounter)o;
            return sequence.equals(that.sequence);
        }

        @Override
        public int hashCode() {
            return sequence.hashCode();
        }

        // compareTo is reversed to start from bigger counts
        @Override
        public int compareTo(SequenceCounter other) {
            return -Long.compare(count, other.count);
        }
    }

    private static class SequenceCounterExtractor implements SequenceExtractor<SequenceCounter, NucleotideSequence> {
        @Override
        public NucleotideSequence getSequence(SequenceCounter sequenceCounter) {
            return sequenceCounter.sequence;
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
