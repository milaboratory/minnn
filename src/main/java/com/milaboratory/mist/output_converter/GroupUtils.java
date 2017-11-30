package com.milaboratory.mist.output_converter;

import com.milaboratory.core.Range;
import com.milaboratory.mist.pattern.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public final class GroupUtils {
    public static ArrayList<MatchedGroup> getGroupsFromMatch(Match match) {
        ArrayList<MatchedGroup> matchedGroups = new ArrayList<>();
        ArrayList<MatchedGroupEdge> matchedGroupEdges = match.getMatchedGroupEdges();
        MatchedGroupEdge endOfCurrentGroup;
        Range currentRange;

        for (MatchedGroupEdge matchedGroupEdge : matchedGroupEdges)
            if (matchedGroupEdge.isStart()) {
                endOfCurrentGroup = match.getMatchedGroupEdge(matchedGroupEdge.getGroupName(), false);
                if (matchedGroupEdge.getPosition() >= endOfCurrentGroup.getPosition())
                    throw new IllegalStateException("Group start must be lower than the end. Start: "
                        + matchedGroupEdge.getPosition() + ", end: " + endOfCurrentGroup.getPosition());
                if (matchedGroupEdge.getPatternIndex() != endOfCurrentGroup.getPatternIndex())
                    throw new IllegalStateException("Start and end of the group " + matchedGroupEdge.getGroupName()
                        + " have different pattern indexes (start: " + matchedGroupEdge.getPatternIndex() + ", end: "
                        + endOfCurrentGroup.getPatternIndex() + ")!");
                currentRange = new Range(matchedGroupEdge.getPosition(), endOfCurrentGroup.getPosition());
                matchedGroups.add(new MatchedGroup(matchedGroupEdge.getGroupName(), matchedGroupEdge.getTarget(),
                        matchedGroupEdge.getTargetId(), matchedGroupEdge.getPatternIndex(), currentRange));
            }

        return matchedGroups;
    }

    public static ArrayList<MatchedGroup> getGroupsFromMatch(Match match, int patternIndex) {
        return getGroupsFromMatch(match).stream().filter(g -> g.getPatternIndex() == patternIndex)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return only groups that are fully inside main group, or groups that are not fully inside main group, depending
     * on insideMain flag. For groups that are inside main, relative ranges will be calculated.
     *
     * @param groups groups list to filter and calculate relative ranges
     * @param mainRange range of main group
     * @param insideMain true if we need to get only groups that are inside main and calculate relative ranges,
     *                   false if we need to get only groups that are not inside main
     * @return filtered list of groups
     */
    public static ArrayList<MatchedGroup> getGroupsInsideMain(ArrayList<MatchedGroup> groups, Range mainRange,
                                                              boolean insideMain) {
        if (insideMain)
            return groups.stream()
                    .filter(g -> mainRange.contains(g.getRange()))
                    .map(g -> new MatchedGroup(g.getGroupName(), g.getTarget(), g.getTargetId(),
                            g.getPatternIndex(), g.getRange(), g.getRange().move(-mainRange.getLower())))
                    .collect(Collectors.toCollection(ArrayList::new));
        else
            return groups.stream()
                    .filter(g -> !mainRange.contains(g.getRange()))
                    .collect(Collectors.toCollection(ArrayList::new));
    }

    public static String groupsToReadDescription(ArrayList<MatchedGroup> groups, String mainGroupName,
                                                 boolean insideMain) {
        StringBuilder descriptionBuilder = new StringBuilder();
        for (int i = 0; i < groups.size(); i++) {
            MatchedGroup currentGroup = groups.get(i);
            descriptionBuilder.append(currentGroup.getGroupName());
            descriptionBuilder.append('~');
            descriptionBuilder.append(currentGroup.getValue().getSequence().toString());
            descriptionBuilder.append('~');
            descriptionBuilder.append(currentGroup.getValue().getQuality().toString().replaceAll("[{}~|]", "z"));
            if (insideMain) {
                descriptionBuilder.append('{');
                descriptionBuilder.append(mainGroupName);
                descriptionBuilder.append('~');
                descriptionBuilder.append(Integer.toString(currentGroup.getRelativeRange().getLower()));
                descriptionBuilder.append('~');
                descriptionBuilder.append(Integer.toString(currentGroup.getRelativeRange().getUpper()));
                descriptionBuilder.append('}');
            }
            if (i < groups.size() - 1)
                descriptionBuilder.append('~');
        }
        return descriptionBuilder.toString();
    }

    public static String descriptionForNotMatchedGroups(Pattern topLevelPattern, int patternIndex,
                                                        ArrayList<MatchedGroup> matchedGroups) {
        HashSet<String> matchedGroupNames = matchedGroups.stream().map(MatchedGroup::getGroupName)
                .collect(Collectors.toCollection(HashSet::new));
        HashSet<String> notMatchedGroupNames = topLevelPattern.getGroupEdges(patternIndex).stream()
                .map(GroupEdge::getGroupName).filter(gn -> !matchedGroupNames.contains(gn))
                .collect(Collectors.toCollection(HashSet::new));
        StringBuilder descriptionBuilder = new StringBuilder();
        for (String notMatchedGroupName : notMatchedGroupNames) {
            descriptionBuilder.append(notMatchedGroupName);
            descriptionBuilder.append("~~");
        }
        return descriptionBuilder.toString();
    }
}
