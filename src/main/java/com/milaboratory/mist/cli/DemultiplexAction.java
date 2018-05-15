package com.milaboratory.mist.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
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

import static com.milaboratory.mist.cli.Defaults.*;
import static com.milaboratory.mist.util.SystemUtils.*;

public final class DemultiplexAction implements Action {
    private final DemultiplexActionParameters params = new DemultiplexActionParameters();

    @Override
    public void go(ActionHelper helper) {
        String argumentsQuery = "#" + String.join("#", params.argumentsQuery);
        ParsedDemultiplexArguments parsedDemultiplexArguments = parseArgumentsQuery(argumentsQuery);
        if (parsedDemultiplexArguments == null)
            throw exitWithError("Arguments not parsed: " + argumentsQuery);
        DemultiplexIO demultiplexIO = new DemultiplexIO(parsedDemultiplexArguments.inputFileName,
                parsedDemultiplexArguments.demultiplexArguments, params.threads);
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

        @Parameter(description = "Number of threads for parsing reads.",
                names = {"--threads"})
        int threads = DEFAULT_THREADS;
    }

    private static final class ParsedDemultiplexArguments {
        final String inputFileName;
        final List<DemultiplexArgument> demultiplexArguments;

        public ParsedDemultiplexArguments(String inputFileName, List<DemultiplexArgument> demultiplexArguments) {
            this.inputFileName = inputFileName;
            this.demultiplexArguments = demultiplexArguments;
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

    private class FileNameListener extends DemultiplexGrammarBaseListener {
        private String fileName = null;

        String getFileName() {
            return fileName;
        }

        @Override
        public void enterFileName(DemultiplexGrammarParser.FileNameContext ctx) {
            fileName = stripQuotes(ctx.getText());
        }

        private String stripQuotes(String str) {
            return str.replace("/(^\"|\')|(\"|\'$)/g", "");
        }
    }

    private class InputFileNameListener extends DemultiplexGrammarBaseListener {
        private String inputFileName = null;

        String getInputFileName() {
            return inputFileName;
        }

        @Override
        public void enterInputFileName(DemultiplexGrammarParser.InputFileNameContext ctx) {
            FileNameListener fileNameListener = new FileNameListener();
            ctx.fileName().enterRule(fileNameListener);
            inputFileName = fileNameListener.getFileName();
        }
    }

    private class ByBarcodeListener extends DemultiplexGrammarBaseListener {
        private String barcodeName = null;

        String getBarcodeName() {
            return barcodeName;
        }

        @Override
        public void enterByBarcode(DemultiplexGrammarParser.ByBarcodeContext ctx) {
            barcodeName = ctx.barcodeName().getText();
        }
    }

    private class BySampleListener extends DemultiplexGrammarBaseListener {
        private String sampleFileName = null;

        String getSampleFileName() {
            return sampleFileName;
        }

        @Override
        public void enterBySample(DemultiplexGrammarParser.BySampleContext ctx) {
            FileNameListener fileNameListener = new FileNameListener();
            ctx.fileName().enterRule(fileNameListener);
            sampleFileName = fileNameListener.getFileName();
        }
    }

    private class DemultiplexArgumentsListener extends DemultiplexGrammarBaseListener {
        private List<DemultiplexArgument> demultiplexArguments = new ArrayList<>();
        private List<String> inputFileNames = new ArrayList<>();

        @Override
        public void enterDemultiplexArguments(DemultiplexGrammarParser.DemultiplexArgumentsContext ctx) {
            ctx.bySample().forEach(currentBySampleCtx -> {
                BySampleListener bySampleListener = new BySampleListener();
                currentBySampleCtx.enterRule(bySampleListener);
                demultiplexArguments.add(new DemultiplexArgument(false,
                        bySampleListener.getSampleFileName()));
            });
            ctx.byBarcode().forEach(currentByBarcodeCtx -> {
                ByBarcodeListener byBarcodeListener = new ByBarcodeListener();
                currentByBarcodeCtx.enterRule(byBarcodeListener);
                demultiplexArguments.add(new DemultiplexArgument(true,
                        byBarcodeListener.getBarcodeName()));
            });
            ctx.inputFileName().forEach(currentInputFileNameCtx -> {
                InputFileNameListener inputFileNameListener = new InputFileNameListener();
                currentInputFileNameCtx.enterRule(inputFileNameListener);
                inputFileNames.add(inputFileNameListener.getInputFileName());
            });
        }

        ParsedDemultiplexArguments getParsedArguments() throws ParameterException {
            if (inputFileNames.size() > 1)
                throw new ParameterException("Expected 1 input file name, found multiple: " + inputFileNames);
            else if (inputFileNames.size() == 0)
                throw new ParameterException("Missing input file name!");
            if (demultiplexArguments.size() == 0)
                throw new ParameterException("Expected at least 1 barcode or sample configuration file!");
            return new ParsedDemultiplexArguments(inputFileNames.get(0), demultiplexArguments);
        }
    }
}
