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

import com.milaboratory.cli.*;
import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;
import com.milaboratory.minnn.io.MinnnDataFormat;
import com.milaboratory.minnn.io.ReadProcessor;
import com.milaboratory.minnn.parser.Parser;
import com.milaboratory.minnn.parser.ParserException;
import com.milaboratory.minnn.pattern.BasePatternAligner;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.minnn.pattern.Pattern;
import com.milaboratory.minnn.pattern.PatternAligner;
import com.milaboratory.minnn.util.MinnnVersionInfo;
import picocli.CommandLine.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.ExtractAction.EXTRACT_ACTION_NAME;
import static com.milaboratory.minnn.cli.PipelineConfigurationReaderMiNNN.pipelineConfigurationReaderInstance;
import static com.milaboratory.minnn.io.MifInfoExtractor.mifInfoExtractor;
import static com.milaboratory.minnn.io.MinnnDataFormatNames.parameterNames;
import static com.milaboratory.minnn.parser.ParserFormat.*;
import static com.milaboratory.minnn.util.SystemUtils.exitWithError;

@Command(name = EXTRACT_ACTION_NAME,
        sortOptions = false,
        separator = " ",
        description = "Read target nucleotide sequence and find groups and patterns as specified in query.")
public final class ExtractAction extends ACommandWithSmartOverwrite implements MiNNNCommand {
    public static final String EXTRACT_ACTION_NAME = "extract";

    public ExtractAction() {
        super(APP_NAME, mifInfoExtractor, pipelineConfigurationReaderInstance);
    }

    @Override
    public void run1() {
        PatternAndTargetAlignmentScoring scoring = new PatternAndTargetAlignmentScoring(matchScore, mismatchScore,
                gapScore, uppercaseMismatchScore, (byte)goodQuality, (byte)badQuality, maxQualityPenalty);
        PatternAligner patternAligner = new BasePatternAligner(scoring, scoreThreshold, singleOverlapPenalty,
                bitapMaxErrors, maxOverlap);
        Parser patternParser = new Parser(patternAligner);
        Pattern pattern;
        try {
            pattern = simplifiedSyntax ? patternParser.parseQuery(query, SIMPLIFIED)
                    : patternParser.parseQuery(query);
        } catch (ParserException e) {
            System.err.println("Error while parsing the pattern!");
            throw exitWithError(e.getMessage());
        }
        HashSet<String> patternGroups = pattern.getGroupEdges().stream().map(GroupEdge::getGroupName)
                .collect(Collectors.toCollection(HashSet::new));
        DescriptionGroups descriptionGroups = new DescriptionGroups(descriptionGroupsMap);
        patternGroups.retainAll(descriptionGroups.getGroupNames());
        if (patternGroups.size() > 0)
            throw exitWithError("Error: groups " + patternGroups + " are both in pattern and in description groups!");
        MinnnDataFormat inputFormat = parameterNames.get(inputFormatName);
        ReadProcessor readProcessor = new ReadProcessor(getFullPipelineConfiguration(), getInputFiles(),
                outputFileName, notMatchedOutputFileName, pattern, oriented, fairSorting, inputReadsLimit, threads,
                inputFormat, descriptionGroups);
        readProcessor.processReadsParallel();
    }

    @Override
    public void validateInfo(String inputFile) {
        MiNNNCommand.super.validateInfo(inputFile);
    }

    @Override
    public void validate() {
        super.validate();
        if (parameterNames.get(inputFormatName) == null)
            throwValidationException("Unknown input format: " + inputFormatName);
        validateQuality(goodQuality);
        validateQuality(badQuality);
    }

    @Override
    protected List<String> getInputFiles() {
        return inputFileNames;
    }

    @Override
    protected List<String> getOutputFiles() {
        List<String> outputFileNames = new ArrayList<>();
        if (outputFileName != null)
            outputFileNames.add(outputFileName);
        if (notMatchedOutputFileName != null)
            outputFileNames.add(notMatchedOutputFileName);
        return outputFileNames;
    }

    @Override
    public ActionConfiguration getConfiguration() {
        return new ExtractActionConfiguration(new ExtractActionConfiguration.ExtractActionParameters(query,
                inputFormatName, oriented, matchScore, mismatchScore, uppercaseMismatchScore, gapScore, scoreThreshold,
                goodQuality, badQuality, maxQualityPenalty, singleOverlapPenalty, maxOverlap, bitapMaxErrors,
                fairSorting, inputReadsLimit, descriptionGroupsMap, simplifiedSyntax));
    }

