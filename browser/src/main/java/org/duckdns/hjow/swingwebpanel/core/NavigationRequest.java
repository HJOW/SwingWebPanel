package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.net.BlobUrlStore;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.CancelToken;
import org.duckdns.hjow.swingwebpanel.net.RequestHeaders;

/**
 * Everything a {@link PageLoader} needs to load one page: what to load, the settings that were in effect when the
 * navigation started (user agent, extra headers, JavaScript flag, forced reload) and the means to report progress and to
 * notice cancellation. Instances are immutable except for the cancel token.
 * <p>
 * {@link PageLoader} 가 페이지 하나를 읽는 데 필요한 모든 것: 읽을 대상, 이동이 시작될 때의 설정(User-Agent, 추가 헤더, JavaScript 설정,
 * 강제 새로고침), 진행률 보고 수단과 취소 확인 수단. 취소 토큰을 제외하면 불변이다.
 */
public final class NavigationRequest {

    /**
     * Request number; grows with every navigation of a component.
     * <p>
     * 요청 번호. 컴포넌트의 이동마다 증가한다.
     */
    private final int id;

    /**
     * Why the navigation was started.
     * <p>
     * 이동이 시작된 이유.
     */
    private final NavigationType type;

    /**
     * Target URL (<code>about:text</code> for set-content navigations).
     * <p>
     * 대상 URL(콘텐츠 설정 이동은 <code>about:text</code>).
     */
    private final String url;

    /**
     * Source text for <code>about:text</code> navigations, otherwise null.
     * <p>
     * <code>about:text</code> 이동의 원문. 아니면 null.
     */
    private final String content;

    /**
     * Normalized MIME type for <code>about:text</code> navigations, otherwise null.
     * <p>
     * <code>about:text</code> 이동의 정규화한 MIME 타입. 아니면 null.
     */
    private final String mimeType;

    /**
     * True to bypass caches (<code>refresh(true)</code>).
     * <p>
     * 캐시를 우회하면 true(<code>refresh(true)</code>).
     */
    private final boolean force;

    /**
     * The stored entry for history moves and refreshes, otherwise null.
     * <p>
     * 이력 이동과 새로고침에서 보관된 항목. 아니면 null.
     */
    private final PageSnapshot restore;

    /**
     * History position to move to for {@link NavigationType#HISTORY}, otherwise -1.
     * <p>
     * {@link NavigationType#HISTORY} 에서 이동할 이력 위치. 아니면 -1.
     */
    private final int targetIndex;

    /**
     * User agent in effect at the start of the navigation.
     * <p>
     * 이동 시작 시점의 User-Agent.
     */
    private final String userAgent;

    /**
     * Copy of the extra headers in effect at the start of the navigation.
     * <p>
     * 이동 시작 시점의 추가 헤더 복사본.
     */
    private final RequestHeaders headers;

    /**
     * JavaScript flag in effect at the start of the navigation.
     * <p>
     * 이동 시작 시점의 JavaScript 설정.
     */
    private final boolean javascriptEnabled;

    /**
     * Token that is cancelled when a newer navigation starts or the component is cleaned or disposed.
     * <p>
     * 더 새로운 이동이 시작되거나 컴포넌트가 clean/dispose 될 때 취소되는 토큰.
     */
    private final CancelToken cancel;

    /**
     * Sink for status and progress.
     * <p>
     * 상태와 진행률을 받는 대상.
     */
    private final ProgressSink progress;

    /**
     * The form submission to send (<code>POST</code>), or null for a plain <code>GET</code> navigation.
     * <p>
     * 보낼 폼 제출(<code>POST</code>). 일반 <code>GET</code> 이동이면 null.
     */
    private final PostData post;

    /**
     * The suggested file name when the response is saved as a file whatever its type (the link had the <code>download</code> attribute; empty
     * if the attribute had no value), or null for an ordinary navigation.
     * <p>
     * 응답을 형식과 관계없이 파일로 저장할 때 제안하는 파일 이름(링크에 <code>download</code> 속성이 있었고 값이 없으면 빈 문자열). 일반 이동이면 null.
     */
    private final String downloadName;

    /**
     * The nested browsing context that this navigation loads into, or null for a navigation of the top-level document of the component.
     * <p>
     * 이 이동이 로드되는 중첩 브라우징 문맥. 컴포넌트의 최상위 문서의 이동이면 null.
     */
    private FrameContext frame;

    /** Object URL entry accepted before the previous document could be unloaded. / 이전 문서가 언로드되기 전에 받아들인 객체 URL 항목. */
    private BlobUrlStore.Entry blobUrlEntry;

