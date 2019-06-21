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

import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceQuality;
import com.milaboratory.core.sequence.Wildcard;
import gnu.trove.map.hash.TByteObjectHashMap;
import gnu.trove.map.hash.TCharObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.cli.Defaults.DEFAULT_MAX_QUALITY;

public final class SequencesCache {
    private SequencesCache() {}

    public static final HashMap<NucleotideSequence, NucleotideSequence> sequencesCache = new HashMap<>();
    public static final TByteObjectHashMap<SequenceQuality> qualityCache = new TByteObjectHashMap<>();
    public static final HashMap<NucleotideSequence, Wildcard> wildcards = new HashMap<>();
    public static final TByteObjectHashMap<NucleotideSequence> wildcardCodeToSequence = new TByteObjectHashMap<>();
    public static final TCharObjectHashMap<Wildcard> charToWildcard = new TCharObjectHashMap<>();
    public static final TObjectLongHashMap<NucleotideSequence> basicLettersMasks = new TObjectLongHashMap<>();

    static {
        List<String> alphabet = NucleotideSequence.ALPHABET.getAllWildcards().stream()
                .map(wildcard -> String.valueOf(wildcard.getSymbol())).collect(Collectors.toList());
        alphabet.stream().map(NucleotideSequence::new).forEach(seq -> sequencesCache.put(seq, seq));
        alphabet.forEach(first -> alphabet.forEach(second -> {
            NucleotideSequence currentSequence = new NucleotideSequence(first + second);
            sequencesCache.put(currentSequence, currentSequence);
        }));
        NucleotideSequence.ALPHABET.getAllWildcards().forEach(wildcard -> {
            String letter = String.valueOf(wildcard.getSymbol());
            NucleotideSequence sequence = sequencesCache.get(new NucleotideSequence(letter));
            wildcards.put(sequence, wildcard);
            wildcardCodeToSequence.put(wildcard.getCode(), sequence);
            charToWildcard.put(wildcard.getSymbol(), wildcard);
        });
        for (byte quality = 0; quality <= DEFAULT_MAX_QUALITY; quality++)
            qualityCache.put(quality, new SequenceQuality(new byte[] { quality }));
        NucleotideSequence.ALPHABET.getAllWildcards().stream().filter(Wildcard::isBasic).forEach(wildcard ->
                basicLettersMasks.put(wildcardCodeToSequence.get(wildcard.getCode()), wildcard.getBasicMask()));
    }
}
