package com.milaboratory.mist.cli;

import org.junit.*;

import java.io.File;
import java.util.*;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.cli.TestResources.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;
import static org.junit.Assert.*;

public class FastqActionsTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void simpleTest() throws Exception {
        String inputFile = getExampleMif("singleReadWithG1-G3");
        StringBuilder outputArgs = new StringBuilder();
        Arrays.stream(new String[] {"R1", "G1", "G2", "G3"}).forEach(groupName -> outputArgs.append(" --group-")
                .append(groupName).append(' ').append(TEMP_DIR).append("out_").append(groupName).append(".fastq"));
        String query = "mif2fastq --input " + inputFile + outputArgs + " --copy-original-headers";
        exec(query);
        for (String groupName : new String[] {"R1", "G1", "G2", "G3"}) {
            String fileName = TEMP_DIR + "out_" + groupName + ".fastq";
            assertEquals(400, countLinesInFile(fileName));
            assertTrue(new File(fileName).delete());
        }
        assertTrue(new File(inputFile).delete());
    }
}
