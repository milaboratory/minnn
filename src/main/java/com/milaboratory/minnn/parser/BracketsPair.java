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

final class BracketsPair extends CharPair {
    final BracketsType bracketsType;
    final int nestedLevel;

    /**
     * Pair of brackets.
     *
     * @param bracketsType parentheses, square brackets or braces
     * @param start opening bracket position in query
     * @param end closing bracket position in query
     * @param nestedLevel number of brackets of any type inside which this one is placed; 0 is the top level
     */
    BracketsPair(BracketsType bracketsType, int start, int end, int nestedLevel) {
        super(start, end);
        this.bracketsType = bracketsType;
        this.nestedLevel = nestedLevel;
    }

    /**
     * Returns true if this brackets pair is inside the specified brackets pair, otherwise false.
     *
     * @param outer other brackets pair
     * @return is this brackets pair inside specified brackets pair
     */
    boolean inside(BracketsPair outer) {
        return (start > outer.start) && (end < outer.end) && (nestedLevel > outer.nestedLevel);
    }

    /**
     * Returns true if this brackets pair contains the specified brackets pair inside, otherwise false.
     *
     * @param inner other brackets pair
     * @return is this brackets pair contain specified brackets pair
     */
    boolean contains(BracketsPair inner) {
        return (start < inner.start) && (end > inner.end) && (nestedLevel < inner.nestedLevel);
    }

    @Override
    public String toString() {
        return "BracketsPair{" + "bracketsType=" + bracketsType + ", start=" + start + ", end=" + end
                + ", nestedLevel=" + nestedLevel + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BracketsPair that = (BracketsPair)o;

        return (start == that.start) && (end == that.end) && (nestedLevel == that.nestedLevel)
                && (bracketsType == that.bracketsType);
    }

    @Override
    public int hashCode() {
        int result = bracketsType.hashCode();
        result = 31 * result + start;
        result = 31 * result + end;
        result = 31 * result + nestedLevel;
        return result;
    }
}
