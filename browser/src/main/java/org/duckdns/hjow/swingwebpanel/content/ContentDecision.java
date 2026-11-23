package org.duckdns.hjow.swingwebpanel.content;

/**
 * The outcome of {@link ContentClassifier}: which processor shows a response body, the media type that was used for the decision
 * (the declared one, or the sniffed one if the type was missing), the declared charset label and, for a download, why the body is not
 * shown. Instances are immutable.
 * <p>
 * Typical use: <code>ContentDecision d = ContentClassifier.classifyResponse(header, attachment, firstBytes);</code>, then
 * <code>switch (d.getKind())</code> to choose the HTML parser, the XHTML parser, the plain text builder, the Markdown converter, the media
 * wrapper or the download path.
 * <p>
 * {@link ContentClassifier} 의 결과: 응답 본문을 어느 처리기가 보여 줄지, 판정에 쓴 미디어 타입(선언된 것, 타입이 없으면 내용으로 알아낸 것),
 * 선언된 charset 라벨, 다운로드이면 본문을 보여 주지 않는 이유. 인스턴스는 불변이다.
 * <p>
 * 사용 예: <code>ContentDecision d = ContentClassifier.classifyResponse(header, attachment, firstBytes);</code> 뒤에 <code>switch (d.getKind())</code> 로
 * HTML 파서, XHTML 파서, 일반 텍스트 생성기, Markdown 변환기, 미디어 감싸기, 다운로드 경로 중 하나를 고른다.
 */
public final class ContentDecision {

    /**
     * Why a body is downloaded instead of shown.
     * <p>
     * 본문을 보여 주지 않고 내려받는 이유.
     */
    public enum Reason {
        /**
         * The body is shown (no download).
         * <p>
         * 본문을 보여 준다(다운로드 아님).
         */
        NONE,

        /**
         * The response asked for a download (<code>Content-Disposition: attachment</code>).
         * <p>
         * 응답이 다운로드를 요구했다(<code>Content-Disposition: attachment</code>).
         */
        ATTACHMENT,

        /**
         * The media type is not one that a page can show.
         * <p>
         * 미디어 타입이 페이지가 보여 줄 수 있는 것이 아니다.
         */
        NOT_DISPLAYABLE
    }

    /**
     * The processor.
     * <p>
     * 처리기 종류.
     */
    private final ContentKind kind;

    /**
     * The media type used for the decision, lower case, never null.
     * <p>
     * 판정에 쓴 소문자 미디어 타입. null 이 아니다.
     */
    private final String mime;

    /**
     * The declared charset label or null.
     * <p>
     * 선언된 charset 라벨 또는 null.
     */
    private final String charset;

    /**
     * True if the media type was found by looking at the bytes because the header had none.
     * <p>
     * 헤더에 타입이 없어 바이트를 살펴 미디어 타입을 알아냈으면 true.
     */
    private final boolean sniffed;

    /**
     * The download reason.
     * <p>
     * 다운로드 이유.
     */
    private final Reason reason;

    /**
     * Creates a decision.
     * <p>
     * 결정을 만든다.
     *
     * @param kind    processor / 처리기 종류
     * @param mime    media type / 미디어 타입
     * @param charset charset label or null / charset 라벨 또는 null
     * @param sniffed true if the type was sniffed / 내용으로 알아냈으면 true
     * @param reason  download reason / 다운로드 이유
     */
    ContentDecision(ContentKind kind, String mime, String charset, boolean sniffed, Reason reason) {
        this.kind = kind;
        this.mime = mime;
        this.charset = charset;
        this.sniffed = sniffed;
        this.reason = reason;
    }

    /**
     * Returns the processor that shows the body.
     * <p>
     * 본문을 보여 줄 처리기 종류를 반환한다.
     *
     * @return the kind, never {@link ContentKind#UNKNOWN} / 처리기 종류({@link ContentKind#UNKNOWN} 은 아님)
     */
    public ContentKind getKind() {
        return kind;
    }

    /**
     * Returns the media type used for the decision, in lower case (for example <code>image/png</code>); a sniffed type if the header had
     * none.
     * <p>
     * 판정에 쓴 소문자 미디어 타입(예: <code>image/png</code>)을 반환한다. 헤더에 타입이 없었다면 내용으로 알아낸 타입이다.
     *
     * @return the media type / 미디어 타입
     */
    public String getMime() {
        return mime;
    }

    /**
     * Returns the media type to store with the page for the document processors: the canonical type of the processor
     * (<code>text/html</code>, <code>application/xhtml+xml</code>, <code>text/markdown</code>, <code>text/plain</code> for all text that is shown
     * as plain text) or, for the other kinds, {@link #getMime()}.
     * <p>
     * 문서 처리기를 위해 페이지와 함께 보관할 미디어 타입을 반환한다: 처리기의 정규 타입(<code>text/html</code>, <code>application/xhtml+xml</code>,
     * <code>text/markdown</code>, 일반 텍스트로 보이는 모든 텍스트는 <code>text/plain</code>). 다른 종류는 {@link #getMime()} 이다.
     *
     * @return the canonical media type / 정규 미디어 타입
     */
    public String getDisplayMime() {
        switch (kind) {
        case HTML:
            return "text/html";
        case XHTML:
            return "application/xhtml+xml";
        case MARKDOWN:
            return "text/markdown";
        case PLAIN_TEXT:
            return "text/plain";
        default:
            return mime;
        }
    }

    /**
     * Returns the declared charset label, as written in the header, or null.
     * <p>
     * 헤더에 쓰인 대로의 선언된 charset 라벨을 반환한다. 없으면 null.
     *
     * @return the charset label or null / charset 라벨 또는 null
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Tells whether the media type was found by looking at the bytes (the header had no usable type).
     * <p>
     * 헤더에 쓸 만한 타입이 없어 바이트를 살펴 미디어 타입을 알아냈는지 알려 준다.
     *
     * @return true if sniffed / 내용으로 알아냈으면 true
     */
    public boolean isSniffed() {
        return sniffed;
    }

    /**
     * Tells whether the body goes to the download path.
     * <p>
     * 본문이 다운로드 경로로 가는지 알려 준다.
     *
     * @return true for a download / 다운로드이면 true
     */
    public boolean isDownload() {
        return kind == ContentKind.DOWNLOAD;
    }

    /**
     * Returns why the body is downloaded, or {@link Reason#NONE} if it is shown.
     * <p>
     * 본문을 내려받는 이유를 반환한다. 보여 주면 {@link Reason#NONE}.
     *
     * @return the reason / 이유
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * Returns a short description for logs and tests.
     * <p>
     * 로그와 시험을 위한 짧은 설명을 반환한다.
     *
     * @return text such as <code>HTML text/html</code> / <code>HTML text/html</code> 같은 문자열
     */
    public String toString() {
        return kind + " " + mime + (sniffed ? " (sniffed)" : "") + (reason == Reason.NONE ? "" : " " + reason);
    }
}
