#!/usr/bin/env bash


MODE="BUILD"
PROC="/shared/preprocessed/resources/NYT-TA-JSON"
IDX="/shared/preprocessed/shared/experiments/mssammon/lorelei/2018/nyt-index-2"

MAIN="io.github.mayhewsw.BuildSFIndex"
LIB="target/dependency"
CP="target/classes:config"


for JAR in `ls $LIB/*jar`; do
    CP="${CP}:$JAR"
done


CMD="nice java -Xmx32g -cp $CP $MAIN $MODE $PROC $IDX"

echo "$0: running command '$CMD'..."

$CMD

echo "$0: done."