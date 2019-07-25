/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
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

import com.milaboratory.core.alignment.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.test.TestUtil;
import org.junit.*;

import static com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive.fromNucleotideSequence;
import static com.milaboratory.minnn.util.DebugUtils.*;
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

    @Ignore
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

    @Ignore
    @Test
    public void alignerProfilingTest() throws Exception {
        final LinearGapAlignmentScoring<NucleotideSequence> scoringOld = new LinearGapAlignmentScoring<>(
                NucleotideSequence.ALPHABET, 0, -9, -10);
        final PatternAndTargetAlignmentScoring scoringNew = new PatternAndTargetAlignmentScoring(
                0, -9, -10, -9,
                (byte)34, (byte)0, 0);
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
