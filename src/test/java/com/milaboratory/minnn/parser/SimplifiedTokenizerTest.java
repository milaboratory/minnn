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

import java.util.*;

import static com.milaboratory.minnn.parser.ParserFormat.SIMPLIFIED;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class SimplifiedTokenizerTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void twoSidesConversionTest() throws Exception {
        ArrayList<GroupEdgePosition> groups = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("ABC", true), 1));
            add(new GroupEdgePosition(new GroupEdge("ABC", false), 3));
            add(new GroupEdgePosition(new GroupEdge("DEF", true), 6));
            add(new GroupEdgePosition(new GroupEdge("DEF", false), 7));
            add(new GroupEdgePosition(new GroupEdge("GH", true), 10));
            add(new GroupEdgePosition(new GroupEdge("GH", false), 11));
        }};

        FuzzyMatchPattern fuzzyMatchPattern1 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("gtggttgtgttgt"), groups);
        FuzzyMatchPattern fuzzyMatchPattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("attg"));
        AndPattern andPattern = new AndPattern(getTestPatternAligner(), fuzzyMatchPattern2, fuzzyMatchPattern2);
        PlusPattern plusPattern = new PlusPattern(getTestPatternAligner(), andPattern, fuzzyMatchPattern2);
        OrPattern orPattern = new OrPattern(getTestPatternAligner(), plusPattern, andPattern);
        ScoreFilter scoreFilter = new ScoreFilter(-3);
        FilterPattern scoreFilterPatternS = new FilterPattern(getTestPatternAligner(), scoreFilter, plusPattern);
        MultiPattern multiPattern1 = createMultiPattern(getTestPatternAligner(), true,
                orPattern, scoreFilterPatternS, fuzzyMatchPattern1, andPattern);
        MultiPattern multiPattern2 = createMultiPattern(getTestPatternAligner(), false,
                scoreFilterPatternS, fuzzyMatchPattern2, andPattern);
        AndOperator andOperator1 = new AndOperator(getTestPatternAligner(), multiPattern1, multiPattern2);
        AndOperator andOperator2 = new AndOperator(getTestPatternAligner(), multiPattern2, multiPattern2);
        MultipleReadsFilterPattern scoreFilterPatternM = new MultipleReadsFilterPattern(getTestPatternAligner(),
                scoreFilter, andOperator2);
        NotOperator notOperator = new NotOperator(getTestPatternAligner(), scoreFilterPatternM);
        OrOperator orOperator = new OrOperator(getTestPatternAligner(), andOperator1, notOperator, scoreFilterPatternM);

        Parser parser = new Parser(getTestPatternAligner());
        Pattern parseResult = parser.parseQuery(orOperator.toString(), SIMPLIFIED);
        assertNotNull(parseResult);
        assertEquals(orOperator.toString(), parseResult.toString());
    }

    @Test
    public void randomTest() throws Exception {
        for (int i = 0; i < rg.nextInt(50) + 300; i++) {
            int nestedSingleLevel = rg.nextInt(7) + 1;
            int nestedMultiLevel = rg.nextInt(5) + 1;
            ArrayList<SinglePattern> singlePatterns = new ArrayList<>();
            for (int j = 0; j < nestedSingleLevel; j++) {
                singlePatterns.add(getRandomRawSinglePattern(singlePatterns
                        .toArray(new SinglePattern[singlePatterns.size()])));
                Collections.reverse(singlePatterns);
            }
            Parser parser = new Parser(getRandomPatternAligner());
            Pattern parseResult = parser.parseQuery(singlePatterns.get(0).toString(), SIMPLIFIED);
            assertNotNull(parseResult);
            assertEquals(singlePatterns.get(0).toString(), parseResult.toString());
            ArrayList<MultipleReadsOperator> multiPatterns = new ArrayList<>();
            multiPatterns.add(createMultiPattern(getRandomPatternAligner(), rg.nextBoolean(), singlePatterns.get(0)));
            multiPatterns.add(getRandomMultiReadPattern());
            for (int j = 1; j < nestedMultiLevel; j++) {
                multiPatterns.add(getRandomMultiReadPattern(multiPatterns.toArray(
                        new MultipleReadsOperator[multiPatterns.size()])));
                Collections.reverse(multiPatterns);
            }
            parseResult = parser.parseQuery(multiPatterns.get(0).toString(), SIMPLIFIED);
            assertNotNull(parseResult);
            assertEquals(multiPatterns.get(0).toString(), parseResult.toString());
            multiPatterns.add(createMultiPattern(getRandomPatternAligner(), rg.nextBoolean(),
                    getRandomRawSinglePattern(getRandomBasicPattern(true), getRandomBasicPattern(),
                            singlePatterns.get(0))));
            parseResult = parser.parseQuery(multiPatterns.get(multiPatterns.size() - 1).toString(), SIMPLIFIED);
            assertNotNull(parseResult);
            assertEquals(multiPatterns.get(multiPatterns.size() - 1).toString(), parseResult.toString());
        }
    }

    @Test
    public void wrongOperandClassTest() throws Exception {
        Parser parser = new Parser(getTestPatternAligner());
        exception.expect(ParserException.class);
        parser.parseQuery("AndOperator([FuzzyMatchPattern(GATCACGTCGGGCTTCGT, -1, -1, []), "
                + "FuzzyMatchPattern(GATCACGTCGGGCTTCGT, -1, -1, [])])", SIMPLIFIED);
    }
}
