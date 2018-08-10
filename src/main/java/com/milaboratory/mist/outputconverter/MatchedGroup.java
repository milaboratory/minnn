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
package com.milaboratory.mist.outputconverter;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.pattern.MatchedRange;

public final class MatchedGroup extends MatchedRange {
    private final String groupName;
    private final NSequenceWithQuality valueOverride;

    public MatchedGroup(String groupName, NSequenceWithQuality target, byte targetId, Range range) {
        super(target, targetId, 0, range);
        this.groupName = groupName;
        this.valueOverride = null;
    }

    public MatchedGroup(String groupName, NSequenceWithQuality target, byte targetId,
                        NSequenceWithQuality valueOverride) {
        super(target, targetId, 0, null);
        this.groupName = groupName;
        this.valueOverride = valueOverride;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public int getPatternIndex() {
        throw new IllegalStateException("getPatternIndex() called for MatchedGroup!");
    }

    @Override
    public NSequenceWithQuality getValue() {
        if (getRange() != null)
            return super.getValue();
        else
            return valueOverride;
    }
}
