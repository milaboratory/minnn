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
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.blocks.ParallelProcessor;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.minnn.correct.*;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.correct.WildcardsCollapsingMethod.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.getShortestVersionString;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.FormatUtils.nanoTimeToString;

public final class CorrectBarcodesIO {
    private final PipelineConfiguration pipelineConfiguration;
    private final String inputFileName;
    private final String outputFileName;
    private final LinkedHashSet<String> keyGroups;
    private final LinkedHashSet<String> primaryGroups;
    private final BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory;
    private final int maxUniqueBarcodes;
    private final int minCount;
    private final String excludedBarcodesOutputFileName;
    private final WildcardsCollapsingMethod wildcardsCollapsingMethod;
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
            String excludedBarcodesOutputFileName, boolean fairWildcardsCollapsing, boolean disableWildcardsCollapsing,
            long inputReadsLimit, boolean suppressWarnings, int threads,
            String reportFileName, String jsonReportFileName) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.keyGroups = new LinkedHashSet<>(groupNames);
        this.primaryGroups = (primaryGroupNames == null) ? new LinkedHashSet<>()
                : new LinkedHashSet<>(primaryGroupNames);
        this.barcodeClusteringStrategyFactory = barcodeClusteringStrategyFactory;
        this.maxUniqueBarcodes = maxUniqueBarcodes;
        this.minCount = minCount;
        this.excludedBarcodesOutputFileName = excludedBarcodesOutputFileName;
        this.wildcardsCollapsingMethod = disableWildcardsCollapsing ? DISABLED_COLLAPSING
                : (fairWildcardsCollapsing ? FAIR_COLLAPSING : UNFAIR_COLLAPSING);
        this.inputReadsLimit = inputReadsLimit;
        this.suppressWarnings = suppressWarnings;
        this.threads = threads;
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
            validateInputGroups(pass1Reader, keyGroups, false, "--groups");
            if (primaryGroups.size() > 0)
                validateInputGroups(pass1Reader, primaryGroups, false,
                        "--primary-groups");
            LinkedHashSet<String> notSortedGroups = new LinkedHashSet<>(keyGroups);
            notSortedGroups.removeAll(pass1Reader.getFullySortedGroups());

            OutputPort<CorrectionCluster> clusterOutputPort;
            if (notSortedGroups.size() > 0) {
                // not all groups are sorted; we must read the entire file into memory to create clusters
                System.err.println("WARNING: group(s) " + notSortedGroups + " not sorted, but specified for " +
                        "correction; correction will consume much more memory!");
                SmartProgressReporter.startProgressReport("Counting barcodes quality", pass1Reader, System.err);
                // keys: group names and values; values: created clusters
                Map<LinkedHashMap<String, NucleotideSequence>, CorrectionCluster> allClusters = new HashMap<>();
                AtomicLong orderedPortIndex = new AtomicLong(0);
                for (ParsedRead parsedRead : CUtils.it(pass1Reader)) {
                    LinkedHashMap<String, NucleotideSequence> groups = extractKeyGroups(parsedRead);
                    allClusters.computeIfAbsent(groups,
                            g -> new CorrectionCluster(orderedPortIndex.getAndIncrement()));
                    allClusters.get(groups).groupValues.add(extractGroupValues(parsedRead));
                    if (totalReads.incrementAndGet() == inputReadsLimit)
                        break;
                }
                clusterOutputPort = new OutputPort<CorrectionCluster>() {
                    Iterator<CorrectionCluster> clusters = allClusters.values().iterator();

                    @Override
                    public synchronized CorrectionCluster take() {
                        if (!clusters.hasNext())
                            return null;
                        return clusters.next();
                    }
                };
            } else {
                SmartProgressReporter.startProgressReport("Counting barcodes quality", pass1Reader, System.err);
                // all groups are sorted; we can add input reads to the cluster while their group values are the same
                clusterOutputPort = new OutputPort<CorrectionCluster>() {
                    LinkedHashMap<String, NucleotideSequence> previousGroups = null;
                    CorrectionCluster currentCluster = new CorrectionCluster(0);
                    int orderedPortIndex = 0;
                    boolean finished = false;

                    @Override
                    public synchronized CorrectionCluster take() {
                        if (finished)
                            return null;
                        CorrectionCluster preparedCluster = null;
                        while (preparedCluster == null) {
                            ParsedRead parsedRead = ((inputReadsLimit == 0) || (totalReads.get() < inputReadsLimit))
                                    ? pass1Reader.take() : null;
                            if (parsedRead != null) {
                                LinkedHashMap<String, NucleotideSequence> currentGroups = extractKeyGroups(parsedRead);
                                if (!currentGroups.equals(previousGroups)) {
                                    if (previousGroups != null) {
                                        preparedCluster = currentCluster;
                                        currentCluster = new CorrectionCluster(++orderedPortIndex);
                                    }
                                    previousGroups = currentGroups;
                                }
                                currentCluster.groupValues.add(extractGroupValues(parsedRead));
                                totalReads.getAndIncrement();
                            } else {
                                finished = true;
                                if (previousGroups != null)
                                    return currentCluster;
                                else
                                    return null;
                            }
                        }
                        return preparedCluster;
                    }
                };
            }

            OutputPort<CorrectionQualityPreprocessingResult> correctionPreprocessorPort = new ParallelProcessor<>(
                    clusterOutputPort, new CorrectionQualityPreprocessor(), threads);



            if (!suppressWarnings && (pass1Reader.getQuicklySortedGroups().size() > 0) && (primaryGroups.size() == 0))
                System.err.println("WARNING: correcting sorted MIF file; output file will be unsorted!");
            LinkedHashSet<String> unsortedPrimaryGroups = new LinkedHashSet<>(primaryGroups);
            unsortedPrimaryGroups.removeAll(pass1Reader.getQuicklySortedGroups());
            if (!suppressWarnings && (unsortedPrimaryGroups.size() > 0))
                System.err.println("WARNING: correcting MIF file with unsorted primary groups " +
                        unsortedPrimaryGroups + "; correction will be slower and more memory consuming!");
            List<String> correctedAgainGroups = keyGroups.stream().filter(gn -> pass1Reader.getCorrectedGroups()
                    .stream().anyMatch(gn::equals)).collect(Collectors.toList());
            if (!suppressWarnings && (correctedAgainGroups.size() != 0))
                System.err.println("WARNING: group(s) " + correctedAgainGroups + " already corrected and will be " +
                        "corrected again!");
            CorrectionAlgorithms correctionAlgorithms = new CorrectionAlgorithms(inputReadsLimit,
                    barcodeClusteringStrategyFactory, maxUniqueBarcodes, minCount, wildcardsCollapsingMethod);
            if (primaryGroups.size() == 0)
                stats = correctionAlgorithms.fullFileCorrect(pass1Reader, pass2Reader, writer, excludedBarcodesWriter,
                        keyGroups);
            else if (unsortedPrimaryGroups.size() == 0)
                stats = correctionAlgorithms.sortedClustersCorrect(pass1Reader, writer, excludedBarcodesWriter,
                        primaryGroups, keyGroups);
            else
                stats = correctionAlgorithms.unsortedClustersCorrect(pass1Reader, writer, excludedBarcodesWriter,
                        primaryGroups, keyGroups);
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

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        float percent = (stats.totalReads == 0) ? 0 : (float)stats.correctedReads / stats.totalReads * 100;
        report.append("Processed ").append(stats.totalReads).append(" reads").append('\n');
        report.append("Reads with corrected barcodes: ").append(stats.correctedReads).append(" (")
                .append(floatFormat.format(percent)).append("%)\n");
        if (stats.excludedReads > 0)
            report.append("Reads excluded by too low barcode count: ").append(stats.excludedReads).append(" (")
                    .append(floatFormat.format((float)stats.excludedReads / stats.totalReads * 100)).append("%)\n");

        jsonReportData.put("version", getShortestVersionString());
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("excludedBarcodesOutputFileName", excludedBarcodesOutputFileName);
        jsonReportData.put("keyGroups", keyGroups);
        jsonReportData.put("primaryGroups", primaryGroups);
        jsonReportData.put("maxUniqueBarcodes", maxUniqueBarcodes);
        jsonReportData.put("minCount", minCount);
        jsonReportData.put("wildcardsCollapsingMethod", wildcardsCollapsingMethod);
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("correctedReads", stats.correctedReads);
        jsonReportData.put("excludedReads", stats.excludedReads);
        jsonReportData.put("totalReads", stats.totalReads);

        humanReadableReport(reportFileName, reportFileHeader.toString(), report.toString());
        jsonReport(jsonReportFileName, jsonReportData);
    }

    private MifWriter createWriter(MifHeader inputHeader, boolean excludedBarcodes) throws IOException {
        LinkedHashSet<String> allCorrectedGroups = new LinkedHashSet<>(inputHeader.getCorrectedGroups());
        allCorrectedGroups.addAll(keyGroups);
        MifHeader outputHeader = new MifHeader(pipelineConfiguration, inputHeader.getNumberOfTargets(),
                new ArrayList<>(allCorrectedGroups), new ArrayList<>(), new ArrayList<>(),
                inputHeader.getGroupEdges());
        if (excludedBarcodes)
            return (excludedBarcodesOutputFileName == null) ? null
                    : new MifWriter(excludedBarcodesOutputFileName, outputHeader);
        else
            return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                    : new MifWriter(outputFileName, outputHeader);
    }

    private LinkedHashMap<String, NucleotideSequence> extractKeyGroups(ParsedRead parsedRead) {
        LinkedHashMap<String, NucleotideSequence> extractedKeyGroups = new LinkedHashMap<>();
        for (String keyGroup : keyGroups)
            extractedKeyGroups.put(keyGroup, parsedRead.getGroupValue(keyGroup).getSequence());
        return extractedKeyGroups;
    }

    private Map<String, NSequenceWithQuality> extractGroupValues(ParsedRead parsedRead) {
        Map<String, NSequenceWithQuality> groupValues = new HashMap<>();
        for (String keyGroup : keyGroups)
            groupValues.put(keyGroup, parsedRead.getGroupValue(keyGroup));
        return groupValues;
    }
}
