#!/bin/sh
# Java 6 자체 회귀 실행기를 Maven으로 실행한다. 기존 --filter 인수도 받는다.
cd "$(dirname "$0")" || exit 1
if [ "${1:-}" = "--filter" ]; then
  if [ "$#" -lt 2 ]; then
    echo "Usage: $0 [--filter text] [Maven options]" >&2
    exit 2
  fi
  filter="$2"
  shift 2
  exec mvn -B "-Dtest.filter=$filter" test "$@"
fi
exec mvn -B test "$@"
