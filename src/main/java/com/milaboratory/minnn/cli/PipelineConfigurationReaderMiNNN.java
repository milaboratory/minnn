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
package com.milaboratory.minnn.cli;

import com.milaboratory.cli.BinaryFileInfo;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.cli.PipelineConfigurationReader;
import com.milaboratory.primitivio.PrimitivI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.io.IOException;

import static com.milaboratory.minnn.cli.Magic.BEGIN_MAGIC_MIF;
import static com.milaboratory.minnn.io.MifInfoExtractor.*;
import static com.milaboratory.minnn.io.MifReader.readPipelineHeader;
import static com.milaboratory.minnn.util.SystemUtils.*;

public class PipelineConfigurationReaderMiNNN implements PipelineConfigurationReader {
    static final PipelineConfigurationReader pipelineConfigurationReaderInstance =
            new PipelineConfigurationReaderMiNNN();

    protected PipelineConfigurationReaderMiNNN() {}

    /**
     * Read pipeline configuration from file or return null
     */
    @Override
    public PipelineConfiguration fromFileOrNull(String fileName, BinaryFileInfo fileInfo) {
        if (fileInfo == null)
            return null;
        if (!fileInfo.valid)
            return null;
        try {
            return fromFile(fileName, fileInfo);
        } catch (RuntimeException ignored) {}
        return null;
    }

    @Override
    public PipelineConfiguration fromFile(String fileName) {
        BinaryFileInfo fileInfo = mifInfoExtractor.getFileInfo(fileName);
        if (!fileInfo.valid)
            throw exitWithError("File " + fileName + " corrupted.");
        return fromFile(fileName, fileInfo);
    }

    /**
     * Read pipeline configuration from file or throw exception
     */
    @Override
    public PipelineConfiguration fromFile(String fileName, BinaryFileInfo fileInfo) {
        try {
            if (fileInfo == null)
                throw exitWithError("Not a MiNNN file");
            if (BEGIN_MAGIC_MIF.equals(fileInfo.fileType)) {
                try (PrimitivI primitivI = new PrimitivI(new FileInputStream(new File(fileName)))) {
                    return readPipelineHeader(primitivI).getPipelineConfiguration();
                }
            }
            throw exitWithError("Not a MiNNN file");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
