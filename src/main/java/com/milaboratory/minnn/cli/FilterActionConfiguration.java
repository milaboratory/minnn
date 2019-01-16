/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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

import java.util.Objects;

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
            return Objects.equals(filterQuery, that.filterQuery);
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
