package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceQuality;
import com.milaboratory.test.TestUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Random;

import static com.milaboratory.mist.pattern.Match.COMMON_GROUP_NAME_PREFIX;
import static org.junit.Assert.*;

public class MultiPatternTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void mismatchedReadsAndPatternsTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(new NucleotideSequence("ATTAGACA"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(new NucleotideSequence("GCGAT"));
        MultiPattern multiPattern = new MultiPattern(pattern1, pattern2);
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQuality() {
            @Override
            public int numberOfSequences() {
                return 1;
            }

            @Override
            public NSequenceWithQuality get(int id) {
                return new NSequenceWithQuality("AT");
            }
        };

        exception.expect(IllegalStateException.class);
        multiPattern.match(mseq);
    }

    @Test
    public void mismatchedReadsAndRangesTest() throws Exception {
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(new NucleotideSequence("ATTAGACA"));
        MultiPattern multiPattern = new MultiPattern(pattern);
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQuality() {
            @Override
            public int numberOfSequences() {
                return 3;
            }

            @Override
            public NSequenceWithQuality get(int id) {
                return new NSequenceWithQuality("AT");
            }
        };

        exception.expect(IllegalStateException.class);
        multiPattern.match(mseq, new Range(0, 2), new Range(2, 3));
    }

    @Test
    public void mismatchedReadsAndComplementsTest1() throws Exception {
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(new NucleotideSequence("ATTAGACA"));
        MultiPattern multiPattern = new MultiPattern(pattern);
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQuality() {
            @Override
            public int numberOfSequences() {
                return 1;
            }

            @Override
            public NSequenceWithQuality get(int id) {
                return new NSequenceWithQuality("AT");
            }
        };

        exception.expect(IllegalStateException.class);
        multiPattern.match(mseq, new Range[]{new Range(0, 2)}, new boolean[]{false, false});
    }

    @Test
    public void mismatchedReadsAndComplementsTest2() throws Exception {
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(new NucleotideSequence("ATTAGACA"));
        MultiPattern multiPattern = new MultiPattern(pattern);
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQuality() {
            @Override
            public int numberOfSequences() {
                return 3;
            }

            @Override
            public NSequenceWithQuality get(int id) {
                return new NSequenceWithQuality("AT");
            }
        };

        exception.expect(IllegalStateException.class);
        multiPattern.match(mseq, false, false);
    }

    @Test
    public void simpleTest() throws Exception {
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(new NucleotideSequence("ATTAGACA"));
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(new NucleotideSequence("GTTATTACCA"));
        AndPattern pattern3 = new AndPattern(new FuzzyMatchPattern(new NucleotideSequence("AT")),
                new FuzzyMatchPattern(new NucleotideSequence("GCAT")));
        MultiPattern multiPattern = new MultiPattern(pattern1, pattern2, pattern3);
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQuality() {
            @Override
            public int numberOfSequences() {
                return 3;
            }

            @Override
            public NSequenceWithQuality get(int id) {
                switch (id) {
                    case 0:
                        return new NSequenceWithQuality("ACAATTAGACA");
                    case 1:
                        return new NSequenceWithQuality("GTTATTACCA").getReverseComplement();
                    case 2:
                        return new NSequenceWithQuality("AACTTGCATAT").getReverseComplement();
                }
                return null;
            }
        };
        assertTrue(multiPattern.match(mseq, new Range[]{new Range(0, 11),
                new Range(0, 10, true), new Range(0, 11, true)},
                new boolean[]{false, true, true}).isFound());
        assertTrue(multiPattern.match(mseq, false, true, true).isFound());
        assertFalse(multiPattern.match(mseq, new Range[]{new Range(0, 11),
                        new Range(1, 10, true), new Range(0, 11, true)},
                new boolean[]{false, true, true}).isFound());
        assertFalse(multiPattern.match(mseq, false, true, false).isFound());
        assertEquals("GCATAT", multiPattern.match(mseq, false, true, true)
                .getMatches().take().getWholePatternMatch(2).getValue().getSequence().toString());
        assertNull(multiPattern.match(mseq).getBestMatch());
        assertNotNull(multiPattern.match(mseq, false, true, true).getBestMatch());
        assertTrue(multiPattern.match(mseq, new Range[]{new Range(0, 11),
                new Range(0, 10, true), new Range(0, 11, true)}).isFound());
    }

    @Test
    public void randomTest() throws Exception {
        int its = TestUtil.its(500, 1000);
        for (int i = 0; i < its; ++i) {
            int sequencesNum = new Random().nextInt(9) + 1;
            NSequenceWithQuality[] sequences = new NSequenceWithQuality[sequencesNum];
            FuzzyMatchPattern[] patterns = new FuzzyMatchPattern[sequencesNum];
            boolean isMatching = true;
            for (int s = 0; s < sequencesNum; s++) {
                NucleotideSequence seq = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 1000);
                NucleotideSequence motifSeq = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 5);
                NSequenceWithQuality seqQ = new NSequenceWithQuality(seq, SequenceQuality
                        .getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, seq.getSequence().size()));
                sequences[s] = seqQ;
                patterns[s] = new FuzzyMatchPattern(motifSeq);
                isMatching = isMatching && seq.toString().contains(motifSeq.toString());
            }
            MultiNSequenceWithQuality mseq = new MultiNSequenceWithQuality() {
                @Override
                public int numberOfSequences() {
                    return sequencesNum;
                }

                @Override
                public NSequenceWithQuality get(int id) {
                    return sequences[id];
                }
            };
            MultiPattern multiPattern = new MultiPattern(patterns);
            assertEquals(isMatching, multiPattern.match(mseq).isFound());
        }
    }

    @Test
    public void groupsTest() throws Exception {
        HashMap<String, Range> groups1 = new HashMap<String, Range>() {{
            put("1", new Range(0, 1));
            put("2", new Range(1, 3));
            put("4", new Range(4, 5));
        }};
        HashMap<String, Range> groups2 = new HashMap<String, Range>() {{
            put("3", new Range(1, 3));
            put("5", new Range(5, 6));
        }};

        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(new NucleotideSequence("TAGCC"), groups1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(new NucleotideSequence("CAGATGCA"), groups2);
        MultiPattern multiPattern = new MultiPattern(pattern1, pattern2);
        MultiNSequenceWithQuality mseq = new MultiNSequenceWithQuality() {
            @Override
            public int numberOfSequences() {
                return 2;
            }

            @Override
            public NSequenceWithQuality get(int id) {
                switch (id) {
                    case 0:
                        return new NSequenceWithQuality("ACAATTAGCCA");
                    case 1:
                        return new NSequenceWithQuality("GTGCATCTGCCA");
                }
                return null;
            }
        };
        MatchingResult result = multiPattern.match(mseq, false, true);
        assertEquals("AG", result.getMatches(false).take().getGroupMatches(true)
                .get(COMMON_GROUP_NAME_PREFIX + "2").getValue().getSequence().toString());
        assertEquals(new Range(8, 9), result.getMatches(true).take().getGroupMatches(true)
                .get(COMMON_GROUP_NAME_PREFIX + "5").getRange());
        assertEquals("AG", result.getBestMatch().getGroupMatches(true)
                .get(COMMON_GROUP_NAME_PREFIX + "3").getValue().getSequence().toString());
        assertNull(result.getMatches().take());
    }

    @Test
    public void groupNamesTest1() throws Exception {
        HashMap<String, Range> groups1 = new HashMap<String, Range>() {{
            put("ABC", new Range(1, 3));
            put("DEF", new Range(6, 7));
            put("GH", new Range(10, 11));
        }};
        HashMap<String, Range> groups2 = new HashMap<String, Range>() {{
            put("XYZ", new Range(1, 3));
            put("GH", new Range(9, 10));
        }};
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(new NucleotideSequence("GTGGTTGTGTTGT"), groups1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(new NucleotideSequence("GTGGTTGTGTTGT"), groups2);
        exception.expect(IllegalStateException.class);
        new MultiPattern(pattern1, pattern2);
    }

    @Test
    public void groupNamesTest2() throws Exception {
        HashMap<String, Range> groups = new HashMap<String, Range>() {{
            put("ABC", new Range(1, 3));
            put("DEF", new Range(6, 7));
            put("GH", new Range(10, 11));
        }};
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(new NucleotideSequence("GTGGTTGTGTTGT"), groups);
        exception.expect(IllegalStateException.class);
        new MultiPattern(pattern, pattern);
    }
}