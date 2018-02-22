package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.tree.SequenceTreeMap;
import com.milaboratory.mist.outputconverter.MatchedGroup;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.mist.pattern.Match;
import com.milaboratory.mist.pattern.MatchedGroupEdge;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class CorrectBarcodesIO {
    private final String inputFileName;
    private final String outputFileName;
    private final int mismatches;
    private final int deletions;
    private final int insertions;
    private final int totalErrors;

    public CorrectBarcodesIO(String inputFileName, String outputFileName, int mismatches, int deletions, int insertions,
                             int totalErrors) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.mismatches = mismatches;
        this.deletions = deletions;
        this.insertions = insertions;
        this.totalErrors = totalErrors;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        long correctedBarcodes = 0;
        try (MifReader pass1Reader = new MifReader(inputFileName);
             MifReader pass2Reader = new MifReader(inputFileName);
             MifWriter writer = createWriter(pass1Reader.getHeader())) {
            SmartProgressReporter.startProgressReport("Counting sequences", pass1Reader, System.err);
            if (pass1Reader.isCorrected())
                System.err.println("WARNING: correcting already corrected MIF file!");
            Set<String> defaultGroups = IntStream.rangeClosed(1, pass1Reader.getNumberOfReads())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());
            Set<String> keyGroups = pass1Reader.getGroupEdges().stream().filter(GroupEdge::isStart)
                    .map(GroupEdge::getGroupName).filter(groupName -> !defaultGroups.contains(groupName))
                    .collect(Collectors.toSet());
            Map<String, HashMap<NucleotideSequence, SequenceCounter>> unsortedMaps = keyGroups.stream()
                    .collect(Collectors.toMap(groupName -> groupName, groupName -> new HashMap<>()));
            for (ParsedRead parsedRead : CUtils.it(pass1Reader))
                for (Map.Entry<String, HashMap<NucleotideSequence, SequenceCounter>> entry : unsortedMaps.entrySet()) {
                    NucleotideSequence groupValue = parsedRead.getGroupValue(entry.getKey()).getSequence();
                    SequenceCounter counter = entry.getValue().get(groupValue);
                    if (counter == null)
                        entry.getValue().put(groupValue, new SequenceCounter(groupValue));
                    else
                        counter.increaseCount();
                }

            Map<String, SequenceTreeMap<NucleotideSequence, SequenceCounter>> sortedMaps = keyGroups.stream()
                    .collect(Collectors.toMap(groupName -> groupName,
                            groupName -> new SequenceTreeMap<>(NucleotideSequence.ALPHABET)));
            for (Map.Entry<String, HashMap<NucleotideSequence, SequenceCounter>> entry : unsortedMaps.entrySet()) {
                SequenceTreeMap<NucleotideSequence, SequenceCounter> currentSortedMap = sortedMaps.get(entry.getKey());
                entry.getValue().entrySet().stream().sorted(Map.Entry.comparingByValue())
                        .forEachOrdered(e -> currentSortedMap.put(e.getKey(), e.getValue()));
            }

            SmartProgressReporter.startProgressReport("Correcting barcodes", pass2Reader, System.err);
            for (ParsedRead parsedRead : CUtils.it(pass2Reader)) {
                Map<String, MatchedGroupData> matchedGroups = parsedRead.getGroups().stream()
                        .collect(Collectors.toMap(MatchedGroup::getGroupName,
                                group -> new MatchedGroupData(group.getValue().getSequence())));
                for (MatchedGroupEdge matchedGroupEdge : parsedRead.getMatchedGroupEdges()) {
                    MatchedGroupData matchedGroup = matchedGroups.get(matchedGroupEdge.getGroupName());
                    if (matchedGroupEdge.isStart())
                        matchedGroup.setStartEdge(matchedGroupEdge);
                    else
                        matchedGroup.setEndEdge(matchedGroupEdge);
                }

                ArrayList<MatchedGroupEdge> newGroupEdges = new ArrayList<>();
                for (Map.Entry<String, MatchedGroupData> entry : matchedGroups.entrySet()) {
                    SequenceTreeMap<NucleotideSequence, SequenceCounter> sequenceTreeMap = sortedMaps
                            .get(entry.getKey());
                    SequenceCounter correctedSequenceCounter = sequenceTreeMap
                            .getNeighborhoodIterator(entry.getValue().getSequence(),
                                    mismatches, deletions, insertions, totalErrors).next();
                    if (correctedSequenceCounter == null) {
                        newGroupEdges.add(entry.getValue().getStartEdge());
                        newGroupEdges.add(entry.getValue().getEndEdge());
                    } else {
                        NucleotideSequence correctValue = correctedSequenceCounter.getSequence();
                        int offset = entry.getValue().getOffset();
                        newGroupEdges.add(convertMatchedGroupEdge(entry.getValue().getStartEdge(),
                                correctValue, offset));
                        newGroupEdges.add(convertMatchedGroupEdge(entry.getValue().getEndEdge(),
                                correctValue, offset));
                        correctedBarcodes++;
                    }
                }

                Match newMatch = new Match(pass2Reader.getNumberOfReads(), parsedRead.getBestMatchScore(),
                        newGroupEdges);
                writer.write(new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(), newMatch));
                totalReads++;
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads, corrected " + correctedBarcodes + " barcodes\n");
    }

    private MifWriter createWriter(MifHeader inputHeader) throws IOException {
        MifHeader outputHeader = new MifHeader(inputHeader.getNumberOfReads(), true,
                inputHeader.getGroupEdges());
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                : new MifWriter(outputFileName, outputHeader);
    }

    private MatchedGroupEdge convertMatchedGroupEdge(MatchedGroupEdge originalEdge, NucleotideSequence newValue,
                                                     int offset) {
        NSequenceWithQuality originalTarget = originalEdge.getTarget();
        NSequenceWithQuality newPart = new NSequenceWithQuality(newValue,
                originalTarget.getRange(offset, newValue.size()).getQuality());
        NSequenceWithQuality firstPart = (offset == 0) ? newPart
                : originalTarget.getRange(0, offset).concatenate(newPart);
        NSequenceWithQuality newTarget = (offset == originalTarget.size() - newValue.size()) ? firstPart
                : firstPart.concatenate(originalTarget.getRange(offset + newValue.size(), originalTarget.size()));
        return new MatchedGroupEdge(newTarget, originalEdge.getTargetId(), originalEdge.getGroupEdge(),
                originalEdge.getPosition());
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
            return Long.compare(other.getCount(), count);   // inverted to make bigger counts first
        }
    }

    private static class MatchedGroupData {
        private final NucleotideSequence sequence;
        private MatchedGroupEdge startEdge = null;
        private MatchedGroupEdge endEdge = null;

        MatchedGroupData(NucleotideSequence sequence) {
            this.sequence = sequence;
        }

        NucleotideSequence getSequence() {
            return sequence;
        }

        void setStartEdge(MatchedGroupEdge startEdge) {
            this.startEdge = startEdge;
        }

        void setEndEdge(MatchedGroupEdge endEdge) {
            this.endEdge = endEdge;
        }

        MatchedGroupEdge getStartEdge() {
            if (startEdge == null)
                throw new IllegalStateException("Starting group edge is null in MatchedGroupData!");
            return startEdge;
        }

        MatchedGroupEdge getEndEdge() {
            if (endEdge == null)
                throw new IllegalStateException("Ending group edge is null in MatchedGroupData!");
            return endEdge;
        }

        int getOffset() {
            return getStartEdge().getPosition();
        }
    }
}
