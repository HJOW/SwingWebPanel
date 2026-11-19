package org.duckdns.hjow.swingwebpanel.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collector of {@link ParseError}s shared by the tokenizer and the tree builder of one parse. It converts offsets to line and column
 * numbers lazily (only when an error actually occurs), caps the number of stored errors and forwards each error to an optional
 * {@link ParserListener}. Not thread-safe: it belongs to the one thread that runs the parser.
 * <p>
 * 한 번의 파싱에서 토큰화기와 트리 생성기가 공유하는 {@link ParseError} 수집기. 오류가 실제로 생길 때만 위치를 줄/열 번호로 바꾸고, 저장하는
 * 오류 수에 상한을 두며, 각 오류를 선택적인 {@link ParserListener} 에 전달한다. 스레드 안전하지 않다: 파서를 실행하는 한 스레드의 것이다.
 */
public final class ParseErrorLog {

    /**
     * Default maximum number of stored errors (the count keeps growing beyond it).
     * <p>
     * 저장하는 오류 수의 기본 상한(그 이상은 개수만 계속 증가한다).
     */
    public static final int DEFAULT_LIMIT = 1000;

    /**
     * The input text that offsets refer to (it may grow when text is inserted at the insertion point).
     * <p>
     * 위치가 가리키는 입력 문자열(삽입 지점에 문자열이 삽입되면 늘어날 수 있다).
     */
    private final CharSequence input;

    /**
     * Stored errors.
     * <p>
     * 저장된 오류.
     */
    private final List<ParseError> errors = new ArrayList<ParseError>();

    /**
     * Maximum number of stored errors.
     * <p>
     * 저장할 오류의 최대 수.
     */
    private final int limit;

    /**
     * Total number of reported errors, including the ones that were not stored.
     * <p>
     * 저장하지 않은 것을 포함한 보고된 오류의 총 수.
     */
    private int total;

    /**
     * Offset of the previous error, used to continue counting lines from there.
     * <p>
     * 직전 오류의 위치. 줄 수를 거기서부터 이어서 세는 데 쓴다.
     */
    private int lastOffset;

    /**
     * Line number at {@link #lastOffset}.
     * <p>
     * {@link #lastOffset} 위치의 줄 번호.
     */
    private int lastLine = 1;

    /**
     * Column number at {@link #lastOffset}.
     * <p>
     * {@link #lastOffset} 위치의 열 번호.
     */
    private int lastColumn = 1;

    /**
     * Listener that receives each error, or null.
     * <p>
     * 각 오류를 받는 리스너 또는 null.
     */
    private final ParserListener listener;

    /**
     * Creates a log.
     * <p>
     * 수집기를 만든다.
     *
     * @param input    input text the offsets refer to / 위치가 가리키는 입력 문자열
     * @param limit    maximum number of stored errors / 저장할 오류의 최대 수
     * @param listener listener or null / 리스너 또는 null
     */
    public ParseErrorLog(CharSequence input, int limit, ParserListener listener) {
        this.input = input;
        this.limit = limit;
        this.listener = listener;
    }

    /**
     * Reports an error at an offset.
     * <p>
     * 위치에 오류를 보고한다.
     *
     * @param code   error code / 오류 코드
     * @param offset offset in the input / 입력에서의 위치
     */
    public void add(String code, int offset) {
        total++;
        if (errors.size() >= limit && listener == null) {
            return;
        }
        int off = Math.max(0, Math.min(offset, input.length()));
        // 직전 오류의 위치 뒤라면 거기서부터 이어서 세고, 아니면 처음부터 센다(오류가 생길 때만 실행된다).
        int from = 0;
        int line = 1;
        int col = 1;
        if (off >= lastOffset) {
            from = lastOffset;
            line = lastLine;
            col = lastColumn;
        }
        // 시작 위치부터 오류 위치까지 훑어 줄과 열 번호를 구한다.
        for (int i = from; i < off; i++) {
            if (input.charAt(i) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        lastOffset = off;
        lastLine = line;
        lastColumn = col;
        ParseError e = new ParseError(code, off, line, col);
        if (errors.size() < limit) {
            errors.add(e);
        }
        if (listener != null) {
            listener.parseError(e);
        }
    }

    /**
     * Returns the stored errors.
     * <p>
     * 저장된 오류를 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<ParseError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Returns how many errors were reported in total.
     * <p>
     * 보고된 오류의 총 수를 반환한다.
     *
     * @return count / 개수
     */
    public int getTotal() {
        return total;
    }

    /**
     * Returns the error codes in order, for compact assertions.
     * <p>
     * 간단히 비교할 수 있도록 오류 코드를 순서대로 반환한다.
     *
     * @return codes / 코드
     */
    public List<String> getCodes() {
        List<String> codes = new ArrayList<String>();
        // 저장된 오류에서 코드만 뽑는다.
        for (int i = 0; i < errors.size(); i++) {
            codes.add(errors.get(i).getCode());
        }
        return codes;
    }
}
