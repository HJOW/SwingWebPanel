# LAYOUT-C 진행 기록 (P10-01 float/위치 지정/쌓임/자르기, P10-02 표)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/layout/**`, `src/org/duckdns/hjow/swingwebpanel/render/**`, `tests/src/org/duckdns/hjow/swingwebpanel/{layout,render}/**`, 이 파일.
시험 실행: `scripts/pkgtest.sh layc "layout render css html dom net" "layout render"` (JDK 6u45; `media`/`core` 를 src 목록에 넣으면 다른 작업자의 미완성 파일이 컴파일되므로 뺀다).
린트: `tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/layout src/org/duckdns/hjow/swingwebpanel/render tests/src/org/duckdns/hjow/swingwebpanel/layout tests/src/org/duckdns/hjow/swingwebpanel/render`.
참고 규격: CSS 2.1 (`https://www.w3.org/TR/CSS21/`, 조회 2026-11-16, `tools/webfetch`, 최종 URL `https://www.w3.org/TR/CSS2/visuren.html`·`tables.html`, Last-Modified 2016-04-12): 9.5.1 float 배치 규칙, 9.5.2 clear, 10.3.7/10.6.4 절대 배치 방정식, 17 표.

## 작업 기록

### 2026-11-16 P10-02 표 레이아웃·합치기 border·그리기 (P10-02 후보, 한계 명시)
완료한 내용:
- 상자 트리(`BoxBuilder`): `display:table|inline-table` 은 `LayoutBox.Type.TABLE`, 내부 부품은 `TABLE_CAPTION/TABLE_ROW_GROUP/TABLE_ROW/TABLE_CELL/TABLE_COLUMN_GROUP/TABLE_COLUMN`. CSS 2.1 17.2.1 익명 상자: 표 안의 홀로 있는 셀/행/콘텐츠는 익명 행 그룹·행·셀로 감싸고, 표 밖의 `display:table-*` 형제들은 익명 표로 감싼다(공백만 있는 텍스트는 버림). 첫 `thead` 는 맨 앞, 첫 `tfoot` 는 맨 뒤(두 번째부터는 본문). `colspan/rowspan/span` 은 HTML `td/th/col/colgroup` 속성에서만 읽는다(`intAttribute`: 1~1000 / 0~65534).
- `TableModel`(신규): 격자(다음 빈 열, rowspan 은 행 그룹 끝까지, rowspan=0 은 그룹 끝까지, 최대 열 1000/격자 200만 칸), `col/colgroup` 열 정의, border-spacing, **border-collapse:collapse 충돌 해결**(hidden > 너비 > 스타일 double>solid>dashed>dotted>ridge>outset>groove>inset > 요소 셀>행>행 그룹>열>열 그룹>표 > 왼쪽/위쪽), 안쪽 격자선은 걸친 셀 안에서는 없음.
- `TableLayouter`(신규): 열 측정(셀 min/max-content + padding/border, 지정 너비=선호 너비, 백분율, `col` 너비, colspan 셀은 모자란 만큼만 비례 분배), 표 너비(shrink-to-fit/지정 너비 ≥ 최소, min/max-width), **자동 분배**(최소 → 백분율 → 너비 지정 열 → 자동 열 → 남는 것은 자동 열에 최대 너비 비례), **고정 레이아웃**(너비 지정 시 `col` → 첫 행 셀 → 나머지 균등, 내용이 최소 너비를 정하지 않음), 셀 배치(`finish`, 셀 height 는 최솟값), 행 높이(가장 큰 셀, 행 height, 기준선 정렬, rowspan 부족분 균등 분배, 표 height 부족분 행 높이에 비례 분배), 셀 세로 정렬(top/middle/bottom/baseline: 내용 이동), caption(위/아래, 표 너비로 배치, 표 높이에 포함), inline-table 기준선, 합쳐진 border 선분(`TableBorder`, 같은 이웃은 한 선분으로 합침, 가로 선분은 모서리를 채우도록 양 끝을 세로 border 너비의 절반만큼 확장), 절대 배치/float/relative/인라인 표.
- 합치기 모델에서 표 border = 바깥 합쳐진 border 의 절반, padding 0, 셀 border = 합쳐진 border 의 절반(격자선이 표/셀 경계 중심이므로 border 는 표 border box 안에 있다). `LayoutBox.getGridRectangle()`(표 배경/border 는 caption 을 뺀 격자), `isCollapsedBorderModel()`, `getCollapsedBorders()`, `isHiddenEmptyCell()`(`empty-cells:hide`), `getTableRow/Column/RowSpan/ColumnSpan`.
- 렌더러/워커: `PaintWalker` 가 표 부품을 정상 흐름 블록으로 훑고(표 → caption → 열 그룹 → 열 → 행 그룹 → 행 → 셀 = 부록 17.5.1 계층), 셀 배경 뒤에 `Visitor.tableBorders` 로 합쳐진 border 를 그린다. `DisplayListBuilder.decorate` 가 표는 격자 사각형에, 합치기 모델의 표/셀은 border 를 건너뛰고 `empty-cells:hide` 빈 셀은 배경/border 를 건너뛴다.
- `BlockLayouter`: `layoutBlockLevel/layoutAtomic/atomicOuterWidth/intrinsic/edges` 에 TABLE 연결, 셀의 `height` 는 최솟값. 표는 BFC 루트이므로 float 옆에서는 좁아지거나 아래로 내려간다.
검증 결과: `scripts/pkgtest.sh layc "layout render css html dom net" "layout render"` (JDK 6u45) → **153 passed / 0 failed**(기존 74 + P10-01 의 FloatLayoutTest 19, PositionLayoutTest 15, StackingPaintTest 10 + P10-02 의 TableLayoutTest 22, TableBorderLayoutTest 6, TablePaintTest 7; 기반 시설 7건 포함). 다른 작업자의 미완성 js 파일 때문에 작업 트리에서는 core 를 함께 컴파일할 수 없어, HEAD(b6186b9)의 임시 worktree 에서 `scripts/pkgtest.sh wtcore "layout render css html dom net core storage content js @root" "layout render core"` → **249 passed / 0 failed / 1 skipped**(JarContentTest: 제품 JAR 없음). sourcecheck 0 위반.
  시험 범위: 기본 격자와 행/열 크기, border-spacing(가로/세로), colspan/rowspan(걸친 열/행 확장), 자동 분배(최대 너비 비례), shrink-to-fit/최소 너비, 백분율 열, 고정 레이아웃(첫 행/남는 열/넘침), 행 높이와 표 height 분배, vertical-align 4종, caption 위/아래, 익명 상자(table-cell 만 있는 div, 홀로 있는 텍스트), thead/tfoot 순서, `col` 너비/span, HTML 속성(border/cellspacing/cellpadding/width), 중첩/float 안 표, auto margin 가운데, float 옆 표, 1000 셀 표 속도, 퇴화 표(빈 표, caption 만, 셀 없는 행, rowspan 한계), inline-table 기준선, relative/absolute/float 표, 텍스트 정렬/셀 늘림, 간격에서의 부품 기하; 합치기: 기하(반 border), 6가지 충돌 규칙(너비/스타일/hidden/요소/위치/행), 분리 모델 유지, 열/행 그룹 border, 걸친 셀 안쪽 border 없음, caption 아래 시작; 그리기(화소): 표/행/셀 계층과 히트, caption 은 표 배경 밖, `empty-cells:hide`, 합쳐진 border 의 색/두께/위치, 더 넓은 border 승리, 셀 내용이 위, 행 그룹이 열 위.
