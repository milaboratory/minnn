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
package com.milaboratory.minnn.util;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.Wildcard;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.*;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.stat.StatUtils.*;
import static com.milaboratory.minnn.util.SequencesCache.*;

/**
 * Helper class for merging multiple letters with quality into one consensus letter. Transforms wildcards to basic
 * letters and stores letter counts and qualities.
 */
public class ConsensusLetter {
    private final TObjectLongHashMap<NucleotideSequence> letterCounts = new TObjectLongHashMap<>();
    private final ArrayList<LetterStats> stats = new ArrayList<>();
    private long inputLettersCount = 0;
    private NSequenceWithQuality lastInputLetter = null;

    public ConsensusLetter() {
        Arrays.stream(consensusMajorBases).forEach(letter -> letterCounts.put(letter, 0));
    }

    public void addLetter(NSequenceWithQuality inputLetter) {
        inputLettersCount++;
        lastInputLetter = inputLetter;
        if (inputLetter.equals(NSequenceWithQuality.EMPTY)) {
            letterCounts.put(NucleotideSequence.EMPTY, letterCounts.get(NucleotideSequence.EMPTY) + 1);
            stats.add(new LetterStats(NucleotideSequence.EMPTY, DEFAULT_BAD_QUALITY));
        } else if (inputLetter.getSequence().containsWildcards()) {
            Wildcard wildcard = wildcards.get(inputLetter.getSequence());
            for (int i = 0; i < wildcard.basicSize(); i++) {
                NucleotideSequence currentBasicLetter = wildcardCodeToSequence.get(wildcard.getMatchingCode(i));
                letterCounts.put(currentBasicLetter, letterCounts.get(currentBasicLetter) + 1);
                stats.add(new LetterStats(currentBasicLetter,
                        (double)(inputLetter.getQuality().value(0)) / wildcard.basicSize()));
            }
        } else {
            NucleotideSequence seq = inputLetter.getSequence();
            letterCounts.put(seq, letterCounts.get(seq) + 1);
            stats.add(new LetterStats(seq, inputLetter.getQuality().value(0)));
        }
    }

    public NSequenceWithQuality calculateConsensusLetter() {
        if (inputLettersCount < 1)
            throw new IllegalStateException("Trying to calculate consensus letter without input letters!");
        else if (inputLettersCount == 1)
            return Objects.requireNonNull(lastInputLetter);
        else {
            double gamma = 1.0 / (consensusMajorBases.length - 1);
            NucleotideSequence bestLetterSequence = null;
            double bestLetterQuality = -1;

            for (int i = 0; i < 4; i++) {
                NucleotideSequence letterOption = consensusMajorBases[i];   // don't count for empty option
                double product = Math.pow(gamma, -letterCounts.get(letterOption));
                for (LetterStats currentStats : stats) {
                    double errorProbability = qualityToProbability(Math.max(DEFAULT_BAD_QUALITY,
                            currentStats.quality));
                    double multiplier;
                    if (currentStats.letter.equals(letterOption))
                        multiplier = (1 - errorProbability) / Math.max(OVERFLOW_PROTECTION_MIN,
                                errorProbability);
                    else
                        multiplier = errorProbability / Math.max(OVERFLOW_PROTECTION_MIN,
                                1 - gamma * errorProbability);
                    product = Math.min(product * multiplier, OVERFLOW_PROTECTION_MAX);
                }

                double errorProbability = 1.0 / (1 + product);
                double quality = probabilityToQuality(errorProbability);
                if (quality > bestLetterQuality) {
                    bestLetterSequence = letterOption;
                    bestLetterQuality = quality;
                }
            }
            return new NSequenceWithQuality(bestLetterSequence, qualityCache.get((byte)bestLetterQuality));
        }
    }

    public TObjectLongHashMap<NucleotideSequence> getLetterCounts() {
        return new TObjectLongHashMap<>(letterCounts);
    }

    private static class LetterStats {
        final NucleotideSequence letter;
        final double quality;

        LetterStats(NucleotideSequence letter, double quality) {
            this.letter = letter;
            this.quality = quality;
        }
    }
}
