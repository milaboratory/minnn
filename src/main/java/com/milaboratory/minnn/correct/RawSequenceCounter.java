package com.milaboratory.minnn.correct;

import com.milaboratory.core.sequence.NucleotideSequence;

import java.util.Objects;

final class RawSequenceCounter implements Comparable<RawSequenceCounter> {
    final NucleotideSequence seq;
    long count;

    RawSequenceCounter(NucleotideSequence seq) {
        this.seq = Objects.requireNonNull(seq);
        count = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawSequenceCounter that = (RawSequenceCounter)o;
        return seq.equals(that.seq);
    }

    @Override
    public int hashCode() {
        return seq.hashCode();
    }

    // compareTo is reversed to start from bigger counts
    @Override
    public int compareTo(RawSequenceCounter other) {
        int comparisonResult = -Long.compare(count, other.count);
        // disable equal counts because they lead to objects loss
        return (comparisonResult == 0) ? 1 : comparisonResult;
    }
}
