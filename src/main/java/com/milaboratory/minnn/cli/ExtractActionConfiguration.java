package com.milaboratory.minnn.cli;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.milaboratory.cli.ActionConfiguration;
import com.milaboratory.primitivio.annotations.Serializable;
import com.milaboratory.util.GlobalObjectMappers;

import java.util.LinkedHashMap;

import static com.milaboratory.minnn.cli.ExtractAction.EXTRACT_ACTION_NAME;

public final class ExtractActionConfiguration implements ActionConfiguration {
    private static final String EXTRACT_ACTION_VERSION_ID = "1";
    private final ExtractActionParameters extractParameters;

    @JsonCreator
    public ExtractActionConfiguration(@JsonProperty("extractParameters") ExtractActionParameters extractParameters) {
        this.extractParameters = extractParameters;
    }

    @Override
    public String actionName() {
        return EXTRACT_ACTION_NAME;
    }

    @Override
    public String versionId() {
        return EXTRACT_ACTION_VERSION_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;
        ExtractActionConfiguration that = (ExtractActionConfiguration)o;
        return extractParameters.equals(that.extractParameters);
    }

    @Override
    public int hashCode() {
        return extractParameters.hashCode();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE)
    @Serializable(asJson = true)
    public static final class ExtractActionParameters implements java.io.Serializable {
        private String query;
        private String inputFormatName;
        private boolean oriented;
        private int matchScore;
        private int mismatchScore;
        private int uppercaseMismatchScore;
        private int gapScore;
        private long scoreThreshold;
        private byte goodQuality;
        private byte badQuality;
        private int maxQualityPenalty;
        private long singleOverlapPenalty;
        private int maxOverlap;
        private int bitapMaxErrors;
        private boolean fairSorting;
        private long inputReadsLimit;
        private LinkedHashMap<String, String> descriptionGroupsMap;
        private boolean simplifiedSyntax;

