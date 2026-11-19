# WEBAPI2 진행 기록 (P20-13 웹 플랫폼 나머지 기능)

작업자: WEBAPI2. 근거 항목: `AGENTS.md` P20-13(기능 표 11~13절이 미구현으로 남긴 기능), P11-01(이벤트 표). 커밋 번호 대역 `20261117-620~`.
빌드/시험: `scripts/pkgtest.sh <이름> "js dom html css net storage core content layout render download media @root" "<시험 패키지>" --filter <접두사>`(JDK 6u45).
규격 조회는 `tools/webfetch`(JRE Zulu25, 아래 표). 직접 돌려 통과한 것만 "통과"로 적는다.

## 조정자 요청

1. **체크 후보**: P20-13 은 아직 하위 항목이 남아 있으므로 전체 체크 후보가 아니다. 하위 항목별 상태는 아래 "항목별 상태"를 본다.
2. **API_CONTRACT/ARCHITECTURE 문구 후보**
   - 계약(요청 계층): "페이지의 `EventSource` 는 `js.host.StreamingWebRequestSource`(`PageWebRequests` 가 구현)로 공통 요청 계층을 거친다. 스트림은 메모리에 읽지 않고
     동시 요청 칸을 쓰지 않으며(열린 스트림은 페이지당 6개까지) 읽기 제한 시간은 5분이다. 스트림은 자체 소켓 연결을 쓰므로(`ResourceRequest.setStreaming`) 취소가 즉시 끝난다."
   - 아키텍처: `js.host.web.EventSourceApi`/`EventStreamParser`, `js.host.StreamingWebRequestSource`, `net.ResourceRequest.{streaming, readTimeoutMs}`.
3. FEATURE_MATRIX 는 내가 구현한 기능의 행만 고쳤다(EVT-*, WEB-*, 11~13절의 해당 행).

## 규격 판본과 조회 기록 (조회일 2026-11-17, `tools/webfetch`, JRE Zulu25)

| 규격 | URL | 서버의 Last-Modified |
| --- | --- | --- |
| HTML Standard 9.2 Server-sent events | https://html.spec.whatwg.org/multipage/server-sent-events.html | Mon, 16 Nov 2026 15:32:23 GMT |
| Pointer Events Level 3 | https://www.w3.org/TR/pointerevents3/ | Fri, 26 Jun 2026 17:28:12 GMT |
| Page Visibility Level 2 | https://www.w3.org/TR/page-visibility-2/ | Mon, 20 Jun 2022 10:36:00 GMT |
| Intersection Observer | https://www.w3.org/TR/intersection-observer/ | Wed, 18 Oct 2023 15:54:57 GMT |
| Resize Observer Module Level 1 | https://www.w3.org/TR/resize-observer-1/ | Mon, 10 Feb 2020 13:28:11 GMT |
| WebVTT: The Web Video Text Tracks Format | https://www.w3.org/TR/webvtt1/ | Wed, 20 May 2026 14:48:38 GMT |
| User Timing Level 3 | https://www.w3.org/TR/user-timing/ | Wed, 11 Mar 2026 16:02:27 GMT |
| Navigation Timing Level 2 | https://www.w3.org/TR/navigation-timing-2/ | Mon, 02 Nov 2026 10:14:04 GMT |

(WHATWG Living Standard 와 W3C 편집 초안은 조회 시점의 본문이 기준이다.)

## 1. EventSource (P20-13 의 첫 항목)

### 설계

- `js.host.StreamingWebRequestSource`(새 서비스 계약): `WebRequestSource` 의 스트리밍 짝. `Listener.onHeaders(head)`(받아들일지 정함), `onData(bytes, len)`, `onEnd(end)`.
  `core.PageWebRequests` 가 두 인터페이스를 모두 구현한다(스트림은 같은 `ResourceFetcher` 와 같은 페이지 규칙: 스킴 제한, 동일 출처/CORS 검사, 자격 증명 모드, User-Agent, 추가 헤더, 쿠키, SSL 훅).
  - 스트림은 메모리에 읽지 않는다(`ResponseHandler` 가 본문을 `OutputStream` 으로 흘린다). 페이지 규칙에 어긋나거나(`futileError`) 받는 쪽이 거부한 응답(상태가 200 이 아니거나 MIME 이 `text/event-stream` 이 아님)은 본문을 읽기 전에 중단한다.
  - 열린 스트림은 동시 요청 칸(6)을 차지하지 않는다. 대신 페이지당 열린 스트림은 6개까지이며 일곱째는 곧바로 실패한다. 읽기 제한 시간은 5분(`STREAM_READ_TIMEOUT_MS`).
  - `net.ResourceRequest` 에 `readTimeoutMs`, `streaming` 을 더했다. **`streaming` 요청은 자체 소켓 연결(`net.SocketHttp*`)을 쓴다**: JRE `HttpURLConnection.disconnect()` 는 다른 스레드가 `ChunkedInputStream.read` 로 기다리는 동안 그 스트림의
    잠금에 막혀 돌아오지 않는다(침묵한 스트림에서 페이지 이동/폐기가 영원히 멈추는 것을 `EventSourceIntegrationTest` 가 드러냈다). 프록시가 적용되는 URL 은 JRE 연결로 돌아가며, 그때는 별도 스레드에서 `disconnect()` 하여 취소하는 쪽이 기다리지 않게 했다(스트림 읽기 스레드는 다음 데이터가 올 때까지 남는다).
  - `js.host.WebResponse` 에 `futileError`/`isFutile`(다시 시도해도 고쳐지지 않는 오류: 허용되지 않은 스킴, 동일 출처 모드 위반, CORS 거부, 리디렉션 모드 오류). EventSource 는 이런 오류에서 재연결하지 않고 연결을 실패시킨다.
- `js.host.web.EventStreamParser`: 줄 단위 해석기(규격 9.2.5/9.2.6; CRLF/LF/CR 와 조각 경계, UTF-8 은 줄이 완성된 뒤 디코딩, BOM 제거, `retry` 숫자만, id 의 NUL 무시, 8 MiB 한도).
- `js.host.web.EventSourceApi`: 인터페이스(`EventSource`, 상수 CONNECTING/OPEN/CLOSED, `url`, `withCredentials`, `readyState`, `close()`, `onopen/onmessage/onerror`), 연결 처리 모델(announce → open, 재연결, fail), `MessageEvent`(data, origin=스트림 최종 URL 의 오리진, lastEventId).
  worker 의 `WebApis` 도 같은 클래스를 설치하며 `js.worker.WorkerWebHost` 가 스트리밍 서비스를 그대로 넘긴다(worker 스크립트 URL 이 참조).
- 규격 대비 선택: ① 새 연결의 마지막 이벤트 ID 버퍼는 이벤트 소스의 마지막 ID 로 시작한다(Chromium 방식; 규격 본문은 빈 문자열로 시작). ② 재연결 시간은 3초에서 시작한다(Chromium). 지수 백오프는 하지 않는다.
  ③ `Last-Event-ID` 의 비ASCII 는 UTF-8 바이트를 ISO-8859-1 문자로 실어 보낸다. ④ `Last-Event-ID` 는 교차 오리진에서 사전 요청을 일으키지 않는다(브라우저와 같다). ⑤ 교차 오리진 `Origin` 은 소켓 연결이 싣는다.
- 한계: JVM 프록시가 적용되면 JRE 연결을 쓰므로 교차 오리진 `Origin` 헤더가 실리지 않는다(기존 net 한계). 스트림 읽기는 gzip/deflate 도 풀지만 서버가 압축하면 실시간성이 떨어질 수 있다.

### 검증 (JDK 6u45, 직접 실행)

- `js.host.web.EventStreamParserTest` 10건 통과(규격의 예제 모두: 여러 줄 데이터, 이벤트 종류, id 유지/지움, 빈 data, 마지막 블록 폐기, CR/LF/CRLF 와 조각 경계, BOM, retry, UTF-8 모든 위치 분할, 8 MiB 넘침).
- `js.host.web.EventSourceTest` 14건 통과(가짜 스트리밍 서비스와 수동 시계: 인터페이스, 요청 모양, open/message 순서, 거부된 답과 MIME 비교, 재연결과 `Last-Event-ID`, `retry`, 네트워크 오류와 futile 오류, `close()`, 핸들러 안의 close, 오래된 시도 무시, 넘침, 스트리밍 불가 호스트, 폐기).
- `core.EventSourceIntegrationTest` 6건 통과(실제 `SwingWebPanel` 과 로컬 HTTP 서버: 응답이 열려 있는 동안 이벤트 도착(버퍼링 없음), 재연결/`Last-Event-ID`/204 정지, 요청 계층(UA, 추가 헤더, 쿠키, Referer, no-cache), 서버가 끊은 연결의 재연결,
  404/500/잘못된 MIME/차단된 스킴 6종의 실패, CORS(허용/거부/자격 증명/와일드카드)와 `Origin`, 페이지 이동과 컴포넌트 폐기 때 연결 닫힘, 열린 스트림 6개 한도).
- `js.worker.WorkerEventSourceTest` 2건 통과(worker 의 EventSource 와 `terminate()` 때의 연결 닫힘).
- 회귀: `js.host.web` 67건, `core.WebRequestApiTest` 15건, `net` 140건, `js.worker` 47건 통과.

