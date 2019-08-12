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
import com.milaboratory.core.motif.*;
import com.milaboratory.core.sequence.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.pattern.PatternUtils.*;
import static com.milaboratory.minnn.util.UnfairSorterConfiguration.*;

public final class FuzzyMatchPattern extends SinglePattern implements CanBeSingleSequence, CanFixBorders {
    private final ArrayList<NucleotideSequenceCaseSensitive> sequences;
    private final ArrayList<Motif<NucleotideSequence>> motifs;
    private final int leftCut;
    private final int rightCut;
    private final int fixedLeftBorder;
    private final int fixedRightBorder;
    private final List<GroupEdgePosition> groupEdgePositions;
    private final ArrayList<Integer> groupOffsets;

    public FuzzyMatchPattern(
            PatternConfiguration conf, NucleotideSequenceCaseSensitive patternSeq) {
        this(conf, patternSeq, new ArrayList<>());
    }

    public FuzzyMatchPattern(
            PatternConfiguration conf, NucleotideSequenceCaseSensitive patternSeq, int leftCut, int rightCut,
            int fixedLeftBorder, int fixedRightBorder) {
        this(conf, patternSeq, leftCut, rightCut, fixedLeftBorder, fixedRightBorder, new ArrayList<>());
    }

    public FuzzyMatchPattern(
            PatternConfiguration conf, NucleotideSequenceCaseSensitive patternSeq,
            List<GroupEdgePosition> groupEdgePositions) {
        this(conf, patternSeq, 0, 0, -1, -1, groupEdgePositions);
    }

    /**
     * Find match with possible insertions and deletions using bitap and aligner. If fixedLeftBorder
     * or fixedRightBorder is specified, find only matches near that border.
     *
     * @param conf                  pattern configuration: it provides information about maxErrors for bitap
     * @param patternSeq            sequence to find in the target
     * @param leftCut               number of nucleotides that can be cut on the left without penalty
     * @param rightCut              number of nucleotides that can be cut on the right without penalty
     * @param fixedLeftBorder       position in target where must be the left border;
     *                              -1 if there is no fixed left border;
     *                              -2 - x means coordinate from the end of target;
     *                              fixedLeftBorder is inclusive
     * @param fixedRightBorder      position in target where must be the right border;
     *                              -1 if there is no fixed right border;
     *                              -2 - x means coordinate from the end of target;
     *                              fixedRightBorder is inclusive
     * @param groupEdgePositions    list of group edges and their positions
     */
    public FuzzyMatchPattern(
            PatternConfiguration conf, NucleotideSequenceCaseSensitive patternSeq, int leftCut, int rightCut,
            int fixedLeftBorder, int fixedRightBorder, List<GroupEdgePosition> groupEdgePositions) {
        super(conf);
        int size = patternSeq.size();
        if (leftCut + rightCut >= size)
            throw new IllegalArgumentException("Wrong arguments: leftCut=" + leftCut + ", rightCut=" + rightCut
                    + ", patternSeq=" + patternSeq);
        else {
            this.leftCut = leftCut;
            this.rightCut = rightCut;
        }
        this.sequences = new ArrayList<>();
        this.motifs = new ArrayList<>();
        this.groupOffsets = new ArrayList<>();
        if ((leftCut == 0) && (rightCut == 0)) {
            sequences.add(patternSeq);
            motifs.add(patternSeq.toNucleotideSequence().toMotif());
            groupOffsets.add(0);
        } else
            for (int i = 0; i <= leftCut; i++)
                for (int j = 0; j <= rightCut; j++) {
                    NucleotideSequenceCaseSensitive seq = new NucleotideSequenceCaseSensitive(patternSeq.toString()
                            .substring(i, size - j));
                    sequences.add(seq);
                    motifs.add(seq.toNucleotideSequence().toMotif());
                    groupOffsets.add(-i);
                }
        this.fixedLeftBorder = fixedLeftBorder;
        this.fixedRightBorder = fixedRightBorder;
        this.groupEdgePositions = groupEdgePositions;

        for (GroupEdgePosition groupEdgePosition : groupEdgePositions)
            if (groupEdgePosition.getPosition() > size)
                throw new IllegalArgumentException("Group edge " + groupEdgePosition.getGroupEdge().getGroupName()
                        + (groupEdgePosition.getGroupEdge().isStart() ? " start" : " end") + " ("
                        + groupEdgePosition.getPosition() + ") is outside of motif (motif size: " + size + ")");
    }

