# FORMS-A 진행 기록 (P11-05/06/07 중 폼 컨트롤의 DOM 상태, 검증, 직렬화)

소유 경로: `src/org/duckdns/hjow/swingwebpanel/dom/html/**`, `src/org/duckdns/hjow/swingwebpanel/js/dom/FormBindings*.java`, `src/org/duckdns/hjow/swingwebpanel/css/selector/HtmlStates.java`(상태 기반 의사 클래스 추가만),
`src/org/duckdns/hjow/swingwebpanel/js/host/web/FormDataApi.java`(FormData(form) 연결), `tests/src/org/duckdns/hjow/swingwebpanel/dom/html/**`, `tests/src/org/duckdns/hjow/swingwebpanel/js/dom/FormBindingTest.java`, 이 파일.
공용 파일(`dom/Document.java` 등)은 최소한의 추가만 한다.
시험 실행(격리 스냅샷): `sh scripts/snaptest.sh forms "src/org/duckdns/hjow/swingwebpanel/dom src/org/duckdns/hjow/swingwebpanel/css/selector tests/src/org/duckdns/hjow/swingwebpanel/dom" "js dom html css net storage core content layout render download media @root" "js dom html css net storage core content layout render download media @root" --filter swingwebpanel`.
빠른 반복: 작업 트리의 `src/org/duckdns/hjow/swingwebpanel/dom`, `html` 만 JDK 6u45 `javac` 로 컴파일(`-sourcepath src`)하고 `tests/src/org/duckdns/hjow/swingwebpanel/dom/html` 의 시험을 `TestRunner --scan ... --filter ...` 로 실행.
린트: `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/dom/html tests/src/org/duckdns/hjow/swingwebpanel/dom/html src/org/duckdns/hjow/swingwebpanel/dom/Document.java`.

참고 규격(조회 도구 `tools/webfetch`, 새 JRE = Zulu 25 로 실행):
- HTML5 W3C Recommendation 2014-10-28, 4.10 Forms — `https://www.w3.org/TR/2014/REC-html5-20141028/forms.html` (Last-Modified 2014-10-24, 조회 2026-11-17, 본문 333082 바이트; 저장 `build/html5-forms-20141028.txt`).

## 작업 기록

### 2026-11-17 FORMS-A: JS 연결, FormData(form), 상태 의사 클래스, 제약 검증/제출/활성화 시험
현재 작업 번호: P11-05/06/07 (DOM 모델과 JS 연결 부분)
완료한 내용:
- **JS 연결**(`js/dom/FormBindings.java`, `FormBindingsCollections.java`, `FormBindingsMembers.java`): `HTMLFormElement`, `HTMLInputElement`, `HTMLButtonElement`, `HTMLSelectElement`, `HTMLOptionElement`, `HTMLOptGroupElement`, `HTMLTextAreaElement`, `HTMLLabelElement`,
  `HTMLFieldSetElement`, `HTMLLegendElement`, `HTMLOutputElement`, `HTMLDataListElement`, `HTMLProgressElement`, `HTMLMeterElement`, `SubmitEvent`, `ValidityState`, `HTMLFormControlsCollection`, `HTMLOptionsCollection`, `RadioNodeList`, `FileList`, `Option` 생성자.
  IDL 속성은 콘텐츠 속성이 아니라 컨트롤 **상태**를 읽고 쓴다(`value`/`checked`/`selected`/`validity`/`valueAsNumber`/`valueAsDate`(Date)/`selectionStart`(null 허용)/`files`...). `form.q`, `form[0]`, `select[i]`, `elements.name`(RadioNodeList) 접근, `add/remove/stepUp/stepDown/setRangeText/requestSubmit`.
  `DomInterfaces` 는 폼 인터페이스 7개의 정의를 지우고 `FormBindings.install(bridge)` 호출 1줄과 태그 표 7개(optgroup/fieldset/legend/output/datalist/progress/meter), `SubmitEvent` 분류 1줄만 바꿨다.