        @JsonCreator
        public ExtractActionParameters(
                @JsonProperty("query") String query,
                @JsonProperty("inputFormatName") String inputFormatName,
                @JsonProperty("oriented") boolean oriented,
                @JsonProperty("matchScore") int matchScore,
                @JsonProperty("mismatchScore") int mismatchScore,
                @JsonProperty("uppercaseMismatchScore") int uppercaseMismatchScore,
                @JsonProperty("gapScore") int gapScore,
                @JsonProperty("scoreThreshold") long scoreThreshold,
                @JsonProperty("goodQuality") byte goodQuality,
                @JsonProperty("badQuality") byte badQuality,
                @JsonProperty("maxQualityPenalty") int maxQualityPenalty,
                @JsonProperty("singleOverlapPenalty") long singleOverlapPenalty,
                @JsonProperty("maxOverlap") int maxOverlap,
                @JsonProperty("bitapMaxErrors") int bitapMaxErrors,
                @JsonProperty("fairSorting") boolean fairSorting,
                @JsonProperty("inputReadsLimit") long inputReadsLimit,
                @JsonProperty("descriptionGroupsMap") LinkedHashMap<String, String> descriptionGroupsMap,
                @JsonProperty("simplifiedSyntax") boolean simplifiedSyntax) {
            this.query = query;
            this.inputFormatName = inputFormatName;
            this.oriented = oriented;
            this.matchScore = matchScore;
            this.mismatchScore = mismatchScore;
            this.uppercaseMismatchScore = uppercaseMismatchScore;
            this.gapScore = gapScore;
            this.scoreThreshold = scoreThreshold;
            this.goodQuality = goodQuality;
            this.badQuality = badQuality;
            this.maxQualityPenalty = maxQualityPenalty;
            this.singleOverlapPenalty = singleOverlapPenalty;
            this.maxOverlap = maxOverlap;
            this.bitapMaxErrors = bitapMaxErrors;
            this.fairSorting = fairSorting;
            this.inputReadsLimit = inputReadsLimit;
            this.descriptionGroupsMap = descriptionGroupsMap;
            this.simplifiedSyntax = simplifiedSyntax;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getInputFormatName() {
            return inputFormatName;
        }

        public void setInputFormatName(String inputFormatName) {
            this.inputFormatName = inputFormatName;
        }

        public boolean isOriented() {
            return oriented;
        }

        public void setOriented(boolean oriented) {
            this.oriented = oriented;
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

        public int getUppercaseMismatchScore() {
            return uppercaseMismatchScore;
        }

        public void setUppercaseMismatchScore(int uppercaseMismatchScore) {
            this.uppercaseMismatchScore = uppercaseMismatchScore;
        }

        public int getGapScore() {
            return gapScore;
        }

        public void setGapScore(int gapScore) {
            this.gapScore = gapScore;
        }

        public long getScoreThreshold() {
            return scoreThreshold;
        }

        public void setScoreThreshold(long scoreThreshold) {
            this.scoreThreshold = scoreThreshold;
        }

        public byte getGoodQuality() {
            return goodQuality;
        }

        public void setGoodQuality(byte goodQuality) {
            this.goodQuality = goodQuality;
        }

        public byte getBadQuality() {
            return badQuality;
        }

        public void setBadQuality(byte badQuality) {
            this.badQuality = badQuality;
        }

        public int getMaxQualityPenalty() {
            return maxQualityPenalty;
        }

        public void setMaxQualityPenalty(int maxQualityPenalty) {
            this.maxQualityPenalty = maxQualityPenalty;
        }

        public long getSingleOverlapPenalty() {
            return singleOverlapPenalty;
        }

        public void setSingleOverlapPenalty(long singleOverlapPenalty) {
            this.singleOverlapPenalty = singleOverlapPenalty;
        }

        public int getMaxOverlap() {
            return maxOverlap;
        }

        public void setMaxOverlap(int maxOverlap) {
            this.maxOverlap = maxOverlap;
        }

        public int getBitapMaxErrors() {
            return bitapMaxErrors;
        }

        public void setBitapMaxErrors(int bitapMaxErrors) {
            this.bitapMaxErrors = bitapMaxErrors;
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

        public LinkedHashMap<String, String> getDescriptionGroupsMap() {
            return descriptionGroupsMap;
        }

        public void setDescriptionGroupsMap(LinkedHashMap<String, String> descriptionGroupsMap) {
            this.descriptionGroupsMap = descriptionGroupsMap;
        }

        public boolean isSimplifiedSyntax() {
            return simplifiedSyntax;
        }

        public void setSimplifiedSyntax(boolean simplifiedSyntax) {
            this.simplifiedSyntax = simplifiedSyntax;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExtractActionParameters that = (ExtractActionParameters)o;
            if (oriented != that.oriented) return false;
            if (matchScore != that.matchScore) return false;
            if (mismatchScore != that.mismatchScore) return false;
            if (uppercaseMismatchScore != that.uppercaseMismatchScore) return false;
            if (gapScore != that.gapScore) return false;
            if (scoreThreshold != that.scoreThreshold) return false;
            if (goodQuality != that.goodQuality) return false;
            if (badQuality != that.badQuality) return false;
            if (maxQualityPenalty != that.maxQualityPenalty) return false;
            if (singleOverlapPenalty != that.singleOverlapPenalty) return false;
            if (maxOverlap != that.maxOverlap) return false;
            if (bitapMaxErrors != that.bitapMaxErrors) return false;
            if (fairSorting != that.fairSorting) return false;
            if (inputReadsLimit != that.inputReadsLimit) return false;
            if (simplifiedSyntax != that.simplifiedSyntax) return false;
            if (query != null ? !query.equals(that.query) : that.query != null) return false;
            if (inputFormatName != null ? !inputFormatName.equals(that.inputFormatName) : that.inputFormatName != null)
                return false;
            return descriptionGroupsMap != null ? descriptionGroupsMap.equals(that.descriptionGroupsMap)
                    : that.descriptionGroupsMap == null;
        }

        @Override
        public int hashCode() {
            int result = query != null ? query.hashCode() : 0;
            result = 31 * result + (inputFormatName != null ? inputFormatName.hashCode() : 0);
            result = 31 * result + (oriented ? 1 : 0);
            result = 31 * result + matchScore;
            result = 31 * result + mismatchScore;
            result = 31 * result + uppercaseMismatchScore;
            result = 31 * result + gapScore;
            result = 31 * result + (int)(scoreThreshold ^ (scoreThreshold >>> 32));
            result = 31 * result + (int)goodQuality;
            result = 31 * result + (int)badQuality;
            result = 31 * result + maxQualityPenalty;
            result = 31 * result + (int)(singleOverlapPenalty ^ (singleOverlapPenalty >>> 32));
            result = 31 * result + maxOverlap;
            result = 31 * result + bitapMaxErrors;
            result = 31 * result + (fairSorting ? 1 : 0);
            result = 31 * result + (int)(inputReadsLimit ^ (inputReadsLimit >>> 32));
            result = 31 * result + (descriptionGroupsMap != null ? descriptionGroupsMap.hashCode() : 0);
            result = 31 * result + (simplifiedSyntax ? 1 : 0);
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
