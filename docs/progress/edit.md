# EDIT 진행 기록 (P20-03 마무리, P20-06 편집·끌어다 놓기·데이터 전달)

작업자: EDIT(이어받은 작업자 포함). 커밋 번호 대역: 이전 작업자 `20261117-950`~`961`, 이어받은 작업자 `20261117-970` 부터. 이 문서는 항목별 절로 되어 있다(1절 P20-03, 2절 이하 P20-06).

## 조정자 요청 (AGENTS.md / FEATURE_MATRIX / API_CONTRACT / ARCHITECTURE 에 넣을 문구)

**AGENTS.md P20-06**: "드래그/드롭, 요소 편집, 데이터 전달, 포커스/입력과 관련된 남은 HTML API와 DOM 이벤트를 구현한다. 사용자 입력을 필요로 하는 기능은 프로그램 호출만으로 실제 입력 검증을 대체하지 않는다."
- 구현과 자동 검증(실제 AWT 이벤트를 컴포넌트의 리스너에 보낸 시험)은 끝났다: 편집 호스트의 캐럿·키 입력·마우스·단축키·한글 조합(손으로 만든 `InputMethodEvent`)·`cut`/`paste`(편집 호스트와 폼 컨트롤)·HTML 끌어다 놓기. 시험 수는 5절.
- **실제 입력 확인은 하지 못했다**: 한글 IME(Windows)와 실제 키보드(AltGr, 데드 키, 자동 반복), 실제 마우스 드래그와 커서 모양, 시스템 클립보드의 `text/html` 서식, 후보 창 위치. 미확인 목록은 6절. P11-02/P11-10 의 한글 IME 확인과 같은 조건이므로 **체크 후보이되 조정자가 6절을 받아들일지 판단**한다(P11-08 과 같은 방식). 체크한다면 하위 항목: P20-06-a 편집(캐럿, 키, 마우스, 서식 단축키, 되돌리기), -b 조합 입력, -c 클립보드 이벤트, -d 끌어다 놓기.

**FEATURE_MATRIX** (편집/끌어다 놓기/클립보드 행): 구현 클래스 `dom/edit/{Editor,UndoManager,TextUnits,EditingHosts}`, `dom/{StaticRange,InputEvent,CompositionEvent,ClipboardData,ClipboardEvent,DragEvent,DataTransferItem,DataTransferItemList}`, `core/{PageEditing,PageComposition,PageDragDrop,ClipboardSource}`, `js/dom/{EditBindings,ClipboardBindings}`; 지원 상태 "부분 구현(자동 검증 완료, 실제 IME·마우스·시스템 클립보드 서식은 미확인)". 제약: 끌기는 컴포넌트 안에서 흉내 내며(운영체제 끌기 시작 안 함, 외부 앱으로 끌기/외부에서 놓기 없음), 파일 항목과 `setDragImage` 그림은 없고, 두 프레임에 걸친 끌기를 따로 다루지 않으며, 폼 컨트롤에 놓은 텍스트는 값의 끝에 붙는다. 조합 입력은 `insertCompositionText` 만 쓰고(Input Events 2 의 `deleteCompositionText`/`insertFromComposition` 없음) 조합 속성(밑줄 등)을 그리지 않는다.

**API_CONTRACT** (공개 메서드 목록은 그대로, 확장으로 기록):
- `protected Transferable readClipboard() throws Exception`: 사용자가 편집 가능한 곳이나 폼 컨트롤에 붙여넣을 때(Ctrl+V, Shift+Insert, 편집기의 붙여넣기 동작) EDT 에서 호출된다. 기본은 시스템 클립보드를 읽는다. 호스트와 시험이 재정의한다(`copyToClipboard` 와 한 쌍). 예외나 null 은 "붙여넣을 것 없음"이며 예외는 상태 메시지("Paste failed ...")로만 알린다.
- `public InputMethodRequests getInputMethodRequests()`: 한글 등 입력기가 캐럿 위치(후보 창 자리)를 물을 때 답한다. 컴포넌트는 편집 호스트가 키보드 포커스를 가진 동안만 입력기를 켜고(`enableInputMethods`) 아니면 끈다. 폼 컨트롤은 자기 Swing 편집기가 입력기를 다룬다.
- `dispose()` 의 제거 대상 표: 내부 `InputMethodListener`(`PageComposition`)가 dispose 때 제거되고, 끌기 타이머가 멈추며 진행 중인 끌기는 이벤트 없이 끝난다. 사용자가 등록한 입력기 리스너도 기존 표대로 제거된다.

