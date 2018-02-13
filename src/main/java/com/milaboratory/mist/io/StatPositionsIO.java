package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import com.milaboratory.core.sequence.*;
import com.milaboratory.mist.outputconverter.*;
import com.milaboratory.mist.pattern.*;
import com.milaboratory.util.SmartProgressReporter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class StatPositionsIO {
    private final LinkedHashSet<String> groupList;
    private final LinkedHashSet<String> readIdList;
    private final boolean outputWithSeq;
    private final String inputFileName;
    private final String outputFileName;
    private final int numberOfReads;
    private final int minCountFilter;
    private final float minFracFilter;
    private final HashMap<StatGroupsKey, Long> statGroups = new HashMap<>();

    public StatPositionsIO(List<String> groupList, List<String> readIdList, boolean outputWithSeq,
                           String inputFileName, String outputFileName, int numberOfReads, int minCountFilter,
                           float minFracFilter) {
        this.groupList = new LinkedHashSet<>(groupList);
        this.readIdList = (readIdList == null) ? null : new LinkedHashSet<>(readIdList);
        this.outputWithSeq = outputWithSeq;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.numberOfReads = numberOfReads;
        this.minCountFilter = minCountFilter;
        this.minFracFilter = minFracFilter;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        long countedReads = 0;

        try (MifReader reader = createReader()) {
            if (numberOfReads > 0)
                reader.setParsedReadsLimit(numberOfReads);
            SmartProgressReporter.startProgressReport("Processing", reader, System.err);
            for (ParsedRead parsedRead : CUtils.it(reader)) {
                boolean readCounted = false;
                Map<String, MatchedGroupEdge> startGroupEdges = parsedRead.getBestMatch().getMatchedGroupEdges()
                        .stream().filter(MatchedGroupEdge::isStart)
                        .collect(Collectors.toMap(MatchedGroupEdge::getGroupName, mge -> mge));
                Map<String, NucleotideSequence> groupValues = outputWithSeq ? parsedRead.getGroups().stream()
                            .collect(Collectors.toMap(MatchedGroup::getGroupName, mg -> mg.getValue().getSequence()))
                        : null;
                for (Map.Entry<String, MatchedGroupEdge> entry : startGroupEdges.entrySet())
                    if (groupList.contains(entry.getKey())) {
                        String readId = getReadId(entry.getValue(), parsedRead.isReverseMatch());
                        if ((readIdList == null) || readIdList.contains(readId)) {
                            readCounted = true;
                            StatGroupsKey currentKey = new StatGroupsKey(entry.getValue(), readId,
                                    outputWithSeq ? groupValues.get(entry.getKey()) : null);
                            Long count = statGroups.get(currentKey);
                            statGroups.put(currentKey, count == null ? 1 : count + 1);
                        }
                    }
                if (readCounted)
                    countedReads++;
                if (++totalReads == numberOfReads)
                    break;
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        ArrayList<StatGroupsTableLine> table = new ArrayList<>();
        for (HashMap.Entry<StatGroupsKey, Long> statGroup : statGroups.entrySet()) {
            final float PRECISION = 0.00001f;
            long count = statGroup.getValue();
            if (((minCountFilter == 0) || (count >= minCountFilter))
                    && ((minFracFilter < PRECISION) || ((float)count / totalReads >= minFracFilter)))
                table.add(new StatGroupsTableLine(statGroup));
        }
        Collections.sort(table);

        try (PrintStream writer = createWriter()) {
            writer.println(table.get(0).getHeader());
            table.forEach(line -> writer.println(line.getTableLine()));
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        long countedReadsPercent = (countedReads * 100) / totalReads;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Checked " + totalReads + " reads");
        System.err.println("Counted reads: " + countedReadsPercent + "% of checked reads\n");
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private PrintStream createWriter() throws IOException {
        return (outputFileName == null) ? new PrintStream(new SystemOutStream())
                : new PrintStream(new FileOutputStream(outputFileName));
    }

    private String getReadId(MatchedGroupEdge matchedGroupEdge, boolean reverseMatch) {
        int readNumber = matchedGroupEdge.getTargetId();
        // this will not work for retargeted .mif files!
        if (reverseMatch)
            if (readNumber == 0)
                readNumber = 1;
            else if (readNumber == 1)
                readNumber = 0;
        return "R" + (readNumber + 1);
    }

    private class StatGroupsKey {
        final String groupName;
        final String readId;
        final int position;
        final NucleotideSequence seq;

        StatGroupsKey(MatchedGroupEdge startGroupEdge, String readId, NucleotideSequence seq) {
            this.groupName = startGroupEdge.getGroupName();
            this.readId = readId;
            this.position = startGroupEdge.getPosition();
            this.seq = seq;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StatGroupsKey that = (StatGroupsKey)o;
            return position == that.position && groupName.equals(that.groupName) && readId.equals(that.readId)
                    && (seq != null ? seq.equals(that.seq) : that.seq == null);
        }

        @Override
        public int hashCode() {
            int result = groupName.hashCode();
            result = 31 * result + readId.hashCode();
            result = 31 * result + position;
            result = 31 * result + (seq != null ? seq.hashCode() : 0);
            return result;
        }
    }

    private class StatGroupsTableLine implements Comparable<StatGroupsTableLine> {
        final String groupId;
        final String readId;
        final int position;
        final long count;
        final NucleotideSequence seq;

        StatGroupsTableLine(HashMap.Entry<StatGroupsKey, Long> entry) {
            this.groupId = entry.getKey().groupName;
            this.readId = entry.getKey().readId;
            this.position = entry.getKey().position;
            this.count = entry.getValue();
            this.seq = entry.getKey().seq;
        }

        @Override
        public int compareTo(StatGroupsTableLine that) {
            return Long.compare(that.count, count);     // reversed to start from bigger counts
        }

        String getHeader() {
            if (outputWithSeq)
                return "group.id read pos count seq";
            else
                return "group.id read pos count";
        }

        String getTableLine() {
            StringBuilder line = new StringBuilder();
            line.append(groupId).append(' ');
            line.append(readId).append(' ');
            line.append(position).append(' ');
            line.append(count);
            if (outputWithSeq)
                line.append(' ').append(seq);
            return line.toString();
        }
    }
}
