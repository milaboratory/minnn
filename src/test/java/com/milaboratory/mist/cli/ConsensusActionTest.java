package com.milaboratory.mist.cli;

import org.junit.*;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import static com.milaboratory.mist.cli.CommandLineTestUtils.*;
import static com.milaboratory.mist.cli.Defaults.DEFAULT_GOOD_QUALITY;
import static com.milaboratory.mist.cli.TestResources.*;
import static com.milaboratory.mist.util.CommonTestUtils.*;
import static com.milaboratory.mist.util.SystemUtils.*;
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
        String outputFile4 = TEMP_DIR + "consensusOutput4.mif";
        for (int i = 0; i < 50; i++) {
            createRandomMifFile(startFile);
            String consensusGroups = Arrays.asList(new String[] {"G1", "G2", "G1 G2", "G2 G1"}).get(rg.nextInt(4));
            int width = rg.nextInt(50) + 1;
            int mismatchScore = -rg.nextInt(10) - 1;
            int gapScore = -rg.nextInt(10) - 1;
            exec("extract --input-format mif --input " + startFile + " --output " + inputFile
                    + " --pattern \"(G1:annnt)(G2:NN)\" --bitap-max-errors 0");
            exec("correct --max-mismatches " + rg.nextInt(4) + " --max-indels " + rg.nextInt(4)
                    + " --max-total-errors " + rg.nextInt(5) + " --input " + inputFile
                    + " --output " + correctedFile + " --groups " + consensusGroups);
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
            Stream.of(new String[] { outputFile1, outputFile2 }, new String[] { outputFile2, outputFile3 },
                    new String[] { outputFile3, outputFile4 })
                    .forEach(files -> exec("consensus --input " + files[0] + " --output " + files[1]
                            + " --groups " + consensusGroups + " --threads " + (rg.nextInt(10) + 1)
                            + " --score-threshold 0 --width " + width
                            + " --max-consensuses-per-cluster 100 --skipped-fraction-to-repeat 0.001"
                            + " --reads-avg-quality-threshold 0 --avg-quality-threshold 0 --aligner-match-score 0"
                            + " --aligner-mismatch-score " + mismatchScore + " --aligner-gap-score " + gapScore));
            String parameterValuesMessage = "consensusGroups: " + consensusGroups + ", width: " + width
                    + ", mismatchScore: " + mismatchScore + ", gapScore: " + gapScore;
            assertFileEquals("Files are different with parameter values: " + parameterValuesMessage,
                    outputFile3, outputFile4);
        }
        for (String fileName : new String[] { startFile, inputFile, correctedFile, sortedFile,
                outputFile1, outputFile2, outputFile3, outputFile4 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void preparedMifTest() throws Exception {
        String inputFile = getExampleMif("twosided");
        String correctedFile = TEMP_DIR + "corrected.mif";
        String sortedFile = TEMP_DIR + "sorted.mif";
        String consensusFile = TEMP_DIR + "consensus.mif";
        String consensusFile2 = TEMP_DIR + "consensus2.mif";
        String consensusFile3 = TEMP_DIR + "consensus3.mif";
        exec("correct --input " + inputFile + " --output " + correctedFile + " --groups G3 G4 G1 G2");
        exec("sort --input " + correctedFile + " --output " + sortedFile + " --groups G3 G4 G1 G2 R1 R2");
        exec("consensus --input " + sortedFile + " --output " + consensusFile + " --groups G3 G4 G1"
                + " --threads 5 --score-threshold -1200 --width 30 --max-consensuses-per-cluster 5"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 3");
        exec("consensus --input " + consensusFile + " --output " + consensusFile2
                + " --groups G3 G4 G1 --threads 3 --score-threshold -1200 --width 30 --reads-avg-quality-threshold 0"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 0");
        exec("consensus --input " + consensusFile2 + " --output " + consensusFile3
                + " --groups G3 G4 G1 --threads 3 --score-threshold -1200 --width 30 --reads-avg-quality-threshold 0"
                + " --skipped-fraction-to-repeat 0.75 --avg-quality-threshold 0");
        assertFileEquals(consensusFile2, consensusFile3);
        for (String fileName : new String[] {
                inputFile, correctedFile, sortedFile, consensusFile, consensusFile2, consensusFile3 })
            assertTrue(new File(fileName).delete());
    }

    @Test
    public void qualityOverflowTest() throws Exception {
        String inputFile = getExampleMif("good-quality");
        String consensusFile = TEMP_DIR + "consensus-qual-test.mif";
        exec("consensus --input " + inputFile + " --output " + consensusFile + " --groups G1");
        for (String fileName : new String[] { inputFile, consensusFile })
            assertTrue(new File(fileName).delete());
    }
}
