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
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.minnn.correct.SequenceCounter;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.FormatUtils.nanoTimeToString;

public final class FilterByCountIO {
    private final PipelineConfiguration pipelineConfiguration;
    private final String inputFileName;
    private final String outputFileName;
    private final LinkedHashSet<String> keyGroups;
    private final int maxUniqueBarcodes;
    private final int minCount;
    private final String excludedBarcodesOutputFileName;
    private final long inputReadsLimit;
    private final String reportFileName;
    private final String jsonReportFileName;
    private final Map<String, Set<NucleotideSequence>> includedBarcodes = new HashMap<>();
    private long totalReads = 0;
    private long excludedReads = 0;

    public FilterByCountIO(
            PipelineConfiguration pipelineConfiguration, String inputFileName, String outputFileName,
            List<String> groupNames, int maxUniqueBarcodes, int minCount, String excludedBarcodesOutputFileName,
            long inputReadsLimit, String reportFileName, String jsonReportFileName) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.keyGroups = new LinkedHashSet<>(groupNames);
        this.maxUniqueBarcodes = maxUniqueBarcodes;
        this.minCount = minCount;
        this.excludedBarcodesOutputFileName = excludedBarcodesOutputFileName;
        this.inputReadsLimit = inputReadsLimit;
        this.reportFileName = reportFileName;
        this.jsonReportFileName = jsonReportFileName;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        try (MifReader pass1Reader = new MifReader(inputFileName);
             MifReader pass2Reader = new MifReader(inputFileName);
             MifWriter writer = Objects.requireNonNull(createWriter(pass1Reader.getMetaInfo(), false));
             MifWriter excludedBarcodesWriter = createWriter(pass1Reader.getMetaInfo(), true)) {
            if (inputReadsLimit > 0) {
                pass1Reader.setParsedReadsLimit(inputReadsLimit);
                pass2Reader.setParsedReadsLimit(inputReadsLimit);
            }
            validateInputGroups(pass1Reader, keyGroups, false, "--groups");

            SmartProgressReporter.startProgressReport("Counting barcodes", pass1Reader, System.err);
            calculateIncludedBarcodes(pass1Reader);

            SmartProgressReporter.startProgressReport("Filtering by count", pass2Reader, System.err);
            for (ParsedRead parsedRead : CUtils.it(pass2Reader)) {
                boolean excluded = false;
                for (String currentGroup : keyGroups)
                    if (!includedBarcodes.get(currentGroup)
                            .contains(parsedRead.getGroupValue(currentGroup).getSequence())) {
                        excluded = true;
                        break;
                    }
                if (excluded) {
                    if (excludedBarcodesWriter != null)
                        excludedBarcodesWriter.write(parsedRead);
                    excludedReads++;
                } else
                    writer.write(parsedRead);
                if (++totalReads == inputReadsLimit)
                    break;
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        StringBuilder reportFileHeader = new StringBuilder();
        StringBuilder report = new StringBuilder();
        LinkedHashMap<String, Object> jsonReportData = new LinkedHashMap<>();

        reportFileHeader.append("MiNNN v").append(getVersionString(VERSION_INFO_SHORTEST)).append('\n');
        reportFileHeader.append("Report for Filter by Count command:\n");
        reportFileHeader.append("Input file name: ").append(inputFileName).append('\n');
        if (outputFileName == null)
            reportFileHeader.append("Output is to stdout\n");
        else
            reportFileHeader.append("Output file name: ").append(outputFileName).append('\n');
        if (excludedBarcodesOutputFileName != null)
            reportFileHeader.append("Output file for excluded reads: ").append(excludedBarcodesOutputFileName)
                    .append('\n');
        reportFileHeader.append("Corrected groups: ").append(keyGroups).append('\n');

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        report.append("Processed ").append(totalReads).append(" reads").append('\n');
        if (excludedReads > 0)
            report.append("Reads excluded by low barcode count: ").append(excludedReads).append(" (")
                    .append(floatFormat.format((float)excludedReads / totalReads * 100)).append("%)\n");

        jsonReportData.put("version", getVersionString(VERSION_INFO_SHORTEST));
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("excludedBarcodesOutputFileName", excludedBarcodesOutputFileName);
        jsonReportData.put("keyGroups", keyGroups);
        jsonReportData.put("maxUniqueBarcodes", maxUniqueBarcodes);
        jsonReportData.put("minCount", minCount);
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("excludedReads", excludedReads);
        jsonReportData.put("totalReads", totalReads);

        humanReadableReport(reportFileName, reportFileHeader.toString(), report.toString());
        jsonReport(jsonReportFileName, jsonReportData);
    }

    private MifWriter createWriter(MifMetaInfo inputMetaInfo, boolean excludedBarcodes) throws IOException {
        MifMetaInfo outputMetaInfo = new MifMetaInfo(pipelineConfiguration, inputMetaInfo.getNumberOfTargets(),
                inputMetaInfo.getCorrectedGroups(), inputMetaInfo.getSortedGroups(), inputMetaInfo.getGroupEdges(),
                inputMetaInfo.getOriginalNumberOfReads());
        if (excludedBarcodes)
            return (excludedBarcodesOutputFileName == null) ? null
                    : new MifWriter(excludedBarcodesOutputFileName, outputMetaInfo);
        else
            return new MifWriter(outputFileName, outputMetaInfo);
    }

    private void calculateIncludedBarcodes(MifReader pass1Reader) {
        Map<String, Map<NucleotideSequence, SequenceCounter>> sequenceCounters = new HashMap<>();
        for (String groupName : keyGroups) {
            sequenceCounters.put(groupName, new HashMap<>());
            includedBarcodes.put(groupName, new HashSet<>());
        }
        long processedReads = 0;
        for (ParsedRead parsedRead : CUtils.it(pass1Reader)) {
            for (String groupName : keyGroups) {
                Map<NucleotideSequence, SequenceCounter> currentGroupCounters = sequenceCounters.get(groupName);
                NucleotideSequence seq = parsedRead.getGroupValue(groupName).getSequence();
                currentGroupCounters.putIfAbsent(seq, new SequenceCounter(seq));
                currentGroupCounters.get(seq).count++;
            }
            if (++processedReads == inputReadsLimit)
                break;
        }
        // filtering by count
        int maxUniqueBarcodesLimit = (maxUniqueBarcodes == 0) ? Integer.MAX_VALUE : maxUniqueBarcodes;
        for (String groupName : keyGroups) {
            Map<NucleotideSequence, SequenceCounter> currentGroupCounters = sequenceCounters.get(groupName);
            Set<NucleotideSequence> currentGroupIncludedBarcodes = includedBarcodes.get(groupName);
            new TreeSet<>(currentGroupCounters.values()).stream()
                    .limit(maxUniqueBarcodesLimit).filter(counter -> counter.count >= minCount)
                    .map(counter -> counter.seq).forEach(currentGroupIncludedBarcodes::add);
        }
    }
}
