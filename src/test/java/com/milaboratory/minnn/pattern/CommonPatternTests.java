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

import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;
import com.milaboratory.core.sequence.*;
import com.milaboratory.test.TestUtil;
import org.junit.*;

import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.UnfairSorterConfiguration.*;
import static org.junit.Assert.*;

public class CommonPatternTests {
    @Test
    public void estimateComplexityTest() throws Exception {
        PatternConfiguration patternConfiguration = getRandomPatternConfiguration();

        SinglePattern[] patterns = new SinglePattern[21];
        patterns[0] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("ATTAGACA"));
        patterns[1] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("CNNNC"), 2, 2, -1, -1,
                getRandomGroupsForFuzzyMatch(5));
        patterns[2] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("WWATTNB"), 0, 0, 1, -1);
        patterns[3] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("NNNNTNNAN"), 1, 2, 1, 8);
        patterns[4] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("NNN"), 1, 0, -1, -1);
        patterns[5] = new RepeatPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("N"), 20, 20);
        patterns[6] = new RepeatPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("A"), 4, 6);
        patterns[7] = new RepeatPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("B"), 8, 10, 4, -1,
                getRandomGroupsForFuzzyMatch(8));
        patterns[8] = new SequencePattern(patternConfiguration,
                patterns[0], patterns[5]);
        patterns[9] = new SequencePattern(patternConfiguration,
                patterns[7], patterns[1], patterns[0]);
        patterns[10] = new PlusPattern(patternConfiguration,
                patterns[2], patterns[6]);
        patterns[11] = new PlusPattern(patternConfiguration,
                patterns[0], patterns[0]);
        patterns[12] = new SequencePattern(patternConfiguration,
                patterns[10], patterns[0]);
        patterns[13] = new SequencePattern(patternConfiguration,
                patterns[5], patterns[11]);
        patterns[14] = new OrPattern(patternConfiguration,
                patterns[7], patterns[11], patterns[12]);
        patterns[15] = new AndPattern(patternConfiguration,
                patterns[1], patterns[11]);
        patterns[16] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("attagaca"));
        patterns[17] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("atTAGAca"));
        patterns[18] = new FuzzyMatchPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("WwATTnB"));
        patterns[19] = new RepeatPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("a"), 4, 6);
        patterns[20] = new RepeatPattern(patternConfiguration,
                new NucleotideSequenceCaseSensitive("b"),
                8, 10, -1, -1,
                getRandomGroupsForFuzzyMatch(8));

        MultipleReadsOperator[] mPatterns = new MultipleReadsOperator[4];
        mPatterns[0] = createMultiPattern(patternConfiguration, patterns[14], patterns[15]);
        mPatterns[1] = createMultiPattern(patternConfiguration, patterns[0], patterns[4]);
        mPatterns[2] = new AndOperator(patternConfiguration, mPatterns[0], mPatterns[1]);
        mPatterns[3] = new OrOperator(patternConfiguration, mPatterns[0], mPatterns[1]);

        long[] expected = new long[25];
        expected[0] = notFixedSequenceMinComplexity + singleNucleotideComplexity / 8;
        expected[1] = notFixedSequenceMinComplexity + (long)(singleNucleotideComplexity * 9 / (2 + 3.0 / 16));
        expected[2] = 1;
        expected[3] = 6;
        expected[4] = notFixedSequenceMinComplexity + singleNucleotideComplexity * 32;
        expected[5] = notFixedSequenceMinComplexity + singleNucleotideComplexity * 16;
        expected[6] = notFixedSequenceMinComplexity + (singleNucleotideComplexity * 3 / 4);
        expected[7] = 3;
        expected[8] = expected[0] + fixedSequenceMaxComplexity;
        expected[9] = expected[7] + fixedSequenceMaxComplexity * 2;
        expected[10] = expected[2] + expected[6];
        expected[11] = expected[0] * 2;
        expected[12] = expected[0] + expected[10];
        expected[13] = expected[5] + expected[11];
        expected[14] = expected[12];
        expected[15] = expected[1] + expected[11];
        expected[16] = notFixedSequenceMinComplexity + (long)(singleNucleotideComplexity
                / (8.0 / (1 + smallLetterExtraComplexity)));
        expected[17] = notFixedSequenceMinComplexity + (long)(singleNucleotideComplexity
                / (4 + 4.0 / (1 + smallLetterExtraComplexity)));
        expected[18] = notFixedSequenceMinComplexity + (long)(singleNucleotideComplexity / (1.0 / 4
                + 1.0 / (4 + smallLetterExtraComplexity) + 3 + 1.0 / (16 + smallLetterExtraComplexity) + 1.0 / 9));
        expected[19] = notFixedSequenceMinComplexity + (singleNucleotideComplexity
                * 3 * (1 + smallLetterExtraComplexity) / 4);
        expected[20] = notFixedSequenceMinComplexity + (singleNucleotideComplexity
                * 3 * (9 + smallLetterExtraComplexity) / 8);
        expected[21] = expected[14] + expected[15];
        expected[22] = expected[0] + expected[4];
        expected[23] = expected[21] + expected[22];
        expected[24] = expected[22];

        for (int i = 0; i <= 20; i++)
            assertEquals(expected[i], patterns[i].estimateComplexity());
        for (int i = 0; i <= 3; i++)
            assertEquals(expected[i + 21], mPatterns[i].estimateComplexity());
    }

    @Test
    public void estimateComplexityRandomTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            PatternConfiguration patternConfiguration = getRandomPatternConfiguration();
            boolean hasGroups = rg.nextBoolean();
            SinglePattern pattern = getRandomBasicPattern(patternConfiguration, hasGroups);
            Filter filter = new ScoreFilter(-rg.nextInt(75));
            FilterPattern filterPattern = new FilterPattern(patternConfiguration, filter, pattern);
            NotOperator notOperator = hasGroups ? null : new NotOperator(patternConfiguration,
                    createMultiPattern(patternConfiguration, pattern));
            MultipleReadsFilterPattern mFilterPattern = new MultipleReadsFilterPattern(patternConfiguration,
                    filter, createMultiPattern(patternConfiguration, pattern));

            if (pattern instanceof CanFixBorders) {
                boolean isBorderFixed = false;
                try {
                    tryToFixBorder(tryToFixBorder(pattern, true, 0), false, 0);
                    tryToFixBorder(tryToFixBorder(pattern, true, 1), false, 1);
                } catch (IllegalStateException e) {
                    isBorderFixed = true;
                }
                if (isBorderFixed)
                    assertTrue(pattern.estimateComplexity() <= fixedSequenceMaxComplexity);
                else
                    assertTrue(pattern.estimateComplexity() >= notFixedSequenceMinComplexity);
            } else
                assertEquals(1, pattern.estimateComplexity());
            assertEquals(pattern.estimateComplexity(), filterPattern.estimateComplexity());
            if (notOperator != null)
                assertEquals(pattern.estimateComplexity(), notOperator.estimateComplexity());
            assertEquals(pattern.estimateComplexity(), mFilterPattern.estimateComplexity());
        }
    }

    private SinglePattern tryToFixBorder(SinglePattern pattern, boolean left, int position) {
        return ((CanFixBorders)pattern).fixBorder(left, position);
    }

    @Test
    public void fuzzingSinglePatternTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            NucleotideSequence randomSeq = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    1, 100);
            SinglePattern randomPattern = rg.nextBoolean() ? getRandomRawSinglePattern()
                    : getRandomSingleReadPattern();
            MatchingResult matchingResult = randomPattern.match(new NSequenceWithQuality(randomSeq.toString()));
            matchingResult.getBestMatch();
        }
    }

    @Test
    public void fuzzingMultiPatternTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            int numPatterns = rg.nextInt(5) + 1;
            Pattern randomPattern = getRandomMultiReadPattern(numPatterns);
            NSequenceWithQuality[] randomSequences = new NSequenceWithQuality[numPatterns];
            for (int j = 0; j < numPatterns; j++) {
                NucleotideSequence randomSeq = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                        1, 100);
                randomSequences[j] = new NSequenceWithQuality(randomSeq.toString());
            }
            MultiNSequenceWithQualityImpl mSeq = new MultiNSequenceWithQualityImpl(randomSequences);
            MatchingResult matchingResult = randomPattern.match(mSeq);
            matchingResult.getBestMatch();
        }
    }

    @Test
    public void specialCasesTest() throws Exception {
        PatternAndTargetAlignmentScoring[] scorings = new PatternAndTargetAlignmentScoring[1];
        PatternConfiguration[] patternConfigurations = new PatternConfiguration[2];
        String[] sequences = new String[1];
        NSequenceWithQuality[] targets = new NSequenceWithQuality[1];
        SinglePattern[] patterns = new SinglePattern[3];

        scorings[0] = new PatternAndTargetAlignmentScoring(0, -2, -1,
                -2, (byte)25, (byte)6, -1);
        patternConfigurations[0] = getTestPatternConfiguration();
        patternConfigurations[1] = getTestPatternConfiguration(-40, 1, 0,
                -1, -1, -1, scorings[0], false);
        sequences[0] = "t";
        targets[0] = new NSequenceWithQuality(sequences[0]);
        patterns[0] = new RepeatPattern(patternConfigurations[1],
                new NucleotideSequenceCaseSensitive("c"),
                1, 1, -1, 45);
        patterns[1] = new RepeatPattern(patternConfigurations[0],
                new NucleotideSequenceCaseSensitive(sequences[0]),
                4, 30, -1, -1);
        patterns[2] = new SequencePattern(patternConfigurations[0],
                patterns[0], patterns[1]);

        MatchingResult matchingResult = patterns[2].match(targets[0]);
        matchingResult.getBestMatch();
    }
}
