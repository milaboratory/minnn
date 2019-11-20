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
package com.milaboratory.minnn.readfilter;

import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.Wildcard;
import com.milaboratory.minnn.outputconverter.ParsedRead;

import java.util.*;

import static com.milaboratory.minnn.util.SequencesCache.*;
import static com.milaboratory.minnn.util.SystemUtils.*;

public final class WhitelistReadFilter implements ReadFilter {
    private final String groupName;
    private final Set<NucleotideSequence> sequencesWithoutWildcards = new HashSet<>();
    private final Set<WildcardSequence> sequencesWithWildcards = new HashSet<>();
    private final Set<WildcardSequence> allSequences = new HashSet<>();

    public WhitelistReadFilter(String groupName, Collection<String> whitelistValues) {
        this.groupName = groupName;
        for (String whitelistValue : new HashSet<>(whitelistValues)) {
            NucleotideSequence seq;
            try {
                seq = new NucleotideSequence(whitelistValue);
            } catch (IllegalArgumentException e) {
                throw exitWithError("Wrong whitelist value: " + whitelistValue + "! " + e.getMessage());
            }
            if (seq.containsWildcards()) {
                WildcardSequence wildcardSequence = new WildcardSequence(seq);
                sequencesWithWildcards.add(wildcardSequence);
                allSequences.add(wildcardSequence);
            } else {
                sequencesWithoutWildcards.add(seq);
                allSequences.add(new WildcardSequence(seq));
            }
        }
    }

    @Override
    public ParsedRead filter(ParsedRead parsedRead) {
        NucleotideSequence seq = getGroupByName(parsedRead, groupName).getValue().getSequence();
        if (sequencesWithoutWildcards.contains(seq))
            return parsedRead;
        else {
            for (WildcardSequence whitelistEntry : (seq.containsWildcards() ? allSequences : sequencesWithWildcards))
                if (whitelistEntry.equalsByWildcards(seq))
                    return parsedRead;
            return notMatchedRead(parsedRead);
        }
    }

    private static class WildcardSequence {
        final Wildcard[] letters;

        WildcardSequence(NucleotideSequence seq) {
            this.letters = new Wildcard[seq.size()];
            for (int i = 0; i < seq.size(); i++)
                letters[i] = codeToWildcard[seq.codeAt(i)];
        }

        boolean equalsByWildcards(NucleotideSequence seq) {
            if (letters.length != seq.size())
                return false;
            for (int i = 0; i < letters.length; i++)
                if (!letters[i].intersectsWith(codeToWildcard[seq.codeAt(i)]))
                    return false;
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WildcardSequence that = (WildcardSequence)o;
            return Arrays.equals(letters, that.letters);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(letters);
        }
    }
}
