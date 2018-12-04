package com.milaboratory.minnn.cli;

import com.milaboratory.cli.BinaryFileInfo;

import static com.milaboratory.minnn.io.MifInfoExtractor.mifInfoExtractor;

public interface MiNNNCommand {
    void throwValidationException(String message, boolean printHelp);

    /** Validate injected parameters and options */
    default void validateInfo(String inputFile) {
        BinaryFileInfo info = mifInfoExtractor.getFileInfo(inputFile);
        if ((info != null) && !info.valid)
            throwValidationException("ERROR: input file \"" + inputFile + "\" is corrupted.", false);
    }
}
