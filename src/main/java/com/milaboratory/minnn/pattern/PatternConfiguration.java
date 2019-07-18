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

import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;

public class PatternConfiguration {
    public final boolean defaultGroupsOverride;
    public final PatternAligner patternAligner;
    public final PatternAndTargetAlignmentScoring scoring;
    public final long scoreThreshold;
    final long singleOverlapPenalty;
    final int bitapMaxErrors;
    public final int maxOverlap;
    final int leftBorder;
    final long notResultScore;

    /**
     * @param defaultGroupsOverride     true if there is default groups override in any pattern in the query
     * @param patternAligner            pattern aligner
     * @param scoring                   scoring for pattern and target alignment
     * @param scoreThreshold            matches with score below this threshold will be considered invalid
     * @param singleOverlapPenalty      0 or negative; this is penalty for 1 nucleotide overlap between 2 patterns
     * @param bitapMaxErrors            0 or positive; max errors to use in bitap matcher
     * @param maxOverlap                0 or positive; maximal allowed overlap for two intersecting sub-patterns
     * @param leftBorder                fixed left border position for alignment, -1 if not specified;
     *                                  if left border is specified, global aligner is used
     * @param notResultScore            score for result of Not operator
     */
    public PatternConfiguration(
            boolean defaultGroupsOverride, PatternAligner patternAligner, PatternAndTargetAlignmentScoring scoring,
            long scoreThreshold, long singleOverlapPenalty, int bitapMaxErrors, int maxOverlap, int leftBorder,
            long notResultScore) {
        this.defaultGroupsOverride = defaultGroupsOverride;
        this.patternAligner = patternAligner;
        this.scoring = scoring;
        this.scoreThreshold = scoreThreshold;
        this.singleOverlapPenalty = singleOverlapPenalty;
        this.bitapMaxErrors = bitapMaxErrors;
        this.maxOverlap = maxOverlap;
        this.leftBorder = leftBorder;
        this.notResultScore = notResultScore;
    }

    public PatternConfiguration overrideScoreThreshold(long newThresholdValue) {
        return new PatternConfiguration(defaultGroupsOverride, patternAligner, scoring, newThresholdValue,
                singleOverlapPenalty, bitapMaxErrors, maxOverlap, leftBorder, notResultScore);
    }

    public PatternConfiguration setLeftBorder(int newLeftBorder) {
        return new PatternConfiguration(defaultGroupsOverride, patternAligner, scoring, scoreThreshold,
                singleOverlapPenalty, bitapMaxErrors, maxOverlap, newLeftBorder, notResultScore);
    }
}
