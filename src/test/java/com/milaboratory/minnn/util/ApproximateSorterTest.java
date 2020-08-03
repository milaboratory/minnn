/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
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
package com.milaboratory.minnn.util;

import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;
import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.pattern.*;
import com.milaboratory.test.TestUtil;
import org.junit.*;

import java.util.*;
import java.util.stream.*;

import static com.milaboratory.minnn.pattern.MatchValidationType.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class ApproximateSorterTest {
    @Test
    public void simpleTest() throws Exception {
        PatternConfiguration patternConfiguration = getTestPatternConfiguration(true);
        SinglePattern[] patterns = new SinglePattern[6];
        MultipleReadsOperator[] mPatterns = new MultipleReadsOperator[5];
        NSequenceWithQuality[] targets = new NSequenceWithQuality[2];
        MultiNSequenceWithQuality[] mTargets = new MultiNSequenceWithQuality[3];
        ApproximateSorterConfiguration[] configurations = new ApproximateSorterConfiguration[28];

        patterns[0] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("attagaca"));
        patterns[1] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("gt"));
        patterns[2] = new RepeatPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("t"), 2, 8);
        patterns[3] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("a"));
        patterns[4] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("att"));
        patterns[5] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("gcc"));

        mPatterns[0] = createMultiPattern(patternConfiguration, patterns[0], patterns[3]);
        mPatterns[1] = createMultiPattern(patternConfiguration, patterns[2], patterns[5]);
        mPatterns[2] = createMultiPattern(patternConfiguration, patterns[2], patterns[0], patterns[0]);
        mPatterns[3] = createMultiPattern(patternConfiguration, patterns[3], patterns[3], patterns[3]);
        mPatterns[4] = createMultiPattern(patternConfiguration, patterns[0], patterns[0]);

        targets[0] = new NSequenceWithQuality("ACTGCGATAAATTAGACAGTACGTATTAGACATTATTATTAGACAGAGACAGT");
        targets[1] = new NSequenceWithQuality("ATTATTGCCGCCATTATTGCCGCC");

        mTargets[0] = new MultiNSequenceWithQualityImpl(targets);
        mTargets[1] = new MultiNSequenceWithQualityImpl(targets[0], targets[0], targets[0]);
        mTargets[2] = new MultiNSequenceWithQualityImpl(targets[1], targets[1]);

        // SequencePattern
        configurations[0] = new ApproximateSorterConfiguration(targets[0], 0, targets[0].size(),
                patternConfiguration, true, true, FOLLOWING, 0,
                patterns[0], patterns[1]);
        configurations[1] = new ApproximateSorterConfiguration(targets[0], 26, 40,
                patternConfiguration, true, true, FOLLOWING, 0,
                patterns[2], patterns[3]);
        configurations[2] = new ApproximateSorterConfiguration(targets[0], 0, targets[0].size(),
                patternConfiguration, true, false, FOLLOWING, 20,
                patterns[0], patterns[1]);
        configurations[3] = new ApproximateSorterConfiguration(targets[0], 26, 40,
                patternConfiguration, true, false, FOLLOWING, 20,
                patterns[2], patterns[3]);
        assertEquals(1, countPortValues(new ApproximateSorter(configurations[0]).getOutputPort()));
        assertEquals(2, countPortValues(new ApproximateSorter(configurations[1]).getOutputPort()));
        assertEquals(1, countPortValues(new ApproximateSorter(configurations[2]).getOutputPort()));
        assertEquals(2, countPortValues(new ApproximateSorter(configurations[3]).getOutputPort()));

        // PlusPattern
        configurations[4] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternConfiguration, true, true, ORDER, 0,
                patterns[4], patterns[5]);
        configurations[5] = new ApproximateSorterConfiguration(targets[0], 26, 40,
                patternConfiguration, true, true, ORDER, 0,
                patterns[2], patterns[3]);
        configurations[6] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternConfiguration, true, false, ORDER, 20,
                patterns[4], patterns[5]);
        configurations[7] = new ApproximateSorterConfiguration(targets[0], 26, 40,
                patternConfiguration, true, false, ORDER, 20,
                patterns[2], patterns[3]);
        assertEquals(12, countPortValues(new ApproximateSorter(configurations[4]).getOutputPort()));
        assertEquals(3, countPortValues(new ApproximateSorter(configurations[5]).getOutputPort()));
        assertEquals(12, countPortValues(new ApproximateSorter(configurations[6]).getOutputPort()));
        assertEquals(3, countPortValues(new ApproximateSorter(configurations[7]).getOutputPort()));

        // AndPattern
        configurations[8] = new ApproximateSorterConfiguration(targets[0], 0, targets[0].size(),
                patternConfiguration, true, true, INTERSECTION, 0,
                patterns[1], patterns[2], patterns[3]);
        configurations[9] = new ApproximateSorterConfiguration(targets[1], 5, 23,
                patternConfiguration, true, true, INTERSECTION, 0,
                patterns[4], patterns[5]);
        configurations[10] = new ApproximateSorterConfiguration(targets[0], 0, targets[0].size(),
                patternConfiguration, true, false, INTERSECTION, 100,
                patterns[1], patterns[2], patterns[3]);
        configurations[11] = new ApproximateSorterConfiguration(targets[1], 5, 23,
                patternConfiguration, true, false, INTERSECTION, 20,
                patterns[4], patterns[5]);
        assertEquals(315, countPortValues(new ApproximateSorter(configurations[8]).getOutputPort()));
        assertEquals(6, countPortValues(new ApproximateSorter(configurations[9]).getOutputPort()));
        assertEquals(100, countPortValues(new ApproximateSorter(configurations[10]).getOutputPort()));
        assertEquals(6, countPortValues(new ApproximateSorter(configurations[11]).getOutputPort()));

        // OrPattern
        configurations[12] = new ApproximateSorterConfiguration(targets[0], 7, 15,
                patternConfiguration, false, true, FIRST, 0,
                patterns[2], patterns[4]);
        configurations[13] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternConfiguration, false, true, FIRST, 0,
                patterns[5], patterns[4], patterns[2]);
        configurations[14] = new ApproximateSorterConfiguration(targets[0], 7, 15,
                patternConfiguration, false, false, FIRST, 20,
                patterns[2], patterns[4]);
        configurations[15] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternConfiguration, false, false, FIRST, 20,
                patterns[5], patterns[4], patterns[2]);
        assertEquals(1, countPortValues(new ApproximateSorter(configurations[12]).getOutputPort()));
        assertEquals(64, countPortValues(new ApproximateSorter(configurations[13]).getOutputPort()));
        assertEquals(1, countPortValues(new ApproximateSorter(configurations[14]).getOutputPort()));
        assertEquals(20, countPortValues(new ApproximateSorter(configurations[15]).getOutputPort()));

        // MultiPattern
        configurations[16] = new ApproximateSorterConfiguration(mTargets[0], patternConfiguration, true,
                true, true, LOGICAL_AND, 0,
                patterns[3], patterns[4]);
        configurations[17] = new ApproximateSorterConfiguration(mTargets[1], patternConfiguration, true,
                true, true, LOGICAL_AND, 0,
                patterns[4], patterns[2], patterns[0]);
        configurations[18] = new ApproximateSorterConfiguration(mTargets[0], patternConfiguration, true,
                true, false, LOGICAL_AND, 20,
                patterns[3], patterns[4]);
        configurations[19] = new ApproximateSorterConfiguration(mTargets[1], patternConfiguration, true,
                true, false, LOGICAL_AND, 100,
                patterns[4], patterns[2], patterns[0]);
        assertEquals(84, countPortValues(new ApproximateSorter(configurations[16]).getOutputPort()));
        assertEquals(75, countPortValues(new ApproximateSorter(configurations[17]).getOutputPort()));
        assertEquals(20, countPortValues(new ApproximateSorter(configurations[18]).getOutputPort()));
        assertEquals(75, countPortValues(new ApproximateSorter(configurations[19]).getOutputPort()));

        // AndOperator
        configurations[20] = new ApproximateSorterConfiguration(mTargets[0], patternConfiguration, false,
                true, true, LOGICAL_AND, 0,
                mPatterns[0], mPatterns[1]);
        configurations[21] = new ApproximateSorterConfiguration(mTargets[1], patternConfiguration, false,
                true, true, LOGICAL_AND, 0,
                mPatterns[2], mPatterns[3]);
        configurations[22] = new ApproximateSorterConfiguration(mTargets[0], patternConfiguration, false,
                true, false, LOGICAL_AND, 100,
                mPatterns[0], mPatterns[1]);
        configurations[23] = new ApproximateSorterConfiguration(mTargets[1], patternConfiguration, false,
                true, false, LOGICAL_AND, 100,
                mPatterns[2], mPatterns[3], mPatterns[3], mPatterns[3]);
        assertEquals(240, countPortValues(new ApproximateSorter(configurations[20]).getOutputPort()));
        assertEquals(416745, countPortValues(new ApproximateSorter(configurations[21]).getOutputPort()));
        assertEquals(100, countPortValues(new ApproximateSorter(configurations[22]).getOutputPort()));
        assertEquals(100, countPortValues(new ApproximateSorter(configurations[23]).getOutputPort()));

        // OrOperator
        configurations[24] = new ApproximateSorterConfiguration(mTargets[2], patternConfiguration, false,
                false, true, LOGICAL_OR, 0,
                mPatterns[4], mPatterns[4]);
        configurations[25] = new ApproximateSorterConfiguration(mTargets[1], patternConfiguration, false,
                false, true, LOGICAL_OR, 0,
                mPatterns[2], mPatterns[3]);
        configurations[26] = new ApproximateSorterConfiguration(mTargets[2], patternConfiguration, false,
                false, false, LOGICAL_OR, 100,
                mPatterns[4], mPatterns[4]);
        configurations[27] = new ApproximateSorterConfiguration(mTargets[1], patternConfiguration, false,
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

            PatternConfiguration patternConfiguration = getTestPatternConfiguration(penaltyThreshold, 2,
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

            FuzzyMatchPattern pattern = new FuzzyMatchPattern(patternConfiguration, fragment);

            sorter = new ApproximateSorter(new ApproximateSorterConfiguration(finalTarget, 0, finalTarget.size(),
                    patternConfiguration, combineScoresBySum, fairSorting, matchValidationType,
                    expectedMatchesNum + 1, pattern, pattern, pattern, pattern));

            assertEquals(expectedMatchesNum, countPortValues(sorter.getOutputPort()));
        }
    }

    @Test
    public void specialCasesTest() throws Exception {
        PatternConfiguration[] patternConfigurations = new PatternConfiguration[4];
        String[] sequences = new String[2];
        SinglePattern[] patterns = new SinglePattern[10];
        NSequenceWithQuality[] targets = new NSequenceWithQuality[3];
        ApproximateSorterConfiguration[] configurations = new ApproximateSorterConfiguration[5];

        patternConfigurations[0] = getTestPatternConfiguration(-11000, 2, 0,
                -200);
        patternConfigurations[1] = getTestPatternConfiguration(-100, 0,
                0, 0, 1);
        patternConfigurations[2] = new PatternConfiguration(false, new BasePatternAligner(),
                new PatternAndTargetAlignmentScoring(0, -1, -10,
                        -1, (byte)34, (byte)0, -4),
                -100, -10, 1, 2, -1, 0);
        patternConfigurations[3] = new PatternConfiguration(false, new BasePatternAligner(),
                new PatternAndTargetAlignmentScoring(0, -7, -11,
                        -7, (byte)34, (byte)0, -4),
                -200, -10, 3, 2, -1, 0);

        sequences[0] = "atgggcgcaaatatagggagctccgatcgacatcgggtatcgccctggtacgatcccg";
        sequences[1] = "ggcaaagt";

        patterns[0] = new FuzzyMatchPattern(patternConfigurations[0],
                new NucleotideSequenceCaseSensitive(sequences[0]));
        patterns[1] = new FuzzyMatchPattern(patternConfigurations[1],
                new NucleotideSequenceCaseSensitive("ggca"));
        patterns[2] = new FuzzyMatchPattern(patternConfigurations[1],
                new NucleotideSequenceCaseSensitive("aaagt"));
        patterns[3] = new FuzzyMatchPattern(patternConfigurations[2],
                new NucleotideSequenceCaseSensitive("NN"));
        patterns[4] = new RepeatNPattern(patternConfigurations[2],
                12, 12);
        patterns[5] = new RepeatNPattern(patternConfigurations[2],
                22, 22);
        patterns[6] = new FuzzyMatchPattern(patternConfigurations[2],
                new NucleotideSequenceCaseSensitive("TCAG"));
        patterns[7] = new RepeatNPattern(patternConfigurations[3],
                14, 14, 0, -1);
        patterns[8] = new RepeatNPattern(patternConfigurations[3],
                22, 22);
        patterns[9] = new RepeatNPattern(patternConfigurations[3],
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
                patternConfigurations[0], true, true, FOLLOWING, 0,
                patterns[0], patterns[0], patterns[0], patterns[0]);
        assertEquals(2, countPortValues(new ApproximateSorter(configurations[0]).getOutputPort()));

        configurations[1] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternConfigurations[1], true, false, ORDER, 0,
                patterns[1], patterns[2]);
        configurations[2] = new ApproximateSorterConfiguration(targets[1], 0, targets[1].size(),
                patternConfigurations[1], true, false, ORDER, 1,
                patterns[1], patterns[2]);
        assertNull(new ApproximateSorter(configurations[1]).getOutputPort().take());
        assertEquals(sequences[1], new ApproximateSorter(configurations[2]).getOutputPort().take().getValue()
                .getSequence().toString().toLowerCase());

        configurations[3] = new ApproximateSorterConfiguration(targets[2], 0, targets[2].size(),
                patternConfigurations[2], true, false, FOLLOWING, 3,
                patterns[3], patterns[4], patterns[5], patterns[6]);
        assertNotNull(new ApproximateSorter(configurations[3]).getOutputPort().take());

        configurations[4] = new ApproximateSorterConfiguration(targets[2], 0, targets[2].size(),
                patternConfigurations[3], true, false, FOLLOWING, 100,
                patterns[7], patterns[8], patterns[9]);
        assertNotNull(new ApproximateSorter(configurations[4]).getOutputPort().take());
    }

    @Test
    public void matchesWithMisplacedRangesTest() throws Exception {
        NSequenceWithQuality seq = new NSequenceWithQuality("AATTAAGGCAAAGTAAATTGAGCA");
        for (boolean fairSorting : new boolean[] {true, false}) {
            for (int i = 0; i <= 1; i++) {
                PatternConfiguration patternConfiguration = getTestPatternConfiguration(-100,
                        0, 0, 0, i);
                FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(patternConfiguration,
                        new NucleotideSequenceCaseSensitive("ggca"));
                FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(patternConfiguration,
                        new NucleotideSequenceCaseSensitive("aaagt"));
                assertEquals(i, countPortValues(new ApproximateSorter(new ApproximateSorterConfiguration(seq,
                        0, seq.size(), patternConfiguration, true, fairSorting, ORDER,
                        10, pattern1, pattern2)).getOutputPort()));
                assertEquals(0, countPortValues(new ApproximateSorter(new ApproximateSorterConfiguration(seq,
                        0, seq.size(), patternConfiguration, true, fairSorting, ORDER,
                        10, pattern2, pattern1)).getOutputPort()));
                assertEquals(i, countPortValues(new ApproximateSorter(new ApproximateSorterConfiguration(seq,
                        0, seq.size(), patternConfiguration, true, fairSorting, INTERSECTION,
                        10, pattern1, pattern2)).getOutputPort()));
                assertEquals(i, countPortValues(new ApproximateSorter(new ApproximateSorterConfiguration(seq,
                        0, seq.size(), patternConfiguration, true, fairSorting, INTERSECTION,
                        10, pattern2, pattern1)).getOutputPort()));
            }
        }
    }

    @Test
    public void matchesWithNullValuesTest() throws Exception {
        PatternConfiguration patternConfiguration = getTestPatternConfiguration();
        NSequenceWithQuality seq = new NSequenceWithQuality("ATTA");
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQualityImpl(seq, seq, seq);
        FuzzyMatchPattern matchingPattern = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("a"));
        FuzzyMatchPattern notMatchingPattern = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("ccc"));
        MultiPattern matchingMPattern = createMultiPattern(patternConfiguration,
                matchingPattern, matchingPattern, matchingPattern);
        MultiPattern notMatchingMPattern = createMultiPattern(patternConfiguration,
                matchingPattern, notMatchingPattern, matchingPattern);
        NotOperator matchingNot = new NotOperator(patternConfiguration, notMatchingMPattern);
        NotOperator notMatchingNot = new NotOperator(patternConfiguration, matchingMPattern);
        AndOperator matchingAnd = new AndOperator(patternConfiguration, matchingMPattern, matchingNot);
        AndOperator notMatchingAnd = new AndOperator(patternConfiguration, matchingAnd, notMatchingNot);
        for (boolean fairSorting : new boolean[] {true, false}) {
            ApproximateSorterConfiguration conf = new ApproximateSorterConfiguration(mseq, patternConfiguration,
                    false, false, fairSorting, LOGICAL_OR, 100,
                    notMatchingAnd, matchingAnd, notMatchingAnd);
            assertEquals(8, countPortValues(new ApproximateSorter(conf).getOutputPort()));
        }
    }

    @Test
    public void uppercaseLettersTest() throws Exception {
        PatternConfiguration patternConfiguration = getTestPatternConfiguration(-100,
                0, 0, -1, 1);
        FuzzyMatchPattern[] patterns = Stream.of("aaa", "aaA", "aAa", "att", "Att", "aTt")
                .map(s -> new FuzzyMatchPattern(patternConfiguration,
                        new NucleotideSequenceCaseSensitive(s))).toArray(FuzzyMatchPattern[]::new);
        List<NSequenceWithQuality> targets = Stream.of("AAATT", "AAACATT").map(NSequenceWithQuality::new)
                .collect(Collectors.toList());
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
                ApproximateSorterConfiguration[] conf = targets.stream().map(t -> new ApproximateSorterConfiguration(t,
                        0, t.size(), patternConfiguration, true, true,
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