    private FuzzyMatchPattern(
            PatternConfiguration conf, byte targetId, ArrayList<NucleotideSequenceCaseSensitive> sequences,
            ArrayList<Motif<NucleotideSequence>> motifs, int leftCut, int rightCut, int fixedLeftBorder,
            int fixedRightBorder, List<GroupEdgePosition> groupEdgePositions, ArrayList<Integer> groupOffsets) {
        super(conf, targetId);
        this.sequences = sequences;
        this.motifs = motifs;
        this.leftCut = leftCut;
        this.rightCut = rightCut;
        this.fixedLeftBorder = fixedLeftBorder;
        this.fixedRightBorder = fixedRightBorder;
        this.groupEdgePositions = groupEdgePositions;
        this.groupOffsets = groupOffsets;
    }

    @Override
    public String toString() {
        if (groupEdgePositions.size() > 0)
            return "FuzzyMatchPattern(" + sequences.get(0) + ", " + leftCut + ", " + rightCut + ", "
                    + fixedLeftBorder + ", " + fixedRightBorder + ", " + groupEdgePositions + ")";
        else
            return "FuzzyMatchPattern(" + sequences.get(0) + ", " + leftCut + ", " + rightCut + ", "
                    + fixedLeftBorder + ", " + fixedRightBorder + ")";
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
        return new FuzzyMatchingResult(borders.fixedLeftBorder, borders.fixedRightBorder, target,
                borders.fromWithBorder, borders.toWithBorder);
    }

    @Override
    public int estimateMinLength() {
        return Math.max(1, sequences.get(sequences.size() - 1).size() - conf.bitapMaxErrors);
    }

    @Override
    public int estimateMaxLength() {
        return sequences.get(0).size() + conf.bitapMaxErrors;
    }

    @Override
    public int estimateMaxOverlap() {
        String seq0 = sequences.get(0).toString();
        String currentSeq;
        int maxOverlap = -1;
        int position = 0;
        int uppercasePosition;
        while (position <= leftCut) {
            currentSeq = seq0.substring(position);  // try what will be if we cut off the last found uppercase letter
            uppercasePosition = firstUppercase(currentSeq);
            if (uppercasePosition == -1)
                return currentSeq.length() - 1;
            maxOverlap = Math.max(maxOverlap, Math.max(0, uppercasePosition - 1));
            position += uppercasePosition + 1;
        }
        position = seq0.length() - 1;
        while (position >= seq0.length() - 1 - rightCut) {
            currentSeq = seq0.substring(0, position + 1);
            uppercasePosition = lastUppercase(currentSeq);
            if (uppercasePosition == -1)
                return currentSeq.length() - 1;
            maxOverlap = Math.max(maxOverlap, Math.max(0, currentSeq.length() - uppercasePosition - 2));
            position = uppercasePosition - 1;
        }

        return maxOverlap;
    }

    @Override
    public long estimateComplexity() {
        if ((fixedLeftBorder != -1) || (fixedRightBorder != -1))
            return Math.min(fixedSequenceMaxComplexity, sequences.size());
        else
            return notFixedSequenceMinComplexity + (long)(sequences.size()
                    * estimateSequenceComplexity(sequences.get(0).toString()));
    }

    /**
     * Estimate complexity for single sequence. Used in estimateComplexity() and in bitap matching
     * for long (>63 nucleotides) sequences.
     *
     * @param s     nucleotide sequence string
     * @return      estimated complexity for this sequence
     */
    private static double estimateSequenceComplexity(String s) {
        if (s.chars().allMatch(c -> nLetters.contains(Character.toString((char)c))))
            return singleNucleotideComplexity * lettersComplexity.get(s.charAt(0));
        else
            return singleNucleotideComplexity / s.chars().mapToDouble(c -> 1.0 / lettersComplexity.get((char)c)).sum();
    }

    @Override
    public boolean isSingleSequence() {
        return true;
    }

    @Override
    public SinglePattern fixBorder(boolean left, int position) {
        LeftAndRightBorders newBorders = prepareNewBorders(left, position, fixedLeftBorder, fixedRightBorder,
                toString());
        return new FuzzyMatchPattern(conf, sequences.get(0), leftCut, rightCut,
                newBorders.fixedLeftBorder, newBorders.fixedRightBorder, groupEdgePositions);
    }

