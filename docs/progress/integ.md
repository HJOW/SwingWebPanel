# INTEG-A 진행 기록 (P09-07/08, P11-03/04 기본, 스타일시트·이미지 로딩 연결)

작업자: INTEG-A. 소유 경로: `src/org/duckdns/hjow/swingwebpanel/core/**`, `src/org/duckdns/hjow/swingwebpanel/SwingWebPanel.java`, `tests/src/org/duckdns/hjow/swingwebpanel/core/**`, `tests/src/org/duckdns/hjow/swingwebpanel/*.java`, `examples/**`, 이 파일.
상태: **P09-07-a/b 완료(2026-11-16).** `core/PageView`와 Swing 연결, CSS/스크롤/폭 변경/DOM 무효화/clean·dispose, `PageResources`의 외부 CSS/import/이미지 비동기 로딩을 구현했다.
JDK 6u45 core+루트 시험 90건 통과에 이어 리소스·캐시 관련 43건 통과. P09-08-a의 PageInput 링크 연결도 완료하여 입력/상태/컴포넌트 35건 통과, 변경 Java 4파일 sourcecheck 0 위반. 정적 예제와 HTTP/classpath 이력(P09-08-b)이 다음이다.
P09-08-b-1도 완료: headless SwingRenderingTest 6건 통과, StaticPageExample bat smoke 통과. b-2는 실제 창은 생성됐지만 Computer Use 캡처가 두 번 timeout하여 미완료. 환경 복구 후 링크/스크롤/닫기를 확인한다.
아래 "현재 상태"와 설계는 이전 중지 시점의 기록이며 최신 변경은 PROGRESS의 P09-07-a 항목과 ARCHITECTURE §15를 기준으로 한다.

## 현재 상태 (검증)

- 내 경로의 소스 변경: 없음(작업 트리 깨끗함).
- `scripts/pkgtest.sh integstop "core net storage dom html css layout render content media download js @root" "core @root"` (JDK 6u45): **85 passed / 0 failed / 0 skipped**(core + 루트 시험, 기반 시설 포함).
- `tools/sourcecheck/run.sh src/org/duckdns/hjow/swingwebpanel/core src/org/duckdns/hjow/swingwebpanel/SwingWebPanel.java tests/src/org/duckdns/hjow/swingwebpanel/core tests/src/org/duckdns/hjow/swingwebpanel/*.java examples`: 0 위반(41 파일).
  참고: `tests/src/org/duckdns/hjow/swingwebpanel` 전체를 검사하면 다른 작업자(MEDIA-AUD-B)의 `media/audio/webm/WebmTestBuilder.java` 등에서 위반 5건이 나온다(내 경로 아님).

## 완료 / 미완료

| 항목 | 상태 |
| --- | --- |
| 1. `PageView` 컨트롤러(문서 스레드 빌드, EDT 전달, 메트릭, paintPage) | **미구현** |
| 2. 스타일시트 로더(`StyleSheetLoader` over `ResourceFetcher`) | **미구현** |
| 3. 이미지/미디어 `ImageSource` + 비동기 디코딩 + 재배치 | **미구현** |
| 4. 마우스/링크(호버, 클릭 → DOM click → HyperlinkEvent → 이동) | **미구현** |
| 5. 시험(정적 페이지, 클릭, 이력, clean/dispose) | **미구현** |
| 6. 예제 `StaticPageExample` | **미구현** |

체크박스 후보: 없음(P09-07, P09-08, P09-09, P11-03, P11-04, P08-02 로딩 모두 미체크 유지).

## 조사한 사실 (재개 시 다시 읽지 않아도 되도록)

