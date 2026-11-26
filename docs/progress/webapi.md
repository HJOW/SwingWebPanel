# WEBAPI 진행 기록 (P20-08 비동기 웹 요청 API)

작업자: WEBAPI. 소유 경로: `src/org/duckdns/hjow/swingwebpanel/js/host/web/**`, `src/org/duckdns/hjow/swingwebpanel/js/host/{WebRequest,WebResponse,WebRequestSource}.java`, `src/org/duckdns/hjow/swingwebpanel/core/PageWebRequests.java`,
`tests/src/org/duckdns/hjow/swingwebpanel/js/host/web/**`, `tests/src/org/duckdns/hjow/swingwebpanel/core/WebRequestApiTest.java`, `tests/resources/org/duckdns/hjow/swingwebpanel/webapi/**`, 이 파일.
소유 경로 밖의 작은 추가 변경(아래 "변경 파일" 참고): `js/host/{HostEnvironment,PageScripts,WindowGlobals}.java`, `core/PageRuntime.java`, `net/{ResourceRequest,HttpFetcher}.java`.
빌드/시험: `scripts/snaptest.sh webapi "<소유 경로들>" "js dom html css net storage core content layout render download media @root" "js core" --filter <시험 이름>`.

## 설계 요약

- **서비스 계약** `js.host.WebRequestSource`(`HostEnvironment.getWebRequestSource()`): 페이지가 `WebRequest`(URL, 메서드, 작성자 헤더, 본문, 자격 증명 모드, 요청 모드, 캐시 모드, 리디렉션 모드, 참조/참조 정책)를 주면
  컴포넌트가 요청 스레드에서 실행하고 `Callback.onHeaders/onProgress/onComplete` 로 `WebResponse`(basic/cors/opaque/error 로 이미 걸러짐)를 돌려준다. 네트워크 정책은 모두 컴포넌트 쪽(`core.PageWebRequests`)이 집행한다.
- **`core.PageWebRequests`**: 주 문서/스크립트/스타일/이미지와 같은 `ResourceFetcher` 를 쓴다. 따라서 User-Agent, 추가 헤더, SSL 훅, HTTP 캐시, 리디렉션·크기 제한, 취소가 그대로 적용된다.
  쿠키는 컴포넌트의 `CookieStore` 를 자격 증명 모드(omit/same-origin/include)에 따라 쓴다. 스킴은 http/https(와 클래스패스 페이지의 classpath)만, 동일 출처 정책, CORS 응답 검사(`OriginPolicy`),
  사전 요청 미지원(안전하지 않은 교차 오리진 요청은 아무것도 보내기 전에 네트워크 오류), 응답 헤더 거르기(Set-Cookie 숨김, CORS 응답은 안전 목록 + `Access-Control-Expose-Headers`), 참조 정책, 최대 6개 동시 실행/256개 존재 제한,
  페이지 리소스 취소 토큰(`PageRuntime.resources`)에 연결된 요청별 취소.
- **`js.host.web.WebApis`**(페이지마다 하나, `WindowGlobals.install()` 끝에서 설치): 결과를 `inbox` 큐에 줄 세우고 페이지의 `Scheduler` 로 한 건씩 `PageScripts.runTask` 페이지 작업으로 실행한다(마이크로태스크 검사점 포함).
  요청 목록을 관리하여 `PageScripts.requestDisposal()`(비차단) → `WindowGlobals.cancelRequests()` → `WebApis.cancelAll()` 로 모두 취소하고, 폐기/스크립트 중단 뒤 도착한 결과는 실행하지 않는다.
