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

import com.milaboratory.minnn.pattern.*;

import java.util.*;
import java.util.stream.*;

final class TokenizedString {
    private final LinkedList<Token> tokenizedString;
    private final int length;

    /**
     * Tokenized string that allows to replace parts of string with patterns.
     *
     * @param query initial string
     */
    TokenizedString(String query) {
        this.tokenizedString = new LinkedList<>();
        this.tokenizedString.add(new Token(query, 0));
        this.length = query.length();
    }

    @Override
    public String toString() {
        return tokenizedString.toString();
    }

    /**
     * Wrapper for tokenizeSubstring that gets FoundToken object as parameter.
     *
     * @param foundToken FoundToken object that contains found pattern, start and end positions
     */
    void tokenizeSubstring(FoundToken foundToken) {
        tokenizeSubstring(foundToken.pattern, foundToken.from, foundToken.to);
    }

    /**
     * Wrapper for tokenizeSubstring that gets coordinates in string as parameters.
     *
     * @param pattern pattern to put into tokenized string
     * @param from start position to delete, inclusive
     * @param to end position to delete, exclusive
     */
    void tokenizeSubstring(Pattern pattern, int from, int to) {
        int startIndex = getIndexByPosition(from);
        int endIndex = getIndexByPosition(to - 1);
        int startIndexLastPosition;
        int endIndexFirstPosition;

        Token startToken = tokenizedString.get(startIndex);
        if ((from == startToken.getStartCoordinate()) && !startToken.isString())
            startIndexLastPosition = -1;
        else
            if (startToken.isString())
                startIndexLastPosition = from - startToken.getStartCoordinate();
            else throw new IllegalArgumentException("Trying to tokenize in the middle of " + startToken
                    + ": from=" + from + ", to=" + to);

        Token endToken = tokenizedString.get(endIndex);
        if ((to == endToken.getStartCoordinate() + endToken.getLength()) && !endToken.isString())
            endIndexFirstPosition = -1;
        else
            if (endToken.isString())
                endIndexFirstPosition = to - endToken.getStartCoordinate();
            else throw new IllegalArgumentException("Trying to tokenize in the middle of " + endToken
                    + ": from=" + from + ", to=" + to);

        tokenizeSubstring(pattern, startIndex, startIndexLastPosition, endIndex, endIndexFirstPosition);
    }

