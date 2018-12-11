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

import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.SinglePattern;

import java.io.File;

import static com.milaboratory.minnn.cli.Main.main;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class CommandLineTestUtils {
    public static void exec(String cmdLine) {
        ParsedRead.clearStaticCache();
        main(cmdLine.split("[ ]+(?=([^\"]*\"[^\"]*\")*[^\"]*$)"));
    }

    public static Void callableExec(String cmdLine) {
        try {
            exec(cmdLine);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void createRandomMifFile(String fileName) {
        String fastqFile = EXAMPLES_PATH + "small/100.fastq";
        SinglePattern randomPattern = getRandomSinglePattern();
        exec("extract -f --input " + fastqFile + " --output " + fileName + " --devel-parser-syntax"
                + " --pattern \"" + randomPattern.toString() + "\"");
    }

    public static void assertMifEqualsAsFastq(String mif1, String mif2, boolean withR2) throws Exception {
        checkFastqEquality(mif1, mif2, withR2, true);
    }

    public static void assertMifNotEqualsAsFastq(String mif1, String mif2, boolean withR2) throws Exception {
        checkFastqEquality(mif1, mif2, withR2, false);
    }

    private static void checkFastqEquality(String mif1, String mif2, boolean withR2, boolean equals) throws Exception {
        String fastq1R1 = TEMP_DIR + "1r1.fastq";
        String fastq1R2 = TEMP_DIR + "1r2.fastq";
        String fastq2R1 = TEMP_DIR + "2r1.fastq";
        String fastq2R2 = TEMP_DIR + "2r2.fastq";
        if (withR2) {
            exec("mif2fastq -f --input " + mif1 + " --group R1=" + fastq1R1 + " --group R2=" + fastq1R2);
            exec("mif2fastq -f --input " + mif2 + " --group R1=" + fastq2R1 + " --group R2=" + fastq2R2);
            if (equals) {
                assertFileEquals(fastq1R1, fastq2R1);
                assertFileEquals(fastq1R2, fastq2R2);
            } else {
                assertFileNotEquals(fastq1R1, fastq2R1);
                assertFileNotEquals(fastq1R2, fastq2R2);
            }
        } else {
            exec("mif2fastq -f --input " + mif1 + " --group R1=" + fastq1R1);
            exec("mif2fastq -f --input " + mif2 + " --group R1=" + fastq2R1);
            if (equals)
                assertFileEquals(fastq1R1, fastq2R1);
            else
                assertFileNotEquals(fastq1R1, fastq2R1);
        }
        String[] tempFiles = withR2 ? new String[] { fastq1R1, fastq1R2, fastq2R1, fastq2R2 } : new String[] {
                fastq1R1, fastq2R1 };
        for (String tempFile : tempFiles)
            assertTrue(new File(tempFile).delete());
    }
}
