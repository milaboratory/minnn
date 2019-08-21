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
import com.milaboratory.core.sequence.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.pattern.PatternUtils.*;
import static com.milaboratory.minnn.util.UnfairSorterConfiguration.*;

public final class RepeatPattern extends SinglePattern implements CanBeSingleSequence, CanFixBorders {
    private final NucleotideSequenceCaseSensitive patternSeq;
    private final int minRepeats;
    private final int maxRepeats;
    private final int fixedLeftBorder;
    private final int fixedRightBorder;
    private final List<GroupEdgePosition> groupEdgePositions;

    public RepeatPattern(
            PatternConfiguration conf, NucleotideSequenceCaseSensitive patternSeq, int minRepeats, int maxRepeats) {
        this(conf, patternSeq, minRepeats, maxRepeats, new ArrayList<>());
    }

    public RepeatPattern(
            PatternConfiguration conf, NucleotideSequenceCaseSensitive patternSeq, int minRepeats, int maxRepeats,
            int fixedLeftBorder, int fixedRightBorder) {
        this(conf, patternSeq, minRepeats, maxRepeats, fixedLeftBorder, fixedRightBorder, new ArrayList<>());
    }

    public RepeatPattern(
            PatternConfiguration conf, NucleotideSequenceCaseSensitive patternSeq, int minRepeats, int maxRepeats,
            List<GroupEdgePosition> groupEdgePositions) {
        this(conf, patternSeq, minRepeats, maxRepeats, -1, -1, groupEdgePositions);
    }

    /**
     * Match several repeats of specified nucleotide or wildcard. Number of repeats specified as interval.
     * Searching for longest available target section based on minRepeats and maxRepeats values.
     *
     * @param conf                  pattern configuration
     * @param patternSeq            1 character case sensitive nucleotide sequence to repeat
     * @param minRepeats            minimal number of repeats; minimal allowed value is 1
     * @param maxRepeats            maximal number of repeats; use Integer.MAX_VALUE to match
     *                              without maximal limit of repeats
     * @param fixedLeftBorder       position in target where must be the left border
     * @param fixedRightBorder      position in target where must be the right border
     * @param groupEdgePositions    list of group edges and their positions, can be only on the edges.
     *                              Group edges beyond the right border of motif will be moved to the right border.
     */
    public RepeatPattern(
            PatternConfiguration conf, NucleotideSequenceCaseSensitive patternSeq, int minRepeats, int maxRepeats,
            int fixedLeftBorder, int fixedRightBorder, List<GroupEdgePosition> groupEdgePositions) {
        super(conf);
        this.patternSeq = patternSeq;
        if ((minRepeats < 1) || (maxRepeats < minRepeats))
            throw new IllegalArgumentException("Wrong arguments: minRepeats=" + minRepeats
                    + ", maxRepeats=" + maxRepeats);
        else {
            this.minRepeats = minRepeats;
            this.maxRepeats = maxRepeats;
        }
        if (patternSeq.size() != 1)
            throw new IllegalArgumentException("patternSeq length must be 1 for RepeatPattern, found " + patternSeq);
        this.fixedLeftBorder = fixedLeftBorder;
        this.fixedRightBorder = fixedRightBorder;
        this.groupEdgePositions = groupEdgePositions;
    }

    private RepeatPattern(
            PatternConfiguration conf, byte targetId, NucleotideSequenceCaseSensitive patternSeq, int minRepeats,
            int maxRepeats, int fixedLeftBorder, int fixedRightBorder, List<GroupEdgePosition> groupEdgePositions) {
        super(conf, targetId);
        this.patternSeq = patternSeq;
        this.minRepeats = minRepeats;
        this.maxRepeats = maxRepeats;
        this.fixedLeftBorder = fixedLeftBorder;
        this.fixedRightBorder = fixedRightBorder;
        this.groupEdgePositions = groupEdgePositions;
    }

