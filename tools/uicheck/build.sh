#!/bin/sh
# uicheck 도구를 JDK 6 으로 컴파일한다. 제품 클래스(기본 build/classes, SWP_CLASSES 로 저장소 루트 기준 경로 지정)가 먼저 빌드되어 있어야 한다. (JAVA6_HOME 으로 JDK 경로 지정 가능)
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh"
cd "$DIR/../.." || exit 1
SWP_CLASSES="${SWP_CLASSES:-build/classes}"
if [ ! -d "$SWP_CLASSES" ]; then
  echo "[ERROR] Build the product first: ./build.sh" >&2
  exit 2
fi
rm -rf tools/uicheck/build/classes
mkdir -p tools/uicheck/build/classes
find tools/uicheck/src -name "*.java" | sort > tools/uicheck/build/sources.txt
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -cp "$SWP_CLASSES" -d tools/uicheck/build/classes @tools/uicheck/build/sources.txt
exit $?
