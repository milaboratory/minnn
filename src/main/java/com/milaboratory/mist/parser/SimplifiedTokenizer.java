package com.milaboratory.mist.parser;

import com.milaboratory.mist.pattern.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.mist.parser.BracketsDetector.*;
import static com.milaboratory.mist.parser.BracketsType.*;
import static com.milaboratory.mist.parser.GroupsChecker.checkGroups;
import static com.milaboratory.mist.parser.ParserFormat.*;
import static com.milaboratory.mist.parser.ParserUtils.getObjectName;
import static com.milaboratory.mist.parser.ParserUtils.getScoreThresholds;
import static com.milaboratory.mist.parser.SimplifiedParsers.*;
import static com.milaboratory.mist.parser.SimplifiedSyntaxStrings.*;

final class SimplifiedTokenizer {
    private final PatternAligner patternAligner;

    SimplifiedTokenizer(PatternAligner patternAligner) {
        this.patternAligner = patternAligner;
    }

    /**
     * Convert all tokenizedString contents into pattern. This class is for simplified parser syntax.
     *
     * @param tokenizedString TokenizedString object that was created from query string
     */
    void tokenize(TokenizedString tokenizedString) throws ParserException {
        String fullString = tokenizedString.getOneString();
        List<BracketsPair> parenthesesPairs = getAllBrackets(PARENTHESES, fullString);
        List<BracketsPair> squareBracketsPairs = getAllBrackets(SQUARE, fullString);
        ArrayList<ScoreThreshold> scoreThresholds = getScoreThresholds(fullString, SIMPLIFIED);
        checkGroups(fullString, SIMPLIFIED);
        ArrayList<ObjectString> objectStrings = new ArrayList<>();

        for (BracketsPair parenthesesPair : parenthesesPairs) {
            if (parenthesesPair.end == parenthesesPair.start + 1)
                throw new ParserException("Found empty parentheses: "
                        + parenthesesPair.start + ", " + parenthesesPair.end + "; argument list must not be empty!");
            objectStrings.add(new ObjectString(getObjectName(parenthesesPair.start, fullString), parenthesesPair));
        }
        objectStrings.sort(Comparator.comparingInt(ObjectString::getNestedLevel).reversed());

        for (ObjectString objectString : objectStrings) {
            PatternAligner currentPatternAligner = getPatternAligner(scoreThresholds, objectString);
            switch (objectString.getName()) {
                case GROUP_EDGE_NAME:
                case GROUP_EDGE_POSITION_NAME:
                case SCORE_FILTER_NAME:
                case BORDER_FILTER_NAME:
                    break;
                case FUZZY_MATCH_PATTERN_NAME:
                    ArrayList<GroupEdgePosition> groupEdgePositions;
                    List<BracketsPair> innerSquareBrackets = squareBracketsPairs.stream().
                            filter(bp -> objectString.getParenthesesPair().contains(bp)).collect(Collectors.toList());
                    switch (innerSquareBrackets.size()) {
                        case 0:
                            groupEdgePositions = new ArrayList<>();
                            break;
                        case 1:
                            groupEdgePositions = parseArrayOfGroupEdgePositions(tokenizedString, innerSquareBrackets.get(0));
                            break;
                        default:
                            throw new ParserException("Found multiple square bracket pairs in FuzzyMatchPattern!");
                    }
                    String fuzzyMatchPatternString = tokenizedString.getOneString(
                            objectString.getDataStart(), objectString.getDataEnd());
                    FuzzyMatchPattern fuzzyMatchPattern = parseFuzzyMatchPattern(currentPatternAligner,
                            fuzzyMatchPatternString, groupEdgePositions);
                    tokenizedString.tokenizeSubstring(fuzzyMatchPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case AND_PATTERN_NAME:
                    ArrayList<SinglePattern> andPatternOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString);
                    ArrayList<Token> andPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    AndPattern andPattern = parseAndPattern(currentPatternAligner, andPatternTokenizedSubstring,
                            andPatternOperands);
                    tokenizedString.tokenizeSubstring(andPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case PLUS_PATTERN_NAME:
                    ArrayList<SinglePattern> plusPatternOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString);
                    ArrayList<Token> plusPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    PlusPattern plusPattern = parsePlusPattern(currentPatternAligner, plusPatternTokenizedSubstring,
                            plusPatternOperands);
                    tokenizedString.tokenizeSubstring(plusPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case OR_PATTERN_NAME:
                    ArrayList<SinglePattern> orPatternOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString);
                    ArrayList<Token> orPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    OrPattern orPattern = parseOrPattern(currentPatternAligner, orPatternTokenizedSubstring,
                            orPatternOperands);
                    tokenizedString.tokenizeSubstring(orPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case MULTI_PATTERN_NAME:
                    ArrayList<SinglePattern> multiPatternOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString);
                    ArrayList<Token> multiPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    MultiPattern multiPattern = parseMultiPattern(currentPatternAligner, multiPatternTokenizedSubstring,
                            multiPatternOperands);
                    tokenizedString.tokenizeSubstring(multiPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case AND_OPERATOR_NAME:
                    ArrayList<MultipleReadsOperator> andOperatorOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString);
                    ArrayList<Token> andOperatorTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    AndOperator andOperator = parseAndOperator(currentPatternAligner, andOperatorTokenizedSubstring,
                            andOperatorOperands);
                    tokenizedString.tokenizeSubstring(andOperator,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case OR_OPERATOR_NAME:
                    ArrayList<MultipleReadsOperator> orOperatorOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString);
                    ArrayList<Token> orOperatorTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    OrOperator orOperator = parseOrOperator(currentPatternAligner, orOperatorTokenizedSubstring,
                            orOperatorOperands);
                    tokenizedString.tokenizeSubstring(orOperator,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case NOT_OPERATOR_NAME:
                    ArrayList<Token> notOperatorTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    NotOperator notOperator = parseNotOperator(currentPatternAligner, notOperatorTokenizedSubstring);
                    tokenizedString.tokenizeSubstring(notOperator,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case MULTIPLE_READS_FILTER_PATTERN_NAME:
                    ArrayList<Token> mFilterPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    MultipleReadsFilterPattern mFilterPattern = (MultipleReadsFilterPattern)parseFilterPattern(
                            currentPatternAligner, mFilterPatternTokenizedSubstring, true);
                    tokenizedString.tokenizeSubstring(mFilterPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case FILTER_PATTERN_NAME:
                    ArrayList<Token> filterPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    FilterPattern filterPattern = (FilterPattern)parseFilterPattern(currentPatternAligner,
                            filterPatternTokenizedSubstring, false);
                    tokenizedString.tokenizeSubstring(filterPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                default:
                    throw new ParserException("Found wrong object name: " + objectString.getName());
            }
        }
    }

    private <P extends Pattern> ArrayList<P> getPatternOperands(TokenizedString tokenizedString,
            List<BracketsPair> squareBracketsPairs, ObjectString objectString) throws ParserException {
        List<BracketsPair> innerSquareBrackets = squareBracketsPairs.stream().
                filter(bp -> objectString.getParenthesesPair().contains(bp)).collect(Collectors.toList());
        innerSquareBrackets.sort(Comparator.comparingInt((BracketsPair bp) -> bp.nestedLevel));
        if (innerSquareBrackets.size() == 0)
            throw new ParserException("Missing square bracket pair in " + objectString.getName());
        else
            return parseArrayOfPatterns(tokenizedString, innerSquareBrackets.get(0));
    }

    private <P extends Pattern> ArrayList<P> parseArrayOfPatterns(TokenizedString tokenizedString,
            BracketsPair bracketsPair) throws ParserException {
        if (bracketsPair.bracketsType != SQUARE)
            throw new IllegalArgumentException("Array of patterns must be in square brackets; got brackets pair: "
                    + bracketsPair);
        if (bracketsPair.end == bracketsPair.start + 1)
            throw new ParserException("Expected array of patterns, found empty square brackets pair: " + bracketsPair);
        else {
            ArrayList<P> patterns = new ArrayList<>();
            for (Token token : tokenizedString.getTokens(bracketsPair.start + 1, bracketsPair.end)) {
                if (token.isString()) {
                    if (!token.getString().equals(", "))
                        throw new ParserException("Found not parsed token in array of patterns: " + token.getString());
                } else
                    patterns.add(token.getSpecificPattern());
            }
            return patterns;
        }
    }

    private ArrayList<GroupEdgePosition> parseArrayOfGroupEdgePositions(
            TokenizedString tokenizedString, BracketsPair bracketsPair) throws ParserException {
        if (bracketsPair.bracketsType != SQUARE)
            throw new IllegalArgumentException(
                    "Array of group edge positions must be in square brackets; got brackets pair of type "
                    + bracketsPair);
        if (bracketsPair.end == bracketsPair.start + 1)
            return new ArrayList<>();
        else {
            ArrayList<GroupEdgePosition> groupEdgePositions = new ArrayList<>();
            String arrayString = tokenizedString.getOneString(bracketsPair.start + 1, bracketsPair.end);
            List<QuotesPair> quotesPairs = getAllQuotes(arrayString);
            int currentPosition = 0;
            int foundTokenPosition;
            do {
                foundTokenPosition = nonQuotedIndexOf(quotesPairs, arrayString, ", " + GROUP_EDGE_POSITION_START,
                        currentPosition);
                if (foundTokenPosition != -1) {
                    String currentToken = arrayString.substring(currentPosition, foundTokenPosition);
                    groupEdgePositions.add(parseGroupEdgePosition(currentToken));
                    currentPosition = foundTokenPosition + 2;
                }
            } while (foundTokenPosition != -1);
            String lastToken = arrayString.substring(currentPosition);
            groupEdgePositions.add(parseGroupEdgePosition(lastToken));

            return groupEdgePositions;
        }
    }

    /**
     * Find score threshold for specified object and return PatternAligner with this threshold. If there is no score
     * threshold, return pattern aligner without changing its threshold.
     *
     * @param scoreThresholds score thresholds list
     * @param objectString string of the object for which we calculate score threshold
     * @return PatternAligner with updated score threshold for the specified object
     */
    private PatternAligner getPatternAligner(ArrayList<ScoreThreshold> scoreThresholds, ObjectString objectString)
            throws ParserException {
        int currentNestedLevel = -1;
        int currentThreshold = 0;
        for (ScoreThreshold scoreThreshold : scoreThresholds)
            if (scoreThreshold.contains(objectString.getFullStringStart(), objectString.getFullStringEnd())
                    && (scoreThreshold.nestedLevel > currentNestedLevel)) {
                currentNestedLevel = scoreThreshold.nestedLevel;
                currentThreshold = scoreThreshold.threshold;
            }
        if (currentNestedLevel == -1)
            return patternAligner;
        else
            return patternAligner.overridePenaltyThreshold(currentThreshold);
    }

    private static class ObjectString {
        private String name;
        private BracketsPair parenthesesPair;

        ObjectString(String name, BracketsPair parenthesesPair) {
            this.name = name;
            this.parenthesesPair = parenthesesPair;
        }

        String getName() {
            return name;
        }

        BracketsPair getParenthesesPair() {
            return parenthesesPair;
        }

        int getDataStart() {
            return parenthesesPair.start + 1;
        }

        int getDataEnd() {
            return parenthesesPair.end;
        }

        int getFullStringStart() {
            return parenthesesPair.start - name.length();
        }

        int getFullStringEnd() {
            return parenthesesPair.end + 1;
        }

        int getNestedLevel() {
            return parenthesesPair.nestedLevel;
        }

        @Override
        public String toString() {
            return "ObjectString{" + "name='" + name + "', parenthesesPair=" + parenthesesPair + "}";
        }
    }
}
