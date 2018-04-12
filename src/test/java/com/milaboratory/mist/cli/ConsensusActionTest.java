package com.milaboratory.mist.cli;

import org.junit.*;

import java.io.File;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.mist.util.TestSettings.*;
import static org.junit.Assert.*;

public class ConsensusActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = EXAMPLES_PATH + "mif/twosided.mif.gz";
        String correctedFile = TEMP_DIR + "corrected.mif";
        String sortedFile = TEMP_DIR + "sorted.mif";
        String consensusFile = TEMP_DIR + "consensus.mif";
        exec("correct --input " + inputFile + " --output " + correctedFile);
        exec("sort --input " + correctedFile + " --output " + sortedFile + " --groups G3 G4 G1 G2 R1 R2");
        exec("consensus --input " + sortedFile + " --output " + consensusFile + " --groups G3 G4 G1"
                + " --threads 4 --penalty-threshold -300");

//        for (String fileName : new String[] { correctedFile, sortedFile, consensusFile })
//            assertTrue(new File(fileName).delete());
    }

}