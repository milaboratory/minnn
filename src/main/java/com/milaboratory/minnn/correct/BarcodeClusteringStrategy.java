package com.milaboratory.minnn.correct;

import com.milaboratory.core.clustering.Cluster;
import com.milaboratory.core.clustering.ClusteringStrategy;
import com.milaboratory.core.mutations.Mutations;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.tree.NeighborhoodIterator;
import com.milaboratory.core.tree.TreeSearchParameters;
import com.milaboratory.minnn.stat.MutationProbability;

public final class BarcodeClusteringStrategy implements ClusteringStrategy<SequenceCounter, NucleotideSequence> {
    private final TreeSearchParameters treeSearchParameters;
    private final float threshold;
    private final int maxClusterDepth;
    private final MutationProbability mutationProbability;

    public BarcodeClusteringStrategy(int mismatches, int indels, int totalErrors, float threshold, int maxClusterDepth,
                                     MutationProbability mutationProbability) {
        this.treeSearchParameters = new TreeSearchParameters(mismatches, indels, indels, totalErrors);
        this.threshold = threshold;
        this.maxClusterDepth = maxClusterDepth;
        this.mutationProbability = mutationProbability;
    }

    @Override
    public boolean canAddToCluster(Cluster<SequenceCounter> cluster, SequenceCounter minorSequenceCounter,
                                   NeighborhoodIterator<NucleotideSequence, SequenceCounter[]> iterator) {
        Mutations<NucleotideSequence> currentMutations = iterator.getCurrentMutations();
        long majorClusterCount = cluster.getHead().count;
        long minorClusterCount = minorSequenceCounter.count;
        float expected = majorClusterCount;
        for (int mutationCode : currentMutations.getRAWMutations())
            expected *= mutationProbability.mutationProbability(mutationCode);
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
        return Long.compare(c1.count, c2.count);
    }
}
