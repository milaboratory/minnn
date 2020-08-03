/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact MiLaboratory LLC, which owns exclusive
 * rights for distribution of this program for commercial purposes, using the
 * following email address: licensing@milaboratory.com.
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.minnn.pattern;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.minnn.io.IO;
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
