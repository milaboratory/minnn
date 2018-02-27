package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.CorrectBarcodesIO;

import static com.milaboratory.mist.cli.Defaults.*;

public final class CorrectAction implements Action {
    private final CorrectActionParameters params = new CorrectActionParameters();

    @Override
    public void go(ActionHelper helper) {
        CorrectBarcodesIO correctBarcodesIO = new CorrectBarcodesIO(params.inputFileName, params.outputFileName,
                params.mismatches, params.deletions, params.insertions, params.totalErrors, params.threads);
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

        @Parameter(description = "Input file in \"mif\" format. This argument is required; stdin is not supported.",
                names = {"--input"}, order = 0, required = true)
        String inputFileName = null;

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 1)
        String outputFileName = null;

        @Parameter(description = "Maximum number of mismatches between barcodes for which they are considered " +
                "identical.", names = {"--max-mismatches"}, order = 2)
        int mismatches = DEFAULT_CORRECT_MAX_MISMATCHES;

        @Parameter(description = "Maximum number of deletions between barcodes for which they are considered " +
                "identical.", names = {"--max-deletions"}, order = 3)
        int deletions = DEFAULT_CORRECT_MAX_DELETIONS;

        @Parameter(description = "Maximum number of insertions between barcodes for which they are considered " +
                "identical.", names = {"--max-insertions"}, order = 4)
        int insertions = DEFAULT_CORRECT_MAX_INSERTIONS;

        @Parameter(description = "Maximum Levenshtein distance between barcodes for which they are considered " +
                "identical.", names = {"--max-total-errors"}, order = 5)
        int totalErrors = DEFAULT_CORRECT_MAX_TOTAL_ERRORS;

        @Parameter(description = "Number of threads for correcting barcodes.",
                names = {"--threads"})
        int threads = DEFAULT_THREADS;
    }
}
