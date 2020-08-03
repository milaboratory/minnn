/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
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
package com.milaboratory.minnn.parser;

import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;
import com.milaboratory.minnn.pattern.BasePatternAligner;
import com.milaboratory.minnn.pattern.PatternConfiguration;

import java.util.Objects;

public class ParserConfiguration {
    private final PatternAndTargetAlignmentScoring scoring;
    private final long scoreThreshold;
    private final long singleOverlapPenalty;
    private final int bitapMaxErrors;
    private final int maxOverlap;
    private final long notResultScore;
    private PatternConfiguration patternConfiguration;
    private Boolean defaultGroupsOverride;

    public ParserConfiguration(PatternAndTargetAlignmentScoring scoring, long scoreThreshold,
                               long singleOverlapPenalty, int bitapMaxErrors, int maxOverlap, long notResultScore) {
        this.scoring = scoring;
        this.scoreThreshold = scoreThreshold;
        this.singleOverlapPenalty = singleOverlapPenalty;
        this.bitapMaxErrors = bitapMaxErrors;
        this.maxOverlap = maxOverlap;
        this.notResultScore = notResultScore;
        this.patternConfiguration = null;
        this.defaultGroupsOverride = null;
    }

    ParserConfiguration(ParserConfiguration originalConf) {
        this.scoring = originalConf.scoring;
        this.scoreThreshold = originalConf.scoreThreshold;
        this.singleOverlapPenalty = originalConf.singleOverlapPenalty;
        this.bitapMaxErrors = originalConf.bitapMaxErrors;
        this.maxOverlap = originalConf.maxOverlap;
        this.notResultScore = originalConf.notResultScore;
        this.patternConfiguration = null;
        this.defaultGroupsOverride = null;
    }

    void init(boolean defaultGroupsOverride) {
        this.patternConfiguration = new PatternConfiguration(defaultGroupsOverride, new BasePatternAligner(), scoring,
                scoreThreshold, singleOverlapPenalty, bitapMaxErrors, maxOverlap, -1, notResultScore);
        this.defaultGroupsOverride = defaultGroupsOverride;
    }

    PatternConfiguration getPatternConfiguration() {
        return Objects.requireNonNull(patternConfiguration);
    }

    boolean isDefaultGroupsOverride() {
        return Objects.requireNonNull(defaultGroupsOverride);
    }
}
