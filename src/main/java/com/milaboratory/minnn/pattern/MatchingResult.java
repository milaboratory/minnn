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
package com.milaboratory.minnn.pattern;

import cc.redberry.pipe.OutputPort;

public interface MatchingResult {
    /**
     * Get iterator for all match results
     *
     * @param fairSorting true if we need fair sorting, otherwise false
     * @return OutputPort iterator for all match results
     */
    OutputPort<MatchIntermediate> getMatches(boolean fairSorting);

    /**
     * Get iterator for all match results
     *
     * @return OutputPort iterator for all match results
     */
    default OutputPort<MatchIntermediate> getMatches() {
        return getMatches(false);
    }

    /**
     * Get best matching result
     *
     * @param fairSorting true if we need fair sorting, otherwise false
     * @return best matching result
     */
    default MatchIntermediate getBestMatch(boolean fairSorting) {
        return getMatches(fairSorting).take();
    }

    /**
     * Get best matching result
     *
     * @return best matching result
     */
    default MatchIntermediate getBestMatch() {
        return getBestMatch(false);
    }

    /**
     * Check is pattern matched or not
     *
     * @return true if pattern matched
     */
    default boolean isFound() {
        return getMatches(false).take() != null;
    }
}
