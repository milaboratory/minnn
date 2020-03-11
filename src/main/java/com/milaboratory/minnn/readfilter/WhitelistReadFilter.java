/*
 * Copyright (c) 2016-2020, MiLaboratory LLC
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
package com.milaboratory.minnn.readfilter;

import cc.redberry.combinatorics.Combinatorics;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceBuilder;
import com.milaboratory.core.sequence.Wildcard;
import com.milaboratory.minnn.outputconverter.ParsedRead;

import java.util.*;

import static com.milaboratory.core.sequence.NucleotideSequence.ALPHABET;
import static com.milaboratory.minnn.util.SystemUtils.exitWithError;

public final class WhitelistReadFilter implements ReadFilter {
    private final String groupName;
    private final Set<NucleotideSequence> sequences = new HashSet<>();

    public WhitelistReadFilter(String groupName, Collection<String> whitelistValues) {
        this.groupName = groupName;
        for (String whitelistValue : new HashSet<>(whitelistValues)) {
            NucleotideSequence seq;
            try {
                seq = new NucleotideSequence(whitelistValue);
            } catch (IllegalArgumentException e) {
                throw exitWithError("Wrong whitelist value: " + whitelistValue + "! " + e.getMessage());
            }

            if (seq.containsWildcards())
                saveAllWildcardCombinations(seq);
            // Sequences with wildcards will also be added "as is", so exact match will also be detected
            sequences.add(seq);
        }
    }

    @Override
    public ParsedRead filter(ParsedRead parsedRead) {
        NucleotideSequence seq = getGroupByName(parsedRead, groupName).getValue().getSequence();
        if (sequences.contains(seq))
            return parsedRead;
        else
            return notMatchedRead(parsedRead);
    }

    /**
     * Adds all possible combinations of basic letters that this wildcard matches.
     */
    private void saveAllWildcardCombinations(NucleotideSequence seq) {
        Wildcard[] letters = new Wildcard[seq.size()];
        for (int i = 0; i < seq.size(); i++)
            letters[i] = ALPHABET.codeToWildcard(seq.codeAt(i));
        for (int[] tuple : Combinatorics.tuples(Arrays.stream(letters).mapToInt(Wildcard::basicSize).toArray())) {
            SequenceBuilder<NucleotideSequence> builder = ALPHABET.createBuilder().ensureCapacity(letters.length);
            for (int i = 0; i < tuple.length; i++)
                builder.append(letters[i].getMatchingCode(tuple[i]));
            sequences.add(builder.createAndDestroy());
        }
    }

    protected Set<NucleotideSequence> getSequences() {
        return sequences;
    }
}
