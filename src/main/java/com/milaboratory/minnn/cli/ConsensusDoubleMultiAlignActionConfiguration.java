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
package com.milaboratory.minnn.cli;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.milaboratory.cli.ActionConfiguration;
import com.milaboratory.primitivio.annotations.Serializable;
import com.milaboratory.util.GlobalObjectMappers;

import java.util.List;
import java.util.Objects;

import static com.milaboratory.minnn.cli.ConsensusDoubleMultiAlignAction.CONSENSUS_DOUBLE_MULTI_ALIGN_ACTION_NAME;

public final class ConsensusDoubleMultiAlignActionConfiguration implements ActionConfiguration {
    private static final String CONSENSUS_DOUBLE_MULTI_ALIGN_ACTION_VERSION_ID = "3";
    private final ConsensusDoubleMultiAlignActionParameters consensusParameters;

    @JsonCreator
    public ConsensusDoubleMultiAlignActionConfiguration(
            @JsonProperty("consensusParameters") ConsensusDoubleMultiAlignActionParameters consensusParameters) {
        this.consensusParameters = consensusParameters;
    }

    @Override
    public String actionName() {
        return CONSENSUS_DOUBLE_MULTI_ALIGN_ACTION_NAME;
    }

    @Override
    public String versionId() {
        return CONSENSUS_DOUBLE_MULTI_ALIGN_ACTION_VERSION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;
        ConsensusDoubleMultiAlignActionConfiguration that = (ConsensusDoubleMultiAlignActionConfiguration)o;
        return consensusParameters.equals(that.consensusParameters);
    }

    @Override
    public int hashCode() {
        return consensusParameters.hashCode();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE)
    @Serializable(asJson = true)
    public static final class ConsensusDoubleMultiAlignActionParameters implements java.io.Serializable {
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
        private boolean dropOversizedClusters;
        private int readsMinGoodSeqLength;
        private float readsAvgQualityThreshold;
        private int readsTrimWindowSize;
        private int minGoodSeqLength;
        private float lowCoverageThreshold;
        private float avgQualityThreshold;
        private float avgQualityThresholdForLowCoverage;
        private int trimWindowSize;
        private boolean toSeparateGroups;
        private long inputReadsLimit;

