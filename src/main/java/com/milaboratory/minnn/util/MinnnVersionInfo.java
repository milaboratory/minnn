package com.milaboratory.minnn.util;

import com.milaboratory.cli.AppVersionInfo;
import com.milaboratory.cli.AppVersionInfo.OutputType;
import com.milaboratory.util.VersionInfo;

import java.util.Map;

import static com.milaboratory.minnn.cli.Defaults.*;

public final class MinnnVersionInfo {
    private MinnnVersionInfo() {
    }

    public static String getShortestVersionString() {
        VersionInfo minnn = AppVersionInfo.get().getComponentVersions().get(APP_NAME);
        return minnn.getVersion() +
                "; built=" +
                minnn.getTimestamp() +
                "; rev=" +
                minnn.getRevision();
    }

    public static String getVersionString(OutputType outputType, boolean full) {
        Map<String, VersionInfo> componentVersions = AppVersionInfo.get().getComponentVersions();
        VersionInfo minnn = componentVersions.get(APP_NAME);
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
