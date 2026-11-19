# FRAMES 진행 기록 (P20-02 iframe/srcdoc/frameset 브라우징 문맥, P15-04-b-2 의 프레임 부분)

작업자: FRAMES. 브랜치: `worktree-agent-a19ec237e7b4417f7`. 최신 항목이 위에 온다.

## 2026-11-17 단계 (c)~(g) 창 관계, target/이력/window.open, sandbox, frameset, 수명 (이어서 한 작업)

**현재 작업 번호**: P20-02-c ~ P20-02-g (설계 문서의 하위 항목 (c) 창 관계, (d) 이동/이력, (e) sandbox, (f) frameset, (g) 수명)
**완료한 내용** (모두 실제 컴포넌트 + `TestHttpServer`, JDK 6u45):
- (c) 창 관계: `window.parent/top/frames/length/frameElement/name/self/opener`, `frames[i]`/`window[name]`, `iframe.contentWindow/contentDocument`(`frame` 도 같음). 같은 오리진은 상대 문서의 실제 전역 객체이고
  다른 오리진은 `CrossOriginWindow` 프록시(`postMessage`/`location` 쓰기/`closed`/`frames`/`length`/`parent`/`top`/`self`/`window`/`opener`/`focus`/`blur`/인덱스 프레임만 허용, 나머지는 `SecurityError` DOMException).
  이전 작업자의 미커밋 코드를 컴파일해 보니 그대로 동작했다(시험 5건 통과). 추가: 스크립트가 만든 `iframe`/`frame` 은 **스크립트가 문서 스레드에서 실행 중이면 삽입·제거 즉시** 문맥이 생기고 없어진다
  (`FrameSet.mutated` 가 `scan(true)` 를 곧바로 부름; 네트워크가 필요 없는 `about:blank` 는 그 자리에서 문서가 만들어져 `contentWindow.document.open/write/close` 가 된다. `srcdoc` 는 스크립트가 돌아온 뒤 만든다).
  프레임 문서가 **파싱되는 동안**에도 `parent.frames[0]` 같은 참조가 자기 자신을 가리키도록 `FrameContext.parsingStarted` 로 파싱 중인 문서/스크립트 호스트를 문맥이 안다.
- (d) 이동 규칙: `window.open(url, target)` 은 대상이 고르는 기존 문맥(`_self/_parent/_top`, 프레임 이름)을 이동시키고 그 window 를 반환한다(빈 주소는 이동 없이 window 만). 새 창이 필요한 이름(`_blank`, 모르는 이름)과
  allow-popups 가 없는 샌드박스는 `null`(컴포넌트에 창이 없다). 링크/폼 target 은 stage (b) 대로 현재 문맥 재사용. `location`/`history`/`pushState` 는 프레임 지역 이력을 따르고 컴포넌트 이력은 그대로이다.
  프레임 문서가 파싱 중에 한 `pushState` 는 곧바로 지역 이력에 반영되고(`history.length` 가 맞음), 파싱 중의 같은 문서 안 이동(`location.hash`)은 확정될 때까지 기다린다(그러지 않으면 프레임을 다시 읽어 스크립트가 되풀이된다).
  `refresh(true)` 의 force 는 그 페이지의 모든 프레임 문서 요청에 전파된다(`FrameSet.Tree.force`). 컴포넌트가 다른 페이지로 갔다 `back()` 하면 프레임은 마크업(`src`/`srcdoc`)에서 다시 만들어진다(설계 이력 규칙 3).
- (e) sandbox: `allow-scripts`(없으면 스크립트와 이벤트 핸들러 속성이 실행되지 않음), `allow-same-origin`(없으면 불투명 오리진: 부모/저장소/쿠키 접근 `SecurityError`, 부모도 그 문서에 못 닿음), `allow-forms`, `allow-popups`,
  `allow-top-navigation`, 중첩 시 누적, `srcdoc` 오리진 상속(샌드박스가 불투명하게 하지 않는 한), 속성은 프레임이 이동할 때만 읽힘. 새로 한 것: **스크립트가 시작하는 이동에 호출한 문서(entry)의 샌드박스를 적용**한다
  (`PageScripts.entryContext()`: 스레드에서 가장 바깥 작업을 시작한 페이지. `runTask/evaluate/execute/fire` 가 표시). 같은 오리진 window 의 `top.location = ...`(다른 realm 의 setter)도 막힌다(`FrameNavigator.permitted`).
  `document.cookie` 는 샌드박스된(불투명 오리진) **프레임** 문서에서 `SecurityError` 이다(최상위 about:blank/about:text 문서는 평소 규칙). `allow-pointer-lock` 은 토큰으로 인식만 하고 포인터 잠금 API 가 없으므로 효과 없음.
