package com.milaboratory.mist.parser;

import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mist.pattern.*;

import java.util.ArrayList;
import java.util.List;

import static com.milaboratory.mist.parser.BracketsDetector.*;
import static com.milaboratory.mist.parser.ParserUtils.checkGroupName;
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
        int commaPositions[] = new int[3];
        NucleotideSequence seq;
        int fixedLeftBorder;
        int fixedRightBorder;

        commaPositions[0] = nonQuotedIndexOf(quotesPairs, str, ", ", 0);
        if (commaPositions[0] == -1)
            throw new ParserException("Missing first ', ' in FuzzyMatchPattern arguments: " + str);
        else if (commaPositions[0] == 0)
            throw new ParserException("Missing nucleotide sequence in FuzzyMatchPattern: " + str);
        commaPositions[1] = nonQuotedIndexOf(quotesPairs, str, ", ", commaPositions[0] + 1);
        if (commaPositions[1] == -1)
            throw new ParserException("Missing second ', ' in FuzzyMatchPattern arguments: " + str);
        commaPositions[2] = nonQuotedIndexOf(quotesPairs, str, ", ", commaPositions[1] + 1);

        try {
            seq = new NucleotideSequence(str.substring(0, commaPositions[0]));
        } catch (IllegalArgumentException e) {
            throw new ParserException("Wrong nucleotide sequence in " + str + ": " + e);
        }

        String fixedLeftBorderSubstring = str.substring(commaPositions[0] + 2, commaPositions[1]);
        try {
            fixedLeftBorder = Integer.parseInt(fixedLeftBorderSubstring);
        } catch (NumberFormatException e) {
            throw new ParserException("Failed to parse fixedLeftBorder (" + fixedLeftBorderSubstring + ") in " + str);
        }

        String fixedRightBorderSubstring = str.substring(commaPositions[1] + 2, (commaPositions[2] == -1)
                ? str.length() : commaPositions[2]);
        try {
            fixedRightBorder = Integer.parseInt(fixedRightBorderSubstring);
        } catch (NumberFormatException e) {
            throw new ParserException("Failed to parse fixedRightBorder (" + fixedRightBorderSubstring + ") in " + str);
        }

        if (commaPositions[2] != -1)
            if ((str.substring(commaPositions[2]).length() < 3)
                    || (!str.substring(commaPositions[2], commaPositions[2] + 3).equals(", [")))
                throw new ParserException("Error while parsing " + str + ": expected ', [', found '"
                        + str.substring(commaPositions[2]) + "'");

        return new FuzzyMatchPattern(patternAligner, seq, fixedLeftBorder, fixedRightBorder, groupEdgePositions);
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
        NucleotideSequence seq;
        int minRepeats;
        int maxRepeats;
        int fixedLeftBorder;
        int fixedRightBorder;

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

        try {
            seq = new NucleotideSequence(str.substring(0, commaPositions[0]));
        } catch (IllegalArgumentException e) {
            throw new ParserException("Wrong nucleotide sequence in " + str + ": " + e);
        }

        String minRepeatsSubstring = str.substring(commaPositions[0] + 2, commaPositions[1]);
        try {
            minRepeats = Integer.parseInt(minRepeatsSubstring);
        } catch (NumberFormatException e) {
            throw new ParserException("Failed to parse minRepeats (" + minRepeatsSubstring + ") in " + str);
        }

        String maxRepeatsSubstring = str.substring(commaPositions[1] + 2, commaPositions[2]);
        try {
            maxRepeats = Integer.parseInt(maxRepeatsSubstring);
        } catch (NumberFormatException e) {
            throw new ParserException("Failed to parse maxRepeats (" + maxRepeatsSubstring + ") in " + str);
        }

        String fixedLeftBorderSubstring = str.substring(commaPositions[2] + 2, commaPositions[3]);
        try {
            fixedLeftBorder = Integer.parseInt(fixedLeftBorderSubstring);
        } catch (NumberFormatException e) {
            throw new ParserException("Failed to parse fixedLeftBorder (" + fixedLeftBorderSubstring + ") in " + str);
        }

        String fixedRightBorderSubstring = str.substring(commaPositions[3] + 2, (commaPositions[4] == -1)
                ? str.length() : commaPositions[4]);
        try {
            fixedRightBorder = Integer.parseInt(fixedRightBorderSubstring);
        } catch (NumberFormatException e) {
            throw new ParserException("Failed to parse fixedRightBorder (" + fixedRightBorderSubstring + ") in " + str);
        }

        if (commaPositions[4] != -1)
            if ((str.substring(commaPositions[4]).length() < 3)
                    || (!str.substring(commaPositions[4], commaPositions[4] + 3).equals(", [")))
                throw new ParserException("Error while parsing " + str + ": expected ', [', found '"
                        + str.substring(commaPositions[4]) + "'");

        return new RepeatPattern(patternAligner, seq, minRepeats, maxRepeats, fixedLeftBorder, fixedRightBorder,
                groupEdgePositions);
    }

    static AnyPattern parseAnyPattern(PatternAligner patternAligner, String str) throws ParserException {
        if (!str.equals(""))
            throw new ParserException("AnyPattern must not have arguments; found: " + str);
        return new AnyPattern(patternAligner);
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
        return new AndPattern(patternAligner, singlePatterns.toArray(new SinglePattern[singlePatterns.size()]));
    }

    static PlusPattern parsePlusPattern(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                        ArrayList<SinglePattern> singlePatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        return new PlusPattern(patternAligner, singlePatterns.toArray(new SinglePattern[singlePatterns.size()]));
    }

    static SequencePattern parseSequencePattern(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                                ArrayList<SinglePattern> singlePatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        return new SequencePattern(patternAligner, singlePatterns.toArray(new SinglePattern[singlePatterns.size()]));
    }

    static OrPattern parseOrPattern(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                    ArrayList<SinglePattern> singlePatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        return new OrPattern(patternAligner, singlePatterns.toArray(new SinglePattern[singlePatterns.size()]));
    }

    static MultiPattern parseMultiPattern(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                          ArrayList<SinglePattern> singlePatterns) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        return new MultiPattern(patternAligner, singlePatterns.toArray(new SinglePattern[singlePatterns.size()]));
    }

    static AndOperator parseAndOperator(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                        ArrayList<MultipleReadsOperator> operands) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        return new AndOperator(patternAligner, operands.toArray(new MultipleReadsOperator[operands.size()]));
    }

    static OrOperator parseOrOperator(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring,
                                      ArrayList<MultipleReadsOperator> operands) throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        return new OrOperator(patternAligner, operands.toArray(new MultipleReadsOperator[operands.size()]));
    }

    static NotOperator parseNotOperator(PatternAligner patternAligner, ArrayList<Token> tokenizedSubstring)
            throws ParserException {
        if (tokenizedSubstring.size() != 1)
            throw new ParserException("Syntax not parsed correctly for Not operator; possibly missing operand: "
                    + tokenizedSubstring);

        return new NotOperator(patternAligner, tokenizedSubstring.get(0).getMultipleReadsOperator());
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
            case BORDER_FILTER_NAME:
                if (multipleReads)
                    throw new ParserException("BorderFilter must not be used with multiple reads!");
                filter = parseBorderFilter(patternAligner, filterString, filterStartingPart);
                break;
            default:
                throw new ParserException("Wrong filter name: " + filterName);
        }

        if (multipleReads)
            return new MultipleReadsFilterPattern(patternAligner, filter, tokenizedSubstring.get(1).getMultipleReadsOperator());
        else
            return new FilterPattern(patternAligner, filter, tokenizedSubstring.get(1).getSinglePattern());
    }

    static ScoreFilter parseScoreFilter(String str, String startingPart) throws ParserException {
        if (!str.substring(0, startingPart.length()).equals(startingPart))
            throw new ParserException("Incorrect ScoreFilter start in " + str + ", expected: " + startingPart);
        if (!str.substring(str.length() - 1).equals(")"))
            throw new ParserException("Missing closing parenthesis in " + str);

        long scoreThreshold;
        try {
            scoreThreshold = Long.parseLong(str.substring(startingPart.length(), str.length() - 1));
        } catch (NumberFormatException e) {
            throw new ParserException("Failed to parse score threshold ("
                    + str.substring(startingPart.length(), str.length() - 1) + ") in " + str);
        }

        return new ScoreFilter(scoreThreshold);
    }

    private static BorderFilter parseBorderFilter(PatternAligner patternAligner, String str, String startingPart)
            throws ParserException {
        if (!str.substring(0, startingPart.length()).equals(startingPart))
            throw new ParserException("Incorrect BorderFilter start in " + str + ", expected: " + startingPart);
        if (!str.substring(str.length() - 1).equals(")"))
            throw new ParserException("Missing closing parenthesis in " + str);

        boolean leftSide;
        NucleotideSequence seq;
        int minNucleotides;
        boolean useTarget;

        int firstCommaPosition = str.indexOf(", ");
        if (firstCommaPosition == -1)
            throw new ParserException("Missing ', ' in " + str);
        if (str.substring(startingPart.length(), firstCommaPosition).equals("true"))
            leftSide = true;
        else if (str.substring(startingPart.length(), firstCommaPosition).equals("false"))
            leftSide = false;
        else
            throw new ParserException("Failed to parse left/right side flag from " + str);

        int secondCommaPosition = str.substring(firstCommaPosition + 1).indexOf(", ") + firstCommaPosition + 1;
        if (secondCommaPosition == -1) {
            try {
                seq = new NucleotideSequence(str.substring(firstCommaPosition + 2, str.length() - 1));
            } catch (IllegalArgumentException e) {
                throw new ParserException("Wrong nucleotide sequence in " + str + ": " + e);
            }
            return new BorderFilter(patternAligner, leftSide, seq);
        } else try {
            seq = new NucleotideSequence(str.substring(firstCommaPosition + 2, secondCommaPosition));
        } catch (IllegalArgumentException e) {
            throw new ParserException("Wrong nucleotide sequence in " + str + ": " + e);
        }

        int thirdCommaPosition = str.substring(secondCommaPosition + 1).indexOf(", ") + secondCommaPosition + 1;
        if (thirdCommaPosition == -1) {
            if (str.substring(secondCommaPosition + 2, str.length() - 1).equals("true"))
                return new BorderFilter(patternAligner, leftSide, seq, true);
            else if (str.substring(secondCommaPosition + 2, str.length() - 1).equals("false"))
                return new BorderFilter(patternAligner, leftSide, seq, false);
            else try {
                minNucleotides = Integer.parseInt(str.substring(secondCommaPosition + 2, str.length() - 1));
            } catch (NumberFormatException e) {
                throw new ParserException("Failed to parse minimum number of nucleotides ("
                        + str.substring(secondCommaPosition + 2, str.length() - 1) + ") in " + str);
            }
            return new BorderFilter(patternAligner, leftSide, seq, minNucleotides);
        } else {
            try {
                minNucleotides = Integer.parseInt(str.substring(secondCommaPosition + 2, thirdCommaPosition));
            } catch (NumberFormatException e) {
                throw new ParserException("Failed to parse minimum number of nucleotides ("
                        + str.substring(secondCommaPosition + 2, thirdCommaPosition) + ") in " + str);
            }
            if (str.substring(thirdCommaPosition + 2, str.length() - 1).equals("true"))
                useTarget = true;
            else if (str.substring(thirdCommaPosition + 2, str.length() - 1).equals("false"))
                useTarget = false;
            else
                throw new ParserException("Failed to parse use motif/target flag from "
                        + str.substring(thirdCommaPosition + 2, str.length() - 1) + " in " + str);
        }

        return new BorderFilter(patternAligner, leftSide, seq, minNucleotides, useTarget);
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

        if (str.substring(firstCommaPosition - 1, secondCommaPosition).equals("', true)"))
            isStart = true;
        else if (str.substring(firstCommaPosition - 1, secondCommaPosition).equals("', false)"))
            isStart = false;
        else
            throw new ParserException("Failed to parse group edge position from " + str);

        if (!str.substring(str.length() - 1).equals(")"))
            throw new ParserException("Missing closing parenthesis in " + str);
        int position;
        try {
            position = Integer.parseInt(str.substring(secondCommaPosition + 2, str.length() - 1));
        } catch (NumberFormatException e) {
            throw new ParserException("Failed to parse position ("
                    + str.substring(secondCommaPosition + 2, str.length() - 1) + ") in " + str);
        }
        if (position < 0)
            throw new ParserException("Position is negative in " + str);

        return new GroupEdgePosition(new GroupEdge(groupName, isStart), position);
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
