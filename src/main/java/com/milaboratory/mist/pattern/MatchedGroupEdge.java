package com.milaboratory.mist.pattern;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.io.IO;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.annotations.Serializable;

@Serializable(by = IO.MatchedGroupEdgeSerializer.class)
public final class MatchedGroupEdge extends MatchedItem {
    private final GroupEdge groupEdge;
    private final int position;

    public MatchedGroupEdge(NSequenceWithQuality target, byte targetId, GroupEdge groupEdge, int position) {
        this(target, targetId, 0, groupEdge, position);
    }

    public MatchedGroupEdge(NSequenceWithQuality target, byte targetId, int patternIndex, GroupEdge groupEdge,
                            int position) {
        super(target, targetId, patternIndex);
        this.groupEdge = groupEdge;
        this.position = position;
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

    public MatchedGroupEdge overridePosition(int position) {
        return new MatchedGroupEdge(target, targetId, patternIndex, groupEdge, position);
    }

    public static MatchedGroupEdge read(PrimitivI input) {
        GroupEdge groupEdge = input.readObject(GroupEdge.class);
        int position = input.readInt();
        NSequenceWithQuality target = input.readObject(NSequenceWithQuality.class);
        byte targetId = input.readByte();
        // patternIndex is not serialized
        return new MatchedGroupEdge(target, targetId, groupEdge, position);
    }

    public static void write(PrimitivO output, MatchedGroupEdge object) {
        output.writeObject(object.getGroupEdge());
        output.writeInt(object.getPosition());
        output.writeObject(object.getTarget());
        output.writeByte(object.getTargetId());
    }
}
