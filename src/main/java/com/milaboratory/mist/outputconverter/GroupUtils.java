package com.milaboratory.mist.outputconverter;

import com.milaboratory.mist.pattern.MatchedGroupEdge;

import java.util.*;

final class GroupUtils {
    static String generateComments(TreeSet<FastqCommentGroup> commentGroups, boolean reverseMatch, String oldComments) {
        StringBuilder comments = new StringBuilder(oldComments);

        if (commentGroups.size() > 0) {
            if (comments.length() > 0)
                comments.append("~");
            commentGroups.forEach(cg -> comments.append(cg.getDescription()));
            comments.setLength(comments.length() - 1);  // trim last separator
        }

        if (reverseMatch) {
            if (comments.length() > 0)
                comments.append("~");
            comments.append("||~");
        }

        return comments.toString();
    }

    // TODO: implementation
    static String extractMistComments(String comments) {
        return comments;
    }

    static boolean parseReverseMatchFlag(String mistComments) {
        return mistComments.contains("||~");
    }

    // TODO: implementation
    static ArrayList<MatchedGroupEdge> parseGroupEdgesFromComments(List<String> mistComments) {
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
        return matchedGroupEdges;
    }
}
