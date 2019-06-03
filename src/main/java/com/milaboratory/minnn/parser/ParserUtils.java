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

import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.pattern.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.parser.BracketsDetector.*;
import static com.milaboratory.minnn.parser.BracketsType.*;
import static com.milaboratory.minnn.parser.Parser.BUILTIN_READ_GROUPS_NUM;
import static com.milaboratory.minnn.parser.SimplifiedSyntaxStrings.*;

public final class ParserUtils {
    /** Default group names: they are ignored in validateGroupEdges() if there is no default groups override. */
    private static final Set<String> defaultGroupNamesCache = IntStream.rangeClosed(1, BUILTIN_READ_GROUPS_NUM)
            .mapToObj(i -> "R" + i).collect(Collectors.toSet());

    /**
     * Find all non-quoted positions of specified token in string.
     *
     * @param str string in which we search tokens
     * @param token token - string to search
     * @param quotesPairs quotes pairs detected by getAllQuotes function
     * @return list of all found token positions
     */
    static ArrayList<Integer> getTokenPositions(String str, String token, List<QuotesPair> quotesPairs) {
        ArrayList<Integer> positions = new ArrayList<>();
        for (int currentPosition = nonQuotedIndexOf(quotesPairs, str, token, 0); currentPosition >= 0;
             currentPosition = nonQuotedIndexOf(quotesPairs, str, token, currentPosition + 1))
            positions.add(currentPosition);
        return positions;
    }

    /**
     * Check is there any specific character in substring from position to stop character.
     *
     * @param str string where to search characters
     * @param position start position in string, exclusive
     * @param toLeft true if search to the left from position, false if to the right
     * @param includeQuoted true if quoted characters should not be skipped
     * @param specificChars string of specific characters
     * @param stopChars string of stop characters
     * @param quotesPairs quotes pairs detected by getAllQuotes function; may be null if includeQuoted is true
     * @return true if specific character is found, false if not
     */
    static boolean isSpecificCharBeforeStopChar(String str, int position, boolean toLeft, boolean includeQuoted,
            String specificChars, String stopChars, List<QuotesPair> quotesPairs) {
        StringBuilder intermediateCharacters = new StringBuilder();
        String currentChar;
        if (toLeft)
            for (int currentPosition = includeQuoted ? position - 1 : previousNonQuotedPosition(quotesPairs, position);
                 currentPosition >= 0; currentPosition = includeQuoted ? currentPosition - 1
                    : previousNonQuotedPosition(quotesPairs, currentPosition)) {
                currentChar = str.substring(currentPosition, currentPosition + 1);
                if (stopChars.contains(currentChar))
                    break;
                else
                    intermediateCharacters.append(currentChar);
            }
        else
            for (int currentPosition = includeQuoted ? position + 1 : nextNonQuotedPosition(quotesPairs, position);
                 currentPosition < str.length(); currentPosition = includeQuoted ? currentPosition + 1
                    : nextNonQuotedPosition(quotesPairs, currentPosition)) {
                currentChar = str.substring(currentPosition, currentPosition + 1);
                if (stopChars.contains(currentChar))
                    break;
                else
                    intermediateCharacters.append(currentChar);
            }
        return intermediateCharacters.chars().anyMatch(ic -> specificChars.chars().anyMatch(sc -> ic == sc));
    }

    /**
     * Get object name at left of open parenthesis for simplified syntax.
     *
     * @param leftParenthesisPosition position of open parenthesis in string
     * @param fullString string where to search
     * @return object name
     */
    static String getObjectName(int leftParenthesisPosition, String fullString) {
        ArrayList<Character> reversedNameCharacters = new ArrayList<>();
        int currentPosition = leftParenthesisPosition - 1;

        while ((currentPosition >= 0)
                && (!NAME_STOP_CHARACTERS.contains(fullString.substring(currentPosition, currentPosition + 1))))
            reversedNameCharacters.add(fullString.charAt(currentPosition--));
        StringBuilder builder = new StringBuilder(reversedNameCharacters.size());
        for (int i = reversedNameCharacters.size() - 1; i >= 0; i--)
            builder.append(reversedNameCharacters.get(i));
        return builder.toString();
    }

