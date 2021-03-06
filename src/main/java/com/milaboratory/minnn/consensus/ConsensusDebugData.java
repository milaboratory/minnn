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

import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.*;

import static com.milaboratory.minnn.consensus.ConsensusStageForDebug.*;

public final class ConsensusDebugData implements java.io.Serializable {
    private final int numberOfTargets;
    private final byte debugQualityThreshold;
    private final ConsensusStageForDebug stage;
    private final boolean useAlignmentScores;
    // targetIndex is always targetId - 1; targetId -1 is not used because these ids are only for target sequences
    // outer list - targetIndex, second - sequenceIndex, inner - positionIndex
    public List<ArrayList<ArrayList<SequenceWithAttributes>>> data;
    // for consensusData, trimmedConsensusData and alignmentScores: outer list - targetIndex, inner - positionIndex
    public List<ArrayList<NSequenceWithQuality>> consensusData;
    public List<ArrayList<NSequenceWithQuality>> trimmedConsensusData;
    public List<ArrayList<Long>> alignmentScores;

    public ConsensusDebugData(int numberOfTargets, byte debugQualityThreshold, ConsensusStageForDebug stage,
                              boolean useAlignmentScores) {
        this.numberOfTargets = numberOfTargets;
        this.debugQualityThreshold = debugQualityThreshold;
        this.stage = stage;
        this.useAlignmentScores = useAlignmentScores;
        this.data = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> new ArrayList<ArrayList<SequenceWithAttributes>>()).collect(Collectors.toList());
        this.consensusData = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> new ArrayList<NSequenceWithQuality>()).collect(Collectors.toList());
        this.trimmedConsensusData = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> new ArrayList<NSequenceWithQuality>()).collect(Collectors.toList());
        if (useAlignmentScores)
            this.alignmentScores = IntStream.range(0, numberOfTargets)
                    .mapToObj(i -> new ArrayList<Long>()).collect(Collectors.toList());
    }

    public void writeDebugData(PrintStream debugOutputStream, long subclusterIndex, int consensusIndex) {
        String stagePrefix = "";
        if (stage == STAGE1)
            stagePrefix = "Stage 1, ";
        else if (stage == STAGE2)
            stagePrefix = "Stage 2, ";
        debugOutputStream.println("\n" + stagePrefix + "subclusterIndex: " + subclusterIndex
                + ", consensusIndex: " + consensusIndex);
        for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
            debugOutputStream.println("targetIndex: " + targetIndex);
            ArrayList<ArrayList<SequenceWithAttributes>> targetData = data.get(targetIndex);
            ArrayList<NSequenceWithQuality> targetConsensus = consensusData.get(targetIndex);
            ArrayList<NSequenceWithQuality> targetTrimmedConsensus = trimmedConsensusData.get(targetIndex);
            ArrayList<Long> targetAlignmentScores = useAlignmentScores ? alignmentScores.get(targetIndex) : null;

            for (int sequenceIndex = 0; sequenceIndex < targetData.size(); sequenceIndex++) {
                ArrayList<SequenceWithAttributes> sequenceData = targetData.get(sequenceIndex);
                long alignmentScore = useAlignmentScores ? targetAlignmentScores.get(sequenceIndex) : 0;
                StringBuilder sequenceString = new StringBuilder();
                for (SequenceWithAttributes currentLetter : sequenceData) {
                    if (currentLetter.isNull())
                        sequenceString.append(".");
                    else if (currentLetter.isEmpty())
                        sequenceString.append("-");
                    else {
                        if (currentLetter.getQual().value(0) < debugQualityThreshold)
                            sequenceString.append(Character.toLowerCase(currentLetter.getSeq().symbolAt(0)));
                        else
                            sequenceString.append(Character.toUpperCase(currentLetter.getSeq().symbolAt(0)));
                    }
                }
                if (sequenceData.size() > 0) {
                    sequenceString.append(" - originalReadId: ").append(sequenceData.get(0).getOriginalReadId());
                    if (useAlignmentScores)
                        sequenceString.append(", alignmentScore: ").append(alignmentScore);
                }
                debugOutputStream.println(sequenceString.toString());
            }

            StringBuilder consensusString = new StringBuilder();
            for (NSequenceWithQuality currentLetter : targetConsensus) {
                if (currentLetter == NSequenceWithQuality.EMPTY)
                    consensusString.append("-");
                else {
                    if (currentLetter.getQuality().value(0) < debugQualityThreshold)
                        consensusString.append(Character.toLowerCase(currentLetter.getSequence().symbolAt(0)));
                    else
                        consensusString.append(Character.toUpperCase(currentLetter.getSequence().symbolAt(0)));
                }
            }
            if (targetConsensus.size() > 0)
                consensusString.append(" - consensus before trimming");
            debugOutputStream.println(consensusString.toString());

            StringBuilder trimmedConsensusString = new StringBuilder();
            for (NSequenceWithQuality currentLetter : targetTrimmedConsensus) {
                if (currentLetter == NSequenceWithQuality.EMPTY)
                    trimmedConsensusString.append("-");
                else if (currentLetter == null)
                    trimmedConsensusString.append(".");
                else {
                    if (currentLetter.getQuality().value(0) < debugQualityThreshold)
                        trimmedConsensusString.append(Character.toLowerCase(
                                currentLetter.getSequence().symbolAt(0)));
                    else
                        trimmedConsensusString.append(Character.toUpperCase(
                                currentLetter.getSequence().symbolAt(0)));
                }
            }
            if (targetTrimmedConsensus.size() > 0)
                trimmedConsensusString.append(" - trimmed consensus");
            debugOutputStream.println(trimmedConsensusString.toString());
        }
    }
}
