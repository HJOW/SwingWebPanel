# P20-01 / P20-10 / P20-11 / P20-12 / P22-14 작업 기록 (MATRIX 작업자, 2026-11-17)

## 조정자 요청 (AGENTS.md 체크 후보와 근거)

이 작업자는 `AGENTS.md`, `docs/PROGRESS.md`, `docs/ARCHITECTURE.md`, `docs/API_CONTRACT.md` 를 고치지 않았다. 아래는 체크해도 되는지 판단할 근거이다.

| 항목 | 제안 | 근거 |
| --- | --- | --- |
| **P20-01** HTML 요소별 인터페이스/속성 반영/메서드/기본 동작과 document 공통 API | 체크 가능(조정자 판단) | 요소별 점검표(아래 1절)에서 규격의 요소 인터페이스는 모두 전용 인터페이스를 가지고(REC 가 정한 요소 중 `HTMLUnknownElement` 로 나오는 것 없음), 규격에 있는 속성은 반영되거나 상태가 구현되었다. 남은 것은 `HTMLObjectElement.validity`(`ValidityState` 가 폼 컨트롤 전용 클래스), `HTMLImageElement.x/y`, 새 규격의 `showPicker`/`requestClose`/`supports`/`blocking` 같은 HTML Living Standard 에만 있는 멤버, SVG DOM 이다. 이 작업에서 `details`/`summary` 동작, 클라이언트 이미지 맵, `meta refresh`, `document.elementsFromPoint`, `clientTop/clientLeft/offsetParent`, `Node.lookupNamespaceURI` 계열, 폐기된 IDL 반영 속성, `window.event`, `console` 전체, `crypto.getRandomValues`, `applicationCache`, `navigator` 의 나머지, 마이크로데이터를 더했다 |
| **P20-10** HTML 규격과 참조 규격의 나머지 기능 | 체크 가능(조정자 판단) | FEATURE_MATRIX 11절이 HTML5 2014 REC 의 참조 규격(조회한 단일 페이지 판 1,063,542 바이트)을 모두 열거하고 각각 구현함/정책으로 차단·실패/범위 밖(근거)/미구현으로 분류했다. WebSocket 은 스킴 정책으로 실패(EXC-06)하고 `EventSource` 는 설계만 적고 미구현으로 남겼다. "규격 대상과 제약의 충돌이 남으면 미완료" 조건에서 충돌은 없다(WebSocket 은 EXC-06 으로 기록). 미구현이 남아도 체크할지는 조정자가 판단한다: `EventSource`, 텍스트 트랙, 드래그 앤 드롭, `pointer*`, XPath |
| **P20-11** SVG/MathML, 문서 모드, 오래된 속성, 프레임, 폼/표, 현대 문서 혼합 검증 | 체크 가능 | `MixedDocumentsIntegrationTest` 6건 + `core.DetailsMapRefreshIntegrationTest` 3건(실제 컴포넌트, JDK 6u45). 미지원 플러그인(`object`/`embed`/`applet`)은 대체 내용 또는 안내를 보이고 실행했다고 기록하지 않았다. 검증 중 찾은 결함(인라인 SVG 미그림, `object` 대체 내용 미배치, `details`, 이미지 맵, `meta refresh`)은 고쳤다 |
| **P20-12** 축별 대조 | 체크 가능 | FEATURE_MATRIX 13절: 축별 개수와 남은 행 목록, 명시적 예외(EXC-01~07)를 미구현과 구분해 근거와 함께 적었다. 이 조건("남은 항목을 모두 처리한 뒤")을 "미구현·부분 구현을 숨기지 않고 목록화했다"로 읽을지는 조정자가 판단한다 |
| **P22-14** 실제 검증된 판본/환경/사례와 남은 호환성 차이 | 체크 가능(조정자 판단) | 기능 표의 `T-...`/`C-...` 계획용 번호를 실제 시험 이름으로 모두 바꾸고(참조한 시험 클래스가 실제로 있고 통과하는지 스크립트로 확인), 낡은 `미구현` 행을 현실에 맞췄다. 14절에 환경·시험 수·남은 차이를 적었다. "미구현이나 설명 없는 건너뜀이 남으면 전체 목표는 완료로 표시하지 않는다" 조건 때문에 **전체 목표 완료 표시는 하지 않는다** |

