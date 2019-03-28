#!/usr/bin/env bash

source "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -P )"/shell/common.sh

if [[ ! -f ${dir}/milib/pom.xml ]];
then
  echo "Please init git submodules. Try:"
  echo "git submodule update --init --recursive"
  exit 1
fi

function error_exit {
    echo -e "$1"
    exit "${2:-1}"
}

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "Building MiLib."
cd ${dir}/milib
mvn clean install -DskipTests -B || error_exit "${RED}Problem building MiLib!${NC}" 1

echo "Building MiNNN."
cd ${dir}
mvn clean install -DskipTests -B || error_exit "${RED}Problem building MiNNN!${NC}" 1

echo -e "${GREEN}Build successfull!${NC}"
echo "The following is the output of \"minnn -v\" command:"
${dir}/minnn -v || error_exit "${RED}Something went wrong!${NC}" 1
echo -e "${GREEN}Everything seems OK!${NC}"
echo ""
echo "Add minnn script from this folder to your PATH variable or add symlink to your bin folder."
echo ""
echo "MiNNN is free for Academic use. For commercial use please contact licensing@milaboratory.com ."
echo ""
echo "If you discover bugs or have any questions, contact us at https://github.com/milaboratory/minnn/issues ."
