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
package com.milaboratory.minnn.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import java.io.*;
import java.util.*;

import static com.milaboratory.minnn.util.SystemUtils.*;

final class ReportWriter {
    private ReportWriter() {}

    static void humanReadableReport(String fileName, String fileHeader, String text) {
        System.err.println(text);
        PrintStream reportFileOutputStream = null;
        try {
            if (fileName != null)
                reportFileOutputStream = new PrintStream(new FileOutputStream(fileName));
        } catch (IOException e) {
            throw exitWithError(e.toString());
        }
        if (reportFileOutputStream != null) {
            reportFileOutputStream.print(fileHeader);
            reportFileOutputStream.print(text);
            reportFileOutputStream.close();
        }
    }

    static void jsonReport(String fileName, LinkedHashMap<String, Object> data) {
        PrintStream reportFileOutputStream = null;
        try {
            if (fileName != null)
                reportFileOutputStream = new PrintStream(new FileOutputStream(fileName));
        } catch (IOException e) {
            throw exitWithError(e.toString());
        }
        if (reportFileOutputStream != null) {
            ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
            try {
                reportFileOutputStream.println(writer.writeValueAsString(data));
            } catch (JsonProcessingException e) {
                throw exitWithError(e.toString());
            }
        }
    }
}