- (f) 과거 `frameset/frame/noframes`: 스펙(HTML5 REC 10.6)의 rows/cols 크기 목록(픽셀, `%`, `*`, `2*`)을 CSS Grid 트랙으로(`css/cascade/FramesetTracks`) 배치하고(UA 스타일시트의 `frameset`/`frame` 규칙, `HtmlPresentationalHints.frameset/frame`),
  중첩 frameset 은 재귀, 경계선은 프레임 안쪽 `border`(두께 `border` 속성 또는 `framespacing`, 기본 6px 의 절반씩; `frameborder` 규칙은 규격의 "has a border" 알고리즘; 색은 `bordercolor` 사슬 → 회색),
  빈 칸은 경계선 색(`background-color`)으로 보이고 칸을 넘는 프레임은 크기 0 칸(`grid-auto-*: 0px`)으로 안 보인다. `noframes` 는 `display:none`(기존). `frame` 은 치환 요소(`layout/Replaced`)이며 `iframe` 과 같은 문맥/창/이동/폼/로드 처리를 쓴다
  (`FrameSet.frameElements` 가 처음부터 `frame` 을 포함). `frame` 의 문서는 흰 바탕(UA `background-color: white`). **컨테이너 프레임 요소의 `marginwidth`/`marginheight`**(frame, iframe 모두)가 본문 여백이 된다
  (HTML5 REC 10.3.2; `Document.getContainerFrame`). `document.body` 가 frameset 을 반환하고 frameset 의 `onload` 등 이벤트 핸들러 속성이 window 의 것으로 동작함은 기존 구현을 시험으로 확인했다.
- (g) 수명: 시험 6건으로 확인(아래). 프레임 있는 페이지의 교체/`clean()`/`dispose()`/프레임 요소 제거 뒤 프레임 문서의 타이머와 요청이 멈추고, 느린 프레임 문서는 취소되어 실행되지 않으며 연결이 닫히고,
  여러 컴포넌트를 쓰고 `dispose()` 한 뒤 약한 참조가 모두 수거되며(GC 시점은 보장하지 않음) 라이브러리 스레드는 늘지 않는다.

**변경 파일**:
- 프레임 영역: `core/{FrameContext,FrameSet,FrameNavigator,TopContext}`, `js/host/{BrowsingContext,FrameApi,CrossOriginWindow}`, `css/cascade/FramesetTracks`(core/frames 에서 이동), 시험 `tests/.../core/frames/{FrameWindowTest,FrameNavigationTest,FrameSandboxTest,FrameFramesetTest,FrameLifecycleTest,FrameLogicTest}`.
- 기존 파일(가산적 최소 수정): `core/{BrowserSession(isDocumentThread),NavigationController(isDocumentThread),PageRuntime(session(), 이력/이동 정책, 샌드박스 검사),ParsingPageLoader(parsingStarted)}`,
  `js/host/{PageScripts(entry 추적),WindowGlobals(cookie 검사),MessagingApi,HostEnvironment}`, `js/dom/DomInterfaces`(frame/frameset 인터페이스), `dom/Document`(containerFrame), `layout/Replaced`("frame"),
  `css/cssom/UaStyleSheet`(frameset/frame 규칙), `css/cascade/HtmlPresentationalHints`(frameset/frame 힌트, body 여백의 컨테이너 프레임).
**검증 결과** (JDK 6u45, 증분 javac `-source 1.6 -target 1.6` + `TestRunner --scan ... --filter`; 이 PC 는 다른 작업자와 CPU/디스크를 나눠 첫 시험은 클래스 로딩에 수십 초 걸린다):
- `org.duckdns.hjow.swingwebpanel.core.frames` 전체: FrameInputTest 8, FrameRenderingTest 10, FrameLogicTest 6, FrameWindowTest 6, FrameNavigationTest 11, FrameSandboxTest 7, FrameFramesetTest 5, FrameLifecycleTest 6 = **59건 통과 / 0 실패**.
- 회귀(같은 빌드): `org.duckdns.hjow.swingwebpanel.css` 148건, `org.duckdns.hjow.swingwebpanel.layout` 268건, `org.duckdns.hjow.swingwebpanel.render` 134건 통과(UA 스타일/힌트/`Replaced` 변경의 영향 확인).
- `sh tools/sourcecheck/run.sh`(core, css, js/host, dom/Document, layout/Replaced, 새 시험) 위반 0.
- **돌리지 않은 것**: `org.duckdns.hjow.swingwebpanel.core`/`js.host`/`js`/`dom`/`html` 전체 회귀(조정자가 병합 전에 `scripts/snaptest.sh` 로 전체 회귀를 돌려야 한다), 실제 Swing 창/AWT 입력 확인.

