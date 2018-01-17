package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.util.ArrayList;

public abstract class SinglePattern extends Pattern {
    // number of read where sequence is matched; numbers start from 1
    protected byte targetId = 1;

    SinglePattern(PatternAligner patternAligner) {
        super(patternAligner);
    }

    @Override
    public MatchingResult match(MultiNSequenceWithQuality target) {
        if (target instanceof NSequenceWithQuality)
            return match((NSequenceWithQuality)target);
        else if (target.numberOfSequences() == 1)
            return match(target.get(0));
        else
            throw new IllegalArgumentException("Supports only single NSequenceWithQuality.");
    }

    /**
     * Search this pattern in target sequence
     *
     * @param target target sequence
     * @param range searching range in target sequence, non-reversed only
     * @return matching result
     */
    public MatchingResult match(NSequenceWithQuality target, Range range) {
        if (range.isReverse())
            throw new IllegalArgumentException("Doesn't support reversed ranges.");
        return match(target, range.getFrom(), range.getTo());
    }

    public MatchingResult match(NSequenceWithQuality target) {
        return match(target, 0, target.size());
    }

    /**
     * Search this pattern in target sequence.
     *
     * @param target target sequence
     * @param from starting point in target sequence (inclusive)
     * @param to ending point in target sequence (exclusive)
     * @return matching result
     */
    public abstract MatchingResult match(NSequenceWithQuality target, int from, int to);

    /**
     * Return estimated maximum length for this pattern, or if it is unavailable for this pattern, return -1.
     *
     * @return estimated maximum length for this pattern, or -1 if it is unavailable
     */
    public int estimateMaxLength() {
        return -1;
    }

    /**
     * Return estimated max overlap for this pattern, or if it is unavailable for this pattern, return -1.
     *
     * @return estimated max overlap for this pattern, or -1 if it is unavailable
     */
    public int estimateMaxOverlap() {
        return -1;
    }

    void setTargetId(byte targetId) {
        if (targetId < 1)
            throw new IllegalArgumentException("targetId must be positive; found " + targetId);
        this.targetId = targetId;
    }
}