- 문서 스레드 실행: `NavigationController.submitDocumentTask(Runnable)`(패키지 비공개). `BrowserSession` 이 `nav` 를 private 으로 가지므로 `BrowserSession` 에 패키지 공개 위임 메서드(예: `boolean submitDocumentTask(Runnable)`)를 추가해야 한다. `PageView` 는 `core` 패키지에 둔다.
- 이동 횟수: `NavigationController.counter`(AtomicInteger)는 `cancelCurrent()` 때도 증가한다. "사용자 리스너가 직접 이동을 시작했는가" 판정에는 `BrowserSession.start(...)` 에서만 증가하는 별도 카운터(`getNavigationCount()`)를 추가해 쓴다.
- `SessionListener.pageConfirmed(oldUrl, snapshot)` 는 EDT 에서 호출된다(`BrowserSession.firePageConfirmed`). `SwingWebPanel.Observer.pageConfirmed` 가 지금은 `revalidate(); repaint();` 만 한다 → 여기서 `PageView.pageConfirmed(snapshot)` 호출.
- `SwingWebPanel`: `paintComponent` → `paintPage(Graphics2D)`(빈 구현, `g2.dispose()` 만), `viewportWidthChanged(int)`(revalidate+repaint), `ViewMetrics metrics`, `getPreferredSize()` 가 `metrics.getMinWidth()/getHeight()` 와 `DEFAULT_WIDTH=640/DEFAULT_HEIGHT=480` 사용, `getScrollableTracksViewportWidth()` 는 `viewport.getWidth() >= metrics.getMinWidth()`. 내부 리스너는 `internalListeners` 집합에 넣으면 dispose 때 제거되지 않는다. `createNetworkLoader()` 가 `ResourceFetcher` 를 지역 변수로 만든다 → 필드로 승격해 `PageView` 와 공유해야 한다(사용자 정의 `PageLoader` 생성자일 때도 뷰용 fetcher 를 만든다).
- 파이프라인: `ParsingPageLoader` 가 `HtmlPageDocument`(`getDocument()` → `dom.Document`)를 `PageSnapshot` 에 붙인다(HTML/Markdown/일반 텍스트). 문서가 없으면(이미지/바이너리 등) 빈 화면.
- CSS: `new CssEngine(Document, StyleSheetLoader, MediaEnvironment)`, `loadStyleSheets()`(문서 스레드, 동기 — `<link>`/`@import` 를 `StyleSheetLoader.load(absoluteUrl)` 로 직렬 호출), `refreshIfNeeded()`, `getResolver().setMediaEnvironment(env)`(뷰포트 크기가 바뀌면 `env.setViewport(w,h)` 후 호출), `dispose()`. `StyleSheetLoader.load(url)` 는 `LoadedStyleSheet(text, finalUrl)` 을 반환하고 실패 시 `IOException`. 바이트 디코딩은 `css.cssom.StyleSheetDecoder.decode(bytes, httpCharset, referrerCharset)`.
- 렌더: `render.PageRenderer.render(doc, cssEngine, imageSource, viewportW, viewportH)` → 불변 `RenderedPage`(`paint(g, clip, scrollX, scrollY)`, `hitTestDocument(x, y)`, `getPreferredWidth/Height()`). 컴포넌트가 JScrollPane 안에 있으므로 컴포넌트 좌표 = 문서 좌표(스크롤 0, 배율 1.0). `HitResult`: `getElement()`, `getLink()`, `getHref()`(절대 URL), `getTextNode()`, `getTextOffset()`. **`getLink()/getHref()` 는 DOM 을 읽으므로 문서 스레드에서 호출한다.**
- 이미지: `render.ImageSource { ImageResource getImage(url); ImageResource getMedia(url); }` — 값싸고 막지 않아야 하며 null/`PENDING` 은 진행 중. `ImageResource.loaded(DecodedImage)/failed(message)/AVAILABLE/PENDING`. 실제 가져오기는 호스트가 시작한다. 디코딩: `media.image.ImageDecoders.decode(bytes, mime, DecodeLimits.DEFAULT)` → `MediaException`(Kind).
- 네트워크: `ResourceRequest(url).setUserAgent(session.getUserAgent()).setExtraHeaders(RequestHeaders)`(세션의 `getHeaders()` Map → `RequestHeaders.addAll`), `.setCookies(cookieStore).setPurpose(RequestPurpose.STYLESHEET|IMAGE).setCancelToken(token).setInitiator(Origin.of(pageUrl)).setReferrer(pageUrl)`; 가져오기 `fetcher.fetch(req)`(캐시 경유). 실패는 `getFailureKind() != NONE` 또는 `isHttpError()`.
- DOM 이벤트: 사용자 에이전트 이벤트는 `EventTarget.dispatchTrustedEvent(new Event("click", true, true))`, 취소 여부는 `event.isDefaultPrevented()`.
- 하이퍼링크: `HyperlinkDispatcher.fire(event)` 는 EDT 에서 호출하면 동기 전달, 아니면 `invokeLater`. `removeHyperlinkEvents()` 는 `links.clear()` 만 하므로 내부 링크 이동과 분리하려면 클릭 처리 경로(내부)가 `links` 와 무관하게 `session.setPage(url)` 을 호출해야 한다.

