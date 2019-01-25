package com.milaboratory.minnn.correct;

import com.milaboratory.core.sequence.NucleotideSequence;

final class SequenceCounter implements Comparable<SequenceCounter> {
    final MultiSequence multiSequence;
    long count;

    SequenceCounter(NucleotideSequence sequence) {
        multiSequence = new MultiSequence(sequence);
        count = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SequenceCounter that = (SequenceCounter)o;
        // MultiSequence objects will perform mutual merge if equal
        return multiSequence.equals(that.multiSequence);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    // compareTo is reversed to start from bigger counts
    @Override
    public int compareTo(SequenceCounter other) {
        int comparisonResult = -Long.compare(count, other.count);
        // disable equal counts because they lead to objects loss
        return (comparisonResult == 0) ? 1 : comparisonResult;
    }
}
