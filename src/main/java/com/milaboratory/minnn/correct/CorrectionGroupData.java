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
package com.milaboratory.minnn.correct;

import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;

import java.util.*;

final class CorrectionGroupData {
    // intermediate counters for wildcards clustering
    Set<SequenceWithWildcardsCount> wildcardCounters = new HashSet<>();
    // counters for final clustering for correction
    Set<SequenceWithQualityAndCount> sequenceCounters = new HashSet<>();
    // keys: sequences after wildcards collapsing, values: original sequences that were collapsed by wildcards
    Map<NucleotideSequence, Set<NucleotideSequence>> originalSequencesWithWildcards = new HashMap<>();
    // keys: not corrected sequences, values: corrected sequences
    final Map<NucleotideSequence, NSequenceWithQuality> correctionMap = new HashMap<>();
    // counters for original not corrected barcodes, for filtering by count
    Map<NucleotideSequence, SequenceCounter> notCorrectedBarcodeCounters;
    // barcodes that are not filtered out if filtering by count is enabled
    final Set<NucleotideSequence> includedBarcodes;
    long lengthSum = 0;

    CorrectionGroupData(boolean filterByCount) {
        this.notCorrectedBarcodeCounters = filterByCount ? new HashMap<>() : null;
        this.includedBarcodes = filterByCount ? new HashSet<>() : null;
    }
}
