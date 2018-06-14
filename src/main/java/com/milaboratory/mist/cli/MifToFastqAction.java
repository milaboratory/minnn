package com.milaboratory.mist.cli;

import com.beust.jcommander.*;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.MifToFastqIO;

import java.util.*;

public final class MifToFastqAction implements Action {
    public static final String commandName = "mif2fastq";
    private final MifToFastqActionParameters params = new MifToFastqActionParameters();

    @Override
    public void go(ActionHelper helper) {
        MifToFastqIO mifToFastqIO = new MifToFastqIO(params.inputFileName, parseGroups(params.groupsQuery),
                params.copyOriginalHeaders, params.inputReadsLimit);
        mifToFastqIO.go();
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
            "Convert mif file to fastq format.")
    private static final class MifToFastqActionParameters extends ActionParameters {
        @Parameter(description = "group_options\n        Group Options:          Groups and their file names for " +
                "output reads. At least 1 group must be specified. Built-in groups R1, R2, R3... used for input " +
                "reads. Example: --group-R1 out_R1.fastq --group-R2 out_R2.fastq --group-UMI UMI.fastq",
                order = 0, required = true, variableArity = true)
        List<String> groupsQuery = new ArrayList<>();

        @Parameter(description = "Input file in \"mif\" format. If not specified, stdin will be used.",
                names = {"--input"}, order = 1)
        String inputFileName = null;

        @Parameter(description = "Copy original comments from initial fastq files to comments of output " +
                "fastq files.",
                names = {"--copy-original-headers"}, order = 2)
        boolean copyOriginalHeaders = false;

        @Parameter(description = "Number of reads to take; 0 value means to take the entire input file.",
                names = {"-n", "--number-of-reads"}, order = 3)
        long inputReadsLimit = 0;
    }

    private static LinkedHashMap<String, String> parseGroups(List<String> groupsQuery) throws ParameterException {
        if (groupsQuery.size() % 2 != 0)
            throw new ParameterException("Group parameters not parsed, expected pairs of groups and their file names: "
                    + groupsQuery);
        LinkedHashMap<String, String> groups = new LinkedHashMap<>();
        for (int i = 0; i < groupsQuery.size(); i += 2) {
            String currentGroup = groupsQuery.get(i);
            String currentFileName = groupsQuery.get(i + 1);
            if ((currentGroup.length() < 9) || !currentGroup.substring(0, 8).equals("--group-"))
                throw new ParameterException("Syntax error in group parameter: " + currentGroup);
            groups.put(currentGroup.substring(8), currentFileName);
        }
        return groups;
    }
}