## 2026-11-17 단계 (b) iframe src/srcdoc 렌더링·로딩·load·진행률·스크롤·입력 좌표, target 이동(링크/폼), 한도

**현재 작업 번호**: P20-02-b (P20-02-d 의 링크/폼 target 이동 부분도 함께 구현)
**완료한 내용** (모두 실제 컴포넌트 + `TestHttpServer` 로 검증):
- `iframe`(src/srcdoc/속성 없음)이 별도 `Document`/스크립트 호스트/표현 상태를 가진 중첩 브라우징 문맥으로 로드·렌더링된다. 콘텐츠 상자(width/height 속성+CSS, 기본 300x150)로 클립하여 그리고,
  프레임 문서가 상자보다 크면 자기 스크롤 오프셋과 **오버레이 스크롤바**(10px, 레이아웃 공간을 차지하지 않음; `scrolling=no/yes/auto`)를 가진다.
- 스크롤 모델: 마우스 휠(그 방향으로 스크롤할 수 있는 가장 안쪽 프레임이 받고, 아니면 바깥 `JScrollPane` 으로), 스크롤바 썸 끌기, 트랙 클릭(90% 쪽), 프래그먼트 이동. 키보드 스크롤은 미지원.
- 입력: 좌표 하강(`PageInput.locate`)으로 클릭/마우스 이벤트가 맞은 프레임 문서의 스크립트 호스트로 그 문서의 좌표(`clientX` 는 프레임 뷰포트 기준)로 전달된다. 호버/`:active` 스타일, 링크 ENTERED/EXITED/ACTIVATED 통지,
  키보드(마지막으로 클릭한 프레임으로 전달; `FrameSet.focusedFrame`)가 프레임 안에서 동작한다.
- 링크 `target`(`_self/_parent/_top/_blank`/이름, `<base target>`)과 폼 `target` 이 `FrameNavigator` 로 같은 이동 정책(`NavigationPolicy`)을 거쳐 프레임/최상위로 간다.
  `_blank`/없는 이름은 새 창이 없으므로 **현재 문맥 재사용**(샌드박스에 allow-popups 가 없으면 차단+상태 문구). 프레임 이동은 컴포넌트 이력에 항목을 더하지 않는다(설계 문서 "이력 규칙").
- 프레임 요소의 `load` 이벤트(자식 문서의 window load + 자식 리소스/중첩 프레임이 끝난 뒤), 부모의 `load`·진행률(`PagePresentation.hasPendingResources/resourceActivity` 에 프레임 포함)이 프레임을 기다린다.
  실패한 프레임은 프레임 안에 오류 문서를 보이고 컴포넌트 페이지/원문/상태는 그대로이다. 다운로드 응답은 컴포넌트 다운로드 처리로 넘기고 프레임은 그대로이다.
- 한도: 깊이 8, 페이지당 프레임 64, 조상과 같은 URL(프래그먼트 제외)로의 프레임 이동은 오류 문서로 취소(자기 자신 임베드가 첫 프레임에서 멈춤).
- DOM 변경 추적: `src`/`srcdoc`/`name` 변경, 삽입된 `iframe`, 제거된 `iframe` 이 문맥을 갱신/생성/폐기한다(문서 큐에서 `FrameSet.scan` 으로 reconcile).
- `Document.originUrl` 추가(`getOriginUrl()`): `about:blank`/`about:srcdoc` 프레임은 부모의 오리진을 물려받고 샌드박스(allow-same-origin 없음)는 불투명 오리진이다. `PageRuntime`/`PagePresentation`/캔버스/메시징/저장소의
  오리진 계산이 이 값을 쓴다(`WindowGlobals.origin()`, `CanvasHostImpl`, `MessagingApi`).
