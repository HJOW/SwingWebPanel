package org.duckdns.hjow.swingwebpanel.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of a fetch: status, headers, media type, charset, decoded body, final URL, origin and, for failures, the
 * {@link FailureKind} with a message. A fetch never throws for I/O problems; it returns a response whose
 * {@link #getFailureKind()} is not {@link FailureKind#NONE}. HTTP error statuses (404, 500, ...) are normal responses with
 * their body kept ({@link #isHttpError()}). The body is already decompressed (gzip/deflate). When the body was streamed
 * to a {@link ResponseHandler} stream, {@link #getBody()} is empty and {@link #isStreamed()} is true.
 * <p>
 * 가져오기 결과: 상태, 헤더, 미디어 타입, 문자셋, 해독된 본문, 최종 URL, 오리진, 실패 시 {@link FailureKind} 와 메시지.
 * 가져오기는 입출력 문제로 예외를 던지지 않고 {@link #getFailureKind()} 가 {@link FailureKind#NONE} 이 아닌 응답을 돌려준다.
 * HTTP 오류 상태(404, 500 등)는 본문을 유지한 정상 응답이다({@link #isHttpError()}). 본문은 이미 압축이 풀려 있다(gzip/deflate).
 * 본문을 {@link ResponseHandler} 의 스트림으로 흘렸으면 {@link #getBody()} 는 비어 있고 {@link #isStreamed()} 가 true 이다.
 */
public final class ResourceResponse {
    /** Blob provenance of a resolved object URL response. / 조회한 객체 URL 응답의 blob 출처. */
    private BlobUrlStore.Entry blobUrlEntry;

    /** Returns the object URL source, or null for other responses. / 객체 URL 출처를 반환한다. 다른 응답이면 null이다. */
    public BlobUrlStore.Entry getBlobUrlEntry() {
        return blobUrlEntry;
    }

    /** Records the object URL that produced this response. / 이 응답을 만든 객체 URL을 기록한다. */
    void setBlobUrlEntry(BlobUrlStore.Entry entry) {
        blobUrlEntry = entry;
    }

    /**
     * An empty body.
     * <p>
     * 빈 본문.
     */
    private static final byte[] EMPTY = new byte[0];

    /**
     * URL originally requested.
     * <p>
     * 처음 요청한 URL.
     */
    private final String requestUrl;
    /**
     * Final URL after redirects and fallbacks.
     * <p>
     * 리디렉션과 대체 접속 이후의 최종 URL.
     */
    private String finalUrl;
    /**
     * HTTP status code (0 when there was no response; 200/404 are used for classpath and FTP).
     * <p>
     * HTTP 상태 코드(응답이 없으면 0. 클래스패스와 FTP 는 200/404 를 쓴다).
     */
    private int status;
    /**
     * Reason phrase or message.
     * <p>
     * 이유 문구 또는 메시지.
     */
    private String reason = "";
    /**
     * Headers by lower-case name.
     * <p>
     * 소문자 이름별 헤더.
     */
    private final Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
    /**
     * Decoded body bytes.
     * <p>
     * 해독된 본문 바이트.
     */
    private byte[] body = EMPTY;
    /**
     * Lower-case media type or "".
     * <p>
     * 소문자 미디어 타입 또는 "".
     */
    private String mimeType = "";
    /**
     * Charset from the Content-Type header or null.
     * <p>
     * Content-Type 헤더의 문자셋 또는 null.
     */
    private String charset;
    /**
     * Failure kind.
     * <p>
     * 실패 종류.
     */
    private FailureKind failureKind = FailureKind.NONE;
    /**
     * Failure message.
     * <p>
     * 실패 메시지.
     */
    private String failureMessage = "";
    /**
     * Whether the body went to a handler stream.
     * <p>
     * 본문이 처리기의 스트림으로 갔는지 여부.
     */
    private boolean streamed;
    /**
     * Number of body bytes delivered (memory or stream).
     * <p>
     * 전달된 본문 바이트 수(메모리 또는 스트림).
     */
    private long bodyLength;
    /**
     * Declared Content-Length or -1.
     * <p>
     * 선언된 Content-Length 또는 -1.
     */
    private long declaredLength = -1;
    /**
     * Whether the http retry after a failed https attempt was used.
     * <p>
     * https 실패 뒤 http 재시도를 썼는지 여부.
     */
    private boolean httpFallbackUsed;
    /**
     * URLs visited by redirects in order (not including the final URL).
     * <p>
     * 리디렉션으로 거친 URL 들(최종 URL 제외, 순서대로).
     */
    private final List<String> redirects = new ArrayList<String>();
    /**
     * Whether the content is a generated directory listing.
     * <p>
     * 내용이 생성된 디렉터리 목록인지 여부.
     */
    private boolean directoryListing;
    /**
     * Whether the response was served from the cache without contacting the server.
     * <p>
     * 서버에 접속하지 않고 캐시에서 응답했는지 여부.
     */
    private boolean fromCache;
    /**
     * Whether a stored response was confirmed by the server with 304 Not Modified (the body comes from the cache).
     * <p>
     * 저장된 응답을 서버가 304 Not Modified 로 확인해 주었는지 여부(본문은 캐시에서 온다).
     */
    private boolean revalidated;

    /**
     * Creates a response for a request URL.
     * <p>
     * 요청 URL 에 대한 응답을 만든다.
     *
     * @param requestUrl requested URL / 요청한 URL
     */
    ResourceResponse(String requestUrl) {
        this.requestUrl = requestUrl;
        this.finalUrl = requestUrl;
    }

    /**
     * Creates a failure response.
     * <p>
     * 실패 응답을 만든다.
     *
     * @param url     requested URL / 요청한 URL
     * @param kind    failure kind / 실패 종류
     * @param message failure message / 실패 메시지
     * @return the response / 응답
     */
    static ResourceResponse failure(String url, FailureKind kind, String message) {
        ResourceResponse r = new ResourceResponse(url);
        r.failureKind = kind;
        r.failureMessage = message == null ? "" : message;
        return r;
    }

    /**
     * Tells whether the response came from the cache without any request to the server.
     * <p>
     * 서버에 요청하지 않고 캐시에서 온 응답인지 알려 준다.
     *
     * @return true for a cache hit without network access / 네트워크 접근 없는 캐시 적중이면 true
     */
    public boolean isFromCache() {
        return fromCache;
    }

    /**
     * Tells whether the server confirmed a stored response with 304 Not Modified (a conditional request was sent and the stored body is
     * returned with the updated headers).
     * <p>
     * 서버가 저장된 응답을 304 Not Modified 로 확인했는지 알려 준다(조건부 요청을 보냈고 저장된 본문이 갱신된 헤더와 함께 반환된다).
     *
     * @return true after a successful revalidation / 재검증에 성공했으면 true
     */
    public boolean isRevalidated() {
        return revalidated;
    }

    /**
     * Marks the response as served from the cache.
     * <p>
     * 응답을 캐시에서 제공한 것으로 표시한다.
     *
     * @param revalidatedWithServer true if a conditional request confirmed the stored copy / 조건부 요청으로 저장본을 확인했으면 true
     */
    void markFromCache(boolean revalidatedWithServer) {
        this.fromCache = !revalidatedWithServer;
        this.revalidated = revalidatedWithServer;
    }

    /**
     * Returns the requested URL.
     * <p>
     * 요청한 URL 을 돌려준다.
     *
     * @return URL / URL
     */
    public String getRequestUrl() {
        return requestUrl;
    }

    /**
     * Returns the final URL.
     * <p>
     * 최종 URL 을 돌려준다.
     *
     * @return URL / URL
     */
    public String getFinalUrl() {
        return finalUrl;
    }

    /**
     * Sets the final URL.
     * <p>
     * 최종 URL 을 지정한다.
     *
     * @param u URL / URL
     */
    void setFinalUrl(String u) {
        this.finalUrl = u;
    }

    /**
     * Returns the status code (0 if none).
     * <p>
     * 상태 코드를 돌려준다(없으면 0).
     *
     * @return status / 상태
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the reason phrase.
     * <p>
     * 이유 문구를 돌려준다.
     *
     * @return reason / 이유
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets status and reason.
     * <p>
     * 상태와 이유를 지정한다.
     *
     * @param s status / 상태
     * @param r reason / 이유
     */
    void setStatus(int s, String r) {
        this.status = s;
        this.reason = r == null ? "" : r;
    }

    /**
     * Adds a header value under a case-insensitive name.
     * <p>
     * 대소문자 구분 없는 이름으로 헤더 값을 추가한다.
     *
     * @param name  header name / 헤더 이름
     * @param value header value / 헤더 값
     */
    void addHeader(String name, String value) {
        String key = name.toLowerCase();
        List<String> list = headers.get(key);
        if (list == null) {
            list = new ArrayList<String>();
            headers.put(key, list);
        }
        list.add(value);
    }

    /**
     * Returns the first value of a header, or null.
     * <p>
     * 헤더의 첫 값을 돌려준다. 없으면 null.
     *
     * @param name header name (case-insensitive) / 헤더 이름(대소문자 무관)
     * @return value or null / 값 또는 null
     */
    public String getHeader(String name) {
        List<String> list = headers.get(name.toLowerCase());
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /**
     * Returns all values of a header (empty if none).
     * <p>
     * 헤더의 모든 값을 돌려준다(없으면 빈 목록).
     *
     * @param name header name (case-insensitive) / 헤더 이름(대소문자 무관)
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<String> getHeaders(String name) {
        List<String> list = headers.get(name.toLowerCase());
        return list == null ? Collections.<String>emptyList() : Collections.unmodifiableList(list);
    }

    /**
     * Returns the lower-case names of all headers.
     * <p>
     * 모든 헤더의 소문자 이름을 돌려준다.
     *
     * @return names in arrival order / 도착 순서의 이름들
     */
    public List<String> getHeaderNames() {
        return new ArrayList<String>(headers.keySet());
    }

    /**
     * Returns the decoded body bytes (empty when streamed or on failure).
     * <p>
     * 해독된 본문 바이트를 돌려준다(스트리밍했거나 실패하면 비어 있음).
     *
     * @return body / 본문
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Sets the body.
     * <p>
     * 본문을 지정한다.
     *
     * @param b body bytes / 본문 바이트
     */
    void setBody(byte[] b) {
        this.body = b == null ? EMPTY : b;
        this.bodyLength = this.body.length;
    }

    /**
     * Returns the lower-case media type without parameters ("" if unknown).
     * <p>
     * 매개변수를 뺀 소문자 미디어 타입을 돌려준다(모르면 "").
     *
     * @return media type / 미디어 타입
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns the charset parameter of Content-Type or null.
     * <p>
     * Content-Type 의 charset 매개변수를 돌려준다. 없으면 null.
     *
     * @return charset / 문자셋
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Sets media type and charset.
     * <p>
     * 미디어 타입과 문자셋을 지정한다.
     *
     * @param mime media type / 미디어 타입
     * @param cs   charset or null / 문자셋 또는 null
     */
    void setContentType(String mime, String cs) {
        this.mimeType = mime == null ? "" : mime;
        this.charset = cs;
    }

    /**
     * Returns the failure kind.
     * <p>
     * 실패 종류를 돌려준다.
     *
     * @return kind / 종류
     */
    public FailureKind getFailureKind() {
        return failureKind;
    }

    /**
     * Returns the failure message.
     * <p>
     * 실패 메시지를 돌려준다.
     *
     * @return message / 메시지
     */
    public String getFailureMessage() {
        return failureMessage;
    }

    /**
     * Marks the response as failed.
     * <p>
     * 응답을 실패로 표시한다.
     *
     * @param kind    failure kind / 실패 종류
     * @param message message / 메시지
     */
    void setFailure(FailureKind kind, String message) {
        this.failureKind = kind;
        this.failureMessage = message == null ? "" : message;
    }

    /**
     * Tells whether the response is an HTTP-style error status (400 or above) delivered with a body.
     * <p>
     * 응답이 본문과 함께 전달된 HTTP 방식의 오류 상태(400 이상)인지 알려 준다.
     *
     * @return true for status 400+ / 상태가 400 이상이면 true
     */
    public boolean isHttpError() {
        return status >= 400;
    }

    /**
     * Tells whether the fetch failed or ended with an error status.
     * <p>
     * 가져오기가 실패했거나 오류 상태로 끝났는지 알려 준다.
     *
     * @return true on any failure or status 400+ / 실패이거나 상태 400 이상이면 true
     */
    public boolean isError() {
        return failureKind != FailureKind.NONE || status >= 400;
    }

    /**
     * Tells whether the body was streamed to a handler stream.
     * <p>
     * 본문을 처리기 스트림으로 흘렸는지 알려 준다.
     *
     * @return true if streamed / 스트리밍했으면 true
     */
    public boolean isStreamed() {
        return streamed;
    }

    /**
     * Returns the number of body bytes delivered (memory or stream).
     * <p>
     * 전달된 본문 바이트 수를 돌려준다(메모리 또는 스트림).
     *
     * @return bytes / 바이트
     */
    public long getBodyLength() {
        return bodyLength;
    }

    /**
     * Sets streaming information.
     * <p>
     * 스트리밍 정보를 지정한다.
     *
     * @param streamedBytes bytes sent to the stream / 스트림으로 보낸 바이트
     */
    void setStreamed(long streamedBytes) {
        this.streamed = true;
        this.bodyLength = streamedBytes;
    }

    /**
     * Returns the declared Content-Length or -1.
     * <p>
     * 선언된 Content-Length 를 돌려준다. 없으면 -1.
     *
     * @return length or -1 / 길이 또는 -1
     */
    public long getDeclaredLength() {
        return declaredLength;
    }

    /**
     * Sets the declared length.
     * <p>
     * 선언된 길이를 지정한다.
     *
     * @param n length or -1 / 길이 또는 -1
     */
    void setDeclaredLength(long n) {
        this.declaredLength = n;
    }

    /**
     * Tells whether the http retry after a failed https attempt produced this response.
     * <p>
     * https 실패 뒤의 http 재시도가 이 응답을 만들었는지 알려 준다.
     *
     * @return true if the fallback was used / 대체 접속을 썼으면 true
     */
    public boolean isHttpFallbackUsed() {
        return httpFallbackUsed;
    }

    /**
     * Marks that the http fallback was used.
     * <p>
     * http 대체 접속을 썼다고 표시한다.
     */
    void setHttpFallbackUsed() {
        this.httpFallbackUsed = true;
    }

    /**
     * Returns the URLs that redirected to the final URL.
     * <p>
     * 최종 URL 로 이끈 리디렉션 URL 들을 돌려준다.
     *
     * @return redirect chain without the final URL / 최종 URL 을 뺀 리디렉션 연쇄
     */
    public List<String> getRedirects() {
        return Collections.unmodifiableList(redirects);
    }

    /**
     * Records a redirect hop.
     * <p>
     * 리디렉션 한 번을 기록한다.
     *
     * @param fromUrl URL that redirected / 리디렉션한 URL
     */
    void addRedirect(String fromUrl) {
        redirects.add(fromUrl);
    }

    /**
     * Tells whether the body is a generated directory listing page (FTP).
     * <p>
     * 본문이 생성된 디렉터리 목록 페이지(FTP)인지 알려 준다.
     *
     * @return true for a listing / 목록이면 true
     */
    public boolean isDirectoryListing() {
        return directoryListing;
    }

    /**
     * Marks the response as a generated directory listing.
     * <p>
     * 응답을 생성된 디렉터리 목록으로 표시한다.
     */
    void setDirectoryListing() {
        this.directoryListing = true;
    }

    /**
     * Returns the origin of the final URL.
     * <p>
     * 최종 URL 의 오리진을 돌려준다.
     *
     * @return origin / 오리진
     */
    public Origin getOrigin() {
        return Origin.of(finalUrl);
    }

    /**
     * Tells whether the response asks to be saved instead of shown (<code>Content-Disposition: attachment</code>).
     * <p>
     * 응답이 표시하지 않고 저장하도록 요구하는지 알려 준다(<code>Content-Disposition: attachment</code>).
     *
     * @return true for attachment / attachment 이면 true
     */
    public boolean isAttachment() {
        String cd = getHeader("Content-Disposition");
        return cd != null && cd.trim().toLowerCase().startsWith("attachment");
    }

    /**
     * Returns a short description for logs.
     * <p>
     * 로그용 짧은 설명을 돌려준다.
     *
     * @return description / 설명
     */
    public String toString() {
        return "ResourceResponse[" + finalUrl + " status=" + status + " mime=" + mimeType
                + (failureKind != FailureKind.NONE ? " failure=" + failureKind + ":" + failureMessage : "")
                + " bytes=" + bodyLength + "]";
    }
}
