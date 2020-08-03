/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact MiLaboratory LLC, which owns exclusive
 * rights for distribution of this program for commercial purposes, using the
 * following email address: licensing@milaboratory.com.
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.minnn.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.minnn.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.pattern.MatchValidationType.FOLLOWING;
import static com.milaboratory.minnn.util.UnfairSorterConfiguration.*;

public final class SequencePattern extends MultiplePatternsOperator implements CanBeSingleSequence, CanFixBorders {
    public SequencePattern(PatternConfiguration conf, SinglePattern... operandPatterns) {
        super(conf, operandPatterns);
    }

    private SequencePattern(
            PatternConfiguration conf, byte targetId, SinglePattern[] operandPatterns,
            ArrayList<GroupEdge> groupEdges) {
        super(conf, targetId, operandPatterns, groupEdges);
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
    public int estimateMinLength() {
        int summaryLength = 0;
        for (int i = 0; i < operandPatterns.length; i++) {
            if (i == 0)
                summaryLength += operandPatterns[0].estimateMinLength();
            else
                summaryLength += Math.max(1, operandPatterns[i].estimateMinLength() - conf.maxOverlap);
        }
        return summaryLength;
    }

    @Override
    public int estimateMaxLength() {
        int maxGap = Math.max(conf.maxOverlap, conf.bitapMaxErrors);
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
            SinglePattern newOperand = ((CanFixBorders)(operandPatterns[targetOperandIndex]))
                    .fixBorder(left, position);
            return new SequencePattern(conf, IntStream.range(0, operandPatterns.length)
                    .mapToObj((int i) -> (i == targetOperandIndex ? newOperand : operandPatterns[i]))
                    .toArray(SinglePattern[]::new));
        } else
            return this;
    }

    @Override
    SinglePattern setTargetId(byte targetId) {
        validateTargetId(targetId);
        SinglePattern[] newOperandPatterns = setTargetIdForOperands();
        return new SequencePattern(conf, targetId, newOperandPatterns, groupEdges);
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
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            ApproximateSorterConfiguration approximateSorterConfiguration = new ApproximateSorterConfiguration(target,
                    from, to, conf, true, fairSorting, FOLLOWING,
                    unfairSorterPortLimits.get(SequencePattern.class), operandPatterns);
            return new ApproximateSorter(approximateSorterConfiguration).getOutputPort();
        }
    }
}
