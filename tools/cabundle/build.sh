#!/bin/sh
# cabundle 도구를 JDK 6 으로 컴파일한다. (Compile the cabundle tool with JDK 6.)
# JAVA6_HOME 환경 변수로 JDK 6 경로를 바꿀 수 있다. 찾지 못하면 오류로 종료한다.
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh" || exit 1
OUT="$DIR/build/classes"
rm -rf "$OUT"
mkdir -p "$OUT"
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -d "$OUT" "$DIR"/src/cabundle/*.java
exit $?
