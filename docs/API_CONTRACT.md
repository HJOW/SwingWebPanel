# SwingWebPanel 공개 API 계약

작성: 2026-11-16 (P01-01 ~ P01-19). 구현은 이 계약을 따르며, 계약을 바꿀 때는 이 문서와 `FEATURE_MATRIX.md` 를 함께 갱신한다.
검증 사례 ID(`C-xxx`)는 `browser/src/test/java`의 사례 이름과 1:1로 연결한다.

## 1. 클래스와 스레드 계약 (P01-01, P01-03, P01-04)

```java
package org.duckdns.hjow.swingwebpanel;
public class SwingWebPanel extends javax.swing.JPanel implements javax.swing.Scrollable { ... }
```

- 호출자는 `new JScrollPane(panel)` 로 감싼다. 컴포넌트 내부에 별도의 스크롤 창을 만들지 않는다.
- 렌더링 부품과 페이지 상태는 `core`/`render` 의 별도 클래스가 담당하며 `SwingWebPanel` 은 얇은 위임 계층이다.
- **모든 공개 메서드는 EDT 와 일반 스레드에서 호출할 수 있다.** 입력 검증은 호출 스레드에서 즉시 수행한다(오류는 호출자에게 예외로 전달).
  화면 상태 변경은 EDT 에 요청한다. 네트워크와 긴 파싱/스크립트 실행은 EDT 를 막지 않는다.
- 조회 메서드는 마지막 **확정** 상태의 복사본 또는 불변 값을 반환한다. 반환된 컬렉션을 수정해도 내부 상태에 영향이 없다.
- 반환형: 이동/콘텐츠 설정 메서드는 `void`, URL/원문/상태/쿠키/User-Agent 조회는 `String`, 진행률은 `int`,
  이력은 `java.util.Vector<String>`, 헤더는 `java.util.Map<String,String>`, 저장소는 `java.util.Collection<org.duckdns.hjow.swingwebpanel.storage.StorageEntry>`.
- `StorageEntry`: `getOrigin()`, `getKey()`, `getValue()` 를 가진 읽기 전용 불변 객체.

## 2. 메서드 요약표

| 메서드 | 입력 | 반환 | 예외 | 요약 |
| --- | --- | --- | --- | --- |
| `setUserAgent(String)` | 비어 있지 않은 문자열 | void | IAE(null/빈/CR·LF), ISE(폐기) | 이후 요청부터 적용 |
| `getUserAgent()` | - | String | - | 기본 `swingwebpanel/1.0` |
| `setPage(String)` | 절대 URL | void | IAE, ISE | 비동기 이동 |
| `getPage()` | - | String | - | 확정된 현재 URL |
| `setJavascriptEnabled(boolean)` | - | void | ISE | 다음 콘텐츠 설정/이동/새로고침부터 효과 |
| `back()`/`back(int)` | n ≥ 1 | void | ISE | 이력 뒤로 |
| `forward()`/`forward(int)` | n ≥ 1 | void | ISE | 이력 앞으로 |
| `getHistories()` | - | `Vector<String>` | - | 이력 URL 복사본 |
| `refresh()`/`refresh(boolean)` | force | void | ISE | 새로고침 |
| `getContent()` | - | String | - | 수신 원문 |
| `setContent(String,String)` | 원문, MIME | void | IAE, ISE | `about:text` |
| `getStatusText()` | - | String | - | 상태바 메시지 |
| `getProgress()` | - | int | - | 0~100, 불확정 -1 |
| `addHyperlinkListener`/`removeHyperlinkListener` | 리스너 | void | IAE(null) | Swing 규칙 |
| `removeHyperlinkEvents()` | - | void | - | 모든 하이퍼링크 리스너 제거 |
| `addConsoleListener`/`removeConsoleListener`/`getConsoleListeners()` | 리스너 | void / 배열 | IAE(null), ISE(폐기, 등록만) | 콘솔 출력 통지(확장, 5.20) |
| `postMessage(Object[,String])`, `addWebMessageListener`/`removeWebMessageListener`/`getWebMessageListeners()` | 메시지와 대상 오리진, 리스너 | void / 배열 | IAE, ISE | Java 호스트와 페이지 스크립트 간 `message` 통신(5.22, C-MESSAGE-1~8) |
| `setRootWindowClosingEnabled(boolean)`/`isRootWindowClosingEnabled()` | 허용 여부 | void / boolean | ISE(폐기, 변경만) | 본 창의 `window.close()` 허용(확장, 5.23). 허용하면 기본 동작은 `System.exit(0)` |
| `new SwingWebPanel(CookieJar)`, `getCookieJar()`, `clearCookies()`, `CookieJar` | 공유 쿠키 저장소 | - / `CookieJar` / void | IAE(null 저장소), ISE(폐기, `clearCookies`) | 컴포넌트 사이의 쿠키 공유와 쿠키 삭제(확장, 5.24) |
| `copy()`, `cut()`, `paste()`, `selectAll()` | - | void | ISE(폐기) | 호스트의 편집 명령(확장, 5.26) |
| `setDefaultFontFamily(String,String)`/`getDefaultFontFamily(String)`, `setDefaultFontSize(int)`/`getDefaultFontSize()`, `setDefaultMonospaceFontSize(int)`/`getDefaultMonospaceFontSize()` | 종류와 패밀리, 크기 | void / String / int | IAE, ISE(폐기, 변경만) | 컴포넌트별 기본 글꼴(확장, 5.27) |
| `setPopupBlockingEnabled(boolean)`/`isPopupBlockingEnabled()`, `disposeKeepingStorage()`, `WindowRequest`, `PopupSeed`, 보호 메서드 `createPopupPanel`·`canOpenPopupWindows`·`openPopupWindow`·`openNewWindow`·`popupBlocked`·`closeOpenedWindow` | 차단 여부, 창 요청 | void / boolean | ISE(폐기, 변경만) | 페이지가 여는 창(팝업)과 팝업 차단(확장, 5.28) |
| `clean()` | - | void | ISE | 빈 페이지 + 캐시/저장소 삭제 |
| `dispose()` | - | void | - | 반복 호출 안전 |
| `addHeaders(Map<String,String>)` | 헤더 | void | IAE | 덮어쓰기 |
| `getHeaders()` | - | `Map<String,String>` | - | 복사본(UA 제외) |
| `getCookies()` | - | String | - | `name=value; ...` |
| `getLocalStorage()`/`getSessionStorage()` | - | `Collection<StorageEntry>` | - | 복사본 |

IAE = `IllegalArgumentException`, ISE = `IllegalStateException`.
부가 메서드(요구 목록 밖, 편의 확장): `getTitle()`, `getHistoryIndex()`, `getMimeType()`, `isJavascriptEnabled()`, `stop()`.
`stop()` 은 진행 중 로딩을 취소한다. `getTitle()` 은 살아 있는 문서의 제목을 따른다(5.25). 이 확장은 필수 메서드의 동작을 바꾸지 않는다.

## 3. 생성 직후 초기 상태 (P01-02)

| 항목 | 값 |
| --- | --- |
| `getPage()` | `about:blank` |
| `getContent()` | `""` (빈 문자열) |
| `getHistories()` | `["about:blank"]` (원소 1개), `getHistoryIndex()` = 0 |
| `getUserAgent()` | `swingwebpanel/1.0` |
| `getHeaders()` | 빈 맵 |
| `getCookies()` | `""` |
| JavaScript | **활성**(`true`). 사용자 문서에도 기록한다 |
| `isRootWindowClosingEnabled()` | `false`(본 창의 `window.close()` 는 무시한다, 5.23) |
| 기본 글꼴(5.27) | 패밀리 네 종류 모두 `""`(내장 글꼴), `getDefaultFontSize()` = 16, `getDefaultMonospaceFontSize()` = 0 |
| `isPopupBlockingEnabled()` | `true`(사용자의 조작 없이 부른 `window.open` 은 차단한다, 5.28) |
| `getProgress()` | 100 |
| `getStatusText()` | `""` |
| `getLocalStorage()`/`getSessionStorage()` | 빈 컬렉션(저장된 것이 없을 때) |

## 4. 속성 변경 통지 (P03-06)

`PropertyChangeListener` 로 `"page"`(String), `"statusText"`(String), `"progress"`(Integer), `"title"`(String), `"rootWindowClosingEnabled"`(Boolean, 5.23), `"defaultFonts"`(String, 5.27), `"popupBlockingEnabled"`(Boolean, 5.28)를 EDT 에서 통지한다.
값이 실제로 달라졌을 때만 통지한다. `"title"` 은 새 페이지가 확정될 때와, 스크립트가 문서의 제목을 바꿀 때 통지한다(5.25).

## 5. 메서드별 상세

### 5.1 setUserAgent / getUserAgent (C-UA-*)

- `setUserAgent(null)`, 빈 문자열(공백만 포함 포함), CR/LF 포함은 `IllegalArgumentException`. (P01-17: 빈 User-Agent 거부)
- 설정 후 이후 모든 요청(주 문서, 이미지, 오디오, CSS, 스크립트, 다운로드)의 `User-Agent` 헤더와 JavaScript `navigator.userAgent` 에 반영한다.
  이미 진행 중인 요청은 바꾸지 않는다.
- 호출하지 않으면 `swingwebpanel/1.0` 이다. `getUserAgent()` 는 마지막으로 설정한 값을 그대로 반환한다.
- `addHeaders` 로는 변경할 수 없다(5.15).

### 5.2 setPage(String url) (C-PAGE-*) (P01-05, P01-12, P01-13)

- **비동기**이다. 호출은 즉시 반환하고 이동 요청 번호를 올린다. 이전 진행 중 요청은 취소하고 늦게 도착한 완료는 폐기한다.
- 허용 입력:
  - `http://`, `https://`, `ftp://` (대소문자 무관한 스킴). FTP 는 익명 접속만 한다.
  - `classpath://경로` : 클래스패스 루트 기준 리소스. 스킴 뒤 전체가 경로이며 선행 `/` 는 무시한다(`classpath://a/b.html` = `a/b.html`).
  - `file:` 주소는 OS의 로컬 파일을 연다. Java에서는 `new File(path).toURI().toASCIIString()`으로 주소를 만들 수 있다.
  - `blob:` 주소는 이 컴포넌트에서 생성한 객체 URL을 연다. 해제했거나 등록되지 않은 객체의 이동은 오류 페이지를 표시한다.
    공백과 퍼센트 인코딩(`%20`)을 정규화하고, `..`/`.` 세그먼트는 정규화하되 루트 위로 나갈 수 없다. 클래스 로더의 스트림으로만 읽는다(파일 경로 변환 없음).
  - `about:blank` : 빈 페이지.
- `setPage("about:text")` 는 거부한다(`IllegalArgumentException`). `about:text` 는 `setContent` 로 만들어진 상태를 가리키는 내부 값이다.
- 거부 대상(`IllegalArgumentException`): `null`, 빈 문자열, 공백뿐인 문자열, 제어 문자 포함, 스킴이 없는 상대 URL, 파싱할 수 없는 URL.
- **구문은 올바르지만 허용되지 않는 스킴**(`jar`, `mailto`, `javascript`, `data`, `ws` 등)은 서버에 연결하지 않고 "지원하지 않는 주소" **오류 페이지로 확정**한다(이력에 기록, 상태 코드 표시 400).
- 네트워크 실패(DNS, 연결, 제한 시간, TLS)와 HTTP 4xx/5xx, FTP 실패, 클래스패스 누락은 오류 페이지로 확정한다(예외 아님).
- 로딩 중 `getPage()`/`getContent()`/`getHistories()` 는 **이전 확정 페이지**를 반환한다. 새 페이지가 확정되면 URL/원문/문서/이력 위치를 한 번에 교체한다.
- 프래그먼트만 다른 같은 문서 이동(`setPage(현재 주소 + "#조각")` 포함)은 문서를 다시 받지 않고 스크롤만 이동하며, 이력에 항목을 추가하고 `hashchange` 를 발생시킨다. 이때 이전 페이지의 언로드 이벤트는 없다. 현재 주소와 완전히 같은 주소는 별도 항목으로 다시 읽는다(WEBAPI-B 가 구현을 이 계약에 맞췄다: `PageLifecycleIntegrationTest`).
- 상대 URL 해석: `<base>` 가 있으면 그 기준, 없으면 문서 URL 기준. `about:text`/`about:blank` 문서에서 `<base>` 없는 상대 주소는 해석 실패로 처리한다.

### 5.3 getPage() (C-PAGE-GET-*)

