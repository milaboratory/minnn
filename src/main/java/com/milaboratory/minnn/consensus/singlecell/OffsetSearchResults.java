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
package com.milaboratory.minnn.consensus.singlecell;

import com.milaboratory.minnn.consensus.Barcode;
import com.milaboratory.minnn.consensus.DataFromParsedRead;
import gnu.trove.map.hash.TByteObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;

import java.util.*;

final class OffsetSearchResults {
    final TByteObjectHashMap<TLongIntHashMap> kmerOffsetsForTargets;
    final List<DataFromParsedRead> usedReads;
    final List<DataFromParsedRead> remainingReads;
    final List<Barcode> barcodes;

    OffsetSearchResults(TByteObjectHashMap<TLongIntHashMap> kmerOffsetsForTargets, List<DataFromParsedRead> usedReads,
                        List<DataFromParsedRead> remainingReads, List<Barcode> barcodes) {
        this.kmerOffsetsForTargets = kmerOffsetsForTargets;
        if (usedReads.size() < 1)
            throw new IllegalArgumentException("kmerOffsetsForTargets: " + kmerOffsetsForTargets
                    + ", usedReads: " + usedReads + ", remainingReads: " + remainingReads + ", barcodes: " + barcodes);
        this.usedReads = Collections.unmodifiableList(usedReads);
        this.remainingReads = Collections.unmodifiableList(remainingReads);
        this.barcodes = barcodes;
    }

    long[] getUsedReadsIds() {
        return usedReads.stream().mapToLong(DataFromParsedRead::getOriginalReadId).toArray();
    }
}
