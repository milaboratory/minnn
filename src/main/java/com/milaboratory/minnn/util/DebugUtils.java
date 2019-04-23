/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
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

import java.util.HashMap;
import java.util.concurrent.Callable;

public final class DebugUtils {
    private DebugUtils() {}

    public static HashMap<String, Long> timeCounter = new HashMap<>();
    public static HashMap<String, Long> callCounter = new HashMap<>();
    public static HashMap<String, Long> eventCounter = new HashMap<>();

    public static void printExecutionTime(String label, Callable<Void> f) {
        long startTime = System.currentTimeMillis();
        try {
            f.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println(label + ": " + elapsedTime + " ms");
    }

    public static void countExecutionTime(String label, Callable<Void> f) {
        long startTime = System.currentTimeMillis();
        try {
            f.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        Long totalElapsedTime = timeCounter.get(label);
        timeCounter.put(label, (totalElapsedTime == null) ? elapsedTime : totalElapsedTime + elapsedTime);
    }

    public static Object countExecutionTimeR(String label, Callable<Object> f) {
        long startTime = System.currentTimeMillis();
        Object o;
        try {
            o = f.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        Long totalElapsedTime = timeCounter.get(label);
        timeCounter.put(label, (totalElapsedTime == null) ? elapsedTime : totalElapsedTime + elapsedTime);

        return o;
    }

    public static void countCall(String label) {
        Long count = callCounter.get(label);
        callCounter.put(label, (count == null) ? 1 : count + 1);
    }

    public static void countEvent(String label) {
        Long count = eventCounter.get(label);
        eventCounter.put(label, (count == null) ? 1 : count + 1);
    }

    public static void resetTimeCounter() {
        timeCounter = new HashMap<>();
    }

    public static void resetCallCounter() {
        callCounter = new HashMap<>();
    }

    public static void resetEventCounter() {
        eventCounter = new HashMap<>();
    }

    public static class NotImplementedException extends RuntimeException {}
}
