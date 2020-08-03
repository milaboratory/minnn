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

import static com.milaboratory.minnn.pattern.PatternUtils.invertCoordinate;

final class SimplePatternBorders {
    final int fixedLeftBorder;
    final int fixedRightBorder;
    final int fromWithBorder;
    final int toWithBorder;

    /**
     * Calculate left/right borders and from/to constraints for simple pattern (FuzzyMatch, Repeat etc) when
     * the target becomes known.
     *
     * @param targetLength      length of the target sequence, which comes as argument for match() function
     * @param from              from constraint, inclusive, comes as argument for match() function
     * @param to                to constraint, exclusive, comes as argument for match() function
     * @param fixedLeftBorder   left border of the pattern in the target, inclusive;
     *                          may be -1 if not specified or -2 - value if specified from the target's end;
     *                          comes as argument for pattern's constructor
     * @param fixedRightBorder  right border of the pattern in the target, inclusive;
     *                          may be -1 if not specified or -2 - value if specified from the target's end;
     *                          comes as argument for pattern's constructor
     */
    SimplePatternBorders(int targetLength, int from, int to, int fixedLeftBorder, int fixedRightBorder) {
        this.fixedLeftBorder = (fixedLeftBorder > -2) ? fixedLeftBorder
                : targetLength - 1 - invertCoordinate(fixedLeftBorder);
        this.fixedRightBorder = (fixedRightBorder > -2) ? fixedRightBorder
                : targetLength - 1 - invertCoordinate(fixedRightBorder);
        this.fromWithBorder = (this.fixedLeftBorder == -1) ? from : Math.max(from, this.fixedLeftBorder);
        // to is exclusive and fixedRightBorder is inclusive
        this.toWithBorder = (this.fixedRightBorder == -1) ? to : Math.min(to, this.fixedRightBorder + 1);
    }
}
