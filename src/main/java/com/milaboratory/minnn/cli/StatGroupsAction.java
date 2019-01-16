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
import com.milaboratory.minnn.io.StatGroupsIO;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.StatGroupsAction.STAT_GROUPS_ACTION_NAME;

@Command(name = STAT_GROUPS_ACTION_NAME,
        sortOptions = false,
        showDefaultValues = true,
        separator = " ",
        description = "Collect summary statistics: capture group sequence and quality table.")
public final class StatGroupsAction extends ACommandWithOutput implements MiNNNCommand {
    public static final String STAT_GROUPS_ACTION_NAME = "stat-groups";

    public StatGroupsAction() {
        super(APP_NAME);
    }

    @Override
    public void run0() {
        StatGroupsIO statGroupsIO = new StatGroupsIO(groupList, inputFileName, outputFileName, inputReadsLimit,
                readQualityFilter, minQualityFilter, avgQualityFilter, minCountFilter, minFracFilter);
        statGroupsIO.go();
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
        validateQuality(readQualityFilter, spec.commandLine());
        validateQuality(minQualityFilter, spec.commandLine());
        validateQuality(avgQualityFilter, spec.commandLine());
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

    @Option(description = "Space separated list of groups to output, determines the keys by which the output " +
            "table will be aggregated.",
            names = {"--groups"},
            required = true,
            arity = "1..*")
    private List<String> groupList = null;

    @Option(description = IN_FILE_OR_STDIN,
            names = {"--input"})
    private String inputFileName = null;

    @Option(description = OUT_TEXT_FILE,
            names = {"--output"})
    private String outputFileName = null;

    @Option(description = "Filter group values with a min (non-aggregated) quality below a given threshold, " +
            "applied on by-read basis, should be applied prior to any aggregation. 0 value means no threshold.",
            names = {"--read-quality-filter"})
    private byte readQualityFilter = 0;

    @Option(description = "Filter group values based on min aggregated quality. 0 value means no filtering.",
            names = {"--min-quality-filter"})
    private byte minQualityFilter = 0;

    @Option(description = "Filter group values based on average aggregated quality. 0 value means no filtering.",
            names = {"--avg-quality-filter"})
    private byte avgQualityFilter = 0;

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
