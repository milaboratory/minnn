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
package com.milaboratory.minnn.parser;

/**
 * String constants for simplified syntax.
 */
final class SimplifiedSyntaxStrings {
    // characters that can be on left or right of pattern name
    final static String NAME_STOP_CHARACTERS = " ([";

    final static String GROUP_EDGE_NAME = "GroupEdge";
    final static String GROUP_EDGE_POSITION_NAME = "GroupEdgePosition";
    final static String SCORE_FILTER_NAME = "ScoreFilter";
    final static String STICK_FILTER_NAME = "StickFilter";
    final static String FUZZY_MATCH_PATTERN_NAME = "FuzzyMatchPattern";
    final static String REPEAT_PATTERN_NAME = "RepeatPattern";
    final static String REPEAT_N_PATTERN_NAME = "RepeatNPattern";
    final static String ANY_PATTERN_NAME = "AnyPattern";
    final static String AND_PATTERN_NAME = "AndPattern";
    final static String PLUS_PATTERN_NAME = "PlusPattern";
    final static String SEQUENCE_PATTERN_NAME = "SequencePattern";
    final static String FULL_READ_PATTERN_NAME = "FullReadPattern";
    final static String OR_PATTERN_NAME = "OrPattern";
    final static String MULTI_PATTERN_NAME = "MultiPattern";
    final static String AND_OPERATOR_NAME = "AndOperator";
    final static String OR_OPERATOR_NAME = "OrOperator";
    final static String NOT_OPERATOR_NAME = "NotOperator";
    final static String FILTER_PATTERN_NAME = "FilterPattern";
    final static String MULTIPLE_READS_FILTER_PATTERN_NAME = "MultipleReads" + FILTER_PATTERN_NAME;

    final static String GROUP_EDGE_START = GROUP_EDGE_NAME + "('";
    final static String GROUP_EDGE_POSITION_START = GROUP_EDGE_POSITION_NAME + "(" + GROUP_EDGE_START;
    // FILTER_PATTERN_NAME here will also match on MULTIPLE_READS_FILTER_PATTERN_NAME
    final static String SCORE_FILTER_START = FILTER_PATTERN_NAME + "(" + SCORE_FILTER_NAME + "(";
}
