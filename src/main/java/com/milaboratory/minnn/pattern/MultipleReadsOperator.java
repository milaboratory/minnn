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
package com.milaboratory.minnn.pattern;

import java.util.*;

public abstract class MultipleReadsOperator extends Pattern {
    protected final MultipleReadsOperator[] operandPatterns;
    protected final SinglePattern[] singlePatterns;
    private final boolean singlePatternOperands;
    private LinkedHashSet<GroupEdge> groupEdges = null;

    MultipleReadsOperator(PatternAligner patternAligner, boolean defaultGroupsOverride,
                          MultipleReadsOperator... operandPatterns) {
        super(patternAligner, defaultGroupsOverride);
        this.operandPatterns = operandPatterns;
        this.singlePatterns = new SinglePattern[0];
        this.singlePatternOperands = false;
    }

    MultipleReadsOperator(PatternAligner patternAligner, boolean defaultGroupsOverride,
                          SinglePattern... singlePatterns) {
        super(patternAligner, defaultGroupsOverride);
        this.singlePatterns = singlePatterns;
        this.operandPatterns = new MultipleReadsOperator[0];
        this.singlePatternOperands = true;
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        if (groupEdges == null) {
            groupEdges = new LinkedHashSet<>();
            for (Pattern pattern : singlePatternOperands ? singlePatterns : operandPatterns)
                groupEdges.addAll(pattern.getGroupEdges());
        }
        return new ArrayList<>(groupEdges);
    }

    public int getNumberOfPatterns() {
        return Math.max(singlePatterns.length, operandPatterns.length);
    }
}
