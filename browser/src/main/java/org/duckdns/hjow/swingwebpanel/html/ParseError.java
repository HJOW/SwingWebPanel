package org.duckdns.hjow.swingwebpanel.html;

/**
 * One parse error (a diagnostic) reported while decoding, tokenizing or tree building. HTML parse errors never stop parsing; they are
 * only collected for diagnostics. The <code>code</code> is the error code of the HTML Standard (for example
 * <code>eof-in-tag</code>) or a tree construction code of this implementation (for example <code>unexpected-start-tag</code>).
 * <p>
 * 디코딩, 토큰화, 트리 생성 중 보고되는 파싱 오류(진단) 하나. HTML 파싱 오류는 파싱을 멈추지 않으며 진단용으로만 모은다.
 * <code>code</code> 는 HTML 표준의 오류 코드(예: <code>eof-in-tag</code>)이거나 이 구현의 트리 생성 코드(예: <code>unexpected-start-tag</code>)이다.
 */
public final class ParseError {

    /**
     * Error code.
     * <p>
     * 오류 코드.
     */
    private final String code;

    /**
     * Offset in the preprocessed input (0 based, in UTF-16 code units).
     * <p>
     * 전처리된 입력에서의 위치(0 기반, UTF-16 코드 단위).
     */
    private final int offset;

    /**
     * Line number (1 based).
     * <p>
     * 줄 번호(1 기반).
     */
    private final int line;

    /**
     * Column number (1 based).
     * <p>
     * 열 번호(1 기반).
     */
    private final int column;

    /**
     * Creates an error.
     * <p>
     * 오류를 만든다.
     *
     * @param code   error code / 오류 코드
     * @param offset offset in the input / 입력에서의 위치
     * @param line   line number / 줄 번호
     * @param column column number / 열 번호
     */
    ParseError(String code, int offset, int line, int column) {
        this.code = code;
        this.offset = offset;
        this.line = line;
        this.column = column;
    }

    /**
     * Returns the error code.
     * <p>
     * 오류 코드를 반환한다.
     *
     * @return code / 코드
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the offset in the preprocessed input.
     * <p>
     * 전처리된 입력에서의 위치를 반환한다.
     *
     * @return offset / 위치
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the line number (1 based).
     * <p>
     * 줄 번호(1 기반)를 반환한다.
     *
     * @return line / 줄
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column number (1 based).
     * <p>
     * 열 번호(1 기반)를 반환한다.
     *
     * @return column / 열
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns <code>code (line:column)</code>.
     * <p>
     * <code>코드 (줄:열)</code> 문자열을 반환한다.
     *
     * @return text / 문자열
     */
    public String toString() {
        return code + " (" + line + ":" + column + ")";
    }
}