- **FormData(form)** (`js/host/web/FormDataApi.java`): `FormEntryList.construct` 로 상태에서 항목을 만든다(textarea CRLF, submitter, 사용자가 고른 파일은 실제 `File`). `HtmlFormElement` 가 아니면 이전 방식(콘텐츠 속성)으로 대신한다. `realm` 에 `FormBindings.FILE_FACTORY_KEY` 로 `File` 팩터리를 등록해 `input.files` 가 같은 `File` 을 만든다.
- **상태 의사 클래스**(`dom/html/FormStates.java` 신규 + `css/selector/HtmlStates.java` 가 먼저 호출): `:checked :disabled :enabled :indeterminate :default :required :optional :read-write/:read-only :placeholder-shown :valid :invalid` 가 컨트롤의 실제 상태를 따른다.
  `:in-range/:out-of-range` 는 `FormStates.inRange/outOfRange`, `HtmlStates.isInRange/isOutOfRange` 까지만 구현했고 `SelectorMatcher` 연결은 소유 밖이라 하지 않았다(아래 조정자 요청).
- 구현 보완: 파일 선택기는 신뢰된 click 일 때만, 선택기를 열었다 취소했을 때만 `cancel`; `requestSubmit` 은 JS 에서 제출 버튼이 아니면 `TypeError`; `HtmlOptionsCollection.add/remove/selectedIndex`; `HtmlListedElement.isWillValidate`; `HtmlFormElement.setAction/setMethod/setEnctype`.
변경 파일: `src/org/duckdns/hjow/swingwebpanel/dom/html/{FormStates(신규),FormActivation,HtmlControl,HtmlFormElement,HtmlInputElement,HtmlLabelElement,HtmlListedElement,HtmlOptionsCollection,HtmlTextAreaElement,FormEntryList,HtmlGenericElement,HtmlElementFactory,FormSupport}.java`,
`src/org/duckdns/hjow/swingwebpanel/js/dom/{FormBindings,FormBindingsCollections,FormBindingsMembers(신규),DomInterfaces}.java`, `src/org/duckdns/hjow/swingwebpanel/js/host/web/FormDataApi.java`, `src/org/duckdns/hjow/swingwebpanel/css/selector/HtmlStates.java`,
`tests/src/org/duckdns/hjow/swingwebpanel/dom/html/{FormTestSupport,FormControlStateTest,FormValidationTest,FormSelectTest,FormSubmissionTest,FormActivationTest,FormStatesTest}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/js/dom/FormBindingTest.java`, `tests/src/org/duckdns/hjow/swingwebpanel/js/host/web/FormDataFormTest.java`.
검증 결과 (JDK 6u45): 빠른 반복(`build/pkg-forms`): `org.duckdns.hjow.swingwebpanel.dom.html` 70건 통과(상태 18, 검증 13, select 8, 제출 14, 활성화 11, 의사 클래스 6), `FormBindingTest` 11건 + `FormDataFormTest` 3건 통과, sourcecheck 0 위반.
전체 회귀(스냅샷 `build/snap-forms` = HEAD + 내 경로, `--filter swingwebpanel`, JDK 6u45): **2015 통과 / 1 실패 / 2 건너뜀**. 실패 1건은 `css.SelectorTest.testFormAndLinkStates` 의 `:valid` 기대값이 이전 속성 기반 근사이기 때문이다(아래 조정자 요청). 앞선 스냅샷에서는 `js.dom.DomBindingTest.testAttributesAndReflection` 1건도 같은 이유로 실패했고(`input.type` 기본값), 그 파일을 고친 사본을 덮어쓰고 시험해 통과를 확인했다. 건너뜀 2건은 기존: `HttpFetcherTest` Origin 헤더, `JarContentTest` 제품 JAR 미빌드.
미해결 사항/한계(= HTML5 2014 와 Chromium 의 차이 포함):
1. 라디오 그룹의 이름 비교는 2014 의 "compatibility caseless match" 를 따라 대소문자를 무시한다(Chromium/현행 WHATWG 는 정확히 같은 이름). 
2. `pattern` 은 프로젝트 정규식 엔진의 유니코드 모드(`^(?:...)$`, u 플래그)로 검사한다(2014 규격은 플래그 없음, 현행 Chromium 은 v 플래그). 올바르지 않은 pattern 은 무시.
3. `object`/`keygen` 은 폼 연관 요소가 아니다(일반 요소). `output.htmlFor` 는 DOMTokenList 가 아니라 문자열. `fieldset.elements`/`selectedOptions`/`datalist.options` 는 `HTMLFormControlsCollection`, `labels` 도 NodeList 가 아니라 같은 컬렉션이다(`instanceof HTMLCollection` 은 참).
4. `select[i] = option` 처럼 위치로 대입하는 것은 지원하지 않는다(`options.add/length` 는 지원). `input.formAction` 등은 속성만 반영(속성이 없으면 ""; 규격은 문서 URL).
5. 검증 메시지는 영어만이다. `datalist` 자동 완성, 색/날짜 선택 UI, 파일 선택 대화상자, 텍스트 편집과 캐럿, IME, 포커스 그리기는 호스트(렌더러)의 일이며 이 계층에는 훅과 상태만 있다.
6. 파일은 `FileEntry` 가 바이트 배열 전체를 가진다(선택 시 호스트가 읽음). 큰 파일의 메모리 사용은 호스트가 제한해야 한다.
7. `requestSubmit` 은 2014 판에 없는 WHATWG 현행 API 이다(구현함).
다음 작업 번호: 조정자 연결(`FormHost`, 컨트롤 그리기/편집, `NavigationRequest`) → P11-05/06/07 중 호스트 의존 부분
조정자 요청:
- **`FormHost` 설치/연결**: `document.setFormHost(host)` — `submit(FormSubmission, form)`: `FormSubmission` 은 `getMethod()`(GET/POST; ftp/javascript/data 는 GET), `getUrl()`(GET 이면 쿼리가 들어 있음), `getContentType()`/`getBody()`(POST), `getTarget()`, `getEncoding()`, `getEntries()` 를 가진 값 객체이다. `NavigationRequest`(POST 본문, 이력, P19-07)로 연결하고, 다운로드 판정(P15-08-c-2)은 응답 쪽에서 한다.
  `reportInvalid(control, message)`(말풍선), `chooseFiles(input, multiple, accept)`(`JFileChooser`, 취소하면 null, 파일은 `new FileEntry(name, mime, lastModified, bytes)`), `stateChanged(element)`(**스타일/그리기 무효화**, 의사 클래스 `:checked/:valid/:placeholder-shown` 등이 상태를 따르므로 필요).
