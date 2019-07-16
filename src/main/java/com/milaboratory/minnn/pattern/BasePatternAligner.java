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
package com.milaboratory.minnn.pattern;

import com.milaboratory.core.Range;
import com.milaboratory.core.alignment.*;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequenceCaseSensitive;

public class BasePatternAligner implements PatternAligner {
    @Override
    public Alignment<NucleotideSequenceCaseSensitive> align(
            PatternConfiguration conf, NucleotideSequenceCaseSensitive pattern, NSequenceWithQuality target,
            int rightMatchPosition) {
        if (conf.leftBorder == -1) {
            return PatternAndTargetAligner.alignLeftAdded(conf.scoring, pattern, target, rightMatchPosition,
                    conf.bitapMaxErrors);
        } else {
            Range targetRange = new Range(conf.leftBorder, rightMatchPosition + 1);
            NSequenceWithQuality targetPart = new NSequenceWithQuality(target.getSequence().getRange(targetRange),
                    target.getQuality().getRange(targetRange));
            Alignment<NucleotideSequenceCaseSensitive> partAlignment = PatternAndTargetAligner.alignGlobal(
                    conf.scoring, pattern, targetPart);
            return new Alignment<>(pattern, partAlignment.getAbsoluteMutations(),
                    partAlignment.getSequence1Range(), partAlignment.getSequence2Range().move(conf.leftBorder),
                    partAlignment.getScore());
        }
    }

    @Override
    public long overlapPenalty(
            PatternConfiguration conf, NSequenceWithQuality target, int overlapOffset, int overlapLength) {
        return conf.singleOverlapPenalty * overlapLength;
    }

    @Override
    public long insertionPenalty(
            PatternConfiguration conf, NSequenceWithQuality target, int insertionOffset, int insertionLength) {
        return conf.singleOverlapPenalty * insertionLength;
    }
}
