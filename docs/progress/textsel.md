# TEXTSEL 진행 기록 (P11-08: 텍스트 선택, 복사/붙여넣기, 페이지 확대, 캐럿의 범위)

작업자: TEXTSEL. 커밋 번호 대역 `20261117-901` 부터. 소유 경로: `core/{PageInput,PageKeyboard,PageSelection,PageZoom,ZoomControl}`, `render/{TextSelections,SelectionHighlight}`,
`dom/{Selection,Range,SelectionListener,ClipboardEvent,ClipboardData}`(가산 변경만), 해당 시험, 이 문서. `SwingWebPanel.java`/`PageView.java`/`ParsingPageLoader.java`/`ControlOverlays.java`/`ControlEditor.java` 는 작은 가산 변경만 한다.

## 조정자 요청 (AGENTS.md / FEATURE_MATRIX / API_CONTRACT 에 넣을 문구)

**AGENTS.md P11-08**: 범위를 정하고(1절) 구현했으며 자동 검증(4.1)은 통과했다. "클립보드와 파일 선택 같은 OS 기능은 실제 UI 확인 결과를 별도 기록한다" 는 4.2 에 "미확인" 항목으로 적었다. 실제 UI 확인(마우스, 클립보드의 서식 있는 붙여넣기, IME)이 필요한 항목이 남아 있으므로 **체크 후보이되 조정자가 4.2 를 받아들일지 판단**한다(P11-02/P11-10 의 한글 IME 확인과 같은 조건). 하위 항목으로 나누면: 선택(마우스/키보드/`selectionchange`/`user-select`/강조), 복사(`copy` 이벤트, 문자열 규칙, `text/plain`+`text/html`), 확대(`setZoom/getZoom`, Ctrl+휠, 키), 캐럿(범위 문서화: 일반 페이지 본문은 캐럿 없음, `contenteditable` 은 P20-06).

**FEATURE_MATRIX** (P11-08 행과 `Selection`/`Range` 행): 구현 클래스 `render/{TextSelections,SelectionHighlight}`, `core/{PageSelection,PageZoom,ZoomControl,ClipboardSink}`, `dom/{SelectionListener,ClipboardEvent,ClipboardData}`, `js/dom/ClipboardBindings`; 지원 상태 "부분 구현(DOM 선택 모델 + 마우스/키보드 선택 + 강조 + 복사 + 확대: 자동 검증 완료. 실제 마우스·클립보드 서식·IME 확인 안 함. `::selection`, 낱말의 노드 경계, 세로쓰기 Shift+위/아래, `cut`/`paste`/`contenteditable` 미구현)". `Selection`/`Range` 행의 "화면 선택/클립보드는 P11-08" 문구를 이 구현으로 갱신.

**API_CONTRACT** (공개 메서드 목록은 그대로, 추가 확장으로 기록):
- `public double getZoom()`: 페이지 확대 배율(기본 1.0, 범위 0.25~5.0). 어느 스레드에서나 호출 가능, 마지막으로 요청한 값을 반환한다. 폐기 뒤에도 마지막 값을 반환한다.
- `public void setZoom(double)`: 범위 밖은 끝값으로 자른다. NaN/무한/0 이하이면 `IllegalArgumentException`, 폐기 뒤에는 `IllegalStateException`. EDT 에서 적용하고(다른 스레드의 호출은 큐에 들어감) `PROPERTY_ZOOM("zoom")` 속성 변경(Double)을 EDT 에서 알린다. 레이아웃은 `폭/배율`, `높이/배율` 의 CSS 뷰포트로 다시 하고(미디어 질의와 `innerWidth` 포함, `resize` 이벤트), 그리기·마우스 좌표·스크롤 크기·선택 강조·폼 편집기가 같은 배율을 쓴다. 컴포넌트의 설정이므로 `clean()`/페이지 이동이 바꾸지 않고, `getPage()/getContent()/이력/진행률`에 영향이 없다. Ctrl+휠, Ctrl+`+`/`=`/`-`/`0` 도 같은 메서드를 부른다.
- `protected void copyToClipboard(String plainText, String htmlFragment) throws Exception`: 사용자가 선택을 복사했을 때(Ctrl+C/Ctrl+Insert) `copy` 이벤트를 취소하지 않았다면 EDT 에서 호출된다. 기본은 시스템 클립보드에 `text/plain`(과 `text/html` 이 있으면 그것)을 쓴다. 예외는 상태 메시지로만 알린다. 호스트와 시험이 재정의한다(`createDownloadPopup` 과 같은 방식).
- 선택·복사에는 새 공개 메서드가 없다(`window.getSelection()` 이 사용자의 선택). `removeHyperlinkEvents()`/`dispose()` 의 제거 대상 표: 내부 `PageSelection`/`PageInput` 리스너는 dispose 때 제거되며 선택 강조와 드래그 상태는 페이지가 닫힐 때(`PageSelection.pageClosed`) 버려진다.

