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
package com.milaboratory.minnn.io;

import cc.redberry.pipe.CUtils;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;
import static com.milaboratory.minnn.util.SystemUtils.exitWithError;
import static com.milaboratory.util.FormatUtils.nanoTimeToString;

public final class MifInfoIO {
    private final String inputFileName;
    private final boolean noReadsCount;
    private final String reportFileName;
    private final String jsonReportFileName;

    public MifInfoIO(String inputFileName, boolean noReadsCount, String reportFileName, String jsonReportFileName) {
        this.inputFileName = inputFileName;
        this.noReadsCount = noReadsCount;
        this.reportFileName = reportFileName;
        this.jsonReportFileName = jsonReportFileName;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        String mifVersionInfo;
        long numberOfReads = 0;
        long originalNumberOfReads = 0;
        int numberOfTargets;
        List<String> allGroups;
        List<String> correctedGroups;
        List<String> sortedGroups;

        try (MifReader reader = new MifReader(inputFileName)) {
            mifVersionInfo = reader.getMifVersionInfo();
            MifHeader header = reader.getHeader();
            numberOfTargets = header.getNumberOfTargets();
            allGroups = reader.getGroupEdges().stream().map(GroupEdge::getGroupName).distinct()
                    .collect(Collectors.toList());
            correctedGroups = header.getCorrectedGroups();
            sortedGroups = header.getSortedGroups();
            if (!noReadsCount) {
                SmartProgressReporter.startProgressReport("Counting reads", reader, System.err);
                numberOfReads = StreamSupport.stream(CUtils.it(reader).spliterator(), false).count();
                reader.close();
                originalNumberOfReads = reader.getOriginalNumberOfReads();
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        String reportFileHeader = "MiNNN v" + getVersionString(VERSION_INFO_SHORTEST) +
                "\nReport for MifInfo command:\n" +
                "Input file name: " + inputFileName + '\n';
        StringBuilder report = new StringBuilder();
        LinkedHashMap<String, Object> jsonReportData = new LinkedHashMap<>();

        report.append("\nMIF file version: ").append(mifVersionInfo).append('\n');
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (!noReadsCount) {
            report.append("Processing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
            report.append("File contains ").append(numberOfReads).append(" reads\n");
            report.append("Original data (before extraction) contained ").append(originalNumberOfReads)
                    .append(" reads\n");
        }
        report.append("Number of targets in file: ").append(numberOfTargets).append('\n');
        report.append("Groups in file: ").append(allGroups).append('\n');
        if (correctedGroups.size() == 0)
            report.append("File is not corrected\n");
        else
            report.append("Groups ").append(correctedGroups).append(" in file are corrected\n");
        if (sortedGroups.size() == 0)
            report.append("File is not sorted\n");
        else
            report.append("Groups ").append(sortedGroups).append(" in file are sorted\n");

        jsonReportData.put("version", getVersionString(VERSION_INFO_SHORTEST));
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("mifVersionInfo", mifVersionInfo);
        jsonReportData.put("numberOfTargets", numberOfTargets);
        if (!noReadsCount) {
            jsonReportData.put("numberOfReads", numberOfReads);
            jsonReportData.put("originalNumberOfReads", originalNumberOfReads);
            jsonReportData.put("elapsedTime", elapsedTime);
        }
        jsonReportData.put("allGroups", allGroups);
        jsonReportData.put("correctedGroups", correctedGroups);
        jsonReportData.put("sortedGroups", sortedGroups);

        humanReadableReport(reportFileName, reportFileHeader, report.toString());
        jsonReport(jsonReportFileName, jsonReportData);
    }
}
