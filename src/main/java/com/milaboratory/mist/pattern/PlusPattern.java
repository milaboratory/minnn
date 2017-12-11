package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.util.*;

import java.util.Arrays;

import static com.milaboratory.mist.pattern.MatchValidationType.ORDER;
import static com.milaboratory.mist.util.UnfairSorterConfiguration.unfairSorterPortLimits;

public final class PlusPattern extends MultiplePatternsOperator implements CanFixBorders {
    public PlusPattern(PatternAligner patternAligner, SinglePattern... operandPatterns) {
        super(patternAligner, operandPatterns);
    }

    @Override
    public String toString() {
        return "PlusPattern(" + Arrays.toString(operandPatterns) + ")";
    }

    @Override
    public MatchingResult match(NSequenceWithQuality target, int from, int to) {
        return new PlusPatternMatchingResult(target, from, to);
    }

    @Override
    public long estimateComplexity() {
        return Arrays.stream(operandPatterns).mapToLong(Pattern::estimateComplexity).sum();
    }

    @Override
    public void fixBorder(boolean left, int position) {
        int targetOperandIndex = left ? 0 : operandPatterns.length - 1;
        if (operandPatterns[targetOperandIndex] instanceof CanFixBorders)
            ((CanFixBorders)(operandPatterns[targetOperandIndex])).fixBorder(left, position);
    }

    @Override
    public boolean isBorderFixed(boolean left) {
        int targetOperandIndex = left ? 0 : operandPatterns.length - 1;
        return operandPatterns[targetOperandIndex] instanceof CanFixBorders
                && ((CanFixBorders)(operandPatterns[targetOperandIndex])).isBorderFixed(left);
    }

    private class PlusPatternMatchingResult implements MatchingResult {
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;

        PlusPatternMatchingResult(NSequenceWithQuality target, int from, int to) {
            this.target = target;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<Match> getMatches(boolean fairSorting) {
            ApproximateSorterConfiguration conf = new ApproximateSorterConfiguration(target, from, to, patternAligner,
                    true, fairSorting, ORDER, unfairSorterPortLimits.get(PlusPattern.class),
                    operandPatterns);
            return new ApproximateSorter(conf).getOutputPort();
        }
    }
}
