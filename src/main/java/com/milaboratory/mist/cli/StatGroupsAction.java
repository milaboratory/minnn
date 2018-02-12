package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.StatGroupsIO;

import java.util.List;

public final class StatGroupsAction implements Action {
    private final StatGroupsActionParameters params = new StatGroupsActionParameters();

    @Override
    public void go(ActionHelper helper) {
        StatGroupsIO statGroupsIO = new StatGroupsIO(params.groupList, params.inputFileName, params.outputFileName,
                params.numberOfReads, params.readQualityFilter, params.minQualityFilter, params.avgQualityFilter,
                params.minCountFilter, params.minFracFilter);
        statGroupsIO.go();
    }

    @Override
    public String command() {
        return "stat-groups";
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription =
            "Collect summary statistics: capture group sequence and quality table.")
    private static final class StatGroupsActionParameters extends ActionParameters {
        @Parameter(description = "--group-list <group_names>")
        private String description;

        @Parameter(description = "List of groups to output, determines the keys by which the output table " +
                "will be aggregated.",
                names = {"--group-list"}, order = 0, required = true, variableArity = true)
        List<String> groupList = null;

        @Parameter(description = "Input file in \"mif\" format. If not specified, stdin will be used.",
                names = {"--input"}, order = 1)
        String inputFileName = null;

        @Parameter(description = "Output text file. If not specified, stdout will be used.",
                names = {"--output"}, order = 2)
        String outputFileName = null;

        @Parameter(description = "Number of reads to take; 0 value means to take the entire input file.",
                names = {"-n", "--number-of-reads"})
        long numberOfReads = 0;

        @Parameter(description = "Filter group values with a min (non-aggregated) quality below a given threshold, " +
                "applied on by-read basis, should be applied prior to any aggregation. 0 value means no threshold.",
                names = {"--read-quality-filter"})
        byte readQualityFilter = 0;

        @Parameter(description = "Filter group values based on min aggregated quality. 0 value means no filtering.",
                names = {"--min-quality-filter"})
        byte minQualityFilter = 0;

        @Parameter(description = "Filter group values based on average aggregated quality. 0 value means no filtering.",
                names = {"--avg-quality-filter"})
        byte avgQualityFilter = 0;

        @Parameter(description = "Filter unique group values represented by less than specified number of reads.",
                names = {"--min-count-filter"})
        int minCountFilter = 0;

        @Parameter(description = "Filter unique group values represented by less than specified fraction of reads.",
                names = {"--min-frac-filter"})
        float minFracFilter = 0;

        @Override
        public void validate() {
            if (groupList == null)
                throw new ParameterException("List of output groups is not specified!");
        }
    }
}