    @Override
    public String toString() {
        if (groupEdgePositions.size() > 0)
            return "RepeatPattern(" + patternSeq + ", " + minRepeats + ", " + maxRepeats + ", "
                    + fixedLeftBorder + ", " + fixedRightBorder + ", " + groupEdgePositions + ")";
        else
            return "RepeatPattern(" + patternSeq + ", " + minRepeats + ", " + maxRepeats + ", "
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
        return new RepeatPatternMatchingResult(borders.fixedLeftBorder, borders.fixedRightBorder, target,
                borders.fromWithBorder, borders.toWithBorder);
    }

    @Override
    public int estimateMinLength() {
        return Math.max(1, minRepeats - (Character.isLowerCase(patternSeq.symbolAt(0)) ? conf.bitapMaxErrors
                : 0));
    }

    @Override
    public int estimateMaxLength() {
        if (maxRepeats == Integer.MAX_VALUE)
            return -1;
        else
            return maxRepeats + (Character.isLowerCase(patternSeq.symbolAt(0)) ? conf.bitapMaxErrors : 0);
    }

    @Override
    public int estimateMaxOverlap() {
        return Character.isUpperCase(patternSeq.symbolAt(0)) ? 0 : maxRepeats - 1;
    }

    @Override
    public long estimateComplexity() {
        long repeatsRangeLength = Math.min(maxRepeats, minRepeats + repeatsRangeEstimation) - minRepeats + 1;

        if ((fixedLeftBorder != -1) || (fixedRightBorder != -1))
            return Math.min(fixedSequenceMaxComplexity, repeatsRangeLength);
        else
            return notFixedSequenceMinComplexity + repeatsRangeLength * singleNucleotideComplexity
                    * lettersComplexity.get(patternSeq.symbolAt(0)) / minRepeats;
    }

    @Override
    public boolean isSingleSequence() {
        return true;
    }

    @Override
    public SinglePattern fixBorder(boolean left, int position) {
        LeftAndRightBorders newBorders = prepareNewBorders(left, position, fixedLeftBorder, fixedRightBorder,
                toString());
        return new RepeatPattern(conf, patternSeq, minRepeats, maxRepeats,
                newBorders.fixedLeftBorder, newBorders.fixedRightBorder, groupEdgePositions);
    }

    @Override
    SinglePattern setTargetId(byte targetId) {
        validateTargetId(targetId);
        return new RepeatPattern(conf, targetId, patternSeq, minRepeats, maxRepeats, fixedLeftBorder, fixedRightBorder,
                groupEdgePositions);
    }

    private class RepeatPatternMatchingResult implements MatchingResult {
        private final int fixedLeftBorder;
        private final int fixedRightBorder;
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;

