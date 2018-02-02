package com.milaboratory.mist.outputconverter;

import com.milaboratory.core.Range;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.io.IO;
import com.milaboratory.mist.pattern.*;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.annotations.Serializable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.mist.outputconverter.GroupUtils.*;

@Serializable(by = IO.ParsedReadSerializer.class)
public final class ParsedRead {
    private final SequenceRead originalRead;
    private final boolean reverseMatch;
    private final Match bestMatch;
    private LinkedHashSet<String> defaultGroupNames = null;
    private Map<String, MatchedGroup> matchedGroups = null;
    private HashMap<String, ArrayList<GroupEdgePosition>> innerGroupEdgesCache = null;
    private HashMap<String, String> commentsCache = null;

    public ParsedRead(SequenceRead originalRead, boolean reverseMatch, Match bestMatch) {
        this.originalRead = originalRead;
        this.reverseMatch = reverseMatch;
        this.bestMatch = bestMatch;
    }

    public SequenceRead getOriginalRead() {
        return originalRead;
    }

    public boolean isReverseMatch() {
        return reverseMatch;
    }

    public Match getBestMatch() {
        return bestMatch;
    }

    public ArrayList<MatchedGroup> getGroups() {
        if (bestMatch == null)
            return new ArrayList<>();
        else
            return bestMatch.getGroups();
    }

    public long getBestMatchScore() {
        return (bestMatch == null) ? Long.MIN_VALUE : bestMatch.getScore();
    }

    private LinkedHashSet<String> getDefaultGroupNames() {
        if (defaultGroupNames == null)
            defaultGroupNames = IntStream.rangeClosed(1, originalRead.numberOfReads())
                    .mapToObj(n -> "R" + n).collect(Collectors.toCollection(LinkedHashSet::new));
        return new LinkedHashSet<>(defaultGroupNames);
    }

    private boolean defaultGroupsOverride(String... groupNames) {
        return Arrays.stream(groupNames).anyMatch(gn -> getDefaultGroupNames().contains(gn));
    }

    /**
     * Calculate and remember group edge positions for each group that is inside another group, and border group
     * edge positions (0 and target.size) for each group. This function must be called only when matchedGroups map
     * is already initialized.
     */
    private void fillGroupEdgesCache() {
        innerGroupEdgesCache = new HashMap<>();
        for (String groupName : matchedGroups.keySet()) {
            MatchedGroup outerGroup = matchedGroups.get(groupName);
            List<MatchedGroup> sameTargetGroups = getGroups().stream()
                    .filter(mg -> mg.getTargetId() == outerGroup.getTargetId()).collect(Collectors.toList());
            Range outerRange = outerGroup.getRange();
            ArrayList<GroupEdgePosition> groupEdgePositions = new ArrayList<>();
            for (MatchedGroup innerGroup : sameTargetGroups) {
                Range innerRange = innerGroup.getRange();
                if (outerRange.contains(innerRange)) {
                    groupEdgePositions.add(new GroupEdgePosition(new GroupEdge(innerGroup.getGroupName(), true),
                            innerRange.getLower() - outerRange.getLower()));
                    groupEdgePositions.add(new GroupEdgePosition(new GroupEdge(innerGroup.getGroupName(), false),
                            innerRange.getUpper() - outerRange.getLower()));
                }
            }
            innerGroupEdgesCache.put(groupName, groupEdgePositions);
        }
    }

