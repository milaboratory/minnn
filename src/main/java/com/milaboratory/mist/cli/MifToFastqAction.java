package com.milaboratory.mist.cli;

import com.beust.jcommander.*;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.MifToFastqIO;

import java.util.*;

public final class MifToFastqAction implements Action {
    private final MifToFastqActionParameters params = new MifToFastqActionParameters();

    @Override
    public void go(ActionHelper helper) {
        MifToFastqIO mifToFastqIO = new MifToFastqIO(params.inputFileName, params.groups, params.copyOldComments);
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

        @DynamicParameter(description = "Groups and their file names for output reads. At least 1 group must be " +
                "specified. Built-in groups R1, R2, R3... used for input reads. Example: --groupR1 out_R1.fastq " +
                "--groupR2 out_R2.fastq --groupUMI UMI.fastq",
                names = {"--group"})
        LinkedHashMap<String, String> groups = new LinkedHashMap<>();

        @Parameter(description = "Copy original comments from initial fastq files to comments of output " +
                "fastq files.",
                names = {"--copy-old-comments"})
        boolean copyOldComments = false;

        @Override
        public void validate() {
            if (groups.size() == 0)
                throw new ParameterException("Groups for output reads are not specified!");
        }
    }
}
