package com.milaboratory.minnn.consensus.doublemultialign;

import com.milaboratory.minnn.consensus.CalculatedConsensuses;
import com.milaboratory.minnn.consensus.Cluster;
import com.milaboratory.minnn.consensus.ConsensusAlgorithm;
import org.junit.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.consensus.ConsensusAlgorithms.DOUBLE_MULTI_ALIGN;
import static com.milaboratory.minnn.consensus.ConsensusTestData.*;
import static com.milaboratory.minnn.consensus.ConsensusTestUtils.*;
import static org.junit.Assert.*;

public class ConsensusAlgorithmDoubleMultiAlignTest {
    @Test
    public void sequencesTest() throws Exception {
        ConsensusAlgorithm algorithm = createConsensusAlgorithm(DOUBLE_MULTI_ALIGN, 2,
                new HashMap<String, Object>() {{
                    put("READS_MIN_GOOD_SEQ_LENGTH", (byte)4);
                    put("READS_TRIM_WINDOW_SIZE", 3);
                    put("MIN_GOOD_SEQ_LENGTH", (byte)4);
                    put("TRIM_WINDOW_SIZE", 3);
                }});

        for (HashMap.Entry<List<List<String>>, List<List<String>>> testCase : simpleSequencesTestData.entrySet()) {
            Cluster cluster = rawSequencesToCluster(testCase.getKey(), simpleSequencesTestBarcodes);
            CalculatedConsensuses calculatedConsensuses = algorithm.process(cluster);
            List<List<String>> consensusSequences = consensusesToRawSequences(calculatedConsensuses);
            assertEquals(consensusSequences, testCase.getValue());
        }
    }

    @Test
    public void specialCases1() throws Exception {
        ConsensusAlgorithm algorithm = createConsensusAlgorithm(DOUBLE_MULTI_ALIGN, 1,
                null);

        int i = 0;
        for (HashMap.Entry<LinkedHashMap<String, String>, List<String>> entry : specialCaseDataset1.entrySet()) {
            LinkedHashMap<String, String> barcodes = entry.getKey();
            List<String> sequences = entry.getValue();
            Cluster cluster = rawSequencesToCluster(sequences.stream().map(Collections::singletonList)
                    .collect(Collectors.toList()), Collections.singletonList(barcodes));
            CalculatedConsensuses calculatedConsensuses = algorithm.process(cluster);
            List<List<String>> consensusSequences = consensusesToRawSequences(calculatedConsensuses);
            System.out.println("Entry " + i + ", barcodes: " + barcodes + ", total number of sequences: "
                    + sequences.size());
            System.out.println("Calculated consensuses:");
            for (int consensusId = 0; consensusId < consensusSequences.size(); consensusId++) {
                System.out.println("Consensus assembled from "
                        + calculatedConsensuses.consensuses.get(consensusId).consensusReadsNum + " sequences:");
                System.out.println(consensusSequences.get(consensusId).get(0));
            }
            System.out.println();
            i++;
        }
    }
}
