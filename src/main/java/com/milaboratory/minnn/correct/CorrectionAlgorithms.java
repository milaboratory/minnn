package com.milaboratory.minnn.correct;

import cc.redberry.pipe.CUtils;
import com.milaboratory.core.clustering.Clustering;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.minnn.io.MifReader;
import com.milaboratory.minnn.io.MifWriter;
import com.milaboratory.minnn.outputconverter.MatchedGroup;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.Match;
import com.milaboratory.minnn.pattern.MatchedGroupEdge;
import com.milaboratory.minnn.pattern.MatchedItem;
import com.milaboratory.util.SmartProgressReporter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

public final class CorrectionAlgorithms {
    private CorrectionAlgorithms() {}

    public static CorrectionStats fullFileCorrect(
            MifReader pass1Reader, MifReader pass2Reader, MifWriter writer, MifWriter excludedBarcodesWriter,
            long inputReadsLimit, BarcodeClusteringStrategy barcodeClusteringStrategy, Set<String> defaultGroups,
            LinkedHashSet<String> keyGroups, int maxUniqueBarcodes) {
        Map<String, HashMap<NucleotideSequence, NucleotideSequence>> sequenceCorrectionMaps;
        Map<String, Map<NucleotideSequence, RawSequenceCounter>> notCorrectedBarcodeCounters = (maxUniqueBarcodes > 0)
                ? new HashMap<>() : null;
        Map<String, Set<NucleotideSequence>> includedBarcodes;
        Map<String, HashMap<NucleotideSequence, SequenceCounter>> sequenceMaps = keyGroups.stream()
                .collect(Collectors.toMap(groupName -> groupName, groupName -> new HashMap<>()));
        long totalReads = 0;
        long correctedReads = 0;
        long excludedReads = 0;

        // 1st pass: counting barcodes
        for (ParsedRead parsedRead : CUtils.it(pass1Reader)) {
            for (Map.Entry<String, HashMap<NucleotideSequence, SequenceCounter>> entry : sequenceMaps.entrySet()) {
                // creating multi-sequence counters, without merging multi-sequences on this stage
                NucleotideSequence groupValue = parsedRead.getGroupValue(entry.getKey()).getSequence();
                SequenceCounter counter = entry.getValue().get(groupValue);
                if (counter == null)
                    entry.getValue().put(groupValue, new SequenceCounter(groupValue));
                else
                    counter.count++;

                // counting raw barcode sequences if filtering by count is enabled
                if (maxUniqueBarcodes > 0) {
                    notCorrectedBarcodeCounters.computeIfAbsent(entry.getKey(), groupName -> new HashMap<>());
                    Map<NucleotideSequence, RawSequenceCounter> currentGroupCounters = notCorrectedBarcodeCounters
                            .get(entry.getKey());
                    RawSequenceCounter rawSequenceCounter = currentGroupCounters.get(groupValue);
                    if (rawSequenceCounter == null)
                        currentGroupCounters.put(groupValue, new RawSequenceCounter(groupValue));
                    else
                        rawSequenceCounter.count++;
                }
            }
            if (++totalReads == inputReadsLimit)
                break;
        }
        totalReads = 0;

        // sorting nucleotide sequences by count in each group and performing clustering
        SequenceCounterExtractor sequenceCounterExtractor = new SequenceCounterExtractor();
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

        // calculating which barcodes must be included or excluded; only if filtering by count is enabled
        if (maxUniqueBarcodes > 0) {
            Map<String, Map<NucleotideSequence, RawSequenceCounter>> correctedBarcodeCounters = new HashMap<>();
            System.err.println("Filtering corrected barcodes by count...");
            // counting corrected barcodes by not corrected barcodes counts
            for (Map.Entry<String, Map<NucleotideSequence, RawSequenceCounter>> groupEntry
                    : notCorrectedBarcodeCounters.entrySet()) {
                String groupName = groupEntry.getKey();
                correctedBarcodeCounters.computeIfAbsent(groupName, gn -> new HashMap<>());
                Map<NucleotideSequence, RawSequenceCounter> currentGroupCorrectedCounters =
                        correctedBarcodeCounters.get(groupName);
                Map<NucleotideSequence, NucleotideSequence> currentGroupCorrectionMap =
                        sequenceCorrectionMaps.get(groupName);
                for (Map.Entry<NucleotideSequence, RawSequenceCounter> barcodeValueEntry
                        : groupEntry.getValue().entrySet()) {
                    NucleotideSequence oldValue = barcodeValueEntry.getKey();
                    long oldCount = barcodeValueEntry.getValue().count;
                    NucleotideSequence newValue = currentGroupCorrectionMap.get(oldValue);
                    if (newValue == null)
                        newValue = oldValue;
                    RawSequenceCounter correctedSequenceCounter = currentGroupCorrectedCounters.get(newValue);
                    if (correctedSequenceCounter == null) {
                        RawSequenceCounter newCounter = new RawSequenceCounter(newValue);
                        newCounter.count = oldCount;
                        currentGroupCorrectedCounters.put(newValue, newCounter);
                    } else
                        correctedSequenceCounter.count += oldCount;
                }
            }
            // filtering by count
            includedBarcodes = correctedBarcodeCounters.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> new TreeSet<>(entry.getValue().values()).stream().limit(maxUniqueBarcodes)
                                    .map(counter -> counter.seq).collect(Collectors.toSet())));
        }

        // 2nd pass: correcting barcodes
        SmartProgressReporter.startProgressReport("Correcting barcodes", pass2Reader, System.err);
        for (ParsedRead parsedRead : CUtils.it(pass2Reader)) {
            CorrectBarcodesResult correctBarcodesResult = correctBarcodes(parsedRead);
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

    public static CorrectionStats sortedClustersCorrect(LinkedHashSet<String> primaryGroups) {
        throw new NotImplementedException();
    }

    public static CorrectionStats unsortedClustersCorrect() {
        throw new NotImplementedException();
    }

    private static CorrectBarcodesResult correctBarcodes(
            ParsedRead parsedRead, Map<String, HashMap<NucleotideSequence, NucleotideSequence>> sequenceCorrectionMaps,
            Map<String, Set<NucleotideSequence>> includedBarcodes, int numberOfTargets, Set<String> defaultGroups,
            LinkedHashSet<String> keyGroups, int maxUniqueBarcodes) {
        HashMap<Byte, ArrayList<CorrectedGroup>> correctedGroups = new HashMap<>();
        boolean isCorrection = false;
        int numCorrectedBarcodes = 0;
        boolean excluded = false;
        for (Map.Entry<String, MatchedGroup> entry : parsedRead.getGroups().stream()
                .filter(group -> keyGroups.contains(group.getGroupName()))
                .collect(Collectors.toMap(MatchedGroup::getGroupName, group -> group)).entrySet()) {
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
            if (maxUniqueBarcodes > 0)
                excluded |= !includedBarcodes.get(groupName).contains(correctValue);
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
            numCorrectedBarcodes++;
        }

        Match newMatch = new Match(numberOfTargets, parsedRead.getBestMatchScore(), newGroupEdges);
        if (newMatch.getGroups().stream().map(MatchedGroup::getGroupName)
                .filter(defaultGroups::contains).count() != numberOfTargets)
            throw new IllegalStateException("Missing default groups in new Match: expected " + defaultGroups
                    + ", got " + newMatch.getGroups().stream().map(MatchedGroup::getGroupName)
                    .filter(defaultGroups::contains).collect(Collectors.toList()));
        return new CorrectBarcodesResult(new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(),
                newMatch, 0), numCorrectedBarcodes, excluded);
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
