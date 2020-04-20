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
import cc.redberry.pipe.OutputPortCloseable;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.Merger;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.Chunk;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.cli.PipelineConfigurationWriter;
import com.milaboratory.core.io.CompressionType;
import com.milaboratory.core.io.sequence.SequenceRead;
import com.milaboratory.core.io.sequence.SequenceWriter;
import com.milaboratory.core.io.sequence.SingleReadImpl;
import com.milaboratory.core.io.sequence.fastq.SingleFastqWriter;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.minnn.cli.PipelineConfigurationReaderMiNNN;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.util.CanReportProgress;
import com.milaboratory.util.CountingInputStream;
import com.milaboratory.util.SmartProgressReporter;

import java.io.*;
import java.util.*;

import static com.milaboratory.minnn.cli.Magic.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.FormatUtils.*;
import static java.lang.Double.NaN;

public final class PerformanceTestIO {
    private final String inputFileName;
    private final String outputFileName;
    private MifReaderForTest mifReader;

    public PerformanceTestIO(String inputFileName, String outputFileName) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
    }

    @SuppressWarnings("unchecked")
    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        try (MifReaderForTest reader = new MifReaderForTest();
             SequenceWriter writer = new SingleFastqWriter(outputFileName)) {
            mifReader = reader;
            SmartProgressReporter.startProgressReport("Processing", reader, System.err);
            Merger<Chunk<ParsedRead>> bufferedReaderPort = CUtils.buffered(CUtils.chunked(reader, 4 * 20000),
                    4 * 10000);
            OutputPort<Chunk<SequenceRead>> sequenceReads = new ParallelProcessor<>(bufferedReaderPort,
                    CUtils.chunked(new ReadProcessor()), 1);
            OrderedOutputPort<SequenceRead> orderedReadsPort = new OrderedOutputPort<>(CUtils.unchunked(sequenceReads),
                    SequenceRead::getId);
            for (SequenceRead sequenceRead : CUtils.it(orderedReadsPort)) {
                writer.write(sequenceRead);
                totalReads++;
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.out.println("Processed " + totalReads + " reads");
    }

    private class MifReaderForTest extends PipelineConfigurationReaderMiNNN
            implements OutputPortCloseable<ParsedRead>, CanReportProgress {
        final int BUFFER_SIZE = 1 << 20;
        final PrimitivI input;
        final CountingInputStream countingInputStream;
        final long size;
        long parsedReadsTaken = 0;
        boolean finished = false;
        boolean closed = false;
        PipelineConfiguration pipelineConfiguration;
        int numberOfTargets;
        ArrayList<String> correctedGroups = new ArrayList<>();
        ArrayList<String> sortedGroups = new ArrayList<>();
        ArrayList<GroupEdge> groupEdges = new ArrayList<>();
        int firstReadSerializedLength = -1;
        long originalNumberOfReads = -1;
        String mifVersionInfo;

        public MifReaderForTest() throws IOException {
            File file = new File(inputFileName);
            CompressionType ct = CompressionType.detectCompressionType(file);
            this.countingInputStream = new CountingInputStream(new FileInputStream(file));
            if (ct == CompressionType.None) {
                input = new PrimitivI(new BufferedInputStream(this.countingInputStream, BUFFER_SIZE));
                size = file.length();
            } else {
                input = new PrimitivI(ct.createInputStream(this.countingInputStream, BUFFER_SIZE));
                size = -1;
            }
            readHeader();
        }

        private void readHeader() {
            byte[] magicBytes = new byte[BEGIN_MAGIC_LENGTH];
            try {
                input.readFully(magicBytes);
            } catch (RuntimeException e) {
                throw exitWithError("Unsupported file format; error while reading file header: " + e.getMessage());
            }
            String magicString = new String(magicBytes);
            if (!magicString.equals(BEGIN_MAGIC))
                throw exitWithError("Unsupported file format; .mif file of version " + magicString +
                        " while you are running MiNNN " + BEGIN_MAGIC);
            mifVersionInfo = input.readUTF();
            pipelineConfiguration = input.readObject(PipelineConfiguration.class);
            numberOfTargets = input.readInt();
            int correctedGroupsNum = input.readInt();
            for (int i = 0; i < correctedGroupsNum; i++)
                correctedGroups.add(input.readObject(String.class));
            int sortedGroupsNum = input.readInt();
            for (int i = 0; i < sortedGroupsNum; i++)
                sortedGroups.add(input.readObject(String.class));
            int groupEdgesNum = input.readInt();
            for (int i = 0; i < groupEdgesNum; i++) {
                GroupEdge groupEdge = input.readObject(GroupEdge.class);
                input.putKnownObject(groupEdge);
                groupEdges.add(groupEdge);
            }
        }

        @Override
        public synchronized void close() {
            if (!closed) {
                originalNumberOfReads = parsedReadsTaken;
                input.close();
                finished = true;
                closed = true;
            }
        }

        @Override
        public double getProgress() {
            if (size == -1)
                return NaN;
            else
                return (double)(countingInputStream.getBytesRead()) / size;
        }

        @Override
        public synchronized ParsedRead take() {
            if (finished)
                return null;
            ParsedRead parsedRead = input.readObject(ParsedRead.class);
            if (parsedRead == null)
                finished = true;
            else {
                if (firstReadSerializedLength == -1)
                    calculateFirstReadLength(parsedRead);
                parsedReadsTaken++;
            }
            return parsedRead;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        private void calculateFirstReadLength(ParsedRead parsedRead) {
            ByteArrayOutputStream counterStream = new ByteArrayOutputStream();
            PrimitivO outStream = new PrimitivO(counterStream);
            outStream.writeObject(parsedRead);
            outStream.close();
            firstReadSerializedLength = counterStream.toByteArray().length;
        }
    }

    private class MifWriterForTest implements PipelineConfigurationWriter, AutoCloseable {
        final int BUFFER_SIZE = 1 << 20;
        final PrimitivO output;
        boolean closed = false;
        long writtenReads = 0;

        public MifWriterForTest(MifMetaInfo mifMetaInfo) throws IOException {
            output = new PrimitivO(new BufferedOutputStream(new FileOutputStream(outputFileName), BUFFER_SIZE));
            writeHeader(mifMetaInfo);
        }

        private void writeHeader(MifMetaInfo mifMetaInfo) {
            output.write(getBeginMagicBytes());
            output.writeUTF(getVersionString(VERSION_INFO_MIF));
            output.writeObject(mifMetaInfo.getPipelineConfiguration());
            output.writeInt(mifMetaInfo.getNumberOfTargets());
            output.writeInt(mifMetaInfo.getCorrectedGroups().size());
            for (String correctedGroup : mifMetaInfo.getCorrectedGroups())
                output.writeObject(correctedGroup);
            output.writeInt(mifMetaInfo.getSortedGroups().size());
            for (String sortedGroup : mifMetaInfo.getSortedGroups())
                output.writeObject(sortedGroup);
            output.writeInt(mifMetaInfo.getGroupEdges().size());
            for (GroupEdge groupEdge : mifMetaInfo.getGroupEdges()) {
                output.writeObject(groupEdge);
                output.putKnownObject(groupEdge);
            }
        }

        public void write(ParsedRead parsedRead) {
            output.writeObject(parsedRead);
            writtenReads++;
        }

        @Override
        public void close() {
            if (!closed) {
                output.writeObject(null);
                output.writeLong(writtenReads);
                output.write(getEndMagicBytes());
                output.close();
                closed = true;
            }
        }
    }

    private class SequenceReadOutputPort implements OutputPort<SequenceRead> {
        final OutputPort<ParsedRead> bufferedReader;

        SequenceReadOutputPort(OutputPort<ParsedRead> bufferedReader) {
            this.bufferedReader = bufferedReader;
        }

        @Override
        public SequenceRead take() {
            ParsedRead parsedRead = bufferedReader.take();
            if (parsedRead == null)
                return null;
            else
                return parsedRead.toSequenceRead(false, mifReader.groupEdges, "R1");
        }
    }

    private class DummySequenceReadOutputPort implements OutputPort<SequenceRead> {
        final OutputPort<ParsedRead> bufferedReader;
        SequenceRead firstRead = null;

        DummySequenceReadOutputPort(OutputPort<ParsedRead> bufferedReader) {
            this.bufferedReader = bufferedReader;
        }

        @Override
        public SequenceRead take() {
            ParsedRead parsedRead = bufferedReader.take();
            if (parsedRead == null)
                return null;
            else if (firstRead == null)
                firstRead = parsedRead.toSequenceRead(false, mifReader.groupEdges, "R1");
            return firstRead;
        }
    }

    private class ReadProcessor implements Processor<ParsedRead, SequenceRead> {
        long id = 0;

        @Override
        public SequenceRead process(ParsedRead parsedRead) {
            if (parsedRead == null)
                return null;
            SingleReadImpl firstRead = (SingleReadImpl)(parsedRead.toSequenceRead(false,
                    mifReader.groupEdges, "R1"));
            NSequenceWithQuality seq = firstRead.getData();
            String description = firstRead.getDescription();
            return new SingleReadImpl(id++, seq, description);
        }
    }

    private class DummyProcessor implements Processor<ParsedRead, SequenceRead> {
        long id = 0;
        NSequenceWithQuality seq = null;
        String description = null;

        @Override
        public SequenceRead process(ParsedRead parsedRead) {
            if (parsedRead == null)
                return null;
            else if (seq == null) {
                SingleReadImpl firstRead = (SingleReadImpl)(parsedRead.toSequenceRead(false,
                        mifReader.groupEdges, "R1"));
                seq = firstRead.getData();
                description = firstRead.getDescription();
            }
            return new SingleReadImpl(id++, seq, description);
        }
    }
}
