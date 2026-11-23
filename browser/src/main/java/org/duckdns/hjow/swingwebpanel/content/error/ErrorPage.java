package org.duckdns.hjow.swingwebpanel.content.error;

/**
 * A generated error page (plan item P17-06): the HTML source and the facts it was made from. The source is what the component keeps as
 * the page source when no server body exists (see the <code>getContent()</code> contract), and the same string is parsed by the normal
 * HTML path, so the page is shown, scrolled and navigated like any other. Every value that came from outside (the address, messages
 * of the network layer or the XML parser) is HTML-escaped in the source; the plain values are available through the getters. Instances
 * are immutable and the HTML is deterministic: the same input always gives the same string.
 * <p>
 * 생성한 오류 페이지(계획 P17-06): HTML 원문과 그 재료가 된 사실들. 원문은 서버 본문이 없을 때 컴포넌트가 페이지 원문으로 보관하는 값이며
 * (<code>getContent()</code> 계약 참조), 같은 문자열을 일반 HTML 경로가 해석하므로 다른 페이지처럼 보이고 스크롤되고 이동된다. 바깥에서 온 모든 값(주소,
 * 네트워크 계층이나 XML 파서의 메시지)은 원문에서 HTML 이스케이프된다. 이스케이프하지 않은 값은 getter 로 얻는다. 인스턴스는 불변이며 HTML 은
 * 결정적이다: 같은 입력은 항상 같은 문자열을 준다.
 */
public final class ErrorPage {

    /**
     * The media type of the source.
     * <p>
     * 원문의 미디어 타입.
     */
    public static final String MIME_TYPE = "text/html";

    /**
     * The kind.
     * <p>
     * 종류.
     */
    private final ErrorKind kind;

    /**
     * The HTTP-like status shown in the heading, or 0.
     * <p>
     * 제목에 보이는 HTTP 비슷한 상태. 없으면 0.
     */
    private final int status;

    /**
     * The short title.
     * <p>
     * 짧은 제목.
     */
    private final String title;

    /**
     * The explanation.
     * <p>
     * 설명.
     */
    private final String message;

    /**
     * The extra text (for example the offending XML line), or "".
     * <p>
     * 덧붙이는 텍스트(예: 문제가 있는 XML 줄). 없으면 "".
     */
    private final String detail;

    /**
     * The address that could not be shown.
     * <p>
     * 보여 줄 수 없었던 주소.
     */
    private final String url;

    /**
     * True if the page has a link to try the address again.
     * <p>
     * 주소를 다시 시도하는 링크가 있으면 true.
     */
    private final boolean retryLink;

    /**
     * The HTML source.
     * <p>
     * HTML 원문.
     */
    private final String html;

    /**
     * Creates a page.
     * <p>
     * 페이지를 만든다.
     *
     * @param kind      the kind / 종류
     * @param status    the status or 0 / 상태 또는 0
     * @param title     the title / 제목
     * @param message   the explanation / 설명
     * @param detail    extra text / 덧붙이는 텍스트
     * @param url       the address / 주소
     * @param retryLink whether the page has the retry link / 다시 시도 링크가 있는지
     * @param html      the HTML source / HTML 원문
     */
    ErrorPage(ErrorKind kind, int status, String title, String message, String detail, String url, boolean retryLink, String html) {
        this.kind = kind;
        this.status = status;
        this.title = title;
        this.message = message;
        this.detail = detail;
        this.url = url;
        this.retryLink = retryLink;
        this.html = html;
    }

    /**
     * Returns the kind of the error.
     * <p>
     * 오류의 종류를 반환한다.
     *
     * @return the kind / 종류
     */
    public ErrorKind getKind() {
        return kind;
    }

    /**
     * Returns the status shown in the heading, or 0 if the page shows none.
     * <p>
     * 제목에 보이는 상태를 반환한다. 없으면 0.
     *
     * @return the status / 상태
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the short title (for example <code>Not Found</code>), without the status.
     * <p>
     * 상태를 뺀 짧은 제목(예: <code>Not Found</code>)을 반환한다.
     *
     * @return the title / 제목
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the heading as shown on the page and used as the document title: the status and the title (<code>404 Not Found</code>), or
     * the title alone if there is no status.
     * <p>
     * 페이지에 보이고 문서 제목으로 쓰이는 제목줄을 반환한다: 상태와 제목(<code>404 Not Found</code>). 상태가 없으면 제목만.
     *
     * @return the heading / 제목줄
     */
    public String getHeading() {
        return status > 0 ? status + " " + title : title;
    }

    /**
     * Returns the explanation as plain text (not escaped).
     * <p>
     * 이스케이프하지 않은 설명을 반환한다.
     *
     * @return the explanation / 설명
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the extra text as plain text (not escaped), or "".
     * <p>
     * 이스케이프하지 않은 덧붙이는 텍스트를 반환한다. 없으면 "".
     *
     * @return the extra text / 덧붙이는 텍스트
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Returns the address that could not be shown, as it was given (not escaped).
     * <p>
     * 보여 줄 수 없었던 주소를 받은 그대로(이스케이프하지 않고) 반환한다.
     *
     * @return the address / 주소
     */
    public String getUrl() {
        return url;
    }

    /**
     * Tells whether the page contains a link that tries the address again. It does not for addresses that the component would refuse
     * to load anyway (a blocked scheme, an address with control characters or white space, a very long address).
     * <p>
     * 페이지에 주소를 다시 시도하는 링크가 있는지 알려 준다. 컴포넌트가 어차피 읽지 않을 주소(차단된 스킴, 제어 문자나 공백이 든 주소, 아주 긴 주소)에는
     * 링크를 만들지 않는다.
     *
     * @return true if there is a retry link / 다시 시도 링크가 있으면 true
     */
    public boolean hasRetryLink() {
        return retryLink;
    }

    /**
     * Returns the HTML source of the page.
     * <p>
     * 페이지의 HTML 원문을 반환한다.
     *
     * @return the HTML / HTML
     */
    public String getHtml() {
        return html;
    }

    /**
     * Returns the media type of {@link #getHtml()}, always <code>text/html</code>.
     * <p>
     * {@link #getHtml()} 의 미디어 타입을 반환한다. 항상 <code>text/html</code>.
     *
     * @return the media type / 미디어 타입
     */
    public String getMimeType() {
        return MIME_TYPE;
    }
}
