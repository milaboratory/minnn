package com.milaboratory.mist.stat;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;

public interface MutationProbability {
    // position can be -2-position, it means insertion; newLetter can be EMPTY, it means deletion
    float mutationProbability(NSequenceWithQuality seq, int position, NSequenceWithQuality newLetter);
    float mutationProbability(NucleotideSequence seq, int position, NucleotideSequence newLetter);
    float mutationProbability(int mutationCode);
}
