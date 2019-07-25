/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact MiLaboratory LLC, which owns exclusive
 * rights for distribution of this program for commercial purposes, using the
 * following email address: licensing@milaboratory.com.
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.minnn.util;

import org.junit.*;

import java.util.concurrent.Callable;

import static com.milaboratory.minnn.util.CommonTestUtils.*;
import static com.milaboratory.minnn.util.DebugUtils.*;
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
