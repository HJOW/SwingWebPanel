#!/bin/sh
# sourcecheck 도구를 저장소 루트를 기준으로 실행한다. 인수는 그대로 전달한다. (예: tools/sourcecheck/run.sh --only javadoc src)
DIR=$(cd "$(dirname "$0")" && pwd)
. "$DIR/../../scripts/jdk6.sh"
if [ ! -d "$DIR/build/classes" ]; then
  echo "[ERROR] Build first: tools/sourcecheck/build.sh" >&2
  exit 2
fi
cd "$DIR/../.." || exit 2
exec "$JAVA" -cp "tools/sourcecheck/build/classes" sourcecheck.SourceCheck "$@"
