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
package com.milaboratory.minnn.util;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.alignment.*;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.io.sequence.fastq.SingleFastqReader;
import com.milaboratory.core.io.sequence.fastq.SingleFastqWriter;
import com.milaboratory.core.mutations.Mutation;
import com.milaboratory.core.mutations.Mutations;
import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.pattern.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.milaboratory.core.mutations.Mutation.*;
import static com.milaboratory.core.sequence.SequencesUtils.concatenate;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.pattern.PatternUtils.invertCoordinate;
import static com.milaboratory.minnn.util.CommonTestUtils.RandomStringType.*;
import static com.milaboratory.test.TestUtil.randomSequence;
import static org.junit.Assert.*;

public class CommonTestUtils {
    public static final Random rg = new Random();
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/";
    private static final String LN_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String QUERY_CHAR_STRING = " ATGCNatgcn()[]{}^$:+&|\\~0123456789-*";

    public static <T> Stream<T> streamPort(OutputPort<T> port) {
        return StreamSupport.stream(CUtils.it(port).spliterator(), false);
    }

    public static long countPortValues(OutputPort<?> port) {
        return streamPort(port).count();
    }

    public static long countMatches(MatchingResult matchingResult, boolean fair) {
        return countPortValues(matchingResult.getMatches(fair));
    }

    public static NSequenceWithQuality mutateSeqWithRandomQuality(
            NSequenceWithQuality originalSeqWithQuality, Mutations<NucleotideSequence> mutations) {
        Alphabet<NucleotideSequence> alphabet = NucleotideSequence.ALPHABET;
        NucleotideSequence originalSeq = originalSeqWithQuality.getSequence();
        SequenceQuality originalQual = originalSeqWithQuality.getQuality();
        NSequenceWithQualityBuilder builder = new NSequenceWithQualityBuilder();
        int pointer = 0;
        int mutPointer = 0;
        int mut;
        byte qualityAtPointer;
        while (pointer < originalSeq.size() || mutPointer < mutations.size()) {
            if (mutPointer < mutations.size()
                    && ((mut = mutations.getMutation(mutPointer)) >>> POSITION_OFFSET) <= pointer)
                switch (mut & MUTATION_TYPE_MASK) {
                    case RAW_MUTATION_TYPE_SUBSTITUTION:
                        if (((mut >> FROM_OFFSET) & LETTER_MASK) != originalSeq.codeAt(pointer))
                            throw new IllegalArgumentException("Mutation = " + Mutation.toString(alphabet, mut) +
                                    " but seq[" + pointer + "]=" + originalSeq.symbolAt(pointer));
                        qualityAtPointer = originalQual.value(pointer);
                        ++pointer;
                        builder.append(new NSequenceWithQuality(
                                new NucleotideSequence(new byte[] { (byte)(mut & LETTER_MASK) }),
                                (byte)(rg.nextInt(qualityAtPointer + 1))));
                        ++mutPointer;
                        break;
                    case RAW_MUTATION_TYPE_DELETION:
                        if (((mut >> FROM_OFFSET) & LETTER_MASK) != originalSeq.codeAt(pointer))
                            throw new IllegalArgumentException("Mutation = " + Mutation.toString(alphabet, mut) +
                                    " but seq[" + pointer + "]=" + originalSeq.symbolAt(pointer));
                        ++pointer;
                        ++mutPointer;
                        break;
                    case RAW_MUTATION_TYPE_INSERTION:
                        if (pointer >= originalSeq.size())
                            qualityAtPointer = originalQual.value(originalQual.size() - 1);
                        else
                            qualityAtPointer = originalQual.value(pointer);
                        builder.append(new NSequenceWithQuality(
                                new NucleotideSequence(new byte[] { (byte)(mut & LETTER_MASK) }),
                                (byte)(rg.nextInt(qualityAtPointer + 1))));
                        ++mutPointer;
                        break;
                }
            else {
                builder.append(originalSeqWithQuality.getRange(pointer, pointer + 1));
                pointer++;
            }
        }
        return builder.createAndDestroy();
    }

