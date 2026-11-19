#!/bin/sh
# brotlidict 도구를 실행한다. (Run the brotlidict tool.)
# 사용: tools/brotlidict/run.sh rfc7932.txt 출력디렉터리
# 실행 JRE 는 JAVA6_HOME 이며, 없으면 기본 JDK 6 경로를 찾는다. 못 찾으면 오류로 종료한다.
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
  echo "[ERROR] Build first: tools/brotlidict/build.sh" >&2
  exit 1
fi
exec "$RUNHOME/bin/java" -cp "$DIR/build/classes" brotlidict.BrotliDict "$@"
