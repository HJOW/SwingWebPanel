#!/bin/sh
# uicheck 도구를 실행한다. 인수는 그대로 전달한다. 예: tools/uicheck/run.sh --out build/uicheck --url classpath://org/duckdns/hjow/swingwebpanel/examples/pages/index.html --shot first --dispose
# 제품 JAR, 예제 리소스와 Maven으로 컴파일한 예제를 클래스패스에 둔다. 화면이 없으면 종료 코드 3으로 끝난다.
# UICHECK_JVM_OPTS 로 JVM 옵션을 더할 수 있다(예: 다운로드 기본 폴더를 바꾸는 -Duser.home=...).
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh"
if [ ! -d "$DIR/build/classes" ]; then
  echo "[ERROR] Build first: tools/uicheck/build.sh" >&2
  exit 2
fi
cd "$DIR/../.." || exit 2
SWP_CLASSES="${SWP_CLASSES:-browser/target/swingwebpanel.jar}"
exec "$JAVA" $UICHECK_JVM_OPTS -cp "tools/uicheck/build/classes${PATHSEP}${SWP_CLASSES}${PATHSEP}examples/target/classes${PATHSEP}examples/src/main/resources" uicheck.UiCheck "$@"
