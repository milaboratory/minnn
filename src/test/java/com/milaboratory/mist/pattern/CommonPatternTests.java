package com.milaboratory.mist.pattern;

import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import org.junit.Test;

import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.UnfairSorterConfiguration.*;
import static org.junit.Assert.*;

public class CommonPatternTests {
    @Test
    public void estimateComplexityTest() throws Exception {
        PatternAligner patternAligner = getRandomPatternAligner();

        SinglePattern[] patterns = new SinglePattern[16];
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

        MultipleReadsOperator[] mPatterns = new MultipleReadsOperator[4];
        mPatterns[0] = new MultiPattern(patternAligner, patterns[14], patterns[15]);
        mPatterns[1] = new MultiPattern(patternAligner, patterns[0], patterns[4]);
        mPatterns[2] = new AndOperator(patternAligner, mPatterns[0], mPatterns[1]);
        mPatterns[3] = new OrOperator(patternAligner, mPatterns[0], mPatterns[1]);

        long[] expected = new long[20];
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
        expected[16] = expected[14] + expected[15];
        expected[17] = expected[0] + expected[4];
        expected[18] = expected[16] + expected[17];
        expected[19] = expected[17];

        for (int i = 0; i <= 15; i++)
            assertEquals(expected[i], patterns[i].estimateComplexity());
        for (int i = 0; i <= 3; i++)
            assertEquals(expected[i + 16], mPatterns[i].estimateComplexity());
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
}
