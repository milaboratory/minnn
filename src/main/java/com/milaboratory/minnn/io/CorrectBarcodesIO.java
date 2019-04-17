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

import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.minnn.correct.BarcodeClusteringStrategy;
import com.milaboratory.minnn.correct.CorrectionStats;
import com.milaboratory.minnn.stat.SimpleMutationProbability;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.cli.CliUtils.floatFormat;
import static com.milaboratory.minnn.correct.CorrectionAlgorithms.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class CorrectBarcodesIO {
    private final PipelineConfiguration pipelineConfiguration;
    private final String inputFileName;
    private final String outputFileName;
    private final List<String> groupNames;
    private final LinkedHashSet<String> primaryGroups;
    private final BarcodeClusteringStrategy barcodeClusteringStrategy;
    private final int maxUniqueBarcodes;
    private final int minCount;
    private final String excludedBarcodesOutputFileName;
    private final long inputReadsLimit;
    private final boolean suppressWarnings;
    private final String reportFileName;
    private final String jsonReportFileName;

    public CorrectBarcodesIO(PipelineConfiguration pipelineConfiguration, String inputFileName, String outputFileName,
                             int mismatches, int indels, int totalErrors, float threshold, List<String> groupNames,
                             List<String> primaryGroupNames, int maxClusterDepth, float singleSubstitutionProbability,
                             float singleIndelProbability, int maxUniqueBarcodes, int minCount,
                             String excludedBarcodesOutputFileName, long inputReadsLimit, boolean suppressWarnings,
                             String reportFileName, String jsonReportFileName) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.groupNames = groupNames;
        this.primaryGroups = (primaryGroupNames == null) ? new LinkedHashSet<>()
                : new LinkedHashSet<>(primaryGroupNames);
        this.barcodeClusteringStrategy = new BarcodeClusteringStrategy(mismatches, indels, totalErrors, threshold,
                maxClusterDepth, new SimpleMutationProbability(singleSubstitutionProbability, singleIndelProbability));
        this.maxUniqueBarcodes = maxUniqueBarcodes;
        this.minCount = minCount;
        this.excludedBarcodesOutputFileName = excludedBarcodesOutputFileName;
        this.inputReadsLimit = inputReadsLimit;
        this.suppressWarnings = suppressWarnings;
        this.reportFileName = reportFileName;
        this.jsonReportFileName = jsonReportFileName;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        CorrectionStats stats;
        try (MifReader pass1Reader = new MifReader(inputFileName);
             MifReader pass2Reader = (primaryGroups.size() == 0) ? new MifReader(inputFileName) : null;
             MifWriter writer = Objects.requireNonNull(createWriter(pass1Reader.getHeader(), false));
             MifWriter excludedBarcodesWriter = createWriter(pass1Reader.getHeader(), true)) {
            if (inputReadsLimit > 0) {
                pass1Reader.setParsedReadsLimit(inputReadsLimit);
                if (primaryGroups.size() == 0)
                    Objects.requireNonNull(pass2Reader).setParsedReadsLimit(inputReadsLimit);
            }
            Set<String> defaultGroups = IntStream.rangeClosed(1, pass1Reader.getNumberOfTargets())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());
            if (groupNames.stream().anyMatch(defaultGroups::contains))
                throw exitWithError("Default groups R1, R2, etc should not be specified for correction!");
            LinkedHashSet<String> keyGroups = new LinkedHashSet<>(groupNames);
            if (!suppressWarnings && (pass1Reader.getSortedGroups().size() > 0) && (primaryGroups.size() == 0))
                System.err.println("WARNING: correcting sorted MIF file; output file will be unsorted!");
            LinkedHashSet<String> unsortedPrimaryGroups = new LinkedHashSet<>(primaryGroups);
            unsortedPrimaryGroups.removeAll(pass1Reader.getSortedGroups());
            if (!suppressWarnings && (unsortedPrimaryGroups.size() > 0))
                System.err.println("WARNING: correcting MIF file with unsorted primary groups " +
                        unsortedPrimaryGroups + "; correction will be slower and more memory consuming!");
            List<String> correctedAgainGroups = keyGroups.stream().filter(gn -> pass1Reader.getCorrectedGroups()
                    .stream().anyMatch(gn::equals)).collect(Collectors.toList());
            if (!suppressWarnings && (correctedAgainGroups.size() != 0))
                System.err.println("WARNING: group(s) " + correctedAgainGroups + " already corrected and will be " +
                        "corrected again!");
            if (primaryGroups.size() == 0)
                stats = fullFileCorrect(pass1Reader, pass2Reader, writer, excludedBarcodesWriter, inputReadsLimit,
                        barcodeClusteringStrategy, defaultGroups, keyGroups, maxUniqueBarcodes, minCount);
            else if (unsortedPrimaryGroups.size() == 0)
                stats = sortedClustersCorrect(pass1Reader, writer, excludedBarcodesWriter, inputReadsLimit,
                        barcodeClusteringStrategy, defaultGroups, primaryGroups, keyGroups, maxUniqueBarcodes,
                        minCount);
            else
                stats = unsortedClustersCorrect(pass1Reader, writer, excludedBarcodesWriter, inputReadsLimit,
                        barcodeClusteringStrategy, defaultGroups, primaryGroups, keyGroups, maxUniqueBarcodes,
                        minCount);
            pass1Reader.close();
            writer.setOriginalNumberOfReads(pass1Reader.getOriginalNumberOfReads());
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        StringBuilder reportFileHeader = new StringBuilder();
        StringBuilder report = new StringBuilder();
        LinkedHashMap<String, Object> jsonReportData = new LinkedHashMap<>();

        reportFileHeader.append("Report for Correct command:\n");
        if (inputFileName == null)
            reportFileHeader.append("Input is from stdin\n");
        else
            reportFileHeader.append("Input file name: ").append(inputFileName).append('\n');
        if (outputFileName == null)
            reportFileHeader.append("Output is to stdout\n");
        else
            reportFileHeader.append("Output file name: ").append(outputFileName).append('\n');
        reportFileHeader.append("Corrected groups: ").append(groupNames).append('\n');
        if (primaryGroups.size() > 0)
            reportFileHeader.append("Primary groups: ").append(primaryGroups).append('\n');

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        float percent = (stats.totalReads == 0) ? 0 : (float)stats.correctedReads / stats.totalReads * 100;
        report.append("Processed ").append(stats.totalReads).append(" reads").append('\n');
        report.append("Reads with corrected barcodes: ").append(stats.correctedReads).append(" (")
                .append(floatFormat.format(percent)).append("%)\n");
        if (stats.excludedReads > 0)
            report.append("Reads excluded by too low barcode count: ").append(stats.excludedReads).append(" (")
                    .append(floatFormat.format((float)stats.excludedReads / stats.totalReads * 100)).append("%)\n");

        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("groupNames", groupNames);
        jsonReportData.put("primaryGroups", primaryGroups);
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("correctedReads", stats.correctedReads);
        jsonReportData.put("excludedReads", stats.excludedReads);
        jsonReportData.put("totalReads", stats.totalReads);

        humanReadableReport(reportFileName, reportFileHeader.toString(), report.toString());
        jsonReport(jsonReportFileName, jsonReportData);
    }

    private MifWriter createWriter(MifHeader inputHeader, boolean excludedBarcodes) throws IOException {
        LinkedHashSet<String> allCorrectedGroups = new LinkedHashSet<>(inputHeader.getCorrectedGroups());
        allCorrectedGroups.addAll(groupNames);
        MifHeader outputHeader = new MifHeader(pipelineConfiguration, inputHeader.getNumberOfTargets(),
                new ArrayList<>(allCorrectedGroups), new ArrayList<>(), inputHeader.getGroupEdges());
        if (excludedBarcodes)
            return (excludedBarcodesOutputFileName == null) ? null
                    : new MifWriter(excludedBarcodesOutputFileName, outputHeader);
        else
            return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                    : new MifWriter(outputFileName, outputHeader);
    }
}
