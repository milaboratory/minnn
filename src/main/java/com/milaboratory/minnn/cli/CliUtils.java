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

import com.milaboratory.cli.ValidationException;
import com.milaboratory.minnn.io.MifReader;
import com.milaboratory.minnn.pattern.GroupEdge;
import picocli.CommandLine;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.util.SystemUtils.*;

public final class CliUtils {
    private CliUtils() {}

    public final static DecimalFormat floatFormat = new DecimalFormat("#.##");

    static void validateQuality(int quality, CommandLine commandLine) {
        if ((quality < 0) || (quality > DEFAULT_MAX_QUALITY))
            throw new ValidationException(commandLine, quality + " is invalid value for quality! Valid values are "
                    + "from 0 to " + DEFAULT_MAX_QUALITY + ".", false);
    }

    public static void validateInputGroups(
            MifReader mifReader, Collection<String> inputGroups, boolean defaultGroupsAllowed) {
        if (!defaultGroupsAllowed) {
            Set<String> defaultGroups = IntStream.rangeClosed(1, mifReader.getNumberOfTargets())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());
            if (inputGroups.stream().anyMatch(defaultGroups::contains))
                throw exitWithError("Default groups R1, R2 etc are not allowed in --groups argument!");
        }
        Set<String> existingGroups = mifReader.getGroupEdges().stream().map(GroupEdge::getGroupName)
                .collect(Collectors.toSet());
        LinkedHashSet<String> missingGroups = inputGroups.stream().filter(g -> !existingGroups.contains(g))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (missingGroups.size() > 0) {
            Set<String> defaultGroups = IntStream.rangeClosed(1, mifReader.getNumberOfTargets())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());
            LinkedHashSet<String> availableGroups = mifReader.getGroupEdges().stream().map(GroupEdge::getGroupName)
                    .filter(g -> !defaultGroups.contains(g)).collect(Collectors.toCollection(LinkedHashSet::new));
            throw exitWithError("Groups " + missingGroups + " not found in the input! Check whether these groups " +
                    "were specified in extract pattern. Available groups in the input: " + availableGroups);
        }
    }
}
