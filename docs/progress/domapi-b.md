# DOMAPI-B 진행 기록 (구조화 복제, history.pushState, Range/Selection, DOMParser, MessageChannel)

작업자: DOMAPI-B (격리 worktree 브랜치). 항목은 맨 위가 최신이다.

## 2026-11-17 / 20261117-305 P20-09(일부) MessageChannel / MessagePort / window.postMessage / structuredClone

- 현재 작업 번호: P20-09 의 메시징 부분(규격: W3C Web Messaging REC 2015-05-19 — https://www.w3.org/TR/2015/REC-webmessaging-20150519/ 를 `tools/webfetch` 로 2026-11-17 조회,
  Last-Modified 2017-10-02; `structuredClone()` 은 HTML Standard(WHATWG) 확장). HTML5 2014 REC 에는 이 장이 없다(comms.html 404 확인). **Worker 는 시작하지 않았다.**
- 완료한 내용
  - `js/host/MessagingApi`: `window.postMessage(message, targetOrigin[, transfer])`(대상 오리진 `*` `/` 절대 URL, 그 밖은 SyntaxError, 다른 오리진은 조용히 버림, 구조화 복제 +
    ArrayBuffer 전달, 비동기 전달), `MessageChannel`, `MessagePort`(`postMessage`, `start`, `close`, `onmessage` 최초 지정이 start, `message` 이벤트, 시작 전 큐 최대 10000개,
    닫힌 포트의 큐/송신 폐기, 닫기 전에 반대쪽으로 보낸 메시지는 전달), `MessageEvent`(`dom/MessageEvent`, 생성자와 초기화 사전: data/origin/lastEventId/source/ports),
    `structuredClone(value[, {transfer}])`. 전달은 컴포넌트 스케줄러에 0ms 작업으로 올려 보내는 쪽이 실행되는 동안에는 절대 전달되지 않는다(스케줄러가 없으면 타이머처럼 전달 안 됨).
  - 전달 목록은 ArrayBuffer 만 허용(중복/이미 분리됨/원본 포트/그 밖의 객체는 DataCloneError). **MessagePort 의 전달(transfer)은 아직 지원하지 않는다**(DataCloneError).
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/host/MessagingApi.java`, `src/org/duckdns/hjow/swingwebpanel/dom/MessageEvent.java`(신규), `js/host/WindowGlobals.java`, `js/dom/DomInterfaces.java`(각 한 줄/분기),
  `tests/src/org/duckdns/hjow/swingwebpanel/js/host/MessagingApiTest.java`(7).
- 검증 결과(JDK 6u45): `--filter org.duckdns.hjow.swingwebpanel.js.host.MessagingApiTest` 7건 통과. `tools/sourcecheck` 0 위반.
- 미해결/한계: 브라우징 문맥이 하나뿐이라 window 대상은 항상 자기 window 이다(iframe/Worker 가 생기면 대상과 포트 전달이 필요: P20-02, P20-09 Worker). 포트 전달,
  `initMessageEvent`, `MessagePort` 의 GC 규칙(페이지 폐기 시 정리는 스케줄러 닫기에 의존)은 구현하지 않았다. 구조화 복제의 `Blob`/`File`/`ImageData` 등 호스트 객체는 DataCloneError.
- 체크박스 후보: 없음(P20-09 는 Worker 와 포트 전달이 남음).
- 조정자 요청: FEATURE_MATRIX 의 postMessage/MessageChannel/structuredClone 행을 "부분 구현(window 와 채널 메시징, ArrayBuffer 전달 검증 완료; MessagePort 전달과 Worker 미구현)" 으로.

## 2026-11-17 / 20261117-304 P20-04 DOMParser / XMLSerializer

- 현재 작업 번호: P20-04 (규격: HTML Standard "DOMParser"(parseFromString 의 형식 처리), W3C DOM Parsing and Serialization "XML Serialization" 5.2.1 — 알고리즘 본문을
  `tools/webfetch` 로 https://w3c.github.io/DOM-Parsing/ 에서 2026-11-17 조회(Last-Modified 2026-09-02). DOMParser/XMLSerializer 정의는 HTML Standard 로 이동했으며
  XML 직렬화 알고리즘 본문은 DOM Parsing 문서에 남아 있다).
- 완료한 내용
  - `dom/XmlSerializer`: XML 직렬화 알고리즘 전체(require well-formed 끔): 네임스페이스 접두사 맵 복사/기록/선호 접두사/접두사 생성(ns1…), 기본 네임스페이스 선언/해제,
    xml 접두사, 속성의 네임스페이스 선언 생성, HTML 요소(자식 없는 void 요소는 `<br />`, 나머지는 시작·끝 태그), 그 밖의 네임스페이스 빈 요소는 `<a/>`, 텍스트 `& < >`,
    속성값 `& " < > \t \n \r` 이스케이프(현행 브라우저와 같게 `>` 포함), 주석/CDATA/처리 명령/문서 유형(PUBLIC/SYSTEM, 따옴표 선택)/조각/문서, template 내용.
  - `js/host/DomParsing`: `new DOMParser().parseFromString(text, type)` — `text/html`(HTML 파서, 스크립트 비활성이라 script 미실행 + noscript 는 마크업, 문서 URL은 페이지 URL),
    `text/xml` `application/xml` `application/xhtml+xml` `image/svg+xml`(컴포넌트의 XML 파서 `XhtmlParser`, 외부 엔티티/DTD 차단, 콘텐츠 타입은 지정한 type),
    well-formed 가 아니면 `parsererror`(네임스페이스 `http://www.mozilla.org/newlayout/xml/parsererror.xml`)만 가진 문서, 그 밖의 type/인수 누락/new 없는 호출은 TypeError.
    `new XMLSerializer().serializeToString(node)`. 페이지 문서와 `getContent()` 원문은 건드리지 않는다.
  - `Document` 의 JS 멤버에 `createCDATASection`, `createProcessingInstruction` 추가(Java 메서드는 이미 있었고 스크립트에 노출되지 않았다).
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/dom/XmlSerializer.java`, `src/org/duckdns/hjow/swingwebpanel/js/host/DomParsing.java`(신규), `js/host/WindowGlobals.java`(한 줄), `js/dom/DomInterfaces.java`(한 줄),
  `tests/src/org/duckdns/hjow/swingwebpanel/dom/XmlSerializerTest.java`(7), `tests/src/org/duckdns/hjow/swingwebpanel/js/host/DomParsingTest.java`(5).
- 검증 결과(JDK 6u45): `--filter org.duckdns.hjow.swingwebpanel.dom.XmlSerializerTest` 7건, `--filter org.duckdns.hjow.swingwebpanel.js.host.DomParsingTest` 5건 통과. `tools/sourcecheck` 0 위반.
- 미해결/한계: `require well-formed` 모드와 `XMLSerializer` 의 InvalidStateError(직렬화 불가 노드)는 구현하지 않았다(XMLSerializer 는 규격대로 flag 를 끄고 쓰므로 영향 없음).
  `Attr` 노드는 이 DOM 에서 `Node` 가 아니라 직렬화하지 않는다. 텍스트/XML 문서의 `innerHTML` 파서는 HTML 규칙을 쓴다.
- 체크박스 후보: P20-04("JavaScript 에서 쓰는 기능, getContent 와 연결 안 함" — DOMParser/XMLSerializer 와 기존 innerHTML/outerHTML/insertAdjacentHTML 직렬화가 모두 있고 검증됨.
  단 P20-04 의 "HTML 조각과 직렬화 기능"은 기존 innerHTML 계열이 검증된 것으로 간주) — 조정자가 기존 innerHTML 시험 근거를 확인해 결정.
- 조정자 요청: FEATURE_MATRIX 에 DOMParser/XMLSerializer 행을 "검증 완료(DOM Parsing XML Serialization 알고리즘, `XmlSerializerTest` `DomParsingTest`)" 로.

## 2026-11-17 / 20261117-303 P20-03 Range / Selection / NodeIterator / TreeWalker

- 현재 작업 번호: P20-03 (규격: DOM Standard "Interface Range", "Traversal"(NodeIterator, TreeWalker, NodeFilter), W3C Selection API, DOM Parsing 의
  `createContextualFragment`; https://dom.spec.whatwg.org/ , https://www.w3.org/TR/selection-api/ — 알고리즘은 규격 본문을 기준으로 구현, 조회일 2026-11-17).
- 완료한 내용
  - `dom/Range`: 경계점(setStart/End/Before/After, collapse, selectNode(Contents)), compareBoundaryPoints(4방식)/comparePoint/isPointInRange/intersectsNode,
    cloneContents/extractContents/deleteContents(표준 알고리즘 그대로: 부분 포함 자식 재귀, contained 노드, 새 경계점), insertNode(텍스트 쪼개기, 조각 삽입, 접힌 범위 확장),
    surroundContents, toString, createContextualFragment(DOM Parsing: 문맥 요소, html 루트면 body), cloneRange, detach(no-op).
  - **live range**: `dom/LiveRanges`(문서마다 약한 참조 등록부)가 `Document.recordChildList`(모든 자식 목록 변경이 지나는 길)와 `CharacterData.replaceData`, `Text.splitText` 의
    통지를 받아 범위 경계를 규격의 "insert/remove/replace data/split" 규칙대로 조정(제거된 노드 안의 경계 → 제거된 자리, 뒤의 경계는 당김, 삽입 자리 뒤는 밂, 텍스트 쪼개기).
  - `dom/Selection`(문서마다 하나, `Document.getSelection()`): anchor/focus/방향, rangeCount 0|1, getRangeAt, addRange/removeRange/removeAllRanges, collapse/collapseToStart/End,
    extend, setBaseAndExtent, selectAllChildren, deleteFromDocument, containsNode(포함 비교는 경계 포함), toString, type, direction. 모델만이며 화면 선택과 클립보드는 P11-08.
  - `dom/NodeIterator`(기준 노드/포인터, 제거 시 "pre-removing steps" 를 제거 뒤 형제 정보로 적용, 루트를 품은 노드의 제거는 건드리지 않음 — 표준과 다른 제한),
    `dom/TreeWalker`(parent/first/last/sibling/previous/nextNode, currentNode 설정), `dom/NodeFilter`(상수와 콜백), `dom/Traversal`(필터 재진입 InvalidStateError).
  - JS 연결: `js/dom/RangeBindings`(Range+생성자, Selection, NodeIterator, TreeWalker, NodeFilter 상수(SHOW_ALL=4294967295), `document.createRange/createNodeIterator/createTreeWalker/
    getSelection`, `window.getSelection`), `js/dom/JsNodeFilter`(함수 또는 acceptNode 객체, WebIDL 콜백 규칙), `js/host/RangeGeometry`(`Range.getBoundingClientRect/getClientRects`:
    **레이아웃이 요소 단위이므로 근사** — 완전히 덮인 요소는 자기 상자, 닿는 텍스트는 그 텍스트를 가진 요소의 상자, 접힌 범위는 사각형 없음).
  - `MemberInstaller` 에 매개변수 타입 `Range` 추가.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/dom/{Range,Selection,NodeFilter,Traversal,NodeIterator,TreeWalker,LiveRanges}.java`(신규), `dom/{Document,CharacterData,Text}.java`(추가만),
  `js/dom/{RangeBindings,JsNodeFilter}.java`(신규), `js/dom/{MemberInstaller,DomInterfaces}.java`(한 줄씩), `js/host/RangeGeometry.java`(신규), `js/host/WindowGlobals.java`(한 줄),
  `tests/src/org/duckdns/hjow/swingwebpanel/dom/{RangeTest(11),SelectionTest(6),TraversalTest(7)}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/js/host/RangeApiTest.java(7)`(합계 31건; 커밋 메시지의 "27건" 은 착수 시점의 집계).
