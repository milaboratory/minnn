package com.milaboratory.mist.util;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mist.pattern.*;
import org.junit.Test;

import java.util.*;

import static com.milaboratory.mist.pattern.MatchValidationType.*;
import static com.milaboratory.mist.util.CommonTestTemplates.*;
import static com.milaboratory.mist.util.CommonTestUtils.countPortValues;
import static com.milaboratory.mist.util.CommonTestUtils.getTestPatternAligner;
import static org.junit.Assert.*;

public class SorterByScoreTest {
    @Test
    public void simplePredefinedMatchesTest() throws Exception {
        predefinedMatchesApproximateSorterTest(true, false);
    }

    @Test
    public void matchesWithMisplacedRangesTest() throws Exception {
        NSequenceWithQuality seq = new NSequenceWithQuality("AATTAAGGCAAAGTAAATTGAGCA");

        Match testMatch1 = new Match(1, 0, new ArrayList<MatchedItem>() {{
            add(new MatchedRange(seq, (byte)1, 0, new Range(0, 3))); }});
        Match testMatch2 = new Match(1, 0, new ArrayList<MatchedItem>() {{
            add(new MatchedRange(seq, (byte)1, 0, new Range(3, 7))); }});
        Match testMatch3 = new Match(1, 0, new ArrayList<MatchedItem>() {{
            add(new MatchedRange(seq, (byte)1, 0, new Range(9, 12))); }});
        Match testMatch4 = new Match(1, 0, new ArrayList<MatchedItem>() {{
            add(new MatchedRange(seq, (byte)1, 0, new Range(11, 15))); }});

        TestMatchesOutputPort testPort1 = new TestMatchesOutputPort(testMatch1, testMatch2, testMatch3, testMatch2, testMatch1);
        TestMatchesOutputPort testPort2 = new TestMatchesOutputPort(testMatch3, testMatch3, testMatch3, testMatch3, testMatch3);
        TestMatchesOutputPort testPort3 = new TestMatchesOutputPort(testMatch4, testMatch4, testMatch4, testMatch4, testMatch4);

        ApproximateSorter sorter1 = new SorterByScore(getTestPatternAligner(true), false,
                true, false, INTERSECTION);
        ApproximateSorter sorter2 = new SorterByScore(getTestPatternAligner(true), false,
                false, false, INTERSECTION);
        ApproximateSorter sorter3 = new SorterByScore(getTestPatternAligner(true), false,
                true, false, ORDER);
        ApproximateSorter sorter4 = new SorterByScore(getTestPatternAligner(true), false,
                false, false, ORDER);

        assertEquals(0, countPortValues(sorter1.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(testPort2.getCopy()); add(testPort3.getCopy()); }}))));
        assertEquals(0, countPortValues(sorter2.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(testPort2.getCopy()); add(testPort3.getCopy()); }}))));
        assertEquals(0, countPortValues(sorter3.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(testPort3.getCopy()); add(testPort1.getCopy()); }}))));
        assertEquals(0, countPortValues(sorter4.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(testPort3.getCopy()); add(testPort1.getCopy()); }}))));
    }

    @Test
    public void matchesWithNullValuesTest() throws Exception {
        NSequenceWithQuality seq = new NSequenceWithQuality("TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT");
        ArrayList<MatchedItem> testMatchedItemsSingle = new ArrayList<MatchedItem>() {{
            add(new MatchedRange(seq, (byte)1, 0, new Range(0, 40)));
            add(new MatchedGroupEdge(seq, (byte)1, 0, new GroupEdge("0", true), 0));
            add(new MatchedGroupEdge(seq, (byte)1, 0, new GroupEdge("0", false), 40));
        }};
        ArrayList<MatchedItem> testMatchedItemsMulti = new ArrayList<MatchedItem>() {{
            add(new MatchedRange(seq, (byte)1, 0, new Range(0, 40)));
            add(new MatchedGroupEdge(seq, (byte)1, 0, new GroupEdge("0", true), 0));
            add(new MatchedGroupEdge(seq, (byte)1, 0, new GroupEdge("0", false), 40));
            add(new MatchedRange(seq, (byte)1, 1, new Range(0, 40)));
            add(new MatchedGroupEdge(seq, (byte)1, 1, new GroupEdge("1", true), 0));
            add(new MatchedGroupEdge(seq, (byte)1, 1, new GroupEdge("1", false), 40));
        }};

        Match testMatchSingle = new Match(1, 0, testMatchedItemsSingle);
        Match testMatchMulti = new Match(2, 0, testMatchedItemsMulti);

        TestMatchesOutputPort testPortSingle = new TestMatchesOutputPort(testMatchSingle);
        TestMatchesOutputPort testPortMulti = new TestMatchesOutputPort(testMatchMulti);
        TestMatchesOutputPort testPortEmpty = new TestMatchesOutputPort();

        ArrayList<OutputPort<Match>> testPortsSingleWithNull1 = new ArrayList<OutputPort<Match>>() {{
            add(testPortSingle.getCopy()); add(testPortEmpty); }};
        ArrayList<OutputPort<Match>> testPortsSingleWithNull2 = new ArrayList<OutputPort<Match>>() {{
            add(testPortEmpty); add(testPortSingle.getCopy()); }};
        ArrayList<OutputPort<Match>> testPortsMultiWithNull1Copy1 = new ArrayList<OutputPort<Match>>() {{
            add(testPortMulti.getCopy()); add(testPortEmpty); }};
        ArrayList<OutputPort<Match>> testPortsMultiWithNull1Copy2 = new ArrayList<OutputPort<Match>>() {{
            add(testPortMulti.getCopy()); add(testPortEmpty); }};
        ArrayList<OutputPort<Match>> testPortsMultiWithNull2Copy1 = new ArrayList<OutputPort<Match>>() {{
            add(testPortEmpty); add(testPortMulti.getCopy()); }};
        ArrayList<OutputPort<Match>> testPortsMultiWithNull2Copy2 = new ArrayList<OutputPort<Match>>() {{
            add(testPortEmpty); add(testPortMulti.getCopy()); }};

        ApproximateSorter sorterSingle = new SorterByScore(getTestPatternAligner(), false,
                true, false, INTERSECTION);
        ApproximateSorter sorterMulti1 = new SorterByScore(getTestPatternAligner(), true,
                true, false, LOGICAL_AND);
        ApproximateSorter sorterMulti2 = new SorterByScore(getTestPatternAligner(), true,
                true, false, LOGICAL_OR);

        assertEquals(0,
                countPortValues(sorterSingle.getOutputPort(addInfiniteLimits(testPortsSingleWithNull1))));
        assertEquals(0,
                countPortValues(sorterSingle.getOutputPort(addInfiniteLimits(testPortsSingleWithNull2))));
        assertEquals(0,
                countPortValues(sorterMulti1.getOutputPort(addInfiniteLimits(testPortsMultiWithNull1Copy1))));
        assertEquals(0,
                countPortValues(sorterMulti1.getOutputPort(addInfiniteLimits(testPortsMultiWithNull2Copy1))));
        assertEquals(1,
                countPortValues(sorterMulti2.getOutputPort(addInfiniteLimits(testPortsMultiWithNull1Copy2))));
        assertEquals(1,
                countPortValues(sorterMulti2.getOutputPort(addInfiniteLimits(testPortsMultiWithNull2Copy2))));
    }

    @Test
    public void matchesFromOperatorsTest() throws Exception {
        FuzzyMatchPattern pattern = new FuzzyMatchPattern(getTestPatternAligner(), new NucleotideSequence("ATTAGACA"));
        NSequenceWithQuality seq = new NSequenceWithQuality("ACTGCGATAAATTAGACAGTACGTATTAGACATTATTATTAGACAGAGACA");

        ApproximateSorter sorterUnfair1 = new SorterByScore(getTestPatternAligner(), false,
                true, false, INTERSECTION);
        ApproximateSorter sorterUnfair2 = new SorterByScore(getTestPatternAligner(), false,
                true, false, ORDER);
        ApproximateSorter sorterFair1 = new SorterByScore(getTestPatternAligner(), false,
                true, true, INTERSECTION);
        ApproximateSorter sorterFair2 = new SorterByScore(getTestPatternAligner(), false,
                true, true, ORDER);

        assertEquals(3, countPortValues(pattern.match(seq).getMatches()));
        assertEquals(3, countPortValues(sorterUnfair1.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(pattern.match(seq).getMatches()); }}))));
        assertEquals(3, countPortValues(sorterUnfair2.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(pattern.match(seq).getMatches()); }}))));
        assertEquals(3, countPortValues(sorterFair1.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(pattern.match(seq).getMatches()); }}))));
        assertEquals(3, countPortValues(sorterFair2.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(pattern.match(seq).getMatches()); }}))));
        assertEquals(6, countPortValues(sorterUnfair1.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(pattern.match(seq).getMatches()); add(pattern.match(seq).getMatches()); }}))));
        assertEquals(3, countPortValues(sorterUnfair2.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(pattern.match(seq).getMatches()); add(pattern.match(seq).getMatches()); }}))));
        assertEquals(6, countPortValues(sorterFair1.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(pattern.match(seq).getMatches()); add(pattern.match(seq).getMatches()); }}))));
        assertEquals(3, countPortValues(sorterFair2.getOutputPort(addInfiniteLimits(
                new ArrayList<OutputPort<Match>>() {{
                    add(pattern.match(seq).getMatches()); add(pattern.match(seq).getMatches()); }}))));
    }

    @Test
    public void randomGeneratedMatchesTest() throws Exception {
        randomMatchesApproximateSorterTest(true, false);
    }

    @Test
    public void randomMatchesFromOperatorsTest() throws Exception {
        randomMatchesFromOperatorsApproximateSorterTest(true);
    }

    @Test
    public void fairSortingSimpleTest() throws Exception {
        predefinedMatchesApproximateSorterTest(true, true);
    }

    @Test
    public void fairSortingRandomTest() throws Exception {
        randomMatchesApproximateSorterTest(true, true);
    }
}
