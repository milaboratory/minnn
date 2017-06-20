package com.milaboratory.mist.pattern;

public final class ScoreFilter implements Filter {
    private final long scoreThreshold;

    public ScoreFilter(long scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }

    @Override
    public String toString() {
        return "ScoreFilter(" + scoreThreshold + ")";
    }

    @Override
    public Match checkMatch(Match match) {
        if (match.getScore() < scoreThreshold)
            return null;
        else return match;
    }

    public long getScoreThreshold() {
        return scoreThreshold;
    }
}