- 프레임 안 스크립트의 `location.assign/replace/href/hash/reload`, `history.pushState/replaceState/go/back/forward/length` 는 **프레임 지역 이력**(`FrameContext`, 최대 64)을 따른다.

**변경 파일**:
- 새 파일: `src/org/duckdns/hjow/swingwebpanel/core/{FrameSet,FrameContext,FrameNavigator}.java`, `core/frames/{FrameLimits,SandboxFlags,FrameTargets,FrameUrls,FramesetTracks}.java`,
  `render/{FramePicture,FrameSource,FrameCommand,FrameScroll}.java`, `js/host/BrowsingContext.java`(단계 c 에서 사용), 시험 `tests/src/org/duckdns/hjow/swingwebpanel/core/frames/{FrameTestSupport,FrameLogicTest,FrameRenderingTest,FrameInputTest}.java`.
  (`FrameSet`/`FrameContext` 는 `PagePresentation`/`PageRuntime` 같은 패키지 전용 클래스를 쓰므로 지시된 `core/frames/` 가 아니라 `core/` 에 두었다. 순수 규칙만 `core/frames/` 에 있다.)
- 기존 파일(가산적 최소 수정): `core/{BrowserSession,HtmlPageDocument,NavigationRequest,PageInput,PageKeyboard,PagePresentation,PageResources,PageRuntime,PageView,ParsingPageLoader}.java`,
  `dom/Document.java`(originUrl), `render/{DisplayCommand,DisplayListBuilder}.java`(`FRAME` 종류와 iframe/frame 분기), `js/host/{CanvasHostImpl,MessagingApi,WindowGlobals}.java`(오리진 주소 한 줄씩).
**검증 결과** (JDK 6u45, 증분 javac `-source 1.6 -target 1.6` + `TestRunner --scan ... --filter`):
- `org.duckdns.hjow.swingwebpanel.core.frames` 24건 통과(FrameLogicTest 6, FrameRenderingTest 10, FrameInputTest 8).
- `org.duckdns.hjow.swingwebpanel.core` 전체 204건 통과 / 0 실패(기존 PageInput/키보드/폼/다운로드/이력 시험 포함 회귀 없음).
- `sh tools/sourcecheck/run.sh` 위반 0(`src/org/duckdns/hjow/swingwebpanel/core`, `render`, 새 시험).
**미해결 사항** (정직하게):
- 프레임 문서의 **파서 차단 외부 스크립트**를 가져오는 동안 컴포넌트의 문서 큐가 막힌다(문서 큐에서 파싱하기 때문; 이동/폐기로 취소됨).
- 프레임 안의 **폼 컨트롤 편집기(Swing 오버레이)**: `ControlOverlays` 가 최상위 렌더링의 좌표만 알아서 프레임 안 텍스트 입력 편집기는 아직 위치를 맞추지 않았다(단계 g 전에 확인·기록).
- 프레임에 `transform`/`opacity` 가 걸린 경우 입력 좌표 변환은 하지 않는다(프레임 요소 자체의 CSS transform 은 hit test 가 역변환하지 않는 한 어긋남).
- 키보드로 프레임 스크롤(화살표/PageUp/Down)과 Tab 으로 프레임 사이 이동은 미지원.
- `iframe.contentWindow` 등 JS 창 관계(단계 c), sandbox 세부(단계 e), frameset(단계 f), 수명 시험(단계 g)은 아직이다.
**다음 작업 번호**: P20-02-c
**조정자 요청**: (아직 없음; 단계 g 에서 ARCHITECTURE 의 프레임 행을 요청한다)
**체크박스 후보**: 없음(P20-02 는 단계 g 까지 끝나야).

## 2026-11-17 단계 (a) 설계 (구현 전)

**현재 작업 번호**: P20-02 (하위 계획은 아래 "하위 항목" 참조)
**완료한 내용**: 설계만 했다. 코드는 아직 없다.

### 조회한 규격 (조회 2026-11-17, `tools/webfetch`, JRE 25 로 실행; JDK 6u45 는 HTTPS 불가)

