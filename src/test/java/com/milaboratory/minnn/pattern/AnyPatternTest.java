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

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.test.TestUtil;
import org.junit.*;

import java.util.ArrayList;

import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class AnyPatternTest {
    @Test
    public void randomGroupsTest() throws Exception {
        for (int i = 0; i < 30000; i++) {
            PatternConfiguration patternConfiguration = getTestPatternConfiguration(rg.nextInt(10));
            ArrayList<GroupEdge> groupEdges = new ArrayList<>();
            int numGroupEdges = rg.nextInt(40);
            int targetSize = rg.nextInt(1000) + 1;
            for (int j = 0; j < numGroupEdges; j++)
                groupEdges.add(new GroupEdge("1", rg.nextBoolean()));
            AnyPattern pattern = new AnyPattern(patternConfiguration, groupEdges);
            NucleotideSequence targetSeq = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    targetSize, targetSize);
            NSequenceWithQuality target = new NSequenceWithQuality(targetSeq.toString());
            OutputPort<MatchIntermediate> port = pattern.match(target).getMatches(rg.nextBoolean());
            Match match = port.take();
            assertNull(port.take());
            for (int j = 0; j < numGroupEdges; j++) {
                MatchedGroupEdge matchedGroupEdge = match.getMatchedGroupEdges().get(j);
                if (groupEdges.get(j).isStart())
                    assertEquals(0, matchedGroupEdge.getPosition());
                else
                    assertEquals(target.size(), matchedGroupEdge.getPosition());
                assertEquals("1", matchedGroupEdge.getGroupName());
            }
        }
    }
}
