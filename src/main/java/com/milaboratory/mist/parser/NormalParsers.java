package com.milaboratory.mist.parser;

import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mist.pattern.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.milaboratory.mist.parser.BracketsType.*;
import static com.milaboratory.mist.parser.ParserUtils.*;

/**
 * Parsers for objects and their parameters for normal syntax.
 */
final class NormalParsers {
    private final PatternAligner patternAligner;
    private final String query;
    private final List<BracketsPair> squareBracketsPairs;
    private final ArrayList<Integer> startStickMarkers;
    private final ArrayList<Integer> endStickMarkers;
    private final ArrayList<ScoreThreshold> scoreThresholds;
    private final ArrayList<BorderToken> borderTokens;
    private final List<NormalSyntaxGroupName> groupNames;

    NormalParsers(PatternAligner patternAligner, String query, List<BracketsPair> squareBracketsPairs,
                  ArrayList<Integer> startStickMarkers, ArrayList<Integer> endStickMarkers,
                  ArrayList<ScoreThreshold> scoreThresholds, ArrayList<BorderToken> borderTokens,
                  List<NormalSyntaxGroupName> groupNames) {
        this.patternAligner = patternAligner;
        this.query = query;
        this.squareBracketsPairs = squareBracketsPairs;
        this.startStickMarkers = startStickMarkers;
        this.endStickMarkers = endStickMarkers;
        this.scoreThresholds = scoreThresholds;
        this.borderTokens = borderTokens;
        this.groupNames = groupNames;
    }

    ArrayList<FoundToken> parseRepeatPatterns(List<BracketsPair> repeatPatternBracesPairs) throws ParserException {
        // must be changed to Integer.MAX_VALUE when bitap wrapper for longer sequences will be implemented
        final int MAX_REPEATS = 63;

        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        for (BracketsPair bracesPair : repeatPatternBracesPairs) {
            if (bracesPair.start == 0)
                throw new ParserException("Found '{' without nucleotide in the start of query!");
            String arguments = query.substring(bracesPair.start + 1, bracesPair.end);
            NucleotideSequence patternSeq = toNSeq(query.substring(bracesPair.start - 1, bracesPair.start));

            int startStickPosition = findStartStick(bracesPair.start - 1);
            int endStickPosition = findEndStick(bracesPair.end);
            int fixedLeftBorder = -1;
            int fixedRightBorder = -1;
            if (startStickPosition != -1) {
                foundTokens.add(new FoundToken(null, startStickPosition, startStickPosition + 1));
                fixedLeftBorder = 0;
            }
            if (endStickPosition != -1) {
                foundTokens.add(new FoundToken(null, endStickPosition, endStickPosition + 1));
                fixedRightBorder = -2;
            }

            int minRepeats = 1;
            int maxRepeats = MAX_REPEATS;
            if (arguments.length() == 0)
                throw new ParserException("Missing number of repeats in " + query.substring(bracesPair.start - 1,
                        bracesPair.end + 1));
            else if (arguments.equals("*"))
                minRepeats = 1;
            else if (!arguments.contains(":"))
                minRepeats = toInt(arguments, "number of repeats");
            else {
                if (arguments.indexOf(":") != 0)
                    minRepeats = toInt(arguments.substring(0, arguments.indexOf(":")),
                            "minimum number of repeats");
                if (arguments.indexOf(":") != arguments.length() - 1)
                    maxRepeats = toInt(arguments.substring(arguments.indexOf(":") + 1),
                            "maximum number of repeats");
            }

            List<FoundGroupEdgePosition> foundGroupEdgePositions = new ArrayList<>();
            foundGroupEdgePositions.addAll(findGroupsOnBorder(bracesPair.start - 1, true, MAX_REPEATS));
            foundGroupEdgePositions.addAll(findGroupsOnBorder(bracesPair.end, false, MAX_REPEATS));
            foundGroupEdgePositions.forEach(fe -> foundTokens.add(new FoundToken(null, fe.start, fe.end)));
            ArrayList<GroupEdgePosition> groupEdgePositions = foundGroupEdgePositions.stream()
                    .map(fe -> fe.groupEdgePosition).collect(Collectors.toCollection(ArrayList::new));
            validateGroupEdgePositions(groupEdgePositions);

            foundTokens.add(new FoundToken(new RepeatPattern(getPatternAligner(bracesPair.start - 1, bracesPair.end + 1),
                    patternSeq, minRepeats, maxRepeats, fixedLeftBorder, fixedRightBorder, groupEdgePositions),
                    bracesPair.start - 1, bracesPair.end + 1));
        }

        return foundTokens;
    }

