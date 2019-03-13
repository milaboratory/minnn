/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact MiLaboratory LLC, which owns exclusive
 * rights for distribution of this program for commercial purposes, using the
 * following email address: licensing@milaboratory.com.
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.minnn.outputconverter;

import com.milaboratory.core.Range;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.minnn.io.IO;
import com.milaboratory.minnn.pattern.*;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.annotations.Serializable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.outputconverter.GroupUtils.*;

@Serializable(by = IO.ParsedReadSerializer.class)
public final class ParsedRead {
    private final SequenceRead originalRead;
    private final boolean reverseMatch;
    private final Match bestMatch;
    /* number of reads used to calculate this consensus: used for consensuses and for reads that contain consensuses
       as groups (--consensuses-to-separate-groups argument in consensus); in other cases it must be 0 */
    private final int consensusReads;
    private long outputPortId;
    private Map<String, MatchedGroup> matchedGroups = null;
    private HashMap<String, ArrayList<GroupEdgePosition>> innerGroupEdgesCache = null;
    private HashMap<String, HashMap<String, Range>> innerRangesCache = null;
    private HashMap<String, String> commentsCache = null;
    private static Set<String> defaultGroups = null;
    private static Set<String> groupsFromHeader = null;

    public ParsedRead(SequenceRead originalRead, boolean reverseMatch, Match bestMatch, int consensusReads) {
        this(originalRead, reverseMatch, bestMatch, consensusReads, -1);
    }

