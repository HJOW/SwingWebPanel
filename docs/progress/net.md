# NET 작업 진행 기록 (P02-06, P04, P05)

담당 경로: `src/org/duckdns/hjow/swingwebpanel/net/**`, `tests/src/org/duckdns/hjow/swingwebpanel/net/**`, `tests/src/org/duckdns/hjow/swingwebpanel/testing/Test{Http,Ftp}Server.java`, `TestSsl.java`,
`tests/resources/test-keystore.jks`, 이 문서. 빌드/시험: `scripts/pkgtest.sh net "net" "net"` (JDK 6u45).

## 작업 단위 기록 (최신이 위)

### P04-02 · P05-01 ~ P05-07 · FTP, 클래스패스, 단일 진입점 — 2026-11-16

```text
현재 작업 번호: P04-02, P05-01~P05-07 (NET 담당 범위 전체 완료)
완료한 내용: FtpClient(익명 전용, 여러 줄 응답, EPSV→PASV, PASV 주소/포트 검증, MLSD→LIST, CR/LF/NUL 거부, 취소/제한 시간), FtpListParser(UNIX/DOS/MLSD,
             공백/비ASCII 이름, 링크, 해석 불가 줄 보관), FtpListing(이스케이프, ./ 접두 퍼센트 인코딩 링크, ../ 첫 줄, 루트 위 없음), FtpFetcher
             (디렉터리/파일 판정, 파일 MIME, 실패=404/FTP_FAILURE), ClasspathLoader(클래스 로더 스트림만, JAR 포함, 퍼센트 디코딩 1회, 루트 탈출/제어 문자/
             스킴 모양 첫 세그먼트/디렉터리 거부), ResourceFetcher(스킴 분기, 원격 문서의 classpath 접근 차단, about:blank), TestFtpServer
변경 파일: src/org/duckdns/hjow/swingwebpanel/net/{FtpClient,FtpEntry,FtpException,FtpListParser,FtpListing,FtpFetcher,ClasspathLoader,ResourceFetcher}.java,
           tests/src/org/duckdns/hjow/swingwebpanel/testing/TestFtpServer.java, tests/src/org/duckdns/hjow/swingwebpanel/net/{FtpTest,ClasspathAndDispatchTest}.java
검증 결과: scripts/pkgtest.sh net "net" "net" (JDK 6u45) → 99 passed, 0 failed, 1 skipped(Origin 헤더 제한), tools/sourcecheck 0 violations (43 파일)
  - FTP: UNIX/DOS/MLSD 해석, 해석 불가 줄은 UNKNOWN+진단(디렉터리로 추측 안 함), 목록 HTML(이스케이프/링크 인코딩/정렬/../), 루트·하위(공백+한글)·상위·루트 위
    이동, MLSD 미지원→LIST, EPSV 미지원→PASV, 여러 줄 인사말, 잘못된 PASV 주소(10.9.9.9)는 무시하고 제어 연결 주소 사용, 불가능한 PASV 숫자(300)는 거부,
    파일(MIME, 한글/공백 이름, SIZE, 스트리밍, 진행률), 실패(없는 디렉터리/파일, 목록 실패, 로그인 실패, 연결 거부)=404/FTP_FAILURE,
    명령 주입(%0D%0A, %0A, %00)은 서버에 연결도 하지 않음, URL 의 사용자 정보는 쓰지 않고 USER anonymous 만, 잘린 전송 TRUNCATED, 메모리 제한 TOO_LARGE(스트리밍 제외),
    느린 전송 중 취소
  - 클래스패스: 디렉터리/JAR 리소스, 상대 URL 결합, %20 한 번만 디코딩(%2520 은 못 찾음), ../ 정규화와 루트 탈출 시도 8종, 역슬래시/NUL/file:/jar: 모양 이름,
    디렉터리와 없는 리소스는 404/NOT_FOUND, 메모리 제한/스트리밍
  - 디스패치: http/ftp/classpath/about:blank, 금지 스킴 12종은 BLOCKED_SCHEME(서버가 아무것도 받지 않음), 상대 URL 은 INVALID_URL,
    http/ftp 출처의 문서가 classpath:// 를 읽으려 하면 BLOCKED_SCHEME(클래스패스 문서/불투명/최상위는 허용)
미해결 사항:
  - 디렉터리 링크 클릭으로 실제 이동하는 것은 렌더러/이동 제어기 연결 후 다시 검증해야 한다(P05-07 문구대로 현재 검증 범위는 목록 HTML 과 ResourceFetcher 수준).
  - FTP 목록 항목의 심볼릭 링크는 링크 대상 종류를 알 수 없어 파일 모양의 링크로 표시한다. 클릭 시 FtpFetcher 가 CWD 로 디렉터리인지 먼저 판정한다.
  - 레거시 한국어 서버(EUC-KR/CP949)의 이름은 UTF-8 이 아니면 x-windows-949, EUC-KR, ISO-8859-1 순으로 해독하지만 실제 서버로 검증하지 못했다.
  - 연결 수립 중 취소는 연결 제한 시간까지 걸릴 수 있다(HTTP 와 동일한 이유; FTP 는 소켓 connect 라 CancelToken 훅이 소켓을 닫으므로 즉시 끝나는 경우가 많음).
다음 작업 번호: NET 담당 범위 끝. 후속(조정자): SwingWebPanel 연결(P03/P11-09), 캐시/조건부 요청(P19-03/04), 소켓 기반 HTTP 클라이언트(P20-08, Origin 헤더)
조정자 요청: 아래 'Public API for integration' 의 호출 규칙과 SSL 훅 연결, JRE 6 제한 기록(FEATURE_MATRIX NET-07/P22-07)
체크박스 후보: P04-02, P05-01, P05-02, P05-03, P05-04, P05-05, P05-06, P05-07
```

