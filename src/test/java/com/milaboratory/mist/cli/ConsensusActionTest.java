package com.milaboratory.mist.cli;

import org.junit.*;

import java.io.File;
import java.util.Arrays;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.cli.Defaults.DEFAULT_GOOD_QUALITY;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.mist.util.TestSettings.*;
import static org.junit.Assert.*;

public class ConsensusActionTest {
    @BeforeClass
    public static void init() {
        exitOnError = false;
        File outputFilesDirectory = new File(TEMP_DIR);
        if (!outputFilesDirectory.exists())
            throw exitWithError("Directory for temporary output files " + TEMP_DIR + " does not exist!");
    }

    @Test
    public void randomTest() throws Exception {
        String startFile = TEMP_DIR + "consensusStart.mif";
        String inputFile = TEMP_DIR + "consensusInput.mif";
        String correctedFile = TEMP_DIR + "correctCorrected.mif";
        String sortedFile = TEMP_DIR + "correctSorted.mif";
        String outputFile1 = TEMP_DIR + "consensusOutput1.mif";
        String outputFile2 = TEMP_DIR + "consensusOutput2.mif";
        String outputFile3 = TEMP_DIR + "consensusOutput3.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            String consensusGroups = Arrays.asList(new String[] {"G1", "G2", "G1 G2", "G2 G1"}).get(rg.nextInt(4));
            int width = rg.nextInt(50) + 1;
            int mismatchScore = -rg.nextInt(10) - 1;
            int gapScore = -rg.nextInt(10) - 1;
            exec("extract --input-format mif --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            exec("correct --threads " + (rg.nextInt(10) + 1) + " --max-mismatches " + rg.nextInt(4)
                    + " --max-deletions " + rg.nextInt(4) + " --max-total-errors " + rg.nextInt(5)
                    + " --max-insertions " + rg.nextInt(4) + " --input " + inputFile
                    + " --output " + correctedFile);
            exec("sort --input " + correctedFile + " --output " + sortedFile + " --groups " + consensusGroups);
            exec("consensus --input " + sortedFile + " --output " + outputFile1 + " --groups " + consensusGroups
                    + " --threads " + (rg.nextInt(10) + 1) + " --score-threshold " + (rg.nextInt(2000) - 1000)
                    + " --width " + width + " --max-consensuses-per-cluster " + (rg.nextInt(30) + 1)
                    + " --skipped-fraction-to-repeat " + (rg.nextFloat() * 0.8f + 0.1f)
                    + " --reads-avg-quality-threshold " + rg.nextInt(DEFAULT_GOOD_QUALITY)
                    + " --reads-trim-window-size " + (rg.nextInt(15) + 1)
                    + " --reads-min-good-sequence-length " + rg.nextInt(50)
                    + " --avg-quality-threshold " + rg.nextInt(DEFAULT_GOOD_QUALITY)
                    + " --trim-window-size " + (rg.nextInt(15) + 1)
                    + " --min-good-sequence-length " + rg.nextInt(50)
                    + " --aligner-match-score 0 --aligner-mismatch-score " + mismatchScore
                    + " --aligner-gap-score " + gapScore);
            exec("consensus --input " + outputFile1 + " --output " + outputFile2 + " --groups "
                    + consensusGroups + " --threads " + (rg.nextInt(10) + 1) + " --score-threshold 0 --width "
                    + width + " --max-consensuses-per-cluster 100 --skipped-fraction-to-repeat 0.001"
                    + " --reads-avg-quality-threshold 0 --avg-quality-threshold 0 --aligner-match-score 0"
                    + " --aligner-mismatch-score " + mismatchScore + " --aligner-gap-score " + gapScore);
            exec("consensus --input " + outputFile2 + " --output " + outputFile3 + " --groups "
                    + consensusGroups + " --threads " + (rg.nextInt(10) + 1) + " --score-threshold 0 --width "
                    + width + " --max-consensuses-per-cluster 100 --skipped-fraction-to-repeat 0.001"
                    + " --reads-avg-quality-threshold 0 --avg-quality-threshold 0 --aligner-match-score 0"
                    + " --aligner-mismatch-score " + mismatchScore + " --aligner-gap-score " + gapScore);
            assertFileEquals(outputFile2, outputFile3);
        }
        for (String fileName : new String[] { startFile, inputFile, correctedFile, sortedFile,
                outputFile1, outputFile2, outputFile3 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = EXAMPLES_PATH + "mif/twosided.mif.gz";
        String correctedFile = TEMP_DIR + "corrected.mif";
        String sortedFile = TEMP_DIR + "sorted.mif";
        String consensusFile = TEMP_DIR + "consensus.mif";
        String consensusFile2 = TEMP_DIR + "consensus2.mif";
        String consensusFile3 = TEMP_DIR + "consensus3.mif";
        exec("correct --input " + inputFile + " --output " + correctedFile);
        exec("sort --input " + correctedFile + " --output " + sortedFile + " --groups G3 G4 G1 G2 R1 R2");
        exec("consensus --input " + sortedFile + " --output " + consensusFile + " --groups G3 G4 G1"
                + " --threads 5 --score-threshold -1200 --width 30 --max-consensuses-per-cluster 5"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 4");
        exec("consensus --input " + consensusFile + " --output " + consensusFile2
                + " --groups G3 G4 G1 --threads 3 --score-threshold -1200 --width 30 --reads-avg-quality-threshold 0"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 0");
        exec("consensus --input " + consensusFile2 + " --output " + consensusFile3
                + " --groups G3 G4 G1 --threads 3 --score-threshold -1200 --width 30 --reads-avg-quality-threshold 0"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 0");
        assertFileEquals(consensusFile2, consensusFile3);
        for (String fileName : new String[] {
                correctedFile, sortedFile, consensusFile, consensusFile2, consensusFile3 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void specialCaseTest1() throws Exception {
        String inputFile = EXAMPLES_PATH + "mif/consensusSpecialCase1.mif.gz";
        String outputFile = TEMP_DIR + "outputConsensusSCT1.mif";
        exec("consensus --input " + inputFile + " --output " + outputFile + " --score-threshold 4000"
                + " --avg-quality-threshold 0 --reads-avg-quality-threshold 0");
        assertTrue(new File(outputFile).delete());
    }

    @Test
    public void specialCaseTest2() throws Exception {
        String inputFile = EXAMPLES_PATH + "mif/consensusSpecialCase2.mif.gz";
        String outputFile1 = TEMP_DIR + "outputConsensusSCT2-1.mif";
        String outputFile2 = TEMP_DIR + "outputConsensusSCT2-2.mif";
        exec("consensus --input " + inputFile + " --output " + outputFile1 + " --groups G2 G1"
                + " --threads 1 --score-threshold 0 --width 49  --max-consensuses-per-cluster 95"
                + " --skipped-fraction-to-repeat 0.1 --reads-avg-quality-threshold 0 --aligner-match-score 0"
                + " --aligner-mismatch-score -10 --aligner-gap-score -7 --avg-quality-threshold 0");
        exec("consensus --input " + outputFile1 + " --output " + outputFile2 + " --groups G2 G1"
                + " --threads 1 --score-threshold 0 --width 49  --max-consensuses-per-cluster 95"
                + " --skipped-fraction-to-repeat 0.1 --reads-avg-quality-threshold 0 --aligner-match-score 0"
                + " --aligner-mismatch-score -10 --aligner-gap-score -7 --avg-quality-threshold 0");
        assertFileEquals(outputFile1, outputFile2);
        for (String fileName : new String[] { outputFile1, outputFile2 })
            assertTrue(new File(fileName).delete());
    }
}
