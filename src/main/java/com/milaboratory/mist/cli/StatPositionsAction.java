package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.StatPositionsIO;

import java.util.List;

public final class StatPositionsAction implements Action {
    private final StatPositionsActionParameters params = new StatPositionsActionParameters();

    @Override
    public void go(ActionHelper helper) {
        StatPositionsIO statPositionsIO = new StatPositionsIO(params.groupList, params.readIdList, params.outputWithSeq,
                params.inputFileName, params.numberOfReads, params.minCountFilter, params.minFracFilter);
        statPositionsIO.go();
    }

    @Override
    public String command() {
        return "stat-positions";
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription =
            "Collect summary statistics: positions of group matches.")
    private static final class StatPositionsActionParameters extends ActionParameters {
        @Parameter(description = "--group-list <group_names>")
        private String description;

        @Parameter(description = "List of groups to output, determines IDs allowed in group.id column.",
                names = {"--group-list"}, order = 0, required = true, variableArity = true)
        List<String> groupList = null;

        @Parameter(description = "List of original read IDs to output (R1, R2 etc), determines IDs allowed " +
                "in read column. If not specified, all reads are used.",
                names = {"--read-id"}, order = 1, variableArity = true)
        List<String> readIdList = null;

        @Parameter(description = "Also output matched sequences. If specified, key columns are group.id + read " +
                "+ seq + pos; if not specified, key columns are group.id + read + pos.",
                names = {"--output-with-seq"}, order = 2)
        boolean outputWithSeq = false;

        @Parameter(description = "Input file in \"mif\" format. If not specified, stdin will be used.",
                names = {"--input"}, order = 3)
        String inputFileName = null;

        @Parameter(description = "Number of reads to take; 0 value means to take the entire input file.",
                names = {"-n", "--number-of-reads"})
        int numberOfReads = 0;

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
