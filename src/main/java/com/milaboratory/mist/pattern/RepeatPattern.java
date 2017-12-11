package com.milaboratory.mist.pattern;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.Range;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.sequence.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.mist.pattern.PatternUtils.*;
import static com.milaboratory.mist.util.UnfairSorterConfiguration.*;

public final class RepeatPattern extends SinglePattern implements CanBeSingleSequence, CanFixBorders {
    private final NucleotideSequenceCaseSensitive patternSeq;
    private final int minRepeats;
    private final int maxRepeats;
    private int fixedLeftBorder;
    private int fixedRightBorder;
    private final List<GroupEdgePosition> groupEdgePositions;

    public RepeatPattern(PatternAligner patternAligner, NucleotideSequenceCaseSensitive patternSeq,
                         int minRepeats, int maxRepeats) {
        this(patternAligner, patternSeq, minRepeats, maxRepeats, new ArrayList<>());
    }

    public RepeatPattern(PatternAligner patternAligner, NucleotideSequenceCaseSensitive patternSeq,
                         int minRepeats, int maxRepeats, int fixedLeftBorder, int fixedRightBorder) {
        this(patternAligner, patternSeq, minRepeats, maxRepeats, fixedLeftBorder, fixedRightBorder, new ArrayList<>());

    }

    public RepeatPattern(PatternAligner patternAligner, NucleotideSequenceCaseSensitive patternSeq,
                         int minRepeats, int maxRepeats, List<GroupEdgePosition> groupEdgePositions) {
        this(patternAligner, patternSeq, minRepeats, maxRepeats, -1, -1, groupEdgePositions);
    }

    /**
     * Match several repeats of specified nucleotide or wildcard. Number of repeats specified as interval.
     * Calls FuzzyMatchPattern to find matches for each number of repeats.
     *
     * @param patternAligner pattern aligner, for FuzzyMatchPattern
     * @param patternSeq 1 character case sensitive nucleotide sequence to repeat
     * @param minRepeats minimum number of repeats; minimum allowed value is 1
     * @param maxRepeats maximum number of repeats; use Integer.MAX_VALUE to match without maximum limit of repeats
     * @param fixedLeftBorder position in target where must be the left border, for FuzzyMatchPattern
     * @param fixedRightBorder position in target where must be the right border, for FuzzyMatchPattern
     * @param groupEdgePositions list of group edges and their positions, for FuzzyMatchPattern.
     *                           Group edges beyond the right border of motif will be moved to the right border.
     */
    public RepeatPattern(PatternAligner patternAligner, NucleotideSequenceCaseSensitive patternSeq,
                         int minRepeats, int maxRepeats, int fixedLeftBorder, int fixedRightBorder,
                         List<GroupEdgePosition> groupEdgePositions) {
        super(patternAligner);
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
        int fixedLeftBorder = (this.fixedLeftBorder > -2) ? this.fixedLeftBorder
                : target.size() - 1 - invertCoordinate(this.fixedLeftBorder);
        int fixedRightBorder = (this.fixedRightBorder > -2) ? this.fixedRightBorder
                : target.size() - 1 - invertCoordinate(this.fixedRightBorder);
        int fromWithBorder = (fixedLeftBorder == -1) ? from : Math.max(from, fixedLeftBorder);
        // to is exclusive and fixedRightBorder is inclusive
        int toWithBorder = (fixedRightBorder == -1) ? to : Math.min(to, fixedRightBorder + 1);
        return new RepeatPatternMatchingResult(fixedLeftBorder, fixedRightBorder, target, fromWithBorder, toWithBorder);
    }

    @Override
    public int estimateMaxLength() {
        if (maxRepeats == Integer.MAX_VALUE)
            return -1;
        else {
            boolean useBitapMaxErrors = Character.isLowerCase(patternSeq.symbolAt(0))
                    && !nLetters.contains(patternSeq.toString());
            return maxRepeats + (useBitapMaxErrors ? patternAligner.bitapMaxErrors() : 0);
        }
    }

    @Override
    public int estimateMaxOverlap() {
        return Character.isUpperCase(patternSeq.symbolAt(0)) ? 0 : maxRepeats - 1;
    }

    @Override
    public long estimateComplexity() {
        long repeatsRangeLength = Math.min(maxRepeats, minRepeats + repeatsRangeEstimation) - minRepeats + 1;

        if (isBorderFixed())
            return Math.min(fixedSequenceMaxComplexity, repeatsRangeLength);
        else {
            int minRepeatsFactor = nLetters.contains(patternSeq.toString()) ? 1 : minRepeats;
            return notFixedSequenceMinComplexity + repeatsRangeLength * singleNucleotideComplexity
                    * lettersComplexity.get(patternSeq.symbolAt(0)) / minRepeatsFactor;
        }
    }

