/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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
package com.milaboratory.minnn.io;

import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.*;
import com.milaboratory.primitivio.*;

public final class IO {
    private IO() {}

    public static class ParsedReadSerializer implements Serializer<ParsedRead> {
        @Override
        public void write(PrimitivO output, ParsedRead object) {
            ParsedRead.write(output, object);
        }

        @Override
        public ParsedRead read(PrimitivI input) {
            return ParsedRead.read(input);
        }

        @Override
        public boolean isReference() {
            return true;
        }

        @Override
        public boolean handlesReference() {
            return false;
        }
    }

    public static class MatchSerializer implements Serializer<Match> {
        @Override
        public void write(PrimitivO output, Match object) {
            Match.write(output, object);
        }

        @Override
        public Match read(PrimitivI input) {
            return Match.read(input);
        }

        @Override
        public boolean isReference() {
            return true;
        }

        @Override
        public boolean handlesReference() {
            return false;
        }
    }

    public static class MatchedGroupEdgeSerializer implements Serializer<MatchedGroupEdge> {
        @Override
        public void write(PrimitivO output, MatchedGroupEdge object) {
            MatchedGroupEdge.write(output, object);
        }

        @Override
        public MatchedGroupEdge read(PrimitivI input) {
            return MatchedGroupEdge.read(input);
        }

        @Override
        public boolean isReference() {
            return true;
        }

        @Override
        public boolean handlesReference() {
            return false;
        }
    }

    public static class GroupEdgeSerializer implements Serializer<GroupEdge> {
        @Override
        public void write(PrimitivO output, GroupEdge object) {
            GroupEdge.write(output, object);
        }

        @Override
        public GroupEdge read(PrimitivI input) {
            return GroupEdge.read(input);
        }

        @Override
        public boolean isReference() {
            return true;
        }

        @Override
        public boolean handlesReference() {
            return false;
        }
    }
}
