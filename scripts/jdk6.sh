#!/bin/sh
# JDK 6 위치를 찾아 JAVA6_HOME 과 JAVAC/JAVA/JAR 변수를 설정한다. (source 해서 사용: . scripts/jdk6.sh)
# - JAVA6_HOME 환경 변수가 있으면 그 경로를 사용하고, 없으면 기본 경로를 찾는다.
# - JDK 6 을 찾지 못하거나 javac 버전이 1.6 이 아니면 다른 JDK 로 조용히 대체하지 않고 오류로 종료한다.
# - 다른 JDK 는 SWP_ALLOW_NON_JDK6=1 로 명시했을 때만 사용하며, 그 결과는 JRE 6 검증이 아니다.
if [ -z "$JAVA6_HOME" ]; then
  for c in "/c/Program Files/Java/jdk1.6.0_45" "C:/Program Files/Java/jdk1.6.0_45" "/usr/lib/jvm/jdk1.6.0_45" "/usr/lib/jvm/java-6-openjdk"; do
    if [ -x "$c/bin/javac" ] || [ -x "$c/bin/javac.exe" ]; then
      JAVA6_HOME="$c"
      break
    fi
  done
fi
if [ -z "$JAVA6_HOME" ]; then
  echo "[ERROR] JDK 6 not found. Install JDK 6 or set JAVA6_HOME (default: C:\Program Files\Java\jdk1.6.0_45)." >&2
  exit 1
fi
if [ -x "$JAVA6_HOME/bin/javac.exe" ]; then EXE=".exe"; else EXE=""; fi
JAVAC="$JAVA6_HOME/bin/javac$EXE"
JAVA="$JAVA6_HOME/bin/java$EXE"
JAR="$JAVA6_HOME/bin/jar$EXE"
if [ ! -x "$JAVAC" ] || [ ! -x "$JAVA" ]; then
  echo "[ERROR] javac/java not found under JAVA6_HOME=$JAVA6_HOME" >&2
  exit 1
fi
JAVAC_VERSION=$("$JAVAC" -version 2>&1 | head -1)
case "$JAVAC_VERSION" in
  *" 1.6."*) ;;
  *)
    if [ "$SWP_ALLOW_NON_JDK6" = "1" ]; then
      echo "[WARN] NOT JDK 6 ($JAVAC_VERSION). Results are NOT a JRE 6 verification." >&2
    else
      echo "[ERROR] $JAVA6_HOME is not JDK 6 ($JAVAC_VERSION). Set SWP_ALLOW_NON_JDK6=1 to use it explicitly." >&2
      exit 1
    fi
    ;;
esac
case "$(uname -s 2>/dev/null)" in
  MINGW*|MSYS*|CYGWIN*) PATHSEP=";" ;;
  *) PATHSEP=":" ;;
esac
export JAVA6_HOME JAVAC JAVA JAR PATHSEP
