package com.milaboratory.mist.cli;

import com.beust.jcommander.*;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.SorterIO;

import java.util.*;

public final class SortAction implements Action {
    public static final String commandName = "sort";
    private final SortActionParameters params = new SortActionParameters();

    @Override
    public void go(ActionHelper helper) {
        SorterIO sorterIO = new SorterIO(params.inputFileName, params.outputFileName, params.sortGroupNames,
                params.chunkSize, params.suppressWarnings, params.tmpFile);
        sorterIO.go();
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
            "Sort reads by contents (nucleotide sequences) of specified groups.")
    private static final class SortActionParameters extends ActionParameters {
        @Parameter(description = "--groups <group_names>", order = 0)
        private String description;

        @Parameter(description = "Group names to use for sorting. Priority is in descending order.",
                names = {"--groups"}, order = 1, required = true, variableArity = true)
        List<String> sortGroupNames = null;

        @Parameter(description = "Input file in \"mif\" format. If not specified, stdin will be used.",
                names = {"--input"}, order = 2)
        String inputFileName = null;

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 3)
        String outputFileName = null;

        @Parameter(description = "Chunk size for sorter.",
                names = {"--chunk-size"}, order = 4)
        int chunkSize = -1;

        @Parameter(description = "Don't display any warnings.",
                names = {"--suppress-warnings"}, order = 5)
        boolean suppressWarnings = false;

        @Parameter(description = "Custom temp file, used for debugging purposes.",
                names = {"--temp-file"}, hidden = true)
        String tmpFile = null;

        @Override
        public void validate() {
            if (sortGroupNames.size() == 0)
                throw new ParameterException("Sorting groups are not specified!");
        }
    }
}
