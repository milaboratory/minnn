package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.Merger;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.Chunk;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.tree.NeighborhoodIterator;
import com.milaboratory.core.tree.SequenceTreeMap;
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
import java.util.stream.StreamSupport;

import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class CorrectBarcodesIO {
    private final String inputFileName;
    private final String outputFileName;
    private final int mismatches;
    private final int deletions;
    private final int insertions;
    private final int totalErrors;
    private final int threads;
    private Set<String> defaultGroups;
    private Map<String, SequenceTreeMap<NucleotideSequence, SequenceCounter>> sequenceTreeMaps;
    private int numberOfReads;
    private AtomicLong corrected = new AtomicLong(0);
    private AtomicLong overlaps = new AtomicLong(0);

    public CorrectBarcodesIO(String inputFileName, String outputFileName, int mismatches, int deletions, int insertions,
                             int totalErrors, int threads) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.mismatches = mismatches;
        this.deletions = deletions;
        this.insertions = insertions;
        this.totalErrors = totalErrors;
        this.threads = threads;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        try (MifReader pass1Reader = new MifReader(inputFileName);
             MifReader pass2Reader = new MifReader(inputFileName);
             MifWriter writer = createWriter(pass1Reader.getHeader())) {
            SmartProgressReporter.startProgressReport("Counting sequences", pass1Reader, System.err);
            if (pass1Reader.isSorted())
                System.err.println("WARNING: correcting sorted MIF file; output file will be unsorted!");
            if (pass1Reader.isCorrected())
                System.err.println("WARNING: correcting already corrected MIF file!");
            defaultGroups = IntStream.rangeClosed(1, pass1Reader.getNumberOfReads())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());
            Set<String> keyGroups = pass1Reader.getGroupEdges().stream().filter(GroupEdge::isStart)
                    .map(GroupEdge::getGroupName).filter(groupName -> !defaultGroups.contains(groupName))
                    .collect(Collectors.toSet());
            sequenceTreeMaps = keyGroups.stream().collect(Collectors.toMap(groupName -> groupName,
                    groupName -> new SequenceTreeMap<>(NucleotideSequence.ALPHABET)));
            numberOfReads = pass1Reader.getNumberOfReads();
            for (ParsedRead parsedRead : CUtils.it(pass1Reader))
                for (Map.Entry<String, SequenceTreeMap<NucleotideSequence, SequenceCounter>> entry
                        : sequenceTreeMaps.entrySet()) {
                    NucleotideSequence groupValue = parsedRead.getGroupValue(entry.getKey()).getSequence();
                    SequenceCounter counter = entry.getValue().get(groupValue);
                    if (counter == null)
                        entry.getValue().put(groupValue, new SequenceCounter(groupValue));
                    else
                        counter.increaseCount();
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
        System.err.println("Reads with corrected barcodes: " + corrected);
        System.err.println("Reads with barcode overlaps: " + overlaps + "\n");
    }

    private MifWriter createWriter(MifHeader inputHeader) throws IOException {
        MifHeader outputHeader = new MifHeader(inputHeader.getNumberOfReads(), true, false,
                inputHeader.getGroupEdges());
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                : new MifWriter(outputFileName, outputHeader);
    }

    private static class SequenceCounter implements Comparable<SequenceCounter> {
        private final NucleotideSequence sequence;
        private long count;

        SequenceCounter(NucleotideSequence sequence) {
            this.sequence = sequence;
            count = 1;
        }

        NucleotideSequence getSequence() {
            return sequence;
        }

        long getCount() {
            return count;
        }

        void increaseCount() {
            count++;
        }

        @Override
        public int compareTo(SequenceCounter other) {
            return Long.compare(count, other.getCount());
        }
    }

    private class CorrectBarcodesProcessor implements Processor<ParsedRead, ParsedRead> {
        @Override
        public ParsedRead process(ParsedRead parsedRead) {
            Map<String, MatchedGroup> matchedGroups = parsedRead.getGroups().stream()
                    .filter(group -> !defaultGroups.contains(group.getGroupName()))
                    .collect(Collectors.toMap(MatchedGroup::getGroupName, group -> group));
            HashMap<Byte, NSequenceWithQuality> oldTargets = new HashMap<>();
            parsedRead.getGroups().forEach(g -> oldTargets.putIfAbsent(g.getTargetId(), g.getTarget()));
            HashMap<Byte, ArrayList<TargetPatch>> targetPatches = new HashMap<>();
            boolean isCorrection = false;
            for (Map.Entry<String, MatchedGroup> entry : matchedGroups.entrySet()) {
                String groupName = entry.getKey();
                MatchedGroup matchedGroup = entry.getValue();
                byte targetId = matchedGroup.getTargetId();
                NucleotideSequence oldValue = matchedGroup.getValue().getSequence();
                SequenceTreeMap<NucleotideSequence, SequenceCounter> sequenceTreeMap = sequenceTreeMaps.get(groupName);
                NeighborhoodIterator<NucleotideSequence, SequenceCounter> neighborhoodIterator = sequenceTreeMap
                        .getNeighborhoodIterator(oldValue, mismatches, deletions, insertions, totalErrors);
                SequenceCounter correctedSequenceCounter = StreamSupport.stream(neighborhoodIterator.it()
                        .spliterator(), false).max(SequenceCounter::compareTo).orElse(null);
                NucleotideSequence correctValue = (correctedSequenceCounter == null) ? oldValue
                        : correctedSequenceCounter.getSequence();
                isCorrection |= !correctValue.equals(oldValue);
                targetPatches.computeIfAbsent(targetId, id -> new ArrayList<>());
                targetPatches.get(targetId).add(new TargetPatch(groupName, correctValue, matchedGroup.getRange()));
            }

            ArrayList<MatchedGroupEdge> newGroupEdges;
            if (!isCorrection)
                newGroupEdges = parsedRead.getMatchedGroupEdges();
            else {
                boolean isOverlap = false;
                newGroupEdges = new ArrayList<>();
                for (byte targetId : oldTargets.keySet()) {
                    NSequenceWithQuality newTarget = NSequenceWithQuality.EMPTY;
                    ArrayList<TargetPatch> currentTargetPatches = targetPatches.get(targetId);
                    if (currentTargetPatches == null)
                        parsedRead.getMatchedGroupEdges().stream()
                                .filter(mge -> mge.getTargetId() == targetId).forEach(newGroupEdges::add);
                    else {
                        Collections.sort(currentTargetPatches);
                        NSequenceWithQuality currentOldTarget = oldTargets.get(targetId);
                        for (int i = 0; i < currentTargetPatches.size(); i++) {
                            TargetPatch currentPatch = currentTargetPatches.get(i);
                            if (i > 0)
                                isOverlap |= currentPatch.trimRange(currentTargetPatches.get(i - 1));
                            else if (currentPatch.range.getLower() > 0)
                                newTarget = currentOldTarget.getRange(0, currentPatch.range.getLower());
                            currentPatch.newLower = newTarget.size();
                            newTarget = newTarget.concatenate(new NSequenceWithQuality(currentPatch.correctValue));
                            currentPatch.newUpper = newTarget.size();
                            int oldTargetPartLower = currentPatch.range.getUpper();
                            int oldTargetPartUpper = (i < currentTargetPatches.size() - 1)
                                    ? currentTargetPatches.get(i + 1).range.getLower() : currentOldTarget.size();
                            if (oldTargetPartLower < oldTargetPartUpper)
                                newTarget = newTarget.concatenate(currentOldTarget
                                        .getRange(oldTargetPartLower, oldTargetPartUpper));
                        }

                        Map<String, TargetPatch> currentTargetPatchesMap = currentTargetPatches.stream()
                                .collect(Collectors.toMap(tp -> tp.groupName, tp -> tp));
                        for (MatchedGroupEdge matchedGroupEdge : parsedRead.getMatchedGroupEdges().stream()
                                .filter(mge -> mge.getTargetId() == targetId).collect(Collectors.toList())) {
                            int matchedGroupEdgePosition;
                            GroupEdge groupEdge = matchedGroupEdge.getGroupEdge();
                            TargetPatch currentTargetPatch = currentTargetPatchesMap.get(groupEdge.getGroupName());
                            if (currentTargetPatch != null) {
                                matchedGroupEdgePosition = groupEdge.isStart() ? currentTargetPatch.newLower
                                        : currentTargetPatch.newUpper;
                                if (matchedGroupEdgePosition == -1)
                                    throw new IllegalStateException("New group edge position was not calculated!");
                            } else if (defaultGroups.contains(groupEdge.getGroupName()))
                                matchedGroupEdgePosition = groupEdge.isStart() ? 0 : newTarget.size();
                            else
                                throw new IllegalStateException("Group " + groupEdge.getGroupName() + " with target id "
                                        + targetId + " is not default and not in target patches!");
                            newGroupEdges.add(new MatchedGroupEdge(newTarget, matchedGroupEdge.getTargetId(),
                                    matchedGroupEdge.getGroupEdge(), matchedGroupEdgePosition));
                        }
                    }
                }
                if (isOverlap)
                    overlaps.getAndIncrement();
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

        private class TargetPatch implements Comparable<TargetPatch> {
            final String groupName;
            final NucleotideSequence correctValue;
            Range range;
            int newLower = -1;
            int newUpper = -1;

            TargetPatch(String groupName, NucleotideSequence correctValue, Range range) {
                this.groupName = groupName;
                this.correctValue = correctValue;
                this.range = range;
            }

            boolean trimRange(TargetPatch other) {
                if (range.getLower() < other.range.getUpper()) {
                    range = new Range(other.range.getUpper(), Math.max(range.getUpper(), other.range.getUpper()));
                    return true;
                } else
                    return false;
            }

            @Override
            public int compareTo(TargetPatch other) {
                return Integer.compare(range.getLower(), other.range.getLower());
            }
        }
    }
}
