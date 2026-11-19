package org.duckdns.hjow.swingwebpanel.core;

/**
 * Thrown by a {@link PageLoader} when the response of a main document navigation must be downloaded as a file instead of being shown
 * (API contract section 7): the server marked it as an attachment, or its type is not a displayable document. The page, the URL, the
 * history and the progress of the component stay unchanged. The session passes the exception to its {@link DownloadHandler}.
 * <p>
 * 주 문서 이동의 응답을 보여 주는 대신 파일로 내려받아야 할 때 {@link PageLoader} 가 던진다(API 계약 7절): 서버가 첨부 파일로 표시했거나
 * 표시할 수 있는 문서 형식이 아닌 경우이다. 컴포넌트의 페이지, URL, 이력, 진행률은 바뀌지 않는다. 세션은 이 예외를 {@link DownloadHandler} 에 넘긴다.
 */
public final class DownloadRequiredException extends Exception {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Why the response is downloaded.
     * <p>
     * 응답을 내려받는 이유.
     */
    public enum Reason {
        /**
         * The response has a Content-Disposition of type attachment.
         * <p>
         * 응답의 Content-Disposition 이 attachment 이다.
         */
        ATTACHMENT,
        /**
         * The MIME type cannot be shown as a document.
         * <p>
         * MIME 타입을 문서로 보여 줄 수 없다.
         */
        NOT_DISPLAYABLE,
        /**
         * The link that led to the response has the <code>download</code> attribute (same origin).
         * <p>
         * 응답으로 이끈 링크에 <code>download</code> 속성이 있다(같은 오리진).
         */
        LINK_DOWNLOAD
    }

    /**
     * Address the user asked for.
     * <p>
     * 사용자가 요청한 주소.
     */
    private final String url;

    /**
     * Final address after redirects.
     * <p>
     * 리디렉션 이후 최종 주소.
     */
    private final String finalUrl;

    /**
     * MIME type of the response, lower case, possibly empty.
     * <p>
     * 응답의 소문자 MIME 타입. 비어 있을 수 있다.
     */
    private final String mimeType;

    /**
     * Raw value of the Content-Disposition header, or null.
     * <p>
     * Content-Disposition 헤더의 원래 값. 없으면 null.
     */
    private final String contentDisposition;

    /**
     * Declared size in bytes, or -1 if unknown.
     * <p>
     * 선언된 크기(바이트). 모르면 -1.
     */
    private final long length;

    /**
     * Why the response is downloaded.
     * <p>
     * 응답을 내려받는 이유.
     */
    private final Reason reason;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param url                requested address / 요청한 주소
     * @param finalUrl           final address / 최종 주소
     * @param mimeType           MIME type / MIME 타입
     * @param contentDisposition Content-Disposition value or null / Content-Disposition 값 또는 null
     * @param length             declared size or -1 / 선언된 크기 또는 -1
     * @param reason             reason for the download / 내려받는 이유
     */
    public DownloadRequiredException(String url, String finalUrl, String mimeType, String contentDisposition, long length, Reason reason) {
        super("download required: " + finalUrl + " (" + reason + ")");
        this.url = url;
        this.finalUrl = finalUrl;
        this.mimeType = mimeType;
        this.contentDisposition = contentDisposition;
        this.length = length;
        this.reason = reason;
    }

    /**
     * Returns the requested address.
     * <p>
     * 요청한 주소를 반환한다.
     *
     * @return address / 주소
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the final address.
     * <p>
     * 최종 주소를 반환한다.
     *
     * @return address / 주소
     */
    public String getFinalUrl() {
        return finalUrl;
    }

    /**
     * Returns the MIME type.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME type / MIME 타입
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns the raw Content-Disposition header value, or null.
     * <p>
     * Content-Disposition 헤더의 원래 값을 반환한다. 없으면 null.
     *
     * @return header value or null / 헤더 값 또는 null
     */
    public String getContentDisposition() {
        return contentDisposition;
    }

    /**
     * Returns the declared size in bytes, or -1.
     * <p>
     * 선언된 크기(바이트)를 반환한다. 모르면 -1.
     *
     * @return size or -1 / 크기 또는 -1
     */
    public long getLength() {
        return length;
    }

    /**
     * Returns why the response is downloaded.
     * <p>
     * 응답을 내려받는 이유를 반환한다.
     *
     * @return reason / 이유
     */
    public Reason getReason() {
        return reason;
    }
}