    /**
     * Replace a part of tokenized string with a TokenizedStringPattern that contains pattern and length of string
     * that it replaced.
     *
     * @param pattern pattern to put into tokenized string
     * @param startIndex start index to delete (fully or partially), inclusive
     * @param startIndexLastPosition -1 to delete fully; must be always -1 if startIndex points to Pattern;
     *                               if not -1, it is last position to preserve inside string, exclusive
     * @param endIndex end index to delete (fully or partially), inclusive
     * @param endIndexFirstPosition -1 to delete fully; must be always -1 if endIndex points to Pattern;
     *                              if not -1, it is first position to preserve inside string, inclusive
     */
    private void tokenizeSubstring(Pattern pattern, int startIndex, int startIndexLastPosition,
                           int endIndex, int endIndexFirstPosition) {
        if ((startIndex >= tokenizedString.size()) || (startIndex < 0))
            throw new IndexOutOfBoundsException("startIndex = " + startIndex
                    + ", tokenizedString size = " + tokenizedString.size());
        if ((endIndex >= tokenizedString.size()) || (endIndex < 0))
            throw new IndexOutOfBoundsException("endIndex = " + endIndex
                    + ", tokenizedString size = " + tokenizedString.size());
        if (startIndex > endIndex)
            throw new IllegalArgumentException("startIndex = " + startIndex + ", endIndex = " + endIndex);

        if (startIndex == endIndex) {
            Token specifiedToken = tokenizedString.get(startIndex);
            if ((startIndexLastPosition == -1) && (endIndexFirstPosition == -1)) {
                Token patternToken = new Token(pattern, specifiedToken.getLength(), specifiedToken.getStartCoordinate());
                tokenizedString.set(startIndex, patternToken);
            } else {
                if (!specifiedToken.isString())
                    throw new IllegalArgumentException("startIndex = endIndex = " + startIndex +
                            ", startLast = " + startIndexLastPosition + ", endFirst = " + endIndexFirstPosition
                            + ", and specified token is not a string: " + specifiedToken);
                if ((startIndexLastPosition >= 0) && (endIndexFirstPosition > startIndexLastPosition)) {
                    String specifiedString = specifiedToken.getString();
                    int startCoordinate = specifiedToken.getStartCoordinate();
                    int patternLength = endIndexFirstPosition - startIndexLastPosition;

                    String leftPart = specifiedString.substring(0, startIndexLastPosition);
                    String rightPart = specifiedString.substring(endIndexFirstPosition);
                    tokenizedString.remove(startIndex);
                    if (rightPart.length() > 0)
                        tokenizedString.add(startIndex, new Token(rightPart, startCoordinate
                                + leftPart.length() + patternLength));
                    tokenizedString.add(startIndex, new Token(pattern, patternLength, startCoordinate
                            + leftPart.length()));
                    if (leftPart.length() > 0)
                        tokenizedString.add(startIndex, new Token(leftPart, startCoordinate));
                } else throw new IllegalArgumentException("startIndex = endIndex = " + startIndex
                        + ", startLast = " + startIndexLastPosition + ", endFirst = " + endIndexFirstPosition);
            }
        } else {
            Token startToken = tokenizedString.get(startIndex);
            Token endToken = tokenizedString.get(endIndex);
            int deleteIndexFirst = startIndex + 1;
            int deleteIndexLast = endIndex;
            int patternLength = calculateLength(startIndex + 1, endIndex);

            if (startIndexLastPosition == -1)
                patternLength += calculateLength(startIndex, startIndex + 1);
            else {
                if (!startToken.isString())
                    throw new IllegalArgumentException("startLast is not -1 and startIndex doesn't point to String!");
                if ((startIndexLastPosition > startToken.getLength()) || (startIndexLastPosition < 0))
                    throw new IndexOutOfBoundsException("startLast: " + startIndexLastPosition
                            + ", startString: " + startToken.getString());
                patternLength += startToken.getLength() - startIndexLastPosition;
            }

            if (endIndexFirstPosition == -1)
                patternLength += calculateLength(endIndex, endIndex + 1);
            else {
                if (!endToken.isString())
                    throw new IllegalArgumentException("endFirst is not -1 and endIndex doesn't point to String!");
                if ((endIndexFirstPosition > endToken.getLength()) || (endIndexFirstPosition < 0))
                    throw new IndexOutOfBoundsException("endFirst: " + endIndexFirstPosition
                            + ", endString: " + endToken.getString());
                patternLength += endIndexFirstPosition;
            }

            tokenizedString.add(startIndex + 1, new Token(pattern, patternLength, (startIndexLastPosition == -1)
                    ? startToken.getStartCoordinate() : startToken.getStartCoordinate() + startIndexLastPosition));

            if (startIndexLastPosition > 0) {
                tokenizedString.set(startIndex, startToken.getSubstringToken(0, startIndexLastPosition));
                deleteIndexFirst++;
                deleteIndexLast++;
            } else tokenizedString.remove(startIndex);

            if ((endIndexFirstPosition != -1) && (endIndexFirstPosition != endToken.getLength())) {
                tokenizedString.set(deleteIndexLast, endToken.getSubstringToken(endIndexFirstPosition));
                deleteIndexLast--;
            }

            tokenizedString.subList(deleteIndexFirst, deleteIndexLast + 1).clear();
        }

        assertChainNotBroken();
    }

    /**
     * Calculate summary string length of the sequence of tokens.
     *
     * @param start starting element, inclusive
     * @param end ending element, exclusive
     * @return string length
     */
    int calculateLength(int start, int end) {
        if (start > end) throw new IllegalArgumentException("start = " + start + ", end = " + end);
        return ((end == tokenizedString.size()) ? length : tokenizedString.get(end).getStartCoordinate())
                - tokenizedString.get(start).getStartCoordinate();
    }

    /**
     * Get length of full query string.
     *
     * @return length of query string
     */
    int getFullLength() {
        return length;
    }

    /**
     * Get size of the token list.
     *
     * @return size of the token list
     */
    int getSize() {
        return tokenizedString.size();
    }

    /**
     * Calculate token index in tokenizedString that contains the specified position in query string.
     *
     * @param position position in query string
     * @return index of token in tokenizedString
     */
    int getIndexByPosition(int position) {
        if ((position < 0) || (position >= length))
            throw new IllegalArgumentException("Position is outside of the string: position=" + position
                    + ", length=" + length);
        return IntStream.range(0, tokenizedString.size() - 1)
                .filter(i -> tokenizedString.get(i + 1).getStartCoordinate() > position).findFirst()
                .orElse(tokenizedString.size() - 1);
    }

