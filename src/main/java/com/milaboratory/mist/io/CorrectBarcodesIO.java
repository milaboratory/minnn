package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.Merger;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.Chunk;
import cc.redberry.pipe.util.OrderedOutputPort;
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
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final float singleMutationProbability;
    private final int threads;
    private Set<String> defaultGroups;
    private LinkedHashSet<String> keyGroups;
    private Map<String, HashMap<NucleotideSequence, NucleotideSequence>> sequenceCorrectionMaps;
    private int numberOfReads;
    private AtomicLong corrected = new AtomicLong(0);

    public CorrectBarcodesIO(String inputFileName, String outputFileName, int mismatches, int indels,
                             int totalErrors, float threshold, List<String> groupNames, int maxClusterDepth,
                             float singleMutationProbability, int threads) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.mismatches = mismatches;
        this.indels = indels;
        this.totalErrors = totalErrors;
        this.threshold = threshold;
        this.groupNames = groupNames;
        this.maxClusterDepth = maxClusterDepth;
        this.singleMutationProbability = singleMutationProbability;
        this.threads = threads;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        try (MifReader pass1Reader = new MifReader(inputFileName);
             MifReader pass2Reader = new MifReader(inputFileName);
             MifWriter writer = createWriter(pass1Reader.getHeader())) {
            SmartProgressReporter.startProgressReport("Counting sequences", pass1Reader, System.err);
            defaultGroups = IntStream.rangeClosed(1, pass1Reader.getNumberOfReads())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());
            if (groupNames.stream().anyMatch(defaultGroups::contains))
                throw exitWithError("Default groups R1, R2, etc should not be specified for correction!");
            keyGroups = new LinkedHashSet<>(groupNames);
            if (pass1Reader.isSorted())
                System.err.println("WARNING: correcting sorted MIF file; output file will be unsorted!");
            List<String> correctedAgainGroups = keyGroups.stream().filter(gn -> pass1Reader.getCorrectedGroups()
                    .stream().anyMatch(gn::equals)).collect(Collectors.toList());
            if (correctedAgainGroups.size() != 0)
                System.err.println("WARNING: group(s) " + correctedAgainGroups + " already corrected and will be " +
                        "corrected again!");
            Map<String, HashMap<NucleotideSequence, SequenceCounter>> sequenceMaps = keyGroups.stream()
                    .collect(Collectors.toMap(groupName -> groupName, groupName -> new HashMap<>()));
            numberOfReads = pass1Reader.getNumberOfReads();
            for (ParsedRead parsedRead : CUtils.it(pass1Reader))
                for (Map.Entry<String, HashMap<NucleotideSequence, SequenceCounter>> entry : sequenceMaps.entrySet()) {
                    NucleotideSequence groupValue = parsedRead.getGroupValue(entry.getKey()).getSequence();
                    SequenceCounter counter = entry.getValue().get(groupValue);
                    if (counter == null)
                        entry.getValue().put(groupValue, new SequenceCounter(groupValue));
                    else
                        counter.count++;
                }

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

            // second pass: replacing barcodes
            SmartProgressReporter.startProgressReport("Correcting barcodes", pass2Reader, System.err);
            Merger<Chunk<ParsedRead>> bufferedReaderPort = CUtils.buffered(CUtils.chunked(
                    new NumberedParsedReadsPort(pass2Reader), 4 * 64), 4 * 16);
            OutputPort<Chunk<ParsedRead>> correctedReadsPort = new ParallelProcessor<>(bufferedReaderPort,
                    CUtils.chunked(new CorrectBarcodesProcessor()), threads);
            OrderedOutputPort<ParsedRead> orderedReadsPort = new OrderedOutputPort<>(
                    CUtils.unchunked(correctedReadsPort), read -> read.getOriginalRead().getId());
            for (ParsedRead parsedRead : CUtils.it(orderedReadsPort)) {
                writer.write(parsedRead);
                totalReads++;
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads");
        System.err.println("Reads with corrected barcodes: " + corrected + "\n");
    }

    private MifWriter createWriter(MifHeader inputHeader) throws IOException {
        LinkedHashSet<String> allCorrectedGroups = new LinkedHashSet<>(inputHeader.getCorrectedGroups());
        allCorrectedGroups.addAll(groupNames);
        MifHeader outputHeader = new MifHeader(inputHeader.getNumberOfReads(), new ArrayList<>(allCorrectedGroups),
                false, inputHeader.getGroupEdges());
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                : new MifWriter(outputFileName, outputHeader);
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
            double expected = majorClusterCount * Math.pow(singleMutationProbability, currentMutations.size());
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

    private class CorrectBarcodesProcessor implements Processor<ParsedRead, ParsedRead> {
        @Override
        public ParsedRead process(ParsedRead parsedRead) {
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

        private class CorrectedGroup {
            final String groupName;
            final NucleotideSequence correctedValue;

            CorrectedGroup(String groupName, NucleotideSequence correctedValue) {
                this.groupName = groupName;
                this.correctedValue = correctedValue;
            }
        }
    }
}
