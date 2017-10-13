package com.milaboratory.mist.parser;

import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mist.pattern.*;

import java.util.ArrayList;
import java.util.List;

import static com.milaboratory.mist.parser.BracketsDetector.*;
import static com.milaboratory.mist.parser.ParserUtils.*;
import static com.milaboratory.mist.parser.SimplifiedSyntaxStrings.*;

/**
 * Parsers for objects and their parameters for simplified syntax.
 */
final class SimplifiedParsers {
    /**
     * Parse FuzzyMatchPattern parameters; group edge positions must be already parsed in this stage.
     *
     * @param patternAligner pattern aligner
     * @param str string containing FuzzyMatchPattern arguments which were inside parentheses
     * @param groupEdgePositions parsed group edge positions
     * @return FuzzyMatchPattern
     */
    static FuzzyMatchPattern parseFuzzyMatchPattern(PatternAligner patternAligner, String str,
                                                    ArrayList<GroupEdgePosition> groupEdgePositions) throws ParserException {
        List<QuotesPair> quotesPairs = getAllQuotes(str);
        int commaPositions[] = new int[5];

        commaPositions[0] = nonQuotedIndexOf(quotesPairs, str, ", ", 0);
        if (commaPositions[0] == -1)
            throw new ParserException("Missing first ', ' in FuzzyMatchPattern arguments: " + str);
        else if (commaPositions[0] == 0)
            throw new ParserException("Missing nucleotide sequence in FuzzyMatchPattern: " + str);
        for (int i = 1; i <= 4; i++) {
            commaPositions[i] = nonQuotedIndexOf(quotesPairs, str, ", ", commaPositions[i - 1] + 1);
            if ((i < 4) && (commaPositions[i] == -1))
                throw new ParserException("Missing ', ' with index " + i
                        + " in FuzzyMatchPattern arguments (probably, insufficient arguments): " + str);
        }

        NucleotideSequence seq = toNSeq(str.substring(0, commaPositions[0]));
        int leftCut = toInt(str.substring(commaPositions[0] + 2, commaPositions[1]), "leftCut");
        int rightCut = toInt(str.substring(commaPositions[1] + 2, commaPositions[2]), "rightCut");
        int fixedLeftBorder = toInt(str.substring(commaPositions[2] + 2, commaPositions[3]),
                "fixedLeftBorder");
        int fixedRightBorder = toInt(str.substring(commaPositions[3] + 2,
                (commaPositions[4] == -1) ? str.length() : commaPositions[4]), "fixedRightBorder");

        if (commaPositions[4] != -1)
            if ((str.substring(commaPositions[4]).length() < 3)
                    || (!str.substring(commaPositions[4], commaPositions[4] + 3).equals(", [")))
                throw new ParserException("Error while parsing " + str + ": expected ', [', found '"
                        + str.substring(commaPositions[4]) + "'");

        return new FuzzyMatchPattern(patternAligner, seq, leftCut, rightCut, fixedLeftBorder, fixedRightBorder,
                groupEdgePositions);
    }

