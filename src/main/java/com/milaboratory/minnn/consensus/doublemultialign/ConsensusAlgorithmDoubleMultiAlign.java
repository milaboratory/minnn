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
package com.milaboratory.minnn.consensus.doublemultialign;

import com.milaboratory.core.alignment.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.consensus.*;
import gnu.trove.map.hash.TByteObjectHashMap;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.*;

import static com.milaboratory.core.alignment.BandedLinearAligner.alignLocalGlobal;
import static com.milaboratory.core.sequence.quality.QualityTrimmer.trim;
import static com.milaboratory.minnn.consensus.ConsensusStageForDebug.*;
import static com.milaboratory.minnn.consensus.OriginalReadStatus.*;
import static com.milaboratory.minnn.pattern.PatternUtils.*;
import static com.milaboratory.minnn.util.AlignmentTools.calculateAlignmentScore;

public class ConsensusAlgorithmDoubleMultiAlign extends ConsensusAlgorithm {
    private final int alignerWidth;
    private final LinearGapAlignmentScoring<NucleotideSequence> scoring;
    private final long goodQualityMismatchPenalty;
    private final byte goodQualityMismatchThreshold;
    private final long scoreThreshold;

    public ConsensusAlgorithmDoubleMultiAlign(
            Consumer<String> displayWarning, int numberOfTargets, int alignerWidth, int matchScore, int mismatchScore,
            int gapScore, long goodQualityMismatchPenalty, byte goodQualityMismatchThreshold, long scoreThreshold,
            float skippedFractionToRepeat, int maxConsensusesPerCluster, int readsMinGoodSeqLength,
            float readsAvgQualityThreshold, int readsTrimWindowSize, int minGoodSeqLength, float avgQualityThreshold,
            int trimWindowSize, boolean toSeparateGroups, PrintStream debugOutputStream, byte debugQualityThreshold,
            ConcurrentHashMap<Long, OriginalReadData> originalReadsData) {
        super(displayWarning, numberOfTargets, maxConsensusesPerCluster, skippedFractionToRepeat,
                readsMinGoodSeqLength, readsAvgQualityThreshold, readsTrimWindowSize, minGoodSeqLength,
                avgQualityThreshold, trimWindowSize, toSeparateGroups, debugOutputStream, debugQualityThreshold,
                originalReadsData);
        this.alignerWidth = alignerWidth;
        this.scoring = new LinearGapAlignmentScoring<>(NucleotideSequence.ALPHABET, matchScore, mismatchScore,
                gapScore);
        this.goodQualityMismatchPenalty = goodQualityMismatchPenalty;
        this.goodQualityMismatchThreshold = goodQualityMismatchThreshold;
        this.scoreThreshold = scoreThreshold;
    }

