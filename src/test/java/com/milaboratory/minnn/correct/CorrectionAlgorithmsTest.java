/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
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
import com.milaboratory.core.clustering.Cluster;
import com.milaboratory.core.clustering.Clustering;
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
import gnu.trove.map.hash.TObjectIntHashMap;
import org.junit.*;

import java.util.*;
import java.util.stream.*;

import static com.milaboratory.core.mutations.generator.MutationModels.getEmpiricalNucleotideMutationModel;
import static com.milaboratory.core.mutations.generator.MutationsGenerator.generateMutations;
import static com.milaboratory.minnn.cli.CliUtils.floatFormat;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.correct.CorrectionAlgorithms.*;
import static com.milaboratory.minnn.correct.CorrectionAlgorithmsTest.WildcardsOption.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.CommonUtils.*;
import static org.junit.Assert.*;

public class CorrectionAlgorithmsTest {
    private static final int THRESHOLD_FOR_WRONG_NEW_BARCODES = 5;
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
    public void wildcardsClusteringTest() {
        List<SequenceWithWildcardsCount> inputSequences = Stream.of(
                "TTTGGGCCC",
                "TTTNNNCCC",
                "NNTGGGCCC",
                "TTTGGGCNN",
                "NTTGGGCCN",
                "TDDNNC",
                "TNNGGCCC",
                "TTTGGGCCC",
                "NTTGGGCCN",
                "BBBGGGCCC",
                "TTTDDDDDD",
                "TTTCCC")
                .map(seqStr -> new SequenceWithWildcardsCount(new NSequenceWithQuality(new NucleotideSequence(seqStr),
                        DEFAULT_MAX_QUALITY))).collect(Collectors.toList());
        Clustering<SequenceWithWildcardsCount, SequenceWithQualityForClustering> clustering = new Clustering<>(
                inputSequences, new SequenceCounterExtractor<>(),
                new WildcardClusteringStrategy(5));
        List<Cluster<SequenceWithWildcardsCount>> clusters = clustering.performClustering();
        assertEquals(5, clusters.size());

        assertSeq("TTTGGGCCC", clusters.get(0).getHead());
        List<Cluster<SequenceWithWildcardsCount>> children = new ArrayList<>();
        clusters.get(0).processAllChildren(children::add);
        assertEquals(6, children.size());
        assertSeq("NNTGGGCCC", children.get(0).getHead());
        assertSeq("NTTGGGCCN", children.get(1).getHead());
        assertSeq("NTTGGGCCN", children.get(2).getHead());
        assertSeq("TTTGGGCNN", children.get(3).getHead());
        assertSeq("BBBGGGCCC", children.get(4).getHead());
        assertSeq("TTTNNNCCC", children.get(5).getHead());

        assertSeq("TTTGGGCCC", clusters.get(1).getHead());
        children = new ArrayList<>();
        clusters.get(1).processAllChildren(children::add);
        assertEquals(0, children.size());

        assertSeq("TTTCCC", clusters.get(2).getHead());
        children = new ArrayList<>();
        clusters.get(2).processAllChildren(children::add);
        assertEquals(1, children.size());
        assertSeq("TDDNNC", children.get(0).getHead());

        assertSeq("TNNGGCCC", clusters.get(3).getHead());
        children = new ArrayList<>();
        clusters.get(3).processAllChildren(children::add);
        assertEquals(0, children.size());

        assertSeq("TTTDDDDDD", clusters.get(4).getHead());
        children = new ArrayList<>();
        clusters.get(4).processAllChildren(children::add);
        assertEquals(0, children.size());
    }

