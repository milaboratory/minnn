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
package com.milaboratory.minnn.consensus.trimmer;

import com.milaboratory.core.sequence.SequenceQuality;

/**
 * Modified QualityTrimmer that uses different quality thresholds for low coverage and high coverage consensus parts.
 *
 * @see com.milaboratory.core.sequence.quality.QualityTrimmer
 */
public final class ConsensusTrimmer {
    private final int windowSize;
    private final float lowCoverageThreshold;
    private final float qualityThreshold;
    private final float qualityThresholdForLowCoverage;

    public ConsensusTrimmer(
            int windowSize, float lowCoverageThreshold, float qualityThreshold, float qualityThresholdForLowCoverage) {
        this.windowSize = windowSize;
        this.lowCoverageThreshold = lowCoverageThreshold;
        this.qualityThreshold = qualityThreshold;
        this.qualityThresholdForLowCoverage = qualityThresholdForLowCoverage;
    }

    /**
     * Find optimal trimming position for assembled consensus.
     *
     * @param seq                   sequence with quality and coverage
     * @param leftmostPosition      scanning region from, inclusive
     * @param rightmostPosition     scanning region to, exclusive
     * @param leftTrim              true to trim on the left side of the sequence (scan to the right),
     *                              false for the right side
     * @return                      trimming position if search was successful (last position of the region) or
     *                              (-2 - trimming position) if search was unsuccessful
     */
    public int trim(
            SequenceWithQualityAndCoverage seq, int leftmostPosition, int rightmostPosition, boolean leftTrim) {
        if (seq.size() == 0)
            return leftTrim ? -1 : 0;
        int scanIncrement = leftTrim ? 1 : -1;
        int positionsToScan = rightmostPosition - leftmostPosition;
        int windowSize = Math.min(positionsToScan, this.windowSize);
        SequenceQuality quality = seq.getQuality();
        float[] coverage = seq.getCoverage();

        // Minimal sum coverage for the window
        float sumThresholdCoverage = lowCoverageThreshold * windowSize;

        // Minimal sum quality for the window in case of good coverage
        int sumThresholdQuality = (int)Math.ceil(qualityThreshold * windowSize);

        // Minimal sum quality for the window in case of low coverage
        int sumThresholdQualityForLowCoverage = (int)Math.ceil(qualityThresholdForLowCoverage * windowSize);

        // Current sum of quality scores and sum of coverage values inside the window
        int sumQuality = 0;
        float sumCoverage = 0;

        // Current position
        int position = scanIncrement == 1 ? leftmostPosition : rightmostPosition - 1;
        // Last position of current search window
        int windowEndPosition = position;

        // Calculating initial sum values
        for (int i = 0; i < windowSize; i++) {
            sumQuality += quality.value(position);
            sumCoverage += coverage[position];
            position += scanIncrement;
        }

        // Checking whether first window is already inside the good quality region
        if (checkQualityAndCoverage(sumQuality, sumCoverage, sumThresholdQuality, sumThresholdQualityForLowCoverage,
                sumThresholdCoverage)) {
            // Trying to rewind windows by moving outside the search region
            for (int i = 0; i < windowSize; i++) {
                windowEndPosition -= scanIncrement;
                position -= scanIncrement;
                if ((windowEndPosition < 0) || (windowEndPosition >= quality.size())) {
                    // Failed to rewind and quit the good quality region by window average value criteria
                    while ((position >= leftmostPosition) && (position < rightmostPosition)
                            && checkQualityAndCoverage(quality.value(position), coverage[position],
                            this.qualityThreshold, this.qualityThresholdForLowCoverage, this.lowCoverageThreshold))
                        position -= scanIncrement;
                    return position;
                }
                sumQuality += quality.value(windowEndPosition);
                sumQuality -= quality.value(position);
                sumCoverage += coverage[windowEndPosition];
                sumCoverage -= coverage[position];
                if (!checkQualityAndCoverage(sumQuality, sumCoverage,
                        sumThresholdQuality, sumThresholdQualityForLowCoverage, sumThresholdCoverage)) {
                    // Found where window exited the good region, searching for bad letter value inside the window
                    int positionInsideWindow = i;
                    while (checkQualityAndCoverage(quality.value(position), coverage[position],
                            this.qualityThreshold, this.qualityThresholdForLowCoverage, this.lowCoverageThreshold)
                            && (positionInsideWindow < windowSize)) {
                        position -= scanIncrement;
                        positionInsideWindow++;
                    }
                    return position;
                }
            }
            return position - scanIncrement;
        }

        // Main search pass by window average value criteria
        while (!checkQualityAndCoverage(sumQuality, sumCoverage,
                sumThresholdQuality, sumThresholdQualityForLowCoverage, sumThresholdCoverage)
                && (position >= leftmostPosition) && (position < rightmostPosition)) {
            sumQuality -= quality.value(windowEndPosition);
            sumQuality += quality.value(position);
            sumCoverage -= coverage[windowEndPosition];
            sumCoverage += coverage[position];
            windowEndPosition += scanIncrement;
            position += scanIncrement;
        }

        // Determine whether the search was successful
        boolean unsuccessful = !checkQualityAndCoverage(sumQuality, sumCoverage,
                sumThresholdQuality, sumThresholdQualityForLowCoverage, sumThresholdCoverage);

        // Searching for actual boundary of the region, reverse search by bad letter value criteria
        do {
            position -= scanIncrement;
        } while ((position >= leftmostPosition) && (position < rightmostPosition)
                && checkQualityAndCoverage(quality.value(position), coverage[position],
                this.qualityThreshold, this.qualityThresholdForLowCoverage, this.lowCoverageThreshold));

        return unsuccessful ? -2 - position : position;
    }

    /**
     * Returns true if coverage is good and quality is not lower than the threshold
     * OR if coverage is low and quality is not lower than the threshold for low coverage.
     */
    private boolean checkQualityAndCoverage(
            int quality, float coverage, float qualityThreshold, float qualityThresholdForLowCoverage,
            float lowCoverageThreshold) {
        if (coverage >= lowCoverageThreshold)
            return quality >= qualityThreshold;
        else
            return quality >= qualityThresholdForLowCoverage;
    }
}
