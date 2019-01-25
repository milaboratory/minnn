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

import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.minnn.correct.CorrectionStats;
import com.milaboratory.minnn.stat.MutationProbability;
import com.milaboratory.minnn.stat.SimpleMutationProbability;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.minnn.cli.CliUtils.floatFormat;
import static com.milaboratory.minnn.correct.CorrectionAlgorithms.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class CorrectBarcodesIO {
    private final PipelineConfiguration pipelineConfiguration;
    private final String inputFileName;
    private final String outputFileName;
    private final int mismatches;
    private final int indels;
    private final int totalErrors;
    private final float threshold;
    private final List<String> groupNames;
    private final int maxClusterDepth;
    private final MutationProbability mutationProbability;
    private final int maxUniqueBarcodes;
    private final String excludedBarcodesOutputFileName;
    private final long inputReadsLimit;
    private final boolean suppressWarnings;
    private LinkedHashSet<String> primaryGroups;

    public CorrectBarcodesIO(PipelineConfiguration pipelineConfiguration, String inputFileName, String outputFileName,
                             int mismatches, int indels, int totalErrors, float threshold, List<String> groupNames,
                             List<String> primaryGroupNames, int maxClusterDepth, float singleSubstitutionProbability,
                             float singleIndelProbability, int maxUniqueBarcodes,
                             String excludedBarcodesOutputFileName, long inputReadsLimit, boolean suppressWarnings) {
        this.pipelineConfiguration = pipelineConfiguration;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.mismatches = mismatches;
        this.indels = indels;
        this.totalErrors = totalErrors;
        this.threshold = threshold;
        this.groupNames = groupNames;
        this.primaryGroups = (primaryGroupNames == null) ? new LinkedHashSet<>()
                : new LinkedHashSet<>(primaryGroupNames);
        this.maxClusterDepth = maxClusterDepth;
        this.mutationProbability = new SimpleMutationProbability(singleSubstitutionProbability,
                singleIndelProbability);
        this.maxUniqueBarcodes = maxUniqueBarcodes;
        this.excludedBarcodesOutputFileName = excludedBarcodesOutputFileName;
        this.inputReadsLimit = inputReadsLimit;
        this.suppressWarnings = suppressWarnings;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        CorrectionStats stats;
        try (MifReader pass1Reader = new MifReader(inputFileName);
             MifReader pass2Reader = new MifReader(inputFileName);
             MifWriter writer = Objects.requireNonNull(createWriter(pass1Reader.getHeader(), false));
             MifWriter excludedBarcodesWriter = createWriter(pass1Reader.getHeader(), true)) {
            if (inputReadsLimit > 0) {
                pass1Reader.setParsedReadsLimit(inputReadsLimit);
                pass2Reader.setParsedReadsLimit(inputReadsLimit);
            }
            SmartProgressReporter.startProgressReport("Counting sequences", pass1Reader, System.err);
            Set<String> defaultGroups = IntStream.rangeClosed(1, pass1Reader.getNumberOfTargets())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());
            if (groupNames.stream().anyMatch(defaultGroups::contains))
                throw exitWithError("Default groups R1, R2, etc should not be specified for correction!");
            LinkedHashSet<String> keyGroups = new LinkedHashSet<>(groupNames);
            if (!suppressWarnings && pass1Reader.isSorted() && (primaryGroups.size() == 0))
                System.err.println("WARNING: correcting sorted MIF file; output file will be unsorted!");
            if (!suppressWarnings && !pass1Reader.isSorted() && (primaryGroups.size() > 0))
                System.err.println("WARNING: correcting unsorted MIF file with primary groups; correction will be " +
                        "slower and more memory consuming!");
            List<String> correctedAgainGroups = keyGroups.stream().filter(gn -> pass1Reader.getCorrectedGroups()
                    .stream().anyMatch(gn::equals)).collect(Collectors.toList());
            if (!suppressWarnings && (correctedAgainGroups.size() != 0))
                System.err.println("WARNING: group(s) " + correctedAgainGroups + " already corrected and will be " +
                        "corrected again!");
            int numberOfTargets = pass1Reader.getNumberOfTargets();
            if (primaryGroups.size() == 0)
                stats = fullFileCorrect();
            else if (pass1Reader.isSorted())
                stats = sortedClustersCorrect();
            else
                stats = unsortedClustersCorrect();
            pass2Reader.close();
            writer.setOriginalNumberOfReads(pass2Reader.getOriginalNumberOfReads());
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + stats.totalReads + " reads");
        float percent = (stats.totalReads == 0) ? 0 : (float)stats.correctedReads / stats.totalReads * 100;
        System.err.println("Reads with corrected barcodes: " + stats.correctedReads
                + " (" + floatFormat.format(percent) + "%)");
        if (stats.excludedReads > 0)
            System.err.println("Reads excluded by too low barcode count: " + stats.excludedReads
                    + " (" + floatFormat.format((float)stats.excludedReads / stats.totalReads * 100) + "%)\n");
        else
            System.err.println();
    }

    private MifWriter createWriter(MifHeader inputHeader, boolean excludedBarcodes) throws IOException {
        LinkedHashSet<String> allCorrectedGroups = new LinkedHashSet<>(inputHeader.getCorrectedGroups());
        allCorrectedGroups.addAll(groupNames);
        MifHeader outputHeader = new MifHeader(pipelineConfiguration, inputHeader.getNumberOfTargets(),
                new ArrayList<>(allCorrectedGroups), false, inputHeader.getGroupEdges());
        if (excludedBarcodes)
            return (excludedBarcodesOutputFileName == null) ? null
                    : new MifWriter(excludedBarcodesOutputFileName, outputHeader);
        else
            return (outputFileName == null) ? new MifWriter(new SystemOutStream(), outputHeader)
                    : new MifWriter(outputFileName, outputHeader);
    }
}
