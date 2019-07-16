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

import java.util.ArrayList;

public final class FullReadPattern extends SinglePattern {
    private final SinglePattern operandPattern;
    private boolean targetIdInitialized = false;

    public FullReadPattern(PatternConfiguration conf, SinglePattern operandPattern) {
        super(conf);
        this.operandPattern = operandPattern;
    }

    @Override
    public String toString() {
        return "FullReadPattern(" + operandPattern + ")";
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        if (!targetIdInitialized)
            throw new IllegalStateException(
                    "getGroupEdges() called for FullReadPattern when targetId is not initialized!");
        if (conf.defaultGroupsOverride)
            return operandPattern.getGroupEdges();
        else {
            String mainGroupName = "R" + targetId;
            ArrayList<GroupEdge> groupEdges = new ArrayList<>(operandPattern.getGroupEdges());
            groupEdges.add(new GroupEdge(mainGroupName, true));
            groupEdges.add(new GroupEdge(mainGroupName, false));
            return groupEdges;
        }
    }

    public ArrayList<GroupEdge> getOperandGroupEdges() {
        return operandPattern.getGroupEdges();
    }

    @Override
    public MatchingResult match(NSequenceWithQuality target, int from, int to) {
        return new FullReadPatternMatchingResult(target, from, to);
    }

    @Override
    public int estimateMaxLength() {
        throw new IllegalStateException("estimateMaxLength() called for FullReadPattern!");
    }

    @Override
    public long estimateComplexity() {
        return operandPattern.estimateComplexity();
    }

    @Override
    public void setTargetId(byte targetId) {
        super.setTargetId(targetId);
        operandPattern.setTargetId(targetId);
        targetIdInitialized = true;
    }

    public SinglePattern getOperand() {
        return operandPattern;
    }

    private class FullReadPatternMatchingResult implements MatchingResult {
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;

        public FullReadPatternMatchingResult(NSequenceWithQuality target, int from, int to) {
            this.target = target;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            OutputPort<MatchIntermediate> operandPort = operandPattern.match(target, from, to).getMatches(fairSorting);
            return conf.defaultGroupsOverride ? operandPort : () -> {
                MatchIntermediate match = operandPort.take();
                if (match == null) return null;
                String mainGroupName = "R" + targetId;
                int patternIndex = match.getMatchedRange().getPatternIndex();
                ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>(match.getMatchedGroupEdges());
                matchedGroupEdges.add(new MatchedGroupEdge(target, targetId, patternIndex,
                        new GroupEdge(mainGroupName, true), 0));
                matchedGroupEdges.add(new MatchedGroupEdge(null, targetId, patternIndex,
                        new GroupEdge(mainGroupName, false), target.size()));
                return new MatchIntermediate(1, match.getScore(),
                        -1, -1, matchedGroupEdges, match.getMatchedRanges());
            };
        }
    }
}
