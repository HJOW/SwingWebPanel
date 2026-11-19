# LAYOUT-B 진행 기록 (P10-01 ~ P10-04)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/layout/**`, `src/org/duckdns/hjow/swingwebpanel/render/**`, `tests/src/org/duckdns/hjow/swingwebpanel/{layout,render}/**`, 이 파일.
시험 실행: `scripts/pkgtest.sh layoutb "layout render css html dom core net storage content media @root" "layout render"` (JDK 6u45).
린트: `tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/layout src/org/duckdns/hjow/swingwebpanel/render tests/src/org/duckdns/hjow/swingwebpanel/layout tests/src/org/duckdns/hjow/swingwebpanel/render`.

## 현재 상태 (사용자 중지 요청으로 멈춤, 2026-11-16)

- **코드 변경 없음.** 이 작업자는 기존 layout/render 코드를 읽고 설계만 했으며 소스/시험을 한 줄도 바꾸지 않았다.
- 검증(멈춘 시점): 위 명령으로 **74 passed / 0 failed / 0 skipped**(layout 44 + render 30, 실행기 자체 시험 포함 합계), sourcecheck 44 파일 0 위반. 빌드는 JDK 6 에서 컴파일된다.
- P10-01~04 는 **모두 미구현**(체크박스 후보 없음). float/position/table/flex/multicol/gradient/radius/shadow 는 아직 아무것도 들어가 있지 않다. 기존 동작(float·표·flex 상자는 정상 흐름의 보통 블록)이 그대로다.

## 읽고 파악한 구조(재개할 때 참고)

- `BoxBuilder` 가 DOM → `LayoutBox` 트리를 만든다. `Ctx.addBlock/addAtomic/flush` 가 인라인 런(`InlineItem` 목록)과 블록 자식을 나눈다. `isBfcRoot(style)` 는 이미 float/absolute/fixed/overflow/table/flex/grid/inline-block 을 BFC 루트로 표시한다. 요소가 `display:block` 이면 `block()` 으로 블록 상자가 되고 `float`/`position` 은 아직 보지 않는다(`LayoutEngine.assign` 에 relative 오프셋만 있음).
- `BlockLayouter.layoutBlockLevel(b, cbW, cbH)` → `finish()` → (인라인 항목이 있으면 `InlineLayouter.layout`, 아니면 `layoutChildren`). **자식은 먼저 배치하고 위치(`c.ly`)를 나중에 정한다**(margin 합치기 `Margin pend`). 좌표는 부모 border box 기준 `lx/ly` 이고 `LayoutEngine.assign` 이 문서 좌표 `x/y` 로 바꾼다.
- `InlineLayouter.Flow` 는 줄 너비로 `cw` 를 쓴다: 줄 맞춤 판정(약 659, 664, 747, 751행), 줄 배치(`buildLine` 의 `lo/hi`, 약 1033~1035행), 줄 상자(`lb.lx = contentLeft; lb.width = cw`, 약 1147~1149행). float 로 줄을 줄이려면 이 자리들을 줄별 `lineLeft/lineW` 로 바꾼다.
- 렌더러는 `DisplayListBuilder` 가 상자 트리를 순서대로 그리고(`자식 순서 = 그리기 순서`) `HitTester` 가 그 역순으로 찾는다. z-index/쌓임 맥락을 넣으면 두 곳이 같은 순서를 써야 한다.

## 재개 계획(권장 순서와 설계 결정)

