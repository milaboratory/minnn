package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;

import java.util.ArrayList;

public final class NotOperator extends MultipleReadsOperator {
    public NotOperator(PatternAligner patternAligner, MultipleReadsOperator... operandPatterns) {
        super(patternAligner, operandPatterns);
        if (operandPatterns.length != 1)
            throw new IllegalArgumentException("Not operator must take exactly 1 operand!");
    }

    @Override
    public String toString() {
        return "NotOperator(" + operandPatterns[0] + ")";
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        return new ArrayList<>();
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target) {
        return new NotOperatorMatchingResult(operandPatterns[0], target);
    }

    @Override
    public long estimateComplexity() {
        return operandPatterns[0].estimateComplexity();
    }

    private class NotOperatorMatchingResult implements MatchingResult {
        private final MultipleReadsOperator operandPattern;
        private final MultiNSequenceWithQuality target;

        NotOperatorMatchingResult(MultipleReadsOperator operandPattern, MultiNSequenceWithQuality target) {
            this.operandPattern = operandPattern;
            this.target = target;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            return new NotOperatorOutputPort(patternAligner, operandPattern.match(target).getMatches(fairSorting));
        }

        private class NotOperatorOutputPort implements OutputPort<MatchIntermediate> {
            private final OutputPort<MatchIntermediate> operandPort;
            private boolean firstCall = true;
            private boolean operandIsMatching;

            NotOperatorOutputPort(PatternAligner patternAligner, OutputPort<MatchIntermediate> operandPort) {
                this.operandPort = operandPort;
            }

            @Override
            public MatchIntermediate take() {
                if (!firstCall) return null;
                operandIsMatching = (operandPort.take() != null);
                firstCall = false;
                if (operandIsMatching)
                    return null;
                else {
                    return new MatchIntermediate(1, patternAligner.notResultScore(),
                            -1, -1,
                            new ArrayList<>(), new NullMatchedRange(0));
                }
            }
        }
    }
}