    ArrayList<FoundToken> parseFuzzyMatchPatterns(TokenizedString tokenizedString) throws ParserException {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        List<Token> stringTokens = tokenizedString.getTokens(0, tokenizedString.getFullLength()).stream()
                .filter(Token::isString).collect(Collectors.toList());
        for (Token currentStringToken : stringTokens) {
            Matcher regexMatcher = Pattern.compile("[a-zA-Z]+((\\( *\\w *: *[a-zA-Z ]+ *\\))*[a-zA-Z]+)*")
                    .matcher(currentStringToken.getString());
            while (regexMatcher.find()) {
                int start = regexMatcher.start() + currentStringToken.getStartCoordinate();
                int end = regexMatcher.end() + currentStringToken.getStartCoordinate();
                String nucleotideString = cutGroupsAndSpaces(start, end);
                // ignore group names that matched as nucleotide sequences
                if (nucleotideString.isEmpty())
                    continue;
                NucleotideSequence patternSeq = toNSeq(nucleotideString);

                List<FoundGroupEdgePosition> foundGroupEdgePositions = findGroupsForFuzzyPattern(start, end);
                foundGroupEdgePositions.stream().filter(fe -> (fe.start != -1) && (fe.end != -1))
                        .forEach(fe -> foundTokens.add(new FoundToken(null, fe.start, fe.end)));
                ArrayList<GroupEdgePosition> groupEdgePositions = foundGroupEdgePositions.stream()
                        .map(fe -> fe.groupEdgePosition).collect(Collectors.toCollection(ArrayList::new));
                validateGroupEdgePositions(groupEdgePositions);

                int foundLeftCutIndex = findLeftCut(start);
                int foundRightCutIndex = findRightCut(end - 1);
                int foundLeftCut = 0;
                int foundRightCut = 0;
                if (foundLeftCutIndex != -1) {
                    BorderToken leftBorderToken = borderTokens.get(foundLeftCutIndex);
                    foundTokens.add(new FoundToken(null, leftBorderToken.start, leftBorderToken.end));
                    foundLeftCut = leftBorderToken.numberOfRepeats;
                }
                if (foundRightCutIndex != -1) {
                    BorderToken rightBorderToken = borderTokens.get(foundRightCutIndex);
                    foundTokens.add(new FoundToken(null, rightBorderToken.start, rightBorderToken.end));
                    foundRightCut = rightBorderToken.numberOfRepeats;
                }

                int startStickPosition = findStartStick(start);
                int endStickPosition = findEndStick(end - 1);
                int fixedLeftBorder = -1;
                int fixedRightBorder = -1;
                if (startStickPosition != -1) {
                    foundTokens.add(new FoundToken(null, startStickPosition, startStickPosition + 1));
                    fixedLeftBorder = 0;
                }
                if (endStickPosition != -1) {
                    foundTokens.add(new FoundToken(null, endStickPosition, endStickPosition + 1));
                    fixedRightBorder = -2;
                }

                foundTokens.add(new FoundToken(new FuzzyMatchPattern(getPatternAligner(start, end), patternSeq,
                        foundLeftCut, foundRightCut, fixedLeftBorder, fixedRightBorder, groupEdgePositions),
                        start, end));
            }
        }

        return foundTokens;
    }

    ArrayList<FoundToken> parseAnyPatterns(TokenizedString tokenizedString) throws ParserException {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        List<Token> stringTokens = tokenizedString.getTokens(0, tokenizedString.getFullLength()).stream()
                .filter(Token::isString).collect(Collectors.toList());
        for (int i = 0; i < stringTokens.size(); i++) {
            Token currentStringToken = stringTokens.get(i);
            String currentString = currentStringToken.getString();
            int asteriskPosition = currentString.indexOf("*");
            if (asteriskPosition != -1) {
                int start = asteriskPosition + currentStringToken.getStartCoordinate();
                int index = tokenizedString.getIndexByPosition(start);
                if (((i != 0) && !currentString.substring(0, start).contains("\\"))
                        || ((i != stringTokens.size() - 1) && !currentString.substring(start).contains("\\"))
                        || ((i == 0) && (index != 0))
                        || ((i == stringTokens.size() - 1) && (index != tokenizedString.getSize() - 1))
                        || (currentString.indexOf("*", start + 1) != -1))
                    throw new ParserException("'*' pattern is invalid if there are other patterns in the same read, "
                            + "use 'N{*}' instead!");

                List<FoundGroupEdgePosition> foundGroupEdgePositions = new ArrayList<>();
                foundGroupEdgePositions.addAll(findGroupsOnBorder(start, true, 0));
                foundGroupEdgePositions.addAll(findGroupsOnBorder(start, false, 0));
                ArrayList<GroupEdge> groupEdges = foundGroupEdgePositions.stream()
                        .map(fe -> fe.groupEdgePosition.getGroupEdge())
                        .collect(Collectors.toCollection(ArrayList::new));
                validateGroupEdges(groupEdges, true, false);
                foundGroupEdgePositions.forEach(fe -> foundTokens.add(new FoundToken(null, fe.start, fe.end)));

                foundTokens.add(new FoundToken(new AnyPattern(patternAligner, groupEdges), start, start + 1));
            }
        }

        return foundTokens;
    }

