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
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.core.sequence.*;
import com.milaboratory.minnn.consensus.*;
import com.milaboratory.minnn.consensus.doublemultialign.ConsensusAlgorithmDoubleMultiAlign;
import com.milaboratory.minnn.consensus.singlecell.ConsensusAlgorithmSingleCell;
import com.milaboratory.minnn.outputconverter.MatchedGroup;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.util.SmartProgressReporter;
import com.milaboratory.util.TempFileManager;
import gnu.trove.map.hash.TLongLongHashMap;
import org.clapper.util.misc.FileHashMap;
import org.clapper.util.misc.ObjectExistsException;
import org.clapper.util.misc.VersionMismatchException;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.consensus.ConsensusAlgorithms.*;
import static com.milaboratory.minnn.consensus.OriginalReadStatus.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.FormatUtils.nanoTimeToString;

public final class ConsensusIO {
    private final PipelineConfiguration pipelineConfiguration;
    private final String inputFileName;
    private final String outputFileName;
    private final ConsensusAlgorithms consensusAlgorithmType;
    private final int alignerWidth;
    private final int matchScore;
    private final int mismatchScore;
    private final int gapScore;
    private final long goodQualityMismatchPenalty;
    private final byte goodQualityMismatchThreshold;
    private final long scoreThreshold;
    private final float skippedFractionToRepeat;
    private final int maxConsensusesPerCluster;
    private final boolean dropOversizedClusters;
    private final int readsMinGoodSeqLength;
    private final float readsAvgQualityThreshold;
    private final int readsTrimWindowSize;
    private final int minGoodSeqLength;
    private final float lowCoverageThreshold;
    private final float avgQualityThreshold;
    private final float avgQualityThresholdForLowCoverage;
    private final int trimWindowSize;
    private final String originalReadStatsFileName;
    private final String notUsedReadsOutputFileName;
    private final boolean saveNotUsedReads;
    private final boolean toSeparateGroups;
    private final long inputReadsLimit;
    private final int maxWarnings;
    private final int threads;
    private final int kmerLength;
    private final int kmerMaxOffset;
    private final int kmerMatchMaxErrors;
    private final PrintStream reportFileOutputStream;
    private final String jsonReportFileName;
    private final PrintStream debugOutputStream;
    private final byte debugQualityThreshold;
    private final AtomicLong totalReads = new AtomicLong(0);
    private final FileHashMap<Long, OriginalReadData> originalReadsData;
    private final TLongLongHashMap consensusFinalIds;
    private ConsensusAlgorithm consensusAlgorithm;
    private long consensusReads = 0;
    private long clustersCount = 0;
    private long notUsedReadsCount = 0;
    private int warningsDisplayed = 0;
    private final LinkedHashSet<String> consensusGroups;
    private final TreeMap<Integer, Long> consensusesByReadsCounts = new TreeMap<>(Collections.reverseOrder());
    private int numberOfTargets;

