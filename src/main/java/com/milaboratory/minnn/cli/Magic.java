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
package com.milaboratory.minnn.cli;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.nio.charset.StandardCharsets;

public final class Magic {
    private Magic() {}

    public static final int BEGIN_MAGIC_LENGTH = 14;
    public static final int BEGIN_MAGIC_LENGTH_SHORT = 9;
    public static final String BEGIN_MAGIC_MIF = "MiNNN.MIF";
    private static final int MAGIC_VERSION = 10;
    private static final TIntObjectHashMap<String> MAGIC_VERSIONS = new TIntObjectHashMap<>();
    static {
        for (int i = 1; i <= MAGIC_VERSION; i++)
            MAGIC_VERSIONS.put(i, BEGIN_MAGIC_MIF + ".V" + String.format("%03d", i));
    }
    public static final String BEGIN_MAGIC = MAGIC_VERSIONS.get(MAGIC_VERSION);
    public static final String END_MAGIC = "#MiNNN.File.End#";
    private static final byte[] BEGIN_MAGIC_BYTES = BEGIN_MAGIC.getBytes(StandardCharsets.US_ASCII);
    private static final byte[] END_MAGIC_BYTES = END_MAGIC.getBytes(StandardCharsets.US_ASCII);
    public static final int END_MAGIC_LENGTH = END_MAGIC_BYTES.length;

    public static byte[] getBeginMagicBytes() {
        return BEGIN_MAGIC_BYTES.clone();
    }

    public static byte[] getEndMagicBytes() {
        return END_MAGIC_BYTES.clone();
    }
}
