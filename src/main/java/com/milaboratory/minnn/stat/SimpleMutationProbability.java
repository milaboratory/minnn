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
package com.milaboratory.minnn.stat;

import com.milaboratory.core.sequence.*;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import static com.milaboratory.core.mutations.Mutation.*;
import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.stat.StatUtils.*;
import static com.milaboratory.minnn.util.SequencesCache.*;

public final class SimpleMutationProbability implements MutationProbability {
    private final float basicSubstitutionProbability;
    private final float indelProbability;

    public SimpleMutationProbability(float basicSubstitutionProbability, float indelProbability) {
        this.basicSubstitutionProbability = basicSubstitutionProbability;
        this.indelProbability = indelProbability;
    }

    @Override
    public float mutationProbability(NSequenceWithQuality from, NSequenceWithQuality to) {
        if ((from == NSequenceWithQuality.EMPTY) && (to == NSequenceWithQuality.EMPTY))
            throw new IllegalArgumentException("Mutation must not be insertion and deletion in the same time!");
        else if ((from == NSequenceWithQuality.EMPTY) || (to == NSequenceWithQuality.EMPTY))
            return indelProbability;
        else
            return calculateSubstitutionProbability(new ProbabilityDistribution(from),
                    new ProbabilityDistribution(to));
    }

    @Override
    public float mutationProbability(NucleotideSequence from, NucleotideSequence to) {
        throw new IllegalStateException("Mutation probability with quality must be used!");
    }

    @Override
    public float mutationProbability(int mutationCode, byte originalLetterQuality) {
        if (isInDel(mutationCode))
            return indelProbability;
        else {
            ProbabilityDistribution from = new ProbabilityDistribution(getFromSymbol(mutationCode,
                    NucleotideSequence.ALPHABET), originalLetterQuality);
            ProbabilityDistribution to = new ProbabilityDistribution(getToSymbol(mutationCode,
                    NucleotideSequence.ALPHABET), DEFAULT_MAX_QUALITY);
            return calculateSubstitutionProbability(from, to);
        }
    }

    @Override
    public float mutationProbability(int mutationCode) {
        throw new IllegalStateException("Mutation probability with quality must be used!");
    }

    private float calculateSubstitutionProbability(ProbabilityDistribution from, ProbabilityDistribution to) {
        MutableDouble substitutionProbability = new MutableDouble(0);
        from.basicLettersProbabilities.forEachEntry((fromLetter, fromProbability) -> {
            to.basicLettersProbabilities.forEachEntry((toLetter, toProbability) -> {
                double combinationProbability = fromProbability * toProbability;
                if (fromLetter == toLetter)
                    substitutionProbability.value += combinationProbability;
                else
                    substitutionProbability.value += combinationProbability * basicSubstitutionProbability;
                return true;
            });
            return true;
        });
        return (float)(substitutionProbability.value);
    }

    private class MutableDouble {
        double value;

        MutableDouble(double value) {
            this.value = value;
        }
    }

    private class ProbabilityDistribution {
        TObjectDoubleHashMap<NucleotideSequence> basicLettersProbabilities = new TObjectDoubleHashMap<>();

        ProbabilityDistribution(NSequenceWithQuality seq) {
            this(seq.getSequence().symbolAt(0), seq.getQuality().value(0));
        }

        ProbabilityDistribution(char letter, byte letterQuality) {
            Wildcard wildcard = charToWildcard.get(letter);
            double letterProbability = qualityToProbability(letterQuality);
            basicLettersMasks.forEachEntry((basicLetter, mask) -> {
                basicLettersProbabilities.put(basicLetter, ((mask & wildcard.getBasicMask()) == 0)
                        ? (1 - letterProbability) / (basicLettersMasks.size() - wildcard.basicSize())
                        : letterProbability / wildcard.basicSize());
                return true;
            });
        }
    }
}
