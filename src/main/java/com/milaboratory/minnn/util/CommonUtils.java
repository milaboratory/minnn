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
package com.milaboratory.minnn.util;

import com.milaboratory.core.sequence.NucleotideSequence;

public final class CommonUtils {
    private CommonUtils() {}

    public static String stripQuotes(String str) {
        return str.replaceAll("^\"|\"$", "");
    }

    public static int calculateLevenshteinDistance(NucleotideSequence seq1, NucleotideSequence seq2) {
        int arraySize1 = seq1.size() + 1;
        int arraySize2 = seq2.size() + 1;

        // the array of distances
        int[] cost = new int[arraySize1];
        int[] newCost = new int[arraySize1];

        // initial cost of skipping prefix in seq1
        for (int i = 0; i < arraySize1; i++)
            cost[i] = i;

        // transformation cost for each letter in seq2
        for (int j = 1; j < arraySize2; j++) {
            // initial cost of skipping prefix in seq2
            newCost[0] = j;

            // transformation cost for each letter in seq1
            for (int i = 1; i < arraySize1; i++) {
                // matching current letters in both strings
                int match = (seq1.codeAt(i - 1) == seq2.codeAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int costReplace = cost[i - 1] + match;
                int costInsert  = cost[i] + 1;
                int costDelete  = newCost[i - 1] + 1;

                // keep minimum cost
                newCost[i] = Math.min(Math.min(costInsert, costDelete), costReplace);
            }

            int[] tmp = cost;
            cost = newCost;
            newCost = tmp;
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[arraySize1 - 1];
    }
}
