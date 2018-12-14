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
package com.milaboratory.minnn.consensus;

import com.milaboratory.core.sequence.*;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.consensus.SpecialSequences.*;
import static com.milaboratory.minnn.util.SequencesCache.*;

public final class SequenceWithAttributes {
    private final NucleotideSequence seq;
    private final SequenceQuality qual;
    private final long originalReadId;

    public SequenceWithAttributes(NucleotideSequence seq, SequenceQuality qual, long originalReadId) {
        this.seq = seq;
        this.qual = qual;
        this.originalReadId = originalReadId;
    }

    public SequenceWithAttributes(SpecialSequences type, long originalReadId) {
        if (type == NULL_SEQ) {
            this.seq = null;
            this.qual = null;
        } else if (type == EMPTY_SEQ) {
            this.seq = NucleotideSequence.EMPTY;
            this.qual = SequenceQuality.EMPTY;
        } else throw new IllegalArgumentException("Unknown special sequence type: " + type);
        this.originalReadId = originalReadId;
    }

    public NucleotideSequence getSeq() {
        return seq;
    }

    public SequenceQuality getQual() {
        return qual;
    }

    public long getOriginalReadId() {
        return originalReadId;
    }

    private SequenceWithAttributes resultWithCachedValues(int from, int to) {
        NucleotideSequence seqPart = (seq == null) ? null : seq.getRange(from, to);
        SequenceQuality qualPart = (qual == null) ? null : qual.getRange(from, to);
        NucleotideSequence cachedSeq = sequencesCache.get(seqPart);
        if (cachedSeq == null)
            cachedSeq = seqPart;
        SequenceQuality cachedQual = ((qualPart != null) && (qualPart.size() == 1))
                ? qualityCache.get(qualPart.value(0)) : qualPart;
        return new SequenceWithAttributes(cachedSeq, cachedQual, originalReadId);
    }

    SequenceWithAttributes letterAt(int position) {
        if ((seq == null) || (position < 0) || (position >= seq.size()))
            return new SequenceWithAttributes(NULL_SEQ, originalReadId);
        return resultWithCachedValues(position, position + 1);
    }

    SequenceWithAttributes getSubSequence(int from, int to) {
        if (seq == null)
            throw new IllegalStateException("getSubSequence() called for null sequence! Read id: "
                    + originalReadId);
        if ((from < 0) || (to > seq.size()) || (to - from < 1))
            throw new IndexOutOfBoundsException("seq.size(): " + seq.size() + ", from: " + from + ", to: " + to);
        return resultWithCachedValues(from, to);
    }

    NSequenceWithQuality toNSequenceWithQuality() {
        return new NSequenceWithQuality(seq, qual);
    }

    long calculateSumQuality() {
        if ((qual == null) || isEmpty())
            return 0;
        long sum = 0;
        for (byte quality : qual.asArray())
            sum += quality;
        return sum;
    }

    byte calculateMinQuality() {
        if ((qual == null) || isEmpty())
            return 0;
        byte minQuality = DEFAULT_MAX_QUALITY;
        for (byte quality : qual.asArray())
            if (quality < minQuality)
                minQuality = quality;
        return minQuality;
    }

    boolean isNull() {
        return seq == null;
    }

    boolean isEmpty() {
        return NucleotideSequence.EMPTY.equals(seq);
    }

    int size() {
        return (seq == null) ? 0 : seq.size();
    }

    @Override
    public String toString() {
        return "SequenceWithAttributes{" + "seq=" + seq + ", qual=" + qual
                + ", originalReadId=" + originalReadId + '}';
    }
}