    /**
     * This function will remove space strings on the left by merging them into patterns on the right from them.
     *
     * @param tokenizedString tokenized string object for query string
     * @return list of enlarged pattern tokens that will overwrite neighbor space strings
     */
    ArrayList<FoundToken> removeSpaceStringsLeft(TokenizedString tokenizedString) {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        ArrayList<Token> tokens = tokenizedString.getTokens(0, tokenizedString.getFullLength());
        for (int i = 0; i < tokens.size() - 1; i++) {
            Token currentToken = tokens.get(i);
            if (currentToken.isString()
                    && currentToken.getString().replace(" ", "").equals("")) {
                Token nextToken = tokens.get(i + 1);
                foundTokens.add(new FoundToken(nextToken.getPattern(), currentToken.getStartCoordinate(),
                        nextToken.getStartCoordinate() + nextToken.getLength()));
            }
        }

        return foundTokens;
    }

    /**
     * This function will remove space strings on the right by merging them into patterns on the left from them.
     *
     * @param tokenizedString tokenized string object for query string
     * @return list of enlarged pattern tokens that will overwrite neighbor space strings
     */
    ArrayList<FoundToken> removeSpaceStringsRight(TokenizedString tokenizedString) {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        ArrayList<Token> tokens = tokenizedString.getTokens(0, tokenizedString.getFullLength());
        for (int i = 1; i < tokens.size(); i++) {
            Token currentToken = tokens.get(i);
            if (currentToken.isString()
                    && currentToken.getString().replace(" ", "").equals("")) {
                Token previousToken = tokens.get(i - 1);
                foundTokens.add(new FoundToken(previousToken.getPattern(), previousToken.getStartCoordinate(),
                        currentToken.getStartCoordinate() + currentToken.getLength()));
            }
        }

        return foundTokens;
    }

    /**
     * This function will remove null patterns on the left by merging them into neighbor patterns on the right.
     * Null patterns are used for substrings that are parts of already parsed tokens, but are not placed
     * directly near them.
     *
     * @param tokenizedString tokenized string object for query string
     * @return list of enlarged pattern tokens that will overwrite neighbor null patterns
     */
    ArrayList<FoundToken> removeNullPatternsLeft(TokenizedString tokenizedString) {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        ArrayList<Token> tokens = tokenizedString.getTokens(0, tokenizedString.getFullLength());
        for (int i = 0; i < tokens.size() - 1; i++) {
            Token currentToken = tokens.get(i);
            if (!currentToken.isString() && (currentToken.getPattern() == null)) {
                Token nextToken = tokens.get(i + 1);
                if (!nextToken.isString() && (nextToken.getPattern() != null))
                    foundTokens.add(new FoundToken(nextToken.getPattern(),
                            currentToken.getStartCoordinate(),
                            nextToken.getStartCoordinate() + nextToken.getLength()));
            }
        }

        return foundTokens;
    }

    /**
     * This function will remove null patterns on the right by merging them into neighbor patterns on the left.
     * Null patterns are used for substrings that are parts of already parsed tokens, but are not placed
     * directly near them.
     *
     * @param tokenizedString tokenized string object for query string
     * @return list of enlarged pattern tokens that will overwrite neighbor null patterns
     */
    ArrayList<FoundToken> removeNullPatternsRight(TokenizedString tokenizedString) {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        ArrayList<Token> tokens = tokenizedString.getTokens(0, tokenizedString.getFullLength());
        for (int i = 1; i < tokens.size(); i++) {
            Token currentToken = tokens.get(i);
            if (!currentToken.isString() && (currentToken.getPattern() == null)) {
                Token previousToken = tokens.get(i - 1);
                if (!previousToken.isString() && (previousToken.getPattern() != null))
                    foundTokens.add(new FoundToken(previousToken.getPattern(),
                            previousToken.getStartCoordinate(),
                            currentToken.getStartCoordinate() + currentToken.getLength()));
            }
        }

        return foundTokens;
    }

    /**
     * This function will parse sequences of already parsed patterns. It will be called multiple times.
     *
     * @param tokenizedString tokenized string object for query string
     * @return list of found tokens
     */
    ArrayList<FoundToken> parseSequencePatterns(TokenizedString tokenizedString) throws ParserException {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        ArrayList<Token> tokens = tokenizedString.getTokens(0, tokenizedString.getFullLength());
        boolean sequenceStarted = false;
        int sequenceStart = 0;
        for (int i = 0; i <= tokens.size(); i++) {
            if ((i == tokens.size()) || tokens.get(i).isString()) {
                if (sequenceStarted) {
                    if (sequenceStart < i - 1) {
                        SinglePattern[] operands = new SinglePattern[i - sequenceStart];
                        for (int j = sequenceStart; j < i; j++)
                            operands[j - sequenceStart] = tokens.get(j).getSinglePattern();
                        int sequenceTokenStart = tokens.get(sequenceStart).getStartCoordinate();
                        int sequenceTokenEnd = (i == tokens.size()) ? tokenizedString.getFullLength()
                                : tokens.get(i).getStartCoordinate();
                        validateGroupEdges(false, false, true, operands);
                        foundTokens.add(new FoundToken(new SequencePattern(getPatternAligner(sequenceTokenStart,
                                sequenceTokenEnd), operands), sequenceTokenStart, sequenceTokenEnd));
                    }
                    sequenceStarted = false;
                }
            } else if (!sequenceStarted) {
                sequenceStart = i;
                sequenceStarted = true;
            }
        }

        return foundTokens;
    }

