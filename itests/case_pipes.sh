#!/usr/bin/env bash

set -euxo pipefail

input="test01_R1.fastq.gz"

prefix=fastq_pipe_extract_test
cat ${input} | minnn extract -n 100000 --pattern "^(UMI:NNNNNNNN)" --output ${prefix}_extracted.mif