- HTML5 W3C REC 2014-10-28, `https://www.w3.org/TR/2014/REC-html5-20141028/embedded-content-0.html` 4.7.2 "The iframe element"
  (원문을 `scratchpad/frames-work/embedded-content-0.txt` 로 받아 읽음).
  - `srcdoc` 가 `src` 보다 우선한다. srcdoc 문서의 URL 은 `about:srcdoc`, Content-Type 은 `text/html`.
  - `src` 가 비었거나 해석 실패면 `about:blank`. **조상 브라우징 문맥 중 활성 문서 주소(프래그먼트 제외)가 이 URL 과 같으면 이동을 취소**한다 (재귀 방지 규칙).
  - iframe 이 삽입되면 중첩 브라우징 문맥을 만들고 속성을 처리하며, 제거되면 문맥을 버린다(unload 이벤트 없음). srcdoc/src 변경 시 다시 처리한다.
  - 문서가 "completely loaded" 가 되면 iframe 요소에 `load` 이벤트를 보낸다. src 도 srcdoc 도 없이 만들어지면 만들 때 load 를 보낸다.
  - `name` 은 문맥 이름, `sandbox` 는 **문맥이 이동될 때만 효력**(이미 읽은 문서의 플래그는 속성 제거로 바뀌지 않음). 토큰: `allow-forms allow-pointer-lock allow-popups allow-same-origin allow-scripts allow-top-navigation`.
    sandbox 가 있으면 유일한 오리진, 폼/스크립트/팝업 차단, 다른 브라우징 문맥으로의 링크 차단. 플래그는 아래로 누적된다(조상이 막은 것은 자손의 allow-* 가 되살리지 못함).
- 같은 문서 5.1 "browsing contexts": `about:blank` 초기 문서는 **생성자(부모) 문서의 오리진을 별칭으로** 갖는다; 생성자가 없으면 유일한 오리진.
  5.1.? "rules for choosing a browsing context": 빈 문자열/`_self` = 현재, `_parent` = 부모(없으면 현재), `_top` = 최상위, 그 밖의 이름 = 이름이 같은 *연관된* 문맥,
  없거나 `_blank` 면 새 문맥 요청이며 사용자 에이전트 설정에 따른다("현재 문맥 재사용"도 허용되는 설정). 샌드박스 문서는 새 보조 문맥을 못 연다.
- 프레임셋/프레임(구형): HTML5 REC 에서는 폐기(obsolete)이며 파서 삽입 모드는 이미 구현됨. 크기 목록 규칙(`rows`/`cols`: 픽셀, `%`, `*`)은 HTML 4.01 § 16.2.4 와 같다.
  이 구현은 `frameset` 을 CSS Grid 로 배치한다(`css/cascade/HtmlPresentationalHints`).
- Chromium 참고: `third_party/blink/renderer/core/frame/` 와 `core/html/html_frame_owner_element.cc` 의 구조(프레임 소유 요소가 자식 문맥을 소유)를 개념적으로만 참고했고
  코드는 옮기지 않았다. 리비전은 조회하지 않았다(참고 수준).

### 데이터 모델

한 **브라우징 문맥** = (문서 `Document`, 스크립트 호스트 `PageRuntime`/`JsRealm`/Window, 표현 상태 `PagePresentation`(CSS 엔진/리소스/배치), 수명).
최상위 문맥은 기존 그대로 `HtmlPageDocument`(세션이 확정)이고, 프레임 문맥은 **같은 클래스 `HtmlPageDocument` 를 부모 페이지가 소유**한다.

```
SwingWebPanel → BrowserSession → PageSnapshot(top) → HtmlPageDocument(top) ─ frames: FrameSet ─ FrameContext* ─ content: PageSnapshot → HtmlPageDocument(child) ─ frames: FrameSet …
```

