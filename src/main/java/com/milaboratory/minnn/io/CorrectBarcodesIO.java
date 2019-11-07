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
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.minnn.correct.*;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.util.SmartProgressReporter;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.getShortestVersionString;
import static com.milaboratory.minnn.util.SequencesCache.*;
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
    private final boolean disableBarcodesQuality;
    private final boolean disableWildcardsCollapsing;
    private final float wildcardsCollapsingMergeThreshold;
    private final long inputReadsLimit;
    private final boolean suppressWarnings;
    private final int threads;
    private final String reportFileName;
    private final String jsonReportFileName;
    private final AtomicLong totalReads = new AtomicLong(0);
    private final AtomicLong totalWildcards = new AtomicLong(0);
    private final AtomicLong totalNucleotides = new AtomicLong(0);

    public CorrectBarcodesIO(
            PipelineConfiguration pipelineConfiguration, String inputFileName, String outputFileName,
            List<String> groupNames, List<String> primaryGroupNames,
            BarcodeClusteringStrategyFactory barcodeClusteringStrategyFactory, int maxUniqueBarcodes, int minCount,
            String excludedBarcodesOutputFileName, boolean disableBarcodesQuality, boolean disableWildcardsCollapsing,
            float wildcardsCollapsingMergeThreshold, long inputReadsLimit, boolean suppressWarnings, int threads,
            String reportFileName, String jsonReportFileName) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.correctionAlgorithms = new CorrectionAlgorithms(barcodeClusteringStrategyFactory,
                maxUniqueBarcodes, minCount, disableBarcodesQuality, disableWildcardsCollapsing,
                wildcardsCollapsingMergeThreshold);
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.keyGroups = new LinkedHashSet<>(groupNames);
        this.primaryGroups = (primaryGroupNames == null) ? new LinkedHashSet<>()
                : new LinkedHashSet<>(primaryGroupNames);
        this.maxUniqueBarcodes = maxUniqueBarcodes;
        this.minCount = minCount;
        this.excludedBarcodesOutputFileName = excludedBarcodesOutputFileName;
        this.disableBarcodesQuality = disableBarcodesQuality;
        this.disableWildcardsCollapsing = disableWildcardsCollapsing;
        this.wildcardsCollapsingMergeThreshold = wildcardsCollapsingMergeThreshold;
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
            List<String> correctedAgainGroups = keyGroups.stream().filter(gn -> pass1Reader.getCorrectedGroups()
                    .stream().anyMatch(gn::equals)).collect(Collectors.toList());
            if (!suppressWarnings && (correctedAgainGroups.size() != 0))
                System.err.println("WARNING: group(s) " + correctedAgainGroups + " already corrected and will be " +
                        "corrected again!");

            if (primaryGroups.size() > 0) {
                // secondary barcodes correction
                validateInputGroups(pass1Reader, primaryGroups, false,
                        "--primary-groups");
                LinkedHashSet<String> unsortedPrimaryGroups = new LinkedHashSet<>(primaryGroups);
                unsortedPrimaryGroups.removeAll(pass1Reader.getQuicklySortedGroups());
                if (!suppressWarnings && (unsortedPrimaryGroups.size() > 0))
                    System.err.println("WARNING: correcting MIF file with unsorted primary groups " +
                            unsortedPrimaryGroups + "; correction will be slower and more memory consuming!");
                SmartProgressReporter.startProgressReport((unsortedPrimaryGroups.size() > 0)
                        ? "Reading input file into memory and counting barcodes" : "Counting barcodes",
                        pass1Reader, System.err);
                stats = performSecondaryBarcodesCorrection(
                        getParsedReadOutputPort(pass1Reader, true), writer, excludedBarcodesWriter,
                        unsortedPrimaryGroups.size() > 0);
            } else {
                // full file correction
                LinkedHashSet<String> unsortedKeyGroups = new LinkedHashSet<>(keyGroups);
                unsortedKeyGroups.removeAll(pass1Reader.getFullySortedGroups());
                if (unsortedKeyGroups.size() > 0)
                    System.err.println("WARNING: group(s) " + unsortedKeyGroups + " not sorted, but specified for " +
                            "correction; correction will consume much more memory!");
                SmartProgressReporter.startProgressReport("Counting barcodes and filling correction maps",
                        pass1Reader, System.err);
                OutputPort<CorrectionQualityPreprocessingResult> preprocessorPort = getPreprocessingResultOutputPort(
                        getParsedReadOutputPort(pass1Reader, true),
                        unsortedKeyGroups.size() > 0, threads);
                CorrectionData correctionData = correctionAlgorithms.prepareCorrectionData(preprocessorPort,
                        keyGroups, 0);
                SmartProgressReporter.startProgressReport("Correcting barcodes", pass2Reader, System.err);
                stats = correctionAlgorithms.correctAndWrite(correctionData,
                        getParsedReadOutputPort(pass2Reader, false),
                        writer, excludedBarcodesWriter);
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

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        float percent = (totalReads.get() == 0) ? 0 : (float)stats.correctedReads / totalReads.get() * 100;
        report.append("Processed ").append(totalReads).append(" reads").append('\n');
        report.append("Reads with corrected barcodes: ").append(stats.correctedReads).append(" (")
                .append(floatFormat.format(percent)).append("%)\n");
        if (stats.excludedReads > 0)
            report.append("Reads excluded by too low barcode count: ").append(stats.excludedReads).append(" (")
                    .append(floatFormat.format((float)stats.excludedReads / totalReads.get() * 100)).append("%)\n");
        if (totalNucleotides.get() > 0)
            report.append("Wildcards in barcodes: ").append(totalWildcards).append(" (")
                    .append(floatFormat.format((float)totalWildcards.get() / totalNucleotides.get() * 100))
                    .append("% of all letters in barcodes)\n");

        jsonReportData.put("version", getShortestVersionString());
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("excludedBarcodesOutputFileName", excludedBarcodesOutputFileName);
        jsonReportData.put("keyGroups", keyGroups);
        jsonReportData.put("primaryGroups", primaryGroups);
        jsonReportData.put("maxUniqueBarcodes", maxUniqueBarcodes);
        jsonReportData.put("minCount", minCount);
        jsonReportData.put("disableBarcodesQuality", disableBarcodesQuality);
        jsonReportData.put("disableWildcardsCollapsing", disableWildcardsCollapsing);
        jsonReportData.put("wildcardsCollapsingMergeThreshold", wildcardsCollapsingMergeThreshold);
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("correctedReads", stats.correctedReads);
        jsonReportData.put("excludedReads", stats.excludedReads);
        jsonReportData.put("totalReads", totalReads);
        jsonReportData.put("totalWildcards", totalWildcards);
        jsonReportData.put("totalNucleotides", totalNucleotides);

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

    /**
     * Prepare output port for quality preprocessor.
     *
     * @param inputPort     port with parsed reads for input file (in case of full file correction)
     *                      or current cluster (in case of secondary barcodes correction)
     * @param unsortedInput true if input is unsorted, so clusters cannot be created in lazy take() function
     * @param threads       number of threads specified in command line or 1 if multi-threading is already
     *                      used in outer port
     * @return              output port with quality preprocessor results
     */
    private OutputPort<CorrectionQualityPreprocessingResult> getPreprocessingResultOutputPort(
            OutputPort<ParsedRead> inputPort, boolean unsortedInput, int threads) {
        OutputPort<CorrectionQualityPreprocessingResult> correctionPreprocessorPort;
        if (!disableBarcodesQuality && unsortedInput) {
            // read barcodes from the entire input into memory to create clusters
            // keys: group names and values; values: created clusters
            Map<LinkedHashMap<String, NucleotideSequence>, CorrectionCluster> allClusters = new HashMap<>();
            AtomicLong orderedPortIndex = new AtomicLong(0);
            for (ParsedRead parsedRead : CUtils.it(inputPort)) {
                LinkedHashMap<String, NucleotideSequence> groups = extractKeyGroups(parsedRead);
                allClusters.computeIfAbsent(groups,
                        g -> new CorrectionCluster(orderedPortIndex.getAndIncrement()));
                allClusters.get(groups).groupValues.add(extractGroupValues(parsedRead));
            }
            OutputPort<CorrectionCluster> clusterOutputPort = new OutputPort<CorrectionCluster>() {
                Iterator<CorrectionCluster> clusters = allClusters.values().iterator();

                @Override
                public synchronized CorrectionCluster take() {
                    if (!clusters.hasNext())
                        return null;
                    return clusters.next();
                }
            };
            OutputPort<CorrectionQualityPreprocessingResult> unorderedPort = new ParallelProcessor<>(clusterOutputPort,
                    new CorrectionQualityPreprocessor(), threads);
            correctionPreprocessorPort = new OrderedOutputPort<>(unorderedPort, result -> result.orderedPortIndex);
        } else if (!disableBarcodesQuality) {
            // all groups are sorted; we can add input reads to the cluster while their group values are the same
            OutputPort<CorrectionCluster> clusterOutputPort = new OutputPort<CorrectionCluster>() {
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
                        ParsedRead parsedRead = inputPort.take();
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
            OutputPort<CorrectionQualityPreprocessingResult> unorderedPort = new ParallelProcessor<>(clusterOutputPort,
                    new CorrectionQualityPreprocessor(), threads);
            correctionPreprocessorPort = new OrderedOutputPort<>(unorderedPort, result -> result.orderedPortIndex);
        } else if (unsortedInput) {
            // count all barcodes without calculating quality
            TObjectIntHashMap<LinkedHashMap<String, NSequenceWithQuality>> allCounters = new TObjectIntHashMap<>();
            for (ParsedRead parsedRead : CUtils.it(inputPort)) {
                LinkedHashMap<String, NSequenceWithQuality> groups = extractKeyGroupsWithMaxQuality(parsedRead);
                allCounters.adjustOrPutValue(groups, 1, 1);
            }
            correctionPreprocessorPort = new OutputPort<CorrectionQualityPreprocessingResult>() {
                TObjectIntIterator<LinkedHashMap<String, NSequenceWithQuality>> counter = allCounters.iterator();

                @Override
                public CorrectionQualityPreprocessingResult take() {
                    return new CorrectionQualityPreprocessingResult(counter.key(), counter.value(), 0);
                }
            };
        } else {
            // all groups are sorted; convert clusters to preprocessing results with maximal quality
            correctionPreprocessorPort = new OutputPort<CorrectionQualityPreprocessingResult>() {
                LinkedHashMap<String, NSequenceWithQuality> previousGroups = null;
                int currentCounter = 0;
                boolean finished = false;

                @Override
                public CorrectionQualityPreprocessingResult take() {
                    if (finished)
                        return null;
                    CorrectionQualityPreprocessingResult preparedResult = null;
                    while (preparedResult == null) {
                        ParsedRead parsedRead = inputPort.take();
                        if (parsedRead != null) {
                            LinkedHashMap<String, NSequenceWithQuality> currentGroups
                                    = extractKeyGroupsWithMaxQuality(parsedRead);
                            if (!currentGroups.equals(previousGroups)) {
                                if (previousGroups != null) {
                                    preparedResult = new CorrectionQualityPreprocessingResult(
                                            previousGroups, currentCounter, 0);
                                    currentCounter = 0;
                                }
                                previousGroups = currentGroups;
                            }
                            currentCounter++;
                        } else {
                            finished = true;
                            if (previousGroups != null)
                                return new CorrectionQualityPreprocessingResult(
                                        previousGroups, currentCounter, 0);
                            else
                                return null;
                        }
                    }
                    return preparedResult;
                }
            };
        }

        return correctionPreprocessorPort;
    }

    /**
     * Get port with parsed reads: MIF reader with inputReadsLimit checking and wildcard stats calculation.
     *
     * @param mifReader                 MIF reader
     * @param countStatsForWildcards    true if stats for wildcards count and total nucleotides count must be
     *                                  calculated on this stage (used only for 1st pass MIF reader)
     * @return                          output port with parsed reads
     */
    private OutputPort<ParsedRead> getParsedReadOutputPort(MifReader mifReader, boolean countStatsForWildcards) {
        return () -> {
            ParsedRead parsedRead = ((inputReadsLimit == 0) || (totalReads.get() < inputReadsLimit))
                    ? mifReader.take() : null;
            if (parsedRead != null) {
                if (countStatsForWildcards)
                    parsedRead.getGroups().stream().filter(g -> keyGroups.contains(g.getGroupName())).forEach(g -> {
                        NucleotideSequence seq = g.getValue().getSequence();
                        totalNucleotides.getAndAdd(seq.size());
                        int wildcardsCount = 0;
                        for (int i = 0; i < seq.size(); i++)
                            if (charToWildcard.get(seq.symbolAt(i)).basicSize() > 1)
                                wildcardsCount++;
                        totalWildcards.getAndAdd(wildcardsCount);
                    });
                totalReads.getAndIncrement();
            }
            return parsedRead;
        };
    }

    private OutputPort<ParsedRead> getParsedReadOutputPort(List<ParsedRead> currentCluster) {
        return new OutputPort<ParsedRead>() {
            int currentIndex = 0;

            @Override
            public ParsedRead take() {
                return (currentIndex == currentCluster.size()) ? null : currentCluster.get(currentIndex++);
            }
        };
    }

    private LinkedHashMap<String, NucleotideSequence> extractKeyGroups(ParsedRead parsedRead) {
        LinkedHashMap<String, NucleotideSequence> extractedKeyGroups = new LinkedHashMap<>();
        for (String keyGroup : keyGroups)
            extractedKeyGroups.put(keyGroup, parsedRead.getGroupValue(keyGroup).getSequence());
        return extractedKeyGroups;
    }

    private LinkedHashMap<String, NSequenceWithQuality> extractKeyGroupsWithMaxQuality(ParsedRead parsedRead) {
        LinkedHashMap<String, NSequenceWithQuality> extractedKeyGroups = new LinkedHashMap<>();
        for (String keyGroup : keyGroups)
            extractedKeyGroups.put(keyGroup, new NSequenceWithQuality(parsedRead.getGroupValue(keyGroup).getSequence(),
                    DEFAULT_MAX_QUALITY));
        return extractedKeyGroups;
    }

    private LinkedHashMap<String, NucleotideSequence> extractPrimaryGroups(ParsedRead parsedRead) {
        LinkedHashMap<String, NucleotideSequence> extractedPrimaryGroups = new LinkedHashMap<>();
        for (String primaryGroup : primaryGroups)
            extractedPrimaryGroups.put(primaryGroup, parsedRead.getGroupValue(primaryGroup).getSequence());
        return extractedPrimaryGroups;
    }

    private Map<String, NSequenceWithQuality> extractGroupValues(ParsedRead parsedRead) {
        Map<String, NSequenceWithQuality> groupValues = new HashMap<>();
        for (String keyGroup : keyGroups)
            groupValues.put(keyGroup, parsedRead.getGroupValue(keyGroup));
        return groupValues;
    }

    private CorrectionStats performSecondaryBarcodesCorrection(
            OutputPort<ParsedRead> inputPort, MifWriter writer, MifWriter excludedBarcodesWriter,
            boolean unsortedInput) {
        OutputPort<PrimaryBarcodeCluster> clusterOutputPort;
        AtomicLong orderedPortIndex = new AtomicLong(0);

        if (unsortedInput) {
            // keys: primary barcodes values; values: all reads that have this combination of barcodes values
            HashMap<LinkedHashMap<String, NucleotideSequence>, PrimaryBarcodeCluster> allClusters = new HashMap<>();

            for (ParsedRead parsedRead : CUtils.it(inputPort)) {
                LinkedHashMap<String, NucleotideSequence> primaryGroups = extractPrimaryGroups(parsedRead);
                allClusters.putIfAbsent(primaryGroups, new PrimaryBarcodeCluster(
                        new ArrayList<>(), orderedPortIndex.getAndIncrement()));
                allClusters.get(primaryGroups).parsedReads.add(parsedRead);
            }

            clusterOutputPort = new OutputPort<PrimaryBarcodeCluster>() {
                Iterator<PrimaryBarcodeCluster> clusters = allClusters.values().iterator();
                boolean correctionStarted = false;

                @Override
                public synchronized PrimaryBarcodeCluster take() {
                    if (!correctionStarted) {
                        SmartProgressReporter.startProgressReport("Correcting barcodes", writer, System.err);
                        correctionStarted = true;
                    }
                    if (!clusters.hasNext())
                        return null;
                    return clusters.next();
                }
            };
        } else {
            clusterOutputPort = new OutputPort<PrimaryBarcodeCluster>() {
                LinkedHashMap<String, NucleotideSequence> previousGroups = null;
                PrimaryBarcodeCluster currentCluster = new PrimaryBarcodeCluster(new ArrayList<>(),
                        orderedPortIndex.getAndIncrement());
                boolean correctionStarted = false;
                boolean finished = false;

                @Override
                public synchronized PrimaryBarcodeCluster take() {
                    if (!correctionStarted) {
                        SmartProgressReporter.startProgressReport("Correcting barcodes", writer, System.err);
                        correctionStarted = true;
                    }
                    if (finished)
                        return null;
                    PrimaryBarcodeCluster preparedCluster = null;
                    while (preparedCluster == null) {
                        ParsedRead parsedRead = inputPort.take();
                        if (parsedRead != null) {
                            LinkedHashMap<String, NucleotideSequence> currentGroups = extractPrimaryGroups(parsedRead);
                            if (!currentGroups.equals(previousGroups)) {
                                if (previousGroups != null) {
                                    preparedCluster = currentCluster;
                                    currentCluster = new PrimaryBarcodeCluster(new ArrayList<>(),
                                            orderedPortIndex.getAndIncrement());
                                }
                                previousGroups = currentGroups;
                            }
                            currentCluster.parsedReads.add(parsedRead);
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

        long correctedReads = 0;
        long excludedReads = 0;
        OutputPort<ProcessedPrimaryBarcodeCluster> correctionDataUnorderedPort = new ParallelProcessor<>(
                clusterOutputPort, new PrimaryBarcodeClustersProcessor(), threads);
        for (ProcessedPrimaryBarcodeCluster processedCluster : CUtils.it(new OrderedOutputPort<>(
                correctionDataUnorderedPort, cluster -> cluster.correctionData.orderedPortIndex))) {
            CorrectionStats stats = correctionAlgorithms.correctAndWrite(processedCluster.correctionData,
                    getParsedReadOutputPort(processedCluster.parsedReads), writer, excludedBarcodesWriter);
            correctedReads += stats.correctedReads;
            excludedReads += stats.excludedReads;
        }
        return new CorrectionStats(correctedReads, excludedReads);
    }

    private class PrimaryBarcodeClustersProcessor
            implements Processor<PrimaryBarcodeCluster, ProcessedPrimaryBarcodeCluster> {
        @Override
        public ProcessedPrimaryBarcodeCluster process(PrimaryBarcodeCluster primaryBarcodeCluster) {
            OutputPort<CorrectionQualityPreprocessingResult> preprocessorPort = getPreprocessingResultOutputPort(
                    getParsedReadOutputPort(primaryBarcodeCluster.parsedReads), true, 1);
            return new ProcessedPrimaryBarcodeCluster(primaryBarcodeCluster.parsedReads,
                    correctionAlgorithms.prepareCorrectionData(preprocessorPort, keyGroups,
                            primaryBarcodeCluster.orderedPortIndex));
        }
    }

    private static class ProcessedPrimaryBarcodeCluster {
        final List<ParsedRead> parsedReads;
        final CorrectionData correctionData;

        ProcessedPrimaryBarcodeCluster(List<ParsedRead> parsedReads, CorrectionData correctionData) {
            this.parsedReads = parsedReads;
            this.correctionData = correctionData;
        }
    }
}
