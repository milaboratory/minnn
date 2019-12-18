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
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NSequenceWithQualityBuilder;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.minnn.correct.CorrectionAlgorithms.*;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.minnn.pattern.Match;
import com.milaboratory.minnn.pattern.MatchedGroupEdge;
import com.milaboratory.minnn.stat.SimpleMutationProbability;
import gnu.trove.map.hash.TByteObjectHashMap;
import org.junit.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.correct.CorrectionAlgorithms.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class CorrectionAlgorithmsTest {
    private static final BarcodeClusteringStrategyFactory simpleClusteringStrategyFactory
            = new BarcodeClusteringStrategyFactory(0.12f, 0, 1, 2,
            new SimpleMutationProbability(0.1f, 0.02f));
    private static final CorrectionAlgorithms simpleCorrectionAlgorithms = new CorrectionAlgorithms(
            simpleClusteringStrategyFactory, 0, 0, 10);

    @Test
    public void simpleRandomTest() {
        for (int i = 0; i < 1000; i++) {
            CorrectionTestData testData = generateSimpleRandomTestData();
            OutputPort<CorrectionQualityPreprocessingResult> preprocessorPort = getPreprocessingResultOutputPort(
                    testData.getInputPort(), testData.keyGroups, testData.primaryGroups);
            OutputPort<ParsedRead> parsedReadsPort = testData.getInputPort();
            List<CorrectBarcodesResult> results = new ArrayList<>();
            if (testData.primaryGroups.size() > 0) {
                OutputPort<CorrectionData> correctionDataPort = performSecondaryBarcodesCorrection(preprocessorPort,
                        simpleCorrectionAlgorithms, testData.keyGroups, rg.nextInt(10) + 1);
                for (CorrectionData correctionData : CUtils.it(correctionDataPort))
                    for (int j = 0; j < correctionData.parsedReadsCount; j++) {
                        ParsedRead parsedRead = Objects.requireNonNull(parsedReadsPort.take());
                        results.add(simpleCorrectionAlgorithms.correctBarcodes(parsedRead, correctionData));
                    }
            } else {
                CorrectionData correctionData = simpleCorrectionAlgorithms.prepareCorrectionData(preprocessorPort,
                        testData.keyGroups, 0);
                streamPort(parsedReadsPort).forEach(parsedRead ->
                        results.add(simpleCorrectionAlgorithms.correctBarcodes(parsedRead, correctionData)));
            }
            testData.assertCorrectionResults(results);
        }
    }

    private static CorrectionTestData generateSimpleRandomTestData() {
        int numberOfTargets = rg.nextInt(4) + 1;
        int numberOfKeyGroups = rg.nextInt(6) + 1;
        int numberOfPrimaryGroups = rg.nextInt(3);
        int totalNumberOfGroups = numberOfKeyGroups + numberOfPrimaryGroups + rg.nextInt(3);
        List<String> keyGroups = IntStream.rangeClosed(1, numberOfKeyGroups).mapToObj(i -> "G" + i)
                .collect(Collectors.toList());
        List<String> primaryGroups = IntStream.rangeClosed(1, numberOfPrimaryGroups).mapToObj(i -> "PG" + i)
                .collect(Collectors.toList());
        HashMap<String, Byte> groupTargetIds = new HashMap<>();
        for (int i = 0; i < totalNumberOfGroups; i++) {
            byte randomTargetId = (byte)(rg.nextInt(numberOfTargets) + 1);
            if (i < numberOfPrimaryGroups)
                groupTargetIds.put(primaryGroups.get(i), randomTargetId);
            else if (i < numberOfPrimaryGroups + numberOfKeyGroups)
                groupTargetIds.put(keyGroups.get(i - numberOfPrimaryGroups), randomTargetId);
            else
                groupTargetIds.put("N" + (i - numberOfPrimaryGroups - numberOfKeyGroups + 1), randomTargetId);
        }

        ReadWithGroups readWithGroups = new ReadWithGroups();
        for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
            List<String> currentTargetGroups = new ArrayList<>();
            for (HashMap.Entry<String, Byte> entry : groupTargetIds.entrySet())
                if (entry.getValue() == targetId)
                    currentTargetGroups.add(entry.getKey());
            int sequenceLength = (currentTargetGroups.size() == 0) ? rg.nextInt(6) + 1 : rg.nextInt(7);
            for (String group : currentTargetGroups) {
                int start = sequenceLength;
                int end = sequenceLength + rg.nextInt(8) + 1;
                sequenceLength = end + rg.nextInt(6);
                readWithGroups.groups.put(group, new GroupCoordinates(targetId, start, end));
            }
            readWithGroups.targetSequences.put(targetId, randomSeqWithQuality(sequenceLength, true));
        }

        List<ReadWithGroups> clusters;
        if (numberOfPrimaryGroups > 0) {
            int numClusters = rg.nextInt(6) + 1;
            clusters = createPrimaryGroupClusters(readWithGroups, numClusters);
        } else
            clusters = Collections.singletonList(readWithGroups);

        List<TByteObjectHashMap<NSequenceWithQuality>> preparedInputSequences = new ArrayList<>();
        List<Map<String, GroupCoordinates>> preparedGroups = new ArrayList<>();
        List<TByteObjectHashMap<NucleotideSequence>> preparedExpectedSequences = new ArrayList<>();

        for (ReadWithGroups currentReadWithGroups : clusters) {
            int mutatedSequencesNum = rg.nextInt(10);
            int goodSequencesNum = mutatedSequencesNum + rg.nextInt(5) + 1;
            int generatedSeqCount = 0;
            int generatedGoodSeqCount = 0;
            while (generatedSeqCount < mutatedSequencesNum + goodSequencesNum) {
                if (generatedGoodSeqCount < goodSequencesNum) {
                    preparedInputSequences.add(currentReadWithGroups.targetSequences);
                    preparedGroups.add(currentReadWithGroups.groups);
                    generatedGoodSeqCount++;
                } else {
                    ReadWithGroups mutatedReadWithGroups = mutate(currentReadWithGroups,
                            rg.nextInt(10) + 1);
                    preparedInputSequences.add(mutatedReadWithGroups.targetSequences);
                    preparedGroups.add(mutatedReadWithGroups.groups);
                }
                preparedExpectedSequences.add(removeQuality(currentReadWithGroups.targetSequences));
                generatedSeqCount++;
            }
        }

        return new CorrectionTestData(numberOfTargets, new LinkedHashSet<>(keyGroups),
                new LinkedHashSet<>(primaryGroups), preparedInputSequences, preparedGroups, preparedExpectedSequences);
    }

    private static ReadWithGroups mutate(ReadWithGroups input, int numErrors) {
        ReadWithGroups output = new ReadWithGroups();
    }

    private static List<ReadWithGroups> createPrimaryGroupClusters(ReadWithGroups input, int numClusters) {
        Map<String, GroupCoordinates> primaryGroups = input.groups.entrySet().stream()
                .filter(entry -> entry.getKey().substring(0, 2).equals("PG"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Set<PrimaryGroups> clusters = new HashSet<>();
        while (clusters.size() < numClusters) {
            Map<String, NSequenceWithQuality> groupValues = new HashMap<>();
            for (Map.Entry<String, GroupCoordinates> entry : primaryGroups.entrySet()) {
                GroupCoordinates groupCoordinates = entry.getValue();
                NSequenceWithQuality randomGroupValue = randomSeqWithQuality(
                        groupCoordinates.end - groupCoordinates.start, false);
                groupValues.put(entry.getKey(), randomGroupValue);
            }
            clusters.add(new PrimaryGroups(groupValues));
        }
        List<ReadWithGroups> updatedSequencesForClusters = new ArrayList<>();
        for (PrimaryGroups cluster : clusters) {
            ReadWithGroups currentReadWithGroups = new ReadWithGroups();
            TByteObjectHashMap<NSequenceWithQuality> currentTargetSequences = currentReadWithGroups.targetSequences;
            currentTargetSequences.putAll(input.targetSequences);
            currentReadWithGroups.groups.putAll(input.groups);
            for (String groupName : cluster.groupValues.keySet()) {
                GroupCoordinates groupCoordinates = primaryGroups.get(groupName);
                NSequenceWithQuality currentTargetSeq = currentTargetSequences.get(groupCoordinates.targetId);
                NSequenceWithQualityBuilder builder = new NSequenceWithQualityBuilder();
                builder.append(currentTargetSeq.getRange(0, groupCoordinates.start));
                builder.append(cluster.groupValues.get(groupName));
                builder.append(currentTargetSeq.getRange(groupCoordinates.end, currentTargetSeq.size()));
                currentTargetSequences.put(groupCoordinates.targetId, builder.createAndDestroy());
            }
            updatedSequencesForClusters.add(currentReadWithGroups);
        }
        return updatedSequencesForClusters;
    }

    private static TByteObjectHashMap<NucleotideSequence> removeQuality(
            TByteObjectHashMap<NSequenceWithQuality> targetSequences) {
        TByteObjectHashMap<NucleotideSequence> result = new TByteObjectHashMap<>();
        targetSequences.forEachEntry((key, value) -> {
            result.put(key, value.getSequence());
            return true;
        });
        return result;
    }

    private static class CorrectionTestData {
        final int numberOfTargets;
        final LinkedHashSet<String> keyGroups;
        final LinkedHashSet<String> primaryGroups;
        // keys: targetIds, starting from 1 (numbers after "R" in R1, R2 etc)
        final List<TByteObjectHashMap<NSequenceWithQuality>> inputSequences;
        final List<Map<String, GroupCoordinates>> groups;
        final List<TByteObjectHashMap<NucleotideSequence>> expectedSequences;

        CorrectionTestData(
                int numberOfTargets, LinkedHashSet<String> keyGroups, LinkedHashSet<String> primaryGroups,
                List<TByteObjectHashMap<NSequenceWithQuality>> inputSequences,
                List<Map<String, GroupCoordinates>> groups,
                List<TByteObjectHashMap<NucleotideSequence>> expectedSequences) {
            this.numberOfTargets = numberOfTargets;
            this.keyGroups = keyGroups;
            this.primaryGroups = primaryGroups;
            this.inputSequences = inputSequences;
            this.groups = groups;
            this.expectedSequences = expectedSequences;
        }

        OutputPort<ParsedRead> getInputPort() {
            return new OutputPort<ParsedRead>() {
                int counter = 0;

                @Override
                public ParsedRead take() {
                    if (counter == inputSequences.size())
                        return null;
                    SequenceRead originalRead;
                    switch (numberOfTargets) {
                        case 1:
                            originalRead = new SingleReadImpl(
                                    counter, inputSequences.get(counter).get((byte)1), "");
                            break;
                        case 2:
                            originalRead = new PairedRead(
                                    new SingleReadImpl(
                                            counter, inputSequences.get(counter).get((byte)1), ""),
                                    new SingleReadImpl(
                                            counter, inputSequences.get(counter).get((byte)2), ""));
                            break;
                        default:
                            SingleRead[] originalReads = new SingleRead[numberOfTargets];
                            for (int i = 0; i < numberOfTargets; i++)
                                originalReads[i] = new SingleReadImpl(
                                        counter, inputSequences.get(counter).get((byte)(i + 1)), "");
                            originalRead = new MultiRead(originalReads);
                    }
                    ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
                    for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
                        NSequenceWithQuality target = inputSequences.get(counter).get(targetId);
                        String groupName = "R" + targetId;
                        matchedGroupEdges.add(new MatchedGroupEdge(target, targetId,
                                new GroupEdge(groupName, true), 0));
                        matchedGroupEdges.add(new MatchedGroupEdge(target, targetId,
                                new GroupEdge(groupName, false), target.size()));
                    }
                    for (Map.Entry<String, GroupCoordinates> groupEntry : groups.get(counter).entrySet()) {
                        String groupName = groupEntry.getKey();
                        GroupCoordinates groupCoordinates = groupEntry.getValue();
                        NSequenceWithQuality target = inputSequences.get(counter).get(groupCoordinates.targetId);
                        matchedGroupEdges.add(new MatchedGroupEdge(target, groupCoordinates.targetId,
                                new GroupEdge(groupName, true), groupCoordinates.start));
                        matchedGroupEdges.add(new MatchedGroupEdge(target, groupCoordinates.targetId,
                                new GroupEdge(groupName, false), groupCoordinates.end));
                    }
                    Match bestMatch = new Match(numberOfTargets, 0, matchedGroupEdges);
                    counter++;
                    return new ParsedRead(originalRead, false, -1,
                            bestMatch, 0);
                }
            };
        }

        void assertCorrectionResults(List<CorrectBarcodesResult> results) {
            assertEquals(expectedSequences.size(), results.size());
            for (int i = 0; i < results.size(); i++) {
                TByteObjectHashMap<NucleotideSequence> actualSequences = new TByteObjectHashMap<>();
                boolean expectedCorrection = false;
                for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
                    actualSequences.put(targetId, results.get(i).parsedRead.getMatchTarget(targetId).getSequence());
                    expectedCorrection |= (expectedSequences.get(i).get(targetId).getSequence()
                            != inputSequences.get(i).get(targetId).getSequence());
                }
                assertEquals(expectedSequences.get(i), actualSequences);
                assertEquals(expectedCorrection, results.get(i).corrected);
            }
        }
    }

    private static class GroupCoordinates {
        final byte targetId;
        final int start;
        final int end;

        GroupCoordinates(byte targetId, int start, int end) {
            this.targetId = targetId;
            this.start = start;
            this.end = end;
        }
    }

    private static class ReadWithGroups {
        final TByteObjectHashMap<NSequenceWithQuality> targetSequences = new TByteObjectHashMap<>();
        final Map<String, GroupCoordinates> groups = new HashMap<>();
    }

    private static class PrimaryGroups {
        final Map<String, NSequenceWithQuality> groupValues;

        PrimaryGroups(Map<String, NSequenceWithQuality> groupValues) {
            this.groupValues = groupValues;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PrimaryGroups that = (PrimaryGroups)o;
            if (!groupValues.keySet().equals(that.groupValues.keySet()))
                return false;
            for (String groupName : groupValues.keySet())
                if (!groupValues.get(groupName).getSequence().equals(that.groupValues.get(groupName).getSequence()))
                    return false;
            return true;
        }

        @Override
        public int hashCode() {
            return groupValues.keySet().hashCode();
        }
    }
}
