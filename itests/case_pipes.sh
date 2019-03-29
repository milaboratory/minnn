#!/usr/bin/env bash

set -euxo pipefail

prefix=pipes_t1
input="test01_R1.fastq.gz test01_R2.fastq.gz"
minnn extract -n 100000 --input ${input} --pattern "^(UMI:NNNNNNNN)\*" |
    minnn sort -f --groups UMI --output ${prefix}_sorted.mif

prefix=pipes_t2
minnn extract -n 100000 --input ${input} --pattern "^(UMI:NNNNNNNN)\*" |
    minnn sort --groups UMI --output ${prefix}_sorted.mif |
    minnn consensus --groups UMI |
    minnn mif2fastq -f --group R1=${prefix}_R1.fastq R2=${prefix}_R2.fastq
