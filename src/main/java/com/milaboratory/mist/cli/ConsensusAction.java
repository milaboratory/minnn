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
                params.skippedFractionToRepeat, params.maxConsensusesPerCluster, params.readsMinGoodSeqLength,
                params.readsAvgQualityThreshold, params.readsTrimWindowSize, params.minGoodSeqLength,
                params.avgQualityThreshold, params.trimWindowSize, params.inputReadsLimit, params.maxWarnings,
                params.threads, params.debugOutputFileName, params.debugQualityThreshold);
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
                names = {"--groups"}, order = 2, variableArity = true)
        List<String> groupList = null;

        @Parameter(description = "Window width (maximum allowed number of indels) for banded aligner.",
                names = {"--width"}, order = 3)
        int alignerWidth = DEFAULT_CONSENSUS_ALIGNER_WIDTH;

        @Parameter(description = "Score for perfectly matched nucleotide, used in sequences alignment.",
                names = {"--aligner-match-score"}, order = 4)
        int matchScore = DEFAULT_MATCH_SCORE;

        @Parameter(description = "Score for mismatched nucleotide, used in sequences alignment.",
                names = {"--aligner-mismatch-score"}, order = 5)
        int mismatchScore = DEFAULT_MISMATCH_SCORE;

        @Parameter(description = "Score for gap or insertion, used in sequences alignment.",
                names = {"--aligner-gap-score"}, order = 6)
        int gapScore = DEFAULT_GAP_SCORE;

        @Parameter(description = "Score threshold that used to filter reads for calculating consensus.",
                names = {"--score-threshold"}, order = 7)
        long scoreThreshold = DEFAULT_CONSENSUS_SCORE_THRESHOLD;

        @Parameter(description = "Fraction of reads skipped by score threshold that must start the search for " +
                "another consensus in skipped reads. Value 1 means always get only 1 consensus from one set of " +
                "reads with identical barcodes.",
                names = {"--skipped-fraction-to-repeat"}, order = 8)
        float skippedFractionToRepeat = DEFAULT_CONSENSUS_SKIPPED_FRACTION_TO_REPEAT;

        @Parameter(description = "Maximal number of consensuses generated from 1 cluster. Every time this threshold " +
                "is applied to stop searching for new consensuses, warning will be displayed. Too many consensuses " +
                "per cluster indicate that score threshold, aligner width or skipped fraction to repeat is too low.",
                names = {"--max-consensuses-per-cluster"}, order = 9)
        int maxConsensusesPerCluster = DEFAULT_CONSENSUS_MAX_PER_CLUSTER;

        @Parameter(description = "Minimal length of good sequence that will be still considered good after trimming " +
                "bad quality tails. This parameter is for trimming input reads.",
                names = {"--reads-min-good-sequence-length"}, order = 10)
        int readsMinGoodSeqLength = DEFAULT_CONSENSUS_READS_MIN_GOOD_SEQ_LENGTH;

        @Parameter(description = "Minimal average quality for bad quality tails trimmer. This parameter is for " +
                "trimming input reads.",
                names = {"--reads-avg-quality-threshold"}, order = 11)
        float readsAvgQualityThreshold = DEFAULT_CONSENSUS_READS_AVG_QUALITY_THRESHOLD;

        @Parameter(description = "Window size for bad quality tails trimmer. This parameter is for trimming input " +
                "reads.",
                names = {"--reads-trim-window-size"}, order = 12)
        int readsTrimWindowSize = DEFAULT_CONSENSUS_READS_TRIM_WINDOW_SIZE;

        @Parameter(description = "Minimal length of good sequence that will be still considered good after trimming " +
                "bad quality tails. This parameter is for trimming output consensuses.",
                names = {"--min-good-sequence-length"}, order = 13)
        int minGoodSeqLength = DEFAULT_CONSENSUS_MIN_GOOD_SEQ_LENGTH;

        @Parameter(description = "Minimal average quality for bad quality tails trimmer. This parameter is for " +
                "trimming output consensuses.",
                names = {"--avg-quality-threshold"}, order = 14)
        float avgQualityThreshold = DEFAULT_CONSENSUS_AVG_QUALITY_THRESHOLD;

        @Parameter(description = "Window size for bad quality tails trimmer. This parameter is for trimming output " +
                "consensuses.",
                names = {"--trim-window-size"}, order = 15)
        int trimWindowSize = DEFAULT_CONSENSUS_TRIM_WINDOW_SIZE;

        @Parameter(description = "Number of reads to take; 0 value means to take the entire input file.",
                names = {"-n", "--number-of-reads"}, order = 16)
        long inputReadsLimit = 0;

        @Parameter(description = "Maximum allowed number of warnings; -1 means no limit.",
                names = {"--max-warnings"}, order = 17)
        int maxWarnings = -1;

        @Parameter(description = "Number of threads for calculating consensus sequences.",
                names = {"--threads"}, order = 18)
        int threads = DEFAULT_THREADS;

        @Parameter(description = "Output text file for consensus algorithm debug information.",
                names = {"--debug-output"}, hidden = true)
        String debugOutputFileName = null;

        @Parameter(description = "Quality threshold to write capital letter in debug output file.",
                names = {"--debug-quality-threshold"}, hidden = true)
        byte debugQualityThreshold = (byte)(DEFAULT_GOOD_QUALITY / 2);
    }
}
