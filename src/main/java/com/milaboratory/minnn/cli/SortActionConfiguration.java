package com.milaboratory.minnn.cli;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.milaboratory.cli.ActionConfiguration;
import com.milaboratory.primitivio.annotations.Serializable;
import com.milaboratory.util.GlobalObjectMappers;

import java.util.List;

import static com.milaboratory.minnn.cli.SortAction.SORT_ACTION_NAME;

public final class SortActionConfiguration implements ActionConfiguration {
    private static final String SORT_ACTION_VERSION_ID = "1";
    private final SortActionParameters sortParameters;

    @JsonCreator
    public SortActionConfiguration(@JsonProperty("sortParameters") SortActionParameters sortParameters) {
        this.sortParameters = sortParameters;
    }

    @Override
    public String actionName() {
        return SORT_ACTION_NAME;
    }

    @Override
    public String versionId() {
        return SORT_ACTION_VERSION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;
        SortActionConfiguration that = (SortActionConfiguration)o;
        return sortParameters.equals(that.sortParameters);
    }

    @Override
    public int hashCode() {
        return sortParameters.hashCode();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE)
    @Serializable(asJson = true)
    public static final class SortActionParameters implements java.io.Serializable {
        private List<String> sortGroupNames;
        private int chunkSize;

        @JsonCreator
        public SortActionParameters(
                @JsonProperty("sortGroupNames") List<String> sortGroupNames,
                @JsonProperty("chunkSize") int chunkSize) {
            this.sortGroupNames = sortGroupNames;
            this.chunkSize = chunkSize;
        }

        public List<String> getSortGroupNames() {
            return sortGroupNames;
        }

        public void setSortGroupNames(List<String> sortGroupNames) {
            this.sortGroupNames = sortGroupNames;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SortActionParameters that = (SortActionParameters)o;
            if (chunkSize != that.chunkSize) return false;
            return sortGroupNames != null ? sortGroupNames.equals(that.sortGroupNames) : that.sortGroupNames == null;
        }

        @Override
        public int hashCode() {
            int result = sortGroupNames != null ? sortGroupNames.hashCode() : 0;
            result = 31 * result + chunkSize;
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
