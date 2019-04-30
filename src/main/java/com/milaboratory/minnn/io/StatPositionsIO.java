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
import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.outputconverter.*;
import com.milaboratory.minnn.pattern.*;
import com.milaboratory.util.SmartProgressReporter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.FormatUtils.nanoTimeToString;

public final class StatPositionsIO {
    private final LinkedHashSet<String> groupList;
    private final LinkedHashSet<String> readIdList;
    private final boolean outputWithSeq;
    private final String inputFileName;
    private final String outputFileName;
    private final long inputReadsLimit;
    private final int minCountFilter;
    private final float minFracFilter;
    private final String reportFileName;
    private final String jsonReportFileName;
    private final boolean debugMode;
    private final HashMap<StatGroupsKey, Long> statGroups = new HashMap<>();
    private long totalReads = 0;

    public StatPositionsIO(List<String> groupList, List<String> readIdList, boolean outputWithSeq,
                           String inputFileName, String outputFileName, long inputReadsLimit, int minCountFilter,
                           float minFracFilter, String reportFileName, String jsonReportFileName, boolean debugMode) {
        this.groupList = new LinkedHashSet<>(groupList);
        this.readIdList = (readIdList == null) ? null : new LinkedHashSet<>(readIdList);
        this.outputWithSeq = outputWithSeq;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.inputReadsLimit = inputReadsLimit;
        this.minCountFilter = minCountFilter;
        this.minFracFilter = minFracFilter;
        this.reportFileName = reportFileName;
        this.jsonReportFileName = jsonReportFileName;
        this.debugMode = debugMode;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        ArrayList<String> correctedGroups;
        ArrayList<String> sortedGroups;
        String readerStats = null;
        try (MifReader reader = createReader()) {
            validateInputGroups(reader, groupList, true, "--groups");
            correctedGroups = reader.getCorrectedGroups();
            sortedGroups = reader.getSortedGroups();
            if (inputReadsLimit > 0)
                reader.setParsedReadsLimit(inputReadsLimit);
            SmartProgressReporter.startProgressReport("Processing", reader, System.err);
            for (ParsedRead parsedRead : CUtils.it(reader)) {
                Map<String, MatchedGroupEdge> startGroupEdges = parsedRead.getBestMatch().getMatchedGroupEdges()
                        .stream().filter(MatchedGroupEdge::isStart)
                        .collect(Collectors.toMap(MatchedGroupEdge::getGroupName, mge -> mge));
                Map<String, NucleotideSequence> groupValues = outputWithSeq ? parsedRead.getGroups().stream()
                            .collect(Collectors.toMap(MatchedGroup::getGroupName, mg -> mg.getValue().getSequence()))
                        : null;
                for (Map.Entry<String, MatchedGroupEdge> entry : startGroupEdges.entrySet())
                    if (groupList.contains(entry.getKey())) {
                        String readId = getReadId(entry.getValue(), parsedRead.isReverseMatch());
                        if ((readIdList == null) || readIdList.contains(readId)) {
                            StatGroupsKey currentKey = new StatGroupsKey(entry.getValue(), readId,
                                    outputWithSeq ? groupValues.get(entry.getKey()) : null);
                            Long count = statGroups.get(currentKey);
                            statGroups.put(currentKey, count == null ? 1 : count + 1);
                        }
                    }
                if (++totalReads == inputReadsLimit)
                    break;
            }
            if (debugMode)
                readerStats = reader.getStats().toString();
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        ArrayList<StatGroupsTableLine> table = new ArrayList<>();
        for (HashMap.Entry<StatGroupsKey, Long> statGroup : statGroups.entrySet()) {
            final float PRECISION = 0.00001f;
            long count = statGroup.getValue();
            if (((minCountFilter == 0) || (count >= minCountFilter))
                    && ((minFracFilter < PRECISION) || ((float)count / totalReads >= minFracFilter)))
                table.add(new StatGroupsTableLine(statGroup));
        }

        System.err.println("Sorting and writing...");
        try (PrintStream writer = createWriter()) {
            writer.println(getHeader());
            table.parallelStream().sorted().forEachOrdered(line -> writer.println(line.getTableLine()));
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        StringBuilder reportFileHeader = new StringBuilder();
        StringBuilder report = new StringBuilder();
        LinkedHashMap<String, Object> jsonReportData = new LinkedHashMap<>();

        reportFileHeader.append("MiNNN v").append(getVersionString(VERSION_INFO_SHORTEST)).append('\n');
        reportFileHeader.append("Report for StatPositions command:\n");
        if (inputFileName == null)
            reportFileHeader.append("Input is from stdin\n");
        else
            reportFileHeader.append("Input file name: ").append(inputFileName).append('\n');
        if (outputFileName == null)
            reportFileHeader.append("Output is to stdout\n");
        else
            reportFileHeader.append("Output file name: ").append(outputFileName).append('\n');
        if (debugMode) {
            reportFileHeader.append("\n\nDebug information:\n\n");
            reportFileHeader.append("Reader stats:\n").append(readerStats).append("\n\n");
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        report.append("Checked ").append(totalReads).append(" reads, ").append(totalReads * groupList.size())
                .append(" groups\n");
        long countedGroups = table.stream().mapToLong(line -> line.count).sum();
        if (totalReads > 0)
            report.append("Counted groups: ").append(countedGroups).append(" (")
                    .append(floatFormat.format((float)countedGroups / totalReads / groupList.size() * 100))
                    .append("% of checked groups)\n");

        jsonReportData.put("version", getVersionString(VERSION_INFO_SHORTEST));
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("correctedGroups", correctedGroups);
        jsonReportData.put("sortedGroups", sortedGroups);
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("groupList", groupList);
        jsonReportData.put("countedGroups", countedGroups);
        jsonReportData.put("totalReads", totalReads);

        humanReadableReport(reportFileName, reportFileHeader.toString(), report.toString());
        jsonReport(jsonReportFileName, jsonReportData);
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private PrintStream createWriter() throws IOException {
        return (outputFileName == null) ? new PrintStream(new SystemOutStream())
                : new PrintStream(new FileOutputStream(outputFileName));
    }

    private String getReadId(MatchedGroupEdge matchedGroupEdge, boolean reverseMatch) {
        int readNumber = matchedGroupEdge.getTargetId();
        // this will not work for retargeted .mif files!
        if (reverseMatch)
            if (readNumber == 1)
                readNumber = 2;
            else if (readNumber == 2)
                readNumber = 1;
        return "R" + readNumber;
    }

    private String getHeader() {
        if (outputWithSeq)
            return "group.id read pos count percent seq";
        else
            return "group.id read pos count percent";
    }

    private static class StatGroupsKey {
        final String groupName;
        final String readId;
        final int position;
        final NucleotideSequence seq;

        StatGroupsKey(MatchedGroupEdge startGroupEdge, String readId, NucleotideSequence seq) {
            this.groupName = startGroupEdge.getGroupName();
            this.readId = readId;
            this.position = startGroupEdge.getPosition();
            this.seq = seq;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StatGroupsKey that = (StatGroupsKey)o;
            return position == that.position && groupName.equals(that.groupName) && readId.equals(that.readId)
                    && Objects.equals(seq, that.seq);
        }

        @Override
        public int hashCode() {
            int result = groupName.hashCode();
            result = 31 * result + readId.hashCode();
            result = 31 * result + position;
            result = 31 * result + (seq != null ? seq.hashCode() : 0);
            return result;
        }
    }

    private class StatGroupsTableLine implements Comparable<StatGroupsTableLine> {
        final String groupId;
        final String readId;
        final int position;
        final long count;
        final NucleotideSequence seq;

        StatGroupsTableLine(HashMap.Entry<StatGroupsKey, Long> entry) {
            this.groupId = entry.getKey().groupName;
            this.readId = entry.getKey().readId;
            this.position = entry.getKey().position;
            this.count = entry.getValue();
            this.seq = entry.getKey().seq;
        }

        @Override
        public int compareTo(StatGroupsTableLine that) {
            return Long.compare(that.count, count);     // reversed to start from bigger counts
        }

        String getTableLine() {
            StringBuilder line = new StringBuilder();
            line.append(groupId).append(' ');
            line.append(readId).append(' ');
            line.append(position).append(' ');
            line.append(count).append(' ');
            float percent = (totalReads == 0) ? 0 : (float)count / totalReads * 100;
            line.append(floatFormat.format(percent)).append('%');
            if (outputWithSeq)
                line.append(' ').append(seq);
            return line.toString();
        }
    }
}
