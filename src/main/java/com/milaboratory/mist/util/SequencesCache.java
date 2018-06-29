package com.milaboratory.mist.util;

import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceQuality;
import com.milaboratory.core.sequence.Wildcard;
import gnu.trove.map.hash.TByteObjectHashMap;
import gnu.trove.map.hash.TCharObjectHashMap;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.core.sequence.SequenceQuality.MAX_QUALITY_VALUE;

public final class SequencesCache {
    public static final HashMap<NucleotideSequence, NucleotideSequence> sequencesCache = new HashMap<>();
    public static final TByteObjectHashMap<SequenceQuality> qualityCache = new TByteObjectHashMap<>();
    public static final HashMap<NucleotideSequence, Wildcard> wildcards = new HashMap<>();
    public static final TByteObjectHashMap<NucleotideSequence> wildcardCodeToSequence = new TByteObjectHashMap<>();
    public static final TCharObjectHashMap<Wildcard> charToWildcard = new TCharObjectHashMap<>();

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
        for (byte quality = 0; quality <= MAX_QUALITY_VALUE; quality++)
            qualityCache.put(quality, new SequenceQuality(new byte[] { quality }));
    }
}
