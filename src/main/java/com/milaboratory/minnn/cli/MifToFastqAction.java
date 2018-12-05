/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact MiLaboratory LLC, which owns exclusive
 * rights for distribution of this program for commercial purposes, using the
 * following email address: licensing@milaboratory.com.
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.minnn.cli;

import com.milaboratory.cli.*;
import com.milaboratory.minnn.io.MifToFastqIO;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.MifToFastqAction.MIF_TO_FASTQ_ACTION_NAME;

@Command(name = MIF_TO_FASTQ_ACTION_NAME,
        sortOptions = false,
        separator = " ",
        description = "Convert mif file to fastq format.")
public final class MifToFastqAction extends ACommandWithOutput implements MiNNNCommand {
    public static final String MIF_TO_FASTQ_ACTION_NAME = "mif2fastq";

    public MifToFastqAction() {
        super(APP_NAME);
    }

    @Override
    public void run0() {
        MifToFastqIO mifToFastqIO = new MifToFastqIO(inputFileName, parseGroups(groupsQuery), copyOriginalHeaders,
                inputReadsLimit);
        mifToFastqIO.go();
    }

    @Override
    public void validateInfo(String inputFile) {
        MiNNNCommand.super.validateInfo(inputFile);
    }

    @Override
    public void validate() {
        super.validate();
        if (groupsQuery.size() == 0)
            throwValidationException("Groups for output files (Group Options) are not specified!");
    }

    @Override
    protected List<String> getInputFiles() {
        List<String> inputFileNames = new ArrayList<>();
        if (inputFileName != null)
            inputFileNames.add(inputFileName);
        return inputFileNames;
    }

    @Override
    protected List<String> getOutputFiles() {
        return new ArrayList<>(parseGroups(groupsQuery).values());
    }

    @Parameters(arity = "1..*",
            description = "Group Options: Groups and their file names for output reads. At least 1 group must " +
            "be specified. Built-in groups R1, R2, R3... used for input reads. Example: --group-R1 out_R1.fastq " +
            "--group-R2 out_R2.fastq --group-UMI UMI.fastq")
    private List<String> groupsQuery = new ArrayList<>();

    @Option(description = IN_FILE_OR_STDIN,
            names = {"--input"})
    private String inputFileName = null;

    @Option(description = "Copy original comments from initial fastq files to comments of output fastq files.",
            names = {"--copy-original-headers"})
    private boolean copyOriginalHeaders = false;

    @Option(description = NUMBER_OF_READS,
            names = {"-n", "--number-of-reads"})
    private long inputReadsLimit = 0;

    private LinkedHashMap<String, String> parseGroups(List<String> groupsQuery) {
        if (groupsQuery.size() % 2 != 0)
            throwValidationException("Group Options not parsed, expected pairs of groups and their file names: "
                    + groupsQuery);
        LinkedHashMap<String, String> groups = new LinkedHashMap<>();
        for (int i = 0; i < groupsQuery.size(); i += 2) {
            String currentGroup = groupsQuery.get(i);
            String currentFileName = groupsQuery.get(i + 1);
            if ((currentGroup.length() < 9) || !currentGroup.substring(0, 8).equals("--group-"))
                throwValidationException("Syntax error in group Option: " + currentGroup);
            groups.put(currentGroup.substring(8), currentFileName);
        }
        return groups;
    }
}