    public static NSequenceWithQuality randomSeqWithQuality(int length, boolean basicLettersOnly) {
        NucleotideSequence randomSeq = randomSequence(NucleotideSequence.ALPHABET, length, length, basicLettersOnly);
        SequenceQualityBuilder randomQualityBuilder = new SequenceQualityBuilder();
        IntStream.range(0, length)
                .forEach(i -> randomQualityBuilder.append((byte)(rg.nextInt(DEFAULT_MAX_QUALITY) + 1)));
        return new NSequenceWithQuality(randomSeq, randomQualityBuilder.createAndDestroy());
    }

    public static NSequenceWithQuality randomSeqWithWildcardShare(int length, float wildcardShare) {
        NucleotideAlphabet alphabet = NucleotideSequence.ALPHABET;
        NSequenceWithQualityBuilder builder = new NSequenceWithQualityBuilder();
        for (int i = 0; i < length; i++) {
            int letter = (rg.nextFloat() < wildcardShare)
                    ? rg.nextInt(alphabet.size() - alphabet.basicSize()) + alphabet.basicSize()
                    : rg.nextInt(alphabet.basicSize());
            NucleotideSequence seq = new NucleotideSequence(new byte[] { (byte)letter });
            SequenceQuality qual = new SequenceQuality(new byte[] { (byte)(rg.nextInt(DEFAULT_MAX_QUALITY) + 1) });
            builder.append(new NSequenceWithQuality(seq, qual));
        }
        return builder.createAndDestroy();
    }

    @SuppressWarnings("unchecked")
    public static <S extends Seq<S>> S randomSeq(S sampleInstance, int length) {
        if (sampleInstance instanceof NucleotideSequenceCaseSensitive)
            return (S)randomSequence(NucleotideSequenceCaseSensitive.ALPHABET, length, length);
        else if (sampleInstance instanceof NucleotideSequence)
            return (S)randomSequence(NucleotideSequence.ALPHABET, length, length);
        else if (sampleInstance instanceof NSequenceWithQuality)
            return (S)randomSeqWithQuality(length, false);
        else
            throw new IllegalArgumentException(sampleInstance.getClass().toString());
    }

