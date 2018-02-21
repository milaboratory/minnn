package com.milaboratory.mist.cli;

import org.junit.Test;

import java.io.File;
import java.util.*;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.TestSettings.*;
import static org.junit.Assert.*;

public class FastqActionsTest {
    @Test
    public void simpleTest() throws Exception {
        String inputFile = EXAMPLES_PATH + "mif/singleReadWithG1-G3.mif.gz";
        StringBuilder outputArgs = new StringBuilder();
        Arrays.stream(new String[] {"R1", "G1", "G2", "G3"}).forEach(groupName -> outputArgs.append(" --group")
                .append(groupName).append('=').append(TEMP_DIR).append("out_").append(groupName).append(".fastq"));
        String query = "mif2fastq --input " + inputFile + outputArgs + " --copy-old-comments";
        exec(query);
        for (String groupName : new String[] {"R1", "G1", "G2", "G3"}) {
            String fileName = TEMP_DIR + "out_" + groupName + ".fastq";
            assertEquals(400, countLinesInFile(fileName));
            assertTrue(new File(fileName).delete());
        }
    }
}