- **JS 인터페이스**: `Natives`(인터페이스 정의, 리플렉션으로 설치기 메서드를 스크립트 함수에 묶기, WebIDL 변환), `Promises`(realm 의 `%Promise%` 로 Java 에서 만드는 프라미스),
  `AbortApi/AbortSignal`, `BlobApi`(Blob/File), `SearchParamsApi`(URLSearchParams), `UrlApi`(URL), `FormDataApi`(FormData + multipart 인코더), `HeadersApi/HeaderRules`(Headers + guard + Fetch 헤더 규칙),
  `BodyApi`(BodyInit 추출 + Body mixin), `FetchApi`(Request/Response/fetch), `XhrApi/Xhr/XhrEventTarget/XhrUpload/ProgressEvent`(XMLHttpRequest).
  EventTarget 인 것(XHR, XHRUpload, AbortSignal)은 기존 `DomBridge` 의 래퍼/`EventHandlers`(on* 핸들러)를 쓰고 나머지는 `JsObject` 하위 클래스이다.
  Java 클래스/리플렉션 객체는 스크립트에 노출되지 않는다.

## 규격 조회 기록 (`tools/webfetch`, JRE Zulu25 openjdk 25, 조회일 2026-11-16)

| 규격 | URL | 서버의 Last-Modified |
| --- | --- | --- |
| XMLHttpRequest Standard | https://xhr.spec.whatwg.org/ | Wed, 11 Nov 2026 07:31:22 GMT |
| Fetch Standard | https://fetch.spec.whatwg.org/ | Sat, 14 Nov 2026 16:09:49 GMT |
| URL Standard | https://url.spec.whatwg.org/ | Sat, 14 Nov 2026 09:16:36 GMT |
| DOM Standard (AbortController/AbortSignal) | https://dom.spec.whatwg.org/ | Sun, 15 Nov 2026 07:40:07 GMT |

(WHATWG Living Standard 라서 판본은 조회 시점의 본문이다. 이 조회본을 기준으로 구현했다.)

## 작업 로그 (최신이 위)

### P20-08-a/b · 비동기 웹 요청 API 전체(서비스 계층, XMLHttpRequest, fetch, AbortController, FormData, Blob/File/FileReader, URL/URLSearchParams, TextEncoder/TextDecoder)

- **현재 작업 번호**: P20-08 (대상 표 WEB-06, WEB-07 의 API 전부 구현·검증. 아래 "지원하지 않는 것" 은 문서화된 예외)
- **완료한 내용**
  - `js.host.WebRequestSource/WebRequest/WebResponse` 서비스 계약과 `HostEnvironment.getWebRequestSource()` 훅, `core.PageWebRequests`(구현), `PageRuntime` 연결.
  - **XMLHttpRequest**: open/setRequestHeader/send/abort, readyState/status/statusText/responseURL/getResponseHeader/getAllResponseHeaders/overrideMimeType/timeout/withCredentials/upload,
    responseType ""·text·json·arraybuffer·blob·document, response/responseText/responseXML(HTML 은 `document` 형식일 때, XML 은 자체 `XhtmlParser`), 이벤트 readystatechange·loadstart·progress·abort·error·load·timeout·loadend 와 on* 핸들러,
    `ProgressEvent`/`XMLHttpRequestUpload`. 동기 모드는 `InvalidAccessError`(표준이 페이지에 허용), `open/setRequestHeader/send` 의 표준 예외(SyntaxError, SecurityError, InvalidStateError, InvalidAccessError).
  - **fetch()/Request/Response/Headers**: Headers guard(none·request·request-no-cors·response·immutable)와 Fetch 의 금지 헤더 규칙, Body mixin(text·json·arrayBuffer·bytes·blob·formData·bodyUsed·clone), `Response.error/redirect/json`,
    옵션 method·headers·body·credentials·mode·cache·redirect·referrer·referrerPolicy·integrity(SHA-256/384/512 실제 검증)·signal·keepalive·priority, 응답 type basic/cors/opaque, 네트워크 오류는 `TypeError` + 콘솔에 이유 기록.
  - **AbortController/AbortSignal**(abort/timeout/any/throwIfAborted/onabort/reason, fetch 와 연결), **FormData**(+ `new FormData(form)`, multipart 인코더/파서), **Blob/File/FileReader**, **URLSearchParams**, **URL**(canParse/parse, IDN, 설정자),
    **TextEncoder/TextDecoder**(Encoding 표준 레이블, fatal, ignoreBOM, 스트리밍).
  - **공통 요청 계층 연결**: User-Agent(컴포넌트 값, 페이지가 못 바꿈), 추가 헤더(페이지 헤더가 같은 이름을 덮어씀), 쿠키(컴포넌트 CookieStore 를 자격 증명 모드 omit/same-origin/include 로 제어), SSL 훅(`configureSsl` 이 스크립트 요청에도 호출됨),
    스킴 제한(http/https 와 클래스패스 페이지의 classpath 만; ftp/file/data/javascript/about/blob/ws/mailto 는 접속 없이 네트워크 오류), HTTP 캐시와 cache 옵션, 동일 출처 정책/CORS 응답 검사(`OriginPolicy`), 응답 헤더 거르기(Set-Cookie 숨김,
    CORS 응답은 안전 목록 + `Access-Control-Expose-Headers`), 참조 정책(기본 strict-origin-when-cross-origin), 동시 6개/존재 256개 제한, 페이지 리소스 토큰과 `PageScripts.requestDisposal()` 로 취소.
  - net 계층에 작은 추가: **same-origin 자격 증명 모드는 리디렉션으로 다른 오리진에 간 구간에 쿠키를 보내거나 저장하지 않는다**(`ResourceRequest.setSameOriginCredentialsOnly`, `HttpFetcher.credentialsFor`),
    **페이지가 쓴 헤더에만 CORS 단순 요청 규칙을 적용**(`ResourceRequest.setCorsHeadersChecked`; 컴포넌트의 추가 헤더가 교차 오리진 요청을 사전 요청 필요로 만들지 않게).
