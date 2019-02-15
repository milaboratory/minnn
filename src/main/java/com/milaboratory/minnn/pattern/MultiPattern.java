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
import com.milaboratory.minnn.util.*;

import java.util.Arrays;

import static com.milaboratory.minnn.pattern.MatchValidationType.LOGICAL_AND;
import static com.milaboratory.minnn.util.UnfairSorterConfiguration.unfairSorterPortLimits;

public final class MultiPattern extends MultipleReadsOperator {
    public MultiPattern(PatternAligner patternAligner, SinglePattern... operandPatterns) {
        super(patternAligner, operandPatterns);
        for (byte i = 0; i < operandPatterns.length; i++) {
            if (!(operandPatterns[i] instanceof FullReadPattern))
                throw new IllegalArgumentException("All MultiPattern arguments must be FullReadPattern, got "
                        + operandPatterns[i]);
            operandPatterns[i].setTargetId((byte)(i + 1));
        }
    }

    @Override
    public String toString() {
        return "MultiPattern(" + Arrays.toString(singlePatterns) + ")";
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target) {
        if (target.numberOfSequences() != singlePatterns.length)
            throw new IllegalArgumentException("Mismatched number of reads (" + target.numberOfSequences()
                    + ") and patterns (" + singlePatterns.length + ")!");

        return new MultiPatternMatchingResult(target);
    }

    @Override
    public long estimateComplexity() {
        return Arrays.stream(singlePatterns).mapToLong(Pattern::estimateComplexity).sum();
    }

    private class MultiPatternMatchingResult implements MatchingResult {
        private final MultiNSequenceWithQuality target;

        MultiPatternMatchingResult(MultiNSequenceWithQuality target) {
            this.target = target;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            ApproximateSorterConfiguration conf = new ApproximateSorterConfiguration(target, patternAligner,
                    true, true, fairSorting, LOGICAL_AND,
                    unfairSorterPortLimits.get(MultiPattern.class), singlePatterns);
            return new ApproximateSorter(conf).getOutputPort();
        }
    }
}
