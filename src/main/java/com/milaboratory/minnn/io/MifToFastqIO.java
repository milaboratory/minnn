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
import com.milaboratory.core.io.sequence.SequenceRead;
import com.milaboratory.core.io.sequence.SequenceWriter;
import com.milaboratory.core.io.sequence.fastq.MultiFastqWriter;
import com.milaboratory.core.io.sequence.fastq.PairedFastqWriter;
import com.milaboratory.core.io.sequence.fastq.SingleFastqWriter;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.io.ReportWriter.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;
import static com.milaboratory.minnn.util.SystemUtils.exitWithError;
import static com.milaboratory.util.FormatUtils.nanoTimeToString;

public final class MifToFastqIO {
    private final String inputFileName;
    private final String[] outputGroupNames;
    private final String[] outputFileNames;
    private final boolean copyOriginalHeaders;
    private final long inputReadsLimit;
    private final String reportFileName;
    private final String jsonReportFileName;
    private final boolean debugMode;

    public MifToFastqIO(String inputFileName, LinkedHashMap<String, String> outputGroups, boolean copyOriginalHeaders,
                        long inputReadsLimit, String reportFileName, String jsonReportFileName, boolean debugMode) {
        this.inputFileName = inputFileName;
        this.outputGroupNames = new String[outputGroups.size()];
        this.outputFileNames = new String[outputGroups.size()];
        int index = 0;
        for (HashMap.Entry<String, String> entry : outputGroups.entrySet()) {
            outputGroupNames[index] = entry.getKey();
            outputFileNames[index] = entry.getValue();
            index++;
        }
        this.copyOriginalHeaders = copyOriginalHeaders;
        this.inputReadsLimit = inputReadsLimit;
        this.reportFileName = reportFileName;
        this.jsonReportFileName = jsonReportFileName;
        this.debugMode = debugMode;
    }

    @SuppressWarnings("unchecked")
    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        String readerStats = null;
        try (MifReader reader = createReader();
             SequenceWriter writer = createWriter()) {
            LinkedHashSet<String> availableGroupNames = reader.getGroupEdges().stream().map(GroupEdge::getGroupName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            for (String groupName : outputGroupNames)
                if (!availableGroupNames.contains(groupName))
                    throw exitWithError("Group " + groupName + " not found in the input; available groups: "
                            + availableGroupNames);
            if (inputReadsLimit > 0)
                reader.setParsedReadsLimit(inputReadsLimit);
            SmartProgressReporter.startProgressReport("Processing", reader, System.err);
            OutputPortCloseable<SequenceRead> sequenceReads = new SequenceReadOutputPort(reader);
            for (SequenceRead sequenceRead : CUtils.it(sequenceReads)) {
                writer.write(sequenceRead);
                if (++totalReads == inputReadsLimit)
                    break;
            }
            if (debugMode)
                readerStats = reader.getStats().toString();
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        StringBuilder reportFileHeader = new StringBuilder();
        StringBuilder report = new StringBuilder();
        LinkedHashMap<String, Object> jsonReportData = new LinkedHashMap<>();

        reportFileHeader.append("MiNNN v").append(getVersionString(VERSION_INFO_SHORTEST)).append('\n');
        reportFileHeader.append("Report for MifToFastq command:\n");
        if (inputFileName == null)
            reportFileHeader.append("Input is from stdin\n");
        else
            reportFileHeader.append("Input file name: ").append(inputFileName).append('\n');
        reportFileHeader.append("Output group names: ").append(Arrays.toString(outputGroupNames)).append('\n');
        reportFileHeader.append("Output file names: ").append(Arrays.toString(outputFileNames)).append('\n');
        if (debugMode) {
            reportFileHeader.append("\n\nDebug information:\n\n");
            reportFileHeader.append("Reader stats:\n").append(readerStats).append("\n\n");
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        report.append("\nProcessing time: ").append(nanoTimeToString(elapsedTime * 1000000)).append('\n');
        report.append("Processed ").append(totalReads).append(" reads\n");

        jsonReportData.put("version", getVersionString(VERSION_INFO_SHORTEST));
        jsonReportData.put("inputFileName", inputFileName);
        jsonReportData.put("outputGroupNames", Arrays.toString(outputGroupNames));
        jsonReportData.put("outputFileNames", Arrays.toString(outputFileNames));
        jsonReportData.put("copyOriginalHeaders", copyOriginalHeaders);
        jsonReportData.put("elapsedTime", elapsedTime);
        jsonReportData.put("totalReads", totalReads);

        humanReadableReport(reportFileName, reportFileHeader.toString(), report.toString());
        jsonReport(jsonReportFileName, jsonReportData);
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private SequenceWriter createWriter() throws IOException {
        switch (outputFileNames.length) {
            case 1:
                return new SingleFastqWriter(outputFileNames[0]);
            case 2:
                return new PairedFastqWriter(outputFileNames[0], outputFileNames[1]);
            default:
                return new MultiFastqWriter(outputFileNames);
        }
    }

    private class SequenceReadOutputPort implements OutputPortCloseable<SequenceRead> {
        private final MifReader reader;
        private final ArrayList<GroupEdge> groupEdges;

        SequenceReadOutputPort(MifReader reader) {
            this.reader = reader;
            this.groupEdges = reader.getGroupEdges();
        }

        @Override
        public SequenceRead take() {
            ParsedRead parsedRead = reader.take();
            if (parsedRead == null)
                return null;
            else
                return parsedRead.toSequenceRead(copyOriginalHeaders, groupEdges, outputGroupNames);
        }

        @Override
        public void close() {
            reader.close();
        }
    }
}
