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
package com.milaboratory.minnn.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import static com.milaboratory.minnn.pattern.PatternUtils.invertCoordinate;

public final class StickFilter implements Filter {
    private final boolean left;
    private int position;

    public StickFilter(boolean left, int position) {
        this.left = left;
        this.position = position;
    }

    void fixPosition(NSequenceWithQuality target) {
        if (position < -1)
            position = target.size() - 1 - invertCoordinate(position);
    }

    @Override
    public String toString() {
        return "StickFilter(" + left + ", " + position + ")";
    }

    @Override
    public MatchIntermediate checkMatch(MatchIntermediate match) {
        if (position < 0)
            throw new IllegalStateException("Position (" + position + ") is negative on checkMatch() stage!");
        MatchedRange[] matchedRanges = match.getMatchedRanges();
        if (matchedRanges.length != 1)
            throw new IllegalArgumentException("Expected exactly 1 matched range in StickFilter; got "
                    + matchedRanges.length);
        else {
            Range range = match.getRange();
            // position is always inclusive; range.getFrom() is inclusive, range.getTo() is exclusive
            if ((left && (range.getFrom() != position)) || (!left && (range.getTo() - 1 != position)))
                return null;
            else
                return match;
        }
    }
}
