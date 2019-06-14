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
package com.milaboratory.minnn.correct;

import com.milaboratory.core.sequence.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.minnn.cli.Defaults.*;
import static com.milaboratory.minnn.util.SequencesCache.*;

final class CorrectionUtils {
    private CorrectionUtils() {}

    private static final NucleotideSequence[] letterOptions = new NucleotideSequence[] {
            sequencesCache.get(new NucleotideSequence("A")), sequencesCache.get(new NucleotideSequence("T")),
            sequencesCache.get(new NucleotideSequence("G")), sequencesCache.get(new NucleotideSequence("C")),
            NucleotideSequence.EMPTY };

    static NSequenceWithQuality multipleSequencesMerged(List<NSequenceWithQuality> sequences) {
        int maxLength = sequences.stream().mapToInt(SequenceWithQuality::size).max()
                .orElseThrow(IllegalArgumentException::new);
        NSequenceWithQualityBuilder builder = new NSequenceWithQualityBuilder();
        for (int positionIndex = 0; positionIndex < maxLength; positionIndex++) {
            List<NSequenceWithQuality> currentPositionLetters = new ArrayList<>();
            for (NSequenceWithQuality sequence : sequences) {
                if (sequence.size() > positionIndex) {
                    NSequenceWithQuality currentLetter = sequence.getRange(positionIndex, positionIndex + 1);
                    NucleotideSequence currentLetterSequence = sequencesCache.get(currentLetter.getSequence());
                    byte currentLetterQuality = getLetterQuality(currentLetter);
                    if (currentLetterSequence.containsWildcards()) {
                        Wildcard wildcard = wildcards.get(currentLetterSequence);
                        for (int i = 0; i < wildcard.basicSize(); i++) {
                            NucleotideSequence currentBasicLetter = wildcardCodeToSequence
                                    .get(wildcard.getMatchingCode(i));
                            currentPositionLetters.add(new NSequenceWithQuality(currentBasicLetter, qualityCache
                                    .get((byte)(currentLetterQuality / wildcard.basicSize()))));
                        }
                    } else {
                        currentPositionLetters.add(new NSequenceWithQuality(currentLetterSequence,
                                qualityCache.get(currentLetterQuality)));
                    }
                } else {
                    currentPositionLetters.add(NSequenceWithQuality.EMPTY);
                }
            }
            if (currentPositionLetters.size() < 1)
                throw new IllegalStateException("Illegal state of currentPositionLetters: " + currentPositionLetters
                        + ", sequences: " + sequences);
            else if (currentPositionLetters.size() == 1) {
                builder.append(currentPositionLetters.get(0));
            } else {
                Map<NucleotideSequence, Integer> letterCounts = Arrays.stream(letterOptions)
                        .collect(Collectors.toMap(majorBase -> majorBase,
                                majorBase -> (int)(currentPositionLetters.stream()
                                        .map(SequenceWithQuality::getSequence).filter(majorBase::equals).count())));
                double gamma = 1.0 / (letterOptions.length - 1);
                NucleotideSequence bestLetterSequence = null;
                double bestLetterQuality = -1;

                for (int i = 0; i < 4; i++) {
                    NucleotideSequence letterOption = letterOptions[i];     // don't count for empty option
                    double product = Math.pow(gamma, -letterCounts.get(letterOption));
                    for (NSequenceWithQuality currentLetter : currentPositionLetters) {
                        double errorProbability = Math.pow(10.0, -getLetterQuality(currentLetter) / 10.0);
                        if (currentLetter.getSequence().equals(letterOption))
                            product *= (1 - errorProbability) / Math.max(OVERFLOW_PROTECTION_MIN, errorProbability);
                        else
                            product *= errorProbability / Math.max(OVERFLOW_PROTECTION_MIN,
                                    1 - gamma * errorProbability);
                        product = Math.min(product, OVERFLOW_PROTECTION_MAX);
                    }

                    double errorProbability = 1.0 / (1 + product);
                    double quality = -10 * Math.log10(errorProbability);
                    if (quality > bestLetterQuality) {
                        bestLetterSequence = letterOption;
                        bestLetterQuality = quality;
                    }
                }
                builder.append(new NSequenceWithQuality(bestLetterSequence,
                        qualityCache.get((byte)Math.min(DEFAULT_MAX_QUALITY, bestLetterQuality))));
            }
        }

        return builder.createAndDestroy();
    }

    private static byte getLetterQuality(NSequenceWithQuality letter) {
        return (letter == NSequenceWithQuality.EMPTY) ? DEFAULT_BAD_QUALITY :
                (byte)Math.min(DEFAULT_MAX_QUALITY, letter.getQuality().value(0));
    }
}
