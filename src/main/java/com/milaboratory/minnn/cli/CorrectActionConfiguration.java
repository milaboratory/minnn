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

import static com.milaboratory.minnn.cli.CorrectAction.CORRECT_ACTION_NAME;

public final class CorrectActionConfiguration implements ActionConfiguration {
    private static final String CORRECT_ACTION_VERSION_ID = "5";
    private final CorrectActionParameters correctParameters;

    @JsonCreator
    public CorrectActionConfiguration(@JsonProperty("correctParameters") CorrectActionParameters correctParameters) {
        this.correctParameters = correctParameters;
    }

    @Override
    public String actionName() {
        return CORRECT_ACTION_NAME;
    }

    @Override
    public String versionId() {
        return CORRECT_ACTION_VERSION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;
        CorrectActionConfiguration that = (CorrectActionConfiguration)o;
        return correctParameters.equals(that.correctParameters);
    }

    @Override
    public int hashCode() {
        return correctParameters.hashCode();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE)
    @Serializable(asJson = true)
    public static final class CorrectActionParameters implements java.io.Serializable {
        private List<String> groupNames;
        private List<String> primaryGroupNames;
        private float maxErrorsShare;
        private int maxErrors;
        private float threshold;
        private int maxClusterDepth;
        private float singleSubstitutionProbability;
        private float singleIndelProbability;
        private int maxUniqueBarcodes;
        private int minCount;
        private float wildcardsCollapsingMergeThreshold;
        private long inputReadsLimit;
        private int threads;

        @JsonCreator
        public CorrectActionParameters(
                @JsonProperty("groupNames") List<String> groupNames,
                @JsonProperty("primaryGroupNames") List<String> primaryGroupNames,
                @JsonProperty("maxErrorsShare") float maxErrorsShare,
                @JsonProperty("maxErrors") int maxErrors,
                @JsonProperty("threshold") float threshold,
                @JsonProperty("maxClusterDepth") int maxClusterDepth,
                @JsonProperty("singleSubstitutionProbability") float singleSubstitutionProbability,
                @JsonProperty("singleIndelProbability") float singleIndelProbability,
                @JsonProperty("maxUniqueBarcodes") int maxUniqueBarcodes,
                @JsonProperty("minCount") int minCount,
                @JsonProperty("wildcardsCollapsingMergeThreshold") float wildcardsCollapsingMergeThreshold,
                @JsonProperty("inputReadsLimit") long inputReadsLimit,
                @JsonProperty("threads") int threads) {
            this.groupNames = groupNames;
            this.primaryGroupNames = primaryGroupNames;
            this.maxErrorsShare = maxErrorsShare;
            this.maxErrors = maxErrors;
            this.threshold = threshold;
            this.maxClusterDepth = maxClusterDepth;
            this.singleSubstitutionProbability = singleSubstitutionProbability;
            this.singleIndelProbability = singleIndelProbability;
            this.maxUniqueBarcodes = maxUniqueBarcodes;
            this.minCount = minCount;
            this.wildcardsCollapsingMergeThreshold = wildcardsCollapsingMergeThreshold;
            this.inputReadsLimit = inputReadsLimit;
            this.threads = threads;
        }

        public List<String> getGroupNames() {
            return groupNames;
        }

        public void setGroupNames(List<String> groupNames) {
            this.groupNames = groupNames;
        }

        public List<String> getPrimaryGroupNames() {
            return primaryGroupNames;
        }

        public void setPrimaryGroupNames(List<String> primaryGroupNames) {
            this.primaryGroupNames = primaryGroupNames;
        }

        public float getMaxErrorsShare() {
            return maxErrorsShare;
        }

        public void setMaxErrorsShare(float maxErrorsShare) {
            this.maxErrorsShare = maxErrorsShare;
        }

        public int getMaxErrors() {
            return maxErrors;
        }

        public void setMaxErrors(int maxErrors) {
            this.maxErrors = maxErrors;
        }

        public float getThreshold() {
            return threshold;
        }

        public void setThreshold(float threshold) {
            this.threshold = threshold;
        }

