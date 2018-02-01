package com.milaboratory.mist.outputconverter;

import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.mist.pattern.MatchedGroupEdge;

import java.util.*;

public final class GroupUtils {
    static String generateComments(List<MatchedGroup> groupsInsideMain, List<MatchedGroup> groupsNotInsideMain,
                                   List<GroupEdge> allGroupEdges, boolean reverseMatch, String oldComments) {
        return oldComments;
    }

    static boolean parseReverseMatchFlag(String comments) {
        return comments.contains("||~");
    }

    static ArrayList<MatchedGroupEdge> parseGroupEdgesFromComments(List<String> commentsList) {
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
        return matchedGroupEdges;
    }
}
