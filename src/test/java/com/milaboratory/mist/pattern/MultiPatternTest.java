package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.*;
import com.milaboratory.test.TestUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;

import static com.milaboratory.mist.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class MultiPatternTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void mismatchedReadsAndPatternsTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("attagaca"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("gcgat"));
        MultiPattern multiPattern = new MultiPattern(getTestPatternAligner(), pattern1, pattern2);
        MultiNSequenceWithQuality mseq = createMultiNSeq("AT");
        exception.expect(IllegalArgumentException.class);
        multiPattern.match(mseq);
    }

    @Test
    public void simpleTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("attagaca"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("tattac"));
        AndPattern pattern3 = new AndPattern(getTestPatternAligner(),
                new FuzzyMatchPattern(getTestPatternAligner(), new NucleotideSequenceCaseSensitive("at")),
                new FuzzyMatchPattern(getTestPatternAligner(), new NucleotideSequenceCaseSensitive("atgc")));
        MultiPattern multiPattern = new MultiPattern(getTestPatternAligner(), pattern1, pattern2, pattern3);
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
                patterns[s] = new FuzzyMatchPattern(getTestPatternAligner(), motifSeq);
                isMatching = isMatching && seq.toString().contains(motifSeq.toString().toUpperCase());
            }
            MultiNSequenceWithQuality mseq = new MultiNSequenceWithQualityImpl(sequences);
            MultiPattern multiPattern = new MultiPattern(getTestPatternAligner(), patterns);
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

        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("ataggagggtagcc"), groups1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("ttttcaatgcattag"), groups2);
        MultiPattern multiPattern = new MultiPattern(getTestPatternAligner(), pattern1, pattern2);
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQualityImpl(
                new NSequenceWithQuality("ATAGGAGGGTAGCCACAATTAGCCA"),
                new NSequenceWithQuality("GTGCATCTGCCATTTTCAATGCATTAG"));
        MatchingResult result = multiPattern.match(mseq);
        OutputPort<Match> matchOutputPort = result.getMatches();
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

        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("gtggttgtgttgt"), groups1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("gtggttgtgttgt"), groups2);
        exception.expect(IllegalStateException.class);
        new MultiPattern(getTestPatternAligner(), pattern1, pattern2);
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

        FuzzyMatchPattern pattern = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("gtggttgtgttgt"), groups);
        exception.expect(IllegalStateException.class);
        new MultiPattern(getTestPatternAligner(), pattern, pattern);
    }

    @Test
    public void scoringRandomTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            NucleotideSequenceCaseSensitive motifs[] = new NucleotideSequenceCaseSensitive[2];
            motifs[0] = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET, 5, 50);
            motifs[1] = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET, 5, 50);
            MultiNSequenceWithQuality target = new MultiNSequenceWithQualityImpl(
                    new NSequenceWithQuality(motifs[0].toString()),
                    new NSequenceWithQuality(motifs[1].toString()));
            FuzzyMatchPattern pattern0 = new FuzzyMatchPattern(getTestPatternAligner(), motifs[0]);
            FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(getTestPatternAligner(), motifs[1]);
            MultiPattern multiPattern0 = new MultiPattern(getTestPatternAligner(), pattern0, pattern1);
            MultiPattern multiPattern1 = new MultiPattern(getTestPatternAligner(), pattern1, pattern0);
            assertEquals(pattern0.match(target.get(0)).getBestMatch().getScore()
                    + pattern1.match(target.get(1)).getBestMatch().getScore(),
                    multiPattern0.match(target).getBestMatch().getScore());
            if (!motifs[0].toString().equals(motifs[1].toString()))
                assertNull(multiPattern1.match(target).getBestMatch());
        }
    }
}
