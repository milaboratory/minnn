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
import cc.redberry.pipe.OutputPort;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.minnn.correct.*;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.correct.CorrectionAlgorithms.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.FormatUtils.nanoTimeToString;

public final class CorrectBarcodesIO {
    private final PipelineConfiguration pipelineConfiguration;
    private final CorrectionAlgorithms correctionAlgorithms;
    private final String inputFileName;
    private final String outputFileName;
    private final LinkedHashSet<String> keyGroups;
    private final LinkedHashSet<String> primaryGroups;
    private final int maxUniqueBarcodes;
    private final int minCount;
    private final String excludedBarcodesOutputFileName;
    private final float wildcardsCollapsingMergeThreshold;
    private final long inputReadsLimit;
    private final boolean suppressWarnings;
    private final int threads;
    private final String reportFileName;
    private final String jsonReportFileName;
    private final AtomicLong totalReads = new AtomicLong(0);

    public CorrectBarcodesIO(
            PipelineConfiguration pipelineConfiguration, String inputFileName, String outputFileName,
            List<String> groupNames, List<String> primaryGroupNames,
            BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory, int maxUniqueBarcodes, int minCount,
            String excludedBarcodesOutputFileName, float wildcardsCollapsingMergeThreshold, long inputReadsLimit,
            boolean suppressWarnings, int threads, String reportFileName, String jsonReportFileName) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.correctionAlgorithms = new CorrectionAlgorithms(barcodeClusteringStrategyFactory,
                maxUniqueBarcodes, minCount, wildcardsCollapsingMergeThreshold);
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.keyGroups = new LinkedHashSet<>(groupNames);
        this.primaryGroups = (primaryGroupNames == null) ? new LinkedHashSet<>()
                : new LinkedHashSet<>(primaryGroupNames);
        this.maxUniqueBarcodes = maxUniqueBarcodes;
        this.minCount = minCount;
        this.excludedBarcodesOutputFileName = excludedBarcodesOutputFileName;
        this.wildcardsCollapsingMergeThreshold = wildcardsCollapsingMergeThreshold;
        this.inputReadsLimit = inputReadsLimit;
        this.suppressWarnings = suppressWarnings;
        this.threads = threads;
        this.reportFileName = reportFileName;
        this.jsonReportFileName = jsonReportFileName;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        CorrectionStats stats = new CorrectionStats();
        try (MifReader pass1Reader = new MifReader(inputFileName);
             MifReader pass2Reader = new MifReader(inputFileName);
             MifWriter writer = Objects.requireNonNull(createWriter(pass1Reader.getHeader(), false));
             MifWriter excludedBarcodesWriter = createWriter(pass1Reader.getHeader(), true)) {
            if (inputReadsLimit > 0) {
                pass1Reader.setParsedReadsLimit(inputReadsLimit);
                pass2Reader.setParsedReadsLimit(inputReadsLimit);
            }

            validateInputGroups(pass1Reader, keyGroups, false, "--groups");
            validateInputGroups(pass1Reader, primaryGroups, false, "--primary-groups");
            LinkedHashSet<String> groupsIntersection = new LinkedHashSet<>(keyGroups);
            groupsIntersection.retainAll(primaryGroups);
            if (groupsIntersection.size() > 0)
                throw exitWithError("ERROR: Group(s) " + groupsIntersection +
                        " specified in both --groups and --primary-groups arguments!");
            LinkedHashSet<String> expectedSortingOrder = new LinkedHashSet<>();
            List<String> actualSortingOrder = pass1Reader.getSortedGroups();
            expectedSortingOrder.addAll(primaryGroups);
            expectedSortingOrder.addAll(keyGroups);
            int actualSortingOrderIndex = 0;
            boolean orderIsCorrect = true;
            for (String groupName : expectedSortingOrder) {
                boolean groupIsFound = false;
                while (orderIsCorrect && !groupIsFound) {
                    if (actualSortingOrderIndex == actualSortingOrder.size()) {
                        orderIsCorrect = false;
                        break;
                    }
                    if (actualSortingOrder.get(actualSortingOrderIndex).equals(groupName))
                        groupIsFound = true;
                    else
                        actualSortingOrderIndex++;
                }
            }
            if (!orderIsCorrect)
                throw exitWithError("ERROR: all groups used in correction must be sorted with sort command " +
                        "in exactly the same order as they used in correct command. If --primary-groups argument " +
                        "is used, primary groups must be sorted too and must be first in the list of groups in " +
                        "--groups argument of sort command. Expected sorting order: " + expectedSortingOrder +
                        ", actual sorting order found in the input: " + actualSortingOrder);
            List<String> correctedAgainGroups = keyGroups.stream().filter(gn -> pass1Reader.getCorrectedGroups()
                    .stream().anyMatch(gn::equals)).collect(Collectors.toList());
            if (!suppressWarnings && (correctedAgainGroups.size() != 0))
                System.err.println("WARNING: group(s) " + correctedAgainGroups + " already corrected and will be " +
                        "corrected again!");

            OutputPort<CorrectionQualityPreprocessingResult> preprocessorPort = getPreprocessingResultOutputPort(
                    getParsedReadOutputPort(pass1Reader, "Counting barcodes", false),
                    keyGroups, primaryGroups);
            OutputPort<ParsedRead> pass2RawReadsPort = getParsedReadOutputPort(
                    pass2Reader, "Correcting barcodes", true);

            if (primaryGroups.size() > 0) {
                // secondary barcodes correction
                OutputPort<CorrectionData> correctionDataPort = performSecondaryBarcodesCorrection(preprocessorPort,
                        correctionAlgorithms, keyGroups, threads);
                for (CorrectionData correctionData : CUtils.it(correctionDataPort)) {
                    CorrectionStats statsForCurrentPrimaryGroups = correctionAlgorithms.correctAndWrite(
                            correctionData, pass2RawReadsPort, writer, excludedBarcodesWriter);
                    stats.add(statsForCurrentPrimaryGroups);
                }
            } else {
                // full file correction
                CorrectionData correctionData = correctionAlgorithms.prepareCorrectionData(preprocessorPort,
                        keyGroups, 0);
                stats.add(correctionAlgorithms.correctAndWrite(correctionData, pass2RawReadsPort,
                        writer, excludedBarcodesWriter));
            }
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
        reportFileHeader.append("Report for Correct command:\n");
        reportFileHeader.append("Input file name: ").append(inputFileName).append('\n');
        if (outputFileName == null)
            reportFileHeader.append("Output is to stdout\n");
        else
            reportFileHeader.append("Output file name: ").append(outputFileName).append('\n');
        if (excludedBarcodesOutputFileName != null)
            reportFileHeader.append("Output file for excluded reads: ").append(excludedBarcodesOutputFileName)
                    .append('\n');
        reportFileHeader.append("Corrected groups: ").append(keyGroups).append('\n');
        if (primaryGroups.size() > 0)
            reportFileHeader.append("Primary groups: ").append(primaryGroups).append('\n');
        reportFileHeader.append("Stats for 1st stage correction (merging by wildcards):\n");
        reportFileHeader.append("Clusters checked for possible merge on wildcards processing stage: ")
                .append(stats.wildcardCanAddToClusterCalls).append('\n');
        float wildcardClusterNotAddedByThresholdPercent = (stats.wildcardCanAddToClusterCalls == 0) ? 0
                : (float)stats.wildcardClusterNotAddedByThreshold / stats.wildcardCanAddToClusterCalls * 100;
        reportFileHeader.append("Wildcard clusters not merged by size threshold: ")
                .append(stats.wildcardClusterNotAddedByThreshold).append(" (")
                .append(floatFormat.format(wildcardClusterNotAddedByThresholdPercent)).append("%)\n");
        reportFileHeader.append("Stats for 2nd stage correction (correction of mutations in barcodes):\n");
        reportFileHeader.append("Clusters checked for possible merge on barcodes correction stage: ")
                .append(stats.barcodeCanAddToClusterCalls).append('\n');
        float barcodeClusterNotAddedByWildcardsPercent = (stats.barcodeCanAddToClusterCalls == 0) ? 0
                : (float)stats.barcodeClusterNotAddedByWildcards / stats.barcodeCanAddToClusterCalls * 100;
        float barcodeClusterNotAddedByExpectedCountPercent = (stats.barcodeCanAddToClusterCalls == 0) ? 0
                : (float)stats.barcodeClusterNotAddedByExpectedCount / stats.barcodeCanAddToClusterCalls * 100;
        float barcodeClusterNotAddedByThresholdPercent = (stats.barcodeCanAddToClusterCalls == 0) ? 0
                : (float)stats.barcodeClusterNotAddedByThreshold / stats.barcodeCanAddToClusterCalls * 100;
        reportFileHeader.append("Barcode clusters not merged because they are equal by wildcards ")
                .append("and were not previously merged on wildcards processing stage: ")
                .append(stats.barcodeClusterNotAddedByWildcards).append(" (")
                .append(floatFormat.format(barcodeClusterNotAddedByWildcardsPercent)).append("%)\n");
        reportFileHeader.append("Barcode clusters not merged because minor cluster count was bigger ")
                .append("than expected with specified mutation probabilities: ")
                .append(stats.barcodeClusterNotAddedByExpectedCount).append(" (")
                .append(floatFormat.format(barcodeClusterNotAddedByExpectedCountPercent)).append("%)\n");
        reportFileHeader.append("Barcode clusters not merged by size threshold: ")
                .append(stats.barcodeClusterNotAddedByThreshold).append(" (")
                .append(floatFormat.format(barcodeClusterNotAddedByThresholdPercent)).append("%)\n");

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        float correctedPercent = (totalReads.get() == 0) ? 0 : (float)stats.correctedReads / totalReads.get() * 100;
        report.append("Processed ").append(totalReads).append(" reads").append('\n');
        report.append("Reads with corrected barcodes: ").append(stats.correctedReads).append(" (")
                .append(floatFormat.format(correctedPercent)).append("%)\n");
        float qualityUpdatedPercent = (totalReads.get() == 0)
                ? 0 : (float)stats.updatedQualityReads / totalReads.get() * 100;
        report.append("Reads with not changed barcode sequences, but updated qualities: ")
                .append(stats.updatedQualityReads).append(" (")
                .append(floatFormat.format(qualityUpdatedPercent)).append("%)\n");
        if (stats.excludedReads > 0)
            report.append("Reads excluded by low barcode count: ").append(stats.excludedReads).append(" (")
                    .append(floatFormat.format((float)stats.excludedReads / totalReads.get() * 100)).append("%)\n");
        if (stats.totalNucleotides > 0)
            report.append("Wildcards in barcodes: ").append(stats.totalWildcards).append(" (")
                    .append(floatFormat.format((float)stats.totalWildcards / stats.totalNucleotides * 100))
                    .append("% of all letters in barcodes)\n");

        jsonReportData.put("version", getShortestVersionString());
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("excludedBarcodesOutputFileName", excludedBarcodesOutputFileName);
        jsonReportData.put("keyGroups", keyGroups);
        jsonReportData.put("primaryGroups", primaryGroups);
        jsonReportData.put("maxUniqueBarcodes", maxUniqueBarcodes);
        jsonReportData.put("minCount", minCount);
        jsonReportData.put("wildcardsCollapsingMergeThreshold", wildcardsCollapsingMergeThreshold);
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("correctedReads", stats.correctedReads);
        jsonReportData.put("updatedQualityReads", stats.updatedQualityReads);
        jsonReportData.put("excludedReads", stats.excludedReads);
        jsonReportData.put("totalReads", totalReads);
        jsonReportData.put("totalWildcards", stats.totalWildcards);
        jsonReportData.put("totalNucleotides", stats.totalNucleotides);
        jsonReportData.put("wildcardClusterNotAddedByThreshold", stats.wildcardClusterNotAddedByThreshold);
        jsonReportData.put("wildcardCanAddToClusterCalls", stats.wildcardCanAddToClusterCalls);
        jsonReportData.put("barcodeClusterNotAddedByWildcards", stats.barcodeClusterNotAddedByWildcards);
        jsonReportData.put("barcodeClusterNotAddedByExpectedCount", stats.barcodeClusterNotAddedByExpectedCount);
        jsonReportData.put("barcodeClusterNotAddedByThreshold", stats.barcodeClusterNotAddedByThreshold);
        jsonReportData.put("barcodeCanAddToClusterCalls", stats.barcodeCanAddToClusterCalls);

        humanReadableReport(reportFileName, reportFileHeader.toString(), report.toString());
        jsonReport(jsonReportFileName, jsonReportData);
    }