    @Override
    SinglePattern setTargetId(byte targetId) {
        validateTargetId(targetId);
        return new FuzzyMatchPattern(conf, targetId, sequences, motifs, leftCut, rightCut, fixedLeftBorder,
                fixedRightBorder, groupEdgePositions, groupOffsets);
    }

    private class FuzzyMatchingResult implements MatchingResult {
        private final int fixedLeftBorder;
        private final int fixedRightBorder;
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;

        FuzzyMatchingResult(int fixedLeftBorder, int fixedRightBorder, NSequenceWithQuality target, int from, int to) {
            this.fixedLeftBorder = fixedLeftBorder;
            this.fixedRightBorder = fixedRightBorder;
            this.target = target;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            return new FuzzyMatchOutputPort(fairSorting);
        }

        private class FuzzyMatchOutputPort implements OutputPort<MatchIntermediate> {
            private final static int BITAP_MAX_LENGTH = 63;
            private final boolean fixedBorder;
            private final boolean fairSorting;
            private final List<BitapPattern> bitapPatterns;
            private final List<BitapMatcherFilter> bitapMatcherFilters;
            private final ArrayList<Integer> bitapPositionCorrections;

            /* Current index in lists of sequences and bitap patterns.
             * Index represents combination of numbers of cut nucleotides on the left and right sides. */
            private int currentIndex = 0;

            // Data structures used for fair sorting and for matching in fixed position.
            private MatchIntermediate[] allMatches;
            private boolean sortingPerformed = false;
            private int takenValues = 0;

            // Used only in takeUnfairByScore(). Current number of bitap errors get matches with this number of errors.
            private int currentNumBitapErrors = 0;

            /* Used only in takeUnfairByScore(). Already returned positions saved to skip them when searching with
             * bigger number of errors. Separate HashSet for each pattern index. */
            private ArrayList<HashSet<Integer>> alreadyReturnedPositions;

            FuzzyMatchOutputPort(boolean fairSorting) {
                this.fairSorting = fairSorting;
                this.fixedBorder = (fixedLeftBorder != -1) || (fixedRightBorder != -1);
                this.bitapPatterns = new ArrayList<>();
                this.bitapPositionCorrections = new ArrayList<>();
                for (int i = 0; i < sequences.size(); i++) {
                    NucleotideSequenceCaseSensitive currentSequence = sequences.get(i);
                    if (currentSequence.size() > BITAP_MAX_LENGTH) {
                        String seqString = currentSequence.toString();
                        int seqLength = currentSequence.size();
                        String seqStart = seqString.substring(0, BITAP_MAX_LENGTH);
                        String seqEnd = seqString.substring(seqLength - BITAP_MAX_LENGTH, seqLength);
                        if (estimateSequenceComplexity(seqStart) > estimateSequenceComplexity(seqEnd)) {
                            this.bitapPatterns.add(new NucleotideSequence(seqEnd).toMotif().getBitapPattern());
                            this.bitapPositionCorrections.add(0);
                        } else {
                            this.bitapPatterns.add(new NucleotideSequence(seqStart).toMotif().getBitapPattern());
                            this.bitapPositionCorrections.add(seqLength - BITAP_MAX_LENGTH);
                        }
                    } else {
                        this.bitapPatterns.add(motifs.get(i).getBitapPattern());
                        this.bitapPositionCorrections.add(0);
                    }
                }
                if (!fixedBorder && !fairSorting) {
                    this.bitapMatcherFilters = bitapPatterns.stream().map(bp -> new BitapMatcherFilter(
                            bp.substitutionAndIndelMatcherLast(0, target.getSequence(), from, to)))
                            .collect(Collectors.toList());
                    this.alreadyReturnedPositions = IntStream.range(0, sequences.size())
                            .mapToObj(i -> new HashSet<Integer>()).collect(Collectors.toCollection(ArrayList::new));
                } else
                    this.bitapMatcherFilters = bitapPatterns.stream().map(bp -> new BitapMatcherFilter(
                            bp.substitutionAndIndelMatcherLast(conf.bitapMaxErrors, target.getSequence(), from, to)))
                            .collect(Collectors.toList());
            }

            @Override
            public MatchIntermediate take() {
                MatchIntermediate match;
                if (fixedBorder)
                    match = takeFromFixedPosition();
                else
                    if (fairSorting)
                        match = takeFair();
                    else
                        match = takeUnfair();

                return match;
            }

