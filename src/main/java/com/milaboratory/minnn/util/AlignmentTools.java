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
package com.milaboratory.minnn.util;

import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.alignment.AlignmentIteratorForward;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;

import static com.milaboratory.core.mutations.Mutation.*;

public class AlignmentTools {
    public static long calculateAlignmentScore(long goodQualityMismatchPenalty, byte goodQualityThreshold,
                                               Alignment<NucleotideSequence> alignment, NSequenceWithQuality seq1,
                                               NSequenceWithQuality seq2) {
        long score = (long)(alignment.getScore());
        AlignmentIteratorForward<NucleotideSequence> iterator = alignment.forwardIterator();
        while (iterator.advance()) {
            final int mut = iterator.getCurrentMutation();
            if (getRawTypeCode(mut) == RAW_MUTATION_TYPE_SUBSTITUTION) {
                int seq1Position = iterator.getSeq1Position();
                int seq2Position = iterator.getSeq2Position();
                byte seq1CurrentQuality = seq1.getQuality().value(seq1Position);
                byte seq2CurrentQuality = seq2.getQuality().value(seq2Position);
                if ((seq1CurrentQuality >= goodQualityThreshold) && (seq2CurrentQuality >= goodQualityThreshold))
                    score += goodQualityMismatchPenalty;
            }
        }
        return score;
    }
}
