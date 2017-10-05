package com.milaboratory.mist.util;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.pattern.*;

import java.util.*;

import static com.milaboratory.mist.pattern.MatchValidationType.*;
import static com.milaboratory.mist.util.RangeTools.*;
import static com.milaboratory.mist.util.UnfairSorterConfiguration.*;

public final class ApproximateSorter {
    private final ApproximateSorterConfiguration conf;
    private final OutputPort<Match> matchesOutputPort;
    private final ArrayList<SpecificOutputPort> unfairOutputPorts = new ArrayList<>();
    private final HashSet<IncompatibleIndexes> allIncompatibleIndexes = new HashSet<>();
    private final HashSet<Integer> unfairReturnedCombinationsHashes = new HashSet<>();

    private int unfairSorterTakenValues = 0;

    /**
     * This sorter allows to get output port for approximately or fair sorted matches by score from input ports.
     *
     * @param conf sorter configuration
     */
    public ApproximateSorter(ApproximateSorterConfiguration conf) {
        this.conf = conf;
        this.matchesOutputPort = new MatchesOutputPort();
    }

    /**
     * Get output port for sorted combined matches.
     *
     * @return output port
     */
    public OutputPort<Match> getOutputPort() {
        return matchesOutputPort;
    }

    /**
     * Get combined match from a group of input matches. It uses multipleReads flag to determine how to combine matches
     * (by combining ranges for single read or by numbering the matched ranges for multiple reads).
     *
     * @param matches input matches
     * @return combined match
     */
    private Match combineMatches(Match... matches) {
        ArrayList<MatchedItem> matchedItems = new ArrayList<>();

        if (conf.multipleReads) {
            int patternIndex = 0;
            boolean allMatchesAreNull = true;
            for (Match match : matches) {
                if (match == null) {
                    if (conf.matchValidationType == LOGICAL_OR) {
                        matchedItems.add(new NullMatchedRange(patternIndex++));
                        continue;
                    } else throw new IllegalStateException(
                            "Found null match when MatchValidationType doesn't allow them");
                } else allMatchesAreNull = false;
                for (int i = 0; i < match.getNumberOfPatterns(); i++) {
                    MatchedRange currentMatchedRange = match.getMatchedRange(i);
                    if (currentMatchedRange instanceof NullMatchedRange) {
                        if (match.getMatchedGroupEdgesByPattern(i).size() > 0)
                            throw new IllegalStateException("Null pattern contains "
                                    + match.getMatchedGroupEdgesByPattern(i).size() + " group edges");
                        matchedItems.add(new NullMatchedRange(patternIndex++));
                    } else {
                        matchedItems.add(new MatchedRange(currentMatchedRange.getTarget(),
                                currentMatchedRange.getTargetId(), patternIndex, currentMatchedRange.getRange()));
                        for (MatchedGroupEdge matchedGroupEdge : match.getMatchedGroupEdgesByPattern(i))
                            matchedItems.add(new MatchedGroupEdge(matchedGroupEdge.getTarget(),
                                    matchedGroupEdge.getTargetId(), patternIndex, matchedGroupEdge.getGroupEdge(),
                                    matchedGroupEdge.getPosition()));
                        patternIndex++;
                    }
                }
            }

            if (allMatchesAreNull)
                return null;
            else
                return new Match(patternIndex, combineMatchScores(matches), matchedItems);
        } else if (conf.matchValidationType == FIRST) {
            boolean matchExist = false;
            int bestMatchPort = 0;
            long bestScore = Long.MIN_VALUE;
            for (int i = 0; i < matches.length; i++)
                if ((matches[i] != null) && (matches[i].getScore() > bestScore)) {
                    matchExist = true;
                    bestScore = matches[i].getScore();
                    bestMatchPort = i;
                }

            if (matchExist) {
                return matches[bestMatchPort];
            } else
                return null;
        } else {
            NSequenceWithQuality target = matches[0].getMatchedRange().getTarget();
            byte targetId = matches[0].getMatchedRange().getTargetId();
            Range[] ranges = new Range[matches.length];
            ArrayList<ArrayList<MatchedGroupEdge>> matchedGroupEdgesFromOperands = new ArrayList<>();

            for (int i = 0; i < matches.length; i++) {
                matchedGroupEdgesFromOperands.add(new ArrayList<>());
                matchedGroupEdgesFromOperands.get(i).addAll(matches[i].getMatchedGroupEdges());
                ranges[i] = matches[i].getRange();
            }

            Arrays.sort(ranges, Comparator.comparingInt(Range::getLower));
            CombinedRange combinedRange = combineRanges(conf.patternAligner, matchedGroupEdgesFromOperands,
                    target, conf.matchValidationType == FOLLOWING, ranges);
            matchedItems.addAll(combinedRange.getMatchedGroupEdges());
            matchedItems.add(new MatchedRange(target, targetId, 0, combinedRange.getRange()));

            return new Match(1, combineMatchScores(matches) + combinedRange.getScorePenalty(),
                    matchedItems);
        }
    }