            private MatchIntermediate takeUnfair() {
                while (currentNumBitapErrors <= conf.bitapMaxErrors) {
                    while (currentIndex < sequences.size()) {
                        int position = correctBitapPosition(bitapMatcherFilters.get(currentIndex).findNext());
                        if (position == -1) {
                            bitapMatcherFilters.set(currentIndex, new BitapMatcherFilter(bitapPatterns.get(currentIndex)
                                    .substitutionAndIndelMatcherLast(currentNumBitapErrors + 1,
                                            target.getSequence(), from, to)));
                            currentIndex++;
                        } else {
                            HashSet<Integer> currentReturnedPositions = alreadyReturnedPositions.get(currentIndex);
                            if (currentReturnedPositions.contains(position))
                                continue;
                            else
                                currentReturnedPositions.add(position);
                            NucleotideSequenceCaseSensitive currentSeq = sequences.get(currentIndex);
                            Alignment<NucleotideSequenceCaseSensitive> alignment = conf.patternAligner.align(
                                    conf, currentSeq, target, position);
                            if (alignment.getScore() >= conf.scoreThreshold)
                                return generateMatch(alignment, target, targetId,
                                        firstUppercase(currentSeq), lastUppercase(currentSeq),
                                        fixGroupEdgePositions(groupEdgePositions, groupOffsets.get(currentIndex),
                                                currentSeq.size()), 0, conf.defaultGroupsOverride);
                        }
                    }
                    currentIndex = 0;
                    currentNumBitapErrors++;
                }
                return null;
            }

            private MatchIntermediate takeFair() {
                if (!sortingPerformed) {
                    fillAllMatchesForFairSorting();
                    sortAllMatches();
                    sortingPerformed = true;
                }
                if (takenValues == allMatches.length) return null;
                return allMatches[takenValues++];
            }

            private MatchIntermediate takeFromFixedPosition() {
                // important: to is exclusive and fixedRightBorder is inclusive
                if (((fixedLeftBorder != -1) && (from > fixedLeftBorder))
                        || ((fixedRightBorder != -1) && (to <= fixedRightBorder)))
                    return null;
                if (!sortingPerformed) {
                    if (fixedRightBorder != -1)
                        fillAllMatchesForFixedRightBorder();
                    else if (fixedLeftBorder != -1)
                        fillAllMatchesForFixedLeftBorder();
                    else throw new IllegalArgumentException("Wrong call of takeFromFixedPosition: fixedLeftBorder="
                                + fixedLeftBorder + ", fixedRightBorder=" + fixedRightBorder);
                    sortAllMatches();
                    sortingPerformed = true;
                }
                if (takenValues == allMatches.length) return null;
                return allMatches[takenValues++];
            }

            private void sortAllMatches() {
                Arrays.sort(allMatches, Comparator.comparing(MatchIntermediate::getScore,
                        (s1, s2) -> Long.compare(s2, s1)).thenComparing(MatchIntermediate::getRange,
                        (r1, r2) -> Integer.compare(r2.length(), r1.length())));
            }

            /**
             * Fill allMatches array with all existing matches for fair sorting.
             */
            private void fillAllMatchesForFairSorting() {
                ArrayList<MatchIntermediate> allMatchesList = new ArrayList<>();
                Alignment<NucleotideSequenceCaseSensitive> alignment;
                int matchLastPosition;

                for (currentIndex = 0; currentIndex < sequences.size(); currentIndex++) {
                    BitapMatcherFilter currentBitapFilter = bitapMatcherFilters.get(currentIndex);
                    NucleotideSequenceCaseSensitive currentSeq = sequences.get(currentIndex);
                    HashSet<Range> uniqueRanges = new HashSet<>();
                    do {
                        matchLastPosition = correctBitapPosition(currentBitapFilter.findNext());
                        if (matchLastPosition != -1) {
                            alignment = conf.patternAligner.align(conf, currentSeq, target, matchLastPosition);
                            if ((alignment.getScore() >= conf.scoreThreshold)
                                    && !uniqueRanges.contains(alignment.getSequence2Range())) {
                                uniqueRanges.add(alignment.getSequence2Range());
                                allMatchesList.add(generateMatch(alignment, target, targetId,
                                        firstUppercase(currentSeq), lastUppercase(currentSeq),
                                        fixGroupEdgePositions(groupEdgePositions, groupOffsets.get(currentIndex),
                                        currentSeq.size()), 0, conf.defaultGroupsOverride));
                            }
                        }
                    } while (matchLastPosition != -1);
                }

                allMatches = new MatchIntermediate[allMatchesList.size()];
                allMatchesList.toArray(allMatches);
            }

