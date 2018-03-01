package com.milaboratory.mist.readfilter;

import com.milaboratory.mist.outputconverter.ParsedRead;

import java.util.*;

public final class AndReadFilter implements ReadFilter {
    private final ReadFilter[] operands;

    public AndReadFilter(ReadFilter... operands) {
        this.operands = operands;
    }

    @Override
    public ParsedRead filter(ParsedRead parsedRead) {
        if (Arrays.stream(operands).map(o -> o.filter(parsedRead).getBestMatch()).anyMatch(Objects::isNull))
            return new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(), null);
        else
            return parsedRead;
    }
}
