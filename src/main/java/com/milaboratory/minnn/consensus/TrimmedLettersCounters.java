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

import gnu.trove.map.hash.TByteIntHashMap;

import java.util.Objects;

public final class TrimmedLettersCounters {
    private final TByteIntHashMap counterByTargetId;

    public TrimmedLettersCounters(boolean collectStatsForOriginalReads, int numberOfTargets) {
        this.counterByTargetId = collectStatsForOriginalReads ? getEmptyCounters(numberOfTargets) : null;
    }

    private static TByteIntHashMap getEmptyCounters(int numberOfTargets) {
        TByteIntHashMap trimmedBadQualityLetters = new TByteIntHashMap();
        for (byte targetId = 1; targetId <= numberOfTargets; targetId++)
            trimmedBadQualityLetters.put(targetId, 0);
        return trimmedBadQualityLetters;
    }

    public void setCountByTargetId(byte targetId, int trimmedBadQualityLetters) {
        if (counterByTargetId != null)
            counterByTargetId.put(targetId, trimmedBadQualityLetters);
    }

    public int getCountByTargetId(byte targetId) {
        return Objects.requireNonNull(counterByTargetId).get(targetId);
    }
}
