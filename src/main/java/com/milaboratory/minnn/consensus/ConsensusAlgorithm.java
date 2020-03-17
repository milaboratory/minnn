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
package com.milaboratory.minnn.consensus;

import cc.redberry.pipe.Processor;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.minnn.consensus.trimmer.ConsensusTrimmer;
import com.milaboratory.minnn.consensus.trimmer.SequenceWithQualityAndCoverage;
import com.milaboratory.minnn.util.ConsensusLetter;
import gnu.trove.map.hash.TByteObjectHashMap;
import org.clapper.util.misc.FileHashMap;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.milaboratory.core.sequence.quality.QualityTrimmer.trim;
import static com.milaboratory.minnn.consensus.OriginalReadStatus.*;

public abstract class ConsensusAlgorithm implements Processor<Cluster, CalculatedConsensuses> {
    protected final Consumer<String> displayWarning;
    protected final int numberOfTargets;
    protected final int maxConsensusesPerCluster;
    protected final float skippedFractionToRepeat;
    protected final boolean toSeparateGroups;
    protected final PrintStream debugOutputStream;
    protected final byte debugQualityThreshold;
    private final FileHashMap<Long, OriginalReadData> originalReadsData;
    protected final boolean collectOriginalReadsData;
    protected final AtomicLong consensusCurrentTempId;
    // this flag must be set after reading 1st cluster in process() function
    protected final AtomicBoolean defaultGroupsOverride = new AtomicBoolean(false);
    private final int readsMinGoodSeqLength;
    private final float readsAvgQualityThreshold;
    private final int readsTrimWindowSize;
    private final int minGoodSeqLength;
    private final ConsensusTrimmer consensusTrimmer;

    public ConsensusAlgorithm(
            Consumer<String> displayWarning, int numberOfTargets, int maxConsensusesPerCluster,
            float skippedFractionToRepeat, int readsMinGoodSeqLength, float readsAvgQualityThreshold,
            int readsTrimWindowSize, int minGoodSeqLength, float lowCoverageThreshold, float avgQualityThreshold,
            float avgQualityThresholdForLowCoverage, int trimWindowSize, boolean toSeparateGroups,
            PrintStream debugOutputStream, byte debugQualityThreshold,
            FileHashMap<Long, OriginalReadData> originalReadsData) {
        this.displayWarning = displayWarning;
        this.numberOfTargets = numberOfTargets;
        this.maxConsensusesPerCluster = maxConsensusesPerCluster;
        this.skippedFractionToRepeat = skippedFractionToRepeat;
        this.readsMinGoodSeqLength = readsMinGoodSeqLength;
        this.readsAvgQualityThreshold = readsAvgQualityThreshold;
        this.readsTrimWindowSize = readsTrimWindowSize;
        this.minGoodSeqLength = minGoodSeqLength;
        this.consensusTrimmer = new ConsensusTrimmer(trimWindowSize, lowCoverageThreshold, avgQualityThreshold,
                avgQualityThresholdForLowCoverage);
        this.toSeparateGroups = toSeparateGroups;
        this.debugOutputStream = debugOutputStream;
        this.debugQualityThreshold = debugQualityThreshold;
        this.originalReadsData = originalReadsData;
        this.collectOriginalReadsData = (originalReadsData != null);
        this.consensusCurrentTempId = new AtomicLong(0);
    }

    protected OriginalReadData getOriginalReadData(long readId) {
        synchronized (originalReadsData) {
            return originalReadsData.get(readId);
        }
    }

    protected void setOriginalReadData(long readId, OriginalReadData originalReadData) {
        synchronized (originalReadsData) {
            originalReadsData.put(readId, originalReadData);
        }
    }

    /**
     * Calculate consensus letter from list of base letters.
     *
     * @param baseLetters       base letters; can be basic letters, wildcards or EMPTY_SEQ (deletion)
     * @return                  calculated consensus letter: letter with quality or EMPTY for deletion
     */
    protected NSequenceWithQuality calculateConsensusLetter(List<SequenceWithAttributes> baseLetters) {
        if (baseLetters.size() == 1)
            return baseLetters.get(0).toNSequenceWithQuality();
        ConsensusLetter consensusLetter = new ConsensusLetter(baseLetters.stream()
                .map(SequenceWithAttributes::toNSequenceWithQuality).collect(Collectors.toList()));
        if (consensusLetter.isDeletionMaxCount())
            return NSequenceWithQuality.EMPTY;
        else
            return consensusLetter.getConsensusLetter();
    }

