/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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
import com.milaboratory.core.Range;
import com.milaboratory.core.alignment.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.pattern.*;
import com.milaboratory.test.TestUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.pattern.PatternUtils.invertCoordinate;
import static com.milaboratory.minnn.util.CommonTestUtils.RandomStringType.*;
import static org.junit.Assert.*;

public class CommonTestUtils {
    public static final Random rg = new Random();
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/";
    private static final String LN_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String QUERY_CHAR_STRING = " ATGCNatgcn()[]{}^$:+&|\\~0123456789-*";

    public static <T> Stream<T> streamPort(OutputPort<T> port) {
        return StreamSupport.stream(CUtils.it(port).spliterator(), false);
    }

    public static long countPortValues(OutputPort<MatchIntermediate> port) {
        return streamPort(port).count();
    }

    public static long countMatches(MatchingResult matchingResult, boolean fair) {
        return countPortValues(matchingResult.getMatches(fair));
    }

    public static NucleotideSequenceCaseSensitive makeRandomInsertions(NucleotideSequenceCaseSensitive seq,
                                                                       int number) {
        NucleotideSequenceCaseSensitive result = seq;
        int currentLength;
        int currentInsertPosition;
        for (int i = 0; i < number; i++) {
            currentLength = seq.size() + i;
            currentInsertPosition = rg.nextInt(currentLength);
            result = SequencesUtils.concatenate(result.getRange(0, currentInsertPosition),
                    TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET, 1, 1),
                    result.getRange(currentInsertPosition, currentLength));
        }
        return result;
    }

    public static NucleotideSequenceCaseSensitive makeRandomDeletions(NucleotideSequenceCaseSensitive seq,
                                                                      int number) {
        assertTrue(seq.size() > number);
        NucleotideSequenceCaseSensitive result = seq;
        int currentLength;
        int currentDeletePosition;
        for (int i = 0; i < number; i++) {
            currentLength = seq.size() - i;
            currentDeletePosition = rg.nextInt(currentLength);
            result = SequencesUtils.concatenate(result.getRange(0, currentDeletePosition),
                    result.getRange(currentDeletePosition + 1, currentLength));
        }
        return result;
    }

    public static NucleotideSequenceCaseSensitive makeRandomReplacements(NucleotideSequenceCaseSensitive seq,
                                                                         int number) {
        NucleotideSequenceCaseSensitive result = seq;
        int currentPosition;
        for (int i = 0; i < number; i++) {
            currentPosition = rg.nextInt(seq.size());
            result = SequencesUtils.concatenate(result.getRange(0, currentPosition),
                    TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET, 1, 1),
                    result.getRange(currentPosition + 1, seq.size()));
        }
        return result;
    }

    public static NucleotideSequenceCaseSensitive makeRandomErrors(NucleotideSequenceCaseSensitive seq, int number) {
        NucleotideSequenceCaseSensitive result = seq;
        for (int i = 0; i < number; i++) {
            switch (rg.nextInt(3)) {
                case 0:
                    result = makeRandomInsertions(result, 1);
                    break;
                case 1:
                    if (result.size() < 2) break;
                    result = makeRandomDeletions(result, 1);
                    break;
                case 2:
                    result = makeRandomReplacements(result, 1);
                    break;
            }
        }
        return result;
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

    public static PatternAligner getTestPatternAligner() {
        return getTestPatternAligner(0);
    }

    public static PatternAligner getTestPatternAligner(boolean zeroThreshold) {
        if (zeroThreshold) return getTestPatternAligner(0, 0, 0,
                -1);
        else return getTestPatternAligner();
    }

    public static PatternAligner getTestPatternAligner(int bitapMaxErrors) {
        return getTestPatternAligner(Long.MIN_VALUE, bitapMaxErrors, 0, -1);
    }

    public static PatternAligner getTestPatternAligner(long penaltyThreshold, int bitapMaxErrors, long notResultScore,
                                                       long singleOverlapPenalty) {
        return getTestPatternAligner(penaltyThreshold, bitapMaxErrors, notResultScore, singleOverlapPenalty, -1);
    }

    public static PatternAligner getTestPatternAligner(long penaltyThreshold, int bitapMaxErrors, long notResultScore,
                                                       long singleOverlapPenalty, int maxOverlap) {
        return getTestPatternAligner(penaltyThreshold, bitapMaxErrors, notResultScore, singleOverlapPenalty,
                maxOverlap, -1, getTestScoring());
    }

    public static PatternAligner getTestPatternAligner(long penaltyThreshold, int bitapMaxErrors, long notResultScore,
                                                       long singleOverlapPenalty, int maxOverlap, int fixedLeftBorder,
                                                       PatternAndTargetAlignmentScoring scoring) {
        return new PatternAligner() {
            @Override
            public Alignment<NucleotideSequenceCaseSensitive> align(NucleotideSequenceCaseSensitive pattern,
                    NSequenceWithQuality target, int rightMatchPosition) {
                if (fixedLeftBorder == -1) {
                    return PatternAndTargetAligner.alignLeftAdded(scoring, pattern, target, rightMatchPosition,
                            bitapMaxErrors);
                } else {
                    Range targetRange = new Range(fixedLeftBorder, rightMatchPosition + 1);
                    NSequenceWithQuality targetPart = new NSequenceWithQuality(
                            target.getSequence().getRange(targetRange), target.getQuality().getRange(targetRange));
                    Alignment<NucleotideSequenceCaseSensitive> partAlignment = PatternAndTargetAligner.alignGlobal(
                            scoring, pattern, targetPart);
                    return new Alignment<>(pattern, partAlignment.getAbsoluteMutations(),
                            partAlignment.getSequence1Range(), partAlignment.getSequence2Range().move(fixedLeftBorder),
                            partAlignment.getScore());
                }
            }

            @Override
            public long penaltyThreshold() {
                return penaltyThreshold;
            }

            @Override
            public long overlapPenalty(NSequenceWithQuality target, int overlapOffset, int overlapLength) {
                return singleOverlapPenalty * overlapLength;
            }

            @Override
            public long insertionPenalty(NSequenceWithQuality target, int insertionOffset, int insertionLength) {
                return singleOverlapPenalty * insertionLength;
            }

            @Override
            public int bitapMaxErrors() {
                return bitapMaxErrors;
            }

            @Override
            public long notResultScore() {
                return notResultScore;
            }

            @Override
            public int maxOverlap() {
                return maxOverlap;
            }

            @Override
            public PatternAligner overridePenaltyThreshold(long newThresholdValue) {
                return getTestPatternAligner(newThresholdValue, bitapMaxErrors, notResultScore, singleOverlapPenalty,
                        maxOverlap, fixedLeftBorder, scoring);
            }

            @Override
            public PatternAligner setLeftBorder(int leftBorder) {
                return getTestPatternAligner(penaltyThreshold, bitapMaxErrors, notResultScore, singleOverlapPenalty,
                        maxOverlap, leftBorder, scoring);
            }
        };
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
            String groupName = getRandomString(rg.nextInt(30) + 1, "R", LETTERS_AND_NUMBERS);
            if (groupEdgePositions.stream().anyMatch(g -> g.getGroupEdge().getGroupName().equals(groupName)))
                continue;
            int leftPosition = rg.nextInt(maxCoordinate);
            int rightPosition = rg.nextInt(maxCoordinate - leftPosition) + leftPosition + 1;
            groupEdgePositions.add(new GroupEdgePosition(new GroupEdge(groupName, true), leftPosition));
            groupEdgePositions.add(new GroupEdgePosition(new GroupEdge(groupName, false), rightPosition));
        }
        return groupEdgePositions;
    }

    public static PatternAligner getRandomPatternAligner() {
        return getTestPatternAligner(-rg.nextInt(100), rg.nextInt(4), -rg.nextInt(4), -rg.nextInt(3),
                -1, -1, getRandomScoring());
    }

    public static FuzzyMatchPattern getRandomFuzzyPattern(PatternAligner patternAligner, boolean withGroups) {
        int length = rg.nextInt(150) + 1;
        RandomBorders randomBorders = new RandomBorders(length);
        RandomCuts randomCuts = new RandomCuts(length);
        NucleotideSequenceCaseSensitive seq = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                length, length);
        return new FuzzyMatchPattern(patternAligner, seq, randomCuts.left, randomCuts.right,
                randomBorders.left, randomBorders.right,
                withGroups ? getRandomGroupsForFuzzyMatch(length) : new ArrayList<>());
    }

    public static RepeatPattern getRandomRepeatPattern(PatternAligner patternAligner, boolean withGroups) {
        int minRepeats = rg.nextInt(10) + 1;
        int maxRepeats = rg.nextInt(100) + minRepeats;
        RandomBorders randomBorders = new RandomBorders(maxRepeats);
        NucleotideSequenceCaseSensitive seq = TestUtil.randomSequence(NucleotideSequenceCaseSensitive.ALPHABET,
                1, 1);
        return new RepeatPattern(patternAligner, seq, minRepeats, maxRepeats, randomBorders.left, randomBorders.right,
                withGroups ? getRandomGroupsForFuzzyMatch(maxRepeats) : new ArrayList<>());
    }

    public static AnyPattern getRandomAnyPattern(PatternAligner patternAligner, boolean withGroups) {
        return new AnyPattern(patternAligner, withGroups ? getRandomGroupsForFuzzyMatch(1).stream()
                .map(GroupEdgePosition::getGroupEdge).collect(Collectors.toCollection(ArrayList::new))
                : new ArrayList<>());
    }

    public static SinglePattern getRandomBasicPattern() {
        return getRandomBasicPattern(false);
    }

    public static SinglePattern getRandomBasicPattern(PatternAligner patternAligner) {
        return getRandomBasicPattern(patternAligner, false);
    }

    public static SinglePattern getRandomBasicPattern(boolean withGroups) {
        return getRandomBasicPattern(getRandomPatternAligner(), withGroups);
    }

    public static SinglePattern getRandomBasicPattern(PatternAligner patternAligner, boolean withGroups) {
        switch (rg.nextInt(3)) {
            case 0:
                return getRandomFuzzyPattern(patternAligner, withGroups);
            case 1:
                return getRandomRepeatPattern(patternAligner, withGroups);
            default:
                return getRandomAnyPattern(patternAligner, withGroups);
        }
    }

    public static FullReadPattern getRandomSingleReadPattern(SinglePattern... patterns) {
        return getRandomSingleReadPattern(getRandomPatternAligner(), patterns);
    }

    public static FullReadPattern getRandomSingleReadPattern(PatternAligner patternAligner,
                                                             SinglePattern... singlePatterns) {
        return wrapWithFullReadPattern(patternAligner, getRandomRawSinglePattern(patternAligner, singlePatterns));
    }

    public static SinglePattern getRandomRawSinglePattern(SinglePattern... patterns) {
        return getRandomRawSinglePattern(getRandomPatternAligner(), patterns);
    }

    public static SinglePattern getRandomRawSinglePattern(PatternAligner patternAligner,
                                                          SinglePattern... singlePatterns) {
        SinglePattern[] patterns;
        if (singlePatterns.length == 0) {
            int numPatterns = rg.nextInt(5) + 1;
            patterns = new SinglePattern[numPatterns];
            for (int i = 0; i < numPatterns; i++)
                patterns[i] = getRandomBasicPattern(patternAligner);
        } else
            patterns = singlePatterns;
        boolean foundAnyPattern = Arrays.stream(patterns).anyMatch(p -> p instanceof AnyPattern);
        switch (patterns[0] instanceof AnyPattern ? 0 : rg.nextInt(foundAnyPattern ? 3 : 7)) {
            case 0:
                return patterns[0];
            case 1:
                return new FilterPattern(patternAligner, new ScoreFilter(-rg.nextInt(75)), patterns[0]);
            case 2:
                return new FilterPattern(patternAligner, new StickFilter(rg.nextBoolean(), rg.nextInt(30)),
                        patterns[0]);
            case 3:
                return new AndPattern(patternAligner, patterns);
            case 4:
                return new PlusPattern(patternAligner, patterns);
            case 5:
                return new SequencePattern(patternAligner, patterns);
            case 6:
            default:
                return new OrPattern(patternAligner, patterns);
        }
    }

    private static FullReadPattern wrapWithFullReadPattern(PatternAligner patternAligner,
                                                           SinglePattern singlePattern) {
        FullReadPattern fullReadPattern = new FullReadPattern(patternAligner, false, singlePattern);
        fullReadPattern.setTargetId((byte)1);
        return fullReadPattern;
    }

    public static MultipleReadsOperator getRandomMultiReadPattern(MultipleReadsOperator... patterns) {
        return getRandomMultiReadPattern(getRandomPatternAligner(), -1, patterns);
    }

    public static MultipleReadsOperator getRandomMultiReadPattern(int numPatterns) {
        return getRandomMultiReadPattern(getRandomPatternAligner(), numPatterns);
    }

    public static MultipleReadsOperator getRandomMultiReadPattern(PatternAligner patternAligner, int numPatterns,
                                                                  MultipleReadsOperator... patterns) {
        if (patterns.length == 0) {
            if (numPatterns == -1)
                numPatterns = rg.nextInt(5) + 1;
            SinglePattern[] basicPatterns = new SinglePattern[numPatterns];
            for (int i = 0; i < numPatterns; i++)
                basicPatterns[i] = getRandomBasicPattern(patternAligner);
            return createMultiPattern(patternAligner, basicPatterns);
        } else {
            switch (rg.nextInt(4)) {
                case 0:
                    return new AndOperator(patternAligner, patterns);
                case 1:
                    return new OrOperator(patternAligner, patterns);
                case 2:
                    return new NotOperator(patternAligner, patterns[0]);
                case 3:
                default:
                    return new MultipleReadsFilterPattern(patternAligner,
                            new ScoreFilter(-rg.nextInt(75)), patterns[0]);
            }
        }
    }

    public static MultipleReadsOperator[] singleToMultiPatterns(SinglePattern... singlePatterns) {
        return singleToMultiPatterns(getTestPatternAligner(), singlePatterns);
    }

    public static MultipleReadsOperator[] singleToMultiPatterns(PatternAligner patternAligner,
                                                                SinglePattern... singlePatterns) {
        return Arrays.stream(singlePatterns).map(sp -> createMultiPattern(patternAligner, sp))
                .toArray(MultipleReadsOperator[]::new);
    }

    public static MultiPattern createMultiPattern(PatternAligner patternAligner, SinglePattern... singlePatterns) {
        return createMultiPattern(patternAligner, true, singlePatterns);
    }

    public static MultiPattern createMultiPattern(PatternAligner patternAligner, boolean defaultGroupsOverride,
                                                  SinglePattern... singlePatterns) {
        return new MultiPattern(patternAligner, Arrays.stream(singlePatterns)
                .map(sp -> new FullReadPattern(patternAligner, defaultGroupsOverride, sp))
                .toArray(SinglePattern[]::new));
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
