package org.duckdns.hjow.swingwebpanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

/**
 * One message of the page scripts' console, delivered to a {@link ConsoleListener}. It is immutable and holds only strings and numbers (no
 * script object), so a listener may keep it. The <code>source</code> of the event is the {@link SwingWebPanel}. Besides the message it
 * tells which document wrote it (fixed when the message was written, so a message that arrives after a navigation still names the page that
 * wrote it), which script and line, and whether the document, one of its frames or a worker wrote it.
 * <p>
 * 페이지 스크립트의 콘솔 메시지 하나로, {@link ConsoleListener} 에 전달된다. 불변이며 문자열과 숫자만 가지므로(스크립트 객체 없음) 리스너가
 * 보관해도 된다. 이벤트의 <code>source</code> 는 {@link SwingWebPanel} 이다. 메시지 외에 어느 문서가 썼는지(메시지를 쓴 시점에 고정되므로
 * 이동 뒤에 도착한 메시지도 그것을 쓴 페이지를 가리킨다), 어느 스크립트의 몇 번째 줄인지, 문서·프레임·worker 중 누가 썼는지를 알려 준다.
 */
public final class ConsoleEvent extends EventObject {

    /**
     * The severity, with the meaning of the <code>console</code> method that wrote the message.
     * <p>
     * 심각도. 메시지를 쓴 <code>console</code> 메서드의 의미를 따른다.
     */
    public enum Level {
        /** <code>console.log</code> and the methods without a level of their own. / <code>console.log</code> 와 자체 수준이 없는 메서드. */
        LOG,
        /** <code>console.info</code>, and the dialog functions. / <code>console.info</code> 와 대화 상자 함수. */
        INFO,
        /** The method <code>console.warn</code>. / <code>console.warn</code> 메서드. */
        WARN,
        /** <code>console.error</code>, a failed <code>console.assert</code>, uncaught exceptions. / <code>console.error</code>, 실패한 <code>console.assert</code>, 잡히지 않은 예외. */
        ERROR,
        /** The method <code>console.debug</code>. / <code>console.debug</code> 메서드. */
        DEBUG
    }

    /**
     * What produced the message.
     * <p>
     * 메시지를 만든 것.
     */
    public enum Kind {
        /** A call of a <code>console</code> method. / <code>console</code> 메서드의 호출. */
        CONSOLE_CALL,
        /** A call of <code>alert</code>, <code>confirm</code> or <code>prompt</code> (no dialog is shown). / <code>alert</code>, <code>confirm</code>, <code>prompt</code> 의 호출(대화 상자는 뜨지 않는다). */
        DIALOG,
        /** An exception that script code threw and nobody caught (level {@link Level#ERROR}). / 스크립트가 던졌으나 아무도 잡지 않은 예외(수준 {@link Level#ERROR}). */
        UNCAUGHT_EXCEPTION,
        /** A rejected promise that still had no handler when the microtasks were done (level {@link Level#ERROR}). / 마이크로태스크가 끝날 때까지 처리기가 없던 거부된 promise(수준 {@link Level#ERROR}). */
        UNHANDLED_REJECTION,
        /** A script that the engine stopped: execution limit, time limit or cancellation (level {@link Level#ERROR}). / 엔진이 중단시킨 스크립트: 실행 한도, 시간 한도, 취소(수준 {@link Level#ERROR}). */
        SCRIPT_ABORTED,
        /** Another problem with the page's scripts, such as a script that could not be loaded (level {@link Level#ERROR}). / 가져오지 못한 스크립트 등 페이지 스크립트의 그 밖의 문제(수준 {@link Level#ERROR}). */
        HOST_ERROR,
        /** A notice that messages were dropped because the listeners were too slow; see {@link #getDroppedCount()} (level {@link Level#WARN}). / 리스너가 느려 메시지를 버렸다는 알림. {@link #getDroppedCount()} 참조(수준 {@link Level#WARN}). */
        MESSAGES_DROPPED
    }

    /**
     * Who wrote the message.
     * <p>
     * 메시지를 쓴 주체.
     */
    public enum Origin {
        /** The main document of the component. / 컴포넌트의 주 문서. */
        DOCUMENT,
        /** A document in a frame (<code>iframe</code>, <code>frame</code>). / 프레임(<code>iframe</code>, <code>frame</code>)의 문서. */
        FRAME,
        /** A dedicated worker. / 전용 worker. */
        WORKER,
        /** A shared worker. / 공유 worker. */
        SHARED_WORKER
    }

    /** Serial version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The number of the message among the messages of the component, starting at 1. / 컴포넌트의 메시지 중 몇 번째인지(1부터). */
    private final long sequence;

