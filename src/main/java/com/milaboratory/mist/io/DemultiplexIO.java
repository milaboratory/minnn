package com.milaboratory.mist.io;

import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mist.cli.DemultiplexArgument;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class DemultiplexIO {
    private final String inputFileName;
    private final List<DemultiplexArgument> demultiplexArguments;

    public DemultiplexIO(String inputFileName, List<DemultiplexArgument> demultiplexArguments) {
        this.inputFileName = inputFileName;
        this.demultiplexArguments = demultiplexArguments;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;

        try (MifReader reader = new MifReader(inputFileName)) {


        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads\n");
    }

    private List<Sample> getSamples(String sampleFileName) {
        List<Sample> parsedSamples = new ArrayList<>();
        File sampleFile = new File(sampleFileName);
        try (Scanner samples = new Scanner(sampleFile)) {
            String[] barcodeNames;
            if (samples.hasNextLine()) {
                String[] header = getTokens(samples.nextLine());
                if ((header.length < 2) || !header[0].equals("Sample"))
                    throw exitWithError("Wrong sample file " + sampleFileName + ": first line is expected to start "
                            + "with Sample keyword and contain at least 1 barcode name!");
                barcodeNames = new String[header.length - 1];
                System.arraycopy(header, 1, barcodeNames, 0, barcodeNames.length);
            } else
                throw exitWithError("Missing header in sample file " + sampleFileName);
            if (!samples.hasNextLine())
                throw exitWithError("Expected at least 1 sample in sample file " + sampleFileName);
            while (samples.hasNextLine()) {
                String[] sampleTokens = getTokens(samples.nextLine());
                if (sampleTokens.length == 0)
                    break;
                else if (sampleTokens.length == 1)
                    throw exitWithError("Wrong line in " + sampleFileName + ": " + sampleTokens[0]);
                else {
                    NucleotideSequence[] barcodeSequences = new NucleotideSequence[sampleTokens.length - 1];
                    for (int i = 0; i < barcodeSequences.length; i++)
                        barcodeSequences[i] = new NucleotideSequence(sampleTokens[i + 1]);
                    parsedSamples.add(new Sample(sampleTokens[0], barcodeNames, barcodeSequences));
                }
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        return parsedSamples;
    }

    private String[] getTokens(String string) {
        return string.split("[ \\t]");
    }

    private interface DemultiplexParameterValue {}

    private class Barcode implements DemultiplexParameterValue {
        final NucleotideSequence barcode;

        Barcode(String sequence) {
            barcode = new NucleotideSequence(sequence);
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
        final String prefix;
        final List<DemultiplexParameterValue> parameterValues;

        OutputFileIdentifier(String prefix, List<DemultiplexParameterValue> parameterValues) {
            this.prefix = prefix;
            this.parameterValues = parameterValues;
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
                builder.append(parameterValue);
            }
            builder.append(".mif");
            return builder.toString();
        }
    }
}