### P04-03 ~ P04-10 · P04-12(HTTP/HTTPS 부분) · HttpFetcher 와 SSL 확장 지점 — 2026-11-16

```text
현재 작업 번호: P04-03~P04-10, P04-12
완료한 내용: HttpFetcher(수동 리디렉션, 쿠키, gzip/deflate(zlib/raw), 크기/시간 제한, 취소, 진행률, ResponseHandler 스트리밍, 오류 본문 유지,
             HTTPS→HTTP 일회성 재시도, 요청 목적별 Accept/Referer/Origin/캐시 헤더), SslConfigurer + DefaultSslConfigurer(연결별 프로토콜 선택),
             FailureKind.CORS(사전 요청이 필요한 교차 오리진 요청 거부)
변경 파일: src/org/duckdns/hjow/swingwebpanel/net/{HttpFetcher,SslConfigurer,DefaultSslConfigurer,FailureKind,package-info}.java,
           tests/src/org/duckdns/hjow/swingwebpanel/net/{HttpFetcherTest,HttpsFetcherTest}.java
검증 결과: scripts/pkgtest.sh net "net" "net" (JDK 6u45) → 75 passed, 0 failed, 1 skipped, tools/sourcecheck 0 violations
  - HTTP: 200/404/500 본문 유지, HEAD, UA/추가 헤더 덮어쓰기가 모든 RequestPurpose 에서 동일, 관리 헤더/Cookie 추가 헤더 무시, Referer 규칙,
    상대/절대 Location, 연쇄, 프래그먼트 상속, POST→GET(302/303)·307/308 유지, 순환/20회(시험은 5회) 제한, 금지 스킴 리디렉션(file/javascript/ftp/
    classpath/data/mailto/about)은 서버에 닿지 않음, 쿠키(리디렉션 단계에서 저장/전송, 경로 범위, HttpOnly, 컴포넌트별), Authorization 은 다른 오리진으로
    전달 안 함, gzip/zlib/raw deflate/chunked+gzip, 빈 gzip, 지원 안 하는 br → IO 실패, 크기 제한(선언/chunked/해독 후), 스트리밍은 제한 없음,
    잘림(짧은 Content-Length, chunked 종결 없음, 연결 끊김), 읽기 제한 시간, 연결 거부, 취소(느린 응답 중, 사전 취소), 진행률(-1 불확정)
  - HTTPS: 시험 신뢰 훅으로 성공, 훅이 주 문서/하위 리소스/리디렉션 단계마다 호출됨, protected 메서드 재정의 방식(PanelStub)과 전역 상태 불변,
    일반 HTTP 포트로 https 접속 → TLS 실패 → http 재시도 1회(최종 URL 이 실제 http URL), 신뢰할 수 없는 인증서 → TLS 실패, 재시도 후 순환 없음,
    HTTP 404/500 은 재시도 없음, 다운그레이드 요청에서 Secure 쿠키와 Authorization 제외(일반 쿠키/헤더 유지), 훅 예외 → TLS 실패
  - JRE 6u45 실측: 지원 프로토콜 [SSLv2Hello, SSLv3, TLSv1], 기본 활성 동일, DefaultSslConfigurer 선택 = [TLSv1] (TLS 1.2/SNI 없음)
미해결 사항:
  - JRE 6u45 의 HttpURLConnection 은 제한 헤더 Origin 을 조용히 버린다(시험 testRefererOriginAndCacheHeaders 가 이유와 함께 SKIP).
    sun.net.http.allowRestrictedHeaders 는 JVM 전역 속성이라 설정하지 않았다. 교차 오리진 XHR/글꼴의 Origin 헤더가 필요하면
    P20-08 에서 소켓 기반 HTTP 클라이언트를 고려해야 한다(조정자 결정 필요).
  - 연결 단계의 취소는 연결 제한 시간(기본 15초)까지 늦어질 수 있다(HttpURLConnection.disconnect 가 연결 수립 중에는 효과 없음). 응답 대기/본문 읽기 중 취소는 즉시 동작함을 시험으로 확인.
  - Windows 에서 닫힌 포트 접속은 거부까지 약 2초 걸린다(시험 시간 증가의 원인).
다음 작업 번호: P05-06/ResourceFetcher(클래스패스, 디스패치) → P04-02 최종 확인 → P05-01~
조정자 요청: SwingWebPanel 에서 `protected void configureSsl(HttpsURLConnection) throws IOException` 을 만들고 SslConfigurer 로 연결할 것
            (아래 'Public API for integration' 참조). 기본 구현은 DefaultSslConfigurer.INSTANCE.configureSsl(connection) 호출.
체크박스 후보: P04-03, P04-04, P04-05, P04-06, P04-08, P04-09, P04-10 (P04-07 은 net 계층의 훅 부분 완료, SwingWebPanel 의 protected 메서드 연결은 조정자 작업)
```

