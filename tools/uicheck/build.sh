#!/bin/sh
# uicheck 도구를 JDK 6 으로 컴파일한다. 기본 제품 JAR는 Maven 빌드가 만들며 SWP_CLASSES로 재지정할 수 있다.
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh"
cd "$DIR/../.." || exit 1
SWP_CLASSES="${SWP_CLASSES:-browser/target/swingwebpanel.jar}"
if [ ! -e "$SWP_CLASSES" ]; then
  echo "[ERROR] Build the product first: ./build.sh" >&2
  exit 2
fi
rm -rf tools/uicheck/build/classes
mkdir -p tools/uicheck/build/classes
find tools/uicheck/src -name "*.java" | sort > tools/uicheck/build/sources.txt
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 -cp "$SWP_CLASSES" -d tools/uicheck/build/classes @tools/uicheck/build/sources.txt
exit $?
