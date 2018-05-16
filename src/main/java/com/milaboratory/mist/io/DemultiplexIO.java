package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import cc.redberry.pipe.OutputPort;
import cc.redberry.pipe.Processor;
import cc.redberry.pipe.blocks.Merger;
import cc.redberry.pipe.blocks.ParallelProcessor;
import cc.redberry.pipe.util.Chunk;
import cc.redberry.pipe.util.OrderedOutputPort;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mist.cli.DemultiplexArgument;
import com.milaboratory.mist.outputconverter.MatchedGroup;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.util.SmartProgressReporter;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class DemultiplexIO {
    private final String inputFileName;
    private final List<DemultiplexFilter> demultiplexFilters;
    private final int threads;
    private final int outputBufferSize;
    private final String prefix;
    private final TIntObjectHashMap<OutputFileIdentifier> outputFileIdentifiers = new TIntObjectHashMap<>();
    private MifHeader header;

    public DemultiplexIO(String inputFileName, List<DemultiplexArgument> demultiplexArguments, int threads,
                         int outputBufferSize) {
        this.inputFileName = inputFileName;
        this.demultiplexFilters = demultiplexArguments.stream().map(this::parseFilter).collect(Collectors.toList());
        this.threads = threads;
        this.outputBufferSize = outputBufferSize;
        this.prefix = ((inputFileName.length() > 4)
                && inputFileName.substring(inputFileName.length() - 4).equals(".mif"))
                ? inputFileName.substring(0, inputFileName.length() - 4) : inputFileName;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        long matchedReads = 0;
        try (MifReader reader = new MifReader(inputFileName)) {
            header = reader.getHeader();
            SmartProgressReporter.startProgressReport("Demultiplexing reads", reader, System.err);
            Merger<Chunk<ParsedRead>> bufferedReaderPort = CUtils.buffered(CUtils.chunked(
                    new NumberedParsedReadsPort(reader), 4 * 64), 4 * 16);
            OutputPort<Chunk<ProcessorOutput>> processorOutputPort = new ParallelProcessor<>(bufferedReaderPort,
                    CUtils.chunked(new DemultiplexProcessor()), threads);
            OrderedOutputPort<ProcessorOutput> orderedPort = new OrderedOutputPort<>(
                    CUtils.unchunked(processorOutputPort), output -> output.parsedRead.getOriginalRead().getId());
            for (ProcessorOutput processorOutput : CUtils.it(orderedPort)) {
                totalReads++;
                if (processorOutput.mifWriter != null) {
                    processorOutput.mifWriter.write(processorOutput.parsedRead);
                    matchedReads++;
                }
            }
            outputFileIdentifiers.valueCollection().forEach(OutputFileIdentifier::closeWriter);
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads, matched " + matchedReads + " reads\n");
    }

    private DemultiplexFilter parseFilter(DemultiplexArgument demultiplexArgument) {
        if (demultiplexArgument.isBarcode)
            return new BarcodeFilter(demultiplexArgument);
        else
            return new SampleFilter(demultiplexArgument);
    }

    private interface DemultiplexFilter {
        DemultiplexParameterValue filter(ParsedRead parsedRead);
    }

    private class BarcodeFilter implements DemultiplexFilter {
        private final String name;

        BarcodeFilter(DemultiplexArgument argument) {
            if (!argument.isBarcode)
                throw new IllegalArgumentException("Non-barcode argument " + argument.argument
                        + " passed to BarcodeFilter!");
            name = argument.argument;
        }

        @Override
        public DemultiplexParameterValue filter(ParsedRead parsedRead) {
            for (MatchedGroup matchedGroup : parsedRead.getGroups())
                if (matchedGroup.getGroupName().equals(name))
                    return new Barcode(matchedGroup.getValue().getSequence());
            return null;
        }
    }

    private class SampleFilter implements DemultiplexFilter {
        private final List<Sample> samples;

        SampleFilter(DemultiplexArgument argument) {
            if (argument.isBarcode)
                throw new IllegalArgumentException("Non-sample argument " + argument.argument
                        + " passed to SampleFilter!");
            samples = new ArrayList<>();
            File sampleFile = new File(argument.argument);
            try (Scanner sampleScanner = new Scanner(sampleFile)) {
                String[] barcodeNames;
                if (sampleScanner.hasNextLine()) {
                    String[] header = getTokens(sampleScanner.nextLine());
                    if ((header.length < 2) || !header[0].equals("Sample"))
                        throw exitWithError("Wrong sample file " + argument.argument + ": first line is expected " +
                                "to start with Sample keyword and contain at least 1 barcode name!");
                    barcodeNames = new String[header.length - 1];
                    System.arraycopy(header, 1, barcodeNames, 0, barcodeNames.length);
                } else
                    throw exitWithError("Missing header in sample file " + argument.argument);
                if (!sampleScanner.hasNextLine())
                    throw exitWithError("Expected at least 1 sample in sample file " + argument.argument);
                while (sampleScanner.hasNextLine()) {
                    String[] sampleTokens = getTokens(sampleScanner.nextLine());
                    if (sampleTokens.length == 0)
                        break;
                    else if (sampleTokens.length == 1)
                        throw exitWithError("Wrong line in " + argument.argument + ": " + sampleTokens[0]);
                    else {
                        NucleotideSequence[] barcodeSequences = new NucleotideSequence[sampleTokens.length - 1];
                        for (int i = 0; i < barcodeSequences.length; i++)
                            barcodeSequences[i] = new NucleotideSequence(sampleTokens[i + 1]);
                        samples.add(new Sample(sampleTokens[0], barcodeNames, barcodeSequences));
                    }
                }
            } catch (IOException e) {
                throw exitWithError(e.getMessage());
            }
        }

        private String[] getTokens(String string) {
            return string.split("[ \\t]");
        }

        @Override
        public DemultiplexParameterValue filter(ParsedRead parsedRead) {
            for (Sample sample : samples) {
                boolean allMatch = true;
                for (int i = 0; i < sample.numBarcodes(); i++) {
                    String currentName = sample.barcodeNames[i];
                    NucleotideSequence currentSequence = sample.barcodeSequences[i];
                    boolean groupFound = false;
                    for (MatchedGroup matchedGroup : parsedRead.getGroups())
                        if (matchedGroup.getGroupName().equals(currentName)) {
                            groupFound = true;
                            if (!matchedGroup.getValue().getSequence().equals(currentSequence))
                                allMatch = false;
                            break;
                        }
                    if (!groupFound)
                        allMatch = false;
                    if (!allMatch)
                        break;
                }
                if (allMatch)
                    return sample;
            }
            return null;
        }
    }

    private interface DemultiplexParameterValue {}

    private class Barcode implements DemultiplexParameterValue {
        final NucleotideSequence barcode;

        Barcode(NucleotideSequence barcode) {
            this.barcode = barcode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Barcode that = (Barcode)o;
            return barcode.equals(that.barcode);
        }

        @Override
        public int hashCode() {
            return barcode.hashCode();
        }

        @Override
        public String toString() {
            return barcode.toString();
        }
    }

    private class Sample implements DemultiplexParameterValue {
        final String name;
        final String[] barcodeNames;
        final NucleotideSequence[] barcodeSequences;

        Sample(String name, String[] barcodeNames, NucleotideSequence[] barcodeSequences) {
            if (barcodeNames.length == 0)
                throw exitWithError("Invalid sample file: missing barcode names!");
            if (barcodeNames.length != barcodeSequences.length)
                throw exitWithError("Invalid sample: mismatched number of barcode names "
                        + Arrays.toString(barcodeNames) + " and barcodes " + Arrays.toString(barcodeSequences));
            this.name = name;
            this.barcodeNames = barcodeNames;
            this.barcodeSequences = barcodeSequences;
        }

        int numBarcodes() {
            return barcodeNames.length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Sample sample = (Sample)o;
            return Arrays.equals(barcodeSequences, sample.barcodeSequences);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(barcodeSequences);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private class OutputFileIdentifier {
        final List<DemultiplexParameterValue> parameterValues;
        MifWriter writer = null;

        OutputFileIdentifier(List<DemultiplexParameterValue> parameterValues) {
            this.parameterValues = parameterValues;
        }

        MifWriter getWriter() {
            if (writer == null) {
                try {
                    writer = new MifWriter(toString(), header, outputBufferSize);
                } catch (IOException e) {
                    throw exitWithError(e.getMessage());
                }
            }
            return writer;
        }

        void closeWriter() {
            if (writer != null)
                writer.close();
            writer = null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OutputFileIdentifier that = (OutputFileIdentifier)o;
            return parameterValues.equals(that.parameterValues);
        }

        @Override
        public int hashCode() {
            return parameterValues.hashCode();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(prefix);
            for (DemultiplexParameterValue parameterValue : parameterValues) {
                builder.append('_');
                builder.append(parameterValue.toString());
            }
            builder.append(".mif");
            return builder.toString();
        }
    }

    private class ProcessorOutput {
        final ParsedRead parsedRead;
        final MifWriter mifWriter;

        ProcessorOutput(ParsedRead parsedRead, MifWriter mifWriter) {
            this.parsedRead = parsedRead;
            this.mifWriter = mifWriter;
        }
    }

    private class DemultiplexProcessor implements Processor<ParsedRead, ProcessorOutput> {
        @Override
        public ProcessorOutput process(ParsedRead parsedRead) {
            List<DemultiplexParameterValue> parameterValues = new ArrayList<>();
            for (DemultiplexFilter demultiplexFilter : demultiplexFilters) {
                DemultiplexParameterValue parameterValue = demultiplexFilter.filter(parsedRead);
                if (parameterValue == null)
                    return new ProcessorOutput(parsedRead, null);
                else
                    parameterValues.add(parameterValue);
            }

            return new ProcessorOutput(parsedRead, getMifWriter(new OutputFileIdentifier(parameterValues)));
        }

        private synchronized MifWriter getMifWriter(OutputFileIdentifier outputFileIdentifier) {
            int hashCode = outputFileIdentifier.hashCode();
            if (outputFileIdentifiers.containsKey(hashCode))
                return outputFileIdentifiers.get(hashCode).getWriter();
            else {
                outputFileIdentifiers.put(hashCode, outputFileIdentifier);
                return outputFileIdentifier.getWriter();
            }
        }
    }
}
