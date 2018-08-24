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
import java.util.Arrays;
import java.util.stream.Stream;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static org.junit.Assert.*;

public class ConsensusActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "consensusStart.mif";
        String inputFile = TEMP_DIR + "consensusInput.mif";
        String correctedFile = TEMP_DIR + "correctCorrected.mif";
        String sortedFile = TEMP_DIR + "correctSorted.mif";
        String outputFile1 = TEMP_DIR + "consensusOutput1.mif";
        String outputFile2 = TEMP_DIR + "consensusOutput2.mif";
        String outputFile3 = TEMP_DIR + "consensusOutput3.mif";
        String outputFile4 = TEMP_DIR + "consensusOutput4.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            String consensusGroups = Arrays.asList(new String[] {"G1", "G2", "G1 G2", "G2 G1"}).get(rg.nextInt(4));
            int width = rg.nextInt(50) + 1;
            int mismatchScore = -rg.nextInt(10) - 1;
            int gapScore = -rg.nextInt(10) - 1;
            exec("extract --input-format mif --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            exec("correct --max-mismatches " + rg.nextInt(4) + " --max-indels " + rg.nextInt(4)
                    + " --max-total-errors " + rg.nextInt(5) + " --input " + inputFile
                    + " --output " + correctedFile + " --groups " + consensusGroups);
            exec("sort --input " + correctedFile + " --output " + sortedFile + " --groups " + consensusGroups);
            exec("consensus --input " + sortedFile + " --output " + outputFile1 + " --groups " + consensusGroups
                    + " --threads " + (rg.nextInt(10) + 1) + " --score-threshold " + (rg.nextInt(2000) - 1000)
                    + " --width " + width + " --max-consensuses-per-cluster " + (rg.nextInt(30) + 1)
                    + " --skipped-fraction-to-repeat " + (rg.nextFloat() * 0.8f + 0.1f)
                    + " --reads-avg-quality-threshold " + rg.nextInt(DEFAULT_GOOD_QUALITY)
                    + " --reads-trim-window-size " + (rg.nextInt(15) + 1)
                    + " --reads-min-good-sequence-length " + rg.nextInt(50)
                    + " --avg-quality-threshold " + rg.nextInt(DEFAULT_GOOD_QUALITY)
                    + " --trim-window-size " + (rg.nextInt(15) + 1)
                    + " --min-good-sequence-length " + rg.nextInt(50)
                    + " --aligner-match-score 0 --aligner-mismatch-score " + mismatchScore
                    + " --aligner-gap-score " + gapScore);
            Stream.of(new String[] { outputFile1, outputFile2 }, new String[] { outputFile2, outputFile3 },
                    new String[] { outputFile3, outputFile4 })
                    .forEach(files -> exec("consensus --input " + files[0] + " --output " + files[1]
                            + " --groups " + consensusGroups + " --threads " + (rg.nextInt(10) + 1)
                            + " --score-threshold 0 --width " + width
                            + " --max-consensuses-per-cluster 100 --skipped-fraction-to-repeat 0.001"
                            + " --reads-avg-quality-threshold 0 --avg-quality-threshold 0 --aligner-match-score 0"
                            + " --aligner-mismatch-score " + mismatchScore + " --aligner-gap-score " + gapScore));
            String parameterValuesMessage = "consensusGroups: " + consensusGroups + ", width: " + width
                    + ", mismatchScore: " + mismatchScore + ", gapScore: " + gapScore;
            assertFileEquals("Files are different with parameter values: " + parameterValuesMessage,
                    outputFile3, outputFile4);
        }
        for (String fileName : new String[] { startFile, inputFile, correctedFile, sortedFile,
                outputFile1, outputFile2, outputFile3, outputFile4 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String correctedFile = TEMP_DIR + "corrected.mif";
        String sortedFile = TEMP_DIR + "sorted.mif";
        String consensusFile = TEMP_DIR + "consensus.mif";
        String consensusFile2 = TEMP_DIR + "consensus2.mif";
        String consensusFile3 = TEMP_DIR + "consensus3.mif";
        exec("correct --input " + inputFile + " --output " + correctedFile + " --groups G3 G4 G1 G2");
        exec("sort --input " + correctedFile + " --output " + sortedFile + " --groups G3 G4 G1 G2 R1 R2");
        exec("consensus --input " + sortedFile + " --output " + consensusFile + " --groups G3 G4 G1"
                + " --threads 5 --score-threshold -1200 --width 30 --max-consensuses-per-cluster 5"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 3");
        exec("consensus --input " + consensusFile + " --output " + consensusFile2
                + " --groups G3 G4 G1 --threads 3 --score-threshold -1200 --width 30 --reads-avg-quality-threshold 0"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 0");
        exec("consensus --input " + consensusFile2 + " --output " + consensusFile3
                + " --groups G3 G4 G1 --threads 3 --score-threshold -1200 --width 30 --reads-avg-quality-threshold 0"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 0");
        assertFileEquals(consensusFile2, consensusFile3);
        for (String fileName : new String[] {
                inputFile, correctedFile, sortedFile, consensusFile, consensusFile2, consensusFile3 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void qualityOverflowTest() throws Exception {
        String inputFile = getExampleMif("good-quality");
        String consensusFile = TEMP_DIR + "consensus-qual-test.mif";
        exec("consensus --input " + inputFile + " --output " + consensusFile + " --groups G1");
        for (String fileName : new String[] { inputFile, consensusFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void numberOfReadsTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String correctedFile = TEMP_DIR + "corrected.mif";
        String sortedFile = TEMP_DIR + "sorted.mif";
        String consensusFile = TEMP_DIR + "consensus.mif";
        exec("correct --input " + inputFile + " --output " + correctedFile + " --groups G1 G2 -n 10000");
        exec("sort --input " + correctedFile + " --output " + sortedFile + " --groups G1 G2");
        exec("consensus --input " + sortedFile + " --output " + consensusFile + " --groups G1 G2 -n 1000");
        for (String fileName : new String[] { inputFile, correctedFile, sortedFile, consensusFile })
            assertTrue(new File(fileName).delete());
    }
}
