package com.milaboratory.minnn.correct;

public final class CorrectionStats {
    public final long totalReads;
    public final long correctedReads;
    public final long excludedReads;

    public CorrectionStats(long totalReads, long correctedReads, long excludedReads) {
        this.totalReads = totalReads;
        this.correctedReads = correctedReads;
        this.excludedReads = excludedReads;
    }
}
