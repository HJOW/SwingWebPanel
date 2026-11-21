#!/bin/sh
# 제품을 빌드하고 시험을 JDK 6 으로 컴파일/실행한다. 실패가 있으면 0 이 아닌 종료 코드로 끝난다.
# 사용: ./test.sh [--filter 문자열]   (JAVA6_HOME 으로 JDK 6 경로, BUILD_DIR 로 출력 디렉터리 지정 가능)
# 단계(P26):
#   1) 엔진 단계: 엔진 시험(engine/tests)을 엔진 클래스만으로(브라우저와 AWT 없이, java.awt.headless=true) 실행한다. (engine/test.sh)
#   2) 브라우저 단계: 브라우저 시험(tests)을 합친 클래스($B/classes)로 실행한다.
# 마지막에 두 단계의 통과/실패/건너뜀 수를 합쳐 한 줄로 출력한다.
cd "$(dirname "$0")" || exit 1
ROOT=$(pwd)
. "$ROOT/scripts/jdk6.sh"
B="${BUILD_DIR:-build}"
./build.sh || exit 1

# 1) 엔진 단계: 이미 빌드했으므로 엔진 빌드는 건너뛴다.
{ SWP_ENGINE_BUILT=1 sh engine/test.sh "$@"; echo $? > "$B/engine-stage.rc"; } 2>&1 | tee "$B/engine-stage.log"
ERC=$(cat "$B/engine-stage.rc")

# 2) 브라우저 단계: 시험 실행기와 검사 도우미(Check 등)는 엔진 시험 클래스에 있다. 그 디렉터리는 클래스 경로에만 두고 스위트로는 검색하지 않는다.
rm -rf "$B/test-classes"
mkdir -p "$B/test-classes"
find tests/src -name '*.java' | sort > "$B/test-sources.txt"
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp "$B/classes${PATHSEP}$B/engine-test-classes" -d "$B/test-classes" @"$B/test-sources.txt" || exit 1
# 시험 입력 자료를 클래스 경로 루트로 복사한다.
(cd tests/resources && find . -type f -exec cp --parents {} "../../$B/test-classes/" \; ) 2>/dev/null

CP="$B/classes${PATHSEP}$B/engine-test-classes${PATHSEP}$B/test-classes"
{ "$JAVA" -Dswp.jar="$B/swingwebpanel.jar" -Dswp.engine.jar="$B/swingwebpanel-js.jar" -Dswp.browser.jar="$B/swingwebpanel-browser.jar" -cp "$CP" org.duckdns.hjow.swingwebpanel.testing.TestRunner --scan "$B/test-classes" "$@"; echo $? > "$B/browser-stage.rc"; } | tee "$B/browser-stage.log"
BRC=$(cat "$B/browser-stage.rc")

# 두 단계의 요약 줄("=== N passed, M failed, K skipped (..) ===")을 합산한다.
cat "$B/engine-stage.log" "$B/browser-stage.log" | grep '^=== ' | sed 's/^=== \([0-9]*\) passed, \([0-9]*\) failed, \([0-9]*\) skipped.*/\1 \2 \3/' | awk '{p+=$1; f+=$2; s+=$3} END {print "=== TOTAL (engine + browser): " p " passed, " f " failed, " s " skipped ==="}'

# 실행기가 실패 사례에 0 이 아닌 종료 코드를 내는지 확인한다. (엔진 단계에서도 확인하지만 합친 클래스 경로에서 한 번 더 확인한다.)
"$JAVA" -cp "$CP" org.duckdns.hjow.swingwebpanel.testing.TestRunner org.duckdns.hjow.swingwebpanel.testing.selftest.failing.FailingSuite > "$B/failing-selftest.log" 2>&1
FRC=$?
if [ "$FRC" -ne 1 ]; then
  echo "[ERROR] runner self-check failed: expected exit code 1 for failing suite, got $FRC" >&2
  exit 1
fi
echo "[OK] runner exit-code self-check passed (failing suite -> 1)"
if [ "$ERC" -ne 0 ]; then
  exit "$ERC"
fi
exit "$BRC"