    /**
     * Detect all score thresholds in query string.
     *
     * @param query query string
     * @param format parser format: NORMAL for end users or SIMPLIFIED as toString() output in inner classes
     * @return list of score thresholds
     */
    static ArrayList<ScoreThreshold> getScoreThresholds(String query, ParserFormat format) throws ParserException {
        ArrayList<ScoreThreshold> scoreThresholds = new ArrayList<>();
        switch (format) {
            case NORMAL:
                List<BracketsPair> squareBrackets = getAllBrackets(SQUARE, query);
                squareBrackets.sort(Comparator.comparingInt(bp -> bp.start));
                for (BracketsPair bracketsPair : squareBrackets) {
                    boolean isScoreLimit = false;
                    int colonPosition = 0;
                    for (int i = bracketsPair.start + 1; i < bracketsPair.end; i++) {
                        if ("([{\"'".contains(query.substring(i, i + 1)))
                            break;
                        if (query.charAt(i) == ':') {
                            isScoreLimit = true;
                            colonPosition = i;
                            break;
                        }
                    }
                    if (isScoreLimit) {
                        long scoreThreshold = toLong(query.substring(bracketsPair.start + 1, colonPosition),
                                "score threshold");
                        int currentNestedLevel = 0;
                        for (ScoreThreshold currentScoreThreshold : scoreThresholds)
                            if (currentScoreThreshold.contains(bracketsPair.start, bracketsPair.end))
                                currentNestedLevel++;
                        scoreThresholds.add(new ScoreThreshold(scoreThreshold,
                                bracketsPair.start, bracketsPair.end + 1, currentNestedLevel));
                    }
                }
                break;
            case SIMPLIFIED:
                int minFilterLength = SCORE_FILTER_START.length() + 8;
                List<BracketsPair> parentheses = getAllBrackets(PARENTHESES, query);
                for (int currentPosition = 0; currentPosition < query.length() - minFilterLength; currentPosition++)
                    if (query.substring(currentPosition, currentPosition + SCORE_FILTER_START.length())
                            .equals(SCORE_FILTER_START)) {
                        int startCoordinate = currentPosition + FILTER_PATTERN_NAME.length();
                        int endCoordinate = getEndByStart(parentheses, startCoordinate);
                        int filterStartCoordinate = startCoordinate + 1;
                        int filterEndCoordinate = getEndByStart(parentheses,
                                currentPosition + SCORE_FILTER_START.length() - 1);
                        String scoreFilterSubstring = query.substring(filterStartCoordinate,
                                filterEndCoordinate + 1);
                        long scoreThreshold = toLong(scoreFilterSubstring.substring(SCORE_FILTER_NAME.length() + 1,
                                scoreFilterSubstring.length() - 1), "score threshold");
                        int currentNestedLevel = 0;
                        for (ScoreThreshold currentScoreThreshold : scoreThresholds)
                            if (currentScoreThreshold.contains(startCoordinate, endCoordinate))
                                currentNestedLevel++;
                        scoreThresholds.add(new ScoreThreshold(scoreThreshold, startCoordinate, endCoordinate + 1,
                                currentNestedLevel));
                    }
        }
        return scoreThresholds;
    }

    /**
     * Test group name for validity, throw ParserException if it is invalid. Special characters are not allowed
     * in group names because they may lead to bad Fastq output files.
     *
     * @param groupName group name
     */
    static void checkGroupName(String groupName) throws ParserException {
        if (groupName.length() == 0)
            throw new ParserException("Group name must not be empty!");
        if (!groupName.matches("[a-zA-Z0-9]*"))
            throw new ParserException("Group names must contain only letters and digits; invalid group name: "
                    + groupName);
    }