    public ConsensusIO(
            PipelineConfiguration pipelineConfiguration, List<String> groupList, String inputFileName,
            String outputFileName, ConsensusAlgorithms consensusAlgorithmType, int alignerWidth,
            int matchScore, int mismatchScore, int gapScore, long goodQualityMismatchPenalty,
            byte goodQualityMismatchThreshold, long scoreThreshold, float skippedFractionToRepeat,
            int maxConsensusesPerCluster, boolean dropOversizedClusters, int readsMinGoodSeqLength,
            float readsAvgQualityThreshold, int readsTrimWindowSize, int minGoodSeqLength,
            float lowCoverageThreshold, float avgQualityThreshold, float avgQualityThresholdForLowCoverage,
            int trimWindowSize, String originalReadStatsFileName, String notUsedReadsOutputFileName,
            boolean toSeparateGroups, long inputReadsLimit, int maxWarnings, int threads,
            int kmerLength, int kmerMaxOffset, int kmerMatchMaxErrors, String reportFileName,
            String jsonReportFileName, String debugOutputFileName, byte debugQualityThreshold) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.consensusGroups = new LinkedHashSet<>(Objects.requireNonNull(groupList));
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.consensusAlgorithmType = consensusAlgorithmType;
        this.alignerWidth = alignerWidth;
        this.matchScore = matchScore;
        this.mismatchScore = mismatchScore;
        this.gapScore = gapScore;
        this.goodQualityMismatchPenalty = goodQualityMismatchPenalty;
        this.goodQualityMismatchThreshold = goodQualityMismatchThreshold;
        this.scoreThreshold = scoreThreshold;
        this.skippedFractionToRepeat = skippedFractionToRepeat;
        this.maxConsensusesPerCluster = maxConsensusesPerCluster;
        this.dropOversizedClusters = dropOversizedClusters;
        this.readsMinGoodSeqLength = readsMinGoodSeqLength;
        this.readsAvgQualityThreshold = readsAvgQualityThreshold;
        this.readsTrimWindowSize = readsTrimWindowSize;
        this.toSeparateGroups = toSeparateGroups;
        this.minGoodSeqLength = minGoodSeqLength;
        this.lowCoverageThreshold = lowCoverageThreshold;
        this.avgQualityThreshold = avgQualityThreshold;
        this.avgQualityThresholdForLowCoverage = avgQualityThresholdForLowCoverage;
        this.trimWindowSize = trimWindowSize;
        this.originalReadStatsFileName = originalReadStatsFileName;
        this.notUsedReadsOutputFileName = notUsedReadsOutputFileName;
        this.saveNotUsedReads = (notUsedReadsOutputFileName != null);
        this.inputReadsLimit = inputReadsLimit;
        this.maxWarnings = maxWarnings;
        this.threads = threads;
        this.kmerLength = kmerLength;
        this.kmerMaxOffset = kmerMaxOffset;
        this.kmerMatchMaxErrors = kmerMatchMaxErrors;
        if (reportFileName == null)
            this.reportFileOutputStream = null;
        else
            try {
                this.reportFileOutputStream = new PrintStream(new FileOutputStream(reportFileName, true));
            } catch (IOException e) {
                throw exitWithError(e.toString());
            }
        this.jsonReportFileName = jsonReportFileName;
        try {
            debugOutputStream = (debugOutputFileName == null) ? null
                    : new PrintStream(new FileOutputStream(debugOutputFileName));
        } catch (IOException e) {
            throw exitWithError(e.toString());
        }
        this.debugQualityThreshold = debugQualityThreshold;
        if (originalReadStatsFileName != null) {
            try {
                File tempFile = TempFileManager.getTempFile((outputFileName == null) ? null
                        : Paths.get(new File(outputFileName).getAbsolutePath()).getParent());
                this.originalReadsData = new FileHashMap<>(tempFile.getAbsolutePath(), FileHashMap.TRANSIENT);
            } catch (IOException | ObjectExistsException | ClassNotFoundException | VersionMismatchException e) {
                throw exitWithError(e.getMessage());
            }
        } else
            this.originalReadsData = null;
        this.consensusFinalIds = (originalReadStatsFileName == null) ? null : new TLongLongHashMap();
    }

    private void consensusAlgorithmInit() {
        switch(consensusAlgorithmType) {
            case DOUBLE_MULTI_ALIGN:
                consensusAlgorithm = new ConsensusAlgorithmDoubleMultiAlign(this::displayWarning, numberOfTargets,
                        alignerWidth, matchScore, mismatchScore, gapScore, goodQualityMismatchPenalty,
                        goodQualityMismatchThreshold, scoreThreshold, skippedFractionToRepeat,
                        maxConsensusesPerCluster, dropOversizedClusters, readsMinGoodSeqLength,
                        readsAvgQualityThreshold, readsTrimWindowSize, minGoodSeqLength, lowCoverageThreshold,
                        avgQualityThreshold, avgQualityThresholdForLowCoverage, trimWindowSize, toSeparateGroups,
                        debugOutputStream, debugQualityThreshold, originalReadsData, saveNotUsedReads);
                break;
            case RNA_SEQ:
                consensusAlgorithm = new ConsensusAlgorithmRNASeq();
                break;
            case SINGLE_CELL:
                consensusAlgorithm = new ConsensusAlgorithmSingleCell(this::displayWarning, numberOfTargets,
                        maxConsensusesPerCluster, dropOversizedClusters, skippedFractionToRepeat, readsMinGoodSeqLength,
                        readsAvgQualityThreshold, readsTrimWindowSize, minGoodSeqLength, lowCoverageThreshold,
                        avgQualityThreshold, avgQualityThresholdForLowCoverage, trimWindowSize, toSeparateGroups,
                        debugOutputStream, debugQualityThreshold, originalReadsData, saveNotUsedReads,
                        kmerLength, kmerMaxOffset, kmerMatchMaxErrors);
                break;
        }
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        MifHeader mifHeader;
        long originalNumberOfReads;
        if (reportFileOutputStream != null) {
            reportFileOutputStream.println("MiNNN v" + getVersionString(VERSION_INFO_SHORTEST));
            reportFileOutputStream.println("Report for Consensus command:");
            if (inputFileName == null)
                reportFileOutputStream.println("Input is from stdin");
            else
                reportFileOutputStream.println("Input file name: " + inputFileName);
            if (outputFileName == null)
                reportFileOutputStream.println("Output is to stdout");
            else
                reportFileOutputStream.println("Output file name: " + outputFileName);
            reportFileOutputStream.println("Consensus assembled by groups: " + consensusGroups);
            reportFileOutputStream.println("Consensus algorithm: " + consensusAlgorithmType);
        }
        try (MifReader reader = createReader();
             MifWriter writer = createWriter(mifHeader = reader.getHeader());
             MifWriter notUsedReadsWriter = saveNotUsedReads ? new MifWriter(notUsedReadsOutputFileName, new MifHeader(
                     pipelineConfiguration, numberOfTargets, mifHeader.getCorrectedGroups(),
                     mifHeader.getSortedGroups(), mifHeader.getGroupEdges())) : null)
        {
            if (inputReadsLimit > 0)
                reader.setParsedReadsLimit(inputReadsLimit);
            validateInputGroups(reader, consensusGroups, false, "--groups");
            LinkedHashSet<String> notCorrectedGroups = new LinkedHashSet<>(consensusGroups);
            notCorrectedGroups.removeAll(reader.getCorrectedGroups());
            LinkedHashSet<String> notSortedGroups = new LinkedHashSet<>(consensusGroups);
            notSortedGroups.removeAll(reader.getSortedGroups());
            if (notCorrectedGroups.size() > 0)
                displayWarning("WARNING: group(s) " + notCorrectedGroups + " not corrected, but used in " +
                        "consensus calculation!");
            OutputPort<Cluster> clusterOutputPort;
            if (notSortedGroups.size() > 0) {
                // not all groups are sorted; we must read the entire file into memory to create clusters
                displayWarning("WARNING: group(s) " + notSortedGroups + " not sorted, but used in " +
                        "consensus calculation; consensus calculation will consume much more memory!");
                SmartProgressReporter.startProgressReport("Reading", reader, System.err);
                // keys: group names and values; values: created clusters
                HashMap<LinkedHashMap<String, NucleotideSequence>, Cluster> allClusters = new HashMap<>();
                AtomicLong orderedPortIndex = new AtomicLong(0);
                for (ParsedRead parsedRead : CUtils.it(reader)) {
                    LinkedHashMap<String, NucleotideSequence> groups = extractConsensusGroups(parsedRead);
                    allClusters.computeIfAbsent(groups, g -> new Cluster(
                            orderedPortIndex.getAndIncrement(), saveNotUsedReads));
                    Cluster currentCluster = allClusters.get(groups);
                    currentCluster.data.add(extractData(parsedRead));
                    if (saveNotUsedReads)
                        currentCluster.savedReads.add(parsedRead);
                    saveOriginalReadsData(parsedRead);
                    if (totalReads.incrementAndGet() == inputReadsLimit)
                        break;
                }
                clusterOutputPort = new OutputPort<Cluster>() {
                    final Iterator<Cluster> clusters = allClusters.values().iterator();

                    @Override
                    public synchronized Cluster take() {
                        if (!clusters.hasNext())
                            return null;
                        return clusters.next();
                    }
                };
            } else {
                SmartProgressReporter.startProgressReport("Calculating consensuses", reader, System.err);
                // all groups are sorted; we can add input reads to the cluster while their group values are the same
                clusterOutputPort = new OutputPort<Cluster>() {
                    LinkedHashMap<String, NucleotideSequence> previousGroups = null;
                    Cluster currentCluster = new Cluster(0, saveNotUsedReads);
                    int orderedPortIndex = 0;
                    boolean finished = false;

                    @Override
                    public synchronized Cluster take() {
                        if (finished)
                            return null;
                        Cluster preparedCluster = null;
                        while (preparedCluster == null) {
                            ParsedRead parsedRead = ((inputReadsLimit == 0) || (totalReads.get() < inputReadsLimit))
                                    ? reader.take() : null;
                            if (parsedRead != null) {
                                Set<String> allGroups = parsedRead.getNotDefaultGroups().stream()
                                        .map(MatchedGroup::getGroupName).collect(Collectors.toSet());
                                for (String groupName : consensusGroups)
                                    if (!allGroups.contains(groupName))
                                        throw exitWithError("Group " + groupName + " not found in the input!");
                                LinkedHashMap<String, NucleotideSequence> currentGroups =
                                        extractConsensusGroups(parsedRead);
                                if (!currentGroups.equals(previousGroups)) {
                                    if (previousGroups != null) {
                                        preparedCluster = currentCluster;
                                        currentCluster = new Cluster(++orderedPortIndex, saveNotUsedReads);
                                    }
                                    previousGroups = currentGroups;
                                }
                                currentCluster.data.add(extractData(parsedRead));
                                if (saveNotUsedReads)
                                    currentCluster.savedReads.add(parsedRead);
                                saveOriginalReadsData(parsedRead);
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
            consensusAlgorithmInit();

            OutputPort<CalculatedConsensuses> calculatedConsensusesPort = new ParallelProcessor<>(clusterOutputPort,
                    consensusAlgorithm, threads);
            OrderedOutputPort<CalculatedConsensuses> orderedConsensusesPort = new OrderedOutputPort<>(
                    calculatedConsensusesPort, cc -> cc.orderedPortIndex);
            long subclusterDebugIndex = -1;
            for (CalculatedConsensuses calculatedConsensuses : CUtils.it(orderedConsensusesPort)) {
                clustersCount++;
                for (int i = 0; i < calculatedConsensuses.consensuses.size(); i++) {
                    Consensus consensus = calculatedConsensuses.consensuses.get(i);
                    if (consensus.isConsensus && consensus.finalConsensus) {
                        if (consensusFinalIds != null)
                            consensusFinalIds.put(consensus.tempId, consensusReads);
                        consensusReads++;
                        int consensusReadsNum = consensus.consensusReadsNum;
                        Long currentSizeCount = consensusesByReadsCounts.get(consensusReadsNum);
                        consensusesByReadsCounts.put(consensusReadsNum, currentSizeCount == null ? 1 : currentSizeCount + 1);
                        if (toSeparateGroups)
                            consensus.getReadsWithConsensuses().forEach(writer::write);
                        else
                            writer.write(consensus.toParsedRead());
                    }
                    if (debugOutputStream != null) {
                        if ((consensusAlgorithmType != DOUBLE_MULTI_ALIGN) || !consensus.finalConsensus)
                            subclusterDebugIndex++;
                        consensus.debugData.writeDebugData(debugOutputStream, subclusterDebugIndex, i);
                    }
                }
                if (saveNotUsedReads)
                    calculatedConsensuses.notUsedReads.forEach(notUsedReadsWriter::write);
                notUsedReadsCount += calculatedConsensuses.notUsedReadsCount;
            }
            reader.close();
            originalNumberOfReads = reader.getOriginalNumberOfReads();
            writer.setOriginalNumberOfReads(originalNumberOfReads);
            if (saveNotUsedReads)
                notUsedReadsWriter.setOriginalNumberOfReads(originalNumberOfReads);
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        if (originalReadStatsFileName != null) {
            System.err.println("Writing file with stats for original reads...");
            try (PrintStream originalReadsDataWriter = new PrintStream(
                    new FileOutputStream(originalReadStatsFileName))) {
                List<String> defaultGroups = IntStream.rangeClosed(1, numberOfTargets).mapToObj(i -> "R" + i)
                        .collect(Collectors.toList());
                StringBuilder header = new StringBuilder();
                header.append("read.id ");              // common column 1
                header.append("consensus.id ");         // common column 2
                header.append("status ");               // common column 3
                header.append("consensus.best.id ");    // common column 4
                header.append("reads.num");             // common column 5
                for (String groupName : defaultGroups) {
                    header.append(' ').append(groupName).append(".seq ");           // target column 1
                    header.append(groupName).append(".qual ");                      // target column 2
                    header.append(groupName).append(".consensus.seq ");             // target column 3
                    header.append(groupName).append(".consensus.qual ");            // target column 4
                    header.append(groupName).append(".consensus.distance ");        // target column 5
                    header.append(groupName).append(".read.trimmed ");              // target column 6
                    header.append(groupName).append(".consensus.trimmed ");         // target column 7
                    header.append(groupName).append(".alignment.score.stage1 ");    // target column 8
                    header.append(groupName).append(".alignment.score.stage2");     // target column 9
                }
                originalReadsDataWriter.println(header);

                for (long readId = 0; readId < originalNumberOfReads; readId++) {
                    OriginalReadData currentReadData = originalReadsData.get(readId);
                    OriginalReadStatus status = (currentReadData == null) ? NOT_MATCHED : currentReadData.status;
                    Consensus consensus = (status == USED_IN_CONSENSUS) ? currentReadData.getConsensus() : null;

                    StringBuilder line = new StringBuilder();
                    line.append(readId).append(' ');        // common column 1
                    if (consensus == null) {
                        line.append("-1 ");                 // common column 2
                    } else {
                        long finalId = Objects.requireNonNull(consensusFinalIds).get(consensus.tempId);
                        if (finalId == -1)
                            throw new IllegalStateException("Consensus finalId == -1 for tempId " + consensus.tempId);
                        line.append(finalId).append(' ');   // common column 2
                    }
                    line.append(status.name()).append(' '); // common column 3
                    line.append((consensus == null) ? -1 : consensus.sequences.get((byte)1).getOriginalReadId())
                            .append(' ');                   // common column 4
                    line.append((consensus == null) ? 0 : consensus.consensusReadsNum);     // common column 5
                    for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
                        byte targetId = (byte)(targetIndex + 1);
                        long alignmentScoreStage1 = Long.MIN_VALUE;
                        long alignmentScoreStage2 = Long.MIN_VALUE;
                        if ((status == CONSENSUS_DISCARDED_TRIM_STAGE1) || (status == CONSENSUS_DISCARDED_TRIM_STAGE2)
                                || ((consensusAlgorithmType == DOUBLE_MULTI_ALIGN) && (status == USED_IN_CONSENSUS))) {
                            long[] alignmentScoresStage1 = Objects.requireNonNull(currentReadData)
                                    .alignmentScores.get(0);
                            if (alignmentScoresStage1 != null)
                                alignmentScoreStage1 = alignmentScoresStage1[targetIndex];
                            long[] alignmentScoresStage2 = Objects.requireNonNull(currentReadData)
                                    .alignmentScores.get(1);
                            if (alignmentScoresStage2 != null)
                                alignmentScoreStage2 = alignmentScoresStage2[targetIndex];
                        }
                        if (currentReadData == null) {
                            line.append(" - -");        // target columns 1, 2
                        } else {
                            ParsedRead parsedRead = currentReadData.read;
                            NSequenceWithQuality currentOriginalRead;
                            if (parsedRead.isNumberOfTargetsOverride()) {
                                currentOriginalRead = parsedRead.getMatchTarget(targetId);
                            } else {
                                int originalTargetIndex = targetIndex;
                                if (parsedRead.isReverseMatch()) {
                                    if (originalTargetIndex == 0)
                                        originalTargetIndex = 1;
                                    else if (originalTargetIndex == 1)
                                        originalTargetIndex = 0;
                                }
                                currentOriginalRead = parsedRead.getOriginalRead().getRead(originalTargetIndex)
                                        .getData();
                            }
                            line.append(' ').append(currentOriginalRead.getSequence());     // target column 1
                            line.append(' ').append(currentOriginalRead.getQuality());      // target column 2
                        }
                        if (consensus == null) {
                            line.append(" - - -1 ");    // target columns 3, 4, 5
                        } else {
                            SequenceWithAttributes currentSeq = consensus.sequences.get(targetId);
                            line.append(' ').append(currentSeq.getSeq());                   // target column 3
                            line.append(' ').append(currentSeq.getQual());                  // target column 4
                            int consensusDistance = (currentReadData == null) ? -1
                                    : currentReadData.getConsensusDistance(targetId);
                            line.append(' ').append(consensusDistance).append(' ');         // target column 5
                        }
                        if (currentReadData == null) {
                            line.append("0 ");      // target column 6
                        } else {
                            // target column 6
                            line.append(currentReadData.trimmedLettersCounters.byTargetId.get(targetId)).append(' ');
                        }
                        if ((currentReadData == null) || (currentReadData.consensusTrimmedLettersCounters == null)) {
                            line.append('0');      // target column 7
                        } else {
                            // target column 7
                            line.append(currentReadData.consensusTrimmedLettersCounters.byTargetId.get(targetId));
                        }
                        if (consensus == null) {
                            // target columns 8, 9
                            line.append(' ').append(Long.MIN_VALUE).append(' ').append(Long.MIN_VALUE);
                        } else {
                            line.append(' ').append(alignmentScoreStage1);      // target column 8
                            line.append(' ').append(alignmentScoreStage2);      // target column 9
                        }
                    }
                    originalReadsDataWriter.println(line);
                }
            } catch (IOException e) {
                throw exitWithError(e.getMessage());
            }
        }

        StringBuilder report = new StringBuilder();
        LinkedHashMap<String, Object> jsonReportData = new LinkedHashMap<>();
        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        report.append("Processed ").append(totalReads).append(" reads\n");
        report.append("Calculated ").append(consensusReads).append(" consensuses\n");
        report.append("Reads not used in any consensus: ").append(notUsedReadsCount).append("\n");
        if (consensusReads > 0)
            report.append("Average reads per consensus: ")
                    .append(floatFormat.format((float)totalReads.get() / consensusReads)).append("\n");
        if (clustersCount > 0)
            report.append("Average number of consensuses per barcode group: ")
                    .append(floatFormat.format((float)consensusReads / clustersCount)).append("\n");

        jsonReportData.put("version", getVersionString(VERSION_INFO_SHORTEST));
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("consensusGroups", consensusGroups);
        jsonReportData.put("consensusAlgorithmType", consensusAlgorithmType.toString());
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("totalReads", totalReads.get());
        jsonReportData.put("consensusReads", consensusReads);
        jsonReportData.put("clustersCount", clustersCount);
        jsonReportData.put("notUsedReadsCount", notUsedReadsCount);
        jsonReportData.put("consensusesByReadsCounts", consensusesByReadsCounts);

        System.err.println(report.toString());
        if (reportFileOutputStream != null) {
            if (consensusesByReadsCounts.size() > 0) {
                reportFileOutputStream.println("\nConsensuses counts distribution:");
                consensusesByReadsCounts.forEach((reads, count) -> reportFileOutputStream.println(
                        count + " consensus" + (count == 1 ? "" : "es")
                                +  " assembled from " + reads + " read" + (reads == 1 ? "" : "s")));
            }
            reportFileOutputStream.println(report.toString());
            reportFileOutputStream.close();
        }
        jsonReport(jsonReportFileName, jsonReportData);
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private MifWriter createWriter(MifHeader mifHeader) throws IOException {
        ArrayList<GroupEdge> groupEdges = mifHeader.getGroupEdges();
        numberOfTargets = mifHeader.getNumberOfTargets();
        MifHeader newHeader;
        if (toSeparateGroups) {
            Set<String> defaultSeparateGroups = IntStream.rangeClosed(1, numberOfTargets)
                    .mapToObj(i -> "CR" + i).collect(Collectors.toSet());
            if ((consensusGroups.stream().anyMatch(defaultSeparateGroups::contains))
                    || (groupEdges.stream().map(GroupEdge::getGroupName).anyMatch(defaultSeparateGroups::contains)))
                throw exitWithError("Groups CR1, CR2 etc must not be used in --groups flag and input file if "
                        + "--consensuses-to-separate-groups flag is specified!");
            defaultSeparateGroups.stream().sorted().forEachOrdered(name -> {
                groupEdges.add(new GroupEdge(name, true));
                groupEdges.add(new GroupEdge(name, false));
            });
        }
        newHeader = new MifHeader(pipelineConfiguration, numberOfTargets, mifHeader.getCorrectedGroups(),
                new ArrayList<>(), groupEdges);
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), newHeader)
                : new MifWriter(outputFileName, newHeader);
    }

    private LinkedHashMap<String, NucleotideSequence> extractConsensusGroups(ParsedRead parsedRead) {
        return parsedRead.getGroups().stream().filter(g -> consensusGroups.contains(g.getGroupName()))
                .collect(LinkedHashMap::new, (m, g) -> m.put(g.getGroupName(), g.getValue().getSequence()),
                        Map::putAll);
    }

    private DataFromParsedRead extractData(ParsedRead parsedRead) {
        return toSeparateGroups ? new DataFromParsedReadWithAllGroups(parsedRead, consensusGroups)
                : new DataFromParsedRead(parsedRead, consensusGroups);
    }

    private void saveOriginalReadsData(ParsedRead parsedRead) {
        if (originalReadsData != null) {
            synchronized (originalReadsData) {
                originalReadsData.putIfAbsent(parsedRead.getOriginalRead().getId(), new OriginalReadData(parsedRead));
            }
        }
    }

    private synchronized void displayWarning(String text) {
        if (maxWarnings == -1)
            System.err.println(text);
        else if ((maxWarnings > 0) && (warningsDisplayed < maxWarnings)) {
            System.err.println(text);
            warningsDisplayed++;
            if (warningsDisplayed == maxWarnings)
                System.err.println("Warnings limit reached!");
        }
        if (reportFileOutputStream != null)
            reportFileOutputStream.println(text);
    }
}
