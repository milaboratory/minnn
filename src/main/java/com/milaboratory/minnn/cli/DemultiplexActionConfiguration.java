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

import java.util.List;
import java.util.Objects;

import static com.milaboratory.minnn.cli.DemultiplexAction.DEMULTIPLEX_ACTION_NAME;

public final class DemultiplexActionConfiguration implements ActionConfiguration {
    private static final String DEMULTIPLEX_ACTION_VERSION_ID = "1";
    private final DemultiplexActionParameters demultiplexParameters;

    @JsonCreator
    public DemultiplexActionConfiguration(
            @JsonProperty("filterParameters") DemultiplexActionParameters demultiplexParameters) {
        this.demultiplexParameters = demultiplexParameters;
    }

    @Override
    public String actionName() {
        return DEMULTIPLEX_ACTION_NAME;
    }

    @Override
    public String versionId() {
        return DEMULTIPLEX_ACTION_VERSION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;
        DemultiplexActionConfiguration that = (DemultiplexActionConfiguration)o;
        return demultiplexParameters.equals(that.demultiplexParameters);
    }

    @Override
    public int hashCode() {
        return demultiplexParameters.hashCode();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE)
    @Serializable(asJson = true)
    public static final class DemultiplexActionParameters implements java.io.Serializable {
        private List<String> argumentsQuery;
        private long inputReadsLimit;

        @JsonCreator
        public DemultiplexActionParameters(
                @JsonProperty("argumentsQuery") List<String> argumentsQuery,
                @JsonProperty("inputReadsLimit") long inputReadsLimit) {
            this.argumentsQuery = argumentsQuery;
            this.inputReadsLimit = inputReadsLimit;
        }

        public List<String> getArgumentsQuery() {
            return argumentsQuery;
        }

        public void setArgumentsQuery(List<String> argumentsQuery) {
            this.argumentsQuery = argumentsQuery;
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
            DemultiplexActionParameters that = (DemultiplexActionParameters)o;
            if (inputReadsLimit != that.inputReadsLimit) return false;
            return Objects.equals(argumentsQuery, that.argumentsQuery);
        }

        @Override
        public int hashCode() {
            int result = argumentsQuery != null ? argumentsQuery.hashCode() : 0;
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
