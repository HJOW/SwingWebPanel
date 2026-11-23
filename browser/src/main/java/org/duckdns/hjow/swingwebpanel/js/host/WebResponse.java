package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of a {@link WebRequest}, already filtered by the component's network policy the way the Fetch Standard filters responses:
 * a <code>basic</code> response (same origin, without <code>Set-Cookie</code>), a <code>cors</code> response (cross origin that the server
 * allowed, with only the safelisted and the exposed headers), an <code>opaque</code> response (cross origin <code>no-cors</code>: status 0,
 * no headers, no body) or an <code>error</code> response (network error: refused by CORS or by the scheme policy, blocked redirect,
 * failed connection, cancelled request). A response with an HTTP error status such as 404 is a normal response, not an error.
 * <p>
 * 컴포넌트의 네트워크 정책이 Fetch 표준이 응답을 거르는 방식으로 이미 거른 {@link WebRequest} 의 결과: <code>basic</code> 응답(같은 오리진, <code>Set-Cookie</code>
 * 제외), <code>cors</code> 응답(서버가 허용한 교차 오리진, 안전 목록과 노출한 헤더만), <code>opaque</code> 응답(교차 오리진 <code>no-cors</code>: 상태 0,
 * 헤더와 본문 없음), <code>error</code> 응답(네트워크 오류: CORS 나 스킴 정책이 거부, 막힌 리디렉션, 연결 실패, 취소된 요청). 404 같은 HTTP 오류 상태의
 * 응답은 오류가 아닌 일반 응답이다.
 */
public final class WebResponse {

    /** Type of a same-origin response. / 같은 오리진 응답의 종류. */
    public static final String TYPE_BASIC = "basic";

    /** Type of an allowed cross-origin response. / 허용된 교차 오리진 응답의 종류. */
    public static final String TYPE_CORS = "cors";

    /** Type of an opaque cross-origin response. / 불투명한 교차 오리진 응답의 종류. */
    public static final String TYPE_OPAQUE = "opaque";

    /** Type of a network error. / 네트워크 오류의 종류. */
    public static final String TYPE_ERROR = "error";

    /** The response type. / 응답 종류. */
    private final String type;

    /** The status code, 0 for opaque responses and errors. / 상태 코드. 불투명 응답과 오류는 0. */
    private int status;

    /** The reason phrase. / 이유 문구. */
    private String statusText = "";

    /** The visible headers as (lower-case name, value) pairs. / 보이는 헤더의 (소문자 이름, 값) 쌍. */
    private final List<String[]> headers = new ArrayList<String[]>();

    /** The body bytes (never null, empty when absent or hidden). / 본문 바이트(null 아님, 없거나 숨기면 비어 있음). */
    private byte[] body = new byte[0];

    /** The final URL after redirects, or "". / 리디렉션 뒤의 최종 URL 또는 "". */
    private String url = "";

    /** True if at least one redirect was followed. / 리디렉션을 한 번 이상 따랐으면 true. */
    private boolean redirected;

    /** The declared body length or -1. / 선언된 본문 길이 또는 -1. */
    private long declaredLength = -1;

    /** The reason for an error response, or null. / 오류 응답의 이유. 없으면 null. */
    private String errorMessage;

    /** True if an error response is the result of a cancellation. / 오류 응답이 취소의 결과이면 true. */
    private boolean cancelled;

    /** True if an error response is one that a retry cannot fix (a blocked scheme or a refused cross-origin access). / 오류 응답이 다시 시도해도 고쳐지지 않는 것(막힌 스킴이나 거부된 교차 오리진 접근)이면 true. */
    private boolean futile;

    /**
     * Creates a response.
     * <p>
     * 응답을 만든다.
     *
     * @param type one of the TYPE constants / TYPE 상수 중 하나
     */
    public WebResponse(String type) {
        this.type = type;
    }

    /**
     * Creates a network error response.
     * <p>
     * 네트워크 오류 응답을 만든다.
     *
     * @param message the reason for diagnostics / 진단용 이유
     * @param cancelled true if the request was cancelled / 요청이 취소되었으면 true
     * @return the error response / 오류 응답
     */
    public static WebResponse error(String message, boolean cancelled) {
        WebResponse r = new WebResponse(TYPE_ERROR);
        r.errorMessage = message == null ? "network error" : message;
        r.cancelled = cancelled;
        return r;
    }

    /**
     * Creates a network error response that a retry cannot fix: a scheme that is not allowed, a cross-origin access that the server does not
     * grant, a redirect that the request forbids. A consumer that retries after network errors (an event stream) gives up on such an error.
     * <p>
     * 다시 시도해도 고쳐지지 않는 네트워크 오류 응답을 만든다: 허용되지 않은 스킴, 서버가 허락하지 않는 교차 오리진 접근, 요청이 금지한 리디렉션.
     * 네트워크 오류 뒤에 다시 시도하는 쪽(이벤트 스트림)은 이런 오류에서는 포기한다.
     *
     * @param message the reason for diagnostics / 진단용 이유
     * @return the error response / 오류 응답
     */
    public static WebResponse futileError(String message) {
        WebResponse r = error(message, false);
        r.futile = true;
        return r;
    }

