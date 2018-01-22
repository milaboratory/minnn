package com.milaboratory.mist.parser;

import com.milaboratory.mist.pattern.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.mist.parser.BracketsDetector.*;
import static com.milaboratory.mist.parser.BracketsType.*;
import static com.milaboratory.mist.parser.ParserFormat.*;
import static com.milaboratory.mist.parser.ParserUtils.*;
import static com.milaboratory.mist.parser.SimplifiedParsers.*;
import static com.milaboratory.mist.parser.SimplifiedSyntaxStrings.*;

final class SimplifiedTokenizer extends Tokenizer {
    SimplifiedTokenizer(PatternAligner patternAligner) {
        super(patternAligner);
    }

    /**
     * Convert all tokenizedString contents into pattern. This class is for simplified parser syntax.
     *
     * @param tokenizedString TokenizedString object that was created from query string
     */
    @Override
    void tokenize(TokenizedString tokenizedString) throws ParserException {
        String fullString = tokenizedString.getOneString();
        List<BracketsPair> parenthesesPairs = getAllBrackets(PARENTHESES, fullString);
        List<BracketsPair> squareBracketsPairs = getAllBrackets(SQUARE, fullString);
        ArrayList<ScoreThreshold> scoreThresholds = getScoreThresholds(fullString, SIMPLIFIED);
        ArrayList<ObjectString> objectStrings = new ArrayList<>();

        for (BracketsPair parenthesesPair : parenthesesPairs) {
            if ((parenthesesPair.end == parenthesesPair.start + 1)
                    && !getObjectName(parenthesesPair.start, fullString).equals(ANY_PATTERN_NAME))
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
                case STICK_FILTER_NAME:
                    break;
                case FUZZY_MATCH_PATTERN_NAME:
                    ArrayList<GroupEdgePosition> groupEdgePositionsFuzzy;
                    List<BracketsPair> innerSquareBracketsFuzzy = squareBracketsPairs.stream().
                            filter(bp -> objectString.getParenthesesPair().contains(bp)).collect(Collectors.toList());
                    switch (innerSquareBracketsFuzzy.size()) {
                        case 0:
                            groupEdgePositionsFuzzy = new ArrayList<>();
                            break;
                        case 1:
                            groupEdgePositionsFuzzy = parseArrayOfGroupEdgePositions(tokenizedString,
                                    innerSquareBracketsFuzzy.get(0));
                            break;
                        default:
                            throw new ParserException("Found multiple square bracket pairs in FuzzyMatchPattern!");
                    }
                    String fuzzyMatchPatternString = tokenizedString.getOneString(
                            objectString.getDataStart(), objectString.getDataEnd());
                    FuzzyMatchPattern fuzzyMatchPattern = parseFuzzyMatchPattern(currentPatternAligner,
                            fuzzyMatchPatternString, groupEdgePositionsFuzzy);
                    tokenizedString.tokenizeSubstring(fuzzyMatchPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case REPEAT_PATTERN_NAME:
                    ArrayList<GroupEdgePosition> groupEdgePositionsRepeat;
                    List<BracketsPair> innerSquareBracketsRepeat = squareBracketsPairs.stream().
                            filter(bp -> objectString.getParenthesesPair().contains(bp)).collect(Collectors.toList());
                    switch (innerSquareBracketsRepeat.size()) {
                        case 0:
                            groupEdgePositionsRepeat = new ArrayList<>();
                            break;
                        case 1:
                            groupEdgePositionsRepeat = parseArrayOfGroupEdgePositions(tokenizedString,
                                    innerSquareBracketsRepeat.get(0));
                            break;
                        default:
                            throw new ParserException("Found multiple square bracket pairs in RepeatPattern!");
                    }
                    String repeatPatternString = tokenizedString.getOneString(
                            objectString.getDataStart(), objectString.getDataEnd());
                    RepeatPattern repeatPattern = parseRepeatPattern(currentPatternAligner,
                            repeatPatternString, groupEdgePositionsRepeat);
                    tokenizedString.tokenizeSubstring(repeatPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case ANY_PATTERN_NAME:
                    ArrayList<GroupEdge> groupEdgesAny;
                    List<BracketsPair> innerSquareBracketsAny = squareBracketsPairs.stream().
                            filter(bp -> objectString.getParenthesesPair().contains(bp)).collect(Collectors.toList());
                    switch (innerSquareBracketsAny.size()) {
                        case 0:
                            groupEdgesAny = new ArrayList<>();
                            break;
                        case 1:
                            groupEdgesAny = parseArrayOfGroupEdges(tokenizedString, innerSquareBracketsAny.get(0));
                            break;
                        default:
                            throw new ParserException("Found multiple square bracket pairs in AnyPattern!");
                    }
                    String anyPatternString = tokenizedString.getOneString(
                            objectString.getDataStart(), objectString.getDataEnd());
                    AnyPattern anyPattern = parseAnyPattern(currentPatternAligner, anyPatternString, groupEdgesAny);
                    tokenizedString.tokenizeSubstring(anyPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case AND_PATTERN_NAME:
                    ArrayList<SinglePattern> andPatternOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString, SinglePattern.class);
                    ArrayList<Token> andPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    AndPattern andPattern = parseAndPattern(currentPatternAligner, andPatternTokenizedSubstring,
                            andPatternOperands);
                    tokenizedString.tokenizeSubstring(andPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case PLUS_PATTERN_NAME:
                    ArrayList<SinglePattern> plusPatternOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString, SinglePattern.class);
                    ArrayList<Token> plusPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    PlusPattern plusPattern = parsePlusPattern(currentPatternAligner, plusPatternTokenizedSubstring,
                            plusPatternOperands);
                    tokenizedString.tokenizeSubstring(plusPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case SEQUENCE_PATTERN_NAME:
                    ArrayList<SinglePattern> sequencePatternOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString, SinglePattern.class);
                    ArrayList<Token> sequencePatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    SequencePattern sequencePattern = parseSequencePattern(currentPatternAligner,
                            sequencePatternTokenizedSubstring, sequencePatternOperands);
                    tokenizedString.tokenizeSubstring(sequencePattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case OR_PATTERN_NAME:
                    ArrayList<SinglePattern> orPatternOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString, SinglePattern.class);
                    ArrayList<Token> orPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    OrPattern orPattern = parseOrPattern(currentPatternAligner, orPatternTokenizedSubstring,
                            orPatternOperands);
                    tokenizedString.tokenizeSubstring(orPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case FULL_READ_PATTERN_NAME:
                    ArrayList<Token> fullReadPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    FullReadPattern fullReadPattern = parseFullReadPattern(currentPatternAligner,
                            fullReadPatternTokenizedSubstring);
                    tokenizedString.tokenizeSubstring(fullReadPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case MULTI_PATTERN_NAME:
                    ArrayList<SinglePattern> multiPatternOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString, SinglePattern.class);
                    ArrayList<Token> multiPatternTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    MultiPattern multiPattern = parseMultiPattern(currentPatternAligner, multiPatternTokenizedSubstring,
                            multiPatternOperands);
                    tokenizedString.tokenizeSubstring(multiPattern,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case AND_OPERATOR_NAME:
                    ArrayList<MultipleReadsOperator> andOperatorOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString, MultipleReadsOperator.class);
                    ArrayList<Token> andOperatorTokenizedSubstring = tokenizedString.getTokens(
                            objectString.getDataStart(), objectString.getDataEnd());
                    AndOperator andOperator = parseAndOperator(currentPatternAligner, andOperatorTokenizedSubstring,
                            andOperatorOperands);
                    tokenizedString.tokenizeSubstring(andOperator,
                            objectString.getFullStringStart(), objectString.getFullStringEnd());
                    break;
                case OR_OPERATOR_NAME:
                    ArrayList<MultipleReadsOperator> orOperatorOperands = getPatternOperands(
                            tokenizedString, squareBracketsPairs, objectString, MultipleReadsOperator.class);
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

        Pattern finalPattern = tokenizedString.getFinalPattern();
        if (finalPattern instanceof FullReadPattern)
            ((FullReadPattern)finalPattern).setTargetId((byte)1);
        boolean duplicateGroupsAllowed = (finalPattern instanceof FullReadPattern
                && ((FullReadPattern)finalPattern).getOperand() instanceof OrPattern)
                || finalPattern instanceof OrPattern || finalPattern instanceof OrOperator;
        validateGroupEdges(filterGroupEdgesForValidation(finalPattern.getGroupEdges()), true,
                duplicateGroupsAllowed);
    }

    private <P extends Pattern> ArrayList<P> getPatternOperands(TokenizedString tokenizedString,
            List<BracketsPair> squareBracketsPairs, ObjectString objectString, Class<P> operandClass)
            throws ParserException {
        List<BracketsPair> innerSquareBrackets = squareBracketsPairs.stream()
                .filter(bp -> objectString.getParenthesesPair().contains(bp))
                .sorted(Comparator.comparingInt((BracketsPair bp) -> bp.nestedLevel))
                .collect(Collectors.toList());
        if (innerSquareBrackets.size() == 0)
            throw new ParserException("Missing square bracket pair in " + objectString.getName());
        else
            return parseArrayOfPatterns(tokenizedString, innerSquareBrackets.get(0), operandClass);
    }

    private <P extends Pattern> ArrayList<P> parseArrayOfPatterns(TokenizedString tokenizedString,
            BracketsPair bracketsPair, Class<P> operandClass) throws ParserException {
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
                    patterns.add(token.getSpecificPattern(operandClass));
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

            validateGroupEdgePositions(groupEdgePositions);
            return groupEdgePositions;
        }
    }

    private ArrayList<GroupEdge> parseArrayOfGroupEdges(TokenizedString tokenizedString, BracketsPair bracketsPair)
            throws ParserException {
        if (bracketsPair.bracketsType != SQUARE)
            throw new IllegalArgumentException(
                    "Array of group edges must be in square brackets; got brackets pair of type " + bracketsPair);
        if (bracketsPair.end == bracketsPair.start + 1)
            return new ArrayList<>();
        else {
            ArrayList<GroupEdge> groupEdges = new ArrayList<>();
            String arrayString = tokenizedString.getOneString(bracketsPair.start + 1, bracketsPair.end);
            List<QuotesPair> quotesPairs = getAllQuotes(arrayString);
            int currentPosition = 0;
            int foundTokenPosition;
            do {
                foundTokenPosition = nonQuotedIndexOf(quotesPairs, arrayString, ", " + GROUP_EDGE_START,
                        currentPosition);
                if (foundTokenPosition != -1) {
                    String currentToken = arrayString.substring(currentPosition, foundTokenPosition);
                    groupEdges.add(parseGroupEdge(currentToken));
                    currentPosition = foundTokenPosition + 2;
                }
            } while (foundTokenPosition != -1);
            String lastToken = arrayString.substring(currentPosition);
            groupEdges.add(parseGroupEdge(lastToken));

            validateGroupEdges(groupEdges, true, false);
            return groupEdges;
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
    private PatternAligner getPatternAligner(ArrayList<ScoreThreshold> scoreThresholds, ObjectString objectString) {
        int currentNestedLevel = -1;
        long currentThreshold = 0;
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
