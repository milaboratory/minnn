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

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.io.sequence.*;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.minnn.correct.CorrectionAlgorithms.*;
import com.milaboratory.minnn.outputconverter.ParsedRead;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.minnn.pattern.Match;
import com.milaboratory.minnn.pattern.MatchedGroupEdge;
import gnu.trove.map.hash.TByteObjectHashMap;

import java.util.*;

import static org.junit.Assert.*;

public class CorrectionAlgorithmsTest {


    private static class CorrectionTestData {
        final int numberOfTargets;
        final LinkedHashSet<String> keyGroups;
        final LinkedHashSet<String> primaryGroups;
        final List<TByteObjectHashMap<NSequenceWithQuality>> inputSequences;
        final List<Map<String, GroupCoordinates>> groups;
        final List<TByteObjectHashMap<NSequenceWithQuality>> expectedSequences;

        CorrectionTestData(
                int numberOfTargets, LinkedHashSet<String> keyGroups, LinkedHashSet<String> primaryGroups,
                List<TByteObjectHashMap<NSequenceWithQuality>> inputSequences,
                List<Map<String, GroupCoordinates>> groups,
                List<TByteObjectHashMap<NSequenceWithQuality>> expectedSequences) {
            this.numberOfTargets = numberOfTargets;
            this.keyGroups = keyGroups;
            this.primaryGroups = primaryGroups;
            this.inputSequences = inputSequences;
            this.groups = groups;
            this.expectedSequences = expectedSequences;
        }

        OutputPort<ParsedRead> getInputPort() {
            return new OutputPort<ParsedRead>() {
                int counter = 0;

                @Override
                public synchronized ParsedRead take() {
                    if (counter == inputSequences.size())
                        return null;
                    SequenceRead originalRead;
                    switch (numberOfTargets) {
                        case 1:
                            originalRead = new SingleReadImpl(
                                    counter, inputSequences.get(counter).get((byte)1), "");
                            break;
                        case 2:
                            originalRead = new PairedRead(
                                    new SingleReadImpl(
                                            counter, inputSequences.get(counter).get((byte)1), ""),
                                    new SingleReadImpl(
                                            counter, inputSequences.get(counter).get((byte)2), ""));
                            break;
                        default:
                            SingleRead[] originalReads = new SingleRead[numberOfTargets];
                            for (int i = 0; i < numberOfTargets; i++)
                                originalReads[i] = new SingleReadImpl(
                                        counter, inputSequences.get(counter).get((byte)(i + 1)), "");
                            originalRead = new MultiRead(originalReads);
                    }
                    ArrayList<MatchedGroupEdge> matchedGroupEdges = new ArrayList<>();
                    for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
                        NSequenceWithQuality target = inputSequences.get(counter).get(targetId);
                        String groupName = "R" + targetId;
                        matchedGroupEdges.add(new MatchedGroupEdge(target, targetId,
                                new GroupEdge(groupName, true), 0));
                        matchedGroupEdges.add(new MatchedGroupEdge(target, targetId,
                                new GroupEdge(groupName, false), target.size()));
                    }
                    for (Map.Entry<String, GroupCoordinates> groupEntry : groups.get(counter).entrySet()) {
                        String groupName = groupEntry.getKey();
                        GroupCoordinates groupCoordinates = groupEntry.getValue();
                        NSequenceWithQuality target = inputSequences.get(counter).get(groupCoordinates.targetId);
                        matchedGroupEdges.add(new MatchedGroupEdge(target, groupCoordinates.targetId,
                                new GroupEdge(groupName, true), groupCoordinates.start));
                        matchedGroupEdges.add(new MatchedGroupEdge(target, groupCoordinates.targetId,
                                new GroupEdge(groupName, false), groupCoordinates.end));
                    }
                    Match bestMatch = new Match(numberOfTargets, 0, matchedGroupEdges);
                    counter++;
                    return new ParsedRead(originalRead, false, -1,
                            bestMatch, 0);
                }
            };
        }

        void assertCorrectionResults(List<CorrectBarcodesResult> results) {
            assertEquals(expectedSequences.size(), results.size());
            for (int i = 0; i < results.size(); i++) {
                TByteObjectHashMap<NSequenceWithQuality> actualSequences = new TByteObjectHashMap<>();
                boolean expectedCorrection = false;
                for (byte targetId = 1; targetId <= numberOfTargets; targetId++) {
                    actualSequences.put(targetId, results.get(i).parsedRead.getMatchTarget(targetId));
                    expectedCorrection |= (expectedSequences.get(i).get(targetId).getSequence()
                            != inputSequences.get(i).get(targetId).getSequence());
                }
                assertEquals(expectedSequences.get(i), actualSequences);
                assertEquals(expectedCorrection, results.get(i).corrected);
            }
        }
    }

    private static class GroupCoordinates {
        final byte targetId;
        final int start;
        final int end;

        GroupCoordinates(byte targetId, int start, int end) {
            this.targetId = targetId;
            this.start = start;
            this.end = end;
        }
    }
}
