package com.milaboratory.mist.output_converter;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.pattern.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;

import static com.milaboratory.mist.output_converter.GroupUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.RandomStringType.*;
import static org.junit.Assert.*;

public class GroupUtilsTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void getGroupsFromMatchTest() throws Exception {
        NSequenceWithQuality seqSingle = new NSequenceWithQuality("AATTAAGGCAAA");
        NSequenceWithQuality seqMulti1 = new NSequenceWithQuality("AATTAAGGCAAA");
        NSequenceWithQuality seqMulti2 = new NSequenceWithQuality("ATTAGACA");

        MatchedRange testMatchedRangeSingle = new MatchedRange(seqSingle, (byte)1, 0, new Range(6, 9));
        ArrayList<MatchedGroupEdge> testMatchedGroupEdgesSingle = new ArrayList<MatchedGroupEdge>() {{
            add(new MatchedGroupEdge(seqSingle, (byte)1, 0, new GroupEdge("0", true),
                    6));
            add(new MatchedGroupEdge(seqSingle, (byte)1, 0, new GroupEdge("0", false),
                    7));
            add(new MatchedGroupEdge(seqSingle, (byte)1, 0, new GroupEdge("1", true),
                    7));
            add(new MatchedGroupEdge(seqSingle, (byte)1, 0, new GroupEdge("1", false),
                    9));
        }};

        MatchedRange[] testMatchedRangesMulti = new MatchedRange[] {
                new MatchedRange(seqMulti1, (byte)1, 0, new Range(0, 9)),
                new MatchedRange(seqMulti2, (byte)1, 1, new Range(0, 8))
        };
        ArrayList<MatchedGroupEdge> testMatchedGroupEdgesMulti = new ArrayList<MatchedGroupEdge>() {{
            add(new MatchedGroupEdge(seqMulti1, (byte)1, 0, new GroupEdge("0", true),
                    1));
            add(new MatchedGroupEdge(seqMulti1, (byte)1, 0, new GroupEdge("0", false),
                    4));
            add(new MatchedGroupEdge(seqMulti1, (byte)1, 0, new GroupEdge("1", true),
                    4));
            add(new MatchedGroupEdge(seqMulti1, (byte)1, 0, new GroupEdge("1", false),
                    8));
            add(new MatchedGroupEdge(seqMulti2, (byte)1, 1, new GroupEdge("2", true),
                    0));
            add(new MatchedGroupEdge(seqMulti2, (byte)1, 1, new GroupEdge("2", false),
                    4));
            add(new MatchedGroupEdge(seqMulti2, (byte)1, 1, new GroupEdge("3", true),
                    5));
            add(new MatchedGroupEdge(seqMulti2, (byte)1, 1, new GroupEdge("3", false),
                    8));
        }};

        Match testMatchSingle = new Match(1, -5, -1, -1,
                testMatchedGroupEdgesSingle, testMatchedRangeSingle);
        Match testMatchMulti = new Match(2, -11, -1, -1,
                testMatchedGroupEdgesMulti, testMatchedRangesMulti);
        ArrayList<MatchedGroup> groupsSingle = getGroupsFromMatch(testMatchSingle);
        ArrayList<MatchedGroup> groupsMulti = getGroupsFromMatch(testMatchMulti);