**ARCHITECTURE**: `core` 행에 `PageEditing`(편집 호스트 안의 키/잘라내기/붙여넣기/끌어 놓기 삽입/조합 입력의 제어기), `PageComposition`(입력기 리스너와 질의), `PageDragDrop`(끌어다 놓기 상태 기계), `ClipboardSource`(붙여넣기 읽기 훅); `dom` 행에 `DragEvent`, `DataTransferItem`, `DataTransferItemList`(`ClipboardData` 가 `DataTransfer` 전체를 맡는다: 모드, `effectAllowed`, `dropEffect`, `items`, `files`, `setDragImage`).

**PROGRESS**: 다음 작업 후보는 7절.

## 1. P20-03 마무리 (Range/Selection 화면 좌표, Geometry 인터페이스, live range 보정)

기준 규격과 조회(모두 `tools/webfetch`, 실행 JRE = Zulu 25(`Program Files/Java/zuu25`), 조회일 2026-11-17):
- CSSOM View Module `https://drafts.csswg.org/cssom-view/` (Last-Modified 2026-11-16, 9절 "Extensions to the Range Interface": 선택된 요소의 border area, 선택된 텍스트의 글자 사각형, 글자 단위 확장, 합집합 규칙).
- Geometry Interfaces Module Level 1 `https://www.w3.org/TR/geometry-1/` (Last-Modified 2025-12-02, `DOMRectReadOnly`/`DOMRect`/`DOMPoint*`, NaN-safe 가장자리). `https://drafts.fxtf.org/geometry/` 는 짧은 안내 페이지(75 바이트)만 준다.
- DOM Standard(`normalize()`, `insertNode`, NodeIterator pre-removing steps) `https://dom.spec.whatwg.org/` 는 이 세션에서 다시 조회하지 않았고 이전 세션이 같은 알고리즘을 구현한 기록(`docs/progress/domapi-b.md`)과 알고리즘 단계(normalize 4~7, insertNode 6~7)를 대조했다(미확인: 본문 재조회).
- Chromium 참고: `NodeIterator::NodeWillBeRemoved`(제거된 노드가 루트이거나 루트의 후손이 아니면 무시)는 코드를 직접 열람하지 않고 기존 구현의 설명에 있는 동작만 확인했다.