    /**
     * Parse score filters and remove square brackets around completely parsed patterns by merging them into patterns.
     * It will parse score filters only around completely parsed patterns, and it will be called multiple times.
     *
     * @param tokenizedString tokenized string object for query string
     * @return list of found tokens
     */
    ArrayList<FoundToken> parseScoreFilters(TokenizedString tokenizedString) throws ParserException {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        ArrayList<Token> tokens = tokenizedString.getTokens(0, tokenizedString.getFullLength());
        for (int i = 1; i < tokens.size() - 1; i++) {
            Token previousToken = tokens.get(i - 1);
            Token currentToken = tokens.get(i);
            Token nextToken = tokens.get(i + 1);
            if (!currentToken.isString() && previousToken.isString() && nextToken.isString()) {
                String previousString = previousToken.getString();
                String nextString = nextToken.getString();
                boolean bracketsFound = false;
                int bracketsRelativeStart = 0;
                int bracketsRelativeEnd = 0;
                int leftTokenizedCharactersNum = 0;
                int rightTokenizedCharactersNum = 0;
                boolean noMoreNestedBrackets = false;
                while (!noMoreNestedBrackets) {
                    Matcher previousMatcher = Pattern.compile(" *\\[( *-?\\d+ *:)? *$").matcher(previousString);
                    if (previousMatcher.find()) {
                        Matcher nextMatcher = Pattern.compile("^ *] *").matcher(nextString);
                        if (nextMatcher.find()) {
                            bracketsFound = true;
                            bracketsRelativeStart = previousMatcher.start();
                            bracketsRelativeEnd = nextMatcher.end();
                            leftTokenizedCharactersNum += previousString.length() - bracketsRelativeStart;
                            rightTokenizedCharactersNum += bracketsRelativeEnd;
                            previousString = previousString.substring(0, bracketsRelativeStart);
                            nextString = nextString.substring(bracketsRelativeEnd);
                        } else
                            noMoreNestedBrackets = true;
                    } else
                        noMoreNestedBrackets = true;
                }
                if (bracketsFound) {
                    int lastFoundTokenEnd = (foundTokens.size() > 0) ? foundTokens.get(foundTokens.size() - 1).to : 0;
                    int tokenStart = Math.max(currentToken.getStartCoordinate() - leftTokenizedCharactersNum,
                            lastFoundTokenEnd);
                    int tokenEnd = nextToken.getStartCoordinate() + rightTokenizedCharactersNum;
                    FoundToken foundToken;
                    List<ScoreThreshold> foundScoreThresholds = scoreThresholds.stream()
                            .filter(st -> (st.start >= tokenStart) && (st.start < currentToken.getStartCoordinate()))
                            .collect(Collectors.toList());
                    if (!foundScoreThresholds.isEmpty()) {
                        long scoreThreshold = foundScoreThresholds.stream().mapToLong(st -> st.threshold).max()
                                .orElseThrow(IllegalStateException::new);
                        foundToken = new FoundToken(SinglePattern.class.isAssignableFrom(
                                currentToken.getPattern().getClass())
                                ? wrapWithScoreFilter(currentToken.getSinglePattern(), scoreThreshold)
                                : wrapWithScoreFilter(currentToken.getMultipleReadsOperator(), scoreThreshold),
                                tokenStart, tokenEnd);
                    } else
                        foundToken = new FoundToken(currentToken.getPattern(), tokenStart, tokenEnd);
                    foundTokens.add(foundToken);
                }
            }
        }

        return foundTokens;
    }

