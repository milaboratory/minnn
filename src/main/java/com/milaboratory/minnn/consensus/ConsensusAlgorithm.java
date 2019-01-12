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
package com.milaboratory.minnn.consensus;

import cc.redberry.pipe.Processor;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.Wildcard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.milaboratory.core.sequence.quality.QualityTrimmer.trim;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.consensus.OriginalReadStatus.*;
import static com.milaboratory.minnn.util.SequencesCache.*;

public abstract class ConsensusAlgorithm implements Processor<Cluster, CalculatedConsensuses> {
    private static final double OVERFLOW_PROTECTION_MIN = 1E-100D;
    private static final double OVERFLOW_PROTECTION_MAX = 1E100D;
    private static final NucleotideSequence[] consensusMajorBases = new NucleotideSequence[] {
            sequencesCache.get(new NucleotideSequence("A")), sequencesCache.get(new NucleotideSequence("T")),
            sequencesCache.get(new NucleotideSequence("G")), sequencesCache.get(new NucleotideSequence("C")) };
    protected final Consumer<String> displayWarning;
    protected final int numberOfTargets;
    protected final int maxConsensusesPerCluster;
    protected final float skippedFractionToRepeat;
    protected final int minGoodSeqLength;
    protected final float avgQualityThreshold;
    protected final int trimWindowSize;
    protected final ConcurrentHashMap<Long, OriginalReadData> originalReadsData;
    private final int readsMinGoodSeqLength;
    private final float readsAvgQualityThreshold;
    private final int readsTrimWindowSize;

    public ConsensusAlgorithm(
            Consumer<String> displayWarning, int numberOfTargets, int maxConsensusesPerCluster,
            float skippedFractionToRepeat, int readsMinGoodSeqLength, float readsAvgQualityThreshold,
            int readsTrimWindowSize, int minGoodSeqLength, float avgQualityThreshold, int trimWindowSize,
            ConcurrentHashMap<Long, OriginalReadData> originalReadsData) {
        this.displayWarning = displayWarning;
        this.numberOfTargets = numberOfTargets;
        this.maxConsensusesPerCluster = maxConsensusesPerCluster;
        this.skippedFractionToRepeat = skippedFractionToRepeat;
        this.readsMinGoodSeqLength = readsMinGoodSeqLength;
        this.readsAvgQualityThreshold = readsAvgQualityThreshold;
        this.readsTrimWindowSize = readsTrimWindowSize;
        this.minGoodSeqLength = minGoodSeqLength;
        this.avgQualityThreshold = avgQualityThreshold;
        this.trimWindowSize = trimWindowSize;
        this.originalReadsData = originalReadsData;
    }

    /**
     * Calculate consensus letter from list of base letters.
     *
     * @param baseLetters       base letters; allowed values A, T, G, C and EMPTY_SEQ (deletion)
     * @return                  calculated consensus letter: letter with quality or EMPTY_SEQ for deletion
     */
    protected SequenceWithAttributes calculateConsensusLetter(List<SequenceWithAttributes> baseLetters) {
        if (baseLetters.size() == 1)
            return baseLetters.get(0);
        Map<NucleotideSequence, Integer> letterCounts = Arrays.stream(consensusMajorBases)
                .collect(Collectors.toMap(majorBase -> majorBase, majorBase -> (int)(baseLetters.stream()
                        .map(SequenceWithAttributes::getSeq).filter(majorBase::equals).count())));
        int deletionsCount = (int)(baseLetters.stream().filter(SequenceWithAttributes::isEmpty).count());
        if (letterCounts.values().stream().allMatch(count -> count <= deletionsCount))
            return new SequenceWithAttributes(SpecialSequences.EMPTY_SEQ, -1);
        final double gamma = 1.0 / (consensusMajorBases.length - 1);

        NucleotideSequence bestMajorBase = null;
        double bestQuality = -1;
        for (NucleotideSequence majorBase : consensusMajorBases) {
            double product = Math.pow(gamma, -letterCounts.get(majorBase));
            for (SequenceWithAttributes currentLetter : baseLetters)
                if (!currentLetter.isEmpty()) {
                    double errorProbability = Math.pow(10.0, -currentLetter.getQual().value(0) / 10.0);
                    if (currentLetter.getSeq().equals(majorBase))
                        product *= (1 - errorProbability) / Math.max(OVERFLOW_PROTECTION_MIN, errorProbability);
                    else
                        product *= errorProbability / Math.max(OVERFLOW_PROTECTION_MIN,
                                1 - gamma * errorProbability);
                    product = Math.min(product, OVERFLOW_PROTECTION_MAX);
                }

            double majorErrorProbability = 1.0 / (1 + product);
            double quality = -10 * Math.log10(majorErrorProbability);
            if (quality > bestQuality) {
                bestMajorBase = majorBase;
                bestQuality = quality;
            }
        }

        return new SequenceWithAttributes(Objects.requireNonNull(bestMajorBase),
                qualityCache.get((byte)Math.min(DEFAULT_MAX_QUALITY, bestQuality)), -1);
    }

