/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
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

import java.util.*;

import static com.milaboratory.minnn.pattern.MatchValidationType.INTERSECTION;
import static com.milaboratory.minnn.util.UnfairSorterConfiguration.unfairSorterPortLimits;

public final class AndPattern extends MultiplePatternsOperator {
    public AndPattern(PatternConfiguration conf, SinglePattern... operandPatterns) {
        super(conf, operandPatterns);
    }

    private AndPattern(
            PatternConfiguration conf, byte targetId, SinglePattern[] operandPatterns,
            ArrayList<GroupEdge> groupEdges) {
        super(conf, targetId, operandPatterns, groupEdges);
    }

    @Override
    public String toString() {
        return "AndPattern(" + Arrays.toString(operandPatterns) + ")";
    }

    @Override
    public MatchingResult match(NSequenceWithQuality target, int from, int to) {
        return new AndPatternMatchingResult(target, from, to);
    }

    @Override
    public int estimateMinLength() {
        int summaryLength = Arrays.stream(operandPatterns).mapToInt(SinglePattern::estimateMinLength).sum();
        return Math.max(1, summaryLength - conf.maxOverlap * Math.max(0, operandPatterns.length - 1));
    }

    @Override
    public long estimateComplexity() {
        return Arrays.stream(operandPatterns).mapToLong(Pattern::estimateComplexity).sum();
    }

    @Override
    SinglePattern setTargetId(byte targetId) {
        validateTargetId(targetId);
        SinglePattern[] newOperandPatterns = setTargetIdForOperands();
        return new AndPattern(conf, targetId, newOperandPatterns, groupEdges);
    }

    private class AndPatternMatchingResult implements MatchingResult {
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;

        AndPatternMatchingResult(NSequenceWithQuality target, int from, int to) {
            this.target = target;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            ApproximateSorterConfiguration approximateSorterConfiguration = new ApproximateSorterConfiguration(target,
                    from, to, conf, true, fairSorting, INTERSECTION,
                    unfairSorterPortLimits.get(AndPattern.class), operandPatterns);
            return new ApproximateSorter(approximateSorterConfiguration).getOutputPort();
        }
    }
}
