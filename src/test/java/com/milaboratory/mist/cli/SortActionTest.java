package com.milaboratory.mist.cli;

import org.junit.*;

import java.io.File;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.cli.TestResources.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;
import static org.junit.Assert.*;

public class SortActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "sortStart.mif";
        String inputFile = TEMP_DIR + "sortInput.mif";
        String outputFile1 = TEMP_DIR + "sortOutput1.mif";
        String outputFile2 = TEMP_DIR + "sortOutput2.mif";
        String outputFile3 = TEMP_DIR + "sortOutput3.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            exec("extract --input-format mif --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:an{3}t)(G2:n{2})\" --bitap-max-errors 0");
            exec("sort --chunk-size " + (rg.nextInt(50000) + 100) + " --input " + inputFile
                    + " --output " + outputFile1 + " --groups G2 G1");
            exec("sort --input " + outputFile1 + " --output " + outputFile2 + " --groups G1");
            exec("sort --chunk-size " + (rg.nextInt(50000) + 100) + " --input " + outputFile2
                    + " --output " + outputFile3 + " --groups G2 G1");
            assertFileEquals(outputFile1, outputFile3);
        }
        for (String fileName : new String[] { startFile, inputFile, outputFile1, outputFile2, outputFile3 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String outputFile1 = TEMP_DIR + "sortOutput1.mif";
        String outputFile2 = TEMP_DIR + "sortOutput2.mif";
        String outputFile3 = TEMP_DIR + "sortOutput3.mif";
        exec("sort --input " + inputFile + " --output " + outputFile1 + " --groups G3 G4 G1 G2 R1 R2");
        exec("sort --input " + outputFile1 + " --output " + outputFile2 + " --groups R2 G2 R1");
        exec("sort --input " + outputFile2 + " --output " + outputFile3 + " --groups G3 G4 G1 G2 R1 R2");
        assertFileEquals(outputFile1, outputFile3);
        for (String fileName : new String[] { inputFile, outputFile1, outputFile2, outputFile3 })
            assertTrue(new File(fileName).delete());
    }
}
