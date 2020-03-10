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

public class FilterByCountActionTest {
    @BeforeClass
    public static void init() {
        actionTestInit();
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "correctStart.mif";
        String inputFile = TEMP_DIR + "correctInput.mif";
        String outputFile = TEMP_DIR + "correctOutput.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            exec("extract -f --input-format MIF --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            exec("filter-by-count -f --min-count " + rg.nextInt(20)
                    + " --max-unique-barcodes " + rg.nextInt(10)
                    + " --input " + inputFile + " --output " + outputFile + " --groups G1 G2");
            assertFileNotEquals(inputFile, outputFile);
        }
        for (String fileName : new String[] { startFile, inputFile, outputFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void maxUniqueBarcodesTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        for (int i = 0; i < 10; i++) {
            String currentInput = (i == 0) ? inputFile : TEMP_DIR + "filtered" + i + ".mif";
            String currentOutput = TEMP_DIR + "filtered" + (i + 1) + ".mif";
            String currentExcludedOutput = TEMP_DIR + "excluded" + (i + 1) + ".mif";
            if (i < 9) {
                int maxUniqueBarcodes = 50 - i * 5;
                exec("filter-by-count -f --groups G3 G4 --input " + currentInput + " --output " + currentOutput
                        + " --excluded-barcodes-output " + currentExcludedOutput
                        + " --max-unique-barcodes " + maxUniqueBarcodes);
                assertFileNotEquals(currentInput, currentOutput);
                assertMifNotEqualsAsFastq(currentInput, currentOutput, true);
            } else {
                exec("filter-by-count -f --groups G3 G4 --input " + currentInput + " --output " + currentOutput
                        + " --max-unique-barcodes 0");
                assertFileNotEquals(currentInput, currentOutput);
                assertMifEqualsAsFastq(currentInput, currentOutput, true);
            }
        }
        assertTrue(new File(inputFile).delete());
        for (int i = 1; i <= 10; i++) {
            assertTrue(new File(TEMP_DIR + "filtered" + i + ".mif").delete());
            if (i < 10)
                assertTrue(new File(TEMP_DIR + "excluded" + i + ".mif").delete());
        }
    }

    @Test
    public void minCountTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        for (int i = 0; i < 10; i++) {
            String currentInput = (i == 0) ? inputFile : TEMP_DIR + "filtered" + i + ".mif";
            String currentOutput = TEMP_DIR + "filtered" + (i + 1) + ".mif";
            exec("filter-by-count -f --groups G3 G4 --input " + currentInput + " --output " + currentOutput
                    + " --min-count " + (int)Math.pow(i, 2));
            assertFileNotEquals(currentInput, currentOutput);
            if (i < 2)
                assertMifEqualsAsFastq(currentInput, currentOutput, true);
            else
                assertMifNotEqualsAsFastq(currentInput, currentOutput, true);
        }
        assertTrue(new File(inputFile).delete());
        for (int i = 1; i <= 10; i++)
            assertTrue(new File(TEMP_DIR + "filtered" + i + ".mif").delete());
    }
}
