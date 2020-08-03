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
package com.milaboratory.minnn.correct;

import com.milaboratory.core.clustering.Cluster;
import com.milaboratory.core.clustering.ClusteringStrategy;
import com.milaboratory.core.tree.MutationGuide;
import com.milaboratory.core.tree.NeighborhoodIterator;
import com.milaboratory.core.tree.TreeSearchParameters;

final class WildcardClusteringStrategy
        implements ClusteringStrategy<SequenceWithWildcardsCount, SequenceWithQualityForClustering> {
    private final CorrectionStats stats = new CorrectionStats();
    private final float wildcardsCollapsingMergeThreshold;

    WildcardClusteringStrategy(float wildcardsCollapsingMergeThreshold) {
        this.wildcardsCollapsingMergeThreshold = wildcardsCollapsingMergeThreshold;
    }

    @Override
    public boolean canAddToCluster(
            Cluster<SequenceWithWildcardsCount> cluster, SequenceWithWildcardsCount minorSequenceCounter,
            NeighborhoodIterator<SequenceWithQualityForClustering, SequenceWithWildcardsCount[]> iterator) {
        stats.wildcardCanAddToClusterCalls++;
        // major cluster can have lower count if head contains less wildcards than minorSequenceCounter
        long majorClusterCount = cluster.getHead().count;
        long minorClusterCount = minorSequenceCounter.count;
        boolean canAddByThreshold = majorClusterCount * wildcardsCollapsingMergeThreshold >= minorClusterCount;
        if (!canAddByThreshold)
            stats.wildcardClusterNotAddedByThreshold++;
        return canAddByThreshold;
    }

    @Override
    public TreeSearchParameters getSearchParameters(Cluster<SequenceWithWildcardsCount> cluster) {
        int sequenceLength = cluster.getHead().seq.size();
        return new TreeSearchParameters(sequenceLength, 0, 0, sequenceLength);
    }

    @Override
    public MutationGuide<SequenceWithQualityForClustering> getMutationGuide(
            Cluster<SequenceWithWildcardsCount> cluster) {
        return MutationGuideForWildcards.INSTANCE;
    }

    @Override
    public int getMaxClusterDepth() {
        return 1;
    }

    @Override
    public int compare(SequenceWithWildcardsCount s1, SequenceWithWildcardsCount s2) {
        int result = -Long.compare(s1.wildcardsCount, s2.wildcardsCount);
        if (result == 0)
            result = Long.compare(s1.count, s2.count);
        return result;
    }

    CorrectionStats getStats() {
        return stats;
    }
}
