package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.util.ArrayList;

/**
 * Filter pattern can be used for both single and multiple patterns; it overrides match() methods for both single
 * and multiple patterns. It filters matches from pattern with specified Filter. For usage with MultipleReadsOperator
 * patterns, it must be wrapped with MultipleReadsFilterPattern.
 */
public final class FilterPattern extends SinglePattern {
    private final Filter filter;
    private final Pattern pattern;

    public FilterPattern(PatternAligner patternAligner, Filter filter, Pattern pattern) {
        super(patternAligner);
        testAlignersCompatibility(pattern);
        this.filter = filter;
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return "FilterPattern(" + filter + ", " + pattern + ")";
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        return pattern.getGroupEdges();
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target) {
        return new FilterMatchingResult(filter, pattern, target);
    }

    @Override
    public MatchingResult match(NSequenceWithQuality target, int from, int to) {
        return new FilterMatchingResult(filter, pattern, target, from, to);
    }

    @Override
    public int estimateMaxLength() {
        if (pattern instanceof SinglePattern)
            return ((SinglePattern)pattern).estimateMaxLength();
        else
            throw new IllegalStateException("estimateMaxLength() called for argument of class " + pattern.getClass());
    }

    @Override
    void setTargetId(byte targetId) {
        super.setTargetId(targetId);
        if (pattern instanceof SinglePattern)
            ((SinglePattern)pattern).setTargetId(targetId);
        else
            throw new IllegalStateException("setTargetId() called for argument of class " + pattern.getClass());
    }

    private static class FilterMatchingResult extends MatchingResult {
        private final Filter filter;
        private final Pattern pattern;
        private final MultiNSequenceWithQuality targetMulti;
        private final NSequenceWithQuality targetSingle;
        private final int from;
        private final int to;

        FilterMatchingResult(Filter filter, Pattern pattern, MultiNSequenceWithQuality targetMulti) {
            this(filter, pattern, targetMulti, null, 0, 0);
        }

        FilterMatchingResult(Filter filter, Pattern pattern, NSequenceWithQuality targetSingle, int from, int to) {
            this(filter, pattern, null, targetSingle, from, to);
        }

        private FilterMatchingResult(Filter filter, Pattern pattern, MultiNSequenceWithQuality targetMulti,
                                     NSequenceWithQuality targetSingle, int from, int to) {
            this.filter = filter;
            this.pattern = pattern;
            this.targetMulti = targetMulti;
            this.targetSingle = targetSingle;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<Match> getMatches(boolean byScore, boolean fairSorting) {
            if (targetMulti != null) {
                if (!(pattern instanceof MultipleReadsOperator)) throw new IllegalArgumentException(
                        "Trying to use filter with single-target pattern and multi-target match arguments.");
                return new FilterOutputPort(filter, pattern.match(targetMulti).getMatches(byScore, fairSorting));
            } else if (targetSingle != null) {
                if (!(pattern instanceof SinglePattern)) throw new IllegalArgumentException(
                        "Trying to use filter with multi-target pattern and single-target match arguments.");
                return new FilterOutputPort(filter, ((SinglePattern)pattern).match(targetSingle, from, to)
                        .getMatches(byScore, fairSorting));
            } else throw new IllegalStateException("Both targetMulti and targetSingle are null.");
        }

        private static class FilterOutputPort implements OutputPort<Match> {
            private final Filter filter;
            private final OutputPort<Match> operandPort;

            FilterOutputPort(Filter filter, OutputPort<Match> operandPort) {
                this.filter = filter;
                this.operandPort = operandPort;
            }

            @Override
            public Match take() {
                Match currentMatch, currentFilteredMatch;
                do {
                    currentMatch = operandPort.take();
                    if (currentMatch == null)
                        return null;
                    currentFilteredMatch = filter.checkMatch(currentMatch);
                } while (currentFilteredMatch == null);

                return currentFilteredMatch;
            }
        }
    }
}