### P04-01 · P04-11 · 기초 클래스(URL/스킴/오리진/헤더/쿠키/모델) — 2026-11-16

```text
현재 작업 번호: P04-01, P04-11 (+ P04-04/P04-10 의 기반 클래스)
완료한 내용: UrlUtil(RFC 3986 결합/정규화, 이중 디코딩 없음, '+' 유지, classpath:// 정규 형식, encodeIllegal),
             SchemePolicy, Origin, OriginPolicy(+RequestPurpose: 표시 목적 vs CORS 목적), RequestHeaders, Cookie/CookieStore(RFC 6265,
             시계 주입 가능), ContentType, MimeTypes, CancelToken/CancelledException, FailureKind, ResourceRequest/ResourceResponse,
             ResponseHandler, ProgressListener, FetchLimits(문서화된 기본값)
변경 파일: src/org/duckdns/hjow/swingwebpanel/net/*.java, tests/src/org/duckdns/hjow/swingwebpanel/net/{UrlUtilTest,OriginPolicyTest,RequestHeadersTest,CookieStoreTest,ContentTypeTest}.java
검증 결과: scripts/pkgtest.sh net "net" "net" (JDK 6u45) → 47 passed, 0 failed (RFC 3986 5.4 정상/비정상 예제 전부, 스킴 분류 차단 목록,
           오리진 동일성/불투명, CORS 규칙, 헤더 덮어쓰기/UA 제외/전체 거부, 쿠키 속성/만료/삭제/정렬/Secure/HttpOnly/스크립트 보기)
           tools/sourcecheck --only encoding,forbidden,javadoc,loops → 0 violations
미해결 사항: P04-02(리디렉션/하위 리소스 적용)와 P04-04/P04-10 의 요청 계층 연결은 HttpFetcher 작성 후 검증한다.
            공개 접미사 목록이 없어 co.kr 같은 다중 레이블 공개 접미사를 Domain 으로 거부하지 못한다(알려진 한계).
다음 작업 번호: P04-03 (HttpFetcher)
조정자 요청: 없음
체크박스 후보: P04-01, P04-11
```

