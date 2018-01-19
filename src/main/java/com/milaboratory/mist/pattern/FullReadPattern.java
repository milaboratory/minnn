package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.util.ArrayList;

public final class FullReadPattern extends SinglePattern {
    private final SinglePattern operandPattern;
    private final boolean defaultGroupsOverride;
    private boolean targetIdInitialized = false;

    public FullReadPattern(PatternAligner patternAligner, boolean defaultGroupsOverride, SinglePattern operandPattern) {
        super(patternAligner);
        this.operandPattern = operandPattern;
        this.defaultGroupsOverride = defaultGroupsOverride;
    }

    @Override
    public String toString() {
        return "FullReadPattern(" + defaultGroupsOverride + ", " + operandPattern + ")";
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        if (!targetIdInitialized)
            throw new IllegalStateException(
                    "getGroupEdges() called for FullReadPattern when targetId is not initialized!");
        if (defaultGroupsOverride)
            return operandPattern.getGroupEdges();
        else {
            String mainGroupName = "R" + targetId;
            ArrayList<GroupEdge> groupEdges = new ArrayList<>(operandPattern.getGroupEdges());
            groupEdges.add(new GroupEdge(mainGroupName, true));
            groupEdges.add(new GroupEdge(mainGroupName, false));
            return groupEdges;
        }
    }

    public ArrayList<GroupEdge> getOperandGroupEdges() {
        return operandPattern.getGroupEdges();
    }

    @Override
    public MatchingResult match(NSequenceWithQuality target, int from, int to) {
        return new FullReadPatternMatchingResult(target, from, to);
    }

    @Override
    public int estimateMaxLength() {
        throw new IllegalStateException("estimateMaxLength() called for FullReadPattern!");
    }

    @Override
    public long estimateComplexity() {
        return operandPattern.estimateComplexity();
    }

    @Override
    public void setTargetId(byte targetId) {
        super.setTargetId(targetId);
        targetIdInitialized = true;
    }

    public SinglePattern getOperand() {
        return operandPattern;
    }

    private class FullReadPatternMatchingResult implements MatchingResult {
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;

        public FullReadPatternMatchingResult(NSequenceWithQuality target, int from, int to) {
            this.target = target;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            OutputPort<MatchIntermediate> operandPort = operandPattern.match(target, from, to).getMatches(fairSorting);
            return defaultGroupsOverride ? operandPort : () -> {
                MatchIntermediate match = operandPort.take();
                if (match == null) return null;
                String mainGroupName = "R" + targetId;
                int patternIndex = match.getMatchedRange().getPatternIndex();
                ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>(match.getMatchedGroupEdges());
                matchedGroupEdges.add(new MatchedGroupEdge(target, targetId, patternIndex,
                        new GroupEdge(mainGroupName, true), 0));
                matchedGroupEdges.add(new MatchedGroupEdge(target, targetId, patternIndex,
                        new GroupEdge(mainGroupName, false), target.size()));
                return new MatchIntermediate(1, match.getScore(),
                        -1, -1, matchedGroupEdges, match.getMatchedRanges());
            };
        }
    }
}
