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
package com.milaboratory.minnn.correct;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.Wildcard;

import java.util.*;

import static com.milaboratory.minnn.correct.CorrectionUtils.*;
import static com.milaboratory.minnn.util.SequencesCache.*;

final class SequenceCounter implements Comparable<SequenceCounter> {
    private final List<NSequenceWithQuality> sequences = new ArrayList<>();
    private final int index;
    private boolean containsWildcards;
    private NSequenceWithQuality cachedSequence = null;

    SequenceCounter(NSequenceWithQuality sequence, int index) {
        sequences.add(sequence);
        this.containsWildcards = sequence.getSequence().containsWildcards();
        this.index = index;
    }

    List<NSequenceWithQuality> getSequences() {
        return Collections.unmodifiableList(sequences);
    }

    /**
     * @return consensus sequence with quality
     */
    NSequenceWithQuality getSequence() {
        if (cachedSequence == null)
            cachedSequence = (sequences.size() == 1) ? sequences.get(0) : multipleSequencesMerged(sequences);
        return cachedSequence;
    }

    /**
     * Add other sequence to this counter if other sequence equals by wildcards, otherwise return false.
     *
     * @param other other sequence
     * @return      true if other sequence was added, otherwise false
     */
    boolean add(NSequenceWithQuality other) {
        boolean otherContainsWildcards = other.getSequence().containsWildcards();
        if (containsWildcards || otherContainsWildcards) {
            if (sequences.stream().allMatch(seq -> equalByWildcards(seq, other))) {
                sequences.add(other);
                containsWildcards = true;
                cachedSequence = null;
                return true;
            } else
                return false;
        } else {
            if (sequences.get(0).getSequence().equals(other.getSequence())) {
                sequences.add(other);
                cachedSequence = null;
                return true;
            } else
                return false;
        }
    }

    long getCount() {
        return sequences.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SequenceCounter that = (SequenceCounter)o;
        return index == that.index;
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public int compareTo(SequenceCounter other) {
        return Long.compare(sequences.size(), other.sequences.size());
    }

    int compareForTreeSet(SequenceCounter other) {
        int comparisonResult = -compareTo(other);
        return (comparisonResult == 0) ? Integer.compare(index, other.index) : comparisonResult;
    }

    private boolean equalByWildcards(NSequenceWithQuality seq1, NSequenceWithQuality seq2) {
        if (seq1.size() != seq2.size())
            return false;
        NucleotideSequence s1 = seq1.getSequence();
        NucleotideSequence s2 = seq2.getSequence();
        if (s1.equals(s2))
            return true;
        for (int i = 0; i < seq1.size(); i++) {
            Wildcard wildcard1 = charToWildcard.get(s1.symbolAt(i));
            Wildcard wildcard2 = charToWildcard.get(s2.symbolAt(i));
            if ((wildcard1.getBasicMask() & wildcard2.getBasicMask()) == 0)
                return false;
        }
        return true;
    }
}