**ARCHITECTURE**: 위 3절의 표를 `core`/`render`/`dom` 행에 반영.

**PROGRESS**: 다음 작업 후보는 6절.

## 1. 범위 (P11-08 "텍스트 선택, 캐럿, 복사/붙여넣기와 페이지 확대의 범위를 정하고 구현한다")

AGENTS.md 의 공개 메서드 목록은 바꾸지 않는다. 아래의 `setZoom/getZoom` 은 추가 확장이며 기존 공개 메서드의 동작은 그대로이다.

| 항목 | 구현한다 | 구현하지 않는다(이유) |
| --- | --- | --- |
| 텍스트 선택 | 마우스 드래그, 더블 클릭(낱말)과 트리플 클릭(문단), 그 클릭 뒤 드래그(낱말/문단 단위 확장), Shift+클릭 확장, Ctrl+A 전체 선택, Shift+화살표/Home/End 확장(시간이 되면), 드래그 중 자동 스크롤, `user-select: none/all/text/auto` 존중, 선택 강조 그리기(시스템 파란색 기본) | `::selection` 스타일(CSS-G 몫이며 계산 스타일만 있다), 다중 범위(Firefox 식), 선택한 텍스트의 끌어다 놓기(P20-06), iframe 안의 선택(FRAMES 작업) |
| 선택 모델 | 기존 `dom.Selection`/`dom.Range` 하나가 사용자 선택과 스크립트 선택을 모두 나타낸다(`window.getSelection()` 이 사용자가 고른 선택을 보고, 스크립트의 `addRange` 가 화면에 보인다). 문서에 `selectionchange`(비버블, 취소 불가, 묶어서 한 번)와 `selectstart`(취소 가능) 이벤트 | `Selection.toString()`(스크립트)은 계속 `Range.toString()`(원문 텍스트 노드 데이터)이다. 복사 문자열(렌더링 기준)과 다르다 |
| 복사 | Ctrl+C, Ctrl+Insert 로 시스템 클립보드에 `text/plain`(렌더링 기준 문자열)과 `text/html` 조각, `copy` DOM 이벤트(취소 가능, `clipboardData.setData/getData` 최소), 실패(헤드리스, 접근 거부)는 조용히 무시하고 상태 메시지 | `cut`/`paste` 의 페이지 본문 처리(편집할 수 없는 본문이므로 의미가 없다. `contenteditable` 은 P20-06), 서식 있는 클립보드의 다른 형식, 컨텍스트 메뉴 |
| 붙여넣기 | 폼 컨트롤의 Swing 편집기(`ControlEditor`)가 Swing 기본 키 바인딩으로 복사/잘라내기/붙여넣기를 한다(이미 있음; 실제 클립보드 확인은 사람이 필요) | 페이지 본문으로의 붙여넣기 |
| 페이지 확대 | `SwingWebPanel.setZoom(double)`/`getZoom()`(범위 0.25~5.0, 범위 밖은 끝값으로 자른다), Ctrl+휠, Ctrl+`+`/`-`/`0`. 레이아웃은 CSS 뷰포트(`폭/배율`)로 다시 하고 그리기, 히트 테스트, 스크롤 크기, 선택 강조, 폼 편집기 위치가 같은 배율을 쓴다 | 핀치 확대, 텍스트만 확대, 확대 배율의 저장(origin 별 기억) |
| 캐럿 | 일반 페이지 본문에는 캐럿 브라우징(F7)을 하지 않는다. 폼 컨트롤의 캐럿은 기존 `ControlEditor` 가 맡는다. 캐럿은 선택이 접힌 상태(`Selection.type == "Caret"`)로만 나타내며 화면에는 그리지 않는다 | `contenteditable` 편집(P20-06) |

