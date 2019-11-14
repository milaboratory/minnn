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
package com.milaboratory.minnn.correct;

import com.milaboratory.core.clustering.SequenceExtractor;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.Sequence;

import java.util.*;

final class CorrectionGroupData {
    private final boolean disableBarcodesQuality;

    public final Map<NucleotideSequence, SequenceWithCount> sequenceCounters = new HashMap<>();
    // keys: not corrected sequences, values: corrected sequences
    final Map<NucleotideSequence, NSequenceWithQuality> correctionMapWithQualities;
    final Map<NucleotideSequence, NucleotideSequence> correctionMapWithoutQualities;
    // counters for original not corrected barcodes, for filtering by count
    final Map<NucleotideSequence, RawSequenceCounter> notCorrectedBarcodeCounters;
    // barcodes that are not filtered out if filtering by count is enabled
    final Set<NucleotideSequence> includedBarcodes;
    long lengthSum = 0;

    CorrectionGroupData(boolean disableBarcodesQuality, boolean filterByCount) {
        this.disableBarcodesQuality = disableBarcodesQuality;
        this.correctionMapWithQualities = disableBarcodesQuality ? null : new HashMap<>();
        this.correctionMapWithoutQualities = disableBarcodesQuality ? new HashMap<>() : null;
        this.notCorrectedBarcodeCounters = filterByCount ? new HashMap<>() : null;
        this.includedBarcodes = filterByCount ? new HashSet<>() : null;
    }

    static class SequenceWithCount {
        final NSequenceWithQuality seqWithQuality;
        final NucleotideSequence seqWithoutQuality;
        long count;

        SequenceWithCount(NSequenceWithQuality seqWithQuality, long count, boolean disableBarcodesQuality) {
            this.seqWithQuality = disableBarcodesQuality ? null : seqWithQuality;
            this.seqWithoutQuality = disableBarcodesQuality ? seqWithQuality.getSequence() : null;
            this.count = count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SequenceWithCount that = (SequenceWithCount)o;
            if (!Objects.equals(seqWithQuality, that.seqWithQuality))
                return false;
            return Objects.equals(seqWithoutQuality, that.seqWithoutQuality);
        }

        @Override
        public int hashCode() {
            int result = seqWithQuality != null ? seqWithQuality.hashCode() : 0;
            result = result + (seqWithoutQuality != null ? seqWithoutQuality.hashCode() : 0);
            return result;
        }
    }

    class SequenceCounterExtractor<S extends Sequence> implements SequenceExtractor<SequenceWithCount, S> {
        @Override
        @SuppressWarnings("unchecked")
        public S getSequence(SequenceWithCount sequenceWithCount) {
            if (disableBarcodesQuality)
                return (S)(sequenceWithCount.seqWithoutQuality);
            else
                return (S)(new SequenceWithQualityForClustering(sequenceWithCount.seqWithQuality));
        }
    }
}