    /**
     * Parse RepeatPattern parameters; group edge positions must be already parsed in this stage.
     *
     * @param patternAligner pattern aligner
     * @param str string containing RepeatPattern arguments which were inside parentheses
     * @param groupEdgePositions parsed group edge positions
     * @return RepeatPattern
     */
    static RepeatPattern parseRepeatPattern(PatternAligner patternAligner, String str,
                                            ArrayList<GroupEdgePosition> groupEdgePositions) throws ParserException {
        List<QuotesPair> quotesPairs = getAllQuotes(str);
        int commaPositions[] = new int[5];

        commaPositions[0] = nonQuotedIndexOf(quotesPairs, str, ", ", 0);
        if (commaPositions[0] == -1)
            throw new ParserException("Missing first ', ' in RepeatPattern arguments: " + str);
        else if (commaPositions[0] == 0)
            throw new ParserException("Missing nucleotide sequence in RepeatPattern: " + str);
        for (int i = 1; i <= 4; i++) {
            commaPositions[i] = nonQuotedIndexOf(quotesPairs, str, ", ", commaPositions[i - 1] + 1);
            if ((i < 4) && (commaPositions[i] == -1))
                throw new ParserException("Missing ', ' with index " + i
                        + " in RepeatPattern arguments (probably, insufficient arguments): " + str);
        }

        NucleotideSequence seq = toNSeq(str.substring(0, commaPositions[0]));
        int minRepeats = toInt(str.substring(commaPositions[0] + 2, commaPositions[1]), "minRepeats");
        int maxRepeats = toInt(str.substring(commaPositions[1] + 2, commaPositions[2]), "maxRepeats");
        int fixedLeftBorder = toInt(str.substring(commaPositions[2] + 2, commaPositions[3]),
                "fixedLeftBorder");
        int fixedRightBorder = toInt(str.substring(commaPositions[3] + 2,
                (commaPositions[4] == -1) ? str.length() : commaPositions[4]), "fixedRightBorder");

        if (commaPositions[4] != -1)
            if ((str.substring(commaPositions[4]).length() < 3)
                    || (!str.substring(commaPositions[4], commaPositions[4] + 3).equals(", [")))
                throw new ParserException("Error while parsing " + str + ": expected ', [', found '"
                        + str.substring(commaPositions[4]) + "'");

        return new RepeatPattern(patternAligner, seq, minRepeats, maxRepeats, fixedLeftBorder, fixedRightBorder,
                groupEdgePositions);
    }

    static AnyPattern parseAnyPattern(PatternAligner patternAligner, String str,
                                      ArrayList<GroupEdge> groupEdges) throws ParserException {
        if (!(str.equals("") || ((str.charAt(0) == '[') && (str.charAt(str.length() - 1) == ']'))))
            throw new ParserException("Found unexpected tokens in AnyPattern: " + str);
        return new AnyPattern(patternAligner, groupEdges);
    }

    /**
     * Parse AndPattern from tokenized substring returned by getTokens() function and already parsed operand patterns.
     *
     * @param patternAligner pattern aligner
     * @param tokenizedSubstring tokenized substring for this AndPattern that returned by getTokens() function
     * @param singlePatterns parsed operand patterns
     * @return AndPattern
     */
    static AndPattern parseAndPattern(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                      ArrayList<SinglePattern> singlePatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        SinglePattern[] operands = singlePatterns.toArray(new SinglePattern[singlePatterns.size()]);
        validateGroupEdges(true, false, true, operands);
        return new AndPattern(patternAligner, operands);
    }

    static PlusPattern parsePlusPattern(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                        ArrayList<SinglePattern> singlePatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        SinglePattern[] operands = singlePatterns.toArray(new SinglePattern[singlePatterns.size()]);
        validateGroupEdges(false, false, true, operands);
        return new PlusPattern(patternAligner, operands);
    }

    static SequencePattern parseSequencePattern(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                                ArrayList<SinglePattern> singlePatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        SinglePattern[] operands = singlePatterns.toArray(new SinglePattern[singlePatterns.size()]);
        validateGroupEdges(false, false, true, operands);
        return new SequencePattern(patternAligner, operands);
    }

    static OrPattern parseOrPattern(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                    ArrayList<SinglePattern> singlePatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        SinglePattern[] operands = singlePatterns.toArray(new SinglePattern[singlePatterns.size()]);
        validateGroupEdges(true, true, true, operands);
        return new OrPattern(patternAligner, operands);
    }

    static MultiPattern parseMultiPattern(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                          ArrayList<SinglePattern> singlePatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        SinglePattern[] operands = singlePatterns.toArray(new SinglePattern[singlePatterns.size()]);
        validateGroupEdges(true, false, true, operands);
        return new MultiPattern(patternAligner, operands);
    }

    static AndOperator parseAndOperator(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                        ArrayList<MultipleReadsOperator> multiReadPatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        MultipleReadsOperator[] operands = multiReadPatterns.toArray(new MultipleReadsOperator[multiReadPatterns.size()]);
        validateGroupEdges(true, false, true, operands);
        return new AndOperator(patternAligner, operands);
    }