확정된 현재 URL. 리디렉션되었으면 최종 URL, `setContent` 로 채웠으면 `about:text`, 빈 페이지는 `about:blank`.
프래그먼트가 있으면 포함한다.

### 5.4 setJavascriptEnabled(boolean) (C-JS-*) (P15-10)

- 기본값 `true`. `false` 로 바꾸면 현재 페이지의 script/인라인 핸들러/타이머/후속 script 실행을 중지한다.
  HTML/CSS/링크/기본 폼 기능과 CSS 자체 애니메이션은 계속 동작한다.
- `true` 로 되돌려도 이미 중단된 페이지의 script 는 자동 재실행하지 않는다. **다음 `setContent`/`setPage`/새로고침부터** 정상 실행한다.
- 호스트가 관리하는 페이지에서 초기화에 10초 이상 필요한 경우 `protected long getScriptTaskTimeLimitMillis(String url)`을 재정의할 수 있다. 로더 스레드에서 최상위 문서의 최종 URL로 한 번 호출한다. 0 이하이면 기본 10초를 사용한다. 이 문서의 Worker와 AudioWorklet은 문서의 시간 한도를 사용하고, 하위 프레임은 기본값을 사용한다. 단계 제한과 이동·폐기 취소도 적용한다.

### 5.5 back / forward (C-HIST-*) (P01-07, P01-08)

- `back()` = `back(1)`, `forward()` = `forward(1)`.
- `n <= 0` 이거나 **정확히 n 칸을 이동할 수 없으면 아무 동작도 하지 않는다**. 가능한 범위로 n 을 보정하지 않는다.
- 이동은 새 이력 항목을 추가하지 않으며 현재 위치만 옮긴다. 이동 후의 로딩은 비동기이고 `setPage` 와 같은 확정 규칙을 따른다.
- 이력 항목은 URL, 원문, MIME, 기준 주소를 함께 보관해 복원한다(`about:text` 도 원문으로 복원). 일반 URL 은 캐시 정책을 따르며 캐시가 없으면 다시 요청한다.

### 5.6 이력 규칙 (P01-07)

- 기록 대상: 확정된 **주 문서** 이동(`setPage`, 링크, 폼, 스크립트 이동, `setContent`). 리디렉션은 **최종 URL 한 건**, 하위 리소스 요청은 이력에서 제외한다.
- 뒤로 간 뒤 새 이동을 하면 앞쪽(forward) 이력을 삭제한다.
- 연속된 동일 URL 이동도 별도 항목으로 보관한다(프래그먼트 이동 포함).
- 새로고침(`refresh`)과 `back`/`forward` 는 항목을 추가하지 않는다.
- 오류 페이지로 확정된 이동도 이력에 기록한다. 오류 페이지에서 새로고침하면 같은 URL 을 다시 요청한다.
- `history.pushState` 는 요청 없이 새 항목(URL 과 직렬화한 상태)을 추가하고 앞쪽 항목을 지우며, `replaceState` 는 현재 항목을 바꾼다(P20-07). 이 항목들도 `getHistories()` 에 URL 로 보이고 뒤/앞 이동이 `popstate` 와 `hashchange` 를 보낸다. 폼 POST 는 pushState 항목에서 잊는다(HTML5 2014 5.5.2).

### 5.7 getHistories() (C-HIST-GET-*)

전체 이력 URL 의 복사본 `Vector<String>`. 마지막 원소가 최신. `setContent` 항목은 `about:text`.
반환 컬렉션 수정은 내부 상태에 영향이 없다. `getHistoryIndex()`(확장)는 현재 위치(0 기반).

### 5.8 refresh / refresh(boolean force) (C-REFRESH-*) (P01-09)

- `refresh()` = `refresh(false)`: 현재 원문/리소스의 캐시 정책을 따른다(조건부 요청 포함).
- `refresh(true)`: 주 문서와 그 이동에서 요청하는 **하위 리소스의 캐시를 우회**한다. 이 컴포넌트의 이동 요청에만 적용하며 다른 컴포넌트의 캐시를 삭제하지 않는다.
- `about:text` 는 저장한 원문/MIME 으로 다시 구성한다. `about:blank` 는 빈 상태를 유지한다.
- 새로고침은 이력 항목을 추가하지 않는다.
- **폼 제출(POST)이 만든 페이지의 정책(P19-07)**: 응답한 페이지는 제출(`core.PostData`: 콘텐츠 타입, 본문 4 MiB 까지, 폼이 있던 페이지의 주소)을 이력 항목과 함께 기억한다.
  - 이력 이동(`back`/`forward`)은 **폼을 다시 보내지 않고** 항목에 저장한 수신 원문으로 페이지를 다시 만든다(`getContent()` 도 그 원문). 하위 리소스는 일반 규칙으로 가져온다.
  - `refresh()`/`refresh(true)` 는 자동으로 다시 보내지 않고 `protected boolean SwingWebPanel.confirmResubmit(String url)`(기본: EDT 의 확인 대화상자, 헤드리스는 아니오)에 묻는다. 예이면 같은 본문으로 다시 보내고 응답이 **현재 이력 항목을 교체**한다(항목 추가 없음). 아니오이거나 확인기가 없으면 아무것도 보내지 않고 상태 메시지만 바뀐다. 본문이 4 MiB 를 넘어 보관하지 않았으면 다시 보낼 수 없다.
  - 리디렉션(301/302/303)을 거친 응답은 GET 으로 바뀌므로 일반 페이지이며 새로고침에 묻지 않는다.
  - POST 의 응답이 파일 저장 대상(첨부, 보여 줄 수 없는 형식)이면 첫 응답은 버리고 같은 확인을 거친 뒤 같은 본문으로 다시 요청해 저장한다(`DownloadRequest` 가 본문을 가진다). 확인기가 없거나 아니오이면 저장하지 않는다.
- **폼 제출의 이동(P11-06)**: `GET` 폼은 쿼리가 붙은 주소로의 일반 이동(이력 항목 추가), `POST` 폼은 http/https 주소에만 가능하다(그 밖의 스킴은 무시하고 상태 메시지). 링크와 같은 이동 정책이 적용된다(허용 스킴만, 네트워크 페이지에서 클래스패스로는 가지 않음). `target` 은 쓰지 않는다(브라우징 문맥이 하나). JRE 6 의 `HttpURLConnection` 이 `Origin` 헤더를 보낼 수 없으므로 POST 에 `Origin` 헤더가 없다(P20-08-c).


### 5.9 getContent() (C-CONTENT-*) (P01-06)

- 문자셋으로 디코딩한 **수신 원문**을 그대로 반환한다. DOM 변경(스크립트), Markdown 의 HTML 변환, 오류 페이지의 표시용 DOM 으로 값을 바꾸지 않는다.
- `setContent` 로 채운 경우 입력 문자열을 그대로 반환한다.
- HTTP 오류 응답(404/500 등) 본문이 있으면 그 원문을 유지한다. 본문이 없거나 내부에서 생성한 오류 페이지, FTP 디렉터리 목록은 **내부 생성 문서의 원문**을 반환한다.
- `about:blank` 은 `""`.
- 로딩 중에는 이전 확정 페이지의 원문.

### 5.10 setContent(String content, String contentType) (C-SETCONTENT-*) (P01-17)

- `content == null` → `IllegalArgumentException`. 빈 문자열은 허용(빈 문서).
- `contentType == null` → `IllegalArgumentException`. MIME 은 매개변수(`; charset=...`)를 분리하고 **타입 부분을 대소문자 무관**하게 비교한다.
- 지원 MIME: `text/html`, `text/plain`, `text/markdown`, `application/xhtml+xml`. 그 외는 `IllegalArgumentException`(다운로드 대상이 아니다).
- 확정되면 `getPage()` = `about:text`, `getContent()` = 입력 원문, 이력에 항목을 추가한다(원문과 MIME 보관).
- `setContent` 도 비동기 확정 규칙을 따른다(요청 번호 증가, 이전 진행 중 작업 취소). 입력은 호출 시점의 문자열 값을 사용한다.

### 5.11 getStatusText / getProgress (C-STATUS-*) (P01-19)

| 상황 | 상태 메시지(예) | 진행률 |
| --- | --- | --- |
| 초기/완료 | `""` 또는 완료 메시지 | 100 |
| 이동 시작 | `Loading <URL>` | 0 |
| 본문 수신 중, 전체 크기를 모름 | `Loading <URL>` | -1 |
| 본문 수신 중, 크기를 앎 | 상동 | 0~50 (수신 비율) |
| 파싱/하위 리소스 | `Loading resources (n/m)` | 50~95 |
| 문서와 필수 하위 작업의 성공/실패 정리 완료 | `Done` | 100 |
| 오류 페이지 확정 | `Error: ...` 요약 | 100 |

- '연결 성공'만으로 100 을 반환하지 않는다. 하위 리소스 하나의 실패가 로딩 완료를 영원히 막지 않는다(실패를 완료로 센다).
- 진행률은 요청 번호에 속한다. 취소된 요청의 진행률은 반영하지 않는다.
- 상태 메시지에는 링크 위에 마우스를 올렸을 때의 URL 도 표시한다(링크 진입 시 `href`, 이탈 시 복원).

### 5.12 하이퍼링크 리스너 (C-LINK-*) (P01-17, P11-03, P11-04)

- `addHyperlinkListener(null)`/`removeHyperlinkListener(null)` → `IllegalArgumentException`.
- 등록은 Swing 의 일반 동작과 같다: 같은 리스너를 두 번 등록하면 두 번 통지하고, 제거는 한 번에 하나씩 제거한다. 등록하지 않은 리스너 제거는 무시한다.
- 통지: `HyperlinkEvent.EventType.ENTERED/EXITED/ACTIVATED`. EDT 에서 호출한다.
- 기본 포인터 링크 활성화는 문서 큐의 DOM click, EDT의 Swing ACTIVATED 통지, 취소되지 않은 기본 이동 순서다. DOM이 기본 동작을 취소해도 호스트는 ACTIVATED를 관찰할 수 있다.
  DOM 또는 Swing 리스너가 직접 이동/clean/dispose를 요청했으면 이전 링크의 기본 이동은 추가하지 않는다. href는 DOM 전달 후의 값을 사용한다.
  classpath 등 Java URL 처리기가 없는 특수 주소는 null URL과 전체 주소 설명을 전달한다. 금지 스키마도 호스트에 통지하지만 기본 이동은 하지 않는다.
  네트워크 문서의 링크로 애플리케이션 classpath에 진입하는 것도 차단한다. 호스트의 직접 setPage(classpath 주소)는 허용된다.
- `removeHyperlinkEvents()` : 이 객체에 `addHyperlinkListener` 로 등록된 모든 리스너를 제거한다. **브라우저 자체의 링크 이동은 계속 작동한다**(사용자 통지와 내부 이동 처리를 분리).

### 5.13 clean() (C-CLEAN-*) (P01-15)

- 진행 중 작업(요청, 스크립트, 타이머, 미디어, 다운로드 팝업과 다운로드)을 취소하고 `about:blank` 로 전환, **이력을 `about:blank` 하나로 초기화**한다.
- 이 컴포넌트의 모든 **캐시**와 **sessionStorage**, 그리고 **프로세스 공용 localStorage 전체**를 비운다.
- **유지**: User-Agent, 추가 헤더, **쿠키**, JavaScript 설정, 본 창 닫기 허용 설정(5.23), 기본 글꼴(5.27), 팝업 차단 설정(5.28), 사용자 리스너(하이퍼링크·콘솔·웹 메시지 포함). 대기 중인 웹 메시지는 비운다. 예약된 창 닫기(5.23)는 취소한다.
- 공유 localStorage 삭제는 같은 JVM 의 **다른 컴포넌트에도 영향**을 준다(그 컴포넌트의 이후 조회가 빈 값을 반환하고, 같은 오리진의 문서에는 storage 이벤트가 전달된다). 이 영향은 사용자 문서에 명시한다.
- 완료된 다운로드 파일은 지우지 않는다. 이미 폐기된 컴포넌트에서의 호출은 `IllegalStateException`.

### 5.14 dispose() (C-DISPOSE-*) (P01-16)

