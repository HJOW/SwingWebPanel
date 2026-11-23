#!/bin/sh
# Maven 으로 독립 JavaScript 엔진 JAR만 만든다.
cd "$(dirname "$0")/.." || exit 1
exec ./build.sh -pl engine -am "$@"
