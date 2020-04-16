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
import com.milaboratory.primitivio.PrimitivI;
import com.milaboratory.primitivio.blocks.PrimitivIBlocks;
import com.milaboratory.primitivio.blocks.PrimitivIBlocksStats;
import com.milaboratory.primitivio.blocks.PrimitivIHybrid;
import com.milaboratory.util.CanReportProgress;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.milaboratory.minnn.cli.Magic.*;
import static com.milaboratory.minnn.io.IODefaults.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static java.lang.Double.NaN;

public final class MifReader extends PipelineConfigurationReaderMiNNN
        implements OutputPortCloseable<ParsedRead>, CanReportProgress {
    private final String fileName;
    private final PrimitivIHybrid primitivIHybrid;
    private final PrimitivIBlocks<ParsedRead>.Reader reader;
    private long parsedReadsLimit = -1;
    private long parsedReadsTaken = 0;
    private boolean finished = false;
    private boolean closed = false;
    private PipelineConfiguration pipelineConfiguration;
    private int numberOfTargets;
    private final ArrayList<String> correctedGroups = new ArrayList<>();
    private final ArrayList<String> sortedGroups = new ArrayList<>();
    private final ArrayList<GroupEdge> groupEdges = new ArrayList<>();
    private long numberOfReads;
    private long originalNumberOfReads;
    private String mifVersionInfo;

    public MifReader(String fileName) throws IOException {
        this(fileName, Executors.newCachedThreadPool(), DEFAULT_CONCURRENCY);
    }

    public MifReader(String fileName, ExecutorService executorService, int concurrency) throws IOException {
        this.fileName = fileName;
        File file = new File(fileName);
        primitivIHybrid = new PrimitivIHybrid(executorService, file.toPath(), concurrency);
        readMetaInfo();
        reader = primitivIHybrid.beginPrimitivIBlocks(ParsedRead.class, DEFAULT_READ_AHEAD_BLOCKS);
    }

    private void readMetaInfo() {
        try (PrimitivI primitivI = primitivIHybrid.beginPrimitivI()) {
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
        }
        try (PrimitivI primitivI = primitivIHybrid.beginRandomAccessPrimitivI(-FOOTER_LENGTH)) {
            numberOfReads = primitivI.readLong();
            originalNumberOfReads = primitivI.readLong();
            if (!Arrays.equals(primitivI.readBytes(END_MAGIC_LENGTH), getEndMagicBytes()))
                throw exitWithError("Error in MIF file " + fileName + ": END_MAGIC mismatch.");
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            reader.close();
            if (finished) {
                try (PrimitivI primitivI = primitivIHybrid.beginPrimitivI()) {
                    originalNumberOfReads = primitivI.readLong();
                }
                try {
                    primitivIHybrid.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                originalNumberOfReads = parsedReadsTaken;
                finished = true;
            }
            closed = true;
        }
    }

    @Override
    public double getProgress() {
        return (getNumberOfReads() == 0) ? NaN : (double)parsedReadsTaken / getNumberOfReads();
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

    public String getMifVersionInfo() {
        return mifVersionInfo;
    }

    public long getNumberOfReads() {
        return (parsedReadsLimit == -1) ? numberOfReads : Math.min(parsedReadsLimit, numberOfReads);
    }

    public long getNumberOfReadsInFile() {
        return numberOfReads;
    }

    public long getOriginalNumberOfReads() {
        return originalNumberOfReads;
    }

    public MifMetaInfo getMetaInfo() {
        return new MifMetaInfo(pipelineConfiguration, numberOfTargets, correctedGroups, sortedGroups, groupEdges,
                numberOfReads, originalNumberOfReads);
    }

    public PrimitivIBlocksStats getStats() {
        return reader.getParent().getStats();
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
