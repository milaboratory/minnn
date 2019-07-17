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
package com.milaboratory.minnn.util;

import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.minnn.pattern.*;

import java.util.*;

import static com.milaboratory.minnn.pattern.MatchValidationType.*;

public final class ApproximateSorterConfiguration {
    final Pattern[] operandPatterns;
    private final int[] operandOrder;
    final MultiNSequenceWithQuality target;
    private final int from;
    private final int to;
    final PatternConfiguration patternConfiguration;
    final boolean multipleReads;
    final boolean separateTargets;
    final boolean combineScoresBySum;
    final boolean fairSorting;
    final MatchValidationType matchValidationType;
    final int unfairSorterLimit;
    final boolean specificOutputPorts;

    /**
     * Configuration for MultipleReadsOperator patterns.
     *
     * @param target                target nucleotide sequence (or multiple sequences)
     * @param patternConfiguration  pattern configuration that provides information about scoring
     *                              and pattern overlap limits
     * @param separateTargets       true only for MultiPattern when each operand pattern gets its own
     *                              part of multi-target
     * @param combineScoresBySum    true if combined score must be equal to sum of match scores;
     *                              false if combined score must be the highest of match scores
     * @param fairSorting           true if we need slow but fair sorting
     * @param matchValidationType   type of validation used to determine that current matches combination is invalid
     * @param unfairSorterLimit     maximum number of output values for this port for unfair sorter
     * @param operandPatterns       operand patterns
     */
    public ApproximateSorterConfiguration(
            MultiNSequenceWithQuality target, PatternConfiguration patternConfiguration, boolean separateTargets,
            boolean combineScoresBySum, boolean fairSorting, MatchValidationType matchValidationType,
            int unfairSorterLimit, Pattern... operandPatterns) {
        this.operandPatterns = operandPatterns;
        this.target = target;
        this.from = -1;
        this.to = -1;
        this.patternConfiguration = patternConfiguration;
        this.multipleReads = true;
        this.separateTargets = separateTargets;
        this.combineScoresBySum = combineScoresBySum;
        this.fairSorting = fairSorting;
        this.matchValidationType = matchValidationType;
        this.unfairSorterLimit = unfairSorterLimit;
        this.specificOutputPorts = false;
        this.operandOrder = null;
        if (((matchValidationType == INTERSECTION) || (matchValidationType == ORDER)
                || (matchValidationType == FOLLOWING) || (matchValidationType == FIRST)))
            throw new IllegalArgumentException("Invalid combination of multipleReads and matchValidationType flags: " +
                    "multipleReads = true, matchValidationType = " + matchValidationType);
        if (operandPatterns.length == 0)
            throw new IllegalArgumentException("Operand patterns array is empty!");
        for (Pattern operandPattern : operandPatterns)
            if (!separateTargets) {
                if (!(operandPattern instanceof MultipleReadsOperator))
                    throw new IllegalArgumentException("Invalid combination of parameters: separateTargets = false, "
                            + "multipleReads = true, operand class: " + operandPattern.getClass());
            } else {
                if (!(operandPattern instanceof SinglePattern))
                    throw new IllegalArgumentException("Invalid combination of parameters: separateTargets = true, "
                            + "multipleReads = true, operand class: " + operandPattern.getClass());
            }
    }

    /**
     * Configuration for SinglePattern patterns.
     *
     * @param target                target nucleotide sequence (or multiple sequences)
     * @param from                  left border in target for range where to search matches, inclusive
     * @param to                    right border in target for range where to search matches, exclusive
     * @param patternConfiguration  pattern configuration that provides information about scoring
     *                              and pattern overlap limits
     * @param combineScoresBySum    true if combined score must be equal to sum of match scores;
     *                              false if combined score must be the highest of match scores
     * @param fairSorting           true if we need slow but fair sorting
     * @param matchValidationType   type of validation used to determine that current matches combination is invalid
     * @param unfairSorterLimit     maximum number of output values for this port for unfair sorter
     * @param operandPatterns       operand patterns
     */
    public ApproximateSorterConfiguration(
            MultiNSequenceWithQuality target, int from, int to, PatternConfiguration patternConfiguration,
            boolean combineScoresBySum, boolean fairSorting, MatchValidationType matchValidationType,
            int unfairSorterLimit, SinglePattern... operandPatterns) {
        this.operandPatterns = operandPatterns;
        this.target = target;
        this.from = from;
        this.to = to;
        this.patternConfiguration = patternConfiguration;
        this.multipleReads = false;
        this.separateTargets = false;
        this.combineScoresBySum = combineScoresBySum;
        this.fairSorting = fairSorting;
        this.matchValidationType = matchValidationType;
        this.unfairSorterLimit = unfairSorterLimit;
        this.specificOutputPorts = !fairSorting
                && ((matchValidationType == ORDER) || (matchValidationType == FOLLOWING));
        if (this.specificOutputPorts) {
            ArrayList<Integer> operandOrderList = new ArrayList<>();
            int numberOfOperands = operandPatterns.length;
            Long[] patternComplexities = Arrays.stream(operandPatterns).map(Pattern::estimateComplexity)
                    .toArray(Long[]::new);
            long minComplexity = Long.MAX_VALUE;
            int firstPatternIndex = 0;
            for (int i = 0; i < numberOfOperands; i++)
                if (minComplexity > patternComplexities[i]) {
                    minComplexity = patternComplexities[i];
                    firstPatternIndex = i;
                }
            operandOrderList.add(firstPatternIndex);
            while (operandOrderList.size() < numberOfOperands) {
                int leftIndex = Collections.min(operandOrderList) - 1;
                int rightIndex = Collections.max(operandOrderList) + 1;
                long leftComplexity = leftIndex < 0 ? Long.MAX_VALUE
                        : patternComplexities[leftIndex];
                long rightComplexity = rightIndex >= numberOfOperands ? Long.MAX_VALUE
                        : patternComplexities[rightIndex];
                operandOrderList.add(leftComplexity > rightComplexity ? rightIndex : leftIndex);
            }
            this.operandOrder = operandOrderList.stream().mapToInt(Integer::intValue).toArray();
        } else
            this.operandOrder = null;
        if ((from < 0) || (to < 0))
            throw new IllegalArgumentException("Invalid from and to arguments: from = " + from + ", to = " + to);
        if ((matchValidationType == LOGICAL_AND) || (matchValidationType == LOGICAL_OR))
            throw new IllegalArgumentException("Invalid combination of multipleReads and matchValidationType flags: " +
                    "multipleReads = false, matchValidationType = " + matchValidationType);
        if (operandPatterns.length == 0)
            throw new IllegalArgumentException("Operand patterns array is empty!");
        for (Pattern operandPattern : operandPatterns)
            if (!(operandPattern instanceof SinglePattern))
                throw new IllegalArgumentException("Invalid combination of multipleReads and operand pattern class: "
                        + "multipleReads = false, operand class: " + operandPattern.getClass());
        if (target.numberOfSequences() != 1)
            throw new IllegalArgumentException("Invalid combination of multipleReads and target number of sequences: "
                    + "multipleReads = false, target number of sequences: " + target.numberOfSequences());
    }

    int from() {
        if (multipleReads)
            throw new IllegalStateException("Trying to get \"from\" when multipleReads is true!");
        return from;
    }

    int to() {
        if (multipleReads)
            throw new IllegalStateException("Trying to get \"to\" when multipleReads is true!");
        return to;
    }

    int[] operandOrder() {
        if (specificOutputPorts)
            return operandOrder;
        else
            throw new IllegalStateException("Trying to get \"operandOrder\" when specificOutputPorts is false!");
    }
}
