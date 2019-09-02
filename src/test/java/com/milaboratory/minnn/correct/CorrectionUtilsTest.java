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
package com.milaboratory.minnn.correct;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NSequenceWithQualityBuilder;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceWithQuality;
import com.milaboratory.minnn.util.ConsensusLetter;
import org.junit.*;

import java.util.*;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.correct.CorrectionUtils.*;
import static org.junit.Assert.*;

public class CorrectionUtilsTest {
    @Test
    public void mergeSequenceTest() {
        List<MergeTestData> testData = new ArrayList<>();

        MergeTestData data0 = new MergeTestData();
        data0.addSequence("AAA", "###");
        data0.addSequence("ATA", "3C3");
        data0.addSequence("GTC", "111");
        data0.setMergeAllResult("ATA", "8U8");
        data0.setMergeOneByOneResult("ATA", ">Z>");
        testData.add(data0);

        NSequenceWithQuality maxQualitySeq = new NSequenceWithQuality(new NucleotideSequence("ATTAGACA"),
                DEFAULT_MAX_QUALITY);
        MergeTestData data1 = new MergeTestData();
        data1.originalSequences.add(maxQualitySeq);
        data1.originalSequences.add(maxQualitySeq);
        data1.mergeAllResult = maxQualitySeq;
        data1.mergeOneByOneResult = maxQualitySeq;
        testData.add(data1);

        MergeTestData data2 = new MergeTestData();
        data2.addSequence("ATTAGACA");
        data2.addSequence("ATTAGACA");
        data2.addSequence("ATTAGACA");
        data2.mergeAllResult = maxQualitySeq;
        data2.mergeOneByOneResult = maxQualitySeq;
        testData.add(data2);

        MergeTestData data3 = new MergeTestData();
        data3.addSequence("WDNNNGWCCCAGTBAAAAGCA");
        data3.addSequence("ATTCCCGKNNNNNNNNNNNNN");
        data3.addSequence("NNNNNNAAAABBBGGGGGGTT");
        data3.setMergeAllResult(
                "ATTCCGAAAAAGTGAAAAGTA", "@5---!-!!!!555!!!![!!");
        data3.setMergeOneByOneResult(
                "ATTCCGAAAAAGTGGGGGGTT", "E:111$??00\"::?0000[00");
        testData.add(data3);

        NSequenceWithQuality testSeq = new NSequenceWithQuality("ATTAGACA", "12345678");
        MergeTestData data4 = new MergeTestData();
        data4.originalSequences.add(testSeq);
        data4.mergeAllResult = testSeq;
        data4.mergeOneByOneResult = testSeq;
        testData.add(data4);

        MergeTestData data5 = new MergeTestData();
        data5.addSequence("AHABATYANGNHTRAGNDGRADAGAADDA", "597349AA8FNW54#%^385703583476");
        data5.originalSequences.add(NSequenceWithQuality.EMPTY);
        data5.addSequence("BBABBHHYAANAAAABBBNNNNNA", "1597349A8F4#%^5703583476");
        data5.addSequence("RAGCDGRADAGAACDA", "FNW54#%^38570476");
        data5.addSequence("AHABATYABGBHTRAGBDGRADAGAABDA", "597349AA8FNW54#%^385703583476");
        data5.addSequence("ATA", ",[,");
        data5.setMergeAllResult(
                "ATACATTAAGGATAAGTTGAAAAGAATAA", "D\"G!\";![!(!#)HB!!!/!-!&$3)!!/");
        data5.setMergeOneByOneResult(
                "AAACATTAAAGATAAATAGAAAAGAAAAA", "['I-FQ2W2**Q-UW)'(I2H(D-JD'(G");
        testData.add(data5);

        MergeTestData data6 = new MergeTestData();
        data6.addSequence("ATA", "[[[");
        data6.addSequence("NTA", "###");
        data6.setMergeAllResult("ATA", "R[[");
        data6.setMergeOneByOneResult("ATA", "R[[");
        testData.add(data6);

        MergeTestData data7 = new MergeTestData();
        data7.addSequence("R", "4");
        data7.originalSequences.add(NSequenceWithQuality.EMPTY);
        data7.addSequence("A", "^");
        data7.addSequence("C", "4");
        data7.addSequence("R", "4");
        data7.originalSequences.add(NSequenceWithQuality.EMPTY);
        data7.setMergeAllResult("A", "H");
        data7.setMergeOneByOneResult("A", "U");
        testData.add(data7);

        for (MergeTestData currentTestData : testData) {
            int maxLength = currentTestData.originalSequences.stream().mapToInt(SequenceWithQuality::size).max()
                    .orElseThrow(RuntimeException::new);
            NSequenceWithQualityBuilder builder = new NSequenceWithQualityBuilder();
            for (int positionIndex = 0; positionIndex < maxLength; positionIndex++) {
                ConsensusLetter consensusLetter = new ConsensusLetter();
                for (NSequenceWithQuality currentSequence : currentTestData.originalSequences)
                    consensusLetter.addLetter((positionIndex >= currentSequence.size()) ? NSequenceWithQuality.EMPTY
                            : currentSequence.getRange(positionIndex, positionIndex + 1));
                builder.append(consensusLetter.calculateConsensusLetter());
            }
            NSequenceWithQuality mergeAllResult = builder.createAndDestroy();
            assertEquals(currentTestData.mergeAllResult, mergeAllResult);

            Iterator<NSequenceWithQuality> iterator = currentTestData.originalSequences.iterator();
            NSequenceWithQuality mergeResult = iterator.next();
            while (iterator.hasNext())
                mergeResult = mergeSequence(mergeResult, iterator.next());
            assertEquals(currentTestData.mergeOneByOneResult, mergeResult);
        }
    }

    private static class MergeTestData {
        final List<NSequenceWithQuality> originalSequences = new ArrayList<>();
        NSequenceWithQuality mergeAllResult = null;
        NSequenceWithQuality mergeOneByOneResult = null;

        void addSequence(String seq) {
            originalSequences.add(new NSequenceWithQuality(seq));
        }

        void addSequence(String seq, String qual) {
            originalSequences.add(new NSequenceWithQuality(seq, qual));
        }

        void setMergeAllResult(String seq, String qual) {
            mergeAllResult = new NSequenceWithQuality(seq, qual);
        }

        void setMergeOneByOneResult(String seq, String qual) {
            mergeOneByOneResult = new NSequenceWithQuality(seq, qual);
        }
    }
}
