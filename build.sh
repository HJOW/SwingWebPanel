#!/bin/sh
# 제품을 JDK 6 으로 컴파일하고 JAR 를 만든다. (기본 BUILD_DIR=build)
# 사용: ./build.sh   (JAVA6_HOME 으로 JDK 6 경로, BUILD_DIR 로 출력 디렉터리 지정 가능)
# 구성(P26): 엔진 모듈(engine/src, 브라우저 의존 없음)을 먼저 빌드하고, 브라우저 모듈(src)을 엔진 클래스에만 의존해 컴파일한다.
#   $B/swingwebpanel-js.jar       엔진만 (javax.script 제공자 포함)
#   $B/swingwebpanel-browser.jar  브라우저만 (실행에는 엔진 JAR 이 필요)
#   $B/swingwebpanel.jar          둘을 합친 단일 JAR (기존 사용 방식 그대로: 이 JAR 하나만 클래스패스에 두면 된다)
#   $B/classes                    합친 클래스 디렉터리(시험, 예제, 도구가 사용)
cd "$(dirname "$0")" || exit 1
ROOT=$(pwd)
. "$ROOT/scripts/jdk6.sh"
B="${BUILD_DIR:-build}"

sh engine/build.sh || exit 1

rm -rf "$B/classes" "$B/browser-classes" "$B/swingwebpanel.jar" "$B/swingwebpanel-browser.jar"
mkdir -p "$B/classes" "$B/browser-classes"
# 컴파일할 소스 목록을 상대 경로로 만든다. (javac @파일 에서 경로 변환 문제를 피한다.)
find src -name '*.java' | sort > "$B/sources.txt"
if [ ! -s "$B/sources.txt" ]; then
  echo "[ERROR] no java sources under src" >&2
  exit 1
fi
# 브라우저 소스는 컴파일된 엔진 클래스만 참조할 수 있다. (엔진 소스는 소스패스에 두지 않는다.)
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp "$B/engine-classes" -d "$B/browser-classes" @"$B/sources.txt" || exit 1

# java 가 아닌 리소스 파일(템플릿, 기본 스타일 등)을 클래스 경로로 복사한다.
(cd src && find . -type f ! -name '*.java' -exec cp --parents {} "../$B/browser-classes/" \; ) 2>/dev/null

# 합친 클래스 디렉터리: 엔진 클래스 위에 브라우저 클래스를 얹는다. 두 모듈의 패키지는 겹치지 않는다.
cp -R "$B/engine-classes/." "$B/classes/" || exit 1
cp -R "$B/browser-classes/." "$B/classes/" || exit 1

cat > "$B/MANIFEST.MF" <<MF
Manifest-Version: 1.0
Implementation-Title: SwingWebPanel
Implementation-Version: 0.1.0
Created-By: build.sh (JDK 6)
MF
cat > "$B/browser-MANIFEST.MF" <<MF
Manifest-Version: 1.0
Implementation-Title: SwingWebPanel browser component (requires swingwebpanel-js.jar)
Implementation-Version: 0.1.0
Created-By: build.sh (JDK 6)
MF
"$JAR" cfm "$B/swingwebpanel-browser.jar" "$B/browser-MANIFEST.MF" -C "$B/browser-classes" . || exit 1
"$JAR" cfm "$B/swingwebpanel.jar" "$B/MANIFEST.MF" -C "$B/classes" . || exit 1
echo "[OK] $B/swingwebpanel.jar (engine + browser), $B/swingwebpanel-browser.jar, $B/swingwebpanel-js.jar created ($JAVAC_VERSION)"