## 2. 규칙

### 2.1 좌표
- 컴포넌트 좌표(마우스 이벤트) = 문서 좌표 × 배율(`PageView.getZoom()`). 스크롤은 `JScrollPane` 이 컴포넌트를 옮기므로 컴포넌트 좌표에 이미 반영되어 있다.
- 문서 좌표 = 컴포넌트 좌표 / 배율. 기존 DOM 마우스 이벤트(`clientX` 등)는 CSS 픽셀(문서 좌표)로 전달한다.
- 뷰포트 크기와 위치는 `PageView.setViewport/setViewportPosition` 에 컴포넌트 픽셀로 주면 `PageView` 가 CSS 픽셀(`/배율`)로 바꾸어 보관한다. 그 밖의 코드(`PageRuntime`, 미디어 질의, `getBoundingClientRect`)는 CSS 픽셀만 본다.

### 2.2 캐럿 위치(점 → 텍스트 노드와 오프셋)
1. 점이 `TEXT` 상자 위이면 `HitTester` 의 문자 오프셋(가장 가까운 캐럿 경계)을 쓴다.
2. 아니면 가장 가까운 `TEXT` 상자의 가장자리: 점의 y 가 속하는 줄(상자의 y 범위)의 상자 중 x 가 가장 가까운 것, 없으면 y 거리가 가장 가까운 상자. 상자 왼쪽이면 `srcStart`, 오른쪽이면 `srcEnd`.
3. 렌더링할 텍스트가 없으면 위치가 없다(선택을 비우기만 한다).
4. 변환(`transform`)된 상자 안의 점은 `HitTester` 가 하는 만큼만 맞다(역변환은 상자 조회에만 적용).

### 2.3 단위
- 낱말: `java.text.BreakIterator.getWordInstance()` 를 텍스트 **노드의 데이터**에 적용한다(캐럿 뒤 문자가 속한 낱말; 끝이면 앞 문자). 공백만 있는 곳은 그 공백 연속이다. 낱말은 텍스트 노드를 넘지 않는다(`<b>foo</b>bar` 의 `foo` 를 두 번 클릭하면 `foo` 만).
- 문단: 텍스트 조각의 가장 가까운 블록 수준 조상(아래 2.5 의 "블록 키")의 내용 전체.
- 드래그 확장: 낱말/문단 단위로 처음 선택한 단위를 포함한 채 단위 경계까지 확장한다.

### 2.4 `user-select`
- 텍스트 조각의 사용 값: 조각의 상자에서 조상으로 올라가며 처음으로 `auto` 가 아닌 값. 없으면 `text`.
- `none`: 그 조각에서 선택을 시작하지 않고(눌러도 기존 선택은 그대로), 선택 범위 안에 있어도 강조하지 않고 복사하지 않는다.
- `all`: 그 요소 안을 누르면 요소의 내용 전체를 선택한다.
- 폼 컨트롤(`input`, `textarea`, `select`, `button`) 위에서 누르면 선택을 시작하지 않고 기존 선택을 비운다(편집은 Swing 편집기가 맡는다).

