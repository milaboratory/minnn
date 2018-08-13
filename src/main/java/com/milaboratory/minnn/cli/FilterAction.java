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

import com.beust.jcommander.*;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.minnn.io.FilterIO;
import com.milaboratory.minnn.readfilter.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.*;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.util.SystemUtils.*;

public final class FilterAction implements Action {
    public static final String commandName = "filter";
    private final FilterActionParameters params = new FilterActionParameters();

    @Override
    public void go(ActionHelper helper) {
        String filterQuery = String.join("", params.filterQuery);
        ReadFilter parsedReadFilter = parseFilterQuery(filterQuery);
        if (parsedReadFilter == null)
            throw exitWithError("Filter query not parsed: " + filterQuery);
        FilterIO filterIO = new FilterIO(parsedReadFilter, params.inputFileName, params.outputFileName,
                params.inputReadsLimit, params.threads);
        filterIO.go();
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
            "Filter target nucleotide sequences, pass only sequences matching the query.")
    private static final class FilterActionParameters extends ActionParameters {
        @Parameter(description = "\"<filter_query>\"", order = 0, required = true)
        List<String> filterQuery = new ArrayList<>();

        @Parameter(description = "Input file in \"mif\" format. If not specified, stdin will be used.",
                names = {"--input"}, order = 1)
        String inputFileName = null;

        @Parameter(description = "Output file in \"mif\" format. If not specified, stdout will be used.",
                names = {"--output"}, order = 2)
        String outputFileName = null;

        @Parameter(description = "Use fair sorting and fair best match by score for all patterns.",
                names = {"--fair-sorting"}, order = 3)
        boolean fairSorting = false;

        @Parameter(description = "Number of reads to take; 0 value means to take the entire input file.",
                names = {"-n", "--number-of-reads"}, order = 4)
        long inputReadsLimit = 0;

        @Parameter(description = "Number of threads for parsing reads.",
                names = {"--threads"}, order = 5)
        int threads = DEFAULT_THREADS;
    }

    private ReadFilter parseFilterQuery(String filterQuery) {
        CodePointCharStream charStream = CharStreams.fromString(filterQuery);
        FilterGrammarLexer lexer = new FilterGrammarLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        FilterGrammarParser parser = new FilterGrammarParser(tokenStream);
        ParseTreeWalker walker = new ParseTreeWalker();
        FilterListener listener = new FilterListener();
        walker.walk(listener, parser.filter());
        return listener.getFilter();
    }

    private class AntlrFilterListener extends FilterGrammarBaseListener {
        protected ReadFilter filter = null;

        ReadFilter getFilter() {
            return filter;
        }

        protected void setIfNotNull(ParserRuleContext ctx, AntlrFilterListener listener) {
            if (ctx != null) {
                ctx.enterRule(listener);
                ReadFilter parsedFilter = listener.getFilter();
                if (parsedFilter != null)
                    filter = parsedFilter;
            }
        }
    }

    private class LenListener extends AntlrFilterListener {
        @Override
        public void enterLen(FilterGrammarParser.LenContext ctx) {
            filter = new LenReadFilter(ctx.groupName().getText(), Integer.parseInt(ctx.groupLength().getText()));
        }
    }

    private class MinConsensusReadsListener extends AntlrFilterListener {
        @Override
        public void enterMinConsensusReads(FilterGrammarParser.MinConsensusReadsContext ctx) {
            filter = new ConsensusReadsReadFilter(Integer.parseInt(ctx.minConsensusReadsNum().getText()));
        }
    }

    private class PatternListener extends AntlrFilterListener {
        @Override
        public void enterPattern(FilterGrammarParser.PatternContext ctx) {
            String patternString = ctx.patternString().getText();
            if ((patternString.charAt(0) != '\'') && (patternString.charAt(patternString.length() - 1) != '\''))
                throw exitWithError("Missing single quotes in pattern query " + patternString);
            filter = new PatternReadFilter(ctx.groupName().getText(),
                    patternString.substring(1, patternString.length() - 1), params.fairSorting);
        }
    }

    private class AndOperandListener extends AntlrFilterListener {
        ArrayList<ReadFilter> readFilters = new ArrayList<>();

        @Override
        public void enterAndOperand(FilterGrammarParser.AndOperandContext ctx) {
            setIfNotNull(ctx.pattern(), new PatternListener());
            setIfNotNull(ctx.minConsensusReads(), new MinConsensusReadsListener());
            setIfNotNull(ctx.len(), new LenListener());
            setIfNotNull(ctx.filterInParentheses(), new FilterInParenthesesListener());
            readFilters.add(filter);
        }
    }

    private class AndListener extends AntlrFilterListener {
        @Override
        public void enterAnd(FilterGrammarParser.AndContext ctx) {
            AndOperandListener andOperandListener = new AndOperandListener();
            ctx.andOperand().forEach(andOperandContext -> andOperandContext.enterRule(andOperandListener));
            filter = new AndReadFilter(andOperandListener.readFilters);
        }
    }

    private class OrOperandListener extends AntlrFilterListener {
        ArrayList<ReadFilter> readFilters = new ArrayList<>();

        @Override
        public void enterOrOperand(FilterGrammarParser.OrOperandContext ctx) {
            setIfNotNull(ctx.pattern(), new PatternListener());
            setIfNotNull(ctx.minConsensusReads(), new MinConsensusReadsListener());
            setIfNotNull(ctx.len(), new LenListener());
            setIfNotNull(ctx.and(), new AndListener());
            setIfNotNull(ctx.filterInParentheses(), new FilterInParenthesesListener());
            readFilters.add(filter);
        }
    }

    private class OrListener extends AntlrFilterListener {
        @Override
        public void enterOr(FilterGrammarParser.OrContext ctx) {
            OrOperandListener orOperandListener = new OrOperandListener();
            ctx.orOperand().forEach(orOperandContext -> orOperandContext.enterRule(orOperandListener));
            filter = new OrReadFilter(orOperandListener.readFilters);
        }
    }

    private class AnySingleFilterListener extends AntlrFilterListener {
        @Override
        public void enterAnySingleFilter(FilterGrammarParser.AnySingleFilterContext ctx) {
            setIfNotNull(ctx.pattern(), new PatternListener());
            setIfNotNull(ctx.minConsensusReads(), new MinConsensusReadsListener());
            setIfNotNull(ctx.len(), new LenListener());
            setIfNotNull(ctx.and(), new AndListener());
            setIfNotNull(ctx.or(), new OrListener());
        }
    }

    private class FilterInParenthesesListener extends AntlrFilterListener {
        @Override
        public void enterFilterInParentheses(FilterGrammarParser.FilterInParenthesesContext ctx) {
            setIfNotNull(ctx.anySingleFilter(), new AnySingleFilterListener());
        }
    }

    private class FilterListener extends AntlrFilterListener {
        @Override
        public void enterFilter(FilterGrammarParser.FilterContext ctx) {
            setIfNotNull(ctx.filterInParentheses(), new FilterInParenthesesListener());
            setIfNotNull(ctx.anySingleFilter(), new AnySingleFilterListener());
        }
    }
}