### 변경 파일

- 새 파일: `src/org/duckdns/hjow/swingwebpanel/js/host/StreamingWebRequestSource.java`, `js/host/web/{EventSourceApi,EventStreamParser}.java`, `tests/.../js/host/web/{EventSourceTest,EventStreamParserTest}.java`,
  `tests/.../core/EventSourceIntegrationTest.java`, `tests/.../js/worker/WorkerEventSourceTest.java`.
- 소유 경로 밖의 작은 가산 변경: `core/PageWebRequests.java`(`startStream`, 스트림 처리기), `js/host/WebResponse.java`(`futileError`), `js/host/web/WebApis.java`(설치 한 줄), `js/worker/WorkerWebHost.java`(스트리밍 감싸개),
  `net/ResourceRequest.java`(`readTimeoutMs`, `streaming`), `net/HttpFetcher.java`(읽기 제한 시간, 스트림은 소켓 연결, 비소켓 스트림의 분리 disconnect), 시험 틀 `tests/.../testing/TestHttpServer.java`(`Response.streamedBy`),
  `tests/.../js/host/web/WebFixture.java`(가짜 스트리밍 서비스).

## 2. PointerEvent (P20-13 의 두 번째 항목)

### 설계

- **`dom.PointerEvent`**(`MouseEvent` 하위): `pointerId`, `width/height`, `pressure`, `tangentialPressure`, `tiltX/tiltY/twist`, `altitudeAngle/azimuthAngle`, `pointerType`, `isPrimary`, `getCoalescedEvents()/getPredictedEvents()`(복사본).
  `PointerEvent.forMouse(...)` 가 컴포넌트의 마우스 포인터 이벤트를 만든다(ID 1, `mouse`, 주 포인터). JS 인터페이스와 `PointerEventInit` 생성자는 `js.dom.DomInterfaces`.
- **이벤트 순서와 속성**(`core.PageInput`, 문서 큐): 이동은 `pointerout/pointerleave`(안쪽부터) → `pointerover/pointerenter`(바깥쪽부터) → 마우스 경계 이벤트(`mouseout/leave/over/enter`) → `pointermove` → `mousemove`.
  누름은 `pointerdown` → `mousedown`, 뗌은 `pointerup` → `mouseup`(→ `contextmenu`). 압력은 버튼이 눌려 있으면 0.5(`pointerup` 은 0). `button` 은 상태가 바뀐 버튼이고 바뀐 것이 없으면 -1.
  첫 버튼만 `pointerdown`, 이어서 누르거나 떼는 버튼은 `button` 을 가진 `pointermove`(동시 버튼; 호환 `mousedown/mouseup` 은 그대로 나감), 마지막 버튼을 떼면 `pointerup`.
  `bubbles/cancelable` 은 규격 표(4.1.3.1): enter/leave 는 둘 다 false, `pointercancel/gotpointercapture/lostpointercapture` 는 취소 불가.
  `click/auxclick/contextmenu` 는 `PointerEvent`(`pointerId=1`, `mouse`, 압력 0)이다.
- **호환 마우스 이벤트 억제**(규격 11.2의 PREVENT MOUSE EVENT 표시): 취소된 `pointerdown` 은 버튼을 모두 뗄 때까지 `mousedown/mousemove/mouseup` 을 막는다. `mouseover/out/enter/leave` 와 `click` 은 막지 않는다.
  `mousedown` 의 기본 동작(포커스 이동, 텍스트 선택 시작)은 `mousedown` 이 억제되어도 그대로 한다(Chromium 과 같다).
- **포인터 캡처**(규격 9절, `core.PagePointers` = `js.host.PointerService`): 대기 중 캡처(`setPointerCapture` 가 바꾸고 `hasPointerCapture` 가 보고)와 재정의(이벤트가 가는 곳)를 나눴다. 다음 포인터 이벤트를 발생시킬 때
  "process pending pointer capture" 가 `lostpointercapture`(이전 재정의)와 `gotpointercapture`(새 재정의)를 보낸다. 캡처 중에는 `pointermove`, `mousemove` 와 경계 이벤트가 캡처 대상을 포인터 아래의 요소로 취급한다.
  `pointerup`/`pointercancel` 뒤 암묵적 해제(`lostpointercapture`)와, 캡처가 포인터를 붙들고 있었으면 실제 포인터 아래 요소로 옮기는 경계 이벤트를 보낸다. 눌린 버튼이 없거나 요소가 포인터가 활성인 문서에 없으면 `setPointerCapture` 는 조용히 아무것도 하지 않고,
  활성이 아닌 ID 는 `NotFoundError`, 연결되지 않은 요소는 `InvalidStateError`(`js.host.PointerApi`). 캡처 대상이 문서에서 떨어지면 캡처를 잃고 `lostpointercapture` 는 문서 요소에서 발생한다.
- **끌기 시작**(규격 4.1.3.3): `PageDragDrop.begin` 이 `dragstart` 를 취소하지 않으면 `PageInput.pointerCancelled` 가 `pointercancel`, `pointerout`, `pointerleave` 를 보내고 캡처를 풀며 버튼 상태를 비운다.
- 페이지가 바뀌거나 마우스가 컴포넌트를 떠나면(`reset`) `pointerout/pointerleave` 를 보낸 뒤 캡처·버튼·활성 문서 상태를 모두 잊는다. `touch-action` 은 이미 계산 값만 있는 CSS 속성이다.

### 검증 (JDK 6u45, 직접 실행)

- `js.host.PointerApiTest` 5건 통과(인터페이스·체인·기본값, 초기화 사전, 핸들러 속성, 서비스 없을 때 `NotFoundError`, 가짜 서비스로 세 메서드의 검사).
- `core.PointerEventIntegrationTest` 5건 통과(실제 AWT 마우스 이벤트: 진입/이동/이탈의 전체 순서, 누름·뗌·우클릭 `contextmenu`·동시 버튼·`click` 이 `PointerEvent`, 취소된 `pointerdown` 의 억제와 해제, 캡처의 시작·유지·종료와 경계 이벤트,
  즉시 해제한 캡처와 버튼 없는 상태의 캡처 거부).
- 회귀: `core` 전체(프레임, 끌어다 놓기, 선택, 키보드, 요청 계층, EventSource 포함) 357건 통과.
- 확인하지 못한 것: 끌기 시작 때의 `pointercancel` 을 실제 끌어다 놓기로 시험하지 않았다(호출은 `PageDragDrop.begin` 에 연결되어 있다). 실제 사람의 마우스·터치는 확인하지 못했다.

### 변경 파일

- 새 파일: `dom/PointerEvent.java`, `js/host/{PointerService,PointerApi}.java`, `core/PagePointers.java`, `tests/.../js/host/PointerApiTest.java`, `tests/.../core/PointerEventIntegrationTest.java`.
- 소유 경로 밖의 가산 변경: `js/dom/DomInterfaces.java`(PointerEvent 인터페이스와 `PointerEventInit` 읽기), `js/host/{HostEnvironment,WindowGlobals}.java`(서비스 훅, 설치 한 줄), `core/{BrowserSession,PageRuntime,PageInput,PageDragDrop}.java`,
  `tests/.../js/host/PageFixture.java`(가짜 포인터 서비스).

## 3. 페이지 수명 이벤트의 컴포넌트 수준 통합과 결함 수정 (P20-13 의 세 번째 항목; 작업자 WEBAPI-B, 커밋 번호 630~)

### 설계(`js.host.PageLifecycle`, `core.PageRuntime`, `core.BrowserSession`)

- `pageshow` 는 `load` 뒤에 페이지가 스스로 발생시키며 그때부터 페이지는 "보인 페이지"(`PageScripts.isPageShown()`)이다. **한 번도 보이지 않은 페이지(load 가 오기 전에 떠난 페이지)는 `beforeunload/pagehide/unload` 를 듣지 않는다.**
  규격(HTML Standard "unload a document")은 그런 문서에도 `unload` 를 보내므로 이것은 이 컴포넌트가 의도적으로 택한 단순화이다(시험 `testPageThatWasNeverShownHearsNothing` 가 고정한다).
- 떠나는 순서는 `beforeunload`(window, 취소 가능 `BeforeUnloadEvent`) → `pagehide`(window, `PageTransitionEvent`, `persisted` false, 버블링 없음) → `visibilitychange`(문서, hidden, 버블링) → `unload`(window)이다. 한 페이지에서 한 번만 실행한다.
  `beforeunload` 의 답은 대화상자가 없어 이동을 붙잡지 못하고 상태 메시지로만 알린다.
- 순서를 일으키는 곳: 새 이동이 시작될 때 이전 페이지의 요청 토큰이 취소되면서(`NavigationController.begin`) `PageRuntime.onCancel` 이 문서 큐에 순서를 올리고 그 뒤 페이지를 닫는다(`setPage`, `back`, `forward`, `refresh`, `setContent`,
  링크, 스크립트 이동 모두 같다). `clean()` 과 `dispose()` 는 `BrowserSession.unloadCurrentPage` 가 호출한 스레드에서 최대 `UNLOAD_WAIT_MS`(500ms) 동안 핸들러를 기다리며(이벤트 디스패치 스레드에서 불러도 막히지 않고 끝난다), 저장소를 비우기 전에 실행한다.
  언로드 이벤트의 리스너가 하나도 없는 페이지는 문서 큐 작업 없이 곧바로 닫힌다(`PageLifecycle.hasUnloadListeners`). 스크립트를 끈 페이지는 아무것도 듣지 않는다.
