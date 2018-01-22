package com.milaboratory.mist.parser;

import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import com.milaboratory.mist.pattern.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collections;

import static com.milaboratory.mist.parser.ParserFormat.SIMPLIFIED;
import static com.milaboratory.mist.util.CommonTestUtils.*;
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
        MultiPattern multiPattern1 = createMultiPattern(getTestPatternAligner(), orPattern, scoreFilterPatternS,
                fuzzyMatchPattern1, andPattern);
        MultiPattern multiPattern2 = createMultiPattern(getTestPatternAligner(), scoreFilterPatternS,
                fuzzyMatchPattern2, andPattern);
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
                singlePatterns.add(getRandomSinglePattern(singlePatterns.toArray(new SinglePattern[singlePatterns.size()])));
                Collections.reverse(singlePatterns);
            }
            Parser parser = new Parser(getRandomPatternAligner());
            Pattern parseResult = parser.parseQuery(singlePatterns.get(0).toString(), SIMPLIFIED);
            assertNotNull(parseResult);
            assertEquals(singlePatterns.get(0).toString(), parseResult.toString());
            ArrayList<MultipleReadsOperator> multiPatterns = new ArrayList<>();
            multiPatterns.add(createMultiPattern(getRandomPatternAligner(), singlePatterns.get(0)));
            multiPatterns.add(getRandomMultiReadPattern());
            for (int j = 1; j < nestedMultiLevel; j++) {
                multiPatterns.add(getRandomMultiReadPattern(multiPatterns.toArray(
                        new MultipleReadsOperator[multiPatterns.size()])));
                Collections.reverse(multiPatterns);
            }
            parseResult = parser.parseQuery(multiPatterns.get(0).toString(), SIMPLIFIED);
            assertNotNull(parseResult);
            assertEquals(multiPatterns.get(0).toString(), parseResult.toString());
            multiPatterns.add(createMultiPattern(getRandomPatternAligner(), getRandomSinglePattern(
                    getRandomBasicPattern(true), getRandomBasicPattern(), singlePatterns.get(0))));
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