### P02-06 · 시험 서버(HTTP/HTTPS) — 2026-11-16

```text
현재 작업 번호: P02-06
완료한 내용: TestHttpServer(ServerSocket, 경로 등록, 요청 기록, 지연/리디렉션/chunked/gzip/deflate/잘림/끊김, 정리),
             TestSsl(시험 키 저장소 기반 서버/클라이언트 SSLContext), tests/resources/test-keystore.jks
변경 파일: tests/src/org/duckdns/hjow/swingwebpanel/testing/TestHttpServer.java, TestSsl.java, tests/resources/test-keystore.jks,
           tests/src/org/duckdns/hjow/swingwebpanel/net/TestServersTest.java
검증 결과: scripts/pkgtest.sh net "net" "net" (JDK 6u45) → TestServersTest 9 사례 + 기반 시설 6 사례 통과
           (평문/HTTPS GET, 라우팅 404/500/접두사, POST Content-Length/chunked, chunked/잘림/끊김, gzip/deflate, 지연,
            stop 후 접속 실패, stop 후 스레드 0개, 시험 신뢰만 통과/기본 신뢰는 SSLException, 평문 서버에 TLS 접속 시 즉시 SSLException)
미해결 사항: FTP 시험 서버는 P05 작업에서 추가한다. HTTPS 서버용 인증서는 JDK 6 keytool 이 -ext(SAN)를 지원하지 않아
            CN=localhost 만 있다 → HTTPS 시험은 반드시 호스트 이름 localhost 로 접속한다(127.0.0.1 은 호스트명 검증 실패).
다음 작업 번호: P04-01
조정자 요청: 없음
체크박스 후보: P02-06 (FTP 서버는 P05-07 에서 사용; P02-06 문구는 HTTP/HTTPS 서버와 정리를 요구하므로 충족)
```

- 키 저장소: `keytool -genkeypair -alias swptest -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 36500
  -keystore tests/resources/test-keystore.jks -storepass changeit -keypass changeit -dname "CN=localhost, OU=SwingWebPanel Test, O=Test, C=KR"`
  (JDK 6u45 keytool). 시험 범위에만 적용되며 JVM 전역 신뢰/`SSLContext`/`HttpsURLConnection` 기본값을 바꾸지 않는다.
- Windows 에서 닫힌 로컬 포트에 접속하면 연결 실패까지 약 2초 걸린다(시험 시간이 길어 보이는 원인).

## Public API for integration (조정자용)

모든 클래스는 `org.duckdns.hjow.swingwebpanel.net` 패키지이며 JDK 6 으로 컴파일된다. 스레드: **가져오기는 호출한 스레드를 막는다. EDT 에서 부르지 말고 문서 소유 스레드/작업 스레드에서 호출한다.**
`ResourceFetcher.fetch` 는 네트워크/파일 문제로 예외를 던지지 않고 `ResourceResponse` 의 `getFailureKind()` 로 알린다. 취소는 어느 스레드에서나 `CancelToken.cancel()`.

### 컴포넌트가 만들어 두는 것 (컴포넌트당 하나)

