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

import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.util.UnfairSorterConfiguration.*;

public final class RepeatNPattern extends SinglePattern implements CanBeSingleSequence, CanFixBorders {
    private final int minRepeats;
    private final int maxRepeats;
    private final int fixedLeftBorder;
    private final int fixedRightBorder;
    private final List<GroupEdgePosition> groupEdgePositions;

    public RepeatNPattern(
            PatternConfiguration conf, int minRepeats, int maxRepeats) {
        this(conf, minRepeats, maxRepeats, new ArrayList<>());
    }

    public RepeatNPattern(
            PatternConfiguration conf, int minRepeats, int maxRepeats, int fixedLeftBorder, int fixedRightBorder) {
        this(conf, minRepeats, maxRepeats, fixedLeftBorder, fixedRightBorder, new ArrayList<>());
    }

    public RepeatNPattern(
            PatternConfiguration conf, int minRepeats, int maxRepeats, List<GroupEdgePosition> groupEdgePositions) {
        this(conf, minRepeats, maxRepeats, -1, -1, groupEdgePositions);
    }

    /**
     * Match sequence of any letters, number of letters specified as interval. Searching for longest available target
     * section based on minRepeats and maxRepeats values.
     *
     * @param conf                  pattern configuration
     * @param minRepeats            minimal number of repeats; minimal allowed value is 1
     * @param maxRepeats            maximal number of repeats; use Integer.MAX_VALUE to match
     *                              without maximal limit of repeats
     * @param fixedLeftBorder       position in target where must be the left border
     * @param fixedRightBorder      position in target where must be the right border
     * @param groupEdgePositions    list of group edges and their positions, can be only on the edges.
     *                              Group edges beyond the right border of motif will be moved to the right border.
     */
    public RepeatNPattern(
            PatternConfiguration conf, int minRepeats, int maxRepeats, int fixedLeftBorder, int fixedRightBorder,
            List<GroupEdgePosition> groupEdgePositions) {
        super(conf);
        if ((minRepeats < 1) || (maxRepeats < minRepeats))
            throw new IllegalArgumentException("Wrong arguments: minRepeats=" + minRepeats
                    + ", maxRepeats=" + maxRepeats);
        else {
            this.minRepeats = minRepeats;
            this.maxRepeats = maxRepeats;
        }
        this.fixedLeftBorder = fixedLeftBorder;
        this.fixedRightBorder = fixedRightBorder;
        this.groupEdgePositions = groupEdgePositions;
    }

    private RepeatNPattern(
            PatternConfiguration conf, byte targetId, int minRepeats, int maxRepeats, int fixedLeftBorder,
            int fixedRightBorder, List<GroupEdgePosition> groupEdgePositions) {
        super(conf, targetId);
        this.minRepeats = minRepeats;
        this.maxRepeats = maxRepeats;
        this.fixedLeftBorder = fixedLeftBorder;
        this.fixedRightBorder = fixedRightBorder;
        this.groupEdgePositions = groupEdgePositions;
    }

    @Override
    public String toString() {
        if (groupEdgePositions.size() > 0)
            return "RepeatNPattern(" + minRepeats + ", " + maxRepeats + ", " + fixedLeftBorder + ", "
                    + fixedRightBorder + ", " + groupEdgePositions + ")";
        else
            return "RepeatNPattern(" + minRepeats + ", " + maxRepeats + ", " + fixedLeftBorder + ", "
                    + fixedRightBorder + ")";
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        return groupEdgePositions.stream().map(GroupEdgePosition::getGroupEdge)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public MatchingResult match(NSequenceWithQuality target, int from, int to) {
        SimplePatternBorders borders = new SimplePatternBorders(target.size(), from, to,
                fixedLeftBorder, fixedRightBorder);
        return new RepeatNPatternMatchingResult(borders.fixedLeftBorder, borders.fixedRightBorder, target,
                borders.fromWithBorder, borders.toWithBorder);
    }

    @Override
    public int estimateMinLength() {
        // indels are disabled for repeat-N patterns
        return minRepeats;
    }

    @Override
    public int estimateMaxLength() {
        // indels are disabled for repeat-N patterns
        return (maxRepeats == Integer.MAX_VALUE) ? -1 : maxRepeats;
    }

    @Override
    public int estimateMaxOverlap() {
        // overlap is disabled for repeat-N patterns
        return 0;
    }

    @Override
    public long estimateComplexity() {
        long repeatsRangeLength = Math.min(maxRepeats, minRepeats + repeatsRangeEstimation) - minRepeats + 1;

        if ((fixedLeftBorder != -1) || (fixedRightBorder != -1))
            return Math.min(fixedSequenceMaxComplexity, repeatsRangeLength);
        else
            return notFixedSequenceMinComplexity + repeatsRangeLength * singleNucleotideComplexity
                    * lettersComplexity.get('N');
    }

    @Override
    public boolean isSingleSequence() {
        return true;
    }

    @Override
    public SinglePattern fixBorder(boolean left, int position) {
        LeftAndRightBorders newBorders = prepareNewBorders(left, position, fixedLeftBorder, fixedRightBorder,
                toString());
        return new RepeatNPattern(conf, minRepeats, maxRepeats,
                newBorders.fixedLeftBorder, newBorders.fixedRightBorder, groupEdgePositions);
    }

    @Override
    SinglePattern setTargetId(byte targetId) {
        validateTargetId(targetId);
        return new RepeatNPattern(conf, targetId, minRepeats, maxRepeats, fixedLeftBorder, fixedRightBorder,
                groupEdgePositions);
    }
}
