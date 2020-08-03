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

import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.clustering.Cluster;
import com.milaboratory.core.clustering.ClusteringStrategy;
import com.milaboratory.core.mutations.Mutation;
import com.milaboratory.core.mutations.MutationType;
import com.milaboratory.core.mutations.Mutations;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.tree.MutationGuide;
import com.milaboratory.core.tree.NeighborhoodIterator;
import com.milaboratory.core.tree.TreeSearchParameters;
import com.milaboratory.minnn.stat.SimpleMutationProbability;

import java.util.Objects;

import static com.milaboratory.core.sequence.NucleotideSequence.ALPHABET;

final class BarcodeClusteringStrategy
        implements ClusteringStrategy<SequenceWithQualityAndCount, SequenceWithQualityForClustering> {
    private final CorrectionStats stats = new CorrectionStats();
    private final TreeSearchParameters treeSearchParameters;
    private final float threshold;
    private final int maxClusterDepth;
    private final SimpleMutationProbability mutationProbability;
    private final float indelProbability;

    BarcodeClusteringStrategy(TreeSearchParameters treeSearchParameters, float threshold, int maxClusterDepth,
                              SimpleMutationProbability mutationProbability) {
        this.treeSearchParameters = treeSearchParameters;
        this.threshold = threshold;
        this.maxClusterDepth = maxClusterDepth;
        this.mutationProbability = mutationProbability;
        this.indelProbability = mutationProbability.mutationProbability((byte)-1, (byte)0, (byte)0, (byte)0);
    }

    @Override
    public boolean canAddToCluster(
            Cluster<SequenceWithQualityAndCount> cluster, SequenceWithQualityAndCount minorSequenceCounter,
            NeighborhoodIterator<SequenceWithQualityForClustering, SequenceWithQualityAndCount[]> iterator) {
        stats.barcodeCanAddToClusterCalls++;
        Alignment<SequenceWithQualityForClustering> currentAlignment = iterator.getCurrentAlignment();
        Mutations<SequenceWithQualityForClustering> currentMutations = currentAlignment.getAbsoluteMutations();
        NSequenceWithQuality seq1 = currentAlignment.getSequence1().nSequenceWithQuality;
        NSequenceWithQuality seq2 = minorSequenceCounter.seq;
        long majorClusterCount = cluster.getHead().count;
        long minorClusterCount = minorSequenceCounter.count;
        float expected = majorClusterCount;
        boolean equalByWildcards = currentMutations.countOfIndels() == 0;
        for (int i = 0; i < currentMutations.size(); i++) {
            MutationType mutationType = Objects.requireNonNull(Mutation.getType(currentMutations.getMutation(i)));
            switch (mutationType) {
                case Substitution:
                    int position1 = currentMutations.getPositionByIndex(i);
                    int position2 = currentMutations.convertToSeq2Position(position1);
                    byte from = seq1.getSequence().codeAt(position1);
                    byte fromQual = seq1.getQuality().value(position1);
                    byte to = seq2.getSequence().codeAt(position2);
                    byte toQual = seq2.getQuality().value(position2);
                    expected *= mutationProbability.mutationProbability(from, fromQual, to, toQual);
                    equalByWildcards &= ALPHABET.codeToWildcard(from).intersectsWith(ALPHABET.codeToWildcard(to));
                    break;
                case Insertion:
                case Deletion:
                    expected *= indelProbability;
                    break;
                default:
                    throw new IllegalStateException("Wrong mutation type: " + mutationType);
            }
        }

        boolean canAddByExpectedCount = minorClusterCount <= expected;
        boolean canAddByThreshold = (float)minorClusterCount / majorClusterCount < threshold;
        if (equalByWildcards)
            stats.barcodeClusterNotAddedByWildcards++;
        else if (!canAddByExpectedCount)
            stats.barcodeClusterNotAddedByExpectedCount++;
        else if (!canAddByThreshold)
            stats.barcodeClusterNotAddedByThreshold++;
        return !equalByWildcards && canAddByExpectedCount && canAddByThreshold;
    }

    @Override
    public TreeSearchParameters getSearchParameters(Cluster<SequenceWithQualityAndCount> cluster) {
        return treeSearchParameters;
    }

    @Override
    public MutationGuide<SequenceWithQualityForClustering> getMutationGuide(
            Cluster<SequenceWithQualityAndCount> cluster) {
        return MutationGuideForClustering.INSTANCE;
    }

    @Override
    public int getMaxClusterDepth() {
        return maxClusterDepth;
    }

    @Override
    public int compare(SequenceWithQualityAndCount s1, SequenceWithQualityAndCount s2) {
        return Long.compare(s1.count, s2.count);
    }

    CorrectionStats getStats() {
        return stats;
    }
}
