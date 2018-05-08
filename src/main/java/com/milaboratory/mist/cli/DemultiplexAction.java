package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.milaboratory.cli.Action;
import com.milaboratory.cli.ActionHelper;
import com.milaboratory.cli.ActionParameters;
import com.milaboratory.mist.io.DemultiplexIO;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.*;

import static com.milaboratory.mist.util.SystemUtils.*;

public final class DemultiplexAction implements Action {
    private final DemultiplexActionParameters params = new DemultiplexActionParameters();


    @Override
    public void go(ActionHelper helper) {
        String argumentsQuery = String.join("", params.argumentsQuery);
        ParsedDemultiplexArguments parsedDemultiplexArguments = parseArgumentsQuery(argumentsQuery);
        if (parsedDemultiplexArguments == null)
            throw exitWithError("Arguments not parsed: " + argumentsQuery);
        DemultiplexIO demultiplexIO = new DemultiplexIO();
        demultiplexIO.go();
    }

    @Override
    public String command() {
        return "demultiplex";
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription = "Multi-filtering (one to many) for nucleotide sequences.")
    private static final class DemultiplexActionParameters extends ActionParameters {
        @Parameter(description = "\"<configuration_files_and_filters>\"", order = 0, required = true)
        List<String> argumentsQuery = new ArrayList<>();

    }

    static final class ParsedDemultiplexArguments {
        final String dummy;

        public ParsedDemultiplexArguments(String dummy) {
            this.dummy = dummy;
        }
    }

    private ParsedDemultiplexArguments parseArgumentsQuery(String argumentsQuery) {
        CodePointCharStream charStream = CharStreams.fromString(argumentsQuery);
        DemultiplexGrammarLexer lexer = new DemultiplexGrammarLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        DemultiplexGrammarParser parser = new DemultiplexGrammarParser(tokenStream);
        ParseTreeWalker walker = new ParseTreeWalker();
        DemultiplexArgumentsListener listener = new DemultiplexArgumentsListener();
        walker.walk(listener, parser.demultiplexArguments());
        return listener.getParsedArguments();
    }

    private class AntlrDemultiplexListener extends DemultiplexGrammarBaseListener {

    }

    private class DemultiplexArgumentsListener extends AntlrDemultiplexListener {
        @Override
        public void enterDemultiplexArguments(DemultiplexGrammarParser.DemultiplexArgumentsContext ctx) {

        }

        ParsedDemultiplexArguments getParsedArguments() {
            return new ParsedDemultiplexArguments("");
        }
    }
}