## 1. P20-01 구현 점검 (요소별 인터페이스와 document 멤버)

**시작할 때 이미 구현되어 있던 것**(이 작업에서 시험으로 확인: `ElementStatesTest` 8건 통과): `HTMLMapElement.areas`, `HTMLDialogElement.show/showModal/close/returnValue`(+`close` 이벤트), `HTMLTrackElement.readyState/track`, `HTMLObjectElement.form/contentDocument/contentWindow/getSVGDocument`, `rowSpan` 기본값 1, `document.elementFromPoint`, `scrollingElement`, 표 객체 모델, 28개 반영 전용 인터페이스. 조정자의 이전 기록(PROGRESS 3절)은 이 항목들을 "남은 것"으로 적었으나 063/065 커밋에서 구현되어 있었다.

**이 작업에서 더한 것**(시험은 `LegacyWebApiTest` 7, `LegacyWebApiIntegrationTest` 3, `MicrodataTest` 5, `ConsoleApiTest` 8, `MixedDocumentsIntegrationTest` 6, `core.DetailsMapRefreshIntegrationTest` 3):
- `js.host.LegacyWebApi`(신규): `window.event`(리스너가 도는 동안의 현재 이벤트, `DomBridge.enterEvent/leaveEvent` 로 중첩 전달 복원), `external`, `moveTo/moveBy/resizeTo/resizeBy`(효과 없음), `locationbar/menubar/personalbar/scrollbars/statusbar/toolbar`(`visible` false), `document.elementsFromPoint`(최상위 요소와 조상), `applets`, `clear/captureEvents/releaseEvents`, `fgColor/bgColor/linkColor/vlinkColor/alinkColor`, `Node.lookupNamespaceURI/lookupPrefix/isDefaultNamespace`(`dom.NamespaceLookup`, DOM Standard 알고리즘), `Element.clientTop/clientLeft/offsetParent`(CSSOM View), `scrollIntoViewIfNeeded`, `window.applicationCache`, `navigator.taintEnabled/registerProtocolHandler/registerContentHandler/unregister*/yieldForStorageUpdates/webdriver`, `crypto.getRandomValues/randomUUID`.
- 폐기된 IDL 의 반영 속성(HTML5 2014 REC 11.2): `HTMLTableElement`(align, bgColor, cellPadding, cellSpacing, frame, rules, width), 행·셀(align, bgColor, ch, chOff, vAlign, axis, height, noWrap, width), `HTMLAnchorElement`(coords, charset, name, rev, shape), `HTMLBodyElement`(text, link, vLink, aLink, bgColor, background), `HTMLHtmlElement.version`, `HTMLHeadElement.profile`, `HTMLBRElement.clear`, `HTMLHRElement`(align, color, noShade, size, width), `HTMLPreElement.width`, `HTMLUListElement`(compact, type), `HTMLOListElement.compact`, `HTMLInputElement`(align, useMap, capture), `HTMLScriptElement`(event, htmlFor, integrity, referrerPolicy), `HTMLLinkElement`(charset, rev, target, sizes, imageSrcset, imageSizes), `HTMLMetaElement`(scheme, media), `HTMLIFrameElement`(allow, loading, referrerPolicy, getSVGDocument), `HTMLEmbedElement.getSVGDocument`, `HTMLFormElement`(rel, relList), `HTMLDetailsElement.name`, `HTMLVideoElement.playsInline`, `HTMLImageElement.fetchPriority`, `HTMLLegendElement.form`, `Element.setAttributeNodeNS`.
- `js.builtins.ConsoleBuiltins`(신규): Console Standard 전체와 서식 문자열(WEB-13).
- `details`/`summary`(`layout.BoxBuilder.isHiddenDetailsChild`, `dom.html.FormActivation.activateSummary`, `ElementStates.mutated` 의 `toggle` 이벤트), 클라이언트 이미지 맵(`render.ImageMaps`, `render.HitTester`), `meta http-equiv=refresh`(`js.host.MetaRefresh`).
- 인라인 `<svg>` 그리기(`render.InlineSvg`), `object` 의 대체 내용 배치(`layout.Replaced.isReplaced`)와 `embed`/`object` 안내(`render.DisplayListBuilder`).
- 마이크로데이터(`dom.Microdata`, `dom.PropertiesCollection`, `dom.PropertyNodeList`, `js.host.MicrodataApi`).

