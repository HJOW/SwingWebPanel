#!/bin/sh
set -eu
DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
JAVA6_HOME=${JAVA6_HOME:-}
if [ -z "$JAVA6_HOME" ] || [ ! -x "$JAVA6_HOME/bin/java" ]; then
  echo "[ERROR] JDK 6 not found: $JAVA6_HOME" >&2
  exit 1
fi
if [ ! -f "$DIR/build/classes/unicodegen/UnicodeGen.class" ]; then
  JAVA6_HOME=$JAVA6_HOME "$DIR/build.sh"
fi
exec "$JAVA6_HOME/bin/java" -cp "$DIR/build/classes" unicodegen.UnicodeGen "$@"
