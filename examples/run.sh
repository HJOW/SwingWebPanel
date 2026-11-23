#!/bin/sh
# Maven으로 예제를 빌드하고 JDK 6으로 실행한다. 뒤의 인수는 예제에 전달한다.
cd "$(dirname "$0")/.." || exit 1
if [ "$#" -lt 1 ]; then
  echo "Usage: $0 ExampleClassName [example arguments]" >&2
  exit 2
fi
example="$1"
shift
mvn -B -pl examples -am -Dmaven.test.skip=true package || exit 1
. ./scripts/jdk6.sh
# 독립 엔진 예제는 브라우저 클래스 없이 실행한다.
case "$example" in
  EngineExample|script.ScriptEngineExample) product=engine/target/swingwebpanel-js.jar ;;
  *) product=browser/target/swingwebpanel.jar ;;
esac
exec "$JAVA" -cp "examples/target/classes${PATHSEP}${SWP_CLASSES:-$product}" "org.duckdns.hjow.swingwebpanel.examples.$example" "$@"