    @SuppressWarnings("unchecked")
    public static <S extends Seq<S>> S makeRandomInsertions(S seq, int number) {
        S result = seq;
        int currentLength;
        int currentInsertPosition;
        for (int i = 0; i < number; i++) {
            currentLength = seq.size() + i;
            currentInsertPosition = (currentLength == 0) ? 0 : rg.nextInt(currentLength);
            result = concatenate(result.getRange(0, currentInsertPosition), randomSeq(seq, 1),
                    result.getRange(currentInsertPosition, currentLength));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <S extends Seq<S>> S makeRandomDeletions(S seq, int number) {
        assertTrue(seq.size() > number);
        S result = seq;
        int currentLength;
        int currentDeletePosition;
        for (int i = 0; i < number; i++) {
            currentLength = seq.size() - i;
            currentDeletePosition = rg.nextInt(currentLength);
            result = concatenate(result.getRange(0, currentDeletePosition),
                    result.getRange(currentDeletePosition + 1, currentLength));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <S extends Seq<S>> S makeRandomSubstitutions(S seq, int number) {
        assertTrue(seq.size() > 0);
        S result = seq;
        int currentPosition;
        for (int i = 0; i < number; i++) {
            currentPosition = rg.nextInt(seq.size());
            result = concatenate(result.getRange(0, currentPosition), randomSeq(seq, 1),
                    result.getRange(currentPosition + 1, seq.size()));
        }
        return result;
    }

    public static <S extends Seq<S>> S makeRandomErrors(S seq, int number) {
        assertTrue(seq.size() > 0);
        if (number == 0)
            return seq;
        int numSubstitutions = rg.nextInt(number + 1);
        int numDeletions = Math.min(seq.size() - 1, rg.nextInt(number - numSubstitutions + 1));
        int numInsertions = number - numSubstitutions - numDeletions;
        S result = makeRandomSubstitutions(seq, numSubstitutions);
        result = makeRandomDeletions(result, numDeletions);
        return makeRandomInsertions(result, numInsertions);
    }

    public static NucleotideSequenceCaseSensitive toLowerCase(NucleotideSequenceCaseSensitive seq) {
        return new NucleotideSequenceCaseSensitive(seq.toString().toLowerCase());
    }

    public static NucleotideSequence getRandomSubsequence(NucleotideSequence seq) {
        assertTrue(seq.size() > 0);
        int position1 = rg.nextInt(seq.size());
        int position2 = rg.nextInt(seq.size());

        return seq.getRange(Math.min(position1, position2), Math.max(position1, position2) + 1);
    }

    public static NSequenceWithQuality setRandomQuality(String seq) {
        int length = seq.length();
        byte[] quality = new byte[length];
        for (int i = 0; i < length; i++)
            quality[i] = (byte)(rg.nextInt(DEFAULT_GOOD_QUALITY + 1 - DEFAULT_BAD_QUALITY) + DEFAULT_BAD_QUALITY);
        return new NSequenceWithQuality(new NucleotideSequence(seq), new SequenceQuality(quality));
    }

    public static boolean equalByWildcards(NucleotideSequence seq1, NucleotideSequence seq2) {
        if (seq1.size() != seq2.size())
            return false;
        for (int i = 0; i < seq1.size(); i++)
            if (!NucleotideSequence.ALPHABET.codeToWildcard(seq1.codeAt(i))
                    .intersectsWith(NucleotideSequence.ALPHABET.codeToWildcard(seq2.codeAt(i))))
                return false;
        return true;
    }

    public static PatternAndTargetAlignmentScoring getTestScoring() {
        return new PatternAndTargetAlignmentScoring(0, -9, -10,
                -9, DEFAULT_GOOD_QUALITY, DEFAULT_BAD_QUALITY, 0);
    }

    public static PatternAndTargetAlignmentScoring getRandomScoring() {
        int matchScore = rg.nextInt(20) - 10;
        int mismatchScore = Math.min(-1, matchScore - 1 - rg.nextInt(10));
        int gapPenalty = Math.min(-1, matchScore - 1 - rg.nextInt(10));
        int uppercaseMismatchScore = mismatchScore - rg.nextInt(10);
        byte goodQuality = (byte)(rg.nextInt(10) + 24);
        byte badQuality = (byte)(rg.nextInt(10));
        int minMismatchDifference = Math.min(matchScore - mismatchScore, matchScore - gapPenalty);
        int maxQualityPenalty = (minMismatchDifference > 1) ? -rg.nextInt(minMismatchDifference) : 0;
        return new PatternAndTargetAlignmentScoring(matchScore, mismatchScore, gapPenalty, uppercaseMismatchScore,
                goodQuality, badQuality, maxQualityPenalty);
    }

    public static PatternConfiguration getTestPatternConfiguration() {
        return getTestPatternConfiguration(0);
    }

    public static PatternConfiguration getTestPatternConfiguration(boolean zeroThreshold) {
        return getTestPatternConfiguration(zeroThreshold, false);
    }

    public static PatternConfiguration getTestPatternConfiguration(
            boolean zeroThreshold, boolean defaultGroupsOverride) {
        return getTestPatternConfiguration(zeroThreshold ? 0 : Long.MIN_VALUE,  0, 0,
                -1, -1, -1, getTestScoring(), defaultGroupsOverride);
    }

    public static PatternConfiguration getTestPatternConfiguration(int bitapMaxErrors) {
        return getTestPatternConfiguration(Long.MIN_VALUE, bitapMaxErrors, 0, -1);
    }

    public static PatternConfiguration getTestPatternConfiguration(
            long scoreThreshold, int bitapMaxErrors, long notResultScore, long singleOverlapPenalty) {
        return getTestPatternConfiguration(scoreThreshold, bitapMaxErrors, notResultScore, singleOverlapPenalty,
                -1);
    }

    public static PatternConfiguration getTestPatternConfiguration(
            long scoreThreshold, int bitapMaxErrors, long notResultScore, long singleOverlapPenalty, int maxOverlap) {
        return getTestPatternConfiguration(scoreThreshold, bitapMaxErrors, notResultScore, singleOverlapPenalty,
                maxOverlap, -1, getTestScoring(), false);
    }

    public static PatternConfiguration getTestPatternConfiguration(
            long scoreThreshold, int bitapMaxErrors, long notResultScore, long singleOverlapPenalty, int maxOverlap,
            int fixedLeftBorder, PatternAndTargetAlignmentScoring scoring, boolean defaultGroupsOverride) {
        return new PatternConfiguration(defaultGroupsOverride, new BasePatternAligner(), scoring, scoreThreshold,
                singleOverlapPenalty, bitapMaxErrors, maxOverlap, fixedLeftBorder, notResultScore);
    }

    public static String inQuotes(String str) {
        return '"' + str + '"';
    }

    public static String repeatString(String str, int num) {
        return new String(new char[num]).replace("\0", str);
    }

    public enum RandomStringType {
        UNICODE,
        LIMITED,
        LETTERS_AND_NUMBERS,
        QUERY_CHARACTERS
    }

    public static String getRandomString(int length) {
        return getRandomString(length, "", UNICODE);
    }

    public static String getRandomString(int length, String exclude) {
        return getRandomString(length, exclude, UNICODE);
    }

    public static String getRandomString(int length, String exclude, RandomStringType type) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c;
            do {
                switch (type) {
                    case LIMITED:
                        c = (char)(rg.nextInt(rg.nextInt(128) + 128));
                        break;
                    case LETTERS_AND_NUMBERS:
                        c = LN_STRING.charAt(rg.nextInt(LN_STRING.length()));
                        break;
                    case QUERY_CHARACTERS:
                        c = QUERY_CHAR_STRING.charAt(rg.nextInt(QUERY_CHAR_STRING.length()));
                        break;
                    case UNICODE:
                    default:
                        c = (char)(rg.nextInt(rg.nextInt(2) == 0 ? 128 : Character.MAX_VALUE));
                }
            } while (exclude.contains(Character.toString(c)));
            sb.append(c);
        }
        return sb.toString();
    }

    public static <T extends Enum<?>> T getRandomEnumItem(Class<T> enumClass) {
        return enumClass.getEnumConstants()[rg.nextInt(enumClass.getEnumConstants().length)];
    }

    public static ArrayList<GroupEdgePosition> getRandomGroupsForFuzzyMatch(int maxCoordinate) {
         return getRandomGroupsForFuzzyMatch(maxCoordinate, rg.nextInt(40) + 1);
    }

    public static ArrayList<GroupEdgePosition> getRandomGroupsForFuzzyMatch(int maxCoordinate, int numGroups) {
        if (maxCoordinate < 1)
            throw new IllegalArgumentException("maxCoordinate=" + maxCoordinate);
        ArrayList<GroupEdgePosition> groupEdgePositions = new ArrayList<>();
        while (groupEdgePositions.size() < numGroups * 2) {
            String groupName = getRandomString(rg.nextInt(25) + 5, "R", LETTERS_AND_NUMBERS);
            if (groupEdgePositions.stream().anyMatch(g -> g.getGroupEdge().getGroupName().equals(groupName)))
                continue;
            int leftPosition = rg.nextInt(maxCoordinate);
            int rightPosition = rg.nextInt(maxCoordinate - leftPosition) + leftPosition + 1;
            groupEdgePositions.add(new GroupEdgePosition(new GroupEdge(groupName, true), leftPosition));
            groupEdgePositions.add(new GroupEdgePosition(new GroupEdge(groupName, false), rightPosition));
        }
        return groupEdgePositions;
    }

    public static PatternConfiguration getRandomPatternConfiguration() {
        return getTestPatternConfiguration(-rg.nextInt(100), rg.nextInt(4), -rg.nextInt(4), -rg.nextInt(3),
                -1, -1, getRandomScoring(), rg.nextBoolean());
    }

    public static FuzzyMatchPattern getRandomFuzzyPattern(
            PatternConfiguration patternConfiguration, boolean withGroups) {
        int length = rg.nextInt(150) + 1;
        RandomBorders randomBorders = new RandomBorders(length);
        RandomCuts randomCuts = new RandomCuts(length);
        NucleotideSequenceCaseSensitive seq = randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                length, length);
        return new FuzzyMatchPattern(patternConfiguration, seq, randomCuts.left, randomCuts.right,
                randomBorders.left, randomBorders.right,
                withGroups ? getRandomGroupsForFuzzyMatch(length) : new ArrayList<>());
    }

    public static SinglePattern getRandomRepeatPattern(
            PatternConfiguration patternConfiguration, boolean withGroups) {
        int minRepeats = rg.nextInt(10) + 1;
        int maxRepeats = rg.nextInt(100) + minRepeats;
        RandomBorders randomBorders = new RandomBorders(maxRepeats);
        NucleotideSequenceCaseSensitive seq = randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                1, 1);
        if (Character.toUpperCase(seq.toString().charAt(0)) == 'N')
            return new RepeatNPattern(patternConfiguration, minRepeats, maxRepeats,
                    randomBorders.left, randomBorders.right,
                    withGroups ? getRandomGroupsForFuzzyMatch(maxRepeats) : new ArrayList<>());
        else
            return new RepeatPattern(patternConfiguration, seq, minRepeats, maxRepeats,
                    randomBorders.left, randomBorders.right,
                    withGroups ? getRandomGroupsForFuzzyMatch(maxRepeats) : new ArrayList<>());
    }

