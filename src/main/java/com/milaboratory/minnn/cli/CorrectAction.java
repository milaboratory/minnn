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

import com.milaboratory.cli.ACommandWithSmartOverwrite;
import com.milaboratory.cli.ActionConfiguration;
import com.milaboratory.cli.AppVersionInfo;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.minnn.io.CorrectBarcodesIO;
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
        CorrectBarcodesIO correctBarcodesIO = new CorrectBarcodesIO(getFullPipelineConfiguration(), inputFileName,
                outputFileName, mismatches, indels, totalErrors, threshold, groupNames, maxClusterDepth,
                singleSubstitutionProbability, singleIndelProbability, minCount, excludedBarcodesOutputFileName,
                inputReadsLimit, quiet);
        correctBarcodesIO.go();
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
            MiNNNCommand.super.handleExistenceOfOutputFile(outFileName, forceOverwrite);
        else
            super.handleExistenceOfOutputFile(outFileName);
    }

    @Override
    public ActionConfiguration getConfiguration() {
        return new CorrectActionConfiguration(new CorrectActionConfiguration.CorrectActionParameters(groupNames,
                mismatches, indels, totalErrors, threshold, maxClusterDepth, singleSubstitutionProbability,
                singleIndelProbability, minCount, inputReadsLimit));
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

    @Option(description = IN_FILE_NO_STDIN,
            names = {"--input"},
            required = true)
    private String inputFileName = null;

    @Option(description = OUT_FILE_OR_STDOUT,
            names = {"--output"})
    private String outputFileName = null;

    @Option(description = "Maximum number of mismatches between barcodes for which they are considered identical.",
            names = {"--max-mismatches"})
    private int mismatches = DEFAULT_CORRECT_MAX_MISMATCHES;

    @Option(description = "Maximum number of insertions or deletions between barcodes for which they are " +
            "considered identical.",
            names = {"--max-indels"})
    private int indels = DEFAULT_CORRECT_MAX_INDELS;

    @Option(description = "Maximum Levenshtein distance between barcodes for which they are considered identical.",
            names = {"--max-total-errors"})
    private int totalErrors = DEFAULT_CORRECT_MAX_TOTAL_ERRORS;

    @Option(description = "Threshold for UMI clustering: if smaller UMI count divided to larger UMI count " +
            "is below this threshold, UMI will be merged to the cluster.",
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

    @Option(description = "Barcodes with count less than specified will not be included in the output.",
            names = {"--min-count"})
    private int minCount = 0;

    @Option(description = "Output file for reads with barcodes excluded by count. If not specified, reads with " +
            "excluded barcodes will not be written anywhere",
            names = {"--excluded-barcodes-output"})
    private String excludedBarcodesOutputFileName = null;

    @Option(description = NUMBER_OF_READS,
            names = {"-n", "--number-of-reads"})
    private long inputReadsLimit = 0;
}