```java
CookieStore cookies = new CookieStore();                 // getCookies(): cookies.getCookies()
RequestHeaders headers = new RequestHeaders();           // addHeaders(Map): headers.addAll(map)  (IAE 시 부분 적용 없음, User-Agent 무시)  getHeaders(): headers.toMap()
String userAgent = "swingwebpanel/1.0";                  // ResourceRequest.DEFAULT_USER_AGENT
ResourceFetcher fetcher = new ResourceFetcher(sslConfigurer, FetchLimits.DEFAULT, classLoader);
```

### SwingWebPanel 의 SSL 확장 지점 연결

```java
protected void configureSsl(HttpsURLConnection c) throws IOException { DefaultSslConfigurer.INSTANCE.configureSsl(c); }   // 호스트가 재정의
SslConfigurer ssl = new SslConfigurer() { public void configureSsl(HttpsURLConnection c) throws IOException { SwingWebPanel.this.configureSsl(c); } };
```
모든 https 연결(주 문서, 리디렉션, 하위 리소스)에 호출된다. 전역 SSL 상태를 바꾸지 말 것. HTTPS 실패 시 http 재시도는 `fetcher.getHttpFetcher().setHttpFallbackEnabled(..)` (기본 켜짐, 프로젝트 요구).

### 요청 만들기와 실행

```java
ResourceRequest req = new ResourceRequest(url)                  // 절대 URL (이동 제어기가 about:blank/about:text 는 직접 처리)
    .setUserAgent(userAgent).setExtraHeaders(headers).setCookies(cookies)
    .setPurpose(RequestPurpose.DOCUMENT)                        // IMAGE/AUDIO/STYLESHEET/SCRIPT/FONT/XHR/FRAME/DOWNLOAD/OTHER
    .setCacheMode(force ? ResourceRequest.CacheMode.FORCE_RELOAD : ResourceRequest.CacheMode.DEFAULT)   // refresh(true)
    .setCancelToken(token).setReferrer(pageUrl).setInitiator(Origin.of(pageUrl))   // initiator: 하위 리소스/XHR 에서 필수(classpath 보호, Origin 헤더)
    .setProgressListener(listener);                             // (received, total) total=-1 이면 진행률 불확정
ResourceResponse r = fetcher.fetch(req);                        // 메모리. 크기 제한 16MiB(FetchLimits)
ResourceResponse r2 = fetcher.fetch(req, handler);              // 다운로드: handler.onResponse(head) 가 OutputStream 을 돌려주면 제한 없이 스트리밍(상태 400 미만만)
```

### 응답 읽기

- `getStatus()` (HTTP 상태; classpath/FTP 는 200 또는 404, 응답 자체가 없으면 0), `getFailureKind()`(NONE 이 아니면 전송 실패: DNS/CONNECT/TIMEOUT/TLS/TOO_MANY_REDIRECTS/
  REDIRECT_LOOP/TOO_LARGE/TRUNCATED/CANCELLED/NOT_FOUND/FTP_FAILURE/BLOCKED_SCHEME/INVALID_URL/CORS/IO), `isHttpError()` (4xx/5xx, 본문 유지), `isError()`.
- `getBody()`(압축 해제됨), `getMimeType()`(소문자, 매개변수 없음), `getCharset()`(Content-Type 의 charset 또는 null → html 계층이 BOM/meta 로 판정),
  `getFinalUrl()`(리디렉션/FTP 슬래시/https→http 재시도 반영, 프래그먼트 포함), `getRedirects()`, `isHttpFallbackUsed()`, `getHeader(name)`/`getHeaders(name)`.
- 다운로드 판정(P01-18)에 쓸 것: `isAttachment()`(Content-Disposition), `getMimeType()`(표시 가능 형식 여부는 content 계층), FTP 파일은 `application/octet-stream`.
- FTP 디렉터리: `isDirectoryListing()` + HTML 본문(UTF-8). 실패는 status 404 + FTP_FAILURE → content 계층이 404 화면으로.
- `getContent()` 용 원문은 `getBody()` 를 문자셋으로 디코딩해 보관(응답 객체가 DOM 이나 변환 결과로 바뀌지 않음).

### 쿠키와 스크립트

