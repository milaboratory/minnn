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
        return new BasePatternAligner(getRandomScoring(), -rg.nextInt(100), -rg.nextInt(10),
                rg.nextInt(4), rg.nextInt(4));
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
                -9, -10, -9,
                DEFAULT_GOOD_QUALITY, DEFAULT_BAD_QUALITY, -3);
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
