#!/bin/sh
# 제품 소스를 JDK 6 으로 컴파일하고 $BUILD_DIR/swingwebpanel.jar 를 만든다. (기본 BUILD_DIR=build)
# 사용: ./build.sh   (JAVA6_HOME 으로 JDK 6 경로, BUILD_DIR 로 출력 디렉터리 지정 가능)
cd "$(dirname "$0")" || exit 1
ROOT=$(pwd)
. "$ROOT/scripts/jdk6.sh"
B="${BUILD_DIR:-build}"

rm -rf "$B/classes" "$B/swingwebpanel.jar"
mkdir -p "$B/classes"
# 컴파일할 소스 목록을 상대 경로로 만든다. (javac @파일 에서 경로 변환 문제를 피한다.)
find src -name '*.java' | sort > "$B/sources.txt"
if [ ! -s "$B/sources.txt" ]; then
  echo "[ERROR] no java sources under src" >&2
  exit 1
fi
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -g -d "$B/classes" @"$B/sources.txt" || exit 1

# java 가 아닌 리소스 파일(템플릿, 기본 스타일 등)을 클래스 경로로 복사한다.
(cd src && find . -type f ! -name '*.java' -exec cp --parents {} "../$B/classes/" \; ) 2>/dev/null

cat > "$B/MANIFEST.MF" <<MF
Manifest-Version: 1.0
Implementation-Title: SwingWebPanel
Implementation-Version: 0.1.0
Created-By: build.sh (JDK 6)
MF
"$JAR" cfm "$B/swingwebpanel.jar" "$B/MANIFEST.MF" -C "$B/classes" . || exit 1
echo "[OK] $B/swingwebpanel.jar created ($JAVAC_VERSION)"
