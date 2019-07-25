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

public class SortActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "sortStart.mif";
        String inputFile = TEMP_DIR + "sortInput.mif";
        String outputFile1 = TEMP_DIR + "sortOutput1.mif";
        String outputFile2 = TEMP_DIR + "sortOutput2.mif";
        String outputFile3 = TEMP_DIR + "sortOutput3.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            exec("extract -f --input-format MIF --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:an{3}t)(G2:n{2})\" --bitap-max-errors 0");
            exec("sort -f --chunk-size " + (rg.nextInt(50000) + 100) + " --input " + inputFile
                    + " --output " + outputFile1 + " --groups G2 G1");
            exec("sort -f --input " + outputFile1 + " --output " + outputFile2 + " --groups G1");
            exec("sort -f --chunk-size " + (rg.nextInt(50000) + 100) + " --input " + outputFile2
                    + " --output " + outputFile3 + " --groups G2 G1");
            assertFileNotEquals(outputFile1, outputFile3);
            assertMifEqualsAsFastq(outputFile1, outputFile3, false);
        }
        for (String fileName : new String[] { startFile, inputFile, outputFile1, outputFile2, outputFile3 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String outputFile1 = TEMP_DIR + "sortOutput1.mif";
        String outputFile2 = TEMP_DIR + "sortOutput2.mif";
        String outputFile3 = TEMP_DIR + "sortOutput3.mif";
        exec("sort -f --input " + inputFile + " --output " + outputFile1 + " --groups G3 G4 G1 G2 R1 R2");
        exec("sort -f --input " + outputFile1 + " --output " + outputFile2 + " --groups R2 G2 R1");
        exec("sort -f --input " + outputFile2 + " --output " + outputFile3 + " --groups G3 G4 G1 G2 R1 R2");
        assertFileNotEquals(outputFile1, outputFile3);
        assertMifEqualsAsFastq(outputFile1, outputFile3, true);
        for (String fileName : new String[] { inputFile, outputFile1, outputFile2, outputFile3 })
            assertTrue(new File(fileName).delete());
    }
}
