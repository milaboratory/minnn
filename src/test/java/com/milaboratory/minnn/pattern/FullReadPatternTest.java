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
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import org.junit.*;

import java.util.*;

import static com.milaboratory.minnn.util.CommonTestUtils.*;

public class FullReadPatternTest {
    @Test
    public void groupsTest() throws Exception {
        List<GroupEdgePosition> groupEdgePositions1 = new ArrayList<>();
        groupEdgePositions1.add(new GroupEdgePosition(new GroupEdge("R1", true), 1));
        groupEdgePositions1.add(new GroupEdgePosition(new GroupEdge("R1", false), 2));
        List<GroupEdgePosition> groupEdgePositions2 = new ArrayList<>();
        groupEdgePositions2.add(new GroupEdgePosition(new GroupEdge("R2", true), 1));
        groupEdgePositions2.add(new GroupEdgePosition(new GroupEdge("R2", false), 2));
        List<GroupEdgePosition> groupEdgePositions3 = new ArrayList<>();
        groupEdgePositions3.add(new GroupEdgePosition(new GroupEdge("ABC", true), 1));
        groupEdgePositions3.add(new GroupEdgePosition(new GroupEdge("ABC", false), 2));

        NSequenceWithQuality target = new NSequenceWithQuality("AAAAA");
        FuzzyMatchPattern fuzzyMatchPattern1 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("AAAAA"), groupEdgePositions1);
        FuzzyMatchPattern fuzzyMatchPattern2 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("AAAAA"), groupEdgePositions2);
        FuzzyMatchPattern fuzzyMatchPattern3 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("AAAAA"), groupEdgePositions3);
        FullReadPattern fullReadPattern1a = new FullReadPattern(getTestPatternConfiguration(
                false, true), fuzzyMatchPattern1);
        FullReadPattern fullReadPattern1b = new FullReadPattern(getTestPatternConfiguration(), fuzzyMatchPattern1);
        FullReadPattern fullReadPattern2a = new FullReadPattern(getTestPatternConfiguration(
                false, true), fuzzyMatchPattern2);
        FullReadPattern fullReadPattern2b = new FullReadPattern(getTestPatternConfiguration(), fuzzyMatchPattern2);
        FullReadPattern fullReadPattern3a = new FullReadPattern(getTestPatternConfiguration(
                false, true), fuzzyMatchPattern3);
        FullReadPattern fullReadPattern3b = new FullReadPattern(getTestPatternConfiguration(), fuzzyMatchPattern3);
        for (FullReadPattern pattern : new FullReadPattern[] { fullReadPattern1a, fullReadPattern1b,
                fullReadPattern2a, fullReadPattern2b, fullReadPattern3a, fullReadPattern3b })
            assertException(IllegalStateException.class, () -> { pattern.getGroupEdges(); return null; });
        fullReadPattern1a = (FullReadPattern)(fullReadPattern1a.setTargetId((byte)1));
        fullReadPattern1b = (FullReadPattern)(fullReadPattern1b.setTargetId((byte)1));
        fullReadPattern2a = (FullReadPattern)(fullReadPattern2a.setTargetId((byte)1));
        fullReadPattern2b = (FullReadPattern)(fullReadPattern2b.setTargetId((byte)1));
        fullReadPattern3a = (FullReadPattern)(fullReadPattern3a.setTargetId((byte)1));
        fullReadPattern3b = (FullReadPattern)(fullReadPattern3b.setTargetId((byte)1));

        GroupEdge[] expectedGroupEdges1a = new GroupEdge[] { new GroupEdge("R1", true),
                new GroupEdge("R1", false) };
        assertUnorderedArrayEquals(expectedGroupEdges1a, fullReadPattern1a.getGroupEdges().toArray());
        List<MatchedGroupEdge> matchedGroupEdges1a = fullReadPattern1a.match(target).getBestMatch()
                .getMatchedGroupEdges();
        assertUnorderedArrayEquals(expectedGroupEdges1a, matchedGroupEdges1a.stream()
                .map(MatchedGroupEdge::getGroupEdge).toArray());

        GroupEdge[] expectedGroupEdges1b = new GroupEdge[] { new GroupEdge("R1", true),
                new GroupEdge("R1", false), new GroupEdge("R1", true),
                new GroupEdge("R1", false) };
        assertUnorderedArrayEquals(expectedGroupEdges1b, fullReadPattern1b.getGroupEdges().toArray());
        List<MatchedGroupEdge> matchedGroupEdges1b = fullReadPattern1b.match(target).getBestMatch()
                .getMatchedGroupEdges();
        assertUnorderedArrayEquals(expectedGroupEdges1b, matchedGroupEdges1b.stream()
                .map(MatchedGroupEdge::getGroupEdge).toArray());

        GroupEdge[] expectedGroupEdges2a = new GroupEdge[] { new GroupEdge("R2", true),
                new GroupEdge("R2", false) };
        assertUnorderedArrayEquals(expectedGroupEdges2a, fullReadPattern2a.getGroupEdges().toArray());
        List<MatchedGroupEdge> matchedGroupEdges2a = fullReadPattern2a.match(target).getBestMatch()
                .getMatchedGroupEdges();
        assertUnorderedArrayEquals(expectedGroupEdges2a, matchedGroupEdges2a.stream()
                .map(MatchedGroupEdge::getGroupEdge).toArray());

        GroupEdge[] expectedGroupEdges2b = new GroupEdge[] { new GroupEdge("R2", true),
                new GroupEdge("R2", false), new GroupEdge("R1", true),
                new GroupEdge("R1", false) };
        assertUnorderedArrayEquals(expectedGroupEdges2b, fullReadPattern2b.getGroupEdges().toArray());
        List<MatchedGroupEdge> matchedGroupEdges2b = fullReadPattern2b.match(target).getBestMatch()
                .getMatchedGroupEdges();
        assertUnorderedArrayEquals(expectedGroupEdges2b, matchedGroupEdges2b.stream()
                .map(MatchedGroupEdge::getGroupEdge).toArray());

        GroupEdge[] expectedGroupEdges3a = new GroupEdge[] { new GroupEdge("ABC", true),
                new GroupEdge("ABC", false) };
        assertUnorderedArrayEquals(expectedGroupEdges3a, fullReadPattern3a.getGroupEdges().toArray());
        List<MatchedGroupEdge> matchedGroupEdges3a = fullReadPattern3a.match(target).getBestMatch()
                .getMatchedGroupEdges();
        assertUnorderedArrayEquals(expectedGroupEdges3a, matchedGroupEdges3a.stream()
                .map(MatchedGroupEdge::getGroupEdge).toArray());

        GroupEdge[] expectedGroupEdges3b = new GroupEdge[] { new GroupEdge("ABC", true),
                new GroupEdge("ABC", false), new GroupEdge("R1", true),
                new GroupEdge("R1", false) };
        assertUnorderedArrayEquals(expectedGroupEdges3b, fullReadPattern3b.getGroupEdges().toArray());
        List<MatchedGroupEdge> matchedGroupEdges3b = fullReadPattern3b.match(target).getBestMatch()
                .getMatchedGroupEdges();
        assertUnorderedArrayEquals(expectedGroupEdges3b, matchedGroupEdges3b.stream()
                .map(MatchedGroupEdge::getGroupEdge).toArray());
    }
}
