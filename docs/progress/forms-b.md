# FORMS-B 진행 기록 (P11-05 컨트롤 화면, P11-02 IME/포커스, P11-06/07 호스트 쪽)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/layout/**`, `render/**`(컨트롤 관련만 추가), `core/{PageFormHost,FormSubmitSink,UploadChooser,ColorChooser,ControlOverlays,ControlEditor,ControlPicker,ControlActions,EditSpec,PickSpec}.java`(신규),
`core/{PageKeyboard,PageView,ParsingPageLoader,HtmlPageDocument}.java`, `SwingWebPanel.java`(폼 연결의 최소 변경), `css/cssom/UaStyleSheet.java`, `dom/html/**`(훅 추가만), 위 경로의 시험, 이 파일.
시험 실행(격리): `sh scripts/snaptest.sh formsb "<소유 파일을 정확히 나열>" "js dom html css net storage core content layout render download media @root" "js core render layout css dom html" --filter swingwebpanel`.
이번 작업은 반복이 많아 스냅샷(`build/snap-formsb` = git HEAD + 소유 경로)을 한 번 만들고 변경한 파일만 `javac -sourcepath` 로 다시 컴파일하는 보조 스크립트(`build/fb.sh`, 저장소 밖 `build/` 아래라 commit 하지 않음)로 시험했다. 모든 컴파일과 실행은 JDK 6u45.
린트: `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/core src/org/duckdns/hjow/swingwebpanel/layout src/org/duckdns/hjow/swingwebpanel/render src/org/duckdns/hjow/swingwebpanel/dom/html src/org/duckdns/hjow/swingwebpanel/SwingWebPanel.java src/org/duckdns/hjow/swingwebpanel/css/cssom/UaStyleSheet.java tests/src/org/duckdns/hjow/swingwebpanel/core tests/src/org/duckdns/hjow/swingwebpanel/layout tests/src/org/duckdns/hjow/swingwebpanel/render tests/src/org/duckdns/hjow/swingwebpanel/FormComponentIntegrationTest.java`.

참고 규격(조회 도구 `tools/webfetch`, 새 JRE = Zulu 25, 조회 2026-11-17):
- HTML5 W3C Recommendation 2014-10-28, 10 Rendering — `https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html` (Last-Modified 2014-10-24, 본문 106651 바이트, 저장 `build/html5-rendering-20141028.txt`): 10.3.11 Form controls, 10.3.13 fieldset/legend, 10.4.3 이미지 단추, 10.5.2~10.5.14 컨트롤 바인딩.
- HTML5 2014, 4.10 Forms — FORMS-A 가 조회한 `build/html5-forms-20141028.txt`.
- Chromium 의 컨트롤 모양과 UA 스타일은 소스를 열람하지 않고 일반 지식으로 따랐다(차이는 아래 "규격/Chromium 과의 차이"). Chromium 코드를 옮기지 않았다.

## 작업 기록

### 2026-11-17 FORMS-B: 컨트롤 크기와 그리기, Swing 편집기 오버레이, 호스트, select 목록, fieldset
현재 작업 번호: P11-05(화면 부분), P11-02(포커스/IME 연결), P11-06/07(호스트 쪽)
완료한 내용:

**구조(스레드 규칙 포함)**

