/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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

import java.io.File;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.minnn.cli.Main.main;
import static org.junit.Assert.*;

public class ExtractActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void simpleTest() throws Exception {
        for (String suffix : new String[] {"", ".gz"}) {
            String testInputR1 = TEST_RESOURCES_PATH + "sample_r1.fastq" + suffix;
            String testInputR2 = TEST_RESOURCES_PATH + "sample_r2.fastq" + suffix;
            String testOutput1Double = TEMP_DIR + "output1_double.mif";
            String testOutput1Single = TEMP_DIR + "output1_single.mif";
            String testOutput2Double = TEMP_DIR + "output2_double.mif";
            String testOutput2Single = TEMP_DIR + "output2_single.mif";

            String[] args1 = {"extract", "-f", "--pattern",
                    inQuotes("MultiPattern([FullReadPattern(false, FuzzyMatchPattern(GAAGCA, 1, 0, -1, -1, " +
                            "[GroupEdgePosition(GroupEdge('UMI', true), 2), " +
                            "GroupEdgePosition(GroupEdge('UMI', false), 4)])), " +
                            "FullReadPattern(false, FuzzyMatchPattern(AA, 0, 0, -1, -1))])"),
                    "--input", testInputR1, testInputR2, "--output", testOutput1Double,
                    "--devel-parser-syntax", "--score-threshold", "0"};
            main(args1);

            String[] args2 = {"extract", "-f", "--score-threshold", "0", "--devel-parser-syntax", "--match-score", "0",
                    "--oriented", "--pattern", inQuotes("FullReadPattern(false, FuzzyMatchPattern(ATTAGACA, " +
                    "0, 0, -1, -1))"), "--input", testInputR1, "--output", testOutput1Single};
            main(args2);

            String[] args3 = {"extract", "-f", "--pattern", "<GA(UMI:AG)CA \\ AA",
                    "--input", testInputR1, testInputR2, "--output", testOutput2Double, "--score-threshold", "0"};
            main(args3);

            String[] args4 = {"extract", "-f", "--score-threshold", "0", "--match-score", "0", "--oriented",
                    "--pattern", "ATTAGACA", "--input", testInputR1, "--output", testOutput2Single};
            main(args4);

            assertMifEqualsAsFastq(testOutput1Double, testOutput2Double, true);
            assertMifEqualsAsFastq(testOutput1Single, testOutput2Single, false);
            for (String fileName : new String[] {
                    testOutput1Double, testOutput2Double, testOutput1Single, testOutput2Single })
                assertTrue(new File(fileName).delete());
        }
    }

    @Test
    public void examplesTest() throws Exception {
        String posR1 = EXAMPLES_PATH + "positional/polyfid10_R1.fastq.gz";
        String posR2 = EXAMPLES_PATH + "positional/polyfid10_R2.fastq.gz";
        String outPos = TEMP_DIR + "outputPos.mif";
        String positionalArgs = "extract -f --input-format FASTQ --input " + posR1 + " " + posR2
                + " --output " + outPos;

        String oneR1 = EXAMPLES_PATH + "onesided/sl4000_R1.fastq.gz";
        String oneR2 = EXAMPLES_PATH + "onesided/sl4000_R2.fastq.gz";
        String outOne = TEMP_DIR + "outputOne.mif";
        String onesidedArgs = "extract -f --input-format FASTQ --input " + oneR1 + " " + oneR2
                + " --output " + outOne;

        String twoR1 = EXAMPLES_PATH + "twosided/p109_R1.fastq.gz";
        String twoR2 = EXAMPLES_PATH + "twosided/p109_R2.fastq.gz";
        String outTwo = TEMP_DIR + "outputTwo.mif";
        String twosidedArgs = "extract -f --input-format FASTQ --input " + twoR1 + " " + twoR2
                + " --output " + outTwo;

        String twoMif = getExampleMif("twosided-raw");
        String outTwoMif = TEMP_DIR + "outputTwoMif.mif";
        String twosidedMifArgs = "extract -f --input-format MIF --input " + twoMif
                + " --output " + outTwoMif;

        String configuration = " --match-score 0 --mismatch-score -7 --gap-score -11 --single-overlap-penalty -10 "
                + "--bitap-max-errors 3 --threads 4";

        String patternPos = "^(UMI:N{14})n{22}(SB:N{4}) \\ *";
        String penaltyPos = " --score-threshold -200";

        String patternOne = "(SB:N{5})[aagc || c]agtggtatcaacgcagagt(UMI:N{14}) \\ *";
        String penaltyOne = " --score-threshold -100";

        String patternTwo = "(SB1:N{5})[aagc || c]agtggtatcaacgcagagt(UMI:N{14}) \\ (SB2:N{5})gtcacatttctcagatcct";
        String penaltyTwo = " --score-threshold -130";

        exec(positionalArgs + configuration + penaltyPos + " --pattern " + inQuotes(patternPos));
        exec(onesidedArgs + configuration + penaltyOne + " --pattern " + inQuotes(patternOne));
        exec(twosidedArgs + configuration + penaltyTwo + " --pattern " + inQuotes(patternTwo));
        exec(twosidedMifArgs + configuration + penaltyTwo + " --pattern " + inQuotes(patternTwo));

        assertMifEqualsAsFastq(outTwo, outTwoMif, true);

        for (String fileName : new String[] { outPos, outOne, outTwo, twoMif, outTwoMif })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void specialCaseTest1() throws Exception {
        String R1 = EXAMPLES_PATH + "positional/polyfid10_R1.fastq.gz";
        String R2 = EXAMPLES_PATH + "positional/polyfid10_R2.fastq.gz";
        String out = TEMP_DIR + "outputSCT1.mif";
        String argsIO = "extract -f --input " + R1 + " " + R2 + " --output " + out;
        String query = argsIO + " --pattern \"NN(G1:N{12})N{22}TCAG\\NN(G2:N{12})N{22}TCAG\" --threads 3 "
                + "--bitap-max-errors 1 --mismatch-score -1 --score-threshold -73";
        exec(query);
        assertTrue(new File(out).delete());
    }

    @Test
    public void specialCaseTest2() throws Exception {
        String inputFile = getExampleMif("100reads");
        String outputFile = TEMP_DIR + "outputSCT2.mif";
        String argsIO = "extract -f --input-format MIF --input " + inputFile + " --output " + outputFile;
        String query = argsIO + " --pattern \"(G1:accnt) & (G2:nctn) & (G3:atta)\" --bitap-max-errors 2 " +
                "--score-threshold -80";
        exec(query);
        for (String fileName : new String[] { inputFile, outputFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void mifRandomTest() throws Exception {
        String mifFile1 = TEMP_DIR + "output1.mif";
        String mifFile2 = TEMP_DIR + "output2.mif";
        String mismatchedMif = TEMP_DIR + "mismatched.mif";
        String mismatchedFastq = TEMP_DIR + "mismatched.fastq";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(mifFile1);
            exec("extract -f --input-format MIF --input " + mifFile1 + " --output " + mifFile2
                    + " --pattern \"*\" --not-matched-output " + mismatchedMif);
            assertMifEqualsAsFastq(mifFile1, mifFile2, false);
            exec("mif2fastq -f --input " + mismatchedMif + " --group R1=" + mismatchedFastq);
        }
        for (String fileName : new String[] { mifFile1, mifFile2, mismatchedMif, mismatchedFastq })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void descriptionGroupsTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String fastqR1 = TEMP_DIR + "desc_group_test_R1.fastq";
        String fastqR2 = TEMP_DIR + "desc_group_test_R2.fastq";
        String outputFile = TEMP_DIR + "desc_group_test_out.mif";
        exec("mif2fastq -f --copy-original-headers --input " + inputFile + " --group R1=" + fastqR1
                + " --group R2=" + fastqR2);
        exec("extract -f --input " + fastqR1 + " " + fastqR2 + " --output " + outputFile
                + " --description-group DG1='(?<=G1~)[a-zA-Z]*(?=~)' --pattern \"(G1:cccnn)\\*\""
                + " --description-group DG4='G4~(?<seq>[a-zA-Z]*)~(?<qual>.*?)\\{' --score-threshold 0");
        for (String fileName : new String[] { inputFile, fastqR1, fastqR2, outputFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void multipleThreadsTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String outputFile = TEMP_DIR + "outputMTT.mif";
        String mismatchedOutputFile = TEMP_DIR + "mismatchedOutputMTT.mif";
        String mismatchedR1 = TEMP_DIR + "mismatchedR1-MTT.fastq";
        String mismatchedR2 = TEMP_DIR + "mismatchedR2-MTT.fastq";
        String argsIO = "extract -f --input-format MIF --input " + inputFile + " --output " + outputFile
                + " --not-matched-output " + mismatchedOutputFile;
        String query = argsIO + " --pattern \"TTC + N{1:10} & TTC || AAC \\ *\" --threads 1000 --score-threshold 0";
        exec(query);
        exec("mif2fastq -f --input " + mismatchedOutputFile + " --group R1=" + mismatchedR1
                + " --group R2=" + mismatchedR2);
        for (String fileName : new String[] { inputFile, outputFile, mismatchedOutputFile,
                mismatchedR1, mismatchedR2 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void groupsOverrideTest() throws Exception {
        String r1 = EXAMPLES_PATH + "positional/polyfid10_R1.fastq.gz";
        String r2 = EXAMPLES_PATH + "positional/polyfid10_R2.fastq.gz";
        String extractedFile = TEMP_DIR + "groups_override_extracted.mif";
        String correctedFile = TEMP_DIR + "groups_override_corrected.mif";
        String sortedFile = TEMP_DIR + "groups_override_sorted.mif";
        String consensusFile = TEMP_DIR + "groups_override_consensus.mif";
        exec("extract -f --input " + r1 + " " + r2 + " --output " + extractedFile + " --score-threshold -100"
                + " --pattern \"^(R1:(G1:NNN))aac\\cctc(R2:aaa)(R3:t(G3:tt)t)$\" --bitap-max-errors 10");
        exec("correct -f --input " + extractedFile + " --output " + correctedFile + " --groups G1 G3");
        exec("sort -f --input " + correctedFile + " --output " + sortedFile + " --groups G1 G3");
        exec("consensus-dma -f --input " + sortedFile + " --output " + consensusFile + " --groups G1 G3"
                + " --score-threshold -100 --min-good-sequence-length 2 --reads-min-good-sequence-length 2"
                + " --kmer-length 2 --trim-window-size 2 --reads-trim-window-size 2");
        for (String fileName : new String[] { extractedFile, correctedFile, sortedFile, consensusFile })
            assertTrue(new File(fileName).delete());
    }
}
