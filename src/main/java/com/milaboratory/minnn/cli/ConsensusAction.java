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
import com.milaboratory.minnn.io.ConsensusIO;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.ConsensusAction.CONSENSUS_ACTION_NAME;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.PipelineConfigurationReaderMiNNN.pipelineConfigurationReaderInstance;
import static com.milaboratory.minnn.io.MifInfoExtractor.mifInfoExtractor;

@Command(name = CONSENSUS_ACTION_NAME,
        sortOptions = false,
        separator = " ",
        description = "Calculate consensus sequences for all barcodes.")
public final class ConsensusAction extends ACommandWithSmartOverwrite implements MiNNNCommand {
    public static final String CONSENSUS_ACTION_NAME = "consensus";

    public ConsensusAction() {
        super(APP_NAME, mifInfoExtractor, pipelineConfigurationReaderInstance);
    }

    @Override
    public void run1() {
        ConsensusIO consensusIO = new ConsensusIO(getFullPipelineConfiguration(), groupList, inputFileName,
                outputFileName, alignerWidth, matchScore, mismatchScore, gapScore, goodQualityMismatchPenalty,
                goodQualityMismatchThreshold, scoreThreshold, skippedFractionToRepeat, maxConsensusesPerCluster,
                readsMinGoodSeqLength, readsAvgQualityThreshold, readsTrimWindowSize, minGoodSeqLength,
                avgQualityThreshold, trimWindowSize, originalReadStatsFileName, notUsedReadsOutputFileName,
                toSeparateGroups, inputReadsLimit, maxWarnings, threads, debugOutputFileName, debugQualityThreshold);
        consensusIO.go();
    }

    @Override
    public void validateInfo(String inputFile) {
        MiNNNCommand.super.validateInfo(inputFile);
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
        // disable smart overwrite if extra output files are specified
        if ((originalReadStatsFileName != null) || (notUsedReadsOutputFileName != null))
            MiNNNCommand.super.handleExistenceOfOutputFile(outFileName, forceOverwrite);
        else
            super.handleExistenceOfOutputFile(outFileName);
    }