- **사용자 입력 API**(호스트가 호출): 텍스트 편집은 `HtmlInputElement/HtmlTextAreaElement.setValueFromUser(text)` + `setSelectionRange` + `HtmlControl.fireInputEvent(inputType, data)`, 포커스를 잃거나 Enter 를 누르면 `fireChangeEvent()`; 숫자/날짜 입력에서 변환 불가 글이면 `setBadInput(true)`; Enter 는 `HtmlFormElement.implicitSubmit()`;
  option 선택은 `HtmlOptionElement.setSelected(true)` 후 `fireInputEvent`/`fireChangeEvent`; 체크박스/라디오/버튼/label/파일은 **마우스 click 을 `dispatchTrustedEvent(MouseEvent("click", true, true))` 로 보내면** 활성화 동작(토글, 제출, 초기화, label→컨트롤, 파일 선택기)이 실행된다(이미지 버튼은 `offsetX/offsetY` 가 선택 좌표).
- **SelectorMatcher**: `case PseudoClasses.IN_RANGE: return state(ctx, e, id, HtmlStates.isInRange(e)); case OUT_OF_RANGE: ... HtmlStates.isOutOfRange(e)` 로 바꾸면 `:in-range/:out-of-range` 가 연결된다(소유 밖이라 하지 않음).
- **`tests/src/org/duckdns/hjow/swingwebpanel/css/SelectorTest.java` 345행**: `:valid` 기대값 `"cb tx fieldset inleg infs"` 를 `"cb b1 fieldset inleg"` 로 고쳐 달라. 규격(HTML5 2014 4.16.3, 4.10.20)상 비활성 컨트롤(`tx`, 비활성 fieldset 안의 `infs`)은 제약 검증 대상이 아니라 `:valid` 도 `:invalid` 도 아니고, 제출 버튼(`b1`)은 검증 대상이라 `:valid` 이다. 이전 값은 속성 기반 근사였다.
- **`tests/src/org/duckdns/hjow/swingwebpanel/js/dom/DomBindingTest.java` 114행**: `document.createElement('input').type === ''` 는 이전 반영 방식의 기대값이다. 규격(와 새 구현)은 `'text'` 이므로 `=== 'text'` 로 고쳐 달라(소유 밖이라 고치지 않음; 스냅샷에서 이 한 건이 실패한다).
- `js/host/ElementExtras.java` 460행 `textAccessor(HTMLOptionElement...)` 는 `FormBindings` 의 `option.text`(공백 정리) 를 덮어쓴다. 이 줄을 지우거나 option 을 제외해 달라.
- FEATURE_MATRIX: 폼/입력 행의 구현 클래스에 `dom/html/**`, `js/dom/FormBindings*`, 지원 상태는 "부분 구현(DOM 상태/검증/직렬화/JS 연결 검증 완료, 컨트롤 그리기·편집·파일 선택기는 미구현)".
체크박스 후보: 없음(P11-05/06/07 은 렌더러 연결과 실제 입력 확인이 필요하다. DOM 모델/직렬화/검증/JS 연결 부분은 시험으로 검증됨).