- **변경 파일**
  - 새 파일: `src/org/duckdns/hjow/swingwebpanel/js/host/{WebRequest,WebResponse,WebRequestSource}.java`, `src/org/duckdns/hjow/swingwebpanel/js/host/web/*.java`(AbortApi, AbortSignal, BlobApi, BodyApi, EncodingApi, Encodings, FetchApi, FileReaderApi, FormDataApi,
    HeaderRules, HeadersApi, Natives, ProgressEvent, Promises, SearchParamsApi, UrlApi, WebApis, Xhr, XhrApi, XhrEventTarget, XhrUpload), `src/org/duckdns/hjow/swingwebpanel/core/PageWebRequests.java`,
    `tests/src/org/duckdns/hjow/swingwebpanel/js/host/web/{WebFixture,WebTypesTest,XhrTest,FetchTest}.java`, `tests/src/org/duckdns/hjow/swingwebpanel/core/WebRequestApiTest.java`, `tests/resources/org/duckdns/hjow/swingwebpanel/webapi/{page.html,data.txt}`, 이 파일.
  - 소유 경로 밖의 작은 추가 변경: `js/host/HostEnvironment.java`(+`getWebRequestSource`), `js/host/PageScripts.java`(requestDisposal 에 `globals.cancelRequests()` 한 줄), `js/host/WindowGlobals.java`(WebApis 설치/취소/폐기),
    `core/PageRuntime.java`(`PageWebRequests` 생성과 `getWebRequestSource`), `net/ResourceRequest.java`(+`sameOriginCredentialsOnly`, `corsHeadersChecked`), `net/HttpFetcher.java`(+`credentialsFor`, 쿠키 전송/저장 두 곳, 단순 요청 검사 조건 한 줄).
