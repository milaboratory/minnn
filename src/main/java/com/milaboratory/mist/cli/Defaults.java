package com.milaboratory.mist.cli;

import static com.milaboratory.core.sequence.SequenceQuality.BAD_QUALITY_VALUE;
import static com.milaboratory.core.sequence.SequenceQuality.GOOD_QUALITY_VALUE;

public final class Defaults {
    public final static long DEFAULT_PENALTY_THRESHOLD = -30;
    public final static int DEFAULT_BITAP_MAX_ERRORS = 2;
    public final static int DEFAULT_MAX_OVERLAP = 2;
    public final static int DEFAULT_MATCH_SCORE = 0;
    public final static int DEFAULT_MISMATCH_SCORE = -9;
    public final static int DEFAULT_GAP_SCORE = -10;
    public final static byte DEFAULT_GOOD_QUALITY = GOOD_QUALITY_VALUE;
    public final static byte DEFAULT_BAD_QUALITY = BAD_QUALITY_VALUE;
    public final static int DEFAULT_MAX_QUALITY_PENALTY = -4;
    public final static long DEFAULT_SINGLE_OVERLAP_PENALTY = DEFAULT_GAP_SCORE;
    public final static int DEFAULT_THREADS = 4;
    public final static String DEFAULT_INPUT_FORMAT = "fastq";
    public final static int DEFAULT_SORT_CHUNK_SIZE = 1000000;
    public final static int DEFAULT_CORRECT_MAX_MISMATCHES = 2;
    public final static int DEFAULT_CORRECT_MAX_DELETIONS = 2;
    public final static int DEFAULT_CORRECT_MAX_INSERTIONS = 2;
    public final static int DEFAULT_CORRECT_MAX_TOTAL_ERRORS = 3;
    public final static int DEFAULT_CONSENSUS_ALIGNER_WIDTH = 5;
    public final static int DEFAULT_CONSENSUS_PENALTY_THRESHOLD = -100;
    public final static float DEFAULT_CONSENSUS_SKIPPED_FRACTION_TO_REPEAT = 0.5f;
}
