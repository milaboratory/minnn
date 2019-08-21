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

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.pattern.PatternUtils.*;
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
     * @param conf               pattern configuration
     * @param minRepeats         minimal number of repeats; minimal allowed value is 1
     * @param maxRepeats         maximal number of repeats; use Integer.MAX_VALUE to match
     *                           without maximal limit of repeats
     * @param fixedLeftBorder    position in target where must be the left border
     * @param fixedRightBorder   position in target where must be the right border
     * @param groupEdgePositions list of group edges and their positions, can be only on the edges.
     *                           Group edges beyond the right border of motif will be moved to the right border.
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
            PatternConfiguration conf, byte targetId, int minRepeats, int maxRepeats,
            int fixedLeftBorder, int fixedRightBorder, List<GroupEdgePosition> groupEdgePositions) {
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

    private class RepeatNPatternMatchingResult implements MatchingResult {
        private final int fixedLeftBorder;
        private final int fixedRightBorder;
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;

        RepeatNPatternMatchingResult(
                int fixedLeftBorder, int fixedRightBorder, NSequenceWithQuality target, int from, int to) {
            this.fixedLeftBorder = fixedLeftBorder;
            this.fixedRightBorder = fixedRightBorder;
            this.target = target;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            return new RepeatNPatternOutputPort(fairSorting);
        }

        private class RepeatNPatternOutputPort implements OutputPort<MatchIntermediate> {
            private final int maxRepeats;
            private final boolean fixedBorder;
            private final boolean fairSorting;
            private boolean noMoreMatches = false;
            private int currentRepeats;
            private int currentPosition;

            // sequences are needed to produce alignments; keys: number of repeats, values: sequences of N letters
            private final TIntObjectHashMap<NucleotideSequenceCaseSensitive> sequences = new TIntObjectHashMap<>();

            // used for fair sorting and for matching in fixed position
            private TreeSet<ComparableMatch> allMatches = null;
            private Iterator<ComparableMatch> allMatchesIterator = null;

            RepeatNPatternOutputPort(boolean fairSorting) {
                this.maxRepeats = Math.min(RepeatNPattern.this.maxRepeats, to - from);
                this.fixedBorder = (fixedLeftBorder != -1) || (fixedRightBorder != -1);
                this.fairSorting = fairSorting;
                if ((from + minRepeats > to) || (minRepeats > this.maxRepeats)
                        || ((fixedLeftBorder != -1) && (from > fixedLeftBorder))
                        || ((fixedRightBorder != -1) && (to <= fixedRightBorder)))
                    noMoreMatches = true;
                else {
                    for (int repeats = minRepeats; repeats <= this.maxRepeats; repeats++)
                        sequences.put(repeats, new NucleotideSequenceCaseSensitive(new String(new char[repeats])
                                .replace("\0", "N")));
                    this.currentRepeats = this.maxRepeats;
                    this.currentPosition = 0;
                }
            }

            @Override
            public MatchIntermediate take() {
                MatchIntermediate match;
                if (noMoreMatches)
                    match = null;
                else if (fixedBorder)
                    match = takeFromFixedPosition();
                else if (fairSorting)
                    match = takeFair();
                else
                    match = takeUnfair();

                return match;
            }

            private MatchIntermediate takeFair() {
                if (allMatchesIterator == null) {
                    allMatches = new TreeSet<>();
                    for (int rangeFrom = from; rangeFrom <= to - minRepeats; rangeFrom++)
                        for (int rangeTo = rangeFrom + minRepeats; rangeTo <= Math.min(to, rangeFrom + maxRepeats);
                             rangeTo++) {
                            Range range = new Range(rangeFrom, rangeTo);
                            allMatches.add(new ComparableMatch(range, rangeToMatch(range)));
                        }
                    allMatchesIterator = allMatches.iterator();
                }
                return (allMatchesIterator.hasNext()) ? allMatchesIterator.next().match : null;
            }

            private MatchIntermediate takeFromFixedPosition() {
                if (allMatchesIterator == null) {
                    allMatches = new TreeSet<>();
                    if (fixedRightBorder != -1) {
                        // at this point, from must be equal to fixedLeftBorder if fixedLeftBorder != -1
                        int rangeFromMin;
                        int rangeFromMax;
                        if (fixedLeftBorder != -1) {
                            rangeFromMin = fixedLeftBorder;
                            rangeFromMax = fixedLeftBorder;
                        } else {
                            rangeFromMin = Math.max(from, fixedRightBorder - maxRepeats + 1);
                            rangeFromMax = fixedRightBorder - minRepeats + 1;
                        }
                        for (int rangeFrom = rangeFromMin; rangeFrom <= rangeFromMax; rangeFrom++) {
                            Range range = new Range(rangeFrom, fixedRightBorder + 1);
                            allMatches.add(new ComparableMatch(range, rangeToMatch(range)));
                        }
                    } else if (fixedLeftBorder != -1) {
                        for (int rangeTo = fixedLeftBorder + minRepeats;
                             rangeTo <= Math.min(to, fixedLeftBorder + maxRepeats); rangeTo++) {
                            Range range = new Range(fixedLeftBorder, rangeTo);
                            allMatches.add(new ComparableMatch(range, rangeToMatch(range)));
                        }
                    } else throw new IllegalArgumentException("Wrong call of takeFromFixedPosition: fixedLeftBorder="
                            + fixedLeftBorder + ", fixedRightBorder=" + fixedRightBorder);
                    allMatchesIterator = allMatches.iterator();
                }
                return (allMatchesIterator.hasNext()) ? allMatchesIterator.next().match : null;
            }

            private MatchIntermediate takeUnfair() {
                while (!noMoreMatches) {
                    Range range = new Range(currentPosition + from,
                            Math.min(to, currentPosition + currentRepeats + from));
                    MatchIntermediate match = rangeToMatch(range);
                    pointToNextUnfairMatch();
                    if (match.getScore() >= conf.scoreThreshold)
                        return match;
                }
                return null;
            }

            private void pointToNextUnfairMatch() {
                currentPosition++;
                if (currentPosition > to - from - currentRepeats) {
                    currentPosition = 0;
                    currentRepeats--;
                    if (currentRepeats < minRepeats)
                        noMoreMatches = true;
                }
            }

            private MatchIntermediate rangeToMatch(Range range) {
                int numberOfRepeats = range.length();
                int firstUppercase = 0;
                int lastUppercase = numberOfRepeats - 1;
                NucleotideSequenceCaseSensitive seq = sequences.get(numberOfRepeats);
                PatternConfiguration fixedBorderConfiguration = conf.setLeftBorder(range.getLower());
                Alignment<NucleotideSequenceCaseSensitive> alignment = Objects.requireNonNull(
                        fixedBorderConfiguration.patternAligner.align(fixedBorderConfiguration, true,
                                seq, target, range.getUpper() - 1));
                Range targetRange = alignment.getSequence2Range();
                List<GroupEdgePosition> groupEdgePositions = fixGroupEdgePositions(
                        RepeatNPattern.this.groupEdgePositions, 0, targetRange.length());
                long repeatsPenalty = fixedBorderConfiguration.patternAligner.repeatsPenalty(fixedBorderConfiguration,
                        seq, numberOfRepeats, maxRepeats);
                return generateMatch(alignment, target, targetId, firstUppercase, lastUppercase, groupEdgePositions,
                        repeatsPenalty, fixedBorderConfiguration.defaultGroupsOverride);
            }

            private class ComparableMatch implements Comparable<ComparableMatch> {
                final Range range;
                final MatchIntermediate match;

                ComparableMatch(Range range, MatchIntermediate match) {
                    this.range = range;
                    this.match = match;
                }

                @Override
                public int compareTo(ComparableMatch other) {
                    // start from high scores
                    int result = -Long.compare(match.getScore(), other.match.getScore());
                    // if scores are equal, start from long sequences
                    if (result == 0)
                        result = -Integer.compare(range.length(), other.range.length());
                    // compare objects' unique hashcodes to avoid 0 result that can cause loss of objects in TreeSet
                    if (result == 0)
                        result = Integer.compare(System.identityHashCode(this), System.identityHashCode(other));
                    return result;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    ComparableMatch that = (ComparableMatch)o;
                    return range.equals(that.range);
                }

                @Override
                public int hashCode() {
                    return range.hashCode();
                }
            }
        }
    }
}
