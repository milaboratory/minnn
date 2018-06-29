package com.milaboratory.mist.stat;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.Wildcard;

import static com.milaboratory.core.mutations.Mutation.getFromSymbol;
import static com.milaboratory.core.mutations.Mutation.getToSymbol;
import static com.milaboratory.core.mutations.Mutation.isInDel;
import static com.milaboratory.mist.util.SequencesCache.charToWildcard;
import static com.milaboratory.mist.util.SequencesCache.wildcards;

public final class SimpleMutationProbability implements MutationProbability {
    private final float basicSubstitutionProbability;
    private final float indelProbability;

    public SimpleMutationProbability(float basicSubstitutionProbability, float indelProbability) {
        this.basicSubstitutionProbability = basicSubstitutionProbability;
        this.indelProbability = indelProbability;
    }

    @Override
    public float mutationProbability(NSequenceWithQuality seq, int position, NSequenceWithQuality newLetter) {
        return mutationProbability(seq.getSequence(), position, newLetter.getSequence());
    }

    @Override
    public float mutationProbability(NucleotideSequence seq, int position, NucleotideSequence newLetter) {
        if ((position < 0) && (newLetter == NucleotideSequence.EMPTY))
            throw new IllegalArgumentException("Mutation must not be insertion and deletion in the same time: "
                    + "seq=" + seq + ", position=" + position + ", newLetter is empty");
        else if ((position < 0) || (newLetter == NucleotideSequence.EMPTY))
            return indelProbability;
        else {
            Wildcard wildcardFrom = wildcards.get(seq.getRange(position, position + 1));
            Wildcard wildcardTo = wildcards.get(newLetter);
            if ((wildcardFrom.getBasicMask() & wildcardTo.getBasicMask()) == 0)
                return basicSubstitutionProbability;
            else
                return 1;
        }
    }

    @Override
    public float mutationProbability(int mutationCode) {
        if (isInDel(mutationCode))
            return indelProbability;
        else {
            Wildcard wildcardFrom = charToWildcard.get(getFromSymbol(mutationCode, NucleotideSequence.ALPHABET));
            Wildcard wildcardTo = charToWildcard.get(getToSymbol(mutationCode, NucleotideSequence.ALPHABET));
            if ((wildcardFrom.getBasicMask() & wildcardTo.getBasicMask()) == 0)
                return basicSubstitutionProbability;
            else
                return 1;
        }
    }
}
