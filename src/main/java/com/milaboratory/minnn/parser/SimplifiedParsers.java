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
package com.milaboratory.minnn.parser;

import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import com.milaboratory.minnn.pattern.*;

import java.util.*;

import static com.milaboratory.minnn.parser.BracketsDetector.*;
import static com.milaboratory.minnn.parser.ParserUtils.*;
import static com.milaboratory.minnn.parser.SimplifiedSyntaxStrings.*;

/**
 * Parsers for objects and their parameters for simplified syntax.
 */
final class SimplifiedParsers {
    /**
     * Parse FuzzyMatchPattern parameters; group edge positions must be already parsed in this stage.
     *
     * @param conf parser configuration
     * @param str string containing FuzzyMatchPattern arguments which were inside parentheses
     * @param groupEdgePositions parsed group edge positions
     * @return FuzzyMatchPattern
     */
    static FuzzyMatchPattern parseFuzzyMatchPattern(
            ParserConfiguration conf, String str, ArrayList<GroupEdgePosition> groupEdgePositions)
            throws ParserException {
        List<QuotesPair> quotesPairs = getAllQuotes(str);
        int[] commaPositions = new int[5];

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

        NucleotideSequenceCaseSensitive seq = toNSeq(str.substring(0, commaPositions[0]));
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

        return new FuzzyMatchPattern(conf.getPatternConfiguration(), seq, leftCut, rightCut, fixedLeftBorder,
                fixedRightBorder, groupEdgePositions);
    }

    /**
     * Parse RepeatPattern parameters; group edge positions must be already parsed in this stage.
     *
     * @param patternAligner pattern aligner
     * @param defaultGroupsOverride true if there is default groups override in any pattern in the query
     * @param str string containing RepeatPattern arguments which were inside parentheses
     * @param groupEdgePositions parsed group edge positions
     * @return RepeatPattern
     */
    static RepeatPattern parseRepeatPattern(PatternAligner patternAligner, boolean defaultGroupsOverride, String str,
                                            ArrayList<GroupEdgePosition> groupEdgePositions) throws ParserException {
        List<QuotesPair> quotesPairs = getAllQuotes(str);
        int[] commaPositions = new int[5];

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

        NucleotideSequenceCaseSensitive seq = toNSeq(str.substring(0, commaPositions[0]));
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

        return new RepeatPattern(patternAligner, defaultGroupsOverride, seq, minRepeats, maxRepeats,
                fixedLeftBorder, fixedRightBorder, groupEdgePositions);
    }

    static AnyPattern parseAnyPattern(PatternAligner patternAligner, boolean defaultGroupsOverride, String str,
                                      ArrayList<GroupEdge> groupEdges) throws ParserException {
        if (!(str.equals("") || ((str.charAt(0) == '[') && (str.charAt(str.length() - 1) == ']'))))
            throw new ParserException("Found unexpected tokens in AnyPattern: " + str);
        return new AnyPattern(patternAligner, defaultGroupsOverride, groupEdges);
    }

    static FullReadPattern parseFullReadPattern(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                                ArrayList<Token> tokenizedSubstring)
            throws ParserException {
        if ((tokenizedSubstring.size() != 1) || !tokenizedSubstring.get(0).isPatternAndNotNull()) {
            throw new ParserException("Invalid tokens as arguments for FullReadPattern: " + tokenizedSubstring);
        }
        return new FullReadPattern(patternAligner, defaultGroupsOverride,
                tokenizedSubstring.get(0).getSinglePattern());
    }

