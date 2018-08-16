#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd "$SCRIPT_DIR/../milib"
mvn clean install -DskipTests=true
cd ..
mvn clean package shade:shade -Dmaven.test.skip=true
MINNN="$( readlink -f "$( find "$SCRIPT_DIR/../target" -name '*distribution.jar' | head -1 )" )"
java -jar "$MINNN" docs --output "$SCRIPT_DIR/source/reference.rst"
soffice --headless --convert-to svg "$SCRIPT_DIR/source/usage-chart.odg" --outdir "$SCRIPT_DIR/source/_static"
