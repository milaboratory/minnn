#!/usr/bin/env bash

set -euxo pipefail

input="test01_R1.fastq.gz test01_R2.fastq.gz"

prefix=pipes_extract_test1
minnn extract -n 100000 --input ${input} --pattern "^(UMI:NNNNNNNN)\*" |
    minnn sort -f --groups UMI --output ${prefix}_sorted.mif

prefix=pipes_extract_test2
minnn extract -n 100000 --input ${input} --pattern "^(UMI:NNNNNNNN)\*" |
    minnn sort --groups UMI | minnn consensus --groups UMI |
    minnn mif2fastq -f --group R1=${prefix}_R1.fastq R2=${prefix}_R2.fastq

prefix=pipes_filter_test1
minnn extract --input ${input} --pattern "^(UMI:NNNNNNNN)\*" --output ${prefix}_extracted.mif
minnn filter -n 10000 --input ${prefix}_extracted.mif "MinGroupQuality(UMI)=7 & AvgGroupQuality(*)=15" |
    minnn sort --groups UMI --output ${prefix}_sorted.mif

prefix=pipes_correct_test1
minnn extract --input ${input} --pattern "^(UMI:NNNNNNNN)\*" --output ${prefix}_extracted.mif
minnn sort --input --input ${prefix}_extracted.mif --output ${prefix}_sorted1.mif --groups UMI
minnn correct -n 10000 --input ${prefix}_sorted1.mif --groups UMI |
    minnn sort --groups UMI --output ${prefix}_sorted2.mif
