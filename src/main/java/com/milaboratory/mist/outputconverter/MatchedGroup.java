package com.milaboratory.mist.outputconverter;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.pattern.MatchedRange;

public final class MatchedGroup extends MatchedRange {
    private final String groupName;

    public MatchedGroup(String groupName, NSequenceWithQuality target, byte targetId, Range range) {
        super(target, targetId, 0, range);
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public int getPatternIndex() {
        throw new IllegalStateException("getPatternIndex() called for MatchedGroup!");
    }
}
