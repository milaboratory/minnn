package com.milaboratory.mist.io;

import java.io.IOException;

import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class CorrectBarcodesIO {
    private final String inputFileName;
    private final String outputFileName;

    public CorrectBarcodesIO(String inputFileName, String outputFileName) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        long correctedBarcodes = 0;
        try (MifReader pass1Reader = new MifReader(inputFileName);
             MifReader pass2Reader = new MifReader(inputFileName);
             MifWriter writer = createWriter(pass1Reader.getHeader())) {

        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads, corrected " + correctedBarcodes + " barcodes\n");
    }

    private MifWriter createWriter(MifHeader inputHeader) throws IOException {
        MifHeader outputHeader = new MifHeader(inputHeader.getNumberOfReads(), true,
                inputHeader.getGroupEdges());
        return (outputFileName == null) ? new MifWriter(System.out, outputHeader)
                : new MifWriter(outputFileName, outputHeader);
    }

}
