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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.parser.BracketsType.*;
import static com.milaboratory.minnn.parser.ParserUtils.*;

/**
 * Parsers for objects and their parameters for normal syntax.
 */
final class NormalParsers {
    private final ParserConfiguration conf;
    private final String query;
    private final List<BracketsPair> squareBracketsPairs;
    private final ArrayList<Integer> startStickMarkers;
    private final ArrayList<Integer> endStickMarkers;
    private final ArrayList<ScoreThreshold> scoreThresholds;
    private final ArrayList<BorderToken> borderTokens;
    private final List<NormalSyntaxGroupName> groupNames;

    NormalParsers(ParserConfiguration conf, String query, List<BracketsPair> squareBracketsPairs,
                  ArrayList<Integer> startStickMarkers, ArrayList<Integer> endStickMarkers,
                  ArrayList<ScoreThreshold> scoreThresholds, ArrayList<BorderToken> borderTokens,
                  List<NormalSyntaxGroupName> groupNames) {
        this.conf = conf;
        this.query = query;
        this.squareBracketsPairs = squareBracketsPairs;
        this.startStickMarkers = startStickMarkers;
        this.endStickMarkers = endStickMarkers;
        this.scoreThresholds = scoreThresholds;
        this.borderTokens = borderTokens;
        this.groupNames = groupNames;
    }

    ArrayList<FoundToken> parseRepeatPatterns(List<BracketsPair> repeatPatternBracesPairs) throws ParserException {
        final int MAX_REPEATS = Integer.MAX_VALUE;

        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        for (BracketsPair bracesPair : repeatPatternBracesPairs) {
            if (bracesPair.start == 0)
                throw new ParserException("Found '{' without nucleotide in the start of query!");
            String arguments = query.substring(bracesPair.start + 1, bracesPair.end);
            NucleotideSequenceCaseSensitive patternSeq = toNSeq(query.substring(
                    bracesPair.start - 1, bracesPair.start));

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

            int minRepeats;
            int maxRepeats;
            if (arguments.length() == 0)
                throw new ParserException("Missing number of repeats in " + query.substring(bracesPair.start - 1,
                        bracesPair.end + 1));
            else if (arguments.equals("*")) {
                minRepeats = 1;
                maxRepeats = MAX_REPEATS;
            } else if (!arguments.contains(":")) {
                minRepeats = toInt(arguments, "number of repeats");
                maxRepeats = minRepeats;
            } else {
                minRepeats = 1;
                maxRepeats = MAX_REPEATS;
                if (arguments.indexOf(":") != 0)
                    minRepeats = toInt(arguments.substring(0, arguments.indexOf(":")),
                            "minimum number of repeats");
                if (arguments.indexOf(":") != arguments.length() - 1)
                    maxRepeats = toInt(arguments.substring(arguments.indexOf(":") + 1),
                            "maximum number of repeats");
            }
            if ((minRepeats < 1) || (maxRepeats < 1))
                throw new ParserException("Number of repeats must not be less than 1; found: min "
                        + minRepeats + ", max " + maxRepeats);
            if (minRepeats > maxRepeats)
                throw new ParserException("Minimum number of repeats (" + minRepeats + ") is bigger than maximum ("
                        + maxRepeats + ")!");

            List<FoundGroupEdgePosition> foundGroupEdgePositions = new ArrayList<>();
            foundGroupEdgePositions.addAll(findGroupsOnBorder(bracesPair.start - 1, true, MAX_REPEATS));
            foundGroupEdgePositions.addAll(findGroupsOnBorder(bracesPair.end, false, MAX_REPEATS));
            foundGroupEdgePositions.forEach(fe -> foundTokens.add(new FoundToken(null, fe.start, fe.end)));
            ArrayList<GroupEdgePosition> groupEdgePositions = foundGroupEdgePositions.stream()
                    .map(fe -> fe.groupEdgePosition).collect(Collectors.toCollection(ArrayList::new));
            validateGroupEdgePositions(groupEdgePositions);

            foundTokens.add(new FoundToken(new RepeatPattern(getPatternConfiguration(
                    bracesPair.start - 1, bracesPair.end + 1), patternSeq, minRepeats, maxRepeats,
                    fixedLeftBorder, fixedRightBorder, groupEdgePositions),
                    bracesPair.start - 1, bracesPair.end + 1));
        }

        return foundTokens;
    }

