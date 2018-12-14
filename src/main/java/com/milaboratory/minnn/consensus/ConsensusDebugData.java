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

import java.io.PrintStream;
import java.util.*;
import java.util.stream.*;

public final class ConsensusDebugData {
    private final int numberOfTargets;
    private final byte debugQualityThreshold;
    private final boolean stage2;
    // outer list - targetIndex, second - sequenceIndex, inner - positionIndex
    List<ArrayList<ArrayList<SequenceWithAttributes>>> data;
    // outer list - targetIndex, inner - positionIndex
    List<ArrayList<SequenceWithAttributes>> consensusData;
    // outer list - targetIndex, inner - sequenceIndex
    List<ArrayList<Long>> alignmentScores;

    public ConsensusDebugData(int numberOfTargets, byte debugQualityThreshold, boolean stage2) {
        this.numberOfTargets = numberOfTargets;
        this.debugQualityThreshold = debugQualityThreshold;
        this.stage2 = stage2;
        this.data = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> new ArrayList<ArrayList<SequenceWithAttributes>>()).collect(Collectors.toList());
        this.consensusData = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> new ArrayList<SequenceWithAttributes>()).collect(Collectors.toList());
    }

    public void writeDebugData(PrintStream debugOutputStream, int clusterIndex, int consensusIndex) {
        debugOutputStream.println("\n" + (stage2 ? "Stage 2, " : "Stage 1, ")
                + "clusterIndex: " + clusterIndex + ", consensusIndex: " + consensusIndex);
        for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
            debugOutputStream.println("targetIndex: " + targetIndex);
            ArrayList<ArrayList<SequenceWithAttributes>> targetData = data.get(targetIndex);
            ArrayList<SequenceWithAttributes> targetConsensus = consensusData.get(targetIndex);
            ArrayList<Long> targetAlignmentScores = alignmentScores.get(targetIndex);
            for (int sequenceIndex = 0; sequenceIndex < targetData.size(); sequenceIndex++) {
                ArrayList<SequenceWithAttributes> sequenceData = targetData.get(sequenceIndex);
                long alignmentScore = targetAlignmentScores.get(sequenceIndex);
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
                    sequenceString.append(", alignmentScore: ").append(alignmentScore);
                }
                debugOutputStream.println(sequenceString.toString());
            }
            StringBuilder consensusString = new StringBuilder();
            for (SequenceWithAttributes currentLetter : targetConsensus) {
                if (currentLetter.isEmpty())
                    consensusString.append("-");
                else {
                    if (currentLetter.getQual().value(0) < debugQualityThreshold)
                        consensusString.append(Character.toLowerCase(currentLetter.getSeq().symbolAt(0)));
                    else
                        consensusString.append(Character.toUpperCase(currentLetter.getSeq().symbolAt(0)));
                }
            }
            if (targetConsensus.size() > 0)
                consensusString.append(" - consensus");
            debugOutputStream.println(consensusString.toString());
        }
    }
}
