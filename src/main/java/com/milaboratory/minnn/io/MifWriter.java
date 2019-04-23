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

import com.milaboratory.cli.PipelineConfigurationWriter;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.minnn.util.DebugUtils.*;
import com.milaboratory.primitivio.PrimitivOState;
import com.milaboratory.primitivio.blocks.PrimitivOBlocks;
import com.milaboratory.util.CanReportProgress;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.milaboratory.minnn.cli.Magic.*;
import static com.milaboratory.minnn.io.IODefaults.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;
import static java.lang.Double.NaN;

public final class MifWriter implements PipelineConfigurationWriter, AutoCloseable, CanReportProgress {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final PrimitivOBlocks<Object> primitivOBlocks;
    private final PrimitivOBlocks<Object>.Writer writer;
    private boolean closed = false;
    private long estimatedNumberOfReads = -1;
    private long writtenReads = 0;
    private long originalNumberOfReads = -1;

    public MifWriter(OutputStream outputStream, MifHeader mifHeader) {
        this(outputStream, mifHeader, DEFAULT_CONCURRENCY, DEFAULT_BLOCK_SIZE, DEFAULT_COMPRESSION_TYPE);
    }

    public MifWriter(String fileName, MifHeader mifHeader) throws IOException {
        this(fileName, mifHeader, DEFAULT_CONCURRENCY, DEFAULT_BLOCK_SIZE, DEFAULT_COMPRESSION_TYPE);
    }

    public MifWriter(String fileName, MifHeader mifHeader, int concurrency, int blockSize,
                     CompressionType compressionType) throws IOException {
        LZ4Factory lz4Factory = LZ4Factory.fastestInstance();
        LZ4Compressor compressor;
        switch (compressionType) {
            case FAST:
                compressor = lz4Factory.fastCompressor();
                break;
            case HIGH:
                compressor = lz4Factory.highCompressor();
                break;
            default:
                throw new IllegalStateException("Unexpected compression type: " + compressionType);
        }
        primitivOBlocks = new PrimitivOBlocks<>(executorService, concurrency, PrimitivOState.INITIAL, blockSize,
                compressor);
        writer = primitivOBlocks.newWriter(new File(fileName).toPath());
        writeHeader(mifHeader);
        // this(new FileOutputStream(file), mifHeader, concurrency, blockSize, compressionType);
    }

    public MifWriter(OutputStream outputStream, MifHeader mifHeader, int concurrency, int blockSize,
                     CompressionType compressionType) {
        throw new NotImplementedException();
//        LZ4Factory lz4Factory = LZ4Factory.fastestInstance();
//        LZ4Compressor compressor;
//        switch (compressionType) {
//            case FAST:
//                compressor = lz4Factory.fastCompressor();
//                break;
//            case HIGH:
//                compressor = lz4Factory.highCompressor();
//                break;
//            default:
//                throw new IllegalStateException("Unexpected compression type: " + compressionType);
//        }
//        primitivOBlocks = new PrimitivOBlocks(executorService, concurrency, PrimitivOState.INITIAL, blockSize,
//                compressor);
//        writer = primitivOBlocks.newWriter(outputStream);
    }

    private void writeHeader(MifHeader mifHeader) {
        writer.write(getBeginMagicBytes());
        writer.write(getVersionString(VERSION_INFO_MIF));
        writer.write(mifHeader.getPipelineConfiguration());
        writer.write(mifHeader.getNumberOfTargets());
        writer.write(mifHeader.getCorrectedGroups().size());
        for (String correctedGroup : mifHeader.getCorrectedGroups())
            writer.write(correctedGroup);
        writer.write(mifHeader.getSortedGroups().size());
        for (String sortedGroup : mifHeader.getSortedGroups())
            writer.write(sortedGroup);
        writer.write(mifHeader.getGroupEdges().size());
        for (GroupEdge groupEdge : mifHeader.getGroupEdges())
            writer.write(groupEdge);
    }

    public void write(ParsedRead parsedRead) {
        writer.write(parsedRead);
        writtenReads++;
    }

    @Override
    public void close() {
        if (!closed) {
            writer.write(null);
            writer.write(originalNumberOfReads);
            writer.write(getEndMagicBytes());
            writer.close();
            closed = true;
        }
    }

    public void setOriginalNumberOfReads(long originalNumberOfReads) {
        this.originalNumberOfReads = originalNumberOfReads;
    }

    public void setEstimatedNumberOfReads(long estimatedNumberOfReads) {
        this.estimatedNumberOfReads = estimatedNumberOfReads;
    }

    @Override
    public double getProgress() {
        if (estimatedNumberOfReads == -1)
            return (writtenReads == 0) ? 0 : NaN;
        else
            return Math.min(1, (double)writtenReads / estimatedNumberOfReads);
    }

    @Override
    public boolean isFinished() {
        return closed;
    }
}
