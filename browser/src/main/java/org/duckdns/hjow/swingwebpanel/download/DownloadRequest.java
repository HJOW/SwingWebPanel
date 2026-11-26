package org.duckdns.hjow.swingwebpanel.download;

import org.duckdns.hjow.swingwebpanel.core.DownloadRequiredException;
import org.duckdns.hjow.swingwebpanel.core.NavigationRequest;
import org.duckdns.hjow.swingwebpanel.net.BlobUrlStore;
import org.duckdns.hjow.swingwebpanel.net.RequestHeaders;

/**
 * Everything a download needs to know: where to fetch it, how to ask for it (the user agent and extra headers of the component that found
 * it) and what the server said about it (type, declared size, Content-Disposition), plus the safe file name derived from that. It is
 * immutable and can be handed to a popup implementation and to the transfer thread.
 * <p>
 * 다운로드에 필요한 모든 정보: 어디서 가져올지, 어떻게 요청할지(발견한 컴포넌트의 User-Agent 와 추가 헤더), 서버가 알려 준 것(형식, 선언된 크기,
 * Content-Disposition), 그리고 그로부터 정한 안전한 파일 이름. 불변이며 팝업 구현과 전송 스레드에 넘길 수 있다.
 */
public final class DownloadRequest {

    /** Final address to fetch. 가져올 최종 주소. */
    private final String url;

    /** Address the user asked for. 사용자가 요청한 주소. */
    private final String requestedUrl;

    /** MIME type, lower case, may be empty. MIME 타입. 소문자이며 비어 있을 수 있다. */
    private final String mimeType;

    /** Raw Content-Disposition value or null. Content-Disposition 의 원래 값 또는 null. */
    private final String contentDisposition;

    /** Declared size in bytes or -1. 선언된 크기(바이트) 또는 -1. */
    private final long length;

    /** User agent for the transfer. 전송에 쓸 User-Agent. */
    private final String userAgent;

    /** Extra headers (a private copy) or null. 추가 헤더(개인 복사본) 또는 null. */
    private final RequestHeaders headers;

    /** Body of a form that is sent again to save the response, or null for a plain GET. 응답을 저장하려고 다시 보내는 폼의 본문. 일반 GET 이면 null. */
    private byte[] postBody;

    /** Media type of {@link #postBody}. {@link #postBody} 의 미디어 타입. */
    private String postContentType;

    /** Address of the page that contained the form (the referrer) or null. 폼이 있던 페이지의 주소(참조 주소). 없으면 null. */
    private String postReferrer;

    /** Object bytes captured before display of the popup. / 팝업을 표시하기 전에 확보한 객체 바이트. */
    private BlobUrlStore.Entry blobUrlEntry;


    /** Safe file name. 안전한 파일 이름. */
    private final String fileName;