    public ParsedRead(SequenceRead originalRead, boolean reverseMatch, Match bestMatch, int consensusReads,
                      long outputPortId) {
        this.originalRead = originalRead;
        this.reverseMatch = reverseMatch;
        this.bestMatch = bestMatch;
        this.consensusReads = consensusReads;
        this.outputPortId = outputPortId;
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

    public int getConsensusReads() {
        return consensusReads;
    }

    public ArrayList<MatchedGroup> getGroups() {
        if (bestMatch == null)
            return new ArrayList<>();
        else
            return bestMatch.getGroups();
    }

    public ArrayList<MatchedGroupEdge> getMatchedGroupEdges() {
        if (bestMatch == null)
            return new ArrayList<>();
        else
            return new ArrayList<>(bestMatch.getMatchedGroupEdges());
    }

    public long getBestMatchScore() {
        return (bestMatch == null) ? Long.MIN_VALUE : bestMatch.getScore();
    }

    public NSequenceWithQuality getGroupValue(String groupName) {
        if (bestMatch == null) {
            if (defaultGroups == null)
                calculateDefaultGroups(originalRead.numberOfReads());
            if (defaultGroups.contains(groupName)) {
                byte targetId = Byte.parseByte(groupName.substring(1));
                if (reverseMatch) {
                    if (targetId == 1)
                        targetId = 2;
                    else if (targetId == 2)
                        targetId = 1;
                }
                return originalRead.getRead(targetId - 1).getData();
            } else
                return NSequenceWithQuality.EMPTY;
        } else
            return bestMatch.getGroupValue(groupName);
    }

    public long getOutputPortId() {
        return outputPortId;
    }

    public void setOutputPortId(long outputPortId) {
        this.outputPortId = outputPortId;
    }

    public Set<String> getDefaultGroupNames() {
        if (defaultGroups == null)
            calculateDefaultGroups(getOriginalRead().numberOfReads());
        return Collections.unmodifiableSet(defaultGroups);
    }

    public List<MatchedGroup> getNotDefaultGroups() {
        return getGroups().stream().filter(group -> !getDefaultGroupNames().contains(group.getGroupName()))
                .collect(Collectors.toList());
    }

    public MatchedGroup getGroupByName(String groupName) {
        return getGroups().stream().filter(group -> group.getGroupName().equals(groupName)).findFirst().orElse(null);
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
            if (outerRange != null) {
                ArrayList<GroupEdgePosition> groupEdgePositions = new ArrayList<>();
                HashMap<String, Range> innerRanges = new HashMap<>();
                for (MatchedGroup innerGroup : sameTargetGroups) {
                    Range innerRange = innerGroup.getRange();
                    if ((innerRange != null) && outerRange.contains(innerRange)) {
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
    }

    /**
     * Calculate built-in group names that will not be included in comments for FASTQ file. This cache is static because
     * it depends only on number of reads, and it's the same for all reads.
     *
     * @param numberOfReads number of reads in input
     */
    private static void calculateDefaultGroups(int numberOfReads) {
        defaultGroups = IntStream.rangeClosed(1, numberOfReads).mapToObj(i -> "R" + i).collect(Collectors.toSet());
    }

    /**
     * Fill cache for group names from input MIF header; and don't include built-in groups R1, R2 etc.
     *
     * @param allGroupEdges all group edges from input MIF header
     */
    private static void collectGroupNamesFromHeader(ArrayList<GroupEdge> allGroupEdges) {
        groupsFromHeader = allGroupEdges.stream().filter(GroupEdge::isStart).map(GroupEdge::getGroupName)
                .filter(gn -> !defaultGroups.contains(gn)).collect(Collectors.toSet());
    }

    public static void clearStaticCache() {
        defaultGroups = null;
        groupsFromHeader = null;
    }

    public ParsedRead retarget(String... groupNames) {
        if (groupNames.length == 0)
            throw new IllegalArgumentException("Basic groups for output parsed read are not specified!");

        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
        if (matchedGroups == null)
            matchedGroups = getGroups().stream().collect(Collectors.toMap(MatchedGroup::getGroupName, mg -> mg));
        if (innerGroupEdgesCache == null)
            fillInnerGroupsCache(true, false);

        for (byte targetId = 1; targetId <= groupNames.length; targetId++) {
            String outputGroupName = groupNames[targetId - 1];
            if (!matchedGroups.containsKey(outputGroupName))
                throw new IllegalArgumentException("Group " + outputGroupName
                        + " not found in this ParsedRead; available groups: " + matchedGroups.keySet());
            NSequenceWithQuality target = getGroupValue(outputGroupName);
            for (GroupEdgePosition groupEdgePosition : innerGroupEdgesCache.get(outputGroupName))
                matchedGroupEdges.add(new MatchedGroupEdge(target, targetId, groupEdgePosition.getGroupEdge(),
                        groupEdgePosition.getPosition()));
            List<String> otherGroupNames = matchedGroups.keySet().stream()
                    .filter(name -> !innerGroupEdgesCache.get(outputGroupName).stream()
                            .map(groupEdgePosition -> groupEdgePosition.getGroupEdge().getGroupName())
                            .collect(Collectors.toSet()).contains(name))
                    .collect(Collectors.toList());
            for (String groupName : otherGroupNames) {
                matchedGroupEdges.add(new MatchedGroupEdge(target, targetId, new GroupEdge(groupName, true),
                        getGroupValue(groupName)));
                matchedGroupEdges.add(new MatchedGroupEdge(null, targetId, new GroupEdge(groupName, false),
                        null));
            }
        }

        Match targetMatch = new Match(groupNames.length, getBestMatchScore(), matchedGroupEdges);
        return new ParsedRead(originalRead, reverseMatch, targetMatch, consensusReads, outputPortId);
    }

    public SequenceRead toSequenceRead(boolean copyOriginalHeaders, ArrayList<GroupEdge> allGroupEdges,
                                       String... groupNames) {
        if (groupNames.length == 0)
            throw new IllegalArgumentException("Basic groups for output sequence read are not specified!");

        if (defaultGroups == null) {
            calculateDefaultGroups(originalRead.numberOfReads());
            collectGroupNamesFromHeader(allGroupEdges);
        }
        if (commentsCache == null)
            commentsCache = new HashMap<>();
        if (matchedGroups == null)
            matchedGroups = getGroups().stream().collect(Collectors.toMap(MatchedGroup::getGroupName, mg -> mg));
        if (innerRangesCache == null)
            fillInnerGroupsCache(false, true);

        ArrayList<SingleRead> singleReads = new ArrayList<>();
        for (String outputGroupName : groupNames) {
            if (!defaultGroups.contains(outputGroupName) && !matchedGroups.containsKey(outputGroupName))
                throw new IllegalArgumentException("Group " + outputGroupName
                        + " not found in this ParsedRead; available groups: " + matchedGroups.keySet());
            singleReads.add(new SingleReadImpl(originalRead.getId(), getGroupValue(outputGroupName),
                    generateReadDescription(copyOriginalHeaders, outputGroupName)));
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
        ArrayList<String> minnnComments = new ArrayList<>();
        sequenceRead.iterator()
                .forEachRemaining(singleRead -> minnnComments.add(extractMinnnComments(singleRead.getDescription())));
        Match targetMatch = new Match(sequenceRead.numberOfReads(), 0,
                parseGroupEdgesFromComments(minnnComments));
        return new ParsedRead(sequenceRead, parseReverseMatchFlag(minnnComments.get(0)), targetMatch, 0);
    }

    private String generateReadDescription(boolean copyOriginalHeaders, String outputGroupName) {
        if (commentsCache.containsKey(outputGroupName))
            return commentsCache.get(outputGroupName);

        String readDescription;
        if (bestMatch == null) {
            byte targetIndex = (byte)(Byte.parseByte(outputGroupName.substring(1)) - 1);
            readDescription = originalRead.getRead(targetIndex).getDescription();
        } else {
            TreeSet<FastqCommentGroup> commentGroups = new TreeSet<>();
            for (String groupName : groupsFromHeader) {
                if (innerRangesCache.containsKey(groupName)) {
                    HashMap<String, Range> innerRanges = innerRangesCache.get(outputGroupName);
                    if (innerRanges.containsKey(groupName))
                        commentGroups.add(new FastqCommentGroup(groupName, true, true,
                                getGroupValue(groupName), innerRanges.get(groupName)));
                    else
                        commentGroups.add(new FastqCommentGroup(groupName, getGroupValue(groupName)));
                } else {
                    NSequenceWithQuality groupValue = getGroupValue(groupName);
                    commentGroups.add((groupValue == null) ? new FastqCommentGroup(groupName)
                            : new FastqCommentGroup(groupName, groupValue));
                }
            }

            byte commentsTargetId = matchedGroups.get(outputGroupName).getTargetId();
            if (reverseMatch) {
                if (commentsTargetId == 1)
                    commentsTargetId = 2;
                else if (commentsTargetId == 2)
                    commentsTargetId = 1;
            }

            readDescription = generateComments(commentGroups, reverseMatch,
                    copyOriginalHeaders ? originalRead.getRead(commentsTargetId - 1).getDescription() : "");
        }

        commentsCache.put(outputGroupName, readDescription);
        return readDescription;
    }

    public static ParsedRead read(PrimitivI input) {
        SequenceRead originalRead = input.readObject(SequenceRead.class);
        boolean reverseMatch = input.readBoolean();
        Match bestMatch = input.readObject(Match.class);
        int consensusReads = input.readVarIntZigZag();
        return new ParsedRead(originalRead, reverseMatch, bestMatch, consensusReads);
    }

    public static void write(PrimitivO output, ParsedRead object) {
        output.writeObject(object.getOriginalRead());
        output.writeBoolean(object.isReverseMatch());
        output.writeObject(object.getBestMatch());
        output.writeVarIntZigZag(object.getConsensusReads());
    }
}
