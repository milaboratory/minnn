package com.milaboratory.minnn.correct;

import com.milaboratory.core.clustering.SequenceExtractor;
import com.milaboratory.core.sequence.NucleotideSequence;

final class SequenceCounterExtractor implements SequenceExtractor<SequenceCounter, NucleotideSequence> {
    @Override
    public NucleotideSequence getSequence(SequenceCounter sequenceCounter) {
        return sequenceCounter.multiSequence.getBestSequence();
    }
}
