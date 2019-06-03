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
package com.milaboratory.minnn.pattern;

import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;

public interface PatternAligner {
    /**
     * Returned alignment will have maximal score equals zero.
     *
     * @param pattern case sensitive nucleotide sequence from pattern
     * @param target target nucleotide sequence with quality
     * @param rightMatchPosition right position of found bitap match, inclusive
     * @return alignment with score less or equals than zero
     */
    Alignment<NucleotideSequenceCaseSensitive> align(NucleotideSequenceCaseSensitive pattern,
            NSequenceWithQuality target, int rightMatchPosition);
    /**
     * Penalty threshold, negative value.
     *
     * @return penalty threshold
     */
    long penaltyThreshold();

    /**
     * Calculate penalty value for given overlap in the target.
     *
     * @param target        target
     * @param overlapOffset offset inclusive
     * @param overlapLength length
     * @return negative penalty value
     */
    long overlapPenalty(NSequenceWithQuality target, int overlapOffset, int overlapLength);

    /**
     * Calculate penalty value for given insertion in the target.
     *
     * @param target          target
     * @param insertionOffset offset inclusive
     * @param insertionLength length
     * @return negative penalty value
     */
    long insertionPenalty(NSequenceWithQuality target, int insertionOffset, int insertionLength);

    /**
     * Get penalty value for given number of motif repeats for RepeatPattern match.
     *
     * @param motif motif of this RepeatPattern
     * @param repeats number of motif repeats for this RepeatPattern match
     * @param maxRepeats maximum number of motif repeats for this RepeatPattern
     * @return negative penalty value
     */
    default long repeatsPenalty(NucleotideSequenceCaseSensitive motif, int repeats, int maxRepeats) {
        return 0;
    }

    /**
     * Max errors to use in bitap matcher.
     *
     * @return max errors for bitap
     */
    int bitapMaxErrors();

    /**
     * Score for result of Not operator.
     *
     * @return not result score
     */
    default long notResultScore() {
        return 0;
    }

    /**
     * Maximal allowed overlap for two intersecting sub-patterns
     *
     * @return max overlap; or -1 if no limit on overlap size
     */
    default int maxOverlap() {
        return -1;
    }

    /**
     * Fixed left border position for alignment if it is specified; -1 if not specified.
     * If left border is specified, global aligner is used.
     *
     * @return left border position for alignment if it is specified; -1 if not specified
     */
    default int leftBorder() { return -1; }

    /**
     * Return new pattern with more strict penalty threshold.
     *
     * @return copy of this PatternAligner with more strict penalty threshold
     */
    PatternAligner overridePenaltyThreshold(long newThresholdValue);

    /**
     * Set left border for alignment. When it is set, global aligner is used.
     *
     * @param leftBorder left border for alignment
     * @return copy of this PatternAligner with fixed left border
     */
    PatternAligner setLeftBorder(int leftBorder);
}
