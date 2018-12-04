package com.milaboratory.minnn.cli;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.milaboratory.cli.ActionConfiguration;
import com.milaboratory.primitivio.annotations.Serializable;
import com.milaboratory.util.GlobalObjectMappers;

import java.util.List;

import static com.milaboratory.minnn.cli.ConsensusAction.CONSENSUS_ACTION_NAME;

public final class ConsensusActionConfiguration implements ActionConfiguration {
    private static final String CONSENSUS_ACTION_VERSION_ID = "1";
    private final ConsensusActionParameters consensusParameters;

    @JsonCreator
    public ConsensusActionConfiguration(
            @JsonProperty("consensusParameters") ConsensusActionParameters consensusParameters) {
        this.consensusParameters = consensusParameters;
    }

    @Override
    public String actionName() {
        return CONSENSUS_ACTION_NAME;
    }

    @Override
    public String versionId() {
        return CONSENSUS_ACTION_VERSION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;
        ConsensusActionConfiguration that = (ConsensusActionConfiguration)o;
        return consensusParameters.equals(that.consensusParameters);
    }

    @Override
    public int hashCode() {
        return consensusParameters.hashCode();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE)
    @Serializable(asJson = true)
    public static final class ConsensusActionParameters implements java.io.Serializable {
        private List<String> groupList;
        private int alignerWidth;
        private int matchScore;
        private int mismatchScore;
        private int gapScore;
        private long goodQualityMismatchPenalty;
        private byte goodQualityMismatchThreshold;
        private long scoreThreshold;
        private float skippedFractionToRepeat;
        private int maxConsensusesPerCluster;
        private int readsMinGoodSeqLength;
        private float readsAvgQualityThreshold;
        private int readsTrimWindowSize;
        private int minGoodSeqLength;
        private float avgQualityThreshold;
        private int trimWindowSize;
        private String notUsedReadsOutputFileName;
        private boolean toSeparateGroups;
        private long inputReadsLimit;

        public ConsensusActionParameters(
                @JsonProperty("groupList") List<String> groupList,
                @JsonProperty("alignerWidth") int alignerWidth,
                @JsonProperty("matchScore") int matchScore,
                @JsonProperty("mismatchScore") int mismatchScore,
                @JsonProperty("gapScore") int gapScore,
                @JsonProperty("goodQualityMismatchPenalty") long goodQualityMismatchPenalty,
                @JsonProperty("goodQualityMismatchThreshold") byte goodQualityMismatchThreshold,
                @JsonProperty("scoreThreshold") long scoreThreshold,
                @JsonProperty("skippedFractionToRepeat") float skippedFractionToRepeat,
                @JsonProperty("maxConsensusesPerCluster") int maxConsensusesPerCluster,
                @JsonProperty("readsMinGoodSeqLength") int readsMinGoodSeqLength,
                @JsonProperty("readsAvgQualityThreshold") float readsAvgQualityThreshold,
                @JsonProperty("readsTrimWindowSize") int readsTrimWindowSize,
                @JsonProperty("minGoodSeqLength") int minGoodSeqLength,
                @JsonProperty("avgQualityThreshold") float avgQualityThreshold,
                @JsonProperty("trimWindowSize") int trimWindowSize,
                @JsonProperty("notUsedReadsOutputFileName") String notUsedReadsOutputFileName,
                @JsonProperty("toSeparateGroups") boolean toSeparateGroups,
                @JsonProperty("inputReadsLimit") long inputReadsLimit) {
            this.groupList = groupList;
            this.alignerWidth = alignerWidth;
            this.matchScore = matchScore;
            this.mismatchScore = mismatchScore;
            this.gapScore = gapScore;
            this.goodQualityMismatchPenalty = goodQualityMismatchPenalty;
            this.goodQualityMismatchThreshold = goodQualityMismatchThreshold;
            this.scoreThreshold = scoreThreshold;
            this.skippedFractionToRepeat = skippedFractionToRepeat;
            this.maxConsensusesPerCluster = maxConsensusesPerCluster;
            this.readsMinGoodSeqLength = readsMinGoodSeqLength;
            this.readsAvgQualityThreshold = readsAvgQualityThreshold;
            this.readsTrimWindowSize = readsTrimWindowSize;
            this.minGoodSeqLength = minGoodSeqLength;
            this.avgQualityThreshold = avgQualityThreshold;
            this.trimWindowSize = trimWindowSize;
            this.notUsedReadsOutputFileName = notUsedReadsOutputFileName;
            this.toSeparateGroups = toSeparateGroups;
            this.inputReadsLimit = inputReadsLimit;
        }

