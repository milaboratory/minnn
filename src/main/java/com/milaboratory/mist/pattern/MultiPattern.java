package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.mist.util.*;

import java.util.Arrays;

import static com.milaboratory.mist.pattern.MatchValidationType.LOGICAL_AND;
import static com.milaboratory.mist.util.UnfairSorterConfiguration.unfairSorterPortLimits;

public final class MultiPattern extends MultipleReadsOperator {
    public MultiPattern(PatternAligner patternAligner, SinglePattern... operandPatterns) {
        super(patternAligner, operandPatterns);
        for (byte i = 0; i < operandPatterns.length; i++) {
            if (!(operandPatterns[i] instanceof FullReadPattern))
                throw new IllegalArgumentException("All MultiPattern arguments must be FullReadPattern, got "
                        + operandPatterns[i]);
            operandPatterns[i].setTargetId((byte)(i + 1));
        }
    }

    @Override
    public String toString() {
        return "MultiPattern(" + Arrays.toString(singlePatterns) + ")";
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target) {
        if (target.numberOfSequences() != singlePatterns.length)
            throw new IllegalArgumentException("Mismatched number of reads (" + target.numberOfSequences()
                    + ") and patterns (" + singlePatterns.length + ")!");

        return new MultiPatternMatchingResult(target);
    }

    @Override
    public long estimateComplexity() {
        return Arrays.stream(singlePatterns).mapToLong(Pattern::estimateComplexity).sum();
    }

    private class MultiPatternMatchingResult implements MatchingResult {
        private final MultiNSequenceWithQuality target;

        MultiPatternMatchingResult(MultiNSequenceWithQuality target) {
            this.target = target;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            ApproximateSorterConfiguration conf = new ApproximateSorterConfiguration(target, patternAligner,
                    true, true, fairSorting, LOGICAL_AND,
                    unfairSorterPortLimits.get(MultiPattern.class), singlePatterns);
            return new ApproximateSorter(conf).getOutputPort();
        }
    }
}
