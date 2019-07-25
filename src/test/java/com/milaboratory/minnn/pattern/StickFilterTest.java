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
package com.milaboratory.minnn.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import com.milaboratory.test.TestUtil;
import org.junit.*;

import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class StickFilterTest {
    @Test
    public void randomTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            boolean left = rg.nextBoolean();
            int position = rg.nextInt(30);
            String seq = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 300).toString();
            NSequenceWithQuality target = new NSequenceWithQuality(seq);
            NucleotideSequenceCaseSensitive motif = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    1, 20);
            FuzzyMatchPattern pattern = new FuzzyMatchPattern(getTestPatternConfiguration(rg.nextInt(5)), motif);
            FilterPattern filterPattern = new FilterPattern(getTestPatternConfiguration(),
                    new StickFilter(left, position), pattern);
            MatchIntermediate patternBestMatch = pattern.match(target).getBestMatch(true);
            boolean mustMatch = (patternBestMatch != null)
                    && ((left && (patternBestMatch.getRange().getFrom() == position))
                    || (!left && (patternBestMatch.getRange().getTo() == position + 1)));

            assertTrue(!mustMatch || (filterPattern.match(target).getBestMatch(true) != null));
            assertTrue(countMatches(pattern.match(target), true)
                    >= countMatches(filterPattern.match(target), true));
            OutputPort<MatchIntermediate> filteredPort = filterPattern.match(target).getMatches(rg.nextBoolean());
            streamPort(filteredPort).forEach(match -> assertTrue(
                    (left && (match.getRange().getFrom() == position))
                            || (!left && (match.getRange().getTo() == position + 1))));
        }
    }
}
