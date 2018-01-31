package com.milaboratory.mist.io;

import com.milaboratory.mist.pattern.GroupEdge;

import java.util.ArrayList;

public class MifHeader {
    private final int numberOfReads;
    private final ArrayList<GroupEdge> groupEdges;

    public MifHeader(int numberOfReads, ArrayList<GroupEdge> groupEdges) {
        this.numberOfReads = numberOfReads;
        this.groupEdges = groupEdges;
    }

    public int getNumberOfReads() {
        return numberOfReads;
    }

    public ArrayList<GroupEdge> getGroupEdges() {
        return groupEdges;
    }
}