    public ParsedRead retarget(boolean noDefaultGroups, String... groupNames) {
        LinkedHashSet<String> outputGroupNames = noDefaultGroups || defaultGroupsOverride(groupNames)
                ? new LinkedHashSet<>() : getDefaultGroupNames();
        outputGroupNames.addAll(Arrays.asList(groupNames));

        Match targetMatch;
        if (outputGroupNames.size() == 0)
            throw new IllegalArgumentException("Basic groups for output parsed read are not specified!");
        else {
            ArrayList<String> outputGroupNamesList = new ArrayList<>(outputGroupNames);
            ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
            if (matchedGroups == null)
                matchedGroups = getGroups().stream().collect(Collectors.toMap(MatchedGroup::getGroupName, mg -> mg));
            if (innerGroupEdgesCache == null)
                fillGroupEdgesCache();

            for (byte i = 0; i < outputGroupNamesList.size(); i++) {
                String outputGroupName = outputGroupNamesList.get(i);
                if (!matchedGroups.containsKey(outputGroupName))
                    throw new IllegalArgumentException("Group " + outputGroupName
                            + " not found in this ParsedRead; available groups: " + matchedGroups.keySet());
                NSequenceWithQuality target = bestMatch.getGroupValue(outputGroupName);
                for (GroupEdgePosition groupEdgePosition : innerGroupEdgesCache.get(outputGroupName))
                    matchedGroupEdges.add(new MatchedGroupEdge(target, i, groupEdgePosition.getGroupEdge(),
                            groupEdgePosition.getPosition()));
            }

            targetMatch = new Match(outputGroupNames.size(), bestMatch.getScore(), matchedGroupEdges);
        }

        return new ParsedRead(originalRead, reverseMatch, targetMatch);
    }

    public SequenceRead toSequenceRead(boolean copyOldComments, boolean noDefaultGroups,
                                       ArrayList<GroupEdge> allGroupEdges, String... groupNames) {
        ArrayList<SingleRead> singleReads = new ArrayList<>();
        LinkedHashSet<String> outputGroupNames = noDefaultGroups || defaultGroupsOverride(groupNames)
                ? new LinkedHashSet<>() : getDefaultGroupNames();
        outputGroupNames.addAll(Arrays.asList(groupNames));

        if (outputGroupNames.size() == 0)
            throw new IllegalArgumentException("Basic groups for output sequence read are not specified!");
        else {
            if (commentsCache == null)
                commentsCache = new HashMap<>();
            if (matchedGroups == null)
                matchedGroups = getGroups().stream().collect(Collectors.toMap(MatchedGroup::getGroupName, mg -> mg));
            if (innerGroupEdgesCache == null)
                fillGroupEdgesCache();

            HashSet<String> excludedGroups = defaultGroupsOverride(groupNames) ? new HashSet<>()
                    : new HashSet<>(getDefaultGroupNames());
            for (String outputGroupName : outputGroupNames) {
                if (!matchedGroups.containsKey(outputGroupName))
                    throw new IllegalArgumentException("Group " + outputGroupName
                            + " not found in this ParsedRead; available groups: " + matchedGroups.keySet());
                singleReads.add(new SingleReadImpl(originalRead.getId(), bestMatch.getGroupValue(outputGroupName),
                        generateReadDescription(copyOldComments, excludedGroups, allGroupEdges, outputGroupName)));
            }
        }

        switch (singleReads.size()) {
            case 1:
                return singleReads.get(0);
            case 2:
                return new PairedRead(singleReads.get(0), singleReads.get(1));
            default:
                return new MultiRead(singleReads.toArray(new SingleRead[singleReads.size()]));
        }
    }

    public static ParsedRead fromSequenceRead(SequenceRead sequenceRead) {
        ArrayList<String> mistComments = new ArrayList<>();
        sequenceRead.iterator()
                .forEachRemaining(singleRead -> mistComments.add(extractMistComments(singleRead.getDescription())));
        Match targetMatch = new Match(sequenceRead.numberOfReads(), 0, parseGroupEdgesFromComments(mistComments));
        return new ParsedRead(sequenceRead, parseReverseMatchFlag(mistComments.get(0)), targetMatch);
    }

