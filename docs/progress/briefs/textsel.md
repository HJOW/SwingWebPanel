# TEXTSEL 작업자 브리프 (P11-08: 텍스트 선택, 복사/붙여넣기, 페이지 확대)

## 작업 위치
새 git worktree(격리)에서 작업한다(`pwd` 로 확인). 브랜치는 시작할 때 만들어진 `worktree-agent-<id>`. master(`20261117-061` 이후)에서 갈라졌다.

## 목표
AGENTS.md "P11-08. 텍스트 선택, 캐럿, 복사/붙여넣기와 페이지 확대의 범위를 정하고 구현한다. 클립보드와 파일 선택 같은 OS 기능은 실제 UI 확인 결과를 별도 기록한다." 를 진행한다.
먼저 현재 상태를 읽고 범위를 `docs/progress/textsel.md` 에 정한다(무엇을 구현하고 무엇을 하지 않는지). 읽을 곳: `src/org/duckdns/hjow/swingwebpanel/dom/{Selection,Range,LiveRanges}.java`(DOM 선택은 이미 있음: `window.getSelection()`/`Range`), `core/{PageInput,PageKeyboard,ControlOverlays}.java`(마우스·키보드 입력, 폼 컨트롤 편집용 Swing 오버레이), `render/{RenderedPage,HitTester,HitResult,PagePainter}.java`(`HitResult.getTextNode()` 와 글자 오프셋 히트, 스케일 인수), `core/PageView.java`, `SwingWebPanel.java`, `docs/progress/forms*.md`, `docs/progress/render.md`.

구현할 것(우선순위 순서, 각각 끝나면 commit):
1. **텍스트 선택**: 마우스 드래그로 페이지 텍스트 선택(드래그 중 자동 스크롤은 선택), 더블 클릭=낱말, 트리플 클릭=문단(블록), Shift+클릭으로 확장, Ctrl+A 전체 선택, 선택은 DOM `Selection`/`Range` 와 같은 모델이다(스크립트의 `window.getSelection()` 이 사용자가 고른 선택을 보고, 스크립트가 만든 `Range` 를 `addRange` 하면 화면에 보임). `selectionchange` 이벤트(문서에 발생), `user-select: none` 존중(계산 값이 있는지 확인), 선택 강조 그리기(`::selection` 이 없으면 시스템 파란색 기본; 계산 스타일의 `::selection` 은 CSS-G 작업자 영역이므로 기본색만), 선택이 레이아웃 줄/인라인/변환/스크롤 컨테이너 안에서 맞는 상자로 그려지는지.
2. **복사**: Ctrl+C / Ctrl+Insert, `Toolkit.getDefaultToolkit().getSystemClipboard()` 에 `text/plain`(선택의 `toString()` 규칙: 블록 사이 줄바꿈, 표 셀은 탭 등 `innerText` 와 비슷한 규칙을 정하고 문서화)과 선택적으로 `text/html`(`DataFlavor.fragmentHtmlFlavor` 는 JDK 6 에 없으므로 문자열 flavor 로). `copy` DOM 이벤트(취소 가능; `clipboardData`/`ClipboardEvent` 최소). 헤드리스/클립보드 접근 실패(`HeadlessException`, `IllegalStateException`)는 조용히 무시하고 상태 메시지만. 로컬 폼 컨트롤 오버레이의 텍스트 편집(복사/잘라내기/붙여넣기)은 Swing 부품이 이미 처리하는지 확인하고 상태와 동기화되는지 확인.
3. **페이지 확대**: 확대/축소 배율(범위 0.25~5.0)을 컴포넌트의 확장 API(`setZoom(double)`/`getZoom()` 을 `SwingWebPanel` 에 가산; AGENTS 의 공개 메서드 목록은 그대로이며 이것은 추가 확장이다. `docs/API_CONTRACT.md` 에 넣을 문구는 "조정자 요청"에 적는다)와 Ctrl+마우스 휠, Ctrl+`+`/`-`/`0`. 렌더러는 이미 `scale` 인수가 있는지 확인하고, 입력 좌표/히트 테스트/스크롤 크기(`preferredSize`)/선택 그리기/폼 컨트롤 오버레이 위치가 같은 배율을 쓰게 한다. 뷰포트 폭은 CSS 픽셀 기준(`폭/배율`)으로 다시 배치한다(미디어 질의 영향 포함).
4. **캐럿**: 일반 페이지에는 캐럿 브라우징을 하지 않는다. 폼 컨트롤의 캐럿은 기존 오버레이가 맡는다. `contenteditable` 편집은 P20-06 범위이므로 하지 않는다(문서화).
5. 키보드로 선택 확장(Shift+화살표)은 선택 구현 뒤 시간이 되면.

## 검증
- DOM/모델 수준 시험(선택 좌표 → Range 경계, 낱말/문단 경계 계산, 복사 문자열 규칙, 배율 좌표 변환)은 자동 시험으로.
- 실제 `SwingWebPanel`(오프스크린 `paint`)로 선택 강조 화소 확인, `TestHttpServer` 통합 페이지.
- 실제 마우스·클립보드·한글 입력은 이 PC 에서 자동 확인이 안 되는 부분이므로 "자동 검증"과 "실제 UI 확인 필요"를 `docs/progress/textsel.md` 에 구분해 적는다. 클립보드는 실제 시스템 클립보드를 쓰는 시험을 하나 만들되 접근이 안 되면 건너뜀(이유 기록).

## 소유 경로
`src/org/duckdns/hjow/swingwebpanel/core/{PageInput,PageKeyboard,PageSelection*(새),PageZoom*(새)}.java` 등 입력/선택/확대 관련, `render/{SelectionPainter*(새)}`, `dom/Selection*` 확장은 가산 변경만, `SwingWebPanel.java`/`PageView.java`/`HtmlPageDocument.java` 는 작은 가산 변경만(FRAMES 작업자가 `core/{HtmlPageDocument,PageRuntime,PagePresentation,PageInput}` 에 가산 변경을 하고 있으니 자주 commit), 해당 시험, `docs/progress/textsel.md`.
커밋 번호 대역: `20261117-901` 부터.