    @Test
    public void wildcardsCorrectionTest() {
        CorrectionAlgorithms wildcardsCorrectionAlgorithms = new CorrectionAlgorithms(
                new BarcodeClusteringStrategyFactory(-1, 0, 1, 1,
                        new SimpleMutationProbability(0, 0)),
                0, 0, 7);
        List<NSequenceWithQuality> inputGroupValues = Stream.of(
                "ATTAGACA",
                "ATTAGACA",
                "ATTAGACA",
                "BTTAGGCA",
                "DTTAGACA",
                "ATTNNACA",
                "ATNNGACA",
                "NTNAGACA",
                "BTTDGNCA",
                "BTTANNCA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "ATTANNNN",
                "TNNNNNNN")
                .map(seqStr -> new NSequenceWithQuality(new NucleotideSequence(seqStr), DEFAULT_MAX_QUALITY))
                .collect(Collectors.toList());
        List<NucleotideSequence> expectedCorrectedGroupValues = Stream.of(
                "ATTAGACA",
                "ATTAGACA",
                "ATTAGACA",
                "TTTAGGCA",
                "ATTAGACA",
                "ATTAGACA",
                "ATTAGACA",
                "ATTAGACA",
                "TTTAGGCA",
                "TTTAGGCA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "BTTAGNNA",
                "ATTAGACA",
                "TTTAGGCA")
                .map(NucleotideSequence::new).collect(Collectors.toList());
        CorrectionTestData testData = new CorrectionTestData(inputGroupValues, expectedCorrectedGroupValues);

        // perform correction
        OutputPort<CorrectionQualityPreprocessingResult> preprocessorPort = getPreprocessingResultOutputPort(
                testData.getInputPort(), testData.keyGroups, testData.primaryGroups);
        CorrectionData correctionData = wildcardsCorrectionAlgorithms.prepareCorrectionData(preprocessorPort,
                testData.keyGroups, 0);
        List<CorrectBarcodesResult> results = new ArrayList<>();
        for (ParsedRead parsedRead : CUtils.it(testData.getInputPort()))
            results.add(wildcardsCorrectionAlgorithms.correctBarcodes(parsedRead, correctionData));

        // check correction results
        testData.assertCorrectionResults(results);
    }

    @Test
    public void shortBarcodesMutationsTest() {
        float mutationProbabilitiesMultiplier = 0.5f;
        int numberOfBarcodes = 60;
        int maxNumberOfInstances = 15000;
        int barcodesLength = 5;
        CorrectionTestStats stats = mutationsCorrectionTest(DEFAULT_CORRECT_SINGLE_SUBSTITUTION_PROBABILITY,
                DEFAULT_CORRECT_SINGLE_INDEL_PROBABILITY, DEFAULT_MAX_ERRORS_SHARE,
                DEFAULT_CORRECT_MAX_CLUSTER_DEPTH, mutationProbabilitiesMultiplier, numberOfBarcodes,
                maxNumberOfInstances, NO_WILDCARDS, barcodesLength);
        stats.printCorrectionMap();
        stats.print();
    }

    @Test
    public void longBarcodesMutationsTest() {
        float mutationProbabilitiesMultiplier = 1f;
        int numberOfBarcodes = 100;
        int maxNumberOfInstances = 15000;
        int barcodesLength = 14;
        CorrectionTestStats stats = mutationsCorrectionTest(DEFAULT_CORRECT_SINGLE_SUBSTITUTION_PROBABILITY,
                DEFAULT_CORRECT_SINGLE_INDEL_PROBABILITY, 0.25f,
                DEFAULT_CORRECT_MAX_CLUSTER_DEPTH, mutationProbabilitiesMultiplier, numberOfBarcodes,
                maxNumberOfInstances, NO_WILDCARDS, barcodesLength);
        stats.printCorrectionMap();
        stats.print();
    }

    @Test
    public void barcodesWithWildcardsMutationsTest() {
        float mutationProbabilitiesMultiplier = 1f;
        int numberOfBarcodes = 100;
        int maxNumberOfInstances = 15000;
        int barcodesLength = 14;
        CorrectionTestStats stats = mutationsCorrectionTest(DEFAULT_CORRECT_SINGLE_SUBSTITUTION_PROBABILITY,
                DEFAULT_CORRECT_SINGLE_INDEL_PROBABILITY, DEFAULT_MAX_ERRORS_SHARE,
                DEFAULT_CORRECT_MAX_CLUSTER_DEPTH, mutationProbabilitiesMultiplier, numberOfBarcodes,
                maxNumberOfInstances, WITH_WILDCARDS, barcodesLength);
        stats.printCorrectionMap();
        stats.print();
    }

