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

import java.util.*;

import static com.milaboratory.core.sequence.NucleotideSequence.ALPHABET;

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

    public static boolean equalByWildcards(NucleotideSequence seq1, NucleotideSequence seq2) {
        if (seq1.size() != seq2.size())
            return false;
        for (int i = 0; i < seq1.size(); i++)
            if (!ALPHABET.codeToWildcard(seq1.codeAt(i)).intersectsWith(ALPHABET.codeToWildcard(seq2.codeAt(i))))
                return false;
        return true;
    }

    // storage for sequences that are different by wildcards
    public static class UniqueSequencesSet implements Set<NucleotideSequence> {
        private Set<NucleotideSequence> sequences = new HashSet<>();

        @Override
        public int size() {
            return sequences.size();
        }

        @Override
        public boolean isEmpty() {
            return sequences.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            if (o == null)
                return false;
            if (!(o instanceof NucleotideSequence))
                return false;
            NucleotideSequence newSeq = (NucleotideSequence)o;
            if (sequences.contains(newSeq))
                return true;
            for (NucleotideSequence sequence : sequences)
                if (equalByWildcards(sequence, newSeq))
                    return true;
            return false;
        }

        @Override
        public Iterator<NucleotideSequence> iterator() {
            return sequences.iterator();
        }

        @Override
        public Object[] toArray() {
            return sequences.toArray();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] ts) {
            NucleotideSequence[] sequencesArray = (NucleotideSequence[])ts;
            NucleotideSequence[] result = sequences.toArray(sequencesArray);
            return (T[])result;
        }

        @Override
        public boolean add(NucleotideSequence newSeq) {
            if (newSeq == null)
                throw new NullPointerException();
            if (contains(newSeq))
                return false;
            else
                return sequences.add(newSeq);
        }

        @Override
        public boolean remove(Object o) {
            return sequences.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            for (Object o : collection)
                if (!contains(o))
                    return false;
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends NucleotideSequence> collection) {
            boolean result = false;
            for (NucleotideSequence sequence : collection)
                result |= add(sequence);
            return result;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return sequences.removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            boolean result;
            Set<NucleotideSequence> newSet = new HashSet<>();
            for (NucleotideSequence thisSeq : sequences) {
                if (collection.contains(thisSeq))
                    newSet.add(thisSeq);
                else
                    for (Object thatObj : collection) {
                        if (!(thatObj instanceof NucleotideSequence)) {
                            result = !sequences.equals(new HashSet<>());
                            clear();
                            return result;
                        } else {
                            NucleotideSequence thatSeq = (NucleotideSequence)thatObj;
                            if (equalByWildcards(thisSeq, thatSeq)) {
                                newSet.add(thisSeq);
                                break;
                            }
                        }
                    }
            }
            result = !sequences.equals(newSet);
            sequences = newSet;
            return result;
        }

        @Override
        public void clear() {
            sequences.clear();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UniqueSequencesSet that = (UniqueSequencesSet)o;
            return sequences.equals(that.sequences);
        }

        @Override
        public int hashCode() {
            return sequences.hashCode();
        }
    }
}
