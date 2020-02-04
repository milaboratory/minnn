#!/usr/bin/env bash

set -euxo pipefail

prefix=empty_reads_test
input="R1_with_empty_reads.fastq.gz R2_with_empty_reads.fastq.gz"
pattern="(FULL:tggtatcaacgcagagt(G1:n{4}tn{4}tn{4})tct)\(G2:*)"
minnn extract -Xmx900M -f --pattern "$pattern" --input ${input} --output ${prefix}_extracted.mif --not-matched-output ${prefix}_not_matched.mif
minnn sort -Xmx900M -f --groups G1 G2 --input ${prefix}_extracted.mif --output ${prefix}_sorted_G1G2.mif
minnn sort -Xmx900M -f --groups G2 --input ${prefix}_extracted.mif --output ${prefix}_sorted_G2.mif
minnn correct -Xmx900M -f -n 1000 --groups G2 --input ${prefix}_sorted_G2.mif --output ${prefix}_corrected.mif
minnn consensus -Xmx900M -f --groups G1 G2 --min-good-sequence-length 0 --reads-min-good-sequence-length 0 --input ${prefix}_sorted_G1G2.mif --output ${prefix}_consensus1.mif --not-used-reads-output ${prefix}_consensus_not_used1.mif --original-read-stats ${prefix}_consensus_stats1.txt
#minnn consensus-dma -Xmx900M -f --groups G1 G2 --min-good-sequence-length 0 --reads-min-good-sequence-length 0 --input ${prefix}_sorted_G1G2.mif --output ${prefix}_consensus2.mif --not-used-reads-output ${prefix}_consensus_not_used2.mif --original-read-stats ${prefix}_consensus_stats2.txt
minnn consensus -Xmx900M -f --groups G1 G2 --min-good-sequence-length 0 --reads-min-good-sequence-length 0 --consensuses-to-separate-groups --input ${prefix}_sorted_G1G2.mif --output ${prefix}_consensus3.mif --not-used-reads-output ${prefix}_consensus_not_used3.mif --original-read-stats ${prefix}_consensus_stats3.txt
#minnn consensus-dma -Xmx900M -f --groups G1 G2 --min-good-sequence-length 0 --reads-min-good-sequence-length 0 --consensuses-to-separate-groups --input ${prefix}_sorted_G1G2.mif --output ${prefix}_consensus4.mif --not-used-reads-output ${prefix}_consensus_not_used4.mif --original-read-stats ${prefix}_consensus_stats4.txt
minnn mif2fastq -f --input ${prefix}_extracted.mif --group R1=${prefix}_R1.fastq R2=${prefix}_R2.fastq G1=${prefix}_G1.fastq G2=${prefix}_G2.fastq
minnn stat-groups -Xmx900M -f --groups G1 G2 --input ${prefix}_extracted.mif --output ${prefix}_stat_groups.txt
minnn stat-positions -Xmx900M -f --groups G1 G2 --input ${prefix}_extracted.mif --output ${prefix}_stat_positions.txt
minnn decontaminate -Xmx900M -f --primary-groups G2 --groups G1  --input ${prefix}_extracted.mif --output ${prefix}_decontaminated1.mif
minnn decontaminate -Xmx900M -f --primary-groups G1 --groups G2  --input ${prefix}_extracted.mif --output ${prefix}_decontaminated2.mif
minnn filter-by-count -Xmx900M -f --groups G1 G2 --max-unique-barcodes 100 --input ${prefix}_extracted.mif --output ${prefix}_filtered_by_count1.mif --excluded-barcodes-output ${prefix}_filter_by_count_excluded1.mif
minnn filter-by-count -Xmx900M -f --groups G1 G2 --input ${prefix}_extracted.mif --output ${prefix}_filtered_by_count2.mif --excluded-barcodes-output ${prefix}_filter_by_count_excluded2.mif
minnn filter -Xmx900M -f --input ${prefix}_extracted.mif --output ${prefix}_filtered1.mif "AvgGroupQuality(*)=25"
minnn filter -Xmx900M -f --input ${prefix}_extracted.mif --output ${prefix}_filtered2.mif "GroupMaxNFraction(*)=0.1"
#minnn demultiplex -f ${prefix}_extracted.mif --demultiplex-log ${prefix}_demultiplex.log --by-barcode G2
