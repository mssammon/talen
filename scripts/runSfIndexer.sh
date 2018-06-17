#!/usr/bin/env bash

MODE="PROCESS"
SRC="/shared/corpora/corporaWeb/written/eng/NYT_annotated_corpus/data"
PROC="/shared/preprocessed/resources/NYT-TA-JSON"
IDX="/shared/preprocess/shared/experiments/mssammon/lorelei/2018/nyt-index-2"

MAIN="io.github.mayhewsw.BuildSFIndex"
LIB="target/dependency"
CP="target/classes"


for JAR in `ls $LIB/*jar`; do
    CP="${CP}:$JAR"
done

CMD="java -Xmx32g -cp $CP $MAIN $MODE $SRC $PROC"

echo "$0: running command '$CMD'..."

$CMD

MODE="BUILD"
CMD="nice java -Xmx32g -cp $CP $MAIN $MODE $PROC $IDX"

echo "$0: running command '$CMD'..."

$CMD

echo "$0: done."