- **검증 결과** (JDK 6u45 / JRE 6 에서 실제 실행, `scripts/snaptest.sh webapi "<소유 경로>" "js dom html css net storage core content layout render download media @root" "js core" --filter <이름>` 의 격리 스냅샷)
  - `--filter org.duckdns.hjow.swingwebpanel.js.host.web` → 36 passed / 0 failed (WebTypesTest 10, XhrTest 15, FetchTest 11 — 가짜 요청 서비스와 수동 시계로 결정적).
  - `--filter org.duckdns.hjow.swingwebpanel.core.WebRequestApiTest` → 15 passed / 0 failed (실제 컴포넌트 + 실제 HTTP/HTTPS, 포트가 다른 두 서버 = 두 오리진: 컴포넌트 UA/추가 헤더/쿠키, 금지 헤더, CORS 허용/거부/자격 증명/노출 헤더/no-cors/same-origin,
    사전 요청 미지원 시 서버에 아무것도 가지 않음, 자격 증명 모드와 리디렉션, 스킴 제한 18건, 페이지 교체/폐기 중 요청 취소와 늦은 응답 무실행, 상태/리디렉션/HEAD/gzip/제한 시간, 보낼 수 없는 메서드, 클래스패스 페이지,
    참조 정책, 캐시 모드, SSL 훅, 컴포넌트 헤더).
  - 최종 회귀(전체 스냅샷 `--filter swingwebpanel`): 789 passed / 1 failed / 1 skipped. 실패 1건은 소유 경로 밖의 기존 시험 `PageScriptsTest.testNavigatorAndGlobals` 가 `typeof XMLHttpRequest` 가 `undefined` 이기를 기대하는 것(조정자 요청 1). skip 1건은 JAR 미빌드(`JarContentTest`).
  - `tools/sourcecheck/run.sh` → 변경/추가한 파일 모두 0 위반.
