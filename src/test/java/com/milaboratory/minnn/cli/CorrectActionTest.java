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
        String outputFile = TEMP_DIR + "correctOutput.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            exec("extract -f --input-format mif --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            exec("correct -f --max-mismatches " + rg.nextInt(4) + " --max-indels " + rg.nextInt(4)
                    + " --max-total-errors " + rg.nextInt(5)
                    + " --cluster-threshold " + (rg.nextFloat() * 0.98 + 0.01)
                    + " --input " + inputFile + " --output " + outputFile + " --groups G1 G2");
            assertFileNotEquals(inputFile, outputFile);
            assertMifNotEqualsAsFastq(inputFile, outputFile, false);
        }
        for (String fileName : new String[] { startFile, inputFile, outputFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        assertOutputContains(true, "Error", () -> callableExec("correct -f --output " + inputFile
                + " --groups G1"));
        assertOutputContains(true, "Error", () -> callableExec("correct -f --input " + inputFile
                + " --output " + inputFile));
        for (int i = 0; i < 4; i++) {
            String currentInput = (i == 0) ? inputFile : TEMP_DIR + "correct" + i + ".mif";
            String currentOutput = TEMP_DIR + "correct" + (i + 1) + ".mif";
            exec("correct -f --groups G1 G2 G3 G4 --input " + currentInput + " --output " + currentOutput
                    + " --cluster-threshold 0.4 --single-substitution-probability 0.002"
                    + " --single-indel-probability 0.001");
            assertFileNotEquals(currentInput, currentOutput);
            if (i < 3) {
                assertMifNotEqualsAsFastq(currentInput, currentOutput, true);
            } else
                assertMifEqualsAsFastq(currentInput, currentOutput, true);
        }
        exec("correct -f --input " + inputFile + " --output " + TEMP_DIR + "correct4.mif --max-total-errors 0"
                + " --groups G1 G2 G3 G4");
        assertFileNotEquals(inputFile, TEMP_DIR + "correct4.mif");
        assertMifNotEqualsAsFastq(inputFile, TEMP_DIR + "correct4.mif", true);
        exec("correct -f --input " + inputFile + " --output " + TEMP_DIR + "correct5.mif --max-mismatches 0" +
                " --max-indels 0 --groups G1 G2 G3 G4");
        assertFileNotEquals(TEMP_DIR + "correct4.mif", TEMP_DIR + "correct5.mif");
        assertMifEqualsAsFastq(TEMP_DIR + "correct4.mif", TEMP_DIR + "correct5.mif", true);
        assertTrue(new File(inputFile).delete());
        for (int i = 1; i <= 5; i++)
            assertTrue(new File(TEMP_DIR + "correct" + i + ".mif").delete());
    }
}
