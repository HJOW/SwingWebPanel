#!/bin/sh
# tlsvectors 도구를 실행한다. (Run the tlsvectors tool.)
# 실행 JRE 는 TLSVECTORS_JAVA_HOME 으로 지정한다(Java 11 이상). JRE 6 에는 필요한 알고리즘이 없으므로 대신 쓰지 않는다.
DIR=$(cd "$(dirname "$0")" && pwd)
if [ -z "$TLSVECTORS_JAVA_HOME" ]; then
  echo "[ERROR] Set TLSVECTORS_JAVA_HOME to a JRE of Java 11 or newer." >&2
  exit 1
fi
if [ ! -d "$DIR/build/classes" ]; then
  echo "[ERROR] Build first: tools/tlsvectors/build.sh" >&2
  exit 1
fi
exec "$TLSVECTORS_JAVA_HOME/bin/java" -cp "$DIR/build/classes" tlsvectors.TlsVectors "$@"
