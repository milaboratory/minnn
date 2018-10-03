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

import cc.redberry.pipe.OutputPortCloseable;
import com.milaboratory.core.io.CompressionType;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.PrimitivO;
import com.milaboratory.util.CanReportProgress;
import com.milaboratory.util.CountingInputStream;

import java.io.*;
import java.util.ArrayList;

import static java.lang.Double.NaN;

public final class MifReader implements OutputPortCloseable<ParsedRead>, CanReportProgress {
    private static final int DEFAULT_BUFFER_SIZE = 1 << 20;
    private final PrimitivI input;
    private final CountingInputStream countingInputStream;
    private final long size;
    private long parsedReadsLimit = -1;
    private long parsedReadsTaken = 0;
    private boolean finished = false;
    private boolean closed = false;
    private int numberOfTargets;
    private ArrayList<String> correctedGroups = new ArrayList<>();
    private boolean sortedMif;
    private ArrayList<GroupEdge> groupEdges = new ArrayList<>();
    private long firstReadSerializedLength = -1;
    private long originalNumberOfReads = -1;

    public MifReader(InputStream stream) {
        input = new PrimitivI(this.countingInputStream = new CountingInputStream(stream));
        readHeader();
        size = -1;
    }

    public MifReader(String fileName) throws IOException {
        File file = new File(fileName);
        CompressionType ct = CompressionType.detectCompressionType(file);
        this.countingInputStream = new CountingInputStream(new FileInputStream(file));
        if (ct == CompressionType.None) {
            input = new PrimitivI(new BufferedInputStream(this.countingInputStream, DEFAULT_BUFFER_SIZE));
            size = file.length();
        } else {
            input = new PrimitivI(ct.createInputStream(this.countingInputStream, DEFAULT_BUFFER_SIZE));
            size = -1;
        }
        readHeader();
    }

    private void readHeader() {
        numberOfTargets = input.readInt();
        int correctedGroupsNum = input.readInt();
        for (int i = 0; i < correctedGroupsNum; i++)
            correctedGroups.add(input.readObject(String.class));
        sortedMif = input.readBoolean();
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
            originalNumberOfReads = input.readLong();
            input.close();
            finished = true;
            closed = true;
        }
    }

    @Override
    public double getProgress() {
        if (parsedReadsLimit == -1) {
            if (size == -1)
                return NaN;
            else
                return (double)(countingInputStream.getBytesRead()) / size;
        } else {
            double estimationByTakenReads = (double)parsedReadsTaken / parsedReadsLimit;
            if (size == -1)
                return estimationByTakenReads;
            else
                return Math.max(estimationByTakenReads, (double)(countingInputStream.getBytesRead()) / size);
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
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
            if ((parsedReadsLimit != -1) && (parsedReadsTaken > parsedReadsLimit))
                throw new IllegalStateException("Specified parsed reads limit (" + parsedReadsLimit + ") was "
                        + "exceeded in MifReader!");
        }
        return parsedRead;
    }

    public int getNumberOfTargets() {
        return numberOfTargets;
    }

    public ArrayList<String> getCorrectedGroups() {
        return correctedGroups;
    }

    public boolean isSorted() {
        return sortedMif;
    }

    public ArrayList<GroupEdge> getGroupEdges() {
        return new ArrayList<>(groupEdges);
    }

    public MifHeader getHeader() {
        return new MifHeader(numberOfTargets, correctedGroups, sortedMif, groupEdges);
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