        public ConsensusDoubleMultiAlignActionParameters(
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
                @JsonProperty("dropOversizedClusters") boolean dropOversizedClusters,
                @JsonProperty("readsMinGoodSeqLength") int readsMinGoodSeqLength,
                @JsonProperty("readsAvgQualityThreshold") float readsAvgQualityThreshold,
                @JsonProperty("readsTrimWindowSize") int readsTrimWindowSize,
                @JsonProperty("minGoodSeqLength") int minGoodSeqLength,
                @JsonProperty("lowCoverageThreshold") float lowCoverageThreshold,
                @JsonProperty("avgQualityThreshold") float avgQualityThreshold,
                @JsonProperty("avgQualityThresholdForLowCoverage") float avgQualityThresholdForLowCoverage,
                @JsonProperty("trimWindowSize") int trimWindowSize,
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
            this.dropOversizedClusters = dropOversizedClusters;
            this.readsMinGoodSeqLength = readsMinGoodSeqLength;
            this.readsAvgQualityThreshold = readsAvgQualityThreshold;
            this.readsTrimWindowSize = readsTrimWindowSize;
            this.minGoodSeqLength = minGoodSeqLength;
            this.lowCoverageThreshold = lowCoverageThreshold;
            this.avgQualityThreshold = avgQualityThreshold;
            this.avgQualityThresholdForLowCoverage = avgQualityThresholdForLowCoverage;
            this.trimWindowSize = trimWindowSize;
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

        public boolean isDropOversizedClusters() {
            return dropOversizedClusters;
        }

        public void setDropOversizedClusters(boolean dropOversizedClusters) {
            this.dropOversizedClusters = dropOversizedClusters;
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

        public float getLowCoverageThreshold() {
            return lowCoverageThreshold;
        }

        public void setLowCoverageThreshold(float lowCoverageThreshold) {
            this.lowCoverageThreshold = lowCoverageThreshold;
        }

        public float getAvgQualityThreshold() {
            return avgQualityThreshold;
        }

        public void setAvgQualityThreshold(float avgQualityThreshold) {
            this.avgQualityThreshold = avgQualityThreshold;
        }

        public float getAvgQualityThresholdForLowCoverage() {
            return avgQualityThresholdForLowCoverage;
        }

        public void setAvgQualityThresholdForLowCoverage(float avgQualityThresholdForLowCoverage) {
            this.avgQualityThresholdForLowCoverage = avgQualityThresholdForLowCoverage;
        }

        public int getTrimWindowSize() {
            return trimWindowSize;
        }

        public void setTrimWindowSize(int trimWindowSize) {
            this.trimWindowSize = trimWindowSize;
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
            ConsensusDoubleMultiAlignActionParameters that = (ConsensusDoubleMultiAlignActionParameters)o;
            if (alignerWidth != that.alignerWidth) return false;
            if (matchScore != that.matchScore) return false;
            if (mismatchScore != that.mismatchScore) return false;
            if (gapScore != that.gapScore) return false;
            if (goodQualityMismatchPenalty != that.goodQualityMismatchPenalty) return false;
            if (goodQualityMismatchThreshold != that.goodQualityMismatchThreshold) return false;
            if (scoreThreshold != that.scoreThreshold) return false;
            if (Float.compare(that.skippedFractionToRepeat, skippedFractionToRepeat) != 0) return false;
            if (maxConsensusesPerCluster != that.maxConsensusesPerCluster) return false;
            if (dropOversizedClusters != that.dropOversizedClusters) return false;
            if (readsMinGoodSeqLength != that.readsMinGoodSeqLength) return false;
            if (Float.compare(that.readsAvgQualityThreshold, readsAvgQualityThreshold) != 0) return false;
            if (readsTrimWindowSize != that.readsTrimWindowSize) return false;
            if (minGoodSeqLength != that.minGoodSeqLength) return false;
            if (Float.compare(that.lowCoverageThreshold, lowCoverageThreshold) != 0) return false;
            if (Float.compare(that.avgQualityThreshold, avgQualityThreshold) != 0) return false;
            if (Float.compare(that.avgQualityThresholdForLowCoverage, avgQualityThresholdForLowCoverage) != 0) return false;
            if (trimWindowSize != that.trimWindowSize) return false;
            if (toSeparateGroups != that.toSeparateGroups) return false;
            if (inputReadsLimit != that.inputReadsLimit) return false;
            return Objects.equals(groupList, that.groupList);
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
            result = 31 * result + (skippedFractionToRepeat != +0.0f ? Float.floatToIntBits(skippedFractionToRepeat) : 0);
            result = 31 * result + maxConsensusesPerCluster;
            result = 31 * result + (dropOversizedClusters ? 1 : 0);
            result = 31 * result + readsMinGoodSeqLength;
            result = 31 * result + (readsAvgQualityThreshold != +0.0f ? Float.floatToIntBits(readsAvgQualityThreshold) : 0);
            result = 31 * result + readsTrimWindowSize;
            result = 31 * result + minGoodSeqLength;
            result = 31 * result + (lowCoverageThreshold != +0.0f ? Float.floatToIntBits(lowCoverageThreshold) : 0);
            result = 31 * result + (avgQualityThreshold != +0.0f ? Float.floatToIntBits(avgQualityThreshold) : 0);
            result = 31 * result + (avgQualityThresholdForLowCoverage != +0.0f ? Float.floatToIntBits(avgQualityThresholdForLowCoverage) : 0);
            result = 31 * result + trimWindowSize;
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