    /**
     * Combine match scores. It is used in combineMatches function. Different patterns may combine operand scores
     * by sum or by max value of operand scores.
     *
     * @param matches matches from which we will get the scores
     * @return combined score
     */
    private long combineMatchScores(Match... matches) {
        long resultScore;
        if (conf.combineScoresBySum) {
            resultScore = 0;
            for (Match match : matches)
                if (match != null)
                    resultScore += match.getScore();
        } else {
            resultScore = Long.MIN_VALUE;
            for (Match match : matches)
                if (match != null)
                    if (match.getScore() > resultScore)
                        resultScore = match.getScore();
        }
        return resultScore;
    }

    /**
     * Returns true if null match taken from operand does not guarantee that this operator will not match.
     *
     * @return true if null matches taken from operands must not automatically discard the current combination
     */
    private boolean areNullMatchesAllowed() {
        return ((conf.matchValidationType == LOGICAL_OR) || (conf.matchValidationType == FIRST));
    }

    /**
     * Take all matches with filtering: match combinations that contain incompatible ranges or have score
     * below threshold will not be included. For unfair sorting, output ports with match number limits will be used.
     *
     * @return list of all matches with filtering
     */
    private ArrayList<Match> takeFilteredMatches() {
        ArrayList<Match> allMatchesFiltered = new ArrayList<>();
        long penaltyThreshold = conf.patternAligner.penaltyThreshold();
        int numberOfOperands = conf.operandPatterns.length;
        int[] matchIndexes = new int[numberOfOperands];
        Match[] currentMatches = new Match[numberOfOperands];

        if (conf.fairSorting || !conf.specificOutputPorts) {
            ArrayList<ArrayList<Match>> allMatches = new ArrayList<>();
            OutputPort<Match> currentPort;
            ArrayList<Match> currentPortMatchesList;
            Match currentMatch;
            int totalNumberOfCombinations = 1;

            // take all matches from all operands
            for (int i = 0; i < numberOfOperands; i++) {
                if (conf.fairSorting) {
                    currentPort = conf.multipleReads
                            ? (conf.separateTargets
                                ? ((SinglePattern)conf.operandPatterns[i])
                                    .match(conf.target.get(i)).getMatches(true)
                                : conf.operandPatterns[i].match(conf.target).getMatches(true))
                            : ((SinglePattern)conf.operandPatterns[i])
                                .match(conf.target.get(0), conf.from(), conf.to()).getMatches(true);
                    currentPortMatchesList = new ArrayList<>();
                    do {
                        currentMatch = currentPort.take();
                        if ((currentMatch != null) || (areNullMatchesAllowed() && (currentPortMatchesList.size() == 0)))
                            currentPortMatchesList.add(currentMatch);
                    } while (currentMatch != null);
                } else
                    currentPortMatchesList = getPortWithParams(i).takeAll(areNullMatchesAllowed());

                allMatches.add(currentPortMatchesList);
                totalNumberOfCombinations *= currentPortMatchesList.size();
            }

            for (int i = 0; i < totalNumberOfCombinations; i++) {
                if (areCompatible(matchIndexes) && !alreadyReturned(matchIndexes)) {
                    for (int j = 0; j < numberOfOperands; j++)
                        currentMatches[j] = allMatches.get(j).get(matchIndexes[j]);
                    IncompatibleIndexes incompatibleIndexes = findIncompatibleIndexes(currentMatches, matchIndexes);
                    if (incompatibleIndexes != null)
                        allIncompatibleIndexes.add(incompatibleIndexes);
                    else {
                        Match combinedMatch = combineMatches(currentMatches);
                        if ((combinedMatch != null) && (combinedMatch.getScore() >= penaltyThreshold))
                            allMatchesFiltered.add(combinedMatch);
                    }
                }

                // Update matchIndexes to switch to the next combination on next iteration of outer loop
                for (int j = 0; j < numberOfOperands; j++) {
                    int currentIndex = matchIndexes[j];
                    if (currentIndex + 1 < allMatches.get(j).size()) {
                        matchIndexes[j] = currentIndex + 1;
                        break;
                    }
                    // we need to update next index and reset current index to zero
                    matchIndexes[j] = 0;
                }
            }
        } else {
            int[] operandOrder = conf.operandOrder();
            boolean allPortsFinished = false;
            while (!allPortsFinished) {
                // all variables with "Unordered" suffix must be converted with operandOrder[] before using as index
                int firstFoundNullIndexUnordered = numberOfOperands - 1;
                currentMatches = getMatchesByIndexes(matchIndexes);
                if (!alreadyReturned(matchIndexes)) {
                    if (Arrays.stream(currentMatches).noneMatch(Objects::isNull)) {
                        IncompatibleIndexes incompatibleIndexes = findIncompatibleIndexes(currentMatches, matchIndexes);
                        if (incompatibleIndexes == null) {
                            Match combinedMatch = combineMatches(currentMatches);
                            if ((combinedMatch != null) && (combinedMatch.getScore() >= penaltyThreshold))
                                allMatchesFiltered.add(combinedMatch);
                        }
                    } else
                        for (int indexUnordered = 0; indexUnordered < numberOfOperands - 1; indexUnordered++)
                            if (currentMatches[operandOrder[indexUnordered]] == null) {
                                firstFoundNullIndexUnordered = indexUnordered;
                                break;
                            }
                }

                // update matchIndexes
                if (currentMatches[operandOrder[firstFoundNullIndexUnordered]] == null) {
                    if (firstFoundNullIndexUnordered == 0)
                        allPortsFinished = true;
                    else {
                        matchIndexes[operandOrder[firstFoundNullIndexUnordered - 1]]++;
                        for (int indexUnordered = firstFoundNullIndexUnordered; indexUnordered < numberOfOperands;
                                indexUnordered++)
                            matchIndexes[operandOrder[indexUnordered]] = 0;
                    }
                } else
                    matchIndexes[operandOrder[numberOfOperands - 1]]++;
            }
        }

        return allMatchesFiltered;
    }

