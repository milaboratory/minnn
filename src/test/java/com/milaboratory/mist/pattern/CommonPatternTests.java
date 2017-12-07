package com.milaboratory.mist.pattern;

import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;
import com.milaboratory.core.sequence.*;
import com.milaboratory.test.TestUtil;
import org.junit.Test;

import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.UnfairSorterConfiguration.*;
import static org.junit.Assert.*;

public class CommonPatternTests {
    @Test
    public void estimateComplexityTest() throws Exception {
        PatternAligner patternAligner = getRandomPatternAligner();

        SinglePattern[] patterns = new SinglePattern[21];
        patterns[0] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("ATTAGACA"));
        patterns[1] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("CNNNC"),
                2, 2, -1, -1,
                getRandomGroupsForFuzzyMatch(5));
        patterns[2] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("WWATTNB"),
                0, 0, 1, -1);
        patterns[3] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("NNNNTNNAN"),
                1, 2, 1, 8);
        patterns[4] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("NNN"),
                1, 0, -1, -1);
        patterns[5] = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("N"),
                20, 20);
        patterns[6] = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("A"),
                4, 6);
        patterns[7] = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("B"),
                8, 10, 4, -1,
                getRandomGroupsForFuzzyMatch(8));
        patterns[8] = new SequencePattern(patternAligner, patterns[0], patterns[5]);
        patterns[9] = new SequencePattern(patternAligner, patterns[7], patterns[1], patterns[0]);
        patterns[10] = new PlusPattern(patternAligner, patterns[2], patterns[6]);
        patterns[11] = new PlusPattern(patternAligner, patterns[0], patterns[0]);
        patterns[12] = new SequencePattern(patternAligner, patterns[10], patterns[0]);
        patterns[13] = new SequencePattern(patternAligner, patterns[5], patterns[11]);
        patterns[14] = new OrPattern(patternAligner, patterns[7], patterns[11], patterns[12]);
        patterns[15] = new AndPattern(patternAligner, patterns[1], patterns[11]);
        patterns[16] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("attagaca"));
        patterns[17] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("atTAGAca"));
        patterns[18] = new FuzzyMatchPattern(patternAligner, new NucleotideSequenceCaseSensitive("WwATTnB"));
        patterns[19] = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("a"),
                4, 6);
        patterns[20] = new RepeatPattern(patternAligner, new NucleotideSequenceCaseSensitive("b"),
                8, 10, -1, -1,
                getRandomGroupsForFuzzyMatch(8));

        MultipleReadsOperator[] mPatterns = new MultipleReadsOperator[4];
        mPatterns[0] = new MultiPattern(patternAligner, patterns[14], patterns[15]);
        mPatterns[1] = new MultiPattern(patternAligner, patterns[0], patterns[4]);
        mPatterns[2] = new AndOperator(patternAligner, mPatterns[0], mPatterns[1]);
        mPatterns[3] = new OrOperator(patternAligner, mPatterns[0], mPatterns[1]);

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
            PatternAligner patternAligner = getRandomPatternAligner();
            boolean hasGroups = rg.nextBoolean();
            SinglePattern pattern = getRandomBasicPattern(patternAligner, hasGroups);
            Filter filter = new ScoreFilter(-rg.nextInt(75));
            FilterPattern filterPattern = new FilterPattern(patternAligner, filter, pattern);
            NotOperator notOperator = hasGroups ? null
                    : new NotOperator(patternAligner, new MultiPattern(patternAligner, pattern));
            MultipleReadsFilterPattern mFilterPattern = new MultipleReadsFilterPattern(patternAligner, filter,
                    new MultiPattern(patternAligner, pattern));

            if (pattern instanceof CanFixBorders) {
                if (((CanFixBorders)pattern).isBorderFixed())
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

    @Test
    public void fuzzingSinglePatternTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            NucleotideSequence randomSeq = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    1, 100);
            SinglePattern randomPattern = getRandomSinglePattern();
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
        PatternAligner[] patternAligners = new PatternAligner[2];
        String[] sequences = new String[1];
        NSequenceWithQuality[] targets = new NSequenceWithQuality[1];
        SinglePattern[] patterns = new SinglePattern[3];

        scorings[0] = new PatternAndTargetAlignmentScoring(0, -2, -1,
                (byte)25, (byte)6, -1);
        patternAligners[0] = getTestPatternAligner();
        patternAligners[1] = getTestPatternAligner(-40, 1, 0,
                -1, true, -1, -1, scorings[0]);
        sequences[0] = "t";
        targets[0] = new NSequenceWithQuality(sequences[0]);
        patterns[0] = new RepeatPattern(patternAligners[1], new NucleotideSequenceCaseSensitive("c"),
                1, 1, -1, 45);
        patterns[1] = new RepeatPattern(patternAligners[0], new NucleotideSequenceCaseSensitive(sequences[0]),
                4, 30, -1, -1);
        patterns[2] = new SequencePattern(patternAligners[0], patterns[0], patterns[1]);

        MatchingResult matchingResult = patterns[2].match(targets[0]);
        matchingResult.getBestMatch();
    }
}
