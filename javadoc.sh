#!/bin/sh
# 서브프로젝트별 Javadoc을 저장소의 docs/javadoc 아래에 생성한다.
set -e
cd "$(dirname "$0")"
. ./scripts/jdk6.sh
JAVADOC="$JAVA6_HOME/bin/javadoc$EXE"
if [ ! -x "$JAVADOC" ]; then
  echo "[ERROR] javadoc not found under JAVA6_HOME=$JAVA6_HOME" >&2
  exit 1
fi

mkdir -p build/javadoc
engine_sources=engine/src/main/java
browser_sources=browser/src/main/java

generate_docs() {
  module=$1
  source_dir="$module/src/main/java"
  source_path="$source_dir${2:+$PATHSEP$2}"
  source_list="build/javadoc/$module.args"
  output_dir="docs/javadoc/$module"

  # 문서화 대상은 현재 모듈의 소스 파일만 목록에 넣는다.
  find "$source_dir" -type f -name '*.java' -exec printf '"%s"\n' {} + > "$source_list"
  if [ ! -s "$source_list" ]; then
    echo "[ERROR] No Java sources found in $source_dir" >&2
    exit 1
  fi

  echo "[INFO] Generating Javadoc for $module in $output_dir"
  "$JAVADOC" -quiet -source 1.6 -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 \
    -notimestamp -d "$output_dir" -sourcepath "$source_path" "@$source_list"
}

generate_docs engine
generate_docs tls13
generate_docs browser "$engine_sources"
generate_docs examples "$browser_sources$PATHSEP$engine_sources"
generate_docs desktopapp "$browser_sources$PATHSEP$engine_sources"
