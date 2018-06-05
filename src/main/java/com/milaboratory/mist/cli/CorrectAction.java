package com.milaboratory.mist.cli;

import com.beust.jcommander.*;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.CorrectBarcodesIO;

import java.util.*;

import static com.milaboratory.mist.cli.Defaults.*;

public final class CorrectAction implements Action {
    private final CorrectActionParameters params = new CorrectActionParameters();

    @Override
    public void go(ActionHelper helper) {
        CorrectBarcodesIO correctBarcodesIO = new CorrectBarcodesIO(params.inputFileName, params.outputFileName,
                params.mismatches, params.indels, params.totalErrors, params.threshold, params.groupNames,
                params.threads);
        correctBarcodesIO.go();
    }

    @Override
    public String command() {
        return "correct";
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription =
            "Correct errors in barcodes, and replace all barcodes with corrected variants.")
    private static final class CorrectActionParameters extends ActionParameters {
        @Parameter(description = "--input <input_mif_file>")
        private String description;

        @Parameter(description = "Group names for correction.",
                names = {"--groups"}, order = 0, required = true, variableArity = true)
        List<String> groupNames = null;

        @Parameter(description = "Input file in \"mif\" format. This argument is required; stdin is not supported.",
                names = {"--input"}, order = 1, required = true)
        String inputFileName = null;

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 2)
        String outputFileName = null;

        @Parameter(description = "Maximum number of mismatches between barcodes for which they are considered " +
                "identical.", names = {"--max-mismatches"}, order = 2)
        int mismatches = DEFAULT_CORRECT_MAX_MISMATCHES;

        @Parameter(description = "Maximum number of insertions or deletions between barcodes for which they are " +
                "considered identical.", names = {"--max-indels"}, order = 3)
        int indels = DEFAULT_CORRECT_MAX_INDELS;

        @Parameter(description = "Maximum Levenshtein distance between barcodes for which they are considered " +
                "identical.", names = {"--max-total-errors"}, order = 4)
        int totalErrors = DEFAULT_CORRECT_MAX_TOTAL_ERRORS;

        @Parameter(description = "Threshold for UMI clustering: if smaller UMI count divided to larger UMI count " +
                "is below this threshold, UMI will be merged to the cluster.",
                names = {"--cluster-threshold"})
        float threshold = DEFAULT_CORRECT_CLUSTER_THRESHOLD;

        @Parameter(description = "Threshold for UMI clustering: if smaller UMI count divided to larger UMI count " +
                "is below this threshold, UMI will be merged to the cluster.",
                names = {"--cluster-threshold"})
        int clusterDepth = DEFAULT_CORRECT_CLUSTER_DEPTH;

        @Parameter(description = "Number of threads for correcting barcodes.",
                names = {"--threads"})
        int threads = DEFAULT_THREADS;
    }
}
