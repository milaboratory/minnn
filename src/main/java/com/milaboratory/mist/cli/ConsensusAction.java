package com.milaboratory.mist.cli;

import com.beust.jcommander.*;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.ConsensusIO;

import java.util.*;

import static com.milaboratory.mist.cli.Defaults.*;

public final class ConsensusAction implements Action {
    private final ConsensusActionParameters params = new ConsensusActionParameters();

    @Override
    public void go(ActionHelper helper) {
        ConsensusIO consensusIO = new ConsensusIO(params.groupList, params.inputFileName, params.outputFileName,
                params.alignerWidth, params.matchScore, params.mismatchScore, params.gapScore, params.scoreThreshold,
                params.skippedFractionToRepeat, params.minGoodSeqLength, params.threads,
                params.maxConsensusesPerCluster, params.avgQualityThreshold, params.windowSize);
        consensusIO.go();
    }

    @Override
    public String command() {
        return "consensus";
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription = "Calculate consensus sequences for all barcodes.")
    private static final class ConsensusActionParameters extends ActionParameters {
        @Parameter(description = "Input file in \"mif\" format. If not specified, stdin will be used.",
                names = {"--input"}, order = 0)
        String inputFileName = null;

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 1)
        String outputFileName = null;

        @Parameter(description = "List of groups that represent barcodes. If not specified, all groups will be used.",
                names = {"--groups"}, variableArity = true)
        List<String> groupList = null;

        @Parameter(description = "Window width (maximum allowed number of indels) for banded aligner.",
                names = {"--width"})
        int alignerWidth = DEFAULT_CONSENSUS_ALIGNER_WIDTH;

        @Parameter(description = "Score for perfectly matched nucleotide, used in sequences alignment.",
                names = {"--aligner-match-score"})
        int matchScore = DEFAULT_MATCH_SCORE;

        @Parameter(description = "Score for mismatched nucleotide, used in sequences alignment.",
                names = {"--aligner-mismatch-score"})
        int mismatchScore = DEFAULT_MISMATCH_SCORE;

        @Parameter(description = "Score for gap or insertion, used in sequences alignment.",
                names = {"--aligner-gap-score"})
        int gapScore = DEFAULT_GAP_SCORE;

        @Parameter(description = "Score threshold that used to filter reads for calculating consensus.",
                names = {"--score-threshold"})
        long scoreThreshold = DEFAULT_CONSENSUS_SCORE_THRESHOLD;

        @Parameter(description = "Fraction of reads skipped by score threshold that must start the search for " +
                "another consensus in skipped reads. Value 1 means always get only 1 consensus from one set of " +
                "reads with identical barcodes.",
                names = {"--skipped-fraction-to-repeat"})
        float skippedFractionToRepeat = DEFAULT_CONSENSUS_SKIPPED_FRACTION_TO_REPEAT;

        @Parameter(description = "Minimal length of good sequence that will be still considered good after trimming " +
                "bad quality tails.",
                names = {"--min-good-sequence-length"})
        int minGoodSeqLength = DEFAULT_CONSENSUS_MIN_GOOD_SEQ_LENGTH;

        @Parameter(description = "Number of threads for calculating consensus sequences.",
                names = {"--threads"})
        int threads = DEFAULT_THREADS;

        @Parameter(description = "Maximal number of consensuses generated from 1 cluster. Every time this threshold " +
                "is applied to stop searching for new consensuses, warning will be displayed. Too many consensuses " +
                "per cluster indicate that score threshold, aligner width or skipped fraction to repeat is too low.",
                names = {"--max-consensuses-per-cluster"})
        int maxConsensusesPerCluster = DEFAULT_CONSENSUS_MAX_PER_CLUSTER;

        @Parameter(description = "Minimal average quality for bad quality tails trimmer.",
                names = {"--avg-quality-threshold"})
        float avgQualityThreshold = DEFAULT_CONSENSUS_AVG_QUALITY_THRESHOLD;

        @Parameter(description = "Window size for bad quality tails trimmer.",
                names = {"--window-size"})
        int windowSize = DEFAULT_CONSENSUS_QUALITY_WINDOW_SIZE;
    }
}