### 구현
- `render/TextSelections.rects(Range)`: CSSOM View 의 규칙. 범위가 완전히 선택하고 그 부모는 선택하지 않는 요소는 border box(상자마다), 범위가 선택하거나 일부 선택하는 텍스트 노드는 선택된 글자의 사각형(줄마다 하나, 한 줄의 조각은 합침, RTL/변환 반영), 접힌 범위는 텍스트 안이면 너비 0 사각형, 그 밖이면 캐럿 자리.
- `render/TextSelections.caretRectAt(Node, int)`: 아무 DOM 위치의 캐럿 사각형(텍스트 위치, 요소 안의 위치는 앞 텍스트의 끝/뒤 텍스트의 시작/치환 요소의 가장자리, `br` 바로 뒤는 다음 줄의 시작, 빈 블록은 내용 상자). 편집 캐럿을 그릴 때도 쓴다.
- `HostEnvironment.getRangeRects/getRangeText/getElementRects`(기본 null = 레이아웃 없음), `PageRuntime`/`PagePresentation` 이 실제 레이아웃으로 구현(문서 큐에서 렌더링을 반영한 뒤 뷰포트 좌표로 변환).
- `js/host/RangeGeometry`: 실제 글자 기하를 쓰고(없으면 이전의 요소 상자 근사), `getBoundingClientRect` 는 CSSOM View 의 합집합 규칙(너비·높이가 0 인 것은 제외, 모두 0 이면 첫 사각형), `getClientRects` 는 `DOMRectList`. `Selection.toString()` 은 복사했을 때의 렌더링 텍스트(사용자 선택 불가·스크립트 텍스트 제외; `Range.toString()` 은 DOM 표준대로 원본 데이터).
- `Element.getClientRects()` 가 상자마다 사각형(줄마다 갈라진 인라인은 여러 개, `display:none` 은 빈 목록)을 돌려준다.
- `dom/{DomRectReadOnly,DomRect,DomPointReadOnly,DomPoint,DomRectList}` + `js/dom/GeometryBindings`: `DOMRectReadOnly`/`DOMRect`(생성자, `fromRect`, `toJSON`, 가장자리 계산, 음수 크기), `DOMRectList`(`length`, `item`, 위치 접근, 반복), `DOMPointReadOnly`/`DOMPoint`(`fromPoint`, `toJSON`). `getBoundingClientRect()` 의 결과가 진짜 `DOMRect` 이다(`instanceof DOMRect`, 호출마다 새 객체). 행렬(`DOMMatrix`, `DOMQuad`)은 하지 않았다(P20-05-b).
- live range: `Node.normalize()` 가 DOM 표준 4~7 단계대로 범위 경계점을 옮긴다(`Range.textMerged`, `LiveRanges.textMerged`, `Document.liveTextMerged`): 합쳐진 노드 안의 점은 첫 노드로(오프셋 + 이미 있던 길이), 합쳐진 노드 바로 앞의 부모의 점은 첫 노드의 그 부분 끝으로. `Range.insertNode` 는 삽입이 가능한지 텍스트를 쪼개기 전에 확인한다(`Node.ensurePreInsertionValidity`; 이전에는 실패해도 텍스트가 쪼개져 있었다).
- NodeIterator/TreeWalker: DOM 표준의 pre-removing steps 와 기존 구현(`NodeIterator.nodesRemoved`)을 대조했고 표준과 Chromium(`NodeIterator::NodeWillBeRemoved`: 루트이거나 루트의 후손이 아닌 제거는 무시)에 맞았다. 시험을 보강했다(`replaceChildren` 로 여러 노드를 한꺼번에 제거).

### 시험(직접 돌려 통과한 것, JDK 6u45)
(이 절의 시험 결과는 이전 작업자가 병합 전 전체 회귀에서 확인했다: 2921 통과/0 실패. P20-06 의 시험은 5절.)

## 2. P20-06 범위와 기준 규격

이전 작업자가 DOM 수준 편집 엔진(`dom/edit/*`, `EditorTest` 14, `EditingApiTest` 5)을 만들어 병합했고, 이번 작업은 그것을 **화면 입력에 연결**한다(`wip/edit-2-unverified` 의 미검증 코드는 컴파일과 시험으로 확인해 가져왔고, 아래 버그를 고쳤다).

