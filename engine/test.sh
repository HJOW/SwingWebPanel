#!/bin/sh
# 엔진 모듈만 빌드하고 엔진 시험(engine/tests)을 JDK 6 으로 컴파일/실행한다. 브라우저 클래스와 AWT 없이(java.awt.headless=true) 실행하므로
# 엔진이 브라우저에 기대면 여기서 NoClassDefFoundError 로 실패한다. 실패가 있으면 0 이 아닌 종료 코드로 끝난다.
# 사용: engine/test.sh [--filter 문자열]   (JAVA6_HOME, BUILD_DIR 지원. 이미 엔진을 빌드했으면 SWP_ENGINE_BUILT=1 로 빌드를 건너뛰고,
#       SWP_ENGINE_FROM_JAR=1 이면 클래스 디렉터리 대신 swingwebpanel-js.jar 하나만 클래스 경로에 둔다)
cd "$(dirname "$0")/.." || exit 1
ROOT=$(pwd)
. "$ROOT/scripts/jdk6.sh"
B="${BUILD_DIR:-build}"
if [ "$SWP_ENGINE_BUILT" != "1" ]; then
  sh engine/build.sh || exit 1
fi

rm -rf "$B/engine-test-classes"
mkdir -p "$B/engine-test-classes"
find engine/tests/src -name '*.java' | sort > "$B/engine-test-sources.txt"
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp "$B/engine-classes" -sourcepath "$B/engine-empty" -d "$B/engine-test-classes" @"$B/engine-test-sources.txt" || exit 1
# 시험 입력 자료를 클래스 경로 루트로 복사한다.
(cd engine/tests/resources && find . -type f -exec cp --parents {} "../../../$B/engine-test-classes/" \; ) 2>/dev/null

# SWP_ENGINE_FROM_JAR=1 이면 엔진 클래스 디렉터리 대신 엔진 JAR(swingwebpanel-js.jar) 하나만 클래스 경로에 둔다. (JAR 하나로 동작함을 확인할 때)
if [ "$SWP_ENGINE_FROM_JAR" = "1" ]; then
  CP="$B/swingwebpanel-js.jar${PATHSEP}$B/engine-test-classes"
else
  CP="$B/engine-classes${PATHSEP}$B/engine-test-classes"
fi
"$JAVA" -Djava.awt.headless=true -Dswp.engine.jar="$B/swingwebpanel-js.jar" -cp "$CP" org.duckdns.hjow.swingwebpanel.testing.TestRunner --scan "$B/engine-test-classes" "$@"
RC=$?

# 실행기가 실패 사례에 0 이 아닌 종료 코드를 내는지 확인한다.
"$JAVA" -cp "$CP" org.duckdns.hjow.swingwebpanel.testing.TestRunner org.duckdns.hjow.swingwebpanel.testing.selftest.failing.FailingSuite > "$B/engine-failing-selftest.log" 2>&1
FRC=$?
if [ "$FRC" -ne 1 ]; then
  echo "[ERROR] runner self-check failed: expected exit code 1 for failing suite, got $FRC" >&2
  exit 1
fi
echo "[OK] runner exit-code self-check passed (failing suite -> 1)"
exit $RC