    /**
     * Returns the type.
     * <p>
     * 종류를 반환한다.
     *
     * @return one of the TYPE constants / TYPE 상수 중 하나
     */
    public String getType() {
        return type;
    }

    /**
     * Tells whether this is a network error.
     * <p>
     * 네트워크 오류인지 알려 준다.
     *
     * @return true for an error response / 오류 응답이면 true
     */
    public boolean isError() {
        return TYPE_ERROR.equals(type);
    }

    /**
     * Returns the status code.
     * <p>
     * 상태 코드를 반환한다.
     *
     * @return the status / 상태
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the status line.
     * <p>
     * 상태 줄을 지정한다.
     *
     * @param code the status code / 상태 코드
     * @param text the reason phrase or null / 이유 문구 또는 null
     * @return this response / 이 응답
     */
    public WebResponse setStatus(int code, String text) {
        this.status = code;
        this.statusText = text == null ? "" : text;
        return this;
    }

    /**
     * Returns the reason phrase.
     * <p>
     * 이유 문구를 반환한다.
     *
     * @return the text or "" / 문구 또는 ""
     */
    public String getStatusText() {
        return statusText;
    }

    /**
     * Returns the live list of visible headers.
     * <p>
     * 보이는 헤더의 목록을 반환한다.
     *
     * @return (name, value) pairs in arrival order / 도착 순서의 (이름, 값) 쌍
     */
    public List<String[]> getHeaders() {
        return headers;
    }

    /**
     * Adds a visible header.
     * <p>
     * 보이는 헤더를 추가한다.
     *
     * @param name  the name / 이름
     * @param value the value / 값
     * @return this response / 이 응답
     */
    public WebResponse addHeader(String name, String value) {
        headers.add(new String[] { name.toLowerCase(), value });
        return this;
    }

    /**
     * Returns the first value of a header, case-insensitively.
     * <p>
     * 헤더의 첫 값을 대소문자 구분 없이 반환한다.
     *
     * @param name the header name / 헤더 이름
     * @return the value or null / 값 또는 null
     */
    public String getHeader(String name) {
        String lower = name.toLowerCase();
        // 도착 순서대로 이름이 같은 첫 헤더를 찾는다.
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i)[0].equals(lower)) {
                return headers.get(i)[1];
            }
        }
        return null;
    }

    /**
     * Returns the body.
     * <p>
     * 본문을 반환한다.
     *
     * @return the bytes, never null / 바이트(null 아님)
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Sets the body.
     * <p>
     * 본문을 지정한다.
     *
     * @param bytes the bytes or null / 바이트 또는 null
     * @return this response / 이 응답
     */
    public WebResponse setBody(byte[] bytes) {
        this.body = bytes == null ? new byte[0] : bytes;
        return this;
    }

    /**
     * Returns the final URL.
     * <p>
     * 최종 URL을 반환한다.
     *
     * @return the URL or "" / URL 또는 ""
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the final URL.
     * <p>
     * 최종 URL을 지정한다.
     *
     * @param u the URL / URL
     * @return this response / 이 응답
     */
    public WebResponse setUrl(String u) {
        this.url = u == null ? "" : u;
        return this;
    }

    /**
     * Tells whether a redirect was followed.
     * <p>
     * 리디렉션을 따랐는지 알려 준다.
     *
     * @return true if redirected / 리디렉션되었으면 true
     */
    public boolean isRedirected() {
        return redirected;
    }

    /**
     * Records whether a redirect was followed.
     * <p>
     * 리디렉션을 따랐는지 기록한다.
     *
     * @param r true if redirected / 리디렉션되었으면 true
     * @return this response / 이 응답
     */
    public WebResponse setRedirected(boolean r) {
        this.redirected = r;
        return this;
    }

    /**
     * Returns the declared body length.
     * <p>
     * 선언된 본문 길이를 반환한다.
     *
     * @return the length or -1 / 길이 또는 -1
     */
    public long getDeclaredLength() {
        return declaredLength;
    }

    /**
     * Sets the declared body length.
     * <p>
     * 선언된 본문 길이를 지정한다.
     *
     * @param n the length or -1 / 길이 또는 -1
     * @return this response / 이 응답
     */
    public WebResponse setDeclaredLength(long n) {
        this.declaredLength = n;
        return this;
    }

    /**
     * Returns the error reason.
     * <p>
     * 오류 이유를 반환한다.
     *
     * @return the message, null unless this is an error / 메시지. 오류가 아니면 null
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Tells whether this error was caused by a cancellation.
     * <p>
     * 이 오류가 취소 때문인지 알려 준다.
     *
     * @return true after a cancellation / 취소된 뒤이면 true
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Tells whether this error is one that a retry cannot fix (see {@link #futileError(String)}).
     * <p>
     * 이 오류가 다시 시도해도 고쳐지지 않는 것인지 알려 준다({@link #futileError(String)} 참조).
     *
     * @return true for a futile error / 헛수고가 될 오류이면 true
     */
    public boolean isFutile() {
        return futile;
    }
}