### P10-01 float / position / z-index
1. **float 모델**: BFC 루트마다 `FloatContext`(좌/우 float 의 margin box 사각형을 BFC 좌표로 보관)를 둔다. `BlockLayouter` 가 현재 `fc` 와 content box 의 BFC 원점(`ox, oy`)을 필드로 유지하고 `layoutChildren` 이 자식 배치 전에 자식 border box 의 임시 BFC y(`oy + y + pend.value()`, 자식 안쪽 첫 자식과의 margin 합치기는 무시)를 넘긴다. 자식 배치 뒤 실제 `outTop` 이 임시 가정과 다르고 그 자식이 float 와 상호작용했으면(`usedFloats` 표시) **한 번만** 다시 배치하고 그 사이 `fc` 에 더한 float 는 되돌린다.
2. float 상자: `float:left|right` 인 블록은 shrink-to-fit 너비로 BFC 루트로 배치(`layoutAtomic` 계열 재사용)하고 CSS 2.1 9.5.1 규칙(위로 올라가지 않음, 앞선 float 보다 위에 놓이지 않음, 줄 상자보다 위에 놓이지 않음, 가로로 안 맞으면 아래로)으로 `fc.place`. 위치는 부모 content 원점으로 환산해 `lx/ly` 에 쓴다. 높이: BFC 루트(overflow:hidden, float, inline-block, 표 셀 …)는 자동 높이에 float 의 아래 margin edge 까지 포함한다.
3. 인라인 안의 float: `InlineItem.Kind.FLOAT` 추가(`BoxBuilder.add` 가 float 요소를 `addBlock` 대신 이 항목으로). `Flow` 는 현재 줄이 비어 있으면(또는 남는 폭에 들어가면) 현재 줄 윗면 y 에 float 를 놓고 이후 줄 폭을 줄이며, 아니면 다음 줄 시작에 놓는다. 줄 폭은 줄 윗면 y 에서 `fc.avail(y)` 로 구한다(줄 높이 안에서 시작하는 float 는 무시: 문서화된 단순화). 첫 단위가 남는 폭보다 넓으면 float 의 아래 가장자리로 줄을 내린다.
4. `clear`: `layoutChildren` 에서 자식 윗면 y 가 `fc.clearY(side)` 보다 위이면 y 를 내리고 대기 margin 을 버린다(clearance 가 margin 을 가림).
5. **position**: `absolute/fixed` 는 흐름에서 빼고(`layoutChildren` 이 건너뛰며 정적 위치 `staticX/staticY` 기록), 포함 블록(가장 가까운 positioned 조상의 padding box, 없으면 초기 포함 블록/뷰포트)이 끝난 뒤 CSS 2.1 10.3.7/10.6.4 로 너비/높이/오프셋을 풀어 `LayoutEngine.assign` 에서 `absCb` 기준으로 문서 좌표를 계산한다. `relative` 는 이미 `assign` 에 있으나 인라인 요소는 없음(`LayoutBox.relativeX/Y` 로 하위 트리도 이동하도록 점검). `sticky` 는 기본 구현(정적 위치 + 스크롤 시 고정은 렌더러 몫: 박스에 `stickyTop` 등 기록만)으로 문서화한다.
6. **쌓임 맥락/그리기 순서**: `DisplayListBuilder` 를 CSS 2.1 부록 E 순서(맥락 루트 배경/경계 → 음수 z-index 맥락 → 흐름 내 블록 배경 → float → 인라인 내용 → z-index auto/0 positioned 와 맥락(트리 순서) → 양수 z-index)로 바꾸고 `HitTester` 가 같은 순서의 역순을 쓰게 한다. 맥락 생성 조건: 루트, positioned + z-index ≠ auto, opacity < 1, transform 등. `visibility:hidden` 은 그리지 않되 자손 `visible` 은 그린다. overflow 클립/스크롤 상자는 `LayoutBox.clipsContent` 와 자신의 스크롤 범위(자식 경계 합집합)를 기록한다.
7. 시험: float/clear 좌표(줄 줄이기, 3열 float, clear both, float 와 margin 합치기, BFC 루트 높이), abspos 정적 위치/양쪽 오프셋/auto 너비, z-index 순서(그리기 pixels + `HitTester`), 화면 밖 요소 hit testing.

### P10-02 표
`display: table|inline-table|table-row-group|table-header-group|table-footer-group|table-row|table-column(-group)|table-cell|table-caption` 와 HTML 표 요소의 UA 시트(css 쪽에 이미 있음)를 이용한다. `BoxBuilder` 에서 익명 표 상자(행/셀 누락)를 만들고 `TableLayouter`(신규)에서 (1) 격자 구성(colspan/rowspan 점유표), (2) 자동 표 배치(셀 min/max-content 로 열 너비 분배: 명시 너비/백분율/auto, 표 너비 제약), 고정 표 배치(첫 행/col 너비), (3) 행 높이(rowspan 분배, `vertical-align` 위/가운데/아래/baseline), (4) `border-collapse: separate|collapse`(충돌 해결 규칙) + `border-spacing` + `empty-cells`, caption 배치. 고정 폭 격자만 두는 구현은 완료로 치지 않는다.

### P10-03 flexbox / multicol
`FlexLayouter`(신규): CSS Flexible Box Level 1 알고리즘(9장: 주/교차축, 줄 나누기, flex-basis/grow/shrink 해결 루프(고정/탄력 분류, min/max 위반 처리), order, gap, justify-content/align-items/self/content, auto margin, baseline 정렬, intrinsic 크기). `MulticolLayouter`(신규): column-count/width/gap/rule/fill(balance), 내용을 열 높이로 나누어 이어 놓기(줄/블록 경계), `column-span: all`. 각 모듈을 따로 시험한 뒤 섞는다. `writing-mode` 세로쓰기는 별도 추적(미구현으로 문서화).

### P10-04 시각 효과(렌더러)
`DisplayCommands` 에 gradient(linear/radial/conic, 색 정지점, 반복), 다중 배경(position/size/repeat/clip/origin, attachment 는 scroll 만), `border-radius`(타원 모서리, 배경/경계/자식 클립), `box-shadow`(외부/내부, 블러는 가우시안 근사 컨볼루션), `opacity` 는 이미 그룹이 있으니 radius 클립과 결합. CSS 쪽 값 자료형(`css.values`)은 이미 gradient/shadow 를 파싱한다고 가정하되 `docs/progress/css.md` 속성 표로 확인할 것(34개 속성은 파싱·저장만).

## 다음 단계(재개하는 AI 에게)
1. 위 명령으로 현재 74건이 통과하는지 먼저 확인한다.
2. P10-01 의 1~4(float/clear)부터 작은 단위로 구현·시험·commit 한다(`InlineLayouter.Flow` 수정이 가장 위험하므로 줄 너비 접근을 `lineLeft/lineW` 두 필드로 모으는 리팩터링만 먼저 하고 기존 44건이 통과하는지 확인).
3. 이어서 5~7, P10-02, P10-03, P10-04 순서로 진행한다. 조정자 파일(AGENTS.md, PROGRESS, FEATURE_MATRIX 등)은 수정하지 않는다.
4. 공개 API(`LayoutEngine.layout`, `LayoutResult`, `PageRenderer.render`, `RenderedPage`, `HitTester`)는 INTEG-A 가 사용하므로 추가만 하고 바꾸지 않는다.
