package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.mist.util.*;

import java.util.Arrays;

import static com.milaboratory.mist.pattern.MatchValidationType.LOGICAL_AND;
import static com.milaboratory.mist.util.UnfairSorterConfiguration.unfairSorterPortLimits;

public final class MultiPattern extends MultipleReadsOperator {
    public MultiPattern(PatternAligner patternAligner, SinglePattern... singlePatterns) {
        super(patternAligner, singlePatterns);
        for (byte i = 1; i <= singlePatterns.length; i++)
            singlePatterns[i - 1].setTargetId(i);
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

        return new MultiPatternMatchingResult(patternAligner, singlePatterns, target);
    }

    private static class MultiPatternMatchingResult extends MatchingResult {
        private final PatternAligner patternAligner;
        private final SinglePattern[] singlePatterns;
        private final MultiNSequenceWithQuality target;

        MultiPatternMatchingResult(PatternAligner patternAligner, SinglePattern[] singlePatterns,
                                   MultiNSequenceWithQuality target) {
            this.patternAligner = patternAligner;
            this.singlePatterns = singlePatterns;
            this.target = target;
        }

        @Override
        public OutputPort<Match> getMatches(boolean fairSorting) {
            ApproximateSorterConfiguration conf = new ApproximateSorterConfiguration(target, patternAligner,
                    true, true, fairSorting, LOGICAL_AND,
                    unfairSorterPortLimits.get(MultiPattern.class), singlePatterns);
            return new ApproximateSorter(conf).getOutputPort();
        }
    }
}
