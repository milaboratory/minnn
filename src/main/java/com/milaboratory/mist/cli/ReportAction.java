package com.milaboratory.mist.cli;

import com.beust.jcommander.*;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.mist.outputconverter.MatchedGroup;
import com.milaboratory.mist.parser.Parser;
import com.milaboratory.mist.parser.ParserException;
import com.milaboratory.mist.pattern.*;

import java.util.*;

import static com.milaboratory.mist.cli.CliUtils.*;
import static com.milaboratory.mist.cli.Defaults.*;
import static com.milaboratory.mist.parser.ParserUtils.parseMultiTargetString;
import static com.milaboratory.mist.util.SystemUtils.exitWithError;

public final class ReportAction implements Action {
    public static final String commandName = "report";
    private final ReportActionParameters params = new ReportActionParameters();

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
            pattern = patternParser.parseQuery(params.query);
        } catch (ParserException e) {
            System.err.println("Error while parsing the pattern!");
            throw exitWithError(e.getMessage());
        }
        MultiNSequenceWithQuality target = parseMultiTargetString(params.target);
        try {
            for (int i = 0; i < target.numberOfSequences(); i++)
                target.get(i);
        } catch (Exception e) {
            System.err.println("Error while parsing the target!");
            throw exitWithError(e.getMessage());
        }
        if (pattern instanceof SinglePattern && target.numberOfSequences() > 1)
            throw exitWithError("Pattern is for single target, but found multi-target!");
        if (pattern instanceof MultipleReadsOperator && target.numberOfSequences() == 1)
            throw exitWithError("Pattern is for multi-target, but found single target!");
        MatchIntermediate bestMatch = pattern instanceof SinglePattern
                ? pattern.match(target.get(0)).getBestMatch(params.fairSorting)
                : pattern.match(target).getBestMatch(params.fairSorting);

        if (bestMatch == null)
            System.out.println("Pattern not found in the target.");
        else {
            ArrayList<MatchedGroup> matchedGroups = bestMatch.getGroups();
            if (target.numberOfSequences() == 1) {
                System.out.println("Found match in range " + bestMatch.getRange() + ": "
                        + bestMatch.getValue().getSequence() + "\n");
                for (MatchedGroup matchedGroup : matchedGroups) {
                    System.out.println("Found matched group " + matchedGroup.getGroupName() + ": "
                            + matchedGroup.getValue().getSequence());
                    System.out.println("Range in target string: " + matchedGroup.getRange() + "\n");
                }
            } else {
                for (MatchedRange matchedRange : bestMatch.getMatchedRanges()) {
                    System.out.println("Found match in target " + matchedRange.getTargetId() + " ("
                            + matchedRange.getTarget().getSequence() + "): " + matchedRange.getValue().getSequence());
                    System.out.println("Range in this target: " + matchedRange.getRange() + "\n");
                }
                for (MatchedGroup matchedGroup : matchedGroups) {
                    System.out.println("Found matched group " + matchedGroup.getGroupName() + " in target "
                            + matchedGroup.getTargetId() + " (" + matchedGroup.getTarget().getSequence() + "): "
                            + matchedGroup.getValue().getSequence());
                    System.out.println("Range in this target: " + matchedGroup.getRange() + "\n");
                }
            }
        }
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
            "Find match and groups in query and display report on the screen.")
    private static final class ReportActionParameters extends ActionParameters {
        @Parameter(description = "--pattern <pattern_query> --target <sequence>", order = 0)
        private String description;

        @Parameter(description = "Query, pattern specified in MiST format.",
                names = {"--pattern"}, order = 1, required = true)
        String query = null;

        @Parameter(description = "Target nucleotide sequence, where to search.",
                names = {"--target"}, order = 2, required = true)
        String target = null;

        @Parameter(description = "Score for perfectly matched nucleotide.",
                names = {"--match-score"}, order = 3)
        int matchScore = DEFAULT_MATCH_SCORE;

        @Parameter(description = "Score for mismatched nucleotide.",
                names = {"--mismatch-score"}, order = 4)
        int mismatchScore = DEFAULT_MISMATCH_SCORE;

        @Parameter(description = "Score for mismatched uppercase nucleotide.",
                names = {"--uppercase-mismatch-score"}, order = 5)
        int uppercaseMismatchScore = DEFAULT_UPPERCASE_MISMATCH_SCORE;

        @Parameter(description = "Score for gap or insertion.",
                names = {"--gap-score"}, order = 6)
        int gapScore = DEFAULT_GAP_SCORE;

        @Parameter(description = "Score threshold, matches with score lower than this will not go to output.",
                names = {"--score-threshold"}, order = 7)
        long scoreThreshold = DEFAULT_SCORE_THRESHOLD;

        @Parameter(description = "This or better quality value will be considered good quality, " +
                "without score penalties.",
                names = {"--good-quality-value"}, order = 8)
        int goodQuality = DEFAULT_GOOD_QUALITY;

        @Parameter(description = "This or worse quality value will be considered bad quality, " +
                "with maximal score penalty.",
                names = {"--bad-quality-value"}, order = 9)
        int badQuality = DEFAULT_BAD_QUALITY;

        @Parameter(description = "Maximal score penalty for bad quality nucleotide in target.",
                names = {"--max-quality-penalty"}, order = 10)
        int maxQualityPenalty = DEFAULT_MAX_QUALITY_PENALTY;

        @Parameter(description = "Score penalty for 1 nucleotide overlap between neighbor patterns. Negative value.",
                names = {"--single-overlap-penalty"}, order = 11)
        long singleOverlapPenalty = DEFAULT_SINGLE_OVERLAP_PENALTY;

        @Parameter(description = "Max allowed overlap for 2 intersecting operands in +, & and pattern sequences.",
                names = {"--max-overlap"}, order = 12)
        int maxOverlap = DEFAULT_MAX_OVERLAP;

        @Parameter(description = "Maximum allowed number of errors for bitap matcher.",
                names = {"--bitap-max-errors"}, order = 13)
        int bitapMaxErrors = DEFAULT_BITAP_MAX_ERRORS;

        @Parameter(description = "Use fair sorting and fair best match by score for all patterns.",
                names = {"--fair-sorting"}, order = 14)
        boolean fairSorting = true;

        @Override
        public void validate() {
            validateQuality(goodQuality);
            validateQuality(badQuality);
        }
    }
}
