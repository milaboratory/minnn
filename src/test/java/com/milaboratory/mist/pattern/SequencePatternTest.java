package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.*;
import com.milaboratory.test.TestUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive.fromNucleotideSequence;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.RangeTools.checkFullIntersection;
import static com.milaboratory.mist.util.RangeTools.getIntersectionLength;
import static org.junit.Assert.*;

public class SequencePatternTest {
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
            PatternAligner fuzzyPatternAligner = getTestPatternAligner(maxErrors);
            FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(fuzzyPatternAligner, motif1WithErrors);
            FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(fuzzyPatternAligner, motif2WithErrors);
            boolean targetContainsPattern1 = target.toString().contains(motif1.toString());
            boolean isMatchingPattern1 = pattern1.match(targetQ).isFound();

            if (targetContainsPattern1) {
                assertTrue(pattern1.match(targetQ).isFound());
                assertTrue(pattern1.match(targetQ).getBestMatch(rg.nextBoolean()) != null);
                assertTrue(pattern1.match(targetQ).getMatches(rg.nextBoolean()).take() != null);
            }

            assertTrue(pattern2.match(targetQ).isFound());
            assertTrue(pattern2.match(targetQ).getBestMatch(rg.nextBoolean()) != null);
            assertTrue(pattern2.match(targetQ).getMatches(rg.nextBoolean()).take() != null);

            long errorScorePenalty = -rg.nextInt(50) - 1;
            int maxOverlap = rg.nextInt(5) - 1;
            long penaltyThreshold;
            boolean misplacedPatterns = false;
            if (isMatchingPattern1) {
                Match match1 = pattern1.match(targetQ).getBestMatch(true);
                Match match2 = pattern2.match(targetQ).getBestMatch(true);
                penaltyThreshold = match1.getScore() + match2.getScore()
                        + errorScorePenalty * (getIntersectionLength(match1.getRange(), match2.getRange())
                        + (match2.getRange().getLower() > match1.getRange().getUpper() ?
                        match2.getRange().getLower() - match1.getRange().getUpper() : 0));
                misplacedPatterns = (match1.getRange().getLower() >= match2.getRange().getLower())
                        || checkFullIntersection(match1.getRange(), match2.getRange()) || ((maxOverlap != -1)
                        && (getIntersectionLength(match1.getRange(), match2.getRange()) > maxOverlap));
            } else {
                penaltyThreshold = pattern2.match(targetQ).getBestMatch(true).getScore();
                if ((targetLength <= maxErrors) || (motif1WithErrors.size() <= maxErrors))
                    penaltyThreshold = 0;
            }

            boolean entirePatternMustMatch = isMatchingPattern1;
            if (misplacedPatterns) {
                penaltyThreshold = Long.MIN_VALUE;
                OutputPort<Match> port1 = pattern1.match(targetQ).getMatches(true);
                OutputPort<Match> port2 = pattern2.match(targetQ).getMatches(true);
                List<Range> ranges1 = streamPort(port1).map(Match::getRange).collect(Collectors.toList());
                List<Range> ranges2 = streamPort(port2).map(Match::getRange).collect(Collectors.toList());

                entirePatternMustMatch = false;
                OUTER:
                for (Range range1: ranges1)
                    for (Range range2: ranges2)
                        if (!((range1.getLower() >= range2.getLower())
                                || checkFullIntersection(range1, range2) || ((maxOverlap != -1)
                                && (getIntersectionLength(range1, range2) > maxOverlap)))) {
                            entirePatternMustMatch = true;
                            break OUTER;
                        }
            }

            SequencePattern sequencePattern = new SequencePattern(getTestPatternAligner(penaltyThreshold,
                    0, 0, errorScorePenalty, true, maxOverlap),
                    pattern1, pattern2);

