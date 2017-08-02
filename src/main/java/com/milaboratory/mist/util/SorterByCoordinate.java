package com.milaboratory.mist.util;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.mist.pattern.*;

import java.util.*;

import static com.milaboratory.mist.pattern.MatchValidationType.*;

public final class SorterByCoordinate extends ApproximateSorter {
    /**
     * Sorter by coordinate.
     *
     * @param patternAligner pattern aligner that provides information about scoring and pattern overlap limits
     * @param multipleReads true if we combine matches from multiple reads; false if we combine matches
     *                      from single read
     * @param combineScoresBySum true if combined score must be equal to sum of match scores; false if combined
     *                           score must be the highest of match scores
     * @param fairSorting true if we need slow but fair sorting
     * @param matchValidationType type of validation used to determine that current matches combination is invalid
     */
    public SorterByCoordinate(PatternAligner patternAligner, boolean multipleReads, boolean combineScoresBySum,
                              boolean fairSorting, MatchValidationType matchValidationType) {
        super(patternAligner, multipleReads, combineScoresBySum, fairSorting, matchValidationType);
    }

    @Override
    public OutputPort<Match> getOutputPort(List<ApproximateSorterOperandPort> inputPorts) {
        int numberOfPorts = inputPorts.size();
        if (numberOfPorts == 0)
            throw new IllegalArgumentException("List of input ports is empty!");
        return new MatchesOutputPort(inputPorts, numberOfPorts);
    }

    private class MatchesOutputPort implements OutputPort<Match> {
        private final ArrayList<ArrayList<Match>> takenMatches;
        private final List<ApproximateSorterOperandPort> inputPorts;
        private final int numberOfPorts;
        private ArrayList<Integer> currentIndexes;
        private final Match[] currentMatches;
        private final TableOfIterations tableOfIterations;
        private boolean alwaysReturnNull = false;

        // data structures used for fair sorting
        private Match[] allMatchesFiltered;
        private int filteredMatchesCount = 0;
        private int nextFairSortedMatch = 0;
        private boolean sortingPerformed = false;

        MatchesOutputPort(List<ApproximateSorterOperandPort> inputPorts, int numberOfPorts) {
            this.takenMatches = new ArrayList<>();
            for (int i = 0; i < numberOfPorts; i++)
                this.takenMatches.add(new ArrayList<>());
            this.inputPorts = inputPorts;
            this.numberOfPorts = numberOfPorts;
            this.currentIndexes = new ArrayList<>(Collections.nCopies(numberOfPorts, 0));
            this.currentMatches = new Match[numberOfPorts];
            this.tableOfIterations = new TableOfIterations(numberOfPorts);
        }

        @Override
        public Match take() {
            Match combinedMatch = null;

            if (alwaysReturnNull) return null;
            if (fairSorting) return takeFairSorted();

            boolean combinationFound = false;
            GET_NEXT_COMBINATION:
            while (!combinationFound) {
                if (tableOfIterations.getTotalCombinationsCount() == tableOfIterations.getNumberOfReturnedCombinations()) {
                    alwaysReturnNull = true;
                    return null;
                }

                for (int i = 0; i < numberOfPorts; i++) {
                    ArrayList<Match> currentPortMatches = takenMatches.get(i);
                    ApproximateSorterOperandPort currentPort = inputPorts.get(i);
                    // if we didn't take the needed match before, take it now
                    if (currentIndexes.get(i) == currentPortMatches.size()) {
                        Match takenMatch = currentPort.outputPort.take();
                        if (takenMatch == null) {
                            if (currentPortMatches.size() == 0) {
                                if (areNullMatchesAllowed()) {
                                    currentPortMatches.add(null);
                                    tableOfIterations.setPortEndReached(i, 1);
                                    currentIndexes.set(i, 0);
                                } else {
                                    alwaysReturnNull = true;
                                    return null;
                                }
                            } else {
                                int currentIndex = currentIndexes.get(i);
                                tableOfIterations.setPortEndReached(i, currentIndex);
                                currentIndexes.set(i, currentIndex - 1);
                                calculateNextIndexes();
                                continue GET_NEXT_COMBINATION;
                            }
                        } else {
                            currentPortMatches.add(takenMatch);
                            if (currentPortMatches.size() == currentPort.unfairSorterPortLimit)
                                tableOfIterations.setPortEndReached(i, currentPort.unfairSorterPortLimit);
                        }
                    }
                    currentMatches[i] = currentPortMatches.get(currentIndexes.get(i));
                }

                IncompatibleIndexes incompatibleIndexes = findIncompatibleIndexes(currentMatches, currentIndexes);
                if (incompatibleIndexes == null) {
                    combinedMatch = combineMatches(true, currentMatches);
                    if ((combinedMatch != null) && (combinedMatch.getScore() >= patternAligner.penaltyThreshold()))
                        combinationFound = true;
                    else {
                        /* current combination doesn't fit the score threshold, mark it as returned
                         and continue search */
                        tableOfIterations.addReturnedCombination(currentIndexes);
                        calculateNextIndexes();
                    }
                } else {
                    /* mark invalid match as already returned in table of iterations, write found
                     incompatible indexes to table of iterations and continue search */
                    tableOfIterations.addReturnedCombination(currentIndexes);
                    tableOfIterations.addIncompatibleIndexes(incompatibleIndexes);
                    calculateNextIndexes();
                }
            }
            tableOfIterations.addReturnedCombination(currentIndexes);
            calculateNextIndexes();
            return combinedMatch;
        }

