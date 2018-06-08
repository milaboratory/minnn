package com.milaboratory.mist.cli;

import static com.milaboratory.core.sequence.SequenceQuality.BAD_QUALITY_VALUE;
import static com.milaboratory.core.sequence.SequenceQuality.GOOD_QUALITY_VALUE;

public final class Defaults {
    public final static long DEFAULT_SCORE_THRESHOLD = -30;
    public final static int DEFAULT_BITAP_MAX_ERRORS = 2;
    public final static int DEFAULT_MAX_OVERLAP = 2;
    public final static int DEFAULT_MATCH_SCORE = 0;
    public final static int DEFAULT_MISMATCH_SCORE = -5;
    public final static int DEFAULT_UPPERCASE_MISMATCH_SCORE = -1000000;
    public final static int DEFAULT_GAP_SCORE = -11;
    public final static byte DEFAULT_GOOD_QUALITY = GOOD_QUALITY_VALUE;
    public final static byte DEFAULT_BAD_QUALITY = BAD_QUALITY_VALUE;
    public final static int DEFAULT_MAX_QUALITY_PENALTY = -4;
    public final static long DEFAULT_SINGLE_OVERLAP_PENALTY = DEFAULT_GAP_SCORE;
    public final static int DEFAULT_THREADS = 4;
    public final static String DEFAULT_INPUT_FORMAT = "fastq";
    public final static int DEFAULT_SORT_CHUNK_SIZE = 1000000;
    public final static int DEFAULT_CORRECT_MAX_MISMATCHES = 2;
    public final static int DEFAULT_CORRECT_MAX_INDELS = 2;
    public final static int DEFAULT_CORRECT_MAX_TOTAL_ERRORS = 3;
    public final static float DEFAULT_CORRECT_CLUSTER_THRESHOLD = 0.03f;
    public final static int DEFAULT_CORRECT_MAX_CLUSTER_DEPTH = 2;
    public final static float DEFAULT_CORRECT_SINGLE_MUTATION_PROBABILITY = 0.3f;
    public final static int DEFAULT_CONSENSUS_ALIGNER_WIDTH = 20;
    public final static int DEFAULT_CONSENSUS_SCORE_THRESHOLD = -500;
    public final static float DEFAULT_CONSENSUS_SKIPPED_FRACTION_TO_REPEAT = 0.5f;
    public final static int DEFAULT_CONSENSUS_MAX_PER_CLUSTER = 10;
    public final static byte DEFAULT_CONSENSUS_READS_MIN_GOOD_SEQ_LENGTH = 5;
    public final static float DEFAULT_CONSENSUS_READS_AVG_QUALITY_THRESHOLD = 10.0f;
    public final static int DEFAULT_CONSENSUS_READS_TRIM_WINDOW_SIZE = 20;
    public final static byte DEFAULT_CONSENSUS_MIN_GOOD_SEQ_LENGTH = 5;
    public final static float DEFAULT_CONSENSUS_AVG_QUALITY_THRESHOLD = 10.0f;
    public final static int DEFAULT_CONSENSUS_TRIM_WINDOW_SIZE = 20;
    public final static int DEFAULT_DEMULTIPLEX_OUTPUT_BUFFER_SIZE = 1 << 16;
}
