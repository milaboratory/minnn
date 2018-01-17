package com.milaboratory.mist.pattern;

import java.util.*;

public abstract class MultipleReadsOperator extends Pattern {
    protected final MultipleReadsOperator[] operandPatterns;
    protected final SinglePattern[] singlePatterns;
    protected final ArrayList<GroupEdge> groupEdges;

    MultipleReadsOperator(PatternAligner patternAligner, MultipleReadsOperator... operandPatterns) {
        this(patternAligner, true, operandPatterns);
    }

    MultipleReadsOperator(PatternAligner patternAligner, boolean checkGroupEdges,
                          MultipleReadsOperator... operandPatterns) {
        super(patternAligner);
        this.operandPatterns = operandPatterns;
        this.singlePatterns = new SinglePattern[0];
        this.groupEdges = new ArrayList<>();
        getGroupEdgesFromOperands(checkGroupEdges, operandPatterns);
    }

    MultipleReadsOperator(PatternAligner patternAligner, SinglePattern... singlePatterns) {
        super(patternAligner);
        this.singlePatterns = singlePatterns;
        this.operandPatterns = new MultipleReadsOperator[0];
        this.groupEdges = new ArrayList<>();
        getGroupEdgesFromOperands(true, singlePatterns);
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        return groupEdges;
    }

    public int getNumberOfPatterns() {
        return Math.max(singlePatterns.length, operandPatterns.length);
    }

    private <T extends Pattern> void getGroupEdgesFromOperands(boolean checkGroupEdges, T[] patterns) {
        for (T pattern : patterns)
            groupEdges.addAll(pattern.getGroupEdges());
        if (checkGroupEdges && (groupEdges.size() != new HashSet<>(groupEdges).size()))
            throw new IllegalStateException("Operands contain equal group edges!");
    }
}