알려진 한계(P10-02):
1. `visibility:collapse` 인 행/열은 크기를 줄이지 않고 숨김처럼 그리기만 건너뛴다(CSS 2.1 17.5.5 미구현).
2. 열 너비 분배는 CSS 2.1 의 비규정 자동 알고리즘을 따른 근사이며 브라우저와 소수점 단위로 같지 않다(백분율 열/너비 지정 열/자동 열 우선순위, colspan 셀의 백분율과 지정 너비는 선호 너비로만 반영). 백분율 `height` 는 지원하지 않는다(auto 로 취급).
3. 합쳐진 border 의 절반 너비를 정수로 반올림하지 않는다(브라우저는 반올림). 모서리는 가로 선분을 세로 border 너비의 절반만큼 늘리는 단순 처리이며 마이터/우선순위 모서리 처리가 아니다. groove/ridge/inset/outset 의 밝기는 선분 방향(가로=위쪽, 세로=왼쪽 변)으로만 정해진다.
4. rowspan 셀의 기준선 정렬은 위쪽 정렬로 대신한다. 행/행 그룹/열에는 `position:relative` 오프셋을 적용하지 않는다(표와 셀만).
5. 표 배경 이미지의 기준(origin)은 caption 을 포함한 표 상자이다(배경색/border 는 격자 기준).
6. 행 그룹의 히트 영역은 행 사이의 간격을 포함한다(행 그룹 요소가 간격 위의 클릭을 받음).
7. `table-layout:fixed` 는 `width` 가 auto 가 아닐 때만 적용(CSS 2.1 과 같음).
체크박스 후보: **P10-02**(열 폭, 행 높이, 셀 병합, 경계선 모델(분리/합치기), caption, 고정/자동 표 레이아웃, 셀 세로 정렬, 익명 상자 — 위 한계 1~7 은 문서화된 미지원 세부 동작이며 "HTML 테이블 전체 지원"으로 표시하지 않는다. `visibility:collapse` 와 백분율 높이는 미구현으로 남는다).
조정자 요청: 없음(공개 API 는 추가만: `LayoutBox` 표 getter, `TableBorder`, `PaintWalker.Visitor.tableBorders`). 기능 표(HTML/CSS 표 모듈 행)의 구현 클래스에 `layout/TableModel`, `TableLayouter`, `TableBorder`, `BoxBuilder` 를 적어 달라.

