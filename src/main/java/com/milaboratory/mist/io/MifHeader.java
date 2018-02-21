package com.milaboratory.mist.io;

import com.milaboratory.mist.pattern.GroupEdge;

import java.util.ArrayList;

public class MifHeader {
    private final int numberOfReads;
    private final boolean corrected;
    private final ArrayList<GroupEdge> groupEdges;

    public MifHeader(int numberOfReads, boolean corrected, ArrayList<GroupEdge> groupEdges) {
        this.numberOfReads = numberOfReads;
        this.corrected = corrected;
        this.groupEdges = groupEdges;
    }

    public int getNumberOfReads() {
        return numberOfReads;
    }

    public boolean isCorrected() {
        return corrected;
    }

    public ArrayList<GroupEdge> getGroupEdges() {
        return groupEdges;
    }
}
