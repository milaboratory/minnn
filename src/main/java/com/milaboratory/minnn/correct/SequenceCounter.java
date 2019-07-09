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

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.correct.CorrectionUtils.*;
import static com.milaboratory.minnn.util.SequencesCache.*;

final class SequenceCounter implements Comparable<SequenceCounter> {
    private NSequenceWithQuality consensusSequence;
    private PreparedNewConsensusData preparedData = null;
    private final Set<NucleotideSequence> uniqueOriginalSequences = new HashSet<>();
    private final int index;
    private long count;
    private boolean containsWildcards;
    private boolean isMaxQuality;

    SequenceCounter(NSequenceWithQuality sequence, int index) {
        this.consensusSequence = sequence;
        uniqueOriginalSequences.add(sequence.getSequence());
        this.containsWildcards = sequence.getSequence().containsWildcards();
        this.isMaxQuality = !containsWildcards && (sequence.getQuality().minValue() == DEFAULT_MAX_QUALITY);
        this.index = index;
        this.count = 1;
    }

    Set<NucleotideSequence> getOriginalSequences() {
        return Collections.unmodifiableSet(uniqueOriginalSequences);
    }

    /**
     * @return consensus sequence with quality
     */
    NSequenceWithQuality getSequence() {
        return consensusSequence;
    }

    /**
     * Check if other sequence can be added (equals by wildcards) and prepare updated consensus sequence if
     * the other sequence can be added. This function is designed to be run in parallel in multiple sequence counters
     * to perform parallel wildcards check and consensus sequence calculation.
     *
     * @param other other sequence
     * @return      true if other sequence can be added and new consensus data is prepared, otherwise false
     */
    boolean tryToAdd(NSequenceWithQuality other) {
        boolean otherContainsWildcards = other.getSequence().containsWildcards();
        if (containsWildcards || otherContainsWildcards) {
            if (equalByWildcards(consensusSequence, other)) {
                NSequenceWithQuality newConsensusSequence = multipleSequencesMerged(Arrays.asList(
                        consensusSequence, other));
                NucleotideSequence originalSequence = other.getSequence();
                preparedData = new PreparedNewConsensusData(newConsensusSequence, true,
                        originalSequence, false);
                return true;
            } else {
                preparedData = null;
                return false;
            }
        } else {
            if (consensusSequence.getSequence().equals(other.getSequence())) {
                if (isMaxQuality)
                    preparedData = new PreparedNewConsensusData(consensusSequence, false,
                            null, true);
                else {
                    NSequenceWithQuality newConsensusSequence = multipleSequencesMerged(Arrays.asList(
                            consensusSequence, other));
                    preparedData = new PreparedNewConsensusData(newConsensusSequence, false,
                            null,
                            newConsensusSequence.getQuality().minValue() == DEFAULT_MAX_QUALITY);
                }
                return true;
            } else {
                preparedData = null;
                return false;
            }
        }
    }

    /**
     * Update consensus sequence for this counter. This function must be called only after successful tryToAdd() call.
     */
    void updateConsensusSequence() {
        consensusSequence = preparedData.newConsensusSequence;
        if (preparedData.containedWildcards) {
            isMaxQuality = false;
            uniqueOriginalSequences.add(Objects.requireNonNull(preparedData.originalSequence));
            containsWildcards = false;
        } else
            isMaxQuality = preparedData.isMaxQuality;
        count++;
        preparedData = null;
    }

    long getCount() {
        return count;
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
        return Long.compare(count, other.count);
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

    private class PreparedNewConsensusData {
        final NSequenceWithQuality newConsensusSequence;
        final boolean containedWildcards;
        final NucleotideSequence originalSequence;
        final boolean isMaxQuality;

        PreparedNewConsensusData(NSequenceWithQuality newConsensusSequence, boolean containedWildcards,
                                 NucleotideSequence originalSequence, boolean isMaxQuality) {
            this.newConsensusSequence = newConsensusSequence;
            this.containedWildcards = containedWildcards;
            this.originalSequence = originalSequence;
            this.isMaxQuality = isMaxQuality;
        }
    }
}
