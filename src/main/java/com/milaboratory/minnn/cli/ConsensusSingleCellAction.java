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
import com.milaboratory.minnn.io.ConsensusIO;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.ConsensusSingleCellAction.CONSENSUS_SINGLE_CELL_ACTION_NAME;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.PipelineConfigurationReaderMiNNN.pipelineConfigurationReaderInstance;
import static com.milaboratory.minnn.consensus.ConsensusAlgorithms.SINGLE_CELL;
import static com.milaboratory.minnn.io.MifInfoExtractor.mifInfoExtractor;

@Command(name = CONSENSUS_SINGLE_CELL_ACTION_NAME,
        sortOptions = false,
        showDefaultValues = true,
        separator = " ",
        description = "Calculate consensus sequences for all barcodes.")
public final class ConsensusSingleCellAction extends ACommandWithSmartOverwrite implements MiNNNCommand {
    public static final String CONSENSUS_SINGLE_CELL_ACTION_NAME = "consensus";

    public ConsensusSingleCellAction() {
        super(APP_NAME, mifInfoExtractor, pipelineConfigurationReaderInstance);
    }

    @Override
    public void run1() {
        int actualMaxWarnings = quiet ? 0 : maxWarnings;
        ConsensusIO consensusIO = new ConsensusIO(getFullPipelineConfiguration(), groupList, inputFileName,
                outputFileName, SINGLE_CELL, 0, 0, 0, 0,
                0, (byte)0, 0, skippedFractionToRepeat,
                maxConsensusesPerCluster, dropOversizedClusters, readsMinGoodSeqLength, readsAvgQualityThreshold,
                readsTrimWindowSize, minGoodSeqLength, lowCoverageThreshold, avgQualityThreshold,
                avgQualityThresholdForLowCoverage, trimWindowSize, originalReadStatsFileName, notUsedReadsOutputFileName,
                toSeparateGroups, inputReadsLimit, actualMaxWarnings, threads, kmerLength, kmerOffset, kmerMaxErrors,
                reportFileName, jsonReportFileName, debugOutputFileName, debugQualityThreshold);
        consensusIO.go();
    }

    @Override
    public void validateInfo(String inputFile) {
        MiNNNCommand.super.validateInfo(inputFile);
    }

    @Override
    public void validate() {
        MiNNNCommand.super.validate(getInputFiles(), getOutputFiles());
        if (maxConsensusesPerCluster < 1)
            throwValidationException("--max-consensuses-per-cluster value must be positive!");
        if (readsMinGoodSeqLength < 1)
            throwValidationException("--reads-min-good-sequence-length value must be positive!");
        if (minGoodSeqLength < 1)
            throwValidationException("--min-good-sequence-length value must be positive!");
        if (kmerLength < 1)
            throwValidationException("--kmer-length value must be positive!");
        if (kmerOffset < 0)
            throwValidationException("--kmer-offset value must not be negative!");
        if (kmerMaxErrors < 0)
            throwValidationException("--kmer-max-errors value must not be negative!");
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
        if (originalReadStatsFileName != null)
            outputFileNames.add(originalReadStatsFileName);
        if (notUsedReadsOutputFileName != null)
            outputFileNames.add(notUsedReadsOutputFileName);
        return outputFileNames;
    }

    @Override
    public void handleExistenceOfOutputFile(String outFileName) {
        // disable smart overwrite if input is from pipe or extra output files are specified
        if ((inputFileName == null) || (originalReadStatsFileName != null) || (notUsedReadsOutputFileName != null))
            MiNNNCommand.super.handleExistenceOfOutputFile(outFileName, forceOverwrite || overwriteIfRequired);
        else
            super.handleExistenceOfOutputFile(outFileName);
    }

