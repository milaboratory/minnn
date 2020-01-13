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
import com.milaboratory.core.mutations.Mutations;
import com.milaboratory.core.mutations.generator.NucleotideMutationModel;
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
import java.util.stream.Stream;

import static com.milaboratory.core.mutations.generator.MutationModels.getEmpiricalNucleotideMutationModel;
import static com.milaboratory.core.mutations.generator.MutationsGenerator.generateMutations;
import static com.milaboratory.minnn.cli.CliUtils.floatFormat;
import static com.milaboratory.minnn.correct.CorrectionAlgorithms.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class CorrectionAlgorithmsTest {
    private static final CorrectionAlgorithms simpleCorrectionAlgorithms = new CorrectionAlgorithms(
            new BarcodeClusteringStrategyFactory(0.12f, -1, 1, 2,
                    new SimpleMutationProbability(0.1f, 0.02f)),
            0, 0, 10);
    private static final CorrectionAlgorithms strictCorrectionAlgorithms = new CorrectionAlgorithms(
            new BarcodeClusteringStrategyFactory(-1, 10, 1, 2,
                    new SimpleMutationProbability(1, 1)),
            0, 0, 10);

    @Test
    public void simpleTests() {
        LinkedHashSet<String> keyGroups0 = new LinkedHashSet<>(Arrays.asList("G1", "G2", "G3", "G4", "G5", "G6"));
        List<TByteObjectHashMap<NSequenceWithQuality>> inputSequences0 = new ArrayList<>();
        List<Map<String, GroupCoordinates>> groups0 = new ArrayList<>();
        List<Map<String, NucleotideSequence>> expectedCorrectedGroupValues0 = new ArrayList<>();

        TByteObjectHashMap<NSequenceWithQuality> sequencesT0R_0_1 = new TByteObjectHashMap<>();
        Map<String, GroupCoordinates> groupsT0R_0_2 = new HashMap<>();
        Map<String, NucleotideSequence> expectedT0R_0_2 = new HashMap<>();
        sequencesT0R_0_1.put((byte)1, new NSequenceWithQuality("AATAAGTCGTCC", "%9SS7IOITX27"));
        sequencesT0R_0_1.put((byte)2, new NSequenceWithQuality(
                "GTTCCTGGCGTCCCTCCTGCCCGAGCTTACAA", "\"X7%%$',4'D[(*DU<JQZO9)88%V@X/OY"));
        sequencesT0R_0_1.put((byte)3, new NSequenceWithQuality("TACAAACAA", ">:0NA(2W0"));
        sequencesT0R_0_1.put((byte)4, new NSequenceWithQuality(
                "AGCAGTCCGCACCACCAGCT", ";IH=MX\"-E55#HO[H(>-["));
        groupsT0R_0_2.put("G1", new GroupCoordinates((byte)2, 2, 10));
        groupsT0R_0_2.put("G2", new GroupCoordinates((byte)2, 14, 19));
        groupsT0R_0_2.put("G3", new GroupCoordinates((byte)4, 2, 7));
        groupsT0R_0_2.put("G4", new GroupCoordinates((byte)2, 23, 30));
        groupsT0R_0_2.put("G5", new GroupCoordinates((byte)4, 12, 19));
        groupsT0R_0_2.put("G6", new GroupCoordinates((byte)3, 0, 5));
        expectedT0R_0_2.put("G1", new NucleotideSequence("TCCTGGCG"));
        expectedT0R_0_2.put("G2", new NucleotideSequence("TCCTG"));
        expectedT0R_0_2.put("G3", new NucleotideSequence("CAGTC"));
        expectedT0R_0_2.put("G4", new NucleotideSequence("AGCTTAC"));
        expectedT0R_0_2.put("G5", new NucleotideSequence("CACCAGC"));
        expectedT0R_0_2.put("G6", new NucleotideSequence("TACAA"));
        inputSequences0.add(sequencesT0R_0_1);
        groups0.add(groupsT0R_0_2);
        expectedCorrectedGroupValues0.add(expectedT0R_0_2);
        inputSequences0.add(sequencesT0R_0_1);
        groups0.add(groupsT0R_0_2);
        expectedCorrectedGroupValues0.add(expectedT0R_0_2);

        TByteObjectHashMap<NSequenceWithQuality> sequencesT0R2 = new TByteObjectHashMap<>();
        sequencesT0R2.put((byte)1, sequencesT0R_0_1.get((byte)1));
        sequencesT0R2.put((byte)2, new NSequenceWithQuality(
                "GTTCCTGGCGTCCCNDGSTCCCGAGCTTACAA", "\"X7%%$',4'D[(*$9-.DZO9)88%V@X/OY"));
        sequencesT0R2.put((byte)3, sequencesT0R_0_1.get((byte)3));
        sequencesT0R2.put((byte)4, new NSequenceWithQuality(
                "AGCSBTCCGCACCACCAGCT", ";IH$AX\"-E55#HO[H(>-["));
        inputSequences0.add(sequencesT0R2);
        groups0.add(groupsT0R_0_2);
        expectedCorrectedGroupValues0.add(expectedT0R_0_2);

        CorrectionTestData testData = new CorrectionTestData(sequencesT0R_0_1.size(), keyGroups0,
                new LinkedHashSet<>(), inputSequences0, groups0, expectedCorrectedGroupValues0);
        OutputPort<CorrectionQualityPreprocessingResult> preprocessorPort = getPreprocessingResultOutputPort(
                testData.getInputPort(), testData.keyGroups, testData.primaryGroups);
        CorrectionData correctionData = strictCorrectionAlgorithms.prepareCorrectionData(preprocessorPort,
                testData.keyGroups, 0);
        List<CorrectBarcodesResult> results = new ArrayList<>();
        for (ParsedRead parsedRead : CUtils.it(testData.getInputPort()))
            results.add(strictCorrectionAlgorithms.correctBarcodes(parsedRead, correctionData));
        testData.assertCorrectionResults(results);

        LinkedHashSet<String> keyGroups1 = new LinkedHashSet<>(Collections.singletonList("G1"));
        LinkedHashSet<String> primaryGroups1 = new LinkedHashSet<>(Arrays.asList("PG1", "PG2"));
        List<TByteObjectHashMap<NSequenceWithQuality>> inputSequences1 = new ArrayList<>();
        List<Map<String, GroupCoordinates>> groups1 = new ArrayList<>();
        List<Map<String, NucleotideSequence>> expectedCorrectedGroupValues1 = new ArrayList<>();

        TByteObjectHashMap<NSequenceWithQuality> sequencesT1R_0_5 = new TByteObjectHashMap<>();
        Map<String, GroupCoordinates> groupsT1R_0_5 = new HashMap<>();
        Map<String, NucleotideSequence> expectedT1R_0_24 = new HashMap<String, NucleotideSequence>() {{
            put("G1", NucleotideSequence.A); }};
        sequencesT1R_0_5.put((byte)1, new NSequenceWithQuality(
                "AACGTDTCWHGCAAADDNVWGGA", "EN;1<<$65%AD9N:*./341#A"));
        groupsT1R_0_5.put("PG1", new GroupCoordinates((byte)1, 15, 20));
        groupsT1R_0_5.put("PG2", new GroupCoordinates((byte)1, 4, 10));
        groupsT1R_0_5.put("G1", new GroupCoordinates((byte)1, 14, 15));
        for (int i = 0; i < 6; i++) {
            inputSequences1.add(sequencesT1R_0_5);
            groups1.add(groupsT1R_0_5);
        }
        for (int i = 0; i < 25; i++)
            expectedCorrectedGroupValues1.add(expectedT1R_0_24);

        TByteObjectHashMap<NSequenceWithQuality> sequencesT1R6 = new TByteObjectHashMap<>();
        Map<String, GroupCoordinates> groupsT1R6 = new HashMap<>();
        sequencesT1R6.put((byte)1, new NSequenceWithQuality(
                "AACGTDTCWHGCAAHADDNVWGGA", "EN;1<<$65%AD9ND:*./341#A"));
        groupsT1R6.put("PG1", new GroupCoordinates((byte)1, 16, 21));
        groupsT1R6.put("PG2", new GroupCoordinates((byte)1, 4, 10));
        groupsT1R6.put("G1", new GroupCoordinates((byte)1, 14, 16));
        inputSequences1.add(sequencesT1R6);
        groups1.add(groupsT1R6);

        for (int i = 0; i < 16; i++)
            groups1.add(groupsT1R_0_5);
        groups1.add(new HashMap<String, GroupCoordinates>() {{
            put("PG1", new GroupCoordinates((byte)1, 17, 22));
            put("PG2", new GroupCoordinates((byte)1, 4, 10));
            put("G1", new GroupCoordinates((byte)1, 14, 17)); }});
        groups1.add(groupsT1R6);

        Stream.of(
                new NSequenceWithQuality("AACGTDTCWHGCAABDDNVWGGA", "EN;1<<$65%AD9NQ*./341#A"),
                new NSequenceWithQuality("AACGSMKCDVGCAAASRYATGGA", "EN;1HUBJ(XAD9N:EU&HT1#A"),
                new NSequenceWithQuality("AACGSMKCDVGCAAASRYATGGA", "EN;1HUBJ(XAD9N:EU&HT1#A"),
                new NSequenceWithQuality("AACGSMKCDVGCAAASRYATGGA", "EN;1HUBJ(XAD9N:EU&HT1#A"),
                new NSequenceWithQuality("AACGVRDSGTGCAAAGVWYAGGA", "EN;1VU83X7AD9N:[:G2<1#A"),
                new NSequenceWithQuality("AACGVRDSGTGCAAAGVWYAGGA", "EN;1VU83X7AD9N:[:G2<1#A"),
                new NSequenceWithQuality("AACGVRDSGTGCAAMGVWYAGGA", "EN;1VU83X7AD9NY[:G2<1#A"),
                new NSequenceWithQuality("AACGSAKSCAGCAAAMKVNTGGA", "EN;14PL(;DAD9N:4BGKF1#A"),
                new NSequenceWithQuality("AACGSAKSCAGCAAAMKVNTGGA", "EN;14PL(;DAD9N:4BGKF1#A"),
                new NSequenceWithQuality("AACGSAKSCAGCAAAMKVNTGGA", "EN;14PL(;DAD9N:4BGKF1#A"),
                new NSequenceWithQuality("AACGSAKSCAGCAAAMKVNTGGA", "EN;14PL(;DAD9N:4BGKF1#A"),
                new NSequenceWithQuality("AACGYBRHBGGCAAASBHKKGGA", "EN;19UEHE%AD9N:77&3:1#A"),
                new NSequenceWithQuality("AACGYBRHBGGCAAASBHKKGGA", "EN;19UEHE%AD9N:77&3:1#A"),
                new NSequenceWithQuality("AACGYBRHBGGCAAASBHKKGGA", "EN;19UEHE%AD9N:77&3:1#A"),
                new NSequenceWithQuality("AACGYBRHBGGCAAASBHKKGGA", "EN;19UEHE%AD9N:77&3:1#A"),
                new NSequenceWithQuality("AACGYBRHBGGCAAWSBHKKGGA", "EN;19UEHE%AD9NK77&3:1#A"),
                new NSequenceWithQuality("AACGYBRHBGGCAAKAASBHKKGGA", "EN;19UEHE%AD9NM*:77&3:1#A"),
                new NSequenceWithQuality("AACGYBRHBGGCAAVASBHKKGGA", "EN;19UEHE%AD9NN:77&3:1#A"))
                .map(seq -> {
                    TByteObjectHashMap<NSequenceWithQuality> sequences = new TByteObjectHashMap<>();
                    sequences.put((byte)1, seq);
                    return sequences;
                }).forEach(inputSequences1::add);

        testData = new CorrectionTestData(sequencesT1R_0_5.size(), keyGroups1, primaryGroups1,
                inputSequences1, groups1, expectedCorrectedGroupValues1);
        preprocessorPort = getPreprocessingResultOutputPort(
                testData.getInputPort(), testData.keyGroups, testData.primaryGroups);
        OutputPort<CorrectionData> correctionDataPort = performSecondaryBarcodesCorrection(preprocessorPort,
                strictCorrectionAlgorithms, testData.keyGroups, 10);
        OutputPort<ParsedRead> parsedReadsPort = testData.getInputPort();
        results = new ArrayList<>();
        for (CorrectionData currentCorrectionData : CUtils.it(correctionDataPort)) {
            for (int i = 0; i < currentCorrectionData.parsedReadsCount; i++) {
                ParsedRead parsedRead = Objects.requireNonNull(parsedReadsPort.take());
                results.add(strictCorrectionAlgorithms.correctBarcodes(parsedRead, currentCorrectionData));
            }
        }
        testData.assertCorrectionResults(results);
    }

    @Test
    public void simpleRandomTest() {
        CorrectionAlgorithms correctionAlgorithms = strictCorrectionAlgorithms;
        for (int i = 0; i < 100; i++) {
            CorrectionTestData testData = generateSimpleRandomTestData();
            OutputPort<CorrectionQualityPreprocessingResult> preprocessorPort = getPreprocessingResultOutputPort(
                    testData.getInputPort(), testData.keyGroups, testData.primaryGroups);
            OutputPort<ParsedRead> parsedReadsPort = testData.getInputPort();
            List<CorrectBarcodesResult> results = new ArrayList<>();
            if (testData.primaryGroups.size() > 0) {
                OutputPort<CorrectionData> correctionDataPort = performSecondaryBarcodesCorrection(preprocessorPort,
                        correctionAlgorithms, testData.keyGroups, rg.nextInt(10) + 1);
                for (CorrectionData correctionData : CUtils.it(correctionDataPort))
                    for (int j = 0; j < correctionData.parsedReadsCount; j++) {
                        ParsedRead parsedRead = Objects.requireNonNull(parsedReadsPort.take());
                        results.add(correctionAlgorithms.correctBarcodes(parsedRead, correctionData));
                    }
            } else {
                CorrectionData correctionData = correctionAlgorithms.prepareCorrectionData(preprocessorPort,
                        testData.keyGroups, 0);
                streamPort(parsedReadsPort).forEach(parsedRead ->
                        results.add(correctionAlgorithms.correctBarcodes(parsedRead, correctionData)));
            }
            testData.assertCorrectionResults(results);
        }
    }

    @Test
    public void mutationsCorrectionTest() {
        CorrectionAlgorithms correctionAlgorithms = simpleCorrectionAlgorithms;
        NucleotideMutationModel mutationModel = getEmpiricalNucleotideMutationModel();
        for (int i = 0; i < 10; i++) {
            int numberOfBarcodes = rg.nextInt(100) + 2;
            int[] barcodeNumInstances = new int[numberOfBarcodes];
            for (int j = 0; j < numberOfBarcodes; j++)
                barcodeNumInstances[j] = rg.nextInt(100) + 1;

            // generating original sequences with barcodes
            boolean withWildcards = rg.nextBoolean();
            float wildcardShare = rg.nextFloat() * 0.2f + 0.2f;
            int originalSequencesLength = rg.nextInt(10) + 30;
            int barcodesLength = withWildcards ? rg.nextInt(15) + 9 : rg.nextInt(20) + 4;
            List<NSequenceWithQuality> originalSequences = new ArrayList<>();
            List<GroupCoordinates> barcodeCoordinates = new ArrayList<>();
            for (int j = 0; j < numberOfBarcodes; j++) {
                NSequenceWithQuality randomSeq = withWildcards
                        ? randomSeqWithWildcardShare(originalSequencesLength, wildcardShare)
                        : randomSeqWithQuality(originalSequencesLength, true);
                int barcodeStart = rg.nextInt(originalSequencesLength - barcodesLength + 1);
                int barcodeEnd = barcodeStart + barcodesLength;
                for (int k = 0; k < barcodeNumInstances[j]; k++) {
                    originalSequences.add(randomSeq);
                    barcodeCoordinates.add(new GroupCoordinates((byte)1, barcodeStart, barcodeEnd));
                }
            }

            // generating sequences with mutated barcodes
            int order = 0;
            List<ReadWithGroupsAndOrder> mutatedReads = new ArrayList<>();
            for (int j = 0; j < numberOfBarcodes; j++) {
                NSequenceWithQuality originalSequence = originalSequences.get(order);
                GroupCoordinates originalCoordinates = barcodeCoordinates.get(order);
                NSequenceWithQuality originalBarcodeValue = originalSequence.getRange(
                        originalCoordinates.start, originalCoordinates.end);
                List<ReadWithGroupsAndOrder> currentBarcodeMutatedReads = new ArrayList<>();
                for (int k = 0; k < barcodeNumInstances[j]; k++) {
                    ReadWithGroups currentRead = new ReadWithGroups();
                    currentRead.targetSequences.put((byte)1, originalSequence);
                    currentRead.groups.put("G", originalCoordinates);
                    // mutating one of the previous barcode values
                    NSequenceWithQuality barcodeValueAfterMutations;
                    if (k == 0)
                        barcodeValueAfterMutations = originalBarcodeValue;
                    else {
                        NSequenceWithQuality barcodeValueBeforeMutations = currentBarcodeMutatedReads
                                .get(rg.nextInt(k)).readWithGroups.getGroupValue("G");
                        Mutations<NucleotideSequence> currentBarcodeMutations = generateMutations(
                                barcodeValueBeforeMutations.getSequence(), mutationModel);
                        barcodeValueAfterMutations = mutateSeqWithRandomQuality(barcodeValueBeforeMutations,
                                currentBarcodeMutations);
                    }
                    // adding read with mutated barcode to list
                    currentBarcodeMutatedReads.add(new ReadWithGroupsAndOrder(
                            currentRead.getReadWithChangedGroup("G", barcodeValueAfterMutations),
                            "G", order++));
                }
                mutatedReads.addAll(currentBarcodeMutatedReads);
            }

            List<ReadWithGroupsAndOrder> unsortedMutatedReads = new ArrayList<>(mutatedReads);
            Collections.sort(mutatedReads);
            CorrectionTestData testData = new CorrectionTestData("G", mutatedReads);

            // correcting barcodes
            OutputPort<CorrectionQualityPreprocessingResult> preprocessorPort = getPreprocessingResultOutputPort(
                    testData.getInputPort(), testData.keyGroups, testData.primaryGroups);
            OutputPort<ParsedRead> parsedReadsPort = testData.getInputPort();
            CorrectionData correctionData = correctionAlgorithms.prepareCorrectionData(preprocessorPort,
                    testData.keyGroups, 0);
            for (Map.Entry<NucleotideSequence, NSequenceWithQuality> entry
                    : correctionData.keyGroupsData.get("G").correctionMap.entrySet()) {
                if (!entry.getKey().equals(entry.getValue().getSequence())) {
                    System.out.println("Correction: " + entry.getKey() + " => " + entry.getValue().getSequence());
                    System.out.println("Equal by wildcards: "
                            + equalByWildcards(entry.getKey(), entry.getValue().getSequence()) + "\n");
                }
            }
            List<CorrectBarcodesResult> results = streamPort(parsedReadsPort).map(parsedRead ->
                    correctionAlgorithms.correctBarcodes(parsedRead, correctionData)).collect(Collectors.toList());

            // extracting corrected barcodes and restoring original order
            List<NSequenceWithQuality> correctedBarcodes = results.stream()
                    .map(r -> r.parsedRead.getGroupValue("G")).collect(Collectors.toList());
            NSequenceWithQuality[] correctedBarcodesWithOriginalOrder = new NSequenceWithQuality[mutatedReads.size()];
            for (int newOrder = 0; newOrder < mutatedReads.size(); newOrder++) {
                int originalOrder = mutatedReads.get(newOrder).originalOrder;
                correctedBarcodesWithOriginalOrder[originalOrder] = correctedBarcodes.get(newOrder);
            }

            // calculating stats
            int matchingMutatedReads = 0;
            int matchingCorrectedReads = 0;
            for (int readIndex = 0; readIndex < originalSequences.size(); readIndex++) {
                GroupCoordinates originalBarcodeCoordinates = barcodeCoordinates.get(readIndex);
                NucleotideSequence originalBarcodeSeq = originalSequences.get(readIndex)
                        .getRange(originalBarcodeCoordinates.start, originalBarcodeCoordinates.end).getSequence();
                NucleotideSequence mutatedBarcodeSeq = unsortedMutatedReads.get(readIndex).readWithGroups
                        .getGroupValue("G").getSequence();
                NucleotideSequence correctedBarcodeSeq = correctedBarcodesWithOriginalOrder[readIndex].getSequence();
                if (equalByWildcards(originalBarcodeSeq, mutatedBarcodeSeq)
                        && !equalByWildcards(originalBarcodeSeq, correctedBarcodeSeq)) {
                    System.out.println("Original: " + originalBarcodeSeq);
                    System.out.println("Mutated: " + mutatedBarcodeSeq);
                    System.out.println("Corrected: " + correctedBarcodeSeq + "\n");
                }
                if (equalByWildcards(originalBarcodeSeq, mutatedBarcodeSeq))
                    matchingMutatedReads++;
                if (equalByWildcards(originalBarcodeSeq, correctedBarcodeSeq))
                    matchingCorrectedReads++;
            }
            float matchingMutatedPercent = (float)matchingMutatedReads / originalSequences.size() * 100;
            float matchingCorrectedPercent = (float)matchingCorrectedReads / originalSequences.size() * 100;
            System.out.println("Number of unique barcodes: " + numberOfBarcodes);
            System.out.println("Number of reads: " + originalSequences.size());
            System.out.println("Wildcards share in barcodes: " + (withWildcards ? wildcardShare : 0));
            System.out.println("Mutated reads that match the original: " + matchingMutatedReads + " ("
                    + floatFormat.format(matchingMutatedPercent) + "%)");
            System.out.println("Corrected reads that match the original: " + matchingCorrectedReads + " ("
                    + floatFormat.format(matchingCorrectedPercent) + "%)");
            System.out.println();
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
        List<Map<String, NucleotideSequence>> preparedExpectedGroups = new ArrayList<>();

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
                preparedExpectedGroups.add(currentReadWithGroups.getSequencesForGroups(keyGroups));
                generatedSeqCount++;
            }
        }

        return new CorrectionTestData(numberOfTargets, new LinkedHashSet<>(keyGroups),
                new LinkedHashSet<>(primaryGroups), preparedInputSequences, preparedGroups, preparedExpectedGroups);
    }

    private static ReadWithGroups mutate(ReadWithGroups input, int numErrors) {
        ReadWithGroups output = input;
        int totalMutations = 0;
        while (totalMutations < numErrors) {
            List<String> keyGroupNames = input.groups.keySet().stream()
                    .filter(groupName -> groupName.charAt(0) == 'G').collect(Collectors.toList());
            String randomGroupName = keyGroupNames.get(rg.nextInt(keyGroupNames.size()));
            GroupCoordinates groupCoordinates = input.groups.get(randomGroupName);
            NSequenceWithQuality target = input.targetSequences.get(groupCoordinates.targetId);
            NSequenceWithQuality groupValue = target.getRange(groupCoordinates.start, groupCoordinates.end);
            int currentMutationsNum = rg.nextInt(numErrors - totalMutations) + 1;
            NSequenceWithQuality mutatedGroupValue = makeRandomErrors(groupValue, currentMutationsNum);
            output = output.getReadWithChangedGroup(randomGroupName, mutatedGroupValue);
            totalMutations += currentMutationsNum;
        }
        return output;
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
            ReadWithGroups currentReadWithGroups = input;
            for (Map.Entry<String, NSequenceWithQuality> entry : cluster.groupValues.entrySet())
                currentReadWithGroups = currentReadWithGroups.getReadWithChangedGroup(
                        entry.getKey(), entry.getValue());
            updatedSequencesForClusters.add(currentReadWithGroups);
        }
        return updatedSequencesForClusters;
    }

    private static class CorrectionTestData {
        final int numberOfTargets;
        final LinkedHashSet<String> keyGroups;
        final LinkedHashSet<String> primaryGroups;
        final List<ReadWithGroups> inputReadsWithGroups;
        final List<Map<String, NucleotideSequence>> expectedCorrectedGroupValues;

        // simplified constructor for single group in single target without expected corrected values
        CorrectionTestData(
                String keyGroupName, List<NSequenceWithQuality> inputSequences, List<GroupCoordinates> groups) {
            this(1, new LinkedHashSet<>(Collections.singleton(keyGroupName)), new LinkedHashSet<>(),
                    inputSequences.stream().map(seq -> {
                        TByteObjectHashMap<NSequenceWithQuality> map = new TByteObjectHashMap<>();
                        map.put((byte)1, seq);
                        return map;
                    }).collect(Collectors.toList()),
                    groups.stream().map(coordinates -> {
                        Map<String, GroupCoordinates> map = new HashMap<>();
                        map.put(keyGroupName, coordinates);
                        return map;
                    }).collect(Collectors.toList()), null);
        }

        // single group in single target, no expected values; data is prepared in ReadWithGroupAndOrder objects
        CorrectionTestData(String keyGroupName, List<ReadWithGroupsAndOrder> readsWithSavedOrder) {
            ParsedRead.clearStaticCache();
            this.numberOfTargets = 1;
            this.keyGroups = new LinkedHashSet<>(Collections.singleton(keyGroupName));
            this.primaryGroups = new LinkedHashSet<>();
            this.expectedCorrectedGroupValues = null;
            this.inputReadsWithGroups = readsWithSavedOrder.stream().map(r -> r.readWithGroups)
                    .collect(Collectors.toList());
        }

        CorrectionTestData(
                int numberOfTargets, LinkedHashSet<String> keyGroups, LinkedHashSet<String> primaryGroups,
                List<TByteObjectHashMap<NSequenceWithQuality>> inputSequences,
                List<Map<String, GroupCoordinates>> groups,
                List<Map<String, NucleotideSequence>> expectedCorrectedGroupValues) {
            ParsedRead.clearStaticCache();
            this.numberOfTargets = numberOfTargets;
            this.keyGroups = keyGroups;
            this.primaryGroups = primaryGroups;
            this.expectedCorrectedGroupValues = expectedCorrectedGroupValues;
            assertEquals(inputSequences.size(), groups.size());
            List<ReadWithGroups> inputReadsWithGroups = new ArrayList<>();
            for (int i = 0; i < inputSequences.size(); i++) {
                ReadWithGroups readWithGroups = new ReadWithGroups();
                readWithGroups.targetSequences.putAll(inputSequences.get(i));
                readWithGroups.groups.putAll(groups.get(i));
                inputReadsWithGroups.add(readWithGroups);
            }
            this.inputReadsWithGroups = inputReadsWithGroups;
        }

        OutputPort<ParsedRead> getInputPort() {
            return new OutputPort<ParsedRead>() {
                int counter = 0;

                @Override
                public ParsedRead take() {
                    if (counter == inputReadsWithGroups.size())
                        return null;
                    ReadWithGroups currentReadWithGroups = inputReadsWithGroups.get(counter);
                    TByteObjectHashMap<NSequenceWithQuality> inputSequences = currentReadWithGroups.targetSequences;
                    Map<String, GroupCoordinates> groups = currentReadWithGroups.groups;
                    SequenceRead originalRead;
                    switch (numberOfTargets) {
                        case 1:
                            originalRead = new SingleReadImpl(counter, inputSequences.get((byte)1), "");
                            break;
                        case 2:
                            originalRead = new PairedRead(
                                    new SingleReadImpl(counter, inputSequences.get((byte)1), ""),
                                    new SingleReadImpl(counter, inputSequences.get((byte)2), ""));
                            break;
                        default:
                            SingleRead[] originalReads = new SingleRead[numberOfTargets];
                            for (int i = 0; i < numberOfTargets; i++)
                                originalReads[i] = new SingleReadImpl(
                                        counter, inputSequences.get((byte)(i + 1)), "");
                            originalRead = new MultiRead(originalReads);
                    }
                    ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
                    for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
                        NSequenceWithQuality target = inputSequences.get(targetId);
                        String groupName = "R" + targetId;
                        matchedGroupEdges.add(new MatchedGroupEdge(target, targetId,
                                new GroupEdge(groupName, true), 0));
                        matchedGroupEdges.add(new MatchedGroupEdge(target, targetId,
                                new GroupEdge(groupName, false), target.size()));
                    }
                    for (Map.Entry<String, GroupCoordinates> groupEntry : groups.entrySet()) {
                        String groupName = groupEntry.getKey();
                        GroupCoordinates groupCoordinates = groupEntry.getValue();
                        NSequenceWithQuality target = inputSequences.get(groupCoordinates.targetId);
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
            assertEquals(inputReadsWithGroups.size(), expectedCorrectedGroupValues.size());
            assertEquals(expectedCorrectedGroupValues.size(), results.size());
            for (int i = 0; i < results.size(); i++) {
                Map<String, NucleotideSequence> currentExpectedGroupValues = expectedCorrectedGroupValues.get(i);
                Map<String, NucleotideSequence> currentInputGroupValues = inputReadsWithGroups.get(i)
                        .getSequencesForGroups(keyGroups);
                boolean expectedCorrection = false;
                for (Map.Entry<String, NucleotideSequence> entry : currentExpectedGroupValues.entrySet()) {
                    NucleotideSequence expectedResult = entry.getValue();
                    NucleotideSequence actualResult = results.get(i).parsedRead
                            .getGroupValue(entry.getKey()).getSequence();
                    assertEquals(expectedResult, actualResult);
                    expectedCorrection |= !expectedResult.equals(currentInputGroupValues.get(entry.getKey()));
                }
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

        @Override
        public String toString() {
            return "{" + targetId + ":" + start + "-" + end + '}';
        }
    }

    private static class ReadWithGroups {
        // keys: targetIds, starting from 1 (numbers after "R" in R1, R2 etc)
        final TByteObjectHashMap<NSequenceWithQuality> targetSequences = new TByteObjectHashMap<>();
        final Map<String, GroupCoordinates> groups = new HashMap<>();

        ReadWithGroups getReadWithChangedGroup(String groupName, NSequenceWithQuality newValue) {
            GroupCoordinates groupCoordinates = groups.get(groupName);
            NSequenceWithQuality target = targetSequences.get(groupCoordinates.targetId);
            NSequenceWithQualityBuilder newTarget = new NSequenceWithQualityBuilder();
            newTarget.append(target.getRange(0, groupCoordinates.start));
            newTarget.append(newValue);
            newTarget.append(target.getRange(groupCoordinates.end, target.size()));
            ReadWithGroups newReadWithGroups = new ReadWithGroups();
            newReadWithGroups.targetSequences.putAll(targetSequences);
            newReadWithGroups.targetSequences.put(groupCoordinates.targetId, newTarget.createAndDestroy());
            Map<String, GroupCoordinates> newGroups = newReadWithGroups.groups;
            for (Map.Entry<String, GroupCoordinates> entry : groups.entrySet()) {
                GroupCoordinates currentEntryCoordinates = entry.getValue();
                if (currentEntryCoordinates.targetId != groupCoordinates.targetId)
                    newGroups.put(entry.getKey(), currentEntryCoordinates);
                else {
                    int lengthDiff = newValue.size() - (groupCoordinates.end - groupCoordinates.start);
                    int newStart = (currentEntryCoordinates.start >= groupCoordinates.end)
                            ? currentEntryCoordinates.start + lengthDiff : currentEntryCoordinates.start;
                    int newEnd = (currentEntryCoordinates.end >= groupCoordinates.end)
                            ? currentEntryCoordinates.end + lengthDiff : currentEntryCoordinates.end;
                    newGroups.put(entry.getKey(), new GroupCoordinates(groupCoordinates.targetId, newStart, newEnd));
                }
            }
            return newReadWithGroups;
        }

        Map<String, NucleotideSequence> getSequencesForGroups(Collection<String> keyGroups) {
            Map<String, NucleotideSequence> results = new HashMap<>();
            for (String groupName : keyGroups) {
                GroupCoordinates groupCoordinates = groups.get(groupName);
                NucleotideSequence targetSeq = targetSequences.get(groupCoordinates.targetId).getSequence();
                NucleotideSequence groupValue = targetSeq.getRange(groupCoordinates.start, groupCoordinates.end);
                results.put(groupName, groupValue);
            }
            return results;
        }

        NSequenceWithQuality getGroupValue(String groupName) {
            GroupCoordinates groupCoordinates = groups.get(groupName);
            NSequenceWithQuality targetSeq = targetSequences.get(groupCoordinates.targetId);
            return targetSeq.getRange(groupCoordinates.start, groupCoordinates.end);
        }
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

    // these reads can be sorted by values of key group, and original order of reads will be stored
    private static class ReadWithGroupsAndOrder implements Comparable<ReadWithGroupsAndOrder> {
        final ReadWithGroups readWithGroups;
        final String keyGroupValue;
        final int originalOrder;

        ReadWithGroupsAndOrder(ReadWithGroups readWithGroups, String keyGroupName, int originalOrder) {
            this.readWithGroups = readWithGroups;
            this.keyGroupValue = readWithGroups.getSequencesForGroups(Collections.singleton(keyGroupName))
                    .get(keyGroupName).toString();
            this.originalOrder = originalOrder;
        }

        @Override
        public int compareTo(ReadWithGroupsAndOrder other) {
            return keyGroupValue.compareTo(other.keyGroupValue);
        }
    }
}