    /**
     * Parse AndPattern from tokenized substring returned by getTokens() function and already parsed operand patterns.
     *
     * @param patternAligner pattern aligner
     * @param defaultGroupsOverride true if there is default groups override in any pattern in the query
     * @param tokenizedSubstring tokenized substring for this AndPattern that returned by getTokens() function
     * @param singlePatterns parsed operand patterns
     * @return AndPattern
     */
    static AndPattern parseAndPattern(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                      ArrayList<Token> tokenizedSubstring, ArrayList<SinglePattern> singlePatterns)
            throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        SinglePattern[] operands = singlePatterns.toArray(new SinglePattern[singlePatterns.size()]);
        validateGroupEdges(true, false, true, defaultGroupsOverride, operands);
        return new AndPattern(patternAligner, defaultGroupsOverride, operands);
    }

    static PlusPattern parsePlusPattern(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                        ArrayList<Token> tokenizedSubstring, ArrayList<SinglePattern> singlePatterns)
            throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        SinglePattern[] operands = singlePatterns.toArray(new SinglePattern[singlePatterns.size()]);
        validateGroupEdges(false, false, true, defaultGroupsOverride, operands);
        return new PlusPattern(patternAligner, defaultGroupsOverride, operands);
    }

    static SequencePattern parseSequencePattern(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                                ArrayList<Token> tokenizedSubstring,
                                                ArrayList<SinglePattern> singlePatterns)
            throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        SinglePattern[] operands = singlePatterns.toArray(new SinglePattern[singlePatterns.size()]);
        validateGroupEdges(false, false, true, defaultGroupsOverride, operands);
        return new SequencePattern(patternAligner, defaultGroupsOverride, operands);
    }

    static OrPattern parseOrPattern(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                    ArrayList<Token> tokenizedSubstring, ArrayList<SinglePattern> singlePatterns)
            throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        SinglePattern[] operands = singlePatterns.toArray(new SinglePattern[singlePatterns.size()]);
        validateGroupEdges(true, true, true, defaultGroupsOverride, operands);
        return new OrPattern(patternAligner, defaultGroupsOverride, operands);
    }

    static MultiPattern parseMultiPattern(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                          ArrayList<Token> tokenizedSubstring, ArrayList<SinglePattern> singlePatterns)
            throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        for (SinglePattern singlePattern : singlePatterns)
            if (!(singlePattern instanceof FullReadPattern))
                throw new ParserException("Excepted FullReadPattern argument for MultiPattern, got "
                        + singlePattern);
        SinglePattern[] operands = singlePatterns.toArray(new SinglePattern[singlePatterns.size()]);
        validateGroupEdges(true, false, true, defaultGroupsOverride, operands);
        return new MultiPattern(patternAligner, defaultGroupsOverride, operands);
    }

    static AndOperator parseAndOperator(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                        ArrayList<Token> tokenizedSubstring,
                                        ArrayList<MultipleReadsOperator> multiReadPatterns)
            throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        MultipleReadsOperator[] operands = multiReadPatterns
                .toArray(new MultipleReadsOperator[multiReadPatterns.size()]);
        validateGroupEdges(true, false, true, defaultGroupsOverride, operands);
        return new AndOperator(patternAligner, defaultGroupsOverride, operands);
    }

    static OrOperator parseOrOperator(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                      ArrayList<Token> tokenizedSubstring,
                                      ArrayList<MultipleReadsOperator> multiReadPatterns)
            throws ParserException {
        checkOperandArraySpelling(tokenizedSubstring);
        MultipleReadsOperator[] operands = multiReadPatterns
                .toArray(new MultipleReadsOperator[multiReadPatterns.size()]);
        validateGroupEdges(true, true, true, defaultGroupsOverride, operands);
        return new OrOperator(patternAligner, defaultGroupsOverride, operands);
    }

    static NotOperator parseNotOperator(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                        ArrayList<Token> tokenizedSubstring)
            throws ParserException {
        if (tokenizedSubstring.size() != 1)
            throw new ParserException("Syntax not parsed correctly for Not operator; possibly missing operand: "
                    + tokenizedSubstring);
        MultipleReadsOperator operand = tokenizedSubstring.get(0).getMultipleReadsOperator();
        validateGroupEdges(false, true, false, defaultGroupsOverride, operand);
        return new NotOperator(patternAligner, defaultGroupsOverride, operand);
    }

    static Pattern parseFilterPattern(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                      ArrayList<Token> tokenizedSubstring, boolean multipleReads)
            throws ParserException {
        if (tokenizedSubstring.size() != 2)
            throw new ParserException("Syntax not parsed correctly for Filter pattern; possibly missing operand: "
                    + tokenizedSubstring);
        if (!tokenizedSubstring.get(0).isString())
            throw new IllegalArgumentException("Incorrect start in " + tokenizedSubstring
                    + ", expected filter string!");
        String startingSubstring = tokenizedSubstring.get(0).getString();
        if (tokenizedSubstring.get(1).isString())
            throw new IllegalArgumentException("Wrong tokenizedSubstring for FilterPattern: " + tokenizedSubstring);

        String expectedComma = startingSubstring.substring(startingSubstring.length() - 2);
        if (!expectedComma.equals(", "))
            throw new ParserException("Expected ', ' in FilterPattern starting substring, found '"
                    + expectedComma + "'");

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
            case STICK_FILTER_NAME:
                filter = parseStickFilter(filterString, filterStartingPart);
                break;
            default:
                throw new ParserException("Wrong filter name: " + filterName);
        }

        if (multipleReads)
            return new MultipleReadsFilterPattern(patternAligner, defaultGroupsOverride, filter,
                    tokenizedSubstring.get(1).getMultipleReadsOperator());
        else
            return new FilterPattern(patternAligner, defaultGroupsOverride, filter,
                    tokenizedSubstring.get(1).getSinglePattern());
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

    private static StickFilter parseStickFilter(String str, String startingPart) throws ParserException {
        if (!str.substring(0, startingPart.length()).equals(startingPart))
            throw new ParserException("Incorrect StickFilter start in " + str + ", expected: " + startingPart);
        if (!str.substring(str.length() - 1).equals(")"))
            throw new ParserException("Missing closing parenthesis in " + str);

        int commaPosition = nonQuotedIndexOf(getAllQuotes(str), str, ", ", 0);
        if (commaPosition == -1)
            throw new ParserException("Missing ', ' in " + str);

        boolean left;
        switch (str.substring(startingPart.length(), commaPosition)) {
            case "true":
                left = true;
                break;
            case "false":
                left = false;
                break;
            default:
                throw new ParserException("Failed to parse stick filter left side flag from " + str);
        }

        int position = toInt(str.substring(commaPosition + 2, str.length() - 1), "position");
        if (position < 0)
            throw new ParserException("Position is negative in " + str);

        return new StickFilter(left, position);
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
        switch (str.substring(commaPosition - 1)) {
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
            throw new IllegalArgumentException("Wrong tokenizedSubstring for array of patterns: "
                    + tokenizedSubstring);
        if (!tokenizedSubstring.get(0).isString())
            throw new IllegalArgumentException("Incorrect string start in " + tokenizedSubstring + ", expected '['");
        else if (!tokenizedSubstring.get(0).getString().equals("["))
            throw new ParserException("Incorrect operand string start: " + tokenizedSubstring.get(0).getString()
                    + ", expected '['");
        if (!tokenizedSubstring.get(tokenizedSubstring.size() - 1).isString())
            throw new IllegalArgumentException("Wrong tokenizedSubstring for array of patterns: "
                    + tokenizedSubstring);
        else if (!tokenizedSubstring.get(tokenizedSubstring.size() - 1).getString().equals("]"))
            throw new ParserException("Found wrong end of operand array string: "
                    + tokenizedSubstring.get(tokenizedSubstring.size() - 1).getString());
        if (tokenizedSubstring.get(1).isString())
            throw new IllegalArgumentException("Wrong tokenizedSubstring for array of patterns: "
                    + tokenizedSubstring);
        for (int i = 2; i < tokenizedSubstring.size() - 1; i++) {
            if (i % 2 == 0) {
                if (!tokenizedSubstring.get(i).isString())
                    throw new IllegalArgumentException("Wrong tokenizedSubstring for array of patterns: "
                            + tokenizedSubstring);
                else if (!tokenizedSubstring.get(i).getString().equals(", "))
                    throw new ParserException("Found wrong delimiter in array of patterns: "
                            + tokenizedSubstring.get(i).getString());
            } else if (tokenizedSubstring.get(i).isString())
                throw new IllegalArgumentException("Wrong tokenizedSubstring for array of patterns: "
                        + tokenizedSubstring);
        }
    }
}
