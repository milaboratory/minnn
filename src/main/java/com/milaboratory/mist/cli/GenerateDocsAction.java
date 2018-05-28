package com.milaboratory.mist.cli;

import com.beust.jcommander.*;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.GenerateDocsIO;

public final class GenerateDocsAction implements Action {
    private final GenerateDocsActionParameters params = new GenerateDocsActionParameters();

    @Override
    public void go(ActionHelper helper) {
        GenerateDocsIO generateDocsIO = new GenerateDocsIO(params.outputFileName);
        generateDocsIO.go();
    }

    @Override
    public String command() {
        return "docs";
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription = "Generate docs for all commands. Development use only.", hidden = true)
    private static final class GenerateDocsActionParameters extends ActionParameters {
        @Parameter(description = "Output .rst file.",
                names = {"--output"}, required = true)
        String outputFileName = null;
    }
}
