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
package com.milaboratory.minnn.cli;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class ConsensusActionTest {
    @BeforeClass
    public static void init() {
        actionTestInit();
    }

    @Test
    public void randomTest() throws Exception {
        String start = TEMP_DIR + "start.mif";
        String extracted = TEMP_DIR + "extracted.mif";
        String sorted1 = TEMP_DIR + "sorted1.mif";
        String corrected = TEMP_DIR + "corrected1.mif";
        String sorted2 = TEMP_DIR + "sorted2.mif";
        String output1 = TEMP_DIR + "consensusOutput1.mif";
        String output2 = TEMP_DIR + "consensusOutput2.mif";
        String output3 = TEMP_DIR + "consensusOutput3.mif";
        String output4 = TEMP_DIR + "consensusOutput4.mif";
        String out3Fastq = TEMP_DIR + "consensusOutput3.fastq";
        String out4Fastq = TEMP_DIR + "consensusOutput4.fastq";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(start);
            String consensusGroups = Arrays.asList(new String[] {"G1", "G2", "G1 G2", "G2 G1"}).get(rg.nextInt(4));
            int width = rg.nextInt(50) + 1;
            int mismatchScore = -rg.nextInt(10) - 1;
            int gapScore = -rg.nextInt(10) - 1;
            exec("extract -f --input-format MIF --input " + start + " --output " + extracted
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            sortFile(extracted, sorted1, consensusGroups);
            exec("correct -f --max-errors-share " + (rg.nextInt(10) / 10f)
                    + " --max-errors " + (rg.nextInt(5) - 1) + " --input " + sorted1
                    + " --output " + corrected + " --groups " + consensusGroups);
            sortFile(corrected, sorted2, consensusGroups);
            exec("consensus-dma -f --input " + sorted2 + " --output " + output1
                    + " --groups " + consensusGroups + " --threads " + (rg.nextInt(10) + 1)
                    + " --score-threshold " + (rg.nextInt(2000) - 1000) + " --width " + width
                    + " --max-consensuses-per-cluster " + (rg.nextInt(30) + 1)
                    + " --skipped-fraction-to-repeat " + (rg.nextFloat() * 0.8f + 0.1f)
                    + " --reads-avg-quality-threshold " + rg.nextInt(DEFAULT_GOOD_QUALITY)
                    + " --reads-trim-window-size " + (rg.nextInt(15) + 1)
                    + " --reads-min-good-sequence-length " + (rg.nextInt(50) + 1)
                    + " --avg-quality-threshold " + rg.nextInt(DEFAULT_GOOD_QUALITY)
                    + " --trim-window-size " + (rg.nextInt(15) + 1)
                    + " --min-good-sequence-length " + (rg.nextInt(50) + 1)
                    + " --aligner-match-score 0 --aligner-mismatch-score " + mismatchScore
                    + " --aligner-gap-score " + gapScore);
            Stream.of(new String[] { output1, output2 }, new String[] { output2, output3 },
                    new String[] { output3, output4 })
                    .forEach(files -> exec("consensus-dma -f --input " + files[0] + " --output " + files[1]
                            + " --groups " + consensusGroups + " --threads " + (rg.nextInt(10) + 1)
                            + " --score-threshold 0 --width " + width
                            + " --max-consensuses-per-cluster 100 --skipped-fraction-to-repeat 0.001"
                            + " --reads-avg-quality-threshold 0 --avg-quality-threshold 0 --aligner-match-score 0"
                            + " --aligner-mismatch-score " + mismatchScore + " --aligner-gap-score " + gapScore));
            exec("mif2fastq -f --input " + output3 + " --group R1=" + out3Fastq);
            exec("mif2fastq -f --input " + output4 + " --group R1=" + out4Fastq);
            String parameterValuesMessage = "Files are different with parameter values: consensusGroups: "
                    + consensusGroups + ", width: " + width + ", mismatchScore: " + mismatchScore
                    + ", gapScore: " + gapScore;
            assertFileEquals(parameterValuesMessage, out3Fastq, out4Fastq);
        }
        for (String fileName : new String[] { start, extracted, sorted1, corrected, sorted2,
                output1, output2, output3, output4, out3Fastq, out4Fastq })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String sortedFile1 = TEMP_DIR + "sorted1.mif";
        String correctedFile = TEMP_DIR + "corrected.mif";
        String sortedFile2 = TEMP_DIR + "sorted2.mif";
        String consensusFile = TEMP_DIR + "consensus.mif";
        String notUsedReadsFile = TEMP_DIR + "not_used_reads.mif";
        String consensusFile2 = TEMP_DIR + "consensus2.mif";
        String consensusFile3 = TEMP_DIR + "consensus3.mif";
        String consensusFile4 = TEMP_DIR + "consensus4.mif";
        sortFile(inputFile, sortedFile1, "G3 G4 G1 G2");
        exec("correct -f --input " + sortedFile1 + " --output " + correctedFile + " --groups G3 G4 G1 G2"
                + " --max-errors-share 0.5");
        sortFile(correctedFile, sortedFile2, "G3 G4 G1 G2 R1 R2");
        exec("consensus-dma -f --input " + sortedFile2 + " --output " + consensusFile + " --groups G3 G4 G1"
                + " --threads 5 --score-threshold -1200 --width 30 --max-consensuses-per-cluster 5"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 3 --good-quality-mismatch-penalty 0"
                + " --not-used-reads-output " + notUsedReadsFile);
        exec("consensus-dma -f --input " + consensusFile + " --output " + consensusFile2
                + " --groups G3 G4 G1 --threads 3 --score-threshold -1200 --width 30 --reads-avg-quality-threshold 0"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 0 --good-quality-mismatch-penalty 0");
        exec("consensus-dma -f --input " + consensusFile2 + " --output " + consensusFile3
                + " --groups G3 G4 G1 --threads 3 --score-threshold -1200 --width 30 --reads-avg-quality-threshold 0"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 0 --good-quality-mismatch-penalty 0");
        exec("consensus-dma -f --input " + consensusFile3 + " --output " + consensusFile4
                + " --groups G3 G4 G1 --threads 3 --score-threshold -1200 --width 30 --reads-avg-quality-threshold 0"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 0 --good-quality-mismatch-penalty 0");
        assertMifNotEqualsAsFastq(consensusFile2, consensusFile3, true);    // must differ in "consensusReads" field
        assertMifEqualsAsFastq(consensusFile3, consensusFile4, true);
        for (String fileName : new String[] { inputFile, sortedFile1, correctedFile, sortedFile2, consensusFile,
                notUsedReadsFile, consensusFile2, consensusFile3 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void qualityOverflowTest() throws Exception {
        String inputFile = getExampleMif("good-quality");
        String consensusFile = TEMP_DIR + "consensus-qual-test.mif";
        exec("consensus-dma -f --input " + inputFile + " --output " + consensusFile + " --groups G1");
        for (String fileName : new String[] { inputFile, consensusFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void numberOfReadsTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String sortedFile1 = TEMP_DIR + "sorted1.mif";
        String correctedFile = TEMP_DIR + "corrected.mif";
        String sortedFile2 = TEMP_DIR + "sorted2.mif";
        String consensusFile = TEMP_DIR + "consensus.mif";
        sortFile(inputFile, sortedFile1, "G1 G2");
        exec("correct -f --input " + sortedFile1 + " --output " + correctedFile + " --groups G1 G2 -n 10000");
        sortFile(correctedFile, sortedFile2, "G1 G2");
        exec("consensus-dma -f --input " + sortedFile2 + " --output " + consensusFile
                + " --groups G1 G2 -n 1000");
        for (String fileName : new String[] { inputFile, sortedFile1, correctedFile, sortedFile2, consensusFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void toSeparateGroupsTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String sortedFile1 = TEMP_DIR + "sorted1.mif";
        String correctedFile = TEMP_DIR + "corrected.mif";
        String sortedFile2 = TEMP_DIR + "sorted2.mif";
        String consensusFile = TEMP_DIR + "consensus.mif";
        String outFastqR1 = TEMP_DIR + "consensus_R1.fastq";
        String outFastqR2 = TEMP_DIR + "consensus_R2.fastq";
        sortFile(inputFile, sortedFile1, "G3 G4 G1 G2");
        exec("correct -f --input " + sortedFile1 + " --output " + correctedFile + " --groups G3 G4 G1 G2");
        sortFile(correctedFile, sortedFile2, "G3 G4 G1");
        exec("sort -f --input " + correctedFile + " --output " + sortedFile2 + " --groups G3 G4 G1 G2 R1 R2");
        exec("consensus-dma -f --input " + sortedFile2 + " --output " + consensusFile + " --groups G3 G4 G1"
                + " --consensuses-to-separate-groups");
        exec("mif2fastq -f --input " + consensusFile + " --group R1=" + outFastqR1
                + " --group R2=" + outFastqR2);
        assertFileNotEquals(outFastqR1, outFastqR2);
        for (String fileName : new String[] {
                inputFile, sortedFile1, correctedFile, sortedFile2, consensusFile, outFastqR1, outFastqR2 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void singleCellPreparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String sortedFile1 = TEMP_DIR + "sorted1.mif";
        String correctedFile = TEMP_DIR + "corrected.mif";
        String sortedFile2 = TEMP_DIR + "sorted2.mif";
        String consensusFile = TEMP_DIR + "consensus.mif";
        String notUsedReadsFile = TEMP_DIR + "not_used_reads.mif";
        String consensusFile2 = TEMP_DIR + "consensus2.mif";
        String consensusFile3 = TEMP_DIR + "consensus3.mif";
        String consensusFile4 = TEMP_DIR + "consensus4.mif";
        sortFile(inputFile, sortedFile1, "G3 G4 G1 G2");
        exec("correct -f --input " + sortedFile1 + " --output " + correctedFile + " --groups G3 G4 G1 G2");
        sortFile(correctedFile, sortedFile2, "G3 G4 G1 G2 R1 R2");
        exec("consensus -f --input " + sortedFile2 + " --output " + consensusFile + " --groups G3 G4 G1"
                + " --threads 5 --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 3 --not-used-reads-output "
                + notUsedReadsFile);
        exec("consensus -f --input " + consensusFile + " --output " + consensusFile2 + " --groups G3 G4 G1"
                + " --threads 2 --skipped-fraction-to-repeat 1 --avg-quality-threshold 0"
                + " --reads-avg-quality-threshold 0 --kmer-max-errors 0 --not-used-reads-output " + notUsedReadsFile);
        exec("consensus -f --input " + consensusFile2 + " --output " + consensusFile3 + " --groups G3 G4 G1"
                + " --threads 3 --skipped-fraction-to-repeat 1 --avg-quality-threshold 0"
                + " --reads-avg-quality-threshold 0 --kmer-max-errors 0 --not-used-reads-output " + notUsedReadsFile);
        exec("consensus -f --input " + consensusFile3 + " --output " + consensusFile4 + " --groups G3 G4 G1"
                + " --threads 3 --skipped-fraction-to-repeat 1 --avg-quality-threshold 0"
                + " --reads-avg-quality-threshold 0 --kmer-max-errors 0 --not-used-reads-output " + notUsedReadsFile);
        assertMifNotEqualsAsFastq(consensusFile2, consensusFile3, true);    // must differ in "consensusReads" field
        assertMifEqualsAsFastq(consensusFile3, consensusFile4, true);
        for (String fileName : new String[] { inputFile, sortedFile1, correctedFile, sortedFile2, consensusFile,
                notUsedReadsFile, consensusFile2, consensusFile3 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void sequencesTest() throws Exception {
        String inputFastqFile = TEMP_DIR + "input.fastq";
        String inputMifFile = TEMP_DIR + "input.mif";
        String sortedFile = TEMP_DIR + "sorted.mif";
        String consensusMifFile = TEMP_DIR + "consensus.mif";
        String consensusFastqFile = TEMP_DIR + "consensus.fastq";
        String barcode = "TTT";
        LinkedHashMap<List<String>, String> testData = new LinkedHashMap<>();
        testData.put(Arrays.asList("ATTTGACA", "ACTAGATA", "CTGAGACC"), "ATTAGACA");
        for (HashMap.Entry<List<String>, String> entry : testData.entrySet()) {
            List<NSequenceWithQuality> dataWithBarcodes = entry.getKey().stream()
                    .map(str -> barcode + str).map(NSequenceWithQuality::new).collect(Collectors.toList());
            seqToFastq(dataWithBarcodes, inputFastqFile);
            exec("extract -f --input " + inputFastqFile + " --output " + inputMifFile
                    + " --pattern \"(G1:" + barcode + ")N{*}\"");
            exec("sort -f --input " + inputMifFile + " --output " + sortedFile + " --groups G1");
            exec("consensus -f --input " + sortedFile + " --output " + consensusMifFile
                    + " --kmer-length 4 --groups G1");
            exec("mif2fastq -f --input " + consensusMifFile + " --group R1=" + consensusFastqFile);
            assertEquals(barcode + entry.getValue(),
                    fastqToSeq(consensusFastqFile).get(0).getSequence().toString());
        }
        for (String fileName : new String[] { inputFastqFile, inputMifFile, sortedFile, consensusMifFile,
                consensusFastqFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void debugOutputTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String sortedFile1 = TEMP_DIR + "sorted1.mif";
        String correctedFile = TEMP_DIR + "corrected.mif";
        String sortedFile2 = TEMP_DIR + "sorted2.mif";
        String consensusSCFile = TEMP_DIR + "consensusSC.mif";
        String consensusDMAFile = TEMP_DIR + "consensusDMA.mif";
        String debugSC = TEMP_DIR + "debugSC.txt";
        String debugDMA = TEMP_DIR + "debugDMA.txt";
        sortFile(inputFile, sortedFile1, "G1 G2");
        exec("correct -n 1000 -f --input " + sortedFile1 + " --output " + correctedFile + " --groups G1 G2");
        sortFile(correctedFile, sortedFile2, "G1 G2");
        exec("consensus -f --threads 1 --input " + sortedFile2 + " --output " + consensusSCFile
                + " --debug-output " + debugSC + " --groups G1 G2");
        exec("consensus-dma -f --threads 1 --input " + sortedFile2 + " --output " + consensusDMAFile
                + " --debug-output " + debugDMA + " --groups G1 G2 --score-threshold -1000");
        assertFalse(new File(debugSC).length() == 0);
        assertFalse(new File(debugDMA).length() == 0);
        for (String fileName : new String[] { inputFile, sortedFile1, correctedFile, sortedFile2, consensusSCFile,
                consensusDMAFile, debugSC, debugDMA })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void originalReadStatsTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String sortedFile1 = TEMP_DIR + "sorted1.mif";
        String correctedFile = TEMP_DIR + "corrected.mif";
        String correctedTruncatedFile = TEMP_DIR + "correctedT.mif";
        String sortedFile2 = TEMP_DIR + "sorted2.mif";
        String sortedTruncatedFile = TEMP_DIR + "sortedT.mif";
        String consensusSCFile = TEMP_DIR + "consensusSC.mif";
        String consensusDMAFile = TEMP_DIR + "consensusDMA.mif";
        String consensusSCTruncatedFile = TEMP_DIR + "consensusSCT.mif";
        String statsSC = TEMP_DIR + "statsSC.txt";
        String statsDMA = TEMP_DIR + "statsDMA.txt";
        String statsSCT = TEMP_DIR + "statsSCT.txt";
        sortFile(inputFile, sortedFile1, "G1 G2");
        exec("correct -f --input " + sortedFile1 + " --output " + correctedFile + " --groups G1 G2");
        exec("correct -n 1000 -f --input " + sortedFile1 + " --output " + correctedTruncatedFile
                + " --groups G1 G2");
        sortFile(correctedFile, sortedFile2, "G1 G2");
        sortFile(correctedTruncatedFile, sortedTruncatedFile, "G1 G2");
        exec("consensus -f --threads 2 --input " + sortedFile2 + " --output " + consensusSCFile
                + " --original-read-stats " + statsSC + " --groups G1 G2 --avg-quality-threshold 25");
        exec("consensus-dma -f --threads 2 --input " + sortedFile2 + " --output " + consensusDMAFile
                + " --original-read-stats " + statsDMA + " --groups G1 G2 --avg-quality-threshold 25");
        exec("consensus -f --threads 2 --input " + sortedTruncatedFile
                + " --output " + consensusSCTruncatedFile + " --original-read-stats " + statsSCT
                + " --groups G1 G2 --avg-quality-threshold 25");
        for (HashMap.Entry<String, Integer> entry : new HashMap<String, Integer>() {{
            put(statsSC, 25001); put(statsDMA, 25001); put(statsSCT, 1001); }}.entrySet()) {
            try (Scanner scanner = new Scanner(new File(entry.getKey()))) {
                int linesCount = 0;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    assertEquals(23, line.split(" ").length);
                    linesCount++;
                }
                assertEquals(entry.getValue().intValue(), linesCount);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (String fileName : new String[] { inputFile, sortedFile1, correctedFile, correctedTruncatedFile,
                sortedFile2, sortedTruncatedFile, consensusSCFile, consensusDMAFile, consensusSCTruncatedFile,
                statsSC, statsDMA, statsSCT })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void emptyReadsTest() throws Exception {
        String inputFile = getExampleMif("with-empty-reads");
        String sorted = TEMP_DIR + "sorted.mif";
        sortFile(inputFile, sorted, "G1 G2");
        String consensusSC = TEMP_DIR + "consensusSC.mif";
        String consensusDMA = TEMP_DIR + "consensusDMA.mif";
        exec("consensus -f --groups G1 G2 --input " + sorted + " --output " + consensusSC
                + " --reads-min-good-sequence-length 1 --min-good-sequence-length 1 --kmer-length 1");
        exec("consensus-dma -f --groups G1 G2 --input " + sorted + " --output " + consensusDMA
                + " --reads-min-good-sequence-length 1 --min-good-sequence-length 1");
        for (String fileName : new String[] { inputFile, sorted, consensusSC, consensusDMA })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void notUsedReadsCountTest() throws Exception {
        String start = TEMP_DIR + "start.mif";
        String extracted = TEMP_DIR + "extracted.mif";
        String sorted = TEMP_DIR + "sorted.mif";
        String consensus1 = TEMP_DIR + "consensus1.mif";
        String notUsedReads = TEMP_DIR + "not-used-reads.mif";
        String consensus2 = TEMP_DIR + "consensus2.mif";
        String jsonReport1 = TEMP_DIR + "json-report1.json";
        String jsonReport2 = TEMP_DIR + "json-report2.json";
        String jsonReportMifInfo = TEMP_DIR + "json-report-mif-info.json";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(start);
            String consensusGroups = Arrays.asList("G1", "G2", "G1 G2", "G2 G1").get(rg.nextInt(4));
            int scoreThreshold = rg.nextInt(2000) - 1000;
            int width = rg.nextInt(50) + 1;
            int maxConsensusesPerCluster = rg.nextInt(30) + 1;
            float skippedFractionToRepeat = rg.nextFloat() * 0.8f + 0.1f;
            int readsAvgQualityThreshold = rg.nextInt(DEFAULT_GOOD_QUALITY);
            int readsTrimWindowSize = rg.nextInt(15) + 1;
            int readsMinGoodSeqLength = rg.nextInt(50) + 1;
            int avgQualityThreshold = rg.nextInt(DEFAULT_GOOD_QUALITY);
            int trimWindowSize = rg.nextInt(15) + 1;
            int minGoodSeqLength = rg.nextInt(50) + 1;
            int mismatchScore = -rg.nextInt(10) - 1;
            int gapScore = -rg.nextInt(10) - 1;
            exec("extract -f --input-format MIF --input " + start + " --output " + extracted
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            sortFile(extracted, sorted, consensusGroups);
            for (int j = 1; j <= 2; j++)
                exec("consensus-dma -f --input " + sorted + " --output " + (j == 1 ? consensus1 : consensus2)
                        + " --groups " + consensusGroups
                        + " --score-threshold " + scoreThreshold + " --width " + width
                        + " --max-consensuses-per-cluster " + maxConsensusesPerCluster
                        + " --skipped-fraction-to-repeat " + skippedFractionToRepeat
                        + " --reads-avg-quality-threshold " + readsAvgQualityThreshold
                        + " --reads-trim-window-size " + readsTrimWindowSize
                        + " --reads-min-good-sequence-length " + readsMinGoodSeqLength
                        + " --avg-quality-threshold " + avgQualityThreshold
                        + " --trim-window-size " + trimWindowSize
                        + " --min-good-sequence-length " + minGoodSeqLength
                        + " --aligner-match-score 0 --aligner-mismatch-score " + mismatchScore
                        + " --aligner-gap-score " + gapScore
                        + (j == 1 ? " --not-used-reads-output " + notUsedReads : "")
                        + " --json-report " + (j == 1 ? jsonReport1 : jsonReport2));
            exec("mif-info -f --json-report " + jsonReportMifInfo + " " + notUsedReads);

            List<String> json1Lines = Files.lines(Paths.get(jsonReport1)).collect(Collectors.toList());
            List<String> json2Lines = Files.lines(Paths.get(jsonReport2)).collect(Collectors.toList());
            List<String> jsonMifInfoLines = Files.lines(Paths.get(jsonReportMifInfo)).collect(Collectors.toList());
            Collections.reverse(json1Lines);
            Collections.reverse(json2Lines);
            Collections.reverse(jsonMifInfoLines);
            String json1CountLine = json1Lines.stream().filter(s -> s.contains("notUsedReadsCount")).findFirst().orElseThrow(AssertionError::new);
            String json2CountLine = json2Lines.stream().filter(s -> s.contains("notUsedReadsCount")).findFirst().orElseThrow(AssertionError::new);
            String jsonInfoCountLine = jsonMifInfoLines.stream().filter(s -> s.contains("numberOfReads")).findFirst()
                    .orElseThrow(AssertionError::new);
            int json1Count = Integer.parseInt(json1CountLine.split(":")[1].replaceAll("[ ,]", ""));
            int json2Count = Integer.parseInt(json2CountLine.split(":")[1].replaceAll("[ ,]", ""));
            int jsonInfoCount = Integer.parseInt(jsonInfoCountLine.split(":")[1].replaceAll("[ ,]", ""));
            assertEquals(json1Count, json2Count);
            assertEquals(json1Count, jsonInfoCount);

            for (String fileName : new String[] { start, extracted, sorted, consensus1, notUsedReads, consensus2,
                    jsonReport1, jsonReport2, jsonReportMifInfo })
                assertTrue(new File(fileName).delete());
        }
    }

    @Test
    public void dropOversizedClustersTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String sortedFile = TEMP_DIR + "sorted.mif";
        LinkedHashMap<String, List<String>> testRuns = new LinkedHashMap<>();
        Stream.of("SC1", "SC2", "DMA1", "DMA2").forEach(runName ->
                testRuns.put(runName, Arrays.asList(TEMP_DIR + "consensus" + runName + ".mif",
                        TEMP_DIR + "notUsed" + runName + ".mif",
                        TEMP_DIR + "report" + runName + ".txt", TEMP_DIR + "report" + runName + ".json",
                        TEMP_DIR + "consensus" + runName + "-R1.fastq", TEMP_DIR + "consensus" + runName + "-R2.fastq",
                        TEMP_DIR + "notUsed" + runName + "-R1.fastq", TEMP_DIR + "notUsed" + runName + "-R2.fastq")));

        sortFile(inputFile, sortedFile, "G3 G4 G1 G2");
        for (Map.Entry<String, List<String>> entry : testRuns.entrySet()) {
            String runName = entry.getKey();
            String dropOversized = (runName.charAt(runName.length() - 1) == '2') ? " --drop-oversized-clusters" : "";
            String consensus = runName.substring(0, runName.length() - 1).equals("SC") ? "consensus" : "consensus-dma";
            List<String> args = entry.getValue();
            exec(consensus + " -f --input " + sortedFile + " --output " + args.get(0) + " --groups G3 G4 G1"
                    + " --not-used-reads-output " + args.get(1) + dropOversized + " --consensuses-to-separate-groups"
                    + " --report " + args.get(2) + " --json-report " + args.get(3));
            exec("mif2fastq -f --input " + args.get(0) + " --group R1=" + args.get(4) + " R2=" + args.get(5));
            exec("mif2fastq -f --input " + args.get(1) + " --group R1=" + args.get(6) + " R2=" + args.get(7));
        }

        assertEquals(countFileLines(testRuns.get("SC1").get(4)), countFileLines(testRuns.get("SC1").get(5)));
        assertEquals(countFileLines(testRuns.get("SC1").get(6)), countFileLines(testRuns.get("SC1").get(7)));
        assertEquals(countFileLines(testRuns.get("SC1").get(4)) + countFileLines(testRuns.get("SC1").get(6)),
                countFileLines(testRuns.get("SC2").get(4)) + countFileLines(testRuns.get("SC2").get(6)));

        for (List<String> args : testRuns.values())
            for (String fileName : args)
                assertTrue(new File(fileName).delete());
        for (String fileName : Arrays.asList(inputFile, sortedFile))
            assertTrue(new File(fileName).delete());
    }
}
