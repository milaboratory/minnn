package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.ConsensusIO;

import java.util.*;

import static com.milaboratory.mist.cli.Defaults.*;

public final class ConsensusAction implements Action {
    private final ConsensusActionParameters params = new ConsensusActionParameters();

    @Override
    public void go(ActionHelper helper) {
        ConsensusIO consensusIO = new ConsensusIO(params.groupList, params.inputFileName, params.outputFileName,
                params.alignerWidth, params.threads);
        consensusIO.go();
    }

    @Override
    public String command() {
        return "consensus";
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription = "Calculate consensus sequences for all barcodes.")
    private static final class ConsensusActionParameters extends ActionParameters {
        @Parameter(description = "Input file in \"mif\" format. If not specified, stdin will be used.",
                names = {"--input"}, order = 0)
        String inputFileName = null;

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 1)
        String outputFileName = null;

        @Parameter(description = "List of groups that represent barcodes. If not specified, all groups will be used.",
                names = {"--group-list"}, variableArity = true)
        List<String> groupList = null;

        @Parameter(description = "Window width (maximum allowed number of indels) for banded aligner.",
                names = {"--width"})
        int alignerWidth = DEFAULT_CONSENSUS_ALIGNER_WIDTH;

        @Parameter(description = "Number of threads for calculating consensus sequences.",
                names = {"--threads"})
        int threads = DEFAULT_THREADS;
    }
}