    @Override
    public boolean isSingleSequence() {
        return true;
    }

    @Override
    public void fixBorder(boolean left, int position) {
        if (left) {
            if (fixedLeftBorder == -1)
                fixedLeftBorder = position;
            else if (fixedLeftBorder != position)
                throw new IllegalStateException(toString() + ": trying to set fixed left border to " + position
                        + " when it is already fixed at " + fixedLeftBorder + "!");
        } else {
            if (fixedRightBorder == -1)
                fixedRightBorder = position;
            else if (fixedRightBorder != position)
                throw new IllegalStateException(toString() + ": trying to set fixed right border to " + position
                        + " when it is already fixed at " + fixedRightBorder + "!");
        }
    }

    @Override
    public boolean isBorderFixed(boolean left) {
        if (left)
            return fixedLeftBorder != -1;
        else
            return fixedRightBorder != -1;
    }

    private class RepeatPatternMatchingResult implements MatchingResult {
        private final int fixedLeftBorder;
        private final int fixedRightBorder;
        private final NSequenceWithQuality target;
        private final int from;
        private final int to;

        RepeatPatternMatchingResult(int fixedLeftBorder, int fixedRightBorder, NSequenceWithQuality target,
                                    int from, int to) {
            this.fixedLeftBorder = fixedLeftBorder;
            this.fixedRightBorder = fixedRightBorder;
            this.target = target;
            this.from = from;
            this.to = to;
        }

        @Override
        public OutputPort<Match> getMatches(boolean fairSorting) {
            return new RepeatPatternOutputPort(fairSorting);
        }

        private class RepeatPatternOutputPort implements OutputPort<Match> {
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
            private Match[] allMatches;
            private boolean sortingPerformed = false;
            private int takenValues = 0;

            RepeatPatternOutputPort(boolean fairSorting) {
                int maxErrors = patternAligner.bitapMaxErrors();
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
            public Match take() {
                Match match;
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

            private Match takeUnfair() {
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
                            Alignment<NucleotideSequenceCaseSensitive> alignment = patternAligner.align(
                                    sequences[repeats - minRepeats], target,
                                    currentRange.getUpper() - 1);
                            Range targetRange = alignment.getSequence2Range();
                            UniqueAlignedSequence alignedSequence = new UniqueAlignedSequence(targetRange, repeats);
                            if ((alignment.getScore() >= patternAligner.penaltyThreshold())
                                    && !uniqueAlignedSequencesUnfair.contains(alignedSequence)) {
                                uniqueAlignedSequencesUnfair.add(alignedSequence);
                                pointToNextUnfairMatch();
                                return generateMatch(alignment, target, targetId, firstUppercase, lastUppercase,
                                        fixGroupEdgePositions(groupEdgePositions, 0, targetRange.length()),
                                        patternAligner.repeatsPenalty(patternSeq, repeats, maxRepeats));
                            }
                        }
                    }
                    pointToNextUnfairMatch();
                }
                return null;
            }

