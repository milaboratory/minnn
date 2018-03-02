package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.FilterIO;
import com.milaboratory.mist.readfilter.ReadFilter;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import static com.milaboratory.mist.cli.Defaults.*;
import static com.milaboratory.mist.util.SystemUtils.*;

public final class FilterAction implements Action {
    private final FilterActionParameters params = new FilterActionParameters();
    private ReadFilter parsedReadFilter = null;

    @Override
    public void go(ActionHelper helper) {
        parseFilterQuery(params.filterQuery);
        if (parsedReadFilter == null)
            throw exitWithError("Filter query not parsed: " + params.filterQuery);
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
        @Parameter(description = "\"<filter_query>\"")
        private String description;

        @Parameter(description = "Filter query in MiST format.", order = 0, required = true)
        String filterQuery = null;

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

    private void parseFilterQuery(String filterQuery) {
        CodePointCharStream charStream = CharStreams.fromString(filterQuery);
        FilterGrammarLexer lexer = new FilterGrammarLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        FilterGrammarParser parser = new FilterGrammarParser(tokenStream);
        ParseTreeWalker walker = new ParseTreeWalker();
        AntlrFilterListener listener = new AntlrFilterListener();
        walker.walk(listener, parser.filter());
    }

    private class AntlrFilterListener extends FilterGrammarBaseListener {
        @Override
        public void enterFilter(FilterGrammarParser.FilterContext ctx) {
            System.out.println(ctx.getText());
        }
    }
}
