#!/bin/sh
# netstormparse 도구를 JDK 6 으로 실행한다.
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh"
if [ ! -d "$DIR/build/classes" ]; then
  echo "[ERROR] Build first: tools/netstormparse/build.sh" >&2
  exit 2
fi
ENGINE="$DIR/../../engine/target/swingwebpanel-js.jar"
if [ ! -f "$ENGINE" ]; then
  echo "[ERROR] Build the engine first: ./build.sh -pl engine" >&2
  exit 2
fi
exec "$JAVA" -cp "$DIR/build/classes:$ENGINE" netstormparse.NetstormParse "$@"

