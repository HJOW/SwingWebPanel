# CSS-G 작업자 브리프 (P10-08: CSS3 모듈별 미구현 속성/선택자/규칙 구현·검증)

## 작업 위치
새 git worktree(격리)에서 작업한다. 브랜치는 시작할 때 만들어진 `worktree-agent-<id>` 이다. master(`20261117-061` 이후)에서 갈라진 상태다. 모든 명령은 그 worktree 디렉터리 안에서 실행한다(`pwd` 로 확인).

## 목표
AGENTS.md "P10-08. 기능 표의 CSS3 모듈별 미구현 속성/선택자/규칙을 다시 확인하고 각각 구현/검증한다. 기본 속성 검증만 통과한 상태를 CSS3 전체 완료로 표시하지 않는다." 를 진행한다.
`docs/progress/layout-f.md` 의 "P10-08 CSS3 모듈별 점검" 표가 출발점이다(읽을 것). 그 표의 "미구현/파싱만/미지원 이름" 항목을 **우선순위 순서로** 구현하고 각각 좌표/화소/계산 값 시험을 만든다. 상단 요구에 따라 3D 변환·WebGL 은 명시적 예외(EXC-03)이므로 구현하지 않는다. "효과 없어도 되는 것"으로 표에서 제외한 항목(`contain`, `will-change`, `cursor` 효과 등)은 그대로 둔다.

우선순위(앞에서부터 하고, 각각 끝낼 때마다 commit):
1. **Filter Effects 1**: `filter`(blur, brightness, contrast, drop-shadow, grayscale, hue-rotate, invert, opacity, saturate, sepia, `url()` 제외)와 `backdrop-filter` 는 제외(문서화). 오프스크린 그룹으로 그린다(opacity 그룹/`TransformGroup` 과 같은 구조). Java2D 공개 API 만 사용(`BufferedImage`, `ConvolveOp`, `RescaleOp`, 직접 픽셀 연산). 입력 히트 테스트에는 영향 없음.
2. **Compositing 1**: `mix-blend-mode`, `isolation`, `background-blend-mode`(규격의 separable/non-separable 블렌드 식을 직접 구현; `Composite` 구현).
3. **Masking 1**: `mask-image`(이미지/그라디언트), `mask-size/position/repeat/origin/clip/mode/composite`, `mask` 단축 속성, `clip-rule`, `clip-path` 의 `path()`/`rect()`/`xywh()` 와 `inset()` 모서리 반지름(남은 것). `mask-border` 는 제외 가능(문서화).
4. **논리 속성(CSS Logical Properties 1)**: `margin/padding/border-*-(inline|block)-(start|end)`, `inset-*`, `*-inline-size/block-size`, 단축 속성 — 쓰기 모드(`direction`, `writing-mode`)에 따른 물리 속성 매핑(`PropertyTable`/`Shorthands`).
5. **Fragmentation 3**: `break-before/after/inside`(multicol `MulticolLayouter` 와 연계), `box-decoration-break`.
6. **Shapes 1**: `shape-outside`(`circle/ellipse/inset/polygon`, `shape-margin`, 이미지 알파 `shape-image-threshold` 는 선택) — float 줄 배치(`InlineLayouter`/`BlockLayouter` float 처리).
7. **Pseudo 4 / UI**: `::placeholder`(폼 컨트롤 그리기와 연계), `::selection`(텍스트 선택 구현 후 — TEXTSEL 작업자가 하므로 계산 스타일만), `caret-color`, `accent-color`(계산 값 확인).
8. **Cascade 5 / Conditional**: `@layer`(순서 규칙), `@container`(크기 컨테이너 조건; 최소 `inline-size`/`width` 조건), `@property`(등록 속성의 구문/상속/초기값), CSS nesting(`&`), `@supports selector()`, `@scope`는 제외 가능.
9. **Counter Styles 3**: `@counter-style`, `symbols()`; **Scroll Snap 1**: 파싱과 스크롤 후 정렬 계산은 선택(문서화).
10. 남은 항목은 표에 한계로 적는다.

## 규격 조회
W3C CSS 규격은 `tools/webfetch` 로 조회한다(`tools/webfetch/AGENTS.md`; HTTPS 는 JDK 6 으로 불가하므로 `JAVA_HOME` 을 JDK 25(기본 `java`)로 두고 실행; 제품 검증은 JDK 6). 조회한 규격의 URL/판본/조회일을 `docs/progress/cssg.md` 에 적는다.

## 소유 경로
`src/org/duckdns/hjow/swingwebpanel/css/**`(속성 표 `css/props/PropertyTable`·`Shorthands`·`Prop`, 계산·상속·계단식 `css/cascade/**`, 값 파서 `css/values/**`), `src/org/duckdns/hjow/swingwebpanel/layout/**`, `src/org/duckdns/hjow/swingwebpanel/render/**` 의 이 기능과 직접 관련된 파일, 해당 시험(`tests/src/org/duckdns/hjow/swingwebpanel/css/**`, `layout/**`, `render/**`), `docs/progress/cssg.md`.
LAYOUT-F/LAYOUT-D 가 만든 구조(`LayoutEngine`, `PaintWalker`, `BoxCommands`, `OpacityGroup` 비슷한 그룹 구조 등)를 먼저 읽고 같은 방식으로 확장한다. `core/**`, `js/**` 는 건드리지 않는다(필요하면 "조정자 요청"). 공유 파일(`PropertyTable`, `Prop`, `PaintWalker`, `LayoutEngine`)은 작은 가산 변경만 하고 자주 commit 한다.
커밋 번호 대역: `20261117-801` 부터. 시험 이름 접두사 예: `FilterPaintTest`, `BlendModeTest`, `MaskPaintTest`, `LogicalPropertiesTest`.

## 기록
`docs/progress/cssg.md` 에: 구현한 속성 표(속성, 파싱, 계산, 배치/그리기 반영, 시험 이름), 한계, 조회 규격 판본, 시험 실행 명령과 실제 결과 수, 조정자 요청(FEATURE_MATRIX CSS-* 행 갱신 문구, AGENTS.md P10-08 체크 가능 여부).
