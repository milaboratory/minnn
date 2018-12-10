package com.milaboratory.minnn.cli;

import com.milaboratory.cli.ACommand;
import com.milaboratory.minnn.io.GenerateDocsIO;
import picocli.CommandLine.*;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.GenerateDocsAction.GENERATE_DOCS_ACTION_NAME;

@Command(name = GENERATE_DOCS_ACTION_NAME,
        sortOptions = false,
        separator = " ",
        description = "Generate docs for all commands. Development use only.",
        hidden = true)
public final class GenerateDocsAction extends ACommand {
    public static final String GENERATE_DOCS_ACTION_NAME = "docs";

    public GenerateDocsAction() {
        super(APP_NAME);
    }

    @Override
    public void run0() {
        GenerateDocsIO generateDocsIO = new GenerateDocsIO(outputFileName);
        generateDocsIO.go();
    }

    @Override
    public void validateInfo(String inputFile) {}

    @Option(description = "Output .rst file.",
            names = {"--output"},
            required = true)
    private String outputFileName = null;
}
