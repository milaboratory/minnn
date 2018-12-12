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
import com.milaboratory.core.sequence.MultiNSequenceWithQuality;
import com.milaboratory.minnn.outputconverter.MatchedGroup;
import com.milaboratory.minnn.parser.Parser;
import com.milaboratory.minnn.parser.ParserException;
import com.milaboratory.minnn.pattern.*;
import picocli.CommandLine.*;

import java.util.*;

import static com.milaboratory.minnn.cli.CliUtils.*;
import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.ReportAction.REPORT_ACTION_NAME;
import static com.milaboratory.minnn.parser.ParserUtils.parseMultiTargetString;
import static com.milaboratory.minnn.util.CommonUtils.*;
import static com.milaboratory.minnn.util.SystemUtils.exitWithError;

@Command(name = REPORT_ACTION_NAME,
        sortOptions = false,
        separator = " ",
        description = "Find match and groups in query and display report on the screen.")
public final class ReportAction extends ACommand implements MiNNNCommand {
    public static final String REPORT_ACTION_NAME = "report";

    public ReportAction() {
        super(APP_NAME);
    }
    
    @Override
    public void run0() {
        PatternAndTargetAlignmentScoring scoring = new PatternAndTargetAlignmentScoring(matchScore, mismatchScore,
                gapScore, uppercaseMismatchScore, goodQuality, badQuality, maxQualityPenalty);
        PatternAligner patternAligner = new BasePatternAligner(scoring, scoreThreshold, singleOverlapPenalty,
                bitapMaxErrors, maxOverlap);
        Parser patternParser = new Parser(patternAligner);
        Pattern pattern;
        try {
            pattern = patternParser.parseQuery(stripQuotes(query));
        } catch (ParserException e) {
            System.err.println("Error while parsing the pattern!");
            throw exitWithError(e.getMessage());
        }
        MultiNSequenceWithQuality target = parseMultiTargetString(stripQuotes(targetString));
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
                ? pattern.match(target.get(0)).getBestMatch(fairSorting)
                : pattern.match(target).getBestMatch(fairSorting);

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
                for (MatchedRange matchedRange : bestMatch.getMatchedRanges())
                    if (!(matchedRange instanceof NullMatchedRange)) {
                        System.out.println("Found match in target " + matchedRange.getTargetId() + " ("
                                + matchedRange.getTarget().getSequence() + "): "
                                + matchedRange.getValue().getSequence());
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
    public void validateInfo(String inputFile) {}

    @Override
    public void validate() {
        super.validate();
        validateQuality(goodQuality, spec.commandLine());
        validateQuality(badQuality, spec.commandLine());
    }

    @Option(description = PATTERN_QUERY,
            names = {"--pattern"},
            required = true)
    private String query = null;

    @Option(description = "Target nucleotide sequence, where to search.",
            names = {"--target"},
            required = true)
    private String targetString = null;

    @Option(description = MATCH_SCORE,
            names = {"--match-score"})
    private int matchScore = DEFAULT_MATCH_SCORE;

    @Option(description = MISMATCH_SCORE,
            names = {"--mismatch-score"})
    private int mismatchScore = DEFAULT_MISMATCH_SCORE;

    @Option(description = UPPERCASE_MISMATCH_SCORE,
            names = {"--uppercase-mismatch-score"})
    private int uppercaseMismatchScore = DEFAULT_UPPERCASE_MISMATCH_SCORE;

    @Option(description = GAP_SCORE,
            names = {"--gap-score"})
    private int gapScore = DEFAULT_GAP_SCORE;

    @Option(description = SCORE_THRESHOLD,
            names = {"--score-threshold"})
    private long scoreThreshold = DEFAULT_SCORE_THRESHOLD;

    @Option(description = GOOD_QUALITY_VALUE,
            names = {"--good-quality-value"})
    private byte goodQuality = DEFAULT_GOOD_QUALITY;

    @Option(description = BAD_QUALITY_VALUE,
            names = {"--bad-quality-value"})
    private byte badQuality = DEFAULT_BAD_QUALITY;

    @Option(description = MAX_QUALITY_PENALTY,
            names = {"--max-quality-penalty"})
    private int maxQualityPenalty = DEFAULT_MAX_QUALITY_PENALTY;

    @Option(description = SINGLE_OVERLAP_PENALTY,
            names = {"--single-overlap-penalty"})
    private long singleOverlapPenalty = DEFAULT_SINGLE_OVERLAP_PENALTY;

    @Option(description = MAX_OVERLAP,
            names = {"--max-overlap"})
    private int maxOverlap = DEFAULT_MAX_OVERLAP;

    @Option(description = BITAP_MAX_ERRORS,
            names = {"--bitap-max-errors"})
    private int bitapMaxErrors = DEFAULT_BITAP_MAX_ERRORS;

    @Option(description = FAIR_SORTING,
            names = {"--fair-sorting"})
    private boolean fairSorting = true;
}