### 2026-11-16 P10-01-b 쌓임 순서(부록 E)·자르기·fixed·히트 테스트·인라인 relative (P10-01 완료 후보)
완료한 내용:
- `layout/PaintWalker`(신규, 공개): 상자 트리를 CSS 2.1 부록 E 순서(맥락 자신의 배경/border → z<0 맥락 → 정상 흐름 블록 배경 → float(통째로) → 인라인 콘텐츠(inline-block 은 통째로) → z=auto/0 위치 지정 상자와 맥락(트리 순서 = `LayoutBox.getOrder`) → z>0)로 훑어 `Visitor`(decorate/content/beginClip/endClip/beginOpacity/endOpacity/beginFixed/endFixed)에 알린다. 맥락 = 루트, 정수 z-index 의 위치 지정 상자, fixed, opacity<1, transform≠none. 자르기는 **포함 블록 사슬** 기준(`overflow` 가 visible 이 아닌 상자는 포함 블록 사슬에 있는 자손만 자르고, 포함 블록이 바깥인 절대 배치 상자는 자르지 않음), `clip: rect()` 는 절대 배치 상자 자신을 자른다. 불투명도 0 은 아무것도 그리지 않는다.
- `layout/PaintSequence`(신규): 같은 순회를 기록해 `LayoutResult.boxAt(x, y[, scrollX, scrollY])` 의 근거로 쓴다(마지막으로 점을 포함하고 모든 자르기 안에 있는 상자; `visibility:hidden`/`pointer-events:none` 제외; fixed 항목은 스크롤을 뺀 점과 비교). 기존 "깊이 우선 마지막 자식" 방식을 대체(공개 API 는 그대로, 오버로드만 추가).
- `LayoutResult`: 스크롤 영역 = fixed 상자 제외, 자르는 상자 안으로 제한(절대 배치 상자는 자기 포함 블록 사슬 기준).
- `LayoutEngine.assign`: 인라인 요소의 `position:relative` 가 그 안의 텍스트/원자적 상자/조각을 옮김(`LayoutBox.inlineOwner`).
- 렌더러: `DisplayListBuilder` 가 `PaintWalker.Visitor` 를 구현(기존 blockBackgrounds/inlineContent/emitUnit 제거), `DisplayCommand.Type.FIXED_START/FIXED_END` + `PagePainter`/`PaintContext`(스크롤을 되돌려 뷰포트에 고정), `HitTester.hit(x, y, scrollX, scrollY)`, `RenderedPage.hitTest` 가 스크롤을 전달.
검증 결과: `scripts/pkgtest.sh layc "layout render css html dom net" "layout render"` (JDK 6u45) → **118 passed / 0 failed**(기존 74 + FloatLayoutTest 19 + PositionLayoutTest 15 + StackingPaintTest 10). core/js 를 포함한 `scripts/pkgtest.sh layc2 "layout render css html dom net core storage content js @root" "layout render core"` → 207 passed / 0 failed. sourcecheck 0 위반.
  StackingPaintTest: z-index 순서(소스 순서와 무관, 없으면 나중이 위), 음수/0/양수 맥락 순서(화소 + 히트), float 가 블록 배경 위·텍스트/위치 지정 아래(화소 + 디스플레이 리스트 순서), 포함 블록 사슬 기준 자르기(화소 + 히트), overflow:hidden 자르기(옆으로 넘치는 콘텐츠 포함, border 는 상자 몫), `clip: rect()`, fixed 상자(스크롤 0/500 에서 같은 뷰포트 위치, 히트도 스크롤을 따름), 화면 밖 상자(−500, 3000 좌표)의 히트와 스크롤 영역, visibility:hidden/pointer-events:none, 불투명도 맥락 안 위치 지정 자식.
