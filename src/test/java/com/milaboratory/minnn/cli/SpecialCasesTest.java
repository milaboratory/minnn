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

    @Test
    public void wrongGroupsTest() throws Exception {
        String inputFileR1 = TEST_RESOURCES_PATH + "sample_r1.fastq.gz";
        String inputFileR2 = TEST_RESOURCES_PATH + "sample_r2.fastq.gz";
        String extracted = TEMP_DIR + "extracted.mif";
        String output = TEMP_DIR + "output.mif";
        exec("extract -f --input " + inputFileR1 + " " + inputFileR2 + " --output " + extracted
                + " --pattern \"*\\*\"");
        assertOutputContains(true, "not found", () -> callableExec("correct -f" +
                " --input " + extracted + " --output " + output + " --groups G"));
        assertOutputContains(true, "not allowed", () -> callableExec("correct -f" +
                " --input " + extracted + " --output " + output + " --groups R1"));
        assertOutputContains(true, "not found", () -> callableExec("sort -f" +
                " --input " + extracted + " --output " + output + " --groups G"));
        exec("sort -f --input " + extracted + " --output " + output + " --groups R1");
        assertOutputContains(true, "not found", () -> callableExec("consensus -f" +
                " --input " + extracted + " --output " + output + " --groups G"));
        assertOutputContains(true, "not allowed", () -> callableExec("consensus -f" +
                " --input " + extracted + " --output " + output + " --groups R1"));
        assertOutputContains(true, "not found", () -> callableExec("stat-groups" +
                " --input " + extracted + " --groups G"));
        exec("stat-groups --input " + extracted + " --groups R1");
        assertOutputContains(true, "not found", () -> callableExec("stat-positions" +
                " --input " + extracted + " --groups G"));
        exec("stat-positions --input " + extracted + " --groups R1");
        assertOutputContains(true, "not found", () -> callableExec("filter -f" +
                " --input " + extracted + " --output " + output + " \"Len(G)=3\""));
        exec("filter -f --input " + extracted + " --output " + output + " \"Len(R1)=3\"");
        for (String fileName : new String[] { extracted, output })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void numberOfReadsChangeTest() throws Exception {
        String suffix = "-special-case-3.mif";
        String inputFile = getExampleMif("twosided-raw");
        String extracted = TEMP_DIR + "extracted" + suffix;
        String corrected = TEMP_DIR + "corrected" + suffix;
        String fastqR1 = TEMP_DIR + "R1.fastq";
        String fastqR2 = TEMP_DIR + "R2.fastq";
        exec("extract -f --input " + inputFile + " --output " + extracted + " --input-format MIF"
                + " --pattern \"(BC:NNNNNNNN)(UMI:NNNNNNNN)\\(R1:*)\"");
        exec("correct -f --input " + extracted + " --output " + corrected + " --groups BC UMI");
        assertOutputContains(true, "Group R2 not found", () -> callableExec("mif2fastq -f " +
                "--input " + corrected + " --group R1=" + fastqR1 + " R2=" + fastqR2));
        exec("mif2fastq -f --input " + corrected + " --group R1=" + fastqR1);
        for (String fileName : new String[] { inputFile, extracted, corrected, fastqR1, fastqR2 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void readsNestedOverrideTest() throws Exception {
        String suffix = "-special-case-4.mif";
        String inputFile = getExampleMif("twosided-raw");
        String extracted = TEMP_DIR + "extracted" + suffix;
        String corrected = TEMP_DIR + "corrected" + suffix;
        String sorted = TEMP_DIR + "sorted" + suffix;
        String consensus = TEMP_DIR + "consensus" + suffix;
        String consensusOrig = TEMP_DIR + "consensusOrig" + suffix;
        String consensusDMA = TEMP_DIR + "consensusDMA" + suffix;
        String fastqR1 = TEMP_DIR + "R1.fastq";
        String fastqR4 = TEMP_DIR + "R4.fastq";
        String fastqR5 = TEMP_DIR + "R5.fastq";
        String fastqUMI = TEMP_DIR + "UMI.fastq";
        exec("extract -f --input " + inputFile + " --output " + extracted + " --input-format MIF"
                + " --pattern \"(UMI:NNNNNNNN)\\gaca(R1:n(R2:nn(R3:nn(R4:(R5:nnnntn)nn)))na)\"");
        exec("correct -f --input " + extracted + " --output " + corrected + " --groups UMI"
                + " --max-errors-share 0.4");
        exec("sort -f --input " + extracted + " --output " + sorted + " --groups R5 R3 UMI");
        exec("consensus -f --input " + sorted + " --output " + consensus + " --groups UMI --kmer-length 4");
        exec("mif2fastq -f --input " + consensus + " --group R5=" + fastqR5 + " R1=" + fastqR1
                + " UMI=" + fastqUMI + " R4=" + fastqR4);
        exec("consensus -f --consensuses-to-separate-groups --input " + sorted
                + " --output " + consensusOrig + " --groups UMI --kmer-length 4");
        exec("mif2fastq -f --input " + consensusOrig + " --group R5=" + fastqR5 + " R1=" + fastqR1
                + " UMI=" + fastqUMI + " CR4=" + fastqR4);
        exec("consensus-dma -f --input " + sorted + " --output " + consensusDMA + " --groups UMI");
        exec("mif2fastq -f --input " + consensusDMA + " --group R5=" + fastqR5 + " R1=" + fastqR1
                + " UMI=" + fastqUMI + " R4=" + fastqR4);
        for (String fileName : new String[] { inputFile, extracted, corrected, sorted, consensus, consensusOrig,
                consensusDMA, fastqR1, fastqR4, fastqR5, fastqUMI })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void wrongOverrideTest() throws Exception {
        String inputFile = getExampleMif("twosided-raw");
        assertOutputContains(true, "patterns (3) and target reads (2)", () -> callableExec(
                "extract -f --input " + inputFile + " --output " + inputFile + " --input-format MIF"
                        + " --pattern \"(UMI:NNNNNNNN)\\atta(R1:gaca)(R2:nnn)\\*\""));
        assertOutputContains(true, "R1 not found", () -> callableExec(
                "extract -f --input " + inputFile + " --output " + inputFile + " --input-format MIF"
                        + " --pattern \"(UMI:NNNNNNNN)\\atta(R2:nnn)\""));
        assertOutputContains(true, "R16 is found, but group R15 is missing", () -> callableExec(
                "extract -f --input " + inputFile + " --output " + inputFile + " --input-format MIF"
                        + " --pattern \"(UMI:NNNNNNNN)(R1:gaca)\\atta(R16:nnn)\""));
        assertTrue(new File(inputFile).delete());
    }
}
