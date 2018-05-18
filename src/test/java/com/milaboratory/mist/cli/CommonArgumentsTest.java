package com.milaboratory.mist.cli;

import org.junit.*;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;

public class CommonArgumentsTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
    }

    @Test
    public void simpleTest() throws Exception {
        assertOutputContains(true, "Usage:", () -> callableExec(" "));
        assertOutputContains(true, "Usage:", () -> callableExec("-h"));
        assertOutputContains(true, "Usage:", () -> callableExec("--help"));
        assertOutputContains(false, "MiST", () -> callableExec("-v"));
        assertOutputContains(false, "MiST", () -> callableExec("--version"));
    }
}
