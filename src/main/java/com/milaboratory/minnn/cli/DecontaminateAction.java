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

import com.milaboratory.cli.ACommandWithSmartOverwrite;
import com.milaboratory.cli.ActionConfiguration;
import com.milaboratory.cli.AppVersionInfo;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.minnn.io.DecontaminateIO;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.DecontaminateAction.DECONTAMINATE_ACTION_NAME;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.PipelineConfigurationReaderMiNNN.pipelineConfigurationReaderInstance;
import static com.milaboratory.minnn.io.MifInfoExtractor.mifInfoExtractor;

@Command(name = DECONTAMINATE_ACTION_NAME,
        sortOptions = false,
        showDefaultValues = true,
        separator = " ",
        description = "Remove reads with barcodes that come from other cells.")
public final class DecontaminateAction extends ACommandWithSmartOverwrite implements MiNNNCommand {
    public static final String DECONTAMINATE_ACTION_NAME = "decontaminate";

    public DecontaminateAction() {
        super(APP_NAME, mifInfoExtractor, pipelineConfigurationReaderInstance);
    }

    @Override
    public void run1() {
        DecontaminateIO decontaminateIO = new DecontaminateIO(getFullPipelineConfiguration(), inputFileName,
                outputFileName, excludedBarcodesOutputFileName, groupNames, primaryGroupNames, minCountShare,
                inputReadsLimit, reportFileName, jsonReportFileName);
        decontaminateIO.go();
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
        return Collections.singletonList(inputFileName);
    }

    @Override
    protected List<String> getOutputFiles() {
        List<String> outputFileNames = new ArrayList<>();
        if (outputFileName != null)
            outputFileNames.add(outputFileName);
        if (excludedBarcodesOutputFileName != null)
            outputFileNames.add(excludedBarcodesOutputFileName);
        return outputFileNames;
    }

    @Override
    public void handleExistenceOfOutputFile(String outFileName) {
        // disable smart overwrite if output file for reads with excluded barcodes is specified
        if (excludedBarcodesOutputFileName != null)
            MiNNNCommand.super.handleExistenceOfOutputFile(outFileName, forceOverwrite || overwriteIfRequired);
        else
            super.handleExistenceOfOutputFile(outFileName);
    }

    @Override
    public ActionConfiguration getConfiguration() {
        return new DecontaminateActionConfiguration(new DecontaminateActionConfiguration.DecontaminateActionParameters(
                groupNames, primaryGroupNames, minCountShare, inputReadsLimit));
    }

    @Override
    public PipelineConfiguration getFullPipelineConfiguration() {
        return PipelineConfiguration.appendStep(pipelineConfigurationReader.fromFile(inputFileName,
                binaryFileInfoExtractor.getFileInfo(inputFileName)), getInputFiles(), getConfiguration(),
                AppVersionInfo.get());
    }

    @Option(description = "Group names for molecular barcodes (UMI). Reads where these barcodes are contaminated " +
            "from other cells will be filtered out.",
            names = {"--groups"},
            required = true,
            arity = "1..*")
    private List<String> groupNames = null;

    @Option(description = "Primary group names. These groups contains cell barcodes: each combination of primary " +
            "group values corresponds to 1 cell. Molecular barcodes are counted separately for each cell, and then " +
            "reads containing molecular barcodes with significantly lower counts than in other cell " +
            "will be removed.",
            names = {"--primary-groups"},
            required = true,
            arity = "1..*")
    private List<String> primaryGroupNames = null;

    @Option(description = IN_FILE_NO_STDIN,
            names = {"--input"},
            required = true)
    private String inputFileName = null;

    @Option(description = OUT_FILE_OR_STDOUT,
            names = {"--output"})
    private String outputFileName = null;

    @Option(description = "Output file for reads with filtered out barcodes. If not specified, reads with " +
            "filtered out barcodes will not be written anywhere.",
            names = {"--excluded-barcodes-output"})
    private String excludedBarcodesOutputFileName = null;

    @Option(description = "Threshold for filtering out molecular barcodes. If count of a molecular barcode is " +
            "lower than count of the same barcode in different cell, multiplied on this share, then reads in the " +
            "cell with lower count of this barcode will be considered contaminated and will be filtered out.",
            names = {"--min-count-share"})
    private float minCountShare = DEFAULT_DECONTAMINATE_MIN_COUNT_SHARE;

    @Option(description = NUMBER_OF_READS,
            names = {"-n", "--number-of-reads"})
    private long inputReadsLimit = 0;

    @Option(description = REPORT,
            names = "--report")
    private String reportFileName = null;

    @Option(description = JSON_REPORT,
            names = "--json-report")
    private String jsonReportFileName = null;
}
