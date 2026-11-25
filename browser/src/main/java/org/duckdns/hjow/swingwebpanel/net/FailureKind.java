package org.duckdns.hjow.swingwebpanel.net;

/**
 * Why a fetch did not produce a normal response. An HTTP error status (404, 500, ...) is <em>not</em> a failure kind:
 * such responses carry their status and body and have kind {@link #NONE}. The content layer maps failure kinds to error
 * pages (plan item P17-06).
 * <p>
 * 가져오기가 정상 응답을 만들지 못한 이유. HTTP 오류 상태(404, 500 등)는 실패 종류가 <em>아니며</em> 상태와 본문을 가진
 * {@link #NONE} 응답이다. 콘텐츠 계층이 실패 종류를 오류 페이지로 바꾼다(계획 P17-06).
 */
public enum FailureKind {
    /**
     * No transport failure.
     * <p>
     * 전송 실패 없음.
     */
    NONE,
    /**
     * The URL is not an absolute, well formed address.
     * <p>
     * 올바른 절대 주소가 아님.
     */
    INVALID_URL,
    /**
     * The scheme is not allowed; nothing was contacted.
     * <p>
     * 허용되지 않은 스킴. 어디에도 연결하지 않았다.
     */
    BLOCKED_SCHEME,
    /** A cross-origin resource or frame was blocked by response policy. / 응답 정책이 교차 오리진 리소스나 프레임을 차단했다. */
    BLOCKED_EMBEDDER_POLICY,
    /**
     * The host name could not be resolved.
     * <p>
     * 호스트 이름을 찾을 수 없음.
     */
    DNS,
    /**
     * The connection could not be established (refused, unreachable).
     * <p>
     * 연결을 만들 수 없음(거부, 도달 불가).
     */
    CONNECT,
    /**
     * Connect or read timeout.
     * <p>
     * 연결 또는 읽기 제한 시간 초과.
     */
    TIMEOUT,
    /**
     * TLS/SSL failure (handshake, certificate, host name).
     * <p>
     * TLS/SSL 실패(핸드셰이크, 인증서, 호스트 이름).
     */
    TLS,
    /**
     * More redirects than allowed.
     * <p>
     * 허용보다 많은 리디렉션.
     */
    TOO_MANY_REDIRECTS,
    /**
     * A redirect loop was detected.
     * <p>
     * 리디렉션 순환 감지.
     */
    REDIRECT_LOOP,
    /**
     * The (decoded) body exceeds the size limit.
     * <p>
     * (해독된) 본문이 크기 제한을 넘음.
     */
    TOO_LARGE,
    /**
     * The connection ended before the whole body arrived.
     * <p>
     * 본문이 모두 오기 전에 연결이 끝남.
     */
    TRUNCATED,
    /**
     * The request was cancelled through its token.
     * <p>
     * 토큰으로 요청이 취소됨.
     */
    CANCELLED,
    /**
     * The class path resource does not exist (or is a directory).
     * <p>
     * 클래스패스 리소스가 없음(또는 디렉터리임).
     */
    NOT_FOUND,
    /**
     * Any FTP failure (connect, login, directory, transfer); reported to users as 404.
     * <p>
     * FTP 의 모든 실패(연결, 로그인, 디렉터리, 전송). 사용자에게는 404 로 알린다.
     */
    FTP_FAILURE,
    /**
     * A cross-origin request that needs a CORS preflight was not granted by the server, so the actual request was not sent.
     * <p>
     * CORS 사전 요청이 필요한 교차 오리진 요청을 서버가 허락하지 않아 본 요청을 보내지 않음.
     */
    CORS,
    /**
     * Other I/O failure.
     * <p>
     * 그 밖의 입출력 실패.
     */
    IO
}
