package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceQuality;
import com.milaboratory.test.TestUtil;
import org.junit.Test;

import static com.milaboratory.mist.pattern.MatchUtils.countMatches;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class ScoreFilterTest {
    @Test
    public void randomTest() throws Exception {
        int its = TestUtil.its(1000, 2000);
        for (int i = 0; i < its; ++i) {
            int scoreThreshold = -rg.nextInt(100);
            NucleotideSequence target = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 1000);
            NucleotideSequence motif = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 10);
            NSequenceWithQuality targetQ = new NSequenceWithQuality(target,
                    SequenceQuality.getUniformQuality(SequenceQuality.GOOD_QUALITY_VALUE, target.getSequence().size()));
            FuzzyMatchPattern pattern = new FuzzyMatchPattern(getTestPatternAligner(rg.nextInt(5)), motif);
            FilterPattern filterPattern = new FilterPattern(getTestPatternAligner(), new ScoreFilter(scoreThreshold), pattern);
            MultiPattern multiPattern = new MultiPattern(getTestPatternAligner(), pattern, filterPattern);
            MultipleReadsFilterPattern mFilterPattern = new MultipleReadsFilterPattern(getTestPatternAligner(),
                    new ScoreFilter(scoreThreshold * 2), multiPattern);
            AndOperator andOperator = new AndOperator(getTestPatternAligner(), multiPattern, mFilterPattern);
            boolean isMatching = pattern.match(targetQ).isFound()
                    && (pattern.match(targetQ).getBestMatch(true).getScore() >= scoreThreshold);

            assertEquals(isMatching, filterPattern.match(targetQ).isFound());
            assertEquals(isMatching, andOperator.match(new MultiNSequenceWithQuality() {
                @Override
                public int numberOfSequences() {
                    return 2;
                }
                @Override
                public NSequenceWithQuality get(int id) {
                    return targetQ;
                }}).isFound());
            assertTrue(countMatches(pattern.match(targetQ)) >= countMatches(filterPattern.match(targetQ)));
            Match currentMatch;
            for (OutputPort<Match> filteredPort = filterPattern.match(targetQ).getMatches(
                    rg.nextBoolean(), rg.nextBoolean()); (currentMatch = filteredPort.take()) != null;)
                assertTrue(currentMatch.getScore() >= scoreThreshold);
        }
    }
}