- `cookies.getDocumentCookie(documentUrl)` = `document.cookie` 읽기(HttpOnly 제외), `cookies.setFromScript(documentUrl, "k=v; Path=/")` = 쓰기(HttpOnly 속성은 거부).
- `CookieStore.getCookies()` 형식은 `name=value; name=value` (도메인, 경로, 이름 순, 만료 제외).
- 교차 오리진 스크립트 접근: `OriginPolicy.mayScriptReadBody(initiator, Origin.of(r.getFinalUrl()), purpose, r, withCredentials)`, 표시/적용: `OriginPolicy.mayUse(...)`.

### 이동/URL

- 문서 기준 주소 결합은 항상 `UrlUtil.resolve(baseUrl, ref)` (이중 디코딩 없음, `+` 유지, classpath 정규 형식, `about:` 기준에는 IAE → "해석 실패" 처리).
- `SchemePolicy.classify(url)` 로 setPage 입력 검증(INVALID → IllegalArgumentException, BLOCKED → 오류 페이지, ABOUT_TEXT → IAE).

### 정책/제한 요약 (문서화 요청 P04-09)

- 평문 전환(https→http 재시도)이 일어나면 `r.isHttpFallbackUsed()` 이고 최종 URL 이 http 이다. 그 요청에는 Secure 쿠키, Authorization/Proxy-Authorization 이 없다. 나머지 추가 헤더는 전달한다.
  교차 오리진 리디렉션도 인증 계열 헤더를 전달하지 않는다.
- JRE 6u45: TLS 1.0 만 지원(SSLv2Hello/SSLv3/TLSv1 중 DefaultSslConfigurer 가 TLSv1 만 선택), SNI 없음 → 요즘 HTTPS 사이트는 실패하고 http 재시도로 이어질 수 있다(P22-07 기록 필요).
- JRE 의 HttpURLConnection 은 `Origin`/`Access-Control-Request-*` 요청 헤더를 보내지 못하고 `PATCH` 를 거부한다(제한 헤더). `PATCH` 는 항상 소켓 연결로 보낸다(`ResourceRequest.setMethod` 가 받는다). 그래서 `Origin` 을 싣는 요청(CORS 목적의 교차 오리진 요청, 시작한 오리진이 있는 POST/PUT/DELETE, CORS 사전 요청)은 `SocketHttpURLConnection`/`SocketHttpsURLConnection`(P20-08-c)으로 보낸다. JVM 프록시(`ProxySelector`)가 적용되면 소켓 클라이언트가 프록시를 쓰지 못하므로 JRE 연결을 쓰고 헤더는 예전처럼 사라진다. 이미지/스타일시트 요청은 `Origin` 을 싣지 않는다.
- CORS 사전 요청(P20-08-c): `HttpFetcher.preflightDenial`. 페이지가 쓴 헤더(`ResourceRequest.setCorsAuthorHeaders`)나 안전 목록 밖 메서드가 있는 교차 오리진 CORS 요청(모듈 스크립트 제외)에 대해 `OPTIONS`(쿠키 없음)를 보내고 `OriginPolicy.checkPreflight` 로 허락을 확인한다. 허락 캐시: 오리진·URL·자격 증명별, `Access-Control-Max-Age`(기본 5초, 최대 600초), 최대 256개, `HttpFetcher` 인스턴스(컴포넌트)별. 리디렉션 구간마다 다시 확인한다. 거부는 `FailureKind.CORS`.
- 소켓 연결의 TLS: 인증서 사슬은 JRE 의 기본 신뢰 저장소(또는 SSL 훅이 정한 소켓 팩터리), 호스트 이름은 `HostnameMatcher`(RFC 2818). `HttpsURLConnection` 의 기본 호스트 이름 검증기(JVM 전역)는 무시하고 연결에 설정한 검증기만 인정한다. 실패는 `SSLException`(TLS 실패 → http 재시도 대상).
- 공개 접미사 목록이 없어 쿠키 Domain 이 `co.kr` 같은 다중 레이블 공개 접미사여도 거부하지 못한다. Secure 쿠키는 https 에서만 받는다(엄격).
