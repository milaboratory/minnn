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

public final class CorrectionStats {
    public long correctedReads = 0;
    public long updatedQualityReads = 0;
    public long excludedReads = 0;
    public long totalWildcards = 0;
    public long totalNucleotides = 0;
    public long wildcardClusterNotAddedByThreshold = 0;
    public long wildcardCanAddToClusterCalls = 0;
    public long barcodeClusterNotAddedByWildcards = 0;
    public long barcodeClusterNotAddedByExpectedCount = 0;
    public long barcodeClusterNotAddedByThreshold = 0;
    public long barcodeCanAddToClusterCalls = 0;

    public CorrectionStats() {}

    public CorrectionStats(
            long correctedReads, long updatedQualityReads, long excludedReads, long totalWildcards,
            long totalNucleotides, long wildcardClusterNotAddedByThreshold, long wildcardCanAddToClusterCalls,
            long barcodeClusterNotAddedByWildcards, long barcodeClusterNotAddedByExpectedCount,
            long barcodeClusterNotAddedByThreshold, long barcodeCanAddToClusterCalls) {
        this.correctedReads = correctedReads;
        this.updatedQualityReads = updatedQualityReads;
        this.excludedReads = excludedReads;
        this.totalWildcards = totalWildcards;
        this.totalNucleotides = totalNucleotides;
        this.wildcardClusterNotAddedByThreshold = wildcardClusterNotAddedByThreshold;
        this.wildcardCanAddToClusterCalls = wildcardCanAddToClusterCalls;
        this.barcodeClusterNotAddedByWildcards = barcodeClusterNotAddedByWildcards;
        this.barcodeClusterNotAddedByExpectedCount = barcodeClusterNotAddedByExpectedCount;
        this.barcodeClusterNotAddedByThreshold = barcodeClusterNotAddedByThreshold;
        this.barcodeCanAddToClusterCalls = barcodeCanAddToClusterCalls;
    }

    public void add(CorrectionStats other) {
        correctedReads += other.correctedReads;
        updatedQualityReads += other.updatedQualityReads;
        excludedReads += other.excludedReads;
        totalWildcards += other.totalWildcards;
        totalNucleotides += other.totalNucleotides;
        wildcardClusterNotAddedByThreshold += other.wildcardClusterNotAddedByThreshold;
        wildcardCanAddToClusterCalls += other.wildcardCanAddToClusterCalls;
        barcodeClusterNotAddedByWildcards += other.barcodeClusterNotAddedByWildcards;
        barcodeClusterNotAddedByExpectedCount += other.barcodeClusterNotAddedByExpectedCount;
        barcodeClusterNotAddedByThreshold += other.barcodeClusterNotAddedByThreshold;
        barcodeCanAddToClusterCalls += other.barcodeCanAddToClusterCalls;
    }
}
