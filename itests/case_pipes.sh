#!/usr/bin/env bash

trap "exit 1" TERM
export SCRIPT_PID=$$

set -euxo pipefail

assert_return_141() {
    if [[ $1 -ne 141 ]]; then
        >&2 echo Return value 141 expected, got $1
        kill -s TERM $SCRIPT_PID
    fi
}

prefix=fastq_pipe_extract_test
input="test01_R1.fastq.gz"
set +eo pipefail
(zcat ${input} ; assert_return_141 $?) | minnn extract -n 100000 --pattern "^(UMI:NNNNNNNN)" --output ${prefix}_extracted.mif
set -eo pipefail
zcat ${input} | minnn extract -f -n 1000000 --pattern "^(UMI:NNNNNNNN)" --output ${prefix}_extracted.mif
zcat ${input} | minnn extract -f --pattern "^(UMI:NNNNNNNN)" --output ${prefix}_extracted.mif
