package com.milaboratory.mist.cli;

import org.junit.*;

import java.io.File;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.mist.util.TestSettings.*;
import static org.junit.Assert.*;

public class CorrectActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "correctStart.mif";
        String inputFile = TEMP_DIR + "correctInput.mif";
        String outputFile = TEMP_DIR + "correctOutput.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            exec("extract --input-format mif --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            exec("correct --threads " + (rg.nextInt(10) + 1) + " --max-mismatches " + rg.nextInt(4)
                    + " --max-deletions " + rg.nextInt(4) + " --max-total-errors " + rg.nextInt(5)
                    + " --max-insertions " + rg.nextInt(4) + " --input " + inputFile
                    + " --output " + outputFile);
            assertFileNotEquals(inputFile, outputFile);
        }
        for (String fileName : new String[] { startFile, inputFile, outputFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String startFile = EXAMPLES_PATH + "mif/twosided.mif.gz";
        String inputFile = TEMP_DIR + "correct0.mif";
        gunzip(startFile, inputFile);
        assertOutputContains(true, "Error", () -> callableExec("correct --output " + inputFile));
        for (int i = 0; i < 4; i++) {
            String currentInput = TEMP_DIR + "correct" + i + ".mif";
            String currentOutput = TEMP_DIR + "correct" + (i + 1) + ".mif";
            exec("correct --input " + currentInput + " --output " + currentOutput);
            if (i < 3)
                assertFileNotEquals(currentInput, currentOutput);
            else
                assertFileEquals(currentInput, currentOutput);
        }
        exec("correct --input " + inputFile + " --output " + TEMP_DIR + "correct4.mif --max-total-errors 0");
        assertFileNotEquals(inputFile, TEMP_DIR + "correct4.mif");
        exec("correct --input " + inputFile + " --output " + TEMP_DIR + "correct5.mif --max-mismatches 0" +
                " --max-insertions 0 --max-deletions 0");
        assertFileEquals(TEMP_DIR + "correct4.mif", TEMP_DIR + "correct5.mif");
        for (int i = 0; i <= 5; i++)
            assertTrue(new File(TEMP_DIR + "correct" + i + ".mif").delete());
    }
}
