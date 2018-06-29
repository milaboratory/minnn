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
                params.maxClusterDepth, params.singleSubstitutionProbability, params.singleIndelProbability,
                params.inputReadsLimit, params.suppressWarnings, params.threads);
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
        @Parameter(description = "--input <input_mif_file>", order = 0)
        private String description;

        @Parameter(description = "Group names for correction.",
                names = {"--groups"}, order = 1, required = true, variableArity = true)
        List<String> groupNames = null;

        @Parameter(description = "Input file in \"mif\" format. This argument is required; stdin is not supported.",
                names = {"--input"}, order = 2, required = true)
        String inputFileName = null;

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 3)
        String outputFileName = null;

        @Parameter(description = "Maximum number of mismatches between barcodes for which they are considered " +
                "identical.", names = {"--max-mismatches"}, order = 4)
        int mismatches = DEFAULT_CORRECT_MAX_MISMATCHES;

        @Parameter(description = "Maximum number of insertions or deletions between barcodes for which they are " +
                "considered identical.", names = {"--max-indels"}, order = 5)
        int indels = DEFAULT_CORRECT_MAX_INDELS;

        @Parameter(description = "Maximum Levenshtein distance between barcodes for which they are considered " +
                "identical.", names = {"--max-total-errors"}, order = 6)
        int totalErrors = DEFAULT_CORRECT_MAX_TOTAL_ERRORS;

        @Parameter(description = "Threshold for UMI clustering: if smaller UMI count divided to larger UMI count " +
                "is below this threshold, UMI will be merged to the cluster.",
                names = {"--cluster-threshold"}, order = 7)
        float threshold = DEFAULT_CORRECT_CLUSTER_THRESHOLD;

        @Parameter(description = "Maximum cluster depth for algorithm of similar barcodes clustering.",
                names = {"--max-cluster-depth"}, order = 8)
        int maxClusterDepth = DEFAULT_CORRECT_MAX_CLUSTER_DEPTH;

        @Parameter(description = "Single substitution probability for clustering algorithm.",
                names = {"--single-substitution-probability"}, order = 9)
        float singleSubstitutionProbability = DEFAULT_CORRECT_SINGLE_SUBSTITUTION_PROBABILITY;

        @Parameter(description = "Single insertion/deletion probability for clustering algorithm.",
                names = {"--single-indel-probability"}, order = 10)
        float singleIndelProbability = DEFAULT_CORRECT_SINGLE_INDEL_PROBABILITY;

        @Parameter(description = "Number of reads to take; 0 value means to take the entire input file.",
                names = {"-n", "--number-of-reads"}, order = 11)
        long inputReadsLimit = 0;

        @Parameter(description = "Don't display any warnings.",
                names = {"--suppress-warnings"}, order = 12)
        boolean suppressWarnings = false;

        @Parameter(description = "Number of threads for correcting barcodes.",
                names = {"--threads"}, order = 13)
        int threads = DEFAULT_THREADS;
    }
}
