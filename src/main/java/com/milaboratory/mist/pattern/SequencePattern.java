package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.util.*;

import java.util.Arrays;
import java.util.stream.IntStream;

import static com.milaboratory.mist.pattern.MatchValidationType.FOLLOWING;
import static com.milaboratory.mist.util.UnfairSorterConfiguration.*;

public final class SequencePattern extends MultiplePatternsOperator implements CanBeSingleSequence, CanFixBorders {
    public SequencePattern(PatternAligner patternAligner, SinglePattern... operandPatterns) {
        super(patternAligner, operandPatterns);
    }

    @Override
    public String toString() {
        return "SequencePattern(" + Arrays.toString(operandPatterns) + ")";
    }

    @Override
    public MatchingResult match(NSequenceWithQuality target, int from, int to) {
        return new SequencePatternMatchingResult(target, from, to);
    }

    @Override
    public int estimateMaxLength() {
        int maxGap = Math.max(patternAligner.maxOverlap(), patternAligner.bitapMaxErrors());
        int summaryLength = maxGap * (operandPatterns.length - 1);
        for (SinglePattern currentPattern : operandPatterns) {
            int currentPatternMaxLength = currentPattern.estimateMaxLength();
            if (currentPatternMaxLength == -1)
                return -1;
            else
                summaryLength += currentPatternMaxLength;
        }
        return summaryLength;
    }

    @Override
    public long estimateComplexity() {
        if (isSingleSequence())
            return Arrays.stream(operandPatterns).mapToLong(Pattern::estimateComplexity).min()
                    .orElseThrow(IllegalStateException::new)
                    + fixedSequenceMaxComplexity * (operandPatterns.length - 1);
        else
            return Arrays.stream(operandPatterns).mapToLong(Pattern::estimateComplexity).sum();
    }

    @Override
    public boolean isSingleSequence() {
        return Arrays.stream(operandPatterns)
                .allMatch(p -> p instanceof CanBeSingleSequence && ((CanBeSingleSequence)p).isSingleSequence());
    }

    @Override
    public SinglePattern fixBorder(boolean left, int position) {
        int targetOperandIndex = left ? 0 : operandPatterns.length - 1;
        if (operandPatterns[targetOperandIndex] instanceof CanFixBorders) {
            SinglePattern newOperand = ((CanFixBorders)(operandPatterns[targetOperandIndex])).fixBorder(left, position);
            return new SequencePattern(patternAligner, IntStream.range(0, operandPatterns.length)
                    .mapToObj((int i) -> (i == targetOperandIndex ? newOperand : operandPatterns[i]))
                    .toArray(SinglePattern[]::new));
        } else
            return this;
    }

    private class SequencePatternMatchingResult implements MatchingResult {
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;

        SequencePatternMatchingResult(NSequenceWithQuality target, int from, int to) {
            this.target = target;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<Match> getMatches(boolean fairSorting) {
            ApproximateSorterConfiguration conf = new ApproximateSorterConfiguration(target, from, to, patternAligner,
                    true, fairSorting, FOLLOWING, unfairSorterPortLimits.get(SequencePattern.class),
                    operandPatterns);
            return new ApproximateSorter(conf).getOutputPort();
        }
    }
}
