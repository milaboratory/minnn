package com.milaboratory.mist.pattern;

import com.milaboratory.core.sequence.MultiNSequenceWithQuality;

import java.util.ArrayList;

public abstract class Pattern {
    protected final PatternAligner patternAligner;

    Pattern(PatternAligner patternAligner) {
        this.patternAligner = patternAligner;
    }

    /**
     * Search for matches for this pattern in the input.
     *
     * @param target nucleotide sequence(s), where the search for the pattern will be performed
     * @return matching result, lazy object that contains functions that will perform actual calculations to find matches
     */
    public abstract MatchingResult match(MultiNSequenceWithQuality target);

    /**
     * Get list of group edges that are inside this pattern.
     *
     * @return list of group edges
     */
    public abstract ArrayList<GroupEdge> getGroupEdges();

    public PatternAligner getPatternAligner() {
        return patternAligner;
    }

    /**
     * Estimate computational complexity of this pattern for unfair sorter.
     *
     * @return estimated computational complexity: bigger values mean higher complexity
     */
    public abstract long estimateComplexity();
}