### 2026-11-17 FORMS-A 재개: 컴파일 확인과 활성화 동작 보완
현재 작업 번호: P11-05 (DOM 모델 부분)
완료한 내용:
- 이전 작업자가 남긴 `dom/html/**` 40개 파일은 **빠진 클래스 2개(`HtmlElementFactory`, `FormActivation`)만 추가하면 그대로 컴파일**되었다. 버리지 않고 살렸다.
- `HtmlElementFactory`(신규): form/input/button/select/option/optgroup/textarea/label/fieldset/legend/datalist/output/progress/meter 를 HTML 네임스페이스의 소문자 이름에 대해 특화 클래스로 만들고 나머지는 `HtmlGenericElement`. `Document` 의 기본 팩터리이다.
- `FormActivation`(신규): DOM 표준 "dispatch" 의 활성화 동작. 활성화 대상은 대상 자신 또는(click 이 버블링하므로) 활성화 동작이 있는 가장 가까운 조상. 전달 전 legacy-pre-activation(체크박스 토글/라디오 체크), 취소되면 legacy-canceled-activation, 아니면 activation behavior(input/change, 제출, 초기화, label→labeled control).
  비활성 폼 컨트롤을 대상으로 한 click 은 전달하지 않는다(HTML5 2014 4.10.19.1). `HtmlGenericElement` 도 같은 처리를 하므로 `button` 안 `span` 을 눌러도 동작한다. 같은 이벤트가 두 번 활성화되지 않도록 `FormSupport.dispatchPlain/isPlain` 으로 구분한다.
- 라디오 그룹의 이름 비교를 HTML5 2014 의 "compatibility caseless match" 에 맞춰 대소문자 무시로 고쳤다(Chromium 은 대소문자 구분: 차이로 기록).
- `form[name]`(`getNamedProperty`)이 이미지 버튼도 찾도록 고쳤다.
- `FormEntryList`: HTML5 2014 4.10.22.4 의 "End" 단계에 맞춰 이름과 (파일이 아닌) 값의 줄 바꿈을 항목 목록 단계에서 CRLF 로 정규화(`FormData` 도 같은 값을 본다). textarea 는 제출 값(`getSubmissionValue`: CRLF, `wrap=hard` 이면 cols 로 줄 나눔).
- 파일 선택기(`FormHost.chooseFiles`)는 **신뢰된 click 일 때만** 연다(스크립트의 합성 click 은 열지 않음). 선택기를 열었는데 취소했을 때만 `cancel` 이벤트를 발생시킨다.
변경 파일: `src/org/duckdns/hjow/swingwebpanel/dom/html/{HtmlElementFactory,FormActivation(신규),HtmlControl,HtmlGenericElement,FormSupport,HtmlFormElement,HtmlInputElement,...}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/dom/html/{FormTestSupport,FormControlStateTest}.java`, 이 파일.
검증 결과: 작업 트리를 HEAD 위에 덮어쓴 스냅샷 `build/snap-forms`(JDK 6u45)에서 전체 시험 `--filter swingwebpanel` = **1868 통과 / 0 실패 / 2 건너뜀**(기본 팩터리를 바꾼 뒤에도 기존 시험이 모두 통과; 건너뜀 2건은 기존 것: HttpFetcherTest Origin 헤더, JarContentTest 제품 JAR 미빌드).
`FormValidationTest` 13건, `FormSelectTest` 8건, `FormSubmissionTest` 14건, `FormActivationTest` 11건, `FormControlStateTest` 18건 통과(팩터리 클래스, value/dirty, 텍스트/수치 값 정리, range, valueAsNumber/Date/stepUp, checkedness, 라디오 그룹, 폼 소유자, form.elements, label, 비활성 fieldset, textarea, output, 복제, type 변경, 폼 사이 이동). sourcecheck 0 위반.
미해결 사항: (1) `object`/`keygen` 은 폼 연관 요소(listed)가 아니다(일반 요소). (2) 아래 목록은 작업 진행에 따라 갱신한다.
다음 작업 번호: P11-05 시험 계속(select/제약 검증/제출/활성화) → P11-06/07 → JS 연결
조정자 요청: 없음(아직).
체크박스 후보: 없음.
