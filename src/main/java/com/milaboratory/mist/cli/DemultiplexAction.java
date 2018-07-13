package com.milaboratory.mist.cli;

import com.beust.jcommander.*;
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
    public static final String commandName = "demultiplex";
    private final DemultiplexActionParameters params = new DemultiplexActionParameters();

    @Override
    public void go(ActionHelper helper) {
        String argumentsQuery = "#" + String.join("#", params.argumentsQuery);
        ParsedDemultiplexArguments parsedDemultiplexArguments = parseArgumentsQuery(argumentsQuery);
        if (parsedDemultiplexArguments == null)
            throw exitWithError("Arguments not parsed: " + argumentsQuery);
        DemultiplexIO demultiplexIO = new DemultiplexIO(parsedDemultiplexArguments.inputFileName,
                parsedDemultiplexArguments.demultiplexArguments, params.outputBufferSize, params.inputReadsLimit);
        demultiplexIO.go();
    }

    @Override
    public String command() {
        return commandName;
    }

    @Override
    public ActionParameters params() {
        return params;
    }

    @Parameters(commandDescription = "Multi-filtering (one to many) for nucleotide sequences.")
    private static final class DemultiplexActionParameters extends ActionParameters {
        @Parameter(description = "filter_options\n        Filter Options:      Barcodes and sample configuration " +
                "files that specify sequences for demultiplexing. At least 1 barcode or 1 sample file must be " +
                "specified. Syntax example: mist demultiplex --by-barcode UID --by-sample samples.txt input.mif",
                order = 0, required = true)
        List<String> argumentsQuery = new ArrayList<>();

        @Parameter(description = "Write buffer size for each output file.",
                names = {"--output-buffer-size"}, order = 1)
        int outputBufferSize = DEFAULT_DEMULTIPLEX_OUTPUT_BUFFER_SIZE;

        @Parameter(description = "Number of reads to take; 0 value means to take the entire input file.",
                names = {"-n", "--number-of-reads"}, order = 2)
        long inputReadsLimit = 0;
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
            ctx.demultiplexArgument().forEach(currentArgument -> {
                if (currentArgument.bySample() != null) {
                    BySampleListener bySampleListener = new BySampleListener();
                    currentArgument.bySample().enterRule(bySampleListener);
                    demultiplexArguments.add(new DemultiplexArgument(false,
                            bySampleListener.getSampleFileName()));
                } else if (currentArgument.byBarcode() != null) {
                    ByBarcodeListener byBarcodeListener = new ByBarcodeListener();
                    currentArgument.byBarcode().enterRule(byBarcodeListener);
                    demultiplexArguments.add(new DemultiplexArgument(true,
                            byBarcodeListener.getBarcodeName()));
                } else if (currentArgument.inputFileName() != null) {
                    InputFileNameListener inputFileNameListener = new InputFileNameListener();
                    currentArgument.inputFileName().enterRule(inputFileNameListener);
                    inputFileNames.add(inputFileNameListener.getInputFileName());
                }
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