            assertEquals(entirePatternMustMatch, sequencePattern.match(targetQ)
                    .getBestMatch(true) != null);
            assertEquals(entirePatternMustMatch, sequencePattern.match(targetQ)
                    .getMatches(true).take() != null);
        }
    }

    @Test
    public void scoringRandomTest() throws Exception {
        for (int i = 0; i < 10000; i++) {
            int overlapPenalty = -rg.nextInt(1000) - 1;
            int middleInsertionSize = rg.nextInt(30) + 1;
            NucleotideSequenceCaseSensitive leftPart = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 5, 50);
            NucleotideSequenceCaseSensitive middleLetter = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 1, 1);
            NucleotideSequenceCaseSensitive rightPart = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 5, 50);
            NucleotideSequenceCaseSensitive motif1 = SequencesUtils.concatenate(leftPart, middleLetter);
            NucleotideSequenceCaseSensitive motif2 = SequencesUtils.concatenate(middleLetter, rightPart);
            NucleotideSequenceCaseSensitive target1 = SequencesUtils.concatenate(leftPart, middleLetter, rightPart);
            NucleotideSequenceCaseSensitive middleInsertion = fromNucleotideSequence(TestUtil.randomSequence(
                    NucleotideSequence.ALPHABET, middleInsertionSize, middleInsertionSize), true);
            NucleotideSequenceCaseSensitive target2 = SequencesUtils.concatenate(leftPart, middleInsertion, rightPart);

            NSequenceWithQuality targetQ1 = new NSequenceWithQuality(target1.toString());
            NSequenceWithQuality targetQ2 = new NSequenceWithQuality(target2.toString());

            boolean leftPartUppercaseEnd = Character.isUpperCase(leftPart.symbolAt(leftPart.size() - 1));
            boolean middleLetterUppercase = Character.isUpperCase(middleLetter.symbolAt(0));
            boolean rightPartUppercaseStart = Character.isUpperCase(rightPart.symbolAt(0));

            FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(), motif1);
            FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(), motif2);
            FuzzyMatchPattern pattern3 = new FuzzyMatchPattern(getTestPatternAligner(), leftPart);
            FuzzyMatchPattern pattern4 = new FuzzyMatchPattern(getTestPatternAligner(), rightPart);
            SequencePattern sequencePattern1 = new SequencePattern(getTestPatternAligner(0,
                    0, 0, overlapPenalty), pattern1, pattern2);
            SequencePattern sequencePattern2 = new SequencePattern(getTestPatternAligner(0,
                    0, 0, overlapPenalty), pattern2, pattern1);
            SequencePattern sequencePattern3 = new SequencePattern(getTestPatternAligner(overlapPenalty,
                    0, 0, overlapPenalty), pattern1, pattern2);
            SequencePattern sequencePattern4 = new SequencePattern(getTestPatternAligner(overlapPenalty,
                    0, 0, overlapPenalty), pattern2, pattern1);
            SequencePattern sequencePattern5 = new SequencePattern(getTestPatternAligner(0,
                    0, 0, overlapPenalty), pattern3, pattern4);
            SequencePattern sequencePattern6 = new SequencePattern(getTestPatternAligner(
                    overlapPenalty * middleInsertionSize,
                    0, 0, overlapPenalty), pattern3, pattern4);

            assertNull(sequencePattern1.match(targetQ1).getBestMatch(true));
            assertNull(sequencePattern2.match(targetQ1).getBestMatch(true));
            if (leftPartUppercaseEnd || middleLetterUppercase || rightPartUppercaseStart) {
                assertNull(sequencePattern3.match(targetQ1).getBestMatch());
                assertNull(sequencePattern4.match(targetQ1).getBestMatch());
            } else
                assertEquals(pattern1.match(targetQ1).getBestMatch(true).getScore()
                                + pattern2.match(targetQ1).getBestMatch(true).getScore() + overlapPenalty,
                        sequencePattern3.match(targetQ1).getBestMatch(true).getScore());
            if ((pattern4.match(targetQ1).getBestMatch(true).getRange().getLower() == leftPart.size() + 1)
                    && (countMatches(pattern3.match(targetQ1), true) == 1))
                assertNull(sequencePattern5.match(targetQ1).getBestMatch(true));
            if ((pattern4.match(targetQ2).getBestMatch(true).getRange().getLower()
                    == leftPart.size() + middleInsertionSize)
                    && (countMatches(pattern3.match(targetQ2), true) == 1)) {
                assertNull(sequencePattern5.match(targetQ2).getBestMatch(true));
                if (leftPartUppercaseEnd || rightPartUppercaseStart)
                    assertNull(sequencePattern6.match(targetQ2).getBestMatch());
                else
                    assertEquals(pattern3.match(targetQ2).getBestMatch(true).getScore()
                                    + pattern4.match(targetQ2).getBestMatch(true).getScore()
                                    + overlapPenalty * middleInsertionSize,
                            sequencePattern6.match(targetQ2).getBestMatch(true).getScore());
            }
            if (!leftPart.toString().equals(rightPart.toString()))
                assertNull(sequencePattern4.match(targetQ1).getBestMatch(true));
        }
    }

    @Test
    public void groupsInOverlapsTest() throws Exception {
        ArrayList<GroupEdgePosition> groupsEdgePositions1 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("A", true), 5));
            add(new GroupEdgePosition(new GroupEdge("B", true), 2));
            add(new GroupEdgePosition(new GroupEdge("C", true), 1));
            add(new GroupEdgePosition(new GroupEdge("C", false), 5));
        }};

        ArrayList<GroupEdgePosition> groupsEdgePositions2 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("A", false), 0));
            add(new GroupEdgePosition(new GroupEdge("B", false), 4));
            add(new GroupEdgePosition(new GroupEdge("D", true), 1));
            add(new GroupEdgePosition(new GroupEdge("D", false), 5));
        }};

        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("ataga"), groupsEdgePositions1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("gattc"), groupsEdgePositions2);
        SequencePattern sequencePattern = new SequencePattern(getTestPatternAligner(-10, 0,
                0, -5, true, 2, -1, getTestScoring()),
                pattern1, pattern2);
        NSequenceWithQuality nseq = new NSequenceWithQuality("ATAGATTC");
        MatchingResult result = sequencePattern.match(nseq);
        OutputPort<Match> matchOutputPort = result.getMatches(true);
        Match match = matchOutputPort.take();
        assertEquals(5, match.getMatchedGroupEdge("A", true).getPosition());
        assertEquals(5, match.getMatchedGroupEdge("A", false).getPosition());
        assertEquals(2, match.getMatchedGroupEdge("B", true).getPosition());
        assertEquals(7, match.getMatchedGroupEdge("B", false).getPosition());
        assertEquals(1, match.getMatchedGroupEdge("C", true).getPosition());
        assertEquals(5, match.getMatchedGroupEdge("C", false).getPosition());
        assertEquals(5, match.getMatchedGroupEdge("D", true).getPosition());
        assertEquals(8, match.getMatchedGroupEdge("D", false).getPosition());
    }
}
