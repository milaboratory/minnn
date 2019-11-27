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
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.util.SmartProgressReporter;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.getShortestVersionString;
import static com.milaboratory.minnn.util.SystemUtils.exitWithError;
import static com.milaboratory.util.FormatUtils.nanoTimeToString;

public final class DecontaminateIO {
    private final PipelineConfiguration pipelineConfiguration;
    private final String inputFileName;
    private final String outputFileName;
    private final String excludedBarcodesOutputFileName;
    private final LinkedHashSet<String> groupNames;
    private final LinkedHashSet<String> primaryGroupNames;
    private final float minCountShare;
    private final long inputReadsLimit;
    private final String reportFileName;
    private final String jsonReportFileName;
    private long totalReads = 0;
    private long excludedReads = 0;

    public DecontaminateIO(
            PipelineConfiguration pipelineConfiguration, String inputFileName, String outputFileName,
            String excludedBarcodesOutputFileName, List<String> groupNames, List<String> primaryGroupNames,
            float minCountShare, long inputReadsLimit, String reportFileName, String jsonReportFileName) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.excludedBarcodesOutputFileName = excludedBarcodesOutputFileName;
        this.groupNames = new LinkedHashSet<>(groupNames);
        this.primaryGroupNames = new LinkedHashSet<>(primaryGroupNames);
        this.minCountShare = minCountShare;
        this.inputReadsLimit = inputReadsLimit;
        this.reportFileName = reportFileName;
        this.jsonReportFileName = jsonReportFileName;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        try (MifReader pass1Reader = new MifReader(inputFileName);
             MifReader pass2Reader = new MifReader(inputFileName);
             MifWriter writer = Objects.requireNonNull(createWriter(pass1Reader.getHeader(), false));
             MifWriter excludedBarcodesWriter = createWriter(pass1Reader.getHeader(), true)) {
            if (inputReadsLimit > 0) {
                pass1Reader.setParsedReadsLimit(inputReadsLimit);
                pass2Reader.setParsedReadsLimit(inputReadsLimit);
            }
            validateInputGroups(pass1Reader, groupNames, false, "--groups");
            validateInputGroups(pass1Reader, primaryGroupNames, false,
                    "--primary-groups");
            performDecontamination(pass1Reader, pass2Reader, writer, excludedBarcodesWriter);
            pass1Reader.close();
            writer.setOriginalNumberOfReads(pass1Reader.getOriginalNumberOfReads());
            if (excludedBarcodesWriter != null)
                excludedBarcodesWriter.setOriginalNumberOfReads(pass1Reader.getOriginalNumberOfReads());
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        StringBuilder reportFileHeader = new StringBuilder();
        StringBuilder report = new StringBuilder();
        LinkedHashMap<String, Object> jsonReportData = new LinkedHashMap<>();

        reportFileHeader.append("MiNNN v").append(getShortestVersionString()).append('\n');
        reportFileHeader.append("Report for Decontaminate command:\n");
        reportFileHeader.append("Input file name: ").append(inputFileName).append('\n');
        if (outputFileName == null)
            reportFileHeader.append("Output is to stdout\n");
        else
            reportFileHeader.append("Output file name: ").append(outputFileName).append('\n');
        if (excludedBarcodesOutputFileName != null)
            reportFileHeader.append("Output file for excluded reads: ").append(excludedBarcodesOutputFileName)
                    .append('\n');
        reportFileHeader.append("Decontaminated (molecule identification) groups: ").append(groupNames).append('\n');
        reportFileHeader.append("Primary (cell identification) groups: ").append(primaryGroupNames).append('\n');

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        float percent = (totalReads == 0) ? 0 : (float)excludedReads / totalReads * 100;
        report.append("Processed ").append(totalReads).append(" reads").append('\n');
        report.append("Excluded reads: ").append(excludedReads).append(" (")
                .append(floatFormat.format(percent)).append("%)\n");

        jsonReportData.put("version", getShortestVersionString());
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("excludedBarcodesOutputFileName", excludedBarcodesOutputFileName);
        jsonReportData.put("groupNames", groupNames);
        jsonReportData.put("primaryGroupNames", primaryGroupNames);
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("excludedReads", excludedReads);
        jsonReportData.put("totalReads", totalReads);
        jsonReportData.put("minCountShare", minCountShare);

        humanReadableReport(reportFileName, reportFileHeader.toString(), report.toString());
        jsonReport(jsonReportFileName, jsonReportData);
    }

    private MifWriter createWriter(MifHeader inputHeader, boolean excludedBarcodes) throws IOException {
        MifHeader outputHeader = new MifHeader(pipelineConfiguration, inputHeader.getNumberOfTargets(),
                inputHeader.getCorrectedGroups(), inputHeader.getSortedGroups(), inputHeader.getGroupEdges());
        if (excludedBarcodes)
            return (excludedBarcodesOutputFileName == null) ? null
                    : new MifWriter(excludedBarcodesOutputFileName, outputHeader);
        else
            return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                    : new MifWriter(outputFileName, outputHeader);
    }

