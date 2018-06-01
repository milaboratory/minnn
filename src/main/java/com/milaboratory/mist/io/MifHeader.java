package com.milaboratory.mist.io;

import com.milaboratory.mist.pattern.GroupEdge;

import java.util.ArrayList;

public class MifHeader {
    private final int numberOfReads;
    private final ArrayList<String> correctedGroups;
    private final boolean sorted;
    private final ArrayList<GroupEdge> groupEdges;

    public MifHeader(int numberOfReads, ArrayList<String> correctedGroups, boolean sorted,
                     ArrayList<GroupEdge> groupEdges) {
        this.numberOfReads = numberOfReads;
        this.correctedGroups = correctedGroups;
        this.sorted = sorted;
        this.groupEdges = groupEdges;
    }

    public int getNumberOfReads() {
        return numberOfReads;
    }

    public ArrayList<String> getCorrectedGroups() {
        return correctedGroups;
    }

    public boolean isSorted() {
        return sorted;
    }

    public ArrayList<GroupEdge> getGroupEdges() {
        return groupEdges;
    }
}
