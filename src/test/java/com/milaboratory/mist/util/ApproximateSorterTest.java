package com.milaboratory.mist.util;

import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.pattern.*;
import com.milaboratory.test.TestUtil;
import org.junit.Test;

import java.util.Arrays;

import static com.milaboratory.mist.pattern.MatchValidationType.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class ApproximateSorterTest {
    @Test
    public void simpleTest() throws Exception {
        PatternAligner patternAligner = getTestPatternAligner(true);
        SinglePattern[] patterns = new SinglePattern[6];
        MultipleReadsOperator[] mPatterns = new MultipleReadsOperator[5];
        NSequenceWithQuality[] targets = new NSequenceWithQuality[2];
        MultiNSequenceWithQuality[] mTargets = new MultiNSequenceWithQuality[3];
        ApproximateSorterConfiguration[] configurations = new ApproximateSorterConfiguration[28];

        patterns[0] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("attagaca"));
        patterns[1] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("gt"));
        patterns[2] = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("t"),
                2, 8);
        patterns[3] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("a"));
        patterns[4] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("att"));
        patterns[5] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("gcc"));

        mPatterns[0] = new MultiPattern(patternAligner, patterns[0], patterns[3]);
        mPatterns[1] = new MultiPattern(patternAligner, patterns[2], patterns[5]);
        mPatterns[2] = new MultiPattern(patternAligner, patterns[2], patterns[0], patterns[0]);
        mPatterns[3] = new MultiPattern(patternAligner, patterns[3], patterns[3], patterns[3]);
        mPatterns[4] = new MultiPattern(patternAligner, patterns[0], patterns[0]);

        targets[0] = new NSequenceWithQuality("ACTGCGATAAATTAGACAGTACGTATTAGACATTATTATTAGACAGAGACAGT");
        targets[1] = new NSequenceWithQuality("ATTATTGCCGCCATTATTGCCGCC");

        mTargets[0] = new MultiNSequenceWithQualityImpl(targets);
        mTargets[1] = new MultiNSequenceWithQualityImpl(targets[0], targets[0], targets[0]);
        mTargets[2] = new MultiNSequenceWithQualityImpl(targets[1], targets[1]);

        // SequencePattern
        configurations[0] = new ApproximateSorterConfiguration(targets[0], 0, targets[0].size(),
                patternAligner, true, true, FOLLOWING, 0,
                patterns[0], patterns[1]);
        configurations[1] = new ApproximateSorterConfiguration(targets[0], 26, 40,
                patternAligner, true, true, FOLLOWING, 0,
                patterns[2], patterns[3]);
        configurations[2] = new ApproximateSorterConfiguration(targets[0], 0, targets[0].size(),
                patternAligner, true, false, FOLLOWING, 20,
                patterns[0], patterns[1]);
        configurations[3] = new ApproximateSorterConfiguration(targets[0], 26, 40,
                patternAligner, true, false, FOLLOWING, 20,
                patterns[2], patterns[3]);
        assertEquals(1, countPortValues(new ApproximateSorter(configurations[0]).getOutputPort()));
        assertEquals(2, countPortValues(new ApproximateSorter(configurations[1]).getOutputPort()));
        assertEquals(1, countPortValues(new ApproximateSorter(configurations[2]).getOutputPort()));
        assertEquals(2, countPortValues(new ApproximateSorter(configurations[3]).getOutputPort()));

        // PlusPattern
        configurations[4] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternAligner, true, true, ORDER, 0,
                patterns[4], patterns[5]);
        configurations[5] = new ApproximateSorterConfiguration(targets[0], 26, 40,
                patternAligner, true, true, ORDER, 0,
                patterns[2], patterns[3]);
        configurations[6] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternAligner, true, false, ORDER, 20,
                patterns[4], patterns[5]);
        configurations[7] = new ApproximateSorterConfiguration(targets[0], 26, 40,
                patternAligner, true, false, ORDER, 20,
                patterns[2], patterns[3]);
        assertEquals(12, countPortValues(new ApproximateSorter(configurations[4]).getOutputPort()));
        assertEquals(3, countPortValues(new ApproximateSorter(configurations[5]).getOutputPort()));
        assertEquals(12, countPortValues(new ApproximateSorter(configurations[6]).getOutputPort()));
        assertEquals(3, countPortValues(new ApproximateSorter(configurations[7]).getOutputPort()));

        // AndPattern
        configurations[8] = new ApproximateSorterConfiguration(targets[0], 0, targets[0].size(),
                patternAligner, true, true, INTERSECTION, 0,
                patterns[1], patterns[2], patterns[3]);
        configurations[9] = new ApproximateSorterConfiguration(targets[1], 5, 23,
                patternAligner, true, true, INTERSECTION, 0,
                patterns[4], patterns[5]);
        configurations[10] = new ApproximateSorterConfiguration(targets[0], 0, targets[0].size(),
                patternAligner, true, false, INTERSECTION, 100,
                patterns[1], patterns[2], patterns[3]);
        configurations[11] = new ApproximateSorterConfiguration(targets[1], 5, 23,
                patternAligner, true, false, INTERSECTION, 20,
                patterns[4], patterns[5]);
        assertEquals(315, countPortValues(new ApproximateSorter(configurations[8]).getOutputPort()));
        assertEquals(6, countPortValues(new ApproximateSorter(configurations[9]).getOutputPort()));
        assertEquals(100, countPortValues(new ApproximateSorter(configurations[10]).getOutputPort()));
        assertEquals(6, countPortValues(new ApproximateSorter(configurations[11]).getOutputPort()));

        // OrPattern
        configurations[12] = new ApproximateSorterConfiguration(targets[0], 7, 15,
                patternAligner, false, true, FIRST, 0,
                patterns[2], patterns[4]);
        configurations[13] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternAligner, false, true, FIRST, 0,
                patterns[5], patterns[4], patterns[2]);
        configurations[14] = new ApproximateSorterConfiguration(targets[0], 7, 15,
                patternAligner, false, false, FIRST, 20,
                patterns[2], patterns[4]);
        configurations[15] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternAligner, false, false, FIRST, 20,
                patterns[5], patterns[4], patterns[2]);
        assertEquals(1, countPortValues(new ApproximateSorter(configurations[12]).getOutputPort()));
        assertEquals(64, countPortValues(new ApproximateSorter(configurations[13]).getOutputPort()));
        assertEquals(1, countPortValues(new ApproximateSorter(configurations[14]).getOutputPort()));
        assertEquals(20, countPortValues(new ApproximateSorter(configurations[15]).getOutputPort()));

        // MultiPattern
        configurations[16] = new ApproximateSorterConfiguration(mTargets[0], patternAligner, true,
                true, true, LOGICAL_AND, 0,
                patterns[3], patterns[4]);
        configurations[17] = new ApproximateSorterConfiguration(mTargets[1], patternAligner, true,
                true, true, LOGICAL_AND, 0,
                patterns[4], patterns[2], patterns[0]);
        configurations[18] = new ApproximateSorterConfiguration(mTargets[0], patternAligner, true,
                true, false, LOGICAL_AND, 20,
                patterns[3], patterns[4]);
        configurations[19] = new ApproximateSorterConfiguration(mTargets[1], patternAligner, true,
                true, false, LOGICAL_AND, 100,
                patterns[4], patterns[2], patterns[0]);
        assertEquals(84, countPortValues(new ApproximateSorter(configurations[16]).getOutputPort()));
        assertEquals(75, countPortValues(new ApproximateSorter(configurations[17]).getOutputPort()));
        assertEquals(20, countPortValues(new ApproximateSorter(configurations[18]).getOutputPort()));
        assertEquals(75, countPortValues(new ApproximateSorter(configurations[19]).getOutputPort()));

        // AndOperator
        configurations[20] = new ApproximateSorterConfiguration(mTargets[0], patternAligner, false,
                true, true, LOGICAL_AND, 0,
                mPatterns[0], mPatterns[1]);
        configurations[21] = new ApproximateSorterConfiguration(mTargets[1], patternAligner, false,
                true, true, LOGICAL_AND, 0,
                mPatterns[2], mPatterns[3]);
        configurations[22] = new ApproximateSorterConfiguration(mTargets[0], patternAligner, false,
                true, false, LOGICAL_AND, 100,
                mPatterns[0], mPatterns[1]);
        configurations[23] = new ApproximateSorterConfiguration(mTargets[1], patternAligner, false,
                true, false, LOGICAL_AND, 100,
                mPatterns[2], mPatterns[3], mPatterns[3], mPatterns[3]);
        assertEquals(240, countPortValues(new ApproximateSorter(configurations[20]).getOutputPort()));
        assertEquals(416745, countPortValues(new ApproximateSorter(configurations[21]).getOutputPort()));
        assertEquals(100, countPortValues(new ApproximateSorter(configurations[22]).getOutputPort()));
        assertEquals(100, countPortValues(new ApproximateSorter(configurations[23]).getOutputPort()));

        // OrOperator
        configurations[24] = new ApproximateSorterConfiguration(mTargets[2], patternAligner, false,
                false, true, LOGICAL_OR, 0,
                mPatterns[4], mPatterns[4]);
        configurations[25] = new ApproximateSorterConfiguration(mTargets[1], patternAligner, false,
                false, true, LOGICAL_OR, 0,
                mPatterns[2], mPatterns[3]);
        configurations[26] = new ApproximateSorterConfiguration(mTargets[2], patternAligner, false,
                false, false, LOGICAL_OR, 100,
                mPatterns[4], mPatterns[4]);
        configurations[27] = new ApproximateSorterConfiguration(mTargets[1], patternAligner, false,
                false, false, LOGICAL_OR, 100,
                mPatterns[2], mPatterns[3], mPatterns[3], mPatterns[3]);
        assertEquals(0, countPortValues(new ApproximateSorter(configurations[24]).getOutputPort()));
        assertEquals(416745, countPortValues(new ApproximateSorter(configurations[25]).getOutputPort()));
        assertEquals(0, countPortValues(new ApproximateSorter(configurations[26]).getOutputPort()));
        assertEquals(100, countPortValues(new ApproximateSorter(configurations[27]).getOutputPort()));
    }

    @Test
    public void randomTest() throws Exception {
        ApproximateSorter sorter;
        for (int i = 0; i < 5000; ++i) {
            int singleOverlapPenalty = -rg.nextInt(1000) - 1;
            int numberOfFragments = rg.nextInt(7) + 1;
            int spaceLength = rg.nextInt(3);
            boolean fairSorting = rg.nextBoolean();
            long penaltyThreshold = 500 * singleOverlapPenalty;
            int expectedMatchesNum;
            boolean combineScoresBySum;
            MatchValidationType matchValidationType;
            switch (rg.nextInt(4)) {
                case 0:
                    matchValidationType = FOLLOWING;
                    expectedMatchesNum = Math.max(0, numberOfFragments - 3);
                    combineScoresBySum = true;
                    penaltyThreshold = 0;
                    break;
                case 1:
                    matchValidationType = ORDER;
                    expectedMatchesNum = (numberOfFragments >= 4) ? 1 : 0;
                    for (int j = numberOfFragments; j >= Math.max(5, numberOfFragments - 3); j--)
                        expectedMatchesNum *= j;
                    for (int j = 2; j <= numberOfFragments - 4; j++)
                        expectedMatchesNum /= j;
                    combineScoresBySum = true;
                    break;
                case 2:
                    matchValidationType = INTERSECTION;
                    expectedMatchesNum = numberOfFragments * (numberOfFragments - 1) * (numberOfFragments - 2)
                            * (numberOfFragments - 3);
                    combineScoresBySum = true;
                    break;
                default:
                    matchValidationType = FIRST;
                    expectedMatchesNum = (int)Math.pow(numberOfFragments, 4);
                    combineScoresBySum = false;
            }

            PatternAligner patternAligner = getTestPatternAligner(penaltyThreshold, 2,
                    -rg.nextInt(1000), singleOverlapPenalty);
            NucleotideSequenceCaseSensitive target = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 0, spaceLength);
            NucleotideSequenceCaseSensitive fragment = TestUtil.randomSequence(
                    NucleotideSequenceCaseSensitive.ALPHABET, 50, 100);
            for (int j = 0; j < numberOfFragments; j++) {
                if (matchValidationType != FOLLOWING) {
                    NucleotideSequenceCaseSensitive space = TestUtil.randomSequence(
                            NucleotideSequenceCaseSensitive.ALPHABET, 0, spaceLength);

                    target = SequencesUtils.concatenate(target, fragment, space);
                } else
                    target = SequencesUtils.concatenate(target, fragment);
            }
            final NSequenceWithQuality finalTarget = new NSequenceWithQuality(target.toString());

            FuzzyMatchPattern pattern = new FuzzyMatchPattern(patternAligner, fragment);

            sorter = new ApproximateSorter(new ApproximateSorterConfiguration(finalTarget, 0, finalTarget.size(),
                    patternAligner, combineScoresBySum, fairSorting, matchValidationType,
                    expectedMatchesNum + 1, pattern, pattern, pattern, pattern));

            assertEquals(expectedMatchesNum, countPortValues(sorter.getOutputPort()));
        }
    }

    @Test
    public void specialCasesTest() throws Exception {
        PatternAligner[] patternAligners = new PatternAligner[4];
        String[] sequences = new String[2];
        SinglePattern[] patterns = new SinglePattern[10];
        NSequenceWithQuality[] targets = new NSequenceWithQuality[3];
        ApproximateSorterConfiguration[] configurations = new ApproximateSorterConfiguration[5];

        patternAligners[0] = getTestPatternAligner(-11000, 2, 0,
                -200);
        patternAligners[1] = getTestPatternAligner(-100, 0,
                0, 0, 1);
        patternAligners[2] = new BasePatternAligner(new PatternAndTargetAlignmentScoring(0,
                -1, -10, (byte)34, (byte)0, -4),
                -100, -10, 1, 2);
        patternAligners[3] = new BasePatternAligner(new PatternAndTargetAlignmentScoring(0,
                -7, -11, (byte)34, (byte)0, -4),
                -200, -10, 3, 2);

        sequences[0] = "atgggcgcaaatatagggagctccgatcgacatcgggtatcgccctggtacgatcccg";
        sequences[1] = "ggcaaagt";

        patterns[0] = new FuzzyMatchPattern(patternAligners[0], new NucleotideSequenceCaseSensitive(sequences[0]));
        patterns[1] = new FuzzyMatchPattern(patternAligners[1], new NucleotideSequenceCaseSensitive("ggca"));
        patterns[2] = new FuzzyMatchPattern(patternAligners[1], new NucleotideSequenceCaseSensitive("aaagt"));
        patterns[3] = new FuzzyMatchPattern(patternAligners[2], new NucleotideSequenceCaseSensitive("NN"));
        patterns[4] = new RepeatPattern(patternAligners[2], new NucleotideSequenceCaseSensitive("N"),
                12, 12);
        patterns[5] = new RepeatPattern(patternAligners[2], new NucleotideSequenceCaseSensitive("N"),
                22, 22);
        patterns[6] = new FuzzyMatchPattern(patternAligners[2], new NucleotideSequenceCaseSensitive("TCAG"));
        patterns[7] = new RepeatPattern(patternAligners[3], new NucleotideSequenceCaseSensitive("N"),
                14, 14, 0, -1);
        patterns[8] = new RepeatPattern(patternAligners[3], new NucleotideSequenceCaseSensitive("n"),
                22, 22);
        patterns[9] = new RepeatPattern(patternAligners[3], new NucleotideSequenceCaseSensitive("N"),
                4, 4);

        targets[0] = new NSequenceWithQuality(repeatString(sequences[0], 5));
        targets[1] = new NSequenceWithQuality(sequences[1]);
        targets[2] = new NSequenceWithQuality("TTTCGTTTCGTGATTAGCGTGAAGACGACAGAACCAGAACTGGGATCCAT" +
                "TATCGGCGGCGAATTTACCACCATTGAAAACCAGCCGTGGTTTGCGGTGA" +
                "TTTATCGTCGTCATCGTGGCGGCAGCGTGACCTATGTGTGCGGCGGCAGCC",
                "@@CFFABDDHFHHIEGBFGHIGEGIIIFIIIDHGDHIIIEGDHIBEHIII" +
                        "GIIIGIHFCC@;@5?CACCCCCCCCCCACCCBBBB?><59<BBB@@@0>@" +
                        "C4A>4>+3?<5(8(4(2+55>BBB&(&29<@>44:44:++4&05.9>59@?");

        configurations[0] = new ApproximateSorterConfiguration(targets[0], 0, targets[0].size(),
                patternAligners[0], true, true, FOLLOWING, 0,
                patterns[0], patterns[0], patterns[0], patterns[0]);
        assertEquals(2, countPortValues(new ApproximateSorter(configurations[0]).getOutputPort()));

        configurations[1] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternAligners[1], true, false, ORDER, 0,
                patterns[1], patterns[2]);
        configurations[2] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternAligners[1], true, false, ORDER, 1,
                patterns[1], patterns[2]);
        assertNull(new ApproximateSorter(configurations[1]).getOutputPort().take());
        assertEquals(sequences[1], new ApproximateSorter(configurations[2]).getOutputPort().take().getValue()
                .getSequence().toString().toLowerCase());

        configurations[3] = new ApproximateSorterConfiguration(targets[2], 0, targets[2].size(),
                patternAligners[2], true, false, FOLLOWING, 3,
                patterns[3], patterns[4], patterns[5], patterns[6]);
        assertNotNull(new ApproximateSorter(configurations[3]).getOutputPort().take());

        configurations[4] = new ApproximateSorterConfiguration(targets[2], 0, targets[2].size(),
                patternAligners[3], true, false, FOLLOWING, 100,
                patterns[7], patterns[8], patterns[9]);
        assertNotNull(new ApproximateSorter(configurations[4]).getOutputPort().take());
    }

    @Test
    public void matchesWithMisplacedRangesTest() throws Exception {
        NSequenceWithQuality seq = new NSequenceWithQuality("AATTAAGGCAAAGTAAATTGAGCA");
        for (boolean fairSorting : new boolean[] {true, false}) {
            for (int i = 0; i <= 1; i++) {
                PatternAligner patternAligner = getTestPatternAligner(-100, 0,
                        0, 0, i);
                FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(patternAligner,
                        new NucleotideSequenceCaseSensitive("ggca"));
                FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(patternAligner,
                        new NucleotideSequenceCaseSensitive("aaagt"));
                assertEquals(i, countPortValues(new ApproximateSorter(new ApproximateSorterConfiguration(seq,
                        0, seq.size(), patternAligner, true, fairSorting, ORDER,
                        10, pattern1, pattern2)).getOutputPort()));
                assertEquals(0, countPortValues(new ApproximateSorter(new ApproximateSorterConfiguration(seq,
                        0, seq.size(), patternAligner, true, fairSorting, ORDER,
                        10, pattern2, pattern1)).getOutputPort()));
                assertEquals(i, countPortValues(new ApproximateSorter(new ApproximateSorterConfiguration(seq,
                        0, seq.size(), patternAligner, true, fairSorting, INTERSECTION,
                        10, pattern1, pattern2)).getOutputPort()));
                assertEquals(i, countPortValues(new ApproximateSorter(new ApproximateSorterConfiguration(seq,
                        0, seq.size(), patternAligner, true, fairSorting, INTERSECTION,
                        10, pattern2, pattern1)).getOutputPort()));
            }
        }
    }

    @Test
    public void matchesWithNullValuesTest() throws Exception {
        PatternAligner patternAligner = getTestPatternAligner();
        NSequenceWithQuality seq = new NSequenceWithQuality("ATTA");
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQualityImpl(seq, seq, seq);
        FuzzyMatchPattern matchingPattern = new FuzzyMatchPattern(patternAligner,
                new NucleotideSequenceCaseSensitive("a"));
        FuzzyMatchPattern notMatchingPattern = new FuzzyMatchPattern(patternAligner,
                new NucleotideSequenceCaseSensitive("ccc"));
        MultiPattern matchingMPattern = new MultiPattern(patternAligner,
                matchingPattern, matchingPattern, matchingPattern);
        MultiPattern notMatchingMPattern = new MultiPattern(patternAligner,
                matchingPattern, notMatchingPattern, matchingPattern);
        NotOperator matchingNot = new NotOperator(patternAligner, notMatchingMPattern);
        NotOperator notMatchingNot = new NotOperator(patternAligner, matchingMPattern);
        AndOperator matchingAnd = new AndOperator(patternAligner, matchingMPattern, matchingNot);
        AndOperator notMatchingAnd = new AndOperator(patternAligner, matchingAnd, notMatchingNot);
        for (boolean fairSorting : new boolean[] {true, false}) {
            ApproximateSorterConfiguration conf = new ApproximateSorterConfiguration(mseq, patternAligner,
                    false, false, fairSorting, LOGICAL_OR, 100,
                    notMatchingAnd, matchingAnd, notMatchingAnd);
            assertEquals(8, countPortValues(new ApproximateSorter(conf).getOutputPort()));
        }
    }

    @Test
    public void uppercaseLettersTest() throws Exception {
        PatternAligner patternAligner = getTestPatternAligner(-100,
                0, 0, -1, 1);
        String[] sequences = new String[] { "aaa", "aaA", "aAa", "att", "Att", "aTt" };
        FuzzyMatchPattern[] patterns = Arrays.stream(sequences)
                .map(s -> new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive(s)))
                .toArray(FuzzyMatchPattern[]::new);
        NSequenceWithQuality targets[] = new NSequenceWithQuality[]
                { new NSequenceWithQuality("AAATT"), new NSequenceWithQuality("AAACATT") };
        FuzzyMatchPattern[][] patternPairs = new FuzzyMatchPattern[][] {
                { patterns[0], patterns[3] },   // 0
                { patterns[1], patterns[3] },   // 1
                { patterns[2], patterns[3] },   // 2
                { patterns[0], patterns[4] },   // 3
                { patterns[0], patterns[5] },   // 4
                { patterns[1], patterns[4] },   // 5
                { patterns[2], patterns[5] },   // 6
                { patterns[1], patterns[5] },   // 7
                { patterns[2], patterns[4] }    // 8
        };
        for (MatchValidationType matchValidationType : new MatchValidationType[] { INTERSECTION, ORDER, FOLLOWING })
            for (int i = 0; i < patternPairs.length; i++) {
                final int pairNum = i;
                ApproximateSorterConfiguration[] conf = Arrays.stream(targets)
                        .map(t -> new ApproximateSorterConfiguration(t,
                            0, t.size(), patternAligner, true, true,
                            matchValidationType, 0, patternPairs[pairNum]))
                        .toArray(ApproximateSorterConfiguration[]::new);
                switch (pairNum) {
                    case 0:
                        assertTrue(matchFound(conf[0]));
                        assertTrue(matchFound(conf[1]));
                        break;
                    case 1:
                    case 3:
                    case 5:
                    case 7:
                    case 8:
                        assertFalse(matchFound(conf[0]));
                        assertEquals(matchValidationType != FOLLOWING, matchFound(conf[1]));
                        break;
                    case 2:
                    case 4:
                    case 6:
                        assertFalse(matchFound(conf[0]));
                        assertTrue(matchFound(conf[1]));
                }
            }
    }

    private boolean matchFound(ApproximateSorterConfiguration conf) {
        return new ApproximateSorter(conf).getOutputPort().take() != null;
    }
}