| 클래스 | 위치 | 책임 |
| --- | --- | --- |
| `core.frames.FrameLimits` | 새 파일 | 깊이 8, 페이지 트리당 프레임 64, 기본 크기 300x150, 스크롤바 두께 등 상수 |
| `core.frames.SandboxFlags` | 새 파일 | `sandbox` 속성 해석, 부모 플래그와의 누적(교집합), `allow-*` 질의 |
| `core.frames.FrameTargets` | 새 파일 | `_self/_parent/_top/_blank`/이름 분류(ASCII 대소문자 무시), 이름 유효성 |
| `core.frames.FrameUrls` | 새 파일 | iframe 속성 → 이동 주소(srcdoc 우선, 빈 src, 해석 실패, 프래그먼트 제거 비교) |
| `core.frames.FramesetTracks` | 새 파일 | `rows`/`cols` 목록 → CSS grid 트랙 문자열 |
| `core.FrameSet` | 새 파일 | 문서 하나의 프레임 컨테이너. DOM 변경을 보고 `iframe`/`frame` 요소 ↔ `FrameContext` 를 맞춘다(문서 큐에서 reconcile) |
| `core.FrameContext` | 새 파일 | 중첩 문맥 하나: 요소, 이름, URL, sandbox, 스크롤, 뷰포트, 내용 `PageSnapshot`, 로딩 세대, 취소 토큰, 지역 이력 |
| `core.FrameHits`, `core.FrameFormSink` | 새 파일 | 입력 좌표 하강, 폼 target 라우팅 |
| `render.FrameSource`/`FramePicture`/`FrameCommand` | 새 파일 (`render` 의 생성자가 패키지 전용이라 같은 패키지에 둠) | 렌더러가 부모 DisplayList 를 만들 때 프레임 내용(자식의 불변 `RenderedPage`)을 요청해 한 명령으로 그린다 |
| `js.host.FrameHost`, `js.host.FrameApi` | 새 파일 | JS 쪽: `parent/top/frames/length/frameElement/name`, `contentWindow/contentDocument`, 교차 오리진 창 프록시 |

기존 파일에 가산적으로 손대는 곳(최소): `HtmlPageDocument`(frames 보관/해제), `ParsingPageLoader`(프레임 문서 로딩/파싱 진입점, FrameSet 생성),
`NavigationRequest`(frame 필드), `PageRuntime`(frame 분기: 뷰포트, 이동, 로드 완료 통지), `PagePresentation`(프레임 변경 → 다시 그리기),
`PageResources`(`FrameSource`), `PageInput`/`PageKeyboard`(좌표 하강과 문서 선택), `BrowserSession`(다운로드/이동 도우미 가시성),
`render/DisplayListBuilder`(iframe/frame 분기), `layout/Replaced`(`frame` 을 치환 요소로), `css/cascade/HtmlPresentationalHints`(frameset→grid),
`css/cssom/UaStyleSheet`(frameset/frame 의 display), `js/host/{HostEnvironment,PageScripts,WindowGlobals}`, `js/dom/DomInterfaces`(HTMLFrameElement/FrameSetElement),
`dom/Document`(`originUrl`).

### 스레드/큐 규칙

- 한 컴포넌트의 **모든 문서(최상위와 모든 프레임)는 세션의 단일 문서 큐 스레드가 소유한다**(`BrowserSession.submitDocumentTask`). 프레임 문서의 **파싱과 스크립트 실행도 문서 큐에서** 한다.
  따라서 프레임 사이의 교차 접근(`parent.document`)이 직렬화된다. 네트워크 가져오기와 디코딩(`NetworkPageLoader.load`)만 읽기 스레드에서 한다.
- 최상위 페이지가 **확정되기 전**(로더 스레드가 DOM 을 소유하는 동안)에는 프레임 내용을 파싱/실행하지 않는다. 확정(`HtmlPageDocument.activate`) 뒤 문서 큐에서 `FrameSet.reconcile()`.
  확정 전에도 부모의 `load`/진행률이 "프레임 대기 중"임을 알도록 `FrameSet.hasPending()` 는 프레임 요소가 보이면 true(스레드 안전 플래그).
- 한계(문서화): 프레임 문서가 **파서 차단 외부 스크립트**를 가져오는 동안 그 컴포넌트의 문서 큐가 막힌다(이동/폐기의 취소 토큰으로 끝난다).
- EDT: 불변 `RenderedPage` 만 그린다. 프레임 내용은 부모 DisplayList 안의 `FrameCommand`(자식의 불변 `RenderedPage` 참조 + 스크롤)로 그려진다.

### 렌더링과 스크롤 모델

- 부모 레이아웃은 iframe 을 일반 치환 요소로 배치한다(`width/height` 속성 + CSS, 기본 300x150). 프레임 내용 크기에 의존하지 않는다.
- 부모 DisplayList 를 만들 때(`DisplayListBuilder.paintReplaced`, 문서 큐) `FrameSource.getFrame(iframe, w, h)` 가 자식 `PagePresentation.render(w, h)` 로 **자식을 콘텐츠 상자 크기의 뷰포트로** 배치한 불변 `RenderedPage` 를 돌려준다.
  자식이 바뀌면 자식 표현 상태의 무효화 콜백이 부모 표현 상태에 `frameChanged()`(= 배치는 두고 DisplayList 만 다시 만듦, 캔버스와 같은 경로)를 요청한다.
