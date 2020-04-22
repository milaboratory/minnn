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
import cc.redberry.pipe.OutputPortCloseable;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.core.io.CompressionType;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.outputconverter.ParsedReadObjectSerializer;
import com.milaboratory.util.SmartProgressReporter;
import com.milaboratory.util.Sorter;
import com.milaboratory.util.TempFileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.FormatUtils.nanoTimeToString;

public final class SorterIO {
    private final PipelineConfiguration pipelineConfiguration;
    private final String inputFileName;
    private final String outputFileName;
    private final List<String> sortGroupNames;
    private final int chunkSize;
    private final String reportFileName;
    private final String jsonReportFileName;
    private final boolean debugMode;
    private final File tmpFile;

    public SorterIO(
            PipelineConfiguration pipelineConfiguration, String inputFileName, String outputFileName,
            List<String> sortGroupNames, int chunkSize, String reportFileName, String jsonReportFileName,
            boolean debugMode, String tmpFile) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.sortGroupNames = sortGroupNames;
        this.chunkSize = (chunkSize == -1) ? estimateChunkSize() : chunkSize;
        this.reportFileName = reportFileName;
        this.jsonReportFileName = jsonReportFileName;
        this.debugMode = debugMode;
        this.tmpFile = (tmpFile != null) ? new File(tmpFile) : TempFileManager.getTempFile((outputFileName == null)
                ? null : Paths.get(new File(outputFileName).getAbsolutePath()).getParent());
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        String readerStats = null;
        String writerStats = null;
        try (MifReader reader = new MifReader(inputFileName);
             MifWriter writer = new MifWriter(outputFileName, new MifMetaInfo(pipelineConfiguration,
                     reader.getNumberOfTargets(), reader.getCorrectedGroups(), new ArrayList<>(sortGroupNames),
                     reader.getGroupEdges(), reader.getOriginalNumberOfReads()))) {
            validateInputGroups(reader, sortGroupNames, true, "--groups");
            SmartProgressReporter.startProgressReport("Reading", reader, System.err);
            OutputPortCloseable<ParsedRead> sorted = Sorter.sort(reader, new ParsedReadComparator(), chunkSize,
                    new ParsedReadObjectSerializer(reader.getGroupEdges()), tmpFile);
            SmartProgressReporter.startProgressReport("Writing", writer, System.err);
            writer.setEstimatedNumberOfReads(reader.getNumberOfReads());
            for (ParsedRead parsedRead : CUtils.it(sorted)) {
                writer.write(parsedRead);
                totalReads++;
            }
            if (debugMode) {
                readerStats = reader.getStats().toString();
                writerStats = writer.getStats().toString();
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        StringBuilder reportFileHeader = new StringBuilder();
        StringBuilder report = new StringBuilder();
        LinkedHashMap<String, Object> jsonReportData = new LinkedHashMap<>();

        reportFileHeader.append("MiNNN v").append(getVersionString(VERSION_INFO_SHORTEST)).append('\n');
        reportFileHeader.append("Report for Sort command:\n");
        if (inputFileName == null)
            reportFileHeader.append("Input is from stdin\n");
        else
            reportFileHeader.append("Input file name: ").append(inputFileName).append('\n');
        if (outputFileName == null)
            reportFileHeader.append("Output is to stdout\n");
        else
            reportFileHeader.append("Output file name: ").append(outputFileName).append('\n');
        reportFileHeader.append("Sorted groups: ").append(sortGroupNames).append('\n');
        if (debugMode) {
            reportFileHeader.append("\n\nDebug information:\n\n");
            reportFileHeader.append("Reader stats:\n").append(readerStats).append('\n');
            reportFileHeader.append("Writer stats:\n").append(writerStats).append("\n\n");
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        report.append("Sorted ").append(totalReads).append(" reads\n");

        jsonReportData.put("version", getVersionString(VERSION_INFO_SHORTEST));
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputFileName", outputFileName);
        jsonReportData.put("sortGroupNames", sortGroupNames);
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("totalReads", totalReads);
        jsonReportData.put("chunkSize", chunkSize);

        humanReadableReport(reportFileName, reportFileHeader.toString(), report.toString());
        jsonReport(jsonReportFileName, jsonReportData);
    }

    private int estimateChunkSize() {
        float chunkSize = Runtime.getRuntime().freeMemory() * DEFAULT_SORT_CHUNK_MEMORY_SHARE;
        if (inputFileName != null) {
            // heuristic to auto-determine chunk size by input file size
            File inputFile = new File(inputFileName);
            CompressionType ct = CompressionType.detectCompressionType(inputFile);
            int averageBytesPerParsedRead = (ct == CompressionType.None) ? 50 : 15;
            chunkSize = Math.min((float)inputFile.length() / averageBytesPerParsedRead / 8, chunkSize);
        }
        return (int)(Math.max(DEFAULT_SORT_MIN_CHUNK_SIZE, Math.min(DEFAULT_SORT_MAX_CHUNK_SIZE, chunkSize)));
    }

    // comparator that moves reads with wildcards in any group to the end
    private class ParsedReadComparator implements Comparator<ParsedRead> {
        @Override
        public int compare(ParsedRead parsedRead1, ParsedRead parsedRead2) {
            boolean firstContainsWildcards = false;
            boolean secondContainsWildcards = false;
            int comparisonResult = 0;
            for (String groupName : sortGroupNames) {
                NSequenceWithQuality read1Value = parsedRead1.getBestMatch().getGroupValue(groupName);
                NSequenceWithQuality read2Value = parsedRead2.getBestMatch().getGroupValue(groupName);
                if ((read1Value != null) && !firstContainsWildcards)
                    firstContainsWildcards = read1Value.getSequence().containsWildcards();
                if ((read2Value != null) && !secondContainsWildcards)
                    secondContainsWildcards = read2Value.getSequence().containsWildcards();

                if (comparisonResult == 0) {
                    if ((read1Value == null) && (read2Value != null))
                        comparisonResult = -1;
                    else if ((read1Value != null) && (read2Value == null))
                        comparisonResult = 1;
                    else if (read1Value != null)
                        comparisonResult = read1Value.getSequence().compareTo(read2Value.getSequence());
                }

                if (firstContainsWildcards && secondContainsWildcards && (comparisonResult != 0))
                    return comparisonResult;
            }
            if (firstContainsWildcards && !secondContainsWildcards)
                return 1;
            else if (!firstContainsWildcards && secondContainsWildcards)
                return -1;
            return comparisonResult;
        }
    }
}
