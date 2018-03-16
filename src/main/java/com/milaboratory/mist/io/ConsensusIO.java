package com.milaboratory.mist.io;

import cc.redberry.pipe.CUtils;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;
import com.milaboratory.mist.outputconverter.MatchedGroup;
import com.milaboratory.mist.outputconverter.ParsedRead;
import com.milaboratory.util.SmartProgressReporter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milaboratory.mist.util.SystemUtils.*;
import static com.milaboratory.util.TimeUtils.nanoTimeToString;

public final class ConsensusIO {
    private final String inputFileName;
    private final String outputFileName;
    private Set<String> groupList;

    public ConsensusIO(List<String> groupList, String inputFileName, String outputFileName) {
        this.groupList = (groupList == null) ? null : new LinkedHashSet<>(groupList);
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
    }

    public void go() {
        long startTime = System.currentTimeMillis();
        long totalReads = 0;
        try (MifReader reader = createReader();
             MifWriter writer = createWriter(reader.getHeader())) {
            SmartProgressReporter.startProgressReport("Calculating consensus", reader, System.err);
            if (!reader.isCorrected())
                System.err.println("WARNING: calculating consensus for not corrected MIF file!");
            if (!reader.isSorted())
                System.err.println("WARNING: calculating consensus for not sorted MIF file; result will be wrong!");
            Set<String> defaultGroups = IntStream.rangeClosed(1, reader.getNumberOfReads())
                    .mapToObj(i -> "R" + i).collect(Collectors.toSet());
            LinkedHashMap<String, NucleotideSequence> previousGroups = null;
            final LinkedHashMap<Byte, ArrayList<NSequenceWithQuality>> currentDataset = new LinkedHashMap<>();
            for (ParsedRead parsedRead : CUtils.it(reader)) {
                parsedRead.getGroups().stream().filter(g -> defaultGroups.contains(g.getGroupName()))
                        .forEach(g -> {
                            currentDataset.computeIfAbsent(g.getTargetId(), id -> new ArrayList<>());
                            currentDataset.get(g.getTargetId()).add(g.getValue());
                        });
                Set<String> allGroups = parsedRead.getGroups().stream().map(MatchedGroup::getGroupName)
                        .filter(groupName -> !defaultGroups.contains(groupName)).collect(Collectors.toSet());
                if (groupList != null) {
                    for (String groupName : groupList)
                        if (!allGroups.contains(groupName))
                            throw exitWithError("Group " + groupName + " not found in the input!");
                } else
                    groupList = allGroups;
                LinkedHashMap<String, NucleotideSequence> currentGroups = parsedRead.getGroups().stream()
                        .filter(g -> groupList.contains(g.getGroupName()))
                        .collect(LinkedHashMap::new, (m, g) -> m.put(g.getGroupName(), g.getValue().getSequence()),
                                Map::putAll);
                if (!currentGroups.equals(previousGroups)) {
                    Set<Byte> targetIds = currentDataset.keySet();
                    for (Byte targetId : targetIds) {
                        ArrayList<NSequenceWithQuality> currentReadSequences = currentDataset.get(targetId);
                    }
                    previousGroups = currentGroups;
                    targetIds.forEach(k -> currentDataset.put(k, new ArrayList<>()));
                }
            }
        } catch (IOException e) {
            throw exitWithError(e.getMessage());
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("\nProcessing time: " + nanoTimeToString(elapsedTime * 1000000));
        System.err.println("Processed " + totalReads + " reads\n");
    }

    private MifReader createReader() throws IOException {
        return (inputFileName == null) ? new MifReader(System.in) : new MifReader(inputFileName);
    }

    private MifWriter createWriter(MifHeader mifHeader) throws IOException {
        return (outputFileName == null) ? new MifWriter(new SystemOutStream(), mifHeader)
                : new MifWriter(outputFileName, mifHeader);
    }
}
