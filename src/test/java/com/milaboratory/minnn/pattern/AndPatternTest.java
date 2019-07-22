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
package com.milaboratory.minnn.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.*;
import com.milaboratory.test.TestUtil;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive.fromNucleotideSequence;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.RangeTools.*;
import static org.junit.Assert.*;

public class AndPatternTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void simpleTest() throws Exception {
        PatternConfiguration patternConfiguration = getTestPatternConfiguration(true);
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("attagaca"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("ttag"));
        FuzzyMatchPattern pattern3 = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("agta"));
        FuzzyMatchPattern pattern4 = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("agtag"));
        NSequenceWithQuality nseq1 = new NSequenceWithQuality("ACTGCGATAAATTAGACAGTACGTA");
        NSequenceWithQuality nseq2 = new NSequenceWithQuality("TTAGTAGAGTATTTAGAGA");
        NSequenceWithQuality nseq3 = new NSequenceWithQuality("ATTAGACAAGTAATTAGACATTAG");
        AndPattern andPattern1 = new AndPattern(patternConfiguration, pattern1, pattern3);
        AndPattern andPattern2 = new AndPattern(patternConfiguration, pattern2, pattern3);
        AndPattern andPattern3 = new AndPattern(patternConfiguration, pattern2, pattern1, pattern3);
        AndPattern andPattern4 = new AndPattern(patternConfiguration, pattern4);
        AndPattern andPattern5 = new AndPattern(patternConfiguration);
        AndPattern andPattern6 = new AndPattern(patternConfiguration, pattern1);

        assertFalse(andPattern1.match(nseq1).isFound());
        assertFalse(andPattern1.match(nseq1, 0, 25).isFound());
        assertFalse(andPattern1.match(nseq1, new Range(0, 25)).isFound());
        assertTrue(andPattern2.match(nseq1).isFound());
        assertTrue(andPattern2.match(nseq1, 0, 25).isFound());
        assertTrue(andPattern2.match(nseq1, new Range(0, 25)).isFound());
        assertFalse(andPattern4.match(nseq3, new Range(0, 24)).isFound());
        assertTrue(andPattern3.match(nseq3, new Range(0, 24)).isFound());
        assertFalse(andPattern3.match(nseq1).isFound());
        assertFalse(andPattern6.match(nseq2).isFound());
        assertTrue(andPattern6.match(nseq1).isFound());
        assertFalse(andPattern2.match(nseq1, new Range(12, 21)).isFound());

        assertEquals(new Range(0, 17), andPattern3.match(nseq3, new Range(0, 19)).getBestMatch().getRange());
        assertEquals(new Range(11, 21), andPattern2.match(nseq1, new Range(1, 21)).getBestMatch().getRange());
        assertNull(andPattern2.match(nseq1, new Range(11, 20)).getBestMatch());

        exception.expect(IllegalArgumentException.class);
        andPattern5.match(nseq1).getBestMatch();
    }

    @Test
    public void randomMatchTest() throws Exception {
        for (int i = 0; i < 10000; i++) {
            NucleotideSequenceCaseSensitive seqLeft = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 0, 40);
            NucleotideSequenceCaseSensitive seqMotif1 = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 1, 60);
            NucleotideSequenceCaseSensitive seqMiddle = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 0, 40);
            NucleotideSequenceCaseSensitive seqMotif2 = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 1, 60);
            NucleotideSequenceCaseSensitive seqRight = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 0, 40);
            NucleotideSequenceCaseSensitive fullSeq = SequencesUtils.concatenate(
                    seqLeft, seqMotif1, seqMiddle, seqMotif2, seqRight);
            NSequenceWithQuality target = new NSequenceWithQuality(fullSeq.toString());
            FuzzyMatchPattern patternMotif1 = new FuzzyMatchPattern(getTestPatternConfiguration(), seqMotif1);
            FuzzyMatchPattern patternMotif2 = new FuzzyMatchPattern(getTestPatternConfiguration(), seqMotif2);
            AndPattern andPattern1 = new AndPattern(getTestPatternConfiguration(rg.nextBoolean()),
                    patternMotif1, patternMotif2);
            AndPattern andPattern2 = new AndPattern(getTestPatternConfiguration(rg.nextBoolean()),
                    patternMotif2, patternMotif1);
            assertTrue(andPattern1.match(target).isFound());
            assertTrue(andPattern2.match(target).isFound());
            assertNotNull(andPattern1.match(target).getBestMatch(false));
            assertNotNull(andPattern1.match(target).getMatches(false).take());
            assertNotNull(andPattern1.match(target).getBestMatch(true));
            assertNotNull(andPattern1.match(target).getMatches(true).take());

            NSequenceWithQuality foundSequence = andPattern1.match(target).getBestMatch(true).getValue();
            assertTrue(patternMotif1.match(foundSequence).isFound());
            assertTrue(patternMotif2.match(foundSequence).isFound());

            foundSequence = andPattern1.match(target).getBestMatch(false).getValue();
            assertTrue(patternMotif1.match(foundSequence).isFound());
            assertTrue(patternMotif2.match(foundSequence).isFound());
        }
    }

    @Test
    public void allMatchesTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("atta"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("gaca"));
        NSequenceWithQuality nseq = new NSequenceWithQuality("GACATTATTATTAGACAGACATTAGACATTATTAGACAGACATTAATTA");
        AndPattern andPattern1 = new AndPattern(getTestPatternConfiguration(true),
                pattern1, pattern2);
        AndPattern andPattern2 = new AndPattern(getTestPatternConfiguration(true),
                pattern1, pattern1, pattern2);
        assertNotNull(andPattern1.match(nseq).getBestMatch());
        assertNotNull(andPattern2.match(nseq).getBestMatch());
        assertEquals(44, countMatches(andPattern1.match(nseq), true));
        assertEquals(248, countMatches(andPattern2.match(nseq), true));
        OutputPort<MatchIntermediate> matchesPattern1 = andPattern1.match(nseq).getMatches(true);
        OutputPort<MatchIntermediate> matchesPattern2 = andPattern2.match(nseq).getMatches(true);
        for (int i = 0; i < 44; i++) {
            assertNotNull(matchesPattern1.take().getValue());
        }
        assertNull(matchesPattern1.take());
        for (int i = 0; i < 248; i++) {
            assertNotNull(matchesPattern2.take().getValue());
        }
        assertNull(matchesPattern2.take());
    }

    @Test
    public void matchesIntersectionTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("ata"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("tat"));
        AndPattern andPattern = new AndPattern(getTestPatternConfiguration(true), pattern1, pattern2);
        NSequenceWithQuality nseq = new NSequenceWithQuality("ATATATATTATA");
        andPattern.match(nseq).getMatches(true);
        assertEquals(10, countMatches(andPattern.match(nseq), true));
    }

    @Test
    public void quickSearchTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("ata"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("tat"));
        AndPattern andPattern = new AndPattern(getTestPatternConfiguration(true),
                pattern1, pattern2);
        NSequenceWithQuality nseq1 = new NSequenceWithQuality("ATATATATTATA");
        NSequenceWithQuality nseq2 = new NSequenceWithQuality("GCGGTGCGTATAGCG");
        MatchingResult match1 = andPattern.match(nseq1);
        MatchingResult match2 = andPattern.match(nseq2);
        assertTrue(match1.isFound());
        assertFalse(match2.isFound());
        assertEquals(10, countMatches(match1, true));
        assertEquals(0, countMatches(match2, true));
        match1 = andPattern.match(nseq1);
        match2 = andPattern.match(nseq2);
        assertTrue(match1.isFound());
        assertFalse(match2.isFound());
        assertNotNull(match1.getMatches(true).take());
        assertNotNull(match1.getMatches().take());
        assertNotNull(match2.getMatches(true));
        assertNotNull(match2.getMatches());
        assertNull(match2.getMatches().take());
        assertEquals(10, countMatches(match1, true));
        assertEquals(0, countMatches(match2, true));
    }

    @Test
    public void groupsTest() throws Exception {
        ArrayList<GroupEdgePosition> groupEdgePositions1 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("1", true), 0));
            add(new GroupEdgePosition(new GroupEdge("1", false), 1));
            add(new GroupEdgePosition(new GroupEdge("2", true), 1));
            add(new GroupEdgePosition(new GroupEdge("2", false), 3));
            add(new GroupEdgePosition(new GroupEdge("4", true), 4));
            add(new GroupEdgePosition(new GroupEdge("4", false), 5));
        }};

        ArrayList<GroupEdgePosition> groupEdgePositions2 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("3", true), 1));
            add(new GroupEdgePosition(new GroupEdge("3", false), 3));
            add(new GroupEdgePosition(new GroupEdge("5", true), 5));
            add(new GroupEdgePosition(new GroupEdge("5", false), 6));
        }};

        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("tagcc"), groupEdgePositions1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternConfiguration(),
                new NucleotideSequenceCaseSensitive("cagatgca"), groupEdgePositions2);
        AndPattern andPattern = new AndPattern(getTestPatternConfiguration(true),
                pattern1, pattern2);
        NSequenceWithQuality nseq = new NSequenceWithQuality("AAACAGATGCAGACATAGCC");
        MatchingResult result = andPattern.match(nseq);
        OutputPort<MatchIntermediate> matchOutputPort = result.getMatches(true);
        Match match = matchOutputPort.take();
        assertEquals(16, match.getMatchedGroupEdge("2", true).getPosition());
        assertEquals(9, match.getMatchedGroupEdge("5", false).getPosition());
        assertEquals("3", match.getMatchedGroupEdge("3", true).getGroupName());
        assertNull(matchOutputPort.take());
    }

    @Test
    public void maxErrorsRandomTest() throws Exception {
        for (int i = 0; i < 2000; i++) {
            int maxErrors = rg.nextInt(10);
            int targetLength = rg.nextInt(100 - maxErrors) + 1;
            NucleotideSequence target = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    targetLength, targetLength);
            NucleotideSequenceCaseSensitive motif1 = fromNucleotideSequence(TestUtil.randomSequence(
                    NucleotideSequence.ALPHABET, 1, 70), true);
            NucleotideSequenceCaseSensitive motif2 = fromNucleotideSequence(getRandomSubsequence(target),
                    true);
            NSequenceWithQuality targetQ = new NSequenceWithQuality(target.toString());
            NucleotideSequenceCaseSensitive motif1WithErrors = toLowerCase(makeRandomErrors(motif1, maxErrors));
            NucleotideSequenceCaseSensitive motif2WithErrors = toLowerCase(makeRandomErrors(motif2, maxErrors));
            PatternConfiguration fuzzyPatternConfiguration = getTestPatternConfiguration(maxErrors);
            FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(fuzzyPatternConfiguration, motif1WithErrors);
            FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(fuzzyPatternConfiguration, motif2WithErrors);
            boolean targetContainsPattern1 = target.toString().contains(motif1.toString());
            boolean isMatchingPattern1 = pattern1.match(targetQ).isFound();

            if (targetContainsPattern1) {
                assertTrue(pattern1.match(targetQ).isFound());
                assertNotNull(pattern1.match(targetQ).getBestMatch(rg.nextBoolean()));
                assertNotNull(pattern1.match(targetQ).getMatches(rg.nextBoolean()).take());
            }

            assertTrue(pattern2.match(targetQ).isFound());
            assertNotNull(pattern2.match(targetQ).getBestMatch(rg.nextBoolean()));
            assertNotNull(pattern2.match(targetQ).getMatches(rg.nextBoolean()).take());

            long errorScorePenalty = -rg.nextInt(50) - 1;
            int maxOverlap = rg.nextInt(5) - 1;
            long andScoreThreshold;
            boolean tooBigOverlap = false;
            if (isMatchingPattern1) {
                MatchIntermediate match1 = pattern1.match(targetQ).getBestMatch(true);
                MatchIntermediate match2 = pattern2.match(targetQ).getBestMatch(true);
                andScoreThreshold = match1.getScore() + match2.getScore()
                        + errorScorePenalty * getIntersectionLength(match1.getRange(), match2.getRange());
                tooBigOverlap = checkFullIntersection(match1.getRange(), match2.getRange()) || ((maxOverlap != -1)
                        && (getIntersectionLength(match1.getRange(), match2.getRange()) > maxOverlap));
            } else {
                andScoreThreshold = pattern2.match(targetQ).getBestMatch(true).getScore();
                if ((targetLength <= maxErrors) || (motif1WithErrors.size() <= maxErrors))
                    andScoreThreshold = 0;
            }

            boolean andMustBeFound = isMatchingPattern1;
            if (tooBigOverlap) {
                andScoreThreshold = Long.MIN_VALUE;
                OutputPort<MatchIntermediate> port1 = pattern1.match(targetQ).getMatches(true);
                OutputPort<MatchIntermediate> port2 = pattern2.match(targetQ).getMatches(true);
                List<Range> ranges1 = streamPort(port1).map(MatchIntermediate::getRange).collect(Collectors.toList());
                List<Range> ranges2 = streamPort(port2).map(MatchIntermediate::getRange).collect(Collectors.toList());

                andMustBeFound = false;
                OUTER:
                for (Range range1: ranges1)
                    for (Range range2: ranges2)
                        if (!(checkFullIntersection(range1, range2) || ((maxOverlap != -1)
                                && (getIntersectionLength(range1, range2) > maxOverlap)))) {
                            andMustBeFound = true;
                            break OUTER;
                        }
            }

            AndPattern andPattern = new AndPattern(getTestPatternConfiguration(andScoreThreshold, 0,
                    0, errorScorePenalty, maxOverlap), pattern1, pattern2);

            assertEquals(andMustBeFound, andPattern.match(targetQ).getBestMatch(true) != null);
            assertEquals(andMustBeFound, andPattern.match(targetQ).getMatches(true).take() != null);
        }
    }

    @Test
    public void scoringRandomTest() throws Exception {
        for (int i = 0; i < 10000; i++) {
            int overlapPenalty = -rg.nextInt(1000) - 1;
            NucleotideSequenceCaseSensitive leftPart = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 5, 50);
            NucleotideSequenceCaseSensitive middleLetter = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 1, 1);
            NucleotideSequenceCaseSensitive rightPart = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 5, 50);
            NucleotideSequenceCaseSensitive motif1 = SequencesUtils.concatenate(leftPart, middleLetter);
            NucleotideSequenceCaseSensitive motif2 = SequencesUtils.concatenate(middleLetter, rightPart);
            NucleotideSequenceCaseSensitive target = SequencesUtils.concatenate(leftPart, middleLetter, rightPart);
            NSequenceWithQuality targetQ = new NSequenceWithQuality(target.toString());

            boolean leftPartUppercaseEnd = Character.isUpperCase(leftPart.symbolAt(leftPart.size() - 1));
            boolean middlePartUppercase = Character.isUpperCase(middleLetter.symbolAt(0));
            boolean rightPartUppercaseStart = Character.isUpperCase(rightPart.symbolAt(0));

            FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternConfiguration(), motif1);
            FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternConfiguration(), motif2);
            AndPattern andPattern1 = new AndPattern(getTestPatternConfiguration(0, 0,
                    0, overlapPenalty), pattern1, pattern2);
            AndPattern andPattern2 = new AndPattern(getTestPatternConfiguration(0, 0,
                    0, overlapPenalty), pattern2, pattern1);
            AndPattern andPattern3 = new AndPattern(getTestPatternConfiguration(overlapPenalty, 0,
                    0, overlapPenalty), pattern1, pattern2);
            AndPattern andPattern4 = new AndPattern(getTestPatternConfiguration(overlapPenalty, 0,
                    0, overlapPenalty), pattern2, pattern1);

            assertNull(andPattern1.match(targetQ).getBestMatch());
            assertNull(andPattern2.match(targetQ).getBestMatch());
            if (leftPartUppercaseEnd || middlePartUppercase || rightPartUppercaseStart) {
                assertNull(andPattern3.match(targetQ).getBestMatch());
                assertNull(andPattern4.match(targetQ).getBestMatch());
            } else {
                assertEquals(pattern1.match(targetQ).getBestMatch().getScore()
                                + pattern2.match(targetQ).getBestMatch().getScore() + overlapPenalty,
                        andPattern3.match(targetQ).getBestMatch().getScore());
                assertEquals(andPattern3.match(targetQ).getBestMatch().getScore(),
                        andPattern4.match(targetQ).getBestMatch().getScore());
            }
        }
    }
}
