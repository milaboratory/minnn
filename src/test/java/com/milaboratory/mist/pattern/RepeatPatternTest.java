package com.milaboratory.mist.pattern;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.motif.BitapMatcher;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import com.milaboratory.core.sequence.SequencesUtils;
import com.milaboratory.test.TestUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static com.milaboratory.mist.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class RepeatPatternTest {
    @Test
    public void bestMatchTest() throws Exception {
        RepeatPattern pattern = new RepeatPattern(getTestPatternAligner(), new NucleotideSequenceCaseSensitive("t"),
                3, 6);
        NSequenceWithQuality nseq = new NSequenceWithQuality("TTTATTTTTGTTATTTTTTTATGTTTATGTTTTATGTTA");
        MatchingResult[] results = {
                pattern.match(nseq),
                pattern.match(nseq, 0, 33),
                pattern.match(nseq, 0, 39),
                pattern.match(nseq, 15, 31),
                pattern.match(nseq, new Range(15, 31))
        };
        for (boolean fairSorting : new boolean[] {true, false})
            for (int i = 0; i < results.length; i++) {
                Range expectedRange = (i < 3) ? new Range(13, 19) : new Range(15, 20);
                assertEquals(expectedRange.getLower(), results[i].getBestMatch(fairSorting).getRange().getLower());
                assertEquals(expectedRange.getUpper(), results[i].getBestMatch(fairSorting).getRange().getUpper());
                assertEquals((i < 3) ? "TTTTTT" : "TTTTT", bestToString(results[i], fairSorting));
                assertEquals(nseq, results[i].getBestMatch(fairSorting).getMatchedRange().getTarget());
                assertEquals(true, results[i].isFound());
                assertEquals((i < 3) ? 25 : 7, countMatches(results[i], true));
                assertEquals(1, results[i].getBestMatch(fairSorting).getNumberOfPatterns());
                assertEquals(1, results[i].getBestMatch(fairSorting).getMatchedRanges().length);
                assertEquals(0, results[i].getBestMatch(fairSorting).getMatchedGroupEdges().size());
            }
    }

    @Test
    public void noMatchesTest() throws Exception {
        RepeatPattern pattern = new RepeatPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("k"), 4, 5);
        NSequenceWithQuality nseq1 = new NSequenceWithQuality("TTTTATTATGTACA");
        NSequenceWithQuality nseq2 = new NSequenceWithQuality("ATTATTTATTAATGTATTATGCTATTATATAGACA");
        MatchingResult[] results = {
                pattern.match(nseq1, 1, 12),
                pattern.match(nseq1, new Range(3, 10)),
                pattern.match(nseq2)
        };
        for (MatchingResult result : results) {
            assertEquals(null, result.getBestMatch());
            assertEquals(null, result.getMatches().take());
            assertEquals(false, result.isFound());
            assertEquals(0, countMatches(result, true));
        }
    }

    @Test
    public void uppercaseLettersTest() throws Exception {
        PatternAligner patternAligner = getTestPatternAligner(-30, 2,
                0, -1);
        RepeatPattern pattern1 = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("a"),
                4, 4);
        RepeatPattern pattern2 = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("A"),
                4, 4);
        NSequenceWithQuality target = new NSequenceWithQuality("AATA");
        OutputPort<MatchIntermediate> matches1 = pattern1.match(target).getMatches(true);
        OutputPort<MatchIntermediate> matches2 = pattern2.match(target).getMatches(true);

        MatchIntermediate match = matches1.take();
        assertEquals(new Range(0, 4), match.getRange());
        assertEquals(-9, match.getScore());
        match = matches1.take();
        assertEquals(new Range(0, 3), match.getRange());
        assertEquals(-19, match.getScore());
        match = matches1.take();
        assertEquals(new Range(0, 2), match.getRange());
        assertEquals(-20, match.getScore());
        assertNull(matches1.take());

        match = matches2.take();
        assertEquals(new Range(0, 4), match.getRange());
        assertEquals(-9, match.getScore());
        assertNull(matches2.take());
    }

    @Test
    public void randomMatchTest() throws Exception {
        for (int i = 0; i < 10000; i++) {
            int minRepeats = rg.nextInt(10) + 1;
            int maxRepeats = rg.nextInt(100) + minRepeats;
            int targetRepeats = rg.nextInt(100) + minRepeats;
            NucleotideSequenceCaseSensitive seqM = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    1, 1);
            NucleotideSequenceCaseSensitive seqL = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    0, 40);
            NucleotideSequenceCaseSensitive seqR = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    0, 40);
            NucleotideSequenceCaseSensitive[] seqRepeats = new NucleotideSequenceCaseSensitive[targetRepeats];
            Arrays.fill(seqRepeats, seqM);
            NucleotideSequenceCaseSensitive fullSeq = SequencesUtils.concatenate(
                    seqL, SequencesUtils.concatenate(seqRepeats), seqR);
            NSequenceWithQuality target = new NSequenceWithQuality(fullSeq.toString());
            RepeatPattern pattern = new RepeatPattern(getTestPatternAligner(), seqM, minRepeats, maxRepeats);
            assertTrue(pattern.match(target).isFound());
            assertNotNull(pattern.match(target).getBestMatch(i % 50 == 0));
            assertNotNull(pattern.match(target).getMatches(i % 50 == 0).take());
        }
    }

    @Test
    public void randomTest() throws Exception {
        for (int i = 0; i < 10000; i++) {
            int minRepeats = rg.nextInt(10) + 1;
            int maxRepeats = rg.nextInt(100) + minRepeats;
            NucleotideSequence target = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    1, 1000);
            NucleotideSequenceCaseSensitive motif = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    1, 1);
            NSequenceWithQuality targetQ = new NSequenceWithQuality(target.toString());
            RepeatPattern pattern = new RepeatPattern(getTestPatternAligner(), motif, minRepeats, maxRepeats);
            boolean isMatching = target.toString().contains(repeatString(motif.toString().toUpperCase(), minRepeats));
            assertEquals(isMatching, pattern.match(targetQ).isFound());
            assertEquals(isMatching, pattern.match(targetQ).getBestMatch(i % 50 == 0) != null);
            assertEquals(isMatching, pattern.match(targetQ).getMatches(i % 50 == 0).take() != null);
        }
    }

    @Test
    public void multipleMatchesTest() throws Exception {
        RepeatPattern pattern = new RepeatPattern(getTestPatternAligner(), new NucleotideSequenceCaseSensitive("c"),
                2, 4);
        NSequenceWithQuality nseq = new NSequenceWithQuality(
                "ATCGGAATGTTGTTGTTGTTGTGTATAAAGGACCCAGAGCCCCATGTTGTAGTGTC");
        MatchingResult result = pattern.match(nseq);
        MatchIntermediate bestMatch1 = result.getBestMatch();
        MatchIntermediate firstMatch = result.getMatches(true).take();
        MatchIntermediate bestMatch2 = result.getBestMatch();
        MatchIntermediate bestMatch3 = result.getBestMatch();
        assertEquals(bestMatch1.getRange(), bestMatch2.getRange());
        assertEquals(bestMatch1.getRange(), bestMatch3.getRange());
        assertEquals(bestMatch1.getRange(), firstMatch.getRange());
        assertEquals(new Range(39, 43), firstMatch.getRange());
        assertEquals(true, result.isFound());
        assertEquals(9, countMatches(result, true));
        assertEquals(9, countMatches(result, false));
        result = pattern.match(nseq);
        OutputPort<MatchIntermediate> matches = result.getMatches(true);
        assertEquals(new Range(39, 43), matches.take().getRange());
        assertEquals(new Range(32, 35), matches.take().getMatchedRanges()[0].getRange());
        assertEquals("CCC", matches.take().getValue().getSequence().toString());
        assertEquals(new Range(40, 43), matches.take().getMatchedRange(0).getRange());
    }

    @Test
    public void groupEdgeOutsideOfMotifTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            int minRepeats = rg.nextInt(10) + 1;
            int maxRepeats = rg.nextInt(100) + minRepeats;
            int targetRepeats = rg.nextInt(100) + minRepeats;
            ArrayList<GroupEdgePosition> groups = getRandomGroupsForFuzzyMatch(100);
            NucleotideSequenceCaseSensitive motif = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    1, 1);
            RepeatPattern pattern = new RepeatPattern(getTestPatternAligner(), motif, minRepeats, maxRepeats, groups);
            Match match = pattern.match(new NSequenceWithQuality(repeatString(motif.toString(), targetRepeats)))
                    .getBestMatch();
            for (MatchedGroupEdge matchedGroupEdge : match.getMatchedGroupEdges())
                assertTrue(matchedGroupEdge.getPosition() <= motif.size() * targetRepeats);
        }
    }

    @Test
    public void masksTest() throws Exception {
        for (int i = 0; i < 10000; i++) {
            int minRepeats = rg.nextInt(10) + 1;
            int maxRepeats = rg.nextInt(100) + minRepeats;
            NucleotideSequence target = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    1, 1000);
            NucleotideSequenceCaseSensitive motif = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    1, 1, false);
            NucleotideSequence repeatedMotif = new NucleotideSequence(repeatString(motif.toString(), minRepeats));
            NSequenceWithQuality targetQ = new NSequenceWithQuality(target.toString());
            RepeatPattern pattern = new RepeatPattern(getTestPatternAligner(), motif, minRepeats, maxRepeats);
            BitapMatcher matcher = repeatedMotif.toMotif().getBitapPattern().exactMatcher(target.getSequence(),
                    0, target.size());
            boolean isMatching = (matcher.findNext() != -1);
            assertEquals(isMatching, pattern.match(targetQ).isFound());
        }
    }

    @Test
    public void scoringTest() throws Exception {
        RepeatPattern[] patterns = {
                new RepeatPattern(getTestPatternAligner(0), new NucleotideSequenceCaseSensitive("t"),
                        3, 5),
                new RepeatPattern(getTestPatternAligner(1), new NucleotideSequenceCaseSensitive("g"),
                        3, 5),
                new RepeatPattern(getTestPatternAligner(0), new NucleotideSequenceCaseSensitive("n"),
                        1, 4)
        };
        NSequenceWithQuality[] sequences = {
                new NSequenceWithQuality("TTAGACTTACCAGGAGCAGTTTGCATGCATGCAAGA"),
                new NSequenceWithQuality("AGACTTAGACCTCATGCATGCAGACTGCATGCATGCAGACA"),
                new NSequenceWithQuality("TGCATGCGATGCATGCA")
        };

        MatchingResult[][] matchingResults = new MatchingResult[3][3];
        Match previousMatch;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                matchingResults[i][j] = patterns[i].match(sequences[j]);
                previousMatch = null;
                for (Match currentMatch : CUtils.it(matchingResults[i][j].getMatches(true))) {
                    if (previousMatch != null)
                        assertTrue(currentMatch.getScore() <= previousMatch.getScore());
                    previousMatch = currentMatch;
                }
            }

        for (boolean fairSorting : new boolean[] {true, false}) {
            assertEquals(0, matchingResults[0][0].getBestMatch(fairSorting).getScore());
            assertNull(matchingResults[0][1].getBestMatch(fairSorting));
            assertNull(matchingResults[1][1].getBestMatch(fairSorting));
            assertNull(matchingResults[0][2].getBestMatch(fairSorting));
        }
        assertEquals(-9, matchingResults[1][0].getBestMatch(true).getScore());
        assertEquals(-9, matchingResults[1][2].getBestMatch(true).getScore());
        assertEquals(0, matchingResults[2][0].getBestMatch(true).getScore());
        assertEquals(0, matchingResults[2][1].getBestMatch(true).getScore());
        assertEquals(0, matchingResults[2][2].getBestMatch(true).getScore());
    }

    @Test
    public void fixedBordersTest() throws Exception {
        PatternAligner patternAligner = getTestPatternAligner(1);
        RepeatPattern pattern1 = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("t"),
                8, 12, 2, -1,
                getRandomGroupsForFuzzyMatch(7));
        RepeatPattern pattern2 = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("t"),
                8, Integer.MAX_VALUE, -1, 11,
                getRandomGroupsForFuzzyMatch(3));
        RepeatPattern pattern3 = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("t"),
                8, 16, 3, 9,
                getRandomGroupsForFuzzyMatch(9, 18));
        NSequenceWithQuality target1_1 = new NSequenceWithQuality("GTTTTTTTT");
        NSequenceWithQuality target1_2 = new NSequenceWithQuality("TTTTTTTT");
        NSequenceWithQuality target2_1 = new NSequenceWithQuality("GTTTATTTTTTTTTTA");
        NSequenceWithQuality target2_2 = new NSequenceWithQuality("TGTTCATTTTTTTTCTT");
        NSequenceWithQuality target3_1 = new NSequenceWithQuality("TTATTTTTTTTCTT");
        NSequenceWithQuality target3_2 = new NSequenceWithQuality("TATTTTTTTTGTT");
        NSequenceWithQuality target3_3 = new NSequenceWithQuality("ATTTTTTTTATT");
        for (boolean fairSorting : new boolean[] {true, false}) {
            assertEquals("TTTTTTT", bestToString(pattern1.match(target1_1), fairSorting));
            assertNull(pattern1.match(target1_2).getBestMatch(fairSorting));
            assertEquals("TTTTTTT", bestToString(pattern1.match(target1_1, 2, 9), fairSorting));
            assertNull(pattern1.match(target1_1, 2, 8).getBestMatch(fairSorting));

            assertEquals("TTTATTTTTTT", bestToString(pattern2.match(target2_1), fairSorting));
            assertNull(pattern2.match(target2_2).getBestMatch(fairSorting));
            assertEquals("ATTTTTTT", bestToString(pattern2.match(target2_1, 5, 12), fairSorting));
            assertNull(pattern2.match(target2_1, 6, 12).getBestMatch(fairSorting));

            assertEquals("TTTTTTT", bestToString(pattern3.match(target3_1), fairSorting));
            assertEquals("TTTTTTT", bestToString(pattern3.match(target3_2), fairSorting));
            assertNull(pattern3.match(target3_3).getBestMatch(fairSorting));
            assertEquals("TTTTTTT", bestToString(pattern3.match(target3_1, 3, 10), fairSorting));
            assertNull(pattern3.match(target3_1, 4, 10).getBestMatch(fairSorting));
            assertNull(pattern3.match(target3_1, 3, 9).getBestMatch(fairSorting));
            assertNull(pattern3.match(target3_1, 4, 9).getBestMatch(fairSorting));
        }
    }
}