        public List<String> getGroupList() {
            return groupList;
        }

        public void setGroupList(List<String> groupList) {
            this.groupList = groupList;
        }

        public int getAlignerWidth() {
            return alignerWidth;
        }

        public void setAlignerWidth(int alignerWidth) {
            this.alignerWidth = alignerWidth;
        }

        public int getMatchScore() {
            return matchScore;
        }

        public void setMatchScore(int matchScore) {
            this.matchScore = matchScore;
        }

        public int getMismatchScore() {
            return mismatchScore;
        }

        public void setMismatchScore(int mismatchScore) {
            this.mismatchScore = mismatchScore;
        }

        public int getGapScore() {
            return gapScore;
        }

        public void setGapScore(int gapScore) {
            this.gapScore = gapScore;
        }

        public long getGoodQualityMismatchPenalty() {
            return goodQualityMismatchPenalty;
        }

        public void setGoodQualityMismatchPenalty(long goodQualityMismatchPenalty) {
            this.goodQualityMismatchPenalty = goodQualityMismatchPenalty;
        }

        public byte getGoodQualityMismatchThreshold() {
            return goodQualityMismatchThreshold;
        }

        public void setGoodQualityMismatchThreshold(byte goodQualityMismatchThreshold) {
            this.goodQualityMismatchThreshold = goodQualityMismatchThreshold;
        }

        public long getScoreThreshold() {
            return scoreThreshold;
        }

        public void setScoreThreshold(long scoreThreshold) {
            this.scoreThreshold = scoreThreshold;
        }

        public float getSkippedFractionToRepeat() {
            return skippedFractionToRepeat;
        }

        public void setSkippedFractionToRepeat(float skippedFractionToRepeat) {
            this.skippedFractionToRepeat = skippedFractionToRepeat;
        }

        public int getMaxConsensusesPerCluster() {
            return maxConsensusesPerCluster;
        }

        public void setMaxConsensusesPerCluster(int maxConsensusesPerCluster) {
            this.maxConsensusesPerCluster = maxConsensusesPerCluster;
        }

        public int getReadsMinGoodSeqLength() {
            return readsMinGoodSeqLength;
        }

        public void setReadsMinGoodSeqLength(int readsMinGoodSeqLength) {
            this.readsMinGoodSeqLength = readsMinGoodSeqLength;
        }

        public float getReadsAvgQualityThreshold() {
            return readsAvgQualityThreshold;
        }

        public void setReadsAvgQualityThreshold(float readsAvgQualityThreshold) {
            this.readsAvgQualityThreshold = readsAvgQualityThreshold;
        }

        public int getReadsTrimWindowSize() {
            return readsTrimWindowSize;
        }

        public void setReadsTrimWindowSize(int readsTrimWindowSize) {
            this.readsTrimWindowSize = readsTrimWindowSize;
        }

        public int getMinGoodSeqLength() {
            return minGoodSeqLength;
        }

        public void setMinGoodSeqLength(int minGoodSeqLength) {
            this.minGoodSeqLength = minGoodSeqLength;
        }

        public float getAvgQualityThreshold() {
            return avgQualityThreshold;
        }

        public void setAvgQualityThreshold(float avgQualityThreshold) {
            this.avgQualityThreshold = avgQualityThreshold;
        }

        public int getTrimWindowSize() {
            return trimWindowSize;
        }

        public void setTrimWindowSize(int trimWindowSize) {
            this.trimWindowSize = trimWindowSize;
        }