- 한 번만 자원을 해제한다. **반복 호출은 안전**하게 끝난다(예외 없음).
- `clean()` 과 같은 정리 후, 이 객체에 등록된 모든 사용자 리스너와 자체 생성 이벤트 연결, 타이머, 작업, 미디어, 하위 프레임을 제거한다.
- 폐기 후 **변경 메서드**(`setPage`, `setContent`, `back`, `forward`, `refresh`, `setUserAgent`, `addHeaders`, `setJavascriptEnabled`, `setRootWindowClosingEnabled`, 기본 글꼴 설정(5.27), `setPopupBlockingEnabled`(5.28), `clean`, `postMessage`, 리스너 등록)는 `IllegalStateException`.
  **조회 메서드**는 마지막 빈 상태를 반환한다: `getPage()`=`about:blank`, `getContent()`=`""`, `getHistories()`=`[about:blank]`, `getStatusText()`=`""`, `getProgress()`=100,
  `getHeaders()`=빈 맵, `getCookies()`=`""`, 저장소 조회=빈 컬렉션, `getUserAgent()`=마지막으로 설정한 값, `isRootWindowClosingEnabled()`=마지막으로 설정한 값.
- `disposeKeepingStorage()`(5.28)는 프로세스 공용 localStorage 를 남기는 것만 다르다. 이 컴포넌트가 연 팝업 창은 폐기하지 않는다(기본 팝업 대화 상자는 소유자 창과 함께 닫힌다).
- 제거 대상 표:

| 대상 | dispose 시 | 구분 방법 |
| --- | --- | --- |
| `addHyperlinkListener` 로 등록한 리스너 | 제거 | 내부 목록 |
| `addConsoleListener` 로 등록한 리스너와 전달을 기다리던 콘솔 메시지 | 제거(이후 메시지는 무시) | 내부 목록(`ConsoleDispatcher`) |
| `addWebMessageListener`로 등록한 리스너와 대기 메시지 | 제거 | 내부 목록(`WebMessageDispatcher`) |
| `addPropertyChangeListener` 로 등록한 리스너 | 제거 | `getPropertyChangeListeners()` 중 내부 것 제외 |
| `addMouseListener`/`MouseMotion`/`MouseWheel`/`Key`/`Focus`/`Component`/`Hierarchy`/`InputMethod` 등 사용자가 등록한 AWT 리스너 | 제거 | `getXListeners()` 중 **내부 위임 리스너 집합에 없는 것** |
| 컴포넌트 자신이 등록한 내부 리스너(크기 변경 감시 등) | 제거(자체 정리) | 내부 집합(`internalListeners`) |
| Swing/AWT 클래스가 가진 리스너(UI 위임 계층의 필수 연결) | **유지**(Swing 소유, 사용자 객체를 붙잡지 않음) | 리스너 클래스 이름이 `javax.swing.`, `java.awt.`, `sun.`, `com.sun.` 으로 시작 |
| 타이머/작업/네트워크/오디오 라인/하위 프레임/다운로드 팝업 | 해제 | 수명 토큰 |

### 5.15 addHeaders / getHeaders (C-HEADERS-*) (P01-10)

- `addHeaders(null)` → `IllegalArgumentException`. 맵의 **키/값이 `null` 이거나 `String` 이 아닌 경우**, 키/값에 `CR`/`LF`, 빈 키 → `IllegalArgumentException`(부분 적용 없이 전체 거부).
- 헤더 이름은 **대소문자를 구분하지 않고** 기존 값을 덮어쓴다. 덮어쓸 때 이름은 마지막 입력의 표기를 유지한다. 이미 추가된 다른 헤더는 삭제하지 않는다.
- 어떤 대소문자의 `User-Agent` 도 받지 않고 **조용히 무시**한다. `getHeaders()` 결과에서도 제외한다.
- 요청에는 주 문서/이미지/오디오/CSS/스크립트/다운로드에 동일하게 적용한다. 브라우저가 반드시 관리하는 헤더(`Host`, `Content-Length`, `Cookie` 등)는 요청 계층이 최종 결정한다.
- `getHeaders()` 는 복사본 `Map<String,String>`(삽입 순서 유지).

### 5.16 getCookies() (C-COOKIE-*) (P01-11)

- 쿠키 저장소는 기본적으로 **컴포넌트별**이다(호스트가 `CookieJar` 를 넘겨 여러 컴포넌트가 공유하게 할 수 있다, 5.24). `name=value; name=value` 형식으로 이 저장소의 **만료되지 않은 쿠키 전부**(도메인과 무관)를 반환한다.
- 정렬: 도메인 → 경로 → 이름 순. 같은 이름의 쿠키도 도메인/경로가 다르면 모두 포함한다.
- 비어 있으면 `""`. JavaScript `document.cookie` 는 현재 URL 에 적용되는 쿠키만 반환하고 `HttpOnly` 쿠키는 숨긴다.

### 5.17 getLocalStorage / getSessionStorage (C-STORAGE-*) (P01-14)

- sessionStorage: 컴포넌트별, 오리진별. localStorage: **같은 JVM 프로세스의 컴포넌트가 공유**하되 오리진별로 분리.
- Java 조회 메서드는 각 저장소의 **모든 오리진 항목**을 복사하여 `Collection<StorageEntry>` 로 반환한다(오리진, 키 순). 웹 스크립트에는 현재 오리진 영역만 노출한다.
- 내부 주소의 오리진: `classpath://` 문서는 오리진 `classpath://`(같은 JVM 의 모든 classpath 문서가 공유), `about:text`/`about:blank` 는 불투명 오리진으로 저장소를 사용할 수 없다(`SecurityError`, `localStorage` 접근 시 예외).

### 5.18 확장 메서드: 페이지 확대와 복사 훅 (P11-08, 확장이며 AGENTS.md 의 공개 메서드 목록은 그대로)

- `public double getZoom()`: 페이지 확대 배율(기본 1.0, 범위 0.25~5.0). 어느 스레드에서나 호출할 수 있고 마지막으로 요청한 값을 반환한다. 폐기 뒤에도 마지막 값을 반환한다.
- `public void setZoom(double)`: 범위 밖은 끝값으로 자른다. NaN/무한/0 이하이면 `IllegalArgumentException`, 폐기 뒤에는 `IllegalStateException`. EDT 에서 적용하고(다른 스레드의 호출은 큐에 들어감) 속성 변경 `PROPERTY_ZOOM`("zoom", Double)을 EDT 에서 알린다. 레이아웃은 `폭/배율`, `높이/배율` 의 CSS 뷰포트로 다시 하며(미디어 질의와 `innerWidth` 가 이 값을 본다) 그리기·히트 테스트·스크롤 크기·선택 강조·폼 컨트롤 편집기가 같은 배율을 쓴다. 배율은 페이지 이동과 `clean()` 뒤에도 유지된다. Ctrl+휠과 Ctrl+`+`/`-`/`0` 으로도 바뀐다.
- `protected void copyToClipboard(String plainText, String htmlFragment) throws Exception`: 사용자가 선택을 복사(Ctrl+C/Ctrl+Insert)하고 `copy` 이벤트를 취소하지 않았을 때 EDT 에서 호출된다. 기본은 시스템 클립보드에 `text/plain`(과 있으면 `text/html` 문자열)을 쓴다. 예외는 상태 메시지로만 알린다(헤드리스 포함). 호스트와 시험이 재정의할 수 있다.
- 선택·복사에는 새 공개 메서드가 없다(`window.getSelection()` 이 사용자의 선택). 내부 `PageSelection`/`PageInput` 리스너는 `dispose()` 에서 제거되고 선택 강조와 드래그 상태는 페이지가 닫힐 때 버려진다. 검증 사례: `TextZoomTest`, `TextSelectionIntegrationTest`.

### 5.19 확장 메서드: 붙여넣기 훅과 입력기 질의 (P20-06, 확장이며 AGENTS.md 의 공개 메서드 목록은 그대로)

- `protected Transferable readClipboard() throws Exception`: 사용자가 편집 가능한 곳이나 폼 컨트롤에 붙여넣을 때(Ctrl+V, Shift+Insert, 편집기의 붙여넣기 동작) EDT 에서 호출된다. 기본은 시스템 클립보드를 읽는다. 호스트와 시험이 재정의한다(`copyToClipboard` 와 한 쌍). 예외나 null 은 "붙여넣을 것 없음"이며 예외는 상태 메시지로만 알린다.
- `public InputMethodRequests getInputMethodRequests()`: 한글 등 입력기가 캐럿 위치(후보 창 자리)를 물을 때 답한다. 컴포넌트는 편집 호스트가 키보드 포커스를 가진 동안만 입력기를 켜고 아니면 끈다. 폼 컨트롤은 자기 Swing 편집기가 입력기를 다룬다.
- `dispose()` 의 제거 대상 표 추가: 내부 `InputMethodListener`(`PageComposition`)는 dispose 때 제거되고, 끌기 타이머가 멈추며 진행 중인 끌기는 이벤트 없이 끝난다. 사용자가 등록한 입력기 리스너는 기존 표대로 제거된다. 검증 사례: `EditingInputTest`, `EditingClipboardTest`, `DragDropIntegrationTest`.

### 5.20 확장 메서드: 콘솔 리스너 (P24, 확장이며 AGENTS.md 의 공개 메서드 목록은 그대로)

페이지 스크립트의 `console.log`/`console.error` 등 콘솔 출력을 호스트가 받는다. 공개 타입은 `org.duckdns.hjow.swingwebpanel.ConsoleListener`, `ConsoleEvent` 이고 `SwingWebPanel` 에 세 메서드를 더했다.

| 메서드 | 입력 | 반환 | 예외 | 요약 |
| --- | --- | --- | --- | --- |
| `addConsoleListener(ConsoleListener)` | 리스너 | void | IAE(null), ISE(폐기) | Swing 규칙(같은 리스너를 두 번 등록하면 두 번 통지) |
| `removeConsoleListener(ConsoleListener)` | 리스너 | void | IAE(null) | 등록 하나 제거, 없는 리스너는 무시 |
| `getConsoleListeners()` | - | `ConsoleListener[]` | - | 등록 순서의 복사본, 폐기 뒤에는 빈 배열 |

- **`ConsoleListener`**: `java.util.EventListener` 를 상속하며 메서드는 `consoleMessage(ConsoleEvent)` 하나. **항상 이벤트 디스패치 스레드(EDT)** 에서, 메시지가 쓰인 순서대로 호출한다. 리스너가 던진 예외는 스레드의 처리되지 않은 예외 처리기로 가고 다른 리스너와 페이지에는 영향이 없다.
- **`ConsoleEvent`**(`java.util.EventObject` 상속, `getSource()` = `SwingWebPanel`): 불변이며 문자열/숫자만 가진다(스크립트 객체 참조 없음).

| 접근자 | 값 |
| --- | --- |
| `getSequence()` | 컴포넌트 안의 일련번호(1부터, 번호가 건너뛰면 메시지가 버려진 것) |
| `getTimestamp()` | 메시지를 쓴 시각(epoch 밀리초) |
| `getLevel()` | `LOG`, `INFO`, `WARN`, `ERROR`, `DEBUG` |
| `getKind()` | `CONSOLE_CALL`, `DIALOG`, `UNCAUGHT_EXCEPTION`, `UNHANDLED_REJECTION`, `SCRIPT_ABORTED`, `HOST_ERROR`, `MESSAGES_DROPPED` |
| `getMethod()` | 쓴 메서드 이름(`log`, `table`, `alert` ...), 브라우저의 보고이면 `""` |
| `getMessage()` | 서식 문자열(`%s %d %i %f %o %O %c %%`)을 적용하고 열린 `console.group` 만큼 들여 쓴 문자열. 객체는 스크립트 코드를 실행하지 않고 읽기 쉬운 형태로 나타낸다 |
| `getArguments()` | 서식 적용 전 각 인수의 문자열(읽기 전용 `List<String>`) |
| `getOrigin()` | `DOCUMENT`(주 문서), `FRAME`, `WORKER`, `SHARED_WORKER` |
| `getDocumentUrl()` | 메시지를 쓴 문서(worker 이면 그것을 가진 문서)의 URL. **쓴 시점에 고정**되므로 이동 뒤에도 이전 페이지의 메시지는 이전 페이지를 가리킨다 |
| `getScriptUrl()`, `getLineNumber()`, `getColumnNumber()` | 호출한 스크립트의 URL(인라인은 문서 URL, `eval` 코드는 `eval`)과 스크립트 텍스트 안의 1부터 시작하는 줄/열. 알 수 없으면 `""`/0 |
| `getDroppedCount()` | `MESSAGES_DROPPED` 일 때 버려진 개수, 아니면 0 |

**통지 범위(P24-02)**