    private String generateReadDescription(boolean copyOldComments, HashSet<String> excludedGroupNames,
                                           ArrayList<GroupEdge> allGroupEdges, String outputGroupName) {
        if (commentsCache.containsKey(outputGroupName))
            return commentsCache.get(outputGroupName);

        String oldComments;
        if (copyOldComments) {
            LinkedHashSet<String> defaultGroupNames = getDefaultGroupNames();
            String readNumberStr;
            if (defaultGroupNames.contains(outputGroupName))
                readNumberStr = outputGroupName.substring(1);
            else
                readNumberStr = defaultGroupNames.stream()
                        .filter(defaultGroupName -> innerGroupEdgesCache.containsKey(defaultGroupName))
                        .filter(defaultGroupName -> innerGroupEdgesCache.get(defaultGroupName).stream()
                                .anyMatch(groupEdgePosition -> groupEdgePosition.getGroupEdge()
                                        .getGroupName().equals(outputGroupName)))
                        .findFirst().orElse("R0").substring(1);
            if (readNumberStr.equals("0"))
                oldComments = "";
            else
                oldComments = originalRead.getRead(Integer.parseInt(readNumberStr) - 1).getDescription();
        } else
            oldComments = "";

        Set<String> matchedGroupNames = getGroups().stream().map(MatchedGroup::getGroupName)
                .collect(Collectors.toSet());
        matchedGroupNames.removeAll(excludedGroupNames);
        HashSet<String> groupNamesInsideMain = new HashSet<>();
        ArrayList<MatchedGroup> groupsInsideMain = new ArrayList<>();
        ArrayList<GroupEdgePosition> groupEdgePositionsInsideMain = innerGroupEdgesCache.get(outputGroupName);
        byte outputGroupId = matchedGroups.get(outputGroupName).getTargetId();
        for (GroupEdgePosition groupEdgePosition : groupEdgePositionsInsideMain) {
            String currentGroupName = groupEdgePosition.getGroupEdge().getGroupName();
            if (!excludedGroupNames.contains(currentGroupName)) {
                groupNamesInsideMain.add(currentGroupName);
                if (groupEdgePosition.getGroupEdge().isStart()) {
                    GroupEdgePosition endPosition = groupEdgePositionsInsideMain.stream()
                            .filter(gep -> currentGroupName.equals(gep.getGroupEdge().getGroupName())
                                    && !gep.getGroupEdge().isStart())
                            .findFirst().orElseThrow(IllegalStateException::new);
                    groupsInsideMain.add(new MatchedGroup(currentGroupName,
                            bestMatch.getGroupValue(outputGroupName), outputGroupId,
                            new Range(groupEdgePosition.getPosition(), endPosition.getPosition())));
                }
            }
        }

        HashSet<String> groupNamesNotInsideMain = new HashSet<>(matchedGroupNames);
        groupNamesNotInsideMain.removeAll(groupNamesInsideMain);
        ArrayList<MatchedGroup> groupsNotInsideMain = new ArrayList<>();
        for (String currentGroupName : groupNamesNotInsideMain) {
            NSequenceWithQuality groupValue = bestMatch.getGroupValue(currentGroupName);
            byte currentGroupId = matchedGroups.get(currentGroupName).getTargetId();
            groupsNotInsideMain.add(new MatchedGroup(currentGroupName, groupValue, currentGroupId,
                    new Range(0, groupValue.size())));
        }

        List<String> notMatchedGroupNames = allGroupEdges.stream().filter(GroupEdge::isStart)
                .map(GroupEdge::getGroupName)
                .filter(gn -> !matchedGroupNames.contains(gn) && !excludedGroupNames.contains(gn))
                .collect(Collectors.toList());

        String comments = generateComments(groupsInsideMain, groupsNotInsideMain, notMatchedGroupNames, reverseMatch,
                oldComments);
        commentsCache.put(outputGroupName, comments);
        return comments;
    }

    public static ParsedRead read(PrimitivI input) {
        SequenceRead originalRead = input.readObject(SequenceRead.class);
        boolean reverseMatch = input.readBoolean();
        Match bestMatch = input.readObject(Match.class);
        return new ParsedRead(originalRead, reverseMatch, bestMatch);
    }

    public static void write(PrimitivO output, ParsedRead object) {
        output.writeObject(object.getOriginalRead());
        output.writeBoolean(object.isReverseMatch());
        output.writeObject(object.getBestMatch());
    }
}
