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

    @Override
    public ArrayList<GroupEdge> getGroupEdges(int patternIndex) {
        if (singlePatterns.length == 0) {
            ArrayList<GroupEdge> edges = new ArrayList<>();
            Arrays.stream(operandPatterns).map(op -> op.getGroupEdges(patternIndex)).forEach(edges::addAll);
            return edges;
        } else
            return singlePatterns[patternIndex].getGroupEdges();
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