    Token getToken(int index) {
        return tokenizedString.get(index);
    }

    /**
     * Get a part of TokenizedString by specifying the coordinates interval. If a token is partially in the interval,
     * Pattern will not be included, and String will be cut and returned partially.
     *
     * @param from left side, inclusive
     * @param to right side, exclusive
     * @return list of tokens that are inside the interval
     */
    ArrayList<Token> getTokens(int from, int to) {
        ArrayList<Token> tokens = new ArrayList<>();
        int lastIndex = getIndexByPosition(to - 1);
        int currentIndex = getIndexByPosition(from);
        Token currentToken = tokenizedString.get(currentIndex);
        int currentLeft = currentToken.getStartCoordinate();

        if (currentLeft == from)
            tokens.add(currentToken);
        else if (currentToken.isString())
            if (currentIndex < lastIndex)
                tokens.add(currentToken.getSubstringToken(from - currentLeft));
            else
                tokens.add(currentToken.getSubstringToken(from - currentLeft, to - currentLeft));

        if (currentIndex < lastIndex) {
            currentIndex++;
            while (currentIndex < lastIndex)
                tokens.add(tokenizedString.get(currentIndex++));

            Token lastToken = tokenizedString.get(lastIndex);
            int lastRight = lastToken.getStartCoordinate() + lastToken.getLength();
            if (lastRight == to)
                tokens.add(lastToken);
            else if (lastToken.isString())
                tokens.add(lastToken.getSubstringToken(0, to - lastToken.getStartCoordinate()));
        }
        return tokens;
    }

    /**
     * Convert this TokenizedString to String; must be used only before tokenizing anything.
     *
     * @return full query string
     */
    String getOneString() {
        assertNotTokenized();
        return tokenizedString.get(0).getString();
    }

    /**
     * Return a part of TokenizedString that must be one String, or throw exception if this is not one String.
     *
     * @param from left coordinate, inclusive
     * @param to right coordinate, exclusive
     * @return part of TokenizedString as String
     */
    String getOneString(int from, int to) {
        ArrayList<Token> tokenizedStringPart = getTokens(from, to);
        if ((tokenizedStringPart.size() != 1) || !tokenizedStringPart.get(0).isString())
            throw new IllegalStateException("Expected only 1 string in tokenizedStringPart, got this: "
                    + tokenizedStringPart);
        return tokenizedStringPart.get(0).getString();
    }

    /**
     * Get final pattern when string is fully tokenized.
     *
     * @return final pattern from fully tokenized string
     * @throws ParserException if string was not fully tokenized
     */
    Pattern getFinalPattern() throws ParserException {
        assertFullyTokenized();
        Pattern finalPattern = tokenizedString.get(0).getPattern();
        if (finalPattern instanceof FullReadPattern)
            finalPattern = ((FullReadPattern)finalPattern).setTargetId((byte)1);
        return finalPattern;
    }

    void checkNotParsedNullPatterns() throws ParserException {
        if (tokenizedString.stream().anyMatch(Token::isNullPattern))
            throw new ParserException("Some tokens not parsed: " + tokenizedString);
    }

    private void assertNotTokenized() {
        if ((tokenizedString.size() != 1) || !tokenizedString.get(0).isString())
            throw new IllegalStateException("Expected to find one string, found: " + tokenizedString);
    }

    private void assertFullyTokenized() throws ParserException {
        List<Token> strings = tokenizedString.stream().filter(Token::isString).collect(Collectors.toList());
        if (strings.size() > 0)
            throw new ParserException("Some tokens not parsed: " + strings);
        if (tokenizedString.size() > 1)
            throw new ParserException("After parsing, string contains separate patterns instead of 1 final pattern!");
        if (tokenizedString.size() < 1)
            throw new IllegalStateException("After parsing, tokenizedString size is " + tokenizedString.size());
    }

    private void assertChainNotBroken() {
        for (int i = 0; i < tokenizedString.size(); i++) {
            int expectedEnd = (i < tokenizedString.size() - 1) ? tokenizedString.get(i + 1).getStartCoordinate() : length;
            Token currentToken = tokenizedString.get(i);
            if (currentToken.getStartCoordinate() + currentToken.getLength() != expectedEnd)
                throw new IllegalStateException("Broken chain on token " + i + " ("
                        + currentToken.toString() + "): start coordinate = " + currentToken.getStartCoordinate()
                        + ", length = " + currentToken.getLength() + ", expected end = " + expectedEnd);
        }
    }
}
