#!/bin/sh
# sourcecheck 도구를 JDK 6 으로 컴파일한다. (JAVA6_HOME 으로 경로 지정 가능, 못 찾으면 오류 종료)
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh"
rm -rf "$DIR/build/classes"
mkdir -p "$DIR/build/classes"
cd "$DIR" || exit 1
find src -name '*.java' | sort > build/sources.txt
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -d build/classes @build/sources.txt
exit $?
