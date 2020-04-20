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
import com.milaboratory.minnn.io.FilterByCountIO;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.FilterByCountAction.FILTER_BY_COUNT_ACTION_NAME;
import static com.milaboratory.minnn.cli.PipelineConfigurationReaderMiNNN.pipelineConfigurationReaderInstance;
import static com.milaboratory.minnn.io.MifInfoExtractor.mifInfoExtractor;

@Command(name = FILTER_BY_COUNT_ACTION_NAME,
        sortOptions = false,
        showDefaultValues = true,
        separator = " ",
        description = "Filter reads by count of barcode values.")
public final class FilterByCountAction extends ACommandWithSmartOverwrite implements MiNNNCommand {
    public static final String FILTER_BY_COUNT_ACTION_NAME = "filter-by-count";

    public FilterByCountAction() {
        super(APP_NAME, mifInfoExtractor, pipelineConfigurationReaderInstance);
    }

    @Override
    public void run1() {
        FilterByCountIO filterByCountIO = new FilterByCountIO(getFullPipelineConfiguration(), inputFileName,
                outputFileName, groupNames, maxUniqueBarcodes, minCount, excludedBarcodesOutputFileName,
                inputReadsLimit, reportFileName, jsonReportFileName);
        filterByCountIO.go();
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
        return new FilterByCountActionConfiguration(new FilterByCountActionConfiguration
                .FilterByCountActionParameters(groupNames, maxUniqueBarcodes, minCount, inputReadsLimit));
    }

    @Override
    public PipelineConfiguration getFullPipelineConfiguration() {
        return PipelineConfiguration.appendStep(pipelineConfigurationReader.fromFile(inputFileName,
                binaryFileInfoExtractor.getFileInfo(inputFileName)), getInputFiles(), getConfiguration(),
                AppVersionInfo.get());
    }

    @Option(description = "Group names for filtering by count.",
            names = {"--groups"},
            required = true,
            arity = "1..*")
    private List<String> groupNames = null;

    @Option(description = IN_MIF_FILE,
            names = {"--input"},
            required = true)
    private String inputFileName = null;

    @Option(description = OUT_MIF_FILE,
            names = {"--output"},
            required = true)
    private String outputFileName = null;

    @Option(description = MAX_UNIQUE_BARCODES,
            names = {"--max-unique-barcodes"})
    private int maxUniqueBarcodes = 0;

    @Option(description = MIN_COUNT,
            names = {"--min-count"})
    private int minCount = 0;

    @Option(description = EXCLUDED_BARCODES_OUTPUT,
            names = {"--excluded-barcodes-output"})
    private String excludedBarcodesOutputFileName = null;

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