        public String getNotUsedReadsOutputFileName() {
            return notUsedReadsOutputFileName;
        }

        public void setNotUsedReadsOutputFileName(String notUsedReadsOutputFileName) {
            this.notUsedReadsOutputFileName = notUsedReadsOutputFileName;
        }

        public boolean isToSeparateGroups() {
            return toSeparateGroups;
        }

        public void setToSeparateGroups(boolean toSeparateGroups) {
            this.toSeparateGroups = toSeparateGroups;
        }

        public long getInputReadsLimit() {
            return inputReadsLimit;
        }

        public void setInputReadsLimit(long inputReadsLimit) {
            this.inputReadsLimit = inputReadsLimit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConsensusActionParameters that = (ConsensusActionParameters)o;
            if (alignerWidth != that.alignerWidth) return false;
            if (matchScore != that.matchScore) return false;
            if (mismatchScore != that.mismatchScore) return false;
            if (gapScore != that.gapScore) return false;
            if (goodQualityMismatchPenalty != that.goodQualityMismatchPenalty) return false;
            if (goodQualityMismatchThreshold != that.goodQualityMismatchThreshold) return false;
            if (scoreThreshold != that.scoreThreshold) return false;
            if (Float.compare(that.skippedFractionToRepeat, skippedFractionToRepeat) != 0) return false;
            if (maxConsensusesPerCluster != that.maxConsensusesPerCluster) return false;
            if (readsMinGoodSeqLength != that.readsMinGoodSeqLength) return false;
            if (Float.compare(that.readsAvgQualityThreshold, readsAvgQualityThreshold) != 0) return false;
            if (readsTrimWindowSize != that.readsTrimWindowSize) return false;
            if (minGoodSeqLength != that.minGoodSeqLength) return false;
            if (Float.compare(that.avgQualityThreshold, avgQualityThreshold) != 0) return false;
            if (trimWindowSize != that.trimWindowSize) return false;
            if (toSeparateGroups != that.toSeparateGroups) return false;
            if (inputReadsLimit != that.inputReadsLimit) return false;
            if (groupList != null ? !groupList.equals(that.groupList) : that.groupList != null) return false;
            return notUsedReadsOutputFileName != null
                    ? notUsedReadsOutputFileName.equals(that.notUsedReadsOutputFileName)
                    : that.notUsedReadsOutputFileName == null;
        }

        @Override
        public int hashCode() {
            int result = groupList != null ? groupList.hashCode() : 0;
            result = 31 * result + alignerWidth;
            result = 31 * result + matchScore;
            result = 31 * result + mismatchScore;
            result = 31 * result + gapScore;
            result = 31 * result + (int)(goodQualityMismatchPenalty ^ (goodQualityMismatchPenalty >>> 32));
            result = 31 * result + (int)goodQualityMismatchThreshold;
            result = 31 * result + (int)(scoreThreshold ^ (scoreThreshold >>> 32));
            result = 31 * result + (skippedFractionToRepeat != +0.0f ? Float.floatToIntBits(skippedFractionToRepeat)
                    : 0);
            result = 31 * result + maxConsensusesPerCluster;
            result = 31 * result + readsMinGoodSeqLength;
            result = 31 * result + (readsAvgQualityThreshold != +0.0f ? Float.floatToIntBits(readsAvgQualityThreshold)
                    : 0);
            result = 31 * result + readsTrimWindowSize;
            result = 31 * result + minGoodSeqLength;
            result = 31 * result + (avgQualityThreshold != +0.0f ? Float.floatToIntBits(avgQualityThreshold) : 0);
            result = 31 * result + trimWindowSize;
            result = 31 * result + (notUsedReadsOutputFileName != null ? notUsedReadsOutputFileName.hashCode() : 0);
            result = 31 * result + (toSeparateGroups ? 1 : 0);
            result = 31 * result + (int)(inputReadsLimit ^ (inputReadsLimit >>> 32));
            return result;
        }

        @Override
        public String toString() {
            try {
                return GlobalObjectMappers.PRETTY.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                throw new RuntimeException();
            }
        }
    }
}
