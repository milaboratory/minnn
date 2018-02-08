package com.milaboratory.mist.io;

import java.util.*;

public final class StatGroupsIO {
    private final List<String> groupList;
    private final String inputFileName;
    private final int numberOfReads;
    private final byte readQualityFilter;
    private final byte minQualityFilter;
    private final byte avgQualityFilter;
    private final int minCountFilter;
    private final float minFracFilter;

    public StatGroupsIO(List<String> groupList, String inputFileName, int numberOfReads, byte readQualityFilter,
                        byte minQualityFilter, byte avgQualityFilter, int minCountFilter, float minFracFilter) {
        this.groupList = groupList;
        this.inputFileName = inputFileName;
        this.numberOfReads = numberOfReads;
        this.readQualityFilter = readQualityFilter;
        this.minQualityFilter = minQualityFilter;
        this.avgQualityFilter = avgQualityFilter;
        this.minCountFilter = minCountFilter;
        this.minFracFilter = minFracFilter;
    }

    public void go() {

    }
}