- **지원하지 않는 것 / 알려진 차이** (무시하지 않고 오류나 거부로 알린다)
  - **CORS 사전 요청(preflight)**: 지원하지 않는다. 안전 목록 밖의 메서드(PUT/DELETE/PATCH 등)나 헤더(Content-Type: application/json 포함)를 가진 교차 오리진 요청은 **아무것도 보내기 전에** 네트워크 오류(fetch 는 `TypeError`, XHR 은 `error` 이벤트)이다.
    JRE `HttpURLConnection` 이 `Origin`, `Access-Control-Request-Method/Headers` 를 제한 헤더로 보내지 않아(JRE 6 에서 확인된 제약) 이 net 계층으로는 사전 요청을 보낼 수 없다. 같은 이유로 **교차 오리진 요청에 `Origin` 헤더가 실제로는 전송되지 않을 수 있어**
    `Origin` 을 확인해야 `Access-Control-Allow-Origin` 을 내는 서버는 허용 응답을 주지 않는다(`*` 또는 고정 오리진을 주는 서버는 동작).
  - 동기 XHR(`open(..., false)`) 은 `InvalidAccessError`. `PATCH` 등 `HttpURLConnection` 이 보낼 수 없는 메서드와 DELETE/OPTIONS/PATCH 의 본문은 요청을 조용히 바꾸지 않고 네트워크 오류. 본문은 POST/PUT 에만 전송.
  - `fetch`의 `redirect: "manual"`과 `cache: "only-if-cached"`는 `TypeError`로 거부한다. `no-cache`는 `reload`와 같게 처리하며 `keepalive`·`priority`·`duplex`는 효과가 없다. 요청 본문 스트리밍은 지원하지 않는다.
  - P28-06-b2에서 `Response.body`에 기본 `ReadableStream`을 연결했다. HTTP 응답은 필터링을 마친 헤더 뒤에 디코딩 청크를 전달한다. 캐시와 16 MiB 한도를 유지하며 무결성 검사 및 클래스패스의 본문은 일괄 적재 뒤 청크로 제공한다. 기본 리더, 비동기 반복, 응답 복제 및 전송 중 취소를 Java 6에서 검증했다. BYOB 리더, 파이프, 변환 및 스트림 전송은 미지원이다. [Streams Standard 4.2~4.4절](https://streams.spec.whatwg.org/)은 2026-11-26에 Last-Modified 2026-10-25 14:00:25 GMT 판본을 확인했다.
  - XHR: 본문은 완성된 뒤 한 번에 전달된다(LOADING 동안 `responseText` 는 빈 문자열, progress 는 전송 바이트 수만). 업로드 이벤트(`xhr.upload`)는 서버가 응답을 시작할 때 한 번에 발생한다(요청 계층이 전송 진행을 알리지 않음).
    `open()` 의 username/password 인수는 무시한다. `send(Document)` 는 루트 요소의 outerHTML 로 직렬화한다(doctype 제외).
  - 교차 오리진 리디렉션의 중간 구간은 CORS 검사를 하지 않고 최종 응답만 검사한다. `FormData(form)` 은 컨트롤의 콘텐츠 속성(value/checked/selected/disabled)만 읽는다(실제 사용자 입력 상태는 P11-05 후속).
  - URL: 전체 URL 표준 알고리즘이 아니라 `UrlUtil`(RFC 3986) 위의 해석이다. 특이한 IPv4 표기(`0x7f.1`), 구성요소별 전체 퍼센트 인코딩 집합, blob: URL(`URL.createObjectURL`)은 없다.
  - `Blob.stream()` 없음, `FileReader.error` 는 항상 null, `Request.destination` 은 항상 "". Worker 안의 fetch 는 이 항목의 범위 밖(P20-09).
- **미해결 사항**: 위 지원하지 않는 것 외에 실패하는 사례 없음. 사전 요청을 지원하려면 `HttpURLConnection` 이 아닌 자체 HTTP 클라이언트(소켓)가 필요하며 별도 작업이다.
- **다음 작업 번호**: 이 영역(P20-08)은 조정자 판단 대기. 후속은 P20-09(Worker/message), P20-07(history.pushState).
- **조정자 요청**
  1. `tests/src/org/duckdns/hjow/swingwebpanel/js/host/PageScriptsTest.java:360` 의 `testNavigatorAndGlobals` 가 `typeof XMLHttpRequest` 가 `undefined` 이기를 기대한다(주석: "XMLHttpRequest comes with P20"). 이제 `function` 이므로 기대를 `undefined,undefined,function` 으로(또는 `typeof Java/java/Packages` 만 검사하도록) 고쳐야 한다. 소유 경로 밖이라 고치지 않았다.
  2. FEATURE_MATRIX: WEB-06 은 `js.host.web.Xhr/XhrApi/XhrEventTarget/XhrUpload/ProgressEvent`, `core.PageWebRequests` 로 `검증 완료`(제약: 동기 XHR 거부, 사전 요청 미지원, 본문 일괄 전달). WEB-07 은 `js.host.web.*`(FetchApi, HeadersApi, BodyApi, UrlApi, SearchParamsApi, FormDataApi, BlobApi, FileReaderApi, EncodingApi)로 `검증 완료`(제약: 스트림 없음, URL 은 RFC 3986 기반).
     DOM-12 의 AbortController/AbortSignal 은 `js.host.web.AbortApi/AbortSignal` 로 검증 완료. WEB-12 의 CORS 는 `core.PageWebRequests` + `net.OriginPolicy`(사전 요청 미지원).
  3. ARCHITECTURE: `js.host.web` 패키지(페이지의 웹 요청 API), `js.host.WebRequestSource` 서비스, `core.PageWebRequests`, `net.ResourceRequest` 의 새 표시 둘(`sameOriginCredentialsOnly`, `corsHeadersChecked`)을 추가해 주세요.
  4. `net.OriginPolicy` Javadoc 의 "계획 P20-08 에서 사전 요청 지원" 문구는 "JRE HttpURLConnection 제약으로 지원하지 않음" 으로 정리해야 한다(net 소유자).
- **체크박스 후보**: **P20-08** (대상 표 WEB-06 XMLHttpRequest 와 WEB-07 Fetch/Request/Response/Headers/URL/URLSearchParams/TextEncoder/TextDecoder/Blob/File/FormData/FileReader 를 모두 구현하고 JRE 6 에서 검증.
  User-Agent/쿠키/SSL 훅/스킴 제한을 우회하지 못함을 실제 HTTP/HTTPS 로 확인). 단 위 "지원하지 않는 것"(특히 CORS 사전 요청 미지원, 동기 XHR 거부)을 예외로 받아들일 때만 체크할 것. 받아들이지 않으면 P20-08 은 체크하지 말고 이 작업을 "사전 요청을 뺀 P20-08" 로 기록.
