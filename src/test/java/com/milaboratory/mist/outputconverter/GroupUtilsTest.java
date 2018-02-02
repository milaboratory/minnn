package com.milaboratory.mist.outputconverter;

import com.milaboratory.core.Range;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.pattern.*;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.*;

import static com.milaboratory.mist.outputconverter.GroupUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.RandomStringType.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
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

        MatchIntermediate testMatchSingle = new MatchIntermediate(1, -5,
                -1, -1, testMatchedGroupEdgesSingle, testMatchedRangeSingle);
        MatchIntermediate testMatchMulti = new MatchIntermediate(2, -11,
                -1, -1, testMatchedGroupEdgesMulti, testMatchedRangesMulti);
        ArrayList<MatchedGroup> groupsSingle = testMatchSingle.getGroups();
        ArrayList<MatchedGroup> groupsMulti = testMatchMulti.getGroups();

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
        MatchIntermediate testMatch = new MatchIntermediate(1, 0,
                -1, -1, testMatchedGroupEdges, testMatchedRange);

        exception.expect(IllegalStateException.class);
        testMatch.getGroups();
    }

    @Test
    public void generateCommentsTest() throws Exception {
        ArrayList<MatchedGroup> groupsInsideMain = new ArrayList<MatchedGroup>() {{
            add(generateMatchedGroup(new Range(0, 10), false));
            add(generateMatchedGroup(new Range(1, 5), false));
            add(generateMatchedGroup(new Range(0, 4), false));
            add(generateMatchedGroup(new Range(3, 8), false));
            add(generateMatchedGroup(new Range(2, 3), false));
        }};
        ArrayList<MatchedGroup> groupsNotInsideMain = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            groupsNotInsideMain.add(generateMatchedGroup(new Range(i, i + 4), false));
        ArrayList<String> notMatchedGroupNames = new ArrayList<String>() {{
            add("Test1"); add("Test2"); add("Test3");
        }};

        String expectedInsideMain = "GroupName~ATTAGACATT~CCCCCCCCCC{0~10}|GroupName~TTAG~CCCC{1~5}|"
                + "GroupName~ATTA~CCCC{0~4}|GroupName~AGACA~CCCCC{3~8}|GroupName~T~C{2~3}";
        String expectedNotInsideMain = "GroupName~ATTA~CCCC|GroupName~TTAG~CCCC|GroupName~TAGA~CCCC";
        String expectedNotMatched = "Test1|Test2|Test3";

        assertEquals("ABC~||~" + expectedInsideMain + "|" + expectedNotInsideMain + "|" + expectedNotMatched,
                generateComments(groupsInsideMain, groupsNotInsideMain, notMatchedGroupNames,
                        true, "ABC"));
        assertEquals("||~" + expectedInsideMain + "|" + expectedNotInsideMain + "|" + expectedNotMatched,
                generateComments(groupsInsideMain, groupsNotInsideMain, notMatchedGroupNames,
                        true, ""));
        assertEquals("ABC~" + expectedInsideMain + "|" + expectedNotInsideMain + "|" + expectedNotMatched,
                generateComments(groupsInsideMain, groupsNotInsideMain, notMatchedGroupNames,
                        false, "ABC"));
        assertEquals(expectedInsideMain + "|" + expectedNotInsideMain + "|" + expectedNotMatched,
                generateComments(groupsInsideMain, groupsNotInsideMain, notMatchedGroupNames,
                        false, ""));
        assertEquals("ABC~||~" + expectedNotMatched, generateComments(new ArrayList<>(), new ArrayList<>(),
                notMatchedGroupNames, true, "ABC"));
        assertEquals("ABC~" + expectedInsideMain + "|" + expectedNotMatched,
                generateComments(groupsInsideMain, new ArrayList<>(), notMatchedGroupNames,
                        false, "ABC"));
        assertEquals(expectedNotInsideMain, generateComments(new ArrayList<>(), groupsNotInsideMain, new ArrayList<>(),
                false, ""));
        assertEquals("x~||~", generateComments(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                true, "x"));
        assertEquals("||~", generateComments(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                true, ""));
        assertEquals("x", generateComments(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                false, "x"));
        assertEquals("", generateComments(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                false, ""));
    }

    @Test
    public void generateCommentsFuzzingTest() throws Exception {
        for (int i = 0; i < 10000; i++) {
            ArrayList<MatchedGroup> groupsInsideMain = new ArrayList<>();
            ArrayList<MatchedGroup> groupsNotInsideMain = new ArrayList<>();
            ArrayList<String> notMatchedGroupNames = new ArrayList<>();
            for (int j = 0; j < rg.nextInt(15); j++)
                groupsInsideMain.add(generateMatchedGroup(new Range(rg.nextInt(3), rg.nextInt(7) + 1),
                        true));
            for (int j = 0; j < rg.nextInt(15); j++)
                groupsNotInsideMain.add(generateMatchedGroup(new Range(0, rg.nextInt(10) + 1), true));
            for (int j = 0; j < rg.nextInt(15); j++)
                notMatchedGroupNames.add(getRandomString(rg.nextInt(30) + 1));
            generateComments(groupsInsideMain, groupsNotInsideMain, notMatchedGroupNames, rg.nextBoolean(),
                    getRandomString(100) + 1);
        }
    }

    private static MatchedGroup generateMatchedGroup(Range range, boolean random) {
        return new MatchedGroup(random ? getRandomString(rg.nextInt(30) + 1, "", LETTERS_AND_NUMBERS)
                : "GroupName", new NSequenceWithQuality("ATTAGACATT"), (byte)(random ? rg.nextInt(20) - 10 : 1),
                range);
    }

    @Test
    public void commentsFromParsedReadTest() {
        NSequenceWithQuality read1Value = new NSequenceWithQuality("ATTAGCTTAGGACCT");
        NSequenceWithQuality read2Value = new NSequenceWithQuality("GTTAAATAAA");
        SingleRead read1 = new SingleReadImpl(10, read1Value, "abc");
        SingleRead read2 = new SingleReadImpl(10, read2Value, "123");
        PairedRead pairedRead = new PairedRead(read1, read2);
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<MatchedGroupEdge>() {{
            add(new MatchedGroupEdge(read1Value, (byte)0, new GroupEdge("R1", true), 0));
            add(new MatchedGroupEdge(read1Value, (byte)0, new GroupEdge("R1", false), 15));
            add(new MatchedGroupEdge(read2Value, (byte)1, new GroupEdge("R2", true), 0));
            add(new MatchedGroupEdge(read2Value, (byte)1, new GroupEdge("R2", false), 10));
            add(new MatchedGroupEdge(read1Value, (byte)0, new GroupEdge("G1", true), 3));
            add(new MatchedGroupEdge(read1Value, (byte)0, new GroupEdge("G1", false), 6));
            add(new MatchedGroupEdge(read1Value, (byte)0, new GroupEdge("G2", true), 1));
            add(new MatchedGroupEdge(read1Value, (byte)0, new GroupEdge("G2", false), 9));
            add(new MatchedGroupEdge(read2Value, (byte)1, new GroupEdge("G3", true), 1));
            add(new MatchedGroupEdge(read2Value, (byte)1, new GroupEdge("G3", false), 8));
        }};
        Match match = new Match(2, 0, matchedGroupEdges);
        ParsedRead parsedRead = new ParsedRead(pairedRead, true, match);
        ArrayList<GroupEdge> allGroupEdges = new ArrayList<GroupEdge>() {{
            for (int i = 1; i <= 5; i++) {
                add(new GroupEdge("G" + i, true));
                add(new GroupEdge("G" + i, false));
            }
        }};
        SequenceRead convertedRead = parsedRead.toSequenceRead(true, false, allGroupEdges,
                "G2", "G1", "G3");

        assertEquals(5, convertedRead.numberOfReads());
        assertEquals("abc~||~G1~AGC~CCC{3~6}|G2~TTAGCTTA~CCCCCCCC{1~9}|G3~TTAAATA~CCCCCCC|G4|G5",
                convertedRead.getRead(0).getDescription());
        assertEquals("123~||~G3~TTAAATA~CCCCCCC{1~8}|G1~AGC~CCC|G2~TTAGCTTA~CCCCCCCC|G4|G5",
                convertedRead.getRead(1).getDescription());
        assertEquals("abc~||~G1~AGC~CCC{2~5}|G2~TTAGCTTA~CCCCCCCC{0~8}|G3~TTAAATA~CCCCCCC|G4|G5",
                convertedRead.getRead(2).getDescription());
        assertEquals("abc~||~G1~AGC~CCC{0~3}|G2~TTAGCTTA~CCCCCCCC|G3~TTAAATA~CCCCCCC|G4|G5",
                convertedRead.getRead(3).getDescription());
        assertEquals("123~||~G3~TTAAATA~CCCCCCC{0~7}|G1~AGC~CCC|G2~TTAGCTTA~CCCCCCCC|G4|G5",
                convertedRead.getRead(4).getDescription());
    }
}