| 출처 | `kind` | `level` |
| --- | --- | --- |
| `console.log/info/warn/error/debug`, `dir`, `dirxml`, `table`, `trace`, `group`/`groupCollapsed`(라벨이 있을 때), `count`, `countReset`(없는 카운터는 WARN), `time*`(없거나 중복된 타이머는 WARN) | `CONSOLE_CALL` | 각 메서드의 Console Standard 수준(`log` 계열은 `LOG`) |
| `console.assert` | `CONSOLE_CALL`(**조건이 거짓일 때만**) | `ERROR` |
| 시험용 `print()` | `CONSOLE_CALL`(`method`=`print`) | `LOG` |
| `alert`/`confirm`/`prompt` | `DIALOG` | `INFO`(대화 상자는 뜨지 않으며 `confirm`은 false, `prompt`는 null 로 답한다, WEB-01) |
| 잡히지 않은 스크립트 예외(타이머·이벤트 핸들러·모듈 포함) | `UNCAUGHT_EXCEPTION` | `ERROR`(`console.error` 호출과 `kind` 로 구별) |
| 마이크로태스크가 끝날 때까지 처리기가 없던 거부된 promise | `UNHANDLED_REJECTION` | `ERROR`(제때 `catch` 가 붙은 것은 통지하지 않음) |
| 한도/시간/취소로 중단된 스크립트 | `SCRIPT_ABORTED` | `ERROR` |
| 가져오지 못한 스크립트, 예약하지 못한 작업 | `HOST_ERROR` | `ERROR` |
| 리스너가 느려 버린 메시지의 알림 | `MESSAGES_DROPPED` | `WARN` |

**스레드와 수명(P24-04)**

- 스크립트를 실행하는 스레드는 EDT 를 기다리지 않는다. 메시지는 큐에 넣고 EDT 에서 `ConsoleDispatcher.BATCH_SIZE`(100)개씩 전달한다. 큐에 `MAX_PENDING_EVENTS`(1000)개가 이미 있으면 그 뒤 메시지는 **버리고 개수를 세며**, 큐가 비면 `MESSAGES_DROPPED` 알림 하나가 버린 개수를 알린다.
- **리스너가 하나도 없으면** 이벤트를 만들지 않고 메시지는 버려진다. 이전에는 페이지의 콘솔이 표준 출력/오류에 썼으나 이제는 쓰지 않는다(표준 출력이 필요하면 리스너에서 `System.out.println`).
- 페이지를 떠나면(이동, `clean()`, `dispose()`) 그 페이지의 콘솔은 닫힌다: 아직 도는 타이머/Worker 의 늦은 메시지는 새 페이지의 것으로 나타나지 않고 **버려진다**.
- `setJavascriptEnabled(false)` 이면 스크립트가 실행되지 않으므로 이벤트도 없다(브라우저의 `HOST_ERROR` 도 스크립트가 있어야 나온다).
- `clean()` 은 콘솔 리스너를 **유지**한다. `dispose()` 는 콘솔 리스너와 기다리던 메시지를 **제거**하고 이후 등록은 `IllegalStateException`. `removeHyperlinkEvents()` 는 콘솔 리스너에 영향이 없다.
- 한계: `unhandledrejection`/`rejectionhandled` 이벤트는 아직 없다(콘솔 보고만). `console.trace` 는 호출 스택이 없다(엔진이 스택을 두지 않는다). 줄/열은 호출식의 첫 토큰 위치이며, 기본 함수를 직접 호출한 곳만 안다.
- 검증 사례: `ConsoleEventIntegrationTest`, `js.interp.ConsoleRecordTest`.

### 5.21 확장 메서드: Web Audio 출력 팩터리 (P25-07, 확장이며 AGENTS.md 의 공개 메서드 목록에는 없다)

`protected org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory createWebAudioOutputFactory()` (`SwingWebPanel`)

- 실시간 `AudioContext` 의 출력을 여는 팩터리를 만든다. 기본은 `new SystemOutputFactory()`(Java Sound 기본 믹서)이다. 장치가 없거나 열 수 없으면 컨텍스트는 **소리 없이 실제 속도로** 돌고 스크립트에는 예외가 없다. `null` 을 반환하면 모든 컨텍스트가 소리 없이 돈다.
- 첫 실시간 컨텍스트가 돌기 시작할 때 **한 번** 호출한다(그 컨텍스트의 렌더링 스레드에서). 팩터리의 `open(sampleRate, channels, bufferFrames)` 는 컨텍스트의 **구간**(`resume()` 에서 다음 `suspend()` 까지)마다 그 컨텍스트의 렌더링 스레드에서 불리며 `IOException`/`RuntimeException` 은 "장치 없음"으로 처리된다. 반환한 `AudioOutput.write` 는 장치에 자리가 생길 때까지 막혀도 되지만 `stop()` 이 불리면 곧바로 돌아와야 한다.
- `<audio>` 요소의 `createAudioOutputFactory()` 와는 **독립**이다(두 출력의 성격이 다르다: float·블록 쓰기 대 16비트·비블록 쓰기). 모든 소리를 다른 곳으로 돌리려면 둘 다 재정의한다.
- 공개 면의 변화: `js.host.HostEnvironment.getWebAudioOutputFactory()`(공개 추상 클래스의 새 메서드, 기본 null), `js.host.PageScripts.connectMediaElement(Element, float)`, `media.playback.MediaPlayer.reopenOutput()`, `media.webaudio.MediaElementTap`, `media.webaudio.OutputRenderDriver.restart()`.
- 검증 사례: `WebAudioOutputScriptTest`, `WebAudioOutputComponentTest`(실제 컴포넌트), `OutputRenderDriverTest`, `OutputRenderDriverSwitchTest`, `MediaElementTapTest`, `MediaElementSourceScriptTest`.

### 5.22 Java 호스트와 페이지 스크립트 간 메시지 (P27)

