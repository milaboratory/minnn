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
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.minnn.util.ConsensusLetter;
import org.junit.*;

import java.util.*;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.correct.CorrectionUtils.*;
import static org.junit.Assert.*;

public class CorrectionUtilsTest {
    @Test
    public void mergeSequenceTest() {
        LinkedHashMap<List<NSequenceWithQuality>, NSequenceWithQuality> testData = new LinkedHashMap<>();
        testData.put(Arrays.asList(
                new NSequenceWithQuality("AAA", "###"),
                new NSequenceWithQuality("ATA", "3C3"),
                new NSequenceWithQuality("GTC", "111")),
                new NSequenceWithQuality("ATA", "8U8"));
        testData.put(Arrays.asList(
                new NSequenceWithQuality(new NucleotideSequence("ATTAGACA"), DEFAULT_MAX_QUALITY),
                new NSequenceWithQuality(new NucleotideSequence("ATTAGACA"), DEFAULT_MAX_QUALITY)),
                new NSequenceWithQuality(new NucleotideSequence("ATTAGACA"), DEFAULT_MAX_QUALITY));
        testData.put(Arrays.asList(
                new NSequenceWithQuality("WDNNNGWCCCAGTBAAAAGCA"),
                new NSequenceWithQuality("ATTCCCGKNNNNNNNNNNNNN"),
                new NSequenceWithQuality("NNNNNNAAAABBBGGGGGGTT")),
                new NSequenceWithQuality("ATTCCGAAAAAGTGAAAAGTA", "@5---!-!!!!555!!!![!!"));
        testData.put(Collections.singletonList(
                new NSequenceWithQuality("ATTAGACA", "12345678")),
                new NSequenceWithQuality("ATTAGACA", "12345678"));
        testData.put(Arrays.asList(
                new NSequenceWithQuality("AHABATYANGNHTRAGNDGRADAGAADDA",
                        "597349AA8FNW54#%^385703583476"),
                NSequenceWithQuality.EMPTY,
                new NSequenceWithQuality("BBABBHHYAANAAAABBBNNNNNA",
                        "1597349A8F4#%^5703583476"),
                new NSequenceWithQuality("RAGCDGRADAGAACDA",
                        "FNW54#%^38570476"),
                new NSequenceWithQuality("AHABATYABGBHTRAGBDGRADAGAABDA",
                        "597349AA8FNW54#%^385703583476"),
                new NSequenceWithQuality("ATA", ",[,")),
                new NSequenceWithQuality("ATACATTAAGGATAAGTTGAAAAGAATAA",
                        "D\"G!\";![!(!#)EB!!!/!-!&$3)!!/"));
        testData.put(Arrays.asList(
                new NSequenceWithQuality("ATA", "[[["),
                new NSequenceWithQuality("NTA", "###")),
                new NSequenceWithQuality("ATA", "R[["));
        for (HashMap.Entry<List<NSequenceWithQuality>, NSequenceWithQuality> currentTestData : testData.entrySet()) {
            Iterator<NSequenceWithQuality> iterator = currentTestData.getKey().iterator();
            NSequenceWithQuality mergeResult = iterator.next();
            int count = 1;
            while (iterator.hasNext())
                mergeResult = mergeSequence(mergeResult, count++, iterator.next());
            assertEquals(currentTestData.getValue(), mergeResult);
        }
    }
}
