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
package com.milaboratory.minnn.readfilter;

import com.milaboratory.core.alignment.PatternAndTargetAlignmentScoring;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.parser.Parser;
import com.milaboratory.minnn.parser.ParserException;
import com.milaboratory.minnn.pattern.*;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.util.SystemUtils.*;

public final class PatternReadFilter implements ReadFilter {
    private final String groupName;
    private final Pattern pattern;
    private final boolean fairSorting;

    public PatternReadFilter(String groupName, String patternQuery, boolean fairSorting) {
        this.groupName = groupName;
        PatternAndTargetAlignmentScoring scoring = new PatternAndTargetAlignmentScoring(0,
                -1, -1, DEFAULT_UPPERCASE_MISMATCH_SCORE,
                DEFAULT_GOOD_QUALITY, DEFAULT_BAD_QUALITY, 0);
        PatternAligner patternAligner = new BasePatternAligner(scoring, 0, -1,
                0, 0);
        Parser patternParser = new Parser(patternAligner);
        Pattern pattern;
        try {
            pattern = patternParser.parseQuery(patternQuery);
        } catch (ParserException e) {
            System.err.println("Error while parsing pattern " + patternQuery);
            throw exitWithError(e.getMessage());
        }
        if (pattern.getGroupEdges().stream().map(GroupEdge::getGroupName).anyMatch(g -> !g.equals("R1")))
            throw exitWithError("Filter patterns must be for single read and must not contain capture groups! "
                    + "Found wrong pattern: " + patternQuery);
        this.pattern = pattern;
        this.fairSorting = fairSorting;
    }

    @Override
    public ParsedRead filter(ParsedRead parsedRead) {
        if (parsedRead.getGroups().stream()
                .anyMatch(group -> group.getGroupName().equals(groupName)
                        && (pattern.match(group.getValue()).getBestMatch(fairSorting) != null)))
            return parsedRead;
        else
            return new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(), null,
                    parsedRead.getConsensusReads(), parsedRead.getOutputPortId());
    }
}