### 2.5 복사 문자열 (`text/plain`; `innerText` 와 비슷한 규칙)
선택 범위를 DOM 트리 순서로 훑으며 텍스트 노드의 레이아웃 조각(줄마다 하나)에서 선택된 부분을 모은다. **렌더링된 텍스트**(공백 접힘, `text-transform` 적용)를 쓴다.
- 같은 줄의 조각끼리는 아무것도 넣지 않는다(공백은 조각 텍스트에 있다).
- 같은 블록의 다른 줄(자동 줄바꿈)은 공백 한 칸(앞뒤가 이미 공백이면 넣지 않는다). 같은 텍스트 노드에서 두 조각 사이의 원본에 개행이 있고 `white-space` 가 개행을 보존하면(`pre`, `pre-wrap`, `pre-line`, `break-spaces`) 줄바꿈 한 개.
- `<br>` 은 줄바꿈 한 개.
- 다른 블록(블록 키가 다름): 줄바꿈 한 개. 어느 한쪽이 `p` 요소이면 두 개(빈 줄). 같은 표 행의 다른 셀 사이는 탭, 다른 행 사이는 줄바꿈. `inline-block` 안의 텍스트는 그 바깥 블록에 속한 것으로 본다.
- 맨 앞과 맨 뒤의 개행은 넣지 않는다. 리스트 표식, 이미지의 alt, 폼 컨트롤의 값은 넣지 않는다.
- `visibility: hidden` 이나 `user-select: none` 조각은 넣지 않는다.
- 레이아웃이 없으면(시험, 파싱 중) `Range.toString()` 을 쓴다.
- `text/html`: `Range.cloneContents()` 를 HTML 로 직렬화한 조각.

### 2.6 `selectionchange`
`Selection`(또는 그 `Range`)의 경계가 바뀔 때마다 `Selection` 이 `SelectionListener` 에 알리고, 컴포넌트는 문서 큐에서 한 작업으로 묶어 문서에 `selectionchange` 를 신뢰 이벤트로 보낸다(스크립트 마이크로태스크 포함). 사용자 입력이든 스크립트 호출이든 같다.

## 3. 구조

| 계층 | 클래스 | 책임 |
| --- | --- | --- |
| dom(가산) | `SelectionListener`, `Selection`(리스너·통지), `Range`(경계점 이동을 선택에 알림, `comparePointOrder`), `Document.setSelectionListener` | 선택 모델의 모든 변경(Selection 메서드, 스크립트가 쥔 살아 있는 Range 의 변경, 범위 아래의 트리 변경)이 리스너에 한 번씩 전달된다. 경계점이 실제로 움직인 경우에만 알린다. 선택이 쓰지 않는 Range 는 아무에게도 알리지 않는다. |
| dom(가산) | `ClipboardEvent`, `ClipboardData` | `copy`/`cut`/`paste` 이벤트와 `clipboardData`(MIME 별 문자열). JS 연결은 `js/dom/ClipboardBindings`(`ClipboardEvent`, `DataTransfer` 의 `getData/setData/clearData/types`). |
| render | `TextSelections` | 한 `RenderedPage` 의 선택 기하(문서 스레드): 점 → 캐럿 위치, 범위 → 강조 도형, 범위 → 복사 문자열, 낱말, 문단, 키보드 걸음(글자/낱말/줄/줄 처음·끝). 레이아웃 조각의 `srcStart/srcEnd` 와 `HitTester.textOffset` 의 역함수로 글자 ↔ x 를 대응한다. |
| render | `SelectionHighlight` | 불변 강조(문서 좌표의 도형 하나, 한 번에 채움). EDT 가 배율을 곱해 페이지 위에 덧칠한다. |
| core | `PageSelection` | 제스처(누름/드래그/뗌/더블/트리플/Shift), 전체 선택, 복사, 선택 변경 통지(`selectionchange` 묶음 이벤트와 강조 재계산), `pageClosed()` 로 옛 페이지 참조 해제 |
| core | `PageInput`, `PageKeyboard` | 마우스/키 입력을 `PageSelection`/`ZoomControl` 에 연결. 좌표는 입력 진입점에서 배율로 나눈 문서 좌표로 바꾼다. 텍스트 커서(`Cursor.TEXT_CURSOR`)는 선택 가능한 텍스트 위에서 쓴다. |
| core | `PageView` | 배율(`getZoom/setZoom`)과 컴포넌트 픽셀 → CSS 픽셀 변환(뷰포트, 스크롤), 페이지를 배율로 그리고 강조를 덧칠, 새 레이아웃을 `PageSelection` 에 알림 |
| core | `PageZoom`, `ZoomControl` | 배율 범위(0.25~5.0)와 단계, 입력 처리기가 쓰는 확대 인터페이스 |
| core | `ControlOverlays`, `ControlEditor`, `ControlPicker` | 폼 컨트롤의 Swing 편집기/드롭다운/말풍선의 위치·글꼴·크기를 배율에 맞춘다 |
| SwingWebPanel | `setZoom/getZoom`, `PROPERTY_ZOOM`, `copyToClipboard`(protected) | 확장 API. 확대 기준점 유지, 스크롤 단위/선호 크기의 배율 적용 |

