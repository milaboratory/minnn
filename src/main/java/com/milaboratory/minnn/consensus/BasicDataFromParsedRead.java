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

import com.milaboratory.minnn.outputconverter.MatchedGroup;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import gnu.trove.map.hash.TByteObjectHashMap;

import java.util.*;
import java.util.stream.*;

public class BasicDataFromParsedRead implements DataFromParsedRead {
    protected final TByteObjectHashMap<SequenceWithAttributes> sequences;
    protected final List<Barcode> barcodes;
    protected final long originalReadId;

    public BasicDataFromParsedRead(ParsedRead parsedRead, LinkedHashSet<String> consensusGroups) {
        LinkedHashSet<String> defaultGroupNames = parsedRead.getDefaultGroupNames();
        originalReadId = parsedRead.getOriginalRead().getId();
        List<MatchedGroup> parsedReadGroups = parsedRead.getGroups();
        List<MatchedGroup> extractedGroups = parsedReadGroups.stream()
                .filter(g -> defaultGroupNames.contains(g.getGroupName())).collect(Collectors.toList());
        sequences = new TByteObjectHashMap<>();
        extractedGroups.forEach(group -> sequences.put(group.getTargetId(), new SequenceWithAttributes(
                group.getValue().getSequence(), group.getValue().getQuality(), originalReadId)));
        barcodes = new ArrayList<>();
        parsedReadGroups.forEach(group -> {
            SequenceWithAttributes sequenceWithAttributes = new SequenceWithAttributes(
                    group.getValue().getSequence(), group.getValue().getQuality(), originalReadId);
            if (consensusGroups.contains(group.getGroupName()))
                barcodes.add(new Barcode(group.getGroupName(), sequenceWithAttributes, group.getTargetId()));
        });
    }

    public BasicDataFromParsedRead(TByteObjectHashMap<SequenceWithAttributes> sequences, List<Barcode> barcodes,
                                   long originalReadId) {
        this.sequences = sequences;
        this.barcodes = barcodes;
        this.originalReadId = originalReadId;
    }

    @Override
    public TByteObjectHashMap<SequenceWithAttributes> getSequences() {
        return sequences;
    }

    @Override
    public List<Barcode> getBarcodes() {
        return barcodes;
    }

    @Override
    public long getOriginalReadId() {
        return originalReadId;
    }
}
