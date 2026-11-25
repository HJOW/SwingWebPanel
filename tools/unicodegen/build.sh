#!/bin/sh
set -eu
DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
JAVA6_HOME=${JAVA6_HOME:-}
if [ -z "$JAVA6_HOME" ] || [ ! -x "$JAVA6_HOME/bin/javac" ]; then
  echo "[ERROR] JDK 6 not found: $JAVA6_HOME" >&2
  exit 1
fi
rm -rf "$DIR/build/classes"
mkdir -p "$DIR/build/classes"
"$JAVA6_HOME/bin/javac" -encoding UTF-8 -source 1.6 -target 1.6 -d "$DIR/build/classes" "$DIR/src/unicodegen/UnicodeGen.java"
