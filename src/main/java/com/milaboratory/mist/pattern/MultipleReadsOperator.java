package com.milaboratory.mist.pattern;

import java.util.*;

public abstract class MultipleReadsOperator extends Pattern {
    protected final MultipleReadsOperator[] operandPatterns;
    protected final SinglePattern[] singlePatterns;
    private final boolean singlePatternOperands;
    private HashSet<GroupEdge> groupEdges = null;

    MultipleReadsOperator(PatternAligner patternAligner, MultipleReadsOperator... operandPatterns) {
        super(patternAligner);
        this.operandPatterns = operandPatterns;
        this.singlePatterns = new SinglePattern[0];
        this.singlePatternOperands = false;
    }

    MultipleReadsOperator(PatternAligner patternAligner, SinglePattern... singlePatterns) {
        super(patternAligner);
        this.singlePatterns = singlePatterns;
        this.operandPatterns = new MultipleReadsOperator[0];
        this.singlePatternOperands = true;
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        if (groupEdges == null) {
            groupEdges = new HashSet<>();
            for (Pattern pattern : singlePatternOperands ? singlePatterns : operandPatterns)
                groupEdges.addAll(pattern.getGroupEdges());
        }
        return new ArrayList<>(groupEdges);
    }

    public int getNumberOfPatterns() {
        return Math.max(singlePatterns.length, operandPatterns.length);
    }
}