    /**
     * Prepare letters for consensus calculation: expand wildcards and skip NULL_SEQ (missing edges).
     *
     * @param rawLetter input letter, can be basic, wildcard, EMPTY_SEQ or NULL_SEQ
     * @return          list of valid letters for consensus calculation: can be basic nucleotides,
     *                  deletion (EMPTY_SEQ) or empty list
     */
    protected List<SequenceWithAttributes> prepareForConsensus(SequenceWithAttributes rawLetter) {
        ArrayList<SequenceWithAttributes> baseLetters = new ArrayList<>();
        if (rawLetter.isEmpty())
            baseLetters.add(rawLetter);
        else if (!rawLetter.isNull()) {
            NucleotideSequence letterWithoutQuality = rawLetter.getSeq();
            if (letterWithoutQuality.containsWildcards()) {
                Wildcard wildcard = wildcards.get(letterWithoutQuality);
                for (int i = 0; i < wildcard.basicSize(); i++) {
                    NucleotideSequence currentBasicLetter = wildcardCodeToSequence.get(wildcard.getMatchingCode(i));
                    baseLetters.add(new SequenceWithAttributes(currentBasicLetter, qualityCache
                            .get((byte)(rawLetter.getQual().value(0) / wildcard.basicSize())),
                            rawLetter.getOriginalReadId()));
                }
            } else
                baseLetters.add(rawLetter);
        }

        return baseLetters;
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
            SequenceWithAttributes[] sequences = dataFromParsedRead.getSequences();
            SequenceWithAttributes[] processedSequences = new SequenceWithAttributes[numberOfTargets];
            boolean allSequencesAreGood = true;
            for (int i = 0; i < numberOfTargets; i++) {
                SequenceWithAttributes sequence = sequences[i];
                int trimResultLeft = trim(sequence.getQual(), 0, sequence.size(), 1,
                        true, readsAvgQualityThreshold, readsTrimWindowSize);
                if (trimResultLeft < -1) {
                    allSequencesAreGood = false;
                    break;
                }
                int trimResultRight = trim(sequence.getQual(), 0, sequence.size(), -1,
                        true, readsAvgQualityThreshold, readsTrimWindowSize);
                if (trimResultRight < 0)
                    throw new IllegalStateException("Unexpected negative trimming result");
                else if (trimResultRight - trimResultLeft - 1 < readsMinGoodSeqLength) {
                    allSequencesAreGood = false;
                    break;
                } else
                    processedSequences[i] = sequence.getSubSequence(trimResultLeft + 1, trimResultRight);
            }

            if (allSequencesAreGood) {
                if (dataFromParsedRead instanceof DataFromParsedReadWithAllGroups)
                    processedData.add(new DataFromParsedReadWithAllGroups(processedSequences,
                            dataFromParsedRead.getBarcodes(), dataFromParsedRead.getOriginalReadId(),
                            ((DataFromParsedReadWithAllGroups)dataFromParsedRead).getOtherGroups()));
                else
                    processedData.add(new BasicDataFromParsedRead(processedSequences, dataFromParsedRead.getBarcodes(),
                            dataFromParsedRead.getOriginalReadId()));
            } else if (originalReadsData != null)
                originalReadsData.get(dataFromParsedRead.getOriginalReadId()).status = READ_DISCARDED_TRIM;
        }

        return processedData;
    }

    protected String formatBarcodeValues(TargetBarcodes[] targetBarcodes) {
        ArrayList<Barcode> barcodes = new ArrayList<>();
        Arrays.stream(targetBarcodes).forEach(tb -> barcodes.addAll(tb.targetBarcodes));
        barcodes.sort(Comparator.comparing(b -> b.groupName));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < barcodes.size(); i++) {
            Barcode barcode = barcodes.get(i);
            if (i > 0)
                builder.append(", ");
            builder.append(barcode.groupName).append('=').append(barcode.value.getSeq().toString());
        }
        return builder.toString();
    }
}
