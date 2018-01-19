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
