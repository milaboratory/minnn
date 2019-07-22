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
package com.milaboratory.minnn.parser;

import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import com.milaboratory.minnn.pattern.*;
import org.junit.*;
import org.junit.rules.ExpectedException;

import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class TokenizedStringTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void simpleTest1() throws Exception {
        TokenizedString ts1 = new TokenizedString("AATTAA");
        FuzzyMatchPattern testPattern1 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("aa"));
        FuzzyMatchPattern testPattern2 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("tt"));
        assertEquals("AATTAA", ts1.getOneString());
        ts1.tokenizeSubstring(testPattern1, 0, 2);
        assertEquals("TTAA", ts1.getOneString(2, 6));
        assertEquals(2, ts1.getTokens(0, 6).size());
        ts1.tokenizeSubstring(testPattern2, 2, 4);
        assertEquals("A", ts1.getTokens(1, 5).get(1).getString());
        ts1.tokenizeSubstring(testPattern1, 0, 6);
        assertNotNull(ts1.getFinalPattern());
        TokenizedString ts2 = new TokenizedString("ATTAGACA");
        ts2.tokenizeSubstring(testPattern2, 1, 3);
        assertEquals(3, ts2.calculateLength(0, 2));
        assertEquals("AGACA", ts2.getTokens(0, 8).get(2).getString());
        exception.expect(IllegalArgumentException.class);
        ts2.tokenizeSubstring(testPattern1, 1, 2);
    }

    @Test
    public void simpleTest2() throws Exception {
        FuzzyMatchPattern fuzzyMatchPattern = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("atttgtg"));
        AndPattern andPattern = new AndPattern(getTestPatternConfiguration(), fuzzyMatchPattern, fuzzyMatchPattern);
        ScoreFilter scoreFilter = new ScoreFilter(-1);
        FilterPattern filterPattern = new FilterPattern(getTestPatternConfiguration(), scoreFilter, fuzzyMatchPattern);
        MultiPattern multiPattern = createMultiPattern(getTestPatternConfiguration(), andPattern, filterPattern);
        TokenizedString ts = new TokenizedString(multiPattern.toString());
        ts.tokenizeSubstring(fuzzyMatchPattern, 5, 10);
        assertEquals(3, ts.getTokens(1, 20).size());
        assertEquals("Multi", ts.getTokens(0, 20).get(0).getString());
        assertEquals("lti", ts.getTokens(2, 30).get(0).getString());
        exception.expect(IllegalArgumentException.class);
        ts.tokenizeSubstring(andPattern, 7, 12);
    }
}
