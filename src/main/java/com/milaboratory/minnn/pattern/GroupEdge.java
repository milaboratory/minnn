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

import com.milaboratory.minnn.io.IO;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.annotations.Serializable;

@Serializable(by = IO.GroupEdgeSerializer.class)
public final class GroupEdge implements java.io.Serializable {
    private final String groupName;
    /**
     * true if this is group start, false if this is group end
     */
    private final boolean isStart;

    public GroupEdge(String groupName, boolean isStart) {
        this.groupName = groupName;
        this.isStart = isStart;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isStart() {
        return isStart;
    }

    @Override
    public String toString() {
        return "GroupEdge('" + groupName + "', " + isStart + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GroupEdge))
            return false;

        GroupEdge that = (GroupEdge) other;

        return (this.groupName.equals(that.getGroupName())) && (this.isStart == that.isStart());
    }

    @Override
    public int hashCode() {
        int hashCode = 1;

        hashCode = hashCode * 37 + this.groupName.hashCode();
        hashCode = hashCode * 37 + (this.isStart ? 1 : 0);

        return hashCode;
    }

    public static GroupEdge read(PrimitivI input) {
        String groupName = input.readUTF();
        boolean isStart = input.readBoolean();
        return new GroupEdge(groupName, isStart);
    }

    public static void write(PrimitivO output, GroupEdge object) {
        output.writeUTF(object.getGroupName());
        output.writeBoolean(object.isStart());
    }
}
