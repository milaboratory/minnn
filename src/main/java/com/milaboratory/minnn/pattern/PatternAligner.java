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

import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;

public interface PatternAligner {
    /**
     * Perform alignment of pattern and target or return null if alignment is impossible with specified parameters.
     *
     * @param conf                  pattern configuration: contains scoring, left border and other pattern settings
     * @param withoutIndels         true if indels are not allowed, otherwise false
     * @param pattern               case sensitive nucleotide sequence from pattern
     * @param target                target nucleotide sequence with quality
     * @param rightMatchPosition    right position of found bitap match, inclusive
     * @return                      alignment or null if alignment is impossible
     */
    Alignment<NucleotideSequenceCaseSensitive> align(
            PatternConfiguration conf, boolean withoutIndels, NucleotideSequenceCaseSensitive pattern,
            NSequenceWithQuality target, int rightMatchPosition);

    /**
     * Calculate penalty value for given overlap in the target.
     *
     * @param conf          pattern configuration
     * @param target        target
     * @param overlapOffset offset inclusive
     * @param overlapLength length
     * @return              negative penalty value
     */
    long overlapPenalty(
            PatternConfiguration conf, NSequenceWithQuality target, int overlapOffset, int overlapLength);

    /**
     * Calculate penalty value for given insertion in the target.
     *
     * @param conf              pattern configuration
     * @param target            target
     * @param insertionOffset   offset inclusive
     * @param insertionLength   length
     * @return                  negative penalty value
     */
    long insertionPenalty(
            PatternConfiguration conf, NSequenceWithQuality target, int insertionOffset, int insertionLength);

    /**
     * Get penalty value for given number of motif repeats for RepeatPattern match.
     *
     * @param conf          pattern configuration
     * @param motif         motif of this RepeatPattern
     * @param repeats       number of motif repeats for this RepeatPattern match
     * @param maxRepeats    maximum number of motif repeats for this RepeatPattern
     * @return              negative penalty value
     */
    default long repeatsPenalty(
            PatternConfiguration conf, NucleotideSequenceCaseSensitive motif, int repeats, int maxRepeats) {
        return 0;
    }
}