스레드: 모든 DOM·레이아웃 읽기는 문서 큐에서, 클립보드 쓰기·repaint·확대 적용은 EDT 에서 한다. 마우스 드래그는 EDT 에서 마지막 위치만 기록하고 문서 큐 작업 하나로 합친다.

## 4. 검증

시험 환경: JDK 6u45(`C:\Program Files\Java\jdk1.6.0_45`), Windows 10, 화면이 있는 JVM(헤드리스 아님). 실행 명령(이 작업자의 스크립트; `scripts/pkgtest.sh` 와 같되 `tests/resources` 복사를 건너뜀):
`sh build/run_ts.sh "js dom html css net storage core content layout render download media @root" "render dom core js" TextSelection TextZoom ...`
(`build/` 는 git 무시. 같은 컴파일과 실행은 `scripts/pkgtest.sh ts "<같은 src>" "render dom core" --filter TextSelection` 으로도 된다. 단 `tests/resources` 복사가 이 PC 에서 매우 느려 이 작업에서는 쓰지 않았다.)

### 4.1 자동 검증(실행해서 통과한 것)

| 시험 | 건수 | 무엇을 확인하나 |
| --- | --- | --- |
| `dom.SelectionListenerTest` | 6 | 선택의 모든 변경(Selection 메서드, 살아 있는 Range 의 변경, 범위 아래의 트리 변경)이 리스너에 한 번씩 전달됨, 경계점이 안 움직이면 알리지 않음, 선택이 쓰지 않는 Range 는 알리지 않음, 경계점 순서 도우미, `ClipboardData`/`ClipboardEvent` |
| `render.TextSelectionTest` | 11 | 한 줄 일부/여러 줄·블록/오른쪽에서 왼쪽/이미지 강조 도형, 점 → 캐럿 위치(글자 위, 줄 밖, 문서 위·아래), 낱말 경계(영어/한글), 문단 요소, `user-select` none/auto/text/all, 복사 문자열 규칙 13가지와 부분 선택, 캐럿 걸음(글자/낱말/노드 넘김/줄/줄 처음·끝) |
| `core.TextSelectionIntegrationTest` | 8 | 실제 `SwingWebPanel` 에 AWT 마우스·키 이벤트: 드래그 선택 + 강조 화소, 한 블록에서 다음 블록까지 드래그 + Ctrl+C/Ctrl+Insert(복사 문자열과 마크업), 더블/트리플/더블 뒤 드래그/Shift+클릭, Shift+화살표/Home/End/Ctrl+낱말, Ctrl+A + `user-select:none`(시작 안 함, 강조 안 함, 복사 안 함), 스크립트 `selectAllChildren` 이 화면에 보임, `copy` 이벤트에서 `clipboardData.setData` + `preventDefault` 로 클립보드 대체와 취소하지 않은 경우, 시스템 클립보드(이 PC 에서 문자열 형식 확인. 시험 전 내용은 복구) |
| `core.TextZoomTest` | 6 | 확대 배율 규칙(0.25~5.0, NaN/음수 거부, `zoom` 속성 변경 통지, 페이지 교체와 `clean()` 에도 유지, dispose 뒤 `IllegalStateException`), 200 % 에서 화소와 스크롤 크기 2 배·스크립트가 보는 `innerWidth/innerHeight` 반쪽·`resize` 이벤트·확대 전에 만든 선택의 강조가 확대된 자리에 그려짐, 마우스 좌표 변환(200 %, 50 % 에서 같은 글자 선택과 강조 위치), Ctrl+=/+/-/0·Ctrl+휠, `JScrollPane` 안에서 확대 기준점(뷰포트 위쪽, Ctrl+휠 포인터 아래) 유지, 폼 편집기 위치·크기·글꼴 확대 |

