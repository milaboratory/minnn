#!/usr/bin/env bash

set -euxo pipefail

prefix=standard_pipeline_test
input="test01_R1.fastq.gz test01_R2.fastq.gz"
pattern="(FULL:tggtatcaacgcagagt(UMI:nnnntnnnntnnnn)tct)\*"
minnn extract -Xmx900M -f --pattern "$pattern" --input ${input} --output ${prefix}_extracted.mif
minnn sort -Xmx900M -f --chunk-size 5000 --groups UMI --input ${prefix}_extracted.mif --output ${prefix}_sorted1.mif
minnn correct -Xmx900M -f --groups UMI --input ${prefix}_sorted1.mif --output ${prefix}_corrected.mif
minnn sort -Xmx900M -f --chunk-size 5000 --groups UMI --input ${prefix}_corrected.mif --output ${prefix}_sorted2.mif
minnn consensus -Xmx900M -f --groups UMI --input ${prefix}_sorted2.mif --output ${prefix}_consensus.mif
minnn mif2fastq -f --input ${prefix}_consensus.mif --group R1=${prefix}_R1.fastq R2=${prefix}_R2.fastq