    ArrayList<FoundToken> parseFuzzyMatchPatterns(TokenizedString tokenizedString) throws ParserException {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        List<Token> stringTokens = tokenizedString.getTokens(0, tokenizedString.getFullLength()).stream()
                .filter(Token::isString).collect(Collectors.toList());
        for (Token currentStringToken : stringTokens) {
            Matcher regexMatcher = Pattern.compile("[a-zA-Z]+(((\\( *[a-zA-Z0-9]+ *:|\\)| )[a-zA-Z ]*)*[a-zA-Z]+)*")
                    .matcher(currentStringToken.getString());
            while (regexMatcher.find()) {
                int start = regexMatcher.start() + currentStringToken.getStartCoordinate();
                int end = regexMatcher.end() + currentStringToken.getStartCoordinate();
                String nucleotideString = cutGroupsAndSpaces(start, end);
                // ignore group names that matched as nucleotide sequences
                if (nucleotideString.isEmpty())
                    continue;
                NucleotideSequenceCaseSensitive patternSeq = toNSeq(nucleotideString);

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
                if (foundLeftCut + foundRightCut >= patternSeq.size())
                    throw new ParserException("Number of cut nucleotides must be less than pattern length; found: "
                        + "pattern: " + patternSeq + ", left cut: " + foundLeftCut + ", right cut: " + foundRightCut);

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

                foundTokens.add(new FoundToken(new FuzzyMatchPattern(getPatternConfiguration(start, end), patternSeq,
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
            while (asteriskPosition != -1) {
                int start = asteriskPosition + currentStringToken.getStartCoordinate();
                if (((i != 0) && !currentString.substring(0, asteriskPosition).matches(".*(&&|\\|\\||\\\\).*"))
                        || ((i != stringTokens.size() - 1) && !currentString.substring(asteriskPosition)
                            .matches(".*(&&|\\|\\||\\\\).*")))
                    throw new ParserException("'*' pattern is invalid if there are other patterns in the same read, "
                            + "use 'n{*}' instead!");

                List<FoundGroupEdgePosition> foundGroupEdgePositions = new ArrayList<>();
                foundGroupEdgePositions.addAll(findGroupsOnBorder(start, true, 0));
                foundGroupEdgePositions.addAll(findGroupsOnBorder(start, false, 0));
                ArrayList<GroupEdge> groupEdges = foundGroupEdgePositions.stream()
                        .map(fe -> fe.groupEdgePosition.getGroupEdge())
                        .collect(Collectors.toCollection(ArrayList::new));
                validateGroupEdges(groupEdges, true, false);
                foundGroupEdgePositions.forEach(fe -> foundTokens.add(new FoundToken(null, fe.start, fe.end)));

                foundTokens.add(new FoundToken(new AnyPattern(conf.getPatternConfiguration(), groupEdges),
                        start, start + 1));
                asteriskPosition = (asteriskPosition == currentString.length() - 1) ? -1
                        : currentString.indexOf("*", asteriskPosition + 1);
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
                foundTokens.add(new FoundToken(nextToken.getNullablePattern(), currentToken.getStartCoordinate(),
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
                foundTokens.add(new FoundToken(previousToken.getNullablePattern(), previousToken.getStartCoordinate(),
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
            if (!currentToken.isString() && (currentToken.getNullablePattern() == null)) {
                Token nextToken = tokens.get(i + 1);
                if (nextToken.isPatternAndNotNull())
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
            if (!currentToken.isString() && (currentToken.getNullablePattern() == null)) {
                Token previousToken = tokens.get(i - 1);
                if (previousToken.isPatternAndNotNull())
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
            if ((i == tokens.size()) || !tokens.get(i).isPatternAndNotNull()) {
                if (sequenceStarted) {
                    if (sequenceStart < i - 1) {
                        SinglePattern[] operands = new SinglePattern[i - sequenceStart];
                        for (int j = sequenceStart; j < i; j++)
                            operands[j - sequenceStart] = tokens.get(j).getSinglePatternExceptAnyPattern();
                        int sequenceTokenStart = tokens.get(sequenceStart).getStartCoordinate();
                        int sequenceTokenEnd = (i == tokens.size()) ? tokenizedString.getFullLength()
                                : tokens.get(i).getStartCoordinate();
                        validateGroupEdges(false, false, true,
                                conf.isDefaultGroupsOverride(), operands);
                        foundTokens.add(new FoundToken(new SequencePattern(getPatternConfiguration(
                                sequenceTokenStart, sequenceTokenEnd), operands),
                                sequenceTokenStart, sequenceTokenEnd));
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
     * Parse score and stick filters; and remove square brackets, ^ and $ tokens around completely parsed patterns
     * by merging them into patterns. Also call fixBorder() for operand patterns if needed.
     * It will parse filters only around completely parsed patterns, and it will be called multiple times.
     *
     * @param tokenizedString tokenized string object for query string
     * @param scoreOnly if true, parse score filters only; don't search for ^ and $ tokens
     * @return list of found tokens
     */
    ArrayList<FoundToken> parseFilters(TokenizedString tokenizedString, boolean scoreOnly) throws ParserException {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        ArrayList<Token> tokens = tokenizedString.getTokens(0, tokenizedString.getFullLength());
        for (int i = 1; i < tokens.size() - 1; i++) {
            Token previousToken = tokens.get(i - 1);
            Token currentToken = tokens.get(i);
            Token nextToken = tokens.get(i + 1);
            if (currentToken.isPatternAndNotNull() && previousToken.isString() && nextToken.isString()) {
                String previousString = previousToken.getString();
                String nextString = nextToken.getString();
                boolean bracketsFound = false;
                boolean leftStick = false;
                boolean rightStick = false;
                int bracketsRelativeStart;
                int bracketsRelativeEnd;
                int leftTokenizedCharactersNum = 0;
                int rightTokenizedCharactersNum = 0;
                boolean noMoreNestedBrackets = false;
                while (!noMoreNestedBrackets) {
                    Matcher previousMatcher = scoreOnly
                            ? Pattern.compile(" *\\[( *-?\\d+ *:)? *$").matcher(previousString)
                            : Pattern.compile(" *\\^? *\\[( *-?\\d+ *:)? *$").matcher(previousString);
                    if (previousMatcher.find()) {
                        Matcher nextMatcher = scoreOnly
                                ? Pattern.compile("^ *] *").matcher(nextString)
                                : Pattern.compile("^ *] *\\$? *").matcher(nextString);
                        if (nextMatcher.find()) {
                            bracketsFound = true;
                            bracketsRelativeStart = previousMatcher.start();
                            bracketsRelativeEnd = nextMatcher.end();
                            leftTokenizedCharactersNum += previousString.length() - bracketsRelativeStart;
                            rightTokenizedCharactersNum += bracketsRelativeEnd;
                            previousString = previousString.substring(0, bracketsRelativeStart);
                            nextString = nextString.substring(bracketsRelativeEnd);
                            leftStick = !scoreOnly && previousMatcher.group().contains("^");
                            rightStick = !scoreOnly && nextMatcher.group().contains("$");
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
                    SinglePattern wrappedPattern = null;
                    if (leftStick || rightStick) {
                        wrappedPattern = currentToken.getSinglePattern();
                        if (leftStick)
                            wrappedPattern = wrapWithStickFilter(wrappedPattern, true, 0);
                        if (rightStick)
                            wrappedPattern = wrapWithStickFilter(wrappedPattern, false, -2);
                    }
                    if (!foundScoreThresholds.isEmpty()) {
                        long scoreThreshold = foundScoreThresholds.stream().mapToLong(st -> st.threshold).max()
                                .orElseThrow(IllegalStateException::new);
                        foundToken = new FoundToken(currentToken.getPattern() instanceof SinglePattern
                                ? wrapWithScoreFilter(
                                        wrappedPattern != null ? wrappedPattern : currentToken.getSinglePattern(),
                                scoreThreshold)
                                : wrapWithScoreFilter(currentToken.getMultipleReadsOperator(), scoreThreshold),
                                tokenStart, tokenEnd);
                    } else
                        foundToken = new FoundToken(wrappedPattern != null ? wrappedPattern : currentToken.getPattern(),
                                tokenStart, tokenEnd);
                    foundTokens.add(foundToken);
                }
            }
        }

        return foundTokens;
    }

    /**
     * Wrap all SinglePattern tokens with FullRead patterns. This function is called when all single patterns
     * are parsed, and, if query is correct, number of single patterns must be equal to the number of reads.
     * FullRead patterns add built-in matched groups R1, R2, ... for each read, if there are no groups in query
     * that override some of built-in group names.
     *
     * @param tokenizedString tokenized string object for query string
     * @return list of found tokens
     */
    ArrayList<FoundToken> parseFullReadPatterns(TokenizedString tokenizedString) throws ParserException {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        List<Token> patternTokens = tokenizedString.getTokens(0, tokenizedString.getFullLength()).stream()
                .filter(Token::isPatternAndNotNull).collect(Collectors.toList());
        for (Token token : patternTokens) {
            SinglePattern pattern = token.getSinglePattern();
            foundTokens.add(new FoundToken(new FullReadPattern(conf.getPatternConfiguration(), pattern),
                    token.getStartCoordinate(), token.getStartCoordinate() + token.getLength()));
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
                if ((i == tokens.size()) || tokens.get(i).isNullPattern() || (tokens.get(i).isString()
                        && !tokens.get(i).getString().matches(operatorRegexp))) {
                    if (sequenceStarted) {
                        if (sequenceStart < i - 2) {
                            int numOperands = (i - sequenceStart + 1) / 2;
                            SinglePattern[] operands = new SinglePattern[numOperands];
                            for (int j = 0; j < numOperands; j++)
                                if (operatorRegexp.contains("\\\\"))
                                    operands[j] = tokens.get(sequenceStart + j * 2).getSinglePattern();
                                else
                                    operands[j] = tokens.get(sequenceStart + j * 2).getSinglePatternExceptAnyPattern();
                            int sequenceTokenStart = tokens.get(sequenceStart).getStartCoordinate();
                            int sequenceTokenEnd = tokens.get(i - 1).getStartCoordinate()
                                    + tokens.get(i - 1).getLength();
                            FoundToken foundToken;
                            if (operatorRegexp.contains("+")) {
                                validateGroupEdges(false, false, true,
                                        conf.isDefaultGroupsOverride(), operands);
                                foundToken = new FoundToken(new PlusPattern(getPatternConfiguration(
                                        sequenceTokenStart, sequenceTokenEnd), operands),
                                        sequenceTokenStart, sequenceTokenEnd);
                            } else if (operatorRegexp.contains("&")) {
                                validateGroupEdges(true, false, true,
                                        conf.isDefaultGroupsOverride(), operands);
                                foundToken = new FoundToken(new AndPattern(getPatternConfiguration(
                                        sequenceTokenStart, sequenceTokenEnd), operands),
                                        sequenceTokenStart, sequenceTokenEnd);
                            } else if (operatorRegexp.contains("|")) {
                                validateGroupEdges(true, true, true,
                                        conf.isDefaultGroupsOverride(), operands);
                                foundToken = new FoundToken(new OrPattern(getPatternConfiguration(
                                        sequenceTokenStart, sequenceTokenEnd), operands),
                                        sequenceTokenStart, sequenceTokenEnd);
                            } else if (operatorRegexp.contains("\\\\")) {
                                validateGroupEdges(true, false, true,
                                        conf.isDefaultGroupsOverride(), operands);
                                if (Arrays.stream(operands).anyMatch(o -> !(o instanceof FullReadPattern)))
                                    throw new IllegalStateException(
                                            "MultiPattern argument not wrapped with FullReadPattern: "
                                                    + tokenizedString);
                                foundToken = new FoundToken(new MultiPattern(getPatternConfiguration(
                                        sequenceTokenStart, sequenceTokenEnd), operands),
                                        sequenceTokenStart, sequenceTokenEnd);
                            } else
                                throw new IllegalArgumentException("Invalid operator regexp: " + operatorRegexp);
                            foundTokens.add(foundToken);
                        }
                        sequenceStarted = false;
                    }
                } else if (!sequenceStarted && tokens.get(i).isPatternAndNotNull()) {
                    sequenceStart = i;
                    sequenceStarted = true;
                }
            }
        }

        return foundTokens;
    }

    /**
     * This function will wrap SinglePattern tokens with MultiPatterns, for case when high level logic operators
     * are used with single read patterns.
     *
     * @param tokenizedString tokenized string object for query string
     * @return list of found tokens
     */
    ArrayList<FoundToken> wrapWithMultiPatterns(TokenizedString tokenizedString) throws ParserException {
        ArrayList<FoundToken> foundTokens = new ArrayList<>();
        ArrayList<Token> tokens = tokenizedString.getTokens(0, tokenizedString.getFullLength());
        if (tokens.size() > 1) {
            boolean onlySinglePatterns = tokens.parallelStream().filter(Token::isPatternAndNotNull)
                    .allMatch(t -> t.getPattern() instanceof SinglePattern);
            boolean onlyMultiPatterns = tokens.parallelStream().filter(Token::isPatternAndNotNull)
                    .allMatch(t -> t.getPattern() instanceof MultipleReadsOperator);
            if (onlySinglePatterns && onlyMultiPatterns)
                throw new ParserException("Query not parsed: no patterns found!");
            if (!onlySinglePatterns && !onlyMultiPatterns)
                throw new ParserException("Single read patterns are mixed with multiple reads patterns: "
                        + tokenizedString);
            if (onlySinglePatterns) {
                if (tokens.parallelStream().filter(Token::isPatternAndNotNull)
                        .anyMatch(t -> !(t.getPattern() instanceof FullReadPattern)))
                    throw new IllegalStateException("MultiPattern argument not wrapped with FullReadPattern: "
                            + tokenizedString);
                tokens.stream().filter(Token::isPatternAndNotNull).forEach(token -> foundTokens.add(
                        new FoundToken(new MultiPattern(getPatternConfiguration(
                                token.getStartCoordinate(), token.getStartCoordinate() + token.getLength()),
                                (SinglePattern)(token.getPattern())),
                                token.getStartCoordinate(), token.getStartCoordinate() + token.getLength())));
            }
        } else if (tokens.size() == 1) {
            Token token = tokens.get(0);
            if (!token.isPatternAndNotNull())
                throw new ParserException("Token not parsed: " + token);
            else if (token.getPattern() instanceof SinglePattern)
                token.getSpecificPattern(FullReadPattern.class).setTargetId((byte)1);
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
                if ((i == tokens.size())
                        || (tokens.get(i).isString() && !tokens.get(i).getString().matches(operatorRegexp))
                        || (sequenceStarted && (i > 0) && (tokens.get(i - 1).isString() == tokens.get(i).isString()))
                        || (sequenceStarted && operatorRegexp.contains("~"))) {
                    if (sequenceStarted) {
                        if (operatorRegexp.contains("~")) {
                            if ((i > 1) && tokens.get(i - 2).isString() &&
                                    tokens.get(i - 2).getString().matches(operatorRegexp)) {
                                Token operatorToken = tokens.get(i - 2);
                                Token operandToken = tokens.get(i - 1);
                                int tokenStart = operatorToken.getStartCoordinate()
                                        + operatorToken.getString().lastIndexOf("~");
                                int tokenEnd = operandToken.getStartCoordinate() + operandToken.getLength();
                                MultipleReadsOperator operand = operandToken.getMultipleReadsOperator();
                                validateGroupEdges(false, true, false,
                                        conf.isDefaultGroupsOverride(), operand);
                                foundTokens.add(new FoundToken(new NotOperator(getPatternConfiguration(
                                        tokenStart, tokenEnd), operand), tokenStart, tokenEnd));
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
                                validateGroupEdges(true, false, true,
                                        conf.isDefaultGroupsOverride(), operands);
                                multipleReadsOperator = new AndOperator(getPatternConfiguration(
                                        sequenceTokenStart, sequenceTokenEnd), operands);
                            } else if (operatorRegexp.contains("|")) {
                                validateGroupEdges(true, true, true,
                                        conf.isDefaultGroupsOverride(), operands);
                                multipleReadsOperator = new OrOperator(getPatternConfiguration(
                                        sequenceTokenStart, sequenceTokenEnd), operands);
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
                    currentPosition, onLeft ? currentGroupName.end + 1 : currentGroupName.bracketsPair.end + 1));
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
        int foundIndex = -1;
        int minDistance = Integer.MAX_VALUE;
        for (int i = 0; i < groupNames.size(); i++) {
            NormalSyntaxGroupName currentGroupName = groupNames.get(i);
            int nameEnd = currentGroupName.end;
            int parenthesesEnd = currentGroupName.bracketsPair.end;
            if (toLeft) {
                if ((position > nameEnd) && (position - nameEnd < minDistance)) {
                    foundIndex = i;
                    minDistance = position - nameEnd;
                }
            } else {
                if ((position < parenthesesEnd) && (parenthesesEnd - position < minDistance)) {
                    foundIndex = i;
                    minDistance = parenthesesEnd - position;
                }
            }
        }
        return foundIndex;
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
        for (NormalSyntaxGroupName groupName : groupNames) {
            int groupStart = groupName.start - start;
            int groupNameEnd = groupName.end - start;           // inclusive
            int groupEnd = groupName.bracketsPair.end - start;  // inclusive
            int patternStringLength = end - start;
            if ((groupStart > 0) && (groupStart < patternStringLength - 1)) {
                if (groupNameEnd >= patternStringLength - 1)
                    throw new IllegalStateException("FuzzyMatchPattern: start=" + start + ", end=" + end
                            + ", group name: start=" + groupName.start + ", end=" + groupName.end);
                int ignoredCharactersBeforeStart = countIgnoredCharactersInRange(start, groupName.start);
                foundGroupEdgePositions.add(new FoundGroupEdgePosition(new GroupEdgePosition(new GroupEdge(
                        groupName.name, true), groupStart - ignoredCharactersBeforeStart)));
            }
            if ((groupEnd > 0) && (groupEnd < patternStringLength - 1)) {
                int ignoredCharactersBeforeEnd = countIgnoredCharactersInRange(start, groupName.bracketsPair.end);
                foundGroupEdgePositions.add(new FoundGroupEdgePosition(new GroupEdgePosition(new GroupEdge(
                        groupName.name, false), groupEnd - ignoredCharactersBeforeEnd)));
            }
        }

        final int patternLength = end - start - countIgnoredCharactersInRange(start, end);
        foundGroupEdgePositions.addAll(findGroupsOnBorder(start, true, patternLength));
        foundGroupEdgePositions.addAll(findGroupsOnBorder(end - 1, false, patternLength));
        return foundGroupEdgePositions;
    }

    /**
     * Count ignored characters (spaces, parentheses, colons and group names) in specified range.
     *
     * @param start start of search interval in query, inclusive
     * @param end end of search interval in query, exclusive
     * @return number of ignored characters inside the specified range
     */
    private int countIgnoredCharactersInRange(int start, int end) {
        if ((start < 0) || (end > query.length()) || (end < start))
            throw new IllegalArgumentException("countIgnoredCharactersInRange called with start=" + start
                    + ", end=" + end + " for query " + query);
        else {
            int rangeLength = end - start;
            int ignoredCharactersCount = countCharacters(query.substring(start, end), ' ');
            for (NormalSyntaxGroupName groupName : groupNames) {
                int groupStart = groupName.start - start;
                int groupNameEnd = groupName.end - start;           // inclusive
                int groupEnd = groupName.bracketsPair.end - start;  // inclusive
                if (((groupStart < 0) && (groupNameEnd >= 0))
                        || ((groupStart < rangeLength) && (groupNameEnd > rangeLength - 1)))
                    throw new IllegalStateException("Name of group " + groupName.name
                            + " is partially in specified range: start=" + start + ", end=" + end
                            + ", groupStart=" + groupStart + ", groupNameEnd=" + groupNameEnd + ", query: " + query);
                else if (groupNameEnd <= groupStart + 1)
                    throw new IllegalStateException("Wrong state of group " + groupName.name
                            + ": groupName.start=" + groupName.start + ", groupName.end=" + groupName.end
                            + ", query: " + query);
                else if ((groupStart >= 0) && (groupNameEnd < rangeLength))
                    ignoredCharactersCount += groupNameEnd - groupStart + 1
                            - countCharacters(query.substring(groupName.start, groupName.end + 1), ' ');
                if ((groupEnd >= 0) && (groupEnd < rangeLength))
                    ignoredCharactersCount++;
            }
            return ignoredCharactersCount;
        }
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

        if ((markerPosition != -1)
                && !query.substring(markerPosition + 1, position).matches(".*[\\^$\\[\\]+&|\\\\].*")
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

        if ((markerPosition != -1)
                && !query.substring(position + 1, markerPosition).matches(".*[\\^$+&|\\[\\]\\\\].*")
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

        if ((tokenPosition != -1) && !query.substring(tokenPosition, position).matches(".*((\\\\)|\\[|]).*")
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
            if ((query.charAt(position) != ' ') && groupNames.stream()
                    .noneMatch(gn -> ((gn.start <= position) && (gn.end >= position))
                            || (gn.bracketsPair.end == position)))
                result.append(query.charAt(position));
        }
        return result.toString();
    }

    private SinglePattern wrapWithScoreFilter(SinglePattern singlePattern, long scoreThreshold) {
        return new FilterPattern(conf.getPatternConfiguration(), new ScoreFilter(scoreThreshold), singlePattern);
    }

    private MultipleReadsOperator wrapWithScoreFilter(MultipleReadsOperator multiReadPattern, long scoreThreshold) {
        return new MultipleReadsFilterPattern(conf.getPatternConfiguration(), new ScoreFilter(scoreThreshold),
                multiReadPattern);
    }

    private SinglePattern wrapWithStickFilter(SinglePattern singlePattern, boolean left, int position) {
        SinglePattern wrappedPattern;
        if (singlePattern instanceof FuzzyMatchPattern || singlePattern instanceof RepeatPattern) {
            wrappedPattern = ((CanFixBorders)singlePattern).fixBorder(left, position);
        } else if (singlePattern instanceof CanFixBorders) {
            wrappedPattern = new FilterPattern(conf.getPatternConfiguration(), new StickFilter(left, position),
                    ((CanFixBorders)singlePattern).fixBorder(left, position));
        } else
            wrappedPattern = singlePattern;
        return wrappedPattern;
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
            String insideBrackets = query.substring(bracketsPair.start + 1, bracketsPair.end);
            int colonPosition = insideBrackets.indexOf(":");
            if (colonPosition == -1)
                throw new IllegalStateException("No colon inside brackets with score threshold: " + insideBrackets);
            else
                return bracketsPair.start + colonPosition + 2;
        } else
            return bracketsPair.start + 1;
    }

    /**
     * Find score threshold for specified range and return PatternConfiguration with this threshold. If there is
     * no score threshold, return pattern configuration without changing its threshold.
     *
     * @param start start of range, inclusive
     * @param end end of range, exclusive
     * @return PatternConfiguration with updated score threshold for the specified range
     */
    private PatternConfiguration getPatternConfiguration(int start, int end) {
        int currentNestedLevel = -1;
        long currentThreshold = 0;
        for (ScoreThreshold scoreThreshold : scoreThresholds)
            if (scoreThreshold.contains(start, end) && (scoreThreshold.nestedLevel > currentNestedLevel)) {
                currentNestedLevel = scoreThreshold.nestedLevel;
                currentThreshold = scoreThreshold.threshold;
            }
        if (currentNestedLevel == -1)
            return conf.getPatternConfiguration();
        else
            return conf.getPatternConfiguration().overrideScoreThreshold(currentThreshold);
    }

    private static class FoundGroupEdgePosition {
        final GroupEdgePosition groupEdgePosition;
        final int start;
        final int end;

        FoundGroupEdgePosition(GroupEdgePosition groupEdgePosition) {
            this(groupEdgePosition, -1, -1);
        }

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
