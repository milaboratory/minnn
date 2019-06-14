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
import com.milaboratory.core.sequence.Wildcard;

import java.util.*;

import static com.milaboratory.minnn.correct.CorrectionUtils.*;
import static com.milaboratory.minnn.util.SequencesCache.*;

final class MultiSequence {
    private final HashSet<NSequenceWithQuality> sequences = new HashSet<>();
    private NSequenceWithQuality cachedSequence = null;

    MultiSequence(NSequenceWithQuality seq) {
        sequences.add(seq);
    }

    Set<NSequenceWithQuality> getSequences() {
        return Collections.unmodifiableSet(sequences);
    }

    /**
     * @return consensus sequence with quality
     */
    NSequenceWithQuality getSequence() {
        if (cachedSequence == null)
            cachedSequence = (sequences.size() == 1)
                    ? sequences.stream().findFirst().orElseThrow(IllegalStateException::new)
                    : multipleSequencesMerged(new ArrayList<>(sequences));
        return cachedSequence;
    }

    private boolean equalByWildcards(NSequenceWithQuality seq1, NSequenceWithQuality seq2) {
        if (seq1.size() != seq2.size())
            return false;
        for (int i = 0; i < seq1.size(); i++) {
            Wildcard wildcard1 = charToWildcard.get(seq1.getSequence().symbolAt(i));
            Wildcard wildcard2 = charToWildcard.get(seq2.getSequence().symbolAt(i));
            if ((wildcard1.getBasicMask() & wildcard2.getBasicMask()) == 0)
                return false;
        }
        return true;
    }

    /**
     * Important! Call to equals() will perform mutual merge if MultiSequences are equal by wildcards!
     *
     * @param o     other MultiSequence
     * @return      true if MultiSequences are equal by wildcards
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiSequence that = (MultiSequence)o;
        if (sequences.parallelStream().allMatch(seq1 -> that.sequences.stream()
                .allMatch(seq2 -> equalByWildcards(seq1, seq2)))) {
            sequences.addAll(that.sequences);
            that.sequences.addAll(sequences);
            cachedSequence = null;
            that.cachedSequence = null;
            return true;
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
