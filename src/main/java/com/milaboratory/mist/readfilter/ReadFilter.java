package com.milaboratory.mist.readfilter;

import com.milaboratory.mist.outputconverter.ParsedRead;

public interface ReadFilter {
    ParsedRead filter(ParsedRead parsedRead);
}
