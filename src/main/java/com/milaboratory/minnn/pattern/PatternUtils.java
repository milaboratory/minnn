/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact MiLaboratory LLC, which owns exclusive
 * rights for distribution of this program for commercial purposes, using the
 * following email address: licensing@milaboratory.com.
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.minnn.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideAlphabetCaseSensitive;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import gnu.trove.map.hash.TObjectByteHashMap;

import java.util.*;

import static com.milaboratory.minnn.cli.Defaults.BUILTIN_READ_GROUPS_NUM;

public final class PatternUtils {
    private PatternUtils() {}

    static final TObjectByteHashMap<String> defaultGroupIds = new TObjectByteHashMap<>(BUILTIN_READ_GROUPS_NUM,
            0.5f, (byte)-1);

    static {
        for (int targetId = 1; targetId <= BUILTIN_READ_GROUPS_NUM; targetId++)
            defaultGroupIds.put("R" + targetId, (byte)targetId);
    }

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
        ArrayList<GroupEdgePosition> fixedPositions = new ArrayList<>();
        for (GroupEdgePosition groupEdgePosition : groupEdgePositions) {
            int currentPosition = groupEdgePosition.getPosition();
            if ((move == 0) && (currentPosition <= maxPosition))
                fixedPositions.add(groupEdgePosition);
            else {
                currentPosition += move;
                if (currentPosition < 0)
                    currentPosition = 0;
                if (currentPosition > maxPosition)
                    currentPosition = maxPosition;
                fixedPositions.add(new GroupEdgePosition(groupEdgePosition.getGroupEdge(), currentPosition));
            }
        }

        return fixedPositions;
    }

    /**
     * Generate match from alignment, for FuzzyMatchPattern and RepeatPattern.
     *
     * @param alignment             alignment for pattern and target
     * @param target                target
     * @param targetId              target id, comes from SinglePattern (FuzzyMatch or Repeat)
     *                              that generates this match
     * @param firstUppercase        position of the first uppercase letter in the pattern;
     *                              or -1 if all letters are lowercase
     * @param lastUppercase         position of the last uppercase letter in the pattern;
     *                              or -1 if all letters are lowercase
     * @param groupEdgePositions    group edge positions in the pattern; must be already corrected
     *                              with fixGroupEdgePositions()
     * @param extraScorePenalty     extra score penalty specified by pattern; 0 or negative
     * @param overrideTargetIds     true if there were groups R1, R2 etc in the pattern, so targetIds override
     *                              is needed; in this case groups R1, R2, R3 etc will have targetIds based
     *                              on their name, and all other groups will have targetId -1
     * @return generated match
     */
    static MatchIntermediate generateMatch(Alignment<NucleotideSequenceCaseSensitive> alignment,
            NSequenceWithQuality target, byte targetId, int firstUppercase, int lastUppercase,
            List<GroupEdgePosition> groupEdgePositions, long extraScorePenalty, boolean overrideTargetIds) {
        Range foundRange = alignment.getSequence2Range();
        long matchScore = (long)alignment.getScore() + extraScorePenalty;
        MatchedRange matchedRange = new MatchedRange(target, targetId, 0, foundRange);
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();

        for (GroupEdgePosition groupEdgePosition : groupEdgePositions) {
            byte matchedGroupTargetId = overrideTargetIds
                    ? defaultGroupIds.get(groupEdgePosition.getGroupEdge().getGroupName())
                    : targetId;
            int foundGroupEdgePosition = toSeq2Position(alignment, groupEdgePosition.getPosition());
            MatchedGroupEdge matchedGroupEdge = new MatchedGroupEdge(target, matchedGroupTargetId, 0,
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

        return new MatchIntermediate(1, matchScore, leftUppercaseDistance, rightUppercaseDistance,
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
            if (NucleotideAlphabetCaseSensitive.isUpperCase(sequence.codeAt(i)))
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
            if (NucleotideAlphabetCaseSensitive.isUpperCase(sequence.codeAt(i)))
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
