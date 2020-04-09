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
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.blocks.PrimitivOBlocks;
import com.milaboratory.primitivio.blocks.PrimitivOBlocksStats;
import com.milaboratory.primitivio.blocks.PrimitivOHybrid;
import com.milaboratory.util.CanReportProgress;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.milaboratory.minnn.cli.Magic.*;
import static com.milaboratory.minnn.io.IODefaults.*;
import static com.milaboratory.minnn.util.MinnnVersionInfo.*;
import static com.milaboratory.minnn.util.MinnnVersionInfoType.*;
import static java.lang.Double.NaN;

public final class MifWriter implements PipelineConfigurationWriter, AutoCloseable, CanReportProgress {
    private final PrimitivOHybrid primitivOHybrid;
    private final PrimitivOBlocks<ParsedRead>.Writer writer;
    private boolean closed = false;
    private long estimatedNumberOfReads = -1;
    private long writtenReads = 0;
    private long originalNumberOfReads = -1;

    public MifWriter(OutputStream outputStream, MifHeader mifHeader) {
        this(outputStream, mifHeader, Executors.newCachedThreadPool(), DEFAULT_CONCURRENCY);
    }

    public MifWriter(
            OutputStream outputStream, MifHeader mifHeader, ExecutorService executorService, int concurrency) {
        throw new NotImplementedException();
    }

    public MifWriter(String fileName, MifHeader mifHeader) throws IOException {
        this(fileName, mifHeader, Executors.newCachedThreadPool(), DEFAULT_CONCURRENCY);
    }

    public MifWriter(String fileName, MifHeader mifHeader, ExecutorService executorService, int concurrency)
            throws IOException {
        primitivOHybrid = new PrimitivOHybrid(executorService, new File(fileName).toPath());
        writeHeader(mifHeader);
        writer = primitivOHybrid.beginPrimitivOBlocks(concurrency, DEFAULT_BLOCK_SIZE);
    }

    private void writeHeader(MifHeader mifHeader) {
        try (PrimitivO primitivO = primitivOHybrid.beginPrimitivO()) {
            primitivO.write(getBeginMagicBytes());
            primitivO.writeUTF(getVersionString(VERSION_INFO_MIF));
            primitivO.writeObject(mifHeader.getPipelineConfiguration());
            primitivO.writeInt(mifHeader.getNumberOfTargets());
            primitivO.writeInt(mifHeader.getCorrectedGroups().size());
            for (String correctedGroup : mifHeader.getCorrectedGroups())
                primitivO.writeObject(correctedGroup);
            primitivO.writeInt(mifHeader.getSortedGroups().size());
            for (String sortedGroup : mifHeader.getSortedGroups())
                primitivO.writeObject(sortedGroup);
            primitivO.writeInt(mifHeader.getGroupEdges().size());
            for (GroupEdge groupEdge : mifHeader.getGroupEdges()) {
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
            writer.close();
            try (PrimitivO primitivO = primitivOHybrid.beginPrimitivO()) {
                primitivO.writeObject(null);
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