| 계층 | 클래스 | 하는 일 |
| --- | --- | --- |
| dom/html | `ControlDisplay`(신규) | 컨트롤 종류, 단추 라벨, 필드 글/placeholder, option 행 목록, 비활성/읽기 전용/maxlength 를 렌더러와 레이아웃에 알려 주는 읽기 전용 도우미 |
| layout | `ControlSizes`(신규) | 컨트롤의 고유 크기(문서 스레드)와 기준선, `fieldset` 의 rendered legend 찾기. `Replaced.natural` 이 먼저 부른다. `progress`/`meter` 를 치환 요소로 추가 |
| layout | `BlockLayouter.layoutChildren` | rendered legend 를 shrink-to-fit 으로 먼저 배치해 위쪽 경계선 위 세로 가운데에 놓고(`align`/`direction` 반영) 나머지 내용을 legend 아래에서 시작, 위로 튀어나온 만큼 fieldset 의 위쪽 margin 으로 비움 |
| render | `ControlSnapshot`/`ControlPainter`/`ControlCommand` | 디스플레이 리스트를 만들 때(문서 스레드) 컨트롤의 상태를 **불변 스냅샷**으로 떠서 명령 하나로 낸다. EDT 가 Java2D 로 그린다(체크박스, 라디오, 단추 라벨, 텍스트/암호/placeholder, 텍스트 영역 줄 바꿈, 드롭다운, 리스트 박스, 파일, progress, meter, range, color). `RenderedPage.getControl(element)` |
| render | `DisplayListBuilder.border` | fieldset 의 위쪽 경계선을 legend 구간에서 끊는다(위쪽 없는 경계선 + 양옆 조각) |
| css | `UaStyleSheet` | 컨트롤 UA 스타일(`line-height: normal`, 회색 inset 경계선, 비활성 색, 체크박스 margin, range/color/file/image/progress/meter, 규격 10.3.11 의 `box-sizing: border-box`, 호환 모드의 `input:not([type=image]), textarea`) |
| core | `PageFormHost`(신규, `FormHost` 구현) | 문서마다 하나. `ParsingPageLoader.parseMarkup` 가 파싱 **전에** `document.setFormHost` 로 설치(파싱 중 스크립트의 폼 조작도 전달). 제출은 `FormSubmitSink` 로 위임만 한다. `stateChanged` → `PagePresentation.elementStateChanged()`(스타일+그림 무효화). 검증 말풍선, 파일 선택(`File` → `FileEntry`, 파일당 64 MiB 한도), focusin/focusout 로 편집기 열기/닫기와 `change` 이벤트, 마우스 이벤트로 select/리스트/슬라이더/색, 키 기본 동작 |
| core | `ControlOverlays`(신규, public) / `ControlEditor` / `ControlPicker` | **EDT 전용**. 페이지 컴포넌트의 자식으로 `JTextField`/`JPasswordField`/`JScrollPane(JTextArea)` 편집기, 드롭다운 목록(`JList`), 검증 말풍선(`JLabel`)을 둔다(창이 아니므로 어디서나 동작, 페이지와 함께 스크롤, 좌표는 문서 좌표). `clear()` 가 모두 제거 |
| core | `ControlActions`(신규) | select 고르기/이동/입력으로 찾기, 슬라이더, 라디오 화살표 이동(DOM 만, 이벤트 포함) |
| core | `FormSubmitSink`, `UploadChooser`, `ColorChooser`(신규 인터페이스) | 제출 위임, 업로드 파일 선택, 색 선택의 교체 지점 |
| core/SwingWebPanel | `PageView`, `PageKeyboard`, `SwingWebPanel` | `PageView` 가 오버레이/싱크를 들고 `createFormHost`/`isCurrent` 를 준다. `PageKeyboard` 는 편집기 사이의 포커스 이동을 창 포커스 변화로 보지 않고, Enter(암묵적 제출), 스페이스(체크박스/라디오), select/슬라이더/라디오 키를 처리한다. `SwingWebPanel` 은 `protected submitForm(FormSubmission)`, `protected chooseUploadFiles(boolean, String)`, `isOptimizedDrawingEnabled()` 를 추가 |

