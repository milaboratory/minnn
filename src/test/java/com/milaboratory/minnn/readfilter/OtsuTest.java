package com.milaboratory.minnn.readfilter;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class OtsuTest {

    @Test
    public void test1() {
        int size1 = 100;
        int[] test1 = new int[size1];
        for (int i = 0; i < size1; i++) {
            test1[i] = i;
        }
        Assert.assertEquals(Otsu.logOtsu(test1, 50), 22);
    }

    @Test
    public void test2() {
        int size2 = 10000;
        int[] test2 = new int[size2];
        for (int i = 0; i < size2; i++) {
            test2[i] = i;
        }
        Assert.assertEquals(Otsu.logOtsu(test2, 101), 2121);
    }

    @Test
    public void test3() {
        int[] test3 = new int[]{240, 197, 79, 198, 101, 46, 207, 134, 123, 108, 178, 136, 147, 83, 29, 80, 279, 94, 229, 207, 288, 60, 168, 32, 277, 43, 129, 199, 36, 2, 91, 102, 247, 174, 198, 56, 198, 151, 132, 92, 264, 11, 95, 33, 231, 182, 58, 223, 233, 191, 80, 55, 97, 25, 119, 246, 208, 61, 109, 111, 227, 21, 163, 220, 289, 93, 103, 226, 23, 63, 78, 120, 181, 176, 151, 269, 266, 56, 148, 25, 271, 117, 294, 202, 12, 127, 131, 77, 214, 122, 79, 127, 264, 81, 201, 33, 9, 64, 19, 154};
        Assert.assertEquals(Otsu.logOtsu(test3, 201), 65);
    }
}