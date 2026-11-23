package org.duckdns.hjow.swingwebpanel.content.error;

/**
 * The reasons for which the component shows a generated error page instead of the page that was asked for (plan item P17-06). Every
 * kind has a short title and an explanation in {@link ErrorPageBuilder}; the kind is also written into the page as a CSS class
 * (<code>swp-error-&lt;kind&gt;</code>, lower case, with hyphens) so that a host can style or recognize the page.
 * <p>
 * 컴포넌트가 요청한 페이지 대신 생성한 오류 페이지를 보이는 이유(계획 P17-06). 종류마다 {@link ErrorPageBuilder} 에 짧은 제목과 설명이 있다. 종류는
 * CSS 클래스(<code>swp-error-&lt;종류&gt;</code>, 소문자, 하이픈)로도 페이지에 쓰이므로 호스트가 페이지를 꾸미거나 알아볼 수 있다.
 */
public enum ErrorKind {

    /**
     * The server answered with an HTTP error status (4xx, 5xx) and no body that could be shown.
     * <p>
     * 서버가 HTTP 오류 상태(4xx, 5xx)로 답했고 보여 줄 본문이 없다.
     */
    HTTP_STATUS,

    /**
     * The host name could not be resolved.
     * <p>
     * 호스트 이름을 찾을 수 없다.
     */
    DNS,

    /**
     * The connection was refused or the server cannot be reached.
     * <p>
     * 연결이 거부되었거나 서버에 닿을 수 없다.
     */
    CONNECT,

    /**
     * The connection or the response timed out.
     * <p>
     * 연결 또는 응답이 시간 초과되었다.
     */
    TIMEOUT,

    /**
     * The TLS connection failed.
     * <p>
     * TLS 연결에 실패했다.
     */
    TLS,

    /**
     * Too many redirects.
     * <p>
     * 리디렉션이 너무 많다.
     */
    TOO_MANY_REDIRECTS,

    /**
     * The redirects form a loop.
     * <p>
     * 리디렉션이 순환한다.
     */
    REDIRECT_LOOP,

    /**
     * The response is larger than the limit for pages.
     * <p>
     * 응답이 페이지 크기 제한보다 크다.
     */
    TOO_LARGE,

    /**
     * The connection ended before the whole response arrived.
     * <p>
     * 응답 전체가 도착하기 전에 연결이 끝났다.
     */
    TRUNCATED,

    /**
     * The resource does not exist (shown with status 404).
     * <p>
     * 리소스가 없다(상태 404 로 표시).
     */
    NOT_FOUND,

    /**
     * The FTP access failed; the project handles this as 404.
     * <p>
     * FTP 접근에 실패했다. 이 프로젝트는 이를 404 로 처리한다.
     */
    FTP_FAILURE,

    /**
     * The <code>classpath://</code> resource does not exist (shown with status 404).
     * <p>
     * <code>classpath://</code> 리소스가 없다(상태 404 로 표시).
     */
    CLASSPATH_MISSING,

    /**
     * The address uses a scheme that is not supported (everything except http, https, ftp, classpath).
     * <p>
     * 주소가 지원하지 않는 스킴을 쓴다(http, https, ftp, classpath 외 모두).
     */
    BLOCKED_SCHEME,

    /**
     * The address is not valid.
     * <p>
     * 주소가 올바르지 않다.
     */
    INVALID_URL,

    /**
     * A cross-origin request was blocked.
     * <p>
     * 교차 오리진 요청이 차단되었다.
     */
    CORS,

    /**
     * The page is not well-formed XML (an XHTML document with a syntax error).
     * <p>
     * 페이지가 well-formed XML 이 아니다(구문 오류가 있는 XHTML 문서).
     */
    XML_PARSE_ERROR,

    /**
     * Any other failure to load the page.
     * <p>
     * 페이지를 읽는 데 실패한 그 밖의 경우.
     */
    LOAD_FAILED
}