    @Test
    public void barcodesWithNMutationsTest() {
        float mutationProbabilitiesMultiplier = 1f;
        int numberOfBarcodes = 100;
        int maxNumberOfInstances = 15000;
        int barcodesLength = 14;
        CorrectionTestStats stats = mutationsCorrectionTest(DEFAULT_CORRECT_SINGLE_SUBSTITUTION_PROBABILITY,
                DEFAULT_CORRECT_SINGLE_INDEL_PROBABILITY, DEFAULT_MAX_ERRORS_SHARE,
                DEFAULT_CORRECT_MAX_CLUSTER_DEPTH, mutationProbabilitiesMultiplier, numberOfBarcodes,
                maxNumberOfInstances, WITH_N, barcodesLength);
        stats.printCorrectionMap();
        stats.print();
    }

    @Test
    public void randomMutationsTest() {
        for (int i = 0; i < 10; i++) {
            float mutationProbabilitiesMultiplier = rg.nextFloat() * 0.75f + 0.25f;
            int numberOfBarcodes = rg.nextInt(100) + 2;
            int maxNumberOfInstances = rg.nextInt(10000) + 10000;
            WildcardsOption wildcardsOption = getRandomEnumItem(WildcardsOption.class);
            int barcodesLength = (wildcardsOption == NO_WILDCARDS) ? rg.nextInt(20) + 4 : rg.nextInt(15) + 9;
            CorrectionTestStats stats = mutationsCorrectionTest(DEFAULT_CORRECT_SINGLE_SUBSTITUTION_PROBABILITY,
                    DEFAULT_CORRECT_SINGLE_INDEL_PROBABILITY, DEFAULT_MAX_ERRORS_SHARE,
                    DEFAULT_CORRECT_MAX_CLUSTER_DEPTH, mutationProbabilitiesMultiplier, numberOfBarcodes,
                    maxNumberOfInstances, wildcardsOption, barcodesLength);
            stats.printCorrectionMap();
            stats.print();
        }
    }

