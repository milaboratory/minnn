/*
 * Copyright (c) 2016-2018, MiLaboratory LLC
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
package com.milaboratory.minnn.cli;

import com.milaboratory.core.io.sequence.SequenceRead;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.minnn.pattern.GroupEdge;
import com.milaboratory.minnn.pattern.Match;
import com.milaboratory.minnn.pattern.MatchedGroupEdge;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.milaboratory.minnn.util.SystemUtils.exitWithError;

public final class DescriptionGroups {
    private final LinkedHashMap<String, String> cliArgs;
    private final LinkedHashMap<String, GroupPattern> regexPatterns = new LinkedHashMap<>();

    DescriptionGroups(LinkedHashMap<String, String> cliArgs) {
        this.cliArgs = (cliArgs == null) ? new LinkedHashMap<>() : cliArgs;
        for (HashMap.Entry<String, String> entry : this.cliArgs.entrySet())
            regexPatterns.put(entry.getKey(), new GroupPattern(entry.getValue()));
    }

    Set<String> getGroupNames() {
        return regexPatterns.keySet();
    }

    public Match addDescriptionGroups(Match oldBestMatch, SequenceRead originalRead) {
        if (regexPatterns.size() == 0) {
            oldBestMatch.assembleGroups();
            return oldBestMatch;
        }
        ArrayList<MatchedGroupEdge> matchedGroupEdges = oldBestMatch.getMatchedGroupEdges();
        int numberOfTargets = oldBestMatch.getNumberOfPatterns();
        NSequenceWithQuality target = oldBestMatch.getMatchedGroupEdge("R1", true).getTarget();
        for (HashMap.Entry<String, GroupPattern> entry : regexPatterns.entrySet()) {
            String groupName = entry.getKey();
            NSequenceWithQuality seq = null;
            int readId = 0;
            while (seq == null) {
                if (readId == numberOfTargets)
                    throw exitWithError("Regular expression " + cliArgs.get(groupName)
                            + " didn't match nucleotide sequence in any of read descriptions "
                            + seqDescriptionsToString(originalRead));
                Matcher matcher = entry.getValue().pattern.matcher(originalRead.getRead(readId).getDescription());
                if (matcher.find()) {
                    if (entry.getValue().withQuality) {
                        String seqString = matcher.group("seq");
                        String qualString = matcher.group("qual");
                        if ((seqString != null) && (qualString != null)) {
                            try {
                                seq = new NSequenceWithQuality(seqString, qualString);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    } else {
                        String seqString = matcher.group();
                        try {
                            seq = new NSequenceWithQuality(seqString);
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                readId++;
            }
            matchedGroupEdges.add(new MatchedGroupEdge(target, (byte)1,
                    new GroupEdge(groupName, true), seq));
            matchedGroupEdges.add(new MatchedGroupEdge(null, (byte)1,
                    new GroupEdge(groupName, false), null));
        }
        Match newBestMatch = new Match(numberOfTargets, oldBestMatch.getScore(), matchedGroupEdges);
        newBestMatch.assembleGroups();
        return newBestMatch;
    }

    private String seqDescriptionsToString(SequenceRead sequenceRead) {
        StringBuilder stringBuilder = new StringBuilder("[");
        for (int targetId = 1; targetId <= sequenceRead.numberOfReads(); targetId++) {
            if (targetId > 1)
                stringBuilder.append(", ");
            stringBuilder.append("R").append(targetId).append(": ");
            stringBuilder.append(sequenceRead.getRead(targetId - 1).getDescription());
        }
        return stringBuilder.append("]").toString();
    }

    private class GroupPattern {
        final Pattern pattern;
        final boolean withQuality;

        public GroupPattern(String patternStr) {
            if ((patternStr.charAt(0) != '\'') || (patternStr.charAt(patternStr.length() - 1) != '\''))
                throw exitWithError("Missing single quotes around regular expression: " + patternStr);
            patternStr = patternStr.substring(1, patternStr.length() - 1);
            pattern = Pattern.compile(patternStr);
            withQuality = patternStr.contains("?<seq>") && patternStr.contains("?<qual>");
        }
    }
}