    private void performDecontamination(
            MifReader pass1Reader, MifReader pass2Reader, MifWriter writer, MifWriter excludedBarcodesWriter) {
        Map<Set<NucleotideSequence>, Set<NucleotideSequence>> barcodeCombinationsCache = new HashMap<>();
        /* outer map keys: unique molecular identifiers (barcodes or sets of barcodes)
           inner map keys: unique cell identifiers (barcodes or sets of barcodes)
           inner map values: counts of the molecule in the cell */
        Map<Set<NucleotideSequence>, Map<Set<NucleotideSequence>, BarcodeCountInCell>> allCounts = new HashMap<>();
        TObjectLongHashMap<Set<NucleotideSequence>> barcodeCountThresholds = new TObjectLongHashMap<>();

        // 1st pass: counting barcodes in each cell
        SmartProgressReporter.startProgressReport("Counting barcodes", pass1Reader, System.err);
        for (ParsedRead parsedRead : CUtils.it(pass1Reader)) {
            Set<NucleotideSequence> groupValues = groupNames.stream()
                    .map(name -> parsedRead.getGroupValue(name).getSequence()).collect(Collectors.toSet());
            Set<NucleotideSequence> primaryGroupValues = primaryGroupNames.stream()
                    .map(name -> parsedRead.getGroupValue(name).getSequence()).collect(Collectors.toSet());
            Set<NucleotideSequence> cachedGroupValues = barcodeCombinationsCache.get(groupValues);
            if (cachedGroupValues != null)
                groupValues = cachedGroupValues;
            else
                barcodeCombinationsCache.put(groupValues, groupValues);
            Set<NucleotideSequence> cachedPrimaryGroupValues = barcodeCombinationsCache.get(primaryGroupValues);
            if (cachedPrimaryGroupValues != null)
                primaryGroupValues = cachedPrimaryGroupValues;
            else
                barcodeCombinationsCache.put(primaryGroupValues, primaryGroupValues);

            Map<Set<NucleotideSequence>, BarcodeCountInCell> currentBarcodeCounts = allCounts.get(groupValues);
            if (currentBarcodeCounts != null) {
                BarcodeCountInCell countInCurrentCell = currentBarcodeCounts.get(primaryGroupValues);
                if (countInCurrentCell != null)
                    countInCurrentCell.count++;
                else
                    currentBarcodeCounts.put(primaryGroupValues, new BarcodeCountInCell(primaryGroupValues));
            } else {
                currentBarcodeCounts = new HashMap<>();
                currentBarcodeCounts.put(primaryGroupValues, new BarcodeCountInCell(primaryGroupValues));
                allCounts.put(groupValues, currentBarcodeCounts);
            }

            if (++totalReads == inputReadsLimit)
                break;
        }

        allCounts.forEach((k, v) -> barcodeCountThresholds.put(k, (long)(v.values().stream()
                .mapToLong(BarcodeCountInCell::getCount).max().orElseThrow(IllegalStateException::new)
                * minCountShare)));

        // 2nd pass: filtering reads
        totalReads = 0;
        SmartProgressReporter.startProgressReport("Filtering reads", pass2Reader, System.err);
        for (ParsedRead parsedRead : CUtils.it(pass2Reader)) {
            Set<NucleotideSequence> groupValues = groupNames.stream()
                    .map(name -> parsedRead.getGroupValue(name).getSequence()).collect(Collectors.toSet());
            Set<NucleotideSequence> primaryGroupValues = primaryGroupNames.stream()
                    .map(name -> parsedRead.getGroupValue(name).getSequence()).collect(Collectors.toSet());
            long barcodeCount = allCounts.get(groupValues).get(primaryGroupValues).getCount();
            long threshold = barcodeCountThresholds.get(groupValues);
            if (barcodeCount < threshold) {
                if (excludedBarcodesWriter != null)
                    excludedBarcodesWriter.write(parsedRead);
                excludedReads++;
            } else
                writer.write(parsedRead);
            if (++totalReads == inputReadsLimit)
                break;
        }
    }

    private static class BarcodeCountInCell {
        final Set<NucleotideSequence> primaryGroupsValues;
        long count;

        public BarcodeCountInCell(Set<NucleotideSequence> primaryGroupsValues) {
            this.primaryGroupsValues = primaryGroupsValues;
            count = 1;
        }

        public long getCount() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BarcodeCountInCell that = (BarcodeCountInCell)o;
            return primaryGroupsValues.equals(that.primaryGroupsValues);
        }

        @Override
        public int hashCode() {
            return primaryGroupsValues.hashCode();
        }
    }
}