| 항목 | 구현한다 | 구현하지 않는다(이유) |
| --- | --- | --- |
| 캐럿 | 편집 호스트(`contenteditable`, `designMode`) 안의 접힌 선택을 1 픽셀 막대로 그린다(색은 `caret-color`, `auto` 는 글자색, 확대 배율 반영). 룩앤필의 `TextField.caretBlinkRate`(없으면 500 ms)로 깜박이고, 선택이나 편집이 캐럿을 옮기면 바로 보이며, 창 포커스를 잃으면 숨긴다. 편집이나 캐럿 키가 캐럿을 보이는 영역 밖으로 옮기면 스크롤한다 | 캐럿 브라우징(F7), 두 번째 캐럿(다중 범위), 양방향 텍스트의 분리 캐럿 |
| 포커스 | 편집 호스트(바깥쪽 것)가 포커스를 가진다(클릭, Tab). Tab 으로 온 포커스는 호스트의 처음에 캐럿을 둔다 | 호스트 안의 `contenteditable` 요소의 별도 포커스(브라우저와 같다) |
| 키 | 글자(`keypress`→`insertText`), Enter(문단 나누기), Shift+Enter(줄바꿈), Backspace/Delete(Ctrl 은 낱말), 화살표/Home/End(Ctrl 은 낱말, 호스트의 처음·끝), Shift 로 선택 확장, Ctrl+A/B/I/U/Z/Y/Shift+Z/X/V, Shift+Delete(잘라내기), Shift+Insert(붙여넣기). 이벤트 순서 `keydown`→`keypress`→`beforeinput`→변경→`input`→`keyup`. 취소한 `keydown` 은 글자(`keypress` 와 입력)를 막는다 | Alt/Meta 조합, 맞춤법 검사, 자동 완성, 서식 막대 |
| 마우스 | 클릭이 캐럿을 놓고 호스트에 포커스를 준다. 더블/트리플 클릭, 드래그, Shift+클릭은 TEXTSEL 의 선택 기능이다 | 컨텍스트 메뉴 |
| 입력기 | `InputMethodListener` + `InputMethodRequests`: 조합 중인 글을 DOM 에 조합되는 대로 보이고(`compositionstart` → `beforeinput`(`insertCompositionText`, 취소 불가, `isComposing`) → `compositionupdate` → 변경 → `input`), 확정하면 `compositionend`. 확정 뒤 이어지는 조합(한글 음절 넘김)은 새 조합. 빈 이벤트는 취소(텍스트 제거). 조합 하나는 되돌리기 한 단계. 마우스 누름은 진행 중인 조합을 `endComposition()` 으로 확정한다. 후보 창은 캐럿의 화면 위치에 열린다 | 조합 속성(밑줄/절 강조), `deleteCompositionText` 등 Input Events 2 의 조합 시작·끝 입력 종류, 주변 텍스트 질의(`getCommittedText` 등은 빈 값) |
| 클립보드 | `paste`(취소 가능, 읽기 전용 `clipboardData` 에 `text/plain`, `text/html`), `cut`(`copy` 와 같되 지움이 `deleteByCut`), 취소와 스크립트 데이터(`setData` 가 클립보드에 감), 붙여넣을 마크업은 정리해 넣음, 읽을 수 없으면 상태 메시지. 폼 컨트롤(텍스트 필드, 암호 필드, 텍스트 영역)의 Swing 편집기도 같은 이벤트를 먼저 보낸다(암호 필드는 `cut`/`copy` 없음) | 그림/파일 붙여넣기, 클립보드의 서식 있는 다른 형식 |
| 끌어다 놓기 | HTML 표준 6.11 의 처리 모델: `dragstart`, `drag`, `dragenter`, `dragover`, `dragleave`, `drop`, `dragend`, `DataTransfer` 모드(읽기/쓰기, 읽기 전용, 보호, 무효화), `effectAllowed`/`dropEffect` 표, `items`, `setDragImage`(저장만), `draggable`(이미지와 링크는 기본), 선택한 텍스트 끌기, 편집 호스트 안의 이동(`deleteByDrag` + `insertFromDrop`, Ctrl 은 복사)과 밖에서 안으로의 복사, 텍스트 필드로 놓기, 링크/이미지의 `text/uri-list`, Esc 취소, 350 ms 반복, 끌기 중 마우스/키 이벤트 억제, 마우스 커서(플랫폼의 끌기 커서) | 운영체제의 끌기 시작(다른 앱으로 끌기/바깥에서 놓기), 파일 항목(`files` 는 빈 `FileList`, `items.add(File)` 없음), `setDragImage` 그림 그리기, 두 프레임에 걸친 끌기의 특별 처리, `dragenter` 를 `mouseenter` 처럼 자손에 퍼뜨리는 것(규격 단일 대상 알고리즘 그대로) |

기준 규격과 조회(`tools/webfetch`, 실행 JRE = Zulu 25, 조회일 2026-11-17):
- HTML Standard 6.11 "Drag and drop" `https://html.spec.whatwg.org/multipage/dnd.html` (Living Standard, Last Updated 16 November 2026, Last-Modified Mon, 16 Nov 2026 15:32:23 GMT): 처리 모델, 이벤트 요약표, `DataTransfer` 모드, `effectAllowed`→`dropEffect` 표, `draggable` 규칙을 따랐다.
- Input Events Level 2 `https://www.w3.org/TR/input-events-2/` (W3C Working Draft 15 October 2026, Last-Modified 2026-10-15): "조합 중의 `beforeinput` 은 취소할 수 없고 나머지는 취소할 수 있다"를 확인했다.
- UI Events(키보드/조합 이벤트), Clipboard API and events, HTML Standard 6.8(편집)은 이 세션에서 다시 조회하지 않았다(미확인: 본문 재조회). 구현은 이전 작업자의 설계와 코드의 주석을 따랐다.

