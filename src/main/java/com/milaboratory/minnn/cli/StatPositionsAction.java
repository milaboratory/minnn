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

import com.milaboratory.cli.ACommandWithOutput;
import com.milaboratory.minnn.io.StatPositionsIO;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.StatPositionsAction.STAT_POSITIONS_ACTION_NAME;

@Command(name = STAT_POSITIONS_ACTION_NAME,
        sortOptions = false,
        separator = " ",
        description = "Collect summary statistics: positions of group matches.")
public final class StatPositionsAction extends ACommandWithOutput implements MiNNNCommand {
    public static final String STAT_POSITIONS_ACTION_NAME = "stat-positions";

    public StatPositionsAction() {
        super(APP_NAME);
    }

    @Override
    public void run0() {
        StatPositionsIO statPositionsIO = new StatPositionsIO(groupList, readIdList, outputWithSeq, inputFileName,
                outputFileName, inputReadsLimit, minCountFilter, minFracFilter);
        statPositionsIO.go();
    }

    @Override
    public void validateInfo(String inputFile) {
        MiNNNCommand.super.validateInfo(inputFile);
    }

    @Override
    public void validate() {
        super.validate();
        if (groupList.size() == 0)
            throwValidationException("List of output groups is not specified!");
    }

    @Override
    protected List<String> getInputFiles() {
        List<String> inputFileNames = new ArrayList<>();
        if (inputFileName != null)
            inputFileNames.add(inputFileName);
        return inputFileNames;
    }

    @Override
    protected List<String> getOutputFiles() {
        List<String> outputFileNames = new ArrayList<>();
        if (outputFileName != null)
            outputFileNames.add(outputFileName);
        return outputFileNames;
    }

    @Option(description = "Space separated list of groups to output, determines IDs allowed in group.id column.",
            names = {"--groups"},
            required = true,
            arity = "1..*")
    private List<String> groupList = null;

    @Option(description = "Space separated list of original read IDs to output (R1, R2 etc), determines IDs " +
            "allowed in read column. If not specified, all reads will be used.",
            names = {"--reads"},
            arity = "1..*")
    private List<String> readIdList = null;

    @Option(description = "Also output matched sequences. If specified, key columns are group.id + read " +
            "+ seq + pos; if not specified, key columns are group.id + read + pos.",
            names = {"--output-with-seq"})
    private boolean outputWithSeq = false;

    @Option(description = IN_FILE_OR_STDIN,
            names = {"--input"})
    private String inputFileName = null;

    @Option(description = OUT_TEXT_FILE,
            names = {"--output"})
    private String outputFileName = null;

    @Option(description = MIN_COUNT_FILTER,
            names = {"--min-count-filter"})
    private int minCountFilter = 0;

    @Option(description = MIN_FRAC_FILTER,
            names = {"--min-frac-filter"})
    private float minFracFilter = 0;

    @Option(description = NUMBER_OF_READS,
            names = {"-n", "--number-of-reads"})
    private long inputReadsLimit = 0;
}
