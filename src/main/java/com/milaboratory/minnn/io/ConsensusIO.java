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
import gnu.trove.map.hash.TLongLongHashMap;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.consensus.ConsensusAlgorithms.*;
import static com.milaboratory.minnn.consensus.OriginalReadStatus.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.getShortestVersionString;
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
    private final int readsMinGoodSeqLength;
    private final float readsAvgQualityThreshold;
    private final int readsTrimWindowSize;
    private final int minGoodSeqLength;
    private final float avgQualityThreshold;
    private final int trimWindowSize;
    private final String originalReadStatsFileName;
    private final String notUsedReadsOutputFileName;
    private final boolean toSeparateGroups;
    private final long inputReadsLimit;
    private final int maxWarnings;
    private final int threads;
    private final int kmerLength;
    private final int kmerMaxOffset;
    private final int kmerMatchMaxErrors;
    private final String reportFileName;
    private final String jsonReportFileName;
    private final StringBuilder reportedWarnings = new StringBuilder();
    private final PrintStream debugOutputStream;
    private final byte debugQualityThreshold;
    private final AtomicLong totalReads = new AtomicLong(0);
    private final ConcurrentHashMap<Long, OriginalReadData> originalReadsData;
    private final TLongLongHashMap consensusFinalIds;
    private ConsensusAlgorithm consensusAlgorithm;
    private long consensusReads = 0;
    private long clustersCount = 0;
    private int warningsDisplayed = 0;
    private LinkedHashSet<String> consensusGroups;
    private int numberOfTargets;

    public ConsensusIO(PipelineConfiguration pipelineConfiguration, List<String> groupList, String inputFileName,
                       String outputFileName, ConsensusAlgorithms consensusAlgorithmType, int alignerWidth,
                       int matchScore, int mismatchScore, int gapScore, long goodQualityMismatchPenalty,
                       byte goodQualityMismatchThreshold, long scoreThreshold, float skippedFractionToRepeat,
                       int maxConsensusesPerCluster, int readsMinGoodSeqLength, float readsAvgQualityThreshold,
                       int readsTrimWindowSize, int minGoodSeqLength, float avgQualityThreshold, int trimWindowSize,
                       String originalReadStatsFileName, String notUsedReadsOutputFileName, boolean toSeparateGroups,
                       long inputReadsLimit, int maxWarnings, int threads, int kmerLength, int kmerMaxOffset,
                       int kmerMatchMaxErrors, String reportFileName, String jsonReportFileName,
                       String debugOutputFileName, byte debugQualityThreshold) {
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
        this.readsMinGoodSeqLength = readsMinGoodSeqLength;
        this.readsAvgQualityThreshold = readsAvgQualityThreshold;
        this.readsTrimWindowSize = readsTrimWindowSize;
        this.toSeparateGroups = toSeparateGroups;
        this.minGoodSeqLength = minGoodSeqLength;
        this.avgQualityThreshold = avgQualityThreshold;
        this.trimWindowSize = trimWindowSize;
        this.originalReadStatsFileName = originalReadStatsFileName;
        this.notUsedReadsOutputFileName = notUsedReadsOutputFileName;
        this.inputReadsLimit = inputReadsLimit;
        this.maxWarnings = maxWarnings;
        this.threads = threads;
        this.kmerLength = kmerLength;
        this.kmerMaxOffset = kmerMaxOffset;
        this.kmerMatchMaxErrors = kmerMatchMaxErrors;
        this.reportFileName = reportFileName;
        this.jsonReportFileName = jsonReportFileName;
        try {
            debugOutputStream = (debugOutputFileName == null) ? null
                    : new PrintStream(new FileOutputStream(debugOutputFileName));
        } catch (IOException e) {
            throw exitWithError(e.toString());
        }
        this.debugQualityThreshold = debugQualityThreshold;
        this.originalReadsData = ((originalReadStatsFileName != null) || (notUsedReadsOutputFileName != null))
                ? new ConcurrentHashMap<>() : null;
        this.consensusFinalIds = (originalReadStatsFileName == null) ? null : new TLongLongHashMap();
    }

    private void consensusAlgorithmInit() {
        switch(consensusAlgorithmType) {
            case DOUBLE_MULTI_ALIGN:
                consensusAlgorithm = new ConsensusAlgorithmDoubleMultiAlign(this::displayWarning, numberOfTargets,
                        alignerWidth, matchScore, mismatchScore, gapScore, goodQualityMismatchPenalty,
                        goodQualityMismatchThreshold, scoreThreshold, skippedFractionToRepeat,
                        maxConsensusesPerCluster, readsMinGoodSeqLength, readsAvgQualityThreshold,
                        readsTrimWindowSize, minGoodSeqLength, avgQualityThreshold, trimWindowSize, toSeparateGroups,
                        debugOutputStream, debugQualityThreshold, originalReadsData);
                break;
            case RNA_SEQ:
                consensusAlgorithm = new ConsensusAlgorithmRNASeq();
                break;
            case SINGLE_CELL:
                consensusAlgorithm = new ConsensusAlgorithmSingleCell(this::displayWarning, numberOfTargets,
                        maxConsensusesPerCluster, skippedFractionToRepeat, readsMinGoodSeqLength,
                        readsAvgQualityThreshold, readsTrimWindowSize, minGoodSeqLength, avgQualityThreshold,
                        trimWindowSize, toSeparateGroups, debugOutputStream, debugQualityThreshold, originalReadsData,
                        kmerLength, kmerMaxOffset, kmerMatchMaxErrors);
                break;
        }
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        MifHeader mifHeader;
        long originalNumberOfReads;
        try (MifReader reader = createReader();
             MifWriter writer = createWriter(mifHeader = reader.getHeader())) {
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
                AtomicInteger orderedPortIndex = new AtomicInteger(0);
                for (ParsedRead parsedRead : CUtils.it(reader)) {
                    LinkedHashMap<String, NucleotideSequence> groups = extractConsensusGroups(parsedRead);
                    allClusters.computeIfAbsent(groups, g -> new Cluster(orderedPortIndex.getAndIncrement()));
                    allClusters.get(groups).data.add(extractData(parsedRead));
                    saveOriginalReadsData(parsedRead);
                    if (totalReads.incrementAndGet() == inputReadsLimit)
                        break;
                }
                clusterOutputPort = new OutputPort<Cluster>() {
                    Iterator<Cluster> clusters = allClusters.values().iterator();

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
                    Cluster currentCluster = new Cluster(0);
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
                                        currentCluster = new Cluster(++orderedPortIndex);
                                    }
                                    previousGroups = currentGroups;
                                }
                                currentCluster.data.add(extractData(parsedRead));
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
            }
            reader.close();
            originalNumberOfReads = reader.getOriginalNumberOfReads();
            writer.setOriginalNumberOfReads(originalNumberOfReads);
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        if (originalReadStatsFileName != null) {
            System.err.println("Writing file with stats for original reads...");
            try (PrintStream originalReadsDataWriter = new PrintStream(
                    new FileOutputStream(originalReadStatsFileName))) {
                List<String> defaultGroups = IntStream.rangeClosed(1, numberOfTargets).mapToObj(i -> "R" + i)
                        .collect(Collectors.toList());
                StringBuilder header = new StringBuilder("read.id consensus.id status distance ");
                header.append("read.trimmed consensus.trimmed consensus.best.id reads.num");
                for (String groupName : defaultGroups) {
                    header.append(' ').append(groupName).append(".seq ");
                    header.append(groupName).append(".qual ");
                    header.append(groupName).append(".consensus.seq ");
                    header.append(groupName).append(".consensus.qual ");
                    header.append(groupName).append(".alignment.score.stage1 ");
                    header.append(groupName).append(".alignment.score.stage2");
                }
                originalReadsDataWriter.println(header);

                for (long readId = 0; readId < originalNumberOfReads; readId++) {
                    OriginalReadData currentReadData = originalReadsData.get(readId);
                    OriginalReadStatus status = (currentReadData == null) ? NOT_MATCHED : currentReadData.status;
                    Consensus consensus = (status == USED_IN_CONSENSUS) ? currentReadData.consensus : null;
                    int consensusDistance = (currentReadData == null) ? -1 : currentReadData.getConsensusDistance();

                    StringBuilder line = new StringBuilder();
                    line.append(readId).append(' ');
                    if (consensus == null)
                        line.append("-1 ");
                    else {
                        long finalId = Objects.requireNonNull(consensusFinalIds).get(consensus.tempId);
                        if (finalId == -1)
                            throw new IllegalStateException("Consensus finalId == -1 for tempId " + consensus.tempId);
                        line.append(finalId).append(' ');
                    }
                    line.append(status.name()).append(' ');
                    line.append(consensusDistance).append(' ');
                    line.append(0).append(' '); // TODO: trimmed letters from read
                    line.append((consensus == null) ? 0 : consensus.trimmedBadQualityLetters);
                    line.append((consensus == null) ? -1 : consensus.sequences.get((byte)1).getOriginalReadId())
                            .append(' ');
                    line.append((consensus == null) ? 0 : consensus.consensusReadsNum);
                    // targetIndex is targetId - 1
                    for (int targetIndex = 0; targetIndex < numberOfTargets; targetIndex++) {
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
                        if (currentReadData == null)
                            line.append(" - -");
                        else {
                            ParsedRead parsedRead = currentReadData.read;
                            NSequenceWithQuality currentOriginalRead;
                            if (parsedRead.isNumberOfTargetsOverride()) {
                                currentOriginalRead = parsedRead.getGroupValue("R" + (targetIndex + 1));
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
                            line.append(' ').append(currentOriginalRead.getSequence());
                            line.append(' ').append(currentOriginalRead.getQuality());
                        }
                        if (consensus == null) {
                            line.append(" - - ").append(Long.MIN_VALUE).append(' ').append(Long.MIN_VALUE);
                        } else {
                            SequenceWithAttributes currentSeq = consensus.sequences.get((byte)(targetIndex + 1));
                            line.append(' ').append(currentSeq.getSeq());
                            line.append(' ').append(currentSeq.getQual());
                            line.append(' ').append(alignmentScoreStage1);
                            line.append(' ').append(alignmentScoreStage2);
                        }
                    }
                    originalReadsDataWriter.println(line);
                }
            } catch (IOException e) {
                throw exitWithError(e.getMessage());
            }
        }

        if (notUsedReadsOutputFileName != null) {
            System.err.println("Writing not matched reads...");
            try (MifWriter notUsedReadsWriter = new MifWriter(notUsedReadsOutputFileName, new MifHeader(
                    pipelineConfiguration, numberOfTargets, mifHeader.getCorrectedGroups(),
                    mifHeader.getSortedGroups(), mifHeader.getGroupEdges()))) {
                for (long readId = 0; readId < originalNumberOfReads; readId++) {
                    OriginalReadData currentReadData = originalReadsData.get(readId);
                    if ((currentReadData != null) && (currentReadData.status != USED_IN_CONSENSUS))
                        notUsedReadsWriter.write(currentReadData.read);
                }
                notUsedReadsWriter.setOriginalNumberOfReads(originalNumberOfReads);
            } catch (IOException e) {
                throw exitWithError(e.getMessage());
            }
        }

        StringBuilder reportFileHeader = new StringBuilder();
        StringBuilder report = new StringBuilder();
        LinkedHashMap<String, Object> jsonReportData = new LinkedHashMap<>();

        reportFileHeader.append("MiNNN v").append(getShortestVersionString()).append('\n');
        reportFileHeader.append("Report for Consensus command:\n");
        if (inputFileName == null)
            reportFileHeader.append("Input is from stdin\n");
        else
            reportFileHeader.append("Input file name: ").append(inputFileName).append('\n');
        if (outputFileName == null)
            reportFileHeader.append("Output is to stdout\n");
        else
            reportFileHeader.append("Output file name: ").append(outputFileName).append('\n');
        reportFileHeader.append("Consensus assembled by groups: ").append(consensusGroups).append('\n');
        reportFileHeader.append("Consensus algorithm: ").append(consensusAlgorithmType).append('\n');
        reportFileHeader.append(reportedWarnings);

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        report.append("Processed ").append(totalReads).append(" reads\n");
        report.append("Calculated ").append(consensusReads).append(" consensuses\n");
        if (consensusReads > 0)
            report.append("Average reads per consensus: ")
                    .append(floatFormat.format((float)totalReads.get() / consensusReads)).append("\n");
        if (clustersCount > 0)
            report.append("Average number of consensuses per barcode group: ")
                    .append(floatFormat.format((float)consensusReads / clustersCount)).append("\n");

        jsonReportData.put("version", getShortestVersionString());
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("consensusGroups", consensusGroups);
        jsonReportData.put("consensusAlgorithmType", consensusAlgorithmType.toString());
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("totalReads", totalReads.get());
        jsonReportData.put("consensusReads", consensusReads);
        jsonReportData.put("clustersCount", clustersCount);

        humanReadableReport(reportFileName, reportFileHeader.toString(), report.toString());
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
                : new BasicDataFromParsedRead(parsedRead, consensusGroups);
    }

    private void saveOriginalReadsData(ParsedRead parsedRead) {
        if ((originalReadsData != null) && !originalReadsData.containsKey(parsedRead.getOriginalRead().getId()))
            originalReadsData.put(parsedRead.getOriginalRead().getId(), new OriginalReadData(parsedRead));
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
        if (reportFileName != null)
            reportedWarnings.append(text).append('\n');
    }
}
