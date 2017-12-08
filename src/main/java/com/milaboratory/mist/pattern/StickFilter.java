package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import static com.milaboratory.mist.pattern.PatternUtils.invertCoordinate;

public final class StickFilter implements Filter {
    private final boolean left;
    private int position;

    public StickFilter(boolean left, int position) {
        this.left = left;
        this.position = position;
    }

    void fixPosition(NSequenceWithQuality target) {
        if (position < -1)
            position = target.size() - 1 - invertCoordinate(position);
    }

    @Override
    public String toString() {
        return "StickFilter(" + left + ", " + position + ")";
    }

    @Override
    public Match checkMatch(Match match) {
        if (position < 0)
            throw new IllegalStateException("Position (" + position + ") is negative on checkMatch() stage!");
        MatchedRange[] matchedRanges = match.getMatchedRanges();
        if (matchedRanges.length != 1)
            throw new IllegalArgumentException("Expected exactly 1 matched range in StickFilter; got "
                    + matchedRanges.length);
        else {
            Range range = match.getRange();
            // position is always inclusive; range.getFrom() is inclusive, range.getTo() is exclusive
            if ((left && (range.getFrom() != position)) || (!left && (range.getTo() - 1 != position)))
                return null;
            else
                return match;
        }
    }
}