    private static CorrectionTestStats mutationsCorrectionTest(
            float singleSubstitutionProbability, float singleIndelProbability, float maxErrorsShare,
            int maxClusterDepth, float mutationProbabilitiesMultiplier, int numberOfBarcodes, int maxNumberOfInstances,
            WildcardsOption wildcardsOption, int barcodesLength) {
        CorrectionAlgorithms correctionAlgorithms = new CorrectionAlgorithms(
                new BarcodeClusteringStrategyFactory(maxErrorsShare, -1, DEFAULT_CORRECT_CLUSTER_THRESHOLD,
                        maxClusterDepth,
                        new SimpleMutationProbability(singleSubstitutionProbability, singleIndelProbability)),
                0, 0, DEFAULT_CORRECT_WILDCARDS_COLLAPSING_MERGE_THRESHOLD);
        NucleotideMutationModel mutationModel = getEmpiricalNucleotideMutationModel()
                .multiplyProbabilities(mutationProbabilitiesMultiplier);
        int[] barcodeNumInstances = new int[numberOfBarcodes];
        for (int j = 0; j < numberOfBarcodes; j++)
            barcodeNumInstances[j] = Math.max(1, Math.min(maxNumberOfInstances, (int)getRandomLogNormal(
                    Math.log(maxNumberOfInstances) / 3, Math.log(maxNumberOfInstances) / 3)));

        // generating original sequences with barcodes
        float wildcardShare = (wildcardsOption == NO_WILDCARDS) ? 0 : rg.nextFloat() * 0.2f + 0.2f;
        int originalSequencesLength = barcodesLength + rg.nextInt(20) + 10;
        List<NSequenceWithQuality> originalSequences = new ArrayList<>();
        UniqueSequencesSet uniqueBarcodeSequences = new UniqueSequencesSet();
        List<GroupCoordinates> barcodeCoordinates = new ArrayList<>();
        int j = 0;
        int failuresCount = 0;
        while (uniqueBarcodeSequences.size() < numberOfBarcodes) {
            NSequenceWithQuality randomSeq;
            switch (wildcardsOption) {
                case NO_WILDCARDS:
                    randomSeq = randomSeqWithQuality(originalSequencesLength, true);
                    break;
                case WITH_WILDCARDS:
                    randomSeq = randomSeqWithWildcardShare(originalSequencesLength, wildcardShare, false);
                    break;
                case WITH_N:
                    randomSeq = randomSeqWithWildcardShare(originalSequencesLength, wildcardShare, true);
                    break;
                default:
                    throw new IllegalStateException();
            }
            int barcodeStart = rg.nextInt(originalSequencesLength - barcodesLength + 1);
            int barcodeEnd = barcodeStart + barcodesLength;
            if (!uniqueBarcodeSequences.contains(randomSeq.getSequence().getRange(barcodeStart, barcodeEnd))) {
                uniqueBarcodeSequences.add(randomSeq.getSequence().getRange(barcodeStart, barcodeEnd));
                for (int k = 0; k < barcodeNumInstances[j]; k++) {
                    originalSequences.add(randomSeq);
                    barcodeCoordinates.add(new GroupCoordinates((byte)1, barcodeStart, barcodeEnd));
                }
                j++;
            } else
                failuresCount++;
            if (failuresCount > numberOfBarcodes * 10) {
                /* restart barcodes generation if barcode with too many wildcards is in set,
                 * and it prevents generation of required number of unique barcodes */
                uniqueBarcodeSequences.clear();
                originalSequences.clear();
                barcodeCoordinates.clear();
                j = 0;
                failuresCount = 0;
            }
        }

        // generating sequences with mutated barcodes
        int order = 0;
        List<ReadWithGroupsAndOrder> mutatedReads = new ArrayList<>();
        for (j = 0; j < numberOfBarcodes; j++) {
            NSequenceWithQuality originalSequence = originalSequences.get(order);
            GroupCoordinates originalCoordinates = barcodeCoordinates.get(order);
            List<ReadWithGroupsAndOrder> currentBarcodeMutatedReads = new ArrayList<>();
            ReadWithGroups currentOriginalRead = new ReadWithGroups();
            currentOriginalRead.targetSequences.put((byte)1, originalSequence);
            currentOriginalRead.groups.put("G", originalCoordinates);
            for (ReadWithGroups mutatedRead : createMutatedReads(
                    mutationModel, currentOriginalRead, "G", barcodeNumInstances[j]))
                currentBarcodeMutatedReads.add(new ReadWithGroupsAndOrder(mutatedRead, "G", order++));
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
        LinkedHashMap<NucleotideSequence, NucleotideSequence> correctionMap = new LinkedHashMap<>();
        for (Map.Entry<NucleotideSequence, NSequenceWithQuality> entry
                : correctionData.keyGroupsData.get("G").correctionMap.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().getSequence()))
                correctionMap.put(entry.getKey(), entry.getValue().getSequence());
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
        CorrectionStats correctionStats = correctionData.stats;
        for (CorrectBarcodesResult result : results) {
            if (result.corrected)
                correctionStats.correctedReads++;
            else if (result.qualityUpdated)
                correctionStats.updatedQualityReads++;
        }
        int matchingMutatedReads = 0;
        int matchingCorrectedReads = 0;
        int wronglyCorrectedReads = 0;
        int newBarcodesTotalReads = 0;
        HashMap<NucleotideSequence, WrongBarcodesCounter> newBarcodesCounters = new HashMap<>();
        LinkedHashSet<TestResultSequences> wronglyCorrectedSequences = new LinkedHashSet<>();
        Set<NucleotideSequence> uniqueBarcodesAfterMutation = new HashSet<>();
        Set<NucleotideSequence> uniqueBarcodesAfterCorrection = new HashSet<>();
        for (int readIndex = 0; readIndex < originalSequences.size(); readIndex++) {
            GroupCoordinates originalBarcodeCoordinates = barcodeCoordinates.get(readIndex);
            NucleotideSequence originalBarcodeSeq = originalSequences.get(readIndex)
                    .getRange(originalBarcodeCoordinates.start, originalBarcodeCoordinates.end).getSequence();
            NucleotideSequence mutatedBarcodeSeq = unsortedMutatedReads.get(readIndex).readWithGroups
                    .getGroupValue("G").getSequence();
            NucleotideSequence correctedBarcodeSeq = correctedBarcodesWithOriginalOrder[readIndex].getSequence();
            uniqueBarcodesAfterMutation.add(mutatedBarcodeSeq);
            uniqueBarcodesAfterCorrection.add(correctedBarcodeSeq);
            if (equalByWildcards(originalBarcodeSeq, mutatedBarcodeSeq)
                    && !equalByWildcards(originalBarcodeSeq, correctedBarcodeSeq)) {
                wronglyCorrectedSequences.add(new TestResultSequences(
                        originalBarcodeSeq, mutatedBarcodeSeq, correctedBarcodeSeq));
                wronglyCorrectedReads++;
            }
            if (equalByWildcards(originalBarcodeSeq, mutatedBarcodeSeq))
                matchingMutatedReads++;
            if (equalByWildcards(originalBarcodeSeq, correctedBarcodeSeq))
                matchingCorrectedReads++;
            if (!uniqueBarcodeSequences.contains(correctedBarcodeSeq)) {
                newBarcodesCounters.putIfAbsent(correctedBarcodeSeq, new WrongBarcodesCounter());
                newBarcodesCounters.get(correctedBarcodeSeq).add(new TestResultSequences(
                        originalBarcodeSeq, mutatedBarcodeSeq, correctedBarcodeSeq));
                newBarcodesTotalReads++;
            }
        }
        return new CorrectionTestStats(originalSequences.size(), matchingMutatedReads, matchingCorrectedReads,
                wronglyCorrectedReads, newBarcodesTotalReads, numberOfBarcodes, uniqueBarcodesAfterMutation.size(),
                uniqueBarcodesAfterCorrection.size(), wildcardShare,
                calculateMaxErrors(testData, "G", maxErrorsShare), maxClusterDepth,
                mutationProbabilitiesMultiplier, barcodeNumInstances, THRESHOLD_FOR_WRONG_NEW_BARCODES,
                newBarcodesCounters.values(), wronglyCorrectedSequences, correctionMap, correctionStats);
    }

