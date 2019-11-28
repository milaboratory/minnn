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
package com.milaboratory.minnn.cli;

import org.junit.*;

import java.io.File;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static org.junit.Assert.*;

public class CorrectActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "correctStart.mif";
        String inputFile = TEMP_DIR + "correctInput.mif";
        String sortedFile = TEMP_DIR + "correctInputSorted.mif";
        String outputFile = TEMP_DIR + "correctOutput.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            exec("extract -f --input-format MIF --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            sortFile(inputFile, sortedFile, "G1 G2");
            exec("correct -f --max-errors-share " + (rg.nextInt(10) / 10f)
                    + " --max-errors " + (rg.nextInt(5) - 1) + " --max-unique-barcodes " + rg.nextInt(10)
                    + " --cluster-threshold " + (rg.nextFloat() * 0.98 + 0.01)
                    + " --input " + sortedFile + " --output " + outputFile + " --groups G1 G2");
            assertFileNotEquals(sortedFile, outputFile);
        }
        for (String fileName : new String[] { startFile, inputFile, sortedFile, outputFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String sortedInputFile = TEMP_DIR + "sorted.mif";
        assertOutputContains(true, "ERROR", () -> callableExec("correct -f --input " + inputFile
                + " --groups G1 --output " + sortedInputFile));
        sortFile(inputFile, sortedInputFile, "G1 G2 G3 G4");
        assertOutputContains(true, "Error", () -> callableExec("correct -f --output " + inputFile
                + " --groups G1"));
        assertOutputContains(true, "Error", () -> callableExec("correct -f"
                + " --input " + sortedInputFile + " --output " + inputFile));
        for (int i = 0; i <= 1; i++) {
            String currentInput = (i == 0) ? inputFile : TEMP_DIR + "correct" + i + ".mif";
            String currentSortedFile = TEMP_DIR + "sorted" + (i + 1) + ".mif";
            String currentOutput = TEMP_DIR + "correct" + (i + 1) + ".mif";
            sortFile(currentInput, currentSortedFile, "G1 G2 G3 G4");
            assertOutputContains(true, "Error", () -> callableExec("correct -f" +
                    " --input " + currentSortedFile + " --output " + currentOutput +
                    " --groups G1 --max-errors-share -1"));
            exec("correct -f --groups G1 G2 G3 G4 --input " + currentSortedFile + " --output " + currentOutput
                    + " --cluster-threshold 0.4 --single-substitution-probability 0.002"
                    + " --single-indel-probability 0.001 --max-errors 3 --max-errors-share -1");
            assertFileNotEquals(currentSortedFile, currentOutput);
            if (i == 0) {
                assertMifNotEqualsAsFastq(currentSortedFile, currentOutput, true);
            } else
                assertMifEqualsAsFastq(currentSortedFile, currentOutput, true);
        }
        exec("correct -f --input " + sortedInputFile + " --output " + TEMP_DIR + "correct3.mif --max-errors 0"
                + " --groups G1 G2 G3 G4");
        assertFileNotEquals(sortedInputFile, TEMP_DIR + "correct3.mif");
        assertMifEqualsAsFastq(sortedInputFile, TEMP_DIR + "correct3.mif", true);
        exec("correct -f --input " + sortedInputFile + " --output " + TEMP_DIR + "correct4.mif"
                + " --max-errors 0 --groups G1 G2 G3 G4 --max-errors-share 0.5");
        assertFileNotEquals(TEMP_DIR + "correct3.mif", TEMP_DIR + "correct4.mif");
        assertMifEqualsAsFastq(sortedInputFile, TEMP_DIR + "correct4.mif", true);
        assertTrue(new File(inputFile).delete());
        assertTrue(new File(sortedInputFile).delete());
        for (int i = 1; i <= 4; i++) {
            if (i <= 2)
                assertTrue(new File(TEMP_DIR + "sorted" + i + ".mif").delete());
            assertTrue(new File(TEMP_DIR + "correct" + i + ".mif").delete());
        }
    }

    @Ignore
    @Test
    public void maxUniqueBarcodesTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        for (int i = 0; i < 10; i++) {
            String currentInput = (i == 0) ? inputFile : TEMP_DIR + "correct" + i + ".mif";
            String currentOutput = TEMP_DIR + "correct" + (i + 1) + ".mif";
            String currentExcludedOutput = TEMP_DIR + "excluded" + (i + 1) + ".mif";
            if (i < 9) {
                int maxUniqueBarcodes = 50 - i * 5;
                exec("correct -f --groups G3 G4 --input " + currentInput + " --output " + currentOutput
                        + " --excluded-barcodes-output " + currentExcludedOutput
                        + " --cluster-threshold 0.4 --single-substitution-probability 0.002"
                        + " --single-indel-probability 0.001 --max-unique-barcodes " + maxUniqueBarcodes);
                assertFileNotEquals(currentInput, currentOutput);
                assertMifNotEqualsAsFastq(currentInput, currentOutput, true);
            } else {
                exec("correct -f --groups G3 G4 --input " + currentInput + " --output " + currentOutput
                        + " --cluster-threshold 0.4 --single-substitution-probability 0.002"
                        + " --single-indel-probability 0.001 --max-unique-barcodes 0");
                assertFileNotEquals(currentInput, currentOutput);
                assertMifEqualsAsFastq(currentInput, currentOutput, true);
            }
        }
        assertTrue(new File(inputFile).delete());
        for (int i = 1; i <= 10; i++) {
            assertTrue(new File(TEMP_DIR + "correct" + i + ".mif").delete());
            if (i < 10)
                assertTrue(new File(TEMP_DIR + "excluded" + i + ".mif").delete());
        }
    }

    @Ignore
    @Test
    public void minCountTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        for (int i = 0; i < 10; i++) {
            String currentInput = (i == 0) ? inputFile : TEMP_DIR + "correct" + i + ".mif";
            String currentOutput = TEMP_DIR + "correct" + (i + 1) + ".mif";
            if (i < 9) {
                exec("correct -f --groups G3 G4 --input " + currentInput + " --output " + currentOutput
                        + " --max-errors 0 --min-count " + (int)Math.pow(i, 2));
                assertFileNotEquals(currentInput, currentOutput);
                if (i <= 1)
                    assertMifEqualsAsFastq(currentInput, currentOutput, true);
                else
                    assertMifNotEqualsAsFastq(currentInput, currentOutput, true);
            } else {
                exec("correct -f --groups G3 G4 --input " + currentInput + " --output " + currentOutput
                        + " --max-errors 0 --min-count 1");
                assertFileNotEquals(currentInput, currentOutput);
                assertMifEqualsAsFastq(currentInput, currentOutput, true);
            }
        }
        assertTrue(new File(inputFile).delete());
        for (int i = 1; i <= 10; i++)
            assertTrue(new File(TEMP_DIR + "correct" + i + ".mif").delete());
    }

    @Ignore
    @Test
    public void randomSortedClustersTest() throws Exception {
        String startFile = TEMP_DIR + "correctStart.mif";
        String inputFile = TEMP_DIR + "correctInput.mif";
        String outputPrimary = TEMP_DIR + "correctPrimary.mif";
        String outputSorted = TEMP_DIR + "sortedPrimary.mif";
        String outputSecondary = TEMP_DIR + "correctSecondary.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            exec("extract -f --input-format MIF --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            exec("correct -f --max-errors-share " + (rg.nextInt(10) / 10f)
                    + " --max-errors " + (rg.nextInt(5) - 1) + " --max-unique-barcodes " + rg.nextInt(10)
                    + " --cluster-threshold " + (rg.nextFloat() * 0.98 + 0.01)
                    + " --input " + inputFile + " --output " + outputPrimary + " --groups G1");
            exec("sort -f --input " + outputPrimary + " --output " + outputSorted + " --groups G1");
            exec("correct -f --max-errors-share " + (rg.nextInt(10) / 10f)
                    + " --max-errors " + (rg.nextInt(5) - 1) + " --max-unique-barcodes " + rg.nextInt(10)
                    + " --cluster-threshold " + (rg.nextFloat() * 0.98 + 0.01)
                    + " --input " + outputSorted + " --output " + outputSecondary
                    + " --primary-groups G1 --groups G2");
            assertFileNotEquals(inputFile, outputPrimary);
            assertFileNotEquals(outputSorted, outputSecondary);
        }
        for (String fileName : new String[] { startFile, inputFile, outputPrimary, outputSorted, outputSecondary })
            assertTrue(new File(fileName).delete());
    }

    @Ignore
    @Test
    public void preparedMifClustersTest() throws Exception {
        for (boolean sorted : new boolean[] { true, false }) {
            String inputFile = getExampleMif("twosided");
            for (int i = 0; i <= 2; i++) {
                String currentInput = (i == 0) ? inputFile : TEMP_DIR + "correctedSecondary" + i + ".mif";
                String currentPrimaryOutput = TEMP_DIR + "correctedPrimary" + (i + 1) + ".mif";
                String currentSortedOutput = TEMP_DIR + "sortedPrimary" + (i + 1) + ".mif";
                String currentSecondaryOutput = TEMP_DIR + "correctedSecondary" + (i + 1) + ".mif";
                exec("correct -f --groups G1 G2 --input " + currentInput + " --output " + currentPrimaryOutput
                        + " --max-errors-share 0.4 --cluster-threshold 0.4 --single-substitution-probability 0.002"
                        + " --single-indel-probability 0.001 --fair-wildcards-collapsing");
                if (sorted)
                    exec("sort -f --groups G1 G2 --input " + currentPrimaryOutput
                            + " --output " + currentSortedOutput);
                exec("correct -f --primary-groups G1 G2 --groups G3 G4 --input "
                        + (sorted ? currentSortedOutput : currentPrimaryOutput)
                        + " --output " + currentSecondaryOutput + " --max-errors-share 0.4 --cluster-threshold 0.4"
                        + " --single-substitution-probability 0.002 --single-indel-probability 0.001"
                        + " --fair-wildcards-collapsing");
                assertFileNotEquals(currentInput, currentSecondaryOutput);
                if (i < 2) {
                    assertMifNotEqualsAsFastq(currentInput, currentSecondaryOutput, true);
                } else {
                    if (sorted)
                        assertMifEqualsAsFastq(currentInput, currentSecondaryOutput, true);
                    else {
                        String sorted1 = TEMP_DIR + "sorted1-" + i + ".mif";
                        String sorted2 = TEMP_DIR + "sorted2-" + i + ".mif";
                        exec("sort -f --groups G1 G2 G3 G4 --input " + currentInput
                                + " --output " + sorted1);
                        exec("sort -f --groups G1 G2 G3 G4 --input " + currentSecondaryOutput
                                + " --output " + sorted2);
                        assertMifEqualsAsFastq(sorted1, sorted2, true);
                        for (String sortedFile : new String[] { sorted1, sorted2 })
                            assertTrue(new File(sortedFile).delete());
                    }
                }
            }
            assertTrue(new File(inputFile).delete());
            for (int i = 1; i <= 3; i++) {
                for (String prefix : new String[] { "correctedPrimary", "correctedSecondary" })
                    assertTrue(new File(TEMP_DIR + prefix + i + ".mif").delete());
                if (sorted)
                    assertTrue(new File(TEMP_DIR + "sortedPrimary" + i + ".mif").delete());
            }
        }
    }
}
