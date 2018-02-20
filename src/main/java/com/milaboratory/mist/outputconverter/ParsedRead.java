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
    private Map<String, MatchedGroup> matchedGroups = null;
    private HashMap<String, ArrayList<GroupEdgePosition>> innerGroupEdgesCache = null;
    private HashMap<String, HashMap<String, Range>> innerRangesCache = null;
    private HashMap<String, String> commentsCache = null;
    private static Set<String> excludedGroups = null;
    private static Set<String> groupsFromHeader = null;

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

    /**
     * Fill inner groups cache: group edges and/or inner ranges based on specified flags. This function must be called
     * only when matchedGroups map is already initialized.
     *
     * @param fillGroupEdges calculate and remember group edge positions for each group that is inside another group,
     *                       and border group edge positions (0 and target.size) for each group
     * @param fillRanges     save ranges of inner groups into cache
     */
    private void fillInnerGroupsCache(boolean fillGroupEdges, boolean fillRanges) {
        if (fillGroupEdges)
            innerGroupEdgesCache = new HashMap<>();
        if (fillRanges)
            innerRangesCache = new HashMap<>();
        for (Map.Entry<String, MatchedGroup> outerGroupEntry : matchedGroups.entrySet()) {
            byte currentTargetId = outerGroupEntry.getValue().getTargetId();
            List<MatchedGroup> sameTargetGroups = getGroups().stream()
                    .filter(mg -> mg.getTargetId() == currentTargetId).collect(Collectors.toList());
            Range outerRange = outerGroupEntry.getValue().getRange();
            ArrayList<GroupEdgePosition> groupEdgePositions = new ArrayList<>();
            HashMap<String, Range> innerRanges = new HashMap<>();
            for (MatchedGroup innerGroup : sameTargetGroups) {
                Range innerRange = innerGroup.getRange();
                if (outerRange.contains(innerRange)) {
                    if (fillGroupEdges) {
                        groupEdgePositions.add(new GroupEdgePosition(new GroupEdge(innerGroup.getGroupName(),
                                true), innerRange.getLower() - outerRange.getLower()));
                        groupEdgePositions.add(new GroupEdgePosition(new GroupEdge(innerGroup.getGroupName(),
                                false), innerRange.getUpper() - outerRange.getLower()));
                    }
                    if (fillRanges)
                        innerRanges.put(innerGroup.getGroupName(), innerRange.move(-outerRange.getLower()));
                }
            }
            if (fillGroupEdges)
                innerGroupEdgesCache.put(outerGroupEntry.getKey(), groupEdgePositions);
            if (fillRanges)
                innerRangesCache.put(outerGroupEntry.getKey(), innerRanges);
        }
    }

    /**
     * Calculate group names that will not be included in comments for FASTQ file. This cache is static because
     * it depends only on number of reads, and it's the same for all reads.
     *
     * @param numberOfReads number of reads in input
     */
    private static void calculateExcludedGroups(int numberOfReads) {
        excludedGroups = IntStream.rangeClosed(1, numberOfReads).mapToObj(i -> "R" + i).collect(Collectors.toSet());
    }

    /**
     * Fill cache for group names from input MIF header; and don't include built-in groups R1, R2 etc.
     *
     * @param allGroupEdges all group edges from input MIF header
     */
    private static void collectGroupNamesFromHeader(ArrayList<GroupEdge> allGroupEdges) {
        groupsFromHeader = allGroupEdges.stream().filter(GroupEdge::isStart).map(GroupEdge::getGroupName)
                .filter(gn -> !excludedGroups.contains(gn)).collect(Collectors.toSet());
    }

    public ParsedRead retarget(String... groupNames) {
        if (groupNames.length == 0)
            throw new IllegalArgumentException("Basic groups for output parsed read are not specified!");

        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
        if (matchedGroups == null)
            matchedGroups = getGroups().stream().collect(Collectors.toMap(MatchedGroup::getGroupName, mg -> mg));
        if (innerGroupEdgesCache == null)
            fillInnerGroupsCache(true, false);

        for (byte i = 0; i < groupNames.length; i++) {
            String outputGroupName = groupNames[i];
            if (!matchedGroups.containsKey(outputGroupName))
                throw new IllegalArgumentException("Group " + outputGroupName
                        + " not found in this ParsedRead; available groups: " + matchedGroups.keySet());
            NSequenceWithQuality target = bestMatch.getGroupValue(outputGroupName);
            for (GroupEdgePosition groupEdgePosition : innerGroupEdgesCache.get(outputGroupName))
                matchedGroupEdges.add(new MatchedGroupEdge(target, i, groupEdgePosition.getGroupEdge(),
                        groupEdgePosition.getPosition()));
        }

        Match targetMatch = new Match(groupNames.length, bestMatch.getScore(), matchedGroupEdges);
        return new ParsedRead(originalRead, reverseMatch, targetMatch);
    }

    public SequenceRead toSequenceRead(boolean copyOldComments, ArrayList<GroupEdge> allGroupEdges,
                                       String... groupNames) {
        if (groupNames.length == 0)
            throw new IllegalArgumentException("Basic groups for output sequence read are not specified!");

        if (commentsCache == null)
            commentsCache = new HashMap<>();
        if (matchedGroups == null)
            matchedGroups = getGroups().stream().collect(Collectors.toMap(MatchedGroup::getGroupName, mg -> mg));
        if (innerRangesCache == null)
            fillInnerGroupsCache(false, true);
        if (excludedGroups == null) {
            calculateExcludedGroups(originalRead.numberOfReads());
            collectGroupNamesFromHeader(allGroupEdges);
        }

        ArrayList<SingleRead> singleReads = new ArrayList<>();
        for (String outputGroupName : groupNames) {
            if (!matchedGroups.containsKey(outputGroupName))
                throw new IllegalArgumentException("Group " + outputGroupName
                        + " not found in this ParsedRead; available groups: " + matchedGroups.keySet());
            singleReads.add(new SingleReadImpl(originalRead.getId(), bestMatch.getGroupValue(outputGroupName),
                    generateReadDescription(copyOldComments, outputGroupName)));
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

    private String generateReadDescription(boolean copyOldComments, String outputGroupName) {
        if (commentsCache.containsKey(outputGroupName))
            return commentsCache.get(outputGroupName);

        TreeSet<FastqCommentGroup> commentGroups = new TreeSet<>();
        for (String groupName : groupsFromHeader) {
            if (innerRangesCache.containsKey(groupName)) {
                HashMap<String, Range> innerRanges = innerRangesCache.get(outputGroupName);
                if (innerRanges.containsKey(groupName))
                    commentGroups.add(new FastqCommentGroup(groupName, true, true,
                            bestMatch.getGroupValue(groupName), innerRanges.get(groupName)));
                else
                    commentGroups.add(new FastqCommentGroup(groupName, bestMatch.getGroupValue(groupName)));
            } else
                commentGroups.add(new FastqCommentGroup(groupName));
        }

        byte commentsTargetId = matchedGroups.get(outputGroupName).getTargetId();
        if (reverseMatch) {
            if (commentsTargetId == 1)
                commentsTargetId = 2;
            else if (commentsTargetId == 2)
                commentsTargetId = 1;
        }

        String comments = generateComments(commentGroups, reverseMatch,
                copyOldComments ? originalRead.getRead(commentsTargetId - 1).getDescription() : "");
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
