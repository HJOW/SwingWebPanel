#!/bin/sh
# cabundle 도구를 JDK 6 으로 실행한다. (Run the cabundle tool with JDK 6.)
# 인수: 보고서.csv 출력.txt 원본URL 조회날짜
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh" || exit 1
if [ ! -d "$DIR/build/classes" ]; then
  echo "[ERROR] Build first: tools/cabundle/build.sh" >&2
  exit 1
fi
exec "$JAVA" -cp "$DIR/build/classes" cabundle.CaBundle "$@"
