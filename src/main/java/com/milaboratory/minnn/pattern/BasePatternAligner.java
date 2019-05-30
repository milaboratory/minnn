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

import com.milaboratory.core.Range;
import com.milaboratory.core.alignment.*;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;

public class BasePatternAligner implements PatternAligner {
    private final PatternAndTargetAlignmentScoring scoring;
    private final long penaltyThreshold;
    private final long singleOverlapPenalty;
    private final int bitapMaxErrors;
    private final int maxOverlap;
    private final boolean defaultGroupsOverride;
    private final int leftBorder;

    /**
     * Basic configuration class for scoring and alignment for patterns.
     *
     * @param scoring scoring for pattern and target alignment
     * @param penaltyThreshold 0 or negative; matches with score below this threshold will be considered invalid
     * @param singleOverlapPenalty 0 or negative; this is penalty for 1 nucleotide overlap between 2 patterns
     * @param bitapMaxErrors 0 or positive; maximum allowed number of errors for bitap
     * @param maxOverlap 0 or positive; maximum allowed number of overlapped nucleotides between 2 patterns
     * @param defaultGroupsOverride true if there is default groups override anywhere in the pattern, otherwise false
     */
    public BasePatternAligner(
            PatternAndTargetAlignmentScoring scoring, long penaltyThreshold, long singleOverlapPenalty,
            int bitapMaxErrors, int maxOverlap, boolean defaultGroupsOverride) {
        this(scoring, penaltyThreshold, singleOverlapPenalty, bitapMaxErrors, maxOverlap, defaultGroupsOverride,
                -1);
    }

    private BasePatternAligner(
            PatternAndTargetAlignmentScoring scoring, long penaltyThreshold, long singleOverlapPenalty,
            int bitapMaxErrors, int maxOverlap, boolean defaultGroupsOverride, int leftBorder) {
        this.scoring = scoring;
        this.penaltyThreshold = penaltyThreshold;
        this.singleOverlapPenalty = singleOverlapPenalty;
        this.bitapMaxErrors = bitapMaxErrors;
        this.maxOverlap = maxOverlap;
        this.defaultGroupsOverride = defaultGroupsOverride;
        this.leftBorder = leftBorder;
    }

    @Override
    public Alignment<NucleotideSequenceCaseSensitive> align(NucleotideSequenceCaseSensitive pattern,
            NSequenceWithQuality target, int rightMatchPosition) {
        if (leftBorder == -1) {
            return PatternAndTargetAligner.alignLeftAdded(scoring, pattern, target, rightMatchPosition, bitapMaxErrors);
        } else {
            Range targetRange = new Range(leftBorder, rightMatchPosition + 1);
            NSequenceWithQuality targetPart = new NSequenceWithQuality(target.getSequence().getRange(targetRange),
                    target.getQuality().getRange(targetRange));
            Alignment<NucleotideSequenceCaseSensitive> partAlignment = PatternAndTargetAligner.alignGlobal(scoring,
                    pattern, targetPart);
            return new Alignment<>(pattern, partAlignment.getAbsoluteMutations(),
                    partAlignment.getSequence1Range(), partAlignment.getSequence2Range().move(leftBorder),
                    partAlignment.getScore());
        }
    }

    @Override
    public long penaltyThreshold() {
        return penaltyThreshold;
    }

    @Override
    public long overlapPenalty(NSequenceWithQuality target, int overlapOffset, int overlapLength) {
        return singleOverlapPenalty * overlapLength;
    }

    @Override
    public long insertionPenalty(NSequenceWithQuality target, int insertionOffset, int insertionLength) {
        return singleOverlapPenalty * insertionLength;
    }

    @Override
    public int bitapMaxErrors() {
        return bitapMaxErrors;
    }

    @Override
    public int maxOverlap() {
        return maxOverlap;
    }

    @Override
    public int leftBorder() {
        return leftBorder;
    }

    @Override
    public boolean defaultGroupsOverride() {
        return defaultGroupsOverride;
    }

    @Override
    public PatternAligner overridePenaltyThreshold(long newThresholdValue) {
        return new BasePatternAligner(scoring, newThresholdValue, singleOverlapPenalty, bitapMaxErrors, maxOverlap,
                defaultGroupsOverride, leftBorder);
    }

    @Override
    public PatternAligner setLeftBorder(int newLeftBorder) {
        return new BasePatternAligner(scoring, penaltyThreshold, singleOverlapPenalty, bitapMaxErrors, maxOverlap,
                defaultGroupsOverride, newLeftBorder);
    }
}