## 3. P20-06 구조

| 계층 | 클래스 | 책임 |
| --- | --- | --- |
| core | `PageEditing` | 키(`keyDown`/`keyPress`)를 편집기 작업으로, `cut`/`paste` 이벤트와 기본 동작, 놓은 텍스트 삽입(`drop`), 끌려 나간 선택 삭제(`deleteDragged`), 조합 입력(`compose`). 편집기에 페이지 환경(`PageEnvironment`: 이벤트를 스크립트 호스트로 보냄, 블록과 공백 보존은 계산 스타일로 판단)을 설치한다 |
| core | `PageKeyboard` | AWT 키를 DOM 키 이벤트로(Ctrl+글자의 `key` 는 플랫폼이 제어 문자나 미정의를 줘도 글자로 복원), 붙여넣기 키에서 EDT 에서 클립보드를 읽어 둠, 캐럿 키를 스크롤 창에 넘기지 않음, 끌기 중 키 억제와 Esc 취소, `queueForFocused`(입력기가 쓰는 포커스 요소 작업 예약) |
| core | `PageComposition` | 입력기 리스너(`InputMethodEvent` → 확정/조합 텍스트 → `PageEditing.compose`)와 `InputMethodRequests`(캐럿의 화면 사각형) |
| core | `PageSelection` | 편집 캐럿 표시(`rebuildCaret`, 깜박임 타이머는 약한 참조), 포커스 상태에 따른 캐럿/입력기 켜고 끄기(`InputMethodSwitch`), 캐럿 이동(`moveByKey`), 잘라내기(`cut`), 선택 안 판정(`isInsideSelection`), 점 → 캐럿 위치(`caretAtPoint`), 선택 마크업 |
| core | `PageDragDrop` | 끌어다 놓기 상태 기계(누름 후보 → `begin` → 반복 `step` → `end`), 데이터 저장소와 이벤트별 `DataTransfer`, 타이머, 커서 |
| core | `PageInput` | 누름/뗌/끌기를 `PageDragDrop` 에 연결(`pressed`, `released`, `moved`), `Pointer`/`Located`/`locate`/`fillMouse` 를 패키지 공개로 |
| core | `ControlEditor`, `EditSpec`, `PageFormHost`, `ControlOverlays` | 폼 컨트롤의 Swing 편집기가 `cut`/`copy`/`paste` 를 DOM 이벤트 뒤에 하도록(`userClipboard`), 컴포넌트의 클립보드 메서드를 씀, 텍스트 컨트롤에 놓기(`insertDropped`) |
| dom | `ClipboardData` | `DataTransfer` 전체(모드, 효과, 항목 목록, 파일(빈), `setDragImage`, `duplicate`) |
| dom | `DragEvent`, `DataTransferItem`, `DataTransferItemList` | 끌기 이벤트와 항목 인터페이스 |
| js/dom | `ClipboardBindings`, `DomInterfaces` | `DataTransfer` 의 확장 멤버, `DataTransferItemList`/`DataTransferItem`(`getAsString` 의 콜백은 realm 작업으로 실행), `DragEvent`(초기화 사전 생성자) |
| dom/edit | `Editor` | 조합 입력(`compose`, `isComposing`, `abandonComposition`), `placeCaret`, `defaultEnvironment` |
| SwingWebPanel | `readClipboard`(protected), `getInputMethodRequests`, 내부 리스너 연결 | 확장 API |

스레드: 모든 DOM·레이아웃 읽기와 DOM 변경은 문서 큐에서, 클립보드 읽기·입력기 켜고 끄기·타이머·커서·repaint·스크롤은 EDT 에서 한다.

## 4. P20-06 개발 중 발견해 고친 것

