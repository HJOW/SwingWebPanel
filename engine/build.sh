#!/bin/sh
# 엔진 모듈(자체 JavaScript 엔진, 브라우저와 AWT/Swing 에 의존하지 않음)만 JDK 6 으로 컴파일하고 $BUILD_DIR/swingwebpanel-js.jar 를 만든다.
# 사용: engine/build.sh   (JAVA6_HOME 으로 JDK 6 경로, BUILD_DIR 로 출력 디렉터리 지정 가능. 기본 BUILD_DIR=build)
# 출력: $BUILD_DIR/engine-classes (컴파일 결과 + engine/resources), $BUILD_DIR/swingwebpanel-js.jar
# 경계 강제: 컴파일 클래스패스와 소스패스를 빈 디렉터리 하나로 제한한다. 엔진 소스가 브라우저 클래스(또는 어떤 라이브러리)를 참조하면 컴파일이 실패한다.
cd "$(dirname "$0")/.." || exit 1
ROOT=$(pwd)
. "$ROOT/scripts/jdk6.sh"
B="${BUILD_DIR:-build}"

rm -rf "$B/engine-classes" "$B/engine-empty" "$B/swingwebpanel-js.jar"
mkdir -p "$B/engine-classes" "$B/engine-empty"
find engine/src -name '*.java' | sort > "$B/engine-sources.txt"
if [ ! -s "$B/engine-sources.txt" ]; then
  echo "[ERROR] no java sources under engine/src" >&2
  exit 1
fi
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp "$B/engine-empty" -sourcepath "$B/engine-empty" -d "$B/engine-classes" @"$B/engine-sources.txt" || exit 1

# java 가 아닌 리소스(javax.script 제공자 등록 파일 등)를 클래스 경로 루트로 복사한다.
if [ -d engine/resources ]; then
  (cd engine/resources && find . -type f -exec cp --parents {} "../../$B/engine-classes/" \; ) || exit 1
fi
(cd engine/src && find . -type f ! -name '*.java' -exec cp --parents {} "../../$B/engine-classes/" \; ) 2>/dev/null

cat > "$B/engine-MANIFEST.MF" <<MF
Manifest-Version: 1.0
Implementation-Title: SwingWebPanel JavaScript engine
Implementation-Version: 0.1.0
Created-By: engine/build.sh (JDK 6)
MF
"$JAR" cfm "$B/swingwebpanel-js.jar" "$B/engine-MANIFEST.MF" -C "$B/engine-classes" . || exit 1
echo "[OK] $B/swingwebpanel-js.jar created ($JAVAC_VERSION)"
