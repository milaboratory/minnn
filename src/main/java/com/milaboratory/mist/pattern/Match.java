package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.util.ArrayList;
import java.util.stream.Collectors;

public final class Match {
    private final int numberOfPatterns;
    private final long score;
    private final int leftUppercaseDistance;
    private final int rightUppercaseDistance;
    private final MatchedRange[] matchedRanges;
    private final ArrayList<MatchedGroupEdge> matchedGroupEdges;

    /**
     * Single match for single- or multi-pattern.
     *
     * @param numberOfPatterns number of patterns in multi-pattern, or 1 if it is single pattern
     * @param score match score
     * @param leftUppercaseDistance first uppercase letter position; used for calculating max overlap and insertion
     *                              with other match; -1 means no restrictions for overlaps and insertions
     * @param rightUppercaseDistance first uppercase letter position if count from right to left of this match;
     *                               or -1 for no overlap and insertion restrictions
     * @param matchedGroupEdges     list of matched group edges
     * @param matchedRanges         array of matched ranges for every pattern; size must be equal to numberOfPatterns
     */
    public Match(int numberOfPatterns, long score, int leftUppercaseDistance, int rightUppercaseDistance,
                 ArrayList<MatchedGroupEdge> matchedGroupEdges, MatchedRange... matchedRanges) {
        if (matchedRanges.length == 0) throw new IllegalArgumentException("Missing matched ranges!");
        this.numberOfPatterns = numberOfPatterns;
        this.score = score;
        this.leftUppercaseDistance = leftUppercaseDistance;
        this.rightUppercaseDistance = rightUppercaseDistance;
        this.matchedRanges = matchedRanges;
        this.matchedGroupEdges = matchedGroupEdges;
    }

    /**
     * Return MatchedRange by pattern index.
     *
     * @param patternIndex pattern index for multi-pattern matchers (number of patterns may be bigger than number of
     *                     targets in case of high level logic patterns); 0 - for single target matchers
     * @return MatchedRange for specified pattern
     */
    public MatchedRange getMatchedRange(int patternIndex) {
        return matchedRanges[patternIndex];
    }

    /**
     * Return MatchedRange. Applicable only to single target matchers.
     *
     * @return MatchedRange with patternIndex 0
     */
    public MatchedRange getMatchedRange() {
        if (numberOfPatterns != 1)
            throw new IllegalStateException("Multiple pattern. Use getMatchedRange(int) instead.");
        return matchedRanges[0];
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
     * Get all matched ranges.
     *
     * @return array of all matched ranges.
     */
    public MatchedRange[] getMatchedRanges() {
        return matchedRanges;
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

    public int getLeftUppercaseDistance() {
        return leftUppercaseDistance;
    }

    public int getRightUppercaseDistance() {
        return rightUppercaseDistance;
    }
}
