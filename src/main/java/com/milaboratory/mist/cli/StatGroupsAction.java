package com.milaboratory.mist.cli;

import com.beust.jcommander.*;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.StatGroupsIO;

import java.util.*;

import static com.milaboratory.mist.cli.CliUtils.*;

public final class StatGroupsAction implements Action {
    public static final String commandName = "stat-groups";
    private final StatGroupsActionParameters params = new StatGroupsActionParameters();

    @Override
    public void go(ActionHelper helper) {
        StatGroupsIO statGroupsIO = new StatGroupsIO(params.groupList, params.inputFileName, params.outputFileName,
                params.inputReadsLimit, (byte)(params.readQualityFilter), (byte)(params.minQualityFilter),
                (byte)(params.avgQualityFilter), params.minCountFilter, params.minFracFilter);
        statGroupsIO.go();
    }

    @Override
    public String command() {
        return commandName;
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription =
            "Collect summary statistics: capture group sequence and quality table.")
    private static final class StatGroupsActionParameters extends ActionParameters {
        @Parameter(description = "--groups <group_names>", order = 0)
        private String description;

        @Parameter(description = "Space separated list of groups to output, determines the keys by which the output " +
                "table will be aggregated.",
                names = {"--groups"}, order = 1, required = true, variableArity = true)
        List<String> groupList = null;

        @Parameter(description = "Input file in \"mif\" format. If not specified, stdin will be used.",
                names = {"--input"}, order = 2)
        String inputFileName = null;

        @Parameter(description = "Output text file. If not specified, stdout will be used.",
                names = {"--output"}, order = 3)
        String outputFileName = null;

        @Parameter(description = "Filter group values with a min (non-aggregated) quality below a given threshold, " +
                "applied on by-read basis, should be applied prior to any aggregation. 0 value means no threshold.",
                names = {"--read-quality-filter"}, order = 4)
        int readQualityFilter = 0;

        @Parameter(description = "Filter group values based on min aggregated quality. 0 value means no filtering.",
                names = {"--min-quality-filter"}, order = 5)
        int minQualityFilter = 0;

        @Parameter(description = "Filter group values based on average aggregated quality. 0 value means no filtering.",
                names = {"--avg-quality-filter"}, order = 6)
        int avgQualityFilter = 0;

        @Parameter(description = "Filter unique group values represented by less than specified number of reads.",
                names = {"--min-count-filter"}, order = 7)
        int minCountFilter = 0;

        @Parameter(description = "Filter unique group values represented by less than specified fraction of reads.",
                names = {"--min-frac-filter"}, order = 8)
        float minFracFilter = 0;

        @Parameter(description = "Number of reads to take; 0 value means to take the entire input file.",
                names = {"-n", "--number-of-reads"}, order = 9)
        long inputReadsLimit = 0;

        @Override
        public void validate() {
            if (groupList.size() == 0)
                throw new ParameterException("List of output groups is not specified!");
            validateQuality(readQualityFilter);
            validateQuality(minQualityFilter);
            validateQuality(avgQualityFilter);
        }
    }
}
