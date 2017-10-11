package com.milaboratory.mist.parser;

import com.milaboratory.mist.pattern.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.mist.parser.BracketsDetector.*;
import static com.milaboratory.mist.parser.BracketsType.*;
import static com.milaboratory.mist.parser.ParserFormat.*;
import static com.milaboratory.mist.parser.ParserUtils.*;

final class NormalTokenizer extends Tokenizer {
    NormalTokenizer(PatternAligner patternAligner) {
        super(patternAligner);
    }

    @Override
    void tokenize(TokenizedString tokenizedString) throws ParserException {
        String fullString = tokenizedString.getOneString();
        List<BracketsPair> parenthesesPairs = getAllBrackets(PARENTHESES, fullString);
        List<BracketsPair> squareBracketsPairs = getAllBrackets(SQUARE, fullString);
        List<BracketsPair> bracesPairs = getAllBrackets(BRACES, fullString);
        List<QuotesPair> quotesPairs = getAllQuotes(fullString);
        ArrayList<Integer> startStickMarkers = getTokenPositions(fullString, "^", quotesPairs);
        ArrayList<Integer> endStickMarkers = getTokenPositions(fullString, "$", quotesPairs);
        ArrayList<ScoreThreshold> scoreThresholds = getScoreThresholds(fullString, NORMAL);
        List<BorderBracesPair> borderBracesPairs = getBorderBraces(fullString, bracesPairs);
        ArrayList<BorderToken> borderTokens = getBorderTokens(fullString, borderBracesPairs);
        List<NormalSyntaxGroupName> groupNames = getGroupNames(fullString, parenthesesPairs);
        groupNames.sort(Comparator.comparingInt(gn -> gn.start));

        NormalParsers normalParsers = new NormalParsers(patternAligner, fullString, squareBracketsPairs,
                startStickMarkers, endStickMarkers, scoreThresholds, borderTokens, groupNames);

        normalParsers.parseRepeatPatterns(getRepeatPatternBraces(bracesPairs, borderBracesPairs))
                .forEach(tokenizedString::tokenizeSubstring);
        normalParsers.parseFuzzyMatchPatterns(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
        normalParsers.parseAnyPatterns(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
        clearGarbageTokens(normalParsers, tokenizedString, false);
        normalParsers.parseScoreFilters(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
        clearGarbageTokens(normalParsers, tokenizedString, false);
        normalParsers.parseSequencePatterns(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
        normalParsers.parseScoreFilters(tokenizedString).forEach(tokenizedString::tokenizeSubstring);

        int maxBracketsNestedLevel = squareBracketsPairs.stream().mapToInt(bp -> bp.nestedLevel).max().orElse(0);

        // single read operators
        for (int currentNestedLevel = maxBracketsNestedLevel; currentNestedLevel >= -1; currentNestedLevel--) {
            for (String operatorRegexp : new String[] {" *\\+ *", " *& *", " *\\|\\| *"}) {
                normalParsers.parseSingleReadOperators(tokenizedString, operatorRegexp, currentNestedLevel)
                        .forEach(tokenizedString::tokenizeSubstring);
                clearGarbageTokens(normalParsers, tokenizedString, false);
                normalParsers.parseScoreFilters(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
                clearGarbageTokens(normalParsers, tokenizedString, false);
            }
            normalParsers.parseSequencePatterns(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
            normalParsers.parseScoreFilters(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
        }

        // MultiPatterns
        tokenizedString.checkNotParsedNullPatterns();
        for (int currentNestedLevel = maxBracketsNestedLevel; currentNestedLevel >= -1; currentNestedLevel--) {
            normalParsers.parseSingleReadOperators(tokenizedString, " *\\\\ *", currentNestedLevel)
                    .forEach(tokenizedString::tokenizeSubstring);
            normalParsers.parseScoreFilters(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
        }

        // multiple reads operators
        normalParsers.wrapWithMultiPatterns(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
        for (int currentNestedLevel = maxBracketsNestedLevel; currentNestedLevel >= -1; currentNestedLevel--) {
            for (String operatorRegexp : new String[]{".*~ *", " *&& *", " *\\|\\| *"}) {
                normalParsers.parseMultiReadOperators(tokenizedString, operatorRegexp, currentNestedLevel)
                        .forEach(tokenizedString::tokenizeSubstring);
                normalParsers.parseScoreFilters(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
            }
            clearGarbageTokens(normalParsers, tokenizedString, true);
        }

        Pattern finalPattern = tokenizedString.getFinalPattern();
        boolean duplicateGroupsAllowed = finalPattern instanceof OrPattern || finalPattern instanceof OrOperator;
        validateGroupEdges(finalPattern.getGroupEdges(), true, duplicateGroupsAllowed);
    }

    /**
     * Run procedures for cleaning space string tokens and null pattern tokens multiple times, while they
     * actually clean tokens.
     *
     * @param normalParsers initialized object of NormalParsers class
     * @param tokenizedString tokenized string to clean from garbage tokens
     * @param spaceStringsOnly true if search only for space strings, false if also search for null patterns
     */
    private static void clearGarbageTokens(NormalParsers normalParsers, TokenizedString tokenizedString,
            boolean spaceStringsOnly) throws ParserException {
        int sizeBeforeCleanup;
        int sizeAfterCleanup;
        do {
            sizeBeforeCleanup = tokenizedString.getSize();
            normalParsers.removeSpaceStringsLeft(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
            normalParsers.removeSpaceStringsRight(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
            if (!spaceStringsOnly) {
                normalParsers.removeNullPatternsLeft(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
                normalParsers.removeNullPatternsRight(tokenizedString).forEach(tokenizedString::tokenizeSubstring);
            }
            sizeAfterCleanup = tokenizedString.getSize();
        } while (sizeAfterCleanup < sizeBeforeCleanup);
    }

    /**
     * Get group names from group parentheses pairs.
     *
     * @param fullString full query string
     * @param parenthesesPairs parentheses pairs
     * @return group names
     */
    private static ArrayList<NormalSyntaxGroupName> getGroupNames(String fullString,
            List<BracketsPair> parenthesesPairs) throws ParserException {
        ArrayList<NormalSyntaxGroupName> groupNames = new ArrayList<>();
        for (BracketsPair parenthesesPair : parenthesesPairs) {
            int colonPosition = fullString.indexOf(":", parenthesesPair.start + 1);
            if (colonPosition == -1)
                throw new ParserException("Missing colon in parentheses pair: "
                        + fullString.substring(parenthesesPair.start, parenthesesPair.end + 1));
            groupNames.add(new NormalSyntaxGroupName(parenthesesPair, fullString.substring(parenthesesPair.start + 1,
                    colonPosition)));
        }
        return groupNames;
    }

    /**
     * Get border tokens from border braces pairs and '<<<'/'>>>' syntax constructions.
     *
     * @param fullString full query string
     * @param borderBracesPairs border braces pairs
     * @return border tokens
     */
    private static ArrayList<BorderToken> getBorderTokens(String fullString, List<BorderBracesPair> borderBracesPairs)
            throws ParserException {
        ArrayList<BorderToken> borderTokens = borderBracesPairs.stream()
                .map(bp -> new BorderToken(bp.leftBorder, bp.numberOfRepeats, bp.start - 1, bp.end + 1))
                .collect(Collectors.toCollection(ArrayList::new));
        char lastTokenChar = '-';   // '<' means left chain, '>' - right chain, '-' - no chain
        int tokenStart = 0;
        int tokenRepeats = 0;
        for (int currentPosition = 0; currentPosition < fullString.length(); currentPosition++) {
            switch (fullString.charAt(currentPosition)) {
                case '<':
                    if (lastTokenChar == '<')
                        tokenRepeats++;
                    else {
                        if (lastTokenChar == '>')
                            borderTokens.add(new BorderToken(false,
                                    tokenRepeats, tokenStart, currentPosition));
                        tokenStart = currentPosition;
                        tokenRepeats = 1;
                        lastTokenChar = '<';
                    }
                    break;
                case '>':
                    if (lastTokenChar == '>')
                        tokenRepeats++;
                    else {
                        if (lastTokenChar == '<')
                            borderTokens.add(new BorderToken(true,
                                    tokenRepeats, tokenStart, currentPosition));
                        tokenStart = currentPosition;
                        tokenRepeats = 1;
                        lastTokenChar = '>';
                    }
                    break;
                case '{':
                    if (tokenRepeats > 1)
                        throw new ParserException("Found multiple '" + lastTokenChar + "' characters before braces!");
                    tokenRepeats = 0;
                    lastTokenChar = '-';
                    break;
                default:
                    if (lastTokenChar == '<') {
                        borderTokens.add(new BorderToken(true, tokenRepeats, tokenStart, currentPosition));
                        tokenRepeats = 0;
                        lastTokenChar = '-';
                    } else if (lastTokenChar == '>') {
                        borderTokens.add(new BorderToken(false, tokenRepeats, tokenStart, currentPosition));
                        tokenRepeats = 0;
                        lastTokenChar = '-';
                    }
            }
        }
        borderTokens.sort(Comparator.comparingInt(bt -> bt.start));
        return borderTokens;
    }
}
