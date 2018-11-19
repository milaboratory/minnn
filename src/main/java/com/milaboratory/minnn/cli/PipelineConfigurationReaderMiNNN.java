package com.milaboratory.minnn.cli;

import com.milaboratory.cli.BinaryFileInfo;
import com.milaboratory.cli.PipelineConfiguration;
import com.milaboratory.cli.PipelineConfigurationReader;
import com.milaboratory.minnn.io.MifReader;

import static com.milaboratory.minnn.io.MifInfoExtractor.*;

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
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public PipelineConfiguration fromFile(String fileName) {
        BinaryFileInfo fileInfo = mifInfoExtractor.getFileInfo(fileName);
        if (!fileInfo.valid)
            throw new RuntimeException("File " + fileName + " corrupted.");
        return fromFile(fileName, fileInfo);
    }

    /**
     * Read pipeline configuration from file or throw exception
     */
    @Override
    public PipelineConfiguration fromFile(String fileName, BinaryFileInfo fileInfo) {
        try {
            switch (fileInfo.fileType) {
                case MAGIC_MIF:
                    try (MifReader reader = new MifReader(fileName)) {
                        return reader.getPipelineConfiguration();
                    }
                default:
                    throw new RuntimeException("Not a MiNNN file");
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