**요소별 점검표(2026-11-17, JRE 6 `typeof` 조사 후 시험으로 확인)**: HTML5 2014 REC 의 요소는 모두 전용 인터페이스(또는 규격대로 `HTMLElement`)를 가진다. `HTMLUnknownElement` 로 남는 것은 REC 가 인터페이스를 정하지 않은 과거·비표준 태그(`blink`, `bgsound`, `isindex`, `listing`, `noembed`, `noframes`, `plaintext`, `xmp`, `command`, `element`, `image`, `shadow`, `content`, `spacer`)뿐이다(2026-11-17 `createElement` 조사. `js.dom.HtmlInterfacesTest` 가 `bgsound` 로 이 정책을 확인). 인터페이스가 있는 태그: 문서(`html head title base link meta style body`), 섹션/그룹/텍스트(`HTMLElement`, `h1`~`h6`=`HTMLHeadingElement`, `p div span br hr pre blockquote q ol ul li dl`, `a`, `data time mod(ins del)`), 임베디드(`img picture source iframe embed object param video audio track map area canvas svg`), 표(`table caption colgroup col tbody thead tfoot tr td th`), 폼(`form fieldset legend label input button select datalist optgroup option textarea output progress meter keygen`), 인터랙티브(`details dialog menu menuitem`), 스크립팅(`script template`), 과거(`font basefont dir applet marquee frame frameset`). 의미만 있는 요소(`abbr address article aside b bdi bdo cite code dd dfn dt em figcaption figure footer header hgroup i kbd main mark nav noscript rp rt ruby s samp section small strong sub summary sup u var wbr`와 `acronym big center nobr strike tt`)는 규격대로 `HTMLElement` 이다.

**남은 것(P20-01 의 대상 규격 밖이거나 규모가 큰 것)**: `HTMLObjectElement.validity`, `HTMLImageElement.x/y`, `Navigator/Location/History/Screen/Performance` 인터페이스 객체 이름(`instanceof` 불가), `DOMStringMap`/`DOMStringList`/`MutationRecord`/`XMLDocument` 전역 이름, SVG DOM(`SVGSVGElement`, `getBBox` 등: HEI-09), 텍스트 트랙 객체(VID-02), `caretRangeFromPoint`/`caretPositionFromPoint`, `performance.mark/measure/timing`, `IntersectionObserver`/`ResizeObserver`, `EventSource`, `PointerEvent`/`DragEvent`/`BeforeUnloadEvent`, XPath, Custom Elements/Shadow DOM.

## 2. 시험과 검증 명령 (JDK 6u45, `scripts/pkgtest.sh`)

