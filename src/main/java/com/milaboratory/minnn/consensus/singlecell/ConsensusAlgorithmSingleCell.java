/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
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
package com.milaboratory.minnn.consensus.singlecell;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.minnn.consensus.*;
import com.milaboratory.minnn.consensus.trimmer.SequenceWithQualityAndCoverage;
import com.milaboratory.minnn.consensus.trimmer.ConsensusBuilder;
import gnu.trove.map.hash.TByteObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.milaboratory.minnn.consensus.ConsensusStageForDebug.*;
import static com.milaboratory.minnn.consensus.OriginalReadStatus.*;

public class ConsensusAlgorithmSingleCell extends ConsensusAlgorithm {
    private final int kmerLength;
    private final int kmerMaxOffset;
    private final int kmerMatchMaxErrors;

    public ConsensusAlgorithmSingleCell(
            Consumer<String> displayWarning, int numberOfTargets, int maxConsensusesPerCluster,
            float skippedFractionToRepeat, int readsMinGoodSeqLength, float readsAvgQualityThreshold,
            int readsTrimWindowSize, int minGoodSeqLength, float lowCoverageThreshold, float avgQualityThreshold,
            float avgQualityThresholdForLowCoverage, int trimWindowSize, boolean toSeparateGroups,
            PrintStream debugOutputStream, byte debugQualityThreshold,
            ConcurrentHashMap<Long, OriginalReadData> originalReadsData, int kmerLength, int kmerMaxOffset,
            int kmerMatchMaxErrors) {
        super(displayWarning, numberOfTargets, maxConsensusesPerCluster, skippedFractionToRepeat,
                Math.max(readsMinGoodSeqLength, kmerLength), readsAvgQualityThreshold, readsTrimWindowSize,
                minGoodSeqLength, lowCoverageThreshold, avgQualityThreshold, avgQualityThresholdForLowCoverage,
                trimWindowSize, toSeparateGroups, debugOutputStream, debugQualityThreshold, originalReadsData);
        this.kmerLength = kmerLength;
        this.kmerMaxOffset = kmerMaxOffset;
        this.kmerMatchMaxErrors = kmerMatchMaxErrors;
    }

    @Override
    public CalculatedConsensuses process(Cluster cluster) {
        defaultGroupsOverride.set(cluster.data.get(0).isDefaultGroupsOverride());
        CalculatedConsensuses calculatedConsensuses = new CalculatedConsensuses(cluster.orderedPortIndex);
        List<DataFromParsedRead> remainingData = trimBadQualityTails(cluster.data);
        int clusterSize = cluster.data.size();
        if (remainingData.size() == 0) {
            if (debugOutputStream != null)
                calculatedConsensuses.consensuses.add(new Consensus(new ConsensusDebugData(numberOfTargets,
                        debugQualityThreshold, NO_STAGE, false), numberOfTargets, true));
            if (clusterSize > 1)
                displayWarning.accept("WARNING: all reads discarded after quality trimming from cluster of "
                        + clusterSize + " reads! Barcode values: "
                        + formatBarcodeValues(cluster.data.get(0).getBarcodes()));
        }
        int numValidConsensuses = 0;

        OffsetSearchResults offsetSearchResults = null;
        boolean kmersNotFound = false;
        while ((remainingData.size() > 0) && !kmersNotFound
                && ((float)remainingData.size() / clusterSize >= skippedFractionToRepeat)
                && (numValidConsensuses < maxConsensusesPerCluster)) {
            offsetSearchResults = searchOffsets(remainingData);
            if (offsetSearchResults == null)
                kmersNotFound = true;
            else {
                Consensus consensus = calculateConsensus(offsetSearchResults);
                if (!consensus.isConsensus) {
                    displayWarning.accept("WARNING: consensus assembled from " + offsetSearchResults.usedReads.size()
                            + " reads discarded after quality trimming! Barcode values: "
                            + formatBarcodeValues(offsetSearchResults.barcodes) + ", original read ids: "
                            + Arrays.toString(offsetSearchResults.getUsedReadsIds()));
                    if (debugOutputStream != null)
                        calculatedConsensuses.consensuses.add(consensus);
                } else {
                    if (toSeparateGroups)
                        for (DataFromParsedRead usedRead : offsetSearchResults.usedReads)
                            consensus.savedOriginalSequences.add((DataFromParsedReadWithAllGroups)usedRead);
                    calculatedConsensuses.consensuses.add(consensus);
                    numValidConsensuses++;
                }
                remainingData = offsetSearchResults.remainingReads;
            }
        }

        if (remainingData.size() > 0) {
            if (kmersNotFound)
                displayWarning.accept("WARNING: valid combinations of targets not found when searching for "
                        + "the most frequent k-mers; not processed " + remainingData.size()
                        + " reads from cluster of " + clusterSize + " reads! Barcode values: "
                        + formatBarcodeValues(remainingData.get(0).getBarcodes()));
            else if ((float)remainingData.size() / clusterSize >= skippedFractionToRepeat) {
                displayWarning.accept("WARNING: max consensuses per cluster exceeded; not processed "
                        + remainingData.size() + " reads from cluster of " + clusterSize + " reads! Barcode values: "
                        + formatBarcodeValues(Objects.requireNonNull(offsetSearchResults).barcodes));
            }
        }

        return calculatedConsensuses;
    }

