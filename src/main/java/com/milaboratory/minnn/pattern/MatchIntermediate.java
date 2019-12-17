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

import com.milaboratory.core.Range;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import java.util.*;

public final class MatchIntermediate extends Match {
    private final int leftUppercaseDistance;
    private final int rightUppercaseDistance;
    private final MatchedRange[] matchedRanges;
    private HashMap<Integer, ArrayList<MatchedGroupEdge>> edgesByPatternCache = null;

    /**
     * Single match for single- or multi-pattern. This match holds some intermediate variables that used in patterns.
     * That variables are not serialized when final match is found.
     *
     * @param numberOfPatterns       number of patterns in multi-pattern, or 1 if it is single pattern
     * @param score                  match score
     * @param leftUppercaseDistance  first uppercase letter position; used for calculating max overlap and insertion
     *                               with other match; -1 means no restrictions for overlaps and insertions
     * @param rightUppercaseDistance first uppercase letter position if count from right to left of this match;
     *                               or -1 for no overlap and insertion restrictions
     * @param matchedGroupEdges      list of matched group edges
     * @param matchedRanges          array of matched ranges for every pattern; size must be equal to numberOfPatterns
     */
    public MatchIntermediate(int numberOfPatterns, long score, int leftUppercaseDistance, int rightUppercaseDistance,
                             ArrayList<MatchedGroupEdge> matchedGroupEdges, MatchedRange... matchedRanges) {
        super(numberOfPatterns, score, matchedGroupEdges);
        if (matchedRanges.length == 0) throw new IllegalArgumentException("Missing matched ranges!");
        this.leftUppercaseDistance = leftUppercaseDistance;
        this.rightUppercaseDistance = rightUppercaseDistance;
        this.matchedRanges = matchedRanges;
    }

    /**
     * Return MatchedRange by pattern index.
     *
     * @param patternIndex pattern index for multi-pattern matchers (number of patterns may be bigger than number of
     *                     targets in case of high level logic patterns); 0 - for single target matchers
     * @return MatchedRange for specified pattern
     */
    public MatchedRange getMatchedRange(int patternIndex) {
        return matchedRanges[patternIndex];
    }

    /**
     * Return MatchedRange. Applicable only to single target matchers.
     *
     * @return MatchedRange with patternIndex 0
     */
    public MatchedRange getMatchedRange() {
        if (numberOfTargets != 1)
            throw new IllegalStateException("Multiple pattern. Use getMatchedRange(int) instead.");
        return matchedRanges[0];
    }

    public Range getRange() {
        return getMatchedRange().getRange();
    }

    public NSequenceWithQuality getValue() {
        return getMatchedRange().getValue();
    }

    /**
     * Get all matched ranges.
     *
     * @return array of all matched ranges.
     */
    public MatchedRange[] getMatchedRanges() {
        return matchedRanges;
    }

    public int getLeftUppercaseDistance() {
        return leftUppercaseDistance;
    }

    public int getRightUppercaseDistance() {
        return rightUppercaseDistance;
    }

    /**
     * Return ArrayList of matched group edges with specified pattern index.
     *
     * @param patternIndex pattern index; group edges with this index will be searched
     * @return ArrayList of matched group edges with specified pattern index
     */
    public ArrayList<MatchedGroupEdge> getMatchedGroupEdgesByPattern(int patternIndex) {
        if (edgesByPatternCache == null) {
            edgesByPatternCache = new HashMap<>();
            for (MatchedGroupEdge matchedGroupEdge : matchedGroupEdges) {
                int currentPatternIndex = matchedGroupEdge.getPatternIndex();
                edgesByPatternCache.computeIfAbsent(currentPatternIndex, k -> new ArrayList<>());
                edgesByPatternCache.get(currentPatternIndex).add(matchedGroupEdge);
            }
        }

        edgesByPatternCache.computeIfAbsent(patternIndex, k -> new ArrayList<>());
        return edgesByPatternCache.get(patternIndex);
    }
}
