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
package com.milaboratory.minnn.parser;

import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.parser.BracketsDetector.*;
import static com.milaboratory.minnn.parser.ParserFormat.*;
import static com.milaboratory.minnn.parser.ParserUtils.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class ParserUtilsTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void getScoreThresholdsSimplifiedSyntaxTest() throws Exception {
        final String start = "FilterPattern(ScoreFilter(";
        for (int i = 0; i < rg.nextInt(500) + 500; i++) {
            int repeats = rg.nextInt(10) + 1;
            int nested = rg.nextInt(10) + 1;
            ArrayList<ArrayList<Integer>> scores = new ArrayList<>();
            StringBuilder target = new StringBuilder();
            for (int r = 0; r < repeats; r++) {
                scores.add(new ArrayList<>());
                target.append(getRandomString(rg.nextInt(100), "(){}[]\"\'\\"));
                for (int n = 0; n < nested; n++) {
                    scores.get(r).add(-rg.nextInt(100));
                    target.append(start);
                    target.append(scores.get(r).get(n));
                    target.append("), ");
                    target.append(getRandomString(rg.nextInt(100) + 10, "(){}[]\"\'\\"));
                }
                for (int n = 0; n < nested; n++) {
                    target.append(")");
                    target.append(getRandomString(rg.nextInt(100), "(){}[]\"\'\\"));
                }
            }
            ArrayList<ScoreThreshold> scoreThresholds = getScoreThresholds(target.toString(), SIMPLIFIED);
            assertEquals(repeats * nested, scoreThresholds.size());
            for (int r = 0; r < repeats; r++)
                for (int n = 0; n < nested; n++) {
                    assertEquals((int)(scores.get(r).get(n)), scoreThresholds.get(r * nested + n).threshold);
                    assertEquals(n, scoreThresholds.get(r * nested + n).nestedLevel);
                }
        }
        exception.expect(ParserException.class);
        getScoreThresholds(start, SIMPLIFIED);
    }

    @Test
    public void getScoreThresholdsNormalSyntaxTest() throws Exception {
        String query = "[][0:AT] + (GC & [-16:GC || [-9:AT]]) \\ GGG || [[-1:A][-2:[-3:T]]] \\ [-1:[-2:[[[-3:[-4:]]]]]]";
        ArrayList<ScoreThreshold> scoreThresholds = getScoreThresholds(query, NORMAL);
        assertEquals(10, scoreThresholds.size());

        assertEquals(2, scoreThresholds.get(0).start);
        assertEquals(8, scoreThresholds.get(0).end);
        assertEquals(0, scoreThresholds.get(0).threshold);
        assertEquals(0, scoreThresholds.get(0).nestedLevel);

        assertEquals(17, scoreThresholds.get(1).start);
        assertEquals(36, scoreThresholds.get(1).end);
        assertEquals(-16, scoreThresholds.get(1).threshold);
        assertEquals(0, scoreThresholds.get(1).nestedLevel);

        assertEquals(28, scoreThresholds.get(2).start);
        assertEquals(35, scoreThresholds.get(2).end);
        assertEquals(-9, scoreThresholds.get(2).threshold);
        assertEquals(1, scoreThresholds.get(2).nestedLevel);

        assertEquals(48, scoreThresholds.get(3).start);
        assertEquals(54, scoreThresholds.get(3).end);
        assertEquals(-1, scoreThresholds.get(3).threshold);
        assertEquals(0, scoreThresholds.get(3).nestedLevel);

        assertEquals(54, scoreThresholds.get(4).start);
        assertEquals(65, scoreThresholds.get(4).end);
        assertEquals(-2, scoreThresholds.get(4).threshold);
        assertEquals(0, scoreThresholds.get(4).nestedLevel);

        assertEquals(58, scoreThresholds.get(5).start);
        assertEquals(64, scoreThresholds.get(5).end);
        assertEquals(-3, scoreThresholds.get(5).threshold);
        assertEquals(1, scoreThresholds.get(5).nestedLevel);

        assertEquals(83, scoreThresholds.get(9).start);
        assertEquals(88, scoreThresholds.get(9).end);
        assertEquals(-4, scoreThresholds.get(9).threshold);
        assertEquals(3, scoreThresholds.get(9).nestedLevel);

        assertEquals(16, getScoreThresholds("[-2:A \\ T[-3:G]]", NORMAL).get(0).end);
        assertEquals(0, getScoreThresholds("[(ATTA)-5:GACA]", NORMAL).size());
    }

    @Test
    public void getTokenPositionsTest() throws Exception {
        String str1 = "$ $ \"$\"$";
        ArrayList<Integer> tokenPositions1 = getTokenPositions(str1, "$", getAllQuotes(str1));
        assertEquals(0, (int)tokenPositions1.get(0));
        assertEquals(2, (int)tokenPositions1.get(1));
        assertEquals(7, (int)tokenPositions1.get(2));
        assertEquals(3, tokenPositions1.size());
        String str2 = "\"^\"\"\"'''^'''^'\\\"'\"\"";
        ArrayList<Integer> tokenPositions2 = getTokenPositions(str2, "^", getAllQuotes(str2));
        assertEquals(12, (int)tokenPositions2.get(0));
        assertEquals(1, tokenPositions2.size());
    }

    @Test
    public void specificCharDetectorTest() throws Exception {
        ArrayList<String> strings = new ArrayList<String>() {{
            add("ACBCCADC'C3dD");
            add("12124121'3'\"3\"212122112412");
            add("()()]\"{}()%@$%^&[{}5#$%!#$'\"34][");
        }};
        List<List<QuotesPair>> quotesPairs = strings.stream().map(orNull(BracketsDetector::getAllQuotes))
                .collect(Collectors.toList());
        for (int i = 0; i < 300; i++) {
            assertTrue(isSpecificCharBeforeStopChar(strings.get(0), 10, true, true,
                    "D", "B", null));
            assertFalse(isSpecificCharBeforeStopChar(strings.get(0), 10, true, true,
                    "B", "D", null));
            assertTrue(isSpecificCharBeforeStopChar(strings.get(0), 10, true, true,
                    "B", "\\", null));
            assertTrue(isSpecificCharBeforeStopChar(strings.get(1), 3, false, true,
                    "3", "5", null));
            assertFalse(isSpecificCharBeforeStopChar(strings.get(1), 3, false, false,
                    "3", "5", quotesPairs.get(1)));
            assertTrue(isSpecificCharBeforeStopChar(strings.get(1), 7, rg.nextBoolean(), rg.nextBoolean(),
                    "34", "5\\{}[]", quotesPairs.get(1)));
            assertFalse(isSpecificCharBeforeStopChar(strings.get(2), 14, rg.nextBoolean(), false,
                    "{}", "()", quotesPairs.get(2)));
            assertTrue(isSpecificCharBeforeStopChar(strings.get(2), 14, rg.nextBoolean(), true,
                    "{}", "]\\", quotesPairs.get(2)));
        }
    }
}
