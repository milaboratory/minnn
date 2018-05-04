package com.milaboratory.mist.readfilter;

import com.milaboratory.mist.outputconverter.ParsedRead;

public class LenReadFilter implements ReadFilter {
    private final String groupName;
    private final int valueLength;

    public LenReadFilter(String groupName, int valueLength) {
        this.groupName = groupName;
        this.valueLength = valueLength;
    }

    @Override
    public ParsedRead filter(ParsedRead parsedRead) {
        if (parsedRead.getGroups().stream()
                .anyMatch(group -> group.getGroupName().equals(groupName) && group.getValue().size() == valueLength))
            return parsedRead;
        else
            return new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(), null,
                    parsedRead.getConsensusReads());
    }
}