    private OffsetSearchResults searchOffsets(List<DataFromParsedRead> data) {
        TByteObjectHashMap<TLongIntHashMap> kmerOffsetsForTargets = new TByteObjectHashMap<>();
        TLongHashSet skippedReads = new TLongHashSet();

        List<Barcode> barcodes = null;
        // targetIds only for target sequences, so -1 is not needed here
        for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
            HashMap<KMer, KMer> allKMers = new HashMap<>();
            for (DataFromParsedRead currentReadData : data) {
                if (barcodes == null)
                    barcodes = currentReadData.getBarcodes();
                if (!skippedReads.contains(currentReadData.getOriginalReadId())) {
                    SequenceWithAttributes seq = currentReadData.getSequences().get(targetId);
                    int length = seq.size();
                    if (length < kmerLength)
                        throw new IllegalStateException("length: " + length + ", kmerLength: " + kmerLength);
                    else {
                        int from = Math.max(0, length / 2 - kmerLength / 2 - kmerMaxOffset);
                        int to = Math.min(length - kmerLength, length / 2 - kmerLength / 2 + kmerMaxOffset);
                        for (int kmerStart = from; kmerStart <= to; kmerStart++) {
                            KMer currentKMer = new KMer(seq.getSeq().getRange(kmerStart, kmerStart + kmerLength));
                            KMer currentKMerCounter = allKMers.get(currentKMer);
                            if (currentKMerCounter == null)
                                allKMers.put(currentKMer, currentKMer);
                            else
                                currentKMerCounter.count++;
                        }
                    }
                }
            }
            KMer mostFrequentKMer = allKMers.values().stream().max(KMer::compareTo)
                    .orElseThrow(IllegalStateException::new);
            // key = read id, value = offset of the most frequent kmer
            TLongIntHashMap currentTargetOffsets = new TLongIntHashMap();
            for (DataFromParsedRead currentReadData : data) {
                long readId = currentReadData.getOriginalReadId();
                if (!skippedReads.contains(readId)) {
                    int bestKMerPosition = locateKMer(currentReadData.getSequences().get(targetId).getSeq(),
                            mostFrequentKMer.seq);
                    if (bestKMerPosition == -1)
                        skippedReads.add(readId);
                    else
                        currentTargetOffsets.put(readId, bestKMerPosition);
                }
            }
            kmerOffsetsForTargets.put(targetId, currentTargetOffsets);
        }

        ArrayList<DataFromParsedRead> usedReads = new ArrayList<>();
        ArrayList<DataFromParsedRead> remainingReads = new ArrayList<>();
        for (DataFromParsedRead read : data) {
            if (skippedReads.contains(read.getOriginalReadId()))
                remainingReads.add(read);
            else
                usedReads.add(read);
        }

