package com.milaboratory.mist.util;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.pattern.CaptureGroupMatch;
import com.milaboratory.mist.pattern.Match;
import com.milaboratory.mist.pattern.MatchValidationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.milaboratory.mist.pattern.Match.WHOLE_PATTERN_MATCH_GROUP_NAME_PREFIX;
import static com.milaboratory.mist.util.RangeTools.combineRanges;

public abstract class ApproximateSorter {
    protected final boolean multipleReads;
    protected final boolean combineScoresBySum;
    protected final boolean fairSorting;
    protected final MatchValidationType matchValidationType;
    protected final OutputPort<Match>[] inputPorts;
    protected final int numberOfPorts;

    /**
     * This sorter allows to get output port for approximately sorted matches by score or coordinate from
     * input ports. Specific sorters (by score, coordinate and with different rules) are extending this class.
     *
     * @param multipleReads true if we combine matches from multiple reads; false if we combine matches
     *                      from single read
     * @param combineScoresBySum true if combined score must be equal to sum of match scores; false if combined
     *                           score must be the highest of match scores
     * @param fairSorting true if we need slow but fair sorting
     * @param matchValidationType type of validation used to determine that current matches combination is invalid
     * @param inputPorts ports for input matches; we assume that they are already sorted, maybe approximately
     */
    public ApproximateSorter(boolean multipleReads, boolean combineScoresBySum, boolean fairSorting,
                             MatchValidationType matchValidationType, OutputPort<Match>[] inputPorts) {
        this.multipleReads = multipleReads;
        this.combineScoresBySum = combineScoresBySum;
        this.fairSorting = fairSorting;
        this.matchValidationType = matchValidationType;
        this.inputPorts = inputPorts;
        this.numberOfPorts = inputPorts.length;
    }

    /**
     * Get output port for sorted combined matches.
     *
     * @return output port
     */
    public abstract OutputPort<Match> getOutputPort();

    /**
     * Get combined match from a group of input matches. It uses multipleReads flag to determine how to combine matches
     * (by combining ranges for single read or by numbering the whole group matches for multiple reads).
     *
     * @param matches input matches
     * @return combined match
     */
    protected Match combineMatches(Match... matches) {
        Map<String, CaptureGroupMatch> groupMatches = new HashMap<>();

        if (multipleReads) {
            int wholeGroupIndex = 0;
            for (Match match : matches) {
                if (match == null) {
                    groupMatches.put(WHOLE_PATTERN_MATCH_GROUP_NAME_PREFIX + wholeGroupIndex++, null);
                    continue;
                }
                groupMatches.putAll(match.getGroupMatches(true));
                for (int i = 0; i < match.getNumberOfPatterns(); i++)
                    groupMatches.put(WHOLE_PATTERN_MATCH_GROUP_NAME_PREFIX + wholeGroupIndex++,
                            match.getWholePatternMatch(i));
            }
            return new Match(wholeGroupIndex, combineMatchScores(matches), groupMatches);
        } else {
            NSequenceWithQuality target = matches[0].getWholePatternMatch().getTarget();
            byte targetId = matches[0].getWholePatternMatch().getTargetId();
            Range[] ranges = new Range[matches.length];

            for (int i = 0; i < matches.length; i++) {
                groupMatches.putAll(matches[i].getGroupMatches(true));
                ranges[i] = matches[i].getWholePatternMatch().getRange();
            }

            CaptureGroupMatch wholePatternMatch = new CaptureGroupMatch(target, targetId, combineRanges(ranges));
            groupMatches.put(WHOLE_PATTERN_MATCH_GROUP_NAME_PREFIX + 0, wholePatternMatch);
            return new Match(1, combineMatchScores(matches), groupMatches);
        }
    }

