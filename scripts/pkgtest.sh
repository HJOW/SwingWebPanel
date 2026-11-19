#!/bin/sh
# 일부 패키지만 컴파일하고 시험한다. 여러 작업자가 동시에 일할 때 다른 작업자의 미완성 코드와 build 디렉터리를 건드리지 않는다.
# 사용: scripts/pkgtest.sh <이름> "<src/org/duckdns/hjow/swingwebpanel 하위 패키지들>" "<tests/src/org/duckdns/hjow/swingwebpanel 하위 패키지들>" [TestRunner 인수...]
# 패키지 루트의 클래스(예: SwingWebPanel)는 "@root" 로 지정한다.
# 예)   scripts/pkgtest.sh js "js" "js"                → src/org/duckdns/hjow/swingwebpanel/js/** 와 tests/src/org/duckdns/hjow/swingwebpanel/js/** 를 컴파일/실행
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
rm -rf "$B"
mkdir -p "$B/classes" "$B/test-classes"
: > "$B/sources.txt"
for p in $SRCP; do
  if [ "$p" = "@root" ]; then find src/org/duckdns/hjow/swingwebpanel -maxdepth 1 -name "*.java" | sort >> "$B/sources.txt"; else find "src/org/duckdns/hjow/swingwebpanel/$p" -name "*.java" | sort >> "$B/sources.txt"; fi
done
if [ -s "$B/sources.txt" ]; then
  "$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -g -sourcepath src -d "$B/classes" @"$B/sources.txt" || exit 1
fi
: > "$B/test-sources.txt"
echo tests/src/org/duckdns/hjow/swingwebpanel/testing/TestRunner.java >> "$B/test-sources.txt"
find tests/src/org/duckdns/hjow/swingwebpanel/testing/selftest -name '*.java' | sort >> "$B/test-sources.txt"
for p in $TESTP; do
  if [ "$p" = "@root" ]; then find tests/src/org/duckdns/hjow/swingwebpanel -maxdepth 1 -name "*.java" | sort >> "$B/test-sources.txt"; else find "tests/src/org/duckdns/hjow/swingwebpanel/$p" -name "*.java" | sort >> "$B/test-sources.txt"; fi
done
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -g -cp "$B/classes" -sourcepath tests/src -d "$B/test-classes" @"$B/test-sources.txt" || exit 1
(cd tests/resources && find . -type f -exec cp --parents {} "../../$B/test-classes/" \; ) 2>/dev/null
"$JAVA" -cp "$B/classes${PATHSEP}$B/test-classes" org.duckdns.hjow.swingwebpanel.testing.TestRunner --scan "$B/test-classes" "$@"
