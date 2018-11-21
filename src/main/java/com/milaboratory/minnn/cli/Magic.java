package com.milaboratory.minnn.cli;

import java.nio.charset.StandardCharsets;

public final class Magic {
    private Magic() {}

    public static final int BEGIN_MAGIC_LENGTH = 13;
    public static final int BEGIN_MAGIC_LENGTH_SHORT = 10;
    public static final String BEGIN_MAGIC_MIF = "MiNNN.MIF";
    private static final String MAGIC_V01 = BEGIN_MAGIC_MIF + ".V01";
    public static final String BEGIN_MAGIC = MAGIC_V01;
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