    /**
     * Creates a request.
     * <p>
     * 요청을 만든다.
     *
     * @param url                final address / 최종 주소
     * @param requestedUrl       requested address / 요청한 주소
     * @param mimeType           MIME type or null / MIME 타입 또는 null
     * @param contentDisposition Content-Disposition value or null / Content-Disposition 값 또는 null
     * @param length             declared size or -1 / 선언된 크기 또는 -1
     * @param userAgent          user agent or null for the default / User-Agent. 기본값이면 null
     * @param headers            extra headers or null / 추가 헤더 또는 null
     */
    public DownloadRequest(String url, String requestedUrl, String mimeType, String contentDisposition, long length, String userAgent,
            RequestHeaders headers) {
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("url must not be empty");
        }
        this.url = url;
        this.requestedUrl = requestedUrl == null ? url : requestedUrl;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.contentDisposition = contentDisposition;
        this.length = length < 0 ? -1 : length;
        this.userAgent = userAgent;
        this.headers = headers == null ? null : headers.copy();
        this.fileName = DownloadFileNames.choose(contentDisposition, url);
    }

    /**
     * Creates a request from the navigation that discovered the download.
     * <p>
     * 다운로드를 발견한 이동으로 요청을 만든다.
     *
     * @param navigation the navigation request (user agent, headers) / 이동 요청(User-Agent, 헤더)
     * @param download   the description of the response / 응답의 설명
     * @return the download request / 다운로드 요청
     */
    public static DownloadRequest of(NavigationRequest navigation, DownloadRequiredException download) {
        String finalUrl = download.getFinalUrl() == null || download.getFinalUrl().length() == 0 ? download.getUrl() : download.getFinalUrl();
        DownloadRequest r = new DownloadRequest(finalUrl, download.getUrl(), download.getMimeType(), download.getContentDisposition(), download.getLength(),
                navigation == null ? null : navigation.getUserAgent(), navigation == null ? null : navigation.getHeaders());
        r.blobUrlEntry = navigation == null ? null : navigation.getBlobUrlEntry();
        // 폼 제출(POST)의 응답이면 저장할 때 같은 데이터로 다시 요청해야 한다.
        if (navigation != null && navigation.getPost() != null && navigation.getPost().isResendable()) {
            r.postBody = navigation.getPost().getBody();
            r.postContentType = navigation.getPost().getContentType();
            r.postReferrer = navigation.getPost().getReferrer();
        }
        return r;
    }

    /** Returns the captured object for a local download, if any. / 로컬 다운로드를 위해 확보한 객체를 반환한다. 없으면 null. */
    BlobUrlStore.Entry getBlobUrlEntry() {
        return blobUrlEntry;
    }

    /**
     * Returns the body of the form that is sent again to save the response.
     * <p>
     * 응답을 저장하려고 다시 보내는 폼의 본문을 반환한다.
     *
     * @return a copy of the body, or null for a plain GET / 본문의 복사본. 일반 GET 이면 null
     */
    public byte[] getPostBody() {
        return postBody == null ? null : postBody.clone();
    }

    /**
     * Returns the media type of the form body.
     * <p>
     * 폼 본문의 미디어 타입을 반환한다.
     *
     * @return the media type, or null for a plain GET / 미디어 타입. 일반 GET 이면 null
     */
    public String getPostContentType() {
        return postContentType;
    }

    /**
     * Returns the address of the page that contained the form.
     * <p>
     * 폼이 있던 페이지의 주소를 반환한다.
     *
     * @return the address, or null / 주소. 없으면 null
     */
    public String getPostReferrer() {
        return postReferrer;
    }


    /**
     * Returns the final address to fetch.
     * <p>
     * 가져올 최종 주소를 반환한다.
     *
     * @return address / 주소
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the address the user asked for.
     * <p>
     * 사용자가 요청한 주소를 반환한다.
     *
     * @return address / 주소
     */
    public String getRequestedUrl() {
        return requestedUrl;
    }

    /**
     * Returns the MIME type (empty if unknown).
     * <p>
     * MIME 타입을 반환한다(모르면 빈 문자열).
     *
     * @return MIME type / MIME 타입
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns the Content-Disposition header value or null.
     * <p>
     * Content-Disposition 헤더 값을 반환한다. 없으면 null.
     *
     * @return value or null / 값 또는 null
     */
    public String getContentDisposition() {
        return contentDisposition;
    }

    /**
     * Returns the declared size in bytes or -1.
     * <p>
     * 선언된 크기(바이트)를 반환한다. 모르면 -1.
     *
     * @return size or -1 / 크기 또는 -1
     */
    public long getLength() {
        return length;
    }

    /**
     * Returns the user agent for the transfer or null.
     * <p>
     * 전송에 쓸 User-Agent 를 반환한다. 기본값이면 null.
     *
     * @return user agent or null / User-Agent 또는 null
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Returns a copy of the extra headers, or null.
     * <p>
     * 추가 헤더의 복사본을 반환한다. 없으면 null.
     *
     * @return headers or null / 헤더 또는 null
     */
    public RequestHeaders getHeaders() {
        return headers == null ? null : headers.copy();
    }

    /**
     * Returns the safe file name chosen for this download.
     * <p>
     * 이 다운로드를 위해 정한 안전한 파일 이름을 반환한다.
     *
     * @return file name / 파일 이름
     */
    public String getFileName() {
        return fileName;
    }
}
