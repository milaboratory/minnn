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

import static com.milaboratory.minnn.cli.DecontaminateAction.DECONTAMINATE_ACTION_NAME;

public final class DecontaminateActionConfiguration implements ActionConfiguration {
    private static final String DECONTAMINATE_ACTION_VERSION_ID = "1";
    private final DecontaminateActionParameters decontaminateParameters;

    @JsonCreator
    public DecontaminateActionConfiguration(
            @JsonProperty("decontaminateParameters") DecontaminateActionParameters decontaminateParameters) {
        this.decontaminateParameters = decontaminateParameters;
    }

    @Override
    public String actionName() {
        return DECONTAMINATE_ACTION_NAME;
    }

    @Override
    public String versionId() {
        return DECONTAMINATE_ACTION_VERSION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;
        DecontaminateActionConfiguration that = (DecontaminateActionConfiguration)o;
        return decontaminateParameters.equals(that.decontaminateParameters);
    }

    @Override
    public int hashCode() {
        return decontaminateParameters.hashCode();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE)
    @Serializable(asJson = true)
    public static final class DecontaminateActionParameters implements java.io.Serializable {
        private List<String> groupNames;
        private List<String> primaryGroupNames;
        private float minCountShare;
        private long inputReadsLimit;

        @JsonCreator
        public DecontaminateActionParameters(
                @JsonProperty("groupNames") List<String> groupNames,
                @JsonProperty("primaryGroupNames") List<String> primaryGroupNames,
                @JsonProperty("minCountShare") float minCountShare,
                @JsonProperty("inputReadsLimit") long inputReadsLimit) {
            this.groupNames = groupNames;
            this.primaryGroupNames = primaryGroupNames;
            this.minCountShare = minCountShare;
            this.inputReadsLimit = inputReadsLimit;
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

        public float getMinCountShare() {
            return minCountShare;
        }

        public void setMinCountShare(float minCountShare) {
            this.minCountShare = minCountShare;
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
            DecontaminateActionParameters that = (DecontaminateActionParameters)o;
            if (Float.compare(that.minCountShare, minCountShare) != 0) return false;
            if (inputReadsLimit != that.inputReadsLimit) return false;
            if (!Objects.equals(groupNames, that.groupNames)) return false;
            return Objects.equals(primaryGroupNames, that.primaryGroupNames);
        }

        @Override
        public int hashCode() {
            int result = groupNames != null ? groupNames.hashCode() : 0;
            result = 31 * result + (primaryGroupNames != null ? primaryGroupNames.hashCode() : 0);
            result = 31 * result + (minCountShare != +0.0f ? Float.floatToIntBits(minCountShare) : 0);
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