    /**
     * Filter group edges for validation by excluding default built-in groups (R1, R2 etc).
     *
     * @param groupEdges all group edges
     * @return filtered group edges
     */
    static ArrayList<GroupEdge> filterGroupEdgesForValidation(ArrayList<GroupEdge> groupEdges) {
        return groupEdges.stream().filter(ge -> !defaultGroupNamesCache.contains(ge.getGroupName()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Get filtered group edges from pattern. For FullReadPattern we must use getOperandGroupEdges()
     * because targetId is not initialized on this stage.
     *
     * @param pattern pattern to get group edges
     * @param defaultGroupsOverride true if there is default groups override in any pattern in the query
     * @return filtered group edges
     */
    private static ArrayList<GroupEdge> getGroupEdgesForValidation(Pattern pattern, boolean defaultGroupsOverride) {
        ArrayList<GroupEdge> groupEdges = pattern instanceof FullReadPattern
                ? ((FullReadPattern)pattern).getOperandGroupEdges() : pattern.getGroupEdges();
        if (defaultGroupsOverride)
            return groupEdges;
        else
            return filterGroupEdgesForValidation(groupEdges);
    }

    /**
     * Check that group edges in pattern operands are correct, otherwise throw ParserException.
     *
     * @param pairsRequired group starts and ends must come in pairs in each operand
     * @param duplicatesAllowed duplicates of group edges are allowed in combined list from all operands
     * @param groupsAllowed if false, operands must not contain any group edges
     * @param defaultGroupsOverride true if there is default groups override in any pattern in the query
     * @param operands pattern operands
     */
    static void validateGroupEdges(boolean pairsRequired, boolean duplicatesAllowed, boolean groupsAllowed,
                                   boolean defaultGroupsOverride, Pattern... operands) throws ParserException {
        ArrayList<GroupEdge> allGroupEdges = new ArrayList<>();
        for (Pattern operand : operands) {
            ArrayList<GroupEdge> groupEdges = getGroupEdgesForValidation(operand, defaultGroupsOverride);
            if (!groupsAllowed && (groupEdges.size() > 0))
                throw new ParserException("Found group edges inside pattern that doesn't allow group edges inside: "
                        + groupEdges);
            if (pairsRequired)
                validateGroupEdges(groupEdges, true, true);
            allGroupEdges.addAll(groupEdges);
        }
        if (!duplicatesAllowed) {
            validateGroupEdges(allGroupEdges, false, false);

            // check for group ends that are before starts
            for (int i = 1; i < operands.length; i++)
                for (int j = 0; j < i; j++) {
                    final Pattern operand1 = operands[j];
                    final Pattern operand2 = operands[i];
                    final ArrayList<GroupEdge> groupEdges1 = getGroupEdgesForValidation(
                            operand1, defaultGroupsOverride);
                    final ArrayList<GroupEdge> groupEdges2 = getGroupEdgesForValidation(
                            operand2, defaultGroupsOverride);
                    if (groupEdges1.stream().anyMatch(ge1 -> groupEdges2.stream()
                            .anyMatch(ge2 -> ge1.getGroupName().equals(ge2.getGroupName()) && ge2.isStart())))
                        throw new ParserException("Pattern " + operand1
                                + " contains group end that is before start of this group in pattern " + operand2);
                }
        }
    }

    /**
     * Check that group edges are correct, otherwise throw ParserException.
     *
     * @param groupEdges list of group edges to check
     * @param pairsRequired group starts and ends must come in pairs
     * @param duplicatesAllowed duplicates of group edges are allowed in the list
     */
    static void validateGroupEdges(ArrayList<GroupEdge> groupEdges, boolean pairsRequired, boolean duplicatesAllowed)
            throws ParserException {
        if (!duplicatesAllowed)
            for (int i = 1; i < groupEdges.size(); i++)
                for (int j = 0; j < i; j++) {
                    GroupEdge groupEdge1 = groupEdges.get(i);
                    GroupEdge groupEdge2 = groupEdges.get(j);
                    if (groupEdge1.getGroupName().equals(groupEdge2.getGroupName())
                            && (groupEdge1.isStart() == groupEdge2.isStart()))
                        throw new ParserException("Duplicate groups allowed only on different sides of || operator; "
                                + "found invalid duplicate of group " + groupEdge1.getGroupName());
                }
        if (pairsRequired) {
            HashSet<String> groupNames = groupEdges.stream().map(GroupEdge::getGroupName)
                    .collect(Collectors.toCollection(HashSet::new));
            for (String groupName : groupNames) {
                long starts = groupEdges.stream().filter(ge -> ge.getGroupName().equals(groupName))
                        .filter(GroupEdge::isStart).count();
                long ends = groupEdges.stream().filter(ge -> ge.getGroupName().equals(groupName))
                        .filter(ge -> !ge.isStart()).count();
                if (starts != ends)
                    throw new ParserException("Group " + groupName + " has " + starts + " start(s) and " + ends
                            + " end(s) where expected equal number of starts and ends!");
            }
        }
    }

    /**
     * Check that group edge positions are correct, otherwise throw ParserException.
     *
     * @param groupEdgePositions list of group edge positions to check
     */
    static void validateGroupEdgePositions(ArrayList<GroupEdgePosition> groupEdgePositions) throws ParserException {
        for (int i = 1; i < groupEdgePositions.size(); i++)
            for (int j = 0; j < i; j++) {
                GroupEdgePosition groupEdgePosition1 = groupEdgePositions.get(i);
                GroupEdgePosition groupEdgePosition2 = groupEdgePositions.get(j);
                if (groupEdgePosition1.getGroupEdge().getGroupName()
                        .equals(groupEdgePosition2.getGroupEdge().getGroupName())) {
                    if (groupEdgePosition1.getGroupEdge().isStart() == groupEdgePosition2.getGroupEdge().isStart())
                        throw new ParserException("Found duplicate" + (groupEdgePosition1.getGroupEdge().isStart()
                                ? "start" : "end") + " of group " + groupEdgePosition1.getGroupEdge().getGroupName()
                                + " in 1 sequence of nucleotides!");
                    if ((groupEdgePosition1.getGroupEdge().isStart()
                            && (groupEdgePosition1.getPosition() > groupEdgePosition2.getPosition()))
                            || (groupEdgePosition2.getGroupEdge().isStart()
                            && (groupEdgePosition2.getPosition() > groupEdgePosition1.getPosition())))
                        throw new ParserException("Found end of group "
                                + groupEdgePosition1.getGroupEdge().getGroupName()
                                + " before start of group with the same name in 1 sequence of nucleotides!");
                }
            }
    }

    /**
     * Convert string with space separated targets to MultiNSequenceWithQuality object.
     *
     * @param multiTarget string with space separated targets (nucleotide sequences)
     * @return parsed MultiNSequenceWithQuality object
     */
    public static MultiNSequenceWithQuality parseMultiTargetString(String multiTarget) {
        String[] targets = multiTarget.split(" ");
        return new MultiNSequenceWithQualityImpl(Arrays.stream(targets).map(NSequenceWithQuality::new)
                .toArray(NSequenceWithQuality[]::new));
    }

    /**
     * Get case sensitive nucleotide sequence from string or throw ParserException if failed.
     *
     * @param str string containing nucleotide sequence
     * @return NucleotideSequenceCaseSensitive object
     * @throws ParserException if NucleotideSequenceCaseSensitive constructor throws IllegalArgumentException,
     *                         or if sequence is empty
     */
    static NucleotideSequenceCaseSensitive toNSeq(String str) throws ParserException {
        try {
            NucleotideSequenceCaseSensitive seq = new NucleotideSequenceCaseSensitive(str.trim());
            if (seq.size() < 1)
                throw new ParserException("Failed to parse nucleotide sequence from string \"" + str + "\"");
            return seq;
        } catch (IllegalArgumentException e) {
            throw new ParserException("Failed to parse nucleotide sequence from string \"" + str + "\": "
                    + e.getMessage());
        }
    }

    /**
     * Get integer from string or throw ParserException if failed.
     *
     * @param str string
     * @param paramName parameter name for error message
     * @return int value
     */
    static int toInt(String str, String paramName) throws ParserException {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            throw new ParserException("Failed to parse " + paramName + " from string \"" + str + "\"");
        }
    }

    /**
     * Get long from string or throw ParserException if failed.
     *
     * @param str string
     * @param paramName parameter name for error message
     * @return long value
     */
    static long toLong(String str, String paramName) throws ParserException {
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException e) {
            throw new ParserException("Failed to parse " + paramName + " from string \"" + str + "\"");
        }
    }

    /**
     * Count number of occurrences of character in string.
     *
     * @param str string
     * @param c character to search
     * @return number of occurrences of character in string
     */
    static int countCharacters(String str, char c) {
        return str.length() - str.replace(Character.toString(c), "").length();
    }
}