- 같은 문서 안의 이동(프래그먼트, `location.hash`, `history.pushState/back`)은 페이지의 끝이 아니므로 순서가 없다. 프래그먼트 이동은 `popstate`, `hashchange` 를 이 순서로 일으킨다.
- 호스트가 알리는 이벤트: 컴포넌트가 화면에 나타나거나 사라지면(`HierarchyEvent.SHOWING_CHANGED`) 문서의 `visibilitychange`(`SwingWebPanel.setDocumentVisible` 의 결과), 컴포넌트의 포커스 리스너가 받은 실제 포커스 이벤트로 window 의 `focus/blur`(페이지의 편집기 사이의 이동은 제외),
  `SwingWebPanel.setOnline(boolean)` 으로 `online/offline`.

### 이 작업에서 찾아 고친 결함 셋

1. **`setPage(url#fragment)` 가 같은 문서 안의 이동이 아니었다**(API 계약 5.2 는 그렇다고 하지만 구현은 링크/스크립트 이동만 처리했다): 문서를 다시 받고 이전 페이지를 떠나 `hashchange` 도 없었다. `BrowserSession.setPage` 가
   `startFragmentLocked` 를 쓰게 했다. 현재 주소와 완전히 같은 주소(프래그먼트 포함)는 계약 5.2 대로 별도 항목으로 다시 읽는다.
2. **문서에서만 `visibilitychange` 를 듣는 페이지(가장 흔한 "떠날 때 저장" 방식)가 "리스너 없는 페이지"로 취급되어 떠날 때 아무것도 듣지 못했다.** `dom.Document.hasVisibilityListener()`(등록 때 켜지는 volatile 표시,
   `js.host.Window.hasUnloadListener` 와 같은 방식)를 더하고 `PageLifecycle.hasUnloadListeners()` 가 함께 본다.
3. **숨겨진 컴포넌트에서 읽은 페이지(`document.visibilityState` 는 hidden 으로 시작)가 보이게 되어도 `visibilitychange` 를 받지 못했고, 로딩 중에 일어난 호스트의 변화(가시성, 네트워크 상태)는 영영 알려지지 않았다.** `PageLifecycle.install()` 이
   페이지가 처음 본 상태를 호스트에서 읽어 기억하게 하고, `PageScripts.maybeFinishLoad` 가 `pageshow` 직후 `visibilityChanged()/onlineChanged()` 로 따라잡게 했다.

세 결함 모두 수정 전 소스로 되돌려 시험을 돌리면 실패하는 것을 확인했다(`testSameDocumentNavigationDoesNotUnload`, `testDocumentVisibilityListenerHearsTheLeave`, `testHostVisibilityFocusAndNetworkState`).

### 시험 방법의 메모

- 페이지의 스크립트가 들은 이벤트를 `sessionStorage` 에 덧붙이는 JS 기록(이동에는 남고 `clean()` 에는 지워진다)과, 시험이 window/문서에 붙인 Java 리스너가 남기는 Java 기록(문서 큐가 실행하는 순간의 순서; `clean()`/`dispose()` 뒤에도 남는다)을 함께 쓴다.
  동기 XMLHttpRequest 는 지원하지 않으므로(표준이 페이지에 허용) 언로드 핸들러에서 서버로 보고하는 방식은 쓸 수 없고, 비동기 요청은 페이지가 닫히면서 취소되어 첫 요청만 닿는다(그래서 쓰지 않았다).
- **`pageshow` 는 컴포넌트가 페이지를 확정하기 조금 전에 페이지의 스크립트에 들린다.** `pageshow` 를 본 직후 곧바로 이동하면(시험이 그랬다) 그 페이지는 확정되지 못한 채 취소되어 보이지도, 언로드되지도 않고 `back()` 도 빈 페이지로 간다.
  CPU 가 바쁠 때 드러나므로 시험은 `Rig.shown(id)`(스크립트의 `pageshow` 와 패널의 `getPage()`/진행률 100 을 모두 기다림)을 쓴다. 사용자가 보기에는 "아직 보이지 않은 페이지를 떠난 것"과 같은 결과이고 이것은 설계된 동작이다.

### 검증 (JDK 6u45, 직접 실행)

- `core.PageLifecycleIntegrationTest` 9건 통과(`setPage`/`back`/`forward`/`refresh`/`refresh(true)`/`setContent` 의 순서, `clean()`/`dispose()`(이벤트 디스패치 스레드 포함, 반복 호출), 스크립트를 끈 페이지, 한 번도 보이지 않은 페이지, 같은 문서 이동,
  문서에서만 듣는 `visibilitychange`, 실제 `JFrame` 의 숨김/보임과 포커스 이벤트와 `setOnline`, 숨겨진 상태에서 읽은 페이지의 시작 상태, 로딩 중의 변화).
- `js.host.PageLifecycleTest` 9건 통과(기존 7건에 `testStartStateAndChangesDuringTheLoad`, `testDocumentVisibilityListenerCounts` 추가).
- 회귀: 이 항목의 변경을 넣은 전체 회귀(`pkgtest` 의 시험 패키지 전체) 2910 통과/2 실패(위 두 통합 시험의 `pageshow` 직후 이동 경쟁; 시험 쪽 결함이었고 `Rig.shown` 으로 고쳐 9건 통과)/1 건너뜀(화면이 필요한 시험).
- 확인하지 못한 것: 실제 사람의 창 전환(Alt+Tab)으로 인한 포커스 이벤트(`JFrame` 숨김과 합성 포커스 이벤트 전달만 확인), `dispose()` 가 닫힌 창에서 불렸을 때의 상태 메시지.

### 변경 파일

- `src/org/duckdns/hjow/swingwebpanel/core/BrowserSession.java`(`setPage` 의 프래그먼트 처리), `dom/Document.java`(`hasVisibilityListener`), `js/host/PageLifecycle.java`(문서 리스너, 시작 상태), `js/host/PageScripts.java`(`pageshow` 뒤 따라잡기).
- 시험: `tests/src/org/duckdns/hjow/swingwebpanel/core/PageLifecycleIntegrationTest.java`(새 파일), `tests/src/org/duckdns/hjow/swingwebpanel/js/host/PageLifecycleTest.java`(2건 추가).

## 4. IntersectionObserver 와 ResizeObserver (P20-13; WEBAPI-B, 커밋 631)

