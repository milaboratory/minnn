package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.CorrectBarcodesIO;

public final class CorrectAction implements Action {
    private final CorrectActionParameters params = new CorrectActionParameters();

    @Override
    public void go(ActionHelper helper) {
        CorrectBarcodesIO correctBarcodesIO = new CorrectBarcodesIO(params.inputFileName, params.outputFileName);
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
    }
}
