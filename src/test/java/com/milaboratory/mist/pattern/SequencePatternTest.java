package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.*;
import com.milaboratory.test.TestUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;

import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.RangeTools.checkFullIntersection;
import static com.milaboratory.mist.util.RangeTools.getIntersectionLength;
import static org.junit.Assert.*;

public class SequencePatternTest {
    @Test
    public void maxErrorsRandomTest() throws Exception {
        int its = TestUtil.its(1000, 2000);
        Random randomGenerator = new Random();
        for (int i = 0; i < its; ++i) {
            int maxErrors = randomGenerator.nextInt(10);
            int targetLength = randomGenerator.nextInt(63 - maxErrors) + 1;
            NucleotideSequence target = TestUtil.randomSequence(NucleotideSequence.ALPHABET, targetLength, targetLength);
            NucleotideSequence motif1 = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 50);
            NucleotideSequence motif2 = getRandomSubsequence(target);
            NSequenceWithQuality targetQ = new NSequenceWithQuality(target,
                    SequenceQuality.getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, target.getSequence().size()));
            NucleotideSequence motif1WithErrors = makeRandomErrors(motif1, maxErrors);
            NucleotideSequence motif2WithErrors = makeRandomErrors(motif2, maxErrors);
            PatternAligner fuzzyPatternAligner = getTestPatternAligner(maxErrors);
            FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(fuzzyPatternAligner, motif1WithErrors);
            FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(fuzzyPatternAligner, motif2WithErrors);
            boolean targetContainsPattern1 = target.toString().contains(motif1.toString());
            boolean isMatchingPattern1 = pattern1.match(targetQ).isFound();

            if (targetContainsPattern1) {
                assertTrue(pattern1.match(targetQ).isFound());
                assertTrue(pattern1.match(targetQ).getBestMatch(randomGenerator.nextBoolean()) != null);
                assertTrue(pattern1.match(targetQ).getMatches(randomGenerator.nextBoolean(),
                        randomGenerator.nextBoolean()).take() != null);
            }

            assertTrue(pattern2.match(targetQ).isFound());
            assertTrue(pattern2.match(targetQ).getBestMatch(randomGenerator.nextBoolean()) != null);
            assertTrue(pattern2.match(targetQ).getMatches(randomGenerator.nextBoolean(),
                    randomGenerator.nextBoolean()).take() != null);

