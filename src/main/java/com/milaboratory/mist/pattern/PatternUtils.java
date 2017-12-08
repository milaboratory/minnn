package com.milaboratory.mist.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class PatternUtils {
    public static int invertCoordinate(int x) {
        return -2 - x;
    }

    /**
     * Returns minimal value from valid values when -1 represents invalid value, or -1 if all values are invalid.
     *
     * @param values values
     * @return minimal value from valid values when -1 represents invalid value, or -1 if all values are invalid
     */
    public static int minValid(int... values) {
        int result = -1;
        for (int value : values)
            if (value != -1) {
                if (result == -1)
                    result = value;
                else
                    result = Math.min(result, value);
            }
        return result;
    }

    /**
     * Fix group edge positions to make them not get beyond the right border of pattern sequence; and move group
     * edge positions if specified.
     *
     * @param groupEdgePositions group edge positions
     * @param move if not 0, add this value to all group edge positions; but never move positions below 0
     * @param maxPosition maximum allowed position for group edge; this is size of current sequence
     * @return new group edge positions
     */
    static List<GroupEdgePosition> fixGroupEdgePositions(List<GroupEdgePosition> groupEdgePositions,
                                                         int move, int maxPosition) {
        return groupEdgePositions.stream()
                .map(gp -> (move == 0) ? gp : new GroupEdgePosition(gp.getGroupEdge(),
                        (gp.getPosition() + move >= 0) ? gp.getPosition() + move : 0))
                .map(gp -> (gp.getPosition() <= maxPosition) ? gp
                        : new GroupEdgePosition(gp.getGroupEdge(), maxPosition))
                .collect(Collectors.toList());
    }

    /**
     * Generate match from alignment, for FuzzyMatchPattern and RepeatPattern.
     *
     * @param alignment alignment for pattern and target
     * @param target target
     * @param targetId target id, comes from SinglePattern (FuzzyMatch or Repeat) that generates this match
     * @param firstUppercase position of the first uppercase letter in the pattern; or -1 if all letters are lowercase
     * @param lastUppercase position of the last uppercase letter in the pattern; or -1 if all letters are lowercase
     * @param groupEdgePositions group edge positions in the pattern; must be already corrected
     *                           with fixGroupEdgePositions()
     * @param extraScorePenalty extra score penalty specified by pattern; 0 or negative
     * @return generated match
     */
    static Match generateMatch(Alignment<NucleotideSequenceCaseSensitive> alignment, NSequenceWithQuality target,
            byte targetId, int firstUppercase, int lastUppercase, List<GroupEdgePosition> groupEdgePositions,
            long extraScorePenalty) {
        Range foundRange = alignment.getSequence2Range();
        long matchScore = (long)alignment.getScore() + extraScorePenalty;
        MatchedRange matchedRange = new MatchedRange(target, targetId, 0, foundRange);
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();

        for (GroupEdgePosition groupEdgePosition : groupEdgePositions) {
            int foundGroupEdgePosition = toSeq2Position(alignment, groupEdgePosition.getPosition());
            MatchedGroupEdge matchedGroupEdge = new MatchedGroupEdge(target, targetId, 0,
                    groupEdgePosition.getGroupEdge(), foundGroupEdgePosition);
            matchedGroupEdges.add(matchedGroupEdge);
        }

        if (((firstUppercase != -1) && (firstUppercase < alignment.getSequence1Range().getLower()))
                || ((lastUppercase != -1) && (lastUppercase >= alignment.getSequence1Range().getUpper())))
            throw new IllegalArgumentException("Uppercase position out of bounds of the pattern: firstUppercase="
                    + firstUppercase + ", lastUppercase=" + lastUppercase + ", pattern range "
                    + alignment.getSequence1Range());
        int leftUppercaseDistance = (firstUppercase == -1) ? -1
                : toSeq2Position(alignment, firstUppercase) - foundRange.getLower();
        int rightUppercaseDistance = (lastUppercase == -1) ? -1
                : foundRange.getUpper() - 1 - toSeq2Position(alignment, lastUppercase);

        return new Match(1, matchScore, leftUppercaseDistance, rightUppercaseDistance,
                matchedGroupEdges, matchedRange);
    }

    /**
     * Convert position in the pattern to position in the target; used for group edges and uppercase letter positions.
     *
     * @param alignment alignment for pattern and target
     * @param seq1Position position in the pattern
     * @return position in the target
     */
    private static int toSeq2Position(Alignment<NucleotideSequenceCaseSensitive> alignment, int seq1Position) {
        int seq2Position = alignment.convertToSeq2Position(seq1Position);
        if (seq2Position == -1) {
            if (seq1Position < alignment.getSequence1Range().getLower())
                seq2Position = alignment.getSequence2Range().getLower();
            else if (seq1Position > alignment.getSequence1Range().getUpper())
                seq2Position = alignment.getSequence2Range().getUpper();
            else
                throw new IllegalStateException("Unexpected state when converting pattern position to target: "
                        + "Sequence1Range=" + alignment.getSequence1Range()
                        + ", Sequence2Range=" + alignment.getSequence2Range()
                        + ", seq1Position=" + seq1Position);
        } else if (seq2Position < 0)
            seq2Position = invertCoordinate(seq2Position);
        return seq2Position;
    }

    /**
     * Get position of first uppercase letter in sequence.
     *
     * @param sequence case sensitive nucleotide sequence
     * @return position of first uppercase letter, or -1 if all sequence is lowercase
     */
    public static int firstUppercase(NucleotideSequenceCaseSensitive sequence) {
        for (int i = 0; i < sequence.size(); i++)
            if (Character.isUpperCase(sequence.symbolAt(i)))
                return i;
        return -1;
    }

    /**
     * Get position of last uppercase letter in sequence.
     *
     * @param sequence case sensitive nucleotide sequence
     * @return position of last uppercase letter, or -1 if all sequence is lowercase
     */
    public static int lastUppercase(NucleotideSequenceCaseSensitive sequence) {
        for (int i = sequence.size() - 1; i >= 0; i--)
            if (Character.isUpperCase(sequence.symbolAt(i)))
                return i;
        return -1;
    }

    /**
     * Get position of first uppercase letter in string.
     *
     * @param str string of letters
     * @return position of first uppercase letter, or -1 if all string is lowercase
     */
    public static int firstUppercase(String str) {
        for (int i = 0; i < str.length(); i++)
            if (Character.isUpperCase(str.charAt(i)))
                return i;
        return -1;
    }

    /**
     * Get position of last uppercase letter in string.
     *
     * @param str string of letters
     * @return position of last uppercase letter, or -1 if all string is lowercase
     */
    public static int lastUppercase(String str) {
        for (int i = str.length() - 1; i >= 0; i--)
            if (Character.isUpperCase(str.charAt(i)))
                return i;
        return -1;
    }
}