    @Override
    public CalculatedConsensuses process(Cluster cluster) {
        defaultGroupsOverride.set(cluster.data.get(0).isDefaultGroupsOverride());
        CalculatedConsensuses calculatedConsensuses = new CalculatedConsensuses(cluster.orderedPortIndex);
        List<DataFromParsedRead> data = trimBadQualityTails(cluster.data);
        if (data.size() == 0) {
            calculatedConsensuses.consensuses.add(new Consensus((debugOutputStream == null) ? null
                    : new ConsensusDebugData(numberOfTargets, debugQualityThreshold, STAGE1, true),
                    numberOfTargets, false));
            if (cluster.data.size() > 1)
                displayWarning.accept("WARNING: all reads discarded after quality trimming from cluster of "
                        + cluster.data.size() + " reads! Barcode values: "
                        + formatBarcodeValues(cluster.data.get(0).getBarcodes()));
        }
        long numValidConsensuses = 0;

        while (data.size() > 0) {
            // stage 1: align to best quality
            long bestSumQuality = Long.MIN_VALUE;
            int bestDataIndex = -1;
            for (int i = 0; i < data.size(); i++) {
                long sumQuality = data.get(i).getSequences().valueCollection().stream()
                        .mapToLong(SequenceWithAttributes::calculateQualityOfSequence).sum();
                if (sumQuality > bestSumQuality) {
                    bestSumQuality = sumQuality;
                    bestDataIndex = i;
                }
            }
            DataFromParsedRead bestData = data.get(bestDataIndex);
            HashSet<Integer> filteredOutReads = new HashSet<>();
            ArrayList<AlignedSubsequences> subsequencesList = getAlignedSubsequencesList(data, filteredOutReads,
                    bestData.getSequences(), bestDataIndex);
            Consensus stage1Consensus = generateConsensus(subsequencesList, bestData.getSequences(),
                    bestData.getBarcodes(), false);
            if (debugOutputStream != null)
                calculatedConsensuses.consensuses.add(stage1Consensus);

            if (!stage1Consensus.isConsensus)
                displayWarning.accept("WARNING: consensus assembled from " + (data.size() - filteredOutReads.size())
                        + " reads discarded on stage 1 after quality trimming! Barcode values: "
                        + formatBarcodeValues(bestData.getBarcodes()) + ", best read id: "
                        + bestData.getOriginalReadId());
            else {
                // stage 2: align to consensus from stage 1
                subsequencesList = getAlignedSubsequencesList(data, filteredOutReads, stage1Consensus.sequences,
                        -1);
                if (subsequencesList.size() > 0) {
                    Consensus stage2Consensus = generateConsensus(subsequencesList,
                            Objects.requireNonNull(stage1Consensus.sequences),
                            Objects.requireNonNull(stage1Consensus.barcodes), true);
                    if (!stage2Consensus.isConsensus) {
                        displayWarning.accept("WARNING: consensus assembled from " + (data.size()
                                - filteredOutReads.size()) + " reads discarded on stage 2 after "
                                + "quality trimming! Barcode values: " + formatBarcodeValues(bestData.getBarcodes())
                                + ", best read id: " + bestData.getOriginalReadId());
                        if (debugOutputStream != null)
                            calculatedConsensuses.consensuses.add(stage2Consensus);
                    } else {
                        if (toSeparateGroups)
                            for (int i = 0; i < data.size(); i++) {
                                if (!filteredOutReads.contains(i))
                                    stage2Consensus.savedOriginalSequences.add(
                                            (DataFromParsedReadWithAllGroups)(data.get(i)));
                            }
                        calculatedConsensuses.consensuses.add(stage2Consensus);
                        numValidConsensuses++;
                    }
                }
            }

            if ((filteredOutReads.size() < data.size())
                    && (float)filteredOutReads.size() / cluster.data.size() >= skippedFractionToRepeat) {
                if (numValidConsensuses < maxConsensusesPerCluster) {
                    ArrayList<DataFromParsedRead> remainingData = new ArrayList<>();
                    for (int i = 0; i < data.size(); i++)
                        if (filteredOutReads.contains(i))
                            remainingData.add(data.get(i));
                    data = remainingData;
                } else {
                    displayWarning.accept("WARNING: max consensuses per cluster exceeded; not processed "
                            + filteredOutReads.size() + " reads from cluster of " + cluster.data.size()
                            + " reads! Barcode values: " + formatBarcodeValues(bestData.getBarcodes()));
                    data = new ArrayList<>();
                }
            } else
                data = new ArrayList<>();
        }

        return calculatedConsensuses;
    }

