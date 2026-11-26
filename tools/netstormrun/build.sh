#!/bin/sh
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh"
PRODUCT="$DIR/../../browser/target/swingwebpanel.jar"
if [ ! -f "$PRODUCT" ]; then
  echo "Build the product JAR first." >&2
  exit 2
fi
mkdir -p "$DIR/build/classes"
exec "$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -cp "$PRODUCT" -d "$DIR/build/classes" "$DIR/src/netstormrun/NetstormRun.java"
