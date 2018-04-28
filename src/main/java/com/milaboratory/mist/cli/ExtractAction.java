package com.milaboratory.mist.cli;

import com.beust.jcommander.*;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;
import com.milaboratory.mist.io.MistDataFormat;
import com.milaboratory.mist.io.ReadProcessor;
import com.milaboratory.mist.parser.Parser;
import com.milaboratory.mist.parser.ParserException;
import com.milaboratory.mist.pattern.BasePatternAligner;
import com.milaboratory.mist.pattern.Pattern;
import com.milaboratory.mist.pattern.PatternAligner;

import java.util.*;

import static com.milaboratory.mist.cli.CliUtils.*;
import static com.milaboratory.mist.cli.Defaults.*;
import static com.milaboratory.mist.io.MistDataFormatNames.parameterNames;
import static com.milaboratory.mist.parser.ParserFormat.*;
import static com.milaboratory.mist.util.SystemUtils.exitWithError;

public final class ExtractAction implements Action {
    private final ExtractActionParameters params = new ExtractActionParameters();

    @Override
    public void go(ActionHelper helper) {
        PatternAndTargetAlignmentScoring scoring = new PatternAndTargetAlignmentScoring(params.matchScore,
                params.mismatchScore, params.gapScore, params.uppercaseMismatchScore,
                (byte)(params.goodQuality), (byte)(params.badQuality), params.maxQualityPenalty);
        PatternAligner patternAligner = new BasePatternAligner(scoring, params.penaltyThreshold,
                params.singleOverlapPenalty, params.bitapMaxErrors, params.maxOverlap);
        Parser patternParser = new Parser(patternAligner);
        Pattern pattern;
        try {
            pattern = params.simplifiedSyntax ? patternParser.parseQuery(params.query, SIMPLIFIED)
                    : patternParser.parseQuery(params.query);
        } catch (ParserException e) {
            System.err.println("Error while parsing the pattern!");
            throw exitWithError(e.getMessage());
        }
        MistDataFormat inputFormat = parameterNames.get(params.inputFormat);
        ReadProcessor readProcessor = new ReadProcessor(params.inputFileNames, params.outputFileName, pattern,
                params.oriented, params.fairSorting, params.threads, inputFormat, params.testIOSpeed);
        readProcessor.processReadsParallel();
    }

    @Override
    public String command() {
        return "extract";
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription =
            "Read target nucleotide sequence and find groups and patterns as specified in query.")
    private static final class ExtractActionParameters extends ActionParameters {
        @Parameter(description = "--pattern <pattern_query>")
        private String description;

        @Parameter(description = "Query, pattern specified in MiST format.",
                names = {"--pattern"}, order = 0, required = true)
        String query = null;

        @Parameter(description = "Input files. Single file means that there is 1 read or multi-read file; " +
                "multiple files mean that there is 1 file for each read. " +
                "If not specified, stdin will be used.",
                names = {"--input"}, order = 1, variableArity = true)
        List<String> inputFileNames = new ArrayList<>();

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 2)
        String outputFileName = null;

        @Parameter(description = "Input data format. \"fastq\" (default) or \"mif\".",
                names = {"--input-format"})
        String inputFormat = DEFAULT_INPUT_FORMAT;

        @Parameter(description = "By default, if there are 2 or more reads, 2 last reads are checked in direct " +
                "and reverse order. With this flag, only in direct order.",
                names = {"--oriented"})
        boolean oriented = false;

        @Parameter(description = "Score for perfectly matched nucleotide.",
                names = {"--match-score"})
        int matchScore = DEFAULT_MATCH_SCORE;

        @Parameter(description = "Score for mismatched nucleotide.",
                names = {"--mismatch-score"})
        int mismatchScore = DEFAULT_MISMATCH_SCORE;

        @Parameter(description = "Score for mismatched uppercase nucleotide.",
                names = {"--uppercase-mismatch-score"})
        int uppercaseMismatchScore = DEFAULT_UPPERCASE_MISMATCH_SCORE;

        @Parameter(description = "Score for gap or insertion.",
                names = {"--gap-score"})
        int gapScore = DEFAULT_GAP_SCORE;

        @Parameter(description = "This or better quality value will be considered good quality, " +
                "without score penalties.",
                names = {"--good-quality-value"})
        int goodQuality = DEFAULT_GOOD_QUALITY;

        @Parameter(description = "This or worse quality value will be considered bad quality, " +
                "with maximal score penalty.",
                names = {"--bad-quality-value"})
        int badQuality = DEFAULT_BAD_QUALITY;

        @Parameter(description = "Maximal score penalty for bad quality nucleotide in target.",
                names = {"--max-quality-penalty"})
        int maxQualityPenalty = DEFAULT_MAX_QUALITY_PENALTY;

        @Parameter(description = "Score threshold, matches with score lower than this will not go to output.",
                names = {"--penalty-threshold"})
        long penaltyThreshold = DEFAULT_PENALTY_THRESHOLD;

        @Parameter(description = "Score penalty for 1 nucleotide overlap between neighbor patterns. Negative value.",
                names = {"--single-overlap-penalty"})
        long singleOverlapPenalty = DEFAULT_SINGLE_OVERLAP_PENALTY;

        @Parameter(description = "Maximum allowed number of errors for bitap matcher.",
                names = {"--bitap-max-errors"})
        int bitapMaxErrors = DEFAULT_BITAP_MAX_ERRORS;

        @Parameter(description = "Max allowed overlap for 2 intersecting operands in +, & and pattern sequences.",
                names = {"--max-overlap"})
        int maxOverlap = DEFAULT_MAX_OVERLAP;

        @Parameter(description = "Use fair sorting and fair best match by score for all patterns.",
                names = {"--fair-sorting"})
        boolean fairSorting = false;

        @Parameter(description = "Number of threads for parsing reads.",
                names = {"--threads"})
        int threads = DEFAULT_THREADS;

        @Parameter(description = "Use \"simplified\" parser syntax with class names and their arguments in parentheses",
                names = {"--devel-parser-syntax"}, hidden = true)
        boolean simplifiedSyntax = false;

        @Parameter(description = "Copy input files to output without processing; used for debug purpose only.",
                names = {"--test-io-speed"}, hidden = true)
        boolean testIOSpeed = false;

        @Override
        public void validate() {
            if (parameterNames.get(inputFormat) == null)
                throw new ParameterException("Unknown input format: " + inputFormat);
            validateQuality(goodQuality);
            validateQuality(badQuality);
        }
    }
}
