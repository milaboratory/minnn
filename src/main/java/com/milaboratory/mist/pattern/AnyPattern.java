package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.util.ArrayList;
import java.util.stream.Collectors;

public final class AnyPattern extends SinglePattern {
    private final ArrayList<GroupEdge> groupEdges;

    public AnyPattern(PatternAligner patternAligner, ArrayList<GroupEdge> groupEdges) {
        super(patternAligner);
        this.groupEdges = groupEdges;
    }

    @Override
    public String toString() {
        if (groupEdges.size() > 0)
            return "AnyPattern(" + groupEdges + ")";
        else
            return "AnyPattern()";
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        return groupEdges;
    }

    @Override
    public MatchingResult match(NSequenceWithQuality target, int from, int to) {
        return new AnyPatternMatchingResult(groupEdges, target, from, to, targetId);
    }

    @Override
    public int estimateMaxLength() {
        throw new IllegalStateException("estimateMaxLength() called for AnyPattern!");
    }

    @Override
    public long estimateComplexity() {
        return 1;
    }

    private static class AnyPatternMatchingResult implements MatchingResult {
        private final ArrayList<GroupEdge> groupEdges;
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;
        private final byte targetId;

        AnyPatternMatchingResult(ArrayList<GroupEdge> groupEdges, NSequenceWithQuality target, int from, int to,
                                 byte targetId) {
            this.groupEdges = groupEdges;
            this.target = target;
            this.from = from;
            this.to = to;
            this.targetId = targetId;
        }

        @Override
        public OutputPort<Match> getMatches(boolean fairSorting) {
            return new AnyPatternOutputPort(groupEdges, target, from, to, targetId);
        }

        private static class AnyPatternOutputPort implements OutputPort<Match> {
            private final ArrayList<GroupEdge> groupEdges;
            private final NSequenceWithQuality target;
            private final int from;
            private final int to;
            private final byte targetId;
            private boolean firstTake = true;

            AnyPatternOutputPort(ArrayList<GroupEdge> groupEdges, NSequenceWithQuality target, int from, int to,
                                 byte targetId) {
                this.groupEdges = groupEdges;
                this.target = target;
                this.from = from;
                this.to = to;
                this.targetId = targetId;
            }

            @Override
            public Match take() {
                if (!firstTake) return null;
                firstTake = false;

                MatchedRange matchedRange = new MatchedRange(target, targetId, 0, new Range(from, to));
                ArrayList<MatchedGroupEdge> matchedGroupEdges = groupEdges.stream()
                        .map(ge -> new MatchedGroupEdge(target, targetId, 0, ge,
                                ge.isStart() ? 0 : target.size()))
                        .collect(Collectors.toCollection(ArrayList::new));
                return new Match(1, 0, -1, -1,
                        matchedGroupEdges, matchedRange);
            }
        }
    }
}
