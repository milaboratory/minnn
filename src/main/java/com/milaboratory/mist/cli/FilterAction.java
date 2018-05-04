package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.FilterIO;
import com.milaboratory.mist.readfilter.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.*;

import static com.milaboratory.mist.cli.Defaults.*;
import static com.milaboratory.mist.util.SystemUtils.*;

public final class FilterAction implements Action {
    private final FilterActionParameters params = new FilterActionParameters();

    @Override
    public void go(ActionHelper helper) {
        String filterQuery = String.join("", params.filterQuery);
        ReadFilter parsedReadFilter = parseFilterQuery(filterQuery);
        if (parsedReadFilter == null)
            throw exitWithError("Filter query not parsed: " + filterQuery);
        FilterIO filterIO = new FilterIO(parsedReadFilter, params.inputFileName, params.outputFileName, params.threads);
        filterIO.go();
    }

    @Override
    public String command() {
        return "filter";
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

        @Parameter(description = "Number of threads for parsing reads.",
                names = {"--threads"})
        int threads = DEFAULT_THREADS;

        @Parameter(description = "Use fair sorting and fair best match by score for all patterns.",
                names = {"--fair-sorting"})
        boolean fairSorting = false;
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