- Ctrl+글자 단축키가 동작하지 않을 수 있었다: AWT 는 Ctrl+B 의 `KEY_PRESSED` 에서 제어 문자(`0x02`)나 `CHAR_UNDEFINED` 를 알리고, 이때 `KeyNames.key` 가 `"Unidentified"` 를 돌려줘 `PageEditing.keyDown` 이 글자를 몰랐다(이전 WIP 는 `key` 문자열로 판정). 편집은 이제 가상 키 코드(`keyCode`)로 판정하고, DOM `KeyboardEvent.key` 도 이 경우 글자로 복원한다(`"b"`, Shift 면 `"B"`).
- 취소한 `cut`/`copy` 가 스크립트 데이터 없이도 선택을 클립보드에 쓰고 있었다: 이벤트 데이터는 선택으로 미리 채워지므로 "데이터가 있다"가 아니라 "스크립트가 바꿨다"(`isChanged`)로 판정한다(`PageSelection.transfer` 와 폼 편집기).
- 끌기 시작 때 마지막 포인터가 덮어써지는 경합: `begin()` 이 `lastPointer` 를 시작 때의 포인터로 덮어써서, `dragstart` 를 처리하는 동안 EDT 가 기록한 더 새 위치를 잃었다(드래그가 목표를 놓침). 느린 `dragstart` 리스너로 재현하는 시험 `testMovesDuringASlowDragstartAreNotLost`(수정 전 소스에서 실패를 확인)를 추가했다.
- 시험 환경: 이 PC 에서 JDK 6 의 첫 시험은 `JsRealm` 생성이 10 초 이상 걸려(콜드 JVM + 다른 작업자) `awaitIdle(10000)` 를 쓰는 기존 시험(`FormControlIntegrationTest` 등)이 JVM 의 첫 시험일 때만 실패한다. 이번 변경과 무관하다(단독 실행으로 확인, 병합 전 소스에서도 같다).

## 5. P20-06 검증(직접 돌려 통과한 것, JDK 6u45, Windows 10)

시험 실행: `build/inc.sh`(이 작업자의 임시 스크립트: `scripts/pkgtest.sh` 와 같되 `tests/resources` 복사를 건너뛰고 증분 컴파일; `build/` 는 git 무시). 일반 환경에서는 `sh scripts/pkgtest.sh <이름> "js dom html css net storage core content layout render download media @root" "dom core js" --filter <접두사>`.

| 시험 | 건수 | 무엇을 확인하나 |
| --- | --- | --- |
| `core.EditingInputTest` | 14 | 실제 `SwingWebPanel` 에 AWT 마우스·키·입력기 이벤트: 클릭이 캐럿을 놓음, 글자 입력과 이벤트 순서(`keydown keypress beforeinput input keyup`), Backspace/Delete/Enter/Shift+Enter, 화살표/Home/End/Ctrl 낱말과 호스트 처음·끝, Shift 선택 확장과 선택 대체 입력과 Ctrl+A, Ctrl+B/I/U/Z/Y/Shift+Z(플랫폼이 제어 문자나 미정의를 알리는 경우 모두), 취소한 `keydown`/`beforeinput`, 편집할 수 없는 곳과 Tab 포커스, 캐럿 그리기와 깜박임(실제 화소), 한글 조합(손으로 만든 `InputMethodEvent`: 조합 중 글, 음절 넘김, 확정, 되돌리기 한 단계), 조합 취소와 선택 위 조합, 입력기 켜고 끄기, 디자인 모드, 포커스를 잃으면 캐럿 숨김 |
| `core.EditingClipboardTest` | 7 | Ctrl+V/Shift+Insert 붙여넣기(텍스트, 정리한 마크업), `paste` 의 `clipboardData` 는 읽기 전용, 취소한 붙여넣기와 빈/읽을 수 없는 클립보드, Ctrl+X/Shift+Delete 잘라내기와 `deleteByCut`, 취소와 스크립트 데이터, 편집할 수 없는 곳은 이벤트만 |
| `core.FormClipboardTest` | 3 | 폼 컨트롤의 Swing 편집기의 `cut`/`copy`/`paste` 가 DOM 이벤트 뒤에 일어남, 컴포넌트의 클립보드 메서드를 씀, 취소와 스크립트 데이터 |
| `core.DragDropIntegrationTest` | 12 | 실제 AWT 마우스: 요소 끌기 → 받는 곳(이벤트 순서, 모드별 `DataTransfer`, `drop` 데이터와 효과, `dragend`, `mouseup` 억제), 받지 않는 곳, 문턱 미만과 취소한 `dragstart`, Esc 취소, 포인터가 서 있을 때 `dragover` 반복, 링크 주소, 텍스트 필드로 놓기(`insertFromDrop`), 선택한 텍스트를 편집 호스트로, 편집 호스트 안의 이동(`deleteByDrag` + `insertFromDrop`), 편집할 수 없는 곳, 느린 `dragstart` 중의 이동(경합), 끌기 중 페이지 교체(이벤트 없이 끝남) |
| `js.host.DataTransferApiTest` | 6 | `DataTransfer`(효과, 형식 정규화, `types`, `getData`/`setData`/`clearData`, `items`, `files`, `setDragImage`), 모드 규칙(Java 에서 설정), `DragEvent` 생성자와 `on...` 핸들러, `draggable` |

