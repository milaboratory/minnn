package com.milaboratory.minnn.cli;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.milaboratory.cli.ActionConfiguration;
import com.milaboratory.primitivio.annotations.Serializable;
import com.milaboratory.util.GlobalObjectMappers;

import java.util.List;

import static com.milaboratory.minnn.cli.CorrectAction.CORRECT_ACTION_NAME;

public final class CorrectActionConfiguration implements ActionConfiguration {
    private static final String CORRECT_ACTION_VERSION_ID = "1";
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
        private int mismatches;
        private int indels;
        private int totalErrors;
        private float threshold;
        private int maxClusterDepth;
        private float singleSubstitutionProbability;
        private float singleIndelProbability;
        private long inputReadsLimit;

        @JsonCreator
        public CorrectActionParameters(
                @JsonProperty("groupNames") List<String> groupNames,
                @JsonProperty("mismatches") int mismatches,
                @JsonProperty("indels") int indels,
                @JsonProperty("totalErrors") int totalErrors,
                @JsonProperty("threshold") float threshold,
                @JsonProperty("maxClusterDepth") int maxClusterDepth,
                @JsonProperty("singleSubstitutionProbability") float singleSubstitutionProbability,
                @JsonProperty("singleIndelProbability") float singleIndelProbability,
                @JsonProperty("inputReadsLimit") long inputReadsLimit) {
            this.groupNames = groupNames;
            this.mismatches = mismatches;
            this.indels = indels;
            this.totalErrors = totalErrors;
            this.threshold = threshold;
            this.maxClusterDepth = maxClusterDepth;
            this.singleSubstitutionProbability = singleSubstitutionProbability;
            this.singleIndelProbability = singleIndelProbability;
            this.inputReadsLimit = inputReadsLimit;
        }

        public List<String> getGroupNames() {
            return groupNames;
        }

        public void setGroupNames(List<String> groupNames) {
            this.groupNames = groupNames;
        }

        public int getMismatches() {
            return mismatches;
        }

        public void setMismatches(int mismatches) {
            this.mismatches = mismatches;
        }

        public int getIndels() {
            return indels;
        }

        public void setIndels(int indels) {
            this.indels = indels;
        }

        public int getTotalErrors() {
            return totalErrors;
        }

        public void setTotalErrors(int totalErrors) {
            this.totalErrors = totalErrors;
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
            CorrectActionParameters that = (CorrectActionParameters)o;
            if (mismatches != that.mismatches) return false;
            if (indels != that.indels) return false;
            if (totalErrors != that.totalErrors) return false;
            if (Float.compare(that.threshold, threshold) != 0) return false;
            if (maxClusterDepth != that.maxClusterDepth) return false;
            if (Float.compare(that.singleSubstitutionProbability, singleSubstitutionProbability) != 0) return false;
            if (Float.compare(that.singleIndelProbability, singleIndelProbability) != 0) return false;
            if (inputReadsLimit != that.inputReadsLimit) return false;
            return groupNames != null ? groupNames.equals(that.groupNames) : that.groupNames == null;
        }

        @Override
        public int hashCode() {
            int result = groupNames != null ? groupNames.hashCode() : 0;
            result = 31 * result + mismatches;
            result = 31 * result + indels;
            result = 31 * result + totalErrors;
            result = 31 * result + (threshold != +0.0f ? Float.floatToIntBits(threshold) : 0);
            result = 31 * result + maxClusterDepth;
            result = 31 * result + (singleSubstitutionProbability != +0.0f
                    ? Float.floatToIntBits(singleSubstitutionProbability) : 0);
            result = 31 * result + (singleIndelProbability != +0.0f
                    ? Float.floatToIntBits(singleIndelProbability) : 0);
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