## 설계 (그대로 구현하면 되는 순서)

1. **`core.PageView`** (public final, 생성자 `(BrowserSession, ResourceFetcher, CookieStore, ViewMetrics, HyperlinkDispatcher, Host)`; `Host` 는 EDT 콜백 `viewChanged()`, `setHandCursor(boolean)`, `scrollToRect(Rectangle)`, `getEventSource()`).
   - EDT → `pageConfirmed(PageSnapshot)`: 세대 번호 증가, 이전 상태의 `CancelToken` 취소, `rendered = null`(낡은 페이지를 그리지 않음) 후 문서 스레드에 `openPage(gen, snapshot)` 제출.
   - 문서 스레드 `openPage`: 세대 확인 → 이전 `CssEngine.dispose()` → `HtmlPageDocument` 에서 `Document` 얻기(없으면 `rendered=null`, 메트릭 0) → 스타일시트 선가져오기(`<link rel~=stylesheet href>` 를 작업 스레드 풀에서 병렬 시작, `StyleSheetLoader.load` 는 Future 대기) → `new CssEngine(...)`, `loadStyleSheets()` → `rebuild()`.
   - `rebuild()`(문서 스레드, 세대 확인): `refreshIfNeeded()`, 뷰포트 변경 시 `MediaEnvironment` 갱신, `PageRenderer.render(...)`, 한 번 더 세대 확인 후 `volatile rendered` 교체, `invokeLater` 로 `metrics.setContentSize(contentWidth > viewportW ? contentWidth : 0, contentHeight)` + `host.viewChanged()`. `requestRebuild()` 는 `AtomicBoolean` 으로 합친다(이미지/스타일 도착, 폭 변경, DOM 변경).
   - 폭 변경 루프 방지: 레이아웃 폭은 JViewport 의 extent 폭(부모가 뷰포트가 아니면 컴포넌트 폭, 0 이면 기본 640×480), 같은 폭이면 재빌드하지 않는다. 좁아지면 높아지는 단조성 때문에 진동은 없지만, 최근 폭 두 개와 시각을 보관해 ABA 가 감지되면 건너뛴다.
   - 예외: `render` 에서 Throwable 이면 `rendered=null` 로 두고 uncaught handler 로 보고(낡은 페이지를 남기지 않음).
   - `paint(Graphics2D g)`: `rendered != null` 이면 `rendered.paint(g, g.getClipBounds(), 0, 0)`.
   - `dispose()`: 세대 무효화, 토큰 취소, 작업 스레드 풀 `shutdownNow`, `CssEngine.dispose`.
