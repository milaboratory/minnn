package com.milaboratory.mist.util;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.pattern.*;

import java.util.*;

import static com.milaboratory.mist.pattern.MatchValidationType.*;
import static com.milaboratory.mist.pattern.PatternUtils.minValid;
import static com.milaboratory.mist.util.RangeTools.*;
import static com.milaboratory.mist.util.UnfairSorterConfiguration.*;

public final class ApproximateSorter {
    private final ApproximateSorterConfiguration conf;
    private final OutputPort<MatchIntermediate> matchesOutputPort;
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
    public OutputPort<MatchIntermediate> getOutputPort() {
        return matchesOutputPort;
    }

    /**
     * Get combined match from a group of input matches. It uses multipleReads flag to determine how to combine matches
     * (by combining ranges for single read or by numbering the matched ranges for multiple reads).
     *
     * @param matches input matches
     * @return combined match
     */
    private MatchIntermediate combineMatches(MatchIntermediate... matches) {
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();

        if (conf.multipleReads) {
            ArrayList<MatchedRange> matchedRanges = new ArrayList<>();
            int patternIndex = 0;
            boolean allMatchesAreNull = true;
            for (MatchIntermediate match : matches) {
                if (match == null) {
                    if (conf.matchValidationType == LOGICAL_OR) {
                        matchedRanges.add(new NullMatchedRange(patternIndex++));
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
                        matchedRanges.add(new NullMatchedRange(patternIndex++));
                    } else {
                        matchedRanges.add(new MatchedRange(currentMatchedRange.getTarget(),
                                currentMatchedRange.getTargetId(), patternIndex, currentMatchedRange.getRange()));
                        for (MatchedGroupEdge matchedGroupEdge : match.getMatchedGroupEdgesByPattern(i)) {
                            // remove duplicated R1, R2... group edges from results
                            if (matchedGroupEdges.stream().map(MatchedGroupEdge::getGroupEdge)
                                    .noneMatch(ge -> ge.equals(matchedGroupEdge.getGroupEdge())))
                                matchedGroupEdges.add(new MatchedGroupEdge(matchedGroupEdge.getTarget(),
                                        matchedGroupEdge.getTargetId(), patternIndex, matchedGroupEdge.getGroupEdge(),
                                        matchedGroupEdge.getPosition()));
                        }
                        patternIndex++;
                    }
                }
            }

            if (allMatchesAreNull)
                return null;
            else
                return new MatchIntermediate(patternIndex, combineMatchScores(matches),
                        -1, -1, matchedGroupEdges,
                        matchedRanges.toArray(new MatchedRange[matchedRanges.size()]));
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
            /* combining matches for FOLLOWING, ORDER and INTERSECTION:
               matches must already be checked for compatibility with findIncompatibleIndexes() */
            NSequenceWithQuality target = matches[0].getMatchedRange().getTarget();
            byte targetId = matches[0].getMatchedRange().getTargetId();
            ArrayList<ArrayList<MatchedGroupEdge>> matchedGroupEdgesFromOperands = new ArrayList<>();

            for (int i = 0; i < matches.length; i++) {
                matchedGroupEdgesFromOperands.add(new ArrayList<>());
                matchedGroupEdgesFromOperands.get(i).addAll(matches[i].getMatchedGroupEdges());
            }

            MatchIntermediate[] sortedMatches = matches.clone();
            Arrays.sort(sortedMatches, Comparator.comparingInt(m -> m.getRange().getLower()));

            long rangesCombinationPenalty = 0;
            for (int i = 0; i < sortedMatches.length; i++) {
                Range rangeI = sortedMatches[i].getRange();
                int maxIntersection = 0;
                for (int j = i - 1; j >= 0; j--) {
                    Range rangeJ = sortedMatches[j].getRange();
                    Range intersection = rangeI.intersection(rangeJ);
                    if (intersection != null) {
                        rangesCombinationPenalty += conf.patternAligner.overlapPenalty(target, intersection.getLower(),
                                intersection.length());
                        maxIntersection = Math.max(maxIntersection, intersection.length());
                    }
                    if ((conf.matchValidationType == FOLLOWING) && (j == i - 1)
                            && (rangeI.getLower() > rangeJ.getUpper()))
                        rangesCombinationPenalty += conf.patternAligner.insertionPenalty(target, rangeJ.getUpper(),
                                rangeI.getLower() - rangeJ.getUpper());
                }
                if (maxIntersection > 0) {
                    for (MatchedGroupEdge matchedGroupEdge : matchedGroupEdgesFromOperands.get(i)) {
                        if (matchedGroupEdge.getPosition() >= rangeI.getLower() + maxIntersection)
                            matchedGroupEdges.add(matchedGroupEdge);
                        else
                            matchedGroupEdges.add(matchedGroupEdge.overridePosition(rangeI.getLower()
                                    + maxIntersection));
                    }
                } else
                    matchedGroupEdges.addAll(matchedGroupEdgesFromOperands.get(i));
            }

            MatchedRange matchedRange = new MatchedRange(target, targetId, 0, combineRanges(sortedMatches));

            return new MatchIntermediate(1, combineMatchScores(matches) + rangesCombinationPenalty,
                    sortedMatches[0].getLeftUppercaseDistance(),
                    sortedMatches[sortedMatches.length - 1].getRightUppercaseDistance(),
                    matchedGroupEdges, matchedRange);
        }
    }

