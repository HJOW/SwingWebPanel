#!/bin/sh
# 일부 패키지만 컴파일하고 시험한다. 여러 작업자가 동시에 일할 때 다른 작업자의 미완성 코드와 build 디렉터리를 건드리지 않는다.
# 사용: scripts/pkgtest.sh <이름> "<src/org/duckdns/hjow/swingwebpanel 하위 패키지들>" "<tests/src/org/duckdns/hjow/swingwebpanel 하위 패키지들>" [TestRunner 인수...]
# 패키지 루트의 클래스(예: SwingWebPanel)는 "@root" 로 지정한다.
# 패키지 이름은 브라우저 소스(src, tests/src)와 엔진 소스(engine/src, engine/tests/src, P26) 양쪽에서 찾는다. 예: "js" 는 두 곳의 js 하위를 모두 포함한다.
# 예)   scripts/pkgtest.sh js "js" "js"                → js/** 의 소스(엔진 + js.host/dom/worker)와 시험을 컴파일/실행
#       scripts/pkgtest.sh net "net core" "net" --filter Http
# 출력: build/pkg-<이름>/ (classes, test-classes).
# 지정하지 않은 패키지의 클래스는 javac 의 -sourcepath 로 필요할 때만 함께 컴파일하므로, 참조하지 않는 다른 작업자의 미완성 파일은
# 컴파일되지 않는다. 시험 실행기(TestRunner)와 그 자체 시험(testing/selftest)은 항상 포함한다.
# 전체 통합 확인은 ./test.sh 로 한다.
cd "$(dirname "$0")/.." || exit 1
ROOT=$(pwd)
. "$ROOT/scripts/jdk6.sh"
if [ $# -lt 3 ]; then
  echo "usage: scripts/pkgtest.sh <name> \"<src subpackages>\" \"<test subpackages>\" [TestRunner args]" >&2
  exit 2
fi
NAME="$1"; SRCP="$2"; TESTP="$3"; shift 3
B="build/pkg-$NAME"
PKG=org/duckdns/hjow/swingwebpanel
rm -rf "$B"
mkdir -p "$B/classes" "$B/test-classes"
: > "$B/sources.txt"
for p in $SRCP; do
  for root in src engine/src; do
    if [ "$p" = "@root" ]; then
      [ -d "$root/$PKG" ] && find "$root/$PKG" -maxdepth 1 -name "*.java" | sort >> "$B/sources.txt"
    else
      [ -d "$root/$PKG/$p" ] && find "$root/$PKG/$p" -name "*.java" | sort >> "$B/sources.txt"
    fi
  done
done
if [ -s "$B/sources.txt" ]; then
  "$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -g -sourcepath "src${PATHSEP}engine/src" -d "$B/classes" @"$B/sources.txt" || exit 1
fi
: > "$B/test-sources.txt"
echo engine/tests/src/$PKG/testing/TestRunner.java >> "$B/test-sources.txt"
find engine/tests/src/$PKG/testing/selftest -name '*.java' | sort >> "$B/test-sources.txt"
find tests/src/$PKG/testing/selftest -name '*.java' | sort >> "$B/test-sources.txt"
for p in $TESTP; do
  for root in tests/src engine/tests/src; do
    if [ "$p" = "@root" ]; then
      [ -d "$root/$PKG" ] && find "$root/$PKG" -maxdepth 1 -name "*.java" | sort >> "$B/test-sources.txt"
    else
      [ -d "$root/$PKG/$p" ] && find "$root/$PKG/$p" -name "*.java" | sort >> "$B/test-sources.txt"
    fi
  done
done
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp "$B/classes" -sourcepath "tests/src${PATHSEP}engine/tests/src" -d "$B/test-classes" @"$B/test-sources.txt" || exit 1
# 엔진 모듈의 비 java 리소스(javax.script 제공자 등록 파일 등)와 시험 입력 자료를 클래스 경로 루트로 복사한다.
(cd engine/resources 2>/dev/null && find . -type f -exec cp --parents {} "../../$B/classes/" \; ) 2>/dev/null
(cd engine/tests/resources 2>/dev/null && find . -type f -exec cp --parents {} "../../../$B/test-classes/" \; ) 2>/dev/null
(cd tests/resources && find . -type f -exec cp --parents {} "../../$B/test-classes/" \; ) 2>/dev/null
"$JAVA" -cp "$B/classes${PATHSEP}$B/test-classes" org.duckdns.hjow.swingwebpanel.testing.TestRunner --scan "$B/test-classes" "$@"
