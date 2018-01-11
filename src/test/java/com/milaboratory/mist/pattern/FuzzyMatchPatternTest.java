package com.milaboratory.mist.pattern;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.motif.BitapMatcher;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.util.TestConfiguredPattern;
import com.milaboratory.mist.util.TestPatternAlignerConf;
import com.milaboratory.test.TestUtil;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.*;

import static com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive.fromNucleotideSequence;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class FuzzyMatchPatternTest {
    @BeforeClass
    public static void init() throws Exception {
        PatternAligner.allowValuesOverride();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void bestMatchTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(Long.MIN_VALUE,
                new NucleotideSequenceCaseSensitive("attagaca"));
        NSequenceWithQuality nseq = new NSequenceWithQuality("ACTGCGATAAATTAGACAGTACGTA");
        ArrayList<MatchingResult> results = new ArrayList<>(Arrays.asList(
                pattern.match(nseq, 1, 19),
                pattern.match(nseq, 10, 18),
                pattern.match(nseq, 10, 18),
                pattern.match(nseq, new Range(10, 18)),
                pattern.match(nseq)
        ));
        Range expectedRange = new Range(10, 18);
        for (MatchingResult result : results) {
            assertEquals(expectedRange.getLower(), result.getBestMatch().getRange().getLower());
            assertEquals(expectedRange.getUpper(), result.getBestMatch().getRange().getUpper());
            assertEquals(new NSequenceWithQuality("ATTAGACA"), result.getBestMatch().getValue());
            assertEquals(nseq, result.getBestMatch().getMatchedRange().getTarget());
            assertEquals(true, result.isFound());
            assertEquals(1, countMatches(result, true));
            assertEquals(1, result.getBestMatch().getNumberOfPatterns());
            assertEquals(1, result.getBestMatch().getMatchedRanges().length);
            assertEquals(0, result.getBestMatch().getMatchedGroupEdges().size());
        }
    }

    @Test
    public void noMatchesTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(Long.MIN_VALUE,
                new NucleotideSequenceCaseSensitive("attagaca"));
        NSequenceWithQuality nseq1 = new NSequenceWithQuality("ACTGCGATAAATTAGACAGTACGTA");
        NSequenceWithQuality nseq2 = new NSequenceWithQuality("ACTGCGATAAATTACACAGTACGTA");
        ArrayList<MatchingResult> results = new ArrayList<>(Arrays.asList(
                pattern.match(nseq1, 11, 19),
                pattern.match(nseq1, 10, 17),
                pattern.match(nseq2)
        ));
        for (MatchingResult result : results) {
            assertEquals(null, result.getBestMatch());
            assertEquals(null, result.getMatches().take());
            assertEquals(false, result.isFound());
            assertEquals(0, countMatches(result, true));
        }
    }

    @Test
    public void quickMatchTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(Long.MIN_VALUE,
                new NucleotideSequenceCaseSensitive("attagaca"));
        NSequenceWithQuality nseq = new NSequenceWithQuality("ACTGCGATAAATTAGACAGTACGTA");
        MatchingResult result = pattern.match(nseq, 1, 19);
        assertEquals(true, result.isFound());
        result = pattern.match(nseq, 1, 17);
        assertEquals(false, result.isFound());
        result = pattern.match(nseq, 11, 20);
        assertEquals(false, result.isFound());
        pattern = new FuzzyMatchPattern(Long.MIN_VALUE, new NucleotideSequenceCaseSensitive("attttaca"));
        result = pattern.match(nseq, 1, 19);
        assertEquals(false, result.isFound());
        assertEquals(0, countMatches(result, true));
    }

    @Test
    public void specialCasesTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 7, -1);
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(Long.MIN_VALUE,
                new NucleotideSequenceCaseSensitive("gcatcatgct"));
        NSequenceWithQuality target1 = new NSequenceWithQuality("TTT");
        MatchingResult result1 = pattern1.match(target1);
        assertTrue(result1.isFound());
        assertEquals(1, countMatches(result1, false));
    }

    @Test
    public void uppercaseLettersTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 1, -1);
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(-30,
                new NucleotideSequenceCaseSensitive("cgATTAcgA"));
        NSequenceWithQuality target = new NSequenceWithQuality("TCGATTTACGACTGATTACGATTATTACGATTATCGATTACGTCGATTACAGA");
        OutputPort<MatchIntermediate> matches = pattern.match(target).getMatches(true);

        MatchIntermediate match = matches.take();
        assertEquals(new Range(12, 21), match.getRange());
        assertEquals(-9, match.getScore());
        match = matches.take();
        assertEquals(new Range(34, 43), match.getRange());
        assertEquals(-9, match.getScore());
        match = matches.take();
        assertEquals(new Range(43, 53), match.getRange());
        assertEquals(-10, match.getScore());

        /* bitap finds insertion where it is not allowed, and aligner turns it into 1 mismatch and 1 insertion
           in allowed place */
        match = matches.take();
        assertEquals(new Range(27, 37), match.getRange());
        assertEquals(-19, match.getScore());

        /* bitap finds insertion where it is not allowed, and aligner turns it into 3 mismatches */
        match = matches.take();
        assertEquals(new Range(2, 11), match.getRange());
        assertEquals(-27, match.getScore());
        assertNull(matches.take());
    }

    @Test
    public void randomMatchTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        for (int i = 0; i < 30000; i++) {
            NucleotideSequenceCaseSensitive seqM = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    10, 60);
            NucleotideSequenceCaseSensitive seqL = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    0, 40);
            NucleotideSequenceCaseSensitive seqR = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    0, 40);
            NucleotideSequenceCaseSensitive fullSeq = SequencesUtils.concatenate(seqL, seqM, seqR);
            NSequenceWithQuality target = new NSequenceWithQuality(fullSeq.toString());
            FuzzyMatchPattern pattern = new FuzzyMatchPattern(Long.MIN_VALUE, seqM);
            assertTrue(pattern.match(target).isFound());
            assertNotNull(pattern.match(target).getBestMatch(rg.nextBoolean()));
            assertNotNull(pattern.match(target).getMatches(rg.nextBoolean()).take());
        }
    }

    @Test
    public void randomTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        for (int i = 0; i < 10000; i++) {
            NucleotideSequence target = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    1, 1000);
            NucleotideSequenceCaseSensitive motif = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    1, 50);
            NSequenceWithQuality targetQ = new NSequenceWithQuality(target.toString());
            FuzzyMatchPattern pattern = new FuzzyMatchPattern(Long.MIN_VALUE, motif);
            boolean isMatching = target.toString().contains(motif.toString().toUpperCase());
            assertEquals(isMatching, pattern.match(targetQ).isFound());
            assertEquals(isMatching, pattern.match(targetQ).getBestMatch(rg.nextBoolean()) != null);
            assertEquals(isMatching, pattern.match(targetQ).getMatches(rg.nextBoolean()).take() != null);
        }
    }

    @Test
    public void multipleMatchesTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(Long.MIN_VALUE,
                new NucleotideSequenceCaseSensitive("attagaca"));
        NSequenceWithQuality nseq = new NSequenceWithQuality("ACTGCGATAAATTAGACATTAGACATTAGACAGTACGTATTAGACA");
        MatchingResult result = pattern.match(nseq);
        MatchIntermediate bestMatch1 = result.getBestMatch();
        MatchIntermediate firstMatchByScore = result.getMatches(true).take();
        MatchIntermediate bestMatch2 = result.getBestMatch();
        MatchIntermediate firstMatchByCoordinate = result.getMatches(true).take();
        MatchIntermediate bestMatch3 = result.getBestMatch();
        assertEquals(bestMatch1.getRange(), bestMatch2.getRange());
        assertEquals(bestMatch1.getRange(), bestMatch3.getRange());
        assertEquals(bestMatch1.getRange(), firstMatchByScore.getRange());
        assertEquals(bestMatch1.getRange(), firstMatchByCoordinate.getRange());
        assertEquals(true, result.isFound());
        assertEquals(4, countMatches(result, true));
        result = pattern.match(nseq);
        OutputPort<MatchIntermediate> matches = result.getMatches(true);
        assertEquals(10, matches.take().getRange().getLower());
        assertEquals("ATTAGACA", matches.take().getValue().getSequence().toString());
        assertEquals(24, matches.take().getMatchedRanges()[0].getRange().getLower());
        assertEquals(46, matches.take().getMatchedRange(0).getRange().getUpper());
    }

    @Test
    public void matchesIntersectionTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(Long.MIN_VALUE, new NucleotideSequenceCaseSensitive("ata"));
        NSequenceWithQuality nseq = new NSequenceWithQuality("ATATATTAAATATATATTTAATATATAAT");
        OutputPort<MatchIntermediate> matches = pattern.match(nseq).getMatches();
        assertEquals(new Range(0, 3), matches.take().getRange());
        assertEquals(new Range(2, 5), matches.take().getRange());
        assertEquals(new Range(9, 12), matches.take().getRange());
        assertEquals(new Range(11, 14), matches.take().getRange());
        assertEquals(new Range(13, 16), matches.take().getRange());
        assertEquals(new Range(20, 23), matches.take().getRange());
        assertEquals(new Range(22, 25), matches.take().getRange());
        assertEquals(new Range(24, 27), matches.take().getRange());
        assertEquals(null, matches.take());
    }

    @Test
    public void groupsTest() throws Exception {
        ArrayList<GroupEdgePosition> groups = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("ABC", true), 1));
            add(new GroupEdgePosition(new GroupEdge("ABC", false), 3));
            add(new GroupEdgePosition(new GroupEdge("DEF", true), 6));
            add(new GroupEdgePosition(new GroupEdge("DEF", false), 7));
            add(new GroupEdgePosition(new GroupEdge("GH", true), 10));
            add(new GroupEdgePosition(new GroupEdge("GH", false), 11));
        }};

        PatternAligner.init(getTestScoring(), -1, 0, -1);
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(Long.MIN_VALUE,
                new NucleotideSequenceCaseSensitive("gtggttgtgttgt"), groups);
        NSequenceWithQuality nseq = new NSequenceWithQuality("GTGTTGTGGTTGTGTTGTTGTGGTTGTGTTGTGG");
        MatchingResult result = pattern.match(nseq);
        OutputPort<MatchIntermediate> matches = result.getMatches(false);
        assertEquals("GH", matches.take().getMatchedGroupEdges().get(5).getGroupName());
        assertEquals(15, result.getBestMatch().getMatchedGroupEdges().get(4).getPosition());
        assertEquals(26, matches.take().getMatchedGroupEdge("DEF", false).getPosition());
        assertNull(matches.take());

        exception.expect(IllegalArgumentException.class);
        new FuzzyMatchPattern(Long.MIN_VALUE, new NucleotideSequenceCaseSensitive("ggtgtgtcac"), groups);
    }

    @Test
    public void groupEdgeOutsideOfMotifTest() throws Exception {
        ArrayList<GroupEdgePosition> groups = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("ABC", true), 1));
            add(new GroupEdgePosition(new GroupEdge("ABC", false), 3));
            add(new GroupEdgePosition(new GroupEdge("DEF", true), 6));
            add(new GroupEdgePosition(new GroupEdge("DEF", false), 7));
            add(new GroupEdgePosition(new GroupEdge("GH", true), 10));
            add(new GroupEdgePosition(new GroupEdge("GH", false), 11));
        }};

        PatternAligner.init(getTestScoring(), -1, 0, -1);
        exception.expect(IllegalArgumentException.class);
        new FuzzyMatchPattern(Long.MIN_VALUE, new NucleotideSequenceCaseSensitive("tagcc"), groups);
    }

    @Test
    public void randomGroupsTest() throws Exception {
        for (int i = 0; i < 30000; i++) {
            int numErrors = rg.nextInt(4);
            PatternAligner.init(getTestScoring(), -1, numErrors, -1);
            ArrayList<GroupEdgePosition> groupEdges = new ArrayList<>();
            int numGroupEdges = rg.nextInt(40);
            int motifSize = rg.nextInt(50) + 1 + numErrors;
            for (int j = 0; j < numGroupEdges; j++)
                groupEdges.add(new GroupEdgePosition(new GroupEdge("1", rg.nextBoolean()),
                        rg.nextInt(motifSize + 1 - numErrors)));
            NucleotideSequenceCaseSensitive motif = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    motifSize, motifSize);
            NucleotideSequenceCaseSensitive mutatedMotif = makeRandomErrors(motif, numErrors);
            FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(Long.MIN_VALUE, motif, groupEdges);
            FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(Long.MIN_VALUE, mutatedMotif, groupEdges);
            NSequenceWithQuality target = new NSequenceWithQuality(motif.toString() + motif.toString());
            OutputPort<MatchIntermediate> port1 = pattern1.match(target).getMatches(false);
            OutputPort<MatchIntermediate> port2 = pattern2.match(target).getMatches(rg.nextBoolean());
            Match matches[] = new Match[4];
            matches[0] = port1.take();
            matches[1] = port1.take();
            matches[2] = port2.take();
            matches[3] = port2.take();
            int matchesArraySize;
            if (matches[3] == null) {
                matchesArraySize = 3;
                Range matchedRange = PatternAligner.align(mutatedMotif, target, target.size() - 1,
                        -1).getSequence1Range();
                assertTrue(matchedRange.getLower() == 0);
                assertTrue(matchedRange.getUpper() < target.size());
            } else
                matchesArraySize = 4;
            for (int j = 0; j < numGroupEdges; j++)
                for (int k = 0; k < matchesArraySize; k++) {
                    MatchedGroupEdge matchedGroupEdge = matches[k].getMatchedGroupEdges().get(j);
                    if (k == 0)
                        assertEquals(groupEdges.get(j).getPosition(), matchedGroupEdge.getPosition());
                    assertTrue(matchedGroupEdge.getPosition() >= 0);
                    assertTrue(matchedGroupEdge.getPosition() <= target.size());
                    assertTrue(matchedGroupEdge.getGroupName().equals("1"));
                }
        }
    }

    @Test
    public void masksTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        for (int i = 0; i < 10000; i++) {
            NucleotideSequence target = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    1, 1000);
            NucleotideSequenceCaseSensitive motif = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    1, 50, false);
            NSequenceWithQuality targetQ = new NSequenceWithQuality(target.toString());
            FuzzyMatchPattern pattern = new FuzzyMatchPattern(Long.MIN_VALUE, motif);
            BitapMatcher matcher = motif.toNucleotideSequence().toMotif().getBitapPattern()
                    .exactMatcher(target.getSequence(), 0, target.size());
            boolean isMatching = (matcher.findNext() != -1);
            assertEquals(isMatching, pattern.match(targetQ).isFound());
        }
    }

    @Test
    public void simpleAlignmentTest() throws Exception {
        TestConfiguredPattern[] patterns = {
            new TestConfiguredPattern(new TestPatternAlignerConf(Long.MIN_VALUE, getTestScoring(), -1,
                0, -1),
                new FuzzyMatchPattern(Long.MIN_VALUE, new NucleotideSequenceCaseSensitive("attagaca"))),
            new TestConfiguredPattern(new TestPatternAlignerConf(Long.MIN_VALUE, getTestScoring(), -1,
                1, -1),
                new FuzzyMatchPattern(Long.MIN_VALUE, new NucleotideSequenceCaseSensitive("attagaca"))),
            new TestConfiguredPattern(new TestPatternAlignerConf(Long.MIN_VALUE, getTestScoring(), -1,
                2, -1),
                new FuzzyMatchPattern(Long.MIN_VALUE, new NucleotideSequenceCaseSensitive("attagaca")))
        };
        NSequenceWithQuality[] sequences = {
            new NSequenceWithQuality("ATTAGTTA"),
            new NSequenceWithQuality("ATTAGAAG"),
            new NSequenceWithQuality("ATTAGGACA"),
            new NSequenceWithQuality("ACAGACA"),
            new NSequenceWithQuality("ATTTAGAA"),
            new NSequenceWithQuality("TACAGACA")
        };

        MatchingResult[][] matchingResults = new MatchingResult[3][6];
        for (int i = 0; i < 3; i++) {
            patterns[i].applyConf();
            for (int j = 0; j < 6; j++)
                matchingResults[i][j] = patterns[i].getSinglePattern().match(sequences[j]);
        }

        patterns[0].applyConf();
        for (int j = 0; j < 5; j++)
            assertNull(matchingResults[0][j].getBestMatch());

        patterns[1].applyConf();
        assertNull(matchingResults[1][0].getBestMatch());
        assertEquals(new NSequenceWithQuality("ATTAGAA"), matchingResults[1][1].getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ATTAGGACA"), matchingResults[1][2].getBestMatch().getValue());
        assertNull(matchingResults[1][3].getBestMatch());
        assertNull(matchingResults[1][4].getBestMatch());
        assertNull(matchingResults[1][5].getBestMatch());

        patterns[2].applyConf();
        assertEquals(new NSequenceWithQuality("ATTAGTTA"), matchingResults[2][0].getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ATTAGAA"), matchingResults[2][1].getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ATTAGGACA"), matchingResults[2][2].getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ACAGACA"), matchingResults[2][3].getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("TTTAGAA"), matchingResults[2][4].getBestMatch().getValue());
        assertEquals(new NSequenceWithQuality("ACAGACA"), matchingResults[2][5].getBestMatch().getValue());
    }

    @Test
    public void scoringTest() throws Exception {
        TestConfiguredPattern[] patterns = {
            new TestConfiguredPattern(new TestPatternAlignerConf(Long.MIN_VALUE, getTestScoring(), -1,
                0, -1),
                new FuzzyMatchPattern(Long.MIN_VALUE, new NucleotideSequenceCaseSensitive("attagaca"))),
            new TestConfiguredPattern(new TestPatternAlignerConf(Long.MIN_VALUE, getTestScoring(), -1,
                1, -1),
                new FuzzyMatchPattern(Long.MIN_VALUE, new NucleotideSequenceCaseSensitive("attagaca"))),
            new TestConfiguredPattern(new TestPatternAlignerConf(Long.MIN_VALUE, getTestScoring(), -1,
                0, -1),
                new FuzzyMatchPattern(Long.MIN_VALUE, new NucleotideSequenceCaseSensitive("TA")))
        };
        NSequenceWithQuality[] sequences = {
            new NSequenceWithQuality("TTAGACTTACCAGGAGCAGTTATTAGACAAGA"),
            new NSequenceWithQuality("AGACTTAGACCATAGACAGACATTAGACAGACA"),
            new NSequenceWithQuality("ATTAGGACA")
        };

        MatchingResult[][] matchingResults = new MatchingResult[3][3];
        Match previousMatch;
        for (int i = 0; i < 3; i++) {
            patterns[i].applyConf();
            for (int j = 0; j < 3; j++) {
                matchingResults[i][j] = patterns[i].getSinglePattern().match(sequences[j]);
                previousMatch = null;
                for (Match currentMatch : CUtils.it(matchingResults[i][j].getMatches(true))) {
                    if (previousMatch != null)
                        assertTrue(currentMatch.getScore() <= previousMatch.getScore());
                    previousMatch = currentMatch;
                }
            }
        }

        for (boolean fairSorting : new boolean[] {true, false}) {
            patterns[0].applyConf();
            assertEquals(0, matchingResults[0][0].getBestMatch(fairSorting).getScore());
            patterns[1].applyConf();
            assertEquals(0, matchingResults[1][0].getBestMatch(fairSorting).getScore());
            patterns[2].applyConf();
            assertEquals(0, matchingResults[2][0].getBestMatch(fairSorting).getScore());
            patterns[1].applyConf();
            assertEquals(0, matchingResults[1][1].getBestMatch(fairSorting).getScore());
            assertEquals(-10, matchingResults[1][2].getBestMatch(fairSorting).getScore());
        }
    }

    @Test
    public void fixedBordersTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 1, -1);
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(Long.MIN_VALUE,
                new NucleotideSequenceCaseSensitive("attagaca"),0, 0, 2, -1,
                getRandomGroupsForFuzzyMatch(8));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(Long.MIN_VALUE,
                new NucleotideSequenceCaseSensitive("attagaca"),0, 0, -1, 11,
                getRandomGroupsForFuzzyMatch(4));
        FuzzyMatchPattern pattern3 = new FuzzyMatchPattern(Long.MIN_VALUE,
                new NucleotideSequenceCaseSensitive("attagaca"),0, 0, 3, 9,
                getRandomGroupsForFuzzyMatch(6, 15));
        NSequenceWithQuality target1_1 = new NSequenceWithQuality("GATTAGACA");
        NSequenceWithQuality target1_2 = new NSequenceWithQuality("ATTAGACA");
        NSequenceWithQuality target2_1 = new NSequenceWithQuality("GTTCAATTAGACATTA");
        NSequenceWithQuality target2_2 = new NSequenceWithQuality("GTATCAATTAGACATTA");
        NSequenceWithQuality target3_1 = new NSequenceWithQuality("TTCATTAGACATTA");
        NSequenceWithQuality target3_2 = new NSequenceWithQuality("TCATTAGACATTA");
        NSequenceWithQuality target3_3 = new NSequenceWithQuality("TATTAGACATTA");
        for (boolean fairSorting : new boolean[] {true, false}) {
            assertEquals("TTAGACA", bestToString(pattern1.match(target1_1), fairSorting));
            assertNull(pattern1.match(target1_2).getBestMatch(fairSorting));
            assertEquals("TTAGACA", bestToString(pattern1.match(target1_1, 2, 9), fairSorting));
            assertNull(pattern1.match(target1_1, 2, 8).getBestMatch(fairSorting));

            assertEquals("AATTAGAC", bestToString(pattern2.match(target2_1), fairSorting));
            assertNull(pattern2.match(target2_2).getBestMatch(fairSorting));
            assertEquals("AATTAGAC", bestToString(pattern2.match(target2_1, 5, 12), fairSorting));
            assertNull(pattern2.match(target2_1, 6, 12).getBestMatch(fairSorting));

            assertEquals("ATTAGAC", bestToString(pattern3.match(target3_1), fairSorting));
            assertEquals("TTAGACA", bestToString(pattern3.match(target3_2), fairSorting));
            assertNull(pattern3.match(target3_3).getBestMatch(fairSorting));
            assertEquals("ATTAGAC", bestToString(pattern3.match(target3_1, 3, 10), fairSorting));
            assertNull(pattern3.match(target3_1, 4, 10).getBestMatch(fairSorting));
            assertNull(pattern3.match(target3_1, 3, 9).getBestMatch(fairSorting));
            assertNull(pattern3.match(target3_1, 4, 9).getBestMatch(fairSorting));
        }
    }

    @Test
    public void borderCutsTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        NucleotideSequenceCaseSensitive seq = new NucleotideSequenceCaseSensitive("attagaca");
        NSequenceWithQuality target = new NSequenceWithQuality("TAGAC");
        FuzzyMatchPattern[] patterns = {
                new FuzzyMatchPattern(0, seq, 2, 1, -1, -1),
                new FuzzyMatchPattern(0, seq, 3, 3, -1, -1),
                new FuzzyMatchPattern(0, seq, 2, 1, 0, -2),
                new FuzzyMatchPattern(0, seq, 3, 3, 0, -2),
                new FuzzyMatchPattern(0, seq, 1, 1, -1, -1),
                new FuzzyMatchPattern(0, seq, 0, 0, -1, -1),
                new FuzzyMatchPattern(0, seq, 1, 1, 0, -2),
                new FuzzyMatchPattern(0, seq, 0, 0, 0, -2)
        };
        for (int i = 0; i < 8; i++)
            assertEquals(i < 4, patterns[i].match(target).isFound());
    }

    @Test
    public void groupEdgeMovementTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        for (int i = 0; i < 1000; i++) {
            int length = rg.nextInt(100) + 1;
            RandomCuts randomCuts = new RandomCuts(length);
            List<GroupEdgePosition> randomGroupEdges = getRandomGroupsForFuzzyMatch(length);
            NucleotideSequenceCaseSensitive seq = fromNucleotideSequence(TestUtil.randomSequence(
                    NucleotideSequence.ALPHABET, length, length), true);
            FuzzyMatchPattern pattern = new FuzzyMatchPattern(0, seq, randomCuts.left, randomCuts.right,
                    rg.nextBoolean() ? 0 : -1, rg.nextBoolean() ? -2 : -1, randomGroupEdges);
            int targetCutLeft = (randomCuts.left == 0) ? 0 : rg.nextInt(randomCuts.left);
            int targetCutRight = (randomCuts.right == 0) ? 0 : rg.nextInt(randomCuts.right);
            NSequenceWithQuality target = new NSequenceWithQuality(seq.toString()
                    .substring(targetCutLeft, seq.size() - targetCutRight));
            MatchIntermediate bestMatch = pattern.match(target).getMatches(rg.nextBoolean()).take();
            if (bestMatch != null) {
                boolean multipleMatches = (countMatches(new FuzzyMatchPattern(0,
                        fromNucleotideSequence(bestMatch.getValue().getSequence(), true))
                        .match(new NSequenceWithQuality(seq.toString())), true) > 1);
                ArrayList<MatchedGroupEdge> matchedGroupEdges = bestMatch.getMatchedGroupEdges();
                randomGroupEdges.forEach(groupEdgePosition -> {
                    int originalPosition = groupEdgePosition.getPosition();
                    int finalPosition = matchedGroupEdges.parallelStream()
                            .filter(mge -> mge.getGroupEdge().equals(groupEdgePosition.getGroupEdge()))
                            .findFirst().orElseThrow(IllegalStateException::new).getPosition();
                    assertTrue(finalPosition >= 0);
                    assertTrue(finalPosition <= target.size());
                    if (!multipleMatches) {
                        if (originalPosition < targetCutLeft)
                            assertEquals(0, finalPosition);
                        else if (originalPosition > seq.size() - targetCutRight)
                            assertEquals(target.size(), finalPosition);
                        else
                            assertEquals(originalPosition - targetCutLeft, finalPosition);
                    }
                });
            }
        }
    }

    @Test
    public void estimateMaxOverlapTest() throws Exception {
        PatternAligner.init(getTestScoring(), -1, 0, -1);
        NucleotideSequenceCaseSensitive[] sequences = new NucleotideSequenceCaseSensitive[5];
        FuzzyMatchPattern[] patterns = new FuzzyMatchPattern[6];

        sequences[0] = new NucleotideSequenceCaseSensitive("attagaca");
        sequences[1] = new NucleotideSequenceCaseSensitive("ATTAGACA");
        sequences[2] = new NucleotideSequenceCaseSensitive("aaAaa");
        sequences[3] = new NucleotideSequenceCaseSensitive("tTtttttt");
        sequences[4] = new NucleotideSequenceCaseSensitive("nNnnnnnnnNnnn");
        patterns[0] = new FuzzyMatchPattern(Long.MIN_VALUE, sequences[0]);
        patterns[1] = new FuzzyMatchPattern(Long.MIN_VALUE, sequences[1]);
        patterns[2] = new FuzzyMatchPattern(Long.MIN_VALUE, sequences[2], 0, 2,
                -1, -1);
        patterns[3] = new FuzzyMatchPattern(Long.MIN_VALUE, sequences[3], 2, 1,
                -1, -1);
        patterns[4] = new FuzzyMatchPattern(Long.MIN_VALUE, sequences[4], 2, 3,
                -1, -1);
        patterns[5] = new FuzzyMatchPattern(Long.MIN_VALUE, sequences[4], 0, 4,
                -1, -1);

        assertEquals(7, patterns[0].estimateMaxOverlap());
        assertEquals(0, patterns[1].estimateMaxOverlap());
        assertEquals(1, patterns[2].estimateMaxOverlap());
        assertEquals(5, patterns[3].estimateMaxOverlap());
        assertEquals(6, patterns[4].estimateMaxOverlap());
        assertEquals(6, patterns[5].estimateMaxOverlap());
    }
}