2026-11-26에 확정한 SwingWebPanel 확장 API 계약이다. 구현과 시험은 P27-02~06에서 추적한다.
이 절은 메시지 이벤트·작업·오리진 검사에 WHATWG HTML Living Standard 9.1과 9.3.2~3을, 창 접근에 7.2.1과 7.2.2의 `opener`를 사용한다. `tools/webfetch`로 2026-11-26에 조회한 두 문서의 Last-Modified는 2026-11-25 11:33:24 GMT이다. [Web messaging](https://html.spec.whatwg.org/multipage/web-messaging.html#web-messaging), [창 및 탐색 API](https://html.spec.whatwg.org/multipage/nav-history-apis.html#cross-origin-objects)

**모델**: Java 호스트를 페이지를 연 다른 창처럼 다룬다. 페이지 안에서는 "호스트 창" 객체 하나가 Java 를 나타내며, 최상위 문서의 `window.opener` 와 호스트가 보낸 `message` 이벤트의 `event.source` 가 그 객체다. 그래서 팝업 통신 관용구를 그대로 쓴다.

```javascript
// 페이지 스크립트: Java 에서 받고 답하기
window.addEventListener('message', function (e) {
  if (e.origin !== 'swingwebpanel://host') return;      // SwingWebPanel.HOST_ORIGIN
  e.source.postMessage({ echo: e.data }, e.origin);     // e.source === window.opener
});
// 페이지가 먼저 Java 에 알리기
window.opener.postMessage({ type: 'ready' }, '*');
```

```java
// Java 호스트
panel.addWebMessageListener(new WebMessageListener() {
    public void messageReceived(WebMessageEvent e) {          // EDT
        if (!"https://app.example".equals(e.getOrigin())) return;
        e.reply(Collections.singletonMap("ok", Boolean.TRUE)); // 보낸 창으로 답하기
    }
});
panel.postMessage("hello", "https://app.example");             // 대상 오리진을 정확히 지정
```

| 메서드/타입 | 계약 |
| --- | --- |
| `void postMessage(Object message, String targetOrigin)` | 호출 시점에 설치된 최상위 실행 문서의 작업 큐에 메시지를 보낸다. 아직 새 실행 환경이 설치되지 않았으면 확정 문서를 택한다. `targetOrigin`은 `"*"` 또는 절대 URL이다. `null`·`"/"`·잘못된 주소는 `IllegalArgumentException`, 폐기 후 호출은 `IllegalStateException`이다. |
| `postMessage(Object message)` | 대상 오리진 `"*"` |
| `addWebMessageListener(WebMessageListener)` / `removeWebMessageListener(WebMessageListener)` / `WebMessageListener[] getWebMessageListeners()` | 등록·제거의 null 입력, 중복 등록, 복사 배열 조회는 5.20의 Swing 등록 규칙을 따른다. EDT에서 보낸 순서대로 통지한다. 폐기 상태에서 등록·제거는 `IllegalStateException`, 조회는 빈 배열이다. |
| `WebMessageListener.messageReceived(WebMessageEvent)` | 페이지가 호스트 창에 보낸 메시지 |
| `WebMessageEvent extends EventObject` | 살아 있는 원천은 `SwingWebPanel`이다. `getData()`는 Java 복사본을, `getOrigin()`은 직렬화 오리진(불투명 오리진은 `"null"`), `getDocumentUrl()`은 송신 시점 URL을 반환한다. `getSourceKind()`은 `WebMessageEvent.SourceKind.DOCUMENT` 또는 `FRAME`이다. `getSequence()`와 `getTimestamp()`는 각각 컴포넌트의 일련번호와 UTC 에포크 밀리초를 `long`으로 반환한다. `void reply(Object)`는 정확히 그 송신 문서에 보낸다. 문서가 떠났거나 폐기된 뒤에는 버린다. |
| `SwingWebPanel.HOST_ORIGIN` | 공개 문자열 상수 `"swingwebpanel://host"`. 호스트에서 받은 메시지의 `event.origin`이다. |

- 페이지의 `window`에 `MessageEvent`를 보낸다. `data`는 수신 렐름의 복제값이고, `origin`은 `HOST_ORIGIN`, `source`는 호스트 창, `ports`는 빈 배열, `lastEventId`는 빈 문자열이다. 신뢰 이벤트이며 버블링·취소가 없다. `onmessage`와 `addEventListener`로 받을 수 있다. 작업으로만 실행하며 문서 교체, JavaScript 비활성, 오리진 불일치에서 버린다. 절대 URL은 스킴·호스트·유효 포트의 오리진을 비교한다. `reply`는 저장한 문서의 정체성까지 검사한다.
- 호스트 창에는 `postMessage(message, targetOrigin[, transfer])`, `postMessage(message, {targetOrigin, transfer})`, `closed`, 동작이 없는 `close()/focus()/blur()`가 있다. 그 밖의 속성 접근에서 `SecurityError`가 발생한다. 대상 오리진이 `"*"` 또는 `HOST_ORIGIN`일 때 전달한다. `"/"`와 다른 절대 URL은 버리며 잘못된 주소에는 `SyntaxError`가 발생한다. 최상위 문서의 `opener`를 null로 끊어도 이후 이벤트 `source`는 같은 호스트 창 객체다. 하위 프레임의 `opener`는 null이며, 같은 오리진과 다른 오리진 프레임 모두 `top.opener`에 보낼 수 있다. Worker에는 호스트 창이 없다.
- 값 변환(P27-02): Java → 페이지는 호출 스레드에서 복사한 뒤 수신 렐름의 프로토타입으로 복원한다. 페이지 → Java는 구조화 복제를 거쳐 Java 데이터로 복원한다. 같은 객체를 여러 번 가리킨 참조는 보존한다. 순환·지원 범위 외 값·한도 초과 시 Java 송신은 `IllegalArgumentException`, 페이지 송신은 `DataCloneError`이다.
- 수명(P27-05): EDT 통지 대기는 1,000건 또는 예상 데이터 16 MiB까지다. 한도를 넘은 메시지는 버리고, 큐를 비운 뒤 콘솔 `HOST_ERROR`로 개수를 알린다. 리스너가 없으면 메시지를 저장하지 않는다. 이동·`clean()`은 관련 대기 메시지를 버리며 리스너를 유지한다. `dispose()`는 리스너와 대기를 제거한다. `removeHyperlinkEvents()`는 메시지 리스너에 영향을 주지 않는다. JavaScript 비활성 상태에서는 메시지를 주고받을 수 없다.
- 보안: 어떤 페이지와 그 안의 다른 오리진 프레임도 호스트에 보낼 수 있으므로 Java 쪽은 `getOrigin()` 과 데이터 형식을 먼저 검사한다. Java 가 보낼 때는 `"*"` 대신 정확한 대상 오리진을 쓴다.
- 기본안에서 제외한 방식: 자기 `window` 로 보낸 메시지를 Java 가 엿듣기(페이지 자신의 리스너와 라이브러리의 자기 메시지가 섞임), `window.swingwebpanel` 같은 비표준 전역 객체(표준 관용구를 쓸 수 없음).

| Java 송신값 | 페이지의 수신값 |
| --- | --- |
| `null`, `Boolean`, `BigInteger` | null, boolean, bigint |
| 다른 `Number` | number(double). 큰 정수는 반올림될 수 있고 NaN, 무한대, 음의 0을 유지한다. |
| `String`, `CharSequence`, `Character` | string. 호출 중 문자열을 확보한다. |
| 문자열 키 `Map` | 일반 객체의 자체 데이터 프로퍼티. Map의 반복 순서로 만들며 정수 인덱스 키는 ECMAScript의 열거 순서를 따른다. |
| `Collection`, `Object[]`, `byte[]` 외 기본형 배열 | 반복 또는 인덱스 순서의 Array |
| `byte[]`, `java.util.Date` | 복사한 바이트의 ArrayBuffer, 유효 시간 범위의 Date |
| 그 밖의 객체·살아 있는 핸들·호스트 함수·스크립트 값 | `IllegalArgumentException` |

값·키·문자 단위와 바이트에 적용하는 메시지 한도는 `HostMessageValues.MAX_MESSAGE_UNITS`(262,144)이다. 중첩은 128단계까지 허용한다. Java → 페이지에서 사용한 Java 컬렉션·배열·날짜는 복사 후 바꿔도 수신값에 영향이 없다.

| 페이지 송신값 | Java 수신값 |
| --- | --- |
| undefined, null, boolean, number, bigint, string | `JsEngine.UNDEFINED`, null, `Boolean`, `Double`, `BigInteger`, `String`. 원시값 래퍼도 각각의 Java 값으로 복원한다. |
| Array, 일반 객체 | 읽기 전용 `List<Object>`와 `Map<String,Object>`. 배열의 빈 슬롯은 undefined이며 인덱스 외 속성은 리스트에 포함하지 않는다. |
| Map, Set | 삽입 순서의 읽기 전용 `Map<Object,Object>` 및 `Set<Object>`. 스크립트의 원시 키는 값으로, 객체에서 변환한 키는 Java 참조 정체성으로 조회한다. |
| 유효한 Date | `java.util.Date` 복사본 |
| ArrayBuffer, TypedArray, DataView | 현재 유효 바이트 구간의 `byte[]` 복사본 |
| 함수, DOM, 심볼, Invalid Date, RegExp, 오류 객체, Blob·File 등 별도 타입 | `DataCloneError` |
| SharedArrayBuffer·공유 뷰, MessagePort 전송 | `DataCloneError`. Java 호스트는 브라우저 에이전트 클러스터의 공유 메모리에 참여하지 않는다. |

전송 목록의 일반 ArrayBuffer는 메시지 전체가 Java 변환과 한도 검사를 통과하면 원본에서 분리된다. 날짜와 바이트 배열은 수신 측이 수정할 수 있는 복사본이다.

| 독립 엔진 API(P26)와 비교 | 호스트 메시지의 규칙 |
| --- | --- |
| 공통 원시값 | `Double`, `BigInteger`, 문자열·불리언·null 및 Java 수신의 undefined 표식은 독립 엔진과 같다. |
| 객체·배열 및 함수 | 메시지는 문서 수명과 독립된 복사본을 보낸다. 살아 있는 엔진 핸들, 임의 Java 객체와 호스트 함수는 거부된다. |
| 날짜와 바이트 | 메시지는 날짜와 바이트를 복사값으로 바꾼다. Java의 `byte[]`는 JS ArrayBuffer다. |

### 5.23 확장 메서드: 본 창 닫기 허용 (P30, 확장이며 AGENTS.md 의 공개 메서드 목록에는 없다)

`public void setRootWindowClosingEnabled(boolean enabled)`, `public boolean isRootWindowClosingEnabled()`,
`protected void closeRootWindow()`, 속성 이름 상수 `PROPERTY_ROOT_WINDOW_CLOSING_ENABLED`(`"rootWindowClosingEnabled"`).

기준 규격: WHATWG HTML Living Standard [7.2.2.1 Opening and closing windows](https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-window-close)
(`close()`, `closed`, script-closable; Last Updated 26 November 2026, `tools/webfetch` 로 2026-11-26 조회, 응답 `last-modified: Thu, 26 Nov 2026 00:20:43 GMT`).

**무엇을 하는가.** `true` 로 지정하면 이 컴포넌트에 띄운 페이지가 팝업이 아닌 본 창에서 `window.close()` 를 불러 창을 닫을 수 있다.
이때 기본 동작은 `System.exit(0)` 으로 프로그램을 끝내는 것이다. 기본값은 `false` 이며 이때 본 창의 `window.close()` 는 무시된다.

**설정.**

- 생성 직후 `false`. 어느 스레드에서나 호출할 수 있고, 바뀐 값은 다음 `window.close()` 판정부터 적용한다.
- 값이 실제로 바뀌면 `"rootWindowClosingEnabled"` 속성 변경을 EDT 에서 통지한다(`Boolean`).
- `clean()` 은 값을 유지한다. `dispose()` 뒤의 변경은 `IllegalStateException` 이고 조회는 마지막 값을 반환한다.
- **보안**: 신뢰할 수 없는 페이지를 띄우는 컴포넌트에서 켜면 그 페이지가 프로그램을 종료할 수 있다. 호스트가 내용을 통제하는 페이지에서만 켠다.

**창을 만든 주체.** 표준에서 스크립트가 닫을 수 있는(script-closable) 창은 "웹 콘텐츠가 만든 창"이거나 세션 이력 항목이 하나인 최상위 창이다.
이 컴포넌트는 창(컴포넌트의 최상위 브라우징 문맥)마다 만든 주체를 기록한다.

| 창 | 닫을 수 있는 조건 | 닫는 동작 |
| --- | --- | --- |
| 호스트가 만든 창(공개 생성자로 만든 `SwingWebPanel`. 이 절이 말하는 "본 창") | `setRootWindowClosingEnabled(true)` 일 때만 | EDT 에서 `closeRootWindow()` 한 번. 기본 구현은 `System.exit(0)` |
| 웹 콘텐츠가 만든 창(P31 의 `window.open` 팝업, `_blank` 링크·폼으로 연 창) | 이 설정과 무관하게 표준대로 | 그 창을 담은 쪽만 닫는다. `closeRootWindow()` 와 `System.exit(0)` 은 부르지 않는다 |

- 본 창에는 표준의 "세션 이력 항목이 하나" 규칙을 적용하지 않는다. 이 컴포넌트의 이력은 처음 `about:blank` 항목을 남기고(3절), 본 창은 호스트가 연 창이기 때문이다.
  이 때문에 이력이 하나뿐인 창을 스크립트가 닫게 하는 브라우저(Chromium)와 다르다.
- P30 시점에는 모든 컴포넌트가 "호스트가 만든 창"이다. 웹 콘텐츠가 만든 창을 실제로 만드는 경로는 P31-08 이 추가한다.

**호출한 쪽의 조건.**

- 공통(표준): 최상위 창의 `close()` 만 처리한다. 하위 프레임 자기 자신의 `window.close()` 는 무시한다. 이미 닫는 중이면 무시한다.
- 본 창(호스트가 만든 창)은 닫으면 프로그램이 끝나므로 표준보다 좁게 받는다.
  - 받는 호출: 그 컴포넌트의 최상위 문서 자신, 그리고 최상위 문서와 같은 오리진이며 샌드박스가 최상위 이동을 막지 않는 하위 프레임의 `top.close()`/`parent.close()`.
  - 무시하는 호출: 교차 오리진 하위 프레임의 `top.close()`, `allow-top-navigation` 없는 샌드박스 프레임의 `top.close()`, 다른 창의 스크립트가 부른 `close()`.
  - 표준은 교차 오리진 하위 프레임(자기 최상위와 "familiar")과 같은 오리진의 다른 창이 부르는 것도 허용한다. 다른 문서가 프로그램 전체를 끝내지 못하게 하려는 프로젝트 정책이다.
- 웹 콘텐츠가 만든 창에는 표준 조건(호출한 문서가 대상과 familiar 이고 샌드박스가 대상의 이동을 허용)만 적용한다.
- Worker 와 AudioWorklet 에는 `close()` 로 창을 닫는 길이 없다.

**무시할 때.** 아무 동작도 하지 않고 `closed` 는 `false` 로 남는다. 호출한 문서의 콘솔에 `warn` 수준으로 이유를 알린다(5.20 의 콘솔 리스너로 받을 수 있다).
표준에는 경고 규정이 없으며 Chromium 과 같은 구현 선택이다. 프레임 자기 자신의 `close()` 와 이미 닫는 중인 창의 반복 호출은 경고 없이 무시한다.

**닫을 때(표준의 닫기 절차).**

1. `close()` 를 부른 즉시 창이 "닫는 중"이 된다. `closed` 는 표준대로 "창이 없거나(폐기) 닫는 중"이면 `true` 이다.
   최상위 창의 `closed`, 프레임에서 본 `top.closed`, 교차 오리진에서 본 `closed` 가 모두 같은 값을 읽는다.
2. 실제 닫기는 호출한 스크립트 작업과 그 마이크로태스크 검사점이 끝난 뒤, 문서 큐의 작업으로 수행한다.
   즉 "바로 종료"는 "호출한 스크립트 작업이 끝난 직후 종료"이다. `window.close()` 뒤에 같은 작업에서 실행되는 문장은 끝까지 실행된다.
   파싱 중에 실행되는 인라인 스크립트는 문서 스레드가 아닌 로더 스레드에서 실행되므로, 닫기 작업은 호출한 작업의 페이지 잠금이 풀릴 때까지 기다린 뒤 진행한다(`PageScripts.taskEndBarrier()`).
3. 닫기 작업은 지금 보이는 페이지의 언로드 이벤트(`beforeunload`, `pagehide`, `visibilitychange`, `unload`)를 확인 대화 상자 없이 한 번 실행한다.
   그 뒤의 `dispose()`/`clean()` 은 이 이벤트를 다시 보내지 않는다. 이동할 때와 같이 한 번 보인(`pageshow`) 페이지만 이 이벤트를 듣는다.
   그래서 `load` 이벤트 처리기나 파싱 중의 스크립트처럼 `pageshow` 전에 닫은 페이지는 언로드 이벤트 없이 닫힌다.
4. 그 뒤 EDT 에서 닫는 동작을 한 번 수행한다(위 표).

**취소.** 페이지 스크립트의 이동(`location`, 링크, 폼)과 진행 중인 로딩은 닫기를 취소하지 않는다(표준과 같다).
다만 이 컴포넌트는 이동이 시작되면 떠나는 페이지의 스크립트를 그 자리에서 중단하므로, 같은 스크립트에서 `location` 대입 뒤에 오는 `window.close()` 는 실행되지 않는다.
닫기를 취소하는 것은 닫는 동작이 시작되기 전의 호스트 조작뿐이다: Java 의 `setPage`, `setContent`, `back`, `forward`, `refresh`, `clean()`, `dispose()`,
`setJavascriptEnabled(false)`, 그리고 본 창에서의 `setRootWindowClosingEnabled(false)`. 취소하면 닫는 중 상태가 풀리고 `closed` 는 다시 `false` 가 된다.
아무 일도 하지 않는 `back`/`forward`(이동할 칸이 없음)는 취소하지 않는다.

**확장 지점.** `closeRootWindow()` 는 `configureSsl` 처럼 호스트와 시험이 재정의할 수 있다(예: 프로그램을 끝내지 않고 그 창이나 탭만 닫기).
재정의한 메서드가 반환하면 컴포넌트와 페이지는 그대로 남고 `closed` 는 `true` 를 유지한다(이미 언로드 이벤트를 들은 페이지이다).
그 뒤 컴포넌트를 폐기할지 다른 페이지로 이동시킬지는 호스트가 정한다. 호스트가 이동시키면 위 취소 규칙대로 닫는 중 상태가 풀린다.
JavaScript 를 실행하지 않는 페이지(`setJavascriptEnabled(false)`, `text/plain` 등)에는 `window.close()` 를 부를 스크립트가 없으므로 해당하지 않는다.

### 5.24 확장: 쿠키 저장소 공유와 쿠키 삭제 (P31-04, 확장이며 AGENTS.md 의 공개 메서드 목록에는 없다)

`public final class CookieJar`(`new CookieJar()`, `getCookies()`, `clear()`), `public SwingWebPanel(CookieJar jar)`,
`public CookieJar getCookieJar()`, `public void clearCookies()`.

**무엇을 하는가.** 기본적으로 쿠키 저장소는 컴포넌트마다 따로이다(5.16). 그래서 한 컴포넌트에서 로그인해도 다른 컴포넌트에는 보이지 않는다.
한 사용자의 여러 페이지를 보여 주는 호스트(브라우저의 탭과 창)는 `CookieJar` 하나를 만들어 모든 컴포넌트의 생성자에 넘긴다.

| 항목 | 동작 |
| --- | --- |
| `new SwingWebPanel(jar)` | 같은 `jar` 로 만든 컴포넌트들이 쿠키를 공유한다. 한 컴포넌트의 응답이나 스크립트(`document.cookie`)가 설정한 쿠키를 나머지의 요청과 스크립트가 본다. `jar` 가 null 이면 `IllegalArgumentException` |
| `new SwingWebPanel()` | 지금처럼 자기 저장소를 가진다. `getCookieJar()` 는 null |
| 공유 범위 | 쿠키만 공유한다. 이력, HTTP 캐시, sessionStorage, 추가 헤더, User-Agent 는 컴포넌트마다 따로이다(localStorage 는 원래 프로세스 공용이다, 5.17) |
| `getCookies()` | 공유 저장소의 쿠키 전부를 5.16 의 형식으로 반환한다. `CookieJar.getCookies()` 와 같은 값이다 |
| `dispose()` | 공유 저장소를 비우지 않는다. 폐기된 컴포넌트의 `getCookies()` 는 `""` 이다(5.14 의 빈 상태). 자기 저장소를 가진 컴포넌트는 지금처럼 폐기할 때 쿠키를 잃는다 |
| `clean()` | 지금처럼 쿠키를 유지한다(5.13) |
| `clearCookies()` | 이 컴포넌트의 쿠키를 모두 지운다. 공유 저장소이면 공유하는 모든 컴포넌트에서 지워진다. 보이는 페이지는 계속 실행되고 이후 요청은 지운 쿠키 없이 나간다. 폐기된 컴포넌트에서는 `IllegalStateException` |
| `CookieJar.clear()` | 저장소의 쿠키를 모두 지운다(컴포넌트 없이도 부를 수 있다) |
| 수명 | 저장소는 참조되는 동안 살아 있다. 모든 컴포넌트를 폐기한 뒤 같은 저장소로 새 컴포넌트를 만들면 쿠키가 그대로 있다 |
| 스레드 | `CookieJar` 의 메서드와 `clearCookies()` 는 어느 스레드에서나 부를 수 있다 |

### 5.25 `getTitle()` 과 `title` 속성은 살아 있는 문서의 제목을 따른다 (P31-04)

- `getTitle()` 은 확정된 최상위 문서의 현재 제목(`document.title` 과 같은 값: 첫 `title` 요소의 글, 공백 정리)을 반환한다.
  페이지를 읽은 뒤 스크립트가 `document.title` 을 바꾸거나 `title` 요소의 글을 고치거나 요소를 추가·제거하면 값이 바뀌고, `"title"` 속성 변경을 EDT 에서 통지한다(4절).
- 파싱 중에 스크립트가 바꾼 제목은 페이지가 확정된 뒤 반영된다. 확정 전에는 이전 페이지의 제목을 반환한다(5.3 의 확정 규칙과 같다).
- 하위 프레임 문서의 제목은 컴포넌트의 제목에 영향이 없다. 프레임의 스크립트가 부모 문서의 제목을 바꾸면 반영된다.
- JavaScript 를 실행하지 않는 페이지의 제목은 받은 원문의 제목 그대로이다. `getContent()` 는 제목이 바뀌어도 받은 원문을 반환한다(5.9).
- 이전에는 확정 시점의 제목에서 값이 고정되었다. 브라우저의 제목 표시줄과 탭 제목에 필요해서 P31-04 에서 바꿨다.

### 5.26 확장: 호스트의 편집 명령과 명령 키의 소비 (P31-05-a, 확장이며 AGENTS.md 의 공개 메서드 목록에는 없다)

`public void copy()`, `public void cut()`, `public void paste()`, `public void selectAll()`.

- 메뉴 항목이나 도구 모음 단추처럼 호스트가 실행하는 편집 명령이다. 각각 Ctrl+C, Ctrl+X, Ctrl+V, Ctrl+A 의 기본 동작과 같은 일을 한다.
  페이지에 `copy`/`cut`/`paste` 이벤트와 편집의 `beforeinput`/`input` 이벤트는 가지만 키 이벤트(`keydown` 등)는 가지 않는다.
- 대상: 폼의 텍스트 컨트롤에 포커스가 있으면 그 컨트롤의 편집기, 편집 호스트(`contenteditable`, 디자인 모드)에 있으면 그 호스트, 그 밖에는 페이지이다.
  편집할 수 없는 곳에서 `cut()` 은 아무 일도 하지 않고 `paste()` 는 `paste` 이벤트만 보낸다. `selectAll()` 은 페이지 전체를 선택한다.
- 클립보드는 지금처럼 `copyToClipboard`/`readClipboard` 보호 메서드를 거친다.
- 어느 스레드에서나 부를 수 있다. 명령은 EDT 에서 시작하고 페이지 쪽 일은 문서 큐에서 실행되므로 결과는 호출이 돌아온 조금 뒤에 나타난다.
  폐기된 컴포넌트에서는 `IllegalStateException` 이다.
- **명령 키의 소비(동작 변경)**: 컴포넌트가 스스로 명령으로 처리하는 키는 그 `KeyEvent` 를 소비한다. 그래서 호스트 창의 키 바인딩(메뉴 가속 키)이 같은 키로 한 번 더 실행되지 않는다.
  어디서나 복사(Ctrl+C, Ctrl+Insert), 모두 선택(Ctrl+A), 붙여넣기(Ctrl+V, Shift+Insert), 확대 키(Ctrl 과 +, =, -, 0)가 해당하고,
  편집 가능한 곳에 포커스가 있으면 잘라내기(Ctrl+X, Shift+Delete), 서식(Ctrl+B/I/U), 되돌리기와 다시 하기(Ctrl+Z/Y)도 해당한다.
  폼의 텍스트 컨트롤 편집기에서 온 키는 확대 키만 소비한다(나머지는 편집기 자신의 키 바인딩이 처리한다). 그 밖의 키(Ctrl+T, F5, Alt+화살표 등)는 소비하지 않아 호스트가 쓸 수 있다.
  이전에는 편집 호스트의 캐럿 키만 소비했다.

### 5.27 확장: 컴포넌트별 기본 글꼴 (P31-07, 확장이며 AGENTS.md 의 공개 메서드 목록에는 없다)

`public void setDefaultFontFamily(String kind, String family)`, `public String getDefaultFontFamily(String kind)`,
`public void setDefaultFontSize(int px)`, `public int getDefaultFontSize()`,
`public void setDefaultMonospaceFontSize(int px)`, `public int getDefaultMonospaceFontSize()`.
종류 상수: `FONT_STANDARD`(`"standard"`), `FONT_SERIF`(`"serif"`), `FONT_SANS_SERIF`(`"sans-serif"`), `FONT_MONOSPACE`(`"monospace"`). 속성 이름 `PROPERTY_DEFAULT_FONTS`(`"defaultFonts"`).

| 항목 | 계약 |
| --- | --- |
| 표준 글꼴(`standard`) | 작성자가 글꼴을 지정하지 않은 내용의 글꼴이다. 루트 요소의 `font-family` 를 정하는 **사용자 출처** 규칙(`:root { font-family: "패밀리", serif }`)으로 적용하므로 작성자 규칙이 이긴다. `getComputedStyle` 의 `fontFamily` 는 그 패밀리 이름으로 시작한다. `font-family: initial` 은 지금처럼 `serif` 이다(한계) |
| 일반 이름(`serif`, `sans-serif`, `monospace`) | 스타일의 글꼴 목록에 그 일반 이름이 나오는 모든 곳(작성자 목록의 마지막 이름 포함, 초기값 `serif` 포함)에서 그 패밀리를 쓴다. `cursive`, `fantasy`, `system-ui` 등 다른 일반 이름은 바뀌지 않는다 |
| 설치되지 않은 패밀리 | 값으로는 보관하지만 배치할 때 무시한다(그 종류의 내장 글꼴을 쓴다). 표준 글꼴이 설치되어 있지 않으면 규칙의 다음 이름인 `serif` 로 넘어간다 |
| 기본 크기 | `font-size: medium` 의 크기(CSS 픽셀)이다. 내장 값 16, 범위 6~72. 다른 절대 크기 키워드(`small`, `x-large` 등)는 16 기준 표에 비례한다. 루트 요소가 이 크기에서 시작하므로 루트의 백분율·`em` 과 그에 따른 `rem` 이 따라간다. 픽셀 등 절대 길이는 바뀌지 않는다 |
| 고정폭 크기 | `code`, `kbd`, `samp`, `tt`, `pre`, `listing`, `xmp`, `plaintext` 요소의 글꼴 크기이다. 기본 크기의 글을 기준으로 준 값을 **비율**(`고정폭 크기 ÷ 기본 크기` em)의 사용자 출처 규칙으로 적용하므로 제목 안에서는 함께 커지고, 이 요소들이 중첩되어도 한 번만 적용된다. 0(내장 값)이면 주변 글의 크기이다. 범위 0 또는 6~72. 작성자 규칙이 이긴다. `textarea` 등 폼 컨트롤에는 적용하지 않는다 |
| 적용 범위 | 그 컴포넌트가 보이는 모든 문서: 최상위 문서, 프레임, 오류 화면, `text/plain`·Markdown 표시(같은 HTML 경로로 그린다). 보이는 페이지는 스타일과 배치를 다시 계산한다. 웹 글꼴(`@font-face`)과 작성자가 지정한 패밀리에는 영향이 없다. **캔버스 2D 의 `font`** 는 자체 대응을 쓰므로 바뀌지 않는다(한계). 미디어 질의의 `em` 은 16px 기준 그대로이다(한계) |
| 컴포넌트별 | 값은 컴포넌트마다 따로이며 정적 공유 상태가 없다. 다른 컴포넌트에 영향이 없다 |
| 인수 | 모르는 종류, 제어 문자가 든 패밀리 이름, 범위 밖의 크기는 `IllegalArgumentException`. 패밀리 이름은 앞뒤 공백을 떼고 보관한다. `null` 과 빈 문자열은 내장 글꼴로 되돌린다 |
| 스레드와 통지 | 어느 스레드에서나 부를 수 있다. 값이 실제로 달라지면 `"defaultFonts"` 속성 변경을 EDT 에서 통지한다(값은 한 줄 설명 문자열) |
| 수명 | `clean()` 과 페이지 이동 뒤에도 유지한다. 폐기된 컴포넌트에서 변경은 `IllegalStateException`, 조회는 마지막 값을 반환한다 |

같이 고친 것: 다른 문서(프레임 등)의 요소를 `window.getComputedStyle` 에 넘기면 Java 예외가 스크립트 작업 밖으로 새던 것을 고쳐, 초기값의 선언 객체를 돌려준다. 그 요소의 계산 스타일은 그 문서의 창에서 얻는다.

### 5.28 확장: 페이지가 여는 창(팝업)과 팝업 차단 (P31-08, 확장이며 AGENTS.md 의 공개 메서드 목록에는 없다)

기준: WHATWG HTML Living Standard [7.2.2.1 Opening and closing windows](https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-open)
(`tools/webfetch` 로 2026-11-27 조회, 2026-11-26 00:20:43 GMT 판본).

**바뀐 기본 동작(2026-11-26 사용자 결정).** 이전에는 새 창이 필요한 `window.open` 이 언제나 `null` 을 돌려주었다. 이제 사용자의 조작이 있은 직후의
`window.open` 은 컴포넌트가 제공하는 팝업 창(비모달 `JDialog`)에 새 창을 열고 그 창의 객체를 돌려주며, 조작 없이 부른 호출은 차단한다(`null`, `popupBlocked`).
링크와 폼의 `target="_blank"`(와 없는 이름)는 호스트가 `openNewWindow` 를 재정의하지 않으면 전과 같이 자기 문맥에서 열린다.

공개 메서드: `public void setPopupBlockingEnabled(boolean)`, `public boolean isPopupBlockingEnabled()`, `public void disposeKeepingStorage()`.
속성 이름 `PROPERTY_POPUP_BLOCKING_ENABLED`(`"popupBlockingEnabled"`). 공개 타입: `WindowRequest`, `SwingWebPanel.PopupSeed`.
보호 확장 지점: `protected SwingWebPanel(PopupSeed)`, `createPopupPanel(PopupSeed)`, `canOpenPopupWindows()`, `openPopupWindow(WindowRequest)`,
`openNewWindow(WindowRequest)`, `popupBlocked(WindowRequest)`, `closeOpenedWindow()`.

| 항목 | 계약 |
| --- | --- |
| 팝업 차단 설정 | 생성 직후 `true`. 어느 스레드에서나 바꿀 수 있고 다음 판정부터 적용한다. 값이 달라지면 `"popupBlockingEnabled"`(Boolean)을 EDT 에서 통지한다. `clean()` 뒤에도 유지하고, 폐기 뒤 변경은 `IllegalStateException`, 조회는 마지막 값이다. 페이지가 연 창의 컴포넌트는 여는 창의 값을 이어받는다 |
| 사용자의 조작 | 신뢰할 수 있는 입력 이벤트(주 단추의 `mousedown`/`pointerdown`/`click`, 보조 키와 Esc 를 뺀 반복 아닌 `keydown`)가 전달되면 그 창은 5초 동안 "조작이 있은" 상태이다. `window.open` 한 번이 그 상태를 소비하므로 조작 한 번은 창 하나만 연다. 스크립트가 만든 이벤트(`dispatchEvent`, `element.click()`)는 해당하지 않는다 |
| 차단 판정 | 설정이 켜져 있고 조작이 없으면 `window.open` 은 `null` 을 돌려주고 `popupBlocked` 가 불린다. 설정을 끄면 조작과 무관하게 연다(이때도 조작 상태는 소비한다). 스크립트가 조작 없이 새 창으로 제출한 폼도 호스트가 그 창의 컴포넌트를 요청하는 시점(`WindowRequest.getPanel()`)에 같은 규칙으로 차단하며, 차단하면 폼은 제자리에서도 제출되지 않는다. 사용자가 직접 누른 링크와 제출 단추는 차단하지 않는다 |
| 창을 보일 수 없는 컴포넌트 | `canOpenPopupWindows()` 가 `false` 이면(기본 구현: 화면이 없는 환경) `window.open` 은 차단 알림 없이 `null` 을 돌려준다 |
| `window.open` 의 결과 | 새 창의 `WindowProxy` 를 호출 안에서 곧바로 돌려준다. 새 창은 스크립트를 실행할 수 있는 빈 `about:blank` 문서를 이미 가지며(오리진과 기준 주소는 여는 문서의 것), 주소가 있으면 그 주소를 비동기로 읽어 그 빈 문서의 이력 항목을 대신한다. 여는 창은 그 객체로 문서 읽기·쓰기(`document.write` 포함), `location` 이동, `close()`/`closed`, `postMessage` 를 하고, 새 창의 `window.opener` 는 여는 창이다. 다른 오리진의 문서가 되면 교차 오리진에 허용된 멤버만 쓸 수 있다. sessionStorage 는 여는 창의 것을 복사해서 시작한다 |
| 이름 있는 창 | `window.open(url, 'name')`, 링크·폼의 `target="name"` 은 먼저 프레임을, 다음에 같은 그룹(서로 연 창들)의 창을 이름(`window.name` 또는 열 때의 이름)으로 찾아 그 창을 쓴다 |
| 창 기능 문자열 | `noopener`/`noreferrer` 는 관계없는 창을 연다(호출은 `null`). `width`/`innerWidth`, `height`/`innerHeight`, `left`/`screenX`, `top`/`screenY` 는 `WindowRequest` 로 호스트에 전달한다. 그 밖의 토큰은 무시한다 |
| 관계없는 창 | `noopener` 창, 링크·폼이 연 창, 사용자가 고른 새 탭·새 창은 여는 페이지와 관계가 없다: 그 페이지의 `window.opener` 는 다른 컴포넌트와 같이 Java 호스트 창(5.22)이고 자기 문서 스레드를 쓴다 |
| 파싱 중의 호출 | 문서를 아직 읽는 중에 실행되는 스크립트(인라인 스크립트 등)의 `window.open` 은 창을 열지만 관계를 맺지 못하므로 `noopener` 처럼 `null` 을 돌려준다(한계) |
| 문서 스레드 | 관계있는 창들은 문서 스레드 하나를 함께 쓴다(표준의 이벤트 루프 공유). 그래서 한 창의 긴 스크립트가 같은 그룹의 다른 창을 함께 멈추게 한다. 컴포넌트 하나를 폐기해도 나머지는 계속 동작하고, 마지막 창이 폐기되면 스레드가 끝난다 |
| 새 창의 컴포넌트 | `createPopupPanel(PopupSeed)` 가 만든다(기본 구현 `new SwingWebPanel(seed)`). **`window.open` 의 경우 스크립트의 스레드에서 불리므로** Swing 부품을 붙이거나 보이는 일은 하지 말고 `super(seed)` 를 부르는 생성자로 컴포넌트만 만든다. 씨앗 하나는 컴포넌트 하나에만 쓸 수 있다. 새 컴포넌트는 여는 컴포넌트의 쿠키 저장소(5.24), JavaScript 설정, User-Agent, 추가 헤더, 팝업 차단 설정, 기본 글꼴(5.27)을 이어받고, 재정의하지 않은 정책 메서드(`configureSsl`, `getDownloadDirectory`, 스크립트 시간 한도, 오디오 출력 팩터리, 이 절의 확장 지점)는 여는 컴포넌트에 묻는다 |
| 창을 보이기 | 스크립트가 연 창은 EDT 에서 `openPopupWindow(WindowRequest)` 로 넘긴다. 기본 구현은 여는 컴포넌트가 속한 창을 소유자로 하는 비모달 `JDialog` 에 `JScrollPane` 과 함께 보인다: 내용 크기는 요청한 크기(없으면 640x480)를 화면에 맞춘 것, 위치는 요청한 곳 또는 소유자 위, 제목은 페이지의 제목(없으면 주소)이다. 대화 상자가 닫히면(사용자, `window.close()`, 소유자와 함께) 컴포넌트를 `disposeKeepingStorage()` 로 폐기한다. 재정의한 호스트는 `request.getPanel()` 을 원하는 곳에 두거나 `request.reject()` 로 거절한다(거절한 창은 스크립트에게 닫힌 창이다) |
| 링크·폼·사용자의 요청 | 대상이 새 창을 필요로 하는 링크와 폼(`LINK`), 그리고 사용자가 고른 것(`USER`: Ctrl+클릭과 가운데 단추는 `PLACE_TAB`, Shift+클릭은 `PLACE_WINDOW`)은 EDT 에서 `openNewWindow(WindowRequest)` 로 제안한다. 기본 구현은 `false` 를 돌려주며 그러면 전과 같다: 링크·폼은 자기 문맥에서 열리고, 보조 키를 누른 클릭은 보통의 클릭이며, 가운데 클릭은 아무 일도 하지 않는다. `true` 를 돌려주는 호스트는 `request.getPanel()` 로 새 창의 컴포넌트를 얻거나(주소를 읽거나 폼을 받는다. `LINK` 창은 웹 콘텐츠가 만든 창이다) `request.getUrl()` 을 자기 컴포넌트에서 연다. DOM 에서 취소된 클릭(`click`, 가운데 단추는 `auxclick`)은 제안하지 않는다 |
| 차단 알림 | `popupBlocked(WindowRequest)` 를 EDT 에서 부른다. 기본 구현은 상태 문구와 콘솔 경고(`HOST_ERROR`, 5.20)로 알린다. 요청에는 주소, 대상 이름, 창 기능 문자열, 요청한 문서의 주소가 있다 |
| 닫기 | 페이지가 연 창(스크립트·링크·폼)은 웹 콘텐츠가 만든 창이다(5.23): 그 창의 `window.close()` 와 여는 창의 `w.close()` 가 본 창 닫기 설정과 무관하게 그 창만 닫는다. 읽는 중이어도, 아직 보이지 않았어도 닫힌다. `closeRootWindow()` 는 불리지 않고 EDT 에서 `closeOpenedWindow()` 가 불린다(기본 구현: 창을 보인 쪽이 등록한 닫기 동작을 실행하거나 컴포넌트를 폐기한다). 여는 창은 다른 오리진이 된 자기 팝업도 닫을 수 있다. 팝업은 `opener.close()` 로 호스트가 만든 여는 창을 닫을 수 없다 |
| 수명 | 여는 컴포넌트가 폐기되어도 팝업은 남고 `opener.closed` 가 `true` 가 된다. **`dispose()` 는 계약대로 프로세스 공용 localStorage 를 비우므로**, 다른 창이 남아 있을 때 창이나 탭 하나를 닫는 호스트는 `disposeKeepingStorage()` 를 쓴다: `dispose()` 와 같지만 localStorage 를 남긴다 |
| Cross-Origin-Opener-Policy | 최상위 이동의 응답이 가진 COOP 와 보이는 문서의 COOP 가 표준의 규칙(HTML 7.1.3, 처음 빈 문서의 `same-origin-allow-popups` 예외 포함)으로 그룹 전환을 요구하면 그 창은 그룹을 떠난다: `window.opener` 가 없어지고(호스트 창이 된다), 이전에 관계있던 창의 스크립트는 그 창을 닫힌 창으로 보며 메시지·이동·닫기를 할 수 없고, 이름으로도 찾지 못한다 |
| 샌드박스 | `allow-popups` 가 없는 샌드박스 문서는 전과 같이 새 창을 열 수 없다. 샌드박스 문서가 연 창은 그 샌드박스를 이어받아야 하는데 컴포넌트의 최상위 창은 샌드박스를 지닐 수 없으므로, **`allow-popups-to-escape-sandbox` 가 함께 있을 때만** 새 창을 연다(그 창은 샌드박스가 아니다). 없으면 전과 같다: `window.open` 은 `null`, 링크·폼은 자기 프레임에서 열린다(한계: 샌드박스의 상속은 구현하지 않았다) |
| 메시지 | 읽는 중인 창으로 보낸 `postMessage` 는, 새 페이지의 스크립트가 이미 실행되고 있으면 확정 전이라도 그 페이지가 받는다(파싱 중에 여는 창에 자신을 알린 페이지가 답을 받는다). 그 밖의 접근(`w.document` 등)은 확정될 때까지 이전 문서를 본다 |

`WindowRequest`: `getKind()`(`SCRIPT`/`LINK`/`USER`), `getSource()`, `getUrl()`, `getTargetName()`, `getFeatures()`, `getDocumentUrl()`, `getWidth()`/`getHeight()`/`getLeft()`/`getTop()`(없으면 -1),
`getPlace()`(`PLACE_TAB`/`PLACE_WINDOW`), `getPanel()`, `hasPanel()`, `reject()`, `isRejected()`, `isBlocked()`. EDT 에서 쓴다.

알려진 한계: (1) 처음 빈 문서의 Window 를 첫 같은 오리진 문서가 다시 쓰지 않으므로 `w = window.open(url)` 직후에 건 `w.onload` 는 새 문서에서 실행되지 않는다. (2) 처음 빈 문서의 `document.domain` 은 여는 문서의 것과 같지 않다. (3) `blob:` 주소는 만든 컴포넌트의 것이어서 팝업이 여는 창의 객체 URL 을 열 수 없다. (4) `popup` 등 나머지 창 기능과 `BarProp` 은 반영하지 않는다. (5) 문맥 메뉴는 컴포넌트가 제공하지 않는다(호스트의 일).

## 6. 허용 스킴 정책 (P01-12)

| 스킴 | 정책 |
| --- | --- |
| `http`, `https` | 허용 |
| `ftp` | 허용(익명) |
| `classpath` | 특수 주소로 허용 |
| `blob` | 생성자 문서·워커와 같은 컴포넌트에서 사용. 프레임·하위 리소스·Fetch/XHR·명시적 다운로드는 생성자의 오리진을 적용하고, 최상위 이동은 File API의 파티션 예외를 적용 |
| `about:blank` | 특수 주소로 허용 |
| `about:text` | 내부 전용, `setPage` 로 지정 불가 |
| `file` | Java 호스트와 같은 파일 오리진 문서에서 로컬 일반 파일에 접근한다. 상대 경로, GET·HEAD를 지원한다. 원격 호스트, UNC 주소, 디렉터리 목록, 쓰기 메서드는 거부한다. |
| `jar`, `mailto`, `javascript`, `data`, `ws(s)`, 그 밖 | **차단**. 서버/파일에 접근하지 않고 오류 페이지로 확정 |

차단 정책은 최초 주소, 리디렉션 주소, 하위 리소스(이미지/CSS/스크립트/오디오/글꼴) 모두에 적용한다. 클래스패스 주소가 `file:`/`jar:` URL 로 우회 변환되지 않는다.

로컬 파일 문서는 `file://` 오리진을 공유하며 네트워크 요청에는 로컬 출처를 `null`로 직렬화한다. HTTP 리디렉션, 원격·불투명 문서나 그러한 문서가 지정한 `<base>`는 로컬 파일에 접근할 수 없다. 파일 응답을 HTTP 캐시에 넣지 않는다.

## 7. 파일 다운로드 계약 (P01-18)

- 다음에 해당하면 **페이지를 교체하지 않고** Swing 다운로드 팝업을 띄운다.
  1) 응답이 `Content-Disposition: attachment` 이다.
  2) 동일 오리진의 `<a download>` 링크이다.
  3) MIME 이 P17 에서 정한 표시 가능 형식(HTML/XHTML/plain/markdown/이미지/오디오/비디오 대체 표시 등)이 아니다.
