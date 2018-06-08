package com.milaboratory.mist.cli;

import java.util.*;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;

public class TestResources {
    public static final String TEST_RESOURCES_PATH = "src/test/resources/";
    public static final String EXAMPLES_PATH = "examples/";
    private static final HashMap<String, ExampleMif> examples = new HashMap<>();
    static {
        examples.put("twosided", new ExampleMif(EXAMPLES_PATH + "twosided/p109_R1.fastq.gz "
                + EXAMPLES_PATH + "twosided/p109_R2.fastq.gz", TEMP_DIR + "twosided.mif"));
        examples.put("twosided-raw", examples.get("twosided"));
        examples.put("singleReadWithG1-G3", new ExampleMif(EXAMPLES_PATH + "small/100.fastq",
                TEMP_DIR + "singleReadWithG1-G3.mif"));
        examples.put("100reads", new ExampleMif(EXAMPLES_PATH + "small/100.fastq",
                TEMP_DIR + "100reads.mif"));
    }

    public static String getExampleMif(String example) throws Exception {
        String common = "extract --input " + examples.get(example).fastq + " --output " + examples.get(example).mif
                + " --mismatch-score -9 --gap-score -10 --single-overlap-penalty -10 --pattern ";
        switch (example) {
            case "twosided":
                exec(common + "\"(G1:tctcag)&(G2:ana)(G3:ggnnc)(G4:NN)\\*\" --score-threshold -20");
                break;
            case "twosided-raw":
                exec(common + "\"*\\*\"");
                break;
            case "singleReadWithG1-G3":
                exec(common + "\"(G1:acNNT)&(G2:NCNC)&(G3:ANca)\"");
                break;
            case "100reads":
                exec(common + "\"*\"");
                break;
            default:
                throw new IllegalArgumentException("Unknown example: " + example);
        }
        return examples.get(example).mif;
    }

    private static class ExampleMif {
        final String fastq;
        final String mif;

        ExampleMif(String fastq, String mif) {
            this.fastq = fastq;
            this.mif = mif;
        }
    }
}
