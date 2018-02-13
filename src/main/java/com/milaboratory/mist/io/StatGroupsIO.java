package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.core.sequence.SequenceQuality;
import com.milaboratory.mist.outputconverter.MatchedGroup;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.util.SmartProgressReporter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.mist.util.SystemUtils.exitWithError;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class StatGroupsIO {
    private final LinkedHashSet<String> groupList;
    private final String inputFileName;
    private final String outputFileName;
    private final long numberOfReads;
    private final byte readQualityFilter;
    private final byte minQualityFilter;
    private final byte avgQualityFilter;
    private final int minCountFilter;
    private final float minFracFilter;
    private final HashMap<StatGroupsKey, StatGroupsValue> statGroups = new HashMap<>();

    public StatGroupsIO(List<String> groupList, String inputFileName, String outputFileName, long numberOfReads,
                        byte readQualityFilter, byte minQualityFilter, byte avgQualityFilter, int minCountFilter,
                        float minFracFilter) {
        this.groupList = new LinkedHashSet<>(groupList);
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.numberOfReads = numberOfReads;
        this.readQualityFilter = readQualityFilter;
        this.minQualityFilter = minQualityFilter;
        this.avgQualityFilter = avgQualityFilter;
        this.minCountFilter = minCountFilter;
        this.minFracFilter = minFracFilter;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;

        try (MifReader reader = createReader()) {
            if (numberOfReads > 0)
                reader.setParsedReadsLimit(numberOfReads);
            SmartProgressReporter.startProgressReport("Processing", reader, System.err);
            for (ParsedRead parsedRead : CUtils.it(reader)) {
                Map<String, MatchedGroup> matchedGroups = parsedRead.getGroups().stream()
                        .collect(Collectors.toMap(MatchedGroup::getGroupName, mg -> mg));
                if (groupList.stream().allMatch(matchedGroups::containsKey)) {
                    List<NSequenceWithQuality> groupValues = groupList.stream()
                            .map(groupName -> matchedGroups.get(groupName).getValue()).collect(Collectors.toList());
                    if (groupValues.stream().allMatch(this::checkQuality)) {
                        StatGroupsKey currentKey = new StatGroupsKey(groupValues);
                        StatGroupsValue currentValue = statGroups.get(currentKey);
                        if (currentValue == null)
                            statGroups.put(currentKey, new StatGroupsValue(groupValues));
                        else
                            currentValue.countNewValue(groupValues);
                    }
                }
                if (++totalReads == numberOfReads)
                    break;
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        HashSet<StatGroupsKey> ignoredKeys = new HashSet<>();
        for (HashMap.Entry<StatGroupsKey, StatGroupsValue> statGroup : statGroups.entrySet()) {
            final float PRECISION = 0.00001f;
            StatGroupsValue value = statGroup.getValue();
            if (((minCountFilter > 0) && (value.count < minCountFilter))
                    || ((minFracFilter > PRECISION) && ((float)(value.count) / totalReads < minFracFilter))
                    || IntStream.range(0, groupList.size())
                        .anyMatch(i -> (((minQualityFilter > 0) && (value.getMinQuality(i) < minQualityFilter))
                                || ((avgQualityFilter > 0) && (value.getAvgQuality(i) < avgQualityFilter)))))
                ignoredKeys.add(statGroup.getKey());
        }

        List<StatGroupsTableLine> table = statGroups.entrySet().stream()
                .filter(entry -> !ignoredKeys.contains(entry.getKey()))
                .map(StatGroupsTableLine::new).sorted().collect(Collectors.toList());

        try (PrintStream writer = createWriter()) {
            writer.println(table.get(0).getHeader());
            table.forEach(line -> writer.println(line.getTableLine()));
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        int countedReadsPercent = (int)((float)table.stream().mapToLong(line -> line.count).sum() / totalReads * 100);
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

    private boolean checkQuality(NSequenceWithQuality seq) {
        return (readQualityFilter == 0) || (seq.getQuality().minValue() >= readQualityFilter);
    }

    private class StatGroupsKey {
        // order in array is the same as in input groupList
        final NucleotideSequence[] sequences = new NucleotideSequence[groupList.size()];

        StatGroupsKey(List<NSequenceWithQuality> groupValues) {
            for (int i = 0; i < groupList.size(); i++)
                sequences[i] = groupValues.get(i).getSequence();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StatGroupsKey that = (StatGroupsKey)o;
            return Arrays.equals(sequences, that.sequences);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(sequences);
        }
    }

    private class StatGroupsValue {
        // order in arrays is the same as in input groupList
        final byte[] minQualities = new byte[groupList.size()];
        final long[] averageQualitySums = new long[groupList.size()];
        final ArrayList<ArrayList<Byte>> minPositionQualities = new ArrayList<>();
        final ArrayList<ArrayList<Long>> positionQualitySums = new ArrayList<>();
        long count;

        StatGroupsValue(List<NSequenceWithQuality> groupValues) {
            for (int i = 0; i < groupList.size(); i++) {
                SequenceQuality currentQuality = groupValues.get(i).getQuality();
                minQualities[i] = currentQuality.minValue();
                averageQualitySums[i] = currentQuality.meanValue();
                ArrayList<Byte> currentMinPositionQualities = new ArrayList<>();
                ArrayList<Long> currentSumPositionQualities = new ArrayList<>();
                for (int j = 0; j < groupValues.get(i).size(); j++) {
                    currentMinPositionQualities.add(currentQuality.value(j));
                    currentSumPositionQualities.add((long)currentQuality.value(j));
                }
                minPositionQualities.add(currentMinPositionQualities);
                positionQualitySums.add(currentSumPositionQualities);
            }
            count = 1;
        }

        void countNewValue(List<NSequenceWithQuality> groupValues) {
            for (int i = 0; i < groupList.size(); i++) {
                SequenceQuality currentQuality = groupValues.get(i).getQuality();
                if (currentQuality.minValue() < minQualities[i])
                    minQualities[i] = currentQuality.minValue();
                averageQualitySums[i] += currentQuality.meanValue();
                ArrayList<Byte> currentMinPositionQualities = minPositionQualities.get(i);
                ArrayList<Long> currentSumPositionQualities = positionQualitySums.get(i);
                for (int j = 0; j < groupValues.get(i).size(); j++) {
                    if (currentQuality.value(j) < currentMinPositionQualities.get(j))
                        currentMinPositionQualities.set(j, currentQuality.value(j));
                    currentSumPositionQualities.set(j, currentSumPositionQualities.get(j) + currentQuality.value(j));
                }
            }
            count++;
        }

        byte getMinQuality(int groupIndex) {
            return minQualities[groupIndex];
        }

        byte getAvgQuality(int groupIndex) {
            return (byte)(averageQualitySums[groupIndex] / count);
        }

        SequenceQuality getMinSequenceQuality(int groupIndex) {
            ArrayList<Byte> currentMinPositionQualities = minPositionQualities.get(groupIndex);
            byte[] quality = new byte[currentMinPositionQualities.size()];
            for (int j = 0; j < currentMinPositionQualities.size(); j++)
                quality[j] = currentMinPositionQualities.get(j);
            return new SequenceQuality(quality);
        }

        SequenceQuality getAvgSequenceQuality(int groupIndex) {
            ArrayList<Long> currentSumPositionQualities = positionQualitySums.get(groupIndex);
            byte[] quality = new byte[currentSumPositionQualities.size()];
            for (int j = 0; j < currentSumPositionQualities.size(); j++)
                quality[j] = (byte)(currentSumPositionQualities.get(j) / count);
            return new SequenceQuality(quality);
        }
    }

    private class StatGroupsTableLine implements Comparable<StatGroupsTableLine> {
        // order in arrays is the same as in input groupList
        final NucleotideSequence[] sequences;
        final SequenceQuality[] minQualities = new SequenceQuality[groupList.size()];
        final SequenceQuality[] avgQualities = new SequenceQuality[groupList.size()];
        final long count;

        StatGroupsTableLine(HashMap.Entry<StatGroupsKey, StatGroupsValue> entry) {
            this.sequences = entry.getKey().sequences;
            StatGroupsValue value = entry.getValue();
            this.count = value.count;
            for (int i = 0; i < groupList.size(); i++) {
                minQualities[i] = value.getMinSequenceQuality(i);
                avgQualities[i] = value.getAvgSequenceQuality(i);
            }
        }

        @Override
        public int compareTo(StatGroupsTableLine that) {
            return Long.compare(that.count, count);     // reversed to start from bigger counts
        }

        String getHeader() {
            StringBuilder header = new StringBuilder();
            for (String groupName : groupList) {
                header.append(groupName).append(".seq ");
                header.append(groupName).append(".qual.min ");
                header.append(groupName).append(".qual.avg ");
            }
            header.append("count");
            return header.toString();
        }

        String getTableLine() {
            StringBuilder line = new StringBuilder();
            for (int groupIndex = 0; groupIndex < groupList.size(); groupIndex++) {
                line.append(sequences[groupIndex]).append(' ');
                line.append(minQualities[groupIndex]).append(' ');
                line.append(avgQualities[groupIndex]).append(' ');
            }
            line.append(count);
            return line.toString();
        }
    }
}
