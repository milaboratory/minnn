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

import cc.redberry.pipe.OutputPortCloseable;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.minnn.cli.PipelineConfigurationReaderMiNNN;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.minnn.util.DebugUtils.*;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.primitivio.blocks.PrimitivIBlocks;
import com.milaboratory.primitivio.blocks.PrimitivIHybrid;
import com.milaboratory.util.CanReportProgress;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.milaboratory.minnn.cli.Magic.*;
import static com.milaboratory.minnn.io.IODefaults.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static java.lang.Double.NaN;

public final class MifReader extends PipelineConfigurationReaderMiNNN
        implements OutputPortCloseable<ParsedRead>, CanReportProgress {
    private final PrimitivIHybrid primitivIHybrid;
    private final PrimitivIBlocks<ParsedRead>.Reader reader;
    private final long size;
    private long parsedReadsLimit = -1;
    private long parsedReadsTaken = 0;
    private boolean finished = false;
    private boolean closed = false;
    private PipelineConfiguration pipelineConfiguration;
    private int numberOfTargets;
    private ArrayList<String> correctedGroups = new ArrayList<>();
    private ArrayList<String> sortedGroups = new ArrayList<>();
    private ArrayList<GroupEdge> groupEdges = new ArrayList<>();
    private long firstReadSerializedLength = -1;
    private long originalNumberOfReads = -1;
    private String mifVersionInfo;

    public MifReader(InputStream stream) {
        throw new NotImplementedException();
//        input = new PrimitivI(this.countingInputStream = new CountingInputStream(stream));
//        readHeader();
//        size = -1;
    }

    public MifReader(String fileName) throws IOException {
        this(fileName, DEFAULT_CONCURRENCY, DEFAULT_READ_AHEAD_BLOCKS);
    }

    public MifReader(String fileName, int concurrency, int readAheadBlocks) throws IOException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        File file = new File(fileName);
        size = file.length();
        primitivIHybrid = new PrimitivIHybrid(executorService, file.toPath());
        readHeader();
        reader = primitivIHybrid.beginPrimitivIBlocks(ParsedRead.class, concurrency, readAheadBlocks);
    }

    private void readHeader() {
        PrimitivI primitivI = primitivIHybrid.beginPrimitivI();
        byte[] magicBytes = new byte[BEGIN_MAGIC_LENGTH];
        try {
            primitivI.readFully(magicBytes);
        } catch (RuntimeException e) {
            throw exitWithError("Unsupported file format; error while reading file header: " + e.getMessage());
        }
        String magicString = new String(magicBytes);
        if (!magicString.equals(BEGIN_MAGIC))
            throw exitWithError("Unsupported file format; .mif file of version " + magicString +
                    " while you are running MiNNN " + BEGIN_MAGIC);
        mifVersionInfo = primitivI.readUTF();
        pipelineConfiguration = primitivI.readObject(PipelineConfiguration.class);
        numberOfTargets = primitivI.readInt();
        int correctedGroupsNum = primitivI.readInt();
        for (int i = 0; i < correctedGroupsNum; i++)
            correctedGroups.add(primitivI.readObject(String.class));
        int sortedGroupsNum = primitivI.readInt();
        for (int i = 0; i < sortedGroupsNum; i++)
            sortedGroups.add(primitivI.readObject(String.class));
        int groupEdgesNum = primitivI.readInt();
        for (int i = 0; i < groupEdgesNum; i++) {
            GroupEdge groupEdge = primitivI.readObject(GroupEdge.class);
            primitivI.putKnownObject(groupEdge);
            groupEdges.add(groupEdge);
        }
        primitivIHybrid.endPrimitivI();
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            primitivIHybrid.endPrimitivIBlocks();
            PrimitivI primitivI = primitivIHybrid.beginPrimitivI();
            originalNumberOfReads = finished ? PrimitivI.readLong() : parsedReadsTaken;
            primitivIHybrid.endPrimitivI();
            try {
                primitivIHybrid.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            finished = true;
            closed = true;
        }
    }

    @Override
    public double getProgress() {
        if (parsedReadsLimit == -1) {
            if (size < 1)
                return NaN;
            else
                return (double)(parsedReadsTaken) * firstReadSerializedLength / size;
        } else
            return (double)parsedReadsTaken / parsedReadsLimit;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public synchronized ParsedRead take() {
        if (finished)
            return null;
        ParsedRead parsedRead = reader.take();
        if (parsedRead == null)
            finished = true;
        else {
            if (firstReadSerializedLength == -1)
                calculateFirstReadLength(parsedRead);
            parsedReadsTaken++;
            if ((parsedReadsLimit != -1) && (parsedReadsTaken > parsedReadsLimit))
                throw new IllegalStateException("Specified parsed reads limit (" + parsedReadsLimit + ") was "
                        + "exceeded in MifReader!");
        }
        return parsedRead;
    }

    @Override
    public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfiguration;
    }

    public int getNumberOfTargets() {
        return numberOfTargets;
    }

    public ArrayList<String> getCorrectedGroups() {
        return correctedGroups;
    }

    public ArrayList<String> getSortedGroups() {
        return sortedGroups;
    }

    public ArrayList<GroupEdge> getGroupEdges() {
        return new ArrayList<>(groupEdges);
    }

    public MifHeader getHeader() {
        return new MifHeader(pipelineConfiguration, numberOfTargets, correctedGroups, sortedGroups, groupEdges);
    }

    public String getMifVersionInfo() {
        return mifVersionInfo;
    }

    private void calculateFirstReadLength(ParsedRead parsedRead) {
        ByteArrayOutputStream counterStream = new ByteArrayOutputStream();
        PrimitivO outStream = new PrimitivO(counterStream);
        outStream.writeObject(parsedRead);
        outStream.close();
        firstReadSerializedLength = counterStream.toByteArray().length;
    }

    public long getOriginalNumberOfReads() {
        if (!closed)
            throw new IllegalStateException("getOriginalNumberOfReads() used when reader is not closed!");
        return originalNumberOfReads;
    }

    public long getEstimatedNumberOfReads() {
        if ((size == -1) || (firstReadSerializedLength == -1))
            return -1;
        else {
            long estimatedNumberOfReads = size / Math.max(1, firstReadSerializedLength);
            return (parsedReadsLimit == -1) ? estimatedNumberOfReads
                    : Math.min(parsedReadsLimit, estimatedNumberOfReads);
        }
    }

    /**
     * If number of parsed reads is limited by command line parameter, we can use it for better progress reporting.
     *
     * @param limit maximum number of parsed reads that we can take from input file
     */
    public void setParsedReadsLimit(long limit) {
        parsedReadsLimit = limit;
    }
}
