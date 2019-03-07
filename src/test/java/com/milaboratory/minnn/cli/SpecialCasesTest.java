package com.milaboratory.minnn.cli;

import org.junit.*;

import java.io.File;

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
}
