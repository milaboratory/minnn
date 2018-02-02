package com.milaboratory.mist.outputconverter;

import com.milaboratory.mist.pattern.MatchedGroupEdge;

import java.util.*;

public final class GroupUtils {
    static String generateComments(List<MatchedGroup> groupsInsideMain, List<MatchedGroup> groupsNotInsideMain,
                                   List<String> notMatchedGroupNames, boolean reverseMatch, String oldComments) {
        StringBuilder comments = new StringBuilder(oldComments);
        String nextSeparator = "~";
        if (reverseMatch) {
            if (comments.length() > 0)
                comments.append(nextSeparator);
            comments.append("||~");
            nextSeparator = "";
        }
        if (groupsInsideMain.size() > 0) {
            if (comments.length() > 0)
                comments.append(nextSeparator);
            nextSeparator = "|";
            groupsInsideMain.stream().map(MatchedGroupDescription::new)
                    .forEach(groupDescription -> comments.append(groupDescription.getDescription(true)));
            comments.setLength(comments.length() - 1);  // trim last separator
        }
        if (groupsNotInsideMain.size() > 0) {
            if (comments.length() > 0)
                comments.append(nextSeparator);
            nextSeparator = "|";
            groupsNotInsideMain.stream().map(MatchedGroupDescription::new)
                    .forEach(groupDescription -> comments.append(groupDescription.getDescription(false)));
            comments.setLength(comments.length() - 1);
        }
        if (notMatchedGroupNames.size() > 0) {
            if (comments.length() > 0)
                comments.append(nextSeparator);
            notMatchedGroupNames.forEach(groupName -> {
                comments.append(groupName);
                comments.append('|');
            });
            comments.setLength(comments.length() - 1);
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

    private static class MatchedGroupDescription {
        private final String groupName;
        private final String sequence;
        private final String quality;
        private final String from;
        private final String to;

        MatchedGroupDescription(MatchedGroup matchedGroup) {
            groupName = matchedGroup.getGroupName();
            sequence = matchedGroup.getValue().getSequence().toString();
            quality = matchedGroup.getValue().getQuality().toString().replaceAll("[{}~|]", "z");
            from = Integer.toString(matchedGroup.getRange().getLower());
            to = Integer.toString(matchedGroup.getRange().getUpper());
        }

        StringBuilder getDescription(boolean withCoordinates) {
            StringBuilder description = new StringBuilder();
            description.append(groupName);
            description.append('~');
            description.append(sequence);
            description.append('~');
            description.append(quality);
            if (withCoordinates) {
                description.append('{');
                description.append(from);
                description.append('~');
                description.append(to);
                description.append('}');
            }
            description.append('|');
            return description;
        }
    }
}