- **스크롤 모델**: 자식 내용이 뷰포트보다 크면 자식은 자기 스크롤 오프셋(sx, sy)을 가진다. 스크롤바는 **오버레이**(10px, 레이아웃 공간을 차지하지 않음)이며 `scrolling="no"` 면 없고 `yes` 면 항상,
  기본(auto) 은 넘칠 때만 그린다. 마우스 휠(프레임이 그 방향으로 스크롤할 수 있으면 프레임이, 아니면 바깥 `JScrollPane`), 스크롤바 썸 드래그, 프래그먼트 이동, `window.scrollTo/scrollBy` 로 스크롤한다.
  키보드로 프레임 스크롤은 지원하지 않는다(한계).
- 그리기: `FrameCommand` 가 콘텐츠 상자로 클립하고 `translate(x, y)` 후 `PagePainter.paint(g2, 자식 목록, null, sx, sy, 1.0, t)` 를 호출한다(공개 API, `PagePainter` 수정 없음).
- 입력: 부모 `HitResult` 요소가 프레임 요소이고 점이 콘텐츠 상자 안이면 `(x-cx+sx, y-cy+sy)` 로 자식 `RenderedPage.hitTest` 를 재귀한다(`FrameHits`). 이벤트는 점을 맞은 문서의 `PageScripts` 로 전달된다.

### 이력 규칙 (P20-02 가 요구한 결정)

권장 규칙(공동 세션 이력: 프레임 이동이 항목을 더하고 back()/forward() 가 이를 되짚음)은 **채택하지 않는다.** 이유: `History` 의 항목이 `PageSnapshot` 하나(= 최상위 문서 하나)이고
`API_CONTRACT` 5.6/5.7 이 `getHistories()` 를 "확정된 주 문서 이동의 URL 목록"으로 정의하며, 항목 복원이 최상위 URL 재요청에 의존한다. 프레임 상태까지 복원하려면 항목마다 프레임 트리(각 프레임의 URL/POST/원문)를
저장하고 복원 때 프레임별로 되살려야 하며 `BrowserSession.commit`/`History`/`NavigationController` 를 크게 바꿔야 한다. 대신:

1. 프레임 이동(링크 `target`, 폼 `target`, `iframe.src` 변경, 프레임 안의 `location`)은 **컴포넌트의 이력에 항목을 더하지 않고** `getHistories()`/`getHistoryIndex()`/`back()`/`forward()` 의미는 `API_CONTRACT` 그대로이다.
2. 프레임마다 **지역 이력**을 가진다(`FrameContext`): 프레임 안의 `history.length/back()/forward()/go()` 는 이 목록을 따른다(구현 범위는 진행 기록에 정직하게 적음).
3. 최상위 `back()/forward()`/`refresh()` 가 페이지를 다시 만들면 프레임은 **새 문서의 마크업(`src`/`srcdoc`)에서 다시 만들어진다**(프레임 안에서 이동한 상태는 복원하지 않는다).
4. `refresh(true)`(force) 는 프레임 문서 요청에도 캐시 우회를 적용한다(요청 설정이 부모의 `PageRequestContext` 를 물려받는다).

### 이동 대상 규칙

- 대상 선택(HTML5 §5.1): `_self`/빈 이름 = 현재 문맥(링크가 있는 문서의 문맥), `_parent` = 부모(없으면 현재), `_top` = 최상위(최상위 이동은 기존 `BrowserSession.navigateFromPage`),
  그 밖의 이름 = 같은 최상위 트리 안에서 이름이 같은 프레임(자기와 조상 먼저, 그다음 깊이 우선). 없거나 `_blank` 이면 **새 창이 없으므로 현재 문맥을 재사용**(HTML 이 허용하는 설정, 이 구현의 선택이며
  기존 동작과 호환)하되, 샌드박스에 `allow-popups` 가 없으면 **차단**하고 상태 메시지를 낸다.
- 모든 프레임 이동은 `NavigationPolicy.allowed`(허용 스킴, 네트워크 문서에서 classpath 진입 금지)와 `<a download>`/첨부 판정을 최상위 이동과 같은 코드로 거친다.
- 샌드박스 문맥은 자신과 자손만 이동할 수 있고, 최상위는 `allow-top-navigation` 이 있을 때만 이동한다. 폼은 `allow-forms` 가 있어야 제출된다.