    static OrOperator parseOrOperator(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                      ArrayList<MultipleReadsOperator> multiReadPatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        MultipleReadsOperator[] operands = multiReadPatterns.toArray(new MultipleReadsOperator[multiReadPatterns.size()]);
        validateGroupEdges(true, true, true, operands);
        return new OrOperator(patternAligner, operands);
    }

    static NotOperator parseNotOperator(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring)
            throws ParserException {
        if (tokenizedSubstring.size() != 1)
            throw new ParserException("Syntax not parsed correctly for Not operator; possibly missing operand: "
                    + tokenizedSubstring);
        MultipleReadsOperator operand = tokenizedSubstring.get(0).getMultipleReadsOperator();
        validateGroupEdges(false, true, false, operand);
        return new NotOperator(patternAligner, operand);
    }

    static Pattern parseFilterPattern(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                      boolean multipleReads) throws ParserException {
        if (tokenizedSubstring.size() != 2)
            throw new ParserException("Syntax not parsed correctly for Filter pattern; possibly missing operand: "
                    + tokenizedSubstring);
        if (!tokenizedSubstring.get(0).isString())
            throw new IllegalArgumentException("Incorrect start in " + tokenizedSubstring + ", expected filter string!");
        String startingSubstring = tokenizedSubstring.get(0).getString();
        if (tokenizedSubstring.get(1).isString())
            throw new IllegalArgumentException("Wrong tokenizedSubstring for FilterPattern: " + tokenizedSubstring);

        if (!startingSubstring.substring(startingSubstring.length() - 2).equals(", "))
            throw new ParserException("Expected ', ' in FilterPattern starting substring, found '"
                    + startingSubstring.substring(startingSubstring.length() - 2) + "'");

        String filterString = startingSubstring.substring(0, startingSubstring.length() - 2);
        int parenthesisPosition = filterString.indexOf("(");
        if (parenthesisPosition == -1)
            throw new ParserException("Missing open parenthesis in filter string: " + filterString);
        String filterName = filterString.substring(0, parenthesisPosition);
        String filterStartingPart = filterName + "(";
        Filter filter;
        switch (filterName) {
            case SCORE_FILTER_NAME:
                filter = parseScoreFilter(filterString, filterStartingPart);
                break;
            default:
                throw new ParserException("Wrong filter name: " + filterName);
        }

        if (multipleReads)
            return new MultipleReadsFilterPattern(patternAligner, filter,
                    tokenizedSubstring.get(1).getMultipleReadsOperator());
        else
            return new FilterPattern(patternAligner, filter, tokenizedSubstring.get(1).getSinglePattern());
    }

    private static ScoreFilter parseScoreFilter(String str, String startingPart) throws ParserException {
        if (!str.substring(0, startingPart.length()).equals(startingPart))
            throw new ParserException("Incorrect ScoreFilter start in " + str + ", expected: " + startingPart);
        if (!str.substring(str.length() - 1).equals(")"))
            throw new ParserException("Missing closing parenthesis in " + str);

        long scoreThreshold = toLong(str.substring(startingPart.length(), str.length() - 1),
                "score threshold");

        return new ScoreFilter(scoreThreshold);
    }

