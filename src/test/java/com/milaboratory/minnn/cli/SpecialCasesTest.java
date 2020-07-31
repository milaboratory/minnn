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

import org.junit.*;

import java.io.*;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class SpecialCasesTest {
    @BeforeClass
    public static void init() {
        actionTestInit();
    }

    @Test
    public void scoringForMultipleNTest() throws Exception {
        String inputFile = getExampleMif("twosided-raw");
        String file1 = TEMP_DIR + "file1.mif";
        String file2 = TEMP_DIR + "file2.mif";
        String diff = TEMP_DIR + "diff.mif";
        String diff_R1 = TEMP_DIR + "diff_R1.fastq";
        String diff_R2 = TEMP_DIR + "diff_R2.fastq";
        for (String extractFlags : new String[] { "", " --try-reverse-order" }) {
            exec("extract -f --input " + inputFile + " --output " + file1 + " --input-format MIF"
                    + " --score-threshold -100 --uppercase-mismatch-score -15 --max-quality-penalty 0"
                    + " --bitap-max-errors 4 --pattern \"^(UMI:nnnntnnnntnnnn)TCTTGGG\\*\"" + extractFlags);
            exec("extract -f --input " + file1 + " --output " + file2 + " --input-format MIF"
                    + " --not-matched-output " + diff + " --score-threshold -100 --uppercase-mismatch-score -15"
                    + " --pattern \"^(UMI:nnnntnnnntnnnn)TCTTGGG(R1cut:N{*})\\*\" --max-quality-penalty 0"
                    + " --bitap-max-errors 4" + extractFlags);
            exec("mif2fastq -f --input " + diff + " --group R1=" + diff_R1 + " --group R2=" + diff_R2);
            assertEquals(0, new File(diff_R1).length());
            assertEquals(0, new File(diff_R2).length());
        }
        for (String fileName : new String[] { inputFile, file1, file2, diff, diff_R1, diff_R2 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void correctionSpeedTest() throws Exception {
        String inputFastqFiles = getBigOrSmallFastqTestFileNames("test01_R1.fastq.gz", "test01_R2.fastq.gz");
        String extractOutput = TEMP_DIR + "extracted.mif";
        String sortOutput = TEMP_DIR + "sorted.mif";
        String correctOutput = TEMP_DIR + "corrected.mif";
        exec("extract -f --input " + inputFastqFiles + " --output " + extractOutput
                + " --score-threshold -25 --bitap-max-errors 5 --try-reverse-order"
                + " --pattern \"(FULL:tggtatcaacgcagagt(UMI:nnnntnnnntnnnn)tct)\\*\"");
        exec("sort -f --groups UMI --input " + extractOutput + " --output " + sortOutput
                + " --chunk-size 10000");
        exec("correct -f --groups UMI --input " + sortOutput + " --output " + correctOutput
                + " --cluster-threshold 0.3");
        for (String fileName : new String[] { extractOutput, sortOutput, correctOutput })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void consensusSpeedTest() throws Exception {
        String inputFastqFiles = getBigOrSmallFastqTestFileNames("test01_R1.fastq.gz", "test01_R2.fastq.gz");
        String extractOutput = TEMP_DIR + "extracted.mif";
        String sortOutput = TEMP_DIR + "sorted.mif";
        String consensusOutput = TEMP_DIR + "consensus.mif";
        exec("extract -f --input " + inputFastqFiles + " --output " + extractOutput
                + " --score-threshold -25 --bitap-max-errors 5 --try-reverse-order"
                + " --pattern \"(FULL:tggtatcaacgcagagt(UMI:nnnntnnnntnnnn)tct)\\*\"");
        exec("sort -f --groups UMI --input " + extractOutput + " --output " + sortOutput
                + " --chunk-size 10000");
        exec("consensus -f --groups UMI --input " + sortOutput + " --output " + consensusOutput);
        for (String fileName : new String[] { extractOutput, sortOutput, consensusOutput })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void wrongGroupsTest() throws Exception {
        String inputFileR1 = TEST_RESOURCES_PATH + "sample_r1.fastq.gz";
        String inputFileR2 = TEST_RESOURCES_PATH + "sample_r2.fastq.gz";
        String extracted = TEMP_DIR + "extracted.mif";
        String output = TEMP_DIR + "output.mif";
        exec("extract -f --input " + inputFileR1 + " " + inputFileR2 + " --output " + extracted
                + " --pattern \"*\\*\"");
        assertOutputContains(true, "not found", () -> callableExec("correct -f" +
                " --input " + extracted + " --output " + output + " --groups G"));
        assertOutputContains(true, "not allowed", () -> callableExec("correct -f" +
                " --input " + extracted + " --output " + output + " --groups R1"));
        assertOutputContains(true, "not found", () -> callableExec("sort -f" +
                " --input " + extracted + " --output " + output + " --groups G"));
        exec("sort -f --input " + extracted + " --output " + output + " --groups R1");
        assertOutputContains(true, "not found", () -> callableExec("consensus -f" +
                " --input " + extracted + " --output " + output + " --groups G"));
        assertOutputContains(true, "not allowed", () -> callableExec("consensus -f" +
                " --input " + extracted + " --output " + output + " --groups R1"));
        assertOutputContains(true, "not found", () -> callableExec("stat-groups" +
                " --input " + extracted + " --groups G"));
        exec("stat-groups --input " + extracted + " --groups R1");
        assertOutputContains(true, "not found", () -> callableExec("stat-positions" +
                " --input " + extracted + " --groups G"));
        exec("stat-positions --input " + extracted + " --groups R1");
        assertOutputContains(true, "not found", () -> callableExec("filter -f" +
                " --input " + extracted + " --output " + output + " \"Len(G)=3\""));
        exec("filter -f --input " + extracted + " --output " + output + " \"Len(R1)=3\"");
        for (String fileName : new String[] { extracted, output })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void numberOfReadsChangeTest() throws Exception {
        String suffix = "-special-case-3.mif";
        String inputFile = getExampleMif("twosided-raw");
        String extracted = TEMP_DIR + "extracted" + suffix;
        String sorted = TEMP_DIR + "sorted" + suffix;
        String corrected = TEMP_DIR + "corrected" + suffix;
        String fastqR1 = TEMP_DIR + "R1.fastq";
        String fastqR2 = TEMP_DIR + "R2.fastq";
        exec("extract -f --input " + inputFile + " --output " + extracted + " --input-format MIF"
                + " --pattern \"(BC:NNNNNNNN)(UMI:NNNNNNNN)\\(R1:*)\"");
        sortFile(extracted, sorted, "BC UMI");
        exec("correct -f --input " + sorted + " --output " + corrected + " --groups BC UMI");
        assertOutputContains(true, "Group R2 not found", () -> callableExec("mif2fastq -f " +
                "--input " + corrected + " --group R1=" + fastqR1 + " R2=" + fastqR2));
        exec("mif2fastq -f --input " + corrected + " --group R1=" + fastqR1);
        for (String fileName : new String[] { inputFile, extracted, sorted, corrected, fastqR1, fastqR2 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void readsNestedOverrideTest() throws Exception {
        String suffix = "-special-case-4.mif";
        String inputFile = getExampleMif("twosided-raw");
        String extracted = TEMP_DIR + "extracted" + suffix;
        String sorted1 = TEMP_DIR + "sorted1" + suffix;
        String corrected = TEMP_DIR + "corrected" + suffix;
        String sorted2 = TEMP_DIR + "sorted2" + suffix;
        String consensus = TEMP_DIR + "consensus" + suffix;
        String consensusOrig = TEMP_DIR + "consensusOrig" + suffix;
        String consensusDMA = TEMP_DIR + "consensusDMA" + suffix;
        String fastqR1 = TEMP_DIR + "R1.fastq";
        String fastqR4 = TEMP_DIR + "R4.fastq";
        String fastqR5 = TEMP_DIR + "R5.fastq";
        String fastqUMI = TEMP_DIR + "UMI.fastq";
        exec("extract -f --input " + inputFile + " --output " + extracted + " --input-format MIF"
                + " --pattern \"(UMI:NNNNNNNN)\\gaca(R1:n(R2:nn(R3:nn(R4:(R5:nnnntn)nn)))na)\"");
        sortFile(extracted, sorted1, "UMI");
        exec("correct -f --input " + sorted1 + " --output " + corrected + " --groups UMI"
                + " --max-errors-share 0.4");
        sortFile(extracted, sorted2, "R5 R3 UMI");
        exec("consensus -f --input " + sorted2 + " --output " + consensus + " --groups UMI --kmer-length 4");
        exec("mif2fastq -f --input " + consensus + " --group R5=" + fastqR5 + " R1=" + fastqR1
                + " UMI=" + fastqUMI + " R4=" + fastqR4);
        exec("consensus -f --consensuses-to-separate-groups --input " + sorted2
                + " --output " + consensusOrig + " --groups UMI --kmer-length 4");
        exec("mif2fastq -f --input " + consensusOrig + " --group R5=" + fastqR5 + " R1=" + fastqR1
                + " UMI=" + fastqUMI + " CR4=" + fastqR4);
        exec("consensus-dma -f --input " + sorted2 + " --output " + consensusDMA + " --groups UMI");
        exec("mif2fastq -f --input " + consensusDMA + " --group R5=" + fastqR5 + " R1=" + fastqR1
                + " UMI=" + fastqUMI + " R4=" + fastqR4);
        for (String fileName : new String[] { inputFile, extracted, sorted1, corrected, sorted2,
                consensus, consensusOrig, consensusDMA, fastqR1, fastqR4, fastqR5, fastqUMI })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void wrongOverrideTest() throws Exception {
        String inputFile = getExampleMif("twosided-raw");
        assertOutputContains(true, "patterns (3) and target reads (2)", () -> callableExec(
                "extract -f --input " + inputFile + " --output " + inputFile + " --input-format MIF"
                        + " --pattern \"(UMI:NNNNNNNN)\\atta(R1:gaca)(R2:nnn)\\*\""));
        assertOutputContains(true, "R1 not found", () -> callableExec(
                "extract -f --input " + inputFile + " --output " + inputFile + " --input-format MIF"
                        + " --pattern \"(UMI:NNNNNNNN)\\atta(R2:nnn)\""));
        assertOutputContains(true, "R16 is found, but group R15 is missing", () -> callableExec(
                "extract -f --input " + inputFile + " --output " + inputFile + " --input-format MIF"
                        + " --pattern \"(UMI:NNNNNNNN)(R1:gaca)\\atta(R16:nnn)\""));
        assertTrue(new File(inputFile).delete());
    }

    @Test
    public void barcodesLengthTest() throws Exception {
        String inputFastqFiles = getBigOrSmallFastqTestFileNames("SRR7191987_1.fastq", "SRR7191987_2.fastq");
        String extracted = TEMP_DIR + "extracted.mif";
        String fastqR1 = TEMP_DIR + "R1.fastq";
        String fastqR2 = TEMP_DIR + "R2.fastq";
        String pattern = inputFastqFiles.contains("SRR")
                ? "\"^(B1:N{8:12})gagtgattgcttgtgacgccaa(B2:N{8})(UMI:N{8})\\*\""
                : "\"^(B1:N{8:12})gagt(B2:N{8})(UMI:N{8})\\*\"";
        exec("extract -f --input " + inputFastqFiles + " --output " + extracted + " --pattern " + pattern
                + " --try-reverse-order");
        exec("mif2fastq -f --input " + extracted + " --group R1=" + fastqR1 + " R2=" + fastqR2);
        BufferedReader reader = new BufferedReader(new FileReader(fastqR1));
        String firstLine = reader.readLine();
        reader.close();
        for (String fileName : new String[] { extracted, fastqR1, fastqR2 })
            assertTrue(new File(fileName).delete());
        String[] firstLineParts = firstLine.split("\\|");
        for (int i = 0; i <= 2; i++) {
            String[] currentPartTokens = firstLineParts[i].split("~");
            if (i == 0)
                assertTrue((currentPartTokens[1].length() >= 8) && (currentPartTokens[1].length() <= 12));
            else
                assertEquals(8, currentPartTokens[1].length());
        }
    }

    @Test
    public void debugStatsTest() throws Exception {

    }
}
