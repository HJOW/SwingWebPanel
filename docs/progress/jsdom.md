# JSDOM-A 진행 기록 (P15 JavaScript ↔ DOM 연결)

작업자: JSDOM-A. 소유 경로: `src/org/duckdns/hjow/swingwebpanel/js/dom/**`(DOM 바인딩), `src/org/duckdns/hjow/swingwebpanel/js/host/**`(window, 타이머, 이벤트 루프, 스크립트 실행기), 같은 이름의 `tests/**`, `tests/resources/org/duckdns/hjow/swingwebpanel/js/dom/**`, 이 파일.
빌드/시험: `scripts/pkgtest.sh jsdom "js css html dom core net storage content media @root" "js/dom js/host"`.

## 설계 요약

- `DomBridge`(페이지마다 하나): realm + 인터페이스 표 + 래퍼 캐시. `defineInterface(name, parent, javaClass, specs[])` 가 프로토타입/인터페이스 객체를 만들고 전역에 둔다.
  명세 문자열(`a:`=속성, `m:`=연산, `c:`=상수, `r:`=콘텐츠 속성 반영)은 정의할 때 Java 메서드를 리플렉션으로 한 번 찾아 연결한다 → 잘못된 명세는 시작할 때(시험에서) 실패한다.
  스크립트에는 명세에 나열된 메서드만 보이며 Java 클래스/패키지/리플렉션 객체는 전혀 노출되지 않는다.
- `HostObject`: Java 대상 하나를 가진 JS 객체. 래퍼 캐시는 약한 참조(같은 Java 객체 → 같은 JS 객체), 스크립트가 자체 프로퍼티를 저장하면 `pin` 되어 대상과 같은 기간 유지된다. `IndexedObject`: NodeList/HTMLCollection/NamedNodeMap/DOMTokenList(정수/이름 접근, 읽기 전용).
- `DomInterfaces.install(bridge)`: EventTarget, Node, Document/HTMLDocument, DocumentFragment, DocumentType, Element/SVGElement, Attr, CharacterData/Text/CDATASection/Comment/ProcessingInstruction, DOMImplementation, NodeList, HTMLCollection, NamedNodeMap, DOMTokenList, DOMException(생성자/상수),
  Event/CustomEvent(생성자), 흔한 HTML 요소 인터페이스 40여 개(`r:` 반영 속성). `DocumentExtras`: body/head/title/forms/images/links/scripts/embeds/children/all/activeElement, `Element.baseURI`.
- 이벤트: `JsListener`(함수 또는 `handleEvent` 객체, 콜백 동일성으로 equals → removeEventListener), `HandlerListener`+`EventHandlers`(`onclick` 등 IDL 핸들러: 첫 대입 때 한 번 등록해 위치 유지, `false` 반환 시 취소, 콘텐츠 속성용 지연 컴파일러 지원).
  리스너 오류는 `DomBridge.reportError` → `ErrorSink`(호스트가 window.onerror/콘솔로 전달). `ScriptAbortedException` 은 삼키지 않는다.

## 작업 로그 (최신이 위)

### P15-01/02/03(일부) · DOM 바인딩 기반과 핵심 인터페이스

- 구현: 위 설계. 시험 `DomBindingTest`(8건: 동일성/프로토타입, 트리 API, 속성·반영·classList·URL 해석, 선택자·컬렉션·innerHTML, 이벤트(캡처/버블 순서, once, handleEvent, on* 핸들러, CustomEvent), 리스너 오류 격리, DOMException/TypeError, Java 비노출).
- 검증: `pkgtest jsdom ...` → 15 passed / 0 failed(기반 시설 7건 포함), sourcecheck 0 위반.
- 알려진 한계: `value`/`checked` 는 현재 콘텐츠 속성 반영(상태 분리는 P11-05), `document.links` 는 `a` 태그 전체(href 필터 없음), `getRootNode` 옵션 없음.
- 다음: style/dataset/innerText/click/getComputedStyle(P15-03 나머지), Window/location/navigator/console, 스크립트 실행/이벤트 루프(P15-04~07, 11), 쿠키/Storage(P15-09, P16-01/08).

### P15-03~09, P15-11, P16-01/08 · 페이지 스크립트 실행기, 이벤트 루프, 쿠키/저장소 (JSDOM-A 최종)

- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/host/{PageScripts, WindowGlobals, ElementExtras, Timers, MutationObservers, HostEnvironment, ScriptSource, Scheduler, ThreadScheduler, Window}.java`,
  `src/org/duckdns/hjow/swingwebpanel/js/dom/{DocumentExtras, StyleObject, DatasetObject, StorageObject, JsListener, HandlerListener, EventHandlers}.java`(+`DomInterfaces.installStorage`, `DomBridge.alias/setFactory/ErrorSink`),
  `tests/.../js/host/{PageFixture, PageScriptsTest, StorageAndCookieTest, ElementExtrasTest}.java`, `tests/.../js/dom/{DomTestEnv, DomBindingTest}.java`.
  소유 경로 밖의 작은 추가 변경 1건: `src/org/duckdns/hjow/swingwebpanel/dom/DocumentFragment.java` 에 `querySelector/querySelectorAll`(별도 커밋 `07a6cfb`).
- 검증 (JDK 6u45): `scripts/pkgtest.sh jsdom "js css html dom core net storage content media @root" "js/dom js/host"` → 42 passed / 0 failed(내 시험 35건 + 기반 시설 7).
  회귀: `pkgtest jsfull "js css html dom core net storage content media @root" "js dom html css core"` → **883 passed / 0 failed**. sourcecheck 0 위반(61 파일).
  시험 범위: 래퍼 동일성/프로토타입/Java 비노출(8), 부분 문서를 보는 인라인 스크립트, document.write(해석 중, 개방 후), 일반/defer/async/module 순서와 DOMContentLoaded/load/readystatechange, 없는 스크립트의 error 이벤트,
  핸들러 속성(범위 사슬 element→form→document→global, this/event, return false, 속성 변경/제거, 삽입된 마크업), 클릭 카운터 페이지(const/화살표 함수), 타이머/rAF/마이크로태스크 순서와 취소/중첩/오류 격리,
  MutationObserver(배치 전달, 필터, oldValue, takeRecords/disconnect), window.onerror/error 리스너/콘솔, 무한 루프 중단(시간 제한)과 이후 타이머 계속, dispose 후 아무것도 실행되지 않음, 스크립트 스위치 끔/켬,
  location/history/navigator/screen/performance/atob/btoa, WebGL/WebGPU 객체 부재, 쿠키(HttpOnly 숨김, 경로 범위, 스크립트 쿠키), Storage 이름 있는 프로퍼티/열거/삭제/QuotaExceededError/불투명 오리진 SecurityError/classpath 오리진 공유/
  storage 이벤트(쓴 문서 제외, 값 같으면 무통지, clear 통지, 다른 오리진 제외, dispose 후 해제), style/CSSStyleDeclaration/dataset/getComputedStyle(실제 CssEngine)/template.content/canvas getContext(null).

#### 컴포넌트(core)에 연결하는 방법 (조정자 작업)

1. `core.ParsingPageLoader.parseHtml` 대신 페이지마다: `HostEnvironment env = new ...`(아래), `PageScripts scripts = new PageScripts(doc, env)`(문서에 selector 엔진/마크업 지원을 설치한 뒤, **해석 전에**),
   `HtmlParser p = new HtmlParser(doc, text); p.setScripting(env.isScriptingEnabled()); p.setListener(<스타일/이미지 리스너와 scripts.createParserListener() 를 합친 리스너>); scripts.parsingStarted(); p.run(); scripts.completeParsing();`
   (모두 로더 스레드에서. 외부 스크립트는 `ScriptSource.fetch` 가 그 스레드를 막으므로 취소 토큰을 따르게 구현한다.) 스냅샷에는 `scripts` 를 보관하는 `HtmlPageDocument` 를 붙인다.
2. `HtmlPageDocument.dispose()`에서 **`scripts.requestDisposal()`을 즉시 호출**하고, 전체 `scripts.dispose()`는 정리 작업자에서 호출한다(P15-08-b-1 추가). dispose는 페이지 잠금을 얻으므로 EDT에서 직접 호출하지 않는다. 페이지 교체/clean/dispose와 오래된 로딩 폐기를 모두 이 경로에 연결한다.
3. `HostEnvironment` 구현: `getUserAgent`(이동 시 복사한 UA), `getScriptSource`(ResourceFetcher, RequestPurpose.SCRIPT, 쿠키/헤더/SSL 훅/스킴 제한), `getScheduler`(페이지별 `core.DocumentScheduler`, 확정 후 activate/교체 시 close),
   `getCookies`/`getLocalStorage`/`getSessionStorage`(BrowserSession 의 것), `getViewportWidth/Height`(PageView의 뷰포트 범위), `isScriptingEnabled`(이동 시 초기값과 BrowserSession의 실시간 값에 더해 **한 번 중단한 페이지는 다시 켜지지 않는 상태** 필요. false→true만 읽으면 아직 발화하지 않은 기존 타이머가 다시 실행될 수 있다),
   `navigate`(링크와 같은 공통 이동 제어기로: 스킴 정책/다운로드 판정 적용, 페이지가 직접 이동하지 않음), `reload`, `historyGo`, `documentChanged`(재배치/다시 그리기 예약, 자주 불리므로 합쳐서), `getStyleResolver`(페이지의 `CssEngine.getResolver()`), `getElementBounds`(레이아웃 연결 뒤), `getReferrer`.
4. 호스트 이벤트(클릭 등)는 `scripts.dispatchEvent(target, event)` 로 전달한다(페이지 작업으로 직렬화되고 마이크로태스크 검사점이 따른다). 임의 스레드에서 호출해도 된다.
5. core에서는 기존 ThreadScheduler 대신 **`core.DocumentScheduler`를 사용한다(P15-08-a)**. 파싱 전 생성, 페이지 확정 후 문서 큐에서 activate(snapshot), 교체/clean/dispose에서 close한다. 타이머는 확정 전 보류되고 확정 뒤 렌더링/입력과 같은 문서 큐에서 실행된다. 공용 시계는 페이지를 약하게만 보관한다.
   스크립트 실행 제한은 HostEnvironment.getTaskTimeLimitMillis()(기본 10초)이며 requestDisposal이 limits.cancel을 즉시 호출한다. 전체 해제의 정리 작업자 연결은 아직 P15-08-b에 남는다.

#### P15-08-b 후속 구현 시 주의(2026-11-16)

- 현재 ParsingPageLoader는 DOM만 붙인다. HTML 파싱 전에 PageScripts를 생성하고 parser listener를 합쳐야 한다. 파싱 뒤 한꺼번에 script를 실행하면 부분 문서/document.write 계약이 깨진다.
- PageRequestContext는 UA/헤더/force만 복사하여 NavigationRequest/restore/ProgressSink를 붙잡지 않는다. 런타임도 같은 원칙을 유지한다.
- 로더 스레드에서 파싱하는 동안 타이머가 별도 스레드에서 DOM을 바꾸지 않도록 DocumentScheduler.activate를 확정 후로 미룬다. PageView의 CSS/렌더링 엔진과 HostEnvironment의 스타일/좌표 훅도 같은 문서 큐 소유권으로 연결한다.
- setJavascriptEnabled(false)는 현재 확정 페이지뿐 아니라 파싱/실행 중 페이지도 즉시 중단해야 한다. 새로 true가 되어도 기존 페이지는 재시작하지 않는다. 단순히 실시간 boolean만 반환하는 것으로 완료 처리하지 않는다.
- PageInput의 host click은 JS 마이크로태스크 검사점을 거쳐야 하며, 사용자 에이전트 이벤트의 trusted 플래그도 유지한다. 현재 PageScripts.dispatchEvent는 일반 dispatchEvent이므로 trusted 호스트 경로를 추가할 필요가 있다.
- 외부 스크립트는 ResourceFetcher/쿠키/UA/헤더/force/SSL/스키마 정책과 페이지 취소 토큰을 사용한다. 현재 async 실행은 파싱 후 순서 실행이라는 알려진 차이를 유지 추적하고 완료 판정을 확장하지 않는다.
- location.replace는 이력 교체가 필요하다. 단순 setPage로 연결하면 이력이 추가되므로 P15-08-c가 끝난 것이 아니다. 동일한 원칙으로 download/폼도 공통 이동 계약에 연결한다.

#### 체크박스 후보
- P15-01, P15-02, P15-03, P15-04, P15-05, P15-06, P15-07, P15-09, P15-11, P16-01, P16-08 (모두 스크립트 수준 시험으로 검증).
- P15-08(이동 정책 연결)과 P15-10(core 의 setJavascriptEnabled 연결)은 훅만 제공했으므로 core 연결 뒤 체크. P15-12(통합 페이지)는 `PageScriptsTest.testClickCounterPage` 가 PageScripts 수준에서 통과하며 컴포넌트 연결 후 최종 확인. P16-10 은 P16-01/08 이 끝났으므로 조정자가 저장소 시험(LocalStorageManagerTest/StorageIntegrationTest) 결과와 함께 체크 가능.

#### 알려진 한계 / 후속
- XMLHttpRequest, fetch, Worker, WebSocket, Range/Selection, DOMParser, Canvas 2D, `Image()/Option()/Audio()` 생성자는 P20. `history.pushState/replaceState` 는 상태 값만 보관(주소 변경 없음, P20-07). MouseEvent/KeyboardEvent/UIEvent 등 입력 이벤트 인터페이스와 포커스는 P11.
- `HTMLInputElement.value/checked` 등은 콘텐츠 속성 반영(상태 분리는 P11-05), `HTMLFormElement.elements/submit`, `HTMLSelectElement.options` 없음. `document.links` 는 모든 `a`(href 필터 없음). `innerText` 는 `textContent` 근사.
- MutationObserver 의 `addedNodes/removedNodes` 는 배열(표준은 NodeList), 임시 등록(transient registration) 없음. `Element.cloneNode` 가 template 내용을 복제하지 않는 문제(HTML 작업자 요청 2번)는 dom 쪽에 남아 있다.
- `window.top/parent/frames` 는 전역 자신(프레임은 P20-02). 위치 `search/pathname/host` 등의 setter 는 없다(href/hash/assign/replace 만 이동).
- 외부 스크립트의 async 는 파싱 종료 뒤에 순서대로 실행(네트워크 완료 순서 모델이 아님). 모듈은 import map 미지원, 모듈 그래프 가져오기는 `ScriptSource.fetch` 동기 호출.
