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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

abstract class MultiplePatternsOperator extends SinglePattern {
    protected final SinglePattern[] operandPatterns;
    protected final ArrayList<GroupEdge> groupEdges;

    MultiplePatternsOperator(PatternAligner patternAligner, boolean defaultGroupsOverride,
                             SinglePattern... operandPatterns) {
        this(patternAligner, defaultGroupsOverride, true, operandPatterns);
    }

    /**
     * Common constructor for multiple patterns operator.
     *
     * @param patternAligner pattern aligner; it also provides information about scoring and pattern overlap limits
     * @param defaultGroupsOverride true if there is default groups override in any pattern in the query
     * @param checkGroupEdges true if check that operands contain equal group edges must be performed
     * @param operandPatterns patterns that come as operands for the operator
     */
    MultiplePatternsOperator(PatternAligner patternAligner, boolean defaultGroupsOverride, boolean checkGroupEdges,
                             SinglePattern... operandPatterns) {
        super(patternAligner, defaultGroupsOverride);
        this.operandPatterns = operandPatterns;
        this.groupEdges = new ArrayList<>();
        for (SinglePattern pattern : operandPatterns)
            groupEdges.addAll(pattern.getGroupEdges());
        if (checkGroupEdges && (groupEdges.size() != new HashSet<>(groupEdges).size()))
            throw new IllegalStateException("Operands contain equal group edges!");
    }

    @Override
    public ArrayList<GroupEdge> getGroupEdges() {
        return groupEdges;
    }

    @Override
    void setTargetId(byte targetId) {
        super.setTargetId(targetId);
        Arrays.stream(operandPatterns).forEach(sp -> sp.setTargetId(targetId));
    }
}
