package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.io.IO;
import com.milaboratory.mist.outputconverter.MatchedGroup;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.annotations.Serializable;

import java.util.*;
import java.util.stream.Collectors;

@Serializable(by = IO.MatchSerializer.class)
public class Match {
    protected final int numberOfPatterns;
    protected final long score;
    protected final ArrayList<MatchedGroupEdge> matchedGroupEdges;
    private ArrayList<MatchedGroup> groups = null;
    private HashMap<String, NSequenceWithQuality> groupValues = null;
    private HashMap<MatchedGroupEdgeIndex, MatchedGroupEdge> matchedGroupEdgesCache = null;

    /**
     * Serializable final match for single- or multi-pattern.
     *
     * @param numberOfPatterns      number of patterns in multi-pattern, or 1 if it is single pattern
     * @param score                 match score
     * @param matchedGroupEdges     list of matched group edges
     */
    public Match(int numberOfPatterns, long score, ArrayList<MatchedGroupEdge> matchedGroupEdges) {
        this.numberOfPatterns = numberOfPatterns;
        this.score = score;
        this.matchedGroupEdges = matchedGroupEdges;
    }

    /**
     * Return MatchedGroupEdge by name and isStart flag.
     *
     * @param groupName group name
     * @param isStart flag, true if it must be group start, false if must be group end
     * @return MatchedRange for specified pattern
     */
    public MatchedGroupEdge getMatchedGroupEdge(String groupName, boolean isStart) {
        if (matchedGroupEdgesCache == null)
            matchedGroupEdgesCache = new HashMap<>();
        MatchedGroupEdgeIndex index = new MatchedGroupEdgeIndex(groupName, isStart);
        MatchedGroupEdge cachedMatchedGroupEdge = matchedGroupEdgesCache.get(index);
        if (cachedMatchedGroupEdge != null)
            return cachedMatchedGroupEdge;
        else {
            for (MatchedGroupEdge matchedGroupEdge : matchedGroupEdges)
                if (matchedGroupEdge.getGroupName().equals(groupName) && (matchedGroupEdge.isStart() == isStart)) {
                    matchedGroupEdgesCache.put(index, matchedGroupEdge);
                    return matchedGroupEdge;
                }
            throw new IllegalStateException("Trying to get group " + (isStart ? "start" : "end") + " with name "
                    + groupName + " and it doesn't exist");
        }
    }

    /**
     * Get all matched group edges.
     *
     * @return ArrayList with all matched group edges.
     */
    public ArrayList<MatchedGroupEdge> getMatchedGroupEdges() {
        return matchedGroupEdges;
    }

    public int getNumberOfPatterns() {
        return numberOfPatterns;
    }

    public long getScore() {
        return score;
    }

    public void assembleGroups() {
        if (groups == null) {
            groups = new ArrayList<>();
            ArrayList<MatchedGroupEdge> matchedGroupEdges = getMatchedGroupEdges();
            /* in matches made with ParsedRead.retarget() we can have duplicate groups; in this case use first instance
               of each group */
            LinkedHashSet<String> groupNames = matchedGroupEdges.stream()
                    .map(MatchedGroupEdge::getGroupName).collect(Collectors.toCollection(LinkedHashSet::new));
            MatchedGroupEdge start;
            MatchedGroupEdge end;
            Range range;
            for (String groupName : groupNames) {
                start = getMatchedGroupEdge(groupName, true);
                end = getMatchedGroupEdge(groupName, false);
                if (start.getPosition() >= end.getPosition())
                    throw new IllegalStateException("Group start must be lower than the end. Start: "
                            + start.getPosition() + ", end: " + end.getPosition());
                if (start.getTargetId() != end.getTargetId())
                    throw new IllegalStateException("Group start has targetId " + start.getTargetId()
                            + ", end has targetId " + end.getTargetId());
                if (!start.getTarget().equals(end.getTarget()))
                    throw new IllegalStateException("Group start has target " + start.getTarget()
                            + ", end has target " + end.getTarget());
                range = new Range(start.getPosition(), end.getPosition());
                groups.add(new MatchedGroup(groupName, start.getTarget(), start.getTargetId(), range));
            }
        }
    }

    public ArrayList<MatchedGroup> getGroups() {
        assembleGroups();
        return new ArrayList<>(groups);
    }

    public NSequenceWithQuality getGroupValue(String groupName) {
        if (groupValues == null) {
            groupValues = new HashMap<>();
            getGroups().forEach(group -> {
                String name = group.getGroupName();
                NSequenceWithQuality value = group.getTarget().getRange(group.getRange());
                groupValues.put(name, value);
            });
        }

        return groupValues.get(groupName);
    }

    public static Match read(PrimitivI input) {
        int numberOfPatterns = input.readVarIntZigZag();
        long score = input.readVarLongZigZag();
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
        int matchedGroupEdgesNum = input.readVarIntZigZag();
        for (int i = 0; i < matchedGroupEdgesNum; i++)
            matchedGroupEdges.add(input.readObject(MatchedGroupEdge.class));
        return new Match(numberOfPatterns, score, matchedGroupEdges);
    }

    public static void write(PrimitivO output, Match object) {
        output.writeVarIntZigZag(object.getNumberOfPatterns());
        output.writeVarLongZigZag(object.getScore());
        output.writeVarIntZigZag(object.getMatchedGroupEdges().size());
        for (MatchedGroupEdge matchedGroupEdge : object.getMatchedGroupEdges())
            output.writeObject(matchedGroupEdge);
    }

    private class MatchedGroupEdgeIndex {
        private final String groupName;
        private final boolean isStart;

        MatchedGroupEdgeIndex(String groupName, boolean isStart) {
            this.groupName = groupName;
            this.isStart = isStart;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MatchedGroupEdgeIndex that = (MatchedGroupEdgeIndex)o;

            return isStart == that.isStart && groupName.equals(that.groupName);
        }

        @Override
        public int hashCode() {
            int result = groupName.hashCode();
            result = 31 * result + (isStart ? 1 : 0);
            return result;
        }
    }
}
