/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
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
public final class ParsedRead implements java.io.Serializable {
    private final SequenceRead originalRead;
    private final boolean reverseMatch;
    /* positive value if there was default groups override in the pattern, otherwise -1
     * (number of targets can be determined by originalRead) */
    private final int numberOfTargetsOverride;
    private final Match bestMatch;
    /* number of reads used to calculate this consensus: used for consensuses and for reads that contain consensuses
     * as groups (--consensuses-to-separate-groups argument in consensus); in other cases it must be 0 */
    private final int consensusReads;
    private long outputPortId;
    private Map<String, MatchedGroup> matchedGroups = null;
    private HashMap<String, ArrayList<GroupEdgePosition>> innerGroupEdgesCache = null;
    private HashMap<String, HashMap<String, Range>> innerRangesCache = null;
    private HashMap<String, String> commentsCache = null;
    private static LinkedHashSet<String> defaultGroups = null;
    private static LinkedHashSet<String> originalDefaultGroups = null;
    private static LinkedHashSet<String> notDefaultGroupsFromHeader = null;

    public ParsedRead(SequenceRead originalRead, boolean reverseMatch, int numberOfTargetsOverride, Match bestMatch,
                      int consensusReads) {
        this(originalRead, reverseMatch, numberOfTargetsOverride, bestMatch, consensusReads, -1);
    }

