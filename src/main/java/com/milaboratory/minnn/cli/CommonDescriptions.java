package com.milaboratory.minnn.cli;

class CommonDescriptions {
    private CommonDescriptions() {}

    static final String IN_FILE_NO_STDIN = "Input file in \"mif\" format. This argument is required; stdin is not" +
            " supported.";
    static final String IN_FILE_OR_STDIN = "Input file in \"mif\" format. If not specified, stdin will be used.";
    static final String OUT_FILE_OR_STDOUT = "Output file in \"mif\" format. If not specified, stdout will be used.";
    static final String NUMBER_OF_READS = "Number of reads to take; 0 value means to take the entire input file.";
    static final String FAIR_SORTING = "Use fair sorting and fair best match by score for all patterns.";
    static final String SUPPRESS_WARNINGS = "Don't display any warnings.";
}
