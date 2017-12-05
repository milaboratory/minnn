package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import com.milaboratory.test.TestUtil;
import org.junit.Test;

import static com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive.fromNucleotideSequence;
import static com.milaboratory.mist.cli.Defaults.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class BasePatternAlignerTest {
    private BasePatternAligner getRandomAligner() {
        int matchScore = -rg.nextInt(10);
        int mismatchScore = -rg.nextInt(10) - 1 + matchScore;
        int gapScore = -rg.nextInt(10) - 1 + matchScore;
        int badQualityPenalty = (Math.min(-mismatchScore, -gapScore) + matchScore == 0) ? 0
                : -rg.nextInt(Math.min(-mismatchScore, -gapScore) + matchScore);
        byte badQuality = (byte)(rg.nextInt(5));
        PatternAndTargetAlignmentScoring scoring = new PatternAndTargetAlignmentScoring(matchScore, mismatchScore,
                gapScore, (byte)(rg.nextInt(30) + 1 + badQuality), badQuality, badQualityPenalty);
        return new BasePatternAligner(scoring, -rg.nextInt(100), -rg.nextInt(10),
                rg.nextInt(4), rg.nextInt(4));
    }

    @Test
    public void compatibilityTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            BasePatternAligner aligner = getRandomAligner();
            assertTrue(aligner.compatible(aligner.overridePenaltyThreshold(-rg.nextInt(100))));
            assertTrue(aligner.compatible(aligner.setLeftBorder(rg.nextInt(20))));
        }
    }

    @Test
    public void randomAlignerTest() {
        for (int i = 0; i < 1000; i++) {
            BasePatternAligner[] aligners = new BasePatternAligner[2];
            aligners[0] = getRandomAligner();
            aligners[1] = (BasePatternAligner)(aligners[0].setLeftBorder(0));
            int seqLength = rg.nextInt(40) + 1;
            NucleotideSequenceCaseSensitive[] sequences = new NucleotideSequenceCaseSensitive[3];
            sequences[0] = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET, seqLength, seqLength);
            sequences[1] = fromNucleotideSequence(sequences[0].toNucleotideSequence(), false);
            sequences[2] = fromNucleotideSequence(sequences[0].toNucleotideSequence(), true);
            for (BasePatternAligner aligner : aligners)
                for (NucleotideSequenceCaseSensitive sequence : sequences) {
                    NSequenceWithQuality target = setRandomQuality(sequence.toString());
                    Alignment<NucleotideSequenceCaseSensitive> alignment = aligner.align(sequence, target,
                            seqLength - 1);
                    assertEquals(new Range(0, seqLength), alignment.getSequence1Range());
                    assertEquals(alignment.getSequence1Range(), alignment.getSequence2Range());
                }
        }
    }

    @Test
    public void alignmentTest() {
        PatternAndTargetAlignmentScoring scoring = new PatternAndTargetAlignmentScoring(0,
                -9, -10, DEFAULT_GOOD_QUALITY, DEFAULT_BAD_QUALITY, -3);
        BasePatternAligner patternAligner = new BasePatternAligner(scoring, -30, -10,
                2, 1);
        NucleotideSequenceCaseSensitive pattern = new NucleotideSequenceCaseSensitive("aTTAgaca");
        NSequenceWithQuality target = new NSequenceWithQuality("CCTTATTC");
        Alignment<NucleotideSequenceCaseSensitive> alignment = patternAligner.align(pattern, target, 7);
        assertEquals(new Range(1, 8), alignment.getSequence2Range());
        assertEquals(new Range(0, 8), alignment.getSequence1Range());

        pattern = new NucleotideSequenceCaseSensitive("ATTAgaCA");
        target = new NSequenceWithQuality("ATTTAGACA");
        alignment = patternAligner.align(pattern, target, 8);
        assertEquals(new Range(1, 9), alignment.getSequence2Range());
        assertEquals(-9, (int)alignment.getScore());

        patternAligner = (BasePatternAligner)patternAligner.setLeftBorder(0);
        alignment = patternAligner.align(pattern, target, 8);
        assertEquals(new Range(0, 9), alignment.getSequence2Range());
        assertEquals(-28, (int)alignment.getScore());
    }
}
