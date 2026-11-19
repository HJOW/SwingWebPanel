#!/bin/sh
# entitygen 도구를 JDK 6 으로 컴파일한다. (Compile the entitygen tool with JDK 6.)
# JAVA6_HOME 환경 변수로 JDK 6 경로를 바꿀 수 있다. 찾지 못하면 오류로 종료한다.
DIR=$(cd "$(dirname "$0")" && pwd)

if [ -z "$JAVA6_HOME" ]; then
  for c in "/c/Program Files/Java/jdk1.6.0_45" "C:/Program Files/Java/jdk1.6.0_45" "/usr/lib/jvm/jdk1.6.0_45"; do
    if [ -x "$c/bin/javac" ] || [ -x "$c/bin/javac.exe" ]; then
      JAVA6_HOME="$c"
      break
    fi
  done
fi
if [ -z "$JAVA6_HOME" ] || { [ ! -x "$JAVA6_HOME/bin/javac" ] && [ ! -x "$JAVA6_HOME/bin/javac.exe" ]; }; then
  echo "[ERROR] JDK 6 not found. Set JAVA6_HOME." >&2
  exit 1
fi

OUT="$DIR/build/classes"
rm -rf "$OUT"
mkdir -p "$OUT"
"$JAVA6_HOME/bin/javac" -encoding UTF-8 -source 1.6 -target 1.6 -d "$OUT" "$DIR"/src/entitygen/*.java
exit $?
