/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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

import static com.milaboratory.minnn.cli.Magic.*;

public final class MifInfoExtractor implements BinaryFileInfoExtractor {
    public static final MifInfoExtractor mifInfoExtractor = new MifInfoExtractor();

    private MifInfoExtractor() {}

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
