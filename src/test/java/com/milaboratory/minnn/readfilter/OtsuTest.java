package com.milaboratory.minnn.readfilter;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class OtsuTest {


    @Test
    public void name() {
        int size2 = 10000;
        int[] test2 = new int[size2];
        for (int i = 0; i < size2; i++) {
            test2[i] = i;
        }
        Assert.assertEquals(Otsu.logOtsu(test2, 101), 2121);

    }
}