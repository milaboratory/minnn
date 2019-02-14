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
package com.milaboratory.minnn.cli;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.milaboratory.cli.ActionConfiguration;
import com.milaboratory.primitivio.annotations.Serializable;
import com.milaboratory.util.GlobalObjectMappers;

import java.util.List;
import java.util.Objects;

import static com.milaboratory.minnn.cli.ConsensusSingleCellAction.CONSENSUS_SINGLE_CELL_ACTION_NAME;

public final class ConsensusSingleCellActionConfiguration implements ActionConfiguration {
    private static final String CONSENSUS_SINGLE_CELL_ACTION_VERSION_ID = "1";
    private final ConsensusSingleCellActionParameters consensusParameters;

    @JsonCreator
    public ConsensusSingleCellActionConfiguration(
            @JsonProperty("consensusParameters") ConsensusSingleCellActionParameters consensusParameters) {
        this.consensusParameters = consensusParameters;
    }

    @Override
    public String actionName() {
        return CONSENSUS_SINGLE_CELL_ACTION_NAME;
    }

    @Override
    public String versionId() {
        return CONSENSUS_SINGLE_CELL_ACTION_VERSION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;
        ConsensusSingleCellActionConfiguration that = (ConsensusSingleCellActionConfiguration)o;
        return consensusParameters.equals(that.consensusParameters);
    }

    @Override
    public int hashCode() {
        return consensusParameters.hashCode();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE)
    @Serializable(asJson = true)
    public static final class ConsensusSingleCellActionParameters implements java.io.Serializable {
        private List<String> groupList;
        private float skippedFractionToRepeat;
        private int maxConsensusesPerCluster;
        private int readsMinGoodSeqLength;
        private float readsAvgQualityThreshold;
        private int readsTrimWindowSize;
        private int minGoodSeqLength;
        private float avgQualityThreshold;
        private int trimWindowSize;
        private boolean toSeparateGroups;
        private long inputReadsLimit;
        private int kmerLength;
        private int kmerOffset;
        private int kmerMaxErrors;

        public ConsensusSingleCellActionParameters(
                @JsonProperty("groupList") List<String> groupList,
                @JsonProperty("skippedFractionToRepeat") float skippedFractionToRepeat,
                @JsonProperty("maxConsensusesPerCluster") int maxConsensusesPerCluster,
                @JsonProperty("readsMinGoodSeqLength") int readsMinGoodSeqLength,
                @JsonProperty("readsAvgQualityThreshold") float readsAvgQualityThreshold,
                @JsonProperty("readsTrimWindowSize") int readsTrimWindowSize,
                @JsonProperty("minGoodSeqLength") int minGoodSeqLength,
                @JsonProperty("avgQualityThreshold") float avgQualityThreshold,
                @JsonProperty("trimWindowSize") int trimWindowSize,
                @JsonProperty("toSeparateGroups") boolean toSeparateGroups,
                @JsonProperty("inputReadsLimit") long inputReadsLimit,
                @JsonProperty("kmerLength") int kmerLength,
                @JsonProperty("kmerOffset") int kmerOffset,
                @JsonProperty("kmerMaxErrors") int kmerMaxErrors) {
            this.groupList = groupList;
            this.skippedFractionToRepeat = skippedFractionToRepeat;
            this.maxConsensusesPerCluster = maxConsensusesPerCluster;
            this.readsMinGoodSeqLength = readsMinGoodSeqLength;
            this.readsAvgQualityThreshold = readsAvgQualityThreshold;
            this.readsTrimWindowSize = readsTrimWindowSize;
            this.minGoodSeqLength = minGoodSeqLength;
            this.avgQualityThreshold = avgQualityThreshold;
            this.trimWindowSize = trimWindowSize;
            this.toSeparateGroups = toSeparateGroups;
            this.inputReadsLimit = inputReadsLimit;
            this.kmerLength = kmerLength;
            this.kmerOffset = kmerOffset;
            this.kmerMaxErrors = kmerMaxErrors;
        }

        public List<String> getGroupList() {
            return groupList;
        }

        public void setGroupList(List<String> groupList) {
            this.groupList = groupList;
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

        public int getKmerLength() {
            return kmerLength;
        }

        public void setKmerLength(int kmerLength) {
            this.kmerLength = kmerLength;
        }

        public int getKmerOffset() {
            return kmerOffset;
        }

        public void setKmerOffset(int kmerOffset) {
            this.kmerOffset = kmerOffset;
        }

        public int getKmerMaxErrors() {
            return kmerMaxErrors;
        }

        public void setKmerMaxErrors(int kmerMaxErrors) {
            this.kmerMaxErrors = kmerMaxErrors;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConsensusSingleCellActionParameters that = (ConsensusSingleCellActionParameters)o;
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
            if (kmerLength != that.kmerLength) return false;
            if (kmerOffset != that.kmerOffset) return false;
            if (kmerMaxErrors != that.kmerMaxErrors) return false;
            return Objects.equals(groupList, that.groupList);
        }

        @Override
        public int hashCode() {
            int result = groupList != null ? groupList.hashCode() : 0;
            result = 31 * result + (skippedFractionToRepeat != +0.0f
                    ? Float.floatToIntBits(skippedFractionToRepeat) : 0);
            result = 31 * result + maxConsensusesPerCluster;
            result = 31 * result + readsMinGoodSeqLength;
            result = 31 * result + (readsAvgQualityThreshold != +0.0f
                    ? Float.floatToIntBits(readsAvgQualityThreshold) : 0);
            result = 31 * result + readsTrimWindowSize;
            result = 31 * result + minGoodSeqLength;
            result = 31 * result + (avgQualityThreshold != +0.0f
                    ? Float.floatToIntBits(avgQualityThreshold) : 0);
            result = 31 * result + trimWindowSize;
            result = 31 * result + (toSeparateGroups ? 1 : 0);
            result = 31 * result + (int)(inputReadsLimit ^ (inputReadsLimit >>> 32));
            result = 31 * result + kmerLength;
            result = 31 * result + kmerOffset;
            result = 31 * result + kmerMaxErrors;
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
