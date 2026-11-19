package org.duckdns.hjow.swingwebpanel.content.xhtml;

/**
 * The result of decoding the bytes of an XML document: the text, the encoding that was used, where that decision came from and, if
 * the bytes are not valid in that encoding, a description of the first problem. The text is the "received source" of the document and
 * is what <code>getContent()</code> returns; its line ends are not changed. If an encoding error was found, the text ends just before the
 * malformed bytes, because an XML document with an invalid byte sequence is not well-formed (XML 1.0, section 4.3.3) and must not be
 * shown as if it were.
 * <p>
 * XML 문서 바이트를 디코딩한 결과: 텍스트, 사용한 인코딩, 그 판정의 출처, 그리고 바이트가 그 인코딩에서 올바르지 않다면 첫 문제의 설명. 텍스트는
 * 문서의 "수신 원문"이며 <code>getContent()</code> 가 반환하는 값이다. 줄 끝은 바꾸지 않는다. 인코딩 오류가 있으면 올바르지 않은 바이트 직전에서
 * 텍스트가 끝난다. 잘못된 바이트 순서가 있는 XML 문서는 well-formed 가 아니므로(XML 1.0, 4.3.3절) 그대로 보여 주면 안 되기 때문이다.
 */
public final class XmlText {

    /**
     * Source of the decision: a byte order mark.
     * <p>
     * 판정의 출처: 바이트 순서 표시.
     */
    public static final String SOURCE_BOM = "bom";

    /**
     * Source of the decision: the <code>charset</code> parameter of the transport layer (HTTP Content-Type).
     * <p>
     * 판정의 출처: 전송 계층(HTTP Content-Type)의 <code>charset</code> 매개변수.
     */
    public static final String SOURCE_HTTP = "http";

    /**
     * Source of the decision: the <code>encoding</code> of the XML declaration.
     * <p>
     * 판정의 출처: XML 선언의 <code>encoding</code>.
     */
    public static final String SOURCE_DECLARATION = "declaration";

    /**
     * Source of the decision: the first bytes of a UTF-16 document without byte order mark (<code>3C 00 3F 00</code> or <code>00 3C 00 3F</code>).
     * <p>
     * 판정의 출처: 바이트 순서 표시가 없는 UTF-16 문서의 첫 바이트(<code>3C 00 3F 00</code> 또는 <code>00 3C 00 3F</code>).
     */
    public static final String SOURCE_AUTODETECT = "autodetect";

    /**
     * Source of the decision: nothing decided, UTF-8 is the default of XML.
     * <p>
     * 판정의 출처: 아무것도 정하지 못해 XML 의 기본값 UTF-8 을 썼다.
     */
    public static final String SOURCE_DEFAULT = "default";

    /**
     * Source of the decision: the text was given as a string, not as bytes.
     * <p>
     * 판정의 출처: 바이트가 아니라 문자열로 받은 텍스트이다.
     */
    public static final String SOURCE_STRING = "string";

    /**
     * The decoded text (without a byte order mark).
     * <p>
     * 디코딩한 텍스트(바이트 순서 표시 제외).
     */
    private final String text;

    /**
     * The canonical encoding name.
     * <p>
     * 표준 인코딩 이름.
     */
    private final String encoding;

    /**
     * One of the <code>SOURCE_*</code> constants.
     * <p>
     * <code>SOURCE_*</code> 상수 중 하나.
     */
    private final String source;

    /**
     * Description of the first encoding error, or null.
     * <p>
     * 첫 인코딩 오류의 설명. 없으면 null.
     */
    private final String errorMessage;

    /**
     * Line (1 based) of the encoding error in the decoded text, or 0.
     * <p>
     * 디코딩한 텍스트에서 인코딩 오류의 줄(1부터). 없으면 0.
     */
    private final int errorLine;

    /**
     * Column (1 based) of the encoding error, or 0.
     * <p>
     * 인코딩 오류의 열(1부터). 없으면 0.
     */
    private final int errorColumn;

    /**
     * Creates a result.
     * <p>
     * 결과를 만든다.
     *
     * @param text         decoded text / 디코딩한 텍스트
     * @param encoding     canonical encoding name / 표준 인코딩 이름
     * @param source       source constant / 출처 상수
     * @param errorMessage error description or null / 오류 설명 또는 null
     * @param errorLine    line of the error or 0 / 오류의 줄 또는 0
     * @param errorColumn  column of the error or 0 / 오류의 열 또는 0
     */
    XmlText(String text, String encoding, String source, String errorMessage, int errorLine, int errorColumn) {
        this.text = text;
        this.encoding = encoding;
        this.source = source;
        this.errorMessage = errorMessage;
        this.errorLine = errorLine;
        this.errorColumn = errorColumn;
    }

    /**
     * Wraps a string that was supplied directly (for example by <code>setContent</code>): no bytes, no encoding problem.
     * <p>
     * 직접 받은 문자열(예: <code>setContent</code>)을 감싼다: 바이트도 인코딩 문제도 없다.
     *
     * @param text the text / 텍스트
     * @return the result / 결과
     */
    public static XmlText ofString(String text) {
        return new XmlText(text == null ? "" : text, "UTF-8", SOURCE_STRING, null, 0, 0);
    }

    /**
     * Returns the decoded text.
     * <p>
     * 디코딩한 텍스트를 반환한다.
     *
     * @return the text / 텍스트
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the canonical encoding name (for example <code>UTF-8</code> or <code>EUC-KR</code>).
     * <p>
     * 표준 인코딩 이름(예: <code>UTF-8</code>, <code>EUC-KR</code>)을 반환한다.
     *
     * @return the encoding / 인코딩
     */
    public String getEncoding() {
        return encoding;
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
     * Tells whether the bytes were not valid in the chosen encoding.
     * <p>
     * 바이트가 선택한 인코딩에서 올바르지 않았는지 알려 준다.
     *
     * @return true if there is an encoding error / 인코딩 오류가 있으면 true
     */
    public boolean hasError() {
        return errorMessage != null;
    }

    /**
     * Returns the description of the first encoding error, or null.
     * <p>
     * 첫 인코딩 오류의 설명을 반환한다. 없으면 null.
     *
     * @return the description or null / 설명 또는 null
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the line of the encoding error (1 based), or 0.
     * <p>
     * 인코딩 오류의 줄(1부터)을 반환한다. 없으면 0.
     *
     * @return the line / 줄
     */
    public int getErrorLine() {
        return errorLine;
    }

    /**
     * Returns the column of the encoding error (1 based), or 0.
     * <p>
     * 인코딩 오류의 열(1부터)을 반환한다. 없으면 0.
     *
     * @return the column / 열
     */
    public int getErrorColumn() {
        return errorColumn;
    }
}