    public static AnyPattern getRandomAnyPattern(
            PatternConfiguration patternConfiguration, boolean withGroups) {
        return new AnyPattern(patternConfiguration,
                withGroups ? getRandomGroupsForFuzzyMatch(1).stream()
                        .map(GroupEdgePosition::getGroupEdge).collect(Collectors.toCollection(ArrayList::new))
                        : new ArrayList<>());
    }

    public static SinglePattern getRandomBasicPattern() {
        return getRandomBasicPattern(false);
    }

    public static SinglePattern getRandomBasicPattern(PatternConfiguration patternConfiguration) {
        return getRandomBasicPattern(patternConfiguration, false);
    }

    public static SinglePattern getRandomBasicPattern(boolean withGroups) {
        return getRandomBasicPattern(getRandomPatternConfiguration(), withGroups);
    }

    public static SinglePattern getRandomBasicPattern(PatternConfiguration patternConfiguration, boolean withGroups) {
        switch (rg.nextInt(3)) {
            case 0:
                return getRandomFuzzyPattern(patternConfiguration, withGroups);
            case 1:
                return getRandomRepeatPattern(patternConfiguration, withGroups);
            default:
                return getRandomAnyPattern(patternConfiguration, withGroups);
        }
    }

    public static FullReadPattern getRandomSingleReadPattern(SinglePattern... patterns) {
        return getRandomSingleReadPattern(getRandomPatternConfiguration(), patterns);
    }