    /**
     * Combine match scores. It is used in combineMatches function. Different patterns may combine operand scores
     * by sum or by max value of operand scores.
     *
     * @param matches matches from which we will get the scores
     * @return combined score
     */
    private long combineMatchScores(MatchIntermediate... matches) {
        long resultScore;
        if (conf.combineScoresBySum) {
            resultScore = 0;
            for (MatchIntermediate match : matches)
                if (match != null)
                    resultScore += match.getScore();
        } else {
            resultScore = Long.MIN_VALUE;
            for (MatchIntermediate match : matches)
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
    private ArrayList<MatchIntermediate> takeFilteredMatches() {
        ArrayList<MatchIntermediate> allMatchesFiltered = new ArrayList<>();
        long penaltyThreshold = conf.patternAligner.penaltyThreshold();
        int numberOfOperands = conf.operandPatterns.length;
        int[] matchIndexes = new int[numberOfOperands];
        MatchIntermediate[] currentMatches = new MatchIntermediate[numberOfOperands];

        if (conf.fairSorting || !conf.specificOutputPorts) {
            ArrayList<ArrayList<MatchIntermediate>> allMatches = new ArrayList<>();
            OutputPort<MatchIntermediate> currentPort;
            ArrayList<MatchIntermediate> currentPortMatchesList;
            MatchIntermediate currentMatch;
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
                        MatchIntermediate combinedMatch = combineMatches(currentMatches);
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
                if (!alreadyReturned(matchIndexes) && Arrays.stream(currentMatches).noneMatch(Objects::isNull)) {
                    IncompatibleIndexes incompatibleIndexes = findIncompatibleIndexes(currentMatches, matchIndexes);
                    if (incompatibleIndexes == null) {
                        MatchIntermediate combinedMatch = combineMatches(currentMatches);
                        if ((combinedMatch != null) && (combinedMatch.getScore() >= penaltyThreshold))
                            allMatchesFiltered.add(combinedMatch);
                    }
                } else
                    for (int indexUnordered = 0; indexUnordered < numberOfOperands - 1; indexUnordered++)
                        if (currentMatches[operandOrder[indexUnordered]] == null) {
                            firstFoundNullIndexUnordered = indexUnordered;
                            break;
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
    private IncompatibleIndexes findIncompatibleIndexes(MatchIntermediate[] matches, int[] indexes) {
        if (matches.length != indexes.length)
            throw new IllegalArgumentException("matches length is " + matches.length + ", indexes length is "
                + indexes.length + "; they must be equal!");

        NSequenceWithQuality target;
        IncompatibleIndexes result = null;
        switch (conf.matchValidationType) {
            case LOGICAL_OR:
            case LOGICAL_AND:
            case FIRST:
                break;
            case INTERSECTION:
                target = matches[0].getMatchedRange().getTarget();
                Range ranges[] = new Range[matches.length];

                OUTER:
                for (int i = 0; i < matches.length; i++) {
                    ranges[i] = matches[i].getRange();
                    for (int j = 0; j < i; j++)     // Compare with all previously added matches
                        if (checkFullIntersection(ranges[i], ranges[j])
                                || checkOverlap(target, matches[i], matches[j])) {
                            result = new IncompatibleIndexes(j, indexes[j], i, indexes[i]);
                            break OUTER;
                        }
                }
                break;
            case ORDER:
            case FOLLOWING:
                target = matches[0].getMatchedRange().getTarget();
                MatchIntermediate currentMatch;
                MatchIntermediate previousMatch;
                Range currentRange;
                Range previousRange;

                for (int i = 1; i < matches.length; i++) {
                    currentMatch = matches[i];
                    previousMatch = matches[i - 1];
                    currentRange = currentMatch.getRange();
                    previousRange = previousMatch.getRange();
                    if ((previousRange.getLower() >= currentRange.getLower())
                            || checkFullIntersection(previousRange, currentRange)
                            || checkOverlap(target, previousMatch, currentMatch)
                            || ((conf.matchValidationType == FOLLOWING)
                                && checkInsertion(target, previousMatch, currentMatch))) {
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
        return !conf.fairSorting && unfairReturnedCombinationsHashes.contains(Arrays.hashCode(indexes));
    }

    /**
     * Check is overlap too big to invalidate this combination of matches.
     *
     * @return true if overlap is too big and this combination of matches is invalid
     */
    private boolean checkOverlap(NSequenceWithQuality target, MatchIntermediate match0, MatchIntermediate match1) {
        Range intersection = match0.getRange().intersection(match1.getRange());
        if (intersection == null)
            return false;
        else {
            PatternAligner patternAligner = conf.patternAligner;
            int overlap = intersection.length();
            int maxOverlap = (patternAligner.maxOverlap() == -1) ? Integer.MAX_VALUE : patternAligner.maxOverlap();
            int maxOverlapLeft, maxOverlapRight;
            if (match0.getRange().getLower() < match1.getRange().getLower()) {
                maxOverlapLeft = (match0.getRightUppercaseDistance() == -1) ? Integer.MAX_VALUE
                        : match0.getRightUppercaseDistance() - 1;
                maxOverlapRight = (match1.getLeftUppercaseDistance() == -1) ? Integer.MAX_VALUE
                        : match1.getLeftUppercaseDistance() - 1;
            } else {
                maxOverlapLeft = (match1.getRightUppercaseDistance() == -1) ? Integer.MAX_VALUE
                        : match1.getRightUppercaseDistance() - 1;
                maxOverlapRight = (match0.getLeftUppercaseDistance() == -1) ? Integer.MAX_VALUE
                        : match0.getLeftUppercaseDistance() - 1;
            }
            maxOverlap = Math.min(maxOverlap, Math.min(maxOverlapLeft, maxOverlapRight));
            return (maxOverlap < overlap) || (patternAligner.overlapPenalty(target, intersection.getLower(), overlap)
                    < patternAligner.penaltyThreshold());
        }
    }

    /**
     * Check is insertion between matches too big to invalidate this combination of matches.
     * Must be called only for FOLLOWING match validation type!
     *
     * @return true if insertion penalty is too big and this combination of matches is invalid
     */
    private boolean checkInsertion(NSequenceWithQuality target,
                                   MatchIntermediate leftMatch, MatchIntermediate rightMatch) {
        int insertionLength = rightMatch.getRange().getLower() - leftMatch.getRange().getUpper();
        if (insertionLength <= 0)
            return false;
        else {
            PatternAligner patternAligner = conf.patternAligner;
            return (patternAligner.insertionPenalty(target, leftMatch.getRange().getUpper(), insertionLength)
                        < patternAligner.penaltyThreshold())
                    || (leftMatch.getRightUppercaseDistance() == 0) || (rightMatch.getLeftUppercaseDistance() == 0);
        }
    }

    /**
     * Estimate max overlap for current operand pattern.
     *
     * @param operandIndex index of current operand pattern in operandPatterns array
     * @param previousMatch found match of other overlapping pattern
     * @param overlappingPatternIsLeft true if the other overlapping pattern is on the left from current
     * @return estimated max overlap; 0 or positive
     */
    private int estimateMaxOverlap(int operandIndex, MatchIntermediate previousMatch,
                                   boolean overlappingPatternIsLeft) {
        return minValid(conf.patternAligner.maxOverlap(), previousMatch.getRange().length() - 1,
                overlappingPatternIsLeft ? previousMatch.getRightUppercaseDistance()
                        : previousMatch.getLeftUppercaseDistance(),
                ((SinglePattern)conf.operandPatterns[operandIndex]).estimateMaxOverlap());
    }

    private SpecificOutputPort getPortWithParams(int operandIndex) {
        return getPortWithParams(operandIndex, -1, -1, -1);
    }

    /**
     * Get SpecificOutputPort for specified operand index, "from" and "to" coordinates for operand pattern match() call.
     *
     * @param operandIndex operand index
     * @param from from coordinate for operand pattern match() call, or -1 if conf.from() should be used
     * @param to to coordinate for operand pattern match() call, or -1 if conf.to() should be used
     * @param estimatedMaxOverlap estimated max overlap between this and previous match, or -1 if not used
     * @return new SpecificOutputPort with specified parameters
     */
    private SpecificOutputPort getPortWithParams(int operandIndex, int from, int to, int estimatedMaxOverlap) {
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
                    if (from == -1)
                        matchFrom = Math.max(conf.from(), matchTo - patternMaxLength - estimatedMaxOverlap);
                    else
                        matchTo = Math.min(conf.to(), matchFrom + patternMaxLength + estimatedMaxOverlap);
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
    private MatchIntermediate[] getMatchesByIndexes(int[] portValueIndexes) {
        int numberOfOperands = conf.operandPatterns.length;
        if (portValueIndexes.length != numberOfOperands)
            throw new IllegalArgumentException("portValueIndexes length is " + portValueIndexes.length
                    + ", number of operands: " + numberOfOperands);
        MatchIntermediate[] matches = new MatchIntermediate[numberOfOperands];
        if (conf.specificOutputPorts) {
            int[] operandOrder = conf.operandOrder();
            int firstOperandIndex = operandOrder[0];
            matches[firstOperandIndex] = getPortWithParams(firstOperandIndex).get(portValueIndexes[firstOperandIndex]);
            for (int i = 1; i < numberOfOperands; i++) {
                int currentOperandIndex = operandOrder[i];
                boolean previousMatchIsLeft = currentOperandIndex > firstOperandIndex;
                MatchIntermediate previousMatch = matches[previousMatchIsLeft ? currentOperandIndex - 1
                        : currentOperandIndex + 1];
                MatchIntermediate currentMatch = null;
                if (previousMatch != null) {
                    Range previousMatchRange = previousMatch.getRange();
                    int previousMatchStart = previousMatchRange.getFrom();
                    int previousMatchEnd = previousMatchRange.getTo();
                    int estimatedMaxOverlap = estimateMaxOverlap(currentOperandIndex, previousMatch,
                            previousMatchIsLeft);
                    int thisMatchStart = -1;
                    int thisMatchEnd = -1;
                    if (previousMatchIsLeft)
                        thisMatchStart = Math.max(conf.from(), previousMatchEnd - estimatedMaxOverlap);
                    else
                        thisMatchEnd = Math.min(conf.to(), previousMatchStart + estimatedMaxOverlap);
                    currentMatch = getPortWithParams(currentOperandIndex, thisMatchStart, thisMatchEnd,
                            estimatedMaxOverlap).get(portValueIndexes[currentOperandIndex]);
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

    private class MatchesOutputPort implements OutputPort<MatchIntermediate> {
        private ArrayList<MatchIntermediate> allMatchesFiltered;
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
        private MatchIntermediate[] zeroIndexMatches;
        private boolean stage2Init = false;
        private long[] previousMatchScores;
        private long[] currentMatchScores;

        @Override
        public MatchIntermediate take() {
            if (alwaysReturnNull) return null;
            if (conf.fairSorting) return takeSorted();
            if (++unfairSorterTakenValues > conf.unfairSorterLimit) {
                alwaysReturnNull = true;
                return null;
            }

            MatchIntermediate takenMatch;
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

        private MatchIntermediate takeSorted() {
            if (!sortingPerformed) {
                allMatchesFiltered = takeFilteredMatches();
                filteredMatchesCount = allMatchesFiltered.size();
                allMatchesFiltered.sort(Comparator.comparingLong(MatchIntermediate::getScore).reversed());
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
        private MatchIntermediate takeUnfairStage1() {
            MatchIntermediate currentMatch = null;

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
        private MatchIntermediate takeUnfairStage2() {
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

            MatchIntermediate currentMatch = null;
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

                MatchIntermediate bestOperandMatch = getPortWithParams(bestPort).get(currentIndexes[bestPort] + 1);
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

        private MatchIntermediate takeMatchOrNull(int[] indexes) {
            MatchIntermediate[] currentMatches;
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
                            MatchIntermediate combinedMatch = combineMatches(currentMatches);
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
