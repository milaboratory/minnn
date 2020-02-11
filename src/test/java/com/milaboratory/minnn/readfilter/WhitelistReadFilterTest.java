package com.milaboratory.minnn.readfilter;

import com.milaboratory.core.sequence.NucleotideSequence;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class WhitelistReadFilterTest {
    @Test
    public void test1() {
        Set<NucleotideSequence> set = new HashSet<>();
        new WhitelistReadFilter.WildcardSequence(new NucleotideSequence("WNTTAGA")).addAllCombinationsTo(set);
        Assert.assertEquals(2 * 4, set.size());
        Assert.assertTrue(set.contains(new NucleotideSequence("AGTTAGA")));
        Assert.assertTrue(set.contains(new NucleotideSequence("AATTAGA")));
        Assert.assertFalse(set.contains(new NucleotideSequence("CATTAGA")));
    }
}