알려진 한계(P10-01):
1. 줄 너비는 줄 위쪽 높이(strut 줄 높이 띠)의 float 만 반영한다: 줄 높이 안에서 더 낮게 시작하는 float 는 줄을 줄이지 않는다.
2. `position:sticky` 는 오프셋 없는 relative 로 배치(스크롤 따라 붙지 않음). `position:fixed` 의 레이아웃 좌표는 스크롤 0 의 뷰포트 기준이고 JS `getBoundingClientRect`(HitTester.getClientRects)는 스크롤을 반영하지 않는다.
3. 절대 배치 상자의 포함 블록은 위치 지정된 **블록/원자적 상자**만 된다(위치 지정 인라인 요소는 포함 블록이 아님: 그 인라인 조각들의 합집합 상자를 쓰지 않음). `direction:rtl` 에서 절대 배치 방정식/정적 위치는 왼쪽에서 오른쪽만 구현.
4. `overflow:scroll/auto` 는 스크롤바 없이 자르기만 한다(상자 안 스크롤은 P11). 자르기는 직사각형(padding box)이며 `border-radius` 자르기는 P10-04.
5. 예측 못한 margin 합치기로 위치 추정이 틀리면 하위 트리를 한 번 더 배치하며 한 번의 배치에서 4000회로 제한한다(초과 시 추정 위치로 배치되어 float 옆 줄 너비가 어긋날 수 있음).
6. transform 이 있는 상자는 쌓임 맥락으로만 취급하고 변환 자체는 적용하지 않는다(P10-06).
조정자 요청: `core/PageInput` 이 `page.hitTestDocument(x, y)` 로 문서 좌표만 넘긴다. 스크롤된 페이지에서 fixed 상자를 정확히 찾으려면 `page.hitTest(screenX, screenY, scrollX, scrollY, scale)` 를 쓰거나 `HitTester.hit(docX, docY, scrollX, scrollY)` 를 호출하도록 바꿔야 한다(core 소유자 몫).
체크박스 후보: **P10-01**(float/clear, 상대/절대/고정 배치, 포함 블록, 쌓임 맥락/z-index, overflow/clip, 화면 밖 요소의 표시와 히트 좌표 — 위 한계는 문서화된 단순화이며 미구현이 아니라 규격의 일부 세부 동작).
다음 작업: P10-02 표.

