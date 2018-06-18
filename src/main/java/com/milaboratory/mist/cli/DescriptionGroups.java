package com.milaboratory.mist.cli;

import com.milaboratory.core.io.sequence.SequenceRead;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.mist.pattern.GroupEdge;
import com.milaboratory.mist.pattern.Match;
import com.milaboratory.mist.pattern.MatchedGroupEdge;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;

public final class DescriptionGroups {
    private final LinkedHashMap<String, String> cliArgs;
    private final LinkedHashMap<String, GroupPattern> regexPatterns = new LinkedHashMap<>();

    DescriptionGroups(LinkedHashMap<String, String> cliArgs) {
        this.cliArgs = cliArgs;
        for (HashMap.Entry<String, String> entry : cliArgs.entrySet())
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
                if (matcher.matches()) {
                    if (entry.getValue().withQuality) {
                        String seqString = matcher.group("seq");
                        String qualString = matcher.group("qual");
                        if ((seqString != null) && (qualString != null)) {
                            try {
                                seq = new NSequenceWithQuality(seqString, qualString);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    } else {
                        String seqString = matcher.group(0);
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
            pattern = Pattern.compile(patternStr);
            withQuality = patternStr.contains("?<seq>") && patternStr.contains("?<qual>");
        }
    }
}
