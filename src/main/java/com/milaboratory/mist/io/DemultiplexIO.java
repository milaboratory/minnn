package com.milaboratory.mist.io;

import java.io.IOException;
import java.util.*;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class DemultiplexIO {
    private final String inputFileName;
    private final List<String> barcodes;
    private final List<String> sampleFileNames;

    public DemultiplexIO(String inputFileName, List<String> barcodes, List<String> sampleFileNames) {
        this.inputFileName = inputFileName;
        this.barcodes = barcodes;
        this.sampleFileNames = sampleFileNames;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;

//        try (MifReader reader = new MifReader(inputFileName)) {
//
//        } catch (IOException e) {
//            throw exitWithError(e.getMessage());
//        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads\n");
    }
}
