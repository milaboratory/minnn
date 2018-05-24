package com.milaboratory.mist.pattern;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.io.IO;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.annotations.Serializable;

import java.util.Objects;

@Serializable(by = IO.MatchedGroupEdgeSerializer.class)
public final class MatchedGroupEdge extends MatchedItem {
    private final GroupEdge groupEdge;
    private final int position;
    private final NSequenceWithQuality valueOverride;

    public MatchedGroupEdge(NSequenceWithQuality target, byte targetId, GroupEdge groupEdge, int position) {
        this(target, targetId, 0, groupEdge, position);
    }

    public MatchedGroupEdge(NSequenceWithQuality target, byte targetId, int patternIndex, GroupEdge groupEdge,
                            int position) {
        this(target, targetId, patternIndex, groupEdge, position, null);
    }

    public MatchedGroupEdge(NSequenceWithQuality target, byte targetId, GroupEdge groupEdge,
                            NSequenceWithQuality valueOverride) {
        this(target, targetId, 0, groupEdge, -1, valueOverride);
    }

    private MatchedGroupEdge(NSequenceWithQuality target, byte targetId, int patternIndex, GroupEdge groupEdge,
                             int position, NSequenceWithQuality valueOverride) {
        super(groupEdge.isStart() ? target : null, targetId, patternIndex);
        this.groupEdge = groupEdge;
        this.position = position;
        this.valueOverride = groupEdge.isStart() ? valueOverride : null;
    }

    @Override
    public String toString() {
        return "MatchedGroupEdge{" + groupEdge + ", " + position + "}";
    }

    public GroupEdge getGroupEdge() {
        return groupEdge;
    }

    public String getGroupName() {
        return groupEdge.getGroupName();
    }

    public boolean isStart() {
        return groupEdge.isStart();
    }

    public int getPosition() {
        return position;
    }

    public NSequenceWithQuality getValueOverride() {
        return valueOverride;
    }

    public MatchedGroupEdge overridePosition(int position) {
        return new MatchedGroupEdge(target, targetId, patternIndex, groupEdge, position);
    }

    public static MatchedGroupEdge read(PrimitivI input) {
        GroupEdge groupEdge = input.readObject(GroupEdge.class);
        int position = input.readVarIntZigZag();
        NSequenceWithQuality target = input.readObject(NSequenceWithQuality.class);
        byte targetId = input.readByte();
        NSequenceWithQuality valueOverride = input.readObject(NSequenceWithQuality.class);
        // patternIndex is not serialized
        return new MatchedGroupEdge(target, targetId, 0, Objects.requireNonNull(groupEdge), position,
                valueOverride);
    }

    public static void write(PrimitivO output, MatchedGroupEdge object) {
        output.writeObject(object.getGroupEdge());
        output.writeVarIntZigZag(object.getPosition());
        output.writeObject(object.getTarget());
        output.writeByte(object.getTargetId());
        output.writeObject(object.getValueOverride());
    }
}
