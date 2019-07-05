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
package com.milaboratory.minnn.correct;

import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.clustering.Cluster;
import com.milaboratory.core.clustering.ClusteringStrategy;
import com.milaboratory.core.mutations.Mutation;
import com.milaboratory.core.mutations.MutationType;
import com.milaboratory.core.mutations.Mutations;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.tree.NeighborhoodIterator;
import com.milaboratory.core.tree.TreeSearchParameters;
import com.milaboratory.minnn.stat.MutationProbability;

import java.util.Objects;

final class BarcodeClusteringStrategy
        implements ClusteringStrategy<SequenceCounter, SequenceWithQualityForClustering> {
    private final TreeSearchParameters treeSearchParameters;
    private final float threshold;
    private final int maxClusterDepth;
    private final MutationProbability mutationProbability;

    BarcodeClusteringStrategy(TreeSearchParameters treeSearchParameters, float threshold, int maxClusterDepth,
                              MutationProbability mutationProbability) {
        this.treeSearchParameters = treeSearchParameters;
        this.threshold = threshold;
        this.maxClusterDepth = maxClusterDepth;
        this.mutationProbability = mutationProbability;
    }

    @Override
    public boolean canAddToCluster(
            Cluster<SequenceCounter> cluster, SequenceCounter minorSequenceCounter,
            NeighborhoodIterator<SequenceWithQualityForClustering, SequenceCounter[]> iterator) {
        Alignment<SequenceWithQualityForClustering> currentAlignment = iterator.getCurrentAlignment();
        Mutations<SequenceWithQualityForClustering> currentMutations = currentAlignment.getAbsoluteMutations();
        NSequenceWithQuality seq1 = currentAlignment.getSequence1().nSequenceWithQuality;
        NSequenceWithQuality seq2 = minorSequenceCounter.getSequence();
        long majorClusterCount = cluster.getHead().getCount();
        long minorClusterCount = minorSequenceCounter.getCount();
        float expected = majorClusterCount;
        for (int i = 0; i < currentMutations.size(); i++) {
            int position1;
            int position2;
            NSequenceWithQuality from;
            NSequenceWithQuality to;
            MutationType mutationType = Objects.requireNonNull(Mutation.getType(currentMutations.getMutation(i)));
            switch (mutationType) {
                case Substitution:
                    position1 = currentMutations.getPositionByIndex(i);
                    position2 = currentMutations.convertToSeq2Position(position1);
                    from = seq1.getRange(position1, position1 + 1);
                    to = seq2.getRange(position2, position2 + 1);
                    break;
                case Insertion:
                    position1 = currentMutations.getPositionByIndex(i);
                    position2 = currentMutations.convertToSeq2Position(position1) - 1;
                    from = NSequenceWithQuality.EMPTY;
                    to = seq2.getRange(position2, position2 + 1);
                    break;
                case Deletion:
                    position1 = currentMutations.getPositionByIndex(i);
                    from = seq1.getRange(position1, position1 + 1);
                    to = NSequenceWithQuality.EMPTY;
                    break;
                default:
                    throw new IllegalStateException("Wrong mutation type: " + mutationType);
            }
            expected *= mutationProbability.mutationProbability(from, to);
        }
        return (minorClusterCount <= expected) && ((float)minorClusterCount / majorClusterCount < threshold);
    }

    @Override
    public TreeSearchParameters getSearchParameters() {
        return treeSearchParameters;
    }

    @Override
    public int getMaxClusterDepth() {
        return maxClusterDepth;
    }

    @Override
    public int compare(SequenceCounter c1, SequenceCounter c2) {
        return c1.compareTo(c2);
    }
}