    /**
     * Returns null if this match combination is valid or IncompatibleIndexes structure if it finds
     * 2 matches that have incompatible ranges.
     *
     * @param matches group of matches to check
     * @param indexes indexes of all provided matches for writing to IncompatibleIndexes structure
     * @return IncompatibleIndexes structure
     */
    private IncompatibleIndexes findIncompatibleIndexes(Match[] matches, int[] indexes) {
        if (matches.length != indexes.length)
            throw new IllegalArgumentException("matches length is " + matches.length + ", indexes length is "
                + indexes.length + "; they must be equal!");

        IncompatibleIndexes result = null;
        switch (conf.matchValidationType) {
            case LOGICAL_OR:
            case LOGICAL_AND:
            case FIRST:
                break;
            case INTERSECTION:
                Range ranges[] = new Range[matches.length];

                OUTER:
                for (int i = 0; i < matches.length; i++) {
                    if (matches[i] == null) continue;
                    Range currentRange = matches[i].getRange();
                    ranges[i] = currentRange;
                    for (int j = 0; j < i; j++)     // Compare with all previously added matches
                        if (checkFullIntersection(ranges[i], ranges[j])
                                || checkOverlap(matches[0].getMatchedRange().getTarget(), ranges[i], ranges[j])) {
                            result = new IncompatibleIndexes(j, indexes[j], i, indexes[i]);
                            break OUTER;
                        }
                }
                break;
            case ORDER:
            case FOLLOWING:
                Range currentRange;
                Range previousRange;

                for (int i = 1; i < matches.length; i++) {
                    if (matches[i] == null) continue;
                    NSequenceWithQuality target = matches[0].getMatchedRange().getTarget();
                    currentRange = matches[i].getRange();
                    previousRange = matches[i - 1].getRange();
                    if ((previousRange.getLower() >= currentRange.getLower())
                            || checkFullIntersection(previousRange, currentRange)
                            || checkOverlap(target, previousRange, currentRange)
                            || checkInsertionPenalty(target, previousRange, currentRange)) {
                        result = new IncompatibleIndexes(i - 1, indexes[i - 1], i, indexes[i]);
                        break;
                    }
                }
        }

        return result;
    }

