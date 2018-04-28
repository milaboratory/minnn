package com.milaboratory.mist.util;

import com.milaboratory.core.alignment.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.pattern.PatternAligner;
import com.milaboratory.test.TestUtil;
import org.junit.*;

import static com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive.fromNucleotideSequence;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.DebugUtils.*;
import static org.junit.Assert.*;

public class ProfilingTest {
    @Before
    public void setUp() throws Exception {
        resetTimeCounter();
    }

    @After
    public void tearDown() throws Exception {
        resetTimeCounter();
    }

    @Test
    public void nucleotideSequenceProfilingTest() throws Exception {
        NucleotideAlphabet alphabetOld = NucleotideSequence.ALPHABET;
        NucleotideAlphabetCaseSensitive alphabetNew = NucleotideSequenceCaseSensitive.ALPHABET;
        for (int i = 0; i < 100000; i++) {
            final NucleotideSequence sequenceOld = TestUtil.randomSequence(alphabetOld,
                    500, 500);
            final NucleotideSequenceCaseSensitive sequenceNew = TestUtil.randomSequence(alphabetNew,
                    500, 500);
            final String stringOld = (String)countExecutionTimeR("ToStringOld", sequenceOld::toString);
            final String stringNew = (String)countExecutionTimeR("ToStringNew", sequenceNew::toString);
            countExecutionTime("CreateOld", () -> {
                new NucleotideSequence(stringOld);
                return null;
            });
            countExecutionTime("CreateNew", () -> {
                new NucleotideSequenceCaseSensitive(stringNew);
                return null;
            });
            countExecutionTime("OldToNew", () -> {
                fromNucleotideSequence(sequenceOld, false);
                return null;
            });
            countExecutionTime("NewToOld", () -> {
                sequenceNew.toNucleotideSequence();
                return null;
            });
        }
        assertTrue(timeCounter.get("ToStringOld") * 1.5 > timeCounter.get("ToStringNew"));
        assertTrue(timeCounter.get("CreateOld") * 1.5 > timeCounter.get("CreateNew"));
        assertTrue(timeCounter.get("ToStringOld") * 10 > timeCounter.get("OldToNew"));
        assertTrue(timeCounter.get("ToStringOld") * 10 > timeCounter.get("NewToOld"));
    }

    @Test
    public void alignerProfilingTest() throws Exception {
        final LinearGapAlignmentScoring<NucleotideSequence> scoringOld = new LinearGapAlignmentScoring<>(
                NucleotideSequence.ALPHABET, 0, -9, -10);
        final PatternAndTargetAlignmentScoring scoringNew = new PatternAndTargetAlignmentScoring(
                0, -9, -10, -9,
                (byte)34, (byte)0, 0);
        final PatternAligner patternAligner = getTestPatternAligner();
        for (int i = 0; i < 100000; i++) {
            final NucleotideSequence seq1 = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    20, 40);
            final NucleotideSequence seq2 = TestUtil.randomSequence(NucleotideSequence.ALPHABET,
                    20, 40);
            final NucleotideSequenceCaseSensitive seq1cs = fromNucleotideSequence(seq1, true);
            final NSequenceWithQuality seq2q = new NSequenceWithQuality(seq2.toString());
            final NucleotideSequenceCaseSensitive seq2cs = fromNucleotideSequence(seq2, true);

            countExecutionTime("AlignGlobalOld", () -> {
                Aligner.alignGlobal(scoringOld, seq1, seq2);
                return null;
            });
            countExecutionTime("AlignGlobalNew", () -> {
                PatternAndTargetAligner.alignGlobal(scoringNew, seq1cs, seq2q);
                return null;
            });
        }

        System.out.println(timeCounter.get("AlignGlobalOld") + " " + timeCounter.get("AlignGlobalNew"));
    }
}
