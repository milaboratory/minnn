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

class CommonDescriptions {
    private CommonDescriptions() {}

    static final String IN_FILE_NO_STDIN = "Input file in MIF format. This argument is required; stdin is not" +
            " supported.";
    static final String IN_FILE_OR_STDIN = "Input file in MIF format. If not specified, stdin will be used.";
    static final String OUT_FILE_OR_STDOUT = "Output file in MIF format. If not specified, stdout will be used.";
    static final String OUT_TEXT_FILE = "Output text file. If not specified, stdout will be used.";
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
    static final String MIN_COUNT_FILTER = "Filter unique group values represented by less than specified number of " +
            "reads.";
    static final String MIN_FRAC_FILTER = "Filter unique group values represented by less than specified fraction " +
            "of reads.";
    static final String CORRECT_MAX_ERRORS_COMMON = "It is recommended to set only one of " +
            "--max-errors-share and --max-errors parameters, and set the other one to -1. Negative value means that " +
            "this max errors calculation method is disabled. If both methods are enabled, the lowest calculated " +
            "value of max errors is used.";
    static final String CONSENSUS_GROUP_LIST = "List of groups that represent barcodes. All these groups must be " +
            "sorted with \"sort\" action.";
    static final String SKIPPED_FRACTION_TO_REPEAT = "Fraction of reads skipped by score threshold that must start " +
            "the search for another consensus in skipped reads. Value 1 means always get only 1 consensus from one " +
            "set of reads with identical barcodes.";
    static final String MAX_CONSENSUSES_PER_CLUSTER = "Maximal number of consensuses generated from 1 cluster. " +
            "Every time this threshold is applied to stop searching for new consensuses, warning will be displayed. " +
            "Too many consensuses per cluster indicate that score threshold, aligner width or skipped fraction to " +
            "repeat is too low.";
    private static final String MIN_GOOD_SEQUENCE_LENGTH = "Minimal length of good sequence that will be still " +
            "considered good after trimming bad quality tails.";
    private static final String AVG_QUALITY_THRESHOLD = "Minimal average quality for bad quality tails trimmer.";
    private static final String TRIM_WINDOW_SIZE = "Window size for bad quality tails trimmer.";
    private static final String FOR_INPUT_READS = " This parameter is for trimming input reads.";
    private static final String FOR_OUTPUT_CONSENSUSES = " This parameter is for trimming output consensuses.";
    static final String READS_MIN_GOOD_SEQUENCE_LENGTH = MIN_GOOD_SEQUENCE_LENGTH + FOR_INPUT_READS;
    static final String READS_AVG_QUALITY_THRESHOLD = AVG_QUALITY_THRESHOLD + FOR_INPUT_READS;
    static final String READS_TRIM_WINDOW_SIZE = TRIM_WINDOW_SIZE + FOR_INPUT_READS;
    static final String CONSENSUSES_MIN_GOOD_SEQUENCE_LENGTH = MIN_GOOD_SEQUENCE_LENGTH + FOR_OUTPUT_CONSENSUSES;
    static final String CONSENSUSES_AVG_QUALITY_THRESHOLD = AVG_QUALITY_THRESHOLD + FOR_OUTPUT_CONSENSUSES;
    static final String CONSENSUSES_TRIM_WINDOW_SIZE = TRIM_WINDOW_SIZE + FOR_OUTPUT_CONSENSUSES;
    static final String ORIGINAL_READ_STATS = "Save extra statistics for each original read into separate file. " +
            "Output file in space separated text format.";
    static final String CONSENSUS_NOT_USED_READS_OUTPUT = "Write reads not used in consensus assembly into separate " +
            "file. Output file in MIF format.";
    static final String CONSENSUSES_TO_SEPARATE_GROUPS = "If this parameter is specified, consensuses will not be " +
            "written as reads R1, R2 etc to output file. Instead, original sequences will be written as R1, R2 etc " +
            "and consensuses will be written as CR1, CR2 etc, so it will be possible to cluster original reads by " +
            "consensuses using filter / demultiplex actions, or export original reads and corresponding " +
            "consensuses into separate reads using mif2fastq action.";
    static final String CONSENSUS_NUMBER_OF_THREADS = "Number of threads for calculating consensus sequences.";
    static final String CONSENSUS_DEBUG_OUTPUT = "Output text file for consensus algorithm debug information.";
    static final String CONSENSUS_DEBUG_QUALITY_THRESHOLD = "Quality threshold to write capital letter in debug " +
            "output file.";
    static final String NUMBER_OF_READS = "Number of reads to take; 0 value means to take the entire input file.";
    static final String MAX_WARNINGS = "Maximum allowed number of warnings; -1 means no limit.";
    static final String REPORT = "File to write report in human readable form. If not specified, report is " +
            "displayed on screen only.";
    static final String STAT_REPORT = "File to write brief command execution stats in human readable form. If not " +
            "specified, these stats are displayed on screen only.";
    static final String JSON_REPORT = "File to write command execution stats in JSON format.";
}