    public static FullReadPattern getRandomSingleReadPattern(
            PatternConfiguration patternConfiguration, SinglePattern... singlePatterns) {
        return wrapWithFullReadPattern(patternConfiguration,
                getRandomRawSinglePattern(patternConfiguration, singlePatterns));
    }

    public static SinglePattern getRandomRawSinglePattern(SinglePattern... patterns) {
        return getRandomRawSinglePattern(getRandomPatternConfiguration(), patterns);
    }

    public static SinglePattern getRandomRawSinglePattern(
            PatternConfiguration patternConfiguration, SinglePattern... singlePatterns) {
        SinglePattern[] patterns;
        if (singlePatterns.length == 0) {
            int numPatterns = rg.nextInt(5) + 1;
            patterns = new SinglePattern[numPatterns];
            for (int i = 0; i < numPatterns; i++)
                patterns[i] = getRandomBasicPattern(patternConfiguration);
        } else
            patterns = singlePatterns;
        boolean foundAnyPattern = Arrays.stream(patterns).anyMatch(p -> p instanceof AnyPattern);
        switch (patterns[0] instanceof AnyPattern ? 0 : rg.nextInt(foundAnyPattern ? 3 : 7)) {
            case 0:
                return patterns[0];
            case 1:
                return new FilterPattern(patternConfiguration,
                        new ScoreFilter(-rg.nextInt(75)), patterns[0]);
            case 2:
                return new FilterPattern(patternConfiguration,
                        new StickFilter(rg.nextBoolean(), rg.nextInt(30)),
                        patterns[0]);
            case 3:
                return new AndPattern(patternConfiguration, patterns);
            case 4:
                return new PlusPattern(patternConfiguration, patterns);
            case 5:
                return new SequencePattern(patternConfiguration, patterns);
            case 6:
            default:
                return new OrPattern(patternConfiguration, patterns);
        }
    }

