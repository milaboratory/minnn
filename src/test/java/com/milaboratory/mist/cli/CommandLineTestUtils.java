package com.milaboratory.mist.cli;

import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.mist.pattern.SinglePattern;

import static com.milaboratory.mist.cli.Main.main;
import static com.milaboratory.mist.cli.TestResources.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;

public class CommandLineTestUtils {
    public static void exec(String cmdLine) {
        ParsedRead.clearStaticCache();
        try {
            main(cmdLine.split("[ ]+(?=([^\"]*\"[^\"]*\")*[^\"]*$)"));
        } catch (Exception e) {
            throw exitWithError(e.toString());
        }
    }

    public static Void callableExec(String cmdLine) {
        exec(cmdLine);
        return null;
    }

    public static void createRandomMifFile(String fileName) {
        String fastqFile = EXAMPLES_PATH + "small/100.fastq";
        SinglePattern randomPattern = getRandomSinglePattern();
        exec("extract --input " + fastqFile + " --output " + fileName + " --devel-parser-syntax"
                + " --pattern \"" + randomPattern.toString() + "\"");
    }
}