- 판정은 링크 클릭을 기준으로 하되 폼 전송, 스크립트 이동, `setPage` 도 같은 판정을 쓴다.
- 같은 오리진의 `blob:` 링크는 DOM에 붙이지 않은 앵커의 `click()`으로도 저장할 수 있다. 샌드박스에서는 `allow-downloads`가 필요하다. 요청을 받아들인 뒤 주소를 해제해도 이미 확보한 바이트로 전송하며, 현재 페이지의 스크립트와 타이머는 계속 실행된다.
- 다운로드는 `getPage()`/`getContent()`/이력/주 문서 진행률을 **바꾸지 않는다**.
- 팝업은 파일명/출처 URL/크기, 저장 위치 선택(`JFileChooser`), 진행률(크기를 모르면 불확정), 취소, 완료/실패 결과를 제공한다.
- 팝업 생성은 호스트와 시험 코드가 대체할 수 있도록 **protected 메서드**(`protected DownloadHandle createDownloadPopup(DownloadRequest)`)로 분리한다.
- 헤드리스 환경(`GraphicsEnvironment.isHeadless()`)에서는 다운로드를 취소하고 상태 메시지로 알린다.
- `clean`/`dispose` 는 진행 중 다운로드를 취소하고 팝업을 닫는다. 완료된 파일은 지우지 않는다.

