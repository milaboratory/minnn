package com.milaboratory.mist.util;

import org.junit.*;

import java.util.concurrent.Callable;

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
}