    /**
     * Associates an entry before giving the request to a loader thread.
     * <p>
     * 요청을 로더 스레드에 넘기기 전에 항목을 연결한다.
     *
     * @param entry resolved entry or null / 조회한 항목 또는 null
     * @return this request / 이 요청
     */
    NavigationRequest withBlobUrlEntry(BlobUrlStore.Entry entry) {
        blobUrlEntry = entry;
        return this;
    }

    /** Returns the accepted entry, if any. / 받아들인 항목이 있으면 반환한다. */
    public BlobUrlStore.Entry getBlobUrlEntry() {
        return blobUrlEntry;
    }

    /** Origin initiating a frame navigation to a local file. / 로컬 파일로 프레임 이동을 시작한 오리진. */
    private Origin fileFrameInitiator;

    /**
     * Records the origin required to authorize file access by a frame.
     * <p>
     * 프레임의 파일 접근을 확인하는 데 필요한 오리진을 기록한다.
     *
     * @param origin the initiating document's origin / 시작 문서의 오리진
     * @return the request to submit / 제출할 요청
     */
    NavigationRequest withFileFrameInitiator(Origin origin) {
        fileFrameInitiator = origin;
        return this;
    }

    /** Returns the initiating origin for a frame file request. / 프레임 파일 요청의 시작 오리진을 반환한다. */
    Origin getFileFrameInitiator() {
        return fileFrameInitiator;
    }

    /**
     * Marks this request as the navigation of a frame (set once when the session builds the request of a frame).
     * <p>
     * 이 요청을 프레임의 이동으로 표시한다(세션이 프레임의 요청을 만들 때 한 번 정한다).
     *
     * @param value the frame / 프레임
     * @return this request / 이 요청
     */
    NavigationRequest withFrame(FrameContext value) {
        this.frame = value;
        return this;
    }

    /**
     * Returns the nested browsing context that this navigation loads into.
     * <p>
     * 이 이동이 로드되는 중첩 브라우징 문맥을 반환한다.
     *
     * @return the frame, or null for the top-level document / 프레임. 최상위 문서이면 null
     */
    FrameContext getFrame() {
        return frame;
    }

    /**
     * Creates a request.
     * <p>
     * 요청을 만든다.
     *
     * @param id                request number / 요청 번호
     * @param type              navigation type / 이동 종류
     * @param url               target URL / 대상 URL
     * @param content           source text or null / 원문 또는 null
     * @param mimeType          MIME type or null / MIME 타입 또는 null
     * @param force             bypass caches / 캐시 우회
     * @param restore           stored entry or null / 보관된 항목 또는 null
     * @param targetIndex       history position or -1 / 이력 위치 또는 -1
     * @param userAgent         user agent / User-Agent
     * @param headers           extra headers copy / 추가 헤더 복사본
     * @param javascriptEnabled JavaScript flag / JavaScript 설정
     * @param cancel            cancel token / 취소 토큰
     * @param progress          progress sink / 진행률 대상
     */
    NavigationRequest(int id, NavigationType type, String url, String content, String mimeType, boolean force,
            PageSnapshot restore, int targetIndex, String userAgent, RequestHeaders headers, boolean javascriptEnabled,
            CancelToken cancel, ProgressSink progress) {
        this(id, type, url, content, mimeType, force, restore, targetIndex, userAgent, headers, javascriptEnabled, cancel, progress, null, null);
    }

