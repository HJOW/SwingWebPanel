#!/bin/sh
# Maven 으로 엔진, 브라우저, 예제 JAR를 만든다. 추가 Maven 옵션을 전달할 수 있다.
cd "$(dirname "$0")" || exit 1
exec mvn -B -Dmaven.test.skip=true package "$@"