### 4.3 회귀 결과(실제로 실행한 결과)

- 전체 `BUILD_DIR=build/full sh test.sh`(JDK 6u45, 이 작업자의 worktree HEAD `20261117-903` 의 소스와 시험): **2619 통과 / 0 실패 / 12 건너뜀**(312655 ms). 건너뛴 12건은 선택적 Opus 원본 시험 벡터 파일이 없어서이며 이 작업과 무관하다. 시작 시점(master 병합 뒤 HEAD `9dd77dd`)의 2588 에서 이 작업의 시험 31건(`SelectionListenerTest` 6, `TextSelectionTest` 11, `TextSelectionIntegrationTest` 8, `TextZoomTest` 6)이 늘었고 기존 시험은 모두 그대로 통과했다. `test.sh` 의 실행기 종료 코드 자체 점검도 통과(`[OK] runner exit-code self-check`).
- 개발 중 부분 실행(`build/run_ts.sh`, 자료 복사 없이): `core.` 208 통과/2 실패(`WebRequestApiTest` 2건은 시험 자료 `test-keystore.jks` 와 클래스패스 자료가 이 임시 스크립트에서 복사되지 않아서였고 전체 `test.sh` 에서는 통과), `dom.` 143, `render.` 145, `js.dom` 35, `js.host` 225 통과.
- 소스 규칙 점검: `sh tools/sourcecheck/run.sh <변경한 소스·시험·문서>` → 0 위반(101개 파일).
- 개발 중 발견해 고친 것: `Range` 가 경계점 이동을 선택에 알리도록(`Selection.rangeChanged`); `<br>` 은 레이아웃 상자가 없으므로 상자 유무로 거르지 않음; 한 줄 안에서 조각 사이에 접힌 공백은 한 칸으로 복원(조각이 낱말 단위로 나뉘기 때문); 스크립트는 `document.title` 로 알려도 `getTitle()` 이 확정 시점의 제목만 보므로 시험은 HTTP 페이지의 `sessionStorage`(오리진이 있는 문서) 로 알림; AWT 의 `dispatchEvent(KeyEvent)` 는 포커스 소유자로 돌려보내므로 시험은 키 리스너를 직접 부름(`KeyboardFocusTest` 와 같은 방식).

### 4.2 실제 UI 확인이 필요한 것(이 PC 에서 자동으로 확인하지 못했다. 미확인)

- 실제 마우스 드래그/더블·트리플 클릭의 클릭 횟수(OS 더블 클릭 간격), 창 밖으로 끌 때 Swing 의 자동 스크롤(`setAutoscrolls(true)` + `scrollRectToVisible`)이 일정 간격으로 이어지는지. 시험은 AWT 이벤트를 직접 전달했다.
- 시스템 클립보드로의 복사는 문자열 형식만 확인했다. `text/html` 형식이 워드프로세서나 브라우저에 서식 있는 붙여넣기로 보이는지는 확인하지 못했다. 붙여넣기(폼 컨트롤의 Swing 편집기, Ctrl+V)는 Swing 기본 키 바인딩이며 실제 클립보드 붙여넣기는 확인하지 않았다.
- 한글 IME 로 폼 컨트롤에 입력한 뒤의 선택·복사, 실제 한국어 자판의 Ctrl+`=`/`+`(`VK_EQUALS`/`VK_PLUS`) 키 코드, 확대한 상태에서 실제 `JScrollPane` 스크롤 막대를 끌 때의 모양.
- 선택 강조의 실제 모양(반투명 파란 덧칠 위의 글자 가독성), 텍스트 커서(`Cursor.TEXT_CURSOR`)의 실제 표시, 확대한 폼 편집기와 드롭다운의 실제 모양.

