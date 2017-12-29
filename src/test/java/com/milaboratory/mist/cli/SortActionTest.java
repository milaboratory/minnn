package com.milaboratory.mist.cli;

import org.junit.Test;

import java.io.File;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static org.junit.Assert.*;

public class SortActionTest {
    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "sortStart.mif";
        String inputFile = TEMP_DIR + "sortInput.mif";
        String outputFile1 = TEMP_DIR + "sortOutput1.mif";
        String outputFile2 = TEMP_DIR + "sortOutput2.mif";
        String outputFile3 = TEMP_DIR + "sortOutput3.mif";
        for (int i = 0; i < 100; i++) {
            createRandomMifFile(startFile);
            exec("extract --input-format mif --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:an{3}t)(G2:n{2})\" --bitap-max-errors 0");
            exec("sort --chunk-size " + (rg.nextInt(50000) + 1) + " --input " + inputFile
                    + " --output " + outputFile1 + " --groups G2 G1");
            exec("sort --chunk-size " + (rg.nextInt(50000) + 1) + " --input " + outputFile1
                    + " --output " + outputFile2 + " --groups G1");
            exec("sort --chunk-size " + (rg.nextInt(50000) + 1) + " --input " + outputFile2
                    + " --output " + outputFile3 + " --groups G2 G1");
            assertFileEquals(outputFile1, outputFile3);
        }
        for (String fileName : new String[] { startFile, inputFile, outputFile1, outputFile2, outputFile3 })
            assertTrue(new File(fileName).delete());
    }
}