**편집기 규칙(P11-02/P11-08 의 편집 부분)**
- 텍스트 필드/텍스트 영역이 DOM 포커스(`focusin`)를 받으면 그 content box 위에 편집기를 연다(상자가 아직 없으면 다음 렌더링 뒤). 편집기는 불투명(배경색을 흰색에 올린 값)이라 아래에 그린 글이 비치지 않는다. 마우스를 눌러서 열렸으면 캐럿을 누른 곳에 놓는다. 포커스를 잃으면 닫는다.
- Swing → DOM: `DocumentListener` 가 편집을 합쳐(한 번의 `invokeLater`) `setValueFromUser` + `input` 이벤트(`insertText`/`deleteContentBackward`/`insertCompositionText`); 캐럿/선택은 `setSelectionFromUser`(상태 통지 없이, 글이 선택된 경우만 `select` 이벤트); 입력기의 `InputMethodEvent` 는 `compositionstart/update/end`. 포커스를 잃으면(값이 바뀐 경우) `change`, Enter 는 `change` 후 `implicitSubmit`.
- DOM → Swing: `stateChanged`/렌더링 직후에 **편집기와 DOM 이 공통으로 가진 마지막 값**(`lastValue`)과 다를 때만 글을 바꾸고, DOM 선택이 움직였을 때만 선택을 바꾼다. 그래서 빠르게 입력하는 동안 DOM 이 늦게 따라와도 편집기의 글이 되돌려지지 않고, `type=number` 에 `abc` 를 입력해 DOM 값이 빈 문자열이어도 편집기의 글이 유지된다. `readonly/disabled/maxlength` 변경도 렌더링 뒤에 동기화한다(maxlength 는 `DocumentFilter` 로 UTF-16 단위, 서로게이트 쌍은 쪼개지 않음).
- 키 이벤트와 포커스 이벤트는 페이지의 `PageKeyboard` 가 그대로 받는다(DOM `keydown/keypress/keyup`, Tab 이동, Enter 제출). 편집기 위의 `click` 은 페이지로 다시 전달한다.

**select**: 닫힌 드롭다운은 선택된 라벨과 화살표를 그리고, 클릭하면 컨트롤 아래에 목록(`ControlPicker`)이 열린다(마우스로 행 고르기, 화살표/Enter/스페이스, Esc/Tab/포커스 상실로 닫기, 비활성 option 과 그룹 라벨은 고를 수 없음). 선택이 바뀌면 `input` + `change`. 닫혀 있을 때의 키: 화살표(비활성 option 건너뜀), Home/End, PageUp/PageDown, 글자로 찾기, Enter/스페이스/Alt+아래쪽은 목록 열기. 리스트 박스는 행 클릭(Ctrl 토글, Shift 범위).
**range/color/radio/file**: 슬라이더는 누르기와 끌기(`mousedown/mousemove/mouseup`, 놓으면 `change`)와 방향키/Home/End/PageUp/PageDown, 색 단추는 `ColorChooser`(기본 `JColorChooser`), 라디오는 화살표로 그룹 안 이동(비활성 건너뜀, 한 바퀴), 파일은 `UploadChooser`(기본 `JFileChooser`, 취소는 `null` → `cancel` 이벤트, 고른 파일만 `FileEntry`).

