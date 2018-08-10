/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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
package com.milaboratory.mist.util;

import com.milaboratory.mist.pattern.*;

import java.util.HashMap;
import java.util.HashSet;

public final class UnfairSorterConfiguration {
    public static final HashMap<Class, Integer> unfairSorterPortLimits = new HashMap<>();
    public static final HashMap<Character, Integer> lettersComplexity = new HashMap<>();
    public static final int smallLetterExtraComplexity = 3;
    static {
        unfairSorterPortLimits.put(FuzzyMatchPattern.class, 100);
        unfairSorterPortLimits.put(RepeatPattern.class, 100);
        unfairSorterPortLimits.put(AnyPattern.class, 1);
        unfairSorterPortLimits.put(FilterPattern.class, 25);
        unfairSorterPortLimits.put(AndPattern.class, 20);
        unfairSorterPortLimits.put(PlusPattern.class, 20);
        unfairSorterPortLimits.put(SequencePattern.class, 20);
        unfairSorterPortLimits.put(OrPattern.class, 25);
        unfairSorterPortLimits.put(FullReadPattern.class, 25);
        unfairSorterPortLimits.put(MultiPattern.class, 25);
        unfairSorterPortLimits.put(AndOperator.class, 25);
        unfairSorterPortLimits.put(OrOperator.class, 25);
        unfairSorterPortLimits.put(NotOperator.class, 1);
        unfairSorterPortLimits.put(MultipleReadsFilterPattern.class, 25);

        lettersComplexity.put('A', 1);
        lettersComplexity.put('T', 1);
        lettersComplexity.put('G', 1);
        lettersComplexity.put('C', 1);
        lettersComplexity.put('W', 4);
        lettersComplexity.put('S', 4);
        lettersComplexity.put('M', 4);
        lettersComplexity.put('K', 4);
        lettersComplexity.put('R', 4);
        lettersComplexity.put('Y', 4);
        lettersComplexity.put('B', 9);
        lettersComplexity.put('V', 9);
        lettersComplexity.put('H', 9);
        lettersComplexity.put('D', 9);
        lettersComplexity.put('N', 16);
        new HashSet<>(lettersComplexity.keySet())
                .forEach(l -> lettersComplexity.put(Character.toLowerCase(l),
                        lettersComplexity.get(l) + smallLetterExtraComplexity));
    }
    public static final String nLetters = "Nn";
    public static final int specificPortLimit = 3;
    public static final int approximateSorterStage1Depth = 3;
    public static final long fixedSequenceMaxComplexity = 50;
    public static final long notFixedSequenceMinComplexity = 30;
    public static final long singleNucleotideComplexity = 300;
    public static final int repeatsRangeEstimation = 15;
}
