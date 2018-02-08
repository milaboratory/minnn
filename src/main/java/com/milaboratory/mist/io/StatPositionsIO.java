package com.milaboratory.mist.io;

import java.util.*;

public final class StatPositionsIO {
    private final List<String> groupList;
    private List<String> readIdList;
    private final boolean outputWithSeq;
    private final String inputFileName;
    private final int numberOfReads;
    private final int minCountFilter;
    private final float minFracFilter;

    public StatPositionsIO(List<String> groupList, List<String> readIdList, boolean outputWithSeq,
                           String inputFileName, int numberOfReads, int minCountFilter, float minFracFilter) {
        this.groupList = groupList;
        this.readIdList = readIdList;
        this.outputWithSeq = outputWithSeq;
        this.inputFileName = inputFileName;
        this.numberOfReads = numberOfReads;
        this.minCountFilter = minCountFilter;
        this.minFracFilter = minFracFilter;
    }

    public void go() {

    }
}
