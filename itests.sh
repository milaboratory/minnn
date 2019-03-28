#!/usr/bin/env bash

set -eo pipefail

source "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -P )"/shell/common.sh

# "Integration" tests for MiNNN

tests=("standard_pipeline" "pipes" "smart_overwrite")

create_standard_results=false
run_tests=false
while [[ $# > 0 ]]; do
    key="$1"
    shift
    case ${key} in
        test)
            run_tests=true
        ;;
        case*)
            tests=("$key")
        ;;
        *)
            echo "Unknown option $key";
            exit 1
        ;;
    esac
done

rm -rf ${dir}/test_target
mkdir ${dir}/test_target
cd ${dir}/test_target
for file in $(ls -1 ../src/test/resources/big/); do
    ln -s ../src/test/resources/big/${file}
done

PATH=${dir}:${PATH}
which minnn
minnn -v

function run_test() {
    cd ${dir}/test_target
    echo "========================"
    echo "Running: $1"
    echo "========================"

    if ../itests/${1}; then
        echo "========================"
        echo "$1 executed successfully"
    else
        echo "========================"
        echo "$1 executed with error"
        touch ${1}.error
    fi
    echo "========================"
}

if [[ ${run_tests} == true ]]; then
    for testName in ${tests[@]}; do
        run_test "${testName}.sh"
    done

    if ls ${dir}/test_target/*.error 1>/dev/null 2>&1; then
        echo "There are tests with errors."
        exit 1
    else
        echo "All tests finished successfully."
        exit 0
    fi
fi
