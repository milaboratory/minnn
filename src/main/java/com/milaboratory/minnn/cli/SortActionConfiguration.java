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

import static com.milaboratory.minnn.cli.SortAction.SORT_ACTION_NAME;

public final class SortActionConfiguration implements ActionConfiguration {
    private static final String SORT_ACTION_VERSION_ID = "2";
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
        private boolean quick;
        private int chunkSize;

        @JsonCreator
        public SortActionParameters(
                @JsonProperty("sortGroupNames") List<String> sortGroupNames,
                @JsonProperty("quick") boolean quick,
                @JsonProperty("chunkSize") int chunkSize) {
            this.sortGroupNames = sortGroupNames;
            this.quick = quick;
            this.chunkSize = chunkSize;
        }

        public List<String> getSortGroupNames() {
            return sortGroupNames;
        }

        public void setSortGroupNames(List<String> sortGroupNames) {
            this.sortGroupNames = sortGroupNames;
        }

        public boolean isQuick() {
            return quick;
        }

        public void setQuick(boolean quick) {
            this.quick = quick;
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
            if (quick != that.quick) return false;
            if (chunkSize != that.chunkSize) return false;
            return Objects.equals(sortGroupNames, that.sortGroupNames);
        }

        @Override
        public int hashCode() {
            int result = sortGroupNames != null ? sortGroupNames.hashCode() : 0;
            result = 31 * result + (quick ? 1 : 0);
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
