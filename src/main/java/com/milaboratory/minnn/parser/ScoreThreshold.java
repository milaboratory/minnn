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
package com.milaboratory.minnn.parser;

final class ScoreThreshold {
    final int start;
    final int end;
    final long threshold;
    final int nestedLevel;

    /**
     * Score threshold; used to detect that patterns inside it must use more strict score threshold than final pattern.
     *
     * @param threshold score threshold value
     * @param start start position in TokenizedString, inclusive
     * @param end end position in TokenizedString, exclusive
     * @param nestedLevel number of score thresholds outside of this threshold;
     *                    threshold with higher nestedLevel has higher priority
     */
    ScoreThreshold(long threshold, int start, int end, int nestedLevel) {
        this.threshold = threshold;
        this.start = start;
        this.end = end;
        this.nestedLevel = nestedLevel;
    }

    /**
     * Returns true if the specified range is inside this score threshold.
     *
     * @param start range start, inclusive
     * @param end range end, exclusive
     * @return true if the specified range is inside this score threshold
     */
    boolean contains(int start, int end) {
        if ((start >= this.start) && (end <= this.end))
            return true;
        else if (((start < this.start) && (end <= this.start)) || (start >= this.end)
                || ((start <= this.start) && (end >= this.end)))
            return false;
        else throw new IllegalStateException("Intersection of specified range and this ScoreThreshold: this.start="
                + this.start + ", this.end=" + this.end + ", start=" + start + ", end=" + end);
    }
}