    /** The time the message was written (milliseconds since the epoch). / 메시지를 쓴 시각(epoch 이후 밀리초). */
    private final long timestamp;

    /** The severity. / 심각도. */
    private final Level level;

    /** What produced the message. / 메시지를 만든 것. */
    private final Kind kind;

    /** The console method or dialog function name, or the empty string. / 콘솔 메서드나 대화 상자 함수의 이름. 없으면 빈 문자열. */
    private final String method;

    /** The message text. / 메시지 문자열. */
    private final String message;

    /** The texts of the arguments (read-only). / 인수의 문자열(읽기 전용). */
    private final List<String> arguments;

    /** Who wrote the message. / 메시지를 쓴 주체. */
    private final Origin origin;

    /** The URL of the document that wrote the message (or that owns the worker). / 메시지를 쓴 문서(또는 worker 를 가진 문서)의 URL. */
    private final String documentUrl;

    /** The URL of the script, or the empty string. / 스크립트의 URL. 없으면 빈 문자열. */
    private final String scriptUrl;

    /** The 1-based line, or 0. / 1부터 시작하는 줄. 알 수 없으면 0. */
    private final int lineNumber;

    /** The 1-based column, or 0. / 1부터 시작하는 열. 알 수 없으면 0. */
    private final int columnNumber;

    /** The number of dropped messages for {@link Kind#MESSAGES_DROPPED}, otherwise 0. / {@link Kind#MESSAGES_DROPPED} 이면 버린 메시지 수, 아니면 0. */
    private final int droppedCount;

    /**
     * Creates an event. The component creates the events; a host does not need to.
     * <p>
     * 이벤트를 만든다. 이벤트는 컴포넌트가 만들며 호스트가 만들 필요는 없다.
     *
     * @param source       the component / 컴포넌트
     * @param sequence     the serial number / 일련번호
     * @param timestamp    the time in milliseconds / 밀리초 시각
     * @param level        the severity / 심각도
     * @param kind         what produced it / 만든 것
     * @param method       the method name or null / 메서드 이름 또는 null
     * @param message      the message text / 메시지 문자열
     * @param arguments    the argument texts or null / 인수 문자열 또는 null
     * @param origin       who wrote it / 쓴 주체
     * @param documentUrl  the document URL or null / 문서 URL 또는 null
     * @param scriptUrl    the script URL or null / 스크립트 URL 또는 null
     * @param lineNumber   the line or 0 / 줄 또는 0
     * @param columnNumber the column or 0 / 열 또는 0
     * @param droppedCount the dropped message count or 0 / 버린 메시지 수 또는 0
     */
    public ConsoleEvent(Object source, long sequence, long timestamp, Level level, Kind kind, String method, String message,
            List<String> arguments, Origin origin, String documentUrl, String scriptUrl, int lineNumber, int columnNumber,
            int droppedCount) {
        super(source);
        this.sequence = sequence;
        this.timestamp = timestamp;
        this.level = level == null ? Level.LOG : level;
        this.kind = kind == null ? Kind.CONSOLE_CALL : kind;
        this.method = method == null ? "" : method;
        this.message = message == null ? "" : message;
        this.arguments = Collections.unmodifiableList(arguments == null ? new ArrayList<String>() : new ArrayList<String>(arguments));
        this.origin = origin == null ? Origin.DOCUMENT : origin;
        this.documentUrl = documentUrl == null ? "" : documentUrl;
        this.scriptUrl = scriptUrl == null ? "" : scriptUrl;
        this.lineNumber = lineNumber < 0 ? 0 : lineNumber;
        this.columnNumber = columnNumber < 0 ? 0 : columnNumber;
        this.droppedCount = droppedCount < 0 ? 0 : droppedCount;
    }

    /**
     * Returns the number of the message among the messages of the component, starting at 1. Numbers only grow, so a listener can see the
     * order; a gap means messages were dropped (see {@link Kind#MESSAGES_DROPPED}).
     * <p>
     * 컴포넌트의 메시지 중 몇 번째인지(1부터)를 반환한다. 번호는 늘어나기만 하므로 순서를 알 수 있다. 번호가 건너뛰면 메시지가 버려진 것이다
     * ({@link Kind#MESSAGES_DROPPED} 참조).
     *
     * @return the serial number / 일련번호
     */
    public long getSequence() {
        return sequence;
    }

