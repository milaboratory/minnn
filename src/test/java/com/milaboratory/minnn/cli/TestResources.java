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

import java.util.*;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;

public class TestResources {
    public static final String TEST_RESOURCES_PATH = "src/test/resources/";
    public static final String EXAMPLES_PATH = "examples/";
    private static final HashMap<String, ExampleMif> examples = new HashMap<>();
    static {
        examples.put("twosided", new ExampleMif(EXAMPLES_PATH + "twosided/p109_R1.fastq.gz "
                + EXAMPLES_PATH + "twosided/p109_R2.fastq.gz", TEMP_DIR + "twosided.mif"));
        examples.put("twosided-raw", examples.get("twosided"));
        examples.put("singleReadWithG1-G3", new ExampleMif(EXAMPLES_PATH + "small/100.fastq",
                TEMP_DIR + "singleReadWithG1-G3.mif"));
        examples.put("100reads", new ExampleMif(EXAMPLES_PATH + "small/100.fastq",
                TEMP_DIR + "100reads.mif"));
        examples.put("good-quality", new ExampleMif(EXAMPLES_PATH + "small/good-quality.fastq",
                TEMP_DIR + "good-quality.mif"));
    }

    public static String getExampleMif(String example) throws Exception {
        String common = "extract --input " + examples.get(example).fastq + " --output " + examples.get(example).mif
                + " --mismatch-score -9 --gap-score -10 --single-overlap-penalty -10 --pattern ";
        switch (example) {
            case "twosided":
                exec(common + "\"(G1:tctcag)&(G2:ana)(G3:ggnnc)(G4:NN)\\*\" --score-threshold -20");
                break;
            case "twosided-raw":
                exec(common + "\"*\\*\"");
                break;
            case "singleReadWithG1-G3":
                exec(common + "\"(G1:acNNT)&(G2:NCNC)&(G3:ANca)\"");
                break;
            case "100reads":
                exec(common + "\"*\"");
                break;
            case "good-quality":
                exec(common + "\"(G1:CCCGCCC)\"");
                break;
            default:
                throw new IllegalArgumentException("Unknown example: " + example);
        }
        return examples.get(example).mif;
    }

    private static class ExampleMif {
        final String fastq;
        final String mif;

        ExampleMif(String fastq, String mif) {
            this.fastq = fastq;
            this.mif = mif;
        }
    }
}
