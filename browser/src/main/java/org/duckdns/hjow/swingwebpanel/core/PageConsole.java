package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.js.ConsoleRecord;
import org.duckdns.hjow.swingwebpanel.js.host.HostConsole;

/**
 * The console hook of one page: it turns what the page's realm writes (and what its workers and the page host report) into
 * {@link ConsoleEvent}s and publishes them on the component's {@link ConsoleDispatcher}. The document URL is fixed when the page is created, so
 * a message of an old page still names the old page; after {@link #close()} (the page was left or disposed) it publishes nothing.
 * <p>
 * 한 페이지의 콘솔 연결: 페이지의 realm 이 쓰는 것(그리고 그 worker 와 페이지 호스트가 보고하는 것)을 {@link ConsoleEvent} 로 바꿔 컴포넌트의
 * {@link ConsoleDispatcher} 에 발행한다. 문서 URL 은 페이지를 만들 때 고정되므로 이전 페이지의 메시지는 계속 이전 페이지를 가리키며,
 * {@link #close()} 뒤(페이지를 떠났거나 폐기됨)에는 아무것도 발행하지 않는다.
 */
final class PageConsole implements HostConsole {

    /**
     * The suffix of the internal name of an inline script (the page host names it "document URL" + this).
     * <p>
     * 인라인 스크립트의 내부 이름에 붙는 접미사(페이지 호스트가 "문서 URL" + 이것으로 이름을 붙인다).
     */
    private static final String INLINE_SUFFIX = "#inline";

    /**
     * The dispatcher that receives the events.
     * <p>
     * 이벤트를 받는 통지기.
     */
    private final ConsoleDispatcher dispatcher;

    /**
     * The URL of the document of the page, fixed at creation.
     * <p>
     * 페이지 문서의 URL. 생성 때 고정된다.
     */
    private final String documentUrl;

    /**
     * Whether the page is a frame's document (otherwise the main document).
     * <p>
     * 페이지가 프레임의 문서인지(아니면 주 문서).
     */
    private final boolean frame;

    /**
     * True once the page was left or disposed.
     * <p>
     * 페이지를 떠났거나 폐기된 뒤 true.
     */
    private volatile boolean closed;

    /**
     * Creates the hook of a page.
     * <p>
     * 페이지의 콘솔 연결을 만든다.
     *
     * @param dispatcher  the dispatcher of the component, not null / 컴포넌트의 통지기(null 불가)
     * @param documentUrl the URL of the page's document / 페이지 문서의 URL
     * @param frame       true if the page is the document of a frame / 페이지가 프레임의 문서이면 true
     */
    PageConsole(ConsoleDispatcher dispatcher, String documentUrl, boolean frame) {
        this.dispatcher = dispatcher;
        this.documentUrl = documentUrl == null ? "" : documentUrl;
        this.frame = frame;
    }

    /**
     * Stops publishing: the page was left or disposed, so scripts that are still running must not write to the listeners any more.
     * <p>
     * 발행을 멈춘다: 페이지를 떠났거나 폐기되었으므로 아직 실행 중인 스크립트가 더는 리스너에 쓰면 안 된다.
     */
    void close() {
        closed = true;
    }

    /**
     * Receives a record of the page's own scripts or of the page host.
     * <p>
     * 페이지 자신의 스크립트나 페이지 호스트의 기록을 받는다.
     *
     * @param record the record / 기록
     */
    public void record(ConsoleRecord record) {
        publish(record, frame ? ConsoleEvent.Origin.FRAME : ConsoleEvent.Origin.DOCUMENT, record.getSourceName());
    }

