/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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
import cc.redberry.pipe.blocks.Merger;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.Chunk;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.readfilter.ReadFilter;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;

import static com.milaboratory.minnn.cli.CliUtils.floatFormat;
import static com.milaboratory.minnn.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class FilterIO {
    private final PipelineConfiguration pipelineConfiguration;
    private final ReadFilter readFilter;
    private final String inputFileName;
    private final String outputFileName;
    private final long inputReadsLimit;
    private final int threads;

    public FilterIO(PipelineConfiguration pipelineConfiguration, ReadFilter readFilter, String inputFileName,
                    String outputFileName, long inputReadsLimit, int threads) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.readFilter = readFilter;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.inputReadsLimit = inputReadsLimit;
        this.threads = threads;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        long matchedReads = 0;
        try (MifReader reader = createReader();
             MifWriter writer = createWriter(new MifHeader(pipelineConfiguration, reader.getNumberOfTargets(),
                     reader.getCorrectedGroups(), reader.getSortedGroups(), reader.getGroupEdges()))) {
            if (inputReadsLimit > 0)
                reader.setParsedReadsLimit(inputReadsLimit);
            SmartProgressReporter.startProgressReport("Filtering reads", reader, System.err);
            Merger<Chunk<ParsedRead>> bufferedReaderPort = CUtils.buffered(CUtils.chunked(
                    new NumberedParsedReadsPort(reader), 4 * 64), 4 * 16);
            OutputPort<Chunk<ParsedRead>> filteredReadsPort = new ParallelProcessor<>(bufferedReaderPort,
                    CUtils.chunked(new FilterProcessor()), threads);
            OrderedOutputPort<ParsedRead> orderedReadsPort = new OrderedOutputPort<>(
                    CUtils.unchunked(filteredReadsPort), ParsedRead::getOutputPortId);
            for (ParsedRead parsedRead : CUtils.it(orderedReadsPort)) {
                if (parsedRead.getBestMatch() != null) {
                    writer.write(parsedRead);
                    matchedReads++;
                }
                if (++totalReads == inputReadsLimit)
                    break;
            }
            reader.close();
            writer.setOriginalNumberOfReads(reader.getOriginalNumberOfReads());
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        float percent = (totalReads == 0) ? 0 : (float)matchedReads / totalReads * 100;
        System.err.println("Processed " + totalReads + " reads, matched " + matchedReads + " reads ("
                + floatFormat.format(percent) + "%)\n");
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private MifWriter createWriter(MifHeader mifHeader) throws IOException {
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), mifHeader)
                : new MifWriter(outputFileName, mifHeader);
    }

    private class FilterProcessor implements Processor<ParsedRead, ParsedRead> {
        @Override
        public ParsedRead process(ParsedRead parsedRead) {
            return readFilter.filter(parsedRead);
        }
    }
}
