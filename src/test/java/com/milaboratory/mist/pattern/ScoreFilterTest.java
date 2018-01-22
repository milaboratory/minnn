package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import com.milaboratory.test.TestUtil;
import org.junit.Test;

import static com.milaboratory.mist.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class ScoreFilterTest {
    @Test
    public void randomTest() throws Exception {
        for (int i = 0; i < 1000; i++) {
            int scoreThreshold = -rg.nextInt(100);
            String seq = TestUtil.randomSequence(NucleotideSequence.ALPHABET, 1, 300).toString();
            NSequenceWithQuality target = new NSequenceWithQuality(seq);
            MultiNSequenceWithQuality multiTarget = createMultiNSeq(seq, 2);
            NucleotideSequenceCaseSensitive motif = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                    1, 20);
            FuzzyMatchPattern pattern = new FuzzyMatchPattern(getTestPatternAligner(rg.nextInt(5)), motif);
            FilterPattern filterPattern = new FilterPattern(getTestPatternAligner(),
                    new ScoreFilter(scoreThreshold), pattern);
            MultiPattern multiPattern = createMultiPattern(getTestPatternAligner(), pattern, filterPattern);
            MultipleReadsFilterPattern mFilterPattern = new MultipleReadsFilterPattern(getTestPatternAligner(),
                    new ScoreFilter(scoreThreshold * 2), multiPattern);
            AndOperator andOperator = new AndOperator(getTestPatternAligner(), multiPattern, mFilterPattern);
            Match patternBestMatch = pattern.match(target).getBestMatch(true);
            boolean isMatching = (patternBestMatch != null) && (patternBestMatch.getScore() >= scoreThreshold);

            assertEquals(isMatching, filterPattern.match(target).getBestMatch(true) != null);
            if (countMatches(multiPattern.match(multiTarget), true) < 300)
                assertEquals(isMatching, andOperator.match(multiTarget).getBestMatch(true) != null);
            assertTrue(countMatches(pattern.match(target), true)
                    >= countMatches(filterPattern.match(target), true));
            OutputPort<MatchIntermediate> filteredPort = filterPattern.match(target).getMatches(rg.nextBoolean());
            streamPort(filteredPort).forEach(match -> assertTrue(match.getScore() >= scoreThreshold));
        }
    }
}
