#!/usr/bin/env bash

set -eo pipefail

source "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -P )"/shell/common.sh

mkdir -p ${dir}/src/test/resources/big
cd ${dir}/src/test/resources/big

declare -A links=(
    ["test01_R1.fastq.gz"]="https://www.dropbox.com/s/rh7tdvotn3xiqzf/test01_R1.fastq.gz?dl=1"
    ["test01_R2.fastq.gz"]="https://www.dropbox.com/s/sqvluhyr2kon0il/test01_R2.fastq.gz?dl=1"
)

for filename in "${!links[@]}"; do
    if [[ ! -f ${filename} ]]; then
        wget -O ${filename} "${links[${filename}]}"
    fi
done
