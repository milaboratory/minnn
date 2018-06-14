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
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.mist.pattern.Pattern;
import com.milaboratory.mist.pattern.PatternAligner;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.mist.cli.CliUtils.*;
import static com.milaboratory.mist.cli.Defaults.*;
import static com.milaboratory.mist.io.MistDataFormatNames.parameterNames;
import static com.milaboratory.mist.parser.ParserFormat.*;
import static com.milaboratory.mist.util.SystemUtils.exitWithError;

public final class ExtractAction implements Action {
    public static final String commandName = "extract";
    private final ExtractActionParameters params = new ExtractActionParameters();

    @Override
    public void go(ActionHelper helper) {
        PatternAndTargetAlignmentScoring scoring = new PatternAndTargetAlignmentScoring(params.matchScore,
                params.mismatchScore, params.gapScore, params.uppercaseMismatchScore,
                (byte)(params.goodQuality), (byte)(params.badQuality), params.maxQualityPenalty);
        PatternAligner patternAligner = new BasePatternAligner(scoring, params.scoreThreshold,
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
        HashSet<String> patternGroups = pattern.getGroupEdges().stream().map(GroupEdge::getGroupName)
                .collect(Collectors.toCollection(HashSet::new));
        DescriptionGroups descriptionGroups = new DescriptionGroups(params.descriptionGroups);
        patternGroups.retainAll(descriptionGroups.getGroupNames());
        if (patternGroups.size() > 0)
            throw exitWithError("Error: groups " + patternGroups + " are both in pattern and in description groups!");
        MistDataFormat inputFormat = parameterNames.get(params.inputFormat);
        ReadProcessor readProcessor = new ReadProcessor(params.inputFileNames, params.outputFileName, pattern,
                params.oriented, params.fairSorting, params.inputReadsLimit, params.threads, inputFormat,
                descriptionGroups);
        readProcessor.processReadsParallel();
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
            "Read target nucleotide sequence and find groups and patterns as specified in query.")
    private static final class ExtractActionParameters extends ActionParameters {
        @Parameter(description = "--pattern <pattern_query>", order = 0)
        private String description;

        @Parameter(description = "Query, pattern specified in MiST format.",
                names = {"--pattern"}, order = 1, required = true)
        String query = null;

        @Parameter(description = "Input files. Single file means that there is 1 read or multi-read file; " +
                "multiple files mean that there is 1 file for each read. " +
                "If not specified, stdin will be used.",
                names = {"--input"}, order = 2, variableArity = true)
        List<String> inputFileNames = new ArrayList<>();

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 3)
        String outputFileName = null;

        @Parameter(description = "Input data format. \"fastq\" (default) or \"mif\".",
                names = {"--input-format"}, order = 4)
        String inputFormat = DEFAULT_INPUT_FORMAT;

        @Parameter(description = "By default, if there are 2 or more reads, 2 last reads are checked in direct " +
                "and reverse order. With this flag, only in direct order.",
                names = {"--oriented"}, order = 5)
        boolean oriented = false;

        @Parameter(description = "Score for perfectly matched nucleotide.",
                names = {"--match-score"}, order = 6)
        int matchScore = DEFAULT_MATCH_SCORE;

        @Parameter(description = "Score for mismatched nucleotide.",
                names = {"--mismatch-score"}, order = 7)
        int mismatchScore = DEFAULT_MISMATCH_SCORE;

        @Parameter(description = "Score for mismatched uppercase nucleotide.",
                names = {"--uppercase-mismatch-score"}, order = 8)
        int uppercaseMismatchScore = DEFAULT_UPPERCASE_MISMATCH_SCORE;

        @Parameter(description = "Score for gap or insertion.",
                names = {"--gap-score"}, order = 9)
        int gapScore = DEFAULT_GAP_SCORE;

        @Parameter(description = "Score threshold, matches with score lower than this will not go to output.",
                names = {"--score-threshold"}, order = 10)
        long scoreThreshold = DEFAULT_SCORE_THRESHOLD;

        @Parameter(description = "This or better quality value will be considered good quality, " +
                "without score penalties.",
                names = {"--good-quality-value"}, order = 11)
        int goodQuality = DEFAULT_GOOD_QUALITY;

        @Parameter(description = "This or worse quality value will be considered bad quality, " +
                "with maximal score penalty.",
                names = {"--bad-quality-value"}, order = 12)
        int badQuality = DEFAULT_BAD_QUALITY;

        @Parameter(description = "Maximal score penalty for bad quality nucleotide in target.",
                names = {"--max-quality-penalty"}, order = 13)
        int maxQualityPenalty = DEFAULT_MAX_QUALITY_PENALTY;

        @Parameter(description = "Score penalty for 1 nucleotide overlap between neighbor patterns. Negative value.",
                names = {"--single-overlap-penalty"}, order = 14)
        long singleOverlapPenalty = DEFAULT_SINGLE_OVERLAP_PENALTY;

        @Parameter(description = "Max allowed overlap for 2 intersecting operands in +, & and pattern sequences.",
                names = {"--max-overlap"}, order = 15)
        int maxOverlap = DEFAULT_MAX_OVERLAP;

        @Parameter(description = "Maximum allowed number of errors for bitap matcher.",
                names = {"--bitap-max-errors"}, order = 16)
        int bitapMaxErrors = DEFAULT_BITAP_MAX_ERRORS;

        @Parameter(description = "Use fair sorting and fair best match by score for all patterns.",
                names = {"--fair-sorting"}, order = 17)
        boolean fairSorting = false;

        @Parameter(description = "Number of reads to take; 0 value means to take the entire input file.",
                names = {"-n", "--number-of-reads"}, order = 18)
        long inputReadsLimit = 0;

        @Parameter(description = "Number of threads for parsing reads.",
                names = {"--threads"}, order = 19)
        int threads = DEFAULT_THREADS;

        @Parameter(description = "Use \"simplified\" parser syntax with class names and their arguments in " +
                "parentheses",
                names = {"--devel-parser-syntax"}, hidden = true)
        boolean simplifiedSyntax = false;

        @DynamicParameter(description = "Description group names and regular expressions to parse expected " +
                "nucleotide sequences for that groups from read description. Example: --description-group-CELLID1=" +
                "'ATTA.{2-5}GACA' --description-group-CELLID2='.{11}$'", names = {"--description-group-"})
        LinkedHashMap<String, String> descriptionGroups = new LinkedHashMap<>();

        @Override
        public void validate() {
            if (parameterNames.get(inputFormat) == null)
                throw new ParameterException("Unknown input format: " + inputFormat);
            validateQuality(goodQuality);
            validateQuality(badQuality);
        }
    }
}
