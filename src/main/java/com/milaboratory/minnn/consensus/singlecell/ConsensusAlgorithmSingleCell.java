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
package com.milaboratory.minnn.consensus.singlecell;

import com.milaboratory.minnn.consensus.*;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ConsensusAlgorithmSingleCell extends ConsensusAlgorithm {
    private final int kmerLength;
    private final int kmerMaxOffset;

    public ConsensusAlgorithmSingleCell(
            Consumer<String> displayWarning, int numberOfTargets, int maxConsensusesPerCluster,
            float skippedFractionToRepeat, int readsMinGoodSeqLength, float readsAvgQualityThreshold,
            int readsTrimWindowSize, int minGoodSeqLength, float avgQualityThreshold, int trimWindowSize,
            ConcurrentHashMap<Long, OriginalReadData> originalReadsData, int kmerLength, int kmerMaxOffset) {
        super(displayWarning, numberOfTargets, maxConsensusesPerCluster, skippedFractionToRepeat,
                Math.max(readsMinGoodSeqLength, kmerLength), readsAvgQualityThreshold, readsTrimWindowSize,
                minGoodSeqLength, avgQualityThreshold, trimWindowSize, originalReadsData);
        this.kmerLength = kmerLength;
        this.kmerMaxOffset = kmerMaxOffset;
    }

    @Override
    public CalculatedConsensuses process(Cluster cluster) {
        CalculatedConsensuses calculatedConsensuses = new CalculatedConsensuses(cluster.orderedPortIndex);
        List<DataFromParsedRead> remainingData = trimBadQualityTails(cluster.data);
        int clusterSize = cluster.data.size();
        int numValidConsensuses = 0;

        OffsetSearchResults offsetSearchResults = null;
        while ((remainingData.size() > 0)
                && ((float)remainingData.size() / clusterSize >= skippedFractionToRepeat)
                && (numValidConsensuses < maxConsensusesPerCluster)) {
            offsetSearchResults = searchOffsets(remainingData);
            Consensus consensus = calculateConsensus(offsetSearchResults.usedReads,
                    offsetSearchResults.offsetsForReads);
            remainingData = offsetSearchResults.remainingReads;
            if (!consensus.isConsensus) {
                displayWarning.accept("WARNING: consensus assembled from " + offsetSearchResults.usedReads.size()
                        + " reads discarded after quality trimming! Barcode values: "
                        + formatBarcodeValues(offsetSearchResults.barcodes) + ", original read ids: "
                        + Arrays.toString(offsetSearchResults.getUsedReadsIds()));
            } else {
                calculatedConsensuses.consensuses.add(consensus);
                numValidConsensuses++;
            }
        }

        if (((remainingData.size() > 0)
                && ((float)remainingData.size() / clusterSize >= skippedFractionToRepeat))) {
            displayWarning.accept("WARNING: max consensuses per cluster exceeded; not processed "
                    + remainingData.size() + " reads from cluster of " + clusterSize + " reads! Barcode values: "
                    + formatBarcodeValues(Objects.requireNonNull(offsetSearchResults).barcodes));
        }

        return calculatedConsensuses;
    }

    private OffsetSearchResults searchOffsets(List<DataFromParsedRead> data) {
        TLongHashSet skippedReads = new TLongHashSet();

        TargetBarcodes[] barcodes = null;
        for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
            HashSet<KMer> allKMers = new HashSet<>();
            for (DataFromParsedRead currentReadData : data) {
                if (barcodes == null)
                    barcodes = currentReadData.getBarcodes();
                SequenceWithAttributes seq = currentReadData.getSequences()[targetIndex];
                int length = seq.getSeq().size();
                if (length < kmerLength)
                    throw new IllegalStateException("length: " + length + ", kmerLength: " + kmerLength);
                else {
                    int from = Math.max(0, length / 2 - kmerLength / 2 - kmerMaxOffset);
                    int to = Math.min(length - kmerLength, length / 2 - kmerLength / 2 + kmerMaxOffset);

                }
            }

        }

        ArrayList<DataFromParsedRead> usedReads = new ArrayList<>();
        ArrayList<DataFromParsedRead> remainingReads = new ArrayList<>();
        for (DataFromParsedRead read : data) {
            if (skippedReads.contains(read.getOriginalReadId()))
                remainingReads.add(read);
            else
                usedReads.add(read);
        }
        return new OffsetSearchResults(usedReads, , remainingReads, Objects.requireNonNull(barcodes));
    }

    private Consensus calculateConsensus(List<DataFromParsedRead> usedReads,
                                         TLongObjectHashMap<int[]> offsetsForReads) {

    }
}
