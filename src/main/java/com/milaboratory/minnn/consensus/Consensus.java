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
package com.milaboratory.minnn.consensus;

import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.outputconverter.*;
import com.milaboratory.minnn.pattern.*;
import gnu.trove.map.hash.TByteObjectHashMap;

import java.util.*;

import static com.milaboratory.minnn.util.SystemUtils.*;

public final class Consensus {
    public final TByteObjectHashMap<SequenceWithAttributes> sequences;
    public final List<Barcode> barcodes;
    public final int consensusReadsNum;
    public final ConsensusDebugData debugData;
    public final boolean isConsensus;
    public final ArrayList<DataFromParsedReadWithAllGroups> savedOriginalSequences = new ArrayList<>();
    private final int numberOfTargets;
    public final boolean finalConsensus;
    public final long tempId;
    private final boolean defaultGroupsOverride;

    public Consensus(TByteObjectHashMap<SequenceWithAttributes> sequences, List<Barcode> barcodes,
                     int consensusReadsNum, ConsensusDebugData debugData, int numberOfTargets, boolean finalConsensus,
                     long tempId, boolean defaultGroupsOverride) {
        this.sequences = sequences;
        this.barcodes = barcodes;
        this.consensusReadsNum = consensusReadsNum;
        this.debugData = debugData;
        this.isConsensus = true;
        this.numberOfTargets = numberOfTargets;
        this.finalConsensus = finalConsensus;
        this.tempId = tempId;
        this.defaultGroupsOverride = defaultGroupsOverride;
    }

    public Consensus(ConsensusDebugData debugData, int numberOfTargets, boolean finalConsensus) {
        this.sequences = null;
        this.barcodes = null;
        this.consensusReadsNum = 0;
        this.debugData = debugData;
        this.isConsensus = false;
        this.numberOfTargets = numberOfTargets;
        this.finalConsensus = finalConsensus;
        this.tempId = -1;
        this.defaultGroupsOverride = false;
    }

    public ParsedRead toParsedRead() {
        if (!isConsensus || (sequences == null) || (barcodes == null))
            throw exitWithError("toParsedRead() called for null consensus!");
        SequenceRead originalRead;
        SingleRead[] reads = new SingleRead[numberOfTargets];
        ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
        for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
            SequenceWithAttributes currentSequence = sequences.get(targetId);
            reads[targetId - 1] = new SingleReadImpl(currentSequence.getOriginalReadId(),
                    currentSequence.toNSequenceWithQuality(), "Consensus");
            addReadGroupEdges(matchedGroupEdges, targetId, currentSequence.toNSequenceWithQuality());
        }
        for (Barcode barcode : barcodes) {
            NSequenceWithQuality targetSequence = (barcode.targetId == -1) ? barcode.value.toNSequenceWithQuality()
                    : sequences.get(barcode.targetId).toNSequenceWithQuality();
            addGroupEdges(matchedGroupEdges, barcode.targetId, barcode.groupName, targetSequence,
                    barcode.value.toNSequenceWithQuality());
        }
        if (numberOfTargets == 1)
            originalRead = reads[0];
        else if (numberOfTargets == 2)
            originalRead = new PairedRead(reads);
        else
            originalRead = new MultiRead(reads);

        Match bestMatch = new Match(numberOfTargets, 0, matchedGroupEdges);
        return new ParsedRead(originalRead, false, defaultGroupsOverride ? numberOfTargets : -1,
                bestMatch, consensusReadsNum);
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
                SequenceWithAttributes currentOriginalSequence = currentOriginalData.getSequences().get(targetId);
                SequenceWithAttributes currentConsensusSequence = sequences.get(targetId);
                reads[targetId - 1] = new SingleReadImpl(currentOriginalSequence.getOriginalReadId(),
                        currentOriginalSequence.toNSequenceWithQuality(), "");
                addReadGroupEdges(matchedGroupEdges, targetId,
                        currentOriginalSequence.toNSequenceWithQuality());
                addGroupEdges(matchedGroupEdges, targetId, "CR" + targetId,
                        currentOriginalSequence.toNSequenceWithQuality(),
                        currentConsensusSequence.toNSequenceWithQuality());
                for (HashMap.Entry<String, SequenceWithAttributes> entry
                        : currentOriginalData.getOtherGroups().entrySet())
                    addGroupEdges(matchedGroupEdges, targetId, entry.getKey(),
                            currentOriginalSequence.toNSequenceWithQuality(),
                            entry.getValue().toNSequenceWithQuality());
            }
            for (Barcode barcode : barcodes) {
                NSequenceWithQuality targetSequence = (barcode.targetId == -1) ? barcode.value.toNSequenceWithQuality()
                        : currentOriginalData.getSequences().get(barcode.targetId).toNSequenceWithQuality();
                addGroupEdges(matchedGroupEdges, barcode.targetId, barcode.groupName, targetSequence,
                        barcode.value.toNSequenceWithQuality());
            }

            SequenceRead originalRead;
            if (numberOfTargets == 1)
                originalRead = reads[0];
            else if (numberOfTargets == 2)
                originalRead = new PairedRead(reads);
            else
                originalRead = new MultiRead(reads);

            Match bestMatch = new Match(numberOfTargets, 0, matchedGroupEdges);
            generatedReads.add(new ParsedRead(originalRead, false,
                    defaultGroupsOverride ? numberOfTargets : -1, bestMatch, consensusReadsNum));
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
