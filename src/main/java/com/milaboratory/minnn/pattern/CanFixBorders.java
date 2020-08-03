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

public interface CanFixBorders {
    /**
     * Set fixed left or right border for this pattern.
     *
     * @param left      true for left border, false for right border
     * @param position  coordinate for fixed border
     * @return          copy of this pattern with fixed border
     */
    SinglePattern fixBorder(boolean left, int position);

    default LeftAndRightBorders prepareNewBorders(
            boolean left, int position, int oldLeftBorder, int oldRightBorder, String patternString) {
        int newLeftBorder = oldLeftBorder;
        int newRightBorder = oldRightBorder;
        if (left) {
            if (newLeftBorder == -1)
                newLeftBorder = position;
            else if (newLeftBorder != position)
                throw new IllegalStateException(patternString + ": trying to set fixed left border to " + position
                        + " when it is already fixed at " + newLeftBorder + "!");
        } else {
            if (newRightBorder == -1)
                newRightBorder = position;
            else if (newRightBorder != position)
                throw new IllegalStateException(patternString + ": trying to set fixed right border to " + position
                        + " when it is already fixed at " + newRightBorder + "!");
        }
        return new LeftAndRightBorders(newLeftBorder, newRightBorder);
    }
}
