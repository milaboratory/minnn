package com.milaboratory.mist.cli;

import org.junit.*;

import java.io.File;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.cli.TestResources.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;
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
                    + " --max-indels " + rg.nextInt(4) + " --max-total-errors " + rg.nextInt(5)
                    + " --cluster-threshold " + (rg.nextFloat() * 0.98 + 0.01)
                    + " --input " + inputFile + " --output " + outputFile + " --groups G1 G2");
            assertFileNotEquals(inputFile, outputFile);
        }
        for (String fileName : new String[] { startFile, inputFile, outputFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        assertOutputContains(true, "Error", () -> callableExec("correct --output " + inputFile
                + " --groups G1"));
        assertOutputContains(true, "Error", () -> callableExec("correct --input " + inputFile
                + " --output " + inputFile));
        for (int i = 0; i < 4; i++) {
            String currentInput = (i == 0) ? inputFile : TEMP_DIR + "correct" + i + ".mif";
            String currentOutput = TEMP_DIR + "correct" + (i + 1) + ".mif";
            exec("correct --groups G1 G2 G3 G4 --input " + currentInput + " --output " + currentOutput
                    + " --threads 1 --cluster-threshold 0.4 --single-substitution-probability 0.002"
                    + " --single-indel-probability 0.001");
            if (i < 3)
                assertFileNotEquals(currentInput, currentOutput);
            else
                assertFileEquals(currentInput, currentOutput);
        }
        exec("correct --input " + inputFile + " --output " + TEMP_DIR + "correct4.mif --max-total-errors 0"
                + " --groups G1 G2 G3 G4");
        assertFileNotEquals(inputFile, TEMP_DIR + "correct4.mif");
        exec("correct --input " + inputFile + " --output " + TEMP_DIR + "correct5.mif --max-mismatches 0" +
                " --max-indels 0 --groups G1 G2 G3 G4");
        assertFileEquals(TEMP_DIR + "correct4.mif", TEMP_DIR + "correct5.mif");
        assertTrue(new File(inputFile).delete());
        for (int i = 1; i <= 5; i++)
            assertTrue(new File(TEMP_DIR + "correct" + i + ".mif").delete());
    }
}
