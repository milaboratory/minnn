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

import java.io.File;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class CorrectActionTest {
    @BeforeClass
    public static void init() {
        actionTestInit();
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
        for (int i = 0; i <= 2; i++) {
            String currentInputTest1 = (i == 0) ? inputFile : TEMP_DIR + "correct_t1_" + i + ".mif";
            String currentInputTest2 = (i == 0) ? inputFile : TEMP_DIR + "correct_t2_" + i + ".mif";
            String currentSortedFileTest1 = TEMP_DIR + "sorted_t1_" + (i + 1) + ".mif";
            String currentSortedFileTest2 = TEMP_DIR + "sorted_t2_" + (i + 1) + ".mif";
            String currentOutputTest1 = TEMP_DIR + "correct_t1_" + (i + 1) + ".mif";
            String currentOutputTest2 = TEMP_DIR + "correct_t2_" + (i + 1) + ".mif";
            String currentOutputTest3 = TEMP_DIR + "correct_t3_" + (i + 1) + ".mif";
            sortFile(currentInputTest1, currentSortedFileTest1, "G1 G2 G3 G4");
            sortFile(currentInputTest2, currentSortedFileTest2, "G1 G2 G3 G4");
            assertOutputContains(true, "Error", () -> callableExec("correct -f" +
                    " --input " + currentSortedFileTest1 + " --output " + currentOutputTest1 +
                    " --groups G1 --max-errors-share -1"));
            exec("correct -f --groups G1 G2 G3 G4 --input " + currentSortedFileTest1
                    + " --output " + currentOutputTest1
                    + " --cluster-threshold 0.4 --single-substitution-probability 0.002"
                    + " --single-indel-probability 0.001 --max-errors 3 --max-errors-share -1");
            assertFileNotEquals(currentSortedFileTest1, currentOutputTest1);
            if (i < 2)
                assertMifNotEqualsAsFastq(currentSortedFileTest1, currentOutputTest1, true);
            else
                assertMifEqualsAsFastq(currentSortedFileTest1, currentOutputTest1, true);
            exec("correct -f --input " + currentSortedFileTest2 + " --output " + currentOutputTest2
                    + " --max-errors 0 --groups G1 G2 G3 G4");
            assertFileNotEquals(currentSortedFileTest2, currentOutputTest2);
            if (i < 2)
                assertMifNotEqualsAsFastq(currentSortedFileTest2, currentOutputTest2, true);
            else
                assertMifEqualsAsFastq(currentSortedFileTest2, currentOutputTest2, true);
            exec("correct -f --input " + currentSortedFileTest2 + " --output " + currentOutputTest3
                    + " --max-errors 0 --groups G1 G2 G3 G4 --max-errors-share 0.5");
            assertFileNotEquals(currentOutputTest2, currentOutputTest3);
            assertMifEqualsAsFastq(currentOutputTest2, currentOutputTest3, true);
        }
        assertTrue(new File(inputFile).delete());
        assertTrue(new File(sortedInputFile).delete());
        for (int i = 1; i <= 3; i++)
            for (int j = 1; j <= 3; j++) {
                if (j < 3)
                    assertTrue(new File(TEMP_DIR + "sorted_t" + j + "_" + i + ".mif").delete());
                assertTrue(new File(TEMP_DIR + "correct_t" + j + "_" + i + ".mif").delete());
            }
    }

    @Test
    public void maxUniqueBarcodesTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String sortedInputFile = TEMP_DIR + "sorted.mif";
        sortFile(inputFile, sortedInputFile, "G3 G4");
        for (int i = 0; i < 10; i++) {
            String currentInput;
            if (i == 0)
                currentInput = sortedInputFile;
            else {
                currentInput = TEMP_DIR + "correct" + i + "_sorted.mif";
                sortFile(TEMP_DIR + "correct" + i + ".mif", currentInput, "G3 G4");
            }
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
        assertTrue(new File(sortedInputFile).delete());
        for (int i = 1; i <= 10; i++) {
            assertTrue(new File(TEMP_DIR + "correct" + i + ".mif").delete());
            if (i < 10) {
                assertTrue(new File(TEMP_DIR + "correct" + i + "_sorted.mif").delete());
                assertTrue(new File(TEMP_DIR + "excluded" + i + ".mif").delete());
            }
        }
    }

    @Test
    public void minCountTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String sortedInputFile = TEMP_DIR + "sorted.mif";
        sortFile(inputFile, sortedInputFile, "G3 G4");
        for (int i = 1; i < 10; i++) {
            String currentInput;
            if (i == 1)
                currentInput = sortedInputFile;
            else {
                currentInput = TEMP_DIR + "correct" + i + "_sorted.mif";
                sortFile(TEMP_DIR + "correct" + i + ".mif", currentInput, "G3 G4");
            }
            String currentOutput = TEMP_DIR + "correct" + (i + 1) + ".mif";
            if (i < 9) {
                exec("correct -f --groups G3 G4 --input " + currentInput + " --output " + currentOutput
                        + " --max-errors 0 --min-count " + (int)Math.pow(i, 2));
                assertFileNotEquals(currentInput, currentOutput);
                assertMifNotEqualsAsFastq(currentInput, currentOutput, true);
            } else {
                exec("correct -f --groups G3 G4 --input " + currentInput + " --output " + currentOutput
                        + " --max-errors 0 --min-count 1");
                assertFileNotEquals(currentInput, currentOutput);
                assertMifEqualsAsFastq(currentInput, currentOutput, true);
            }
        }
        assertTrue(new File(inputFile).delete());
        assertTrue(new File(sortedInputFile).delete());
        for (int i = 2; i <= 10; i++) {
            assertTrue(new File(TEMP_DIR + "correct" + i + ".mif").delete());
            if (i < 10)
                assertTrue(new File(TEMP_DIR + "correct" + i + "_sorted.mif").delete());
        }
    }

    @Test
    public void randomSortedClustersTest() throws Exception {
        String start = TEMP_DIR + "start.mif";
        String extracted = TEMP_DIR + "extracted.mif";
        String sorted = TEMP_DIR + "sorted.mif";
        String outputPrimary = TEMP_DIR + "correctPrimary.mif";
        String outputSorted = TEMP_DIR + "sortedPrimary.mif";
        String outputSecondary = TEMP_DIR + "correctSecondary.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(start);
            exec("extract -f --input-format MIF --input " + start + " --output " + extracted
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            sortFile(extracted, sorted, "G1");
            exec("correct -f --max-errors-share " + (rg.nextInt(10) / 10f)
                    + " --max-errors " + (rg.nextInt(5) - 1) + " --max-unique-barcodes " + rg.nextInt(10)
                    + " --cluster-threshold " + (rg.nextFloat() * 0.98 + 0.01)
                    + " --input " + sorted + " --output " + outputPrimary + " --groups G1");
            exec("sort -f --input " + outputPrimary + " --output " + outputSorted + " --groups G1 G2");
            exec("correct -f --max-errors-share " + (rg.nextInt(10) / 10f)
                    + " --max-errors " + (rg.nextInt(5) - 1) + " --max-unique-barcodes " + rg.nextInt(10)
                    + " --cluster-threshold " + (rg.nextFloat() * 0.98 + 0.01)
                    + " --input " + outputSorted + " --output " + outputSecondary
                    + " --primary-groups G1 --groups G2 --threads " + (rg.nextInt(10) + 1));
            assertFileNotEquals(sorted, outputPrimary);
            assertFileNotEquals(outputSorted, outputSecondary);
        }
        for (String fileName : new String[] { start, extracted, sorted, outputPrimary, outputSorted, outputSecondary })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifClustersTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        for (int i = 0; i <= 2; i++) {
            String currentInput = (i == 0) ? inputFile : TEMP_DIR + "correctedSecondary" + i + ".mif";
            String currentSortedInput = TEMP_DIR + "sortedInput" + (i + 1) + ".mif";
            String currentPrimaryOutput = TEMP_DIR + "correctedPrimary" + (i + 1) + ".mif";
            String currentSortedOutput = TEMP_DIR + "sortedPrimary" + (i + 1) + ".mif";
            String currentSecondaryOutput = TEMP_DIR + "correctedSecondary" + (i + 1) + ".mif";
            sortFile(currentInput, currentSortedInput, "G1 G2");
            exec("correct -f --groups G1 G2 --input " + currentSortedInput
                    + " --output " + currentPrimaryOutput + " --max-errors-share 0.3 --cluster-threshold 0.4"
                    + " --single-substitution-probability 0.001 --single-indel-probability 0.0005");
            sortFile(currentPrimaryOutput, currentSortedOutput, "G1 G2 G3 G4");
            exec("correct -f --primary-groups G1 G2 --groups G3 G4 --input " + currentSortedOutput
                    + " --output " + currentSecondaryOutput + " --max-errors-share 0.3 --cluster-threshold 0.4"
                    + " --single-substitution-probability 0.001 --single-indel-probability 0.0005");
            assertFileNotEquals(currentSortedInput, currentSecondaryOutput);
            if (i < 2)
                assertMifNotEqualsAsSortedFastq(currentSortedInput, currentSecondaryOutput, true,
                        "G1 G2 G3 G4");
            else
                assertMifEqualsAsSortedFastq(currentSortedInput, currentSecondaryOutput, true,
                        "G1 G2 G3 G4");
        }
        assertTrue(new File(inputFile).delete());
        for (int i = 1; i <= 3; i++) {
            for (String prefix : new String[] { "sortedInput", "correctedPrimary", "sortedPrimary",
                    "correctedSecondary" })
                assertTrue(new File(TEMP_DIR + prefix + i + ".mif").delete());
        }
    }

    @Test
    public void emptyReadsTest() throws Exception {
        String inputFile = getExampleMif("with-empty-reads");
        String sortedG1G2_1 = TEMP_DIR + "sortedG1G2_1.mif";
        String sortedG2G1_1 = TEMP_DIR + "sortedG2G1_1.mif";
        String sortedG1G2_2 = TEMP_DIR + "sortedG1G2_2.mif";
        String sortedG2G1_2 = TEMP_DIR + "sortedG2G1_2.mif";
        sortFile(inputFile, sortedG1G2_1, "G1 G2");
        sortFile(inputFile, sortedG2G1_1, "G2 G1");

        String correctedG1 = TEMP_DIR + "correctedG1.mif";
        String correctedG2 = TEMP_DIR + "correctedG2.mif";
        String correctedG1G2 = TEMP_DIR + "correctedG1G2.mif";
        String correctedG2inG1 = TEMP_DIR + "correctedG2inG1.mif";
        String correctedG1inG2 = TEMP_DIR + "correctedG1inG2.mif";
        exec("correct -f --groups G1 --input " + sortedG1G2_1 + " --output " + correctedG1);
        exec("correct -f -n 1000 --groups G2 --input " + sortedG2G1_1 + " --output " + correctedG2);
        exec("correct -f -n 1000 --groups G1 G2 --input " + sortedG1G2_1 + " --output " + correctedG1G2);
        sortFile(correctedG1, sortedG1G2_2, "G1 G2");
        sortFile(correctedG2, sortedG2G1_2, "G2 G1");
        exec("correct -f -n 5000 --primary-groups G1 --groups G2 --input " + sortedG1G2_2
                + " --output " + correctedG2inG1);
        exec("correct -f --primary-groups G2 --groups G1 --input " + sortedG2G1_2
                + " --output " + correctedG1inG2);

        for (String fileName : new String[] { inputFile, sortedG1G2_1, sortedG2G1_1, sortedG1G2_2, sortedG2G1_2,
                correctedG1, correctedG2, correctedG1G2, correctedG2inG1, correctedG1inG2 })
            assertTrue(new File(fileName).delete());
    }
}
