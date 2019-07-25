/*
 * Copyright (c) 2016-2019, MiLaboratory LLC
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
            List<List<String>> consensusSequences = consensusesToRawSequences(calculatedConsensuses, false);
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
            List<List<String>> consensusSequences = consensusesToRawSequences(calculatedConsensuses, true);
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