    /**
     * Check if this combination of indexes contains incompatible indexes. Incompatible means that we
     * already know that matches with that indexes have misplaced ranges.
     *
     * @param indexes indexes of matches
     * @return true if there are no incompatible indexes found; false if they are found
     */
    private boolean areCompatible(int[] indexes) {
        if (!conf.specificOutputPorts)
            for (IncompatibleIndexes currentIndexes : allIncompatibleIndexes)
                if ((indexes[currentIndexes.port1] == currentIndexes.index1)
                        && (indexes[currentIndexes.port2] == currentIndexes.index2))
                    return false;
        return true;
    }

    /**
     * Check if this combination of matches was already returned on first stages of unfair sorter.
     *
     * @param indexes indexes of matches
     * @return true if combination was already returned, otherwise false
     */
    private boolean alreadyReturned(int[] indexes) {
        if (!conf.fairSorting) {
            if (unfairReturnedCombinationsHashes.contains(Arrays.hashCode(indexes)))
                return true;
        }
        return false;
    }

    /**
     * Check is overlap too big to invalidate this combination of ranges.
     *
     * @return true if overlap is too big and this combination of ranges is invalid
     */
    private boolean checkOverlap(NSequenceWithQuality target, Range range0, Range range1) {
        PatternAligner patternAligner = conf.patternAligner;
        int maxOverlap = patternAligner.maxOverlap();
        Range intersection = range0.intersection(range1);
        return (intersection != null) && (((maxOverlap != -1) && (maxOverlap < intersection.length()))
                || (patternAligner.overlapPenalty(target, intersection.getLower(), intersection.length())
                    < patternAligner.penaltyThreshold()));
    }

    /**
     * Check is insertion penalty between ranges too big to invalidate this combination of ranges.
     *
     * @return true if insertion penalty is too big and this combination of ranges is invalid
     */
    private boolean checkInsertionPenalty(NSequenceWithQuality target, Range range0, Range range1) {
        PatternAligner patternAligner = conf.patternAligner;
        if (conf.matchValidationType == FOLLOWING) {
            int insertionLength = range1.getLower() - range0.getUpper();
            return (insertionLength > 0) && (patternAligner.insertionPenalty(target, range0.getUpper(), insertionLength)
                    < patternAligner.penaltyThreshold());
        } else return false;
    }

    private SpecificOutputPort getPortWithParams(int operandIndex) {
        return getPortWithParams(operandIndex, -1, -1);
    }