    /**
     * This function will parse operators with specified sign that are in brackets with specified nested level.
     * It will be called from loop with decreasing nested level, once for each operator on every nested
     * level, starting from operators with higher priority. Nested level -1 means to parse operators outside
     * of brackets. Then this function will be called once more for each nested level to parse MultiPatterns.
     *
     * @param tokenizedString tokenized string object for query string
     * @param operatorRegexp regular expression for operator sign
     * @param nestedLevel current nested level of brackets where to parse; -1 means to parse outside of brackets
     * @return list of found tokens
     */
    ArrayList<FoundToken> parseSingleReadOperators(TokenizedString tokenizedString, String operatorRegexp,
            int nestedLevel) throws ParserException {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        List<BracketsPair> bracketsWithThisNestedLevel = (nestedLevel == -1)
                ? Collections.singletonList(new BracketsPair(SQUARE, -1, query.length(), -1))
                : squareBracketsPairs.stream().filter(bp -> bp.nestedLevel == nestedLevel).collect(Collectors.toList());
        for (BracketsPair currentBrackets : bracketsWithThisNestedLevel) {
            ArrayList<Token> tokens = tokenizedString.getTokens(getBracketsContentStart(currentBrackets),
                    currentBrackets.end);
            boolean sequenceStarted = false;
            int sequenceStart = 0;
            for (int i = 0; i <= tokens.size(); i++) {
                if ((i == tokens.size()) || (tokens.get(i).isString()
                        && !tokens.get(i).getString().matches(operatorRegexp))) {
                    if (sequenceStarted) {
                        if (sequenceStart < i - 2) {
                            int numOperands = (i - sequenceStart + 1) / 2;
                            SinglePattern[] operands = new SinglePattern[numOperands];
                            for (int j = 0; j < numOperands; j++)
                                operands[j] = tokens.get(sequenceStart + j * 2).getSinglePattern();
                            int sequenceTokenStart = tokens.get(sequenceStart).getStartCoordinate();
                            int sequenceTokenEnd = tokens.get(i - 1).getStartCoordinate()
                                    + tokens.get(i - 1).getLength();
                            FoundToken foundToken;
                            if (operatorRegexp.contains("+")) {
                                validateGroupEdges(false, false, true, operands);
                                foundToken = new FoundToken(new PlusPattern(getPatternAligner(sequenceTokenStart,
                                        sequenceTokenEnd), operands), sequenceTokenStart, sequenceTokenEnd);
                            } else if (operatorRegexp.contains("&")) {
                                validateGroupEdges(true, false, true, operands);
                                foundToken = new FoundToken(new AndPattern(getPatternAligner(sequenceTokenStart,
                                        sequenceTokenEnd), operands), sequenceTokenStart, sequenceTokenEnd);
                            } else if (operatorRegexp.contains("|")) {
                                validateGroupEdges(true, true, true, operands);
                                foundToken = new FoundToken(new OrPattern(getPatternAligner(sequenceTokenStart,
                                        sequenceTokenEnd), operands), sequenceTokenStart, sequenceTokenEnd);
                            } else if (operatorRegexp.contains("\\\\")) {
                                validateGroupEdges(true, false, true, operands);
                                foundToken = new FoundToken(new MultiPattern(getPatternAligner(sequenceTokenStart,
                                        sequenceTokenEnd), operands), sequenceTokenStart, sequenceTokenEnd);
                            } else
                                throw new IllegalArgumentException("Invalid operator regexp: " + operatorRegexp);
                            foundTokens.add(foundToken);
                        }
                        sequenceStarted = false;
                    }
                } else if (!sequenceStarted && !tokens.get(i).isString()) {
                    sequenceStart = i;
                    sequenceStarted = true;
                }
            }
        }

        return foundTokens;
    }

    /**
     * This function will parse multiple read operators with specified sign that are in brackets with specified
     * nested level. It will be called from loop with decreasing nested level, once for each operator on every nested
     * level, starting from operators with higher priority. Nested level -1 means to parse operators outside
     * of brackets.
     *
     * @param tokenizedString tokenized string object for query string
     * @param operatorRegexp regular expression for operator sign
     * @param nestedLevel current nested level of brackets where to parse; -1 means to parse outside of brackets
     * @return list of found tokens
     */
    ArrayList<FoundToken> parseMultiReadOperators(TokenizedString tokenizedString, String operatorRegexp,
            int nestedLevel) throws ParserException {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        List<BracketsPair> bracketsWithThisNestedLevel = (nestedLevel == -1)
                ? Collections.singletonList(new BracketsPair(SQUARE, -1, query.length(), -1))
                : squareBracketsPairs.stream().filter(bp -> bp.nestedLevel == nestedLevel).collect(Collectors.toList());
        for (BracketsPair currentBrackets : bracketsWithThisNestedLevel) {
            ArrayList<Token> tokens = tokenizedString.getTokens(getBracketsContentStart(currentBrackets),
                    currentBrackets.end);
            boolean sequenceStarted = false;
            int sequenceStart = 0;
            for (int i = 0; i <= tokens.size(); i++) {
                /* for NotOperator, every single pattern will be treated as sequence, and then it will be checked
                   that previous token matches operatorRegexp */
                if ((i == tokens.size()) || (tokens.get(i).isString()
                        && !tokens.get(i).getString().matches(operatorRegexp))
                        || (sequenceStarted && operatorRegexp.contains("~"))) {
                    if (sequenceStarted) {
                        if (operatorRegexp.contains("~")) {
                            if ((i > 1) && tokens.get(i - 2).isString() &&
                                    tokens.get(i - 2).getString().matches(operatorRegexp)) {
                                int tokenStart = tokens.get(i - 2).getStartCoordinate();
                                int tokenEnd = tokens.get(i - 1).getStartCoordinate() + tokens.get(i - 1).getLength();
                                MultipleReadsOperator operand = tokens.get(i - 1).getMultipleReadsOperator();
                                validateGroupEdges(false, true, false, operand);
                                foundTokens.add(new FoundToken(new NotOperator(getPatternAligner(tokenStart, tokenEnd),
                                        operand), tokenStart, tokenEnd));
                            }
                        } else if (sequenceStart < i - 2) {
                            int numOperands = (i - sequenceStart + 1) / 2;
                            MultipleReadsOperator[] operands = new MultipleReadsOperator[numOperands];
                            for (int j = 0; j < numOperands; j++)
                                operands[j] = tokens.get(sequenceStart + j * 2).getMultipleReadsOperator();
                            int sequenceTokenStart = tokens.get(sequenceStart).getStartCoordinate();
                            int sequenceTokenEnd = tokens.get(i - 1).getStartCoordinate()
                                    + tokens.get(i - 1).getLength();
                            MultipleReadsOperator multipleReadsOperator;
                            if (operatorRegexp.contains("&")) {
                                validateGroupEdges(true, false, true, operands);
                                multipleReadsOperator = new AndOperator(getPatternAligner(sequenceTokenStart,
                                        sequenceTokenEnd), operands);
                            } else if (operatorRegexp.contains("|")) {
                                validateGroupEdges(true, true, true, operands);
                                multipleReadsOperator = new OrOperator(getPatternAligner(sequenceTokenStart,
                                        sequenceTokenEnd), operands);
                            } else
                                throw new IllegalArgumentException("Invalid operator regexp: " + operatorRegexp);
                            foundTokens.add(new FoundToken(multipleReadsOperator, sequenceTokenStart, sequenceTokenEnd));
                        }
                        sequenceStarted = false;
                    }
                } else if (!sequenceStarted && !tokens.get(i).isString()) {
                    sequenceStart = i;
                    sequenceStarted = true;
                }
            }
        }

        return foundTokens;
    }

