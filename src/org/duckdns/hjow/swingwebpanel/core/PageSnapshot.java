package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;
import org.duckdns.hjow.swingwebpanel.net.SchemePolicy;

/**
 * An immutable snapshot of one confirmed page: its URL, the received source text, MIME type, base URL and kind.
 * The browser session replaces the whole snapshot at once when a navigation is confirmed, so the URL and the content
 * that callers read always belong to the same page. The source text is never overwritten by DOM changes or by
 * conversions such as Markdown to HTML (API contract 5.9). A snapshot may carry a {@link PageDocument} with the page's live objects.
 * <p>
 * 확정된 페이지 하나의 불변 스냅샷: URL, 수신한 원문, MIME 타입, 기준 URL, 종류. 이동이 확정되면 브라우저 세션이
 * 스냅샷 전체를 한 번에 교체하므로 호출자가 읽는 URL 과 콘텐츠는 항상 같은 페이지에 속한다. 원문은 DOM 변경이나
 * Markdown 의 HTML 변환 같은 가공으로 덮어쓰지 않는다(API 계약 5.9). 스냅샷은 페이지의 살아 있는 객체인
 * {@link PageDocument} 를 가질 수 있다.
 */
public final class PageSnapshot {

    /**
     * Source of document identifiers.
     * <p>
     * 문서 식별자의 공급원.
     */
    private static final java.util.concurrent.atomic.AtomicLong DOCUMENT_IDS = new java.util.concurrent.atomic.AtomicLong();

    /**
     * Shared empty page snapshot.
     * <p>
     * 공유하는 빈 페이지 스냅샷.
     */
    private static final PageSnapshot BLANK = new PageSnapshot(SchemePolicy.ABOUT_BLANK, "", "text/html", SchemePolicy.ABOUT_BLANK,
            PageKind.BLANK, "", 0, null);

    /**
     * Page URL (final URL after redirects, <code>about:blank</code> or <code>about:text</code>).
     * <p>
     * 페이지 URL(리디렉션 이후 최종 URL, <code>about:blank</code> 또는 <code>about:text</code>).
     */
    private volatile String url;

    /**
     * Received source text, decoded with the page charset.
     * <p>
     * 페이지 문자셋으로 디코딩한 수신 원문.
     */
    private final String content;

    /**
     * MIME type without parameters, lower case.
     * <p>
     * 매개변수를 뺀 소문자 MIME 타입.
     */
    private final String mimeType;

    /**
     * Base URL used to resolve relative addresses, or null when there is none.
     * <p>
     * 상대 주소를 해석하는 기준 URL. 없으면 null.
     */
    private final String baseUrl;

    /**
     * Kind of the page.
     * <p>
     * 페이지 종류.
     */
    private final PageKind kind;

    /**
     * Document title, empty if unknown.
     * <p>
     * 문서 제목. 모르면 빈 문자열.
     */
    private final String title;

    /**
     * HTTP status code, or 0 when not applicable.
     * <p>
     * HTTP 상태 코드. 해당 없으면 0.
     */
    private final int statusCode;

    /**
     * Live objects of the page, or null.
     * <p>
     * 페이지의 살아 있는 객체. 없으면 null.
     */
    private final PageDocument document;

    /**
     * Identifies the loaded document: copies of a snapshot (with or without live objects) share it, so that history entries that differ
     * only in the fragment of the URL can be recognized as entries of the same document.
     * <p>
     * 읽어 온 문서를 식별한다. 스냅샷의 복사본(살아 있는 객체의 유무와 상관없이)은 같은 값을 가지므로, URL의 프래그먼트만 다른 이력 항목을 같은 문서의
     * 항목으로 알아볼 수 있다.
     */
    private final long documentId;

    /**
     * Name of the character encoding that the received bytes were decoded with (UTF-8 for pages that are not decoded from bytes). It is the
     * encoding of the document, which external classic scripts inherit when they say nothing about their own.
     * <p>
     * 수신 바이트를 디코딩한 문자 인코딩 이름(바이트에서 디코딩하지 않은 페이지는 UTF-8). 문서의 인코딩이며, 자기 인코딩을 밝히지 않은 외부 일반
     * 스크립트가 이것을 물려받는다.
     */
    private final String encoding;

