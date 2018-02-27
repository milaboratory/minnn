package com.milaboratory.mist.io;

import cc.redberry.pipe.OutputPort;
import com.milaboratory.core.io.sequence.SequenceReadUtil;
import com.milaboratory.mist.outputconverter.ParsedRead;

import java.util.concurrent.atomic.AtomicLong;

final class NumberedParsedReadsPort implements OutputPort<ParsedRead> {
    private final OutputPort<ParsedRead> port;
    private AtomicLong readId = new AtomicLong(0);

    NumberedParsedReadsPort(OutputPort<ParsedRead> port) {
        this.port = port;
    }

    @Override
    public ParsedRead take() {
        ParsedRead oldParsedRead = port.take();
        if (oldParsedRead != null)
            return new ParsedRead(SequenceReadUtil.setReadId(readId.getAndIncrement(), oldParsedRead.getOriginalRead()),
                    oldParsedRead.isReverseMatch(), oldParsedRead.getBestMatch());
        else
            return null;
    }
}
