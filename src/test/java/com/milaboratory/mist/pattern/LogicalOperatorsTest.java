package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.output_converter.MatchedGroup;
import com.milaboratory.test.TestUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;

import static com.milaboratory.mist.output_converter.GroupUtils.getGroupsFromMatch;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class LogicalOperatorsTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void logicTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("attagaca"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("gttattacca"));
        AndPattern pattern3 = new AndPattern(getTestPatternAligner(),
                new FuzzyMatchPattern(getTestPatternAligner(), new NucleotideSequenceCaseSensitive("at")),
                new FuzzyMatchPattern(getTestPatternAligner(), new NucleotideSequenceCaseSensitive("gcat")));
        MultiPattern multiPattern1 = new MultiPattern(getTestPatternAligner(), pattern1, pattern2, pattern3);
        MultiPattern multiPattern2 = new MultiPattern(getTestPatternAligner(), pattern1, pattern3);
        MultiPattern multiPattern3 = new MultiPattern(getTestPatternAligner(), pattern3, pattern2);
        MultiPattern multiPattern4 = new MultiPattern(getTestPatternAligner(), pattern1);

        MultiNSequenceWithQuality mseq1 = new MultiNSequenceWithQualityImpl(
                new NSequenceWithQuality("ACAATTAGACA"),
                new NSequenceWithQuality("GTTATTACCA"),
                new NSequenceWithQuality("AACTTGCATAT"));

        MultiNSequenceWithQuality mseq2 = new MultiNSequenceWithQualityImpl(
                new NSequenceWithQuality("AACTTGCATAT"),
                new NSequenceWithQuality("GTTATTACCA"));

        MultiNSequenceWithQuality mseq3 = createMultiNSeq("ATTAGACA");

        AndOperator andOperatorS1_1 = new AndOperator(getTestPatternAligner(), multiPattern1);
        OrOperator orOperatorS1_1 = new OrOperator(getTestPatternAligner(), multiPattern1);
        NotOperator notOperatorS1_1 = new NotOperator(getTestPatternAligner(), multiPattern1);

        assertTrue(andOperatorS1_1.match(mseq1).isFound());
        assertTrue(orOperatorS1_1.match(mseq1).isFound());
        assertFalse(notOperatorS1_1.match(mseq1).isFound());

        AndOperator andOperatorS1_2 = new AndOperator(getTestPatternAligner(), andOperatorS1_1, notOperatorS1_1);
        AndOperator andOperatorS1_3 = new AndOperator(getTestPatternAligner(), new NotOperator(getTestPatternAligner(),
                notOperatorS1_1), orOperatorS1_1);
        OrOperator orOperatorS1_2 = new OrOperator(getTestPatternAligner(), andOperatorS1_1, notOperatorS1_1);
        OrOperator orOperatorS1_3 = new OrOperator(getTestPatternAligner(), notOperatorS1_1, notOperatorS1_1,
                orOperatorS1_1, notOperatorS1_1);
        OrOperator orOperatorS1_4 = new OrOperator(getTestPatternAligner(), notOperatorS1_1,
                new NotOperator(getTestPatternAligner(), andOperatorS1_1));

        assertFalse(andOperatorS1_2.match(mseq1).isFound());
        assertTrue(andOperatorS1_3.match(mseq1).isFound());
        assertTrue(orOperatorS1_2.match(mseq1).isFound());
        assertTrue(orOperatorS1_3.match(mseq1).isFound());
        assertFalse(orOperatorS1_4.match(mseq1).isFound());

        AndOperator andOperatorS2_1 = new AndOperator(getTestPatternAligner(), multiPattern2, multiPattern3);
        OrOperator orOperatorS2_1 = new OrOperator(getTestPatternAligner(), multiPattern2, multiPattern3);
        AndOperator andOperatorS2_2 = new AndOperator(getTestPatternAligner(), new NotOperator(getTestPatternAligner(),
                multiPattern2), multiPattern3);
        OrOperator orOperatorS2_2 = new OrOperator(getTestPatternAligner(), new NotOperator(getTestPatternAligner(),
                multiPattern2), multiPattern3);

        MatchingResult andResultS2_1 = andOperatorS2_1.match(mseq2);
        MatchingResult orResultS2_1 = orOperatorS2_1.match(mseq2);
        MatchingResult andResultS2_2 = andOperatorS2_2.match(mseq2);
        MatchingResult orResultS2_2 = orOperatorS2_2.match(mseq2);

        assertFalse(andResultS2_1.isFound());
        assertTrue(orResultS2_1.isFound());
        assertTrue(andResultS2_2.isFound());
        assertTrue(orResultS2_2.isFound());

        assertTrue(new AndOperator(getTestPatternAligner(), multiPattern4).match(mseq3).isFound());
        assertTrue(new OrOperator(getTestPatternAligner(), multiPattern4).match(mseq3).isFound());
        assertFalse(new NotOperator(getTestPatternAligner(), multiPattern4).match(mseq3).isFound());
        assertFalse(new AndOperator(getTestPatternAligner(), new NotOperator(getTestPatternAligner(),
                multiPattern4)).match(mseq3).isFound());
        assertFalse(new OrOperator(getTestPatternAligner(), new NotOperator(getTestPatternAligner(),
                multiPattern4)).match(mseq3).isFound());
    }

    @Test
    public void simpleTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("attagaca"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("gttattacca"));
        AndPattern pattern3 = new AndPattern(getTestPatternAligner(),
                new FuzzyMatchPattern(getTestPatternAligner(), new NucleotideSequenceCaseSensitive("at")),
                new FuzzyMatchPattern(getTestPatternAligner(), new NucleotideSequenceCaseSensitive("gcat")));
        MultiPattern multiPattern = new MultiPattern(getTestPatternAligner(), pattern1, pattern2, pattern3);

        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQualityImpl(
                new NSequenceWithQuality("ACAATTAGACA"),
                new NSequenceWithQuality("GTTATTACCA"),
                new NSequenceWithQuality("AACTTGCATAT"));

        NotOperator notOperatorFalse = new NotOperator(getTestPatternAligner(), multiPattern);
        OrOperator orOperatorTrue = new OrOperator(getTestPatternAligner(),
                notOperatorFalse, multiPattern, notOperatorFalse);
        AndOperator andOperatorTrue = new AndOperator(getTestPatternAligner(),
                multiPattern, orOperatorTrue, multiPattern);
        AndOperator andOperatorFalse = new AndOperator(getTestPatternAligner(),
                multiPattern, andOperatorTrue, orOperatorTrue, notOperatorFalse);
        OrOperator orOperatorFalse = new OrOperator(getTestPatternAligner(),
                notOperatorFalse, notOperatorFalse, andOperatorFalse);
        NotOperator notOperatorTrue = new NotOperator(getTestPatternAligner(), orOperatorFalse);
        AndOperator andOperatorSingleFalse = new AndOperator(getTestPatternAligner(), orOperatorFalse);
        OrOperator orOperatorSingleFalse = new OrOperator(getTestPatternAligner(), andOperatorSingleFalse);

        MatchingResult notFalseResult = notOperatorFalse.match(mseq);
        MatchingResult orFalseResult = orOperatorFalse.match(mseq);
        MatchingResult andFalseResult = andOperatorFalse.match(mseq);
        MatchingResult notTrueResult = notOperatorTrue.match(mseq);
        MatchingResult orTrueResult = orOperatorTrue.match(mseq);
        MatchingResult andTrueResult = andOperatorTrue.match(mseq);
        MatchingResult andSingleFalseResult = andOperatorSingleFalse.match(mseq);
        MatchingResult orSingleFalseResult = orOperatorSingleFalse.match(mseq);

        assertNull(notFalseResult.getBestMatch());
        assertNull(orFalseResult.getBestMatch());
        assertNull(andFalseResult.getBestMatch());
        assertNotNull(notTrueResult.getBestMatch());
        assertEquals(NullMatchedRange.class, notTrueResult.getBestMatch().getMatchedRange().getClass());
        assertNotNull(orTrueResult.getBestMatch());
        assertNotNull(andTrueResult.getBestMatch());
        assertNull(andSingleFalseResult.getBestMatch());
        assertNull(orSingleFalseResult.getBestMatch());

        assertEquals(1, countMatches(notTrueResult, false));
        assertEquals(0, countMatches(notFalseResult, false));
        assertEquals(0, countMatches(orFalseResult, false));
        assertEquals(0, countMatches(andFalseResult, false));
        assertEquals(1, countMatches(andTrueResult, false));
        assertEquals(1, countMatches(orTrueResult, false));
        assertEquals(0, countMatches(andSingleFalseResult, false));
        assertEquals(0, countMatches(orSingleFalseResult, false));
        assertEquals(1, countMatches(notTrueResult, true));
        assertEquals(0, countMatches(notFalseResult, true));
        assertEquals(0, countMatches(orFalseResult, true));
        assertEquals(0, countMatches(andFalseResult, true));
        assertEquals(1, countMatches(andTrueResult, true));
        assertEquals(1, countMatches(orTrueResult, true));
        assertEquals(0, countMatches(andSingleFalseResult, true));
        assertEquals(0, countMatches(orSingleFalseResult, true));

        Match testMatch = andTrueResult.getMatches().take();
        assertEquals("GTTATTACCA", testMatch.getMatchedRange(5).getValue().getSequence().toString());
        assertEquals("GCATAT", testMatch.getMatchedRange(6).getValue().getSequence().toString());

        exception.expect(IllegalArgumentException.class);
        new NotOperator(getTestPatternAligner(), orOperatorTrue, orOperatorFalse);
    }

    @Test
    public void groupNamesTest() throws Exception {
        NucleotideSequenceCaseSensitive testSeq = new NucleotideSequenceCaseSensitive("gtggttgtgttgt");
        ArrayList<GroupEdgePosition> groups1 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("ABC", true), 1));
            add(new GroupEdgePosition(new GroupEdge("ABC", false), 3));
            add(new GroupEdgePosition(new GroupEdge("DEF", true), 6));
            add(new GroupEdgePosition(new GroupEdge("DEF", false), 7));
            add(new GroupEdgePosition(new GroupEdge("GH", true), 10));
            add(new GroupEdgePosition(new GroupEdge("GH", false), 11));
        }};

        ArrayList<GroupEdgePosition> groups2 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("XYZ", true), 1));
            add(new GroupEdgePosition(new GroupEdge("XYZ", false), 3));
            add(new GroupEdgePosition(new GroupEdge("GH", false), 10));
        }};

        ArrayList<GroupEdgePosition> groups3 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("123", true), 2));
            add(new GroupEdgePosition(new GroupEdge("123", false), 4));
            add(new GroupEdgePosition(new GroupEdge("456", true), 5));
            add(new GroupEdgePosition(new GroupEdge("456", false), 7));
        }};

        ArrayList<GroupEdgePosition> groups4 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("789", true), 0));
            add(new GroupEdgePosition(new GroupEdge("0", true), 4));
            add(new GroupEdgePosition(new GroupEdge("0", false), 5));
        }};

        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(), testSeq, groups1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(), testSeq, groups2);
        FuzzyMatchPattern pattern3 = new FuzzyMatchPattern(getTestPatternAligner(), testSeq, groups3);
        FuzzyMatchPattern pattern4 = new FuzzyMatchPattern(getTestPatternAligner(), testSeq, groups4);
        MultiPattern multiPattern1 = new MultiPattern(getTestPatternAligner(), pattern1, pattern3);
        MultiPattern multiPattern2 = new MultiPattern(getTestPatternAligner(), pattern2, pattern4);

        exception.expect(IllegalStateException.class);
        new AndOperator(getTestPatternAligner(), multiPattern1, multiPattern2);
    }

    @Test
    public void groupsInNotTest() throws Exception {
        ArrayList<GroupEdgePosition> groups = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("0", true), 0)); }};
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("a"), groups);
        MultiPattern multiPattern = new MultiPattern(getTestPatternAligner(), pattern);

        exception.expect(IllegalStateException.class);
        new NotOperator(getTestPatternAligner(), multiPattern);
    }

    @Test
    public void groupsTest() throws Exception {
        ArrayList<GroupEdgePosition> groups1 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("1", true), 0));
            add(new GroupEdgePosition(new GroupEdge("1", false), 1));
            add(new GroupEdgePosition(new GroupEdge("2", true), 1));
            add(new GroupEdgePosition(new GroupEdge("2", false), 3));
            add(new GroupEdgePosition(new GroupEdge("4", true), 4));
            add(new GroupEdgePosition(new GroupEdge("4", false), 5));
        }};

        ArrayList<GroupEdgePosition> groups2 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("3", true), 1));
            add(new GroupEdgePosition(new GroupEdge("3", false), 3));
            add(new GroupEdgePosition(new GroupEdge("5", true), 5));
            add(new GroupEdgePosition(new GroupEdge("5", false), 6));
        }};

        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("tagcc"), groups1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("cagatgca"), groups2);
        FuzzyMatchPattern pattern3 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("a"));
        MultiPattern multiPattern1 = new MultiPattern(getTestPatternAligner(), pattern1, pattern3);
        MultiPattern multiPattern2 = new MultiPattern(getTestPatternAligner(), pattern3, pattern2);
        MultiPattern multiPattern3 = new MultiPattern(getTestPatternAligner(), pattern3, pattern3);
        NotOperator notOperator = new NotOperator(getTestPatternAligner(), multiPattern3);
        OrOperator orOperator = new OrOperator(getTestPatternAligner(), notOperator, multiPattern1, notOperator);
        AndOperator andOperator = new AndOperator(getTestPatternAligner(), multiPattern2, orOperator);

        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQualityImpl(
                new NSequenceWithQuality("ACAATTAGCCA"),
                new NSequenceWithQuality("TGGCAGATGCAC"));

        MatchingResult result = andOperator.match(mseq);

        assertEquals("1", result.getBestMatch().getMatchedGroupEdge("1", false).getGroupName());
        assertEquals(6, result.getBestMatch().getMatchedGroupEdge("3", false).getPosition());
        assertEquals(9, result.getBestMatch().getMatchedGroupEdge("4", true).getPosition());
        assertTrue(result.getBestMatch().getMatchedGroupEdge("5", true).isStart());
        assertFalse(result.getBestMatch().getMatchedGroupEdge("5", false).isStart());

        assertEquals(5, getGroupsFromMatch(result.getBestMatch()).size());
        for (MatchedGroup group : getGroupsFromMatch(result.getBestMatch())) {
            switch (group.getGroupName()) {
                case "1":
                    assertEquals(new Range(5, 6), group.getRange());
                    break;
                case "2":
                    assertEquals(new Range(6, 8), group.getRange());
                    break;
                case "3":
                    assertEquals(new Range(4, 6), group.getRange());
                    break;
                case "4":
                    assertEquals(new Range(9, 10), group.getRange());
                    break;
                case "5":
                    assertEquals(new Range(8, 9), group.getRange());
                    break;
                default:
                    throw new IllegalStateException("Must not be here!");
            }
        }

        for (boolean fairSorting : new boolean[] {true, false}) {
            OutputPort<Match> matchOutputPort = result.getMatches(fairSorting);
            for (int i = 0; i < 15; i++)
                assertNotNull(matchOutputPort.take());
            assertNull(matchOutputPort.take());
            for (int i = 0; i < 16; i++)
                assertNotNull(result.getMatches().take());
        }
    }

    @Test
    public void alignmentTest() throws Exception {
        FuzzyMatchPattern fuzzyPattern = new FuzzyMatchPattern(getTestPatternAligner(2),
                new NucleotideSequenceCaseSensitive("attagaca"));

        NSequenceWithQuality[] sequences = {
                new NSequenceWithQuality("ATTAGTTA"),
                new NSequenceWithQuality("ATTAGAAG"),
                new NSequenceWithQuality("ACAGACA"),
                new NSequenceWithQuality("ATCTAGAA"),
                new NSequenceWithQuality("TACAGACATCTAGAA")
        };

        MatchingResult[] matchingResults = new MatchingResult[5];
        for (int i = 0; i < 5; i++)
            matchingResults[i] = fuzzyPattern.match(sequences[i]);

        assertEquals(new NSequenceWithQuality("ATTAGTTA"), matchingResults[0].getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ATTAGAA"), matchingResults[1].getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ACAGACA"), matchingResults[2].getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ATCTAGAA"), matchingResults[3].getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ACAGACA"), matchingResults[4].getBestMatch().getValue());

        AndPattern andPattern = new AndPattern(getTestPatternAligner(), fuzzyPattern, fuzzyPattern);
        PlusPattern plusPattern = new PlusPattern(getTestPatternAligner(), fuzzyPattern, fuzzyPattern);

        assertEquals(new NSequenceWithQuality("ACAGACATCTAGAA"),
                andPattern.match(sequences[4]).getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ACAGACATCTAGAA"),
                plusPattern.match(sequences[4]).getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ACAGACATCTAGAA"),
                andPattern.match(sequences[4]).getMatches().take().getValue());
        assertEquals(new NSequenceWithQuality("ACAGACATCTAGAA"),
                plusPattern.match(sequences[4]).getMatches().take().getValue());

        MultiPattern multiPattern = new MultiPattern(getTestPatternAligner(), fuzzyPattern, andPattern, plusPattern);
        NotOperator notOperator = new NotOperator(getTestPatternAligner(), multiPattern);
        OrOperator orOperator = new OrOperator(getTestPatternAligner(), multiPattern, notOperator, multiPattern);
        AndOperator andOperator = new AndOperator(getTestPatternAligner(), orOperator, multiPattern, orOperator);

        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQualityImpl(sequences[1], sequences[4], sequences[4]);

        MatchingResult result = andOperator.match(mseq);

        assertEquals(new NSequenceWithQuality("ATTAGAA"),
                result.getBestMatch().getMatchedRange(0).getValue());
        assertEquals(new NSequenceWithQuality("ACAGACATCTAGAA"),
                result.getBestMatch().getMatchedRange(1).getValue());
        assertEquals(new NSequenceWithQuality("ACAGACATCTAGAA"),
                result.getBestMatch().getMatchedRange(2).getValue());
        assertEquals(NullMatchedRange.class,
                result.getBestMatch().getMatchedRange(3).getClass());
        assertEquals(new NSequenceWithQuality("ATTAGAA"),
                result.getBestMatch().getMatchedRange(14).getValue());

        exception.expect(IndexOutOfBoundsException.class);
        result.getBestMatch().getMatchedRange(17);
    }

    @Test
    public void scoringRandomTest() throws Exception {
        for (int i = 0; i < 2000; i++) {
            NucleotideSequenceCaseSensitive motifs[] = new NucleotideSequenceCaseSensitive[4];
            FuzzyMatchPattern fuzzyPatterns[] = new FuzzyMatchPattern[4];
            for (int j = 0; j < 4; ++j) {
                motifs[j] = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                        1, 10);
                fuzzyPatterns[j] = new FuzzyMatchPattern(getTestPatternAligner(), motifs[j]);
            }
            MultiNSequenceWithQuality targets[] = new MultiNSequenceWithQuality[2];
            MultiPattern multiPatterns[] = new MultiPattern[2];
            for (int j = 0; j < 2; j++) {
                targets[j] = new MultiNSequenceWithQualityImpl(
                        new NSequenceWithQuality(motifs[j * 2].toString()),
                        new NSequenceWithQuality(motifs[j * 2 + 1].toString()));
                multiPatterns[j] = new MultiPattern(getTestPatternAligner(),
                        fuzzyPatterns[j * 2], fuzzyPatterns[j * 2 + 1]);
            }

            NotOperator notOperator = new NotOperator(getTestPatternAligner(), multiPatterns[0]);
            AndOperator andOperator0 = new AndOperator(getTestPatternAligner(), multiPatterns[0], multiPatterns[0]);
            AndOperator andOperator1 = new AndOperator(getTestPatternAligner(), multiPatterns[0], multiPatterns[1]);
            OrOperator orOperator0 = new OrOperator(getTestPatternAligner(), multiPatterns[0], multiPatterns[0]);
            OrOperator orOperator1 = new OrOperator(getTestPatternAligner(), multiPatterns[0], multiPatterns[1]);

            if (!multiPatterns[0].match(targets[1]).isFound())
                assertEquals(getTestPatternAligner().notResultScore(),
                        notOperator.match(targets[1]).getBestMatch().getScore());
            else
                assertNull(notOperator.match(targets[1]).getBestMatch());

            if (!(fuzzyPatterns[2].match(targets[0].get(0)).isFound()
                    && fuzzyPatterns[3].match(targets[0].get(1)).isFound()))
                assertNull(andOperator1.match(targets[0]).getBestMatch());
            if (!(fuzzyPatterns[0].match(targets[1].get(0)).isFound()
                    && fuzzyPatterns[1].match(targets[1].get(1)).isFound()))
                assertNull(andOperator1.match(targets[1]).getBestMatch());

            assertEquals(multiPatterns[0].match(targets[0]).getBestMatch().getScore() * 2,
                    andOperator0.match(targets[0]).getBestMatch().getScore());
            assertEquals(multiPatterns[0].match(targets[0]).getBestMatch().getScore(),
                    orOperator0.match(targets[0]).getBestMatch().getScore());
            assertEquals(multiPatterns[0].match(targets[0]).getBestMatch().getScore(),
                    orOperator1.match(targets[0]).getBestMatch().getScore());
            assertEquals(multiPatterns[1].match(targets[1]).getBestMatch().getScore(),
                    orOperator1.match(targets[1]).getBestMatch().getScore());
        }
    }
}
