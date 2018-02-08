package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.SorterIO;

import java.util.List;

public final class SortAction implements Action {
    private final SortActionParameters params = new SortActionParameters();

    @Override
    public void go(ActionHelper helper) {
        SorterIO sorterIO = new SorterIO(params.inputFileName, params.outputFileName, params.sortGroupNames,
                params.chunkSize);
        sorterIO.go();
    }

    @Override
    public String command() {
        return "sort";
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription =
            "Sort reads by contents (nucleotide sequences) of specified groups.")
    private static final class SortActionParameters extends ActionParameters {
        @Parameter(description = "--groups <group_names>")
        private String description;

        @Parameter(description = "Group names to use for sorting. Priority is in descending order.",
                names = {"--groups"}, order = 0, required = true, variableArity = true)
        List<String> sortGroupNames = null;

        @Parameter(description = "Input file in \"mif\" format. If not specified, stdin will be used.",
                names = {"--input"}, order = 1)
        String inputFileName = null;

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 2)
        String outputFileName = null;

        @Parameter(description = "Chunk size for sorter.",
                names = {"--chunk-size"})
        int chunkSize = -1;

        @Override
        public void validate() {
            if (sortGroupNames == null)
                throw new ParameterException("Sorting groups are not specified!");
        }
    }
}
