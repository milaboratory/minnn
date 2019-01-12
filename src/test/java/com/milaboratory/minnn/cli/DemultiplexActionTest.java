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
import java.util.*;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static org.junit.Assert.*;

public class DemultiplexActionTest {
    private static final String TEST_FILENAME_PREFIX = "demultiplex_test";
    private static final String LOG_FILE = TEMP_DIR + TEST_FILENAME_PREFIX + ".log";

    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "start_" + TEST_FILENAME_PREFIX + ".mif";
        String inputFile = TEMP_DIR + TEST_FILENAME_PREFIX + ".mif";
        String sampleFile4 = EXAMPLES_PATH + "demultiplex_samples/sample4.txt";
        String[] randomFilterOptions = new String[] {
                "--by-barcode G1", "--by-barcode G2", "--by-barcode G1 --by-barcode G2",
                "--by-barcode G2 --by-barcode G1", "--by-sample " + sampleFile4,
                "--by-sample " + sampleFile4 + " --by-barcode G1", "--by-barcode G1 --by-sample " + sampleFile4
        };

        Arrays.stream(getOutputFiles()).map(File::delete).forEach(Assert::assertTrue);
        for (int i = 0; i < 50; i++) {
            String filterOptions = randomFilterOptions[rg.nextInt(randomFilterOptions.length)];
            createRandomMifFile(startFile);
            exec("extract -f --input-format mif --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:tnncn)(G2:ncnc)\" --bitap-max-errors 0");
            exec("demultiplex -f " + filterOptions + " --output-buffer-size " + (rg.nextInt(1 << 17) + 100)
                    + " " + inputFile + " --demultiplex-log " + LOG_FILE);
            File[] outputFiles = getOutputFiles();
            int previousNumberOfFiles = outputFiles.length;
            Arrays.stream(outputFiles).map(File::delete).forEach(Assert::assertTrue);
            exec("demultiplex -f " + filterOptions + " --output-buffer-size " + (rg.nextInt(1 << 17) + 100)
                    + " " + inputFile + " --demultiplex-log " + LOG_FILE);
            outputFiles = getOutputFiles();
            assertEquals(previousNumberOfFiles, outputFiles.length);
            Arrays.stream(outputFiles).map(File::delete).forEach(Assert::assertTrue);
        }
        for (String fileName : new String[] { startFile, inputFile, LOG_FILE })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String startFile = getExampleMif("twosided");
        String inputFile = TEMP_DIR + TEST_FILENAME_PREFIX + ".mif";
        String sampleFile1 = EXAMPLES_PATH + "demultiplex_samples/sample1.txt";
        String sampleFile2 = EXAMPLES_PATH + "demultiplex_samples/sample2.txt";
        String sampleFile3 = EXAMPLES_PATH + "demultiplex_samples/sample3.txt";
        String sampleFileBad = EXAMPLES_PATH + "demultiplex_samples/bad_sample.txt";

        exec("extract -f --input-format mif --input " + startFile + " --output " + inputFile
                + " --pattern \"(G1:NNN)&(G2:AANA)\\(G3:ntt)&(G4:nnnn)\""
                + " --threads 5 --mismatch-score -9 --gap-score -10 --single-overlap-penalty -10");
        Arrays.stream(getOutputFiles()).map(File::delete).forEach(Assert::assertTrue);

        exec("demultiplex -f " + inputFile + " --by-barcode G1 --by-sample " + sampleFile1
                + " --by-barcode G4 --demultiplex-log " + LOG_FILE);
        File[] outputFiles = getOutputFiles();
        assertEquals(4650, outputFiles.length);
        Arrays.stream(outputFiles).map(File::delete).forEach(Assert::assertTrue);

        exec("demultiplex -f " + inputFile + " --by-sample " + sampleFile2 + " --by-sample " + sampleFile3
                + " --demultiplex-log " + LOG_FILE);
        outputFiles = getOutputFiles();
        assertEquals(17, outputFiles.length);
        Arrays.stream(outputFiles).map(File::delete).forEach(Assert::assertTrue);

        assertOutputContains(true, "Invalid sample", () -> callableExec("demultiplex -f " + inputFile
                + " --by-sample " + sampleFileBad + " --demultiplex-log " + LOG_FILE));
        assertOutputContains(true, "Missing required option", () -> callableExec("demultiplex -f "
                + inputFile + " --by-sample " + sampleFile1));
        for (String fileName : new String[] { startFile, inputFile, LOG_FILE })
            assertTrue(new File(fileName).delete());
    }

    private static File[] getOutputFiles() {
        return new File(TEMP_DIR).listFiles((dummy, name) -> name.startsWith(TEST_FILENAME_PREFIX + '_'));
    }
}
