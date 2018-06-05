package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.Merger;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.Chunk;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.clustering.Cluster;
import com.milaboratory.core.clustering.ClusteringStrategy;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.tree.NeighborhoodIterator;
import com.milaboratory.core.tree.SequenceTreeMap;
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
import java.util.stream.StreamSupport;

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
    private final int threads;
    private Set<String> defaultGroups;
    private LinkedHashSet<String> keyGroups;
    private Map<String, TreeMap<NucleotideSequence, SequenceCounter>> sortedSequenceMaps;
    private int numberOfReads;
    private AtomicLong corrected = new AtomicLong(0);

    public CorrectBarcodesIO(String inputFileName, String outputFileName, int mismatches, int indels,
                             int totalErrors, float threshold, List<String> groupNames, int threads) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.mismatches = mismatches;
        this.indels = indels;
        this.totalErrors = totalErrors;
        this.threshold = threshold;
        this.groupNames = groupNames;
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
                        entry.getValue().put(groupValue, new SequenceCounter());
                    else
                        counter.count++;
                }

            // sorting nucleotide sequences by count in each group
            sortedSequenceMaps = new HashMap<>();
            for (Map.Entry<String, HashMap<NucleotideSequence, SequenceCounter>> entry : sequenceMaps.entrySet()) {
                SequenceCountComparator comparator = new SequenceCountComparator(entry.getValue());
                TreeMap<NucleotideSequence, SequenceCounter> sortedMap = new TreeMap<>(comparator);
                sortedMap.putAll(entry.getValue());
                sortedSequenceMaps.put(entry.getKey(), sortedMap);
            }



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

    private static class SequenceCounter {
        long count;

        SequenceCounter() {
            count = 1;
        }
    }

    private class SequenceCountComparator implements Comparator<NucleotideSequence> {
        private final Map<NucleotideSequence, SequenceCounter> unsortedSequenceMap;

        SequenceCountComparator(Map<NucleotideSequence, SequenceCounter> unsortedSequenceMap) {
            this.unsortedSequenceMap = unsortedSequenceMap;
        }

        // long comparator is reversed to start from bigger counts
        @Override
        public int compare(NucleotideSequence seq1, NucleotideSequence seq2) {
            return -Long.compare(unsortedSequenceMap.get(seq1).count, unsortedSequenceMap.get(seq2).count);
        }
    }

    private class BarcodeClusteringStrategy implements ClusteringStrategy<SequenceCounter, NucleotideSequence> {
        @Override
        public boolean canAddToCluster(Cluster<SequenceCounter> cluster, SequenceCounter minorObject,
                                       NeighborhoodIterator<NucleotideSequence, SequenceCounter[]> iterator) {
            return false;
        }

        @Override
        public TreeSearchParameters getSearchParameters() {
            return null;
        }

        @Override
        public int getMaxClusterDepth() {
            return 0;
        }

        @Override
        public int compare(SequenceCounter c1, SequenceCounter c2) {
            return -Long.compare(c1.count, c2.count);
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
                SequenceTreeMap<NucleotideSequence, SequenceCounter> sequenceTreeMap = sequenceTreeMaps.get(groupName);
                NeighborhoodIterator<NucleotideSequence, SequenceCounter> neighborhoodIterator = sequenceTreeMap
                        .getNeighborhoodIterator(oldValue, mismatches, indels, indels, totalErrors);
                SequenceCounter correctedSequenceCounter = StreamSupport.stream(neighborhoodIterator.it()
                        .spliterator(), false).max(SequenceCounter::compareTo).orElse(null);
                NucleotideSequence correctValue = oldValue;
                if ((correctedSequenceCounter != null) && ((float)sequenceTreeMap.get(oldValue).getCount()
                        / correctedSequenceCounter.getCount() < threshold))
                    correctValue = correctedSequenceCounter.getSequence();
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
