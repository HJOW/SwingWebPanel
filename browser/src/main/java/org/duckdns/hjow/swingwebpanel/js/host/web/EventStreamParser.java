package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * The parser of an event stream (HTML Standard "Server-sent events", sections "Parsing an event stream" and "Interpreting an event stream"). It
 * is fed with the raw bytes of the body in whatever pieces they arrive, cuts them into lines (<code>CRLF</code>, <code>LF</code> and a lone
 * <code>CR</code> end a line, also when the pair is split between two pieces), decodes every line as UTF-8 (one leading byte order mark is stripped)
 * and reports what the lines mean to a {@link Sink}: <code>retry</code> fields and the dispatching of an event at each blank line. It knows nothing
 * of JavaScript or of the network. A line or an event whose data is longer than {@link #MAX_BUFFER} makes the stream {@link #isOverflowed()} and the
 * parser ignores the rest of it: the owner is expected to fail the connection. The data that is still pending when the stream ends is never
 * dispatched, as the specification says.
 * <p>
 * 이벤트 스트림의 해석기(HTML Standard "Server-sent events" 의 "Parsing an event stream", "Interpreting an event stream"). 본문의 원시 바이트를
 * 도착하는 조각 그대로 받아 줄로 자르고(<code>CRLF</code>, <code>LF</code>, 홀로 있는 <code>CR</code> 이 줄을 끝낸다. 쌍이 두 조각에 나뉘어도 마찬가지),
 * 줄마다 UTF-8 로 디코딩하며(맨 앞의 바이트 순서 표시 하나는 떼어 낸다), 줄의 뜻을 {@link Sink} 에 알린다: <code>retry</code> 필드와 빈 줄마다의 이벤트
 * 전달. JavaScript 도 네트워크도 모른다. 한 줄이나 한 이벤트의 데이터가 {@link #MAX_BUFFER} 보다 길면 스트림이 {@link #isOverflowed()} 가 되고 해석기는
 * 나머지를 무시한다. 소유자가 연결을 실패시켜야 한다. 스트림이 끝날 때 아직 남은 데이터는 규격대로 전달하지 않는다.
 */
final class EventStreamParser {

    /**
     * Receives the meaning of the lines.
     * <p>
     * 줄의 뜻을 받는다.
     */
    interface Sink {
        /**
         * The stream sets the reconnection time.
         * <p>
         * 스트림이 재연결 시간을 정한다.
         *
         * @param ms the milliseconds / 밀리초
         */
        void retry(long ms);

        /**
         * A blank line ended a block: the last event ID buffer is taken over, and if the block has data, an event is dispatched.
         * <p>
         * 빈 줄이 블록을 끝냈다. 마지막 이벤트 ID 버퍼를 넘기고, 블록에 데이터가 있으면 이벤트를 전달한다.
         *
         * @param lastEventId the last event ID buffer, never null / 마지막 이벤트 ID 버퍼. null 이 아님
         * @param type        the event type (<code>message</code> if the block named none), null if the block has no data / 이벤트 종류
         *                    (블록이 이름을 주지 않았으면 <code>message</code>). 블록에 데이터가 없으면 null
         * @param data        the data without its final line feed, null if the block has no data / 마지막 줄바꿈을 뺀 데이터. 블록에 데이터가 없으면 null
         */
        void dispatch(String lastEventId, String type, String data);
    }

    /**
     * The longest line and the longest data of one event in bytes or characters: 8 MiB.
     * <p>
     * 한 줄과 한 이벤트의 데이터의 최대 길이(바이트 또는 문자): 8 MiB.
     */
    static final int MAX_BUFFER = 8 * 1024 * 1024;

    /**
     * The largest reconnection time that a <code>retry</code> field can set: it is clamped to this so that a huge number cannot overflow.
     * <p>
     * <code>retry</code> 필드가 정할 수 있는 가장 큰 재연결 시간. 큰 수가 넘치지 않도록 이 값으로 제한한다.
     */
    static final long MAX_RETRY_MS = Integer.MAX_VALUE;

    /**
     * Receives the meaning of the lines.
     * <p>
     * 줄의 뜻을 받는다.
     */
    private final Sink sink;

    /**
     * The bytes of the line that is being read.
     * <p>
     * 읽고 있는 줄의 바이트.
     */
    private final ByteArrayOutputStream line = new ByteArrayOutputStream();

    /**
     * The data buffer of the block that is being read (each data field appends its value and a line feed).
     * <p>
     * 읽고 있는 블록의 데이터 버퍼(data 필드마다 값과 줄바꿈을 덧붙인다).
     */
    private final StringBuilder data = new StringBuilder();

    /**
     * The event type buffer of the block that is being read.
     * <p>
     * 읽고 있는 블록의 이벤트 종류 버퍼.
     */
    private String eventType = "";

    /**
     * The last event ID buffer; it is not reset by a blank line.
     * <p>
     * 마지막 이벤트 ID 버퍼. 빈 줄이 비우지 않는다.
     */
    private String lastEventId;

    /**
     * True right after a <code>CR</code> ended a line: a <code>LF</code> that follows belongs to the same line end.
     * <p>
     * <code>CR</code> 이 줄을 끝낸 직후이면 true. 뒤따르는 <code>LF</code> 는 같은 줄 끝에 속한다.
     */
    private boolean skipLineFeed;

    /**
     * True until the first line has been read (the byte order mark can only start the stream).
     * <p>
     * 첫 줄을 읽기 전까지 true(바이트 순서 표시는 스트림의 시작에만 올 수 있다).
     */
    private boolean atStart = true;

    /**
     * True after a line or an event was too long.
     * <p>
     * 줄이나 이벤트가 너무 길었으면 true.
     */
    private boolean overflowed;

    /**
     * Creates a parser for one connection.
     * <p>
     * 연결 하나를 위한 해석기를 만든다.
     *
     * @param initialLastEventId the last event ID of the event source when the connection starts (the buffer begins with it, so a new
     *                           connection that dispatches before it sees an id field keeps the id) / 연결이 시작될 때 이벤트 소스의 마지막 이벤트
     *                           ID(버퍼가 이것으로 시작하므로 id 필드를 보기 전에 이벤트를 전달하는 새 연결도 ID 를 유지한다)
     * @param sink               the receiver / 받는 쪽
     */
    EventStreamParser(String initialLastEventId, Sink sink) {
        this.lastEventId = initialLastEventId == null ? "" : initialLastEventId;
        this.sink = sink;
    }

    /**
     * Tells whether a line or an event was too long (the parser then ignores everything).
     * <p>
     * 줄이나 이벤트가 너무 길었는지 알려 준다(그러면 해석기는 모든 것을 무시한다).
     *
     * @return true after an overflow / 넘친 뒤이면 true
     */
    boolean isOverflowed() {
        return overflowed;
    }

    /**
     * Feeds a piece of the body.
     * <p>
     * 본문의 조각을 받는다.
     *
     * @param bytes  the bytes / 바이트
     * @param offset the first byte / 첫 바이트
     * @param length how many bytes / 바이트 수
     */
    void feed(byte[] bytes, int offset, int length) {
        int end = offset + length;
        // 바이트를 하나씩 보며 줄 끝(CR, LF, CRLF)을 찾는다. 줄 끝은 ASCII 이므로 UTF-8 의 어느 바이트와도 헷갈리지 않는다.
        for (int i = offset; i < end && !overflowed; i++) {
            byte b = bytes[i];
            if (skipLineFeed) {
                skipLineFeed = false;
                // CR 바로 뒤의 LF 는 같은 줄 끝의 일부이므로 건너뛴다.
                if (b == '\n') {
                    continue;
                }
            }
            if (b == '\r') {
                endLine();
                skipLineFeed = true;
            } else if (b == '\n') {
                endLine();
            } else if (line.size() >= MAX_BUFFER) {
                overflowed = true;
            } else {
                line.write(b);
            }
        }
    }

    /**
     * Ends the stream: whatever is pending (a line without its end, a block without its blank line) is discarded.
     * <p>
     * 스트림을 끝낸다. 남은 것(끝이 없는 줄, 빈 줄이 없는 블록)은 버린다.
     */
    void finish() {
        line.reset();
        data.setLength(0);
        eventType = "";
    }

    /**
     * Processes the line that ended.
     * <p>
     * 끝난 줄을 처리한다.
     */
    private void endLine() {
        String text;
        try {
            text = new String(line.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            text = "";
        }
        line.reset();
        // 스트림의 맨 앞에 있는 바이트 순서 표시(U+FEFF) 하나는 떼어 낸다.
        if (atStart) {
            atStart = false;
            if (text.length() > 0 && text.charAt(0) == (char) 0xFEFF) {
                text = text.substring(1);
            }
        }
        if (text.length() == 0) {
            dispatch();
            return;
        }
        // 콜론으로 시작하는 줄은 주석이다.
        if (text.charAt(0) == ':') {
            return;
        }
        String field;
        String value;
        int colon = text.indexOf(':');
        if (colon >= 0) {
            field = text.substring(0, colon);
            value = text.substring(colon + 1);
            // 콜론 바로 뒤의 공백 하나는 값에 속하지 않는다.
            if (value.length() > 0 && value.charAt(0) == ' ') {
                value = value.substring(1);
            }
        } else {
            field = text;
            value = "";
        }
        process(field, value);
    }

    /**
     * Processes one field (field names are compared literally, with no case folding).
     * <p>
     * 필드 하나를 처리한다(필드 이름은 대소문자를 접지 않고 글자 그대로 비교한다).
     *
     * @param field the name / 이름
     * @param value the value / 값
     */
    private void process(String field, String value) {
        if ("event".equals(field)) {
            eventType = value;
        } else if ("data".equals(field)) {
            data.append(value).append('\n');
            if (data.length() > MAX_BUFFER) {
                overflowed = true;
            }
        } else if ("id".equals(field)) {
            // NUL 이 든 값은 무시한다.
            if (value.indexOf('\u0000') < 0) {
                lastEventId = value;
            }
        } else if ("retry".equals(field)) {
            Long ms = parseDigits(value);
            if (ms != null) {
                sink.retry(ms.longValue());
            }
        }
        // 그 밖의 필드는 무시한다.
    }

    /**
     * Reads a value that consists of only ASCII digits.
     * <p>
     * ASCII 숫자만으로 이루어진 값을 읽는다.
     *
     * @param value the text / 텍스트
     * @return the number (clamped to {@link #MAX_RETRY_MS}), or null if the text is empty or has another character / 수({@link #MAX_RETRY_MS} 로 제한).
     *         텍스트가 비었거나 다른 글자가 있으면 null
     */
    private static Long parseDigits(String value) {
        if (value.length() == 0) {
            return null;
        }
        long n = 0;
        // 모든 글자가 ASCII 숫자인지 확인하며 값을 모은다. 자릿수가 많으면 한도에서 멈춘다.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
            if (n < MAX_RETRY_MS) {
                n = n * 10 + (c - '0');
            }
        }
        return Long.valueOf(Math.min(n, MAX_RETRY_MS));
    }

    /**
     * Dispatches the block that a blank line ended.
     * <p>
     * 빈 줄이 끝낸 블록을 전달한다.
     */
    private void dispatch() {
        // 데이터 버퍼가 비어 있으면 이벤트는 없고 마지막 이벤트 ID 만 넘긴다.
        if (data.length() == 0) {
            eventType = "";
            sink.dispatch(lastEventId, null, null);
            return;
        }
        data.setLength(data.length() - 1);
        String type = eventType.length() == 0 ? "message" : eventType;
        String text = data.toString();
        data.setLength(0);
        eventType = "";
        sink.dispatch(lastEventId, type, text);
    }
}
