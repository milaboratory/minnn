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

import com.milaboratory.minnn.consensus.SequenceWithAttributes;
import gnu.trove.map.hash.TLongObjectHashMap;

public final class AlignedSequencesMatrix {
    private final TLongObjectHashMap<AlignedMatrixRow> rows = new TLongObjectHashMap<>();

    public void addRow(SequenceWithAttributes seq, int kmerOffset) {
        rows.put(seq.getOriginalReadId(), new AlignedMatrixRow(seq, kmerOffset));
    }

    public int getMinCoordinate() {
        return rows.valueCollection().stream().mapToInt(row -> row.minCoordinate).min()
                .orElseThrow(IllegalStateException::new);
    }

    public int getMaxCoordinate() {
        return rows.valueCollection().stream().mapToInt(row -> row.maxCoordinate).max()
                .orElseThrow(IllegalStateException::new);
    }

    public SequenceWithAttributes letterAt(long readId, int coordinate) {
        AlignedMatrixRow row = rows.get(readId);
        return row.seq.letterAt(coordinate + row.kmerOffset);
    }

    private final class AlignedMatrixRow {
        final SequenceWithAttributes seq;
        final int kmerOffset;
        final int minCoordinate;
        final int maxCoordinate;

        private AlignedMatrixRow(SequenceWithAttributes seq, int kmerOffset) {
            this.seq = seq;
            this.kmerOffset = kmerOffset;
            this.minCoordinate = -kmerOffset;
            this.maxCoordinate = seq.size() - kmerOffset - 1;   // inclusive
        }
    }
}
