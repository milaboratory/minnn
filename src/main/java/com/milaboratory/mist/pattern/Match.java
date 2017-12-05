package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.util.ArrayList;

public final class Match {
    private final int numberOfPatterns;
    private final long score;

    /* First uppercase letter position; used for calculating max overlap and insertion with other match;
       -1 means no restrictions for overlaps and insertions */
    private final int leftUppercaseDistance;

    /* First uppercase letter position if count from right to left of this match;
       or -1 for no overlap and insertion restrictions */
    private final int rightUppercaseDistance;

    // This list contains both matched ranges and matched group edges.
    private final ArrayList<MatchedItem> matchedItems;

    public Match(int numberOfPatterns, long score, int leftUppercaseDistance, int rightUppercaseDistance,
                 ArrayList<MatchedItem> matchedItems) {
        this.numberOfPatterns = numberOfPatterns;
        this.score = score;
        this.leftUppercaseDistance = leftUppercaseDistance;
        this.rightUppercaseDistance = rightUppercaseDistance;
        this.matchedItems = matchedItems;
    }

    /**
     * Return MatchedRange by pattern index.
     *
     * @param patternIndex pattern index for multi-pattern matchers (number of patterns may be bigger than number of
     *                     targets in case of high level logic patterns); 0 - for single target matchers
     * @return MatchedRange for specified pattern
     */
    public MatchedRange getMatchedRange(int patternIndex) {
        for (MatchedItem item : matchedItems)
            if (item instanceof MatchedRange
                    && (item.getPatternIndex() == patternIndex))
                return (MatchedRange)item;
        if ((patternIndex >= numberOfPatterns) || (patternIndex < 0))
            throw new IndexOutOfBoundsException("Trying to get pattern index " + patternIndex + ", maximum allowed is "
                + (numberOfPatterns - 1));
        else
            throw new IllegalStateException("matchedItems doesn't contain item with index " + patternIndex
                + "; numberOfPatterns is " + numberOfPatterns);
    }

    /**
     * Return MatchedRange. Applicable only to single target matchers.
     *
     * @return MatchedRange with patternIndex 0
     */
    public MatchedRange getMatchedRange() {
        if (numberOfPatterns != 1)
            throw new IllegalStateException("Multiple pattern. Use getMatchedRange(int) instead.");
        return getMatchedRange(0);
    }

    public Range getRange() {
        return getMatchedRange().getRange();
    }

    public NSequenceWithQuality getValue() {
        return getMatchedRange().getValue();
    }

    /**
     * Return MatchedGroupEdge by name and isStart flag.
     *
     * @param groupName group name
     * @param isStart flag, true if it must be group start, false if must be group end
     * @return MatchedRange for specified pattern
     */
    public MatchedGroupEdge getMatchedGroupEdge(String groupName, boolean isStart) {
        for (MatchedItem item : matchedItems)
            if (item instanceof MatchedGroupEdge
                    && (((MatchedGroupEdge)item).getGroupName().equals(groupName))
                    && (((MatchedGroupEdge)item).isStart() == isStart))
                return (MatchedGroupEdge)item;
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
        ArrayList<MatchedGroupEdge> foundGroupEdges = new ArrayList<>();
        for (MatchedItem item : matchedItems)
            if (item instanceof MatchedGroupEdge && (item.getPatternIndex() == patternIndex))
                foundGroupEdges.add((MatchedGroupEdge)item);
        return foundGroupEdges;
    }

    /**
     * Get all matched items.
     *
     * @return ArrayList with all matched items.
     */
    public ArrayList<MatchedItem> getMatchedItems() {
        return matchedItems;
    }

    /**
     * Get all matched ranges.
     *
     * @return ArrayList with all matched ranges.
     */
    public ArrayList<MatchedRange> getMatchedRanges() {
        ArrayList<MatchedRange> matchedRanges = new ArrayList<>();
        for (MatchedItem item : matchedItems)
            if (item instanceof MatchedRange)
                matchedRanges.add((MatchedRange)item);
        return matchedRanges;
    }

    /**
     * Get all matched group edges.
     *
     * @return ArrayList with all matched group edges.
     */
    public ArrayList<MatchedGroupEdge> getMatchedGroupEdges() {
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
        for (MatchedItem item : matchedItems)
            if (item instanceof MatchedGroupEdge)
                matchedGroupEdges.add((MatchedGroupEdge)item);
        return matchedGroupEdges;
    }

    public int getNumberOfPatterns() {
        return numberOfPatterns;
    }

    public long getScore() {
        return score;
    }

    public int getLeftUppercaseDistance() {
        return leftUppercaseDistance;
    }

    public int getRightUppercaseDistance() {
        return rightUppercaseDistance;
    }
}