회귀는 6절. 새 시험이 확인하는 기능(편집 호스트의 키 입력과 캐럿, 끌기 이벤트 등)은 수정 전에는 없었으므로 그 소스에서 통과할 수 없는 시험이다. 다만 수정 전 소스로 되돌려 모두 돌려 보지는 않았고(미확인), 끌기의 경합 시험(`testMovesDuringASlowDragstartAreNotLost`)은 고치기 전 소스로 되돌려 실패하는 것을 직접 확인했다(`dragenter@zone` 이 오지 않음).

## 6. 회귀 결과(실제로 실행한 결과)

- 전체 `BUILD_DIR=build/full sh test.sh`(JDK 6u45, 이 작업자의 worktree 의 커밋 `20261117-972` 소스와 시험): **2998 통과 / 0 실패 / 12 건너뜀**(757907 ms). 시작 시점(master `3764d11`)의 2957 에서 이 작업의 새 시험 41건(`EditingInputTest` 14, `EditingClipboardTest` 6, `FormClipboardTest` 3, `DragDropIntegrationTest` 12, `DataTransferApiTest` 6)이 늘었고 기존 시험은 모두 그대로 통과했다. 건너뛴 12건은 선택적 Opus 원본 시험 벡터 파일이 없어서이며 이 작업과 무관하다. `test.sh` 의 실행기 종료 코드 자체 점검과 제품 JAR 검사도 통과했다.
- 그 뒤의 변경(커밋 `973`: `paste` 의 `clipboardData` 읽기 전용, 시험 1건 추가)은 전체 회귀 뒤에 들어갔으므로 그 부분만 다시 돌렸다: `EditingClipboardTest` 7 + `FormClipboardTest` 3 + `TextSelectionIntegrationTest.testSystemClipboard` + `SelectionListenerTest.testClipboardData` = 12건 통과. 전체 회귀는 이 변경 전 소스이다(조정자가 병합 뒤 전체 회귀를 다시 돌린다).
- 개발 중 부분 회귀: `core.` 310 통과/0 실패(끌기 연결 직후, 페이지 닫힘 처리와 포커스 취소 추가 전). 그 추가 뒤 `EditingInputTest` 14 + `EditingClipboardTest` + `DragDropIntegrationTest` 12 통과.
- 소스 규칙 점검: `sh tools/sourcecheck/run.sh <변경한 소스·시험·예제>` → 0 위반.
- 예제 점검: `SWP_CLASSES=build/full/classes sh examples/run.sh EditingExample --smoke` → `smoke OK`(페이지가 읽히고 `contenteditable`/`draggable` 가 있음). 창을 띄워 사람이 확인하는 것은 7.1.

## 7. 미확인과 한계, 다음 작업

### 7.1 실제 UI 확인이 필요한 것(이 PC 에서 자동으로 확인하지 못했다. 미확인)

