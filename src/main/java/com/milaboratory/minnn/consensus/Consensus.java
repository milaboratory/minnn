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

import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.outputconverter.*;
import com.milaboratory.minnn.pattern.*;

import java.util.*;

import static com.milaboratory.minnn.util.SystemUtils.*;

public final class Consensus {
    public final SequenceWithAttributes[] sequences;
    public final TargetBarcodes[] barcodes;
    public final int consensusReadsNum;
    public final ConsensusDebugData debugData;
    public final boolean isConsensus;
    public final boolean finalConsensus;
    public final long tempId;
    public final ArrayList<DataFromParsedReadWithAllGroups> savedOriginalSequences = new ArrayList<>();
    private final int numberOfTargets;

    public Consensus(SequenceWithAttributes[] sequences, TargetBarcodes[] barcodes, int consensusReadsNum,
                     ConsensusDebugData debugData, int numberOfTargets, boolean finalConsensus, long tempId) {
        this.sequences = sequences;
        this.barcodes = barcodes;
        this.consensusReadsNum = consensusReadsNum;
        this.debugData = debugData;
        this.isConsensus = true;
        this.finalConsensus = finalConsensus;
        this.tempId = tempId;
        this.numberOfTargets = numberOfTargets;
    }

    public Consensus(ConsensusDebugData debugData, int numberOfTargets, boolean finalConsensus) {
        this.sequences = null;
        this.barcodes = null;
        this.consensusReadsNum = 0;
        this.debugData = debugData;
        this.isConsensus = false;
        this.finalConsensus = finalConsensus;
        this.tempId = -1;
        this.numberOfTargets = numberOfTargets;
    }

    public ParsedRead toParsedRead() {
        if (!isConsensus || (sequences == null) || (barcodes == null))
            throw exitWithError("toParsedRead() called for null consensus!");
        SequenceRead originalRead;
        SingleRead[] reads = new SingleRead[numberOfTargets];
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
        for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
            SequenceWithAttributes currentSequence = sequences[targetId - 1];
            TargetBarcodes targetBarcodes = barcodes[targetId - 1];
            reads[targetId - 1] = new SingleReadImpl(currentSequence.getOriginalReadId(),
                    currentSequence.toNSequenceWithQuality(), "Consensus");
            addReadGroupEdges(matchedGroupEdges, targetId, currentSequence.toNSequenceWithQuality());
            for (Barcode barcode : targetBarcodes.targetBarcodes)
                addGroupEdges(matchedGroupEdges, targetId, barcode.groupName,
                        currentSequence.toNSequenceWithQuality(), barcode.value.toNSequenceWithQuality());
        }
        if (numberOfTargets == 1)
            originalRead = reads[0];
        else if (numberOfTargets == 2)
            originalRead = new PairedRead(reads);
        else
            originalRead = new MultiRead(reads);

        Match bestMatch = new Match(numberOfTargets, 0, matchedGroupEdges);
        return new ParsedRead(originalRead, false, bestMatch, consensusReadsNum);
    }

    /** Used only with --consensuses-to-separate-groups flag */
    public List<ParsedRead> getReadsWithConsensuses() {
        if (!isConsensus || (sequences == null) || (barcodes == null))
            throw exitWithError("getReadsWithConsensuses() called for null consensus!");
        List<ParsedRead> generatedReads = new ArrayList<>();
        for (DataFromParsedReadWithAllGroups currentOriginalData : savedOriginalSequences) {
            ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
            SingleRead[] reads = new SingleRead[numberOfTargets];
            for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
                SequenceWithAttributes currentOriginalSequence = currentOriginalData.getSequences()[targetId - 1];
                SequenceWithAttributes currentConsensusSequence = sequences[targetId - 1];
                TargetBarcodes targetBarcodes = barcodes[targetId - 1];
                reads[targetId - 1] = new SingleReadImpl(currentOriginalSequence.getOriginalReadId(),
                        currentOriginalSequence.toNSequenceWithQuality(), "");
                addReadGroupEdges(matchedGroupEdges, targetId,
                        currentOriginalSequence.toNSequenceWithQuality());
                addGroupEdges(matchedGroupEdges, targetId, "CR" + targetId,
                        currentOriginalSequence.toNSequenceWithQuality(),
                        currentConsensusSequence.toNSequenceWithQuality());
                for (Barcode barcode : targetBarcodes.targetBarcodes)
                    addGroupEdges(matchedGroupEdges, targetId, barcode.groupName,
                            currentOriginalSequence.toNSequenceWithQuality(),
                            barcode.value.toNSequenceWithQuality());
                for (HashMap.Entry<String, SequenceWithAttributes> entry
                        : currentOriginalData.getOtherGroups().entrySet())
                    addGroupEdges(matchedGroupEdges, targetId, entry.getKey(),
                            currentOriginalSequence.toNSequenceWithQuality(),
                            entry.getValue().toNSequenceWithQuality());
            }

            SequenceRead originalRead;
            if (numberOfTargets == 1)
                originalRead = reads[0];
            else if (numberOfTargets == 2)
                originalRead = new PairedRead(reads);
            else
                originalRead = new MultiRead(reads);

            Match bestMatch = new Match(numberOfTargets, 0, matchedGroupEdges);
            generatedReads.add(new ParsedRead(originalRead, false, bestMatch, consensusReadsNum));
        }
        return generatedReads;
    }

    private void addReadGroupEdges(ArrayList<MatchedGroupEdge> matchedGroupEdges, byte targetId,
                                   NSequenceWithQuality seq) {
        matchedGroupEdges.add(new MatchedGroupEdge(seq, targetId,
                new GroupEdge("R" + targetId, true), 0));
        matchedGroupEdges.add(new MatchedGroupEdge(null, targetId,
                new GroupEdge("R" + targetId, false), seq.size()));
    }

    private void addGroupEdges(ArrayList<MatchedGroupEdge> matchedGroupEdges, byte targetId, String groupName,
                               NSequenceWithQuality target, NSequenceWithQuality value) {
        matchedGroupEdges.add(new MatchedGroupEdge(target, targetId, new GroupEdge(groupName, true), value));
        matchedGroupEdges.add(new MatchedGroupEdge(null, targetId,
                new GroupEdge(groupName, false), null));
    }
}
