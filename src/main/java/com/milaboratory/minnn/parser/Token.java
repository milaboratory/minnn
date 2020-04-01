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

import com.milaboratory.minnn.pattern.*;

final class Token {
    private final boolean isString;
    private final String string;
    private final Pattern pattern;
    private final int length;
    private final int startCoordinate;

    Token(String string, int startCoordinate) {
        this.isString = true;
        this.string = string;
        this.pattern = null;
        this.length = string.length();
        this.startCoordinate = startCoordinate;
    }

    Token(Pattern pattern, int length, int startCoordinate) {
        this.isString = false;
        this.string = null;
        this.pattern = pattern;
        this.length = length;
        this.startCoordinate = startCoordinate;
    }

    boolean isString() {
        return isString;
    }

    boolean isPatternAndNotNull() {
        return !isString && (pattern != null);
    }

    boolean isNullPattern() {
        return !isString && (pattern == null);
    }

    String getString() {
        if (isString)
            return string;
        else
            throw new IllegalStateException("Trying to get string, but this is pattern token: " + pattern);
    }

    Pattern getPattern() {
        if (isString)
            throw new IllegalStateException("Trying to get pattern, but this is string token: " + string);
        else if (pattern == null)
            throw new IllegalStateException("Trying to get pattern, but pattern is null!");
        else
            return pattern;
    }

    Pattern getNullablePattern() {
        if (!isString)
            return pattern;
        else
            throw new IllegalStateException("Trying to get nullable pattern, but this is string token: " + string);
    }

    int getLength() {
        return length;
    }

    int getStartCoordinate() {
        return startCoordinate;
    }

    String getSubstring(int start) {
        return getString().substring(start);
    }

    String getSubstring(int start, int end) {
        return getString().substring(start, end);
    }

    Token getSubstringToken(int start) {
        return new Token(getSubstring(start), startCoordinate + start);
    }

    Token getSubstringToken(int start, int end) {
        return new Token(getSubstring(start, end), startCoordinate + start);
    }

    SinglePattern getSinglePattern() throws ParserException {
        if (getPattern() instanceof SinglePattern)
            return (SinglePattern)pattern;
        else throw new ParserException("Expected SinglePattern, but got " + pattern);
    }

    SinglePattern getSinglePatternExceptAnyPattern() throws ParserException {
        SinglePattern pattern = getSinglePattern();
        if (pattern instanceof AnyPattern)
            throw new ParserException("'*' pattern is invalid if there are other patterns in the same read, "
                    + "use 'n{*}' instead!");
        return pattern;
    }

    MultipleReadsOperator getMultipleReadsOperator() throws ParserException {
        if (getPattern() instanceof MultipleReadsOperator)
            return (MultipleReadsOperator)pattern;
        else throw new ParserException("Expected MultipleReadsOperator, but got " + pattern);
    }

    @SuppressWarnings("unchecked")
    <P extends Pattern> P getSpecificPattern(Class<P> patternClass) throws ParserException {
        try {
            if (patternClass.isAssignableFrom(getPattern().getClass()))
                return (P)pattern;
            else throw new ParserException("Expected " + patternClass.getName() + ", but got " + pattern);
        } catch (ClassCastException e) {
            throw new IllegalStateException("Unexpected class cast exception with " + pattern + ": " + e);
        }
    }

    @Override
    public String toString() {
        return "Token{" + "isString=" + isString + ", string=" + string + ", pattern=" + pattern + ", length=" + length
                + ", startCoordinate=" + startCoordinate + "}";
    }
}