변경 파일: 위 구조 표의 클래스와 `src/org/duckdns/hjow/swingwebpanel/dom/html/{HtmlSelectElement,HtmlInputElement,HtmlTextAreaElement}.java`(훅 추가), 시험 `tests/src/org/duckdns/hjow/swingwebpanel/{layout/ControlLayoutTest,render/ControlPaintTest,core/FormControlIntegrationTest,core/FormPickerIntegrationTest,FormComponentIntegrationTest}.java`, 이 파일.
`dom/html` 에 추가한 훅(전부 추가만): `ControlDisplay`(신규 클래스), `HtmlSelectElement.getOptionElements()`, `HtmlInputElement.getRadioGroup()`, `HtmlInputElement.setSelectionFromUser(int,int,String)`, `HtmlTextAreaElement.setSelectionFromUser(int,int,String)`.
검증 결과(JDK 6u45, 헤드리스, Windows 10):
- 새 시험 50건: `ControlLayoutTest` 14(고유 크기 규격 공식, 텍스트 필드 박스, 날짜/숫자, 텍스트 영역, 체크박스, 단추 라벨, select, progress/meter, hidden, 기준선 정렬, fieldset legend, legend align, box-sizing, 비례 글꼴), `ControlPaintTest` 14(화소와 스냅샷: 체크박스 상태, 라디오, 단추 가운데 정렬, 텍스트/placeholder/암호, 텍스트 영역 줄 바꿈, 드롭다운, 리스트 박스, progress, meter 영역, range, 기하, fieldset 경계선 끊김), `FormControlIntegrationTest` 13(포커스→편집기, 입력→DOM 값과 `input`/`change`, 텍스트 영역/선택, 스크립트 변경→편집기, 빠른 입력, maxlength/readonly, Enter 제출, 검증 말풍선, 파일 선택, 클릭 토글과 다시 그리기, 배치 따라가기와 페이지 교체, autofocus, 입력기 조합 이벤트), `FormPickerIntegrationTest` 7(드롭다운 열기/고르기, 비활성 option, 키, 리스트 박스 마우스, 슬라이더, 색, 라디오 화살표), `FormComponentIntegrationTest` 2(실제 `SwingWebPanel` + `TestHttpServer` 로 읽은 폼: 편집기 → 스크립트가 값을 읽음 → Enter 가 `submitForm` 으로 제출 → 파일 컨트롤 클릭이 `chooseUploadFiles` 로 → clean/dispose/페이지 교체가 편집기 제거).
- 전체 회귀(스냅샷 = HEAD + 내 경로, `--filter swingwebpanel`, 컴파일한 시험 패키지 js core render layout css dom html): **1512 통과 / 0 실패 / 1 건너뜀**(건너뜀 1건은 기존: `JarContentTest` 제품 JAR 미빌드). 이 숫자는 마지막 코드 변경(규격 공식, legend align, fieldset 경계선 조각) 뒤에 다시 돌린 것이다
- sourcecheck: 위 경로 0 위반.
- 이전 회귀 중 발견·수정: 렌더링 관찰자를 `ControlOverlays` 에 강하게 등록해 `PageReleaseIntegrationTest` 가 실패 → 약한 참조 관찰자(`RenderObserver`)로 교체.

자동 검증과 실제 입력의 구분(P11-10, P22-10):
- **자동 검증(헤드리스)**: 위 시험. AWT 포커스는 화면이 필요하므로 시험이 DOM 포커스를 주고, 편집기의 `Document` 를 직접 편집하고(사용자의 입력과 같은 경로), 편집기의 키 리스너와 페이지의 포커스 리스너를 직접 불렀다. 입력기는 `InputMethodEvent` 를 직접 만들어 보냈다.
- **실제 입력 확인 안 함**: 실제 키보드/마우스, 한글 IME 조합과 삭제, 실제 파일 대화상자와 색 선택 대화상자, 실제 포커스 이동(Tab 으로 편집기 사이), Swing 기본 키 바인딩(Ctrl+C/V/X/Z)과 클립보드, 실제 창에서의 목록/말풍선 모양. 이것들은 창 캡처와 입력이 가능한 환경에서 확인해야 한다(P09-08-b-2 와 같은 조건).