    /**
     * Return list of group edges: left edges on the left of this pattern (if onLeft == true),
     * or right edges on the right of this pattern (if onLeft == false), without any patterns between this
     * pattern and group edges.
     *
     * @param position position where to start the search; exclusive
     * @param onLeft true to search group names on the left from this pattern, false to search group closing parentheses
     *               on the right from this pattern
     * @param patternLength length of the pattern; used as coordinate for group edges on the right border
     * @return parsed GroupEdgePosition objects, and their coordinates in query (to tokenize them as null patterns)
     */
    private List<FoundGroupEdgePosition> findGroupsOnBorder(int position, boolean onLeft, int patternLength) {
        List<FoundGroupEdgePosition> foundGroupEdgePositions = new ArrayList<>();
        int currentPosition = position;
        int closestGroupIndex = getClosestGroupByPosition(position, onLeft);
        while (closestGroupIndex != -1) {
            NormalSyntaxGroupName currentGroupName = groupNames.get(closestGroupIndex);
            String intermediateSubstring = onLeft ? query.substring(currentGroupName.end + 1,
                    currentPosition) : query.substring(currentPosition + 1, currentGroupName.bracketsPair.end);
            if (intermediateSubstring.matches(".*[a-zA-Z()*].*"))
                break;
            currentPosition = onLeft ? currentGroupName.start : currentGroupName.bracketsPair.end;
            foundGroupEdgePositions.add(new FoundGroupEdgePosition(new GroupEdgePosition(new GroupEdge(
                 currentGroupName.name, onLeft), onLeft ? 0 : patternLength),
                    onLeft ? currentGroupName.start : currentGroupName.bracketsPair.end,
                    onLeft ? currentGroupName.end + 1 : currentGroupName.bracketsPair.end + 1));
            closestGroupIndex = getClosestGroupByPosition(currentPosition, onLeft);
        }
        return foundGroupEdgePositions;
    }

    /**
     * Find closest index in group names list by position in query string; return -1 if index not found.
     *
     * @param position position in query string, exclusive (index on this position will not be returned)
     * @param toLeft true: search group name to the left from position,
     *               false: search group closing parenthesis to the right
     * @return found index in list of group names, or -1 if not found
     */
    private int getClosestGroupByPosition(int position, boolean toLeft) {
        for (int i = 0; i < groupNames.size(); i++) {
            if (toLeft && (position > groupNames.get(i).end)
                    && ((i == groupNames.size() - 1) || (position <= groupNames.get(i + 1).start)))
                return i;
            if (!toLeft && (position < groupNames.get(i).bracketsPair.end)
                    && ((i == groupNames.size() - 1) || (position >= groupNames.get(i + 1).bracketsPair.end)))
                return i;
        }
        return -1;
    }

