package com.milaboratory.minnn.cli;

import org.junit.*;

import java.io.*;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.SystemUtils.*;
import static org.junit.Assert.*;

public class SpecialCasesTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void scoringForMultipleNTest() throws Exception {
        String inputFile = getExampleMif("twosided-raw");
        String file1 = TEMP_DIR + "file1.mif";
        String file2 = TEMP_DIR + "file2.mif";
        String diff = TEMP_DIR + "diff.mif";
        String diff_R1 = TEMP_DIR + "diff_R1.fastq";
        String diff_R2 = TEMP_DIR + "diff_R2.fastq";
        exec("extract -f --input " + inputFile + " --output " + file1 + " --input-format MIF"
                + " --score-threshold -100 --uppercase-mismatch-score -15 --max-quality-penalty 0"
                + " --pattern \"^(UMI:nnnntnnnntnnnn)TCTTGGG\\*\"");
        exec("extract -f --input " + file1 + " --output " + file2 + " --input-format MIF"
                + " --not-matched-output " + diff + " --score-threshold -100 --uppercase-mismatch-score -15"
                + " --pattern \"^(UMI:nnnntnnnntnnnn)TCTTGGG(R1cut:N{*})\\*\" --max-quality-penalty 0");
        exec("mif2fastq -f --input " + diff + " --group R1=" + diff_R1 + " --group R2=" + diff_R2);
        assertEquals(0, new File(diff_R1).length());
        assertEquals(0, new File(diff_R2).length());
        for (String fileName : new String[] { inputFile, file1, file2, diff, diff_R1, diff_R2 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void pipeTest() throws Exception {
        String inputFileR1 = TEST_RESOURCES_PATH + "sample_r1.fastq.gz";
        String inputFileR2 = TEST_RESOURCES_PATH + "sample_r2.fastq.gz";
        String outputFile = TEMP_DIR + "sortedUMI.mif";

        InputStream previousIn = System.in;
        PrintStream previousOut = System.out;
        ByteArrayOutputStream savedStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(savedStream));

        exec("extract -n 7 --input " + inputFileR1 + " " + inputFileR2
                + " --pattern \"^(UMI:NNNNNNNN)\\*\"");

        ByteArrayInputStream pipeInput = new ByteArrayInputStream(savedStream.toByteArray());
        System.setIn(pipeInput);
        System.setOut(previousOut);

        exec("sort -f --groups UMI --output " + outputFile);

        System.setIn(previousIn);

        assertTrue(new File(outputFile).delete());
    }

    @Test
    public void correctionSpeedTest() throws Exception {
        String smallR1 = TEST_RESOURCES_PATH + "sample_r1.fastq.gz";
        String smallR2 = TEST_RESOURCES_PATH + "sample_r2.fastq.gz";
        String bigR1 = TEST_RESOURCES_PATH + "big/kit_data_R1.fastq.gz";
        String bigR2 = TEST_RESOURCES_PATH + "big/kit_data_R2.fastq.gz";
        boolean bigFilesExist = new File(bigR1).exists() && new File(bigR2).exists();
        String inputFileR1 = bigFilesExist ? bigR1 : smallR1;
        String inputFileR2 = bigFilesExist ? bigR2 : smallR2;
        String extractOutput = TEMP_DIR + "extracted.mif";
        String correctOutput = TEMP_DIR + "corrected.mif";
        exec("extract -f --input " + inputFileR1 + " " + inputFileR2 + " --output " + extractOutput
                + " --score-threshold -25 --bitap-max-errors 5"
                + " --pattern \"(FULL:tggtatcaacgcagagt(UMI:nnnntnnnntnnnn)tct)\\*\"");
        exec("correct -f --groups UMI --input " + extractOutput + " --output " + correctOutput
                + " --cluster-threshold 0.3");
        for (String fileName : new String[] { extractOutput, correctOutput })
            assertTrue(new File(fileName).delete());
    }
}
