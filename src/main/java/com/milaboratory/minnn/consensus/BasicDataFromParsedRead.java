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

import com.milaboratory.minnn.outputconverter.MatchedGroup;
import com.milaboratory.minnn.outputconverter.ParsedRead;

import java.util.*;
import java.util.stream.*;

public class BasicDataFromParsedRead implements DataFromParsedRead {
    protected final SequenceWithAttributes[] sequences;
    protected final TargetBarcodes[] barcodes;
    protected final long originalReadId;

    public BasicDataFromParsedRead(ParsedRead parsedRead, DefaultGroups defaultGroups,
                                   LinkedHashSet<String> consensusGroups) {
        LinkedHashSet<String> defaultGroupsSet = defaultGroups.get();
        int numberOfTargets = defaultGroups.getNumberOfTargets();
        originalReadId = parsedRead.getOriginalRead().getId();
        List<MatchedGroup> parsedReadGroups = parsedRead.getGroups();
        List<MatchedGroup> extractedGroups = parsedReadGroups.stream()
                .filter(g -> defaultGroupsSet.contains(g.getGroupName())).collect(Collectors.toList());
        if (extractedGroups.size() != numberOfTargets)
            throw new IllegalArgumentException("Wrong number of target groups in ParsedRead: expected "
                    + numberOfTargets + ", target groups in ParsedRead: " + parsedRead.getGroups().stream()
                    .map(MatchedGroup::getGroupName).filter(defaultGroupsSet::contains).collect(Collectors.toList()));
        sequences = new SequenceWithAttributes[numberOfTargets];
        extractedGroups.forEach(group ->
                sequences[getTargetIndex(group.getTargetId(), parsedRead.isReverseMatch())] =
                        new SequenceWithAttributes(group.getValue().getSequence(), group.getValue().getQuality(),
                                originalReadId));
        barcodes = IntStream.range(0, numberOfTargets).mapToObj(i -> new TargetBarcodes(new ArrayList<>()))
                .toArray(TargetBarcodes[]::new);
        parsedReadGroups.forEach(group -> {
            SequenceWithAttributes sequenceWithAttributes = new SequenceWithAttributes(
                    group.getValue().getSequence(), group.getValue().getQuality(), originalReadId);
            if (consensusGroups.contains(group.getGroupName())) {
                int targetIndex = getTargetIndex(group.getTargetId(), parsedRead.isReverseMatch());
                ArrayList<Barcode> currentTargetList = barcodes[targetIndex].targetBarcodes;
                currentTargetList.add(new Barcode(group.getGroupName(), sequenceWithAttributes));
            }
        });
    }

    public BasicDataFromParsedRead(SequenceWithAttributes[] sequences, TargetBarcodes[] barcodes,
                                   long originalReadId) {
        this.sequences = sequences;
        this.barcodes = barcodes;
        this.originalReadId = originalReadId;
    }

    protected int getTargetIndex(byte targetId, boolean isReverseMatch) {
        int index = targetId - 1;
        if (isReverseMatch) {
            if (index == 0)
                index = 1;
            else if (index == 1)
                index = 0;
        }
        return index;
    }

    @Override
    public SequenceWithAttributes[] getSequences() {
        return sequences.clone();
    }

    @Override
    public TargetBarcodes[] getBarcodes() {
        return barcodes.clone();
    }

    @Override
    public long getOriginalReadId() {
        return originalReadId;
    }
}