    /**
     * The form submission that made this page, or null for a page that was loaded by a plain request. It is kept with the history entry so that
     * a history move shows the stored source and a refresh asks the user before the form is sent again.
     * <p>
     * 이 페이지를 만든 폼 제출. 일반 요청으로 읽은 페이지는 null. 이력 이동은 저장한 원문을 보여 주고 새로고침은 폼을 다시 보내기 전에 사용자에게
     * 묻도록 이력 항목과 함께 보관한다.
     */
    private volatile PostData post;

    /**
     * The state object of the history entry (the structured clone that <code>history.pushState</code> stored), or null for an entry without a
     * state. It is kept with the entry, so a document loaded again for the entry, and the entries of one document, find their own state.
     * Like the URL it changes in place when the page changes its entry or moves inside its own document.
     * <p>
     * 이력 항목의 상태 객체(<code>history.pushState</code> 가 저장한 구조화 복제). 상태가 없는 항목이면 null. 항목과 함께 보관되므로 항목을 위해 다시
     * 읽은 문서와 한 문서의 여러 항목이 각자의 상태를 찾는다. URL처럼 페이지가 자기 항목을 바꾸거나 자기 문서 안에서 이동하면 그 자리에서 바뀐다.
     */
    private volatile ClonedValue state;

    /**
     * Creates a snapshot.
     * <p>
     * 스냅샷을 만든다.
     *
     * @param url        page URL / 페이지 URL
     * @param content    source text / 원문
     * @param mimeType   MIME type / MIME 타입
     * @param baseUrl    base URL or null / 기준 URL 또는 null
     * @param kind       page kind / 페이지 종류
     * @param title      title / 제목
     * @param statusCode HTTP status or 0 / HTTP 상태 또는 0
     * @param document   live objects or null / 살아 있는 객체 또는 null
     */
    private PageSnapshot(String url, String content, String mimeType, String baseUrl, PageKind kind, String title,
            int statusCode, PageDocument document) {
        this(url, content, mimeType, baseUrl, kind, title, statusCode, document, DOCUMENT_IDS.incrementAndGet(), "UTF-8", null);
    }

    /**
     * Creates a snapshot that belongs to a given document.
     * <p>
     * 주어진 문서에 속하는 스냅샷을 만든다.
     *
     * @param url        page URL / 페이지 URL
     * @param content    source text / 원문
     * @param mimeType   MIME type / MIME 타입
     * @param baseUrl    base URL or null / 기준 URL 또는 null
     * @param kind       page kind / 페이지 종류
     * @param title      title / 제목
     * @param statusCode HTTP status or 0 / HTTP 상태 또는 0
     * @param document   live objects or null / 살아 있는 객체 또는 null
     * @param documentId identifier of the document / 문서 식별자
     * @param encoding   name of the decoding character encoding / 디코딩한 문자 인코딩 이름
     * @param post       the form submission that made the page or null / 페이지를 만든 폼 제출 또는 null
     */
    private PageSnapshot(String url, String content, String mimeType, String baseUrl, PageKind kind, String title,
            int statusCode, PageDocument document, long documentId, String encoding, PostData post) {
        this.post = post;
        this.documentId = documentId;
        this.encoding = encoding;
        this.url = url;
        this.content = content;
        this.mimeType = mimeType;
        this.baseUrl = baseUrl;
        this.kind = kind;
        this.title = title;
        this.statusCode = statusCode;
        this.document = document;
    }

    /**
     * Returns the empty page snapshot (<code>about:blank</code>, empty source).
     * <p>
     * 빈 페이지 스냅샷(<code>about:blank</code>, 빈 원문)을 반환한다.
     *
     * @return the blank snapshot / 빈 스냅샷
     */
    public static PageSnapshot blank() {
        return BLANK;
    }

    /**
     * Creates a snapshot for content given with <code>setContent</code> (URL <code>about:text</code>).
     * <p>
     * <code>setContent</code> 로 받은 콘텐츠의 스냅샷(URL <code>about:text</code>)을 만든다.
     *
     * @param content  source text kept as given / 입력 그대로 보관하는 원문
     * @param mimeType normalized MIME type / 정규화한 MIME 타입
     * @return the snapshot / 스냅샷
     */
    public static PageSnapshot text(String content, String mimeType) {
        return new PageSnapshot(SchemePolicy.ABOUT_TEXT, content, mimeType, null, PageKind.TEXT, "", 0, null);
    }

