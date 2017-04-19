package com.milaboratory.mist.util;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.mist.pattern.Match;
import com.milaboratory.mist.pattern.MatchValidationType;

public class SorterByCoordinate extends ApproximateSorter {
    private boolean sequentialCoordinates;

    /**
     * Sorter by coordinate.
     *
     * @param multipleReads true if we combine matches from multiple reads; false if we combine matches
     *                      from single read
     * @param combineScoresBySum true if combined score must be equal to sum of match scores; false if combined
     *                           score must be the highest of match scores
     * @param fairSorting true if we need slow but fair sorting
     * @param matchValidationType type of validation used to determine that current matches combination is invalid
     * @param sequentialCoordinates true if we use sequential coordinates only (Plus pattern), otherwise false
     * @param inputPorts ports for input matches; we assume that they are already sorted, maybe approximately
     */
    public SorterByCoordinate(boolean multipleReads, boolean combineScoresBySum, boolean fairSorting,
                              MatchValidationType matchValidationType, boolean sequentialCoordinates,
                              OutputPort<Match>[] inputPorts) {
        super(multipleReads, combineScoresBySum, fairSorting, matchValidationType, inputPorts);
        this.sequentialCoordinates = sequentialCoordinates;
    }

    @Override
    public OutputPort<Match> getOutputPort() {
        return null;
    }
}