            private void pointToNextUnfairMatch() {
                int maxErrors = patternAligner.bitapMaxErrors();
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

            private Match takeFair() {
                if (!sortingPerformed) {
                    fillAllMatchesForFairSorting();
                    Arrays.sort(allMatches, Comparator.comparingInt((Match match) -> match.getRange().length())
                            .reversed());
                    Arrays.sort(allMatches, Comparator.comparingLong(Match::getScore).reversed());
                    sortingPerformed = true;
                }
                if (takenValues == allMatches.length) return null;
                return allMatches[takenValues++];
            }

            private Match takeFromFixedPosition() {
                if (!sortingPerformed) {
                    if (fixedRightBorder != -1)
                        fillAllMatchesForFixedRightBorder();
                    else if (fixedLeftBorder != -1)
                        fillAllMatchesForFixedLeftBorder();
                    else throw new IllegalArgumentException("Wrong call of takeFromFixedPosition: fixedLeftBorder="
                                + fixedLeftBorder + ", fixedRightBorder=" + fixedRightBorder);
                    Arrays.sort(allMatches, Comparator.comparingInt((Match match) -> match.getRange().length())
                            .reversed());
                    Arrays.sort(allMatches, Comparator.comparingLong(Match::getScore).reversed());
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
                int maxErrors = patternAligner.bitapMaxErrors();

                for (int repeats = maxRepeats + maxErrors; repeats >= Math.max(1, minRepeats - maxErrors); repeats--)
                    for (int i = 0; i <= to - from - Math.max(1, repeats - maxErrors); i++) {
                        int currentLongestSection = longestValidSections[i][maxErrors];
                        if (Math.max(minRepeats, repeats) <= currentLongestSection)
                            uniqueRanges.add(new Range(i + from, Math.min(to, i + repeats + from)));
                    }

                ArrayList<Match> allMatchesList = getMatchesList(uniqueRanges, patternAligner);
                allMatches = new Match[allMatchesList.size()];
                allMatchesList.toArray(allMatches);
            }

            /**
             * Fill allMatches array with all possible alignments for fixed left border.
             */
            private void fillAllMatchesForFixedLeftBorder() {
                HashSet<Range> uniqueRanges = new HashSet<>();
                int maxErrors = patternAligner.bitapMaxErrors();

                for (int repeats = maxRepeats + maxErrors; repeats >= Math.max(1, minRepeats - maxErrors); repeats--)
                    for (int i = 0; i <= Math.min(to - from - Math.max(1, repeats - maxErrors), maxErrors); i++) {
                        int currentLongestSection = longestValidSections[i][maxErrors];
                        if (Math.max(minRepeats, repeats) <= currentLongestSection)
                            uniqueRanges.add(new Range(i + from, Math.min(to, i + repeats + from)));
                    }

                ArrayList<Match> allMatchesList = getMatchesList(uniqueRanges, patternAligner.setLeftBorder(from));
                allMatches = new Match[allMatchesList.size()];
                allMatchesList.toArray(allMatches);
            }

            /**
             * Fill allMatches array with all possible alignments for fixed right border.
             */
            private void fillAllMatchesForFixedRightBorder() {
                HashSet<Range> uniqueRanges = new HashSet<>();
                PatternAligner aligner = (fixedLeftBorder == -1) ? patternAligner : patternAligner.setLeftBorder(from);
                int maxErrors = aligner.bitapMaxErrors();

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

                ArrayList<Match> allMatchesList = getMatchesList(uniqueRanges, aligner);
                allMatches = new Match[allMatchesList.size()];
                allMatchesList.toArray(allMatches);
            }

            /**
             * Get list of aligned matches from specified set of ranges. Used for fair sorting and
             * for matching with fixed border.
             *
             * @param uniqueRanges set of ranges where to use aligner
             * @param aligner pattern aligner, maybe configured for matching with fixed border
             * @return list of aligned matches
             */
            private ArrayList<Match> getMatchesList(HashSet<Range> uniqueRanges, PatternAligner aligner) {
                ArrayList<Match> allMatchesList = new ArrayList<>();
                Alignment<NucleotideSequenceCaseSensitive> alignment;
                HashSet<UniqueAlignedSequence> uniqueAlignedSequences = new HashSet<>();

                for (Range range : uniqueRanges) {
                    int repeats = Math.max(minRepeats, Math.min(maxRepeats, range.length()));
                    int firstUppercase = uppercasePattern ? 0 : -1;
                    int lastUppercase = uppercasePattern ? repeats - 1 : -1;
                    alignment = aligner.align(sequences[repeats - minRepeats], target,
                            range.getUpper() - 1);
                    Range targetRange = alignment.getSequence2Range();
                    UniqueAlignedSequence alignedSequence = new UniqueAlignedSequence(targetRange, repeats);
                    if ((alignment.getScore() >= aligner.penaltyThreshold())
                            && !uniqueAlignedSequences.contains(alignedSequence)) {
                        uniqueAlignedSequences.add(alignedSequence);
                        allMatchesList.add(generateMatch(alignment, target, targetId, firstUppercase, lastUppercase,
                                fixGroupEdgePositions(groupEdgePositions, 0, targetRange.length()),
                                        patternAligner.repeatsPenalty(patternSeq, repeats, maxRepeats)));
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
            allMatchingLetters.put('W', "AaTt");
            allMatchingLetters.put('S', "GgCc");
            allMatchingLetters.put('M', "AaCc");
            allMatchingLetters.put('K', "GgTt");
            allMatchingLetters.put('R', "AaGg");
            allMatchingLetters.put('Y', "CcTt");
            allMatchingLetters.put('B', "TtGgCc");
            allMatchingLetters.put('V', "AaGgCc");
            allMatchingLetters.put('H', "AaTtCc");
            allMatchingLetters.put('D', "AaTtGg");
            allMatchingLetters.put('N', "AaTtGgCc");
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