    private MifWriter createWriter(MifHeader inputHeader, boolean excludedBarcodes) throws IOException {
        LinkedHashSet<String> allCorrectedGroups = new LinkedHashSet<>(inputHeader.getCorrectedGroups());
        allCorrectedGroups.addAll(keyGroups);
        MifHeader outputHeader = new MifHeader(pipelineConfiguration, inputHeader.getNumberOfTargets(),
                new ArrayList<>(allCorrectedGroups), new ArrayList<>(), inputHeader.getGroupEdges());
        if (excludedBarcodes)
            return (excludedBarcodesOutputFileName == null) ? null
                    : new MifWriter(excludedBarcodesOutputFileName, outputHeader);
        else
            return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                    : new MifWriter(outputFileName, outputHeader);
    }

    /**
     * Get port with parsed reads: MIF reader with inputReadsLimit checking.
     *
     * @param mifReader             MIF reader
     * @param progressReportHint    text to display in progress report messages
     * @param updateTotalReads      update total reads counter
     * @return                      output port with parsed reads
     */
    private OutputPort<ParsedRead> getParsedReadOutputPort(
            MifReader mifReader, String progressReportHint, boolean updateTotalReads) {
        if (updateTotalReads)
            return new OutputPort<ParsedRead>() {
                boolean progressReportingStarted = false;

                @Override
                public ParsedRead take() {
                    if (!progressReportingStarted) {
                        SmartProgressReporter.startProgressReport(progressReportHint, mifReader, System.err);
                        progressReportingStarted = true;
                    }
                    ParsedRead parsedRead = ((inputReadsLimit == 0) || (totalReads.get() < inputReadsLimit))
                            ? mifReader.take() : null;
                    if (parsedRead != null)
                        totalReads.getAndIncrement();
                    return parsedRead;
                }
            };
        else
            return new OutputPort<ParsedRead>() {
                long takenReads = 0;
                boolean progressReportingStarted = false;

                @Override
                public ParsedRead take() {
                    if (!progressReportingStarted) {
                        SmartProgressReporter.startProgressReport(progressReportHint, mifReader, System.err);
                        progressReportingStarted = true;
                    }
                    ParsedRead parsedRead = ((inputReadsLimit == 0) || (takenReads < inputReadsLimit))
                            ? mifReader.take() : null;
                    if (parsedRead != null)
                        takenReads++;
                    return parsedRead;
                }
            };
    }
}
