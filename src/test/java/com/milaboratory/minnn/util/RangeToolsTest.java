/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
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
package com.milaboratory.minnn.util;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.minnn.pattern.MatchIntermediate;
import com.milaboratory.minnn.pattern.SinglePattern;
import com.milaboratory.test.TestUtil;
import org.junit.*;

import java.util.*;

import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.RangeTools.*;
import static org.junit.Assert.*;

public class RangeToolsTest {
    @Test
    public void checkRangesIntersectionTest() throws Exception {
        Range[] ranges1 = {new Range(1, 3), new Range(3, 5)};
        Range[] ranges2 = {new Range(2, 4), new Range(3, 5)};
        Range[] ranges3 = {new Range(10, 12), new Range(16, 18), new Range(20, 30), new Range(5, 8)};
        Range[] ranges4 = {new Range(6, 11), new Range(13, 15), new Range(9, 11)};
        Range[] ranges5 = {};
        Range[] ranges6 = {new Range(0, 1)};
        Range[] ranges7 = {new Range(0, 3), new Range(1, 4), new Range(2, 5), new Range(3, 6)};
        assertFalse(checkRangesIntersection(ranges1));
        assertTrue(checkRangesIntersection(ranges2));
        assertFalse(checkRangesIntersection(ranges3));
        assertTrue(checkRangesIntersection(ranges4));
        assertFalse(checkRangesIntersection(ranges5));
        assertFalse(checkRangesIntersection(ranges6));
        assertTrue(checkRangesIntersection(0, ranges2));
        assertFalse(checkRangesIntersection(1, ranges2));
        assertTrue(checkRangesIntersection(1, ranges4));
        assertFalse(checkRangesIntersection(2, ranges4));
        assertFalse(checkRangesIntersection(3, ranges4));
        assertTrue(checkRangesIntersection(7, ranges7));
        assertFalse(checkRangesIntersection(8, ranges7));
    }

    @Test
    public void combine2RangesTest() throws Exception {
        assertEquals(new Range(2, 13), combine2Ranges(new Range(2, 6), new Range(9, 13)));
        assertEquals(new Range(1, 3), combine2Ranges(new Range(2, 3), new Range(1, 2)));
        assertEquals(new Range(4, 11), combine2Ranges(new Range(8, 11), new Range(4, 10)));
    }

    @Test
    public void combineRangesTest() throws Exception {
        assertEquals(new Range(2, 13), combineRanges(new Range(2, 13)));
        assertEquals(new Range(3, 10), combineRanges(new Range(5, 10), new Range(8, 10), new Range(3, 6)));
        assertEquals(new Range(0, 20), combineRanges(new Range(0, 2), new Range(17, 20), new Range(10, 14),
                new Range(6, 11)));
        assertException(IllegalArgumentException.class, () -> {
            combineRanges(new Range[0]);
            return null;
        });
        assertException(IllegalArgumentException.class, () -> {
            combineRanges(new MatchIntermediate[0]);
            return null;
        });
    }

    @Test
    public void combineRangesRandomTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            SinglePattern pattern = getRandomRawSinglePattern(getRandomPatternConfiguration());
            NSequenceWithQuality target = new NSequenceWithQuality(TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    10, 100).toString());
            MatchIntermediate[] matches = streamPort(pattern.match(target).getMatches())
                    .toArray(MatchIntermediate[]::new);
            Range[] ranges = Arrays.stream(matches).map(MatchIntermediate::getRange).toArray(Range[]::new);

            if (matches.length > 0)
                assertEquals(combineRanges(matches), combineRanges(ranges));
        }
    }
}
