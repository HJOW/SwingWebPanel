#!/bin/sh
# 제품을 빌드하고 테스트를 JDK 6 으로 컴파일/실행한다. 실패가 있으면 0 이 아닌 종료 코드로 끝난다.
# 사용: ./test.sh [--filter 문자열]   (JAVA6_HOME 으로 JDK 6 경로, BUILD_DIR 로 출력 디렉터리 지정 가능)
cd "$(dirname "$0")" || exit 1
ROOT=$(pwd)
. "$ROOT/scripts/jdk6.sh"
B="${BUILD_DIR:-build}"
./build.sh || exit 1

rm -rf "$B/test-classes"
mkdir -p "$B/test-classes"
find tests/src -name '*.java' | sort > "$B/test-sources.txt"
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp "$B/classes" -d "$B/test-classes" @"$B/test-sources.txt" || exit 1
# 시험 입력 자료를 클래스 경로 루트로 복사한다.
(cd tests/resources && find . -type f -exec cp --parents {} "../../$B/test-classes/" \; ) 2>/dev/null

CP="$B/classes${PATHSEP}$B/test-classes"
"$JAVA" -Dswp.jar="$B/swingwebpanel.jar" -cp "$CP" org.duckdns.hjow.swingwebpanel.testing.TestRunner --scan "$B/test-classes" "$@"
RC=$?

# 실행기가 실패 사례에 0 이 아닌 종료 코드를 내는지 확인한다.
"$JAVA" -cp "$CP" org.duckdns.hjow.swingwebpanel.testing.TestRunner org.duckdns.hjow.swingwebpanel.testing.selftest.failing.FailingSuite > "$B/failing-selftest.log" 2>&1
FRC=$?
if [ "$FRC" -ne 1 ]; then
  echo "[ERROR] runner self-check failed: expected exit code 1 for failing suite, got $FRC" >&2
  exit 1
fi
echo "[OK] runner exit-code self-check passed (failing suite -> 1)"
exit $RC
