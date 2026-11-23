package org.duckdns.hjow.swingwebpanel.html;

/**
 * The result of decoding a document: the text, the encoding that was used, how sure the decision is and the original bytes. The
 * text keeps line ends and white space exactly as received (it is what <code>getContent()</code> returns); the normalization that
 * the HTML parser needs is done separately by {@link InputPreprocessor}. The bytes are kept so that the parser can start over with
 * another encoding when a late <code>meta</code> element reveals that the prescan guessed wrong ({@link #redecode(String)}).
 * <p>
 * 문서를 디코딩한 결과: 문자열, 사용한 인코딩, 판정의 확실성, 원본 바이트. 문자열은 줄 끝과 공백을 받은 그대로 보존한다(<code>getContent()</code> 가
 * 반환하는 값이다). HTML 파서에 필요한 정규화는 {@link InputPreprocessor} 가 따로 한다. 늦게 나온 <code>meta</code> 요소가 사전 검사의 추측이
 * 틀렸음을 알려 줄 때 파서가 다른 인코딩으로 다시 시작할 수 있도록 바이트를 보관한다({@link #redecode(String)}).
 */
public final class DecodedText {

    /**
     * The encoding was decided by a byte order mark.
     * <p>
     * 바이트 순서 표시(BOM)로 정해진 인코딩.
     */
    public static final String SOURCE_BOM = "bom";

    /**
     * The encoding came from the transport layer (the <code>charset</code> parameter of the Content-Type header).
     * <p>
     * 전송 계층(Content-Type 헤더의 <code>charset</code> 매개변수)에서 온 인코딩.
     */
    public static final String SOURCE_HTTP = "http";

    /**
     * The encoding was declared by a <code>meta</code> element (found by the prescan or by the tree builder).
     * <p>
     * <code>meta</code> 요소(사전 검사 또는 트리 생성기가 찾음)가 선언한 인코딩.
     */
    public static final String SOURCE_META = "meta";

    /**
     * The encoding was guessed because the bytes are valid UTF-8 with multi-byte sequences.
     * <p>
     * 바이트가 멀티바이트 순서가 있는 올바른 UTF-8 이라서 추측한 인코딩.
     */
    public static final String SOURCE_GUESS = "guess";

    /**
     * The fallback encoding was used.
     * <p>
     * 대체 인코딩을 사용했다.
     */
    public static final String SOURCE_DEFAULT = "default";

    /**
     * The text was supplied as a string (no decoding happened).
     * <p>
     * 문자열로 받은 텍스트(디코딩이 없었다).
     */
    public static final String SOURCE_STRING = "string";

    /**
     * Decoded text without the byte order mark.
     * <p>
     * BOM 을 제외한 디코딩된 문자열.
     */
    private final String text;

    /**
     * Original bytes (not copied; callers must not modify them), or null for text that was supplied as a string.
     * <p>
     * 원본 바이트(복사하지 않으므로 호출자가 수정하면 안 된다). 문자열로 받은 텍스트는 null.
     */
    private final byte[] raw;

    /**
     * Canonical encoding name.
     * <p>
     * 표준 인코딩 이름.
     */
    private final String encoding;

    /**
     * True if the decision is certain (BOM or transport layer, or a charset that the parser confirmed), false if tentative.
     * <p>
     * 판정이 확실하면(BOM, 전송 계층, 또는 파서가 확인한 인코딩) true, 잠정적이면 false.
     */
    private final boolean certain;

    /**
     * Where the decision came from, one of the <code>SOURCE_*</code> constants.
     * <p>
     * 판정의 출처. <code>SOURCE_*</code> 상수 중 하나.
     */
    private final String source;

    /**
     * Creates a result.
     * <p>
     * 결과를 만든다.
     *
     * @param text     decoded text / 디코딩된 문자열
     * @param raw      original bytes or null / 원본 바이트 또는 null
     * @param encoding canonical encoding name / 표준 인코딩 이름
     * @param certain  whether the decision is certain / 판정이 확실한지
     * @param source   source constant / 출처 상수
     */
    DecodedText(String text, byte[] raw, String encoding, boolean certain, String source) {
        this.text = text;
        this.raw = raw;
        this.encoding = encoding;
        this.certain = certain;
        this.source = source;
    }

    /**
     * Wraps text that was supplied as a string (for example by <code>setContent</code>): UTF-8 is reported, the decision is certain and
     * there are no bytes.
     * <p>
     * 문자열로 받은 텍스트(예: <code>setContent</code>)를 감싼다. UTF-8 로 보고하고 판정은 확실하며 바이트는 없다.
     *
     * @param text text / 문자열
     * @return the result / 결과
     */
    public static DecodedText ofString(String text) {
        return new DecodedText(text, null, EncodingLabels.UTF_8, true, SOURCE_STRING);
    }

    /**
     * Returns the decoded text (byte order mark removed, line ends and white space untouched).
     * <p>
     * 디코딩된 문자열을 반환한다(BOM 제거, 줄 끝과 공백은 그대로).
     *
     * @return text / 문자열
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the original bytes (do not modify), or null if the text was supplied as a string.
     * <p>
     * 원본 바이트를 반환한다(수정 금지). 문자열로 받은 텍스트이면 null.
     *
     * @return bytes or null / 바이트 또는 null
     */
    public byte[] getRawBytes() {
        return raw;
    }

    /**
     * Returns the canonical encoding name (for example <code>UTF-8</code>, <code>EUC-KR</code>, <code>windows-1252</code>).
     * <p>
     * 표준 인코딩 이름을 반환한다(예: <code>UTF-8</code>, <code>EUC-KR</code>, <code>windows-1252</code>).
     *
     * @return encoding name / 인코딩 이름
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Tells whether the encoding decision is certain.
     * <p>
     * 인코딩 판정이 확실한지 알려 준다.
     *
     * @return true if certain, false if tentative / 확실하면 true, 잠정적이면 false
     */
    public boolean isCertain() {
        return certain;
    }

    /**
     * Returns where the encoding decision came from.
     * <p>
     * 인코딩 판정의 출처를 반환한다.
     *
     * @return one of the <code>SOURCE_*</code> constants / <code>SOURCE_*</code> 상수 중 하나
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns a copy of this result whose encoding decision is marked certain (a <code>meta</code> element confirmed the guess).
     * <p>
     * 인코딩 판정을 확실로 표시한 복사본을 반환한다(<code>meta</code> 요소가 추측을 확인했을 때).
     *
     * @return the result marked certain / 확실로 표시한 결과
     */
    public DecodedText withCertainty() {
        return certain ? this : new DecodedText(text, raw, encoding, true, DecodedText.SOURCE_META);
    }

    /**
     * Decodes the original bytes again with another encoding and marks the result certain ("change the encoding"). A byte order mark
     * of the original bytes is skipped as before. Returns this object if there are no bytes or the encoding is the same.
     * <p>
     * 원본 바이트를 다른 인코딩으로 다시 디코딩하고 결과를 확실로 표시한다("change the encoding"). 원본의 BOM 은 전과 같이 건너뛴다. 바이트가 없거나
     * 인코딩이 같으면 이 객체를 반환한다.
     *
     * @param canonicalName canonical encoding name / 표준 인코딩 이름
     * @return the new result / 새 결과
     */
    public DecodedText redecode(String canonicalName) {
        if (raw == null || canonicalName == null || canonicalName.equals(encoding)) {
            return this;
        }
        int skip = EncodingSniffer.bomLength(raw);
        return new DecodedText(EncodingLabels.decode(raw, skip, canonicalName), raw, canonicalName, true, SOURCE_META);
    }
}
