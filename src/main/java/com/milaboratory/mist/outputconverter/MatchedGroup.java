package com.milaboratory.mist.outputconverter;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.pattern.MatchedRange;

public final class MatchedGroup extends MatchedRange {
    private final String groupName;
    private final NSequenceWithQuality valueOverride;

    public MatchedGroup(String groupName, NSequenceWithQuality target, byte targetId, Range range) {
        super(target, targetId, 0, range);
        this.groupName = groupName;
        this.valueOverride = null;
    }

    public MatchedGroup(String groupName, NSequenceWithQuality target, byte targetId,
                        NSequenceWithQuality valueOverride) {
        super(target, targetId, 0, null);
        this.groupName = groupName;
        this.valueOverride = valueOverride;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public int getPatternIndex() {
        throw new IllegalStateException("getPatternIndex() called for MatchedGroup!");
    }

    @Override
    public NSequenceWithQuality getValue() {
        if (getRange() != null)
            return super.getValue();
        else
            return valueOverride;
    }
}
