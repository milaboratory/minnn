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

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.*;
import com.milaboratory.test.TestUtil;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.*;

import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class MultiPatternTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void mismatchedReadsAndPatternsTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(), false,
                new NucleotideSequenceCaseSensitive("attagaca"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(), false,
                new NucleotideSequenceCaseSensitive("gcgat"));
        MultiPattern multiPattern = createMultiPattern(getTestPatternAligner(), pattern1, pattern2);
        MultiNSequenceWithQuality mseq = createMultiNSeq("AT");
        exception.expect(IllegalArgumentException.class);
        multiPattern.match(mseq);
    }

    @Test
    public void simpleTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(), false,
                new NucleotideSequenceCaseSensitive("attagaca"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(), false,
                new NucleotideSequenceCaseSensitive("tattac"));
        AndPattern pattern3 = new AndPattern(getTestPatternAligner(), false,
                new FuzzyMatchPattern(getTestPatternAligner(), false,
                        new NucleotideSequenceCaseSensitive("at")),
                new FuzzyMatchPattern(getTestPatternAligner(), false,
                        new NucleotideSequenceCaseSensitive("atgc")));
        MultiPattern multiPattern = createMultiPattern(getTestPatternAligner(), pattern1, pattern2, pattern3);
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQualityImpl(
                new NSequenceWithQuality("ACAATTAGACA"),
                new NSequenceWithQuality("GTTATTACCA"),
                new NSequenceWithQuality("AACTTGCATGCAT"));
        assertTrue(multiPattern.match(mseq).isFound());
        assertEquals("ATGCAT", multiPattern.match(mseq)
                .getMatches().take().getMatchedRange(2).getValue().getSequence().toString());
    }

    @Test
    public void randomTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            int sequencesNum = rg.nextInt(10) + 1;
            NSequenceWithQuality[] sequences = new NSequenceWithQuality[sequencesNum];
            FuzzyMatchPattern[] patterns = new FuzzyMatchPattern[sequencesNum];
            boolean isMatching = true;
            for (int s = 0; s < sequencesNum; s++) {
                NucleotideSequence seq = TestUtil.randomSequence(
                        NucleotideSequence.ALPHABET, 1, 1000);
                NucleotideSequenceCaseSensitive motifSeq = TestUtil.randomSequence(
                        NucleotideSequenceCaseSensitive.ALPHABET, 1, 5);
                NSequenceWithQuality seqQ = new NSequenceWithQuality(seq.toString());
                sequences[s] = seqQ;
                patterns[s] = new FuzzyMatchPattern(getTestPatternAligner(), false, motifSeq);
                isMatching = isMatching && seq.toString().contains(motifSeq.toString().toUpperCase());
            }
            MultiNSequenceWithQuality mseq = new MultiNSequenceWithQualityImpl(sequences);
            MultiPattern multiPattern = createMultiPattern(getTestPatternAligner(), patterns);
            assertEquals(isMatching, multiPattern.match(mseq).isFound());
            assertEquals(isMatching, multiPattern.match(mseq).getBestMatch() != null);
            assertEquals(isMatching, multiPattern.match(mseq).getMatches(false).take() != null);
        }
    }

    @Test
    public void groupsTest() throws Exception {
        ArrayList<GroupEdgePosition> groups1 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("ABC", true), 1));
            add(new GroupEdgePosition(new GroupEdge("ABC", false), 3));
            add(new GroupEdgePosition(new GroupEdge("DEF", true), 6));
            add(new GroupEdgePosition(new GroupEdge("DEF", false), 7));
            add(new GroupEdgePosition(new GroupEdge("GH", true), 10));
            add(new GroupEdgePosition(new GroupEdge("GH", false), 11));
        }};

        ArrayList<GroupEdgePosition> groups2 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("XYZ", true), 1));
            add(new GroupEdgePosition(new GroupEdge("XYZ", false), 3));
        }};

        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(), false,
                new NucleotideSequenceCaseSensitive("ataggagggtagcc"), groups1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(), false,
                new NucleotideSequenceCaseSensitive("ttttcaatgcattag"), groups2);
        MultiPattern multiPattern = createMultiPattern(getTestPatternAligner(), pattern1, pattern2);
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQualityImpl(
                new NSequenceWithQuality("ATAGGAGGGTAGCCACAATTAGCCA"),
                new NSequenceWithQuality("GTGCATCTGCCATTTTCAATGCATTAG"));
        MatchingResult result = multiPattern.match(mseq);
        OutputPort<MatchIntermediate> matchOutputPort = result.getMatches();
        assertEquals("ABC", result.getBestMatch()
                .getMatchedGroupEdge("ABC", false).getGroupName());
        assertEquals(11, result.getBestMatch()
                .getMatchedGroupEdge("GH", false).getPosition());
        assertEquals(13, matchOutputPort.take()
                .getMatchedGroupEdge("XYZ", true).getPosition());
        assertNull(matchOutputPort.take());
    }

    @Test
    public void groupNamesTest1() throws Exception {
        ArrayList<GroupEdgePosition> groups1 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("ABC", true), 1));
            add(new GroupEdgePosition(new GroupEdge("ABC", false), 3));
            add(new GroupEdgePosition(new GroupEdge("DEF", true), 6));
            add(new GroupEdgePosition(new GroupEdge("DEF", false), 7));
            add(new GroupEdgePosition(new GroupEdge("GH", true), 9));
            add(new GroupEdgePosition(new GroupEdge("GH", false), 10));
        }};

        ArrayList<GroupEdgePosition> groups2 = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("GH", false), 11));
            add(new GroupEdgePosition(new GroupEdge("XYZ", true), 1));
            add(new GroupEdgePosition(new GroupEdge("XYZ", false), 3));
        }};

        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(), false,
                new NucleotideSequenceCaseSensitive("gtggttgtgttgt"), groups1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(), false,
                new NucleotideSequenceCaseSensitive("gtggttgtgttgt"), groups2);
        assertEquals(8,
                createMultiPattern(getTestPatternAligner(), pattern1, pattern2).getGroupEdges().size());
    }

    @Test
    public void groupNamesTest2() throws Exception {
        ArrayList<GroupEdgePosition> groups = new ArrayList<GroupEdgePosition>() {{
            add(new GroupEdgePosition(new GroupEdge("ABC", true), 1));
            add(new GroupEdgePosition(new GroupEdge("ABC", false), 3));
            add(new GroupEdgePosition(new GroupEdge("DEF", true), 6));
            add(new GroupEdgePosition(new GroupEdge("DEF", false), 7));
            add(new GroupEdgePosition(new GroupEdge("GH", true), 10));
            add(new GroupEdgePosition(new GroupEdge("GH", false), 11));
        }};

        FuzzyMatchPattern pattern = new FuzzyMatchPattern(getTestPatternAligner(), false,
                new NucleotideSequenceCaseSensitive("gtggttgtgttgt"), groups);
        assertEquals(6, createMultiPattern(getTestPatternAligner(), pattern, pattern).getGroupEdges().size());
    }

    @Test
    public void scoringRandomTest() throws Exception {
        for (int i = 0; i < 10000; i++) {
            NucleotideSequenceCaseSensitive[] motifs = new NucleotideSequenceCaseSensitive[2];
            motifs[0] = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET, 5, 50);
            motifs[1] = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET, 5, 50);
            MultiNSequenceWithQuality target = new MultiNSequenceWithQualityImpl(
                    new NSequenceWithQuality(motifs[0].toString()),
                    new NSequenceWithQuality(motifs[1].toString()));
            FuzzyMatchPattern pattern0 = new FuzzyMatchPattern(getTestPatternAligner(), false,
                    motifs[0]);
            FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(), false,
                    motifs[1]);
            MultiPattern multiPattern0 = createMultiPattern(getTestPatternAligner(), pattern0, pattern1);
            MultiPattern multiPattern1 = createMultiPattern(getTestPatternAligner(), pattern1, pattern0);
            assertEquals(pattern0.match(target.get(0)).getBestMatch().getScore()
                    + pattern1.match(target.get(1)).getBestMatch().getScore(),
                    multiPattern0.match(target).getBestMatch().getScore());
            if (!motifs[0].toString().equals(motifs[1].toString())) {
                Match multiPattern1Match = multiPattern1.match(target).getBestMatch();
                if (multiPattern1Match != null)
                    System.out.println("motifs: " + Arrays.toString(motifs) + ", multiPattern1: "
                            + multiPattern1.toString());
                assertNull(multiPattern1Match);
            }
        }
    }

    @Test
    public void wrongOperandTest() throws Exception {
        exception.expect(IllegalArgumentException.class);
        new MultiPattern(getTestPatternAligner(), false,
                new FuzzyMatchPattern(getTestPatternAligner(), false,
                        new NucleotideSequenceCaseSensitive("A")));
    }
}