            /**
             * Fill allMatches array with all possible alignments for fixed left border.
             */
            private void fillAllMatchesForFixedLeftBorder() {
                ArrayList<MatchIntermediate> allMatchesList = new ArrayList<>();
                PatternConfiguration fixedConfiguration = conf.setLeftBorder(fixedLeftBorder);
                Alignment<NucleotideSequenceCaseSensitive> alignment;

                for (currentIndex = 0; currentIndex < sequences.size(); currentIndex++) {
                    if (bitapMatcherFilters.get(currentIndex).findNext() == -1)
                        continue;
                    NucleotideSequenceCaseSensitive currentSeq = sequences.get(currentIndex);
                    HashSet<Range> uniqueRanges = new HashSet<>();
                    for (int rightBorder = Math.max(fixedLeftBorder, fixedLeftBorder + currentSeq.size()
                            - fixedConfiguration.bitapMaxErrors - 1);
                         rightBorder <= Math.min(to - 1, fixedLeftBorder + currentSeq.size()
                                 + fixedConfiguration.bitapMaxErrors - 1);
                         rightBorder++)
                        if ((rightBorder >= fixedLeftBorder) && (rightBorder < target.size())) {
                            alignment = fixedConfiguration.patternAligner.align(fixedConfiguration, currentSeq, target,
                                    rightBorder);
                            if ((alignment.getScore() >= fixedConfiguration.scoreThreshold)
                                    && !uniqueRanges.contains(alignment.getSequence2Range())) {
                                uniqueRanges.add(alignment.getSequence2Range());
                                allMatchesList.add(generateMatch(alignment, target, targetId,
                                        firstUppercase(currentSeq), lastUppercase(currentSeq),
                                        fixGroupEdgePositions(groupEdgePositions, groupOffsets.get(currentIndex),
                                        currentSeq.size()), 0, conf.defaultGroupsOverride));
                            }
                        }
                }

                allMatches = new MatchIntermediate[allMatchesList.size()];
                allMatchesList.toArray(allMatches);
            }

            /**
             * Fill allMatches array with all possible alignments for fixed right border.
             */
            private void fillAllMatchesForFixedRightBorder() {
                ArrayList<MatchIntermediate> allMatchesList = new ArrayList<>();
                PatternConfiguration fixedConfiguration = (fixedLeftBorder == -1) ? conf
                        : conf.setLeftBorder(fixedLeftBorder);
                Alignment<NucleotideSequenceCaseSensitive> alignment;

                for (currentIndex = 0; currentIndex < sequences.size(); currentIndex++) {
                    if (bitapMatcherFilters.get(currentIndex).findNext() == -1)
                        continue;
                    NucleotideSequenceCaseSensitive currentSeq = sequences.get(currentIndex);
                    alignment = fixedConfiguration.patternAligner.align(fixedConfiguration, currentSeq, target,
                            fixedRightBorder);
                    if (alignment.getScore() >= fixedConfiguration.scoreThreshold)
                        allMatchesList.add(generateMatch(alignment, target, targetId,
                                firstUppercase(currentSeq), lastUppercase(currentSeq),
                                fixGroupEdgePositions(groupEdgePositions, groupOffsets.get(currentIndex),
                                currentSeq.size()), 0, conf.defaultGroupsOverride));
                }

                allMatches = new MatchIntermediate[allMatchesList.size()];
                allMatchesList.toArray(allMatches);
            }

            /**
             * Apply correction to position found by bitap, if needed. Correction may be needed for long sequence
             * when bitap is searching for part of the sequence.
             *
             * @param position position found by bitap
             * @return position with correction
             */
            private int correctBitapPosition(int position) {
                if (position == -1)
                    return -1;
                int correction = bitapPositionCorrections.get(currentIndex);
                if (correction == 0)
                    return position;
                else {
                    int targetLength = target.size();
                    int correctPosition = position + correction;
                    if (correctPosition >= targetLength + conf.bitapMaxErrors)
                        return -1;
                    else
                        return Math.min(correctPosition, targetLength - 1);
                }
            }
        }
    }
}
