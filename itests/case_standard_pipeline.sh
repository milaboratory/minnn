#!/usr/bin/env bash

set -euxo pipefail

prefix=standard_pipeline_test
input="test01_R1.fastq.gz test01_R2.fastq.gz"
pattern="(FULL:tggtatcaacgcagagt(UMI:nnnntnnnntnnnn)tct)\*"
minnn extract -Xmx4G -f --pattern "$pattern" --input ${input} --output ${prefix}_extracted.mif
minnn correct -Xmx4G -f --groups UMI --input ${prefix}_extracted.mif --output ${prefix}_corrected.mif
minnn sort -Xmx4G -f --groups UMI --input ${prefix}_corrected.mif --output ${prefix}_sorted.mif
minnn consensus -Xmx4G -f --groups UMI --input ${prefix}_sorted.mif --output ${prefix}_consensus.mif
minnn mif2fastq -f --input ${prefix}_consensus.mif --group R1=${prefix}_R1.fastq R2=${prefix}_R2.fastq
