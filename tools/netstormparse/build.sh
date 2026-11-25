#!/bin/sh
# netstormparse 도구를 JDK 6 으로 컴파일한다. JAVA6_HOME 으로 경로를 바꿀 수 있다.
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh"
ENGINE="$DIR/../../engine/target/swingwebpanel-js.jar"
if [ ! -f "$ENGINE" ]; then
  echo "[ERROR] Build the engine first: ./build.sh -pl engine" >&2
  exit 2
fi
rm -rf "$DIR/build/classes"
mkdir -p "$DIR/build/classes"
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -cp "$ENGINE" \
  -d "$DIR/build/classes" "$DIR/src/netstormparse/NetstormParse.java"
exit $?

