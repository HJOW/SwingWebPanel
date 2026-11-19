#!/bin/sh
# uicheck 도구를 실행한다. 인수는 그대로 전달한다. 예: tools/uicheck/run.sh --out build/uicheck --url classpath://org/duckdns/hjow/swingwebpanel/examples/pages/index.html --shot first --dispose
# 제품 클래스(기본 build/classes, SWP_CLASSES 로 저장소 루트 기준 경로 지정)와 예제 리소스(examples/resources)를 클래스패스에 둔다. 화면(디스플레이)이 없으면 종료 코드 3 으로 끝난다.
# UICHECK_JVM_OPTS 로 JVM 옵션을 더할 수 있다(예: 다운로드 기본 폴더를 바꾸는 -Duser.home=...).
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh"
if [ ! -d "$DIR/build/classes" ]; then
  echo "[ERROR] Build first: tools/uicheck/build.sh" >&2
  exit 2
fi
cd "$DIR/../.." || exit 2
SWP_CLASSES="${SWP_CLASSES:-build/classes}"
exec "$JAVA" $UICHECK_JVM_OPTS -cp "tools/uicheck/build/classes${PATHSEP}${SWP_CLASSES}${PATHSEP}examples/resources${PATHSEP}build/examples" uicheck.UiCheck "$@"