    private static FullReadPattern wrapWithFullReadPattern(
            PatternConfiguration patternConfiguration, SinglePattern singlePattern) {
        FullReadPattern fullReadPattern = new FullReadPattern(patternConfiguration, singlePattern);
        fullReadPattern.setTargetId((byte)1);
        return fullReadPattern;
    }

    public static MultipleReadsOperator getRandomMultiReadPattern(MultipleReadsOperator... patterns) {
        return getRandomMultiReadPattern(getRandomPatternConfiguration(), -1, patterns);
    }

    public static MultipleReadsOperator getRandomMultiReadPattern(int numPatterns) {
        return getRandomMultiReadPattern(getRandomPatternConfiguration(), numPatterns);
    }

    public static MultipleReadsOperator getRandomMultiReadPattern(
            PatternConfiguration patternConfiguration, int numPatterns, MultipleReadsOperator... patterns) {
        if (patterns.length == 0) {
            if (numPatterns == -1)
                numPatterns = rg.nextInt(5) + 1;
            SinglePattern[] basicPatterns = new SinglePattern[numPatterns];
            for (int i = 0; i < numPatterns; i++)
                basicPatterns[i] = getRandomBasicPattern(patternConfiguration);
            return createMultiPattern(patternConfiguration, basicPatterns);
        } else {
            switch (rg.nextInt(4)) {
                case 0:
                    return new AndOperator(patternConfiguration, patterns);
                case 1:
                    return new OrOperator(patternConfiguration, patterns);
                case 2:
                    return new NotOperator(patternConfiguration, patterns[0]);
                case 3:
                default:
                    return new MultipleReadsFilterPattern(patternConfiguration,
                            new ScoreFilter(-rg.nextInt(75)), patterns[0]);
            }
        }
    }

    public static MultipleReadsOperator[] singleToMultiPatterns(SinglePattern... singlePatterns) {
        return singleToMultiPatterns(getTestPatternConfiguration(), singlePatterns);
    }

    public static MultipleReadsOperator[] singleToMultiPatterns(
            PatternConfiguration patternConfiguration, SinglePattern... singlePatterns) {
        return Arrays.stream(singlePatterns).map(sp -> createMultiPattern(patternConfiguration, sp))
                .toArray(MultipleReadsOperator[]::new);
    }