## 8. SSL 확장 지점 (P04-07)

`protected void configureSsl(javax.net.ssl.HttpsURLConnection connection) throws java.io.IOException` 을 `SwingWebPanel` 에 두고
모든 HTTPS 요청에서 호출한다. 기본 구현은 JRE 기본 신뢰 저장소와 호스트 이름 검증을 사용한다(전역 설정을 바꾸지 않고, 인증서 검증을 무조건 통과시키지 않음).
HTTPS 의 연결/TLS 실패 시에만 같은 주소의 HTTP 를 한 번 시도한다(기본 443 포트는 80 으로, 명시한 다른 포트는 유지). HTTP 404/500 은 HTTPS 실패가 아니다.

`Origin`/`Access-Control-Request-*` 헤더를 실어야 하는 요청(CORS 요청, 사전 요청, 시작한 오리진이 있는 POST/PUT/DELETE)은 JRE 가 그 헤더를 버리므로 소켓 기반 연결(`net.SocketHttpsURLConnection`)로 보낸다. 이 연결도 같은 `configureSsl` 을 부르며 `setSSLSocketFactory`/`setHostnameVerifier` 를 따른다. 인증서 사슬은 JRE 의 기본 신뢰 저장소(또는 재정의가 정한 소켓 팩터리)로, 호스트 이름은 RFC 2818 규칙으로 확인하며 JVM 전역 호스트 이름 검증기는 쓰지 않는다. JVM 프록시가 적용되면 JRE 연결을 쓰고 그 헤더는 사라진다.

