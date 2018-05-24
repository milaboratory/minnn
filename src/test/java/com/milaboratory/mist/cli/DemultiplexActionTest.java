package com.milaboratory.mist.cli;

import org.junit.*;

import java.io.File;
import java.util.*;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.cli.TestResources.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;
import static org.junit.Assert.*;

public class DemultiplexActionTest {
    private static final String TEST_FILENAME_PREFIX = "demultiplex_test";

    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "start_" + TEST_FILENAME_PREFIX + ".mif";
        String inputFile = TEMP_DIR + TEST_FILENAME_PREFIX + ".mif";
        String sampleFile4 = EXAMPLES_PATH + "demultiplex_samples/sample4.txt";
        String[] randomFilterOptions = new String[] {
                "--by-barcode G1", "--by-barcode G2", "--by-barcode G1 --by-barcode G2",
                "--by-barcode G2 --by-barcode G1", "--by-sample " + sampleFile4,
                "--by-sample " + sampleFile4 + " --by-barcode G1", "--by-barcode G1 --by-sample " + sampleFile4
        };

        Arrays.stream(getOutputFiles()).map(File::delete).forEach(Assert::assertTrue);
        for (int i = 0; i < 50; i++) {
            String filterOptions = randomFilterOptions[rg.nextInt(randomFilterOptions.length)];
            createRandomMifFile(startFile);
            exec("extract --input-format mif --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:tnncn)(G2:ncnc)\" --bitap-max-errors 0");
            exec("demultiplex " + filterOptions + " --threads " + (rg.nextInt(5) + 1)
                    + " --output-buffer-size " + (rg.nextInt(1 << 17) + 100) + " " + inputFile);
            File[] outputFiles = getOutputFiles();
            int previousNumberOfFiles = outputFiles.length;
            Arrays.stream(outputFiles).map(File::delete).forEach(Assert::assertTrue);
            exec("demultiplex " + filterOptions + " --threads " + (rg.nextInt(5) + 1)
                    + " --output-buffer-size " + (rg.nextInt(1 << 17) + 100) + " " + inputFile);
            outputFiles = getOutputFiles();
            assertEquals(previousNumberOfFiles, outputFiles.length);
            Arrays.stream(outputFiles).map(File::delete).forEach(Assert::assertTrue);
        }
        for (String fileName : new String[] { startFile, inputFile })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String startFile = getExampleMif("twosided");
        String inputFile = TEMP_DIR + TEST_FILENAME_PREFIX + ".mif";
        String sampleFile1 = EXAMPLES_PATH + "demultiplex_samples/sample1.txt";
        String sampleFile2 = EXAMPLES_PATH + "demultiplex_samples/sample2.txt";
        String sampleFile3 = EXAMPLES_PATH + "demultiplex_samples/sample3.txt";
        String sampleFileBad = EXAMPLES_PATH + "demultiplex_samples/bad_sample.txt";

        exec("extract --input-format mif --input " + startFile + " --output " + inputFile
                + " --pattern \"(G1:NNN)&(G2:AANA)\\(G3:ntt)&(G4:nnnn)\" --threads 5");
        Arrays.stream(getOutputFiles()).map(File::delete).forEach(Assert::assertTrue);

        exec("demultiplex " + inputFile + " --by-barcode G1 --by-sample " + sampleFile1 + " --by-barcode G4");
        File[] outputFiles = getOutputFiles();
        assertEquals(4663, outputFiles.length);
        Arrays.stream(outputFiles).map(File::delete).forEach(Assert::assertTrue);

        exec("demultiplex " + inputFile + " --by-sample " + sampleFile2 + " --by-sample " + sampleFile3);
        outputFiles = getOutputFiles();
        assertEquals(16, outputFiles.length);
        Arrays.stream(outputFiles).map(File::delete).forEach(Assert::assertTrue);

        assertException(RuntimeException.class, () -> callableExec("demultiplex " + inputFile + " --by-sample "
                + sampleFileBad));
        for (String fileName : new String[] { startFile, inputFile })
            assertTrue(new File(fileName).delete());
    }

    private static File[] getOutputFiles() {
        return new File(TEMP_DIR).listFiles((dummy, name) -> name.startsWith(TEST_FILENAME_PREFIX + '_'));
    }
}
