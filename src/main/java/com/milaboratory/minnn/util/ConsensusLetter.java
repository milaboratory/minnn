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
package com.milaboratory.minnn.util;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.Wildcard;

import java.util.*;

import static com.milaboratory.core.sequence.NucleotideSequence.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.stat.StatUtils.*;
import static com.milaboratory.minnn.util.SequencesCache.*;
import static java.util.stream.Stream.*;

/**
 * Helper class for merging multiple letters with quality into one consensus letter.
 */
public class ConsensusLetter {
    private static final HashMap<NSequenceWithQuality, List<LetterStats>> statsCache = new HashMap<>();
    // major bases are basic letters and EMPTY
    private static final int consensusMajorBasesNum = ALPHABET.basicSize() + 1;

    private final List<NSequenceWithQuality> inputLetters;
    private final long[] letterCounts;
    private List<LetterStats> letterStats = null;

    public ConsensusLetter(List<NSequenceWithQuality> inputLetters) {
        if (inputLetters.size() == 0)
            throw new IllegalArgumentException("inputLetters argument must not be empty!");
        this.inputLetters = inputLetters;
        this.letterCounts = calculateBasicLettersCounts(inputLetters);
        initStatsCache();
    }

    public NSequenceWithQuality getConsensusLetter() {
        if (inputLetters.size() == 1)
            return inputLetters.get(0);
        else {
            calculateStats();
            return calculateConsensusLetter(letterCounts, letterStats);
        }
    }

    public boolean isDeletionMaxCount() {
        for (int i = 0; i < ALPHABET.basicSize(); i++)
            if (letterCounts[i] > letterCounts[consensusMajorBasesNum - 1])
                return false;
        return true;
    }

    private static NSequenceWithQuality calculateConsensusLetter(long[] letterCounts, List<LetterStats> letterStats) {
        double gamma = 1.0 / ALPHABET.basicSize();
        NucleotideSequence bestLetterSequence = null;
        double bestLetterQuality = -1;

        for (byte i = 0; i < ALPHABET.basicSize(); i++) {
            NucleotideSequence letterOption = ONE_LETTER_SEQUENCES[i];  // don't count for empty option
            double product = Math.pow(gamma, -letterCounts[i]);
            for (LetterStats currentStats : letterStats) {
                double errorProbability = qualityToProbability(Math.max(DEFAULT_BAD_QUALITY, currentStats.quality));
                double multiplier;
                if (currentStats.letter.equals(letterOption))
                    multiplier = (1 - errorProbability) / Math.max(OVERFLOW_PROTECTION_MIN, errorProbability);
                else
                    multiplier = errorProbability / Math.max(OVERFLOW_PROTECTION_MIN, 1 - gamma * errorProbability);
                product = Math.min(product * multiplier, OVERFLOW_PROTECTION_MAX);
            }

            double errorProbability = 1.0 / (1 + product);
            double quality = probabilityToQuality(errorProbability);
            if (quality > bestLetterQuality) {
                bestLetterSequence = letterOption;
                bestLetterQuality = quality;
            }
        }

        return new NSequenceWithQuality(bestLetterSequence, getCachedQuality((byte)bestLetterQuality));
    }

    private synchronized void calculateStats() {
        if (letterStats == null) {
            letterStats = new ArrayList<>();
            inputLetters.stream().map(ConsensusLetter::fixQuality).forEach(currentLetter ->
                    letterStats.addAll(statsCache.get(currentLetter)));
        }
    }

    private static NSequenceWithQuality fixQuality(NSequenceWithQuality letter) {
        if (letter == NSequenceWithQuality.EMPTY)
            return letter;
        byte quality = letter.getQuality().value(0);
        if (quality < 0)
            return new NSequenceWithQuality(letter.getSequence(), getCachedQuality((byte)0));
        else if (quality > DEFAULT_MAX_QUALITY)
            return new NSequenceWithQuality(letter.getSequence(), getCachedQuality(DEFAULT_MAX_QUALITY));
        else
            return letter;
    }

    private static long[] calculateBasicLettersCounts(List<NSequenceWithQuality> letters) {
        long[] counts = new long[consensusMajorBasesNum];
        letters.forEach(letter -> {
            if (letter == NSequenceWithQuality.EMPTY)
                counts[consensusMajorBasesNum - 1]++;
            else if (letter.getSequence().containsWildcards()) {
                Wildcard wildcard = ALPHABET.codeToWildcard(letter.getSequence().codeAt(0));
                for (int i = 0; i < wildcard.basicSize(); i++)
                    counts[wildcard.getMatchingCode(i)]++;
            } else
                counts[letter.getSequence().codeAt(0)]++;
        });
        return counts;
    }

    private static synchronized void initStatsCache() {
        if (statsCache.isEmpty()) {
            // calculating counts and stats for all letters with all possible qualities
            concat(Arrays.stream(ONE_LETTER_SEQUENCES), of(EMPTY)).forEach(letter -> {
                for (byte quality = 0; quality <= DEFAULT_MAX_QUALITY; quality++) {
                    List<LetterStats> stats = new ArrayList<>();
                    if (letter == EMPTY) {
                        // EMPTY comes without quality, so add only one entry to the cache
                        if (quality == 0) {
                            stats.add(new LetterStats(EMPTY, DEFAULT_BAD_QUALITY));
                            statsCache.put(NSequenceWithQuality.EMPTY, stats);
                        }
                    } else {
                        if (letter.containsWildcards()) {
                            Wildcard wildcard = ALPHABET.codeToWildcard(letter.codeAt(0));
                            for (int i = 0; i < wildcard.basicSize(); i++) {
                                NucleotideSequence currentBasicLetter = ONE_LETTER_SEQUENCES[
                                        wildcard.getMatchingCode(i)];
                                stats.add(new LetterStats(currentBasicLetter,
                                        (double)quality / wildcard.basicSize()));
                            }
                        } else
                            stats.add(new LetterStats(letter, quality));
                        NSequenceWithQuality seq = getCachedSeqWithQuality(new NSequenceWithQuality(letter,
                                getCachedQuality(quality)));
                        statsCache.put(seq, stats);
                    }
                }
            });
        }
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
