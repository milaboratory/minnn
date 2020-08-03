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
package com.milaboratory.minnn.outputconverter;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;

final class FastqCommentGroup implements Comparable<FastqCommentGroup> {
    private final String name;
    private final boolean matched;
    private final boolean insideMain;
    private final String sequence;
    private final String quality;
    private final String from;
    private final String to;

    FastqCommentGroup(String name) {
        this(name, false, false, null, null);
    }

    FastqCommentGroup(String name, NSequenceWithQuality value) {
        this(name, true, false, value, null);
    }

    FastqCommentGroup(String name, boolean matched, boolean insideMain, NSequenceWithQuality value, Range range) {
        this.name = name;
        this.matched = matched;
        this.insideMain = insideMain;
        if (matched) {
            this.sequence = value.getSequence().toString();
            this.quality = value.getQuality().toString();
            if (insideMain) {
                this.from = Integer.toString(range.getLower());
                this.to = Integer.toString(range.getUpper());
            } else {
                this.from = null;
                this.to = null;
            }
        } else {
            this.sequence = null;
            this.quality = null;
            this.from = null;
            this.to = null;
        }
    }

    @Override
    public int compareTo(FastqCommentGroup otherGroup) {
        return name.compareTo(otherGroup.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FastqCommentGroup that = (FastqCommentGroup)o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    StringBuilder getDescription() {
        StringBuilder description = new StringBuilder();
        description.append(name);
        if (matched) {
            description.append('~');
            description.append(sequence);
            description.append('~');
            description.append(quality);
            if (insideMain) {
                description.append('{');
                description.append(from);
                description.append('~');
                description.append(to);
                description.append('}');
            }
        }
        description.append('|');
        return description;
    }
}
