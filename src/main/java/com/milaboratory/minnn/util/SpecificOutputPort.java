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
package com.milaboratory.minnn.util;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.minnn.pattern.MatchIntermediate;

import java.util.ArrayList;

final class SpecificOutputPort implements OutputPort<MatchIntermediate> {
    private final OutputPort<MatchIntermediate> port;
    private final ArrayList<MatchIntermediate> cachedMatches = new ArrayList<>();
    private final int operandIndex;
    private final int from;
    private final int to;
    private final int portLimit;
    private boolean finished = false;

    SpecificOutputPort(OutputPort<MatchIntermediate> port, int operandIndex, int from, int to, int portLimit) {
        this.port = port;
        this.operandIndex = operandIndex;
        this.from = from;
        this.to = to;
        this.portLimit = portLimit;
    }

    @Override
    public MatchIntermediate take() {
        if (finished)
            return null;
        MatchIntermediate match = port.take();
        if (match == null)
            finished = true;
        else {
            cachedMatches.add(match);
            if (cachedMatches.size() == portLimit)
                finished = true;
        }
        return match;
    }

    ArrayList<MatchIntermediate> takeAll(boolean nullMatchesAllowed) {
        ArrayList<MatchIntermediate> allMatches = new ArrayList<>();
        MatchIntermediate currentMatch;
        int index = 0;
        do {
            currentMatch = get(index);
            if ((currentMatch != null) || (nullMatchesAllowed && (index == 0)))
                allMatches.add(currentMatch);
            index++;
        } while (currentMatch != null);

        return allMatches;
    }

    MatchIntermediate get(int index) {
        if (index < cachedMatches.size())
            return cachedMatches.get(index);
        else if (index == cachedMatches.size())
            return take();
        else
            throw new IndexOutOfBoundsException("index: " + index + ", cachedMatches size: " + cachedMatches.size());
    }

    boolean paramsEqualTo(int operandIndex, int from, int to) {
        return (operandIndex == this.operandIndex) && (from == this.from) && (to == this.to);
    }

    boolean isFinished() {
        return finished;
    }
}
