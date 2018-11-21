package com.milaboratory.minnn.util;

import com.fasterxml.jackson.annotation.*;
import com.milaboratory.cli.AppVersionInfo;
import com.milaboratory.primitivio.annotations.Serializable;
import com.milaboratory.util.VersionInfo;

import java.util.HashMap;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE)
@Serializable(asJson = true)
public final class MinnnVersionInfo extends AppVersionInfo {
    private MinnnVersionInfo(@JsonProperty("minnn") VersionInfo minnn,
                            @JsonProperty("milib") VersionInfo milib) {
        super(prepareComponentVersions(minnn, milib), new HashMap<>());
    }

    private static HashMap<String, VersionInfo> prepareComponentVersions(VersionInfo minnn, VersionInfo milib) {
        HashMap<String, VersionInfo> componentVersions = new HashMap<>();
        componentVersions.put("minnn", minnn);
        componentVersions.put("milib", milib);
        return componentVersions;
    }

    public static MinnnVersionInfo get() {
        if (instance == null)
            synchronized (MinnnVersionInfo.class) {
                if (instance == null) {
                    VersionInfo minnn = VersionInfo.getVersionInfoForArtifact("minnn");
                    VersionInfo milib = VersionInfo.getVersionInfoForArtifact("milib");
                    instance = new MinnnVersionInfo(minnn, milib);
                }
            }
        return (MinnnVersionInfo)instance;
    }

    @Override
    public String getShortestVersionString() {
        VersionInfo minnn = componentVersions.get("minnn");
        return minnn.getVersion() +
                "; built=" +
                minnn.getTimestamp() +
                "; rev=" +
                minnn.getRevision();
    }

    @Override
    public String getVersionString(OutputType outputType, boolean full) {
        VersionInfo minnn = componentVersions.get("minnn");
        VersionInfo milib = componentVersions.get("milib");

        StringBuilder builder = new StringBuilder();

        builder.append("MiNNN v")
                .append(minnn.getVersion())
                .append(" (built ")
                .append(minnn.getTimestamp())
                .append("; rev=")
                .append(minnn.getRevision())
                .append("; branch=")
                .append(minnn.getBranch());

        if (full)
            builder.append("; host=")
                    .append(minnn.getHost());

        builder.append(")")
                .append(outputType.delimiter);

        builder.append("MiLib v")
                .append(milib.getVersion())
                .append(" (rev=")
                .append(milib.getRevision())
                .append(")")
                .append(outputType.delimiter);

        return builder.toString();
    }
}