- **전체 회귀**: `sh scripts/pkgtest.sh full2 "js dom html css net storage core content layout render download media @root" "js dom html css net storage core content layout render download media testing @root"` → **2988 통과 / 0 실패 / 13 건너뜀**(712,626 ms, JDK 6u45). 건너뜀 13건은 선택 시험이다: Opus 원본 벡터 12건(환경 변수 `SWP_OPUS_VECTORS` 지정 시에만 실행)과 제품 JAR 내용 시험 1건(JAR 을 만들지 않았다). 이 실행은 `core.DetailsMapRefreshIntegrationTest` 의 설명 주석 한 줄을 고치기 전의 소스로 컴파일했다(주석만 바뀜).
- 같은 명령의 첫 전체 실행(`full`, `details`/이미지 맵/`meta refresh` 구현과 시험 3건을 더하기 전): 2984 통과 / 1 실패 / 13 건너뜀. 실패 `js.host.web.XhrTest.testTimeout` 은 컴퓨터가 바쁠 때 간헐적으로 실패하는 시험이며 단독 재실행(`XhrTest` 15건)은 통과했다.
- 개별 시험(JDK 6u45, `scripts/pkgtest.sh` 와 부분 컴파일): `ConsoleApiTest` 8, `HostApiTest` 11(기존 콘솔 출력 형식 유지), `LegacyWebApiTest` 7, `LegacyWebApiIntegrationTest` 3, `MicrodataTest` 5, `MixedDocumentsIntegrationTest` 6, `DetailsMapRefreshIntegrationTest` 3, `ElementStatesTest` 8, `HtmlInterfacesTest` 5, `TableApiTest` 6, layout+render 532(1건 건너뜀: JAR) 모두 통과.
- **먼저 동작하지 않음을 확인한 것**: `details`/`summary`, 이미지 맵, `meta refresh`, 포인터 이벤트는 구현 전에 실제 컴포넌트 탐침(임시 프로그램, 저장하지 않음)으로 동작하지 않음을 확인했다(포인터 이벤트는 지금도 없다). 혼합 문서 시험은 처음 실행에서 `object` 대체 내용이 배치되지 않는 것을 드러냈다(고침). 인라인 SVG 는 레이아웃·그리기 코드를 읽고 그려지지 않음을 안 뒤 구현했고 시험으로 화소를 확인했다(수정 전 소스에서 시험이 실패하는 것은 확인하지 못했다).
- **소스 규칙**: `sh tools/sourcecheck/run.sh`(기본 경로 전체) → 0 위반(2038 파일).
- **기능 표의 근거 확인**: 기능 표가 인용한 시험 클래스 이름 430개가 실제로 존재하고 위 전체 실행에서 통과했는지, 인용한 제품 클래스가 `src` 에 있는지를 임시 스크립트(저장하지 않음)로 대조했다. 대조에서 틀린 인용(`AbortTest` 와 옛 계획용 클래스 이름 28개)을 고쳤다.
- **실행한 JRE**: 시험은 모두 JRE 6u45(`C:\Program Files\Java\jdk1.6.0_45`)이다. 규격 조회(`tools/webfetch`)만 JRE 25 로 실행했다(JRE 6 의 TLS 한계).

## 3. 변경 파일

제품: `js/builtins/{ConsoleBuiltins,GlobalBuiltins}`, `js/host/{LegacyWebApi,MicrodataApi,MetaRefresh,ElementStates,WindowGlobals,PageScripts}`, `js/dom/{DomBridge,JsListener,HandlerListener,DomInterfaces,FormBindings,HtmlMoreInterfaces}`, `dom/{NamespaceLookup,Microdata,PropertiesCollection,PropertyNodeList}`, `dom/html/FormActivation`, `layout/{Replaced,BoxBuilder}`, `render/{InlineSvg,ImageMaps,HitTester,DisplayListBuilder}`.
시험: `js/interp/ConsoleApiTest`, `js/host/{LegacyWebApiTest,MicrodataTest}`, `core/{LegacyWebApiIntegrationTest,DetailsMapRefreshIntegrationTest}`, `MixedDocumentsIntegrationTest`.
문서: `docs/FEATURE_MATRIX.md`(낡은 행 갱신, `T-`/`C-` 번호를 실제 시험 이름으로 교체, 11~14절 추가), 이 파일, `docs/progress/api-surface.md`.

## 4. 미해결 사항과 다음 작업 후보

- `EventSource`(11절의 설계), `pointer*` 이벤트(`core.PageInput` 의 마우스 전달에 병행 발생), 드래그 앤 드롭(P20-06), `unload`/`beforeunload`/`pagehide`(이동·clean·dispose 때 스크립트에 알리기), 텍스트 트랙, `IntersectionObserver`/`ResizeObserver`, MathML 배치, SVG DOM, 스크립트를 끈 페이지의 `meta refresh`(페이지 런타임이 없다), `innerText` 의 렌더링 반영, `alert/confirm/prompt` 의 Swing 대화상자.
- 시간에 민감한 시험은 컴퓨터가 바쁘면 간헐적으로 실패한다(`js.host.web.XhrTest.testTimeout` 이 전체 실행 중 한 번 실패하고 단독 실행에서 통과함).
- 제품 JAR 시험(`JarContentTest`)은 이 작업에서 JAR 을 만들지 않아 건너뛰었다.