    /**
     * Trim bad quality tails and filter out entirely bad sequences from data.
     *
     * @param data      data from cluster of parsed reads with same barcodes
     * @return          trimmed and filtered data
     */
    protected List<DataFromParsedRead> trimBadQualityTails(List<DataFromParsedRead> data) {
        List<DataFromParsedRead> processedData = new ArrayList<>();
        for (DataFromParsedRead dataFromParsedRead : data) {
            TByteObjectHashMap<SequenceWithAttributes> sequences = dataFromParsedRead.getSequences();
            TByteObjectHashMap<SequenceWithAttributes> processedSequences = new TByteObjectHashMap<>();
            TrimmedLettersCounters trimmedLettersCounters = collectOriginalReadsData
                    ? new TrimmedLettersCounters(numberOfTargets) : null;
            boolean allSequencesAreGood = true;
            for (byte targetId : sequences.keys()) {
                SequenceWithAttributes sequence = sequences.get(targetId);
                int trimResultLeft = trim(sequence.getQual(), 0, sequence.size(), 1,
                        true, readsAvgQualityThreshold, readsTrimWindowSize);
                if (trimResultLeft < -1) {
                    allSequencesAreGood = false;
                    if (collectOriginalReadsData)
                        trimmedLettersCounters.byTargetId.put(targetId, sequence.size());
                    break;
                }
                int trimResultRight = trim(sequence.getQual(), 0, sequence.size(), -1,
                        true, readsAvgQualityThreshold, readsTrimWindowSize);
                if (trimResultRight < 0)
                    throw new IllegalStateException("Unexpected negative trimming result");
                else if (trimResultRight - trimResultLeft - 1 < readsMinGoodSeqLength) {
                    allSequencesAreGood = false;
                    if (collectOriginalReadsData)
                        trimmedLettersCounters.byTargetId.put(targetId, sequence.size()
                                - Math.max(0, trimResultRight - trimResultLeft - 1));
                    break;
                } else {
                    if (collectOriginalReadsData)
                        trimmedLettersCounters.byTargetId.put(targetId,
                                sequence.size() - (trimResultRight - trimResultLeft - 1));
                    processedSequences.put(targetId, sequence.getSubSequence(trimResultLeft + 1, trimResultRight));
                }
            }

            long originalReadId = dataFromParsedRead.getOriginalReadId();
            OriginalReadData currentReadData = collectOriginalReadsData ? getOriginalReadData(originalReadId) : null;
            if (currentReadData != null)
                currentReadData.trimmedLettersCounters = trimmedLettersCounters;
            if (allSequencesAreGood) {
                if (dataFromParsedRead instanceof DataFromParsedReadWithAllGroups)
                    processedData.add(new DataFromParsedReadWithAllGroups(processedSequences,
                            dataFromParsedRead.getBarcodes(), originalReadId,
                            dataFromParsedRead.isDefaultGroupsOverride(),
                            ((DataFromParsedReadWithAllGroups)dataFromParsedRead).getOtherGroups()));
                else
                    processedData.add(new DataFromParsedRead(processedSequences, dataFromParsedRead.getBarcodes(),
                            originalReadId, dataFromParsedRead.isDefaultGroupsOverride()));
            } else if (currentReadData != null)
                currentReadData.status = READ_DISCARDED_TRIM;
            if (currentReadData != null)
                setOriginalReadData(originalReadId, currentReadData);
        }

        return processedData;
    }

    /**
     * Trim bad quality tails from consensus sequence.
     *
     * @param consensus                 calculated consensus sequence with quality and coverage
     * @param targetId                  target id of this sequence; used for saving counts of trimmed letters
     * @param trimmedLettersCounters    data structure to save counts of trimmed letters,
     *                                  or null if saving original reads data is not enabled
     * @param debugData                 data structure to save trimmed consensus debug data,
     *                                  or null if debug file is not enabled
     * @return                          trimmed sequence or null if consensus was discarded after trimming
     */
    protected NSequenceWithQuality trimConsensusBadQualityTails(
            SequenceWithQualityAndCoverage consensus, byte targetId, TrimmedLettersCounters trimmedLettersCounters,
            ConsensusDebugData debugData) {
        NSequenceWithQuality trimmedConsensus;
        int trimResultLeft = consensusTrimmer.trim(consensus, 0, consensus.size(), true);
        int trimResultRight = -1;
        if (trimResultLeft < -1) {
            if (collectOriginalReadsData)
                trimmedLettersCounters.byTargetId.put(targetId, consensus.size());
            trimmedConsensus = null;
        } else {
            trimResultRight = consensusTrimmer.trim(consensus, 0, consensus.size(), false);
            if (trimResultRight < 0)
                throw new IllegalStateException("Unexpected negative trimming result");
            else if (trimResultRight - trimResultLeft - 1 < minGoodSeqLength) {
                if (collectOriginalReadsData)
                    trimmedLettersCounters.byTargetId.put(targetId, consensus.size()
                            - Math.max(0, trimResultRight - trimResultLeft - 1));
                trimmedConsensus = null;
            } else {
                if (collectOriginalReadsData)
                    trimmedLettersCounters.byTargetId.put(targetId,
                            consensus.size() - (trimResultRight - trimResultLeft - 1));
                trimmedConsensus = consensus.toNSequenceWithQuality(trimResultLeft + 1, trimResultRight);
            }
        }

        if (debugData != null) {
            ArrayList<NSequenceWithQuality> currentRawConsensusData = debugData.consensusData
                    .get(targetId - 1);
            ArrayList<NSequenceWithQuality> currentTrimmedConsensusData = debugData.trimmedConsensusData
                    .get(targetId - 1);
            for (int i = 0; i < currentRawConsensusData.size(); i++) {
                if ((trimmedConsensus == null) || (i <= trimResultLeft) || (i >= trimResultRight))
                    currentTrimmedConsensusData.add(null);
                else
                    currentTrimmedConsensusData.add(currentRawConsensusData.get(i));
            }
        }
        return trimmedConsensus;
    }

    protected String formatBarcodeValues(List<Barcode> barcodes) {
        List<Barcode> sortedBarcodes = new ArrayList<>(barcodes);
        sortedBarcodes.sort(Comparator.comparing(b -> b.groupName));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sortedBarcodes.size(); i++) {
            Barcode barcode = sortedBarcodes.get(i);
            if (i > 0)
                builder.append(", ");
            builder.append(barcode.groupName).append('=').append(barcode.value.getSeq().toString());
        }
        return builder.toString();
    }
}
