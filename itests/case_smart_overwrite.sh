#!/usr/bin/env bash

set -euxo pipefail

assert_return_1() {
    if [[ $1 -ne 1 ]]; then
        echo Return value 1 expected, got $1
        exit 1
    fi
}

prefix=smart_overwrite_test
input="sample_r1.fastq.gz sample_r2.fastq.gz"
pattern="^(UMI:NNNNNNNN)\*"
minnn extract --pattern "$pattern" --input ${input} --output ${prefix}_extracted.mif
minnn sort --groups UMI --input ${prefix}_extracted.mif --output ${prefix}_sorted.mif
set +eo pipefail
minnn extract --pattern "$pattern" --input ${input} --output ${prefix}_extracted.mif
assert_return_1 $?
minnn extract --pattern "$pattern" --mismatch-score -1 --input ${input} --output ${prefix}_extracted.mif
assert_return_1 $?
set -eo pipefail
minnn extract --overwrite-if-required --pattern "$pattern" --input ${input} --output ${prefix}_extracted.mif
minnn extract --overwrite-if-required --pattern "$pattern" --gap-score -2 --input ${input} \
    --output ${prefix}_extracted.mif
minnn extract -f --pattern "$pattern" --gap-score -2 --input ${input} --output ${prefix}_extracted.mif
minnn sort --overwrite-if-required --groups UMI --input ${prefix}_extracted.mif --output ${prefix}_sorted.mif