### 2026-11-16 P10-01-a float/clear/위치 지정 레이아웃 
완료한 내용(레이아웃 쪽):
- `FloatContext`(신규): 블록 서식 문맥(BFC)별 float 사각형 목록(BFC 좌표). `place`(9.5.1: 가능한 한 높이, 앞선 float 위로 안 감, 안 들어가면 방해하는 가장 낮은 float 아래로), `leftEdge/rightEdge`(줄 너비), `clearY`, `nextBottom`, `maxBottom`, `truncate`.
- `BoxBuilder`: float/절대/고정 요소를 `InlineItem.Kind.FLOAT/ABS` 항목(흐름 밖)으로 만든다(블록화, 텍스트 장식 비전달). 위치 지정 상자는 `LayoutBox.posKind/hasZ/zIndex/stackingContext/order/clipRect`, 절대 배치 상자는 포함 블록(가장 가까운 위치 지정 조상 상자, 없으면 초기 포함 블록 목록)의 `absList` 에 등록. `::before/::after` 의 float/절대도 처리.
- `BlockLayouter`: BFC 루트마다 새 `FloatContext`, 높이 auto 인 BFC 루트는 안쪽 float 아래 margin 가장자리까지 늘어남. `layoutChildren` 이 자식 위치(BFC 좌표)를 먼저 추정(첫 자식 사슬의 margin `chainMargin`)하고 틀리면 float 가 있을 때만 한 번 더 배치(float 목록을 되돌림, 한 번의 배치에서 최대 4000회). `clear`(상자를 float 아래로, clearance 는 앞선 margin 과 합쳐지지 않음), BFC 루트 자식(`overflow:hidden`, 표, 블록 수준 치환)은 float 옆 공간으로 좁아지거나 아래로 이동(`besideFloats`). `finish` 가 이전 배치가 만든 줄/float/절대 상자/표식을 지운 뒤 다시 만든다.
- `InlineLayouter`: 줄 너비/왼쪽을 줄 위쪽 높이의 float 로 구함(`availL/availW`, strut 높이 띠), 빈 줄에 안 들어가는 단위는 float 아래로 내림, 줄 중간에서 만난 float 는 내용(대기 단위 포함)이 새 너비에 들어가면 줄 위쪽, 아니면 다음 줄 위쪽(`pendingFloats`), 절대 배치 항목은 정적 위치 기록, 측정(dry) 실행에서는 float 를 원자적 상자처럼 너비에 더함.
- `AbsoluteLayouter`(신규): CSS 2.1 10.3.7/10.6.4(오프셋 7가지 조합, shrink-to-fit, auto margin 가운데 정렬, 제약 초과 시 right/bottom 무시, min/max 는 한계를 크기로 삼아 재계산, top+bottom+height:auto 는 채움), 치환 요소(10.3.8/10.6.5 단순화), 포함 블록 = 위치 지정 조상의 padding box 또는 초기 포함 블록/뷰포트(fixed 도 스크롤 0 의 뷰포트). 결과는 부모 기준 `lx/ly` 로 저장.
- `LayoutBox`: `getFloatSide/isFloat/getPositionKind/isPositioned/isAbsolutelyPositioned/isFixedPosition/getContainingBlock/hasZIndex/getZIndex/isStackingContext/getOrder/getClipRectangle` 추가(추가만 했고 기존 API 는 그대로).
검증 결과: `scripts/pkgtest.sh layc "layout render css html dom net" "layout render"` (JDK 6u45) → 104 passed / 0 failed(기존 74 + FloatLayoutTest 17 + PositionLayoutTest 13). sourcecheck 0 위반.
미해결/다음: (1) 렌더러가 float/위치 지정/z-index 를 CSS 2.1 부록 E 순서로 그리지 않음(현재는 트리 순서), 히트 테스트도 같은 순서 필요(PaintWalker). (2) fixed 의 스크롤 보정, `clip: rect()`, 스크롤 영역에서 fixed/자르기 제외. (3) 인라인 요소 `position:relative`. 
