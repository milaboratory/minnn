package com.milaboratory.mist.pattern;

import java.util.*;

public abstract class MultipleReadsOperator extends Pattern {
    protected final MultipleReadsOperator[] operandPatterns;
    protected final SinglePattern[] singlePatterns;
    private final boolean checkGroupEdges;
    private final boolean singlePatternOperands;
    private ArrayList<GroupEdge> groupEdges = null;

    MultipleReadsOperator(PatternAligner patternAligner, MultipleReadsOperator... operandPatterns) {
        this(patternAligner, true, operandPatterns);
    }

    MultipleReadsOperator(PatternAligner patternAligner, boolean checkGroupEdges,
                          MultipleReadsOperator... operandPatterns) {
        super(patternAligner);
        this.checkGroupEdges = checkGroupEdges;
        this.operandPatterns = operandPatterns;
        this.singlePatterns = new SinglePattern[0];
        this.singlePatternOperands = false;
    }

    MultipleReadsOperator(PatternAligner patternAligner, SinglePattern... singlePatterns) {
        super(patternAligner);
        this.checkGroupEdges = true;
        this.singlePatterns = singlePatterns;
        this.operandPatterns = new MultipleReadsOperator[0];
        this.singlePatternOperands = true;
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        if (groupEdges == null) {
            groupEdges = new ArrayList<>();
            for (Pattern pattern : singlePatternOperands ? singlePatterns : operandPatterns)
                groupEdges.addAll(pattern.getGroupEdges());
            if (checkGroupEdges && (groupEdges.size() != new HashSet<>(groupEdges).size()))
                throw new IllegalStateException("Operands contain equal group edges!");
        }
        return groupEdges;
    }

    public int getNumberOfPatterns() {
        return Math.max(singlePatterns.length, operandPatterns.length);
    }
}
