package com.milaboratory.minnn.correct;

import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.Wildcard;

import java.util.*;

import static com.milaboratory.minnn.util.SequencesCache.*;

final class MultiSequence {
    final HashMap<NucleotideSequence, Long> sequences = new HashMap<>();

    MultiSequence(NucleotideSequence seq) {
        long variability = 1;
        for (int i = 0; i < seq.size(); i++)
            variability *= charToWildcard.get(seq.symbolAt(i)).basicSize();
        sequences.put(seq, variability);
    }

    /**
     * @return sequence with smallest variability by wildcards
     */
    NucleotideSequence getBestSequence() {
        Map.Entry<NucleotideSequence, Long> bestEntry = null;
        for (Map.Entry<NucleotideSequence, Long> entry : sequences.entrySet())
            if ((bestEntry == null) || (bestEntry.getValue() > entry.getValue()))
                bestEntry = entry;
        return Objects.requireNonNull(bestEntry).getKey();
    }

    private boolean equalByWildcards(NucleotideSequence seq1, NucleotideSequence seq2) {
        if (seq1.size() != seq2.size())
            return false;
        for (int i = 0; i < seq1.size(); i++) {
            Wildcard wildcard1 = charToWildcard.get(seq1.symbolAt(i));
            Wildcard wildcard2 = charToWildcard.get(seq2.symbolAt(i));
            if ((wildcard1.getBasicMask() & wildcard2.getBasicMask()) == 0)
                return false;
        }
        return true;
    }

    /**
     * Important! Call to equals() will perform mutual merge if MultiSequences are equal by wildcards!
     *
     * @param o     other MultiSequence
     * @return      true if MultiSequences are equal by wildcards
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiSequence that = (MultiSequence)o;
        if (sequences.keySet().parallelStream().allMatch(seq1 -> that.sequences.keySet().stream()
                .allMatch(seq2 -> equalByWildcards(seq1, seq2)))) {
            sequences.putAll(that.sequences);
            that.sequences.putAll(sequences);
            return true;
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
