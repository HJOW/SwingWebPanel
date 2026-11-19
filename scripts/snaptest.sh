#!/bin/sh
# 다른 작업자의 미완성 소스에 영향을 받지 않고 시험하는 스냅샷 실행기.
# git HEAD 를 build/snap-<이름>/ 에 풀고, 호출자가 소유한 경로만 작업 트리의 현재 내용으로 덮어쓴 뒤 그 안에서 scripts/pkgtest.sh 를 실행한다.
# 사용: scripts/snaptest.sh <이름> "<소유 경로들(저장소 루트 기준, 공백 구분)>" "<src/org/duckdns/hjow/swingwebpanel 하위 패키지들>" "<tests/src/org/duckdns/hjow/swingwebpanel 하위 패키지들>" [TestRunner 인수...]
# 예)   scripts/snaptest.sh web "src/org/duckdns/hjow/swingwebpanel/js/host tests/src/org/duckdns/hjow/swingwebpanel/js/host" "js dom html css net storage core content layout render download media @root" "js core" --filter org.duckdns.hjow.swingwebpanel.js.host
# 결과는 build/snap-<이름>/build/pkg-<이름>/ 에 만들어진다. 스냅샷 전체 컴파일은 몇 분 걸릴 수 있다.
cd "$(dirname "$0")/.." || exit 1
if [ $# -lt 4 ]; then
  echo "usage: scripts/snaptest.sh <name> \"<owned paths>\" \"<src subpackages>\" \"<test subpackages>\" [TestRunner args]" >&2
  exit 2
fi
NAME="$1"; OWNED="$2"; shift 2
SNAP="build/snap-$NAME"
rm -rf "$SNAP"; mkdir -p "$SNAP"
git archive HEAD | tar -x -C "$SNAP" || exit 1
# 소유 경로를 현재 작업 트리의 내용으로 덮어쓴다(새 파일 포함). 없어진 파일은 스냅샷에서도 지운다.
for p in $OWNED; do
  # "<스냅샷 경로>=<원본 파일>" 형식이면 작업 트리 대신 지정한 원본 파일(예: git show HEAD:경로 에 부분 수정을 한 사본)을 그 경로에 덮어쓴다.
  case "$p" in
    *=*)
      dest="${p%%=*}"; srcfile="${p#*=}"
      mkdir -p "$SNAP/$(dirname "$dest")"
      cp "$srcfile" "$SNAP/$dest"
      continue
      ;;
  esac
  if [ -e "$p" ]; then
    rm -rf "$SNAP/$p"
    mkdir -p "$SNAP/$(dirname "$p")"
    cp -r "$p" "$SNAP/$p"
  fi
done
cd "$SNAP" && sh scripts/pkgtest.sh "$NAME" "$@"
