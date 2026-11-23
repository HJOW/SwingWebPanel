package org.duckdns.hjow.swingwebpanel.content;

/**
 * The kinds of processors that a document body can be handed to (plan items P17-01, P01-18): the four text formats that become a DOM
 * document, the media types that are shown through a generated wrapper document (image, audio) or a placeholder (video), and the
 * download path for everything that cannot be shown as a main document. {@link #UNKNOWN} only occurs as the result of looking at a
 * media type alone when the type is missing; a full decision ({@link ContentDecision}) never has it.
 * <p>
 * 문서 본문을 넘길 처리기의 종류(계획 P17-01, P01-18): DOM 문서가 되는 네 가지 텍스트 형식, 생성한 감싸기 문서(이미지, 오디오)나 대체 표시(비디오)로
 * 보이는 미디어 타입, 주 문서로 보일 수 없는 모든 것을 위한 다운로드 경로. {@link #UNKNOWN} 은 타입이 없을 때 미디어 타입만 보고 판정한 결과로만
 * 나오며, 완전한 결정({@link ContentDecision})에는 나오지 않는다.
 */
public enum ContentKind {

    /**
     * HTML (<code>text/html</code>), parsed by the HTML parser.
     * <p>
     * HTML(<code>text/html</code>). HTML 파서가 해석한다.
     */
    HTML,

    /**
     * XHTML (<code>application/xhtml+xml</code>), parsed by the XML parser into a DOM in the XHTML namespace.
     * <p>
     * XHTML(<code>application/xhtml+xml</code>). XML 파서가 XHTML 네임스페이스의 DOM 으로 해석한다.
     */
    XHTML,

    /**
     * Plain text (<code>text/plain</code> and the other types that are shown as text), shown inside one <code>pre</code> element.
     * <p>
     * 일반 텍스트(<code>text/plain</code> 과 텍스트로 보여 주는 다른 타입). <code>pre</code> 요소 하나 안에 보인다.
     */
    PLAIN_TEXT,

    /**
     * Markdown (<code>text/markdown</code>), converted to HTML for display; the original text stays the content.
     * <p>
     * Markdown(<code>text/markdown</code>). 표시를 위해 HTML 로 변환하며 원문은 그대로 콘텐츠로 남는다.
     */
    MARKDOWN,

    /**
     * An image type, shown through a generated document with an <code>img</code> element.
     * <p>
     * 이미지 타입. <code>img</code> 요소가 있는 생성 문서로 보인다.
     */
    IMAGE,

    /**
     * An audio type, shown through a generated document with an <code>audio</code> element.
     * <p>
     * 오디오 타입. <code>audio</code> 요소가 있는 생성 문서로 보인다.
     */
    AUDIO,

    /**
     * A video type: the project does not play video, so a placeholder that says "video is not supported" is shown in its place.
     * <p>
     * 비디오 타입. 이 프로젝트는 비디오를 재생하지 않으므로 그 자리에 "미지원 비디오" 문구를 보인다.
     */
    VIDEO,

    /**
     * A body that cannot be shown as a main document (binary data, archives, attachments): it goes to the download path.
     * <p>
     * 주 문서로 보일 수 없는 본문(바이너리 데이터, 압축 파일, 첨부). 다운로드 경로로 간다.
     */
    DOWNLOAD,

    /**
     * The media type is missing or malformed, so the body has to be looked at (sniffed) to decide.
     * <p>
     * 미디어 타입이 없거나 잘못되어 본문을 살펴(sniff) 판정해야 한다.
     */
    UNKNOWN;

    /**
     * Tells whether a body of this kind is shown in the page area (everything except {@link #DOWNLOAD} and {@link #UNKNOWN}).
     * <p>
     * 이 종류의 본문이 페이지 영역에 보이는지 알려 준다({@link #DOWNLOAD}, {@link #UNKNOWN} 을 제외한 모두).
     *
     * @return true if the page shows it / 페이지가 보여 주면 true
     */
    public boolean isDisplayable() {
        return this != DOWNLOAD && this != UNKNOWN;
    }

    /**
     * Tells whether the body is text that becomes a document through the DOM builders of this layer or the HTML parser
     * ({@link #HTML}, {@link #XHTML}, {@link #PLAIN_TEXT}, {@link #MARKDOWN}).
     * <p>
     * 본문이 이 계층의 DOM 생성기나 HTML 파서로 문서가 되는 텍스트인지 알려 준다({@link #HTML}, {@link #XHTML}, {@link #PLAIN_TEXT}, {@link #MARKDOWN}).
     *
     * @return true for the four text kinds / 네 가지 텍스트 종류이면 true
     */
    public boolean isTextDocument() {
        return this == HTML || this == XHTML || this == PLAIN_TEXT || this == MARKDOWN;
    }
}