    /**
     * Parse group edge position from string that represents it.
     *
     * @param str string representing 1 group edge position
     * @return parsed GroupEdgePosition
     */
    static GroupEdgePosition parseGroupEdgePosition(String str) throws ParserException {
        if (!str.substring(0, GROUP_EDGE_POSITION_START.length()).equals(GROUP_EDGE_POSITION_START))
            throw new IllegalArgumentException("Incorrect string start in " + str + ", expected: "
                    + GROUP_EDGE_POSITION_START);
        List<QuotesPair> quotesPairs = getAllQuotes(str);

        int firstCommaPosition = nonQuotedIndexOf(quotesPairs, str, ", ", 0);
        if (firstCommaPosition == -1)
            throw new ParserException("Missing ', ' in " + str);
        String groupName = str.substring(GROUP_EDGE_POSITION_START.length(), firstCommaPosition - 1);
        checkGroupName(groupName);

        int secondCommaPosition = nonQuotedIndexOf(quotesPairs, str, ", ", firstCommaPosition + 1);
        if (secondCommaPosition == -1)
            throw new ParserException("Missing second ', ' in " + str);

        boolean isStart;
        switch (str.substring(firstCommaPosition - 1, secondCommaPosition)) {
            case "', true)":
                isStart = true;
                break;
            case "', false)":
                isStart = false;
                break;
            default:
                throw new ParserException("Failed to parse group edge position from " + str);
        }

        if (!str.substring(str.length() - 1).equals(")"))
            throw new ParserException("Missing closing parenthesis in " + str);

        int position = toInt(str.substring(secondCommaPosition + 2, str.length() - 1), "position");
        if (position < 0)
            throw new ParserException("Position is negative in " + str);

        return new GroupEdgePosition(new GroupEdge(groupName, isStart), position);
    }

    /**
     * Parse group edge from string that represents it.
     *
     * @param str string representing 1 group edge
     * @return parsed GroupEdge
     */
    static GroupEdge parseGroupEdge(String str) throws ParserException {
        if (!str.substring(0, GROUP_EDGE_START.length()).equals(GROUP_EDGE_START))
            throw new IllegalArgumentException("Incorrect string start in " + str + ", expected: " + GROUP_EDGE_START);
        List<QuotesPair> quotesPairs = getAllQuotes(str);

        int commaPosition = nonQuotedIndexOf(quotesPairs, str, ", ", 0);
        if (commaPosition == -1)
            throw new ParserException("Missing ', ' in " + str);
        String groupName = str.substring(GROUP_EDGE_START.length(), commaPosition - 1);
        checkGroupName(groupName);

        boolean isStart;
        switch (str.substring(commaPosition - 1, str.length())) {
            case "', true)":
                isStart = true;
                break;
            case "', false)":
                isStart = false;
                break;
            default:
                throw new ParserException("Failed to parse group edge from " + str);
        }

        return new GroupEdge(groupName, isStart);
    }

    private static void checkOperandArraySpelling(ArrayList<Token> tokenizedSubstring) throws ParserException {
        if (tokenizedSubstring.size() < 3)
            throw new IllegalArgumentException("Wrong tokenizedSubstring for array of patterns: " + tokenizedSubstring);
        if (!tokenizedSubstring.get(0).isString())
            throw new IllegalArgumentException("Incorrect string start in " + tokenizedSubstring + ", expected '['");
        else if (!tokenizedSubstring.get(0).getString().equals("["))
            throw new ParserException("Incorrect operand string start: " + tokenizedSubstring.get(0).getString()
                    + ", expected '['");
        if (!tokenizedSubstring.get(tokenizedSubstring.size() - 1).isString())
            throw new IllegalArgumentException("Wrong tokenizedSubstring for array of patterns: " + tokenizedSubstring);
        else if (!tokenizedSubstring.get(tokenizedSubstring.size() - 1).getString().equals("]"))
            throw new ParserException("Found wrong end of operand array string: "
                    + tokenizedSubstring.get(tokenizedSubstring.size() - 1).getString());
        if (tokenizedSubstring.get(1).isString())
            throw new IllegalArgumentException("Wrong tokenizedSubstring for array of patterns: " + tokenizedSubstring);
        for (int i = 2; i < tokenizedSubstring.size() - 1; i++) {
            if (i % 2 == 0) {
                if (!tokenizedSubstring.get(i).isString())
                    throw new IllegalArgumentException("Wrong tokenizedSubstring for array of patterns: " + tokenizedSubstring);
                else if (!tokenizedSubstring.get(i).getString().equals(", "))
                    throw new ParserException("Found wrong delimiter in array of patterns: "
                            + tokenizedSubstring.get(i).getString());
            } else if (tokenizedSubstring.get(i).isString())
                throw new IllegalArgumentException("Wrong tokenizedSubstring for array of patterns: " + tokenizedSubstring);
        }
    }
}
