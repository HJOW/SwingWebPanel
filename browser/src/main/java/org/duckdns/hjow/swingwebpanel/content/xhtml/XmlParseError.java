package org.duckdns.hjow.swingwebpanel.content.xhtml;

/**
 * A structured XML error (plan items P17-05, P17-06): what went wrong, where (line and column of the document text, both 1 based) and the
 * text of the offending line. XML has no error recovery: the first well-formedness error ends the parse, and a caller shows an error page
 * built from this object instead of falling back to the forgiving HTML parser. Instances are immutable.
 * <p>
 * 구조화된 XML 오류(계획 P17-05, P17-06): 무엇이 잘못되었는지, 어디인지(문서 텍스트의 줄과 열, 둘 다 1부터), 문제의 줄 텍스트. XML 에는 오류 복구가 없다:
 * 첫 well-formed 오류가 해석을 끝내며, 호출자는 관대한 HTML 파서로 되돌아가는 대신 이 객체로 만든 오류 페이지를 보여 준다. 인스턴스는 불변이다.
 */
public final class XmlParseError {

    /**
     * The kinds of error.
     * <p>
     * 오류의 종류.
     */
    public enum Kind {
        /**
         * A well-formedness error of the XML syntax or of the namespace rules (including an undeclared entity).
         * <p>
         * XML 구문이나 네임스페이스 규칙의 well-formed 오류(선언되지 않은 엔티티 포함).
         */
        NOT_WELL_FORMED,

        /**
         * The bytes are not valid in the document's encoding.
         * <p>
         * 바이트가 문서의 인코딩에서 올바르지 않다.
         */
        ENCODING,

        /**
         * A limit of {@link XhtmlLimits} was exceeded (input size, depth, node count, entity expansion, time).
         * <p>
         * {@link XhtmlLimits} 의 상한을 넘었다(입력 크기, 깊이, 노드 수, 엔티티 확장, 시간).
         */
        LIMIT_EXCEEDED,

        /**
         * The parser itself failed in a way that is not a property of the document.
         * <p>
         * 문서의 성질이 아닌 파서 자체의 실패.
         */
        INTERNAL
    }

    /**
     * Maximum length of the source line that is kept.
     * <p>
     * 보관하는 소스 줄의 최대 길이.
     */
    private static final int MAX_SOURCE_LINE = 200;

    /**
     * The kind.
     * <p>
     * 종류.
     */
    private final Kind kind;

    /**
     * The message.
     * <p>
     * 메시지.
     */
    private final String message;

    /**
     * The line (1 based), 0 if unknown.
     * <p>
     * 줄(1부터). 모르면 0.
     */
    private final int line;

    /**
     * The column (1 based), 0 if unknown.
     * <p>
     * 열(1부터). 모르면 0.
     */
    private final int column;

    /**
     * The text of the offending line, "" if unknown.
     * <p>
     * 문제의 줄 텍스트. 모르면 "".
     */
    private final String sourceLine;

    /**
     * Creates an error; the source line is cut out of the document text.
     * <p>
     * 오류를 만든다. 소스 줄은 문서 텍스트에서 잘라 낸다.
     *
     * @param kind    the kind / 종류
     * @param message the message / 메시지
     * @param line    line (1 based) or 0 / 줄(1부터) 또는 0
     * @param column  column (1 based) or 0 / 열(1부터) 또는 0
     * @param text    the document text the position refers to, or null / 위치가 가리키는 문서 텍스트. 없으면 null
     */
    XmlParseError(Kind kind, String message, int line, int column, String text) {
        this.kind = kind;
        this.message = message == null ? "" : message;
        this.line = line;
        this.column = column;
        this.sourceLine = lineOf(text, line);
    }

    /**
     * Cuts a line out of a text, limited to a maximum length.
     * <p>
     * 텍스트에서 줄 하나를 잘라 낸다. 길이는 제한한다.
     *
     * @param text the text / 텍스트
     * @param line the line number (1 based) / 줄 번호(1부터)
     * @return the line without its line end, or "" / 줄 끝을 뺀 줄. 없으면 ""
     */
    static String lineOf(String text, int line) {
        if (text == null || line < 1) {
            return "";
        }
        int start = 0;
        int current = 1;
        int n = text.length();
        // 원하는 줄의 시작까지 줄 바꿈(LF, CR LF, CR)을 센다.
        while (current < line && start < n) {
            char c = text.charAt(start);
            start++;
            if (c == '\r' && start < n && text.charAt(start) == '\n') {
                start++;
                current++;
            } else if (c == '\n' || c == '\r') {
                current++;
            }
        }
        if (current < line) {
            return "";
        }
        int end = start;
        // 줄 끝까지 가되 최대 길이를 넘기면 멈춘다.
        while (end < n && text.charAt(end) != '\n' && text.charAt(end) != '\r' && end - start < MAX_SOURCE_LINE) {
            end++;
        }
        return text.substring(start, end);
    }

    /**
     * Returns the kind of the error.
     * <p>
     * 오류의 종류를 반환한다.
     *
     * @return the kind / 종류
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the message, as the XML parser phrased it (English).
     * <p>
     * XML 파서가 쓴 대로(영어) 메시지를 반환한다.
     *
     * @return the message / 메시지
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the line of the error (1 based), or 0 if unknown.
     * <p>
     * 오류의 줄(1부터)을 반환한다. 모르면 0.
     *
     * @return the line / 줄
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column of the error (1 based), or 0 if unknown.
     * <p>
     * 오류의 열(1부터)을 반환한다. 모르면 0.
     *
     * @return the column / 열
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns the text of the offending line (at most 200 characters), or "".
     * <p>
     * 문제의 줄 텍스트(최대 200자)를 반환한다. 없으면 "".
     *
     * @return the source line / 소스 줄
     */
    public String getSourceLine() {
        return sourceLine;
    }

    /**
     * Returns a one line description such as <code>NOT_WELL_FORMED at 3:5: The element type "p" must be terminated ...</code>.
     * <p>
     * <code>NOT_WELL_FORMED at 3:5: The element type "p" must be terminated ...</code> 같은 한 줄 설명을 반환한다.
     *
     * @return the description / 설명
     */
    public String toString() {
        return kind + " at " + line + ":" + column + ": " + message;
    }
}
