package com.milaboratory.mist.readfilter;

import com.milaboratory.mist.outputconverter.ParsedRead;

public final class ConsensusReadsReadFilter implements ReadFilter {
    private final int minConsensusReads;

    public ConsensusReadsReadFilter(int minConsensusReads) {
        this.minConsensusReads = minConsensusReads;
    }

    @Override
    public ParsedRead filter(ParsedRead parsedRead) {
        if (parsedRead.getConsensusReads() >= minConsensusReads)
            return parsedRead;
        else
            return new ParsedRead(parsedRead.getOriginalRead(), parsedRead.isReverseMatch(), null,
                    parsedRead.getConsensusReads());
    }
}