### 오리진/창 관계

- 문서 오리진 `Document.originUrl`(새 필드, 없으면 `url`)로 계산한다. `about:blank`/`about:srcdoc` 프레임은 **부모의 originUrl 을 물려받고**, sandbox 이면서 `allow-same-origin` 이 없으면 불투명(`about:blank` → `Origin.of` 불투명).
  `PageRuntime`/`PageResources` 의 `pageUrl`(오리진·Referer 원천)도 `originUrl` 로 바꾼다. 문서의 `baseURI` 는 srcdoc/about:blank 에서 부모의 기준 URL 이다.
- `window.parent/top/frames/length/frameElement/name/self/opener(없음)`; `frames[i]`/`window[i]`/이름 접근은 전역 객체의 설정 가능한 프로퍼티로 프레임 생성/제거 때 정의/삭제한다.
  같은 오리진이면 부모/자식의 실제 전역 객체(다른 `JsRealm`)를 돌려주고, 교차 오리진이면 허용 목록(`postMessage`, `frames`, `length`, `parent`, `top`, `self`, `window`, `closed`, `location`(쓰기)…)만 가진 프록시를
  돌려주며 나머지는 `SecurityError` DOMException 이다. `iframe.contentWindow` 는 항상 프록시/창, `contentDocument` 는 교차 오리진이면 null, `window.frameElement` 는 교차 오리진이면 null.
- realm 이 다른 객체를 직접 쓰는 호출은 같은 문서 큐 스레드이므로 데이터 경합이 없다(스크립트 잠금은 같은 스레드에서 재진입).

### 로딩/진행률/load

- 자식 문서의 window `load` 가 발생하면(스크립트 호스트가 없으면 자식 리소스가 모두 끝나면) iframe 요소에 `load` 이벤트를 보낸다. 부모의 `load` 와 진행률은 프레임이 끝날 때까지 기다린다
  (`PageRuntime.hasPendingResources` 와 `PagePresentation.resourceActivity` 에 프레임 수를 더한다).
- 프레임이 실패하면(404/연결 실패 등) 그 프레임 안에 오류 페이지가 보이고(`NetworkPageLoader` 의 오류 스냅샷을 같은 경로로 파싱) 컴포넌트의 페이지/상태는 바뀌지 않는다. 다운로드 응답이면 프레임은 그대로이고 컴포넌트 다운로드 처리로 넘긴다.
- `X-Frame-Options`/CSP `frame-ancestors` 처리는 이 단계의 범위가 아니다(미구현, 기록만).

### 한도

깊이 8(최상위 0, 프레임 1..8; 그보다 깊으면 로드하지 않고 프레임 안에 오류 문서), 한 페이지 트리의 프레임 수 64, 조상과 같은 URL(프래그먼트 제외)로의 프레임 이동은 규격대로 **취소**하고 오류 문서를 보여 준다(자기 자신을 임베드하는 페이지가 무한 재귀하지 않음).

### 수명

프레임 문맥은 부모 이동의 `CancelToken` 에 훅으로 매달리고 각 프레임의 요청은 프레임별 자식 토큰을 쓴다. 페이지 교체/clean/dispose/요소 제거/프레임 재이동은 `FrameContext.destroy()` 로
요청 취소, 타이머/스크립트(`PageRuntime.close()`), 미디어, 표현 상태(`PagePresentation.retire()`), 자식 프레임을 재귀 해제한다. `HtmlPageDocument.dispose()` 가 `FrameSet.close()` 를 부른다.

### 하위 항목 (P20-02)

- [ ] P20-02-a 설계 문서(이 항목)
- [ ] P20-02-b iframe src/srcdoc 렌더링·로딩·load·진행률·스크롤·입력 좌표
- [ ] P20-02-c 창 관계/오리진/교차 오리진 접근
- [ ] P20-02-d target 이동·이력 규칙·`location`/`src` 변경
- [ ] P20-02-e sandbox
- [ ] P20-02-f frameset/frame/noframes
- [ ] P20-02-g 수명/누수/한도 시험

**변경 파일**: `docs/progress/frames.md` (이 파일).
**검증 결과**: 해당 없음(설계).
**미해결 사항**: 아래 단계에서 갱신.
**다음 작업 번호**: P20-02-b
**조정자 요청**: 없음(아직).
**체크박스 후보**: 없음.
