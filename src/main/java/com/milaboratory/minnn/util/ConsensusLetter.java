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
 * Helper class for merging multiple letters with quality into one consensus letter.
 */
public class ConsensusLetter {
    private static final HashMap<TObjectLongHashMap<NucleotideSequence>, TObjectLongHashMap<NucleotideSequence>>
            countsCache = new HashMap<>();
    private static final HashMap<NSequenceWithQuality, TObjectLongHashMap<NucleotideSequence>> singleCountsCache
            = new HashMap<>();
    private static final HashMap<PairOfLetters, TObjectLongHashMap<NucleotideSequence>> mergedCountsCache
            = new HashMap<>();
    private static final HashMap<PairOfLetters, NSequenceWithQuality> consensusesCache = new HashMap<>();
    private static final HashMap<NSequenceWithQuality, List<LetterStats>> statsCache = new HashMap<>();
    private static boolean cachesInitialized = false;

    private final List<NSequenceWithQuality> inputLetters;
    private TObjectLongHashMap<NucleotideSequence> letterCounts = null;
    private List<LetterStats> letterStats = null;

    public ConsensusLetter(List<NSequenceWithQuality> inputLetters) {
        if (inputLetters.size() == 0)
            throw new IllegalArgumentException("inputLetters argument must not be empty!");
        this.inputLetters = inputLetters;
        initCaches();
    }

    public NSequenceWithQuality getConsensusLetter() {
        if (inputLetters.size() == 1)
            return inputLetters.get(0);
        else {
            if (inputLetters.size() == 2) {
                PairOfLetters pair = new PairOfLetters(inputLetters.get(0), inputLetters.get(1));
                if (consensusesCache.containsKey(pair))
                    return consensusesCache.get(pair);
            }
            calculateCountsAndStats();
            return calculateConsensusLetter(letterCounts, letterStats);
        }
    }

    public TObjectLongHashMap<NucleotideSequence> getLetterCounts() {
        if (inputLetters.size() == 1)
            return singleCountsCache.get(fixQuality((inputLetters.get(0))));
        else {
            if (inputLetters.size() == 2) {
                PairOfLetters pair = new PairOfLetters(inputLetters.get(0), inputLetters.get(1));
                if (mergedCountsCache.containsKey(pair))
                    return mergedCountsCache.get(pair);
            }
            calculateCountsAndStats();
            return new TObjectLongHashMap<>(letterCounts);
        }
    }

