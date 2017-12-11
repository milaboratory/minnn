package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.mist.util.*;

import java.util.Arrays;

import static com.milaboratory.mist.pattern.MatchValidationType.LOGICAL_OR;
import static com.milaboratory.mist.util.UnfairSorterConfiguration.unfairSorterPortLimits;

public final class OrOperator extends MultipleReadsOperator {
    public OrOperator(PatternAligner patternAligner, MultipleReadsOperator... operandPatterns) {
        super(patternAligner, false, operandPatterns);
    }

    @Override
    public String toString() {
        return "OrOperator(" + Arrays.toString(operandPatterns) + ")";
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target) {
        return new OrOperatorMatchingResult(target);
    }

    @Override
    public long estimateComplexity() {
        return Arrays.stream(operandPatterns).mapToLong(Pattern::estimateComplexity).max()
                .orElseThrow(IllegalStateException::new);
    }

    private class OrOperatorMatchingResult implements MatchingResult {
        private final MultiNSequenceWithQuality target;

        OrOperatorMatchingResult(MultiNSequenceWithQuality target) {
            this.target = target;
        }

        @Override
        public OutputPort<Match> getMatches(boolean fairSorting) {
            ApproximateSorterConfiguration conf = new ApproximateSorterConfiguration(target, patternAligner,
                    false, false, fairSorting, LOGICAL_OR,
                    unfairSorterPortLimits.get(OrOperator.class), operandPatterns);
            return new ApproximateSorter(conf).getOutputPort();
        }
    }
}
