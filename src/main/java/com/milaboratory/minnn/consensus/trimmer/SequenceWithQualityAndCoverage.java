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
package com.milaboratory.minnn.consensus.trimmer;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceQuality;

import java.util.*;

/**
 * @see com.milaboratory.core.sequence.SequenceWithQuality
 */
public class SequenceWithQualityAndCoverage {
    private final NucleotideSequence sequence;
    private final SequenceQuality quality;
    private final float[] coverage;

    public SequenceWithQualityAndCoverage(NucleotideSequence sequence, SequenceQuality quality, float[] coverage) {
        if (sequence.size() != quality.size())
            throw new IllegalArgumentException("Different sizes, sequence: " + sequence + ", quality: " + quality);
        if (sequence.size() != coverage.length)
            throw new IllegalArgumentException("Different sizes, sequence: " + sequence + ", coverage: "
                    + Arrays.toString(coverage));
        this.sequence = sequence;
        this.quality = quality;
        this.coverage = coverage;
    }

    public NucleotideSequence getSequence() {
        return sequence;
    }

    public SequenceQuality getQuality() {
        return quality;
    }

    public float[] getCoverage() {
        return coverage;
    }

    public SequenceWithQualityAndCoverage getSubSequence(int from, int to) {
        float[] newCoverage = new float[to - from];
        if (to > from)
            System.arraycopy(coverage, from, newCoverage, 0, to - from);
        return new SequenceWithQualityAndCoverage(sequence.getRange(from, to), quality.getRange(from, to),
                newCoverage);
    }

    public NSequenceWithQuality toNSequenceWithQuality() {
        return new NSequenceWithQuality(sequence, quality);
    }

    public NSequenceWithQuality toNSequenceWithQuality(int from, int to) {
        return new NSequenceWithQuality(sequence.getRange(from, to), quality.getRange(from, to));
    }

    public int size() {
        return sequence.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SequenceWithQualityAndCoverage that = (SequenceWithQualityAndCoverage)o;
        if (!Objects.equals(sequence, that.sequence)) return false;
        if (!Objects.equals(quality, that.quality)) return false;
        return Arrays.equals(coverage, that.coverage);
    }

    @Override
    public int hashCode() {
        int result = sequence.hashCode();
        result = 31 * result + quality.hashCode();
        result = 31 * result + Arrays.hashCode(coverage);
        return result;
    }

    @Override
    public String toString() {
        return sequence + " " + quality + " " + Arrays.toString(coverage);
    }
}
