package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.io.IO;
import com.milaboratory.mist.outputconverter.MatchedGroup;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.annotations.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

@Serializable(by = IO.MatchSerializer.class)
public class Match {
    protected final int numberOfPatterns;
    private final long score;
    private final ArrayList<MatchedGroupEdge> matchedGroupEdges;
    private ArrayList<MatchedGroup> groups = null;
    private HashMap<String, NSequenceWithQuality> groupValues = null;

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
        for (MatchedGroupEdge matchedGroupEdge : matchedGroupEdges)
            if (matchedGroupEdge.getGroupName().equals(groupName) && (matchedGroupEdge.isStart() == isStart))
                return matchedGroupEdge;
        throw new IllegalStateException("Trying to get group " + (isStart ? "start" : "end") + " with name "
                + groupName + " and it doesn't exist");
    }

    /**
     * Return ArrayList of matched group edges with specified pattern index.
     *
     * @param patternIndex pattern index; group edges with this index will be searched
     * @return ArrayList of matched group edges with specified pattern index
     */
    public ArrayList<MatchedGroupEdge> getMatchedGroupEdgesByPattern(int patternIndex) {
        return matchedGroupEdges.stream().filter(mge -> mge.getPatternIndex() == patternIndex)
                .collect(Collectors.toCollection(ArrayList::new));
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

    public ArrayList<MatchedGroup> getGroups() {
        if (groups == null) {
            groups = new ArrayList<>();
            ArrayList<MatchedGroupEdge> matchedGroupEdges = getMatchedGroupEdges();
            MatchedGroupEdge endOfCurrentGroup;
            Range currentRange;

            for (MatchedGroupEdge matchedGroupEdge : matchedGroupEdges)
                if (matchedGroupEdge.isStart()) {
                    endOfCurrentGroup = getMatchedGroupEdge(matchedGroupEdge.getGroupName(), false);
                    if (matchedGroupEdge.getPosition() >= endOfCurrentGroup.getPosition())
                        throw new IllegalStateException("Group start must be lower than the end. Start: "
                                + matchedGroupEdge.getPosition() + ", end: " + endOfCurrentGroup.getPosition());
                    if (matchedGroupEdge.getPatternIndex() != endOfCurrentGroup.getPatternIndex())
                        throw new IllegalStateException("Start and end of the group " + matchedGroupEdge.getGroupName()
                                + " have different pattern indexes (start: " + matchedGroupEdge.getPatternIndex()
                                + ", end: " + endOfCurrentGroup.getPatternIndex() + ")!");
                    currentRange = new Range(matchedGroupEdge.getPosition(), endOfCurrentGroup.getPosition());
                    groups.add(new MatchedGroup(matchedGroupEdge.getGroupName(), matchedGroupEdge.getTarget(),
                            matchedGroupEdge.getTargetId(), matchedGroupEdge.getPatternIndex(), currentRange));
                }
        }

        return groups;
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
        int numberOfPatterns = input.readInt();
        long score = input.readLong();
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
        int matchedGroupEdgesNum = input.readInt();
        for (int i = 0; i < matchedGroupEdgesNum; i++)
            matchedGroupEdges.add(input.readObject(MatchedGroupEdge.class));
        return new Match(numberOfPatterns, score, matchedGroupEdges);
    }

    public static void write(PrimitivO output, Match object) {
        output.writeInt(object.getNumberOfPatterns());
        output.writeLong(object.getScore());
        output.writeInt(object.getMatchedGroupEdges().size());
        for (MatchedGroupEdge matchedGroupEdge : object.getMatchedGroupEdges())
            output.writeObject(matchedGroupEdge);
    }
}