    /**
     * Get SpecificOutputPort for specified operand index, "from" and "to" coordinates for operand pattern match() call.
     *
     * @param operandIndex operand index
     * @param from from coordinate for operand pattern match() call, or -1 if conf.from() should be used
     * @param to to coordinate for operand pattern match() call, or -1 if conf.to() should be used
     * @return new SpecificOutputPort with specified parameters
     */
    private SpecificOutputPort getPortWithParams(int operandIndex, int from, int to) {
        SpecificOutputPort currentPort = unfairOutputPorts.stream().filter(p -> p.paramsEqualTo(operandIndex,
                from, to)).findFirst().orElse(null);
        if (currentPort == null) {
            Pattern currentPattern = conf.operandPatterns[operandIndex];
            int matchFrom = -1;
            int matchTo = -1;
            if (!conf.multipleReads) {
                if (from == -1)
                    matchFrom = conf.from();
                else if (from >= conf.from())
                    matchFrom = from;
                else
                    throw new IllegalStateException("getPortWithParams: from = " + from
                            + ", conf.from() = " + conf.from());
                if (to == -1)
                    matchTo = conf.to();
                else if (to <= conf.to())
                    matchTo = to;
                else
                    throw new IllegalStateException("getPortWithParams: to = " + to
                            + ", conf.to() = " + conf.to());
            }
            int portLimit = unfairSorterPortLimits.get(currentPattern.getClass());

            if ((conf.matchValidationType == FOLLOWING)
                    && (((operandIndex > 0) && (from != -1) && (to == -1))
                    || ((operandIndex < conf.operandPatterns.length - 1) && (from == -1) && (to != -1)))) {
                int patternMaxLength = ((SinglePattern)currentPattern).estimateMaxLength();
                if (patternMaxLength != -1) {
                    int maxOverlap = conf.patternAligner.maxOverlap();
                    boolean canEstimateMaxLength = false;
                    int extraMaxLength = 0;
                    if (maxOverlap == -1) {
                        int overlappingPatternIndex = (from == -1) ? operandIndex + 1 : operandIndex - 1;
                        int overlappingPatternMaxLength = ((SinglePattern)conf.operandPatterns[overlappingPatternIndex])
                                .estimateMaxLength();
                        if (overlappingPatternMaxLength != -1) {
                            extraMaxLength = overlappingPatternMaxLength - 1;
                            canEstimateMaxLength = true;
                        }
                    } else
                        canEstimateMaxLength = true;

                    if (canEstimateMaxLength) {
                        if (from == -1)
                            matchFrom = Math.max(conf.from(), matchTo - (patternMaxLength + extraMaxLength));
                        else
                            matchTo = Math.min(conf.to(), matchFrom + patternMaxLength + extraMaxLength);
                    }
                }
                portLimit = specificPortLimit;
            }

            currentPort = new SpecificOutputPort(conf.multipleReads
                    ? (conf.separateTargets
                        ? ((SinglePattern)currentPattern)
                            .match(conf.target.get(operandIndex)).getMatches(false)
                        : currentPattern.match(conf.target).getMatches(false))
                    : ((SinglePattern)currentPattern)
                        .match(conf.target.get(0), matchFrom, matchTo).getMatches(false),
                    operandIndex, from, to, portLimit);
            unfairOutputPorts.add(currentPort);
        }
        return currentPort;
    }

