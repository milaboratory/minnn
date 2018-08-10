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
package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.core.sequence.NSequenceWithQuality;

public abstract class SinglePattern extends Pattern {
    /**
     * Number of read where sequence is matched; numbers start from 1.
     * IDs from here go to MatchedRange/MatchedGroupEdge objects used in MatchIntermediate objects.
     * 0 value used only in NullMatchedRange that is used in matches for NotOperator and OrOperator.
     */
    protected byte targetId = 1;

    SinglePattern(PatternAligner patternAligner) {
        super(patternAligner);
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target) {
        if (target instanceof NSequenceWithQuality)
            return match((NSequenceWithQuality)target);
        else if (target.numberOfSequences() == 1)
            return match(target.get(0));
        else
            throw new IllegalArgumentException("Supports only single NSequenceWithQuality.");
    }

    /**
     * Search this pattern in target sequence
     *
     * @param target target sequence
     * @param range searching range in target sequence, non-reversed only
     * @return matching result
     */
    public MatchingResult match(NSequenceWithQuality target, Range range) {
        if (range.isReverse())
            throw new IllegalArgumentException("Doesn't support reversed ranges.");
        return match(target, range.getFrom(), range.getTo());
    }

    public MatchingResult match(NSequenceWithQuality target) {
        return match(target, 0, target.size());
    }

    /**
     * Search this pattern in target sequence.
     *
     * @param target target sequence
     * @param from starting point in target sequence (inclusive)
     * @param to ending point in target sequence (exclusive)
     * @return matching result
     */
    public abstract MatchingResult match(NSequenceWithQuality target, int from, int to);

    /**
     * Return estimated maximum length for this pattern, or if it is unavailable for this pattern, return -1.
     *
     * @return estimated maximum length for this pattern, or -1 if it is unavailable
     */
    public int estimateMaxLength() {
        return -1;
    }

    /**
     * Return estimated max overlap for this pattern, or if it is unavailable for this pattern, return -1.
     *
     * @return estimated max overlap for this pattern, or -1 if it is unavailable
     */
    public int estimateMaxOverlap() {
        return -1;
    }

    void setTargetId(byte targetId) {
        if (targetId < 1)
            throw new IllegalArgumentException("targetId must be positive; found " + targetId);
        this.targetId = targetId;
    }
}
