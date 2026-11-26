#!/bin/sh
DIR=$(cd "$(dirname "$0")" && pwd)
if [ -n "$NETSTORM_JAVA_HOME" ]; then
  JAVA="$NETSTORM_JAVA_HOME/bin/java"
else
  . "$DIR/../../scripts/jdk6.sh"
fi
cd "$DIR/../.." || exit 2
exec "$JAVA" -Djava.awt.headless=true -Dfile.encoding=UTF-8 -cp "$DIR/build/classes:browser/target/swingwebpanel.jar" netstormrun.NetstormRun "$@"