    private static NSequenceWithQuality calculateConsensusLetter(
            TObjectLongHashMap<NucleotideSequence> letterCounts, List<LetterStats> letterStats) {
        double gamma = 1.0 / (consensusMajorBases.length - 1);
        NucleotideSequence bestLetterSequence = null;
        double bestLetterQuality = -1;

        for (int i = 0; i < 4; i++) {
            NucleotideSequence letterOption = consensusMajorBases[i];   // don't count for empty option
            double product = Math.pow(gamma, -letterCounts.get(letterOption));
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

        return seqWithQualityCache.get(new NSequenceWithQuality(bestLetterSequence,
                qualityCache.get((byte)bestLetterQuality)));
    }

    private static TObjectLongHashMap<NucleotideSequence> getCachedCounts(
            TObjectLongHashMap<NucleotideSequence> counts) {
        if (countsCache.containsKey(counts))
            return countsCache.get(counts);
        else {
            countsCache.put(counts, counts);
            return counts;
        }
    }

    private static TObjectLongHashMap<NucleotideSequence> mergeCounts(
            List<TObjectLongHashMap<NucleotideSequence>> separateCounts) {
        TObjectLongHashMap<NucleotideSequence> mergedCounts = new TObjectLongHashMap<>();
        Arrays.stream(consensusMajorBases).forEach(baseLetter -> mergedCounts.put(baseLetter, 0));
        separateCounts.forEach(separateCount ->
                separateCount.forEachEntry((currentLetter, currentCount) -> {
                    mergedCounts.put(currentLetter, mergedCounts.get(currentLetter) + currentCount);
                    return true;
                }));
        return mergedCounts;
    }

    private synchronized void calculateCountsAndStats() {
        if ((letterCounts == null) && (letterStats == null)) {
            letterStats = new ArrayList<>();
            List<TObjectLongHashMap<NucleotideSequence>> separateCounts = new ArrayList<>();
            inputLetters.stream().map(ConsensusLetter::fixQuality).forEach(currentLetter -> {
                separateCounts.add(singleCountsCache.get(currentLetter));
                letterStats.addAll(statsCache.get(currentLetter));
            });
            letterCounts = mergeCounts(separateCounts);
        }
    }

    private static NSequenceWithQuality fixQuality(NSequenceWithQuality letter) {
        if (letter == NSequenceWithQuality.EMPTY)
            return letter;
        byte quality = letter.getQuality().value(0);
        if (quality < 0)
            return seqWithQualityCache.get(new NSequenceWithQuality(letter.getSequence(), qualityCache.get((byte)0)));
        else if (quality > DEFAULT_MAX_QUALITY)
            return seqWithQualityCache.get(new NSequenceWithQuality(letter.getSequence(),
                    qualityCache.get(DEFAULT_MAX_QUALITY)));
        else
            return letter;
    }

    private static synchronized void initCaches() {
        if (!cachesInitialized) {
            // calculating counts and stats for all letters with all possible qualities
            List<NucleotideSequence> allLettersWithEmpty = new ArrayList<>(allLetters);
            allLettersWithEmpty.add(NucleotideSequence.EMPTY);
            allLettersWithEmpty.forEach(letter -> {
                for (byte quality = 0; quality <= DEFAULT_MAX_QUALITY; quality++) {
                    TObjectLongHashMap<NucleotideSequence> counts = new TObjectLongHashMap<>();
                    Arrays.stream(consensusMajorBases).forEach(baseLetter -> counts.put(baseLetter, 0));
                    List<LetterStats> stats = new ArrayList<>();
                    if (letter == NucleotideSequence.EMPTY) {
                        // EMPTY comes without quality, so add only one entry to the cache
                        if (quality == 0) {
                            counts.put(NucleotideSequence.EMPTY, 1);
                            stats.add(new LetterStats(NucleotideSequence.EMPTY, DEFAULT_BAD_QUALITY));
                            singleCountsCache.put(NSequenceWithQuality.EMPTY, getCachedCounts(counts));
                            statsCache.put(NSequenceWithQuality.EMPTY, stats);
                        }
                    } else {
                        if (letter.containsWildcards()) {
                            Wildcard wildcard = wildcards.get(letter);
                            for (int i = 0; i < wildcard.basicSize(); i++) {
                                NucleotideSequence currentBasicLetter = wildcardCodeToSequence
                                        .get(wildcard.getMatchingCode(i));
                                counts.put(currentBasicLetter, counts.get(currentBasicLetter) + 1);
                                stats.add(new LetterStats(currentBasicLetter,
                                        (double)quality / wildcard.basicSize()));
                            }
                        } else {
                            counts.put(letter, counts.get(letter) + 1);
                            stats.add(new LetterStats(letter, quality));
                        }
                        NSequenceWithQuality seq = seqWithQualityCache.get(new NSequenceWithQuality(letter,
                                qualityCache.get(quality)));
                        singleCountsCache.put(seq, getCachedCounts(counts));
                        statsCache.put(seq, stats);
                    }
                }
            });

            // calculating cached consensuses for all pairs for base letters and N
            LinkedHashSet<NucleotideSequence> baseLettersWithN = new LinkedHashSet<>(
                    Arrays.asList(consensusMajorBases));
            baseLettersWithN.remove(NucleotideSequence.EMPTY);
            baseLettersWithN.add(new NucleotideSequence("N"));
            baseLettersWithN.forEach(letter1 ->
                    baseLettersWithN.forEach(letter2 -> {
                        for (byte quality1 = 0; quality1 <= DEFAULT_MAX_QUALITY; quality1++)
                            for (byte quality2 = 0; quality2 <= DEFAULT_MAX_QUALITY; quality2++) {
                                NSequenceWithQuality seq1 = seqWithQualityCache.get(new NSequenceWithQuality(letter1,
                                        qualityCache.get(quality1)));
                                NSequenceWithQuality seq2 = seqWithQualityCache.get(new NSequenceWithQuality(letter2,
                                        qualityCache.get(quality2)));
                                PairOfLetters pair = new PairOfLetters(seq1, seq2);
                                if (!consensusesCache.containsKey(pair)) {
                                    TObjectLongHashMap<NucleotideSequence> letterCounts = getCachedCounts(mergeCounts(
                                            Arrays.asList(singleCountsCache.get(seq1), singleCountsCache.get(seq2))));
                                    mergedCountsCache.put(pair, letterCounts);
                                    List<LetterStats> letterStats = new ArrayList<>();
                                    letterStats.addAll(statsCache.get(seq1));
                                    letterStats.addAll(statsCache.get(seq2));
                                    consensusesCache.put(pair, calculateConsensusLetter(letterCounts, letterStats));
                                }
                            }
                    }));

            cachesInitialized = true;
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

    /**
     * Pair of nucleotide sequences with qualities; order is not important for equality of two pairs.
     * This class is used as key for HashMap in cache of precalculated consensuses for letter pairs.
     */
    private static class PairOfLetters {
        private final NSequenceWithQuality letter1;
        private final NSequenceWithQuality letter2;

        PairOfLetters(NSequenceWithQuality letter1, NSequenceWithQuality letter2) {
            this.letter1 = letter1;
            this.letter2 = letter2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PairOfLetters that = (PairOfLetters)o;
            return (letter1.equals(that.letter1) && letter2.equals(that.letter2))
                    || (letter1.equals(that.letter2) && letter2.equals(that.letter1));
        }

        @Override
        public int hashCode() {
            return letter1.hashCode() + letter2.hashCode();
        }
    }
}
