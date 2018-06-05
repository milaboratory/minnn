package com.milaboratory.mist.util;

import org.junit.*;

import java.io.File;
import java.util.concurrent.Callable;

import static com.milaboratory.mist.cli.CommandLineTestUtils.exec;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.DebugUtils.*;
import static org.junit.Assert.*;

public class DebugUtilsTest {
    private final Callable<Void> testCallable = () -> {
        Thread.sleep(10);
        System.out.println("test");
        countCall("call1");
        countEvent("event1");
        return null;
    };
    private final Callable<Object> testObjCallable = () -> {
        Thread.sleep(10);
        countCall("call2");
        countEvent("event1");
        return 35;
    };

    @Before
    public void setUp() {
        resetTimeCounter();
        resetCallCounter();
        resetEventCounter();
    }

    @After
    public void tearDown() {
        resetTimeCounter();
        resetCallCounter();
        resetEventCounter();
    }

    @Test
    public void simpleTest() throws Exception {
        assertOutputContains(false, "test", testCallable);
        assertOutputContains(false, "test", () -> {
            printExecutionTime("label", testCallable); return null; });
        assertOutputContains(true, "ms", () -> {
            printExecutionTime("label", testCallable); return null; });
        countExecutionTime("test1", testCallable);
        assertTrue(timeCounter.get("test1") >= 10);
        assertEquals(35, countExecutionTimeR("test1", testObjCallable));
        assertTrue(timeCounter.get("test1") >= 20);
        assertEquals(4, (long)callCounter.get("call1"));
        assertEquals(1, (long)callCounter.get("call2"));
        assertEquals(5, (long)eventCounter.get("event1"));
    }

    @Test
    public void consensusDebugTest() throws Exception {
        final String STORAGE_PATH = "/mnt/storage/ml/";
        final String TMP_DIR = "/media/user/3795DE7F4B6F1AB4/ml_temp/";
//        String common = "extract --input " + STORAGE_PATH + "data/Rep1_R1.fastq.gz " + STORAGE_PATH
//                + "data/Rep1_R2.fastq.gz --output " + TMP_DIR + "test.mif --pattern ";
//        exec(common + "\"(G1:NNNNNNNNNNNNTCTTGGG)\\*\"");
        String inputFile = STORAGE_PATH + "test_with_G1.mif";
        String correctedFile = TMP_DIR + "corrected.mif";
//        String sortedFile = TMP_DIR + "sorted.mif";
//        String consensusFile = TMP_DIR + "consensus.mif";
        exec("correct --input " + inputFile + " --output " + correctedFile + " --groups G1 --threads 4");
//        exec("sort --input " + correctedFile + " --output " + sortedFile + " --groups G1");
//        exec("consensus --input " + sortedFile + " --output " + consensusFile + " --groups G1"
//                + " --threads 4 --score-threshold -1200 --width 30 --max-consensuses-per-cluster 5"
//                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 4");
    }
}
