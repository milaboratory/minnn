/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
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
import com.milaboratory.minnn.correct.BarcodeClusteringStrategyFactory;
import com.milaboratory.minnn.io.CorrectBarcodesIO;
import com.milaboratory.minnn.stat.SimpleMutationProbability;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.CorrectAction.CORRECT_ACTION_NAME;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.PipelineConfigurationReaderMiNNN.pipelineConfigurationReaderInstance;
import static com.milaboratory.minnn.io.MifInfoExtractor.mifInfoExtractor;

@Command(name = CORRECT_ACTION_NAME,
        sortOptions = false,
        showDefaultValues = true,
        separator = " ",
        description = "Correct errors in barcodes, and replace all barcodes with corrected variants.")
public final class CorrectAction extends ACommandWithSmartOverwrite implements MiNNNCommand {
    public static final String CORRECT_ACTION_NAME = "correct";

    public CorrectAction() {
        super(APP_NAME, mifInfoExtractor, pipelineConfigurationReaderInstance);
    }

    @Override
    public void run1() {
        BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory = new BarcodeClusteringStrategyFactory(
                maxErrorsShare, maxErrors, threshold, maxClusterDepth,
                new SimpleMutationProbability(singleSubstitutionProbability, singleIndelProbability));
        CorrectBarcodesIO correctBarcodesIO = new CorrectBarcodesIO(getFullPipelineConfiguration(), inputFileName,
                outputFileName, groupNames, primaryGroupNames, barcodeClusteringStrategyFactory, maxUniqueBarcodes,
                minCount, excludedBarcodesOutputFileName, disableWildcardsCollapsing, inputReadsLimit, quiet,
                reportFileName, jsonReportFileName);
        correctBarcodesIO.go();
    }

    @Override
    public void validateInfo(String inputFile) {
        MiNNNCommand.super.validateInfo(inputFile);
    }

    @Override
    public void validate() {
        MiNNNCommand.super.validate(getInputFiles(), getOutputFiles());
        if ((maxErrorsShare < 0) && (maxErrors < 0))
            throwValidationException("Both --max-errors and --max-errors-share are disabled (set to negative); " +
                    "enable at least one!");
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
        return new CorrectActionConfiguration(new CorrectActionConfiguration.CorrectActionParameters(groupNames,
                primaryGroupNames, maxErrorsShare, maxErrors, threshold, maxClusterDepth,
                singleSubstitutionProbability, singleIndelProbability, maxUniqueBarcodes, minCount,
                disableWildcardsCollapsing, inputReadsLimit));
    }

    @Override
    public PipelineConfiguration getFullPipelineConfiguration() {
        return PipelineConfiguration.appendStep(pipelineConfigurationReader.fromFile(inputFileName,
                binaryFileInfoExtractor.getFileInfo(inputFileName)), getInputFiles(), getConfiguration(),
                AppVersionInfo.get());
    }

    @Option(description = "Group names for correction.",
            names = {"--groups"},
            required = true,
            arity = "1..*")
    private List<String> groupNames = null;

    @Option(description = "Primary group names. If specified, all groups from --groups argument will be treated as " +
            "secondary. Barcode correction will be performed not in scale of the entire input file, but separately " +
            "in clusters with the same primary group values. If input file is already sorted by primary groups, " +
            "correction will be faster and less memory consuming. Usage example: correct cell barcodes (CB) first, " +
            "then sort by CB, then correct UMI for each CB separately. So, for first correction pass use " +
            "\"--groups CB\", and for second pass use \"--groups UMI --primary-groups CB\". If multiple primary " +
            "groups are specified, clusters will be determined by unique combinations of primary groups values.",
            names = {"--primary-groups"},
            arity = "1..*")
    private List<String> primaryGroupNames = null;

    @Option(description = IN_FILE_NO_STDIN,
            names = {"--input"},
            required = true)
    private String inputFileName = null;

    @Option(description = OUT_FILE_OR_STDOUT,
            names = {"--output"})
    private String outputFileName = null;

    @Option(description = "Relative maximal allowed number of errors (Levenshtein distance) between barcodes for " +
            "which they are considered identical. It is multiplied on average barcode length to calculate maximal " +
            "allowed number of errors; if result is less than 1, it rounds up to 1. This max errors calculation " +
            "method is enabled by default. " +
            CORRECT_MAX_ERRORS_COMMON,
            names = "--max-errors-share")
    private float maxErrorsShare = DEFAULT_MAX_ERRORS_SHARE;

    @Option(description = "Maximal Levenshtein distance between barcodes for which they are considered identical. " +
            CORRECT_MAX_ERRORS_COMMON,
            names = {"--max-errors"})
    private int maxErrors = -1;

    @Option(description = "Threshold for barcode clustering: if smaller barcode count divided to larger barcode " +
            "count is below this threshold, barcode will be merged to the cluster. This feature is turned off " +
            "(set to 1) by default, because there is already filtering by --single-substitution-probability and " +
            "--single-indel-probability enabled. You can turn on this filter (set the threshold) and set single " +
            "error probabilities to 1; or you can use both filters (by cluster threshold and by single error " +
            "probabilities) if you want.",
            names = {"--cluster-threshold"})
    private float threshold = DEFAULT_CORRECT_CLUSTER_THRESHOLD;

    @Option(description = "Maximum cluster depth for algorithm of similar barcodes clustering.",
            names = {"--max-cluster-depth"})
    private int maxClusterDepth = DEFAULT_CORRECT_MAX_CLUSTER_DEPTH;

    @Option(description = "Single substitution probability for clustering algorithm.",
            names = {"--single-substitution-probability"})
    private float singleSubstitutionProbability = DEFAULT_CORRECT_SINGLE_SUBSTITUTION_PROBABILITY;

    @Option(description = "Single insertion/deletion probability for clustering algorithm.",
            names = {"--single-indel-probability"})
    private float singleIndelProbability = DEFAULT_CORRECT_SINGLE_INDEL_PROBABILITY;

    @Option(description = "Maximal number of unique barcodes that will be included into output. Reads containing " +
            "barcodes with biggest counts will be included, reads with barcodes with smaller counts will be " +
            "excluded. Value 0 turns off this feature: if this argument is 0, all barcodes will be included.",
            names = {"--max-unique-barcodes"})
    private int maxUniqueBarcodes = 0;

    @Option(description = "Barcodes with count less than specified will not be included in the output.",
            names = {"--min-count"})
    private int minCount = 0;

    @Option(description = "Output file for reads with barcodes excluded by count. If not specified, reads with " +
            "excluded barcodes will not be written anywhere.",
            names = {"--excluded-barcodes-output"})
    private String excludedBarcodesOutputFileName = null;

    @Option(description = "Don't merge different barcodes that equal by wildcards (for example, AAAT and ANNT). " +
            "It will improve performance. Can be used when there are no wildcards in barcodes, or when barcodes " +
            "correction is not needed, and command is run only to filter barcodes by count.",
            names = {"--disable-wildcards-collapsing"})
    private boolean disableWildcardsCollapsing = false;

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
