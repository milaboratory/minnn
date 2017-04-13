package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.motif.BitapMatcher;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceQuality;
import com.milaboratory.core.sequence.SequencesUtils;
import com.milaboratory.test.TestUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.milaboratory.mist.pattern.Match.COMMON_GROUP_NAME_PREFIX;
import static com.milaboratory.mist.pattern.Match.WHOLE_PATTERN_MATCH_GROUP_NAME_PREFIX;
import static org.junit.Assert.*;

public class FuzzyMatchPatternTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void bestMatchTest() throws Exception {
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(new NSequenceWithQuality("ATTAGACA"));
        NSequenceWithQuality nseq = new NSequenceWithQuality("ACTGCGATAAATTAGACAGTACGTA");
        ArrayList<MatchingResult> results = new ArrayList<>(Arrays.asList(
                pattern.match(nseq, 1, 19, (byte)0),
                pattern.match(nseq, 10, 18, (byte)0),
                pattern.match(nseq, 10, 18, (byte)0),
                pattern.match(nseq, new Range(10, 18)),
                pattern.match(nseq)
        ));
        Range expectedRange = new Range(10, 18);
        for (MatchingResult result : results) {
            assertEquals(expectedRange.getLower(), result.getBestMatch().getWholePatternMatch().getRange().getLower());
            assertEquals(expectedRange.getUpper(), result.getBestMatch().getWholePatternMatch().getRange().getUpper());
            assertEquals(new NSequenceWithQuality("ATTAGACA"), result.getBestMatch().getWholePatternMatch().getValue());
            assertEquals(nseq, result.getBestMatch().getWholePatternMatch().getTarget());
            assertEquals(true, result.isFound());
            assertEquals(1, result.getMatchesNumber());
            assertEquals(true, result.getBestMatch().isFound());
            assertEquals(1, result.getBestMatch().getNumberOfPatterns());
            assertEquals(result.getBestMatch(), result.getMatches().take());
            Range rangeFromGroupMatches = result.getBestMatch().getGroupMatches(false)
                    .get(WHOLE_PATTERN_MATCH_GROUP_NAME_PREFIX + "0").getRange();
            assertEquals(expectedRange, rangeFromGroupMatches);
            assertEquals(null, result.getBestMatch().getGroupMatches(false)
                    .get(COMMON_GROUP_NAME_PREFIX + "0"));
        }
    }

    @Test
    public void noMatchesTest() throws Exception {
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(new NSequenceWithQuality("ATTAGACA"));
        NSequenceWithQuality nseq1 = new NSequenceWithQuality("ACTGCGATAAATTAGACAGTACGTA");
        NSequenceWithQuality nseq2 = new NSequenceWithQuality("ACTGCGATAAATTACACAGTACGTA");
        ArrayList<MatchingResult> results = new ArrayList<>(Arrays.asList(
                pattern.match(nseq1, 11, 19, (byte)0),
                pattern.match(nseq1, 10, 17, (byte)0),
                pattern.match(nseq2)
        ));
        for (MatchingResult result : results) {
            assertEquals(null, result.getBestMatch());
            assertEquals(null, result.getMatches().take());
            assertEquals(false, result.isFound());
            assertEquals(0, result.getMatchesNumber());
        }
    }

    @Test
    public void quickMatchTest() throws Exception {
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(new NSequenceWithQuality("ATTAGACA"));
        NSequenceWithQuality nseq = new NSequenceWithQuality("ACTGCGATAAATTAGACAGTACGTA");
        MatchingResult result = pattern.match(nseq, 1, 19, (byte)0);
        assertEquals(true, result.isFound());
        result = pattern.match(nseq, 1, 17, (byte)0);
        assertEquals(false, result.isFound());
        result = pattern.match(nseq, 11, 20, (byte)0);
        assertEquals(false, result.isFound());
        pattern = new FuzzyMatchPattern(new NSequenceWithQuality("ATTTTACA"));
        result = pattern.match(nseq, 1, 19, (byte)0);
        assertEquals(false, result.isFound());
        assertEquals(0, result.getMatchesNumber());
    }

    @Test
    public void randomMatchTest() throws Exception {
        int its = TestUtil.its(1000, 100000);
        for (int i = 0; i < its; ++i) {
            NucleotideSequence seqM = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 10, 60);
            NucleotideSequence seqL = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 0, 40);
            NucleotideSequence seqR = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 0, 40);
            NucleotideSequence fullSeq = SequencesUtils.concatenate(seqL, seqM, seqR);
            NSequenceWithQuality seqWithQuality = new NSequenceWithQuality(seqM,
                    SequenceQuality.getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, seqM.getSequence().size()));
            NSequenceWithQuality target = new NSequenceWithQuality(fullSeq,
                    SequenceQuality.getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, fullSeq.getSequence().size()));
            FuzzyMatchPattern pattern = new FuzzyMatchPattern(seqWithQuality);
            assertEquals(true, pattern.match(target).isFound());
        }
    }

    @Test
    public void randomTest() throws Exception {
        int its = TestUtil.its(1000, 10000);
        for (int i = 0; i < its; ++i) {
            NucleotideSequence target = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 1000);
            NucleotideSequence motif = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 50);
            NSequenceWithQuality motifQ = new NSequenceWithQuality(motif,
                    SequenceQuality.getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, motif.getSequence().size()));
            NSequenceWithQuality targetQ = new NSequenceWithQuality(target,
                    SequenceQuality.getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, target.getSequence().size()));
            FuzzyMatchPattern pattern = new FuzzyMatchPattern(motifQ);
            boolean isMatching = target.toString().contains(motif.toString());
            assertEquals(isMatching, pattern.match(targetQ).isFound());
        }
    }

    @Test
    public void multipleMatchesTest() throws Exception {
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(new NSequenceWithQuality("ATTAGACA"));
        NSequenceWithQuality nseq = new NSequenceWithQuality("ACTGCGATAAATTAGACATTAGACATTAGACAGTACGTATTAGACA");
        MatchingResult result = pattern.match(nseq);
        Match bestMatch1 = result.getBestMatch();
        Match firstMatchByScore = result.getMatches(true).take();
        Match bestMatch2 = result.getBestMatch();
        Match firstMatchByCoordinate = result.getMatches(false).take();
        Match bestMatch3 = result.getBestMatch();
        assertEquals(bestMatch1, bestMatch2);
        assertEquals(bestMatch1, bestMatch3);
        assertEquals(bestMatch1, firstMatchByScore);
        assertEquals(bestMatch1, firstMatchByCoordinate);
        assertEquals(true, result.isFound());
        assertEquals(4, result.getMatchesNumber());
        result = pattern.match(nseq);
        OutputPort<Match> matches = result.getMatches(false);
        assertEquals(10, matches.take().getWholePatternMatch().getRange().getLower());
        assertEquals("ATTAGACA", matches.take().getWholePatternMatch().getValue().getSequence().toString());
        assertEquals(24, matches.take().getGroupMatches(false)
                .get(WHOLE_PATTERN_MATCH_GROUP_NAME_PREFIX + "0").getRange().getLower());
        assertEquals(46, matches.take().getWholePatternMatch(0).getRange().getUpper());
    }

    @Test
    public void matchesIntersectionTest() throws Exception {
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(new NSequenceWithQuality("ATA"));
        NSequenceWithQuality nseq = new NSequenceWithQuality("ATATATTAAATATATATTTAATATATAAT");
        OutputPort<Match> matches = pattern.match(nseq).getMatches();
        assertEquals(new Range(0, 3), matches.take().getWholePatternMatch().getRange());
        assertEquals(new Range(2, 5), matches.take().getWholePatternMatch().getRange());
        assertEquals(new Range(9, 12), matches.take().getWholePatternMatch().getRange());
        assertEquals(new Range(11, 14), matches.take().getWholePatternMatch().getRange());
        assertEquals(new Range(13, 16), matches.take().getWholePatternMatch().getRange());
        assertEquals(new Range(20, 23), matches.take().getWholePatternMatch().getRange());
        assertEquals(new Range(22, 25), matches.take().getWholePatternMatch().getRange());
        assertEquals(new Range(24, 27), matches.take().getWholePatternMatch().getRange());
        assertEquals(null, matches.take());
    }

    @Test
    public void groupsTest() throws Exception {
        HashMap<String, Range> groups = new HashMap<String, Range>() {{
            put("ABC", new Range(1, 3));
            put("DEF", new Range(6, 7));
            put("GH", new Range(10, 11));
        }};
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(new NSequenceWithQuality("GTGGTTGTGTTGT"), groups);
        NSequenceWithQuality nseq = new NSequenceWithQuality("GTGTTGTGGTTGTGTTGTTGTGGTTGTGTTGTGG");
        MatchingResult result = pattern.match(nseq);
        assertEquals("G", result.getBestMatch().getGroupMatches(true)
                .get(COMMON_GROUP_NAME_PREFIX + "DEF").getValue().getSequence().toString());
        assertEquals("TG", result.getMatches().take().getGroupMatches(true)
                .get(COMMON_GROUP_NAME_PREFIX + "ABC").getValue().getSequence().toString());
        assertEquals(new Range(29, 30), result.getMatches().take().getGroupMatches(true)
                .get(COMMON_GROUP_NAME_PREFIX + "GH").getRange());
        assertNull(result.getMatches().take());

        exception.expect(IllegalStateException.class);
        new FuzzyMatchPattern(new NSequenceWithQuality("GGTGTGTCAC"), groups);
    }

    @Test
    public void masksTest() throws Exception {
        int its = TestUtil.its(1000, 10000);
        for (int i = 0; i < its; ++i) {
            NucleotideSequence target = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 1000);
            NucleotideSequence motif = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 50, false);
            NSequenceWithQuality motifQ = new NSequenceWithQuality(motif,
                    SequenceQuality.getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, motif.getSequence().size()));
            NSequenceWithQuality targetQ = new NSequenceWithQuality(target,
                    SequenceQuality.getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, target.getSequence().size()));
            FuzzyMatchPattern pattern = new FuzzyMatchPattern(motifQ, 0);
            BitapMatcher matcher = motif.toMotif().getBitapPattern().exactMatcher(target.getSequence(), 0, target.size());
            boolean isMatching = (matcher.findNext() != -1);
            assertEquals(isMatching, pattern.match(targetQ).isFound());
        }
    }

    @Test
    public void simpleAlignmentTest() throws Exception {
        FuzzyMatchPattern[] patterns = {
            new FuzzyMatchPattern(new NSequenceWithQuality("ATTAGACA"), 0),
            new FuzzyMatchPattern(new NSequenceWithQuality("ATTAGACA"), 1),
            new FuzzyMatchPattern(new NSequenceWithQuality("ATTAGACA"), 2)
        };
        NSequenceWithQuality[] sequences = {
            new NSequenceWithQuality("ATTAGTTA"),
            new NSequenceWithQuality("ATTAGAAG"),
            new NSequenceWithQuality("ATTAGGACA"),
            new NSequenceWithQuality("ACAGACA"),
            new NSequenceWithQuality("ATTTAGAA")
        };

        MatchingResult[][] matchingResults = new MatchingResult[3][5];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 5; j++)
                matchingResults[i][j] = patterns[i].match(sequences[j]);

        for (int j = 0; j < 5; j++)
            assertNull(matchingResults[0][j].getBestMatch());

        assertNull(matchingResults[1][0].getBestMatch());
        assertEquals(new NSequenceWithQuality("ATTAGAA"), matchingResults[1][1].getBestMatch().getWholePatternMatch().getValue());
        assertEquals(new NSequenceWithQuality("ATTAGGACA"), matchingResults[1][2].getBestMatch().getWholePatternMatch().getValue());
        assertNull(matchingResults[1][3].getBestMatch());
        assertNull(matchingResults[1][4].getBestMatch());

        assertEquals(new NSequenceWithQuality("ATTAGTTA"), matchingResults[2][0].getBestMatch().getWholePatternMatch().getValue());
        assertEquals(new NSequenceWithQuality("ATTAGA"), matchingResults[2][1].getBestMatch().getWholePatternMatch().getValue());
        assertEquals(new NSequenceWithQuality("ATTAGGACA"), matchingResults[2][2].getBestMatch().getWholePatternMatch().getValue());
        assertEquals(new NSequenceWithQuality("AGACA"), matchingResults[2][3].getBestMatch().getWholePatternMatch().getValue());
        assertEquals(new NSequenceWithQuality("TTAGAA"), matchingResults[2][4].getBestMatch().getWholePatternMatch().getValue());
    }
}