    private static int calculateMaxErrors(CorrectionTestData testData, String groupName, float maxErrorsShare) {
        long lengthSum = 0;
        int numberOfReads = 0;
        for (ParsedRead parsedRead : CUtils.it(testData.getInputPort())) {
            lengthSum += parsedRead.getGroupValue(groupName).size();
            numberOfReads++;
        }
        return Math.max(1, Math.round(maxErrorsShare * lengthSum / numberOfReads));
    }

    private static List<ReadWithGroups> createMutatedReads(
            NucleotideMutationModel mutationModel, ReadWithGroups originalRead, String barcodeGroup, int quantity) {
        List<ReadWithGroups> mutatedReads = new ArrayList<>();
        mutatedReads.add(originalRead);
        for (int i = 0; i < quantity - 1; i++) {
            NSequenceWithQuality barcodeValueBeforeMutations = originalRead.getGroupValue(barcodeGroup);
            Mutations<NucleotideSequence> mutations = generateMutations(
                    barcodeValueBeforeMutations.getSequence(), mutationModel);
            NSequenceWithQuality barcodeValueAfterMutations = mutateSeqWithRandomQuality(
                    barcodeValueBeforeMutations, mutations);
            mutatedReads.add(originalRead.getReadWithChangedGroup(barcodeGroup,
                    barcodeValueAfterMutations));

        }
        Collections.shuffle(mutatedReads);
        return mutatedReads;
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

    private void assertSeq(String expected, SequenceWithWildcardsCount actual) {
        assertEquals(expected, actual.seq.getSequence().toString());
    }

    private static class CorrectionTestData {
        final int numberOfTargets;
        final LinkedHashSet<String> keyGroups;
        final LinkedHashSet<String> primaryGroups;
        final List<ReadWithGroups> inputReadsWithGroups;
        final List<Map<String, NucleotideSequence>> expectedCorrectedGroupValues;

        // simple list of sequences correction
        CorrectionTestData(
                Collection<NSequenceWithQuality> inputGroupValues,
                Collection<NucleotideSequence> expectedGroupValues) {
            this(1, new LinkedHashSet<>(Collections.singleton("G")), new LinkedHashSet<>(),
                    inputGroupValues.stream().map(seq -> {
                        TByteObjectHashMap<NSequenceWithQuality> inputSequence = new TByteObjectHashMap<>();
                        inputSequence.put((byte)1, seq);
                        return inputSequence;
                    }).collect(Collectors.toList()),

                    inputGroupValues.stream().map(seq -> {
                        Map<String, GroupCoordinates> group = new HashMap<>();
                        group.put("G", new GroupCoordinates((byte)1, 0, seq.size()));
                        return group;
                    }).collect(Collectors.toList()),

                    expectedGroupValues.stream().map(seq -> {
                        Map<String, NucleotideSequence> expectedCorrectedGroupValue = new HashMap<>();
                        expectedCorrectedGroupValue.put("G", seq);
                        return expectedCorrectedGroupValue;
                    }).collect(Collectors.toList()));
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

    enum WildcardsOption {
        NO_WILDCARDS, WITH_WILDCARDS, WITH_N
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

    private static class CorrectionTestStats {
        final int numberOfReads;
        final int matchingMutatedReads;
        final int matchingCorrectedReads;
        final int wronglyCorrectedReads;
        final int newBarcodesTotalReads;
        final float matchingMutatedPercent;
        final float matchingCorrectedPercent;
        final float wronglyCorrectedPercent;
        final float newBarcodesReadsPercent;
        final int uniqueBarcodesOriginal;
        final int uniqueBarcodesAfterMutation;
        final int uniqueBarcodesAfterCorrection;
        final float wildcardsShareInBarcodes;
        final int maxErrors;
        final int clusteringDepth;
        final float probabilitiesMultiplier;
        final int[] originalBarcodesCounts;
        final int thresholdForWrongNewBarcodes;
        final Collection<WrongBarcodesCounter> newBarcodesCounters;
        final List<WrongBarcodesCounter> wrongBarcodesCounters;
        final Collection<TestResultSequences> wronglyCorrectedSequences;
        final LinkedHashMap<NucleotideSequence, NucleotideSequence> correctionMap;
        final CorrectionStats correctionStats;

        CorrectionTestStats(
                int numberOfReads, int matchingMutatedReads, int matchingCorrectedReads, int wronglyCorrectedReads,
                int newBarcodesTotalReads, int uniqueBarcodesOriginal, int uniqueBarcodesAfterMutation,
                int uniqueBarcodesAfterCorrection, float wildcardsShareInBarcodes, int maxErrors, int clusteringDepth,
                float probabilitiesMultiplier, int[] originalBarcodesCounts,
                int thresholdForWrongNewBarcodes, Collection<WrongBarcodesCounter> newBarcodesCounters,
                Collection<TestResultSequences> wronglyCorrectedSequences,
                LinkedHashMap<NucleotideSequence, NucleotideSequence> correctionMap, CorrectionStats correctionStats) {
            this.numberOfReads = numberOfReads;
            this.matchingMutatedReads = matchingMutatedReads;
            this.matchingCorrectedReads = matchingCorrectedReads;
            this.wronglyCorrectedReads = wronglyCorrectedReads;
            this.newBarcodesTotalReads = newBarcodesTotalReads;
            this.matchingMutatedPercent = (float)matchingMutatedReads / numberOfReads * 100;
            this.matchingCorrectedPercent = (float)matchingCorrectedReads / numberOfReads * 100;
            this.wronglyCorrectedPercent = (float)wronglyCorrectedReads / numberOfReads * 100;
            this.newBarcodesReadsPercent = (float)newBarcodesTotalReads / numberOfReads * 100;
            this.uniqueBarcodesOriginal = uniqueBarcodesOriginal;
            this.uniqueBarcodesAfterMutation = uniqueBarcodesAfterMutation;
            this.uniqueBarcodesAfterCorrection = uniqueBarcodesAfterCorrection;
            this.wildcardsShareInBarcodes = wildcardsShareInBarcodes;
            this.maxErrors = maxErrors;
            this.clusteringDepth = clusteringDepth;
            this.probabilitiesMultiplier = probabilitiesMultiplier;
            this.originalBarcodesCounts = originalBarcodesCounts;
            this.thresholdForWrongNewBarcodes = thresholdForWrongNewBarcodes;
            this.newBarcodesCounters = newBarcodesCounters;
            this.wrongBarcodesCounters = newBarcodesCounters.stream()
                    .filter(c -> c.totalCount >= thresholdForWrongNewBarcodes).collect(Collectors.toList());
            this.wronglyCorrectedSequences = wronglyCorrectedSequences;
            this.correctionMap = correctionMap;
            this.correctionStats = correctionStats;
        }

        void print() {
            if (wronglyCorrectedReads > 0) {
                System.out.println("Wrongly corrected sequences:\n");
                wronglyCorrectedSequences.forEach(TestResultSequences::print);
            }
            if (wrongBarcodesCounters.size() > 0) {
                System.out.println("Wrong new barcodes (count threshold " + thresholdForWrongNewBarcodes + "):\n");
                wrongBarcodesCounters.forEach(WrongBarcodesCounter::print);
            } else if (newBarcodesTotalReads > 0)
                System.out.println("Counts for all new barcodes are below the threshold ("
                        + thresholdForWrongNewBarcodes + ")\n");
            System.out.println("Original number of unique barcodes: " + uniqueBarcodesOriginal);
            System.out.println("Number of unique barcodes after mutation: " + uniqueBarcodesAfterMutation);
            System.out.println("Number of unique barcodes after correction: " + uniqueBarcodesAfterCorrection);
            System.out.println("Number of reads: " + numberOfReads);
            System.out.println("Original barcodes counts: " + Arrays.stream(originalBarcodesCounts)
                    .boxed().sorted((c1, c2) -> Long.compare(c2, c1)).collect(Collectors.toList()));
            System.out.println("Wildcards share in barcodes: " + wildcardsShareInBarcodes);
            System.out.println("Maximum allowed errors in correction: " + maxErrors);
            System.out.println("Clustering depth: " + clusteringDepth);
            System.out.println("Mutation model probabilities multiplier: " + probabilitiesMultiplier);
            System.out.println("Total count for mutated barcodes that match the original: " + matchingMutatedReads
                    + " (" + floatFormat.format(matchingMutatedPercent) + "%)");
            System.out.println("Total count for corrected barcodes that match the original: " + matchingCorrectedReads
                    + " (" + floatFormat.format(matchingCorrectedPercent) + "%)");
            System.out.println("Total count for wrongly corrected barcodes: " + wronglyCorrectedReads
                    + " (" + floatFormat.format(wronglyCorrectedPercent) + "%)");
            System.out.println("Total count for new barcodes that appear after correction: " + newBarcodesTotalReads
                    + " (" + floatFormat.format(newBarcodesReadsPercent) + "%)");
            if (newBarcodesTotalReads > 0)
                System.out.println("New barcodes counts: " + newBarcodesCounters.stream().map(c -> c.totalCount)
                        .sorted((c1, c2) -> Long.compare(c2, c1)).collect(Collectors.toList()));
            float correctedPercent = (numberOfReads == 0) ? 0
                    : (float)correctionStats.correctedReads / numberOfReads * 100;
            float qualityUpdatedPercent = (numberOfReads == 0) ? 0
                    : (float)correctionStats.updatedQualityReads / numberOfReads * 100;
            System.out.println("Reads with corrected barcodes: " + correctionStats.correctedReads
                    + " (" + floatFormat.format(correctedPercent) + "%)");
            System.out.println("Reads with not changed barcode sequences, but updated qualities: "
                    + correctionStats.updatedQualityReads + " (" + floatFormat.format(qualityUpdatedPercent) + "%)");
            System.out.println("Stats for 1st stage correction (merging by wildcards):");
            System.out.println("Clusters checked for possible merge on wildcards processing stage: "
                    + correctionStats.wildcardCanAddToClusterCalls);
            float wildcardClusterNotAddedByThresholdPercent = (correctionStats.wildcardCanAddToClusterCalls == 0) ? 0
                    : (float)correctionStats.wildcardClusterNotAddedByThreshold
                    / correctionStats.wildcardCanAddToClusterCalls * 100;
            System.out.println("Wildcard clusters not merged by size threshold: "
                    + correctionStats.wildcardClusterNotAddedByThreshold + " ("
                    + floatFormat.format(wildcardClusterNotAddedByThresholdPercent) + "%)");
            System.out.println("Stats for 2nd stage correction (correction of mutations in barcodes):");
            System.out.println("Clusters checked for possible merge on barcodes correction stage: "
                    + correctionStats.barcodeCanAddToClusterCalls);
            float barcodeClusterNotAddedByWildcardsPercent = (correctionStats.barcodeCanAddToClusterCalls == 0) ? 0
                    : (float)correctionStats.barcodeClusterNotAddedByWildcards
                    / correctionStats.barcodeCanAddToClusterCalls * 100;
            float barcodeClusterNotAddedByExpectedCountPercent = (correctionStats.barcodeCanAddToClusterCalls == 0) ? 0
                    : (float)correctionStats.barcodeClusterNotAddedByExpectedCount
                    / correctionStats.barcodeCanAddToClusterCalls * 100;
            System.out.println("Barcode clusters not merged because they are equal by wildcards "
                    + "and were not previously merged on wildcards processing stage: "
                    + correctionStats.barcodeClusterNotAddedByWildcards + " ("
                    + floatFormat.format(barcodeClusterNotAddedByWildcardsPercent) + "%)");
            System.out.println("Barcode clusters not merged because minor cluster count was bigger "
                    + "than expected with specified mutation probabilities: "
                    + correctionStats.barcodeClusterNotAddedByExpectedCount + " ("
                    + floatFormat.format(barcodeClusterNotAddedByExpectedCountPercent) + "%)");
            System.out.println();
        }

        void printCorrectionMap() {
            for (Map.Entry<NucleotideSequence, NucleotideSequence> entry : correctionMap.entrySet()) {
                System.out.println("Correction: " + entry.getKey() + " => " + entry.getValue().getSequence());
                System.out.println("Equal by wildcards: "
                        + equalByWildcards(entry.getKey(), entry.getValue().getSequence()) + "\n");
            }
        }
    }

    private static class TestResultSequences {
        final NucleotideSequence original;
        final NucleotideSequence mutated;
        final NucleotideSequence corrected;

        TestResultSequences(NucleotideSequence original, NucleotideSequence mutated, NucleotideSequence corrected) {
            this.original = original;
            this.mutated = mutated;
            this.corrected = corrected;
        }

        void print() {
            System.out.println("Original: " + original);
            System.out.println("Mutated: " + mutated);
            System.out.println("Corrected: " + corrected + "\n");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestResultSequences that = (TestResultSequences)o;
            if (!Objects.equals(original, that.original)) return false;
            if (!Objects.equals(mutated, that.mutated)) return false;
            return Objects.equals(corrected, that.corrected);
        }

        @Override
        public int hashCode() {
            int result = original != null ? original.hashCode() : 0;
            result = 31 * result + (mutated != null ? mutated.hashCode() : 0);
            result = 31 * result + (corrected != null ? corrected.hashCode() : 0);
            return result;
        }
    }

    private static class WrongBarcodesCounter {
        long totalCount = 0;
        NucleotideSequence wrongCorrectedBarcode = null;
        TObjectIntHashMap<TestResultSequences> separateCounters = new TObjectIntHashMap<>();

        void add(TestResultSequences testResultSequences) {
            if (wrongCorrectedBarcode == null)
                wrongCorrectedBarcode = testResultSequences.corrected;
            else
                assertEquals(wrongCorrectedBarcode, testResultSequences.corrected);
            separateCounters.adjustOrPutValue(testResultSequences, 1, 1);
            totalCount++;
        }

        void print() {
            assertNotNull(wrongCorrectedBarcode);
            System.out.println("Count for wrong new barcode " + wrongCorrectedBarcode + ": " + totalCount);
            System.out.println("Details:\n");
            separateCounters.forEachEntry((k, v) -> {
                System.out.println("Entry count: " + v);
                k.print();
                return true;
            });
        }
    }
}
