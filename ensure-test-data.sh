#!/usr/bin/env bash

set -eo pipefail

source "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -P )"/shell/common.sh

mkdir -p ${dir}/src/test/resources/big
cd ${dir}/src/test/resources/big

declare -A links=(
    ["test01_R1.fastq.gz"]="https://s3.amazonaws.com/files.milaboratory.com/test-data/test01_R1.fastq.gz"
    ["test01_R2.fastq.gz"]="https://s3.amazonaws.com/files.milaboratory.com/test-data/test01_R2.fastq.gz"
)

for filename in "${!links[@]}"; do
    if [[ ! -f ${filename} ]]; then
        curl -sS -o "${links[${filename}]}" "${filename}"
    fi
done
