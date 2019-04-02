#!/usr/bin/env bash

set -euxo pipefail

prefix=smart_overwrite_test
input="sample_r1.fastq.gz sample_r2.fastq.gz"
pattern="^(UMI:NNNNNNNN)\*"
minnn extract --pattern "$pattern" --input ${input} --output ${prefix}_extracted.mif
set +eo pipefail
minnn extract --pattern "$pattern" --input ${input} --output ${prefix}_extracted.mif
return_value=$?
echo ${return_value}
if [[ ${return_value} -ne 1 ]]; then
    echo Return value 1 expected, got ${return_value}
    exit 1
fi
minnn extract --pattern "$pattern" --mismatch-score -1 --input ${input} --output ${prefix}_extracted.mif
return_value=$?
echo ${return_value}
if [[ ${return_value} -ne 1 ]]; then
    echo Return value 1 expected, got ${return_value}
    exit 1
fi
set -eo pipefail
minnn extract --overwrite-if-required --pattern "$pattern" --input ${input} --output ${prefix}_extracted.mif
minnn extract --overwrite-if-required --pattern "$pattern" --gap-score -2 --input ${input} \
    --output ${prefix}_extracted.mif
minnn extract -f --pattern "$pattern" --gap-score -2 --input ${input} --output ${prefix}_extracted.mif