    /**
     * Returns the time the message was written, in milliseconds since the epoch.
     * <p>
     * 메시지를 쓴 시각을 epoch 이후 밀리초로 반환한다.
     *
     * @return the time / 시각
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the severity.
     * <p>
     * 심각도를 반환한다.
     *
     * @return the level / 수준
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns what produced the message.
     * <p>
     * 메시지를 만든 것을 반환한다.
     *
     * @return the kind / 종류
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the name of the <code>console</code> method (<code>log</code>, <code>error</code>, <code>table</code>) or dialog function
     * (<code>alert</code>) that wrote the message, or the empty string for the other kinds.
     * <p>
     * 메시지를 쓴 <code>console</code> 메서드(<code>log</code>, <code>error</code>, <code>table</code>)나 대화 상자 함수
     * (<code>alert</code>)의 이름을 반환한다. 다른 종류이면 빈 문자열.
     *
     * @return the method name / 메서드 이름
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the message text: the arguments after the format string (<code>%s %d %i %f %o %O %c %%</code>) was applied, joined by
     * spaces, indented by the open <code>console.group</code>s. Objects are shown in a compact readable form without running script code.
     * <p>
     * 메시지 문자열을 반환한다: 서식 문자열(<code>%s %d %i %f %o %O %c %%</code>)을 적용하고 공백으로 이은 인수를 열린
     * <code>console.group</code> 만큼 들여 쓴 것. 객체는 스크립트 코드를 실행하지 않고 간결하고 읽기 쉬운 형태로 나타낸다.
     *
     * @return the message / 메시지
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the arguments of the call, each as the console prints it (before the format string is applied). The list is read-only. A
     * message of the browser has the message as its only argument.
     * <p>
     * 호출의 인수를 서식 문자열을 적용하기 전의 모습으로, 각각 콘솔이 출력하는 형태로 반환한다. 목록은 읽기 전용이다. 브라우저가 보고하는
     * 메시지는 메시지 자체가 유일한 인수이다.
     *
     * @return the argument texts / 인수 문자열
     */
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * Returns who wrote the message: the main document, a frame, a dedicated worker or a shared worker.
     * <p>
     * 메시지를 쓴 주체를 반환한다: 주 문서, 프레임, 전용 worker, 공유 worker.
     *
     * @return the origin / 주체
     */
    public Origin getOrigin() {
        return origin;
    }

    /**
     * Returns the URL of the document that wrote the message, or of the document that owns the worker that wrote it. The value is fixed
     * when the message is written: after a navigation, a message of the old page still names the old page. For the main document it is
     * the URL the page was loaded from.
     * <p>
     * 메시지를 쓴 문서, 또는 메시지를 쓴 worker 를 가진 문서의 URL 을 반환한다. 값은 메시지를 쓴 시점에 고정되므로 이동 뒤에도 이전 페이지의
     * 메시지는 이전 페이지를 가리킨다. 주 문서이면 페이지를 불러온 URL 이다.
     *
     * @return the document URL / 문서 URL
     */
    public String getDocumentUrl() {
        return documentUrl;
    }

    /**
     * Returns the URL of the script that made the call, or the empty string when it is not known (an inline script has the URL of its
     * document, and <code>eval</code> code is called <code>eval</code>).
     * <p>
     * 호출한 스크립트의 URL 을 반환한다. 알 수 없으면 빈 문자열(인라인 스크립트는 문서의 URL, <code>eval</code> 코드는 <code>eval</code>).
     *
     * @return the script URL / 스크립트 URL
     */
    public String getScriptUrl() {
        return scriptUrl;
    }

    /**
     * Returns the 1-based line of the call in the script, or 0 when it is not known.
     * <p>
     * 스크립트 안의 호출의 1부터 시작하는 줄 번호를 반환한다. 알 수 없으면 0.
     *
     * @return the line / 줄
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the 1-based column of the call in the script, or 0 when it is not known.
     * <p>
     * 스크립트 안의 호출의 1부터 시작하는 열 번호를 반환한다. 알 수 없으면 0.
     *
     * @return the column / 열
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Returns the number of messages that were dropped, for {@link Kind#MESSAGES_DROPPED}; 0 for every other kind.
     * <p>
     * {@link Kind#MESSAGES_DROPPED} 이면 버려진 메시지 수를 반환한다. 다른 종류이면 0.
     *
     * @return the dropped count / 버린 수
     */
    public int getDroppedCount() {
        return droppedCount;
    }

    /**
     * Returns a short description for logs.
     * <p>
     * 로그용 짧은 설명을 반환한다.
     *
     * @return the description / 설명
     */
    public String toString() {
        return "ConsoleEvent[#" + sequence + " " + level + " " + kind + " " + origin + ": " + message + "]";
    }
}