- 검증 결과(JDK 6u45): 증분 컴파일 후 `--filter swingwebpanel`(컴파일한 패키지 `js dom core @root` 의 시험 전체) **1106건 통과, 0 실패, 1 건너뜀**(JAR 시험: 제품 JAR 미빌드).
  `tools/sourcecheck` 0 위반.
- 미해결/한계: 텍스트 노드의 `Node.normalize()` 는 경계점을 옮기지 않는다. `insertNode` 가 계층 오류로 실패하기 전에 시작 텍스트 노드가 이미 쪼개져 있을 수 있다.
  범위의 경계가 다른 문서의 트리에 있으면 그 문서의 변경은 따라가지 않는다(등록부는 만든 문서 기준). 범위 사각형은 요소 단위 근사. 다중 범위 선택(Firefox 식)은 지원하지 않는다.
- 체크박스 후보: P20-03 은 "화면 좌표와 텍스트 추출" 중 텍스트 추출은 검증했으나 화면 좌표가 요소 단위 근사이므로 **체크하지 않는다**(상위 항목). 하위로 "모델/API(live range, Range
  연산, Selection, 탐색)" 만 검증 완료.
- 조정자 요청: FEATURE_MATRIX 에 Range/Selection/NodeIterator/TreeWalker/NodeFilter 행을 "부분 구현(모델과 API 검증 완료, 화면 좌표는 요소 단위 근사, 화면 선택/클립보드는 P11-08)"으로.