    /**
     * Align sequences and generate list of AlignedSubsequences objects that is needed for generateConsensus().
     *
     * @param data              data from cluster of parsed reads with same barcodes
     * @param filteredOutReads  mutable set of filtered out reads: this function will add to this set
     *                          indexes of all reads that didn't fit score threshold
     * @param bestSequences     best sequences map with targetIds as keys
     * @param bestSeqIndex      index of best sequences in cluster; or -1 if they are not from cluster
     * @return                  list of aligned subsequences for generateConsensus() function
     */
    private ArrayList<AlignedSubsequences> getAlignedSubsequencesList(
            List<DataFromParsedRead> data, HashSet<Integer> filteredOutReads,
            TByteObjectHashMap<SequenceWithAttributes> bestSequences, int bestSeqIndex) {
        ArrayList<AlignedSubsequences> subsequencesList = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            if (i != bestSeqIndex) {
                if (!filteredOutReads.contains(i) && (data.get(i) != null)) {
                    DataFromParsedRead currentData = data.get(i);
                    long sumScore = 0;
                    ArrayList<Alignment<NucleotideSequence>> alignments = new ArrayList<>();
                    long[] alignmentScores = new long[numberOfTargets];
                    // targetIndex is targetId - 1
                    for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                        NSequenceWithQuality seq1 = bestSequences.get((byte)(targetIndex + 1))
                                .toNSequenceWithQuality();
                        NSequenceWithQuality seq2 = currentData.getSequences().get((byte)(targetIndex + 1))
                                .toNSequenceWithQuality();
                        Alignment<NucleotideSequence> alignment = alignLocalGlobal(scoring, seq1, seq2,
                                alignerWidth);
                        alignments.add(alignment);
                        long alignmentScore = calculateAlignmentScore(goodQualityMismatchPenalty,
                                goodQualityMismatchThreshold, alignment, seq1, seq2);
                        alignmentScores[targetIndex] = alignmentScore;
                        sumScore += alignmentScore;
                    }
                    if (sumScore < scoreThreshold)
                        filteredOutReads.add(i);
                    else {
                        AlignedSubsequences currentSubsequences = new AlignedSubsequences(
                                IntStream.rangeClosed(1, numberOfTargets).mapToObj(targetId ->
                                        bestSequences.get((byte)targetId)).toArray(SequenceWithAttributes[]::new),
                                currentData.getOriginalReadId(), alignmentScores);
                        for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                            SequenceWithAttributes currentSequence = currentData.getSequences()
                                    .get((byte)(targetIndex + 1));
                            SequenceWithAttributes alignedBestSequence = bestSequences
                                    .get((byte)(targetIndex + 1));
                            int previousSeqPosition = -1;
                            for (int position = 0; position < alignedBestSequence.size(); position++) {
                                Alignment<NucleotideSequence> alignment = alignments.get(targetIndex);
                                int seqPosition = alignment.convertToSeq2Position(position);
                                if (previousSeqPosition < 0) {
                                    if (seqPosition < 0)
                                        currentSubsequences.set(targetIndex, position,
                                                new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                                        currentSequence.getOriginalReadId()));
                                    else
                                        currentSubsequences.set(targetIndex, position,
                                                currentSequence.getSubSequence(0, seqPosition + 1));
                                    previousSeqPosition = seqPosition;
                                } else {
                                    if (seqPosition < 0)
                                        currentSubsequences.set(targetIndex, position,
                                                new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                                        currentSequence.getOriginalReadId()));
                                    else {
                                        if (previousSeqPosition == currentSequence.size() - 1)
                                            currentSubsequences.set(targetIndex, position,
                                                    new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                                            currentSequence.getOriginalReadId()));
                                        else
                                            currentSubsequences.set(targetIndex, position,
                                                    currentSequence.getSubSequence(previousSeqPosition + 1,
                                                            Math.min(currentSequence.size(), seqPosition + 1)));
                                        previousSeqPosition = seqPosition;
                                    }
                                }
                            }
                        }
                        subsequencesList.add(currentSubsequences);
                    }
                }
            } else {
                long[] alignmentScores = IntStream.rangeClosed(1, numberOfTargets).mapToLong(targetId ->
                        bestSequences.get((byte)targetId).size() * scoring.getMaximalMatchScore()).toArray();
                AlignedSubsequences currentSubsequences = new AlignedSubsequences(
                        IntStream.rangeClosed(1, numberOfTargets).mapToObj(targetId ->
                                bestSequences.get((byte)targetId)).toArray(SequenceWithAttributes[]::new),
                        data.get(i).getOriginalReadId(), alignmentScores);
                for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                    SequenceWithAttributes currentSequence = bestSequences.get((byte)(targetIndex + 1));
                    for (int position = 0; position < currentSequence.size(); position++)
                        currentSubsequences.set(targetIndex, position, currentSequence.letterAt(position));
                }
                subsequencesList.add(currentSubsequences);
            }
        }

        return subsequencesList;
    }

    /**
     * Generate consensus from prepared aligned subsequences list.
     *
     * @param subsequencesList  1 element of this list corresponding to 1 read; AlignedSubsequences structure
     *                          contains sequences from cluster splitted by coordinates that came from alignment
     *                          of sequences from this array to sequences from best array
     * @param bestSequences     best sequences map with targetIds as keys
     * @param barcodes          barcodes from best sequences
     * @param stage2            true if this is 2nd stage (best sequences are consensuses from stage1),
     *                          or false if this is 1nd stage
     * @return                  consensus: array of sequences (1 sequence for 1 target) and consensus score
     */
    private Consensus generateConsensus(
            ArrayList<AlignedSubsequences> subsequencesList, TByteObjectHashMap<SequenceWithAttributes> bestSequences,
            List<Barcode> barcodes, boolean stage2) {
        ConsensusDebugData debugData = (debugOutputStream == null) ? null
                : new ConsensusDebugData(numberOfTargets, debugQualityThreshold, stage2 ? STAGE2 : STAGE1,
                true);
        int consensusReadsNum = subsequencesList.size();
        long bestSeqReadId = bestSequences.get((byte)1).getOriginalReadId();
        TByteObjectHashMap<SequenceWithAttributes> sequences = new TByteObjectHashMap<>();
        List<LettersWithPositions> lettersList = IntStream.range(0, consensusReadsNum)
                .mapToObj(i -> new LettersWithPositions()).collect(Collectors.toList());
        for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
            ArrayList<ArrayList<SequenceWithAttributes>> debugDataForThisTarget = (debugData == null) ? null
                    : debugData.data.get(targetIndex);
            ArrayList<NSequenceWithQuality> consensusDebugDataForThisTarget = (debugData == null) ? null
                    : debugData.consensusData.get(targetIndex);
            ArrayList<Long> alignmentScoresDebugForThisTarget = (debugData == null) ? null
                    : debugData.alignmentScores.get(targetIndex);
            List<ArrayList<SequenceWithAttributes>> lettersMatrixList = IntStream.range(0, consensusReadsNum)
                    .mapToObj(i -> new ArrayList<SequenceWithAttributes>()).collect(Collectors.toList());
            for (int position = 0; position < bestSequences.get((byte)(targetIndex + 1)).size(); position++) {
                ArrayList<SequenceWithAttributes> currentPositionSequences = new ArrayList<>();
                int bestQualityIndex = -1;
                byte bestQuality = -1;
                for (int i = 0; i < consensusReadsNum; i++) {
                    AlignedSubsequences currentSubsequences = subsequencesList.get(i);
                    SequenceWithAttributes currentSequence = currentSubsequences.get(targetIndex, position);
                    currentPositionSequences.add(currentSequence);
                    if (!currentSequence.isNull()) {
                        byte currentQuality = currentSequence.calculateMinQuality();
                        if (currentQuality > bestQuality) {
                            bestQuality = currentQuality;
                            bestQualityIndex = i;
                        }
                    }
                }

                LettersMatrix lettersMatrix;
                if (bestQualityIndex == -1) {
                    // in this case lettersMatrix is empty and getRowLength() will return 0
                    lettersMatrix = new LettersMatrix();
                } else {
                    lettersMatrix = new LettersMatrix(currentPositionSequences.get(bestQualityIndex),
                            bestQualityIndex);
                    for (int i = 0; i < currentPositionSequences.size(); i++) {
                        if (i != bestQualityIndex) {
                            SequenceWithAttributes currentSeq = currentPositionSequences.get(i);
                            if (!currentSeq.isNull())
                                lettersMatrix.add(currentSeq, alignLocalGlobal(scoring,
                                        currentPositionSequences.get(bestQualityIndex).toNSequenceWithQuality(),
                                        currentPositionSequences.get(i).toNSequenceWithQuality(), alignerWidth));
                            else
                                lettersMatrix.addNull(currentSeq.getOriginalReadId());
                        }
                    }
                }
                for (int sequenceIndex = 0; sequenceIndex < consensusReadsNum; sequenceIndex++) {
                    ArrayList<SequenceWithAttributes> currentLettersRow = lettersMatrixList.get(sequenceIndex);
                    for (int letterIndex = 0; letterIndex < lettersMatrix.getRowLength(); letterIndex++)
                        addLetterToRow(currentLettersRow,
                                lettersMatrix.getLetterByCoordinate(sequenceIndex, letterIndex));
                }
            }

            // moving letters from lists to LettersWithPositions objects
            for (int sequenceIndex = 0; sequenceIndex < consensusReadsNum; sequenceIndex++) {
                ArrayList<SequenceWithAttributes> currentLettersRow = lettersMatrixList.get(sequenceIndex);
                LettersWithPositions currentLettersWithPositions = lettersList.get(sequenceIndex);
                currentLettersWithPositions.set(targetIndex, currentLettersRow);
                if (debugData != null) {
                    debugDataForThisTarget.add(currentLettersRow);
                    alignmentScoresDebugForThisTarget.add(subsequencesList.get(sequenceIndex)
                            .alignmentScores[targetIndex]);
                }
            }

            // choosing letters for consensus and calculating quality
            int fullRowLength = lettersMatrixList.get(0).size();
            ArrayList<NSequenceWithQuality> consensusLetters = getLettersWithQuality(lettersList, fullRowLength,
                    targetIndex);

            // consensus sequence assembling and quality trimming
            OriginalReadStatus discardedStatus = stage2 ? CONSENSUS_DISCARDED_TRIM_STAGE2
                    : CONSENSUS_DISCARDED_TRIM_STAGE1;
            if (consensusLetters.size() == 0) {
                storeOriginalReadsData(subsequencesList, discardedStatus, null, stage2);
                return new Consensus(debugData, numberOfTargets, stage2);
            }
            NSequenceWithQualityBuilder builder = new NSequenceWithQualityBuilder();
            for (NSequenceWithQuality consensusLetter : consensusLetters) {
                if (consensusLetter != NSequenceWithQuality.EMPTY)
                    builder.append(consensusLetter);
                if (debugData != null)
                    consensusDebugDataForThisTarget.add(consensusLetter);
            }
            NSequenceWithQuality consensusRawSequence = builder.createAndDestroy();
            SequenceWithAttributes consensusSequence = new SequenceWithAttributes(
                    consensusRawSequence.getSequence(), consensusRawSequence.getQuality(), bestSeqReadId);

            int trimResultLeft = trim(consensusSequence.getQual(), 0, consensusSequence.size(),
                    1, true, avgQualityThreshold, trimWindowSize);
            if (trimResultLeft < -1) {
                storeOriginalReadsData(subsequencesList, discardedStatus, null, stage2);
                return new Consensus(debugData, numberOfTargets, stage2);
            }
            int trimResultRight = trim(consensusSequence.getQual(), 0, consensusSequence.size(),
                    -1, true, avgQualityThreshold, trimWindowSize);
            if (trimResultRight < 0)
                throw new IllegalStateException("Unexpected negative trimming result");
            else if (trimResultRight - trimResultLeft - 1 < minGoodSeqLength) {
                storeOriginalReadsData(subsequencesList, discardedStatus, null, stage2);
                return new Consensus(debugData, numberOfTargets, stage2);
            }
            consensusSequence = consensusSequence.getSubSequence(trimResultLeft + 1, trimResultRight);
            sequences.put((byte)(targetIndex + 1), consensusSequence);
        }

        Consensus consensus = new Consensus(sequences, barcodes, consensusReadsNum, debugData,
                numberOfTargets, stage2, consensusCurrentTempId.getAndIncrement(), defaultGroupsOverride.get());
        storeOriginalReadsData(subsequencesList, USED_IN_CONSENSUS, consensus, stage2);
        return consensus;
    }

    private void storeOriginalReadsData(ArrayList<AlignedSubsequences> subsequencesList,
                                        OriginalReadStatus status, Consensus consensus, boolean stage2) {
        if (originalReadsData != null)
            subsequencesList.forEach(alignedSubsequences -> {
                OriginalReadData originalReadData = originalReadsData.get(alignedSubsequences.originalReadId);
                originalReadData.status = status;
                originalReadData.consensus = consensus;
                originalReadData.alignmentScores.set(stage2 ? 1 : 0, alignedSubsequences.alignmentScores);
            });
    }

    /**
     * Choose letters for consensus and calculate their quality.
     *
     * @param lettersList       each element of this list is LettersWithPositions structure that allows to get
     *                          letter by targetIndex and position; one element of lettersList is for one original
     *                          (possibly multi-target) base read for this consensus
     * @param fullRowLength     row length of aligned sequences matrix for current targetIndex
     * @param targetIndex       current targetIndex
     * @return                  list of calculated consensus letters with calculated qualities
     */
    private ArrayList<NSequenceWithQuality> getLettersWithQuality(
            List<LettersWithPositions> lettersList, int fullRowLength, int targetIndex) {
        ArrayList<NSequenceWithQuality> consensusLetters = new ArrayList<>();

        for (int position = 0; position < fullRowLength; position++) {
            ArrayList<SequenceWithAttributes> baseLetters = new ArrayList<>();
            // loop by source reads for this consensus
            for (LettersWithPositions currentLettersWithPositions : lettersList) {
                SequenceWithAttributes currentLetter = currentLettersWithPositions.get(targetIndex, position);
                if (!currentLetter.isNull())
                    baseLetters.add(currentLetter);
            }

            if (baseLetters.size() > 0)
                consensusLetters.add(calculateConsensusLetter(baseLetters));
            else
                consensusLetters.add(NSequenceWithQuality.EMPTY);
        }

        return consensusLetters;
    }

    /**
     * Add new letter to the end of letters row with properly handling of trailing nulls: replace them with
     * deletions because there must be no nulls in the middle of sequence.
     *
     * @param currentLettersRow     current row of letters that will be modified
     * @param newLetter             new letter (can be NULL_SEQ or deletion) that is inserted to the end of row
     */
    private void addLetterToRow(ArrayList<SequenceWithAttributes> currentLettersRow,
                                SequenceWithAttributes newLetter) {
        if (!newLetter.isNull()) {
            // letters from the same row must have the same read id
            long currentReadId = newLetter.getOriginalReadId();
            int firstNullPosition = -1;
            for (int currentPosition = currentLettersRow.size() - 1; currentPosition >= 0; currentPosition--) {
                if (currentLettersRow.get(currentPosition).isNull())
                    firstNullPosition = currentPosition;
                else
                    break;
            }
            if (firstNullPosition > 0)
                for (int currentPosition = firstNullPosition; currentPosition < currentLettersRow.size();
                     currentPosition++)
                    currentLettersRow.set(currentPosition, new SequenceWithAttributes(SpecialSequences.EMPTY_SEQ,
                            currentReadId));
        }
        currentLettersRow.add(newLetter);
    }

    private class AlignedSubsequences {
        private final int[] indexes = new int[numberOfTargets];
        private final SequenceWithAttributes[] sequences;
        private final long originalReadId;
        private final long[] alignmentScores;

        AlignedSubsequences(SequenceWithAttributes[] bestSequences, long originalReadId, long[] alignmentScores) {
            int currentIndex = 0;
            for (int i = 0; i < numberOfTargets; i++) {
                indexes[i] = currentIndex;
                currentIndex += bestSequences[i].size();
            }
            sequences = new SequenceWithAttributes[currentIndex];
            this.originalReadId = originalReadId;
            if (alignmentScores.length != numberOfTargets)
                throw new IllegalArgumentException("alignmentScores array: " + Arrays.toString(alignmentScores)
                        + ", expected length: " + numberOfTargets);
            this.alignmentScores = alignmentScores;
        }

        void set(int targetIndex, int position, SequenceWithAttributes value) {
            sequences[index(targetIndex, position)] = value;
        }

        SequenceWithAttributes get(int targetIndex, int position) {
            return sequences[index(targetIndex, position)];
        }

        private int index(int targetIndex, int position) {
            return indexes[targetIndex] + position;
        }
    }

    private static class LettersWithPositions {
        private HashMap<Integer, ArrayList<SequenceWithAttributes>> targetSequences = new HashMap<>();

        void set(int targetIndex, ArrayList<SequenceWithAttributes> values) {
            for (SequenceWithAttributes value : values)
                if (!value.isNull() && !value.isEmpty() && (value.size() != 1))
                    throw new IllegalArgumentException("Trying to write sequence " + value
                            + " to LettersWithPositions");
            if (targetSequences.containsKey(targetIndex))
                throw new IllegalStateException("Trying to write key " + targetIndex + " to targetSequences "
                        + "when it already exists: targetSequences=" + targetSequences);
            targetSequences.put(targetIndex, values);
        }

        SequenceWithAttributes get(int targetIndex, int position) {
            return targetSequences.get(targetIndex).get(position);
        }
    }

    private static class LettersMatrix {
        // column numbers in the matrix corresponding to base sequence letters; last value is row length
        private final int[] baseLettersCoordinates;
        private final int baseSequenceRealIndex;
        private final ArrayList<ArrayList<Integer>> positionsCache = new ArrayList<>();
        private final ArrayList<SequenceWithAttributes> sequences = new ArrayList<>();
        private final boolean nullBaseSequence;

        LettersMatrix() {
            nullBaseSequence = true;
            baseLettersCoordinates = null;
            this.baseSequenceRealIndex = 0;
        }

        LettersMatrix(SequenceWithAttributes baseSequence, int baseSequenceRealIndex) {
            if (baseSequence.isNull())
                throw new IllegalArgumentException("Base sequence is null in LetterMatrix constructor; "
                        + "baseSequence = " + baseSequence + ", baseSequenceRealIndex = " + baseSequenceRealIndex);
            nullBaseSequence = false;
            baseLettersCoordinates = IntStream.rangeClosed(0, baseSequence.size()).toArray();
            sequences.add(baseSequence);
            this.baseSequenceRealIndex = baseSequenceRealIndex;
        }

        int getRowLength() {
            return nullBaseSequence ? 0 : baseLettersCoordinates[baseLettersCoordinates.length - 1];
        }

        void add(SequenceWithAttributes sequence, Alignment<NucleotideSequence> alignment) {
            if (nullBaseSequence)
                throw new IllegalStateException("add(" + sequence + ", " + alignment + " called for LettersMatrix "
                        + "with null base sequence!");
            int stage = 0;  // 0 - before base start, 1 - inside alignment range, 2 - after base end
            int leftTailLength = 0;
            int rightTailLength = 0;
            int currentPartLength = 1;
            sequences.add(sequence);
            ArrayList<Integer> currentPositions = new ArrayList<>();
            SequenceWithAttributes baseSequence = sequences.get(0);
            for (int i = 0; i < sequence.size(); i++) {
                int baseSequencePosition = alignment.convertToSeq1Position(i);
                if (baseSequencePosition == -1) {
                    if (stage == 0) {
                        currentPositions.add(-1);
                        leftTailLength++;
                    } else {
                        currentPositions.add(baseSequence.size());
                        stage = 2;
                        rightTailLength++;
                    }
                } else {
                    if (stage == 2)
                        throw new IllegalArgumentException("3 or more contiguous parts of seq2 are out of range "
                                + "in seq1; seq1: " + baseSequence + ", seq2: " + sequence + ", alignment: "
                                + alignment);
                    else if (stage == 0) {
                        currentPositions.add((baseSequencePosition >= 0) ? baseSequencePosition
                                : invertCoordinate(baseSequencePosition));
                        stage = 1;
                        extend(0, leftTailLength);
                    } else {
                        int currentCoordinate = (baseSequencePosition >= 0) ? baseSequencePosition
                                : invertCoordinate(baseSequencePosition);
                        int previousCoordinate = currentPositions.get(i - 1);
                        currentPositions.add(currentCoordinate);
                        if (currentCoordinate == previousCoordinate)
                            currentPartLength++;
                        else {
                            if (currentPartLength > 1)
                                extend(currentCoordinate, currentPartLength);
                            currentPartLength = Math.max(1, currentCoordinate - previousCoordinate);
                        }
                    }
                }
            }
            extend(baseLettersCoordinates.length - 1, rightTailLength);
            positionsCache.add(currentPositions);
        }

        void addNull(long originalReadId) {
            sequences.add(new SequenceWithAttributes(SpecialSequences.NULL_SEQ, originalReadId));
            positionsCache.add(new ArrayList<>());
        }

        /**
         * Extend matrix to fit longer subsequence into it.
         *
         * @param arrayIndex 0 means that we put subsequence before 1st base letter,
         *                   (baseLettersCoordinates.length - 1) means that we put it after last letter
         * @param newLength length of subsequence that we put to the gap specified by arrayIndex
         */
        private void extend(int arrayIndex, int newLength) {
            int currentLength = (arrayIndex == 0) ? baseLettersCoordinates[0]
                    : baseLettersCoordinates[arrayIndex] - baseLettersCoordinates[arrayIndex - 1] - 1;
            if (newLength > currentLength) {
                int diff = newLength - currentLength;
                for (int i = arrayIndex; i < baseLettersCoordinates.length; i++)
                    baseLettersCoordinates[i] += diff;
            }
        }

        SequenceWithAttributes getLetterByCoordinate(int sequenceRealIndex, int coordinate) {
            if (nullBaseSequence)
                throw new IllegalStateException("getLetterByCoordinate(" + sequenceRealIndex + ", " + coordinate
                        + " called for LettersMatrix with null base sequence!");
            if (sequenceRealIndex == baseSequenceRealIndex) {
                for (int i = 0; i < baseLettersCoordinates.length - 1; i++) {
                    int currentCoordinate = baseLettersCoordinates[i];
                    if (currentCoordinate == coordinate)
                        return sequences.get(0).letterAt(i);
                    else if (currentCoordinate > coordinate)
                        return new SequenceWithAttributes(SpecialSequences.EMPTY_SEQ,
                                sequences.get(0).getOriginalReadId());
                }
                return new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                        sequences.get(0).getOriginalReadId());
            } else {
                int sequenceIndex = (sequenceRealIndex > baseSequenceRealIndex) ? sequenceRealIndex
                        : sequenceRealIndex + 1;
                SequenceWithAttributes sequence = sequences.get(sequenceIndex);
                if (sequence.isNull())
                    return sequence;
                    /* get positions in current sequence relative to base sequence;
                       sequenceIndex - 1 because there is no base sequence as 1st element */
                ArrayList<Integer> positions = positionsCache.get(sequenceIndex - 1);
                int basePosition = -1;
                int currentBasePosition = -1;
                int currentPartLength = 1;
                int seqPosition;
                for (seqPosition = 0; seqPosition < sequence.size(); seqPosition++) {
                    currentBasePosition = positions.get(seqPosition);
                    if (currentBasePosition > -1) {
                        int currentBaseCoordinate = baseLettersCoordinates[currentBasePosition];
                        if (currentBaseCoordinate == coordinate)
                            return sequence.letterAt(seqPosition);
                        else if (currentBaseCoordinate > coordinate) {
                            if (seqPosition == 0)
                                return new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                        sequence.getOriginalReadId());
                            break;
                        } else if (currentBasePosition == basePosition)
                            currentPartLength++;
                        else
                            currentPartLength = 1;
                    }
                    basePosition = currentBasePosition;
                }
                if (currentBasePosition == -1)
                    throw new IllegalStateException("LettersMatrix error in sequence: " + sequence
                            + "; sequenceIndex: " + sequenceIndex + ", coordinate: " + coordinate);
                if (basePosition == -1) {
                    int seqStartCoordinate = baseLettersCoordinates[0] - seqPosition;
                    if (coordinate >= baseLettersCoordinates[0]) {
                        // there are deletions on base sequence start positions, and we pick one of them
                        return new SequenceWithAttributes(SpecialSequences.EMPTY_SEQ,
                                sequence.getOriginalReadId());
                    } else if (coordinate < seqStartCoordinate)
                        return new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                sequence.getOriginalReadId());
                    else
                        return sequence.letterAt(coordinate - seqStartCoordinate);
                } else {
                    int currentPartStart = seqPosition - currentPartLength;
                    int wantedSeqPosition = currentPartStart + coordinate - baseLettersCoordinates[basePosition];
                    if (wantedSeqPosition >= seqPosition) {
                            /* if nucleotide not found and this is not last position in sequence, this is a deletion,
                               otherwise we are on the right from all sequence and return NULL_SEQ */
                        if (seqPosition < sequence.size())
                            return new SequenceWithAttributes(SpecialSequences.EMPTY_SEQ,
                                    sequence.getOriginalReadId());
                        else
                            return new SequenceWithAttributes(SpecialSequences.NULL_SEQ,
                                    sequence.getOriginalReadId());
                    } else
                        return sequence.letterAt(wantedSeqPosition);
                }
            }
        }
    }
}
