package com.milaboratory.minnn.cli;

import org.junit.*;

import java.io.File;

import static com.milaboratory.minnn.cli.CommandLineTestUtils.*;
import static com.milaboratory.minnn.cli.TestResources.*;
import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.SystemUtils.*;

public class SpecialCasesTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void testCase1() throws Exception {
        String inputFile = getExampleMif("twosided-raw");
        String outputFile = TEMP_DIR + "outputTC1.mif";
        exec("extract -f --input " + inputFile + " --output " + outputFile + " --input-format MIF"
                + " --score-threshold -100 --uppercase-mismatch-score -15"
                + " --pattern \"^(UMI:nnnntnnnntnnnn)TCTTGGG\\*\"");
    }

    @Test
    public void testCase2() throws Exception {
        String inputFile = getExampleMif("twosided-raw");
        String outputFile = TEMP_DIR + "outputTC1.mif";
        exec("extract -f --input " + inputFile + " --output " + outputFile + " --input-format MIF"
                + " --score-threshold -100 --uppercase-mismatch-score -15"
                + " --pattern \"^(UMI:nnnntnnnntnnnn)TCTTGGG(R1cut:N{*})\\*\"");
    }

    @Ignore
    @Test
    public void extractWrongData() throws Exception {
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
    }

    @Ignore
    @Test
    public void testWrongData() throws Exception {
        String wrong = TEMP_DIR + "wrong.fastq";
        exec("extract -f --input " + wrong + " --output /dev/null -n 1 --threads 1"
                + " --score-threshold -100 --uppercase-mismatch-score -15 --max-quality-penalty 0"
                + " --pattern \"^(UMI:nnnntnnnntnnnn)TCTTGGG(R1cut:N{*})\"");
    }
}