    /**
     * Creates a snapshot for a page loaded from a URL.
     * <p>
     * URL 에서 읽은 페이지의 스냅샷을 만든다.
     *
     * @param url        final URL / 최종 URL
     * @param content    received source / 수신 원문
     * @param mimeType   normalized MIME type / 정규화한 MIME 타입
     * @param baseUrl    base URL (normally the final URL) / 기준 URL(보통 최종 URL)
     * @param statusCode HTTP status code or 0 / HTTP 상태 코드 또는 0
     * @return the snapshot / 스냅샷
     */
    public static PageSnapshot loaded(String url, String content, String mimeType, String baseUrl, int statusCode) {
        return new PageSnapshot(url, content, mimeType, baseUrl, PageKind.URL, "", statusCode, null);
    }

    /**
     * Creates a snapshot for a generated or server supplied error page.
     * <p>
     * 생성한 또는 서버가 보낸 오류 페이지의 스냅샷을 만든다.
     *
     * @param url        URL the user asked for / 사용자가 요청한 URL
     * @param content    source text of the error page / 오류 페이지 원문
     * @param mimeType   normalized MIME type / 정규화한 MIME 타입
     * @param statusCode HTTP status code or 0 / HTTP 상태 코드 또는 0
     * @return the snapshot / 스냅샷
     */
    public static PageSnapshot error(String url, String content, String mimeType, int statusCode) {
        return new PageSnapshot(url, content, mimeType, url, PageKind.ERROR, "", statusCode, null);
    }

    /**
     * Returns a copy that carries the given live objects.
     * <p>
     * 주어진 살아 있는 객체를 가진 복사본을 반환한다.
     *
     * @param doc live objects / 살아 있는 객체
     * @return the new snapshot / 새 스냅샷
     */
    public PageSnapshot withDocument(PageDocument doc) {
        return carry(new PageSnapshot(url, content, mimeType, baseUrl, kind, title, statusCode, doc, documentId, encoding, post));
    }

    /**
     * Returns a copy that records the character encoding that the source was decoded with.
     * <p>
     * 원문을 디코딩한 문자 인코딩을 기록한 복사본을 반환한다.
     *
     * @param name encoding name, null or empty keeps UTF-8 / 인코딩 이름. null 이나 빈 값이면 UTF-8
     * @return the new snapshot / 새 스냅샷
     */
    public PageSnapshot withEncoding(String name) {
        return carry(new PageSnapshot(url, content, mimeType, baseUrl, kind, title, statusCode, document, documentId,
                name == null || name.length() == 0 ? "UTF-8" : name, post));
    }

    /**
     * Returns the name of the character encoding that the source was decoded with.
     * <p>
     * 원문을 디코딩한 문자 인코딩의 이름을 반환한다.
     *
     * @return the encoding name / 인코딩 이름
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Returns a copy of this snapshot that remembers the form submission that made the page. A history move then shows the stored source
     * instead of sending the form again, and a refresh sends it again only after the user agreed.
     * <p>
     * 페이지를 만든 폼 제출을 기억하는 이 스냅샷의 복사본을 반환한다. 그러면 이력 이동은 폼을 다시 보내는 대신 저장한 원문을 보여 주고, 새로고침은
     * 사용자가 동의한 뒤에만 다시 보낸다.
     *
     * @param data the form submission, or null / 폼 제출. 없으면 null
     * @return the copy / 복사본
     */
    public PageSnapshot withPost(PostData data) {
        return carry(new PageSnapshot(url, content, mimeType, baseUrl, kind, title, statusCode, document, documentId, encoding, data));
    }

    /**
     * Returns the form submission that made this page.
     * <p>
     * 이 페이지를 만든 폼 제출을 반환한다.
     *
     * @return the submission, or null if the page was loaded by a plain request / 제출. 일반 요청으로 읽은 페이지이면 null
     */
    public PostData getPost() {
        return post;
    }

    /**
     * Returns a copy without live objects (what a history entry keeps).
     * <p>
     * 살아 있는 객체가 없는 복사본을 반환한다(이력 항목이 보관하는 형태).
     *
     * @return the new snapshot / 새 스냅샷
     */
    public PageSnapshot withoutDocument() {
        return document == null ? this : carry(new PageSnapshot(url, content, mimeType, baseUrl, kind, title, statusCode, null, documentId, encoding, post));
    }

