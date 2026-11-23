#!/bin/sh
# 엔진만 선택해 브라우저 없이 회귀 시험을 실행한다.
cd "$(dirname "$0")/.." || exit 1
exec ./test.sh "$@" -pl engine
