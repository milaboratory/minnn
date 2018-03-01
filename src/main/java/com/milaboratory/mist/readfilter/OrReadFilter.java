package com.milaboratory.mist.readfilter;

import com.milaboratory.mist.outputconverter.ParsedRead;

import java.util.*;

public final class OrReadFilter implements ReadFilter {
    private final ReadFilter[] operands;

    public OrReadFilter(ReadFilter... operands) {
        this.operands = operands;
    }

    @Override
    public ParsedRead filter(ParsedRead parsedRead) {
        if (Arrays.stream(operands).map(o -> o.filter(parsedRead).getBestMatch()).allMatch(Objects::isNull))
            return new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(), null);
        else
            return parsedRead;
    }
}
