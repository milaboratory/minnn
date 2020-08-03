/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
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
package com.milaboratory.minnn.util;

import com.milaboratory.cli.AppVersionInfo;
import com.milaboratory.util.VersionInfo;

import java.util.Map;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;

public final class MinnnVersionInfo {
    private MinnnVersionInfo() {}

    public static String getVersionString(MinnnVersionInfoType type) {
        Map<String, VersionInfo> componentVersions = AppVersionInfo.get().getComponentVersions();
        VersionInfo minnn = componentVersions.get(APP_NAME);

        if (type == VERSION_INFO_SHORTEST) {
            return minnn.getVersion() +
                    "; built=" +
                    minnn.getTimestamp() +
                    "; rev=" +
                    minnn.getRevision();
        } else {
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

            if (type == VERSION_INFO_MAIN)
                builder.append("; host=")
                        .append(minnn.getHost())
                        .append(")\n");
            else
                builder.append("); ");

            builder.append("MiLib v")
                    .append(milib.getVersion())
                    .append(" (rev=")
                    .append(milib.getRevision())
                    .append(")");

            if (type == VERSION_INFO_MAIN)
                builder.append("\n");

            return builder.toString();
        }
    }
}
