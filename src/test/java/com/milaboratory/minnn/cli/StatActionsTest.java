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

import java.io.*;
import java.util.*;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static org.junit.Assert.*;

public class StatActionsTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "statStart.mif";
        String inputFile = TEMP_DIR + "statInput.mif";
        String outputG1 = TEMP_DIR + "statGroups1.txt";
        String outputG2 = TEMP_DIR + "statGroups2.txt";
        String outputP1 = TEMP_DIR + "statPositions1.txt";
        String outputP2 = TEMP_DIR + "statPositions2.txt";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            exec("extract -f --input-format MIF --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:accnt) & (G2:nctn) & (G3:atta)\" --bitap-max-errors 2 --score-threshold -80");

            int qualityFilter1 = rg.nextInt(DEFAULT_MAX_QUALITY);
            int qualityFilter2 = rg.nextInt(DEFAULT_MAX_QUALITY);
            for (String filter : new String[] {
                    " --min-quality-filter ", " --avg-quality-filter ", " --read-quality-filter " }) {
                exec("stat-groups -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputG1
                        + filter + qualityFilter1);
                exec("stat-groups -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputG2
                        + filter + qualityFilter2);
                assertRestrictionsAndSizes(Integer.compare(qualityFilter1, qualityFilter2),
                        Long.compare(getFileSize(outputG1), getFileSize(outputG2)));
            }

            int minCountFilter = rg.nextInt(20);
            exec("stat-groups -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputG1
                    + " --min-count-filter " + minCountFilter);
            exec("stat-positions -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputP1
                    + " --min-count-filter " + minCountFilter);
            Scanner[] scanners = new Scanner[] { new Scanner(new File(outputG1)), new Scanner(new File(outputP1)) };
            Arrays.stream(scanners).forEach(scanner -> {
                scanner.useDelimiter(System.getProperty("line.separator"));
                scanner.next();
                scanner.forEachRemaining(l -> assertTrue(parseCount(l) >= minCountFilter));
                scanner.close();
            });

            float fracFilter1 = rg.nextFloat();
            float fracFilter2 = rg.nextFloat();
            exec("stat-groups -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputG1
                    + " --min-frac-filter " + fracFilter1);
            exec("stat-groups -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputG2
                    + " --min-frac-filter " + fracFilter2);
            exec("stat-positions -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputP1
                    + " --min-frac-filter " + fracFilter1);
            exec("stat-positions -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputP2
                    + " --min-frac-filter " + fracFilter2);
            assertRestrictionsAndSizes(Float.compare(fracFilter1, fracFilter2),
                    Long.compare(getFileSize(outputG1), getFileSize(outputG2)));
            assertRestrictionsAndSizes(Float.compare(fracFilter1, fracFilter2),
                    Long.compare(getFileSize(outputP1), getFileSize(outputP2)));

            exec("stat-positions -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputP1);
            exec("stat-positions -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputP2
                    + " --reads R1");
            assertFileEquals(outputP1, outputP2);
            exec("stat-positions -f --groups G1 G3 --input " + inputFile + " --output " + outputP2);
            assertTrue(countLinesInFile(outputP1) >= countLinesInFile(outputP2));

            exec("stat-positions -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputP1
                    + " --reads R2");
            assertEquals(1, countLinesInFile(outputP1));

            exec("stat-positions -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputP1);
            exec("stat-positions -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputP2
                    + " --output-with-seq");
            assertTrue(countLinesInFile(outputP1) <= countLinesInFile(outputP2));
        }
        for (String fileName : new String[] { startFile, inputFile, outputG1, outputG2, outputP1, outputP2 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String outputG1 = TEMP_DIR + "statGroups1.txt";
        String outputG2 = TEMP_DIR + "statGroups2.txt";
        String outputP1 = TEMP_DIR + "statPositions1.txt";
        String outputP2 = TEMP_DIR + "statPositions2.txt";

        int qualityFilter1 = 10;
        int qualityFilter2 = 25;
        for (String filter : new String[] {
                " --min-quality-filter ", " --avg-quality-filter ", " --read-quality-filter " }) {
            exec("stat-groups -f --groups G1 G2 G3 G4 --input " + inputFile + " --output " + outputG1
                    + filter + qualityFilter1);
            exec("stat-groups -f --groups G1 G2 G3 G4 --input " + inputFile + " --output " + outputG2
                    + filter + qualityFilter2);
            assertTrue(getFileSize(outputG1) > getFileSize(outputG2));
        }

        int minCountFilter = 100;
        exec("stat-groups -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputG1
                + " --min-count-filter " + minCountFilter);
        exec("stat-positions -f --groups G1 G2 G3 --input " + inputFile + " --output " + outputP1
                + " --min-count-filter " + minCountFilter);
        Scanner[] scanners = new Scanner[] { new Scanner(new File(outputG1)), new Scanner(new File(outputP1)) };
        Arrays.stream(scanners).forEach(scanner -> {
            scanner.useDelimiter(System.getProperty("line.separator"));
            scanner.next();
            scanner.forEachRemaining(l -> assertTrue(parseCount(l) >= minCountFilter));
            scanner.close();
        });

        float fracFilter1 = 0.005f;
        float fracFilter2 = 0.01f;
        exec("stat-groups -f --groups G1 G2 G3 G4 --input " + inputFile + " --output " + outputG1
                + " --min-frac-filter " + fracFilter1);
        exec("stat-groups -f --groups G1 G2 G3 G4 --input " + inputFile + " --output " + outputG2
                + " --min-frac-filter " + fracFilter2);
        exec("stat-positions -f --groups G1 G2 G3 G4 --input " + inputFile + " --output " + outputP1
                + " --min-frac-filter " + fracFilter1);
        exec("stat-positions -f --groups G1 G2 G3 G4 --input " + inputFile + " --output " + outputP2
                + " --min-frac-filter " + fracFilter2);
        assertTrue(getFileSize(outputG1) > getFileSize(outputG2));
        assertTrue(getFileSize(outputP1) > getFileSize(outputP2));

        exec("stat-positions -f --groups G1 G2 G4 --input " + inputFile + " --output " + outputP1);
        exec("stat-positions -f --groups G1 G2 G4 --input " + inputFile + " --output " + outputP2
                + " --reads R1 R2");
        assertFileEquals(outputP1, outputP2);
        exec("stat-positions -f --groups G1 G4 --input " + inputFile + " --output " + outputP2);
        assertTrue(countLinesInFile(outputP1) > countLinesInFile(outputP2));

        exec("stat-positions -f --groups G2 G3 G4 --input " + inputFile + " --output " + outputP1
                + " --reads R3");
        assertEquals(1, countLinesInFile(outputP1));

        exec("stat-positions -f --groups G1 G2 G3 G4 --input " + inputFile + " --output " + outputP1);
        exec("stat-positions -f --groups G1 G2 G3 G4 --input " + inputFile + " --output " + outputP2
                + " --output-with-seq");
        assertTrue(countLinesInFile(outputP1) < countLinesInFile(outputP2));

        for (String fileName : new String[] { inputFile, outputG1, outputG2, outputP1, outputP2 })
            assertTrue(new File(fileName).delete());
    }

    private int parseCount(String line) {
        String[] tokens = line.split(" ");
        return Integer.parseInt(tokens[tokens.length - 2]);
    }

    private void assertRestrictionsAndSizes(int restrictionComparison, int sizeComparison) {
        if (restrictionComparison == -1)
            assertTrue(sizeComparison >= 0);
        else if (restrictionComparison == 1)
            assertTrue(sizeComparison <= 0);
        else
            assertEquals(0, sizeComparison);
    }
}