규격(조회일 2026-11-17, `tools/webfetch`, JRE Zulu25): Intersection Observer W3C Working Draft 2023-10-18(https://www.w3.org/TR/intersection-observer/, Last-Modified Wed, 18 Oct 2023 15:54:57 GMT), Resize Observer Module Level 1(https://www.w3.org/TR/resize-observer-1/, Mon, 10 Feb 2020 13:28:11 GMT).
본문 인용은 하지 않고 처리 모델(IO 3.2.1~3.2.8, RO 3.4.1~3.4.8)을 따랐다.

### 설계

- `js.host.RenderObservers`(조정자): 렌더링 갱신 단계를 예약한다. 페이지에 프레임 시계가 없으므로 기하를 바꿀 수 있는 일이 일어났고 관찰 대상이 있을 때 16ms(`UPDATE_DELAY_MS`, 한 프레임) 뒤 한 번 실행한다(변경이 몰려도 한 번).
  트리거: 관찰 대상 추가(`observe`), 문서 변경(`PageScripts` 의 변경 리스너), 뷰포트의 크기·스크롤 변화(`PageScripts.viewportChanged`), 컴포넌트의 새 레이아웃 게시(`PageView` → `HtmlPageDocument.layoutPublished` → `PageRuntime.layoutPublished` → `PageScripts.layoutPublished`).
  단계는 HTML 이벤트 루프의 순서대로 크기 관찰자 루프, 그 다음 교차 관찰자이다. 기하는 `HostEnvironment.getElementBounds`(뷰포트 픽셀의 border box)와 스타일 리졸버의 계산 스타일(padding, border, overflow, position, display, writing-mode)에서 온다.
- `js.host.IntersectionObservers`: 생성자(`root`, `rootMargin`, `scrollMargin`, `threshold`, 오류: SyntaxError/RangeError/TypeError), `observe/unobserve/disconnect/takeRecords`, `root/rootMargin/scrollMargin/thresholds`(얼린 배열), `IntersectionObserverEntry`(생성자와 프로토타입 게터).
  교차 계산은 규격 3.2.7: 포함 블록 체인을 따라 올라가며 내용을 자르는 컨테이너(`overflow` 가 `visible` 이 아님)의 padding box 로 자르고 마지막에 root 사각형(여백 적용)과 교차시킨다. 초기 통지는 첫 단계에서 항상 한 번(교차하지 않아도) 온다.
- `js.host.ResizeObservers`: 생성자, `observe(target, {box})`(같은 대상을 다시 관찰하면 새 관찰), `unobserve/disconnect`, 항목(`target`, `contentRect`, `borderBoxSize/contentBoxSize/devicePixelContentBoxSize` 얼린 시퀀스와 `ResizeObserverSize`).
  처리: 활성(`isActive`) 관찰을 깊이 0 에서 모아 방송하고, 방송한 가장 얕은 대상의 깊이보다 깊은 활성 관찰만 다시 모으기를 되풀이한 뒤, 남은(건너뛴) 관찰이 있으면 window 에 `ResizeObserver loop completed with undelivered notifications.` 오류를 보고한다(`error` 이벤트; `error` 속성은 TypeError 객체이다).
  치환되지 않은 인라인 요소와 렌더링되지 않는 요소의 크기는 0 이다.

### 한계(정직하게)

- 포함 블록 체인은 계산된 `position`(fixed → 뷰포트, absolute → 위치 지정 조상, 나머지 → 부모 요소)으로 근사한다. `clip-path`, `contain`, CSS 변환의 자르기는 보지 않는다. 요소의 자체 스크롤이 없으므로 `scrollMargin` 은 해석하고 되돌려 읽을 수만 있고 자르기에 영향이 없다.
- `isIntersecting` 은 자른 사각형을 따른다(규격 문구는 자르지 않은 사각형을 비교한다; Chrome 과 같은 읽기). 상자가 없는 대상은 교차하지 않는다. 교차 출처 프레임 대상은 없다.
- `rootMargin` 의 백분율은 네 변 모두 root 사각형의 폭에 대한 비율이다(규격 문구대로). 관찰자는 항목을 계산한 같은 작업에서 통지받는다(별도 작업이 아님).
- ResizeObserver 의 SVG 요소는 bounding box 가 아니라 border box 로 잰다. 크기 시퀀스는 항목 하나(첫 조각)이다. 스크롤바 폭은 content box 에서 빼지 않는다. `device-pixel-content-box` 는 `devicePixelRatio` 를 곱해 반올림한 값이다.
- 프레임 안의 문서(`iframe`)는 DOM 변경과 뷰포트 변화로만 갱신되고 레이아웃 게시 통지(`PageView`)를 받지 못한다(이미지가 늦게 도착해 크기가 바뀌는 경우 다음 트리거까지 알지 못한다).
- 관찰자는 대상과 콜백을 강하게 참조한다(규격 3.3 의 수명과 달리 스크립트 참조가 없어져도 대상이 있으면 계속 산다). 페이지 폐기 때 모두 버린다.

### 검증 (JDK 6u45, 직접 실행)

- `js.host.RenderObserversTest` 18건 통과(인터페이스와 문자열 태그, 옵션과 오류, 초기 항목과 두 번 observe, 임계값과 가장자리 접촉, 면적 0 대상, rootMargin(px, %, 음수), unobserve/disconnect/takeRecords, 자르는 조상과 absolute/fixed, 명시적 root,
  항목 생성자, RO 초기 항목의 세 상자와 content rect, 빈 상자, 관찰 상자별 보고, 쓰기 방향, 깊이 루프와 루프 오류, 단계의 순서와 트리거, 폐기).
- `core.RenderObserversIntegrationTest` 3건 통과(실제 `JScrollPane`/`SwingWebPanel`/레이아웃: 뷰포트를 좁히면 `width:50%` 요소의 크기를 보고, 뷰포트 스크롤에 따라 `IntersectionObserver` 항목이 false → 0.8 → 1 → false, 떠난 페이지의 관찰자는 보고하지 않음).
- 회귀: `js.host.PageLifecycleTest` 9건 통과. 이 항목만의 전체 회귀는 마지막에 한 번 돌린다.

### 변경 파일

- 새 파일: `src/org/duckdns/hjow/swingwebpanel/js/host/{RenderObservers,IntersectionObservers,ResizeObservers}.java`, `tests/.../js/host/RenderObserversTest.java`, `tests/.../core/RenderObserversIntegrationTest.java`.
- 소유 경로 밖의 가산 변경: `js/host/PageScripts.java`(`rendering` 필드, 변경 리스너·뷰포트·폐기 연결, `layoutPublished`), `js/host/WindowGlobals.java`(설치 한 줄), `core/{HtmlPageDocument,PageRuntime,PageView}.java`(레이아웃 게시 통지).

## 5. performance: User Timing 과 Navigation Timing (P20-13; WEBAPI-B, 커밋 632)

규격(조회일 2026-11-17, `tools/webfetch`, JRE Zulu25): User Timing Level 3(https://www.w3.org/TR/user-timing/, Last-Modified Wed, 11 Mar 2026 16:02:27 GMT), Navigation Timing Level 2(https://www.w3.org/TR/navigation-timing-2/, Mon, 02 Nov 2026 10:14:04 GMT).
옛 `performance.timing`/`performance.navigation` 은 Navigation Timing Level 1 의 속성 이름을 따랐다(Level 2 문서의 부록이 가리킨다).

### 설계

- `js.host.PagePerformance` 가 `window.performance` 를 `Performance` 인터페이스의 객체로 만든다(기존의 `now`/`timeOrigin` 만 있던 보통 객체를 대체). 인터페이스 `Performance`, `PerformanceEntry`, `PerformanceMark`(페이지가 생성 가능), `PerformanceMeasure`,
  `PerformanceNavigationTiming`, `PerformanceTiming`, `PerformanceNavigation` 은 전역에 생성자 함수로 있고 `Symbol.toStringTag` 와 `Illegal constructor` 를 가진다.
  - `mark(name, {startTime, detail})`: 음수·비유한 `startTime` 은 TypeError, `PerformanceTiming` 속성 이름(`navigationStart` 등)은 SyntaxError(DOMException), `detail` 객체는 `structuredClone` 으로 복사. `new PerformanceMark()` 는 타임라인에 들어가지 않는다.
  - `measure(name, startOrMeasureOptions, endMark)`: 문자열 시작 마크, `{start, end, duration, detail}` 옵션(세 개 모두 주면 TypeError, 옵션과 endMark 를 함께 주면 TypeError), 없는 마크는 SyntaxError, 타이밍 속성 이름은 그 시각에서 `navigationStart` 를 뺀 값.
  - `getEntries()/getEntriesByType()/getEntriesByName()`: 탐색 항목(`startTime` 0)이 먼저이고 그 뒤 마크와 측정이 시작 시각 순서(같으면 만든 순서). `clearMarks([name])`, `clearMeasures([name])`.
- `js.host.PageTimeline`: 페이지 호스트가 실제로 지난 단계의 시각(`domInteractive`=`readyState` interactive, `DOMContentLoaded` 시작·끝, `domComplete`=`readyState` complete, `load` 시작·끝)을 스케줄러 시계(에포크 밀리초)로 기록한다(`PageScripts` 의 `setReadyState`, `completeParsing`, `maybeFinishLoad`).
  탐색 항목의 `domInteractive`… 와 옛 `performance.timing` 의 같은 이름은 이 기록에서 읽는다. 이르지 못한 단계는 0 이다.
- 탐색 종류: `HostEnvironment.getNavigationType()`(`navigate`/`reload`/`back_forward`)을 `PageRuntime` 이 이동 요청의 종류(`REFRESH` → reload, `HISTORY` → back_forward)에서 계산한다. 탐색 항목의 `type` 과 `performance.navigation.type`(0/1/2)이 읽는다.

### 한계(정직하게)

- **페이지가 알 수 없는 값은 지어내지 않고 0 으로 둔다**: `redirectStart/End`, `fetchStart`, `domainLookup*`, `connect*`, `secureConnectionStart`, `requestStart`, `responseStart/End`, `unloadEventStart/End`, `transferSize`/`encodedBodySize`/`decodedBodySize`, `redirectCount`.
  이동 요청이 응답과 시각 정보를 페이지 호스트에 넘기지 않기 때문이다(로더 → `PageRuntime` 경로에 필드를 더하면 채울 수 있다: 다음 작업 후보). 옛 `navigationStart`/`domLoading` 은 시간 원점(페이지 호스트가 시작한 순간; 실제 이동 시작은 그보다 이르다)이다.
  `duration` 은 `loadEventEnd` 이다(load 가 끝나기 전에는 0).
- `PerformanceNavigationTiming` 은 `PerformanceResourceTiming` 이 아니라 `PerformanceEntry` 에서만 파생된다. `Performance` 는 `EventTarget` 이 아니다. `PerformanceObserver`, 리소스 항목(`resource`), `paint`/`longtask` 같은 다른 항목 종류는 없다.
- 워커의 `performance`(`js.worker.WorkerGlobals`)는 기존대로 `now()`/`timeOrigin` 만 있다.
- 시계 해상도는 밀리초이다(스케줄러 시계).

### 검증 (JDK 6u45, 직접 실행)

- `js.host.PagePerformanceTest` 6건 통과(인터페이스, `now`/`timeOrigin`, `mark` 의 모든 형태와 오류, `measure` 의 모든 형태와 오류, 타임라인 조회와 지우기, 탐색 항목과 옛 timing 의 단계 순서와 0 값).
- `core.PerformanceIntegrationTest` 1건 통과(실제 컴포넌트와 HTTP 서버: 이동 `navigate`/0, 뒤로 `back_forward`/2, 새로고침 `reload`/1, 실제 시계의 단계 순서, 네트워크 시각 0, `performance.now()` 와 `Date.now() - timeOrigin` 의 일치).
- 회귀: `js.host.PageLifecycleTest` 9건 통과.

### 변경 파일

- 새 파일: `src/org/duckdns/hjow/swingwebpanel/js/host/{PagePerformance,PageTimeline}.java`, `tests/.../js/host/PagePerformanceTest.java`, `tests/.../core/PerformanceIntegrationTest.java`.
- 소유 경로 밖의 가산 변경: `js/host/PageScripts.java`(타임라인·performance 필드와 단계 기록), `js/host/WindowGlobals.java`(기존 performance 객체를 대체), `js/host/HostEnvironment.java`(`getNavigationType`), `core/PageRuntime.java`(`getNavigationType`).

## 6. 인터페이스 객체 이름 (P20-13; WEBAPI-B, 커밋 633)

`Navigator`, `Location`, `History`, `Screen`, `Performance`(5절), `DOMStringMap`, `MutationRecord` 가 전역에 생성자 함수 이름으로 있다(`js.host.InterfaceNames`): `typeof Navigator === 'function'`, `Navigator.prototype.constructor === Navigator`,
`Object.prototype.toString.call(navigator) === '[object Navigator]'`(프로토타입의 `Symbol.toStringTag`), `navigator instanceof Navigator`. 함수로 부르거나 `new` 로 만들면 `TypeError: Illegal constructor`.
규격 근거는 HTML Standard 의 해당 인터페이스 IDL 이며 새로 조회한 규격은 없다(WebIDL 의 인터페이스 객체 규칙).

- 이미 있던 객체(`navigator`, `location`, `history`, `screen`)는 자기 멤버를 그대로 가지고 프로토타입만 새 인터페이스 프로토타입으로 바뀐다(`setPrototypeOf`). 브라우저는 멤버가 프로토타입에 있으므로 자기 프로퍼티를 살피는 코드
  (`Object.keys(navigator)`, `hasOwnProperty('userAgent')`)에서만 차이가 보인다(한계).
- `dataset` 객체는 `DOMStringMap` 프로토타입(`ElementExtras`)을, 변경 기록은 `MutationRecord` 프로토타입(`MutationObservers`)을 쓴다(기록은 여전히 자기 프로퍼티를 가진 객체이고 `addedNodes/removedNodes` 는 배열이다).
- 검증: `js.host.InterfaceNamesTest` 3건 통과(인터페이스 객체의 이름·프로토타입·`Illegal constructor`, 일곱 객체의 `instanceof` 와 `Object.prototype.toString`, 기존 멤버가 그대로 동작). 회귀: `js.host` 패키지 384건 통과/0 실패.
- 변경 파일: `src/org/duckdns/hjow/swingwebpanel/js/host/InterfaceNames.java`(새 파일), `tests/.../js/host/InterfaceNamesTest.java`(새 파일), `js/host/{WindowGlobals,ElementExtras,MutationObservers}.java`.

## 7. 키보드 이벤트의 code 와 isComposing (P20-13; WEBAPI-B, 커밋 634)

규격: UI Events KeyboardEvent code Values(https://www.w3.org/TR/uievents-code/, Last-Modified Mon, 21 Apr 2025 15:04:51 GMT, 조회일 2026-11-18, `tools/webfetch`).

- `core.KeyNames.code(vk, location)`(이미 있었던 표)에 빠진 키를 더했다: `F13`~`F24`(AWT 에서 다른 값 범위), `NumpadAdd/Subtract/Multiply/Divide/Decimal/Comma`, `NumLock`, `ScrollLock`, `PrintScreen`, `Pause`, `ContextMenu`, `AltGraph` → `AltRight`.
  AWT 키 코드에서 만들 수 없는 `IntlBackslash`, `IntlRo`, `IntlYen`, `Lang1~5` 와 키보드 배열 차이는 빈 문자열이다(한계).
- `isComposing`: `PageComposition.inputMethodTextChanged` 가 조합 상태를 `PageKeyboard.setComposing` 에 알리고, 그동안 도착하는 `keydown/keypress/keyup`(`Key` 값 객체에 EDT 시점의 상태를 복사)이 `isComposing` true 가 된다(전에는 항상 false). 입력기가 키를 소비하면 AWT 가 컴포넌트에 키 이벤트를 주지 않으므로
  이것은 입력기가 처리하지 않고 페이지에 닿은 키(예: 조합 중의 화살표)에 해당한다. 규격/브라우저와 달리 조합 중 keydown 의 `key` 를 "Process" 로, `keyCode` 를 229 로 바꾸지는 않는다(편집 엔진이 `key` 로 기본 동작을 정하므로 위험하여 하지 않았다).
- 검증: `core.KeyboardCodeTest` 2건 통과(코드 표 8묶음 전부, 실제 컴포넌트와 AWT 키 이벤트로 `key/code/isComposing/location` 과 한글 조합 시작·확정 전후의 `isComposing`). 회귀: `core.Editing*` 21건, `core.KeyboardFocusTest` 5건, `core.FormControlIntegrationTest` 13건 통과.
- 확인하지 못한 것: 실제 한글 IME 로 조합하는 동안의 키 이벤트(합성 `InputMethodEvent` 만 확인; P11-02/10 의 사람 확인에 남는다).
- 변경 파일: `core/{KeyNames,PageKeyboard,PageComposition}.java`, 시험 `tests/.../core/KeyboardCodeTest.java`(새 파일).

## 8. DOM XPath (P20-13; WEBAPI-B, 커밋 635)

규격(조회일 2026-11-18, `tools/webfetch`, JRE Zulu25): XML Path Language (XPath) 1.0, W3C Recommendation 16 November 1999(https://www.w3.org/TR/1999/REC-xpath-19991116/, Last-Modified Mon, 02 Oct 2017 10:22:05 GMT);
WHATWG DOM Standard 8절 "XPath"(https://dom.spec.whatwg.org/, Last-Modified Sun, 15 Nov 2026 07:40:07 GMT).

### 설계

- `dom.xpath.XPathExpression`(JavaScript 와 무관한 순수 Java 엔진): `compile(text)` → 구문 트리(재귀 하강 분석기: 3절 문법, 3.7절 어휘 규칙의 `*`/`and`/`or`/`div`/`mod` 연산자-이름 판별), `evaluate(contextNode, resolver)` → `Double`/`String`/`Boolean`/노드 집합.
  노드 집합은 문서 순서의 중복 없는 `List<Object>`(원소는 `Node` 또는 `Attr`; `Attr` 는 `Node` 가 아니다)이며 트리마다 한 번 번호를 매겨 정렬한다. 축 13개(`namespace` 는 비어 있음), 노드 테스트(이름, `*`, `prefix:*`, `text()`, `comment()`, `processing-instruction(literal)`, `node()`), 술어(축 방향의 근접 위치),
  연산자와 노드 집합 비교 규칙(3.4), 핵심 함수 27개(arity 는 컴파일할 때 검사), `string()` 의 숫자 변환(4.2), `number()` 의 문자열 해석(`1e3`, `+1` 은 NaN), `round()`/`substring()` 의 반올림 규칙.
  HTML 문서에서는 접두사 없는 이름 테스트가 HTML 네임스페이스의 요소도 일치시키고 소문자로 비교한다(`//DIV` 가 `div` 를 찾는다. 브라우저와 같다). XML 문서는 대소문자를 구분하고 접두사 없는 이름은 네임스페이스 없는 이름이다.
- `js.host.XPathApi`: `XPathResult`(상수 10개, `resultType`, `numberValue`, `stringValue`, `booleanValue`, `singleNodeValue`, `invalidIteratorState`, `snapshotLength`, `iterateNext()`, `snapshotItem()`), `XPathExpression.evaluate`, `XPathEvaluator`(생성 가능), `document.evaluate/createExpression/createNSResolver`.
  결과 종류 규칙: `ANY_TYPE` 은 값에 따라(노드 집합은 `UNORDERED_NODE_ITERATOR_TYPE`), 숫자/문자열/불리언 변환은 `number()/string()/boolean()`, 노드 집합이 아닌 값에 노드 종류를 요청하면 TypeError, 종류가 0~9 밖이면 `NotSupportedError`, 다른 종류의 값 접근자는 TypeError.
  반복자는 결과를 만든 뒤 문서가 바뀌면 무효(`invalidIteratorState`, `iterateNext()` 의 `InvalidStateError`; `PageScripts` 의 변경 리스너가 센다). 오류: 구문 → `SyntaxError`, 해석 못하는 접두사 → `NamespaceError`, 변수/집합이 아닌 값 → `TypeError`.
  리졸버는 함수, `lookupNamespaceURI` 를 가진 객체, 노드(`createNSResolver(node)` 는 노드 자신을 반환).

### 한계

- `namespace` 축은 항상 비어 있다. 변수 참조(`$x`)는 TypeError 이다(DOM XPath 식에는 변수가 없다). `result` 인수(결과 객체 재사용)는 무시하고 새 결과를 만든다. 확장 함수(XSLT 의 `current()` 등)는 없다.
- 문서 순서가 다른 트리(떨어진 노드)끼리는 안정적이지만 의미 없는 순서이다. 노드 집합의 순서 계산은 평가마다 트리를 한 번 훑는다(큰 문서에서 매 `//` 마다 비용이 있다).
- `Attr` 래퍼에는 `nodeType`/`nodeName` 같은 `Node` 멤버가 없다(이 프로젝트의 기존 한계). 속성 노드의 `name`, `value`, `ownerElement` 는 쓸 수 있다.
- XSLT(`XSLTProcessor`)는 구현하지 않았다(WHATWG DOM 9절, 범위 밖).

### 검증 (JDK 6u45, 직접 실행)

- `js.host.XPathApiTest` 13건 통과(인터페이스와 상수, 위치 경로와 위치 술어, 13개 축, 노드 테스트, 연산자와 우선순위, 노드 집합 비교, 문자열/숫자/불리언 함수와 권고안의 예, 결과 종류와 변환, 반복자 무효화, 식 객체·평가기·리졸버, XML 문서의 대소문자와 접두사, 오류).
  시험 작성 중 시험이 `//` 뒤의 둘째 단계가 구문 오류가 되는 결함을 드러냈고 고쳤다(`parseMoreSteps` 누락).
- 회귀: 이 항목 이전(1~5항목)의 전체 회귀(`pkgtest` 의 시험 패키지 전체)는 2945 통과/0 실패/1 건너뜀(제품 JAR 을 빌드하지 않아 `JarContentTest` 건너뜀)이다. XPath 와 6절 이후의 변경은 전체를 다시 돌린 결과를 아래 "최종 검증"에 쓴다.

### 변경 파일

- 새 파일: `src/org/duckdns/hjow/swingwebpanel/dom/xpath/{XPathException,NamespaceResolver,XPathExpression}.java`, `src/org/duckdns/hjow/swingwebpanel/js/host/XPathApi.java`, `tests/.../js/host/XPathApiTest.java`.
- 소유 경로 밖의 가산 변경: `js/host/PageScripts.java`(`xpath` 필드, 변경 리스너), `js/host/WindowGlobals.java`(설치 한 줄).

## 9. WEBAPI-B 작업 요약과 조정자 요청 (커밋 630~635, 2026-11-17~18)

### 항목별 상태(P20-13 나머지)

| 항목 | 상태 | 근거(이 문서) |
| --- | --- | --- |
| 페이지 수명 이벤트의 컴포넌트 수준 통합 시험 | 완료(자동 검증). 결함 3건 수정 | 3절 |
| `IntersectionObserver` / `ResizeObserver` | 구현·검증(단위 18, 통합 3). 한계는 4절 | 4절 |
| `performance.mark/measure/getEntries*/clear*`, `performance.timing`/`PerformanceNavigationTiming` | 구현·검증(단위 6, 통합 1). 네트워크 시각은 0 | 5절 |
| 인터페이스 객체 이름(`Navigator`, `Location`, `History`, `Screen`, `Performance`, `DOMStringMap`, `MutationRecord`) | 구현·검증(3건) | 6절 |
| 키보드 `code` 와 `isComposing` | 구현·검증(2건). 실제 한글 IME 는 미확인 | 7절 |
| XPath(`document.evaluate` 등) | 구현·검증(13건) | 8절 |
| 텍스트 트랙/WebVTT(`<track>`, `TextTrack`, `TextTrackCue`/`VTTCue`, `cuechange`) | **WEBAPI-C 가 구현·검증**(오디오 요소; 자막은 그리지 않음) | 10절 |
| `PointerEvent`/`EventSource` 시험의 경계 사례 점검 | **WEBAPI-C 가 점검**(포인터 캡처 결함 1건 수정, EventSource 리디렉션 시험 추가) | 10절 |

**체크 후보**: P20-13 은 텍스트 트랙/WebVTT 가 남아 있으므로 전체 체크 후보가 아니다. 그 항목만 남기고(그리고 EventSource/PointerEvent 경계 점검) 나머지는 모두 구현되고 시험으로 검증되었다.

### 남은 일 / 다음 작업

1. **텍스트 트랙/WebVTT**: 규격(W3C WebVTT, 조회 판본 위 표)의 파싱 알고리즘(파일 파서와 cue 텍스트 파서, 타임스탬프, 설정), `HTMLTrackElement`(`kind/src/srclang/label/default/readyState/track`), `TextTrack`(`mode`, `cues`, `activeCues`, `addCue/removeCue`, `cuechange`), `TextTrackList`, `VTTCue`, 오디오 요소의 `textTracks`(`MediaElements` 의 재생 위치와 연결)가 필요하다.
   비디오 재생이 없으므로 오디오 요소의 트랙만 대상이다. 규모가 커서(파서만도 규격 알고리즘이 길다) 이번 작업의 남은 시간에 다 하지 못하고 구현하지 않았다. 새 작업자에게 맡긴다(`js/host/MediaElements`, `dom/html/HtmlTrackElement` 부근).
2. **`PointerEvent`/`EventSource` 시험의 경계 사례 점검**: 기록(1, 2절)의 "확인하지 못한 것"(끌기 시작 때의 `pointercancel` 실제 끌어다 놓기 시험, 실제 사람의 마우스·터치)과 EventSource 의 재연결 지수 백오프 없음 등은 그대로이다.
3. `performance` 의 네트워크 시각: 이동 요청이 응답 메타데이터(시작·응답 시각, 리디렉션 수, 전송 크기)를 `PageRuntime` 까지 넘기면 `redirectCount`, `fetchStart`, `responseEnd`, `transferSize` 등을 실제 값으로 채울 수 있다(5절).
4. `IntersectionObserver` 의 `scrollMargin` 적용, `clip-path`/`contain`, 프레임 안의 문서에서 레이아웃 게시 통지(4절).
5. `Attr` 래퍼에 `Node` 멤버(`nodeType`, `nodeName` 등)가 없는 기존 한계(XPath 시험에서 드러남, 8절).

### 조정자 요청

1. **문서 갱신 후보**(제가 고치지 않았다):
   - `docs/ARCHITECTURE.md`: 새 클래스 — `js.host.{RenderObservers,IntersectionObservers,ResizeObservers}`(기하 관찰자와 렌더링 단계), `js.host.{PagePerformance,PageTimeline}`(User/Navigation Timing), `js.host.InterfaceNames`, `js.host.XPathApi`, 패키지 `dom.xpath`(`XPathExpression`, `XPathException`, `NamespaceResolver`).
     `core.PageView` → `HtmlPageDocument.layoutPublished()` → `PageRuntime.layoutPublished()` → `PageScripts.layoutPublished()`(기하 관찰자 갱신 트리거), `HostEnvironment.getNavigationType()`(탐색 종류).
   - `docs/API_CONTRACT.md` 5.2: `setPage(url)` 은 현재 문서와 프래그먼트만 다른 주소를 같은 문서 안의 이동으로 처리한다(문서를 다시 받지 않고, 이전 페이지의 언로드 이벤트가 없으며, `hashchange` 가 발생). 이것은 계약 5.2 가 원래 적은 동작이고 구현이 이제 맞춘 것이다.
     현재 주소와 완전히 같은 주소는 계약대로 별도 항목으로 다시 읽는다.
   - `docs/PROGRESS.md` 0절/남은 일: 위 항목별 상태, 특히 "수명 이벤트 결함 3건"(3절)과 `pageshow` 가 페이지 확정 직전에 스크립트에 들리는 사실(3절 "시험 방법의 메모": 사용자 코드가 `pageshow` 직후 곧바로 이동하면 그 페이지는 확정되지 못하고 취소된다).
2. FEATURE_MATRIX 는 제가 구현한 기능의 행만 고쳤다: WEB-10(IO/RO), WEB-11(performance), DOM-10(MutationRecord), EVT-04(keyboard code/isComposing), 484행(Observers), 486행(XPath). 11~13절의 개수표(축별 검증 완료/부분/미구현)는 갱신하지 않았다(조정자 몫).
3. `AGENTS.md` P20-13 의 입력 목록에서 이번에 구현한 것(`IntersectionObserver`/`ResizeObserver`, XPath, `performance.mark/measure/timing`, 인터페이스 객체 이름, 키보드 `code`/`isComposing`, 페이지 수명 이벤트의 컴포넌트 시험)을 완료로 표시해 주세요. 텍스트 트랙/WebVTT 가 남는다.

### 환경 메모

- 시험 도구: `tools/webfetch` 를 `WEBFETCH_JAVA_HOME`(Zulu 25)로 실행해 규격을 조회했다(조회일 2026-11-17, 2026-11-18). 이 worktree 에는 `tools/webfetch/build.sh`, `tools/sourcecheck/build.sh` 를 먼저 돌려야 한다(빌드 산출물은 git 무시).
- 이번 작업의 수정으로 `Document.addEventListener(5-arg)` 가 재정의되어 `visibilitychange` 등록 여부를 기억한다(`Document.hasVisibilityListener`).

### 최종 검증 (WEBAPI-B, 2026-11-18)

- 명령: `sh scripts/pkgtest.sh wabfinal "js dom html css net storage core content layout render download media @root" "js core render @root layout content download dom html css net storage"`(JDK 6u45, 처음부터 컴파일)
  → **2958 통과/0 실패/1 건너뜀**(건너뜀: `JarContentTest.testProductJarContainsOnlyProductFiles`, 제품 JAR 을 빌드하지 않았음). 마지막 `===` 줄: `=== 2958 passed, 0 failed, 1 skipped (423446 ms) ===`.
  이 목록은 `media`, `frames` 등 일부 시험 패키지를 줄인 것이므로 조정자의 `sh test.sh` 전체 회귀(3274 + 이번 새 시험)와 수가 다르다. 이 worktree 의 새 시험은 모두 이 실행에 들어 있다.
- 소스 규칙: `sh tools/sourcecheck/run.sh`(기본 경로 전체) `0 violation(s) in 2151 file(s) checked`.
- 중간 전체 회귀(1~5항목, 같은 목록): 2945 통과/0 실패/1 건너뜀. 첫 전체 회귀(1항목 직후)에서 통합 시험 2건이 `pageshow` 직후 이동 경쟁으로 실패했고(시험 쪽 결함) `Rig.shown` 으로 고쳐 통과했다(3절).

## 10. 텍스트 트랙과 WebVTT (P20-13 의 마지막 항목; 작업자 WEBAPI-C, 커밋 650~, 2026-11-18)

### 규격 판본과 조회 기록 (조회일 2026-11-18, `tools/webfetch`, JRE Zulu25)

| 규격 | URL | 서버의 Last-Modified |
| --- | --- | --- |
| WebVTT: The Web Video Text Tracks Format (W3C Candidate Recommendation Draft) | https://www.w3.org/TR/webvtt1/ | Wed, 20 May 2026 14:48:38 GMT |
| HTML Standard 4.8.10 The track element, 4.8.11.8 Playing the media resource(time marches on steps), 4.8.11.11 Timed text tracks | https://html.spec.whatwg.org/multipage/media.html | Tue, 17 Nov 2026 11:40:09 GMT |

본문 인용은 하지 않고 알고리즘(WebVTT 6.1~6.6, 9.1, 9.2. HTML 4.8.11.11.1~4.8.11.11.6, time marches on)을 따랐다.

### 설계

- **`media.vtt`(순수 Java, DOM/스크립트와 무관)**: `VttParser`(WebVTT 6.1 파일 파서: 서명 확인, 머리 블록, 블록 모으기, `REGION`/`STYLE`/`NOTE` 블록, cue 블록의 오류 복구, 줄 끝과 NUL 정규화, UTF-8 바이트 입력과 BOM, 6.2 region 설정, 6.3 타이밍과 설정),
  `VttTimestamps`(타임스탬프/백분율), `VttCueText`(6.4 cue 텍스트 토크나이저와 트리 구성, 6.6 장 제목), `VttEntities`(문자 참조: 이름 있는 것과 세미콜론 없는 옛 이름, 숫자, C1 대체), `VttCue`/`VttRegion`/`VttNode`/`VttFile`(자료).
  데이터 모델 객체의 필드는 public 이다(파서가 채우고 스크립트 쪽이 검사하는 단순한 기록). `line`/`position` 의 `auto` 는 NaN(`VttCue.AUTO`)이다.
- **`js.host` 의 스크립트 쪽**: `TextTracks`(페이지마다 하나의 관리자: 트랙 목록, `addTextTrack`, 트랙 처리 모델(가져오기), 자동 선택, time marches on, `getCueAsHTML`), `TextTrackBindings`(인터페이스 설치),
  값 클래스 `TextTrackValue`/`TextTrackListValue`/`TextTrackCueValue`/`TextTrackCueListValue`/`VttRegionValue`(DOM 브리지가 접근자를 리플렉션으로 읽으므로 public), `MediaTracks`(미디어 요소별 상태), `dom.TrackEvent`.
  `MediaElements` 는 재생 위치가 바뀔 때마다(시간 갱신, 탐색 끝, 반복, 끝) `TextTracks.timeMarchesOn` 을 부르고, `track` 자식의 삽입/제거와 `src` 속성 변경, 로드(초기화), 재생/일시정지/속도 변경, 폐기를 알린다. `MediaElements.queueTask` 는 코드를 "미디어 요소의 작업"으로 올린다
  (새 로드가 버리는 일반 작업과, 버리지 않는 고정 작업: 트랙 가져오기의 결과).
- **time marches on**: 규격 순서(현재 cue/그 밖의 cue/놓친 cue, pause-on-exit, enter/exit 정렬: 시각, 텍스트 트랙 cue 순서, enter 먼저, 트랙마다 `cuechange`(트랙, 그 다음 track 요소), 활성 표시 갱신). `activeCues` 는 활성 표시를 읽고, cue/트랙의 변경은 작업으로 한 번 모아 실행한다
  (규격의 "when the script started" 와 같다: 변경한 스크립트가 끝나기 전에는 활성 cue 가 바뀌지 않는다). 오디오가 재생되는 동안 타이머(`Scheduler`)가 다음 cue 경계(위치 뒤의 가장 이른 시작/끝, 재생 속도 환산, 최대 1초 앞)에서 단계를 실행하므로 이벤트가
  시간 갱신(200 ms 마다)에만 매이지 않는다.
- **가져오기**: 모드가 `hidden`/`showing` 이고 부모가 미디어 요소인 `track` 요소는 `WebRequestSource`(`fetch` 와 같은 공통 요청 계층: User-Agent, 추가 헤더, 쿠키, SSL 훅, 스킴 제한)로 파일을 받는다. 요청 모드는 same-origin 대체(미디어 요소에 `crossorigin` 이 없으면 same-origin,
  있으면 CORS, `use-credentials` 이면 자격 증명 포함). 성공(2xx, WebVTT 서명)이면 cue 가 트랙에 들어가고 `readyState` 2 와 `load`, 아니면 `readyState` 3 과 `error`. URL 이 같은 동안은 모드를 바꿔도 다시 가져오지 않고, `src` 가 바뀌면 cue 를 곧바로 비우고 진행 중인 요청을 취소한 뒤(`error`) 다시 가져온다.
- **자동 선택**: `track` 삽입 뒤 작업으로 `default` 가 있는 첫 자막/캡션 트랙, 설명 트랙을 `showing`, chapters/metadata 를 `hidden` 으로 한다. 파서가 만든 요소는 파싱이 끝나기를 기다린다(`PageScripts.completeParsing` → `TextTracks.parsingFinished`).
- 규격과 다르게 한 것(정직하게): ① **show poster 표시를 모델링하지 않는다**(메타데이터를 알면 곧바로 단계를 실행한다). ② 규격 문구 그대로면 마지막 실행 위치가 cue 의 시작 시각과 정확히 같을 때 이미 활성인 cue 가 "놓친 cue" 로도 잡혀 `enter` 가 두 번 나간다
  (시험이 드러냈다): 활성인 cue 는 놓친 것에서 뺀다. ③ 텍스트 트랙이 준비되었는지(pending text tracks)를 window `load` 에 반영하지 않는다. ④ 가져오기는 파일 전체가 도착해야 끝난다(규격은 점진 해석). ⑤ 파일의 콘텐츠 타입은 검사하지 않는다(규격 본문도 정하지 않음).
  ⑥ 가져오는 중에 미디어 요소에서 빠진 트랙은 가져오기를 중단하고 NONE 으로 돌아간다(규격은 말하지 않음). ⑦ 언어 선호가 없으므로 `default` 만 트랙을 고른다. ⑧ `kind` 속성의 잘못된 값은 `metadata`, 없으면 `subtitles`(규격).

### 한계 (구현하지 않은 것)

- **화면에 자막을 그리지 않는다**: `audio` 요소에는 텍스트를 보일 영역이 없고(렌더링 규칙 WebVTT 7은 비디오용) `video` 는 EXC-01 이다. `showing` 은 `hidden` 과 같게 동작한다(`activeCues`/이벤트만). `video` 요소의 `track` 은 구조(목록, 가져오기, cue)만 있고 재생과 연결되지 않는다(`video` 는 로드되지 않으므로 활성 cue 가 없다).
- 인밴드 트랙(미디어 리소스 안의 트랙)과 `audioTracks`/`videoTracks`, `::cue`/`::cue-region` 스타일, `STYLE` 블록의 적용(파서는 텍스트를 모으기만 한다), `VTTRegion` 의 렌더링, `TextTrack.getCueById`(규격에서 삭제됨), `TextTrackCue` 의 직접 생성(`Illegal constructor` 대신 VTTCue 만)은 없다.
- `new TextTrackList()` 등 만들 수 없는 인터페이스는 `TypeError: Illegal constructor`.
- JavaScript 를 끈 페이지에서는 미디어 요소 전체가 동작하지 않는다(기존 한계)이므로 텍스트 트랙도 없다.
- 시험 결과의 시간 민감성: 재생 중 이벤트 시험(`testEventsDuringPlayback`, `testPauseOnExit`)은 가짜 출력과 수동 시계를 25 ms 씩 함께 움직여 결정적으로 만들었으나 재생기 스레드의 응답에 의존한다.

### 포인터 이벤트의 결함 수정 (끌기 시작과 포인터 캡처; 커밋 652)

`pointerdown` 핸들러가 `setPointerCapture` 를 부르고 이어서 끌기가 시작되면(`dragstart` 취소 안 됨) `PageInput.pointerCancelled` 가 대기 중인 캡처를 처리하지 않고 지워서 `gotpointercapture`/`lostpointercapture` 가 없었다.
(Pointer Events: 포인터 이벤트를 보내기 전에 대기 중인 캡처를 처리한다.) 이제 `pointercancel` 전에 대기 중인 캡처를 처리하고(`gotpointercapture`), `pointercancel` 은 캡처 대상에 가고, 이어서 캡처가 풀린다(`lostpointercapture`).
시험 `core.DragDropIntegrationTest.testDragStartCancelsThePointer`(수정 전 소스에서는 실패하는 것을 확인): `pointerdown` → `gotpointercapture` → `pointercancel` → `pointerout`/`pointerleave` → `lostpointercapture`, 끌기 뒤 `pointerup` 도 마우스 이벤트도 없고,
취소된 `dragstart` 는 `pointercancel` 없이 `pointerup` 까지 이어진다.

### EventSource 경계 사례 점검

`docs/progress/webapi2.md` 1절의 "확인하지 못한 것"을 다시 읽고 기존 시험(`EventSourceTest` 14, `EventSourceIntegrationTest` 6)이 이미 덮는 것을 확인했다: `retry`(재연결 시간 유지/변경), `Last-Event-ID`(전송, 빈 `id:` 의 초기화), HTTP 204 와 다른 상태/잘못된 MIME 의 실패(재시도 없음),
네트워크 오류의 재연결, CORS, 열린 스트림 6개 한도, 이동/폐기 때의 취소. 없던 것은 **리디렉션**이어서 `testRedirectedStreams` 를 더했다: 같은 오리진 리디렉션을 따라가고 이벤트의 `origin` 이 최종 URL 의 오리진이며(CORS 를 허락하는 다른 오리진으로의 리디렉션 포함),
재연결은 소스의 원래 주소를 다시 묻고, 허용되지 않은 스킴(`ftp:`)으로의 리디렉션은 네트워크 오류이므로 소스가 다시 시도한다(CONNECTING; 규격의 "reestablish"). 새로 드러난 결함은 없었다.

### 변경 파일

- 새 파일: `src/org/duckdns/hjow/swingwebpanel/media/vtt/{package-info,VttRegion,VttCue,VttFile,VttTimestamps,VttParser,VttNode,VttCueText,VttEntities}.java`, `src/org/duckdns/hjow/swingwebpanel/dom/TrackEvent.java`,
  `src/org/duckdns/hjow/swingwebpanel/js/host/{MediaTracks,TextTracks,TextTrackBindings,TextTrackValue,TextTrackListValue,TextTrackCueValue,TextTrackCueListValue,VttRegionValue}.java`.
  시험: `tests/.../media/vtt/{VttParserTest,VttCueTextTest}.java`, `tests/.../js/host/TextTrackTest.java`, `tests/.../core/TextTrackIntegrationTest.java`.
- 소유 경로 밖의 가산 변경: `js/host/MediaElements.java`(고정 작업, `queueTask`, `pauseNow`, `tracksOf`, 위치 변화 훅), `js/host/PageScripts.java`(`isParsingDocument`, 파싱 종료 훅), `js/host/ElementExtras.java`(설치 한 줄),
  `js/host/ElementStates.java`(트랙 모양 객체 제거), `js/dom/DomInterfaces.java`(`TrackEvent` 분류), `core/PageInput.java`(끌기 시작의 포인터 캡처), 시험 틀 `tests/.../js/host/PageFixture.java`(`web` 요청 서비스), `ElementStatesTest`(`kind` 의 잘못된 값은 metadata),
  `DragDropIntegrationTest`, `EventSourceIntegrationTest`(새 시험 각 1건).
- 문서: 이 절, `docs/FEATURE_MATRIX.md`(VID-02, HEI-08, HTML-08 의 `track` 문구, 11절의 WebVTT 행).

### 검증 (JDK 6u45, 직접 실행)

- `media.vtt.VttParserTest` 7건, `VttCueTextTest` 6건 통과(파일 서명/BOM/UTF-8, 줄 끝, 타임스탬프 정상과 오류 형태 11종, 설정의 정상/오류 18종, region/STYLE/NOTE 블록, cue 텍스트의 태그/클래스/주석/언어/타임스탬프/문자 참조/장 제목).
- `js.host.TextTrackTest` 16건 통과(인터페이스와 생성자, `VTTCue`/`VTTRegion` 의 기본값과 setter 오류, `getCueAsHTML`, 목록 순서와 `addtrack`/`removetrack`, cue 목록 순서와 `addCue`/`removeCue`, 가져오기의 성공과 실패 4종과 `src` 변경, 요청 모드 4종, 자동 선택,
  탐색 뒤의 `enter`/`exit`/`cuechange` 순서, 모드 변경과 새 로드의 초기화, cue 변경, 재생 중 이벤트 순서(놓친 cue 포함), `pauseOnExit`, `change` 이벤트와 트랙 제거, 폐기, video 요소는 구조만).
- `core.TextTrackIntegrationTest` 2건 통과(실제 컴포넌트와 HTTP: `default` 트랙이 저절로 가져와지고 User-Agent/추가 헤더/쿠키가 요청에 실리며 cue 가 재생 중 순서대로 enter/exit, 원문 불변. 다른 오리진의 파일은 `crossorigin` 이 없으면 서버에 닿기 전에 거부되고 CORS 를 허락하는 서버는 로드된다).
- 회귀: `js.host` 패키지 436건 통과(변경 직후), `core.DragDropIntegrationTest` 13건, `core.PointerEventIntegrationTest` 5건, `core.EventSourceIntegrationTest` 7건 통과.
- 소스 규칙: `sh tools/sourcecheck/run.sh <변경 경로>` 0 위반.
- 확인하지 못한 것: 실제 사람이 보는 자막(그리지 않음), 실제 소리 장치에서의 재생 중 이벤트 시점(가짜 출력만), 큰 파일 전체의 화면 성능. 파서만은 20000 cue(1.5 MB)를 JDK 6u45 에서 약 0.2초, 그 cue 텍스트 파싱은 약 0.09초에 처리하는 것을 임시 프로그램으로 쟀다(전체 회귀가 동시에 도는 중의 측정이며 시험으로 두지 않았다).

### 조정자 요청 (WEBAPI-C)

1. **체크 후보**: **P20-13** — 이 문서 3~8절(WEBAPI-B)과 10절(WEBAPI-C)로 AGENTS.md P20-13 의 입력 목록(`EventSource`, `PointerEvent`, 페이지 수명 이벤트, `IntersectionObserver`/`ResizeObserver`, XPath, 텍스트 트랙/WebVTT, `performance`, 인터페이스 객체 이름, 키보드 `code`/`isComposing`)이
   모두 구현되고 JRE 6 시험으로 검증되었다. 남은 한계(자막을 그리지 않음 등)는 기능 표 VID-02/HEI-08 에 있다. 전체 회귀 결과는 아래 "최종 검증"의 줄을 확인한 뒤 체크할 것.
2. **FEATURE_MATRIX**: 내가 고친 행: VID-02(텍스트 트랙 구현, 한계), HEI-08(`HTMLTrackElement` 와 텍스트 트랙 API 구현), HTML-08 의 `track` 문구, 11절의 "WebVTT와 텍스트 트랙" 행(미구현 → 구현함). 13절의 개수표와 "축별로 남은 행"(미디어 행의 `VID-02` 는 여전히 부분 구현)은 고치지 않았다.
3. **ARCHITECTURE 문구 후보**: 패키지 `media.vtt`(WebVTT 파일/cue 텍스트 파서, DOM 과 무관), 클래스 `js.host.{TextTracks,TextTrackBindings,MediaTracks,TextTrackValue,TextTrackListValue,TextTrackCueValue,TextTrackCueListValue,VttRegionValue}`, `dom.TrackEvent`.
   `MediaElements` 가 `TextTracks` 를 소유하고 위치 변화/`track` 변경을 알린다(`MediaElements.queueTask` 로 미디어 요소의 작업을 올림).
4. **API_CONTRACT**: 공개 Java API 의 변화는 없다.
5. **PROGRESS 후보**: "포인터 이벤트의 결함 수정"(끌기 시작 때 대기 중인 캡처, 커밋 652)과 "time marches on 규격 문구의 이중 enter 문제와 대처"(10절 ②).
6. `AGENTS.md` P20-13 의 본문 끝 "남은 것: 텍스트 트랙/WebVTT" 문장은 위 체크와 함께 정리해 주세요.

### 최종 검증 (WEBAPI-C, 2026-11-18)

- 명령: `BUILD_DIR=build/full sh test.sh`(JDK 6u45, 처음부터 컴파일, 커밋 `e708c20` 기준) → **3392 통과/0 실패/12 건너뜀**(건너뜀 12 = 선택적 Opus 원본 벡터). 마지막 `===` 줄: `=== 3392 passed, 0 failed, 12 skipped (551078 ms) ===`, 종료 코드 0.
- 그 뒤에 더한 시험(`TextTrackTest` 의 `HTMLTrackElement.kind` 확인과 `testVideoElementHasOnlyTheStructure`)은 `scripts/pkgtest` 와 같은 컴파일 설정의 증분 빌드로 `TextTrackTest` 16건 통과를 확인했다(전체 회귀에는 들어 있지 않다).
- 소스 규칙: `sh tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel tests/src/org/duckdns/hjow/swingwebpanel/js/host tests/src/org/duckdns/hjow/swingwebpanel/core tests/src/org/duckdns/hjow/swingwebpanel/media/vtt docs/progress/webapi2.md docs/FEATURE_MATRIX.md` → `0 violation(s) in 1204 file(s) checked`.
