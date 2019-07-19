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
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.util.ArrayList;

/**
 * Filter pattern can be used for both single and multiple patterns; it overrides match() methods for both single
 * and multiple patterns. It filters matches from pattern with specified Filter. For usage with MultipleReadsOperator
 * patterns, it must be wrapped with MultipleReadsFilterPattern.
 */
public final class FilterPattern extends SinglePattern implements CanBeSingleSequence, CanFixBorders {
    private final Filter filter;
    private final Pattern pattern;

    public FilterPattern(PatternConfiguration conf, Filter filter, Pattern pattern) {
        super(conf);
        this.filter = filter;
        this.pattern = pattern;
    }

    private FilterPattern(PatternConfiguration conf, byte targetId, Filter filter, Pattern pattern) {
        super(conf, targetId);
        this.filter = filter;
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return "FilterPattern(" + filter + ", " + pattern + ")";
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        return pattern.getGroupEdges();
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target) {
        return new FilterMatchingResult(filter, target);
    }

    @Override
    public MatchingResult match(NSequenceWithQuality target, int from, int to) {
        return new FilterMatchingResult(filter, target, from, to);
    }

    @Override
    public int estimateMaxLength() {
        if (pattern instanceof SinglePattern)
            return ((SinglePattern)pattern).estimateMaxLength();
        else
            throw new IllegalStateException("estimateMaxLength() called for argument of class " + pattern.getClass());
    }

    @Override
    public long estimateComplexity() {
        return pattern.estimateComplexity();
    }

    @Override
    SinglePattern setTargetId(byte targetId) {
        validateTargetId(targetId);
        if (pattern instanceof SinglePattern) {
            SinglePattern newOperandPattern = ((SinglePattern)pattern).setTargetId(targetId);
            return new FilterPattern(conf, targetId, filter, newOperandPattern);
        } else
            throw new IllegalStateException("setTargetId() called for argument of class " + pattern.getClass());
    }

    @Override
    public boolean isSingleSequence() {
        return pattern instanceof CanBeSingleSequence && ((CanBeSingleSequence)pattern).isSingleSequence();
    }

    @Override
    public SinglePattern fixBorder(boolean left, int position) {
        if (pattern instanceof CanFixBorders) {
            return new FilterPattern(conf, filter, ((CanFixBorders)pattern).fixBorder(left, position));
        } else
            return this;
    }

    private class FilterMatchingResult implements MatchingResult {
        private final Filter filter;
        private final MultiNSequenceWithQuality targetMulti;
        private final NSequenceWithQuality targetSingle;
        private final int from;
        private final int to;

        FilterMatchingResult(Filter filter, MultiNSequenceWithQuality targetMulti) {
            this(filter,
                    (targetMulti instanceof NSequenceWithQuality) ? null : targetMulti,
                    (targetMulti instanceof NSequenceWithQuality) ? (NSequenceWithQuality)targetMulti : null,
                    0,
                    (targetMulti instanceof NSequenceWithQuality) ? ((NSequenceWithQuality)targetMulti).size() : 0);
        }

        FilterMatchingResult(Filter filter, NSequenceWithQuality targetSingle, int from, int to) {
            this(filter, null, targetSingle, from, to);
        }

        private FilterMatchingResult(Filter filter, MultiNSequenceWithQuality targetMulti,
                                     NSequenceWithQuality targetSingle, int from, int to) {
            this.filter = filter;
            if (filter instanceof StickFilter)
                ((StickFilter)filter).fixPosition(targetSingle);
            this.targetMulti = targetMulti;
            this.targetSingle = targetSingle;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            if (targetMulti != null) {
                if (!(pattern instanceof MultipleReadsOperator)) throw new IllegalArgumentException(
                        "Trying to use filter with single-target pattern and multi-target match arguments.");
                return new FilterOutputPort(pattern.match(targetMulti).getMatches(fairSorting));
            } else if (targetSingle != null) {
                if (!(pattern instanceof SinglePattern)) throw new IllegalArgumentException(
                        "Trying to use filter with multi-target pattern and single-target match arguments.");
                return new FilterOutputPort(((SinglePattern)pattern).match(targetSingle, from, to)
                        .getMatches(fairSorting));
            } else throw new IllegalStateException("Both targetMulti and targetSingle are null.");
        }

        private class FilterOutputPort implements OutputPort<MatchIntermediate> {
            private final OutputPort<MatchIntermediate> operandPort;

            FilterOutputPort(OutputPort<MatchIntermediate> operandPort) {
                this.operandPort = operandPort;
            }

            @Override
            public MatchIntermediate take() {
                MatchIntermediate currentMatch, currentFilteredMatch;
                do {
                    currentMatch = operandPort.take();
                    if (currentMatch == null)
                        return null;
                    currentFilteredMatch = filter.checkMatch(currentMatch);
                } while (currentFilteredMatch == null);

                return currentFilteredMatch;
            }
        }
    }
}