    /**
     * Get array of matches by array of match indexes in output ports.
     *
     * @param portValueIndexes array of indexes in output ports of pattern operands
     * @return array of matches
     */
    private Match[] getMatchesByIndexes(int[] portValueIndexes) {
        int numberOfOperands = conf.operandPatterns.length;
        if (portValueIndexes.length != numberOfOperands)
            throw new IllegalArgumentException("portValueIndexes length is " + portValueIndexes.length
                    + ", number of operands: " + numberOfOperands);
        Match[] matches = new Match[numberOfOperands];
        if (conf.specificOutputPorts) {
            int maxOverlap = conf.patternAligner.maxOverlap();
            int[] operandOrder = conf.operandOrder();
            int firstOperandIndex = operandOrder[0];
            matches[firstOperandIndex] = getPortWithParams(firstOperandIndex).get(portValueIndexes[firstOperandIndex]);
            for (int i = 1; i < numberOfOperands; i++) {
                int currentOperandIndex = operandOrder[i];
                Match previousMatch = matches[currentOperandIndex > firstOperandIndex
                        ? currentOperandIndex - 1 : currentOperandIndex + 1];
                Match currentMatch = null;
                if (previousMatch != null) {
                    Range previousMatchRange = previousMatch.getRange();
                    int previousMatchStart = previousMatchRange.getFrom();
                    int previousMatchEnd = previousMatchRange.getTo();
                    int estimatedMaxOverlap = Math.min(previousMatchRange.length() - 1,
                            maxOverlap == -1 ? Integer.MAX_VALUE : maxOverlap);
                    int thisMatchStart = -1;
                    int thisMatchEnd = -1;
                    if (currentOperandIndex > firstOperandIndex)
                        thisMatchStart = previousMatchEnd - estimatedMaxOverlap;
                    else
                        thisMatchEnd = previousMatchStart + estimatedMaxOverlap;
                    currentMatch = getPortWithParams(currentOperandIndex, thisMatchStart, thisMatchEnd)
                            .get(portValueIndexes[currentOperandIndex]);
                }
                matches[currentOperandIndex] = currentMatch;
            }
        } else
            for (int i = 0; i < numberOfOperands; i++)
                matches[i] = getPortWithParams(i).get(portValueIndexes[i]);

        return matches;
    }

    private static class IncompatibleIndexes {
        int port1;
        int index1;
        int port2;
        int index2;

        IncompatibleIndexes(int port1, int index1, int port2, int index2) {
            this.port1 = port1;
            this.index1 = index1;
            this.port2 = port2;
            this.index2 = index2;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof IncompatibleIndexes))
                return false;

            IncompatibleIndexes that = (IncompatibleIndexes) other;

