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

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import org.junit.Test;

import java.util.ArrayList;

import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class MatchTest {
    @Test
    public void matchTest() throws Exception {
        NSequenceWithQuality seq0 = new NSequenceWithQuality("AATTAAGGCAAA");
        NSequenceWithQuality seq1 = new NSequenceWithQuality("ATTAGACA");

        MatchedRange testMatchedRange1 = new MatchedRange(seq0, (byte)1, 0, new Range(0, 9));
        ArrayList<MatchedGroupEdge> testMatchedGroupEdges1 = new ArrayList<MatchedGroupEdge>() {{
            add(new MatchedGroupEdge(seq0, (byte)1, 0, new GroupEdge("0", true), 1));
            add(new MatchedGroupEdge(seq0, (byte)1, 0, new GroupEdge("0", false), 4));
        }};

        MatchedRange[] testMatchedRanges2 = new MatchedRange[] {
                new MatchedRange(seq0, (byte)1, 0, new Range(0, 9)),
                new MatchedRange(seq1, (byte)1, 1, new Range(0, 8))
        };
        ArrayList<MatchedGroupEdge> testMatchedGroupEdges2 = new ArrayList<MatchedGroupEdge>() {{
            add(new MatchedGroupEdge(seq0, (byte)1, 0, new GroupEdge("0", true), 1));
            add(new MatchedGroupEdge(seq0, (byte)1, 0, new GroupEdge("0", false), 4));
            add(new MatchedGroupEdge(seq1, (byte)1, 1, new GroupEdge("1", true), 4));
            add(new MatchedGroupEdge(seq1, (byte)1, 1, new GroupEdge("1", false), 8));
            add(new MatchedGroupEdge(seq1, (byte)1, 1, new GroupEdge("2", true), 0));
            add(new MatchedGroupEdge(seq1, (byte)1, 1, new GroupEdge("2", false), 4));
            add(new MatchedGroupEdge(seq1, (byte)1, 1, new GroupEdge("3", true), 5));
            add(new MatchedGroupEdge(seq1, (byte)1, 1, new GroupEdge("3", false), 8));
        }};

        MatchIntermediate testMatch1 = new MatchIntermediate(1, -10,
                1, 2, testMatchedGroupEdges1, testMatchedRange1);
        MatchIntermediate testMatch2 = new MatchIntermediate(2, -5,
                -1, -1, testMatchedGroupEdges2, testMatchedRanges2);

        assertEquals(1, testMatch1.getNumberOfTargets());
        assertEquals(2, testMatch2.getNumberOfTargets());
        assertEquals(-10, testMatch1.getScore());
        assertEquals(-5, testMatch2.getScore());
        assertEquals(new Range(0, 9), testMatch1.getMatchedRange(0).getRange());
        assertEquals(new Range(0, 9), testMatch2.getMatchedRange(0).getRange());
        assertEquals(new Range(0, 9), testMatch1.getMatchedRange().getRange());
        assertEquals(new Range(0, 9), testMatch1.getRange());
        assertEquals(new Range(0, 8), testMatch2.getMatchedRange(1).getRange());
        assertEquals("AATTAAGGC", testMatch1.getMatchedRange(0).getValue().getSequence().toString());
        assertEquals("AATTAAGGC", testMatch2.getMatchedRange(0).getValue().getSequence().toString());
        assertEquals("AATTAAGGC", testMatch1.getMatchedRange().getValue().getSequence().toString());
        assertEquals("AATTAAGGC", testMatch1.getValue().getSequence().toString());
        assertEquals("ATTAGACA", testMatch2.getMatchedRange(1).getValue().getSequence().toString());
        assertEquals(2, testMatch1.getMatchedGroupEdges().size());
        assertEquals(8, testMatch2.getMatchedGroupEdges().size());
        assertEquals(MatchedGroupEdge.class, testMatch1.getMatchedGroupEdges().get(1).getClass());
        assertEquals(MatchedGroupEdge.class, testMatch2.getMatchedGroupEdges().get(7).getClass());
        assertTrue(testMatch1.getMatchedGroupEdge("0", true).isStart());
        assertFalse(testMatch1.getMatchedGroupEdge("0", false).isStart());
        assertTrue(testMatch2.getMatchedGroupEdge("1", true).isStart());
        assertFalse(testMatch2.getMatchedGroupEdge("1", false).isStart());
        assertEquals("2", testMatch2.getMatchedGroupEdge("2", true).getGroupName());
        assertEquals("2", testMatch2.getMatchedGroupEdge("2", false).getGroupName());
        assertEquals(5, testMatch2.getMatchedGroupEdge("3", true).getPosition());
        assertEquals(8, testMatch2.getMatchedGroupEdge("3", false).getPosition());
        assertEquals(seq0, testMatch1.getMatchedRange().getTarget());
        assertEquals(seq1, testMatch2.getMatchedRange(1).getTarget());
        assertEquals(1, testMatch1.getMatchedRange().getTargetId());
        assertEquals(1, testMatch2.getMatchedRange(1).getTargetId());
        assertEquals(0, testMatch1.getMatchedGroupEdge("0", true).getPatternIndex());
        assertEquals(0, testMatch1.getMatchedGroupEdge("0", false).getPatternIndex());
        assertEquals(0, testMatch2.getMatchedGroupEdge("0", true).getPatternIndex());
        assertEquals(1, testMatch2.getMatchedGroupEdge("1", true).getPatternIndex());
        assertEquals(1, testMatch2.getMatchedGroupEdge("1", false).getPatternIndex());
        assertEquals(1, testMatch2.getMatchedGroupEdge("2", true).getPatternIndex());
        assertEquals(1, testMatch2.getMatchedGroupEdge("3", true).getPatternIndex());
        assertEquals(2, testMatch1.getMatchedGroupEdgesByPattern(0).size());
        assertEquals(6, testMatch2.getMatchedGroupEdgesByPattern(1).size());
        assertEquals(1, testMatch1.getLeftUppercaseDistance());
        assertEquals(2, testMatch1.getRightUppercaseDistance());
        assertEquals(-1, testMatch2.getLeftUppercaseDistance());
        assertEquals(-1, testMatch2.getRightUppercaseDistance());
        assertEquals("0", testMatch2.getMatchedGroupEdgesByPattern(0).get(0).getGroupName());

        assertException(IllegalStateException.class, () -> {
            testMatch2.getMatchedRange();
            return null;
        });
    }
}
