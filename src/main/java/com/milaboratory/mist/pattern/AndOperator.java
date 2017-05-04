package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.mist.util.ApproximateSorter;
import com.milaboratory.mist.util.SorterByCoordinate;
import com.milaboratory.mist.util.SorterByScore;

import java.util.ArrayList;

public class AndOperator extends MultipleReadsOperator {
    public AndOperator(MultipleReadsOperator... operandPatterns) {
        super(operandPatterns);
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target, Range[] ranges, boolean[] reverseComplements) {
        return new AndOperatorMatchingResult(operandPatterns, target, ranges, reverseComplements);
    }

    private static class AndOperatorMatchingResult extends MatchingResult {
        private final MultipleReadsOperator[] operandPatterns;
        private final MultiNSequenceWithQuality target;
        private final Range[] ranges;
        private final boolean[] reverseComplements;

        AndOperatorMatchingResult(MultipleReadsOperator[] operandPatterns,
                                  MultiNSequenceWithQuality target, Range[] ranges, boolean[] reverseComplements) {
            this.operandPatterns = operandPatterns;
            this.target = target;
            this.ranges = ranges;
            this.reverseComplements = reverseComplements;
        }

        @Override
        public OutputPort<Match> getMatches(boolean byScore, boolean fairSorting) {
            ArrayList<OutputPort<Match>> operandPorts = new ArrayList<>();
            ApproximateSorter sorter;

            for (MultipleReadsOperator operandPattern : operandPatterns)
                operandPorts.add(operandPattern.match(target, ranges, reverseComplements).getMatches(byScore, fairSorting));

            if (byScore)
                sorter = new SorterByScore(true, true, fairSorting,
                        MatchValidationType.LOGICAL_AND);
            else
                sorter = new SorterByCoordinate(true, true, fairSorting,
                        MatchValidationType.LOGICAL_AND);

            return sorter.getOutputPort(operandPorts);
        }
    }
}
