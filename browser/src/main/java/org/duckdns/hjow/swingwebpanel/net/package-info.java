/**
 * Networking: URL handling, HTTP/HTTPS fetcher, cookies, origins, anonymous FTP client, classpath loader and resource models.
 * <p>
 * Entry point: {@link org.duckdns.hjow.swingwebpanel.net.ResourceFetcher} (dispatches http, https, ftp and classpath). Policies implemented here:
 * <ul>
 * <li><b>Schemes</b>: only http, https, ftp and classpath are fetched ({@link org.duckdns.hjow.swingwebpanel.net.SchemePolicy}); the check runs for
 * the first URL, every redirect target and every sub-resource, and a blocked URL never opens a connection. An http response may
 * not redirect to ftp or classpath.</li>
 * <li><b>https</b>: {@link org.duckdns.hjow.swingwebpanel.net.SslConfigurer} is called for every https connection. A failure at connect or TLS level
 * (including certificate or host name errors, as the project requires) is retried exactly once over http on the same host
 * (443 becomes 80, another explicit port is kept). The plain request carries no Secure cookie and no
 * <code>Authorization</code>/<code>Proxy-Authorization</code> header; all other extra headers are still sent. An HTTP error status is
 * never retried.</li>
 * <li><b>Headers</b>: User-Agent and the component's extra headers are applied by one request layer to every purpose; the
 * <code>Cookie</code> header always comes from the cookie store. Authorization style headers are not forwarded to another
 * origin after a redirect.</li>
 * <li><b>Limits</b>: see {@link org.duckdns.hjow.swingwebpanel.net.FetchLimits} (connect 15 s, read 30 s, 20 redirects, 16 MiB in memory,
 * unlimited when streamed to a download).</li>
 * <li><b>Known limits</b>: the JRE <code>HttpURLConnection</code> silently drops the restricted <code>Origin</code> request header, so a request that must carry
 * it (a CORS request, its preflight, a POST/PUT/DELETE of a page) uses the socket based <code>SocketHttpURLConnection</code> unless a
 * proxy of the JVM applies (then the header is lost as before; image and style sheet requests carry no <code>Origin</code>),
 * JRE 6u45 speaks only TLS 1.0 and sends no SNI, and no public suffix list exists for cookie Domain checks.</li>
 * </ul>
 * <p>
 * 네트워크: URL 처리, HTTP/HTTPS 요청기, 쿠키, 오리진, 익명 FTP 클라이언트, 클래스패스 로더, 리소스 모델.
 * <p>
 * 진입점은 {@link org.duckdns.hjow.swingwebpanel.net.ResourceFetcher}(http, https, ftp, classpath 로 분기)이다. 여기서 구현한 정책:
 * <ul>
 * <li><b>스킴</b>: http, https, ftp, classpath 만 가져온다({@link org.duckdns.hjow.swingwebpanel.net.SchemePolicy}). 최초 URL, 모든 리디렉션 대상,
 * 모든 하위 리소스에 검사하며 차단된 URL 은 연결을 열지 않는다. http 응답은 ftp 나 classpath 로 리디렉션할 수 없다.</li>
 * <li><b>https</b>: 모든 https 연결에 {@link org.duckdns.hjow.swingwebpanel.net.SslConfigurer} 를 호출한다. 연결 또는 TLS 수준의 실패(프로젝트 요구에
 * 따라 인증서/호스트 이름 오류 포함)는 같은 호스트로 정확히 한 번 http 재시도를 한다(443 은 80 으로, 다른 명시 포트는 유지).
 * 평문 요청에는 Secure 쿠키와 <code>Authorization</code>/<code>Proxy-Authorization</code> 헤더가 없고 다른 추가 헤더는 그대로 보낸다.
 * HTTP 오류 상태는 재시도하지 않는다.</li>
 * <li><b>헤더</b>: User-Agent 와 컴포넌트의 추가 헤더는 하나의 요청 계층이 모든 목적에 적용하며 <code>Cookie</code> 헤더는 항상
 * 쿠키 저장소가 만든다. 인증 계열 헤더는 리디렉션 뒤 다른 오리진으로 전달하지 않는다.</li>
 * <li><b>제한</b>: {@link org.duckdns.hjow.swingwebpanel.net.FetchLimits} 참조(연결 15초, 읽기 30초, 리디렉션 20회, 메모리 16 MiB, 다운로드로
 * 스트리밍하면 무제한).</li>
 * <li><b>알려진 한계</b>: JRE 의 <code>HttpURLConnection</code> 이 제한 헤더 <code>Origin</code> 을 조용히 버리므로 그것을 실어야 하는 요청(CORS 요청과 그 사전 요청,
 * 페이지의 POST/PUT/DELETE)은 JVM 의 프록시가 적용되지 않는 한 소켓 기반 <code>SocketHttpURLConnection</code> 으로 보내며(프록시가 있으면 예전처럼
 * 헤더가 사라지고, 이미지와 스타일시트 요청은 <code>Origin</code> 을 싣지 않는다), JRE 6u45 는 TLS 1.0 만
 * 쓰며 SNI 를 보내지 않고, 쿠키 Domain 검사용 공개 접미사 목록이 없다.</li>
 * </ul>
 */
package org.duckdns.hjow.swingwebpanel.net;
