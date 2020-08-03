/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
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
import com.milaboratory.minnn.io.SorterIO;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.PipelineConfigurationReaderMiNNN.pipelineConfigurationReaderInstance;
import static com.milaboratory.minnn.cli.SortAction.SORT_ACTION_NAME;
import static com.milaboratory.minnn.io.MifInfoExtractor.mifInfoExtractor;

@Command(name = SORT_ACTION_NAME,
        sortOptions = false,
        showDefaultValues = true,
        separator = " ",
        description = "Sort reads by contents (nucleotide sequences) of specified groups.")
public final class SortAction extends ACommandWithSmartOverwrite implements MiNNNCommand {
    public static final String SORT_ACTION_NAME = "sort";

    public SortAction() {
        super(APP_NAME, mifInfoExtractor, pipelineConfigurationReaderInstance);
    }

    @Override
    public void run1() {
        SorterIO sorterIO = new SorterIO(getFullPipelineConfiguration(), inputFileName, outputFileName,
                sortGroupNames, chunkSize, reportFileName, jsonReportFileName, tmpFile);
        sorterIO.go();
    }

    @Override
    public void validateInfo(String inputFile) {
        MiNNNCommand.super.validateInfo(inputFile);
    }

    @Override
    public void validate() {
        MiNNNCommand.super.validate(getInputFiles(), getOutputFiles());
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
        List<String> outputFileNames = new ArrayList<>();
        if (outputFileName != null)
            outputFileNames.add(outputFileName);
        return outputFileNames;
    }

    @Override
    public void handleExistenceOfOutputFile(String outFileName) {
        // disable smart overwrite if input is from pipe
        if (inputFileName == null)
            MiNNNCommand.super.handleExistenceOfOutputFile(outFileName, forceOverwrite || overwriteIfRequired);
        else
            super.handleExistenceOfOutputFile(outFileName);
    }

    @Override
    public ActionConfiguration getConfiguration() {
        return new SortActionConfiguration(new SortActionConfiguration.SortActionParameters(sortGroupNames,
                chunkSize));
    }

    @Override
    public PipelineConfiguration getFullPipelineConfiguration() {
        if (inputFileName != null)
            return PipelineConfiguration.appendStep(pipelineConfigurationReader.fromFile(inputFileName,
                    binaryFileInfoExtractor.getFileInfo(inputFileName)), getInputFiles(), getConfiguration(),
                    AppVersionInfo.get());
        else
            return PipelineConfiguration.mkInitial(new ArrayList<>(), getConfiguration(), AppVersionInfo.get());
    }

    @Option(description = "Group names to use for sorting. Priority is in descending order.",
            names = {"--groups"},
            required = true,
            arity = "1..*")
    private List<String> sortGroupNames = null;

    @Option(description = IN_FILE_OR_STDIN,
            names = {"--input"})
    private String inputFileName = null;

    @Option(description = OUT_FILE_OR_STDOUT,
            names = {"--output"})
    private String outputFileName = null;

    @Option(description = "Chunk size for sorter.",
            names = {"--chunk-size"})
    private int chunkSize = -1;

    @Option(description = REPORT,
            names = "--report")
    private String reportFileName = null;

    @Option(description = JSON_REPORT,
            names = "--json-report")
    private String jsonReportFileName = null;

    @Option(description = "Custom temp file, used for debugging purposes.",
            names = {"--temp-file"},
            hidden = true)
    private String tmpFile = null;
}