    /**
     * Receives a message in the plain form (a caller that does not know records): a dialog function name as the level is a dialog, anything
     * else a console call of that level.
     * <p>
     * 일반 형태의 메시지를 받는다(기록을 모르는 호출자): 수준 자리에 대화 상자 함수 이름이 오면 대화 상자, 그 밖에는 그 수준의 콘솔 호출이다.
     *
     * @param level the level or dialog function name / 수준 또는 대화 상자 함수 이름
     * @param text  the text / 문자열
     */
    public void message(String level, String text) {
        // 수준 자리의 이름이 대화 상자 함수이면 그 종류로, 아니면 위치를 모르는 콘솔 호출로 기록한다.
        if ("alert".equals(level) || "confirm".equals(level) || "prompt".equals(level)) {
            record(ConsoleRecord.dialog(level, text, "", 0, 0));
        } else {
            record(ConsoleRecord.call(level, level, text, java.util.Collections.singletonList(text == null ? "" : text), "", 0, 0, 0));
        }
    }

    /**
     * Receives a record that a worker of the page wrote.
     * <p>
     * 페이지의 worker 가 쓴 기록을 받는다.
     *
     * @param record    the record / 기록
     * @param workerUrl the worker's script URL / worker 의 스크립트 URL
     * @param shared    true for a shared worker / 공유 worker 이면 true
     */
    public void workerRecord(ConsoleRecord record, String workerUrl, boolean shared) {
        String script = record.getSourceName().length() > 0 ? record.getSourceName() : workerUrl;
        publish(record, shared ? ConsoleEvent.Origin.SHARED_WORKER : ConsoleEvent.Origin.WORKER, script);
    }

    /**
     * Turns a record into an event and publishes it.
     * <p>
     * 기록을 이벤트로 바꿔 발행한다.
     *
     * @param record    the record / 기록
     * @param origin    who wrote it / 쓴 주체
     * @param scriptUrl the script URL / 스크립트 URL
     */
    private void publish(ConsoleRecord record, ConsoleEvent.Origin origin, String scriptUrl) {
        // 페이지를 떠난 뒤 늦게 도착한 메시지는 새 페이지의 것으로 보이지 않도록 발행하지 않는다.
        if (closed || !dispatcher.isListening()) {
            return;
        }
        String script = scriptUrl == null ? "" : scriptUrl;
        // 인라인 스크립트의 내부 이름("문서URL#inline")은 문서의 URL 로 알린다.
        if (script.endsWith(INLINE_SUFFIX)) {
            script = script.substring(0, script.length() - INLINE_SUFFIX.length());
        }
        dispatcher.publish(level(record.getLevel()), kind(record.getKind()), record.getMethod(), record.getText(), record.getArguments(),
                origin, documentUrl, script, record.getLine(), record.getColumn());
    }

    /**
     * Converts the level name of a record.
     * <p>
     * 기록의 수준 이름을 변환한다.
     *
     * @param level the name / 이름
     * @return the level / 수준
     */
    static ConsoleEvent.Level level(String level) {
        if ("info".equals(level)) {
            return ConsoleEvent.Level.INFO;
        }
        if ("warn".equals(level)) {
            return ConsoleEvent.Level.WARN;
        }
        if ("error".equals(level)) {
            return ConsoleEvent.Level.ERROR;
        }
        if ("debug".equals(level)) {
            return ConsoleEvent.Level.DEBUG;
        }
        return ConsoleEvent.Level.LOG;
    }

    /**
     * Converts the kind of a record.
     * <p>
     * 기록의 종류를 변환한다.
     *
     * @param kind the kind / 종류
     * @return the event kind / 이벤트 종류
     */
    static ConsoleEvent.Kind kind(ConsoleRecord.Kind kind) {
        switch (kind) {
        case DIALOG:
            return ConsoleEvent.Kind.DIALOG;
        case UNCAUGHT_EXCEPTION:
            return ConsoleEvent.Kind.UNCAUGHT_EXCEPTION;
        case UNHANDLED_REJECTION:
            return ConsoleEvent.Kind.UNHANDLED_REJECTION;
        case SCRIPT_ABORTED:
            return ConsoleEvent.Kind.SCRIPT_ABORTED;
        case HOST_ERROR:
            return ConsoleEvent.Kind.HOST_ERROR;
        default:
            return ConsoleEvent.Kind.CONSOLE_CALL;
        }
    }
}
