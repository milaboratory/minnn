package com.milaboratory.mist.pattern;

import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;

public interface PatternAligner {
    /**
     * Returned alignment will have maximal score equals zero.
     *
     * @param pattern case sensitive nucleotide sequence from pattern
     * @param target target nucleotide sequence with quality
     * @param rightMatchPosition right position of found bitap match, inclusive
     * @return alignment with score less or equals than zero
     */
    Alignment<NucleotideSequenceCaseSensitive> align(NucleotideSequenceCaseSensitive pattern,
            NSequenceWithQuality target, int rightMatchPosition);
    /**
     * Penalty threshold, negative value.
     *
     * @return penalty threshold
     */
    long penaltyThreshold();

    /**
     * Calculate penalty value for given overlap in the target.
     *
     * @param target        target
     * @param overlapOffset offset inclusive
     * @param overlapLength length
     * @return negative penalty value
     */
    long overlapPenalty(NSequenceWithQuality target, int overlapOffset, int overlapLength);

    /**
     * Calculate penalty value for given insertion in the target.
     *
     * @param target          target
     * @param insertionOffset offset inclusive
     * @param insertionLength length
     * @return negative penalty value
     */
    long insertionPenalty(NSequenceWithQuality target, int insertionOffset, int insertionLength);

    /**
     * Get penalty value for given number of motif repeats for RepeatPattern match.
     *
     * @param motif motif of this RepeatPattern
     * @param repeats number of motif repeats for this RepeatPattern match
     * @param maxRepeats maximum number of motif repeats for this RepeatPattern
     * @return negative penalty value
     */
    default long repeatsPenalty(NucleotideSequenceCaseSensitive motif, int repeats, int maxRepeats) {
        return 0;
    }

    /**
     * Max errors to use in bitap matcher.
     *
     * @return max errors for bitap
     */
    int bitapMaxErrors();

    /**
     * Score for result of Not operator.
     *
     * @return not result score
     */
    default long notResultScore() {
        return 0;
    }

    /**
     * Maximal allowed overlap for two intersecting sub-patterns
     *
     * @return max overlap; or -1 if no limit on overlap size
     */
    default int maxOverlap() {
        return -1;
    }

    /**
     * Fixed left border position for alignment if it is specified; -1 if not specified.
     * If left border is specified, global aligner is used.
     *
     * @return left border position for alignment if it is specified; -1 if not specified
     */
    default int leftBorder() { return -1; }

    /**
     * Return new pattern with more strict penalty threshold.
     *
     * @return copy of this PatternAligner with more strict penalty threshold
     */
    PatternAligner overridePenaltyThreshold(long newThresholdValue);

    /**
     * Set left border for alignment. When it is set, global aligner is used.
     *
     * @param leftBorder left border for alignment
     * @return copy of this PatternAligner with fixed left border
     */
    PatternAligner setLeftBorder(int leftBorder);
}
