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
import static com.milaboratory.minnn.util.SystemUtils.*;
import static org.junit.Assert.*;

public class FilterActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "filterStart.mif";
        String inputFile = TEMP_DIR + "filterInput.mif";
        String outputFile1 = TEMP_DIR + "filterOutput1.mif";
        String outputFile2 = TEMP_DIR + "filterOutput2.mif";
        String outputFile3 = TEMP_DIR + "filterOutput3.mif";
        for (int i = 0; i < 50; i++) {
            String filter = getRandomFilter();
            String fairSorting = rg.nextBoolean() ? "" : " --fair-sorting";
            createRandomMifFile(startFile);
            exec("extract -f --input-format MIF --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:tnacn)(G2:ncnc)\" --bitap-max-errors 0");
            exec("filter -f --input " + inputFile + " --output " + outputFile1 + fairSorting
                    + " --threads " + (rg.nextInt(5) + 1) + filter);
            exec("filter -f --input " + outputFile1 + " --output " + outputFile2 + fairSorting
                    + " --threads " + (rg.nextInt(5) + 1) + filter);
            exec("filter -f --input " + outputFile2 + " --output " + outputFile3 + fairSorting
                    + " --threads " + (rg.nextInt(5) + 1) + filter);
            assertFileNotEquals(outputFile2, outputFile3);
            assertMifEqualsAsFastq(outputFile2, outputFile3, false);
        }
        for (String fileName : new String[] { startFile, inputFile, outputFile1, outputFile2, outputFile3 })
            assertTrue(new File(fileName).delete());
    }

    private String getRandomFilter() {
        String[] filters = {
                "Len(G1) = 4", "Len(G2)=3", "Len(*) =8", "Len(G1) = 5 & Len(G2)=3", "Len(G1)=6|Len(G2)=4",
                "Len(G1)=5|Len(G1)=4&Len(G2)=3|Len(G1)=6", "Len(G1)=5|(Len(G1)=4&Len(G2)=3|Len(G1)=6)",
                "G1~'TAACT' & Len(G2)=3", "G2~'T&nc'", "G1~'CT'&G1~'ta+[a&t]'", "G1~'~ta'",
                "G2~'^TC||[n{2}]$'|(G1~'<{2}Taac'&G2~'*'| Len(G2)=5)", "G2~'AT && ~^GC'",
                "MinConsensusReads=0", "MinConsensusReads = 5 & Len(G1) = 4", "Len( * )=4", "Len(*)=5",
                "MinGroupQuality(*)=5 & AvgGroupQuality(G1)=10", "GroupMaxNCount(G2)=0", "GroupMaxNFraction(*)=0.01"
        };
        return " \"" + filters[rg.nextInt(filters.length)] + "\"";
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String outputFile1 = TEMP_DIR + "filterOutput1.mif";
        String outputFile2 = TEMP_DIR + "filterOutput2.mif";
        String outputFile3 = TEMP_DIR + "filterOutput3.mif";
        String filter = " \"G1~'AT'|G2~'GGC'&R2~'AANC&TA'|Len(G4)=5\"";
        exec("filter -f --fair-sorting --input " + inputFile + " --output " + outputFile1 + filter);
        for (String fairSorting : new String[] { "", " --fair-sorting" })
            assertOutputContains(true, "100", () -> callableExec("filter -f" + fairSorting
                    + " --input " + inputFile + " --output " + outputFile1 + filter));
        exec("filter -f --fair-sorting --input " + outputFile1 + " --output " + outputFile2 + filter);
        exec("filter -f --fair-sorting --input " + outputFile2 + " --output " + outputFile3 + filter);
        assertFileNotEquals(outputFile2, outputFile3);
        assertMifEqualsAsFastq(outputFile2, outputFile3, true);
        assertOutputContains(true, "must be for single read", () -> callableExec("filter -f --input "
                + inputFile + " G1~'A\\A'"));
        for (String fileName : new String[] { inputFile, outputFile1, outputFile2, outputFile3 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void groupFiltersTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String outputFile1 = TEMP_DIR + "filterOutput1.mif";
        String outputFile2 = TEMP_DIR + "filterOutput2.mif";
        String outputFile3 = TEMP_DIR + "filterOutput3.mif";
        String outputFile4 = TEMP_DIR + "filterOutput4.mif";
        String outputFile5 = TEMP_DIR + "filterOutput5.mif";
        exec("filter -f --input " + inputFile + " --output " + outputFile1
                + " \"GroupMaxNCount(G3)=0\"");
        assertMifNotEqualsAsFastq(inputFile, outputFile1, true);
        exec("filter -f --input " + inputFile + " --output " + outputFile2
                + " \"GroupMaxNFraction(*)=0.2\"");
        assertMifNotEqualsAsFastq(inputFile, outputFile2, true);
        exec("filter -f --input " + inputFile + " --output " + outputFile3
                + " \"AvgGroupQuality(*)=7\"");
        assertMifNotEqualsAsFastq(inputFile, outputFile3, true);
        exec("filter -f --input " + outputFile3 + " --output " + outputFile4
                + " \"MinGroupQuality(*)=7\"");
        assertMifNotEqualsAsFastq(outputFile3, outputFile4, true);
        exec("filter -f --input " + outputFile4 + " --output " + outputFile5
                + " \"AvgGroupQuality(*)=7\"");
        assertMifEqualsAsFastq(outputFile4, outputFile5, true);
        assertTrue(new File(inputFile).delete());
        for (int i = 1; i <= 5; i++)
            assertTrue(new File(TEMP_DIR + "filterOutput" + i + ".mif").delete());
    }

    @Test
    public void barcodeWhitelistTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String outputFile1 = TEMP_DIR + "filterOutput1.mif";
        String outputFile2 = TEMP_DIR + "filterOutput2.mif";
        String whitelist1 = EXAMPLES_PATH + "filter_whitelists/whitelist-1.txt";
        String whitelistPatterns1 = EXAMPLES_PATH + "filter_whitelists/whitelist-patterns-1.txt";
        String whitelistPatterns2 = EXAMPLES_PATH + "filter_whitelists/whitelist-patterns-2.txt";
        assertOutputContains(true, "23092", () -> callableExec("filter -f --input " + inputFile
                + " --output " + outputFile1 + " --whitelist G1=" + whitelist1));
        for (String fairSorting : new String[] { "", " --fair-sorting" }) {
            assertOutputContains(true, "276", () -> callableExec("filter -f" + fairSorting
                    + " --input " + inputFile + " --output " + outputFile1
                    + " --whitelist-patterns G1=" + whitelistPatterns1));
            assertOutputContains(true, "matched 1 reads", () -> callableExec("filter -f"
                    + fairSorting + " --input " + inputFile + " --output " + outputFile2
                    + " --whitelist-patterns R2=" + whitelistPatterns2
                    + " --whitelist-patterns G1=" + whitelistPatterns1 + " \"G2~'nnc'|Len(G4)=5\""));
        }

        try {
            exec("filter -f --input " + inputFile + " --output " + outputFile1);
            fail();
        } catch (RuntimeException ignored) {}

        for (String fileName : new String[] { inputFile, outputFile1, outputFile2 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void emptyReadsTest() throws Exception {
        String inputFile = getExampleMif("with-empty-reads");
        String filtered1 = TEMP_DIR + "filtered1.mif";
        String filtered2 = TEMP_DIR + "filtered2.mif";
        exec("filter -f --input " + inputFile + " --output " + filtered1
                + " \"AvgGroupQuality(*)=20\"");
        exec("filter -f --input " + inputFile + " --output " + filtered2
                + " \"GroupMaxNFraction(*)=0.15\"");
        for (String fileName : new String[] { inputFile, filtered1, filtered2 })
            assertTrue(new File(fileName).delete());
    }
}
