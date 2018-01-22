package com.milaboratory.mist.parser;

import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import com.milaboratory.mist.pattern.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.milaboratory.mist.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class TokenizedStringTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void simpleTest1() throws Exception {
        TokenizedString ts1 = new TokenizedString("AATTAA");
        FuzzyMatchPattern testPattern1 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("aa"));
        FuzzyMatchPattern testPattern2 = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("tt"));
        assertEquals("AATTAA", ts1.getOneString());
        ts1.tokenizeSubstring(testPattern1, 0, 2);
        assertEquals("TTAA", ts1.getOneString(2, 6));
        assertEquals(2, ts1.getTokens(0, 6).size());
        ts1.tokenizeSubstring(testPattern2, 2, 4);
        assertEquals("A", ts1.getTokens(1, 5).get(1).getString());
        ts1.tokenizeSubstring(testPattern1, 0, 6);
        assertNotNull(ts1.getFinalPattern());
        TokenizedString ts2 = new TokenizedString("ATTAGACA");
        ts2.tokenizeSubstring(testPattern2, 1, 3);
        assertEquals(3, ts2.calculateLength(0, 2));
        assertEquals("AGACA", ts2.getTokens(0, 8).get(2).getString());
        exception.expect(IllegalArgumentException.class);
        ts2.tokenizeSubstring(testPattern1, 1, 2);
    }

    @Test
    public void simpleTest2() throws Exception {
        FuzzyMatchPattern fuzzyMatchPattern = new FuzzyMatchPattern(getTestPatternAligner(),
                new NucleotideSequenceCaseSensitive("atttgtg"));
        AndPattern andPattern = new AndPattern(getTestPatternAligner(), fuzzyMatchPattern, fuzzyMatchPattern);
        ScoreFilter scoreFilter = new ScoreFilter(-1);
        FilterPattern filterPattern = new FilterPattern(getTestPatternAligner(), scoreFilter, fuzzyMatchPattern);
        MultiPattern multiPattern = createMultiPattern(getTestPatternAligner(), andPattern, filterPattern);
        TokenizedString ts = new TokenizedString(multiPattern.toString());
        ts.tokenizeSubstring(fuzzyMatchPattern, 5, 10);
        assertEquals(3, ts.getTokens(1, 20).size());
        assertEquals("Multi", ts.getTokens(0, 20).get(0).getString());
        assertEquals("lti", ts.getTokens(2, 30).get(0).getString());
        exception.expect(IllegalArgumentException.class);
        ts.tokenizeSubstring(andPattern, 7, 12);
    }
}
