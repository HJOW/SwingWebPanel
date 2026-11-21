#!/bin/sh
# hrtfgen 도구를 실행한다. (Run the hrtfgen tool.)
# 실행 JRE 는 JAVA6_HOME, 없으면 기본 JDK 6 경로를 사용한다. (이 도구는 네트워크를 쓰지 않으므로 JRE 6 으로 실행한다.)
DIR=$(cd "$(dirname "$0")" && pwd)

RUNHOME="$JAVA6_HOME"
if [ -z "$RUNHOME" ]; then
  for c in "/c/Program Files/Java/jdk1.6.0_45" "C:/Program Files/Java/jdk1.6.0_45" "/usr/lib/jvm/jdk1.6.0_45"; do
    if [ -x "$c/bin/java" ] || [ -x "$c/bin/java.exe" ]; then
      RUNHOME="$c"
      break
    fi
  done
fi
if [ -z "$RUNHOME" ]; then
  echo "[ERROR] JRE 6 not found. Set JAVA6_HOME." >&2
  exit 1
fi
if [ ! -d "$DIR/build/classes" ]; then
  echo "[ERROR] Build first: tools/hrtfgen/build.sh" >&2
  exit 1
fi
exec "$RUNHOME/bin/java" -cp "$DIR/build/classes" hrtfgen.HrtfGen "$@"
