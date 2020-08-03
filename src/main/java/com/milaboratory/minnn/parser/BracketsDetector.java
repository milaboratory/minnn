/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
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

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.parser.BracketsType.*;
import static com.milaboratory.minnn.parser.ParserUtils.*;
import static com.milaboratory.minnn.parser.QuotesType.*;

final class BracketsDetector {
    /**
     * Get list of all quotes pairs of all types.
     *
     * @param str string to search: it may be entire parser query or substring from TokenizedString
     * @return list of all quotes pairs of all types
     */
    static List<QuotesPair> getAllQuotes(String str) throws ParserException {
        List<QuotesPair> quotesPairs = new ArrayList<>();
        OpenQuote lastOpenQuote = null;
        for (int currentPosition = 0; currentPosition < str.length(); currentPosition++) {
            if (lastOpenQuote != null) {
                if (currentPosition < str.length() - 1) {
                    switch (str.substring(currentPosition, currentPosition + 2)) {
                        case "\\\\":
                        case "\\\"":
                        case "\\\'":
                            // skip escaped backslash or escaped quotes inside quotes
                            currentPosition++;
                            continue;
                    }
                }
                switch (str.charAt(currentPosition)) {
                    case '\"':
                        if (lastOpenQuote.quotesType == DOUBLE) {
                            quotesPairs.add(new QuotesPair(DOUBLE, lastOpenQuote.position, currentPosition));
                            lastOpenQuote = null;
                        }
                        break;
                    case '\'':
                        if (lastOpenQuote.quotesType == SINGLE) {
                            quotesPairs.add(new QuotesPair(SINGLE, lastOpenQuote.position, currentPosition));
                            lastOpenQuote = null;
                        }
                }
            } else {
                switch (str.charAt(currentPosition)) {
                    case '\"':
                        lastOpenQuote = new OpenQuote(DOUBLE, currentPosition);
                        break;
                    case '\'':
                        lastOpenQuote = new OpenQuote(SINGLE, currentPosition);
                }
            }
        }
        if (lastOpenQuote != null)
            throw new ParserException("Missing closing " + (lastOpenQuote.quotesType == SINGLE ? "\'" : "\"")
                    + " in " + str);
        return quotesPairs;
    }

    /**
     * Checks is this position inside quotes (inclusive on left and right sides).
     *
     * @param quotesPairs all quotes pairs detected by getAllQuotes function
     * @param position position in string
     * @return is this position inside quotes (inclusive on left and right sides)
     */
    static boolean isInQuotes(List<QuotesPair> quotesPairs, int position) {
        for (QuotesPair quotesPair : quotesPairs)
            if (quotesPair.contains(position))
                return true;
        return false;
    }

    /**
     * String.indexOf function that searches only non-quoted tokens. It will skip tokens for which start position
     * is inside quotes.
     *
     * @param quotesPairs all quotes pairs detected by getAllQuotes function
     * @param str string in which we do the search
     * @param token token to search
     * @param currentPosition current position in string where we start the search
     * @return found position of next token; -1 if not found
     */
    static int nonQuotedIndexOf(List<QuotesPair> quotesPairs, String str, String token, int currentPosition) {
        int foundPosition;
        do {
            if (currentPosition == str.length()) return -1;
            foundPosition = str.indexOf(token, currentPosition);
            currentPosition = foundPosition + 1;
        } while (isInQuotes(quotesPairs, foundPosition));
        return foundPosition;
    }

    /**
     * Returns next position in query that is not inside quotes; used for searching non-quoted tokens in string.
     *
     * @param quotesPairs all quotes pairs detected by getAllQuotes function
     * @param currentPosition current position in string
     * @return next non-quoted position in string; string length is not checked
     */
    static int nextNonQuotedPosition(List<QuotesPair> quotesPairs, int currentPosition) {
        for (QuotesPair quotesPair : quotesPairs)
            if (quotesPair.contains(currentPosition + 1))
                if (!isInQuotes(quotesPairs, quotesPair.end + 1))
                    return quotesPair.end + 1;
                else
                    currentPosition = quotesPair.end;
        return currentPosition + 1;
    }

    /**
     * Returns previous position in query that is not inside quotes; used for searching non-quoted tokens in string.
     *
     * @param quotesPairs all quotes pairs detected by getAllQuotes function
     * @param currentPosition current position in string
     * @return previous non-quoted position in string; string length is not checked
     */
    static int previousNonQuotedPosition(List<QuotesPair> quotesPairs, int currentPosition) {
        for (int i = quotesPairs.size() - 1; i >= 0; i--) {
            QuotesPair quotesPair = quotesPairs.get(i);
            if (quotesPair.contains(currentPosition - 1))
                if (!isInQuotes(quotesPairs, quotesPair.start - 1))
                    return quotesPair.start - 1;
                else
                    currentPosition = quotesPair.start;
        }
        return currentPosition - 1;
    }

