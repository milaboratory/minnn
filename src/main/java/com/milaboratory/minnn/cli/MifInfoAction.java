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

import com.milaboratory.cli.ACommandWithOutput;
import com.milaboratory.minnn.io.MifInfoIO;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.MifInfoAction.MIF_INFO_ACTION_NAME;

@Command(name = MIF_INFO_ACTION_NAME,
        sortOptions = false,
        showDefaultValues = true,
        separator = " ",
        description = "Show information about MIF file: number of reads, group names, corrected and sorted groups.")
public final class MifInfoAction extends ACommandWithOutput implements MiNNNCommand {
    public static final String MIF_INFO_ACTION_NAME = "mif-info";

    public MifInfoAction() {
        super(APP_NAME);
    }

    @Override
    public void run0() {
        MifInfoIO mifInfoIO = new MifInfoIO(Objects.requireNonNull(inputFileName), noReadsCount,
                reportFileName, jsonReportFileName);
        mifInfoIO.go();
    }

    @Override
    public void validateInfo(String inputFile) {
        MiNNNCommand.super.validateInfo(inputFile);
    }

    @Override
    protected List<String> getInputFiles() {
        return Collections.singletonList(inputFileName);
    }

    @Parameters(arity = "1",
            description = "File in MIF format.")
    private String inputFileName = null;

    @Option(description = "Don't count reads, display only info from header.",
            names = {"-q", "--quick", "--no-reads-count"})
    private boolean noReadsCount = false;

    @Option(description = REPORT,
            names = "--report")
    private String reportFileName = null;

    @Option(description = JSON_REPORT,
            names = "--json-report")
    private String jsonReportFileName = null;
}