            for (Boolean fairSorting: new Boolean[] {false, true}) {
                long errorScorePenalty = -randomGenerator.nextInt(50) - 1;
                int maxOverlap = randomGenerator.nextInt(5) - 1;
                long penaltyThreshold;
                boolean misplacedPatterns = false;
                if (isMatchingPattern1) {
                    Match match1 = pattern1.match(targetQ).getBestMatch(fairSorting);
                    Match match2 = pattern2.match(targetQ).getBestMatch(fairSorting);
                    penaltyThreshold = match1.getScore() + match2.getScore()
                            + errorScorePenalty * (getIntersectionLength(match1.getRange(), match2.getRange())
                            + (match2.getRange().getLower() > match1.getRange().getUpper() ?
                            match2.getRange().getLower() - match1.getRange().getUpper() : 0));
                    misplacedPatterns = (match1.getRange().getLower() >= match2.getRange().getLower())
                            || checkFullIntersection(match1.getRange(), match2.getRange()) || ((maxOverlap != -1)
                            && (getIntersectionLength(match1.getRange(), match2.getRange()) > maxOverlap));
                } else {
                    penaltyThreshold = pattern2.match(targetQ).getBestMatch(fairSorting).getScore();
                    if ((targetLength <= maxErrors) || (motif1WithErrors.size() <= maxErrors))
                        penaltyThreshold = 0;
                }

                boolean entirePatternMustMatch = isMatchingPattern1;
                if (misplacedPatterns) {
                    penaltyThreshold = Long.MIN_VALUE;
                    ArrayList<Range> ranges1 = new ArrayList<>();
                    ArrayList<Range> ranges2 = new ArrayList<>();
                    OutputPort<Match> port1 = pattern1.match(targetQ).getMatches(randomGenerator.nextBoolean(), fairSorting);
                    OutputPort<Match> port2 = pattern2.match(targetQ).getMatches(randomGenerator.nextBoolean(), fairSorting);
                    Match match;
                    while ((match = port1.take()) != null)
                        ranges1.add(match.getRange());
                    while ((match = port2.take()) != null)
                        ranges2.add(match.getRange());

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

                SequencePattern sequencePattern = new SequencePattern(getTestPatternAligner(penaltyThreshold, 0,
                        0, errorScorePenalty, true, maxOverlap), pattern1, pattern2);

                if (!fairSorting)
                    assertEquals(entirePatternMustMatch, sequencePattern.match(targetQ).isFound());
                assertEquals(entirePatternMustMatch, sequencePattern.match(targetQ).getBestMatch(fairSorting) != null);
                assertEquals(entirePatternMustMatch, sequencePattern.match(targetQ).getMatches(randomGenerator
                                .nextBoolean(), fairSorting).take() != null);
            }
        }
    }

    @Test
    public void scoringRandomTest() throws Exception {
        Random randomGenerator = new Random();
        for (int i = 0; i < 5000; i++) {
            int errorScorePenalty = -randomGenerator.nextInt(1000) - 1;
            int middleInsertionSize = randomGenerator.nextInt(30) + 1;
            NucleotideSequence leftPart = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 5, 50);
            NucleotideSequence middleLetter = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 1);
            NucleotideSequence rightPart = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 5, 50);
            NucleotideSequence motif1 = SequencesUtils.concatenate(leftPart, middleLetter);
            NucleotideSequence motif2 = SequencesUtils.concatenate(middleLetter, rightPart);
            NucleotideSequence target1 = SequencesUtils.concatenate(leftPart, middleLetter, rightPart);
            NucleotideSequence middleInsertion = TestUtil.randomSequence(NucleotideSequence.ALPHABET, middleInsertionSize,
                    middleInsertionSize);
            NucleotideSequence target2 = SequencesUtils.concatenate(leftPart, middleInsertion, rightPart);

            NSequenceWithQuality targetQ1 = new NSequenceWithQuality(target1,
                    SequenceQuality.getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, target1.getSequence().size()));
            NSequenceWithQuality targetQ2 = new NSequenceWithQuality(target2,
                    SequenceQuality.getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, target2.getSequence().size()));
            FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(), motif1);
            FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(), motif2);
            FuzzyMatchPattern pattern3 = new FuzzyMatchPattern(getTestPatternAligner(), leftPart);
            FuzzyMatchPattern pattern4 = new FuzzyMatchPattern(getTestPatternAligner(), rightPart);
            SequencePattern sequencePattern1 = new SequencePattern(getTestPatternAligner(0,
                    0, 0, errorScorePenalty), pattern1, pattern2);
            SequencePattern sequencePattern2 = new SequencePattern(getTestPatternAligner(0,
                    0, 0, errorScorePenalty), pattern2, pattern1);
            SequencePattern sequencePattern3 = new SequencePattern(getTestPatternAligner(errorScorePenalty,
                    0, 0, errorScorePenalty), pattern1, pattern2);
            SequencePattern sequencePattern4 = new SequencePattern(getTestPatternAligner(errorScorePenalty,
                    0, 0, errorScorePenalty), pattern2, pattern1);
            SequencePattern sequencePattern5 = new SequencePattern(getTestPatternAligner(0,
                    0, 0, errorScorePenalty), pattern3, pattern4);
            SequencePattern sequencePattern6 = new SequencePattern(getTestPatternAligner(errorScorePenalty
                            * middleInsertionSize, 0, 0, errorScorePenalty), pattern3, pattern4);
            assertNull(sequencePattern1.match(targetQ1).getBestMatch());
            assertNull(sequencePattern2.match(targetQ1).getBestMatch());
            assertEquals(pattern1.match(targetQ1).getBestMatch().getScore()
                            + pattern2.match(targetQ1).getBestMatch().getScore() + errorScorePenalty,
                    sequencePattern3.match(targetQ1).getBestMatch().getScore());
            if ((pattern4.match(targetQ1).getBestMatch().getRange().getLower() == leftPart.size() + 1)
                    && (countPortValues(pattern3.match(targetQ1).getMatches()) == 1))
                assertNull(sequencePattern5.match(targetQ1).getBestMatch());
            if ((pattern4.match(targetQ2).getBestMatch().getRange().getLower() == leftPart.size() + middleInsertionSize)
                    && (countPortValues(pattern3.match(targetQ2).getMatches()) == 1)) {
                assertNull(sequencePattern5.match(targetQ2).getBestMatch());
                assertEquals(pattern3.match(targetQ2).getBestMatch().getScore()
                                + pattern4.match(targetQ2).getBestMatch().getScore() + errorScorePenalty * middleInsertionSize,
                        sequencePattern6.match(targetQ2).getBestMatch().getScore());
            }
            if (!leftPart.toString().equals(rightPart.toString()))
                assertNull(sequencePattern4.match(targetQ1).getBestMatch());
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
                new NucleotideSequence("ATAGA"), groupsEdgePositions1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequence("GATTC"), groupsEdgePositions2);
        SequencePattern sequencePattern = new SequencePattern(getTestPatternAligner(-10, 0,
                0, -5, true, 2, -1), pattern1, pattern2);
        NSequenceWithQuality nseq = new NSequenceWithQuality("ATAGATTC");
        MatchingResult result = sequencePattern.match(nseq);
        OutputPort<Match> matchOutputPort = result.getMatches(false, true);
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