    public static MultiPattern createMultiPattern(
            PatternConfiguration patternConfiguration, SinglePattern... singlePatterns) {
        return new MultiPattern(patternConfiguration, Arrays.stream(singlePatterns)
                .map(sp -> new FullReadPattern(patternConfiguration, sp)).toArray(SinglePattern[]::new));
    }

    public static String bestToString(MatchingResult matchingResult) {
        return bestToString(matchingResult, true);
    }

    public static String bestToString(MatchingResult matchingResult, boolean fairSorting) {
        return matchingResult.getBestMatch(fairSorting).getValue().getSequence().toString();
    }

    public static MultiNSequenceWithQuality createMultiNSeq(String seq) {
        return new MultiNSequenceWithQualityImpl(new NSequenceWithQuality(seq));
    }

    public static MultiNSequenceWithQuality createMultiNSeq(String seq, int repeats) {
        NSequenceWithQuality singleSeq = new NSequenceWithQuality(seq);
        return new MultiNSequenceWithQualityImpl(Collections.nCopies(repeats, singleSeq)
                .toArray(new NSequenceWithQuality[repeats]));
    }

    public static <T> void assertUnorderedArrayEquals(T[] array1, T[] array2) {
        assertEquals(array1.length, array2.length);
        Comparator<T> comparator = Comparator.comparing(Object::hashCode);
        List<T> list1Sorted = Arrays.stream(array1).sorted(comparator).collect(Collectors.toList());
        List<T> list2Sorted = Arrays.stream(array2).sorted(comparator).collect(Collectors.toList());
        for (int i = 0; i < array1.length; i++)
            assertEquals(list1Sorted.get(i), list2Sorted.get(i));
    }

    public static <T> void assertUnorderedListEquals(List<T> list1, List<T> list2) {
        assertEquals(list1.size(), list2.size());
        Comparator<T> comparator = Comparator.comparing(Object::hashCode);
        List<T> list1Sorted = list1.stream().sorted(comparator).collect(Collectors.toList());
        List<T> list2Sorted = list2.stream().sorted(comparator).collect(Collectors.toList());
        for (int i = 0; i < list1.size(); i++)
            assertEquals(list1Sorted.get(i), list2Sorted.get(i));
    }

    public static void assertFileEquals(String fileName1, String fileName2) throws Exception {
        assertArrayEquals(Files.readAllBytes(Paths.get(fileName1)), Files.readAllBytes(Paths.get(fileName2)));
    }

    public static void assertFileEquals(String message, String fileName1, String fileName2) throws Exception {
        assertArrayEquals(message, Files.readAllBytes(Paths.get(fileName1)), Files.readAllBytes(Paths.get(fileName2)));
    }

    public static void assertFileNotEquals(String fileName1, String fileName2) throws Exception {
        if (fileEquals(fileName1, fileName2))
            throw new AssertionError();
    }

    public static boolean fileEquals(String fileName1, String fileName2) throws Exception {
        byte[] file1Bytes = Files.readAllBytes(Paths.get(fileName1));
        byte[] file2Bytes = Files.readAllBytes(Paths.get(fileName2));
        if (file1Bytes.length == file2Bytes.length) {
            for (int i = 0; i < file1Bytes.length; i++)
                if (file1Bytes[i] != file2Bytes[i])
                    return false;
            return true;
        }
        return false;
    }

    public static void gzip(String inputFile, String outputFile) throws IOException {
        try (GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(new File(outputFile)))) {
            try (FileInputStream in = new FileInputStream(inputFile)) {
                byte[] buffer = new byte[1 << 20];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        }
    }

