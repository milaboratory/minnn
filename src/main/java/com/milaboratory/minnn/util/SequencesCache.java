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

import com.milaboratory.core.sequence.*;
import gnu.trove.map.hash.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.core.sequence.NucleotideSequence.ALPHABET;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.stat.StatUtils.*;

public final class SequencesCache {
    private SequencesCache() {}

    public static final HashMap<NucleotideSequence, NucleotideSequence> sequencesCache = new HashMap<>();
    public static final NucleotideSequence[] consensusMajorBases;
    public static final TObjectIntHashMap<NucleotideSequence> majorBasesIndexes = new TObjectIntHashMap<>();
    public static final int majorBasesEmptyIndex;
    public static final List<NucleotideSequence> allLetters = new ArrayList<>();
    public static final NucleotideSequence[] codeToSequence = new NucleotideSequence[ALPHABET.size()];
    public static final long[] basicLettersMasks = new long[ALPHABET.basicSize()];
    public static final float[] qualityToLetterProbabilityCache = new float[DEFAULT_MAX_QUALITY + 1];
    private static TByteObjectHashMap<SequenceQuality> qualityCache = null;
    private static HashMap<NSequenceWithQuality, NSequenceWithQuality> seqWithQualityCache = null;
    private static TIntObjectHashMap<NucleotideSequenceCaseSensitive> sequencesOfN = null;
    private static HashMap<NucleotideSequenceCaseSensitive, TIntObjectHashMap<NucleotideSequenceCaseSensitive>>
            sequencesOfCharacters = null;

    static {
        List<String> alphabet = ALPHABET.getAllWildcards().stream()
                .map(wildcard -> String.valueOf(wildcard.getSymbol())).collect(Collectors.toList());
        alphabet.stream().map(NucleotideSequence::new).forEach(seq -> sequencesCache.put(seq, seq));
        alphabet.forEach(first -> alphabet.forEach(second -> {
            NucleotideSequence currentSequence = new NucleotideSequence(first + second);
            sequencesCache.put(currentSequence, currentSequence);
        }));

        consensusMajorBases = new NucleotideSequence[] {
                sequencesCache.get(new NucleotideSequence("A")), sequencesCache.get(new NucleotideSequence("T")),
                sequencesCache.get(new NucleotideSequence("G")), sequencesCache.get(new NucleotideSequence("C")),
                NucleotideSequence.EMPTY };
        for (int i = 0; i < consensusMajorBases.length; i++)
            majorBasesIndexes.put(consensusMajorBases[i], i);
        majorBasesEmptyIndex = majorBasesIndexes.get(NucleotideSequence.EMPTY);

        ALPHABET.getAllWildcards().forEach(wildcard -> {
            String letter = String.valueOf(wildcard.getSymbol());
            NucleotideSequence sequence = sequencesCache.get(new NucleotideSequence(letter));
            allLetters.add(sequence);
            codeToSequence[wildcard.getCode()] = sequence;
        });

        for (byte i = 0; i < basicLettersMasks.length; i++)
            basicLettersMasks[i] = ALPHABET.codeToWildcard(i).getBasicMask();

        for (byte i = 0; i <= DEFAULT_MAX_QUALITY; i++)
            qualityToLetterProbabilityCache[i] = (float)(1 - qualityToProbability(i));
    }

    private static synchronized void initQualityCaches(boolean withSequences) {
        if ((qualityCache == null) || (withSequences && (seqWithQualityCache == null))) {
            TByteObjectHashMap<SequenceQuality> newQualityCache = new TByteObjectHashMap<>();
            HashMap<NSequenceWithQuality, NSequenceWithQuality> newSeqWithQualityCache = withSequences
                    ? new HashMap<>() : null;
            for (byte quality = 0; quality <= DEFAULT_MAX_QUALITY; quality++) {
                SequenceQuality qualObject = (qualityCache == null) ? new SequenceQuality(new byte[] { quality })
                        : qualityCache.get(quality);
                if (qualityCache == null)
                    newQualityCache.put(quality, qualObject);
                if (withSequences)
                    allLetters.forEach(seq -> {
                        NSequenceWithQuality nSequenceWithQuality = new NSequenceWithQuality(seq, qualObject);
                        newSeqWithQualityCache.put(nSequenceWithQuality, nSequenceWithQuality);
                    });
            }
            if (qualityCache == null)
                qualityCache = newQualityCache;
            if (withSequences)
                seqWithQualityCache = newSeqWithQualityCache;
        }
    }

    private static synchronized void initRepeatedLettersCaches() {
        if ((sequencesOfN == null) || (sequencesOfCharacters == null)) {
            TIntObjectHashMap<NucleotideSequenceCaseSensitive> newSequencesOfN = new TIntObjectHashMap<>();
            HashMap<NucleotideSequenceCaseSensitive, TIntObjectHashMap<NucleotideSequenceCaseSensitive>>
                    newSequencesOfCharacters = new HashMap<>();
            for (int i = 0; i <= SEQUENCES_OF_N_CACHE_SIZE; i++)
                newSequencesOfN.put(i, generateSequenceOfCharacters("N", i));
            NucleotideSequenceCaseSensitive.ALPHABET.getAllWildcards().stream()
                    .filter(wildcard -> Character.toUpperCase(wildcard.getSymbol()) != 'N')
                    .map(wildcard -> String.valueOf(wildcard.getSymbol())).forEach(character -> {
                NucleotideSequenceCaseSensitive currentSeq = new NucleotideSequenceCaseSensitive(character);
                TIntObjectHashMap<NucleotideSequenceCaseSensitive> currentCache = new TIntObjectHashMap<>();
                for (int i = 0; i <= SEQUENCES_OF_CHARACTERS_CACHE_SIZE; i++)
                    currentCache.put(i, generateSequenceOfCharacters(character, i));
                newSequencesOfCharacters.put(currentSeq, currentCache);
            });
            sequencesOfN = newSequencesOfN;
            sequencesOfCharacters = newSequencesOfCharacters;
        }
    }

    public static SequenceQuality getCachedQuality(byte quality) {
        if (qualityCache == null)
            initQualityCaches(false);
        return qualityCache.get(quality);
    }

    public static NSequenceWithQuality getCachedSeqWithQuality(NSequenceWithQuality seq) {
        if (seqWithQualityCache == null)
            initQualityCaches(true);
        return seqWithQualityCache.get(seq);
    }

    public static NucleotideSequenceCaseSensitive getSequenceOfN(int number) {
        if (sequencesOfN == null)
            initRepeatedLettersCaches();
        if (number <= SEQUENCES_OF_N_CACHE_SIZE)
            return sequencesOfN.get(number);
        else
            return generateSequenceOfCharacters("N", number);
    }

    public static NucleotideSequenceCaseSensitive getSequenceOfCharacters(
            NucleotideSequenceCaseSensitive character, int number) {
        if (character.size() != 1)
            throw new IllegalArgumentException("getSequenceOfCharacters() called with character argument "
                    + character);
        if (sequencesOfCharacters == null)
            initRepeatedLettersCaches();
        if (number <= SEQUENCES_OF_CHARACTERS_CACHE_SIZE)
            return sequencesOfCharacters.get(character).get(number);
        else
            return generateSequenceOfCharacters(character.toString(), number);
    }

    private static NucleotideSequenceCaseSensitive generateSequenceOfCharacters(String character, int number) {
        if (number == 0)
            return NucleotideSequenceCaseSensitive.EMPTY;
        else
            return new NucleotideSequenceCaseSensitive(new String(new char[number])
                    .replace("\0", character));
    }
}