        public int getMaxClusterDepth() {
            return maxClusterDepth;
        }

        public void setMaxClusterDepth(int maxClusterDepth) {
            this.maxClusterDepth = maxClusterDepth;
        }

        public float getSingleSubstitutionProbability() {
            return singleSubstitutionProbability;
        }

        public void setSingleSubstitutionProbability(float singleSubstitutionProbability) {
            this.singleSubstitutionProbability = singleSubstitutionProbability;
        }

        public float getSingleIndelProbability() {
            return singleIndelProbability;
        }

        public void setSingleIndelProbability(float singleIndelProbability) {
            this.singleIndelProbability = singleIndelProbability;
        }

        public int getMaxUniqueBarcodes() {
            return maxUniqueBarcodes;
        }

        public void setMaxUniqueBarcodes(int maxUniqueBarcodes) {
            this.maxUniqueBarcodes = maxUniqueBarcodes;
        }

        public int getMinCount() {
            return minCount;
        }

        public void setMinCount(int minCount) {
            this.minCount = minCount;
        }

        public float getWildcardsCollapsingMergeThreshold() {
            return wildcardsCollapsingMergeThreshold;
        }

        public void setWildcardsCollapsingMergeThreshold(float wildcardsCollapsingMergeThreshold) {
            this.wildcardsCollapsingMergeThreshold = wildcardsCollapsingMergeThreshold;
        }

        public long getInputReadsLimit() {
            return inputReadsLimit;
        }

        public void setInputReadsLimit(long inputReadsLimit) {
            this.inputReadsLimit = inputReadsLimit;
        }

        public int getThreads() {
            return threads;
        }

        public void setThreads(int threads) {
            this.threads = threads;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CorrectActionParameters that = (CorrectActionParameters)o;
            if (Float.compare(that.maxErrorsShare, maxErrorsShare) != 0) return false;
            if (maxErrors != that.maxErrors) return false;
            if (Float.compare(that.threshold, threshold) != 0) return false;
            if (maxClusterDepth != that.maxClusterDepth) return false;
            if (Float.compare(that.singleSubstitutionProbability, singleSubstitutionProbability) != 0) return false;
            if (Float.compare(that.singleIndelProbability, singleIndelProbability) != 0) return false;
            if (maxUniqueBarcodes != that.maxUniqueBarcodes) return false;
            if (minCount != that.minCount) return false;
            if (Float.compare(that.wildcardsCollapsingMergeThreshold, wildcardsCollapsingMergeThreshold) != 0)
                return false;
            if (inputReadsLimit != that.inputReadsLimit) return false;
            if (threads != that.threads) return false;
            if (!Objects.equals(groupNames, that.groupNames)) return false;
            return Objects.equals(primaryGroupNames, that.primaryGroupNames);
        }

        @Override
        public int hashCode() {
            int result = groupNames != null ? groupNames.hashCode() : 0;
            result = 31 * result + (primaryGroupNames != null ? primaryGroupNames.hashCode() : 0);
            result = 31 * result + (maxErrorsShare != +0.0f ? Float.floatToIntBits(maxErrorsShare) : 0);
            result = 31 * result + maxErrors;
            result = 31 * result + (threshold != +0.0f ? Float.floatToIntBits(threshold) : 0);
            result = 31 * result + maxClusterDepth;
            result = 31 * result + (singleSubstitutionProbability != +0.0f
                    ? Float.floatToIntBits(singleSubstitutionProbability) : 0);
            result = 31 * result + (singleIndelProbability != +0.0f
                    ? Float.floatToIntBits(singleIndelProbability) : 0);
            result = 31 * result + maxUniqueBarcodes;
            result = 31 * result + minCount;
            result = 31 * result + (wildcardsCollapsingMergeThreshold != +0.0f
                    ? Float.floatToIntBits(wildcardsCollapsingMergeThreshold) : 0);
            result = 31 * result + (int)(inputReadsLimit ^ (inputReadsLimit >>> 32));
            result = 31 * result + threads;
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