        RepeatPatternMatchingResult(
                int fixedLeftBorder, int fixedRightBorder, NSequenceWithQuality target, int from, int to) {
            this.fixedLeftBorder = fixedLeftBorder;
            this.fixedRightBorder = fixedRightBorder;
            this.target = target;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<MatchIntermediate> getMatches(boolean fairSorting) {
            return new RepeatPatternOutputPort(fairSorting);
        }

        private class RepeatPatternOutputPort implements OutputPort<MatchIntermediate> {
            private final boolean uppercasePattern;
            private final int maxRepeats;
            private final boolean fixedBorder;
            private final boolean fairSorting;
            private final TargetSections targetSections;

            /* Length of longest valid section starting from this position (index1) in target inside (from->to) range.
             * Section is valid when number of errors (index2) isn't bigger than bitapMaxErrors in PatternAligner. */
            private final int[][] longestValidSections;

            private final NucleotideSequenceCaseSensitive[] sequences;
            private boolean noMoreMatches = false;

            // Data structures used for unfair sorting.
            private final HashSet<Range> uniqueRangesUnfair = new HashSet<>();
            private final HashSet<UniqueAlignedSequence> uniqueAlignedSequencesUnfair = new HashSet<>();
            private int currentRepeats;
            private int currentMaxErrors = 0;
            private int currentPosition = 0;

            // Data structures used for fair sorting and for matching in fixed position.
            private MatchIntermediate[] allMatches;
            private boolean sortingPerformed = false;
            private int takenValues = 0;

            RepeatPatternOutputPort(boolean fairSorting) {
                int maxErrors = conf.bitapMaxErrors;
                this.fixedBorder = (fixedLeftBorder != -1) || (fixedRightBorder != -1);
                if (from >= to)
                    noMoreMatches = true;
                this.uppercasePattern = Character.isUpperCase(patternSeq.symbolAt(0));
                this.maxRepeats = Math.min(RepeatPattern.this.maxRepeats, to - from);
                this.fairSorting = fairSorting;
                this.currentRepeats = maxRepeats + maxErrors;
                if (this.currentRepeats < minRepeats)
                    noMoreMatches = true;

                if (!noMoreMatches) {
                    this.targetSections = new TargetSections(target.getSequence().toString().substring(from, to),
                            patternSeq);
                    this.longestValidSections = new int[to - from][maxErrors + 1];
                    for (int i = 0; i < to - from; i++) {
                        longestValidSections[i] = new int[maxErrors + 1];
                        for (int j = 0; j <= maxErrors; j++)
                            longestValidSections[i][j] = calculateLongestValidSection(i, j);
                    }
                    this.sequences = new NucleotideSequenceCaseSensitive[Math.max(1, maxRepeats - minRepeats + 1)];
                    for (int i = 0; i < this.sequences.length; i++) {
                        NucleotideSequenceCaseSensitive[] sequencesToConcatenate
                                = new NucleotideSequenceCaseSensitive[minRepeats + i];
                        Arrays.fill(sequencesToConcatenate, patternSeq);
                        NucleotideSequenceCaseSensitive currentSequence = SequencesUtils
                                .concatenate(sequencesToConcatenate);
                        this.sequences[i] = currentSequence;
                    }
                } else {
                    this.targetSections = null;
                    this.longestValidSections = null;
                    this.sequences = null;
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

            private MatchIntermediate takeUnfair() {
                while (!noMoreMatches) {
                    int currentLongestSection = longestValidSections[currentPosition][currentMaxErrors];
                    if (Math.max(minRepeats, currentRepeats) <= currentLongestSection) {
                        Range currentRange = new Range(currentPosition + from,
                                Math.min(to, currentPosition + currentRepeats + from));
                        if (!uniqueRangesUnfair.contains(currentRange)) {
                            uniqueRangesUnfair.add(currentRange);
                            int repeats = Math.max(minRepeats, Math.min(maxRepeats, currentRepeats));
                            int firstUppercase = uppercasePattern ? 0 : -1;
                            int lastUppercase = uppercasePattern ? repeats - 1 : -1;
                            Alignment<NucleotideSequenceCaseSensitive> alignment = conf.patternAligner.align(conf,
                                    uppercasePattern, sequences[repeats - minRepeats], target,
                                    currentRange.getUpper() - 1);
                            if (alignment != null) {
                                Range targetRange = alignment.getSequence2Range();
                                UniqueAlignedSequence alignedSequence = new UniqueAlignedSequence(
                                        targetRange, repeats);
                                if ((alignment.getScore() >= conf.scoreThreshold)
                                        && !uniqueAlignedSequencesUnfair.contains(alignedSequence)) {
                                    uniqueAlignedSequencesUnfair.add(alignedSequence);
                                    pointToNextUnfairMatch();
                                    return generateMatch(alignment, target, targetId, firstUppercase, lastUppercase,
                                            fixGroupEdgePositions(groupEdgePositions, 0, targetRange.length()),
                                            conf.patternAligner.repeatsPenalty(conf, patternSeq, repeats, maxRepeats),
                                            conf.defaultGroupsOverride);
                                }
                            }
                        }
                    }
                    pointToNextUnfairMatch();
                }
                return null;
            }

            private void pointToNextUnfairMatch() {
                int maxErrors = conf.bitapMaxErrors;
                currentPosition++;
                if (currentPosition > to - from - Math.max(1, currentRepeats - currentMaxErrors)) {
                    currentPosition = 0;
                    currentMaxErrors++;
                    if (currentMaxErrors > maxErrors) {
                        currentMaxErrors = 0;
                        currentRepeats--;
                        if (currentRepeats < Math.max(1, minRepeats - maxErrors))
                            noMoreMatches = true;
                    }
                }
            }

            private MatchIntermediate takeFair() {
                if (!sortingPerformed) {
                    fillAllMatchesForFairSorting();
                    Arrays.sort(allMatches,
                            Comparator.comparingInt((MatchIntermediate match) ->
                                    match.getRange().length()).reversed());
                    Arrays.sort(allMatches, Comparator.comparingLong(MatchIntermediate::getScore).reversed());
                    sortingPerformed = true;
                }
                if (takenValues == allMatches.length) return null;
                return allMatches[takenValues++];
            }

            private MatchIntermediate takeFromFixedPosition() {
                if (!sortingPerformed) {
                    if (fixedRightBorder != -1)
                        fillAllMatchesForFixedRightBorder();
                    else if (fixedLeftBorder != -1)
                        fillAllMatchesForFixedLeftBorder();
                    else throw new IllegalArgumentException("Wrong call of takeFromFixedPosition: fixedLeftBorder="
                                + fixedLeftBorder + ", fixedRightBorder=" + fixedRightBorder);
                    Arrays.sort(allMatches,
                            Comparator.comparingInt((MatchIntermediate match) ->
                                    match.getRange().length()).reversed());
                    Arrays.sort(allMatches, Comparator.comparingLong(MatchIntermediate::getScore).reversed());
                    sortingPerformed = true;
                }
                if (takenValues == allMatches.length) return null;
                return allMatches[takenValues++];
            }

            /**
             * Fill allMatches array with all existing matches for fair sorting.
             */
            private void fillAllMatchesForFairSorting() {
                HashSet<Range> uniqueRanges = new HashSet<>();
                int maxErrors = conf.bitapMaxErrors;

                for (int repeats = maxRepeats + maxErrors; repeats >= Math.max(1, minRepeats - maxErrors); repeats--)
                    for (int i = 0; i <= to - from - Math.max(1, repeats - maxErrors); i++) {
                        int currentLongestSection = longestValidSections[i][maxErrors];
                        if (Math.max(minRepeats, repeats) <= currentLongestSection)
                            uniqueRanges.add(new Range(i + from, Math.min(to, i + repeats + from)));
                    }

                ArrayList<MatchIntermediate> allMatchesList = getMatchesList(uniqueRanges, conf);
                allMatches = new MatchIntermediate[allMatchesList.size()];
                allMatchesList.toArray(allMatches);
            }

            /**
             * Fill allMatches array with all possible alignments for fixed left border.
             */
            private void fillAllMatchesForFixedLeftBorder() {
                HashSet<Range> uniqueRanges = new HashSet<>();
                int maxErrors = conf.bitapMaxErrors;

                for (int repeats = maxRepeats + maxErrors; repeats >= Math.max(1, minRepeats - maxErrors); repeats--)
                    for (int i = 0; i <= Math.min(to - from - Math.max(1, repeats - maxErrors), maxErrors); i++) {
                        int currentLongestSection = longestValidSections[i][maxErrors];
                        if (Math.max(minRepeats, repeats) <= currentLongestSection)
                            uniqueRanges.add(new Range(i + from, Math.min(to, i + repeats + from)));
                    }

                ArrayList<MatchIntermediate> allMatchesList = getMatchesList(uniqueRanges, conf.setLeftBorder(from));
                allMatches = new MatchIntermediate[allMatchesList.size()];
                allMatchesList.toArray(allMatches);
            }

            /**
             * Fill allMatches array with all possible alignments for fixed right border.
             */
            private void fillAllMatchesForFixedRightBorder() {
                HashSet<Range> uniqueRanges = new HashSet<>();
                PatternConfiguration fixedConfiguration = (fixedLeftBorder == -1) ? conf : conf.setLeftBorder(from);
                int maxErrors = fixedConfiguration.bitapMaxErrors;

                for (int repeats = maxRepeats + maxErrors; repeats >= Math.max(1, minRepeats - maxErrors); repeats--) {
                    int minIndex = (fixedLeftBorder == -1) ? Math.max(0, to - from - repeats - maxErrors) : 0;
                    int maxIndex = (fixedLeftBorder == -1) ? to - from - repeats
                            : Math.min(maxErrors, to - from - repeats);
                    for (int i = minIndex; i <= maxIndex; i++) {
                        int currentLongestSection = longestValidSections[i][maxErrors];
                        if (Math.max(minRepeats, repeats) <= currentLongestSection)
                            uniqueRanges.add(new Range(i + from, Math.min(to, i + repeats + from)));
                    }
                }

                ArrayList<MatchIntermediate> allMatchesList = getMatchesList(uniqueRanges, fixedConfiguration);
                allMatches = new MatchIntermediate[allMatchesList.size()];
                allMatchesList.toArray(allMatches);
            }

            /**
             * Get list of aligned matches from specified set of ranges. Used for fair sorting and
             * for matching with fixed border.
             *
             * @param uniqueRanges          set of ranges where to use aligner
             * @param patternConfiguration  pattern configuration, may be configured for matching with fixed
             *                              or not fixed border
             * @return                      list of aligned matches
             */
            private ArrayList<MatchIntermediate> getMatchesList(
                    HashSet<Range> uniqueRanges, PatternConfiguration patternConfiguration) {
                ArrayList<MatchIntermediate> allMatchesList = new ArrayList<>();
                Alignment<NucleotideSequenceCaseSensitive> alignment;
                HashSet<UniqueAlignedSequence> uniqueAlignedSequences = new HashSet<>();

                for (Range range : uniqueRanges) {
                    int repeats = Math.max(minRepeats, Math.min(maxRepeats, range.length()));
                    int firstUppercase = uppercasePattern ? 0 : -1;
                    int lastUppercase = uppercasePattern ? repeats - 1 : -1;
                    alignment = patternConfiguration.patternAligner.align(patternConfiguration, uppercasePattern,
                            sequences[repeats - minRepeats], target, range.getUpper() - 1);
                    if (alignment != null) {
                        Range targetRange = alignment.getSequence2Range();
                        UniqueAlignedSequence alignedSequence = new UniqueAlignedSequence(targetRange, repeats);
                        if ((alignment.getScore() >= patternConfiguration.scoreThreshold)
                                && !uniqueAlignedSequences.contains(alignedSequence)) {
                            uniqueAlignedSequences.add(alignedSequence);
                            allMatchesList.add(generateMatch(alignment, target, targetId,
                                    firstUppercase, lastUppercase,
                                    fixGroupEdgePositions(groupEdgePositions, 0, targetRange.length()),
                                    patternConfiguration.patternAligner.repeatsPenalty(patternConfiguration,
                                            patternSeq, repeats, maxRepeats),
                                    patternConfiguration.defaultGroupsOverride));
                        }
                    }
                }

                return allMatchesList;
            }

            /**
             * Calculate length of longest valid section starting from specified position inside (from->to) range.
             * Section is considered valid if number of errors in it is not bigger than numberOfErrors.
             * Longest valid section length can end out of target bounds if numberOfErrors is not 0.
             *
             * @param position section starting position inside (from->to) range, inclusive
             * @param numberOfErrors maximum number of errors, inclusive
             * @return length of longest valid section that starts from this position
             */
            private int calculateLongestValidSection(int position, int numberOfErrors) {
                int[] sections = targetSections.sections;
                boolean currentSectionIsMatching = targetSections.firstMatching;
                int currentLength = 0;
                int currentPosition = 0;
                int currentErrors = 0;
                for (int i = 0; i < sections.length; i++, currentSectionIsMatching = !currentSectionIsMatching) {
                    int currentSectionValue = sections[i];
                    if (currentPosition + currentSectionValue < position) {
                        currentPosition += currentSectionValue;
                        continue;
                    } else if (currentPosition <= position) {
                        if (currentSectionIsMatching) {
                            currentLength = currentPosition + currentSectionValue - position;
                        } else if (currentPosition + currentSectionValue - position <= numberOfErrors) {
                            currentErrors = currentPosition + currentSectionValue - position;
                            currentLength = currentErrors;
                        } else {
                            currentLength = numberOfErrors;
                            currentErrors = numberOfErrors;
                            break;
                        }
                    } else {
                        if (currentSectionIsMatching) {
                            currentLength += currentSectionValue;
                        } else if (currentErrors + currentSectionValue <= numberOfErrors) {
                            currentErrors += currentSectionValue;
                            currentLength += currentSectionValue;
                        } else {
                            currentLength += numberOfErrors - currentErrors;
                            currentErrors = numberOfErrors;
                            break;
                        }
                    }
                    currentPosition += currentSectionValue;
                }
                return currentLength + (numberOfErrors - currentErrors);
            }
        }
    }

    /**
     * This class represents sections in the target substring (from "from" to "to" coordinates) with array of
     * integers. Each integer is length of section that consists of only matching or only non-matching letters.
     * firstMatching is true if first section is matching, otherwise false.
     */
    private static class TargetSections {
        final int[] sections;
        final boolean firstMatching;

        private static HashMap<Character, String> allMatchingLetters = new HashMap<>();
        static {
            allMatchingLetters.put('A', "Aa");
            allMatchingLetters.put('T', "Tt");
            allMatchingLetters.put('G', "Gg");
            allMatchingLetters.put('C', "Cc");
            allMatchingLetters.put('W', "AaTtWw");
            allMatchingLetters.put('S', "GgCcSs");
            allMatchingLetters.put('M', "AaCcMm");
            allMatchingLetters.put('K', "GgTtKk");
            allMatchingLetters.put('R', "AaGgRr");
            allMatchingLetters.put('Y', "CcTtYy");
            allMatchingLetters.put('B', "TtGgCcYySsKkBb");
            allMatchingLetters.put('V', "AaGgCcRrSsMmVv");
            allMatchingLetters.put('H', "AaTtCcYyWwMmHh");
            allMatchingLetters.put('D', "AaTtGgRrWwKkDd");
            new HashSet<>(allMatchingLetters.keySet())
                    .forEach(l -> allMatchingLetters.put(Character.toLowerCase(l), allMatchingLetters.get(l)));
        }

        TargetSections(String targetSubstring, NucleotideSequenceCaseSensitive patternSeq) {
            String matchingLetters = allMatchingLetters.get(patternSeq.symbolAt(0));
            if (matchingLetters == null)
                throw new IllegalArgumentException("Wrong patternSeq for RepeatPattern: "
                        + patternSeq);
            if (targetSubstring.length() < 1)
                throw new IllegalArgumentException("Wrong targetSubstring for RepeatPattern: "
                        + targetSubstring);
            else {
                ArrayList<Integer> sectionsList = new ArrayList<>();
                boolean currentSectionMatching = matchingLetters.contains(targetSubstring.substring(0, 1));
                int currentSectionLength = 1;
                this.firstMatching = currentSectionMatching;
                for (int i = 1; i < targetSubstring.length(); i++) {
                    String currentLetter = targetSubstring.substring(i, i + 1);
                    boolean currentLetterMatching = matchingLetters.contains(currentLetter);
                    if (currentLetterMatching != currentSectionMatching) {
                        sectionsList.add(currentSectionLength);
                        currentSectionLength = 1;
                        currentSectionMatching = currentLetterMatching;
                    } else
                        currentSectionLength++;
                }
                sectionsList.add(currentSectionLength);
                this.sections = sectionsList.stream().mapToInt(i -> i).toArray();
            }
        }
    }

    private static class UniqueAlignedSequence {
        private Range range;
        private int repeats;

        UniqueAlignedSequence(Range range, int repeats) {
            this.range = range;
            this.repeats = repeats;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UniqueAlignedSequence that = (UniqueAlignedSequence) o;
            return repeats == that.repeats && range.equals(that.range);
        }

        @Override
        public int hashCode() {
            int result = range.hashCode();
            result = 31 * result + repeats;
            return result;
        }
    }
}
