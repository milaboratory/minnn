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

import java.io.File;
import java.util.*;

import static com.milaboratory.minnn.io.MifInfoExtractor.mifInfoExtractor;

public interface MiNNNCommand {
    void throwValidationException(String message, boolean printHelp);

    /** Validate injected parameters and options */
    default void validateInfo(String inputFile) {
        BinaryFileInfo info = mifInfoExtractor.getFileInfo(inputFile);
        if ((info != null) && !info.valid)
            throwValidationException("ERROR: input file \"" + inputFile + "\" is corrupted.", false);
    }

    /** Default validation procedure for input and output files */
    default void validate(List<String> inputFileNames, List<String> outputFileNames) {
        for (String in : inputFileNames) {
            if (!new File(in).exists())
                throwValidationException("ERROR: input file \"" + in + "\" does not exist.", false);
            validateInfo(in);
        }
        for (String f : outputFileNames)
            if (new File(f).exists())
                handleExistenceOfOutputFile(f);
    }

    /** This function must be implemented in commands with output and not used in commands without output */
    default void handleExistenceOfOutputFile(String outFileName) {}

    /** Specifies behaviour in the case with output exists (default is to throw exception) */
    default void handleExistenceOfOutputFile(String outFileName, boolean overwrite) {
        if (!overwrite)
            throwValidationException("File \"" + outFileName
                    + "\" already exists. Use -f / --force-overwrite option to overwrite it.", false);
    }
}