    /**
     * Creates a request that sends a form or saves the response as a file.
     * <p>
     * 폼을 보내거나 응답을 파일로 저장하는 요청을 만든다.
     *
     * @param id                request number / 요청 번호
     * @param type              navigation type / 이동 종류
     * @param url               target URL / 대상 URL
     * @param content           source text or null / 원문 또는 null
     * @param mimeType          MIME type or null / MIME 타입 또는 null
     * @param force             bypass caches / 캐시 우회
     * @param restore           stored entry or null / 보관된 항목 또는 null
     * @param targetIndex       history position or -1 / 이력 위치 또는 -1
     * @param userAgent         user agent / User-Agent
     * @param headers           extra headers copy / 추가 헤더 복사본
     * @param javascriptEnabled JavaScript flag / JavaScript 설정
     * @param cancel            cancel token / 취소 토큰
     * @param progress          progress sink / 진행률 대상
     * @param post              the form submission to send or null / 보낼 폼 제출 또는 null
     * @param downloadName      the suggested file name to save the response as a file (empty for none), or null / 응답을 파일로 저장할 때 제안하는 이름(없으면 빈 문자열). 일반 이동이면 null
     */
    NavigationRequest(int id, NavigationType type, String url, String content, String mimeType, boolean force,
            PageSnapshot restore, int targetIndex, String userAgent, RequestHeaders headers, boolean javascriptEnabled,
            CancelToken cancel, ProgressSink progress, PostData post, String downloadName) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.content = content;
        this.mimeType = mimeType;
        this.force = force;
        this.restore = restore;
        this.targetIndex = targetIndex;
        this.userAgent = userAgent;
        this.headers = headers;
        this.javascriptEnabled = javascriptEnabled;
        this.cancel = cancel;
        this.progress = progress;
        this.post = post;
        this.downloadName = downloadName;
    }

    /**
     * Returns the request number.
     * <p>
     * 요청 번호를 반환한다.
     *
     * @return request number / 요청 번호
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the navigation type.
     * <p>
     * 이동 종류를 반환한다.
     *
     * @return type / 종류
     */
    public NavigationType getType() {
        return type;
    }

    /**
     * Returns the target URL.
     * <p>
     * 대상 URL 을 반환한다.
     *
     * @return URL / URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the source text of an <code>about:text</code> navigation, or null.
     * <p>
     * <code>about:text</code> 이동의 원문을 반환한다. 아니면 null.
     *
     * @return source text or null / 원문 또는 null
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the MIME type of an <code>about:text</code> navigation, or null.
     * <p>
     * <code>about:text</code> 이동의 MIME 타입을 반환한다. 아니면 null.
     *
     * @return MIME type or null / MIME 타입 또는 null
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Tells whether caches must be bypassed.
     * <p>
     * 캐시를 우회해야 하는지 알려 준다.
     *
     * @return true for a forced reload / 강제 새로고침이면 true
     */
    public boolean isForce() {
        return force;
    }

    /**
     * Returns the stored history entry that is being restored or refreshed, or null.
     * <p>
     * 복원하거나 새로고침하는 보관된 이력 항목을 반환한다. 없으면 null.
     *
     * @return stored entry or null / 보관된 항목 또는 null
     */
    public PageSnapshot getRestore() {
        return restore;
    }

    /**
     * Returns the target history position of a history move, or -1.
     * <p>
     * 이력 이동의 목표 위치를 반환한다. 아니면 -1.
     *
     * @return position or -1 / 위치 또는 -1
     */
    public int getTargetIndex() {
        return targetIndex;
    }

    /**
     * Returns the user agent to send.
     * <p>
     * 보낼 User-Agent 를 반환한다.
     *
     * @return user agent / User-Agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Returns the extra headers to send (a private copy).
     * <p>
     * 보낼 추가 헤더(전용 복사본)를 반환한다.
     *
     * @return headers / 헤더
     */
    public RequestHeaders getHeaders() {
        return headers;
    }

    /**
     * Tells whether JavaScript was enabled when the navigation started.
     * <p>
     * 이동 시작 시 JavaScript 가 켜져 있었는지 알려 준다.
     *
     * @return true if enabled / 켜져 있으면 true
     */
    public boolean isJavascriptEnabled() {
        return javascriptEnabled;
    }

    /**
     * Returns the cancel token of this navigation.
     * <p>
     * 이 이동의 취소 토큰을 반환한다.
     *
     * @return token / 토큰
     */
    public CancelToken getCancelToken() {
        return cancel;
    }

    /**
     * Returns the form submission that this navigation sends.
     * <p>
     * 이 이동이 보내는 폼 제출을 반환한다.
     *
     * @return the submission, or null for a plain <code>GET</code> / 제출. 일반 <code>GET</code> 이면 null
     */
    public PostData getPost() {
        return post;
    }

    /**
     * Tells whether the response must be saved as a file whatever its type (a same-origin link with the <code>download</code> attribute).
     * <p>
     * 응답을 형식과 관계없이 파일로 저장해야 하는지 알려 준다(<code>download</code> 속성이 있는 같은 오리진 링크).
     *
     * @return true to save the response / 응답을 저장해야 하면 true
     */
    public boolean isDownload() {
        return downloadName != null;
    }

    /**
     * Returns the file name that the link suggested for saving the response.
     * <p>
     * 링크가 응답을 저장할 때 제안한 파일 이름을 반환한다.
     *
     * @return the name, empty if the <code>download</code> attribute had no value / 이름. <code>download</code> 속성에 값이 없으면 빈 문자열
     */
    public String getDownloadName() {
        return downloadName == null ? "" : downloadName;
    }

    /**
     * Reports status and progress of this navigation; ignored when the request is no longer current.
     * <p>
     * 이 이동의 상태와 진행률을 보고한다. 요청이 더 이상 현재가 아니면 무시된다.
     *
     * @param statusText status bar text / 상태바 메시지
     * @param percent    0 to 100, or -1 when unknown / 0~100, 모르면 -1
     */
    public void report(String statusText, int percent) {
        progress.report(id, statusText, percent);
    }
}