미해결 사항/한계:
1. 편집기는 페이지 컴포넌트의 자식이라 **항상 맨 위**에 그려진다: 다른 요소가 위에 겹치거나 `overflow` 로 잘린 컨테이너 안의 필드, 스크롤되는 내부 컨테이너 안의 필드는 편집기가 따로 잘리지 않고 컨테이너의 스크롤을 따르지 않는다(화면 밖으로 나간 필드의 편집기는 상자가 없어지면 숨긴다). z-index/정적 위치에 따른 가림은 구현하지 않았다.
2. 편집기에서 문자 입력의 `keydown` `preventDefault()` 는 글 삽입을 막지 못한다(Swing 이 먼저 처리). Tab/Enter 같은 페이지의 기본 동작만 취소할 수 있다. 편집기 위의 `mousedown/mouseup/mousemove` 는 DOM 으로 가지 않는다(`click` 만 다시 전달). 처음 누른 `mousedown` 은 편집기가 생기기 전이라 DOM 에 간다.
3. 텍스트 영역/리스트 박스의 정적 그림은 항상 맨 위(리스트 박스는 선택된 첫 행이 보이는 위치)에서 시작하고 마우스 휠로 스크롤하지 않는다. 텍스트 영역의 편집기 안에서는 스크롤된다. 편집기를 닫으면 스크롤 위치가 사라진다.
4. 텍스트 필드가 값보다 좁을 때의 정적 그림은 앞부분만 보인다(편집기는 캐럿을 따라 스크롤).
5. 날짜/시간/월/주 필드는 선택기 UI 없이 텍스트 필드, `number` 는 스핀 단추 없음, `datalist` 자동 완성 없음, `input type=image` 는 이미지를 그리지 않고 alt/value 글만 그린다(크기는 글 기준), `input type=search` 의 지우기 단추 없음.
6. 진행 막대의 불확정 표시는 정적 줄무늬(움직이지 않는다). `appearance`, `accent-color`, `color-scheme`, 컨트롤의 `border-radius` 와의 상호작용은 구현하지 않았다(모양은 고정 색 `#0075ff`/`#767676` 등).
7. 문자열은 영어 고정이다: `Submit`, `Reset`, `Choose File`, `No file chosen`, `N files`(지역화 없음). 검증 메시지도 FORMS-A 의 영어 메시지이다.
8. 규격/Chromium 과의 차이: (a) `fieldset` 의 UA padding 은 Chromium 값(`0.35em 0.75em 0.625em`)을 쓴다(2014 규격은 `0.35em 0.625em 0.75em`). (b) 파일 컨트롤은 단추가 앞, 이름이 뒤(Chromium)이다(2014 규격은 이름 뒤에 단추). (c) 텍스트 필드 너비 `(size-1)×avg + max` 의 avg 는 숫자 0 의 너비, max 는 `0`/`W`/`M` 중 가장 넓은 것(OS/2 `xAvgCharWidth` 와 글꼴의 최대 문자 너비는 Java2D 로 얻지 못함). 고정폭 글꼴에서는 `size×문자 너비`로 정확하고, 비례 글꼴에서는 Chromium 과 몇 픽셀 다를 수 있다. (d) 텍스트 영역 너비에 스크롤 막대 너비를 `overflow` 가 hidden 이 아닐 때만 더한다(규격은 항상). (e) 리스트 박스 너비에는 규격대로 스크롤 막대 너비를 항상 더한다. (f) `button` 요소의 내용은 세로 가운데 정렬하지 않는다(`height` 를 크게 주면 위에 붙음). (g) `fieldset` 이 새 블록 서식 문맥을 만든다는 규격 문장은 구현하지 않았다(float 와의 상호작용만 다름).
9. 위쪽으로 튀어나온 legend 의 높이는 fieldset 의 위쪽 margin 과 합쳐진 값으로 비운다(Chromium 은 fieldset 의 border box 를 아래로 민다). 시각적 결과는 같지만 `getBoundingClientRect()` 의 fieldset 위쪽 값이 다를 수 있다. `groove` 경계선은 소수 좌표에서 아래쪽 경계선이 1px 로 그려지는 기존 반올림 현상이 있다(legend 와 무관, fieldset 이 없는 상자에서도 같다).
10. 호환 모드의 `input:not([type=image]), textarea { box-sizing: border-box }` 규칙은 넣었지만 호환 모드 문서로 시험하지 않았다(`LayoutTestSupport` 가 DOCTYPE 을 붙인다).
11. 라디오 그룹에서 Tab 정류장이 체크된 하나뿐이어야 한다는 규칙은 구현하지 않았다(`FocusManager` 소유). 텍스트 필드의 컨텍스트 메뉴, 맞춤법 검사는 없다.
12. P11-08 의 페이지 본문 텍스트 선택/복사와 확대는 이 작업 범위가 아니다(편집기 안의 선택, 복사, 붙여넣기는 Swing 기본 동작).
13. 파일 읽기는 선택 시점에 파일 전체를 메모리로 읽는다(한도 64 MiB, 넘는 파일은 건너뜀; 모두 건너뛰면 취소와 같다).
다음 작업 번호: 조정자 연결(`FormSubmitSink` 구현과 `NavigationRequest`) → P15-08-c-2 폼 이동/다운로드 판정, P19-07 POST 이력 정책 → 실제 Swing 창에서 P11-02/P11-10 확인 → P11-08.
조정자 요청:
1. **`BrowserSession implements FormSubmitSink`** (`void submit(FormSubmission)`): `SwingWebPanel` 이 `session instanceof FormSubmitSink` 이면 `submitForm` 이 그대로 위임한다(생성자에서 `view.setFormSubmitSink(...)`, 아직 구현이 없으면 상태 텍스트에 `Form submission is not supported: <url>` 을 보인다). 다른 방법은 `SwingWebPanel.submitForm` 본문을 직접 구현하는 것이다. 문서 스레드에서 호출되므로 막으면 안 된다. 폼 제출 다운로드 판정은 응답 쪽(P15-08-c-2).
2. **FEATURE_MATRIX** 폼/입력 행: 구현 클래스에 `layout/ControlSizes`, `render/Control*`, `core/{PageFormHost,ControlOverlays,ControlEditor,ControlPicker,ControlActions}`, 지원 상태는 "부분 구현(DOM 상태/검증/직렬화/JS 연결 + 컨트롤 그리기, Swing 편집기, select 목록, 슬라이더, 색, 파일 선택, fieldset/legend 자동 검증 완료. 실제 키보드/IME/대화상자 확인 안 함)".
3. **ARCHITECTURE/API_CONTRACT**: 새 공개 확장점 `SwingWebPanel.submitForm`, `chooseUploadFiles`(protected), `core.FormSubmitSink/UploadChooser/ColorChooser/ControlOverlays`. P21-01 소유 목록: `ControlOverlays`(EDT 컴포넌트, `PageView.closeActive` 가 `clearLater()`), `PageFormHost`(문서의 DOM 리스너, `HtmlPageDocument.dispose` 가 `close()`, 렌더링 관찰자는 약한 참조), 편집기/목록/말풍선의 `javax.swing.Timer`(말풍선 5초, `hideMessage` 가 중지).
4. **LAYOUT-E 와의 충돌 가능 파일**(내 변경은 작은 hunk): `layout/{BoxBuilder(2 줄+1 줄), BlockLayouter(`finish` 의 기준선 3 줄, `layoutChildren` 의 legend 블록), Replaced(TAGS 와 `natural` 앞 8 줄), LayoutBox(필드와 getter)}`, `render/{DisplayListBuilder(`paintReplaced` 앞 6 줄, `border` 의 fieldset 갈래), DisplayCommand(enum 상수 `CONTROL`), RenderedPage(`getControl`)}`, `css/cssom/UaStyleSheet`(폼 규칙 블록).
5. 체크박스 후보(자동 검증 기준, 실제 입력 확인은 별도): **P11-05**(컨트롤 상태 화면 표시와 DOM 동기화: DOM 상태/검증은 FORMS-A, 화면/편집/동기화는 이 작업; 입력 상태 표시, 라디오 그룹, label, tabIndex, 파일 선택/취소), **P11-07**(DOM 값 변경이 표시를 갱신, 파일 읽기는 사용자가 고른 파일로 한정; submit/reset 이벤트와 검증은 FORMS-A). P11-02(한글 입력 실제 확인), P11-06(제출 이동 본문), P11-08, P11-10 은 후보가 아니다.
체크박스 후보: P11-05, P11-07 (위 한계 1~13 과 "실제 입력 확인 안 함" 을 PROGRESS 에 함께 기록할 것)
