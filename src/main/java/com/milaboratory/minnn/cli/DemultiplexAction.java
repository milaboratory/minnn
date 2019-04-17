/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
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
import com.milaboratory.minnn.io.DemultiplexIO;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import picocli.CommandLine.*;

import java.io.*;
import java.util.*;

import static com.milaboratory.minnn.cli.CommonDescriptions.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.cli.DemultiplexAction.DEMULTIPLEX_ACTION_NAME;
import static com.milaboratory.minnn.cli.PipelineConfigurationReaderMiNNN.pipelineConfigurationReaderInstance;
import static com.milaboratory.minnn.io.MifInfoExtractor.mifInfoExtractor;
import static com.milaboratory.minnn.util.SystemUtils.*;

@Command(name = DEMULTIPLEX_ACTION_NAME,
        sortOptions = false,
        showDefaultValues = true,
        separator = " ",
        description = "Multi-filtering (one to many) for nucleotide sequences.")
public final class DemultiplexAction extends ACommandWithSmartOverwrite implements MiNNNCommand {
    public static final String DEMULTIPLEX_ACTION_NAME = "demultiplex";
    private ParsedDemultiplexArguments parsedDemultiplexArguments = null;

    public DemultiplexAction() {
        super(APP_NAME, mifInfoExtractor, pipelineConfigurationReaderInstance);
    }

    @Override
    public void run1() {
        prepareDemultiplexArguments();
        DemultiplexIO demultiplexIO = new DemultiplexIO(getFullPipelineConfiguration(),
                parsedDemultiplexArguments.inputFileName, parsedDemultiplexArguments.demultiplexArguments,
                logFileName, outputBufferSize, inputReadsLimit, reportFileName, jsonReportFileName);
        demultiplexIO.go();
    }

    @Override
    public void validateInfo(String inputFile) {
        MiNNNCommand.super.validateInfo(inputFile);
    }

    @Override
    public void validate() {
        if (argumentsQueryList == null)
            throwValidationException("Filter options are not specified!");
        MiNNNCommand.super.validate(getInputFiles(), getOutputFiles());
    }

    @Override
    protected List<String> getInputFiles() {
        prepareDemultiplexArguments();
        List<String> inputFileNames = new ArrayList<>();
        inputFileNames.add(parsedDemultiplexArguments.inputFileName);
        return inputFileNames;
    }

    @Override
    protected List<String> getOutputFiles() {
        List<String> outputFileNames = new ArrayList<>();
        outputFileNames.add(logFileName);
        if (new File(logFileName).exists())
            try (BufferedReader logReader = new BufferedReader(new FileReader(logFileName)))
            {
                String loggedFileName;
                while ((loggedFileName = logReader.readLine()) != null)
                    outputFileNames.add(loggedFileName);
            } catch (IOException e) {
                throw exitWithError("Bad or corrupted log file, read error: " + e.getMessage());
            }
        return outputFileNames;
    }

    @Override
    public void handleExistenceOfOutputFile(String outFileName) {
        MiNNNCommand.super.handleExistenceOfOutputFile(outFileName, forceOverwrite || overwriteIfRequired);
    }

    @Override
    public ActionConfiguration getConfiguration() {
        return new DemultiplexActionConfiguration(new DemultiplexActionConfiguration.DemultiplexActionParameters(
                argumentsQueryList, inputReadsLimit));
    }

    @Override
    public PipelineConfiguration getFullPipelineConfiguration() {
        prepareDemultiplexArguments();
        return PipelineConfiguration.appendStep(pipelineConfigurationReader.fromFile(
                parsedDemultiplexArguments.inputFileName, binaryFileInfoExtractor.getFileInfo(
                        parsedDemultiplexArguments.inputFileName)), getInputFiles(), getConfiguration(),
                AppVersionInfo.get());
    }

    private void prepareDemultiplexArguments() {
        if (parsedDemultiplexArguments != null)
            return;
        String argumentsQuery = "#" + String.join("#", argumentsQueryList);
        parsedDemultiplexArguments = parseArgumentsQuery(argumentsQuery);
        if (parsedDemultiplexArguments == null)
            throw exitWithError("Arguments not parsed: " + argumentsQueryList);
    }

    @Parameters(arity = "1..*",
            description = "Filter Options: Barcodes and sample configuration files that specify sequences for " +
            "demultiplexing. At least 1 barcode or 1 sample file must be specified. Syntax example: " +
            "minnn demultiplex --by-barcode UID --by-sample samples.txt input.mif")
    private List<String> argumentsQueryList = null;

    @Option(description = "Demultiplex log file name, to record names of generated files.",
            names = {"--demultiplex-log"},
            required = true)
    private String logFileName = null;

    @Option(description = "Write buffer size for each output file.",
            names = {"--output-buffer-size"})
    private int outputBufferSize = DEFAULT_DEMULTIPLEX_OUTPUT_BUFFER_SIZE;

    @Option(description = "Number of reads to take; 0 value means to take the entire input file.",
            names = {"-n", "--number-of-reads"})
    private long inputReadsLimit = 0;

    @Option(description = REPORT,
            names = "--report")
    private String reportFileName = null;

    @Option(description = JSON_REPORT,
            names = "--json-report")
    private String jsonReportFileName = null;

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

        ParsedDemultiplexArguments getParsedArguments() {
            if (inputFileNames.size() > 1)
                throwValidationException("Expected 1 input file name, found multiple: " + inputFileNames);
            else if (inputFileNames.size() == 0)
                throwValidationException("Missing input file name!");
            if (demultiplexArguments.size() == 0)
                throwValidationException("Expected at least 1 barcode or sample configuration file!");
            return new ParsedDemultiplexArguments(inputFileNames.get(0), demultiplexArguments);
        }
    }
}
