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
package com.milaboratory.minnn.cli;

import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.minnn.io.CorrectBarcodesIO;

import java.util.*;

import static com.milaboratory.minnn.cli.Defaults.*;

public final class CorrectAction implements Action {
    public static final String commandName = "correct";
    private final CorrectActionParameters params = new CorrectActionParameters();

    @Override
    public void go(ActionHelper helper) {
        CorrectBarcodesIO correctBarcodesIO = new CorrectBarcodesIO(params.inputFileName, params.outputFileName,
                params.mismatches, params.indels, params.totalErrors, params.threshold, params.groupNames,
                params.maxClusterDepth, params.singleSubstitutionProbability, params.singleIndelProbability,
                params.inputReadsLimit, params.suppressWarnings);
        correctBarcodesIO.go();
    }

    @Override
    public String command() {
        return commandName;
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription =
            "Correct errors in barcodes, and replace all barcodes with corrected variants.")
    private static final class CorrectActionParameters extends ActionParameters {
        @Parameter(description = "--input <input_mif_file>", order = 0)
        private String description;

        @Parameter(description = "Group names for correction.",
                names = {"--groups"}, order = 1, required = true, variableArity = true)
        List<String> groupNames = null;

        @Parameter(description = "Input file in \"mif\" format. This argument is required; stdin is not supported.",
                names = {"--input"}, order = 2, required = true)
        String inputFileName = null;

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 3)
        String outputFileName = null;

        @Parameter(description = "Maximum number of mismatches between barcodes for which they are considered " +
                "identical.", names = {"--max-mismatches"}, order = 4)
        int mismatches = DEFAULT_CORRECT_MAX_MISMATCHES;

        @Parameter(description = "Maximum number of insertions or deletions between barcodes for which they are " +
                "considered identical.", names = {"--max-indels"}, order = 5)
        int indels = DEFAULT_CORRECT_MAX_INDELS;

        @Parameter(description = "Maximum Levenshtein distance between barcodes for which they are considered " +
                "identical.", names = {"--max-total-errors"}, order = 6)
        int totalErrors = DEFAULT_CORRECT_MAX_TOTAL_ERRORS;

        @Parameter(description = "Threshold for UMI clustering: if smaller UMI count divided to larger UMI count " +
                "is below this threshold, UMI will be merged to the cluster.",
                names = {"--cluster-threshold"}, order = 7)
        float threshold = DEFAULT_CORRECT_CLUSTER_THRESHOLD;

        @Parameter(description = "Maximum cluster depth for algorithm of similar barcodes clustering.",
                names = {"--max-cluster-depth"}, order = 8)
        int maxClusterDepth = DEFAULT_CORRECT_MAX_CLUSTER_DEPTH;

        @Parameter(description = "Single substitution probability for clustering algorithm.",
                names = {"--single-substitution-probability"}, order = 9)
        float singleSubstitutionProbability = DEFAULT_CORRECT_SINGLE_SUBSTITUTION_PROBABILITY;

        @Parameter(description = "Single insertion/deletion probability for clustering algorithm.",
                names = {"--single-indel-probability"}, order = 10)
        float singleIndelProbability = DEFAULT_CORRECT_SINGLE_INDEL_PROBABILITY;

        @Parameter(description = "Number of reads to take; 0 value means to take the entire input file.",
                names = {"-n", "--number-of-reads"}, order = 11)
        long inputReadsLimit = 0;

        @Parameter(description = "Don't display any warnings.",
                names = {"--suppress-warnings"}, order = 12)
        boolean suppressWarnings = false;
    }
}
