#!/bin/sh
# webfetch 도구를 실행한다. (Run the webfetch tool.)
# 실행 JRE 는 WEBFETCH_JAVA_HOME, 없으면 JAVA6_HOME 을 사용한다.
# JRE 6u45 는 TLS 1.2/SNI 를 지원하지 않으므로 HTTPS 조회는 더 새 JRE 를 WEBFETCH_JAVA_HOME 에 지정한다.
DIR=$(cd "$(dirname "$0")" && pwd)

RUNHOME="$WEBFETCH_JAVA_HOME"
if [ -z "$RUNHOME" ]; then
  RUNHOME="$JAVA6_HOME"
fi
if [ -z "$RUNHOME" ]; then
  for c in "/c/Program Files/Java/jdk1.6.0_45" "C:/Program Files/Java/jdk1.6.0_45" "/usr/lib/jvm/jdk1.6.0_45"; do
    if [ -x "$c/bin/java" ] || [ -x "$c/bin/java.exe" ]; then
      RUNHOME="$c"
      break
    fi
  done
fi
if [ -z "$RUNHOME" ]; then
  echo "[ERROR] JRE not found. Set WEBFETCH_JAVA_HOME or JAVA6_HOME." >&2
  exit 1
fi
if [ ! -d "$DIR/build/classes" ]; then
  echo "[ERROR] Build first: tools/webfetch/build.sh" >&2
  exit 1
fi
exec "$RUNHOME/bin/java" -cp "$DIR/build/classes" webfetch.WebFetch "$@"
