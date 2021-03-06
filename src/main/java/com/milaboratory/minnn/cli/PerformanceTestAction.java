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

import com.milaboratory.cli.ACommand;
import com.milaboratory.minnn.io.PerformanceTestIO;
import picocli.CommandLine.*;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.PerformanceTestAction.PERFORMANCE_TEST_ACTION_NAME;

@Command(name = PERFORMANCE_TEST_ACTION_NAME,
        sortOptions = false,
        showDefaultValues = true,
        separator = " ",
        description = "Test I/O performance. Development use only.",
        hidden = true)
public final class PerformanceTestAction extends ACommand {
    public static final String PERFORMANCE_TEST_ACTION_NAME = "performance-test";

    public PerformanceTestAction() {
        super(APP_NAME);
    }

    @Override
    public void validateInfo(String inputFile) {}

    @Override
    public void run0() {
        PerformanceTestIO performanceTestIO = new PerformanceTestIO(inputFileName, outputFileName);
        performanceTestIO.go();
    }

    @Option(description = "Input file name.",
            names = {"--input"},
            required = true)
    private String inputFileName = null;

    @Option(description = "Output file name.",
            names = {"--output"},
            required = true)
    private String outputFileName = null;
}