    @Override
    public ActionConfiguration getConfiguration() {
        return new ConsensusActionConfiguration(new ConsensusActionConfiguration.ConsensusActionParameters(
                groupList, alignerWidth, matchScore, mismatchScore, gapScore, goodQualityMismatchPenalty,
                goodQualityMismatchThreshold, scoreThreshold, skippedFractionToRepeat, maxConsensusesPerCluster,
                readsMinGoodSeqLength, readsAvgQualityThreshold, readsTrimWindowSize, minGoodSeqLength,
                avgQualityThreshold, trimWindowSize, toSeparateGroups, inputReadsLimit));
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

    @Option(description = "List of groups that represent barcodes. If not specified, all groups will be used.",
            names = {"--groups"},
            arity = "1..*")
    private List<String> groupList = null;

    @Option(description = "Window width (maximum allowed number of indels) for banded aligner.",
            names = {"--width"})
    private int alignerWidth = DEFAULT_CONSENSUS_ALIGNER_WIDTH;

    @Option(description = "Score for perfectly matched nucleotide, used in sequences alignment.",
            names = {"--aligner-match-score"})
    private int matchScore = DEFAULT_MATCH_SCORE;

    @Option(description = "Score for mismatched nucleotide, used in sequences alignment.",
            names = {"--aligner-mismatch-score"})
    private int mismatchScore = DEFAULT_MISMATCH_SCORE;

    @Option(description = "Score for gap or insertion, used in sequences alignment.",
            names = {"--aligner-gap-score"})
    private int gapScore = DEFAULT_GAP_SCORE;

    @Option(description = "Extra score penalty for mismatch when both sequences have good quality.",
            names = {"--good-quality-mismatch-penalty"})
    private long goodQualityMismatchPenalty = DEFAULT_CONSENSUS_GOOD_QUALITY_MISMATCH_PENALTY;

    @Option(description = "Quality that will be considered good for applying extra mismatch penalty.",
            names = {"--good-quality-mismatch-threshold"})
    private byte goodQualityMismatchThreshold = DEFAULT_CONSENSUS_GOOD_QUALITY_MISMATCH_THRESHOLD;

    @Option(description = "Score threshold that used to filter reads for calculating consensus.",
            names = {"--score-threshold"})
    private long scoreThreshold = DEFAULT_CONSENSUS_SCORE_THRESHOLD;

    @Option(description = "Fraction of reads skipped by score threshold that must start the search for " +
            "another consensus in skipped reads. Value 1 means always get only 1 consensus from one set of " +
            "reads with identical barcodes.",
            names = {"--skipped-fraction-to-repeat"})
    private float skippedFractionToRepeat = DEFAULT_CONSENSUS_SKIPPED_FRACTION_TO_REPEAT;

    @Option(description = "Maximal number of consensuses generated from 1 cluster. Every time this threshold " +
            "is applied to stop searching for new consensuses, warning will be displayed. Too many consensuses " +
            "per cluster indicate that score threshold, aligner width or skipped fraction to repeat is too low.",
            names = {"--max-consensuses-per-cluster"})
    private int maxConsensusesPerCluster = DEFAULT_CONSENSUS_MAX_PER_CLUSTER;

    @Option(description = "Minimal length of good sequence that will be still considered good after trimming " +
            "bad quality tails. This Option is for trimming input reads.",
            names = {"--reads-min-good-sequence-length"})
    private int readsMinGoodSeqLength = DEFAULT_CONSENSUS_READS_MIN_GOOD_SEQ_LENGTH;

    @Option(description = "Minimal average quality for bad quality tails trimmer. This Option is for " +
            "trimming input reads.",
            names = {"--reads-avg-quality-threshold"})
    private float readsAvgQualityThreshold = DEFAULT_CONSENSUS_READS_AVG_QUALITY_THRESHOLD;

    @Option(description = "Window size for bad quality tails trimmer. This Option is for trimming input " +
            "reads.",
            names = {"--reads-trim-window-size"})
    private int readsTrimWindowSize = DEFAULT_CONSENSUS_READS_TRIM_WINDOW_SIZE;

    @Option(description = "Minimal length of good sequence that will be still considered good after trimming " +
            "bad quality tails. This Option is for trimming output consensuses.",
            names = {"--min-good-sequence-length"})
    private int minGoodSeqLength = DEFAULT_CONSENSUS_MIN_GOOD_SEQ_LENGTH;

    @Option(description = "Minimal average quality for bad quality tails trimmer. This Option is for " +
            "trimming output consensuses.",
            names = {"--avg-quality-threshold"})
    private float avgQualityThreshold = DEFAULT_CONSENSUS_AVG_QUALITY_THRESHOLD;

    @Option(description = "Window size for bad quality tails trimmer. This Option is for trimming output " +
            "consensuses.",
            names = {"--trim-window-size"})
    private int trimWindowSize = DEFAULT_CONSENSUS_TRIM_WINDOW_SIZE;

    @Option(description = "Save extra statistics for each original read into separate file. Output file in " +
            "space separated text format.",
            names = {"--original-read-stats"})
    private String originalReadStatsFileName = null;

    @Option(description = "Write reads not used in consensus assembly into separate file. Output file in " +
            "\"mif\" format.",
            names = {"--not-used-reads-output"})
    private String notUsedReadsOutputFileName = null;

    @Option(description = "If this Option is specified, consensuses will not be written as " +
            "reads R1, R2 etc to output file. Instead, original sequences will be written as R1, R2 etc and " +
            "consensuses will be written as CR1, CR2 etc, so it will be possible to cluster original reads by " +
            "consensuses using filter / demultiplex actions, or export original reads and corresponding " +
            "consensuses into separate reads using mif2fastq action.",
            names = {"--consensuses-to-separate-groups"})
    private boolean toSeparateGroups = false;

    @Option(description = NUMBER_OF_READS,
            names = {"-n", "--number-of-reads"})
    private long inputReadsLimit = 0;

    @Option(description = "Maximum allowed number of warnings; -1 means no limit.",
            names = {"--max-warnings"})
    private int maxWarnings = -1;

    @Option(description = "Number of threads for calculating consensus sequences.",
            names = {"--threads"})
    private int threads = DEFAULT_THREADS;

    @Option(description = "Output text file for consensus algorithm debug information.",
            names = {"--debug-output"},
            hidden = true)
    private String debugOutputFileName = null;

    @Option(description = "Quality threshold to write capital letter in debug output file.",
            names = {"--debug-quality-threshold"},
            hidden = true)
    private byte debugQualityThreshold = (byte)(DEFAULT_GOOD_QUALITY / 2);
}