    /**
     * Returns a copy with another title.
     * <p>
     * 제목을 바꾼 복사본을 반환한다.
     *
     * @param newTitle new title (null means empty) / 새 제목(null 이면 빈 문자열)
     * @return the new snapshot / 새 스냅샷
     */
    public PageSnapshot withTitle(String newTitle) {
        return carry(new PageSnapshot(url, content, mimeType, baseUrl, kind, newTitle == null ? "" : newTitle, statusCode, document, documentId, encoding, post));
    }

    /**
     * Returns the identifier of the loaded document that this snapshot belongs to.
     * <p>
     * 이 스냅샷이 속한 읽어 온 문서의 식별자를 반환한다.
     *
     * @return the identifier / 식별자
     */
    long getDocumentId() {
        return documentId;
    }

    /**
     * Returns a copy of a snapshot that carries the history state of this one (used by the copy methods).
     * <p>
     * 이 스냅샷의 이력 상태를 가진 복사본을 돌려준다(복사 메서드가 쓴다).
     *
     * @param copy the new copy / 새 복사본
     * @return the same copy / 같은 복사본
     */
    private PageSnapshot carry(PageSnapshot copy) {
        copy.state = state;
        return copy;
    }

    /**
     * Returns the state object of this history entry.
     * <p>
     * 이 이력 항목의 상태 객체를 반환한다.
     *
     * @return the serialized state, or null if the entry has none / 직렬화된 상태. 없으면 null
     */
    public ClonedValue getHistoryState() {
        return state;
    }

    /**
     * Sets the state object of this entry (before the snapshot is shown, or in place for the entry of the current page).
     * <p>
     * 이 항목의 상태 객체를 지정한다(스냅샷이 보이기 전이나 현재 페이지의 항목에서 그 자리에서).
     *
     * @param value the serialized state or null / 직렬화된 상태 또는 null
     */
    void setHistoryState(ClonedValue value) {
        this.state = value;
    }

    /**
     * Moves the entry of the current page to another address of the same document (<code>history.pushState</code>, a traversal inside the
     * document): the URL, the state object and the form submission that the entry represents change together, in place. A pushed entry is
     * a plain GET entry, so the submission is dropped unless the move goes back to the entry that has one.
     * <p>
     * 현재 페이지의 항목을 같은 문서의 다른 주소로 옮긴다(<code>history.pushState</code>, 문서 안의 이동). URL, 상태 객체, 항목이 나타내는 폼 제출이
     * 그 자리에서 함께 바뀐다. 푸시한 항목은 일반 GET 항목이므로 제출을 가진 항목으로 돌아가는 이동이 아니면 제출은 버려진다.
     *
     * @param newUrl   the new URL / 새 URL
     * @param newState the new state or null / 새 상태. 없으면 null
     * @param newPost  the form submission that the new entry represents, or null / 새 항목이 나타내는 폼 제출. 없으면 null
     */
    void changeLocation(String newUrl, ClonedValue newState, PostData newPost) {
        this.url = newUrl;
        this.state = newState;
        this.post = newPost;
    }

    /**
     * Releases the live objects of this snapshot, if any.
     * <p>
     * 이 스냅샷의 살아 있는 객체가 있으면 해제한다.
     */
    public void dispose() {
        if (document != null) {
            document.dispose();
        }
    }

    /**
     * Returns the page URL.
     * <p>
     * 페이지 URL 을 반환한다.
     *
     * @return URL / URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the received source text.
     * <p>
     * 수신 원문을 반환한다.
     *
     * @return source text / 원문
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the MIME type (no parameters, lower case).
     * <p>
     * MIME 타입(매개변수 없음, 소문자)을 반환한다.
     *
     * @return MIME type / MIME 타입
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns the base URL, or null.
     * <p>
     * 기준 URL 을 반환한다. 없으면 null.
     *
     * @return base URL or null / 기준 URL 또는 null
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the page kind.
     * <p>
     * 페이지 종류를 반환한다.
     *
     * @return kind / 종류
     */
    public PageKind getKind() {
        return kind;
    }

    /**
     * Returns the document title (empty if unknown).
     * <p>
     * 문서 제목(모르면 빈 문자열)을 반환한다.
     *
     * @return title / 제목
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the HTTP status code, or 0.
     * <p>
     * HTTP 상태 코드를 반환한다. 없으면 0.
     *
     * @return status code / 상태 코드
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the live objects of the page, or null.
     * <p>
     * 페이지의 살아 있는 객체를 반환한다. 없으면 null.
     *
     * @return live objects or null / 살아 있는 객체 또는 null
     */
    public PageDocument getDocument() {
        return document;
    }
}