        assertEquals(2, groupsSingle.size());
        assertEquals(4, groupsMulti.size());
        assertEquals("0", groupsSingle.get(0).getGroupName());
        assertEquals("1", groupsSingle.get(1).getGroupName());
        assertEquals("0", groupsMulti.get(0).getGroupName());
        assertEquals("1", groupsMulti.get(1).getGroupName());
        assertEquals("2", groupsMulti.get(2).getGroupName());
        assertEquals("3", groupsMulti.get(3).getGroupName());
        assertEquals(new Range(6, 7), groupsSingle.get(0).getRange());
        assertEquals(new Range(7, 9), groupsSingle.get(1).getRange());
        assertEquals(new Range(1, 4), groupsMulti.get(0).getRange());
        assertEquals(new Range(4, 8), groupsMulti.get(1).getRange());
        assertEquals(new Range(0, 4), groupsMulti.get(2).getRange());
        assertEquals(new Range(5, 8), groupsMulti.get(3).getRange());
    }

    @Test
    public void invalidGroupRangeTest() throws Exception {
        NSequenceWithQuality seq = new NSequenceWithQuality("A");
        MatchedRange testMatchedRange = new MatchedRange(seq, (byte)1, 0, new Range(0, 1));
        ArrayList<MatchedGroupEdge> testMatchedGroupEdges = new ArrayList<MatchedGroupEdge>() {{
            add(new MatchedGroupEdge(seq, (byte)1, 0, new GroupEdge("0", true),
                    0));
            add(new MatchedGroupEdge(seq, (byte)1, 0, new GroupEdge("0", false),
                    0));
        }};
        Match testMatch = new Match(1, 0, -1, -1,
                testMatchedGroupEdges, testMatchedRange);

        exception.expect(IllegalStateException.class);
        getGroupsFromMatch(testMatch);
    }

    @Test
    public void groupEdgesInDifferentPatternsTest() throws Exception {
        NSequenceWithQuality seq = new NSequenceWithQuality("AAA");
        MatchedRange[] testMatchedRanges = new MatchedRange[] {
                new MatchedRange(seq, (byte)1, 0, new Range(0, 1)),
                new MatchedRange(seq, (byte)1, 1, new Range(0, 2))
        };
        ArrayList<MatchedGroupEdge> testMatchedGroupEdges = new ArrayList<MatchedGroupEdge>() {{
            add(new MatchedGroupEdge(seq, (byte)1, 0, new GroupEdge("0", true),
                    0));
            add(new MatchedGroupEdge(seq, (byte)1, 1, new GroupEdge("0", false),
                    1));
        }};
        Match testMatch = new Match(2, 0, -1, -1,
                testMatchedGroupEdges, testMatchedRanges);

        exception.expect(IllegalStateException.class);
        getGroupsFromMatch(testMatch);
    }

    @Test
    public void groupsInsideMainTest() throws Exception {
        Range mainRange = new Range(1, 5);
        ArrayList<MatchedGroup> matchedGroups = new ArrayList<MatchedGroup>() {{
           add(generateMatchedGroup(new Range(0, 10), true));
           add(generateMatchedGroup(new Range(1, 5), true));
           add(generateMatchedGroup(new Range(0, 4), true));
           add(generateMatchedGroup(new Range(3, 8), true));
           add(generateMatchedGroup(new Range(2, 3), true));
        }};
        ArrayList<MatchedGroup> groupsInsideMain = getGroupsInsideMain(matchedGroups, mainRange, true);
        ArrayList<MatchedGroup> groupsNotInsideMain = getGroupsInsideMain(matchedGroups, mainRange, false);
        assertEquals(new Range(0, 10), groupsNotInsideMain.get(0).getRange());
        assertEquals(new Range(1, 5), groupsInsideMain.get(0).getRange());
        assertEquals(new Range(0, 4), groupsNotInsideMain.get(1).getRange());
        assertEquals(new Range(3, 8), groupsNotInsideMain.get(2).getRange());
        assertEquals(new Range(2, 3), groupsInsideMain.get(1).getRange());

        groupsNotInsideMain.forEach(g -> assertEquals(new Range(-1, -1), g.getRelativeRange()));
        assertEquals(new Range(0, 4), groupsInsideMain.get(0).getRelativeRange());
        assertEquals(new Range(1, 2), groupsInsideMain.get(1).getRelativeRange());
    }

    @Test
    public void groupsToReadDescriptionTest() throws Exception {
        Range mainRange = new Range(1, 5);
        ArrayList<MatchedGroup> matchedGroups = new ArrayList<MatchedGroup>() {{
            add(generateMatchedGroup(new Range(0, 10), false));
            add(generateMatchedGroup(new Range(1, 5), false));
            add(generateMatchedGroup(new Range(0, 4), false));
            add(generateMatchedGroup(new Range(3, 8), false));
            add(generateMatchedGroup(new Range(2, 3), false));
        }};
        String insideMain = groupsToReadDescription(getGroupsInsideMain(matchedGroups, mainRange, true),
                "MainGroup", true);
        String notInsideMain = groupsToReadDescription(getGroupsInsideMain(matchedGroups, mainRange, false),
                null, false);
        assertEquals("GroupName~TTAG~CCCC{MainGroup~0~4}|GroupName~T~C{MainGroup~1~2}",
                insideMain);
        assertEquals("GroupName~ATTAGACATT~CCCCCCCCCC|GroupName~ATTA~CCCC|GroupName~AGACA~CCCCC",
                notInsideMain);
    }

    private static MatchedGroup generateMatchedGroup(Range range, boolean random) {
        return new MatchedGroup(random ? getRandomString(rg.nextInt(30) + 1, "", LETTERS_AND_NUMBERS)
                : "GroupName", new NSequenceWithQuality("ATTAGACATT"), (byte)(random ? rg.nextInt(20) - 10 : 1),
                random ? rg.nextInt(10) : 2, range);
    }

    @Test
    public void descriptionForNotMatchedGroupsTest() throws Exception {
        ArrayList<GroupEdgePosition> groupEdgePositions1 = new ArrayList<>();
        groupEdgePositions1.add(new GroupEdgePosition(new GroupEdge("1-1", true), 1));
        groupEdgePositions1.add(new GroupEdgePosition(new GroupEdge("1-1", false), 2));
        groupEdgePositions1.add(new GroupEdgePosition(new GroupEdge("1-2", true), 1));
        groupEdgePositions1.add(new GroupEdgePosition(new GroupEdge("1-2", false), 3));

        ArrayList<GroupEdgePosition> groupEdgePositions2 = new ArrayList<>();
        groupEdgePositions2.add(new GroupEdgePosition(new GroupEdge("2-1", true), 0));
        groupEdgePositions2.add(new GroupEdgePosition(new GroupEdge("2-1", false), 2));
        groupEdgePositions2.add(new GroupEdgePosition(new GroupEdge("2-2", true), 0));
        groupEdgePositions2.add(new GroupEdgePosition(new GroupEdge("2-2", false), 3));

        PatternAligner patternAligner = getTestPatternAligner(0);
        NucleotideSequenceCaseSensitive seq1 = new NucleotideSequenceCaseSensitive("ATTAGACA");
        NucleotideSequenceCaseSensitive seq2 = new NucleotideSequenceCaseSensitive("CTCTCT");
        NSequenceWithQuality target1 = new NSequenceWithQuality("TATTAGACA");
        NSequenceWithQuality target2 = new NSequenceWithQuality("CTCTCTC");
        MultiNSequenceWithQualityImpl mTarget = new MultiNSequenceWithQualityImpl(target1, target2);
        FuzzyMatchPattern pattern1 = new FuzzyMatchPattern(patternAligner, seq1, groupEdgePositions1);
        FuzzyMatchPattern pattern2 = new FuzzyMatchPattern(patternAligner, seq2, groupEdgePositions2);
        FuzzyMatchPattern pattern1NoGroups = new FuzzyMatchPattern(patternAligner, seq1);
        FuzzyMatchPattern pattern2NoGroups = new FuzzyMatchPattern(patternAligner, seq2);
        AndPattern andPattern0 = new AndPattern(patternAligner, pattern1, pattern2);
        AndPattern andPattern1 = new AndPattern(patternAligner, pattern1, pattern1NoGroups);
        AndPattern andPattern2 = new AndPattern(patternAligner, pattern2, pattern2NoGroups);
        OrPattern orPattern0 = new OrPattern(patternAligner, pattern1, pattern2);
        OrPattern orPattern1 = new OrPattern(patternAligner, pattern1, andPattern0);
        OrPattern orPattern2 = new OrPattern(patternAligner, andPattern0, pattern2);
        OrPattern orPattern3 = new OrPattern(patternAligner, pattern1NoGroups, andPattern1);
        OrPattern orPattern4 = new OrPattern(patternAligner, andPattern2, pattern2NoGroups);
        MultiPattern multiPattern1 = new MultiPattern(patternAligner, pattern1, orPattern4);
        MultiPattern multiPattern2 = new MultiPattern(patternAligner, orPattern3, pattern2);
        AndOperator andOperator = new AndOperator(patternAligner, multiPattern1,
                new MultiPattern(patternAligner, pattern1NoGroups, pattern2NoGroups));
        OrOperator orOperator = new OrOperator(patternAligner, multiPattern1, multiPattern2);

        assertEquals("", getNotMatchedGroupsDescription(pattern1, target1, 0));
        assertEquals("", getNotMatchedGroupsDescription(pattern2, target2, 0));
        assertEquals("2-1~|2-2~", getNotMatchedGroupsDescription(orPattern0, target1, 0));
        assertEquals("2-1~|2-2~", getNotMatchedGroupsDescription(orPattern1, target1, 0));
        assertEquals("1-1~|1-2~", getNotMatchedGroupsDescription(orPattern0, target2, 0));
        assertEquals("1-1~|1-2~", getNotMatchedGroupsDescription(orPattern2, target2, 0));
        assertEquals("1-1~|1-2~", getNotMatchedGroupsDescription(orPattern3, target1, 0));
        assertEquals("2-1~|2-2~", getNotMatchedGroupsDescription(orPattern4, target2, 0));
        assertException(IllegalArgumentException.class, () -> {
            descriptionForNotMatchedGroups(orPattern0, 1, new ArrayList<>());
            return null;
        });
        assertEquals("", getNotMatchedGroupsDescription(multiPattern1, mTarget, 0));
        assertEquals("2-1~|2-2~", getNotMatchedGroupsDescription(multiPattern1, mTarget, 1));
        assertEquals("1-1~|1-2~", getNotMatchedGroupsDescription(multiPattern2, mTarget, 0));
        assertEquals("", getNotMatchedGroupsDescription(multiPattern2, mTarget, 1));
        assertException(ArrayIndexOutOfBoundsException.class, () -> {
            descriptionForNotMatchedGroups(multiPattern1, 2, new ArrayList<>());
            return null;
        });
        assertEquals("", getNotMatchedGroupsDescription(andOperator, mTarget, 0));
        assertEquals("2-1~|2-2~", getNotMatchedGroupsDescription(andOperator, mTarget, 1));
        assertEquals("", getNotMatchedGroupsDescription(orOperator, mTarget, 0));
        assertEquals("2-1~|2-2~", getNotMatchedGroupsDescription(orOperator, mTarget, 1));
    }

    private static String getNotMatchedGroupsDescription(Pattern pattern,
            MultiNSequenceWithQuality target, int patternIndex) {
        ArrayList<MatchedGroup> matchedGroups = getGroupsFromMatch(pattern.match(target).getBestMatch(true),
                patternIndex);
        return descriptionForNotMatchedGroups(pattern, patternIndex, matchedGroups);
    }
}