2. **스타일시트 로더**: `PageView` 내부 클래스 `FetchingStyleSheetLoader`(위 요청 규칙, 목적 STYLESHEET, 페이지 토큰으로 취소, `StyleSheetDecoder.decode`, 실패 시 `IOException`).
3. **이미지**: `PageImages implements ImageSource`(페이지별 `ConcurrentHashMap<String, ImageResource>`, 미요청이면 `putIfAbsent(PENDING)` 후 작업 스레드에서 가져오기+디코딩, 완료하면 상태 저장 + `requestRebuild()`). 크기 제한은 `FetchLimits` 메모리 상한과 `DecodeLimits.DEFAULT`. `getMedia(url)`: http/https 는 HEAD 로 접근 확인(오류면 `failed`), ftp/classpath 는 스킴 허용만 확인(오디오 실제 재생·가져오기는 P18-08), 차단 스킴은 `failed("blocked scheme")`. `<video>` 의 미지원 문구는 렌더러가 그린다. 애니메이션은 0번 프레임만 그리고 프레임 시계 연결 지점을 문서화한다.
4. **마우스/링크**: `SwingWebPanel` 이 `internalListeners` 에 `MouseAdapter` 를 등록 → `PageView.mouseMoved/mouseExited/mouseClicked(x, y, button)`.
   - 이동: 최신 좌표만 보관하고 한 번에 하나만 문서 스레드에서 `hitTestDocument` → 링크 href → EDT 로 돌아와 이전과 다르면 EXITED/ENTERED 를 `links` 로 발행하고 손 모양 커서.
   - 클릭(버튼 1): 문서 스레드에서 히트 테스트 → 대상 요소에 `dispatchTrustedEvent(click)` → 취소되지 않았고 링크가 있으면 EDT 로: `count = session.getNavigationCount()` → `links.fire(ACTIVATED)`(동기) → 사용자가 새 이동을 시작하지 않았으면 허용 스킴(http/https/ftp/classpath)만 `session.setPage(url)`(그 밖은 무시, 이벤트는 이미 전달됨). `#fragment` 만 다른 같은 문서 링크는 이동 없이 요소로 스크롤(이력 항목은 P19-02). `<a download>`/첨부는 기존 다운로드 판정(P11-09 핸들러 설치는 SwingWebPanel 통합 때 `SwingDownloadHandler` 를 `session.setDownloadHandler` 로 설치 — docs/progress/content-dl.md 참조).
   - URL 객체: `new URL(href)` 실패(특수 주소)면 `HyperlinkEvent` 의 URL 은 null, 설명 문자열은 href.
5. **시험**: `TestHttpServer` 로 CSS+이미지+링크 페이지를 제공하고 `BufferedImage` 에 `paintComponent` 경로로 그려 픽셀 확인(배경색, 이미지 영역, 깨진 이미지 대체 문구 영역), `preferredSize` 변화, 폭 변경 후 한 번만 재배치, 합성 `MouseEvent` 로 클릭 → 이벤트 순서(DOM click → ACTIVATED → 이동), `preventDefault` 로 취소, 뒤/앞/새로고침 내용, `clean()` 뒤 빈 화면, `dispose()` 뒤 스레드 종료, 오래된 페이지가 이동 뒤 다시 그려지지 않음, 빌드가 EDT 에서 일어나지 않음(스레드 이름 단언).
6. **예제** `examples/src/org/duckdns/hjow/swingwebpanel/examples/StaticPageExample.java`: JScrollPane + `--smoke`(헤드리스에서 `classpath://` 페이지를 읽고 0 종료).

## 재개 방법

1. `git status` 로 내 경로가 깨끗한지 확인하고 `scripts/pkgtest.sh integ "core net storage dom html css layout render content media download js @root" "core @root"` 가 통과하는지 먼저 확인한다(중지 시점 85건 통과).
2. 위 설계 1번부터 순서대로 구현하고 조각마다 commit 한다(번호는 `git log --format=%s` 로 확인, 내 경로만 `git add`).
3. `BrowserSession` 에 (a) 문서 스레드 제출 위임, (b) `getNavigationCount()` 를 추가하고, `SwingWebPanel` 의 `createNetworkLoader()` 에서 만든 `ResourceFetcher` 를 필드로 승격한다.
4. JavaScript(`PageScripts`) 연결은 INTEG-B 의 일이며 수명 이음매는 다음 세 곳이다: (a) `ParsingPageLoader` 가 파서를 돌리기 **전**에 `PageScripts` 를 만들어 `createParserListener()` 를 연결하고 파싱 뒤 `completeParsing()`(docs/progress/jsdom.md), (b) `PageView.openPage`(문서 스레드, 페이지 확정 직후)에서 스타일/레이아웃 준비를 알리는 `HostEnvironment` 구현(뷰포트, 요소 좌표, 다시 그리기 요청 = `PageView.requestRebuild()`), (c) 페이지 교체/`clean()`/`dispose()` 에서 `PageScripts.dispose()`.
5. 한계로 남기는 것: 확대/축소(배율 1.0 고정), 키보드 포커스·폼(P11 후속), `target`/수정키 처리(기본 이동만), 프래그먼트 링크의 이력 항목(P19-02), 애니메이션 프레임 시계, 오디오 실제 재생(P18-08).