    /**
     * Get list of all bracket pairs of the specified type.
     *
     * @param bracketsType brackets type
     * @param str string to search: it may be entire parser query or substring from TokenizedString
     * @return list of all brackets pairs of the specified type
     */
    static List<BracketsPair> getAllBrackets(BracketsType bracketsType, String str) throws ParserException {
        ArrayList<BracketsPair> bracketsPairs = new ArrayList<>();
        Stack<OpenBracket> openBrackets = new Stack<>();
        List<QuotesPair> quotesPairs = getAllQuotes(str);
        int currentNestedLevel = 0;
        for (int currentPosition = 0; currentPosition < str.length(); currentPosition = nextNonQuotedPosition(
                quotesPairs, currentPosition)) {
            char currentChar = str.charAt(currentPosition);
            switch (currentChar) {
                case '(':
                    openBrackets.push(new OpenBracket(PARENTHESES, currentPosition, currentNestedLevel++));
                    break;
                case '[':
                    openBrackets.push(new OpenBracket(SQUARE, currentPosition, currentNestedLevel++));
                    break;
                case '{':
                    openBrackets.push(new OpenBracket(BRACES, currentPosition, currentNestedLevel++));
                    break;
                case ')':
                    if (currentNestedLevel == 0)
                        throw new ParserException("Found ')' without '(' in " + str);
                    if (openBrackets.peek().bracketType != PARENTHESES)
                        throw new ParserException("Found ')' after opening bracket of type "
                                + openBrackets.peek().bracketType + " in " + str);
                    bracketsPairs.add(new BracketsPair(PARENTHESES, openBrackets.pop().position, currentPosition,
                            --currentNestedLevel));
                    break;
                case ']':
                    if (currentNestedLevel == 0)
                        throw new ParserException("Found ']' without '[' in " + str);
                    if (openBrackets.peek().bracketType != SQUARE)
                        throw new ParserException("Found ']' after opening bracket of type "
                                + openBrackets.peek().bracketType + " in " + str);
                    bracketsPairs.add(new BracketsPair(SQUARE, openBrackets.pop().position, currentPosition,
                            --currentNestedLevel));
                    break;
                case '}':
                    if (currentNestedLevel == 0)
                        throw new ParserException("Found '}' without '{' in " + str);
                    if (openBrackets.peek().bracketType != BRACES)
                        throw new ParserException("Found '}' after opening bracket of type "
                                + openBrackets.peek().bracketType + " in " + str);
                    bracketsPairs.add(new BracketsPair(BRACES, openBrackets.pop().position, currentPosition,
                            --currentNestedLevel));
                    break;
            }
        }
        if (currentNestedLevel > 0)
            throw new ParserException("Missing " + currentNestedLevel + " closing bracket(s) in " + str);
        return bracketsPairs.stream().filter(bp -> bp.bracketsType == bracketsType).collect(Collectors.toList());
    }

    /**
     * Get braces that used for setting number for cutting FuzzyMatchPattern (NORMAL syntax).
     *
     * @param query query string as it came to the parser
     * @param bracesPairs all braces pairs from the query
     * @return list of braces that used for cutting FuzzyMatchPattern, with sides (left/right) and numbers in braces
     */
    static List<BorderBracesPair> getBorderBraces(
            String query, List<BracketsPair> bracesPairs) throws ParserException {
        ArrayList<BorderBracesPair> borderBracesPairs = new ArrayList<>();
        for (BracketsPair bracesPair : bracesPairs)
            if ((bracesPair.start > 0) && ("<>".contains(query.substring(bracesPair.start - 1,
                    bracesPair.start)))) {
                int numberOfRepeats = toInt(query.substring(bracesPair.start + 1, bracesPair.end),
                        "number of repeats for border filter");
                borderBracesPairs.add(new BorderBracesPair(bracesPair,
                        query.charAt(bracesPair.start - 1) == '<', numberOfRepeats));
            }
        return borderBracesPairs;
    }

    /**
     * Filter list of braces, return only that are used for repeat patterns (NORMAL syntax).
     *
     * @param bracesPairs all braces pairs from the query
     * @param borderBracesPairs found list of border braces pairs
     * @return list of braces that used for repeat patterns
     */
    static List<BracketsPair> getRepeatPatternBraces(
            List<BracketsPair> bracesPairs, List<BorderBracesPair> borderBracesPairs) {
        return bracesPairs.stream().filter(bp -> borderBracesPairs.stream()
                .noneMatch(bf -> bf.start == bp.start)).collect(Collectors.toList());
    }

    /**
     * Get closing bracket coordinate by opening bracket coordinate from a list of bracket pairs.
     *
     * @param bracketsPairs list of bracket pairs
     * @param start opening bracket coordinate
     * @return closing bracket coordinate
     */
    static int getEndByStart(List<BracketsPair> bracketsPairs, int start) {
        for (BracketsPair bracketsPair : bracketsPairs)
            if (bracketsPair.start == start)
                return bracketsPair.end;
        throw new IllegalArgumentException("List of bracket pairs " + bracketsPairs
                + " doesn't contain bracket with start " + start);
    }

    private static class OpenQuote {
        final QuotesType quotesType;
        final int position;

        OpenQuote(QuotesType quotesType, int position) {
            this.quotesType = quotesType;
            this.position = position;
        }
    }

    private static class OpenBracket {
        final BracketsType bracketType;
        final int position;
        final int nestedLevel;

        OpenBracket(BracketsType bracketType, int position, int nestedLevel) {
            this.bracketType = bracketType;
            this.position = position;
            this.nestedLevel = nestedLevel;
        }
    }
}