## 5. 구현 한계와 미해결 사항

- 선택 강조는 글자 위에 반투명 파란색을 덧칠한다(글자 색을 바꾸지 않는다). `::selection`(CSS-G)은 적용하지 않는다. 포커스를 잃은 창의 회색 강조도 없다.
- 낱말은 텍스트 노드를 넘지 않는다(`<b>foo</b>bar` 의 `foo`). 문단은 가장 가까운 블록 요소의 내용 전체이며 익명 블록이면 그 블록을 담은 요소 전체이다.
- 복사 문자열은 목록 표식, 이미지 대체 문구, 폼 컨트롤의 값을 넣지 않는다. `Selection.toString()`(스크립트)은 원본 텍스트 노드 데이터(스크립트 요소의 텍스트 포함)이며 복사 문자열과 다르다.
- 변환(`transform`)·세로쓰기 줄의 히트는 기존 `HitTester` 의 제한을 따른다(역변환은 상자 조회에만). 강조 사각형은 상자의 변환으로 옮기며, 조상의 `overflow` 자르기는 변환이 없는 조상의 padding box 로만 자른다(둥근 모서리 무시). `position: fixed` 안의 텍스트 강조는 스크롤하면 어긋난다(그리기도 같은 제한이다).
- Shift+위/아래는 가로쓰기 텍스트에서만 동작한다(세로쓰기는 `caretRect` 가 null).
- `contenteditable`, `cut`/`paste` 이벤트, 끌어다 놓기, 컨텍스트 메뉴, 캐럿 브라우징(F7), iframe 안의 선택은 구현하지 않았다.
- 확대: 폼 컨트롤의 텍스트 영역/리스트의 Swing 스크롤 막대와 드롭다운의 선 굵기는 확대하지 않는다. 확대는 컴포넌트의 설정이며 페이지/origin 별로 기억하지 않는다. 핀치 확대, 글자만 확대는 없다.
- `Range.getBoundingClientRect` 는 여전히 요소 단위 근사이다(P20-03). `TextSelections` 의 글자 단위 사각형을 연결하면 정확해질 수 있다(후속).

## 6. 다음 작업 후보

1. 4.2 의 실제 UI 확인을 사람이 한 뒤 P11-08 체크(`examples/StaticPageExample` 등으로 드래그, 더블/트리플 클릭, Ctrl+C 후 다른 앱에 붙여넣기, Ctrl+휠, 한국어 자판의 Ctrl+`=`).
2. 병합 시 충돌이 날 수 있는 파일(다른 작업자도 가산 변경): `core/PageInput`(포인터 생성 `new Pointer(event, zoom)`, `sendMouse` 의 선택 호출, 호버 커서), `core/PageView`(확대와 뷰포트 변환, 강조 그리기, `layoutChanged` 호출, `closeActive` 의 `pageClosed`), `core/ParsingPageLoader`(`setSelectionListener` 한 줄), `SwingWebPanel`(확대 필드/메서드, 클립보드 싱크, `getPreferredSize`/Scrollable 배율). FRAMES 가 하위 프레임 입력을 `PageInput` 에 연결하면 프레임마다 `Selection` 리스너와 강조를 두어야 한다.
3. CSS-G 의 `::selection` 이 계산 스타일로 나오면 `SelectionHighlight` 가 색과 글자색을 거기서 가져오도록 확장(지금은 `DEFAULT_COLOR`).
4. P20-06(`contenteditable`)에서 캐럿을 그릴 때 `TextSelections.caretRect` 와 `step/line/lineEdge` 를 재사용한다. `cut`/`paste` 이벤트는 그때 `ClipboardEvent` 를 이어서 쓴다.
5. `Range.getBoundingClientRect/getClientRects`(P20-03)를 `TextSelections.highlight(range)` 의 사각형으로 정밀화.
6. 포커스를 잃은 창의 회색 강조와 강조 위의 글자색 반전(시스템 색), 낱말 선택에서 노드 경계를 넘는 낱말.