    @Override
    public PipelineConfiguration getFullPipelineConfiguration() {
        return PipelineConfiguration.mkInitial(getInputFiles(), getConfiguration(), MinnnVersionInfo.get());
    }

    @Option(description = "Query, pattern specified in MiNNN format.",
            names = "--pattern",
            required = true)
    private String query = null;

    @Option(description = "Input files. Single file means that there is 1 read or multi-read file; " +
            "multiple files mean that there is 1 file for each read. If not specified, stdin will be used.",
            names = "--input",
            arity = "1..*")
    private List<String> inputFileNames = new ArrayList<>();

    @Option(description = OUT_FILE_OR_STDOUT,
            names = "--output")
    private String outputFileName = null;

    @Option(description = "Output file for not matched reads in \"mif\" format. If not specified, not " +
            "matched reads will not be written anywhere.",
            names = "--not-matched-output")
    private String notMatchedOutputFileName = null;

    @Option(description = "Input data format. \"fastq\" (default) or \"mif\".",
            names = "--input-format")
    private String inputFormatName = DEFAULT_INPUT_FORMAT;

    @Option(description = "By default, if there are 2 or more reads, 2 last reads are checked in direct " +
            "and reverse order. With this flag, only in direct order.",
            names = "--oriented")
    private boolean oriented = false;

    @Option(description = "Score for perfectly matched nucleotide.",
            names = "--match-score")
    private int matchScore = DEFAULT_MATCH_SCORE;

    @Option(description = "Score for mismatched nucleotide.",
            names = "--mismatch-score")
    private int mismatchScore = DEFAULT_MISMATCH_SCORE;

    @Option(description = "Score for mismatched uppercase nucleotide.",
            names = "--uppercase-mismatch-score")
    private int uppercaseMismatchScore = DEFAULT_UPPERCASE_MISMATCH_SCORE;

    @Option(description = "Score for gap or insertion.",
            names = "--gap-score")
    private int gapScore = DEFAULT_GAP_SCORE;

    @Option(description = "Score threshold, matches with score lower than this will not go to output.",
            names = "--score-threshold")
    private long scoreThreshold = DEFAULT_SCORE_THRESHOLD;

    @Option(description = "This or better quality value will be considered good quality, " +
            "without score penalties.",
            names = "--good-quality-value")
    private int goodQuality = DEFAULT_GOOD_QUALITY;

    @Option(description = "This or worse quality value will be considered bad quality, " +
            "with maximal score penalty.",
            names = "--bad-quality-value")
    private int badQuality = DEFAULT_BAD_QUALITY;

    @Option(description = "Maximal score penalty for bad quality nucleotide in target.",
            names = "--max-quality-penalty")
    private int maxQualityPenalty = DEFAULT_MAX_QUALITY_PENALTY;

    @Option(description = "Score penalty for 1 nucleotide overlap between neighbor patterns. Negative value.",
            names = "--single-overlap-penalty")
    private long singleOverlapPenalty = DEFAULT_SINGLE_OVERLAP_PENALTY;

    @Option(description = "Max allowed overlap for 2 intersecting operands in +, & and pattern sequences.",
            names = "--max-overlap")
    private int maxOverlap = DEFAULT_MAX_OVERLAP;

    @Option(description = "Maximum allowed number of errors for bitap matcher.",
            names = "--bitap-max-errors")
    private int bitapMaxErrors = DEFAULT_BITAP_MAX_ERRORS;

    @Option(description = FAIR_SORTING,
            names = "--fair-sorting")
    private boolean fairSorting = false;

    @Option(description = "Number of reads to take; 0 value means to take the entire input file.",
            names = {"-n", "--number-of-reads"})
    private long inputReadsLimit = 0;

    @Option(description = "Number of threads for parsing reads.",
            names = "--threads")
    private int threads = DEFAULT_THREADS;

    @Option(description = "Description group names and regular expressions to parse expected " +
            "nucleotide sequences for that groups from read description. Example: --description-group-CELLID1=" +
            "'ATTA.{2-5}GACA' --description-group-CELLID2='.{11}$'",
            names = "--description-group-")
    private LinkedHashMap<String, String> descriptionGroupsMap = new LinkedHashMap<>();

    @Option(description = "Use \"simplified\" parser syntax with class names and their arguments in " +
            "parentheses",
            names = "--devel-parser-syntax",
            hidden = true)
    private boolean simplifiedSyntax = false;
}
