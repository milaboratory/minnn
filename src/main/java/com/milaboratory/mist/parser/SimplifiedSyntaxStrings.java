package com.milaboratory.mist.parser;

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
