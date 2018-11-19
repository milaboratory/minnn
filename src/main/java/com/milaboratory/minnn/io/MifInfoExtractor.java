package com.milaboratory.minnn.io;

import com.milaboratory.cli.BinaryFileInfo;
import com.milaboratory.cli.BinaryFileInfoExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public final class MifInfoExtractor implements BinaryFileInfoExtractor {
    public static final MifInfoExtractor mifInfoExtractor = new MifInfoExtractor();
    public static final int BEGIN_MAGIC_LENGTH = 14;
    public static final int BEGIN_MAGIC_LENGTH_SHORT = 10;
    public static final String MAGIC_MIF = "MiNNN.MIF";
    public static final String END_MAGIC = "#MiNNN.File.End#";
    private static final byte[] END_MAGIC_BYTES = END_MAGIC.getBytes(StandardCharsets.US_ASCII);
    public static final int END_MAGIC_LENGTH = END_MAGIC_BYTES.length;

    private MifInfoExtractor() {}

    public static byte[] getEndMagicBytes() {
        return END_MAGIC_BYTES.clone();
    }

    @Override
    public BinaryFileInfo getFileInfo(File file) {
        try {
            Path path = file.toPath();

            if (!Files.isRegularFile(path))
                return null;

            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                if (channel.size() < BEGIN_MAGIC_LENGTH + END_MAGIC_LENGTH)
                    return null;

                byte[] beginMagic = new byte[BEGIN_MAGIC_LENGTH];
                channel.read(ByteBuffer.wrap(beginMagic));
                String magicFull = new String(beginMagic, StandardCharsets.US_ASCII);
                String magicShort = new String(beginMagic, 0, BEGIN_MAGIC_LENGTH_SHORT,
                        StandardCharsets.US_ASCII);

                byte[] endMagic = new byte[END_MAGIC_LENGTH];
                channel.read(ByteBuffer.wrap(endMagic), channel.size() - END_MAGIC_LENGTH);
                return new BinaryFileInfo(magicShort, magicFull, Arrays.equals(endMagic, getEndMagicBytes()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
