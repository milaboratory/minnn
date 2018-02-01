package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.MifToFastqIO;

import java.util.*;

public final class MifToFastqAction implements Action {
    private final MifToFastqActionParameters params = new MifToFastqActionParameters();

    @Override
    public void go(ActionHelper helper) {
        MifToFastqIO mifToFastqIO = new MifToFastqIO(params.inputFileName, params.outputFileNames, params.groupNames,
                params.noDefaultGroups, params.copyOldComments);
        mifToFastqIO.go();
    }

    @Override
    public String command() {
        return "mif2fastq";
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription =
            "Convert mif file to fastq format.")
    private static final class MifToFastqActionParameters extends ActionParameters {
        @Parameter(description = "Input file in \"mif\" format. If not specified, stdin will be used.",
                names = {"--input"}, order = 0)
        String inputFileName = null;

        @Parameter(description = "Output files, 1 file for each read. If not specified, stdout will be used. " +
                "Using stdout is valid only with single read. Order of files is the same as order of output groups. " +
                "Output groups order: default groups (R1, R2, R3 etc) first, then groups from --groups argument.",
                names = {"--output"}, order = 1, variableArity = true)
        List<String> outputFileNames = new ArrayList<>();

        @Parameter(description = "Don't use default groups R1, R2, R3 etc as output reads. If this option is " +
                "specified, --groups option is mandatory.",
                names = {"--no-default-groups"}, order = 2)
        boolean noDefaultGroups = false;

        @Parameter(description = "Group names to use as separate extra reads. This option is mandatory if " +
                "--no-default-groups is specified. If one of these groups overrides one of default groups " +
                "R1, R2 etc, default groups will not be used. Output groups order: default groups first, then " +
                "groups from this argument.",
                names = {"--groups"}, order = 3, variableArity = true)
        List<String> groupNames = null;

        @Parameter(description = "Copy original comments from initial fastq files to comments of output " +
                "fastq files.",
                names = {"--copy-old-comments"})
        boolean copyOldComments = false;

        @Override
        public void validate() {
            if (((groupNames == null) || (groupNames.size() == 0)) && noDefaultGroups)
                throw new ParameterException("Groups for output reads are not specified!");
        }
    }
}
