#!/bin/sh
# 예제를 JDK 6 으로 컴파일하고 실행한다. 사용: examples/run.sh <예제 클래스 이름> [인수...]   (예: examples/run.sh BlankPageExample --smoke)
# 먼저 제품을 빌드(./build.sh)하여 build/classes 를 사용한다. JAVA6_HOME 으로 JDK 6 경로를 지정할 수 있다.
cd "$(dirname "$0")/.." || exit 1
. scripts/jdk6.sh
if [ $# -lt 1 ]; then
  echo "usage: examples/run.sh <ExampleClassName> [args...]" >&2
  exit 2
fi
NAME="$1"
shift
# SWP_CLASSES 로 이미 컴파일한 제품 클래스 디렉터리를 지정하면 빌드를 건너뛴다. (병렬 작업 중 일부 패키지만 시험할 때 사용)
if [ -z "$SWP_CLASSES" ]; then
  ./build.sh >/dev/null || exit 1
  SWP_CLASSES=build/classes
fi
rm -rf build/examples
mkdir -p build/examples
find examples/src -name '*.java' | sort > build/examples-sources.txt
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -cp "$SWP_CLASSES" -d build/examples @build/examples-sources.txt || exit 1
# 예제 전용 HTML/CSS를 예제 클래스패스에만 복사한다(제품 JAR에는 넣지 않는다).
if [ -d examples/resources ]; then
  cp -R examples/resources/. build/examples/ || exit 1
fi
exec "$JAVA" -cp "${SWP_CLASSES}${PATHSEP}build/examples" "org.duckdns.hjow.swingwebpanel.examples.$NAME" "$@"