    /**
     * Get group edge positions for FuzzyMatchPattern from specified range in query string, including groups on left
     * and right edges; and positions of starts and ends of group edges on borders, to tokenize them as null patterns.
     *
     * @param start start of FuzzyMatchPattern, inclusive
     * @param end end of FuzzyMatchPattern, exclusive
     * @return found group edge positions and their coordinates in query
     */
    private List<FoundGroupEdgePosition> findGroupsForFuzzyPattern(int start, int end) {
        List<FoundGroupEdgePosition> foundGroupEdgePositions = new ArrayList<>();
        int patternStringLength = end - start;
        int ignoredCharactersCount = 0;
        int ignoredCharactersSearchPosition = start;
        for (NormalSyntaxGroupName groupName : groupNames) {
            int groupStart = groupName.start - start;
            int groupNameEnd = groupName.end - start;
            int groupEnd = groupName.bracketsPair.end - start;
            if ((groupStart > 0) && (groupStart < patternStringLength - 1)) {
                if (groupNameEnd < patternStringLength - 1) {
                    ignoredCharactersCount += countCharacters(query.substring(ignoredCharactersSearchPosition,
                            groupName.start), ' ');
                    ignoredCharactersSearchPosition = groupName.end;
                    foundGroupEdgePositions.add(new FoundGroupEdgePosition(new GroupEdgePosition(new GroupEdge(
                            groupName.name, true), groupStart - ignoredCharactersCount), -1, -1));
                    ignoredCharactersCount += groupNameEnd - groupStart + 1;
                } else
                    throw new IllegalStateException("FuzzyMatchPattern: start=" + start + ", end=" + end
                            + ", group name: start=" + groupName.start + ", end=" + groupName.end);
            }
            if ((groupEnd > 0) && (groupEnd < patternStringLength - 1)) {
                ignoredCharactersCount += countCharacters(query.substring(ignoredCharactersSearchPosition,
                        groupName.bracketsPair.end), ' ');
                ignoredCharactersSearchPosition = groupName.bracketsPair.end;
                foundGroupEdgePositions.add(new FoundGroupEdgePosition(new GroupEdgePosition(new GroupEdge(
                        groupName.name, false), groupEnd - ignoredCharactersCount), -1, -1));
                ignoredCharactersCount++;
            }
        }
        ignoredCharactersCount += countCharacters(query.substring(ignoredCharactersSearchPosition, end), ' ');

        final int patternLength = end - start - ignoredCharactersCount;
        foundGroupEdgePositions.addAll(findGroupsOnBorder(start, true, patternLength));
        foundGroupEdgePositions.addAll(findGroupsOnBorder(end - 1, false, patternLength));

        return foundGroupEdgePositions;
    }

    /**
     * Check is this pattern should stick to start (check "^" before the pattern).
     *
     * @param position pattern start position, inclusive
     * @return "^" marker position if this pattern should stick to target start, otherwise -1
     */
    private int findStartStick(int position) {
        int markerPosition = -1;
        for (int i = 0; i < startStickMarkers.size(); i++) {
            if ((position > startStickMarkers.get(i)) && ((i == startStickMarkers.size() - 1)
                    || (position < startStickMarkers.get(i + 1))))
                markerPosition = startStickMarkers.get(i);
        }

        if ((markerPosition != -1) && !query.substring(markerPosition + 1, position).matches(".*\\\\.*")
                && !isAnyNucleotide(markerPosition + 1, position))
            return markerPosition;
        else
            return -1;
    }

    /**
     * Check is this pattern should stick to end (check "$" after the pattern).
     *
     * @param position pattern end position, inclusive
     * @return "$" marker position if this pattern should stick to target end, otherwise -1
     */
    private int findEndStick(int position) {
        int markerPosition = -1;
        for (int i = 0; i < endStickMarkers.size(); i++) {
            if ((position < endStickMarkers.get(i)) && ((i == 0) || (position > endStickMarkers.get(i - 1))))
                markerPosition = endStickMarkers.get(i);
        }

        if ((markerPosition != -1) && !query.substring(position + 1, markerPosition).matches(".*\\\\.*")
                && !isAnyNucleotide(position + 1, markerPosition))
            return markerPosition;
        else
            return -1;
    }

    /**
     * Search for left BorderToken on the left of this FuzzyMatchPattern.
     *
     * @param position pattern start position, inclusive
     * @return index of found left cut in borderTokens list; or -1 if there is no left cut
     */
    private int findLeftCut(int position) {
        List<BorderToken> leftBorderTokens = new ArrayList<>();
        List<Integer> leftBorderTokensIndexes = new ArrayList<>();
        for (int i = 0; i < borderTokens.size(); i++) {
            BorderToken currentToken = borderTokens.get(i);
            if (currentToken.leftBorder) {
                leftBorderTokens.add(currentToken);
                leftBorderTokensIndexes.add(i);
            }
        }
        int tokenPosition = -1;
        int foundIndex = -1;
        for (int i = 0; i < leftBorderTokens.size(); i++) {
            BorderToken currentToken = leftBorderTokens.get(i);
            if ((position >= currentToken.end) && ((i == leftBorderTokens.size() - 1)
                    || (position < leftBorderTokens.get(i + 1).start))) {
                tokenPosition = currentToken.start;
                foundIndex = leftBorderTokensIndexes.get(i);
            }
        }

        if ((tokenPosition != -1) && !query.substring(tokenPosition, position).matches(".*\\\\.*")
                && !isAnyNucleotide(tokenPosition, position))
            return foundIndex;
        else
            return -1;
    }

    /**
     * Search for right BorderToken on the right of this FuzzyMatchPattern.
     *
     * @param position pattern end position, inclusive
     * @return index of found right cut in borderTokens list; or -1 if there is no right cut
     */
    private int findRightCut(int position) {
        List<BorderToken> rightBorderTokens = new ArrayList<>();
        List<Integer> rightBorderTokensIndexes = new ArrayList<>();
        for (int i = 0; i < borderTokens.size(); i++) {
            BorderToken currentToken = borderTokens.get(i);
            if (!currentToken.leftBorder) {
                rightBorderTokens.add(currentToken);
                rightBorderTokensIndexes.add(i);
            }
        }
        int tokenPosition = -1;
        int foundIndex = -1;
        for (int i = 0; i < rightBorderTokens.size(); i++) {
            BorderToken currentToken = rightBorderTokens.get(i);
            if ((position < currentToken.start) && ((i == 0) || (position > rightBorderTokens.get(i - 1).end))) {
                tokenPosition = currentToken.end;
                foundIndex = rightBorderTokensIndexes.get(i);
            }
        }

        if ((tokenPosition != -1) && !query.substring(position + 1, tokenPosition).matches(".*\\\\.*")
                && !isAnyNucleotide(position + 1, tokenPosition))
            return foundIndex;
        else
            return -1;
    }