- **한글 IME**: Java 가 실제 IME 의 조합을 `PageComposition` 으로 보내는지, 조합 중 글이 인라인으로 보이는지(`getInputMethodRequests()` 가 on-the-spot 방식을 고르게 하는지), 후보 창(`getTextLocation`)이 캐럿 옆에 열리는지, 마우스 클릭으로 조합이 확정되는지(`endComposition`), 한글 자판에서 Ctrl 단축키의 `KeyEvent` 값. 시험은 손으로 만든 `InputMethodEvent`(Java 가 주는 모양: 확정 글자 수 + 조합 글)만 썼다.
- **실제 키보드**: 자동 반복, AltGr/데드 키로 만든 글자, Windows 의 Ctrl+글자 `keyChar` 값(시험은 제어 문자와 미정의 둘 다 보냈다), 대문자와 Caps Lock.
- **실제 마우스**: 끌기 문턱(4 CSS 픽셀)의 체감, 플랫폼 끌기 커서(`DragSource.Default*Drop` 이 이 JVM/OS 에서 만들어지는지; 헤드리스에서는 기본 커서로 대체), 창 밖으로 끌 때의 자동 스크롤, 더블/트리플 클릭 간격.
- **시스템 클립보드**: 붙여넣기 읽기는 변수(`readClipboard` 재정의)로만 확인했다. 실제 클립보드의 `text/html`(`StartFragment` 주석 처리 포함)과 일반 텍스트의 읽기, 다른 앱에서 복사한 서식 있는 텍스트의 붙여넣기.
- 캐럿 깜박임의 실제 주기(`TextField.caretBlinkRate` 는 룩앤필마다 다르다: 이 시험에서는 깜박임이 있음을 화소로만 확인), 캐럿의 실제 모양과 굵기, 확대한 상태의 캐럿.

### 7.2 구현 한계

- 끌기는 컴포넌트 안에서 흉내 낸다. 다른 앱으로 텍스트를 끌거나 바깥에서 파일/텍스트를 놓을 수 없다(`DragSource`/`DropTarget` 미사용). 파일 항목은 없다.
- `setDragImage` 의 그림은 그리지 않는다(저장만). 끌 때의 유령 그림이 없고 마우스 커서만 바뀐다.
- 선택을 끌 때 `dragstart` 의 대상은 텍스트 노드가 아니라 그 부모 요소이다(규격은 텍스트 노드).
- 텍스트 컨트롤에 놓은 텍스트는 값의 끝에 붙는다(규격이 허용하는 두 방식 중 하나). 텍스트 컨트롤에서 선택을 끌어 내는 것은 Swing 편집기의 몫이라 지원하지 않는다.
- 프레임 안에서 시작한 끌기, 프레임 사이의 끌기는 별도로 시험하지 않았다(이벤트는 포인터 아래 문서로 간다). 끌기 중 `scroll` 이 일어나는 경우의 좌표 갱신은 시험하지 않았다.
- 조합 입력은 한 호스트의 접힌 캐럿 또는 선택 위에서만 시험했다. 조합 중에 스크립트가 DOM 을 바꾸는 경우는 살아 있는 범위가 따라가지만 시험하지 않았다.
- `getAsString` 의 콜백은 규격의 "작업 큐"가 아니라 realm 작업(마이크로태스크 지연)으로 실행된다.

### 7.3 다음 작업 후보

1. 7.1 의 실제 UI 확인을 사람이 한 뒤 P20-06(및 P11-02/P11-10)의 체크를 판단(`examples/StaticPageExample` 에 `contenteditable` 와 `draggable` 데모를 더하면 확인이 쉽다).
2. 실제 운영체제 끌기(`DragSource`/`DropTarget`)와 외부 파일 드롭: `files` 에 `FileEntry` 를 만들 방법이 필요하다(사용자가 고른 것만 만든다는 `FileEntry` 의 규칙과 충돌하므로 설계 필요).
3. `setDragImage`/기본 끌기 그림을 레이아웃에서 만들어 그리기.
4. IME 조합 속성(밑줄), `deleteCompositionText` 등 Input Events 2 의 조합 입력 종류.
5. 편집 호스트의 맞춤법/자동 완성, 컨텍스트 메뉴(잘라내기/복사/붙여넣기/실행 취소).
