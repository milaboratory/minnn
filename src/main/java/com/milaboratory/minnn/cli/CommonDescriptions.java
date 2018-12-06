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

class CommonDescriptions {
    private CommonDescriptions() {}

    static final String IN_FILE_NO_STDIN = "Input file in \"mif\" format. This argument is required; stdin is not" +
            " supported.";
    static final String IN_FILE_OR_STDIN = "Input file in \"mif\" format. If not specified, stdin will be used.";
    static final String OUT_FILE_OR_STDOUT = "Output file in \"mif\" format. If not specified, stdout will be used.";
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
    static final String NUMBER_OF_READS = "Number of reads to take; 0 value means to take the entire input file.";
    static final String SUPPRESS_WARNINGS = "Don't display any warnings.";
}
