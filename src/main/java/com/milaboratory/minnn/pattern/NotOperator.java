/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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

import java.util.ArrayList;

public final class NotOperator extends MultipleReadsOperator {
    public NotOperator(PatternAligner patternAligner, MultipleReadsOperator... operandPatterns) {
        super(patternAligner, operandPatterns);
        if (operandPatterns.length != 1)
            throw new IllegalArgumentException("Not operator must take exactly 1 operand!");
    }

    @Override
    public String toString() {
        return "NotOperator(" + operandPatterns[0] + ")";
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        return operandPatterns[0].getGroupEdges();
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target) {
        return new NotOperatorMatchingResult(operandPatterns[0], target);
    }

    @Override
    public long estimateComplexity() {
        return operandPatterns[0].estimateComplexity();
    }

    private class NotOperatorMatchingResult implements MatchingResult {
        private final MultipleReadsOperator operandPattern;
        private final MultiNSequenceWithQuality target;

        NotOperatorMatchingResult(MultipleReadsOperator operandPattern, MultiNSequenceWithQuality target) {
            this.operandPattern = operandPattern;
            this.target = target;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            return new NotOperatorOutputPort(operandPattern.match(target).getMatches(fairSorting));
        }

        private class NotOperatorOutputPort implements OutputPort<MatchIntermediate> {
            private final OutputPort<MatchIntermediate> operandPort;
            private boolean firstCall = true;
            private boolean operandIsMatching;

            NotOperatorOutputPort(OutputPort<MatchIntermediate> operandPort) {
                this.operandPort = operandPort;
            }

            @Override
            public MatchIntermediate take() {
                if (!firstCall) return null;
                operandIsMatching = (operandPort.take() != null);
                firstCall = false;
                if (operandIsMatching)
                    return null;
                else {
                    return new MatchIntermediate(1, patternAligner.notResultScore(),
                            -1, -1,
                            new ArrayList<>(), new NullMatchedRange(0));
                }
            }
        }
    }
}