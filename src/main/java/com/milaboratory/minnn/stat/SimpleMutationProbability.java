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

import static com.milaboratory.minnn.util.SequencesCache.*;

public final class SimpleMutationProbability implements MutationProbability {
    private static final byte basicSize = (byte)(NucleotideSequence.ALPHABET.basicSize());
    private static final float badQualityBasicProbability = 1.0f / basicSize;
    private final float basicSubstitutionProbability;
    private final float indelProbability;

    public SimpleMutationProbability(float basicSubstitutionProbability, float indelProbability) {
        this.basicSubstitutionProbability = basicSubstitutionProbability;
        this.indelProbability = indelProbability;
    }

    @Override
    public float mutationProbability(byte from, byte fromQual, byte to, byte toQual) {
        if ((from == -1) && (to == -1))
            throw new IllegalArgumentException("Mutation must not be insertion and deletion in the same time!");
        else if ((from == -1) || (to == -1))
            return indelProbability;
        else {
            float[] fromProbabilities = new float[basicSize];
            float[] toProbabilities = new float[basicSize];

            float fromProbabilitiesSum = 0;
            float toProbabilitiesSum = 0;
            for (int i = 0; i < basicSize; i++) {
                float fromProbability = qualityToLetterProbabilityCache[fromQual];
                Wildcard fromWildcard = NucleotideSequence.ALPHABET.codeToWildcard(from);
                if (fromProbability < badQualityBasicProbability)
                    fromProbability = badQualityBasicProbability;
                else if ((fromWildcard.getBasicMask() & basicLettersMasks[i]) == 0)
                    fromProbability = (1 - fromProbability) / (basicSize - fromWildcard.basicSize());
                else
                    fromProbability = fromProbability / fromWildcard.basicSize();
                fromProbabilities[i] = fromProbability;
                fromProbabilitiesSum += fromProbability;

                float toProbability = qualityToLetterProbabilityCache[toQual];
                Wildcard toWildcard = NucleotideSequence.ALPHABET.codeToWildcard(to);
                if (toProbability < badQualityBasicProbability)
                    toProbability = badQualityBasicProbability;
                else if ((toWildcard.getBasicMask() & basicLettersMasks[i]) == 0)
                    toProbability = (1 - toProbability) / (basicSize - toWildcard.basicSize());
                else
                    toProbability = toProbability / toWildcard.basicSize();
                toProbabilities[i] = toProbability;
                toProbabilitiesSum += toProbability;
            }

            float substitutionProbability = 0;
            for (int i = 0; i < basicSize; i++)
                for (int j = 0; j < basicSize; j++) {
                    float combinationProbability = fromProbabilities[i] * toProbabilities[j]
                            / fromProbabilitiesSum / toProbabilitiesSum;
                    if (from == to)
                        substitutionProbability += combinationProbability;
                    else
                        substitutionProbability += combinationProbability * basicSubstitutionProbability;
                }
            return substitutionProbability;
        }
    }
}
