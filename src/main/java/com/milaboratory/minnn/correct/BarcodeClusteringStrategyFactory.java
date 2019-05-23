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

import com.milaboratory.core.tree.TreeSearchParameters;
import com.milaboratory.minnn.stat.MutationProbability;

public final class BarcodeClusteringStrategyFactory {
    private final float maxErrorsCountMultiplier;
    private final float maxErrorsWorstBarcodesShare;
    private final float maxErrorsShare;
    private final int maxErrors;
    private final float threshold;
    private final int maxClusterDepth;
    private final MutationProbability mutationProbability;

    public BarcodeClusteringStrategyFactory(
            float maxErrorsCountMultiplier, float maxErrorsWorstBarcodesShare, float maxErrorsShare, int maxErrors,
            float threshold, int maxClusterDepth, MutationProbability mutationProbability) {
        this.maxErrorsCountMultiplier = maxErrorsCountMultiplier;
        this.maxErrorsWorstBarcodesShare = maxErrorsWorstBarcodesShare;
        this.maxErrorsShare = maxErrorsShare;
        this.maxErrors = maxErrors;
        this.threshold = threshold;
        this.maxClusterDepth = maxClusterDepth;
        this.mutationProbability = mutationProbability;
    }

    boolean averageBarcodeLengthRequired() {
        return (maxErrorsCountMultiplier >= 0) || (maxErrorsShare >= 0);
    }

    boolean averageErrorProbabilityRequired() {
        return maxErrorsCountMultiplier >= 0;
    }

    float getMaxErrorsWorstBarcodesShare() {
        return maxErrorsWorstBarcodesShare;
    }

    BarcodeClusteringStrategy createStrategy(float averageErrorProbability, float averageBarcodeLength) {
        int calculatedMaxErrors = (maxErrors >= 0) ? maxErrors : Integer.MAX_VALUE;
        if (maxErrorsShare >= 0)
            calculatedMaxErrors = Math.min(calculatedMaxErrors, Math.round(maxErrorsShare * averageBarcodeLength));
        if (maxErrorsCountMultiplier >= 0)
            calculatedMaxErrors = Math.min(calculatedMaxErrors, Math.round(maxErrorsCountMultiplier
                    * averageErrorProbability * averageBarcodeLength));
        return new BarcodeClusteringStrategy(new TreeSearchParameters(calculatedMaxErrors, calculatedMaxErrors,
                calculatedMaxErrors, calculatedMaxErrors), threshold, maxClusterDepth, mutationProbability);
    }
}
