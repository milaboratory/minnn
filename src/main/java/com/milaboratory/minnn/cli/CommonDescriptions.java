package com.milaboratory.minnn.cli;

class CommonDescriptions {
    private CommonDescriptions() {}

    static final String IN_FILE_NO_STDIN = "Input file in \"mif\" format. This argument is required; stdin is not" +
            " supported.";
    static final String IN_FILE_OR_STDIN = "Input file in \"mif\" format. If not specified, stdin will be used.";
    static final String OUT_FILE_OR_STDOUT = "Output file in \"mif\" format. If not specified, stdout will be used.";
    static final String PATTERN_QUERY = "Query, pattern specified in MiNNN format.";
    static final String MATCH_SCORE = "Score for perfectly matched nucleotide.";
    static final String MISMATCH_SCORE = "Score for mismatched nucleotide.";
    static final String UPPERCASE_MISMATCH_SCORE = "Score for mismatched uppercase nucleotide.";
    static final String GAP_SCORE = "Score for gap or insertion.";
    static final String SCORE_THRESHOLD = "Score threshold, matches with score lower than this will not go to output.";
    static final String GOOD_QUALITY_VALUE = "This or better quality value will be considered good quality, " +
            "without score penalties.";
    static final String BAD_QUALITY_VALUE = "This or worse quality value will be considered bad quality, " +
            "with maximal score penalty.";
    static final String MAX_QUALITY_PENALTY = "Maximal score penalty for bad quality nucleotide in target.";
    static final String SINGLE_OVERLAP_PENALTY = "Score penalty for 1 nucleotide overlap between neighbor patterns. " +
            "Negative value.";
    static final String MAX_OVERLAP = "Max allowed overlap for 2 intersecting operands in +, & and pattern sequences.";
    static final String BITAP_MAX_ERRORS = "Maximum allowed number of errors for bitap matcher.";
    static final String FAIR_SORTING = "Use fair sorting and fair best match by score for all patterns.";
    static final String NUMBER_OF_READS = "Number of reads to take; 0 value means to take the entire input file.";
    static final String SUPPRESS_WARNINGS = "Don't display any warnings.";
}
