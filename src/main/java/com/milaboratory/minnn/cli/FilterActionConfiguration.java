package com.milaboratory.minnn.cli;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.milaboratory.cli.ActionConfiguration;
import com.milaboratory.primitivio.annotations.Serializable;
import com.milaboratory.util.GlobalObjectMappers;

import static com.milaboratory.minnn.cli.FilterAction.FILTER_ACTION_NAME;

public final class FilterActionConfiguration implements ActionConfiguration {
    private static final String FILTER_ACTION_VERSION_ID = "1";
    private final FilterActionParameters filterParameters;

    @JsonCreator
    public FilterActionConfiguration(@JsonProperty("filterParameters") FilterActionParameters filterParameters) {
        this.filterParameters = filterParameters;
    }

    @Override
    public String actionName() {
        return FILTER_ACTION_NAME;
    }

    @Override
    public String versionId() {
        return FILTER_ACTION_VERSION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;
        FilterActionConfiguration that = (FilterActionConfiguration)o;
        return filterParameters.equals(that.filterParameters);
    }

    @Override
    public int hashCode() {
        return filterParameters.hashCode();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE)
    @Serializable(asJson = true)
    public static final class FilterActionParameters implements java.io.Serializable {
        private String filterQuery;
        private boolean fairSorting;
        private long inputReadsLimit;

        @JsonCreator
        public FilterActionParameters(
                @JsonProperty("filterQuery") String filterQuery,
                @JsonProperty("fairSorting") boolean fairSorting,
                @JsonProperty("inputReadsLimit") long inputReadsLimit) {
            this.filterQuery = filterQuery;
            this.fairSorting = fairSorting;
            this.inputReadsLimit = inputReadsLimit;
        }

        public String getFilterQuery() {
            return filterQuery;
        }

        public void setFilterQuery(String filterQuery) {
            this.filterQuery = filterQuery;
        }

        public boolean isFairSorting() {
            return fairSorting;
        }

        public void setFairSorting(boolean fairSorting) {
            this.fairSorting = fairSorting;
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
            FilterActionParameters that = (FilterActionParameters)o;
            if (fairSorting != that.fairSorting) return false;
            if (inputReadsLimit != that.inputReadsLimit) return false;
            return filterQuery != null ? filterQuery.equals(that.filterQuery) : that.filterQuery == null;
        }

        @Override
        public int hashCode() {
            int result = filterQuery != null ? filterQuery.hashCode() : 0;
            result = 31 * result + (fairSorting ? 1 : 0);
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
