package com.milaboratory.mist.readfilter;

import com.milaboratory.mist.outputconverter.ParsedRead;

import java.util.*;

public final class OrReadFilter implements ReadFilter {
    private final List<ReadFilter> operands;

    public OrReadFilter(List<ReadFilter> operands) {
        this.operands = operands;
    }

    @Override
    public ParsedRead filter(ParsedRead parsedRead) {
        if (operands.stream().map(o -> o.filter(parsedRead).getBestMatch()).allMatch(Objects::isNull))
            return new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(), null,
                    parsedRead.getConsensusReads());
        else
            return parsedRead;
    }
}
