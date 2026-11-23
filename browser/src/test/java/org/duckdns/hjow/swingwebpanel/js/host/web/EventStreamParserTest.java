package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests {@link EventStreamParser} against the rules and the examples of the HTML Standard "Parsing an event stream" and "Interpreting an event
 * stream" (the specification text fetched on 2026-11-17, see <code>docs/progress/webapi2.md</code>).
 * <p>
 * {@link EventStreamParser} 를 HTML Standard "Parsing an event stream", "Interpreting an event stream" 의 규칙과 예제로 시험한다(2026-11-17 에 조회한 규격
 * 본문, <code>docs/progress/webapi2.md</code> 참조).
 */
public class EventStreamParserTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public EventStreamParserTest() {
    }

    /**
     * Collects what the parser reports as one string per notification.
     * <p>
     * 해석기가 알리는 것을 알림마다 문자열 하나로 모은다.
     */
    private static final class Log implements EventStreamParser.Sink {
        /**
         * The notifications.
         * <p>
         * 알림.
         */
        final List<String> items = new ArrayList<String>();

        /**
         * Records a retry.
         * <p>
         * retry 를 기록한다.
         *
         * @param ms the milliseconds / 밀리초
         */
        public void retry(long ms) {
            items.add("retry:" + ms);
        }

        /**
         * Records a dispatch.
         * <p>
         * dispatch 를 기록한다.
         *
         * @param lastEventId the ID buffer / ID 버퍼
         * @param type        the type / 종류
         * @param data        the data / 데이터
         */
        public void dispatch(String lastEventId, String type, String data) {
            // 데이터가 없는 블록은 ID 가 있을 때만 기록한다(빈 줄마다 오는 알림으로 시험이 시끄러워지지 않게).
            if (data == null) {
                if (lastEventId.length() > 0) {
                    items.add("id[" + lastEventId + "]");
                }
                return;
            }
            items.add(type + "[" + lastEventId + "]=" + data.replace("\n", "\\n"));
        }

        /**
         * Joins the notifications with vertical bars.
         * <p>
         * 알림을 세로줄로 잇는다.
         *
         * @return the text / 텍스트
         */
        String text() {
            StringBuilder sb = new StringBuilder();
            // 알림을 순서대로 이어 붙인다.
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    sb.append('|');
                }
                sb.append(items.get(i));
            }
            return sb.toString();
        }
    }

    /**
     * Parses a whole stream given as one piece (the text is turned into bytes with UTF-8) and returns the notifications.
     * <p>
     * 한 조각으로 준 스트림 전체를 해석하고 알림을 반환한다(텍스트는 UTF-8 바이트로 바꾼다).
     *
     * @param text the stream / 스트림
     * @return the notifications / 알림
     */
    private static String parse(String text) {
        Log log = new Log();
        EventStreamParser p = new EventStreamParser("", log);
        byte[] b = bytes(text);
        p.feed(b, 0, b.length);
        p.finish();
        return log.text();
    }

    /**
     * Turns text into UTF-8 bytes.
     * <p>
     * 텍스트를 UTF-8 바이트로 바꾼다.
     *
     * @param text the text / 텍스트
     * @return the bytes / 바이트
     */
    private static byte[] bytes(String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * A simple event and the multi-line data of the specification example.
     * <p>
     * 단순한 이벤트와 규격 예제의 여러 줄 데이터.
     */
    public void testSimpleAndMultiLineData() {
        Check.equal("message[]=This is the first message.", parse("data: This is the first message.\n\n"), "one event");
        Check.equal("message[]=This is the first message.|message[]=This is the second message, it\\nhas two lines.|message[]=This is the third message.",
                parse("data: This is the first message.\n\ndata: This is the second message, it\ndata: has two lines.\n\ndata: This is the third message.\n\n"),
                "the three events of the introduction");
        Check.equal("message[]=YHOO\\n+2\\n10", parse("data: YHOO\ndata: +2\ndata: 10\n\n"), "the stock example");
    }

    /**
     * The event field names the type; an empty value or a missing field means <code>message</code>.
     * <p>
     * event 필드가 종류를 정한다. 빈 값이나 없는 필드는 <code>message</code> 이다.
     */
    public void testEventTypes() {
        Check.equal("add[]=73857293|remove[]=2153|add[]=113411",
                parse("event: add\ndata: 73857293\n\nevent: remove\ndata: 2153\n\nevent: add\ndata: 113411\n\n"), "the add/remove stream of the introduction");
        Check.equal("message[]=x", parse("event:\ndata: x\n\n"), "an empty event field means message");
        Check.equal("message[]=y", parse("event: add\n\ndata: y\n\n"), "a block without data drops its event type");
    }

    /**
     * The last event ID buffer: set by id fields, kept across blocks, reset by an empty value, ignored when it has a NUL.
     * <p>
     * 마지막 이벤트 ID 버퍼: id 필드가 정하고, 블록을 넘어 유지되며, 빈 값이 지우고, NUL 이 있으면 무시한다.
     */
    public void testLastEventId() {
        Check.equal("message[1]=first event|message[]=second event|message[]= third event",
                parse(": test stream\n\ndata: first event\nid: 1\n\ndata:second event\nid\n\ndata:  third event\n\n"), "the example of the specification");
        Check.equal("message[5]=a|message[5]=b", parse("id: 5\ndata: a\n\ndata: b\n\n"), "the ID stays for later events");
        Check.equal("message[7]=a", parse("id: 7\nid: 7\u0000\ndata: a\n\n"), "an id with a NUL is ignored");
        Check.equal("id[9]|message[9]=a", parse("id: 9\n\ndata: a\n\n"), "a block without data only sets the ID");
        // 새 연결은 소스의 마지막 이벤트 ID 로 시작한다.
        Log log = new Log();
        EventStreamParser p = new EventStreamParser("old", log);
        byte[] b = bytes("data: x\n\n");
        p.feed(b, 0, b.length);
        Check.equal("message[old]=x", log.text(), "a new connection starts with the ID of the source");
    }

    /**
     * Blank data fields and the discarded last block of the specification.
     * <p>
     * 규격의 빈 data 필드와 버려지는 마지막 블록.
     */
    public void testEmptyDataAndUnterminatedBlock() {
        Check.equal("message[]=|message[]=\\n", parse("data\n\ndata\ndata\n\ndata:\n"), "two events: empty data, a single newline; the last block has no blank line");
        Check.equal("message[]=test|message[]=test", parse("data:test\n\ndata: test\n\n"), "the space after the colon is optional");
        Check.equal("", parse("data: never\n"), "an event without its blank line is discarded");
        Check.equal("message[]=a", parse("data: a\n\ndata: b"), "the unterminated line and block at the end are discarded");
    }

    /**
     * Comments, unknown fields, a line without a colon, the spaces and the case of names.
     * <p>
     * 주석, 알 수 없는 필드, 콜론이 없는 줄, 공백, 이름의 대소문자.
     */
    public void testFieldRules() {
        Check.equal("message[]=x", parse(":comment\nfoo: bar\nDATA: no\ndata: x\n\n"), "comments and unknown fields are ignored, names are case-sensitive");
        Check.equal("message[]=a:b", parse("data: a:b\n\n"), "only the first colon splits");
        Check.equal("message[]=  two", parse("data:   two\n\n"), "only one space is removed");
        Check.equal("message[]=", parse("data:\n\n"), "a field with an empty value");
        Check.equal("message[]=", parse("data\n\n"), "a field without a colon has an empty value");
    }

    /**
     * Line ends: CRLF, LF, CR and a pair that is split between two pieces.
     * <p>
     * 줄 끝: CRLF, LF, CR, 그리고 두 조각에 나뉜 쌍.
     */
    public void testLineEnds() {
        Check.equal("message[]=a|message[]=b|message[]=c", parse("data: a\r\n\r\ndata: b\r\rdata: c\n\n"), "CRLF, a lone CR and LF");
        Check.equal("message[]=a|message[]=b", parse("data: a\r\n\r\ndata: b\n\n"), "CRLF pairs are one line end");
        // CR 과 LF 가 서로 다른 조각에 있어도 하나의 줄 끝이다.
        Log log = new Log();
        EventStreamParser p = new EventStreamParser("", log);
        String[] pieces = { "data: a\r", "\n\r", "\ndata: b\n", "\n" };
        // 조각을 하나씩 순서대로 먹인다.
        for (int i = 0; i < pieces.length; i++) {
            byte[] b = bytes(pieces[i]);
            p.feed(b, 0, b.length);
        }
        Check.equal("message[]=a|message[]=b", log.text(), "a CRLF split between pieces is still one line end (no phantom blank line)");
    }

    /**
     * The byte order mark is removed at the start only.
     * <p>
     * 바이트 순서 표시는 시작에서만 뗀다.
     */
    public void testByteOrderMark() {
        Check.equal("message[]=a", parse("" + (char) 0xFEFF + "data: a\n\n"), "a leading BOM is removed");
        Check.equal("message[]=a", parse("" + (char) 0xFEFF + "\ndata: a\n\n"), "a BOM on an otherwise empty first line is just removed");
        Check.equal("", parse("data: x\n" + (char) 0xFEFF + "data: y\n"), "a BOM later in the stream is part of the field name, so the field is unknown and nothing is dispatched");
    }

    /**
     * The retry field takes digits only, and a huge value is clamped.
     * <p>
     * retry 필드는 숫자만 받고 아주 큰 값은 제한한다.
     */
    public void testRetry() {
        Check.equal("retry:1500", parse("retry: 1500\n\n"), "digits");
        Check.equal("retry:0", parse("retry: 0\n"), "zero is accepted (the line ending without a blank line still sets it)");
        Check.equal("", parse("retry: 15s\nretry:\nretry: -3\nretry: 1.5\nretry: +4\n"), "other values are ignored");
        Check.equal("retry:" + Integer.MAX_VALUE, parse("retry: 99999999999999999999999\n"), "a huge value is clamped");
    }

    /**
     * Multi-byte characters are decoded after the line is whole, whatever the pieces are; invalid bytes become U+FFFD.
     * <p>
     * 여러 바이트 글자는 줄이 완성된 뒤에 디코딩하므로 조각이 어떻든 상관없다. 잘못된 바이트는 U+FFFD 가 된다.
     */
    public void testUtf8() {
        byte[] whole = bytes("data: 한글 😀\n\n");
        // 모든 위치에서 두 조각으로 나누어 먹여도 같은 결과여야 한다.
        for (int cut = 0; cut <= whole.length; cut++) {
            Log log = new Log();
            EventStreamParser p = new EventStreamParser("", log);
            p.feed(whole, 0, cut);
            p.feed(whole, cut, whole.length - cut);
            Check.equal("message[]=한글 😀", log.text(), "split at byte " + cut);
        }
        Log log = new Log();
        EventStreamParser p = new EventStreamParser("", log);
        byte[] bad = new byte[] { 'd', 'a', 't', 'a', ':', ' ', 'a', (byte) 0xFF, 'b', '\n', '\n' };
        p.feed(bad, 0, bad.length);
        Check.equal("message[]=a" + (char) 0xFFFD + "b", log.text(), "an invalid byte becomes the replacement character");
    }

    /**
     * A line or an event that is too long makes the parser overflow and stop reporting.
     * <p>
     * 너무 긴 줄이나 이벤트는 해석기를 넘치게 하고 보고를 멈추게 한다.
     */
    public void testOverflow() {
        Log log = new Log();
        EventStreamParser p = new EventStreamParser("", log);
        byte[] chunk = new byte[1024 * 1024];
        // 1 MiB 짜리 조각을 같은 글자로 채운다.
        for (int i = 0; i < chunk.length; i++) {
            chunk[i] = 'x';
        }
        byte[] head = bytes("data: ");
        p.feed(head, 0, head.length);
        // 줄 끝 없이 한계를 넘을 때까지 먹인다.
        for (int i = 0; i < 9 && !p.isOverflowed(); i++) {
            p.feed(chunk, 0, chunk.length);
        }
        Check.isTrue(p.isOverflowed(), "a line longer than the limit overflows");
        byte[] rest = bytes("\n\ndata: more\n\n");
        p.feed(rest, 0, rest.length);
        Check.equal("", log.text(), "nothing is reported after an overflow");
    }
}