    @Override
    public ActionConfiguration getConfiguration() {
        return new ConsensusSingleCellActionConfiguration(new ConsensusSingleCellActionConfiguration
                .ConsensusSingleCellActionParameters(groupList, skippedFractionToRepeat, maxConsensusesPerCluster,
                dropOversizedClusters, readsMinGoodSeqLength, readsAvgQualityThreshold, readsTrimWindowSize, minGoodSeqLength,
                lowCoverageThreshold, avgQualityThreshold, avgQualityThresholdForLowCoverage, trimWindowSize,
                toSeparateGroups, inputReadsLimit, kmerLength, kmerOffset, kmerMaxErrors));
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

    @Option(description = IN_FILE_OR_STDIN,
            names = {"--input"})
    private String inputFileName = null;

    @Option(description = OUT_FILE_OR_STDOUT,
            names = {"--output"})
    private String outputFileName = null;

    @Option(description = CONSENSUS_GROUP_LIST,
            names = {"--groups"},
            required = true,
            arity = "1..*")
    private List<String> groupList = null;

    @Option(description = SKIPPED_FRACTION_TO_REPEAT,
            names = {"--skipped-fraction-to-repeat"})
    private float skippedFractionToRepeat = DEFAULT_CONSENSUS_SKIPPED_FRACTION_TO_REPEAT;

    @Option(description = MAX_CONSENSUSES_PER_CLUSTER,
            names = {"--max-consensuses-per-cluster"})
    private int maxConsensusesPerCluster = DEFAULT_CONSENSUS_MAX_PER_CLUSTER;

    @Option(description = DROP_OVERSIZED_CLUSTERS,
            names = {"--drop-oversized-clusters"})
    private boolean dropOversizedClusters = false;

    @Option(description = READS_MIN_GOOD_SEQUENCE_LENGTH,
            names = {"--reads-min-good-sequence-length"})
    private int readsMinGoodSeqLength = DEFAULT_CONSENSUS_READS_MIN_GOOD_SEQ_LENGTH;

    @Option(description = READS_AVG_QUALITY_THRESHOLD,
            names = {"--reads-avg-quality-threshold"})
    private float readsAvgQualityThreshold = DEFAULT_CONSENSUS_READS_AVG_QUALITY_THRESHOLD;

    @Option(description = READS_TRIM_WINDOW_SIZE,
            names = {"--reads-trim-window-size"})
    private int readsTrimWindowSize = DEFAULT_CONSENSUS_READS_TRIM_WINDOW_SIZE;

    @Option(description = CONSENSUSES_MIN_GOOD_SEQUENCE_LENGTH,
            names = {"--min-good-sequence-length"})
    private int minGoodSeqLength = DEFAULT_CONSENSUS_MIN_GOOD_SEQ_LENGTH;

    @Option(description = CONSENSUSES_LOW_COVERAGE_THRESHOLD,
            names = {"--low-coverage-threshold"})
    private float lowCoverageThreshold = DEFAULT_CONSENSUS_LOW_COVERAGE_THRESHOLD;

    @Option(description = CONSENSUSES_AVG_QUALITY_THRESHOLD,
            names = {"--avg-quality-threshold"})
    private float avgQualityThreshold = DEFAULT_CONSENSUS_AVG_QUALITY_THRESHOLD;

    @Option(description = CONSENSUSES_AVG_QUALITY_THRESHOLD_FOR_LOW_COVERAGE,
            names = {"--avg-quality-threshold-for-low-coverage"})
    private float avgQualityThresholdForLowCoverage = DEFAULT_CONSENSUS_AVG_QUALITY_THRESHOLD_FOR_LOW_COVERAGE;

    @Option(description = CONSENSUSES_TRIM_WINDOW_SIZE,
            names = {"--trim-window-size"})
    private int trimWindowSize = DEFAULT_CONSENSUS_TRIM_WINDOW_SIZE;

    @Option(description = ORIGINAL_READ_STATS,
            names = {"--original-read-stats"})
    private String originalReadStatsFileName = null;

    @Option(description = CONSENSUS_NOT_USED_READS_OUTPUT,
            names = {"--not-used-reads-output"})
    private String notUsedReadsOutputFileName = null;

    @Option(description = CONSENSUSES_TO_SEPARATE_GROUPS,
            names = {"--consensuses-to-separate-groups"})
    private boolean toSeparateGroups = false;

    @Option(description = NUMBER_OF_READS,
            names = {"-n", "--number-of-reads"})
    private long inputReadsLimit = 0;

    @Option(description = MAX_WARNINGS,
            names = {"--max-warnings"})
    private int maxWarnings = DEFAULT_CONSENSUS_MAX_WARNINGS;

    @Option(description = CONSENSUS_NUMBER_OF_THREADS,
            names = {"--threads"})
    private int threads = Math.min(DEFAULT_CONSENSUS_MAX_THREADS, Runtime.getRuntime().availableProcessors());

    @Option(description = REPORT,
            names = "--report")
    private String reportFileName = null;

    @Option(description = JSON_REPORT,
            names = "--json-report")
    private String jsonReportFileName = null;

    @Option(description = "K-mer length. Also affects --min-good-sequence-length because good sequence length must "
            + "not be lower than k-mer length, so the biggest of --kmer-length and --min-good-sequence-length "
            + "will be used as --min-good-sequence-length value.",
            names = {"--kmer-length"})
    private int kmerLength = DEFAULT_CONSENSUS_KMER_LENGTH;

    @Option(description = "Max offset from the middle of the read when searching k-mers.",
            names = {"--kmer-offset"})
    private int kmerOffset = DEFAULT_CONSENSUS_KMER_OFFSET;

    @Option(description = "Maximal allowed number of mismatches when searching k-mers in sequences.",
            names = {"--kmer-max-errors"})
    private int kmerMaxErrors = DEFAULT_CONSENSUS_KMER_MAX_ERRORS;

    @Option(description = CONSENSUS_DEBUG_OUTPUT,
            names = {"--debug-output"},
            hidden = true)
    private String debugOutputFileName = null;

    @Option(description = CONSENSUS_DEBUG_QUALITY_THRESHOLD,
            names = {"--debug-quality-threshold"},
            hidden = true)
    private byte debugQualityThreshold = (byte)(DEFAULT_GOOD_QUALITY / 2);
}