    public ParsedRead(SequenceRead originalRead, boolean reverseMatch, int numberOfTargetsOverride, Match bestMatch,
                      int consensusReads, long outputPortId) {
        this.originalRead = originalRead;
        this.reverseMatch = reverseMatch;
        this.numberOfTargetsOverride = numberOfTargetsOverride;
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

    public boolean isNumberOfTargetsOverride() {
        return numberOfTargetsOverride > 0;
    }

    public int getNumberOfTargets() {
        return isNumberOfTargetsOverride() ? numberOfTargetsOverride : originalRead.numberOfReads();
    }

    public int getRawNumberOfTargetsOverride() {
        return numberOfTargetsOverride;
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

    public NSequenceWithQuality getMatchTarget(byte targetId) {
        return Objects.requireNonNull(bestMatch).getGroupValue("R" + targetId);
    }

    public NSequenceWithQuality getGroupValue(String groupName) {
        if (bestMatch == null) {
            // this is used when mif2fastq called on file with mismatched reads from extract
            if (originalDefaultGroups == null)
                calculateOriginalDefaultGroups(originalRead.numberOfReads());
            if (originalDefaultGroups.contains(groupName)) {
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

    public LinkedHashSet<String> getDefaultGroupNames() {
        if (defaultGroups == null)
            calculateDefaultGroups(getNumberOfTargets());
        return new LinkedHashSet<>(defaultGroups);
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
            // targetId -1 is used in reads with default groups override; inner groups checking is skipped in this case
            List<MatchedGroup> sameTargetGroups = (currentTargetId == -1)
                    ? Collections.singletonList(outerGroupEntry.getValue())
                    : getGroups().stream().filter(mg -> mg.getTargetId() == currentTargetId)
                    .collect(Collectors.toList());
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
     * Calculate built-in group names. This cache is static because it's the same for all reads.
     *
     * @param numberOfTargets number of targets
     */
    private static void calculateDefaultGroups(int numberOfTargets) {
        defaultGroups = IntStream.rangeClosed(1, numberOfTargets).mapToObj(i -> "R" + i)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Original default groups are used when converting mismatched reads from extract to FASTQ.
     *
     * @param originalNumberOfTargets number of targets in original reads
     */
    private static void calculateOriginalDefaultGroups(int originalNumberOfTargets) {
        originalDefaultGroups = IntStream.rangeClosed(1, originalNumberOfTargets).mapToObj(i -> "R" + i)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Fill not default group names from input MIF header.
     *
     * @param allGroupEdges all group edges from input MIF header
     */
    private static void calculateNotDefaultGroups(ArrayList<GroupEdge> allGroupEdges) {
        notDefaultGroupsFromHeader = allGroupEdges.stream().map(GroupEdge::getGroupName)
                .filter(gn -> !defaultGroups.contains(gn)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static void clearStaticCache() {
        defaultGroups = null;
        originalDefaultGroups = null;
        notDefaultGroupsFromHeader = null;
    }

    public ParsedRead retarget(String... groupNames) {
        if (groupNames.length == 0)
            throw new IllegalArgumentException("Basic groups for output parsed read are not specified!");

        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
        HashSet<String> usedGroupNames = new HashSet<>();
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
            for (GroupEdgePosition groupEdgePosition : innerGroupEdgesCache.get(outputGroupName)) {
                matchedGroupEdges.add(new MatchedGroupEdge(target, targetId, groupEdgePosition.getGroupEdge(),
                        groupEdgePosition.getPosition()));
                usedGroupNames.add(groupEdgePosition.getGroupEdge().getGroupName());
            }
        }
        LinkedHashSet<String> otherGroupNames = matchedGroups.keySet().stream()
                .filter(name -> !usedGroupNames.contains(name)).collect(Collectors.toCollection(LinkedHashSet::new));
        for (String groupName : otherGroupNames) {
            NSequenceWithQuality target = getGroupValue(groupName);
            matchedGroupEdges.add(new MatchedGroupEdge(target, (byte)-1, new GroupEdge(groupName, true),
                    target));
            matchedGroupEdges.add(new MatchedGroupEdge(null, (byte)-1, new GroupEdge(groupName, false),
                    null));
        }

        Match targetMatch = new Match(groupNames.length, getBestMatchScore(), matchedGroupEdges);
        return new ParsedRead(originalRead, reverseMatch, groupNames.length, targetMatch, consensusReads,
                outputPortId);
    }

    public SequenceRead toSequenceRead(boolean copyOriginalHeaders, ArrayList<GroupEdge> allGroupEdges,
                                       String... groupNames) {
        if (groupNames.length == 0)
            throw new IllegalArgumentException("Basic groups for output sequence read are not specified!");

        if (defaultGroups == null)
            calculateDefaultGroups(getNumberOfTargets());
        if (notDefaultGroupsFromHeader == null)
            calculateNotDefaultGroups(allGroupEdges);
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
        return new ParsedRead(sequenceRead, parseReverseMatchFlag(minnnComments.get(0)), -1,
                targetMatch, 0);
    }

    private String generateReadDescription(boolean copyOriginalHeaders, String outputGroupName) {
        String cachedDescription = commentsCache.get(outputGroupName);
        if (cachedDescription != null)
            return cachedDescription;

        String readDescription;
        if (bestMatch == null) {
            byte targetIndex = (byte)(Byte.parseByte(outputGroupName.substring(1)) - 1);
            readDescription = originalRead.getRead(targetIndex).getDescription();
        } else {
            TreeSet<FastqCommentGroup> commentGroups = new TreeSet<>();
            for (String groupName : notDefaultGroupsFromHeader) {
                if (innerRangesCache.containsKey(groupName)) {
                    HashMap<String, Range> innerRanges = innerRangesCache.get(outputGroupName);
                    Range currentGroupRange = innerRanges.get(groupName);
                    if (currentGroupRange != null)
                        commentGroups.add(new FastqCommentGroup(groupName, true, true,
                                getGroupValue(groupName), currentGroupRange));
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

            String oldComments = (copyOriginalHeaders && (commentsTargetId != -1))
                    ? originalRead.getRead(commentsTargetId - 1).getDescription() : "";
            readDescription = generateComments(commentGroups, reverseMatch, oldComments);
        }

        commentsCache.put(outputGroupName, readDescription);
        return readDescription;
    }

    public static ParsedRead read(PrimitivI input) {
        SequenceRead originalRead = input.readObject(SequenceRead.class);
        boolean reverseMatch = input.readBoolean();
        int numberOfTargetsOverride = input.readVarIntZigZag();
        Match bestMatch = input.readObject(Match.class);
        int consensusReads = input.readVarIntZigZag();
        return new ParsedRead(originalRead, reverseMatch, numberOfTargetsOverride, bestMatch, consensusReads);
    }

    public static void write(PrimitivO output, ParsedRead object) {
        output.writeObject(object.getOriginalRead());
        output.writeBoolean(object.isReverseMatch());
        output.writeVarIntZigZag(object.getRawNumberOfTargetsOverride());
        output.writeObject(object.getBestMatch());
        output.writeVarIntZigZag(object.getConsensusReads());
    }
}