        if (usedReads.size() == 0) {
            if (collectOriginalReadsData)
                for (long readId : skippedReads.toArray())
                    originalReadsData.get(readId).status = KMERS_NOT_FOUND;
            return null;
        } else
            return new OffsetSearchResults(kmerOffsetsForTargets, usedReads, remainingReads,
                    Objects.requireNonNull(barcodes));
    }

    /**
     * Find best position of kmer start in nucleotide sequence or return -1 if there is no good position.
     *
     * @param seq       sequence where to search kmer
     * @param kmerSeq   kmer sequence
     * @return          best position of kmer or -1 if not found
     */
    private int locateKMer(NucleotideSequence seq, NucleotideSequence kmerSeq) {
        int length = seq.size();
        if (length < kmerLength)
            throw new IllegalStateException("length: " + length + ", kmerLength: " + kmerLength);
        int bestPosition = -1;
        int bestNumErrors = kmerMatchMaxErrors + 1;
        int from = Math.max(0, length / 2 - kmerLength / 2 - kmerMaxOffset);
        int to = Math.min(length - kmerLength, length / 2 - kmerLength / 2 + kmerMaxOffset);
        for (int kmerStart = from; kmerStart <= to; kmerStart++) {
            int currentNumErrors = seqDiff(seq.getRange(kmerStart, kmerStart + kmerLength), kmerSeq);
            if (currentNumErrors < bestNumErrors) {
                bestPosition = kmerStart;
                bestNumErrors = currentNumErrors;
            }
        }
        return bestPosition;
    }

    private int seqDiff(NucleotideSequence seq1, NucleotideSequence seq2) {
        if ((seq1.size() != kmerLength) || (seq2.size() != kmerLength))
            throw new IllegalArgumentException("seq1: " + seq1 + ", seq2: " + seq2 + ", kmerLength = " + kmerLength);
        int diff = 0;
        for (int i = 0; i < kmerLength; i++)
            if (seq1.codeAt(i) != seq2.codeAt(i))
                diff++;
        return diff;
    }

    private Consensus calculateConsensus(OffsetSearchResults offsetSearchResults) {
        ConsensusDebugData debugData = (debugOutputStream == null) ? null
                : new ConsensusDebugData(numberOfTargets, debugQualityThreshold, NO_STAGE, false);
        TByteObjectHashMap<SequenceWithAttributes> sequences = new TByteObjectHashMap<>();
        long[] usedReadIds = offsetSearchResults.getUsedReadsIds();
        TrimmedLettersCounters trimmedLettersCounters = collectOriginalReadsData
                ? new TrimmedLettersCounters(numberOfTargets) : null;
        // targetIds only for target sequences, so -1 is not needed here
        for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
            TLongIntHashMap kmerOffsets = offsetSearchResults.kmerOffsetsForTargets.get(targetId);
            AlignedSequencesMatrix alignedSequencesMatrix = new AlignedSequencesMatrix();
            for (DataFromParsedRead currentRead : offsetSearchResults.usedReads)
                alignedSequencesMatrix.addRow(currentRead.getSequences().get(targetId),
                        kmerOffsets.get(currentRead.getOriginalReadId()));

            ConsensusBuilder consensusBuilder = new ConsensusBuilder();
            ArrayList<NSequenceWithQuality> consensusLetters = (debugData != null) ? new ArrayList<>() : null;
            for (int coordinate = alignedSequencesMatrix.getMinCoordinate();
                 coordinate <= alignedSequencesMatrix.getMaxCoordinate(); coordinate++) {
                ArrayList<SequenceWithAttributes> currentCoordinateLetters = new ArrayList<>();
                for (long readId : usedReadIds) {
                    SequenceWithAttributes currentLetter = alignedSequencesMatrix.letterAt(readId, coordinate);
                    // skip missing edges (null sequences)
                    if (!currentLetter.isNull())
                        currentCoordinateLetters.add(currentLetter);
                }
                if (currentCoordinateLetters.size() > 0) {
                    float coverage = (float)(currentCoordinateLetters.size()) / usedReadIds.length;
                    NSequenceWithQuality consensusLetter = calculateConsensusLetter(currentCoordinateLetters);
                    consensusBuilder.append(consensusLetter, coverage);
                    if (consensusLetters != null)
                        consensusLetters.add(consensusLetter);
                }
            }

            if (debugData != null) {
                ArrayList<ArrayList<SequenceWithAttributes>> debugDataForThisTarget = debugData.data.get(targetId - 1);
                for (long currentReadId : usedReadIds) {
                    ArrayList<SequenceWithAttributes> currentReadLetters = new ArrayList<>();
                    for (int coordinate = alignedSequencesMatrix.getMinCoordinate();
                         coordinate <= alignedSequencesMatrix.getMaxCoordinate(); coordinate++)
                        currentReadLetters.add(alignedSequencesMatrix.letterAt(currentReadId, coordinate));
                    debugDataForThisTarget.add(currentReadLetters);
                }
                debugData.consensusData.set(targetId - 1, consensusLetters);
            }

            // consensus sequence assembling and trimming by quality and coverage
            if (consensusBuilder.size() == 0) {
                if (collectOriginalReadsData)
                    for (long readId : usedReadIds) {
                        OriginalReadData currentReadData = originalReadsData.get(readId);
                        currentReadData.status = CONSENSUS_DISCARDED_TRIM;
                        currentReadData.consensusTrimmedLettersCounters = trimmedLettersCounters;
                    }
                return new Consensus(debugData, numberOfTargets, true);
            }
            SequenceWithQualityAndCoverage consensusRawSequence = consensusBuilder.createAndDestroy();
            NSequenceWithQuality consensusTrimmedSequence = trimConsensusBadQualityTails(
                    consensusRawSequence, targetId, trimmedLettersCounters);
            if (consensusTrimmedSequence == null) {
                if (collectOriginalReadsData)
                    for (long readId : usedReadIds) {
                        OriginalReadData currentReadData = originalReadsData.get(readId);
                        currentReadData.status = CONSENSUS_DISCARDED_TRIM;
                        currentReadData.consensusTrimmedLettersCounters = trimmedLettersCounters;
                    }
                return new Consensus(debugData, numberOfTargets, true);
            } else
                sequences.put(targetId, new SequenceWithAttributes(
                        consensusTrimmedSequence.getSequence(), consensusTrimmedSequence.getQuality(),
                        offsetSearchResults.usedReads.get(0).getOriginalReadId()));
        }

        Consensus consensus = new Consensus(sequences, offsetSearchResults.barcodes,
                offsetSearchResults.usedReads.size(), debugData, numberOfTargets, true, -1,
                defaultGroupsOverride.get());
        if (collectOriginalReadsData)
            for (long readId : usedReadIds) {
                OriginalReadData currentReadData = originalReadsData.get(readId);
                currentReadData.status = USED_IN_CONSENSUS;
                currentReadData.consensusTrimmedLettersCounters = trimmedLettersCounters;
                currentReadData.setConsensus(consensus);
            }
        return consensus;
    }
}
