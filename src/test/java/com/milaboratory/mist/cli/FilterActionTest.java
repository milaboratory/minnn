package com.milaboratory.mist.cli;

import org.junit.*;

import java.io.File;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.mist.util.TestSettings.*;
import static org.junit.Assert.*;

public class FilterActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "filterStart.mif";
        String inputFile = TEMP_DIR + "filterInput.mif";
        String outputFile1 = TEMP_DIR + "filterOutput1.mif";
        String outputFile2 = TEMP_DIR + "filterOutput2.mif";
        String outputFile3 = TEMP_DIR + "filterOutput3.mif";
        for (int i = 0; i < 50; i++) {
            String filter = getRandomFilter();
            String fairSorting = rg.nextBoolean() ? "" : " --fair-sorting";
            createRandomMifFile(startFile);
            exec("extract --input-format mif --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:tnacn)(G2:ncnc)\" --bitap-max-errors 0");
            exec("filter --input " + inputFile + " --output " + outputFile1 + fairSorting
                    + " --threads " + (rg.nextInt(5) + 1) + filter);
            exec("filter --input " + outputFile1 + " --output " + outputFile2 + fairSorting
                    + " --threads " + (rg.nextInt(5) + 1) + filter);
            exec("filter --input " + outputFile2 + " --output " + outputFile3 + fairSorting
                    + " --threads " + (rg.nextInt(5) + 1) + filter);
            assertFileEquals(outputFile2, outputFile3);
        }
        for (String fileName : new String[] { startFile, inputFile, outputFile1, outputFile2, outputFile3 })
            assertTrue(new File(fileName).delete());
    }

    private String getRandomFilter() {
        String[] filters = {
                "Len(G1) = 4", "Len(G2)=3", "Len(UMI) =8", "Len(G1) = 5 & Len(G2)=3", "Len(G1)=6|Len(G2)=4",
                "Len(G1)=5|Len(G1)=4&Len(G2)=3|Len(G1)=6", "Len(G1)=5|(Len(G1)=4&Len(G2)=3|Len(G1)=6)",
                "G1~'TAACT' & Len(G2)=3", "G2~'T&nc'", "G1~'CT'&G1~'(GROUP:ta)+[a&t]'",
                "G2~'^TC||[n{2}]$'|(G1~'<{2}Taac'&G2~'(G2:*)'| Len(G2)=5)"
        };
        return " \"" + filters[rg.nextInt(filters.length)] + "\"";
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = EXAMPLES_PATH + "mif/twosided.mif.gz";
        String outputFile1 = TEMP_DIR + "filterOutput1.mif";
        String outputFile2 = TEMP_DIR + "filterOutput2.mif";
        String outputFile3 = TEMP_DIR + "filterOutput3.mif";
        String filter = " \"G1~'AT'|G2~'GGC'&R2~'AANC&TA'|Len(G4)=5\"";
        exec("filter --fair-sorting --input " + inputFile + " --output " + outputFile1 + filter);
        for (String fairSorting : new String[] { "", " --fair-sorting" })
            assertOutputContains(true, "4632", () -> callableExec("filter" + fairSorting
                    + " --input " + inputFile + " --output " + outputFile1 + filter));
        exec("filter --fair-sorting --input " + outputFile1 + " --output " + outputFile2 + filter);
        exec("filter --fair-sorting --input " + outputFile2 + " --output " + outputFile3 + filter);
        assertFileEquals(outputFile2, outputFile3);
        assertException(RuntimeException.class, () -> callableExec("filter --input " + inputFile
                + " G1~'A\\A'"));
        for (String fileName : new String[] { outputFile1, outputFile2, outputFile3 })
            assertTrue(new File(fileName).delete());
    }
}