    public static void gunzip(String inputFile, String outputFile) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(new File(inputFile)))) {
            try (FileOutputStream out = new FileOutputStream(new File(outputFile))) {
                byte[] buffer = new byte[1 << 20];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        }
    }

    public static long getFileSize(String fileName) {
        return new File(fileName).length();
    }

    public static long countLinesInFile(String fileName) throws Exception {
        Scanner scanner = new Scanner(new File(fileName));
        long count = 0;
        while (scanner.hasNextLine()) {
            scanner.nextLine();
            count++;
        }
        scanner.close();
        return count;
    }

    public static void seqToFastq(List<NSequenceWithQuality> sequences, String fileName) {
        try (SequenceWriter<SingleRead> writer = new SingleFastqWriter(fileName)) {
            long id = 0;
            for (NSequenceWithQuality seq : sequences)
                writer.write(new SingleReadImpl(id++, seq, ""));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<NSequenceWithQuality> fastqToSeq(String fileName) {
        List<NSequenceWithQuality> sequences;
        try (SingleFastqReader reader = new SingleFastqReader(fileName)) {
            sequences = streamPort(reader).map(SingleRead::getData).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sequences;
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    public static <T, R> Function<T, R> rethrow(ThrowingFunction<T, R, Exception> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, R> Function<T, R> orNull(ThrowingFunction<T, R, Exception> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                return null;
            }
        };
    }

    public static <T, R> R untilSuccess(T inputValue, ThrowingFunction<T, R, Exception> function) {
        while (true) {
            R returnValue;
            try {
                returnValue = function.apply(inputValue);
            } catch (Exception e) {
                continue;
            }
            return returnValue;
        }
    }

    public static void assertException(Class exceptionClass, Callable<Void> f) {
        boolean exceptionThrown = false;
        try {
            f.call();
        } catch (Exception e) {
            if (e.getClass().equals(exceptionClass))
                exceptionThrown = true;
            else
                throw new RuntimeException(e);
        }
        assertTrue(exceptionThrown);
    }

    public static void assertOutputContains(boolean stderr, String str, Callable<Void> f) throws Exception {
        PrintStream previousOut = stderr ? System.err : System.out;
        ByteArrayOutputStream capturedStream = new ByteArrayOutputStream();
        if (stderr)
            System.setErr(new PrintStream(capturedStream));
        else
            System.setOut(new PrintStream(capturedStream));

        f.call();
        assertTrue(capturedStream.toString().contains(str));

        if (stderr)
            System.setErr(previousOut);
        else
            System.setOut(previousOut);
    }

    public static void repeatAndExpectExceptionEveryTime(int iterations, Class exceptionClass, Callable<Void> f) {
        for (int i = 0; i < iterations; i++)
            assertException(exceptionClass, f);
    }

    public static class RandomBorders {
        public final int left;
        public final int right;

        public RandomBorders(int targetSize) {
            if (targetSize < 1)
                throw new IllegalArgumentException("Cannot create RandomBorders for targetSize " + targetSize);
            int r = rg.nextInt(12);
            int minRight;
            if (r < 10) {
                minRight = 0;
                left = -1;
            } else if (r == 10) {
                minRight = rg.nextInt(targetSize);
                left = minRight;
            } else {
                minRight = rg.nextInt(targetSize);
                left = invertCoordinate(targetSize - 1 - minRight);
            }
            r = rg.nextInt(12);
            if (r < 10)
                right = -1;
            else if (r == 10)
                right = minRight + rg.nextInt(targetSize - minRight);
            else
                right = invertCoordinate(targetSize - 1 - minRight - rg.nextInt(targetSize - minRight));
        }
    }

    public static class RandomCuts {
        public final int left;
        public final int right;

        public RandomCuts(int motifSize) {
            int leftCut = 0;
            int rightCut = 0;
            if (motifSize > 1)
                do {
                    leftCut = rg.nextInt(motifSize);
                    rightCut = rg.nextInt(motifSize);
                } while (leftCut + rightCut >= motifSize);
            this.left = leftCut;
            this.right = rightCut;
        }
    }
}