    /**
     * Returns true if there is any nucleotide in query substring, otherwise false.
     *
     * @param start substring start position, inclusive
     * @param end substring end position, exclusive
     * @return true if there is any nucleotide in query substring, otherwise false
     */
    private boolean isAnyNucleotide(int start, int end) {
        for (int currentPosition = start; currentPosition < end; currentPosition++) {
            final int position = currentPosition;
            if (query.substring(position, position + 1).matches("[a-zA-Z]")
                    && groupNames.stream().noneMatch(gn -> (gn.start < position) && (gn.end > position)))
                return true;
        }
        return false;
    }

    /**
     * Cuts group start and end tokens and spaces from FuzzyMatchPattern substring.
     *
     * @param start FuzzyMatchPattern start, inclusive
     * @param end FuzzyMatchPattern end, exclusive
     * @return cut string that contains only nucleotide sequence for FuzzyMatchPattern
     */
    private String cutGroupsAndSpaces(int start, int end) {
        StringBuilder result = new StringBuilder();
        for (int currentPosition = start; currentPosition < end; currentPosition++) {
            final int position = currentPosition;
            if ((query.charAt(position) != ' ')
                    && groupNames.stream().noneMatch(gn -> (gn.start <= position) && (gn.end >= position)))
                result.append(query.charAt(position));
        }
        return result.toString();
    }

    private SinglePattern wrapWithScoreFilter(SinglePattern singlePattern, long scoreThreshold) {
        return new FilterPattern(patternAligner, new ScoreFilter(scoreThreshold), singlePattern);
    }

    private MultipleReadsOperator wrapWithScoreFilter(MultipleReadsOperator multiReadPattern, long scoreThreshold) {
        return new MultipleReadsFilterPattern(patternAligner, new ScoreFilter(scoreThreshold), multiReadPattern);
    }

    /**
     * Returns start coordinate of contents of square brackets pair. If there is score filter, it will return
     * the position after colon.
     *
     * @param bracketsPair square brackets pair; can start with -1 if this is virtual brackets pair that includes
     *                     the entire query
     * @return start coordinate of contents of square brackets pair
     */
    private int getBracketsContentStart(BracketsPair bracketsPair) {
        if (bracketsPair.bracketsType != SQUARE)
            throw new IllegalArgumentException("getBracketContentStart called with brackets of type "
                    + bracketsPair.bracketsType);
        boolean isScoreThreshold = scoreThresholds.stream().anyMatch(st -> st.start == bracketsPair.start);
        if (isScoreThreshold) {
            int colonPosition = query.substring(bracketsPair.start + 1, bracketsPair.end).indexOf(":");
            if (colonPosition == -1)
                throw new IllegalStateException("No colon inside brackets with score threshold: "
                        + query.substring(bracketsPair.start + 1, bracketsPair.end));
            else
                return bracketsPair.start + colonPosition + 2;
        } else
            return bracketsPair.start + 1;
    }

    /**
     * Find score threshold for specified range and return PatternAligner with this threshold. If there is no score
     * threshold, return pattern aligner without changing its threshold.
     *
     * @param start start of range, inclusive
     * @param end end of range, exclusive
     * @return PatternAligner with updated score threshold for the specified range
     */
    private PatternAligner getPatternAligner(int start, int end) {
        int currentNestedLevel = -1;
        long currentThreshold = 0;
        for (ScoreThreshold scoreThreshold : scoreThresholds)
            if (scoreThreshold.contains(start, end) && (scoreThreshold.nestedLevel > currentNestedLevel)) {
                currentNestedLevel = scoreThreshold.nestedLevel;
                currentThreshold = scoreThreshold.threshold;
            }
        if (currentNestedLevel == -1)
            return patternAligner;
        else
            return patternAligner.overridePenaltyThreshold(currentThreshold);
    }

    private static class FoundGroupEdgePosition {
        final GroupEdgePosition groupEdgePosition;
        final int start;
        final int end;

        /**
         * Return value for findGroupEdgePositions(): parsed group edge position and its start and end coordinates
         * in query, to tokenize it as null pattern if needed.
         *
         * @param groupEdgePosition group edge position object
         * @param start start coordinate in query, inclusive; or -1 if it is inside the pattern and doesn't need to be
         *              tokenized as null pattern
         * @param end end coordinate in query, exclusive; or -1 if it is inside the pattern and doesn't need to be
         *              tokenized as null pattern
         */
        FoundGroupEdgePosition(GroupEdgePosition groupEdgePosition, int start, int end) {
            this.groupEdgePosition = groupEdgePosition;
            this.start = start;
            this.end = end;
        }
    }
}
