package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;

import static com.milaboratory.mist.pattern.GroupUtils.getGroupsFromMatch;
import static org.junit.Assert.*;

public class GroupUtilsTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void getGroupsFromMatchTest() throws Exception {
        NSequenceWithQuality seqSingle = new NSequenceWithQuality("AATTAAGGCAAA");
        NSequenceWithQuality seqMulti1 = new NSequenceWithQuality("AATTAAGGCAAA");
        NSequenceWithQuality seqMulti2 = new NSequenceWithQuality("ATTAGACA");

        ArrayList<MatchedItem> testMatchedItemsSingle = new ArrayList<MatchedItem>() {{
            add(new MatchedRange(seqSingle, (byte)1, 0, new Range(6, 9)));
            add(new MatchedGroupEdge(seqSingle, (byte)1, 0, new GroupEdge("0", true), 6));
            add(new MatchedGroupEdge(seqSingle, (byte)1, 0, new GroupEdge("0", false), 7));
            add(new MatchedGroupEdge(seqSingle, (byte)1, 0, new GroupEdge("1", true), 7));
            add(new MatchedGroupEdge(seqSingle, (byte)1, 0, new GroupEdge("1", false), 9));
        }};

        ArrayList<MatchedItem> testMatchedItemsMulti = new ArrayList<MatchedItem>() {{
            add(new MatchedRange(seqMulti1, (byte)1, 0, new Range(0, 9)));
            add(new MatchedGroupEdge(seqMulti1, (byte)1, 0, new GroupEdge("0", true), 1));
            add(new MatchedGroupEdge(seqMulti1, (byte)1, 0, new GroupEdge("0", false), 4));
            add(new MatchedGroupEdge(seqMulti1, (byte)1, 0, new GroupEdge("1", true), 4));
            add(new MatchedGroupEdge(seqMulti1, (byte)1, 0, new GroupEdge("1", false), 8));
            add(new MatchedRange(seqMulti2, (byte)1, 1, new Range(0, 8)));
            add(new MatchedGroupEdge(seqMulti2, (byte)1, 1, new GroupEdge("2", true), 0));
            add(new MatchedGroupEdge(seqMulti2, (byte)1, 1, new GroupEdge("2", false), 4));
            add(new MatchedGroupEdge(seqMulti2, (byte)1, 1, new GroupEdge("3", true), 5));
            add(new MatchedGroupEdge(seqMulti2, (byte)1, 1, new GroupEdge("3", false), 8));
        }};

        Match testMatchSingle = new Match(1, -5.0f, testMatchedItemsSingle);
        Match testMatchMulti = new Match(2, -11.0f, testMatchedItemsMulti);
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
        ArrayList<MatchedItem> testMatchedItems = new ArrayList<MatchedItem>() {{
            add(new MatchedRange(seq, (byte)1, 0, new Range(0, 1)));
            add(new MatchedGroupEdge(seq, (byte)1, 0, new GroupEdge("0", true), 0));
            add(new MatchedGroupEdge(seq, (byte)1, 0, new GroupEdge("0", false), 0));
        }};
        Match testMatch = new Match(1, 0, testMatchedItems);

        exception.expect(IllegalStateException.class);
        getGroupsFromMatch(testMatch);
    }

    @Test
    public void groupEdgesInDifferentPatternsTest() throws Exception {
        NSequenceWithQuality seq = new NSequenceWithQuality("AAA");
        ArrayList<MatchedItem> testMatchedItems = new ArrayList<MatchedItem>() {{
            add(new MatchedRange(seq, (byte)1, 0, new Range(0, 1)));
            add(new MatchedRange(seq, (byte)1, 1, new Range(0, 2)));
            add(new MatchedGroupEdge(seq, (byte)1, 0, new GroupEdge("0", true), 0));
            add(new MatchedGroupEdge(seq, (byte)1, 1, new GroupEdge("0", false), 1));
        }};
        Match testMatch = new Match(2, 0, testMatchedItems);

        exception.expect(IllegalStateException.class);
        getGroupsFromMatch(testMatch);
    }
}