    /**
     * Combine match scores. It is used in combineMatches function. Different patterns may combine operand scores
     * by sum or by max value of operand scores.
     *
     * @param matches matches from which we will get the scores
     * @return combined score
     */
    protected float combineMatchScores(Match... matches) {
        float resultScore;
        if (combineScoresBySum) {
            resultScore = 0;
            for (Match match : matches)
                if (match != null)
                    resultScore += match.getScore();
        } else {
            resultScore = Float.NEGATIVE_INFINITY;
            for (Match match : matches)
                if (match != null)
                    if (match.getScore() > resultScore)
                        resultScore = match.getScore();
        }
        return resultScore;
    }

    protected boolean isCombinationValid(Match... matches) {
        switch (matchValidationType) {
            case ALWAYS:
                return true;
            case INTERSECTION:
                Range ranges[] = new Range[matches.length];
                boolean rangeIntersection = false;

                OUTER:
                for (int i = 0; i < matches.length; i++) {
                    Range currentRange = matches[i].getWholePatternMatch().getRange();
                    ranges[i] = currentRange;
                    for (int j = 0; j < i; j++)  // Compare with all previously added matches
                        if (ranges[j].intersectsWith(currentRange)) {
                            rangeIntersection = true;
                            break OUTER;
                        }
                }

                return !rangeIntersection;
            case ORDER:
                Range currentRange;
                Range previousRange;
                boolean rangesMisplaced = false;
                for (int i = 1; i < matches.length; i++) {
                    currentRange = matches[i].getWholePatternMatch().getRange();
                    previousRange = matches[i - 1].getWholePatternMatch().getRange();
                    if (previousRange.getUpper() > currentRange.getLower()) {
                        rangesMisplaced = true;
                        break;
                    }
                }
                return !rangesMisplaced;
        }
        return false;
    }

    protected class TableOfIterations {
        private final HashSet<ArrayList<Integer>> returnedCombinations;
        private final int numberOfPorts;
        private final boolean portEndReached[];
        private final int portMatchesQuantities[];
        private int totalCombinationsCount = -1;

        TableOfIterations(int numberOfPorts) {
            returnedCombinations = new HashSet<>();
            this.numberOfPorts = numberOfPorts;
            this.portEndReached = new boolean[numberOfPorts];   // boolean initialize value is false
            this.portMatchesQuantities = new int[numberOfPorts];
        }

        boolean isPortEndReached(int portNumber) {
            return portEndReached[portNumber];
        }

        int getNumberOfEndedPorts() {
            int endedPorts = 0;
            for (int i = 0; i < numberOfPorts; i++)
                if (isPortEndReached(i)) endedPorts++;
            return endedPorts;
        }

        int getPortMatchesQuantity(int portNumber) {
            return portMatchesQuantities[portNumber];
        }

        void setPortEndReached(int portNumber, int matchesQuantity) {
            portEndReached[portNumber] = true;
            portMatchesQuantities[portNumber] = matchesQuantity;

            if (getNumberOfEndedPorts() == numberOfPorts) {
                totalCombinationsCount = 1;
                for (int currentPortMatchesQuantity : portMatchesQuantities)
                    totalCombinationsCount *= currentPortMatchesQuantity;
            }
        }

        /**
         * If all ports ended and total combinations count is calculated, returns total combinations count,
         * otherwise -1.
         *
         * @return total combinations count
         */
        int getTotalCombinationsCount() {
            return totalCombinationsCount;
        }

        int getNumberOfReturnedCombinations() {
            return returnedCombinations.size();
        }

        boolean isCombinationReturned(int... indexes) {
            if (indexes.length != numberOfPorts)
                throw new IllegalStateException("Number of indexes: " + indexes.length + ", number of ports: "
                    + numberOfPorts + "; they should be equal!");
            return returnedCombinations.contains(new ArrayList<Integer>() {{ for (int i : indexes) add(i); }});
        }

        void addReturnedCombination(int... indexes) {
            if (isCombinationReturned(indexes))
                throw new IllegalStateException("Trying to add already returned combination!");
            returnedCombinations.add(new ArrayList<Integer>() {{ for (int i : indexes) add(i); }});
        }
    }
}