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

public class DecontaminateActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "decontaminateStart.mif";
        String inputFile = TEMP_DIR + "decontaminateInput.mif";
        String outputFile = TEMP_DIR + "decontaminateOutput.mif";
        String excludedFile = TEMP_DIR + "decontaminateExcluded.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            exec("extract -f --input-format MIF --input " + startFile + " --output " + inputFile
                    + " --pattern \"(CB:annnt)(UMI:NN)\" --bitap-max-errors 0");
            exec("decontaminate -f --min-count-share " + rg.nextFloat()
                    + " --input " + inputFile + " --output " + outputFile + " --primary-groups CB --groups UMI"
                    + " --excluded-barcodes-output " + excludedFile);
            assertFileNotEquals(inputFile, outputFile);
        }
        for (String fileName : new String[] { startFile, inputFile, outputFile, excludedFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        for (int i = 0; i <= 1; i++) {
            String currentInput = (i == 0) ? inputFile : TEMP_DIR + "decontaminateIteration" + i + ".mif";
            String currentOutput = TEMP_DIR + "decontaminateIteration" + (i + 1) + ".mif";
            exec("decontaminate -f --primary-groups G1 G2 --groups G3 G4 --input " + currentInput
                    + " --output " + currentOutput);
            assertFileNotEquals(currentInput, currentOutput);
            if (i == 0)
                assertMifNotEqualsAsFastq(currentInput, currentOutput, true);
            else
                assertMifEqualsAsFastq(currentInput, currentOutput, true);
        }
        assertTrue(new File(inputFile).delete());
        for (int i = 1; i <= 2; i++)
            assertTrue(new File(TEMP_DIR + "decontaminateIteration" + i + ".mif").delete());
    }
}