            return (this.port1 == that.port1) && (this.port2 == that.port2)
                    && (this.index1 == that.index1) && (this.index2 == that.index2);
        }

        @Override
        public int hashCode() {
            int hashCode = 1;

            hashCode = hashCode * 37 + this.port1;
            hashCode = hashCode * 37 + this.port2;
            hashCode = hashCode * 37 + this.index1;
            hashCode = hashCode * 37 + this.index2;

            return hashCode;
        }
    }

    private class MatchesOutputPort implements OutputPort<Match> {
        private ArrayList<Match> allMatchesFiltered;
        private long penaltyThreshold = conf.patternAligner.penaltyThreshold();
        private int numberOfPatterns = conf.operandPatterns.length;
        private int filteredMatchesCount = 0;
        private int currentMatchIndex = 0;
        private boolean sortingPerformed = false;
        private boolean alwaysReturnNull = false;
        private int unfairSorterStage = 1;

        // data structures for stages 1 and 2 of unfair sorter
        private boolean stage1Init = false;
        private int[] currentIndexes;
        private boolean[] endedPorts;
        private Match[] zeroIndexMatches;
        private boolean stage2Init = false;
        private long[] previousMatchScores;
        private long[] currentMatchScores;

        @Override
        public Match take() {
            if (alwaysReturnNull) return null;
            if (conf.fairSorting) return takeSorted();
            if (++unfairSorterTakenValues > conf.unfairSorterLimit) {
                alwaysReturnNull = true;
                return null;
            }

            Match takenMatch;
            switch (unfairSorterStage) {
                case 1:
                    takenMatch = takeUnfairStage1();
                    if (takenMatch != null)
                        return takenMatch;
                    else if (!alwaysReturnNull)
                        unfairSorterStage++;
                    else
                        return null;
                case 2:
                    if (conf.specificOutputPorts)
                        unfairSorterStage++;
                    else {
                        takenMatch = takeUnfairStage2();
                        if (takenMatch != null)
                            return takenMatch;
                        else
                            unfairSorterStage++;
                    }
                default:
                    return takeSorted();
            }
        }

        private Match takeSorted() {
            if (!sortingPerformed) {
                allMatchesFiltered = takeFilteredMatches();
                filteredMatchesCount = allMatchesFiltered.size();
                allMatchesFiltered.sort(Comparator.comparingLong(Match::getScore).reversed());
                sortingPerformed = true;
            }

            if (currentMatchIndex >= filteredMatchesCount) {
                alwaysReturnNull = true;
                return null;
            } else
                return allMatchesFiltered.get(currentMatchIndex++);
        }

        /**
         * Stage 1: return combination of 1st values from each port, then combinations of other value from one port
         * and 1st values from other ports.
         *
         * @return match, or null if there are no more matches on stage 1
         */
        private Match takeUnfairStage1() {
            Match currentMatch = null;

            if (!stage1Init) {
                currentIndexes = new int[numberOfPatterns];
                endedPorts = new boolean[numberOfPatterns];
                zeroIndexMatches = getMatchesByIndexes(currentIndexes);
                for (int i = 0; i < numberOfPatterns; i++)
                    if (zeroIndexMatches[i] == null)
                        endedPorts[i] = true;
                currentMatch = takeMatchOrNull(currentIndexes);
                stage1Init = true;
            }

            while (currentMatch == null) {
                boolean indexFound = false;
                for (int i = 0; i < numberOfPatterns; i++) {
                    int currentIndex = currentIndexes[i];
                    if (currentIndex > 0) {
                        if (currentIndex < approximateSorterStage1Depth - 1) {
                            if (checkPortByIndexForStage1(i, currentIndex + 1)) {
                                currentIndexes[i]++;
                                indexFound = true;
                                break;
                            } else
                                endedPorts[i] = true;
                        }

                        currentIndexes[i] = 0;
                        int nextIndex = i - 1;
                        while (nextIndex >= 0) {
                            if (endedPorts[nextIndex])
                                nextIndex--;
                            else {
                                if (checkPortByIndexForStage1(nextIndex, 1))
                                    break;
                                else {
                                    endedPorts[nextIndex] = true;
                                    nextIndex--;
                                }
                            }
                        }
                        if (nextIndex == -1) {
                            // no more matches on stage 1
                            return null;
                        } else {
                            currentIndexes[nextIndex] = 1;
                            indexFound = true;
                            break;
                        }
                    }
                }
                if (!indexFound)
                    for (int i = numberOfPatterns - 1; i >= 0; i--)
                        if (!endedPorts[i]) {
                            if (checkPortByIndexForStage1(i, 1)) {
                                currentIndexes[i] = 1;
                                indexFound = true;
                                break;
                            } else
                                endedPorts[i] = true;
                        }
                if (!indexFound) {
                    // all ports ended
                    alwaysReturnNull = true;
                    return null;
                }

                currentMatch = takeMatchOrNull(currentIndexes);
            }

            return currentMatch;
        }

        /**
         * Return true if port has non-null match with specified index (depth), otherwise false.
         * For specificOutputPorts (when indexes are not fixed and depend on each other) all other indexes are 0 as it
         * defined on stage 1.
         *
         * @param port port number, same as pattern number in conf.operandPatterns array
         * @param index number of match in this port (depth)
         * @return true if port has non-null match with specified index (depth), otherwise false
         */
        private boolean checkPortByIndexForStage1(int port, int index) {
            if (conf.specificOutputPorts) {
                int[] indexes = new int[numberOfPatterns];
                indexes[port] = index;
                return getMatchesByIndexes(indexes)[port] != null;
            } else
                return getPortWithParams(port).get(index) != null;
        }

        /**
         *  Stage 2: iterate over ports, trying to pick better score, based on deltas if we count total score
         *  based on sum, or based on max value if we count total score based on max value.
         *  This stage is not used if conf.specificOutputPorts is true.
         *
         * @return match, or null if there are no more matches on stage 2
         */
        private Match takeUnfairStage2() {
            if (!stage2Init) {
                currentIndexes = new int[numberOfPatterns];
                previousMatchScores = new long[numberOfPatterns];
                currentMatchScores = new long[numberOfPatterns];
                for (int i = 0; i < numberOfPatterns; i++) {
                    endedPorts[i] = (zeroIndexMatches[i] == null) || (getPortWithParams(i).get(1) == null);
                    if (!endedPorts[i]) {
                        if (conf.combineScoresBySum) {
                            previousMatchScores[i] = zeroIndexMatches[i].getScore();
                            currentMatchScores[i] = getPortWithParams(i).get(1).getScore();
                        } else
                            currentMatchScores[i] = zeroIndexMatches[i].getScore();
                    }
                }
                stage2Init = true;
            }

            Match currentMatch = null;
            while (currentMatch == null) {
                int bestPort = 0;
                if (conf.combineScoresBySum) {
                    long bestDelta = Long.MIN_VALUE;
                    for (int i = 0; i < numberOfPatterns; i++) {
                        if (endedPorts[i])
                            continue;
                        long currentDelta = currentMatchScores[i] - previousMatchScores[i];
                        if (currentDelta > bestDelta) {
                            bestDelta = currentDelta;
                            bestPort = i;
                        }
                    }
                } else {
                    long bestScore = Long.MIN_VALUE;
                    for (int i = 0; i < numberOfPatterns; i++) {
                        if (endedPorts[i])
                            continue;
                        if (currentMatchScores[i] > bestScore) {
                            bestScore = currentMatchScores[i];
                            bestPort = i;
                        }
                    }
                }

                Match bestOperandMatch = getPortWithParams(bestPort).get(currentIndexes[bestPort] + 1);
                if (bestOperandMatch != null) {
                    if (conf.combineScoresBySum) {
                        if (currentIndexes[bestPort] > 0) {
                            previousMatchScores[bestPort] = currentMatchScores[bestPort];
                            currentMatchScores[bestPort] = bestOperandMatch.getScore();
                        }
                    } else
                        currentMatchScores[bestPort] = bestOperandMatch.getScore();
                    currentIndexes[bestPort]++;
                } else {
                    endedPorts[bestPort] = true;
                    boolean allPortsEnded = true;
                    for (boolean endedPort : endedPorts)
                        if (!endedPort) {
                            allPortsEnded = false;
                            break;
                        }
                    if (allPortsEnded) {
                        // no more matches on stage 2
                        return null;
                    }
                }

                currentMatch = takeMatchOrNull(currentIndexes);
            }

            return currentMatch;
        }

        private Match takeMatchOrNull(int[] indexes) {
            Match[] currentMatches;
            if (areCompatible(indexes)) {
                if (!alreadyReturned(indexes)) {
                    rememberReturnedCombination(indexes);
                    currentMatches = getMatchesByIndexes(indexes);

                    boolean invalidCombination = false;
                    for (int i = 0; i < numberOfPatterns; i++)
                        if (((indexes[i] > 0) || !areNullMatchesAllowed()) && (currentMatches[i] == null)) {
                            invalidCombination = true;
                            break;
                        }
                    if (!invalidCombination) {
                        IncompatibleIndexes incompatibleIndexes = findIncompatibleIndexes(currentMatches, indexes);
                        if (incompatibleIndexes != null) {
                            // if conf.specificOutputPorts is true, indexes are not fixed; no need to remember them
                            if (!conf.specificOutputPorts)
                                allIncompatibleIndexes.add(incompatibleIndexes);
                        } else {
                            Match combinedMatch = combineMatches(currentMatches);
                            if ((combinedMatch != null) && (combinedMatch.getScore() >= penaltyThreshold))
                                return combinedMatch;
                        }
                    }
                }
            } else
                rememberReturnedCombination(indexes);

            return null;
        }

        private void rememberReturnedCombination(int[] indexes) {
            unfairReturnedCombinationsHashes.add(Arrays.hashCode(indexes));
        }
    }
}
