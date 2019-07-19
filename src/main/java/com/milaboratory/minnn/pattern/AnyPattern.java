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
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.pattern.PatternUtils.defaultGroupIds;

public final class AnyPattern extends SinglePattern {
    private final ArrayList<GroupEdge> groupEdges;

    public AnyPattern(PatternConfiguration conf, ArrayList<GroupEdge> groupEdges) {
        super(conf);
        this.groupEdges = groupEdges;
    }

    private AnyPattern(PatternConfiguration conf, byte targetId, ArrayList<GroupEdge> groupEdges) {
        super(conf, targetId);
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
        return new AnyPatternMatchingResult(target, from, to);
    }

    @Override
    public int estimateMaxLength() {
        throw new IllegalStateException("estimateMaxLength() called for AnyPattern!");
    }

    @Override
    public long estimateComplexity() {
        return 1;
    }

    @Override
    SinglePattern setTargetId(byte targetId) {
        validateTargetId(targetId);
        return new AnyPattern(conf, targetId, groupEdges);
    }

    private class AnyPatternMatchingResult implements MatchingResult {
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;

        AnyPatternMatchingResult(NSequenceWithQuality target, int from, int to) {
            this.target = target;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            return new AnyPatternOutputPort();
        }

        private class AnyPatternOutputPort implements OutputPort<MatchIntermediate> {
            private boolean firstTake = true;

            @Override
            public MatchIntermediate take() {
                if (!firstTake) return null;
                firstTake = false;

                MatchedRange matchedRange = new MatchedRange(target, targetId, 0, new Range(from, to));
                ArrayList<MatchedGroupEdge> matchedGroupEdges = groupEdges.stream().map(ge -> {
                    byte matchedGroupTargetId = conf.defaultGroupsOverride
                            ? defaultGroupIds.getOrDefault(ge.getGroupName(), (byte)-1) : targetId;
                    return new MatchedGroupEdge(target, matchedGroupTargetId, 0, ge,
                            ge.isStart() ? 0 : target.size());
                }).collect(Collectors.toCollection(ArrayList::new));
                return new MatchIntermediate(1, 0, -1, -1,
                        matchedGroupEdges, matchedRange);
            }
        }
    }
}
