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
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.blocks.PrimitivOBlocks;
import com.milaboratory.primitivio.blocks.PrimitivOBlocksStats;
import com.milaboratory.primitivio.blocks.PrimitivOHybrid;
import com.milaboratory.util.CanReportProgress;

import java.io.*;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.Magic.*;
import static com.milaboratory.minnn.io.IOUtils.THREAD_POOL;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;
import static java.lang.Double.NaN;

public final class MifWriter implements PipelineConfigurationWriter, AutoCloseable, CanReportProgress {
    private final PrimitivOHybrid primitivOHybrid;
    private final PrimitivOBlocks<ParsedRead>.Writer writer;
    private boolean closed = false;
    private long estimatedNumberOfReads;
    private long writtenReads = 0;
    private long originalNumberOfReads;

    public MifWriter(String fileName, MifMetaInfo mifMetaInfo) throws IOException {
        this(fileName, mifMetaInfo, PRIMITIVIO_DEFAULT_CONCURRENCY);
    }

    public MifWriter(String fileName, MifMetaInfo mifMetaInfo, int concurrency) throws IOException {
        File file = new File(fileName);
        if (file.exists())
            if (!file.delete())
                throw new IOException("File " + fileName + " already exists and cannot be deleted!");
        primitivOHybrid = new PrimitivOHybrid(THREAD_POOL, file.toPath());
        writeHeader(mifMetaInfo);
        writer = primitivOHybrid.beginPrimitivOBlocks(concurrency, PRIMITIVIO_BLOCK_SIZE);
        this.estimatedNumberOfReads = mifMetaInfo.getNumberOfReads();
        this.originalNumberOfReads = mifMetaInfo.getOriginalNumberOfReads();
    }

    private void writeHeader(MifMetaInfo mifMetaInfo) {
        try (PrimitivO primitivO = beginPrimitivO()) {
            primitivO.write(getBeginMagicBytes());
            primitivO.writeUTF(getVersionString(VERSION_INFO_MIF));
            primitivO.writeObject(mifMetaInfo.getPipelineConfiguration());
            primitivO.writeInt(mifMetaInfo.getNumberOfTargets());
            primitivO.writeInt(mifMetaInfo.getCorrectedGroups().size());
            for (String correctedGroup : mifMetaInfo.getCorrectedGroups())
                primitivO.writeObject(correctedGroup);
            primitivO.writeInt(mifMetaInfo.getSortedGroups().size());
            for (String sortedGroup : mifMetaInfo.getSortedGroups())
                primitivO.writeObject(sortedGroup);
            primitivO.writeInt(mifMetaInfo.getGroupEdges().size());
            for (GroupEdge groupEdge : mifMetaInfo.getGroupEdges()) {
                primitivO.writeObject(groupEdge);
                primitivO.putKnownObject(groupEdge);
            }
        }
    }

    /** Thread unsafe: all writes must be in single thread and keep reads in order */
    public void write(ParsedRead parsedRead) {
        if (closed)
            throw new IllegalStateException("Attempt to write to closed MifWriter!");
        writer.write(parsedRead);
        writtenReads++;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            writer.close(); // This will also write stream termination symbol/block to the stream

            if (originalNumberOfReads == -1)
                throw new IllegalStateException("originalNumberOfReads is not initialized in MifWriter!");

            // writing footer
            try (PrimitivO primitivO = beginPrimitivO()) {
                primitivO.writeLong(writtenReads);
                primitivO.writeLong(originalNumberOfReads);
                primitivO.write(getEndMagicBytes());
            }
            primitivOHybrid.close();
            closed = true;
        }
    }

    public PrimitivOBlocksStats getStats() {
        return writer.getParent().getStats();
    }

    public void setEstimatedNumberOfReads(long estimatedNumberOfReads) {
        this.estimatedNumberOfReads = estimatedNumberOfReads;
    }

    /**
     * This function must be used only when input file is the original, and original number of reads is calculated
     * in the end. Later in pipeline, originalNumberOfReads must be set in the constructor.
     *
     * @param originalNumberOfReads number of reads in the original FASTQ files
     */
    public void setOriginalNumberOfReads(long originalNumberOfReads) {
        if (this.originalNumberOfReads != -1)
            throw new IllegalStateException("originalNumberOfReads is already set to " + this.originalNumberOfReads
                    + " in MifWriter, and trying to set new value " + originalNumberOfReads);
        this.originalNumberOfReads = originalNumberOfReads;
    }

    @Override
    public double getProgress() {
        return (estimatedNumberOfReads == -1) ? NaN : Math.min(1, (double)writtenReads / estimatedNumberOfReads);
    }

    @Override
    public boolean isFinished() {
        return closed;
    }

    private PrimitivO beginPrimitivO() {
        return primitivOHybrid.beginPrimitivO(false, PRIMITIVIO_BUFFER_SIZE);
    }
}