## 2026-11-17 / 20261117-302 P20-07 history.pushState / replaceState / popstate

- 현재 작업 번호: P20-07 (규격: HTML5 2014 REC 5.5.2 History 인터페이스, 5.6.10 History traversal, 5.6.10.1 popstate/hashchange 이벤트;
  https://www.w3.org/TR/2014/REC-html5-20141028/browsers.html#history, `tools/webfetch` 로 2026-11-17 에 조회, Last-Modified 2014-10-24).
- 완료한 내용
  - `js/host/HistoryApi` (WindowGlobals 에서 분리): `length`(환경이 알려 주는 세션 이력 항목 수), `state`(항목의 직렬화 상태를 처음 읽을 때 역직렬화해 캐시, 같은 객체 반환),
    `go/back/forward`, `pushState/replaceState`(인수 2개 필수, data 구조화 복제 → DataCloneError, url 은 문서 기준 URL 로 해석하고 스킴/사용자 정보/호스트/포트가
    다르거나 해석 실패면 SecurityError(코드 18), 성공하면 `document.URL`/`location.href` 와 `history.state` 를 곧바로 바꾸고 `HostEnvironment.updateHistoryState`),
    `scrollRestoration`("auto"/"manual", 잘못된 값 무시). 상태 한도 8 MiB.
  - `core/PageSnapshot`: 이력 항목에 `ClonedValue` 상태(`getHistoryState`), 폼 제출(`post`)이 제자리 변경 가능. `changeLocation(url,state,post)` 가 `changeFragment` 를 대체.
  - `core/BrowserSession.updateHistoryFromPage`: pushState 는 지금 항목을 문서 없는 복사본으로 남기고 앞쪽 항목을 지우며 새 항목을 추가, replaceState 는 현재 항목 교체.
    둘 다 **요청 없음**이며 새 항목은 일반 GET 항목(폼 POST 는 잊는다, 규격 5.5.2). 같은 문서 이동(`startTraversalLocked`)은 문서 식별자만 같으면 URL 이 달라도
    다시 읽지 않고 `commitFragment` 로 처리하며 항목의 URL/상태/폼 제출을 페이지에 적용. 프래그먼트 이동이 만드는 새 항목은 상태 null.
  - `HtmlPageDocument.locationChanged` → `PageScripts.historyTraversed`: `history.state` 갱신 후 `popstate`(PopStateEvent, 버블, state) 다음 `hashchange`(프래그먼트가 다르면).
    규격(5.6.10)대로 프래그먼트 이동도 popstate(state null)를 보낸다(이전에는 hashchange 만 보냈다).
  - 다른 문서의 항목으로 가면 읽어 오되(이력 이동/새로고침) 항목의 상태가 새 문서의 첫 `history.state` 가 된다(파싱 중 스크립트도 본다). `PageRuntime` 생성자에서 시드.
  - 파싱 중(확정 전) pushState 는 확정 때까지 기다렸다 요청 순서대로 적용(프래그먼트 이동과 같은 목록). `history.length` 는 확정 전에는 항목 수를 미리 계산.
  - `history.go/back/forward` 의 이동 위치는 문서 큐에서 계산(앞서 요청한 location.hash 이동이 이력에 먼저 반영된 뒤) — 지연 환경에서 이동이 어긋나는 문제를 막음.
  - 새 이벤트 인터페이스 `PopStateEvent`/`HashChangeEvent`(`dom/`, `js/dom/HistoryBindings`) 생성자와 초기화 사전 포함.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/host/{HistoryApi(신규),HostEnvironment,PageScripts,WindowGlobals}.java`, `src/org/duckdns/hjow/swingwebpanel/js/dom/{HistoryBindings(신규),DomInterfaces}.java`,
  `src/org/duckdns/hjow/swingwebpanel/dom/{PopStateEvent,HashChangeEvent}.java`(신규), `src/org/duckdns/hjow/swingwebpanel/core/{PageSnapshot,BrowserSession,HtmlPageDocument,PageRuntime}.java`,
  `tests/src/org/duckdns/hjow/swingwebpanel/js/host/HistoryApiTest.java`(9건), `tests/src/org/duckdns/hjow/swingwebpanel/core/HistoryStateIntegrationTest.java`(3건).
- 검증 결과(JDK 6u45, 증분 컴파일): `--filter org.duckdns.hjow.swingwebpanel.core` 161건 통과, `--filter org.duckdns.hjow.swingwebpanel.js.host` 138건 통과(0 실패), `tools/sourcecheck` 0 위반.
  통합 시험: SPA 라우터(push/replace/back/forward/go 의 popstate 상태, 서버 요청 0회, 새로고침이 푸시한 주소를 요청하고 상태 유지, 다른 문서의 항목은 상태와 함께 로드),
  프래그먼트 항목과 푸시 항목의 popstate→hashchange 순서, POST 폼 뒤 replaceState 한 항목의 새로고침은 GET(재전송 질문 없음).
- 미해결/한계: `history.length` 는 같은 문서 프래그먼트 이동의 항목이 문서 큐에서 확정될 때까지(비동기) 늘어나지 않는다. pushState 개수 한도는 두지 않았다(상태 8 MiB 한도만).
  about:blank 처럼 문서가 없는 이력 항목에서의 pushState 는 JS 쪽 상태만 바꾼다. popstate 는 문서 로드 시에는 보내지 않는다(현행 브라우저와 같음).
  `:target` 상태는 프래그먼트가 없는 항목으로 돌아갈 때 지우지 않는다(기존 한계).
- 체크박스 후보: P20-07(구현과 검증 완료).
- 조정자 요청: FEATURE_MATRIX 의 history/popstate 행(있다면) 을 "검증 완료(HTML5 2014 REC 5.5.2, 5.6.10; 통합 시험 `HistoryStateIntegrationTest`)" 로, HostEnvironment 에
  `updateHistoryState`/`getHistoryLength` 가 늘었음을 ARCHITECTURE 에 반영. API_CONTRACT 5.5~5.8 에 "pushState 항목은 이력에 URL/상태로 보관되고 `getHistories()` 에 URL 로 보인다".

## 2026-11-17 / 20261117-301 구조화 복제(structured clone) 엔진

- 현재 작업 번호: P20-09 의 선행 기반(HTML 표준 "Safe passing of structured data"). 계획의 P20-07/P20-09 가 이 기반을 쓴다.
- 완료한 내용: `org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone` — JS 값을 realm 에 독립적인 불변 Java 트리(`ClonedValue`)로 직렬화하고 다른 realm 에 역직렬화한다.
  원시 값(-0, NaN, 외톨이 서로게이트 포함), Boolean/Number/String 래퍼, Date(잘못된 날짜 포함), RegExp(lastIndex 0), Array(구멍/length/추가 프로퍼티),
  일반 객체(자체 열거 가능 문자열 키, getter 는 값으로 읽고 getter 가 지운 키는 건너뜀, `__proto__` 키는 자체 프로퍼티), Map, Set, ArrayBuffer(공유 유지),
  형식화 배열 9종, DataView, Error 계열(name/message/stack), 순환/공유 참조를 처리한다. 함수/심볼/Proxy/WeakMap/Promise/arguments/반복자/DOM 같은
  호스트 객체는 `StructuredCloneException`(DataCloneError). 깊이(10000)/객체 수(4,000,000)/바이트(기본 64 MiB) 한도, 반복 탐색(Java 스택 비의존).
  전달 목록(ArrayBuffer transfer): 바이트를 옮기고 원본을 분리하며 잘못된 목록(분리됨/중복/비 ArrayBuffer)은 아무것도 분리하지 않고 거부한다.
  `js.builtins.datajson.DataAccess` — Date/RegExp/Map/Set 내부 슬롯 공개 창구(페이지가 바꿀 수 있는 메서드를 호출하지 않기 위함).
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/host/structured/{StructuredClone,ClonedValue,StructuredCloneException}.java`, `src/org/duckdns/hjow/swingwebpanel/js/builtins/datajson/DataAccess.java`(신규),
  `tests/src/org/duckdns/hjow/swingwebpanel/js/host/structured/StructuredCloneTest.java`.
- 검증 결과: JDK 6u45 로 `scripts/pkgtest.sh domapib "js dom html css net storage core content layout render download media @root" "js dom core @root"` 로 전체 컴파일 후
  증분 컴파일로 `--filter org.duckdns.hjow.swingwebpanel.js.host.structured` 16건 통과(0 실패). `tools/sourcecheck` 0 위반.
- 미해결 사항: 없음(이 층). 한계: 일반 객체 판정을 "클래스가 정확히 JsObject" 로 하므로 다른 호스트 객체(DOM 래퍼, Storage 등)는 모두 DataCloneError 이다.
  Error 의 stack 은 비표준이며 이 엔진의 기본 오류 객체에는 없을 수 있다(있으면 복사).
- 다음 작업 번호: P20-07(history.pushState), P20-03, P20-04, P20-09(MessageChannel/postMessage).
