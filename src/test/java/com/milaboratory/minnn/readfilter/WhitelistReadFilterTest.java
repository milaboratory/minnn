package com.milaboratory.minnn.readfilter;

import com.milaboratory.core.sequence.NucleotideSequence;
import org.junit.*;

import java.util.*;

public class WhitelistReadFilterTest {
    @Test
    public void test1() {
        WhitelistReadFilter whitelistReadFilter = new WhitelistReadFilter("G1",
                Collections.singletonList("WNTTAGA"));
        Set<NucleotideSequence> sequences = whitelistReadFilter.getSequences();
        Assert.assertEquals(2 * 4 + 1, sequences.size());
        Assert.assertTrue(sequences.contains(new NucleotideSequence("AGTTAGA")));
        Assert.assertTrue(sequences.contains(new NucleotideSequence("AATTAGA")));
        Assert.assertFalse(sequences.contains(new NucleotideSequence("CATTAGA")));
    }
}