        private Match takeFairSorted() {
            if (!sortingPerformed) {
                allMatchesFiltered = fillArrayForFairSorting(inputPorts, numberOfPorts, false);
                filteredMatchesCount = allMatchesFiltered.length;
                if (!multipleReads)
                    Arrays.sort(allMatchesFiltered, Comparator.comparingInt(match -> match.getRange().getLower()));
                else
                    Arrays.sort(allMatchesFiltered, Comparator.comparingInt(this::getMatchCoordinateWeight));
                sortingPerformed = true;
            }

            if (nextFairSortedMatch >= filteredMatchesCount) return null;

            return allMatchesFiltered[nextFairSortedMatch++];
        }

        /**
         * Get weight from match to perform fair sorting for multiple pattern matches by coordinate.
         * In multiple patterns, getWholePatternMatch() can return null values.
         *
         * @return weight: lower end of the range in the first non-null match
         */
        private int getMatchCoordinateWeight(Match match) {
            for (int i = 0; i < match.getNumberOfPatterns(); i++) {
                MatchedRange currentMatch = match.getMatchedRange(i);
                if (NullMatchedRange.class.isAssignableFrom(currentMatch.getClass())) continue;
                return currentMatch.getRange().getLower();
            }

            return 0;
        }

        /**
         * Calculate next indexes for matches arrays: which next combination will be returned.
         */
        private void calculateNextIndexes() {
            /* If all combinations already iterated, there is nothing to calculate */
            if (tableOfIterations.getTotalCombinationsCount() == tableOfIterations.getNumberOfReturnedCombinations())
                return;
            /* Iterate through port values, starting from the last port, ending with the first,
            and skipping all combinations with incompatible indexes */
            ArrayList<Integer> innerArrayIndexes = new ArrayList<>(Collections.nCopies(numberOfPorts, 0));
            while (true) {
                if (!tableOfIterations.isCombinationReturned(innerArrayIndexes)
                        && (combinationContainsUnfinishedPort(innerArrayIndexes)
                        || tableOfIterations.isCompatible(matchValidationType == ORDER, innerArrayIndexes))) {
                    currentIndexes = innerArrayIndexes;
                    return;
                }

                // Update innerArrayIndexes to switch to the next combination on next iteration of outer loop
                for (int i = numberOfPorts - 1; i >= 0; i--) {
                    int currentIndex = innerArrayIndexes.get(i);
                    if (!tableOfIterations.isPortEndReached(i)
                            || (currentIndex + 1 < tableOfIterations.getPortMatchesQuantity(i))) {
                        innerArrayIndexes.set(i, currentIndex + 1);
                        break;
                    }
                    // we need to update next index and reset current index to zero
                    innerArrayIndexes.set(i, 0);
                    // if we looped through all combinations, stop the search
                    if (i == 0) {
                        alwaysReturnNull = true;
                        return;
                    }
                }
            }
        }

        /**
         * Returns true if combination contains at least 1 port from which the needed value has not been taken.
         *
         * @param indexes indexes for ports
         * @return true if there is a port for which we need to take the value
         */
        private boolean combinationContainsUnfinishedPort(ArrayList<Integer> indexes) {
            for (int i = 0; i < numberOfPorts; i++) {
                if (!tableOfIterations.isPortEndReached(i) && (indexes.get(i) == takenMatches.get(i).size()))
                    return true;
            }
            return false;
        }
    }
}
