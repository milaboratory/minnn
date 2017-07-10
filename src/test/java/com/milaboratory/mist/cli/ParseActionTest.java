package com.milaboratory.mist.cli;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static com.milaboratory.mist.util.CommonTestUtils.inQuotes;
import static com.milaboratory.mist.util.SystemUtils.exitOnError;
import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.mist.util.TestSettings.*;
import static com.milaboratory.mist.Main.main;
import static org.junit.Assert.*;

public class ParseActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEST_OUTPUT_FILES_PATH);
        if (!outputFilesDirectory.exists())
            if (!outputFilesDirectory.mkdir())
                exitWithError("Cannot create directory for output files!");
    }

    @Test
    public void simpleTest() throws Exception {
        String testInputR1 = TEST_INPUT_FILES_PATH + "sample_r1.fastq";
        String testInputR2 = TEST_INPUT_FILES_PATH + "sample_r2.fastq";
        String testOutputR1 = TEST_OUTPUT_FILES_PATH + "output_r1.fastq";
        String testOutputR2 = TEST_OUTPUT_FILES_PATH + "output_r2.fastq";
        String testOutputSingle = TEST_OUTPUT_FILES_PATH + "output_single.fastq";

        String[] args1 = {"parse", "--pattern",
                inQuotes("MultiPattern([FuzzyMatchPattern(GAAGCA, -1, -1, [GroupEdgePosition(" +
                        "GroupEdge('UMI', true), 2), GroupEdgePosition(GroupEdge('UMI', false), 4)]), " +
                        "FuzzyMatchPattern(AA, -1, -1)])"),
                "--input", testInputR1, testInputR2, "--output", testOutputR1, testOutputR2};
        main(args1);
        String[] args2 = {"parse", "--match-score", "0", "--oriented", "--pattern", inQuotes(
                "FuzzyMatchPattern(ATTAGACA, -1, -1)") , "--input", testInputR1, "--output", testOutputSingle};
        main(args2);
    }
}