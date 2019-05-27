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
package com.milaboratory.minnn.io;

import cc.redberry.pipe.CUtils;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceQuality;
import com.milaboratory.minnn.outputconverter.MatchedGroup;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.util.SmartProgressReporter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class StatGroupsIO {
    private final LinkedHashSet<String> groupList;
    private final String inputFileName;
    private final String outputFileName;
    private final long inputReadsLimit;
    private final byte readQualityFilter;
    private final byte minQualityFilter;
    private final byte avgQualityFilter;
    private final int minCountFilter;
    private final float minFracFilter;
    private final String reportFileName;
    private final String jsonReportFileName;
    private final HashMap<StatGroupsKey, StatGroupsValue> statGroups = new HashMap<>();
    private long totalReads = 0;

    public StatGroupsIO(List<String> groupList, String inputFileName, String outputFileName, long inputReadsLimit,
                        byte readQualityFilter, byte minQualityFilter, byte avgQualityFilter, int minCountFilter,
                        float minFracFilter, String reportFileName, String jsonReportFileName) {
        this.groupList = new LinkedHashSet<>(groupList);
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.inputReadsLimit = inputReadsLimit;
        this.readQualityFilter = readQualityFilter;
        this.minQualityFilter = minQualityFilter;
        this.avgQualityFilter = avgQualityFilter;
        this.minCountFilter = minCountFilter;
        this.minFracFilter = minFracFilter;
        this.reportFileName = reportFileName;
        this.jsonReportFileName = jsonReportFileName;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        ArrayList<String> correctedGroups;
        ArrayList<String> sortedGroups;

        try (MifReader reader = createReader()) {
            validateInputGroups(reader, groupList, true);
            correctedGroups = reader.getCorrectedGroups();
            sortedGroups = reader.getSortedGroups();
            if (inputReadsLimit > 0)
                reader.setParsedReadsLimit(inputReadsLimit);
            SmartProgressReporter.startProgressReport("Processing", reader, System.err);
            for (ParsedRead parsedRead : CUtils.it(reader)) {
                Map<String, MatchedGroup> matchedGroups = parsedRead.getGroups().stream()
                        .collect(Collectors.toMap(MatchedGroup::getGroupName, mg -> mg));
                List<NSequenceWithQuality> groupValues = groupList.stream()
                        .map(groupName -> matchedGroups.get(groupName).getValue()).collect(Collectors.toList());
                if (groupValues.stream().allMatch(this::checkQuality)) {
                    StatGroupsKey currentKey = new StatGroupsKey(groupValues);
                    StatGroupsValue currentValue = statGroups.get(currentKey);
                    if (currentValue == null)
                        statGroups.put(currentKey, new StatGroupsValue(groupValues));
                    else
                        currentValue.countNewValue(groupValues);
                }
                if (++totalReads == inputReadsLimit)
                    break;
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        ArrayList<StatGroupsTableLine> table = new ArrayList<>();
        for (HashMap.Entry<StatGroupsKey, StatGroupsValue> statGroup : statGroups.entrySet()) {
            final float PRECISION = 0.00001f;
            StatGroupsValue value = statGroup.getValue();
            if (((minCountFilter == 0) || (value.count >= minCountFilter))
                    && ((minFracFilter < PRECISION) || ((float)(value.count) / totalReads >= minFracFilter))
                    && IntStream.range(0, groupList.size())
                        .noneMatch(i -> (((minQualityFilter > 0) && (value.getMinQuality(i) < minQualityFilter))
                                || ((avgQualityFilter > 0) && (value.getAvgQuality(i) < avgQualityFilter)))))
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

        reportFileHeader.append("Report for StatGroups command:\n");
        if (inputFileName == null)
            reportFileHeader.append("Input is from stdin\n");
        else
            reportFileHeader.append("Input file name: ").append(inputFileName).append('\n');
        if (outputFileName == null)
            reportFileHeader.append("Output is to stdout\n");
        else
            reportFileHeader.append("Output file name: ").append(outputFileName).append('\n');

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        if (correctedGroups.size() == 0)
            report.append("Input MIF file is not corrected\n");
        else
            report.append("Groups ").append(correctedGroups).append(" in input MIF file are corrected\n");
        if (sortedGroups.size() == 0)
            report.append("Input MIF file is not sorted\n");
        else
            report.append("Groups ").append(sortedGroups).append(" in input MIF file are sorted\n");
        report.append("Checked ").append(totalReads).append(" reads\n");
        long countedReads = table.stream().mapToLong(line -> line.count).sum();
        if (totalReads > 0)
            report.append("Counted reads: ").append(countedReads).append(" (")
                    .append(floatFormat.format((float)countedReads / totalReads * 100))
                    .append("% of checked reads)\n");

        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("correctedGroups", correctedGroups);
        jsonReportData.put("sortedGroups", sortedGroups);
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("groupList", groupList);
        jsonReportData.put("countedReads", countedReads);
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

    private boolean checkQuality(NSequenceWithQuality seq) {
        return (readQualityFilter == 0) || (seq.getQuality().minValue() >= readQualityFilter);
    }

    private String getHeader() {
        StringBuilder header = new StringBuilder();
        for (String groupName : groupList) {
            header.append(groupName).append(".seq ");
            header.append(groupName).append(".qual.min ");
            header.append(groupName).append(".qual.avg ");
        }
        header.append("count percent");
        return header.toString();
    }

    private class StatGroupsKey {
        // order in array is the same as in input groupList
        final NucleotideSequence[] sequences = new NucleotideSequence[groupList.size()];

        StatGroupsKey(List<NSequenceWithQuality> groupValues) {
            for (int i = 0; i < groupList.size(); i++)
                sequences[i] = groupValues.get(i).getSequence();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StatGroupsKey that = (StatGroupsKey)o;
            return Arrays.equals(sequences, that.sequences);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(sequences);
        }
    }

    private class StatGroupsValue {
        // order in arrays is the same as in input groupList
        final byte[] minQualities = new byte[groupList.size()];
        final long[] averageQualitySums = new long[groupList.size()];
        final ArrayList<ArrayList<Byte>> minPositionQualities = new ArrayList<>();
        final ArrayList<ArrayList<Long>> positionQualitySums = new ArrayList<>();
        long count;

        StatGroupsValue(List<NSequenceWithQuality> groupValues) {
            for (int i = 0; i < groupList.size(); i++) {
                SequenceQuality currentQuality = groupValues.get(i).getQuality();
                minQualities[i] = currentQuality.minValue();
                averageQualitySums[i] = currentQuality.meanValue();
                ArrayList<Byte> currentMinPositionQualities = new ArrayList<>();
                ArrayList<Long> currentSumPositionQualities = new ArrayList<>();
                for (int j = 0; j < groupValues.get(i).size(); j++) {
                    currentMinPositionQualities.add(currentQuality.value(j));
                    currentSumPositionQualities.add((long)currentQuality.value(j));
                }
                minPositionQualities.add(currentMinPositionQualities);
                positionQualitySums.add(currentSumPositionQualities);
            }
            count = 1;
        }

        void countNewValue(List<NSequenceWithQuality> groupValues) {
            for (int i = 0; i < groupList.size(); i++) {
                SequenceQuality currentQuality = groupValues.get(i).getQuality();
                if (currentQuality.minValue() < minQualities[i])
                    minQualities[i] = currentQuality.minValue();
                averageQualitySums[i] += currentQuality.meanValue();
                ArrayList<Byte> currentMinPositionQualities = minPositionQualities.get(i);
                ArrayList<Long> currentSumPositionQualities = positionQualitySums.get(i);
                for (int j = 0; j < groupValues.get(i).size(); j++) {
                    if (currentQuality.value(j) < currentMinPositionQualities.get(j))
                        currentMinPositionQualities.set(j, currentQuality.value(j));
                    currentSumPositionQualities.set(j, currentSumPositionQualities.get(j) + currentQuality.value(j));
                }
            }
            count++;
        }

        byte getMinQuality(int groupIndex) {
            return minQualities[groupIndex];
        }

        byte getAvgQuality(int groupIndex) {
            return (byte)(averageQualitySums[groupIndex] / count);
        }

        SequenceQuality getMinSequenceQuality(int groupIndex) {
            ArrayList<Byte> currentMinPositionQualities = minPositionQualities.get(groupIndex);
            byte[] quality = new byte[currentMinPositionQualities.size()];
            for (int j = 0; j < currentMinPositionQualities.size(); j++)
                quality[j] = currentMinPositionQualities.get(j);
            return new SequenceQuality(quality);
        }

        SequenceQuality getAvgSequenceQuality(int groupIndex) {
            ArrayList<Long> currentSumPositionQualities = positionQualitySums.get(groupIndex);
            byte[] quality = new byte[currentSumPositionQualities.size()];
            for (int j = 0; j < currentSumPositionQualities.size(); j++)
                quality[j] = (byte)(currentSumPositionQualities.get(j) / count);
            return new SequenceQuality(quality);
        }
    }

    private class StatGroupsTableLine implements Comparable<StatGroupsTableLine> {
        // order in arrays is the same as in input groupList
        final NucleotideSequence[] sequences;
        final SequenceQuality[] minQualities = new SequenceQuality[groupList.size()];
        final SequenceQuality[] avgQualities = new SequenceQuality[groupList.size()];
        final long count;

        StatGroupsTableLine(HashMap.Entry<StatGroupsKey, StatGroupsValue> entry) {
            this.sequences = entry.getKey().sequences;
            StatGroupsValue value = entry.getValue();
            this.count = value.count;
            for (int i = 0; i < groupList.size(); i++) {
                minQualities[i] = value.getMinSequenceQuality(i);
                avgQualities[i] = value.getAvgSequenceQuality(i);
            }
        }

        @Override
        public int compareTo(StatGroupsTableLine that) {
            return Long.compare(that.count, count);     // reversed to start from bigger counts
        }

        String getTableLine() {
            StringBuilder line = new StringBuilder();
            for (int groupIndex = 0; groupIndex < groupList.size(); groupIndex++) {
                line.append(sequences[groupIndex]).append(' ');
                line.append(minQualities[groupIndex]).append(' ');
                line.append(avgQualities[groupIndex]).append(' ');
            }
            line.append(count).append(' ');
            float percent = (totalReads == 0) ? 0 : (float)count / totalReads * 100;
            line.append(floatFormat.format(percent)).append('%');
            return line.toString();
        }
    }
}
