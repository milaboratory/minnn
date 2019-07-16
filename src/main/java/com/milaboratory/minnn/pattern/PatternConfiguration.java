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
    final PatternAligner patternAligner;
    final PatternAndTargetAlignmentScoring scoring;
    final long scoreThreshold;
    final long singleOverlapPenalty;
    final int bitapMaxErrors;
    final int maxOverlap;
    final int leftBorder;
    final long notResultScore;

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
