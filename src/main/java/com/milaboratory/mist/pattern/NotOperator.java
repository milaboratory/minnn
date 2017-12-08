package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;

import java.util.ArrayList;

public final class NotOperator extends MultipleReadsOperator {
    public NotOperator(PatternAligner patternAligner, MultipleReadsOperator... operandPatterns) {
        super(patternAligner, operandPatterns);
        if (operandPatterns.length != 1)
            throw new IllegalArgumentException("Not operator must take exactly 1 operand!");
        if (groupEdges.size() > 0)
            throw new IllegalStateException("Not operator must not contain group edges inside!");
    }

    @Override
    public String toString() {
        return "NotOperator(" + operandPatterns[0] + ")";
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target) {
        return new NotOperatorMatchingResult(patternAligner, operandPatterns[0], target);
    }

    @Override
    public long estimateComplexity() {
        return operandPatterns[0].estimateComplexity();
    }

    private static class NotOperatorMatchingResult implements MatchingResult {
        private final PatternAligner patternAligner;
        private final MultipleReadsOperator operandPattern;
        private final MultiNSequenceWithQuality target;

        NotOperatorMatchingResult(PatternAligner patternAligner, MultipleReadsOperator operandPattern,
                                 MultiNSequenceWithQuality target) {
            this.patternAligner = patternAligner;
            this.operandPattern = operandPattern;
            this.target = target;
        }

        @Override
        public OutputPort<Match> getMatches(boolean fairSorting) {
            return new NotOperatorOutputPort(patternAligner, operandPattern.match(target).getMatches(fairSorting));
        }

        private static class NotOperatorOutputPort implements OutputPort<Match> {
            private final PatternAligner patternAligner;
            private final OutputPort<Match> operandPort;
            private boolean firstCall = true;
            private boolean operandIsMatching;

            NotOperatorOutputPort(PatternAligner patternAligner, OutputPort<Match> operandPort) {
                this.patternAligner = patternAligner;
                this.operandPort = operandPort;
            }

            @Override
            public Match take() {
                if (!firstCall) return null;
                operandIsMatching = (operandPort.take() != null);
                firstCall = false;
                if (operandIsMatching)
                    return null;
                else {
                    return new Match(1, patternAligner.notResultScore(),
                            -1, -1,
                            new ArrayList<>(), new NullMatchedRange(0));
                }
            }
        }
    }
}