## 9. 타임아웃/제한 기본값 (P04-03 에서 상수로 확정)

| 상수 | 기본값 | 초과 시 |
| --- | --- | --- |
| 연결 제한 시간 | 15 초 | 오류 페이지(연결 시간 초과) |
| 읽기 제한 시간 | 30 초 | 오류 페이지(응답 시간 초과) |
| 최대 리디렉션 | 20 회 | 오류 페이지(리디렉션 초과) |
| 최대 메모리 응답 크기 | 16 MiB | 오류 페이지(응답 과대) |
| 다운로드 크기 | 제한 없음(파일로 스트리밍) | - |

## 10. 검증 사례 목록 (연결 대상)

| ID | 대상 | 확인 내용 |
| --- | --- | --- |
| C-INIT-1 | 초기 상태 | 3절 표 전체 |
| C-UA-1~4 | User-Agent | 기본값, 설정/조회, null/빈/CRLF 거부, 요청 헤더 반영 |
| C-PAGE-1~8 | setPage | 비동기, 로딩 중 이전 페이지 반환, 확정 시 일괄 교체, IAE 입력, 차단 스킴 오류 페이지, 오래된 응답 폐기 |
| C-JS-1~3 | JS 설정 | 기본 true, false 시 script 중지, true 복귀 후 다음 로드부터 실행 |
| C-HIST-1~8 | 이력 | 경계, 정확한 n칸, 분기 이력 삭제, 동일 URL, 리디렉션 한 건, `setContent` 복원, 복사본 |
| C-REFRESH-1~5 | 새로고침 | 캐시/조건부/force, `about:text`/`about:blank`, 이력 불변 |
| C-CONTENT-1~6 | 원문 | 수신 원문 보존, DOM 변경 무영향, 오류 본문, 내부 생성 문서 |
| C-SETCONTENT-1~6 | setContent | null/빈/MIME 정규화/미지원 MIME, `about:text`, 이력 |
| C-STATUS-1~5 | 상태/진행률 | 3절/5.11 표, -1, 취소된 요청 무시 |
| C-LINK-1~6 | 링크 | 등록/중복/제거, ENTERED/EXITED/ACTIVATED, 취소/기본 이동, `removeHyperlinkEvents` 후 내부 이동 |
| C-CLEAN-1~5 | clean | 유지/삭제 항목 표, 공유 localStorage 영향, 진행 중 취소 |
| C-DISPOSE-1~5 | dispose | 반복 호출, 폐기 후 변경 ISE/조회 빈 상태, 리스너 제거 표 |
| C-HEADERS-1~6 | 헤더 | 덮어쓰기, 대소문자, UA 제외, 검증 거부, 복사본 |
| C-COOKIE-1~5 | 쿠키 | 형식, 정렬, 동명 쿠키, 만료, 컴포넌트 분리 |
| C-STORAGE-1~6 | 저장소 | 복사본, 오리진 분리, session 분리/local 공유 |
| C-DOWNLOAD-1~8 | 다운로드 | 7절 전체 |
| C-SSL-1~3 | SSL | protected 재정의 호출, HTTPS 실패 후 HTTP 재시도, 404 비재시도 |
| C-CONSOLE-1~8 | 콘솔 리스너(5.20) | log/error 수준·문자열·인수·위치·EDT, 모든 콘솔 메서드의 수준, Swing 등록 규칙, 리스너 예외 격리, 브라우저 보고의 종류, 모든 스크립트 실행 위치, 프레임/Worker 표시, 대량 출력 상한, 이동·clean·dispose·JavaScript 설정 |
| C-MESSAGE-1~8 | 호스트 메시지(5.22, P27) | `HostOutboundMessageTest`: 1 대상 오리진·신뢰 이벤트, 2 파싱 중 발신·이동 취소. `HostInboundMessageTest`: 3 준비 알림·EDT 응답, 4 값과 전달 오류, 5 프레임·Worker·교체 응답. `HostMessageQueueTest`: 6 마이크로태스크와 리스너 예외, 7 대기 건수·바이트. `HostMessageLifecycleTest`: 8 비활성·정리·폐기와 약한 수명. 클래스패스 왕복은 `MessageExample --smoke`. |
| C-ROOTCLOSE-1~8 | 본 창 닫기 허용(5.23, P30) | `RootWindowClosingTest`: 1 기본값·속성 통지·`clean()` 유지·폐기 뒤 ISE, 2 기본값에서 무시와 콘솔 경고, 3 허용 시 EDT 한 번·작업과 마이크로태스크 뒤(파싱 중 스크립트 포함)·`closed`·반복 호출·타이머/프라미스/인라인 핸들러, 4 프레임(같은/다른 오리진, 샌드박스, 프레임 자신), 5 페이지 이동은 취소하지 않고 호스트 조작은 취소, 6 언로드 이벤트 한 번. `core.WebContentWindowClosingTest`: 7 웹 콘텐츠가 만든 창 경로. `RootWindowClosingExitMain`(Maven `run-root-close-exit`): 8 기본 구현의 `System.exit(0)`을 별도 JVM 에서 확인(허용 시 종료 코드 0, 대조로 미허용 시 3) |
| C-COOKIEJAR-1~4 | 쿠키 저장소 공유와 삭제(5.24, P31-04) | `CookieJarTest`: 1 같은 저장소의 컴포넌트가 요청과 `document.cookie` 에서 쿠키를 공유하고 다른 저장소·자기 저장소에는 보이지 않음, 2 폐기해도 공유 저장소는 남고 폐기된 컴포넌트는 빈 상태, 3 `clean()` 은 유지·`clearCookies()` 와 `CookieJar.clear()` 는 삭제, 4 null 저장소와 폐기 뒤 호출의 예외 |
| C-TITLE-1~3 | 살아 있는 문서의 제목(5.25, P31-04) | `LiveTitleTest`: 1 읽은 뒤 `document.title`·텍스트 노드·요소 제거와 재생성이 `getTitle()` 과 EDT 의 `title` 속성에 순서대로 반영, 2 파싱 중에 바꾼 제목이 확정 뒤 반영, 3 프레임 문서의 제목은 영향 없고 프레임이 바꾼 부모 제목은 반영 |
| C-EDITCMD-1~5 | 호스트의 편집 명령과 명령 키 소비(5.26, P31-05-a) | `core.EditCommandsTest`: 1 편집 호스트에서 모두 선택·복사·잘라내기·붙여넣기와 이벤트 순서, 키 이벤트 없음, 2 폼 텍스트 컨트롤, 3 편집할 수 없는 페이지와 다른 스레드의 호출, 4 폐기 뒤 예외, 5 소비하는 키와 소비하지 않는 키 |

| C-FONT-1~5 | 컴포넌트별 기본 글꼴(5.27, P31-07) | `DefaultFontsTest`: 1 기본 크기와 크기 키워드·루트 백분율·rem, 살아 있는 페이지의 재계산, 2 표준 글꼴과 작성자 규칙, 3 일반 이름의 패밀리와 설치되지 않은 패밀리, 4 고정폭 크기(제목 안, 중첩, 작성자 규칙), 5 인수·통지·`clean()`·프레임·다른 컴포넌트·폐기 |
| C-POPUP-1~10 | 페이지가 여는 창과 팝업 차단(5.28, P31-08) | `PopupWindowsTest`: 1 차단과 사용자 조작(한 번에 창 하나, 설정 끔, 속성 통지, 창 기능), 2 여는 창의 제어(동기 `about:blank`, `document.write`, DOM, `location`, `opener`, sessionStorage 복사, 이름 재사용), 3 닫기(여는 창·자신·읽는 중, 언로드 한 번, 본 창 정책 미사용), 4 `noopener` 와 다른 오리진(메시지 왕복, 접근 거부, 여는 창의 닫기), 5 새 창이 필요한 링크·폼(제안, 기존 동작, POST 본문, 스크립트 제출 차단), 6 사용자의 선택(Ctrl·Shift·가운데 단추, 취소된 클릭), 7 수명(거절, 여는 창 폐기, localStorage 유지, 폐기 뒤 예외, 스레드 종료), 8 한계(창을 보일 수 없음, 파싱 중 호출, 샌드박스), 9 COOP 로 관계 끊기, 10 기본 팝업 대화 상자(실제 창). `core.DocumentLoopTest`: 공유 문서 루프 |

사례 ID는 구현 시 `browser/src/test/java`의 메서드 이름에 포함하고 `FEATURE_MATRIX.md`의 검증 사례 열과 일치시킨다.
