package org.duckdns.hjow.swingwebpanel.js;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One message of the script's <code>console</code> object, or one diagnostic that the host reports on behalf of the script (an uncaught
 * exception, an unhandled promise rejection, a dialog function). It is immutable and holds only Java strings and numbers, never a
 * reference to a script object, so a host may keep it and hand it to another thread. A host that wants the structured form implements
 * {@link ConsoleRecordHook}; every other {@link ConsoleHook} receives {@link #getText()} and {@link #getLegacyLevel()} as before.
 * <p>
 * 스크립트의 <code>console</code> 객체의 메시지 하나, 또는 호스트가 스크립트를 대신해 보고하는 진단 하나(잡히지 않은 예외, 처리되지 않은
 * promise 거부, 대화 상자 함수). 불변이며 Java 문자열과 숫자만 가지고 스크립트 객체를 참조하지 않으므로 호스트가 보관하거나 다른 스레드에
 * 넘겨도 된다. 구조화된 형태가 필요한 호스트는 {@link ConsoleRecordHook} 을 구현하고, 다른 {@link ConsoleHook} 은 지금까지처럼
 * {@link #getText()} 와 {@link #getLegacyLevel()} 을 받는다.
 */
public final class ConsoleRecord {

    /**
     * What produced a record.
     * <p>
     * 기록을 만든 것.
     */
    public enum Kind {
        /**
         * A call of a <code>console</code> method (or the test-only <code>print</code> function).
         * <p>
         * <code>console</code> 메서드(또는 시험용 <code>print</code> 함수)의 호출.
         */
        CALL,
        /**
         * A call of <code>alert</code>, <code>confirm</code> or <code>prompt</code>.
         * <p>
         * <code>alert</code>, <code>confirm</code>, <code>prompt</code> 의 호출.
         */
        DIALOG,
        /**
         * An exception that script code threw and nobody caught.
         * <p>
         * 스크립트 코드가 던졌으나 아무도 잡지 않은 예외.
         */
        UNCAUGHT_EXCEPTION,
        /**
         * A promise that was rejected and still had no rejection handler when the microtasks were done.
         * <p>
         * 거부되었고 마이크로태스크가 끝날 때까지 거부 처리기가 없던 promise.
         */
        UNHANDLED_REJECTION,
        /**
         * A script that the engine stopped (execution limit, time limit, cancellation).
         * <p>
         * 엔진이 중단시킨 스크립트(실행 한도, 시간 한도, 취소).
         */
        SCRIPT_ABORTED,
        /**
         * Another problem of the host with the page's scripts (a script that could not be loaded, a task that could not be scheduled).
         * <p>
         * 호스트가 페이지의 스크립트에서 겪은 그 밖의 문제(가져오지 못한 스크립트, 예약하지 못한 작업).
         */
        HOST_ERROR
    }

    /**
     * The kind of the record.
     * <p>
     * 기록의 종류.
     */
    private final Kind kind;

    /**
     * The name of the console method or dialog function (<code>log</code>, <code>table</code>, <code>alert</code>), or the empty string.
     * <p>
     * 콘솔 메서드나 대화 상자 함수의 이름(<code>log</code>, <code>table</code>, <code>alert</code>). 없으면 빈 문자열.
     */
    private final String method;

    /**
     * The level: one of <code>log</code>, <code>info</code>, <code>warn</code>, <code>error</code>, <code>debug</code>.
     * <p>
     * 수준: <code>log</code>, <code>info</code>, <code>warn</code>, <code>error</code>, <code>debug</code> 중 하나.
     */
    private final String level;

    /**
     * The message text: the arguments after the Formatter operation of the Console Standard, indented by the open groups.
     * <p>
     * 메시지 문자열: Console Standard 의 Formatter 연산을 거친 인수를 열린 그룹만큼 들여 쓴 것.
     */
    private final String text;

    /**
     * The arguments of the call, each as the text that the console prints for it (read-only).
     * <p>
     * 호출의 인수. 각각 콘솔이 그 인수를 출력하는 문자열이다(읽기 전용).
     */
    private final List<String> arguments;

    /**
     * The name (URL) of the script that made the call, or the empty string when it is not known.
     * <p>
     * 호출한 스크립트의 이름(URL). 알 수 없으면 빈 문자열.
     */
    private final String sourceName;

    /**
     * The 1-based line of the call, or 0 when it is not known.
     * <p>
     * 호출의 1부터 시작하는 줄 번호. 알 수 없으면 0.
     */
    private final int line;

    /**
     * The 1-based column of the call, or 0 when it is not known.
     * <p>
     * 호출의 1부터 시작하는 열 번호. 알 수 없으면 0.
     */
    private final int column;

    /**
     * The number of open <code>console.group</code> levels when the record was made.
     * <p>
     * 기록을 만들 때 열려 있던 <code>console.group</code> 단계 수.
     */
    private final int groupDepth;

    /**
     * Creates a record. Use the factory methods.
     * <p>
     * 기록을 만든다. 팩터리 메서드를 사용한다.
     *
     * @param kind       the kind / 종류
     * @param method     the method name or "" / 메서드 이름 또는 ""
     * @param level      the level / 수준
     * @param text       the text / 문자열
     * @param arguments  the argument texts / 인수 문자열
     * @param sourceName the script name or "" / 스크립트 이름 또는 ""
     * @param line       the line or 0 / 줄 또는 0
     * @param column     the column or 0 / 열 또는 0
     * @param groupDepth the open group levels / 열린 그룹 단계 수
     */
    private ConsoleRecord(Kind kind, String method, String level, String text, List<String> arguments, String sourceName, int line,
            int column, int groupDepth) {
        this.kind = kind;
        this.method = method == null ? "" : method;
        this.level = level == null ? "log" : level;
        this.text = text == null ? "" : text;
        this.arguments = Collections.unmodifiableList(new ArrayList<String>(arguments));
        this.sourceName = sourceName == null ? "" : sourceName;
        this.line = line < 0 ? 0 : line;
        this.column = column < 0 ? 0 : column;
        this.groupDepth = groupDepth < 0 ? 0 : groupDepth;
    }

    /**
     * Creates the record of a console method call.
     * <p>
     * 콘솔 메서드 호출의 기록을 만든다.
     *
     * @param method     the method name, for example <code>log</code> / 메서드 이름. 예: <code>log</code>
     * @param level      the level / 수준
     * @param text       the formatted, indented text / 서식과 들여쓰기를 마친 문자열
     * @param arguments  the texts of the arguments / 인수의 문자열
     * @param sourceName the script name or null / 스크립트 이름 또는 null
     * @param line       the line or 0 / 줄 또는 0
     * @param column     the column or 0 / 열 또는 0
     * @param groupDepth the open group levels / 열린 그룹 단계 수
     * @return the record / 기록
     */
    public static ConsoleRecord call(String method, String level, String text, List<String> arguments, String sourceName, int line,
            int column, int groupDepth) {
        return new ConsoleRecord(Kind.CALL, method, level, text, arguments, sourceName, line, column, groupDepth);
    }

    /**
     * Creates the record of a dialog function call; its level is <code>info</code>.
     * <p>
     * 대화 상자 함수 호출의 기록을 만든다. 수준은 <code>info</code> 이다.
     *
     * @param method     <code>alert</code>, <code>confirm</code> or <code>prompt</code> / <code>alert</code>, <code>confirm</code>, <code>prompt</code>
     * @param message    the message the script passed / 스크립트가 넘긴 메시지
     * @param sourceName the script name or null / 스크립트 이름 또는 null
     * @param line       the line or 0 / 줄 또는 0
     * @param column     the column or 0 / 열 또는 0
     * @return the record / 기록
     */
    public static ConsoleRecord dialog(String method, String message, String sourceName, int line, int column) {
        List<String> a = new ArrayList<String>(1);
        a.add(message == null ? "" : message);
        return new ConsoleRecord(Kind.DIALOG, method, "info", message, a, sourceName, line, column, 0);
    }

    /**
     * Creates the record of a problem that the host reports (not a console call): level <code>error</code>.
     * <p>
     * 호스트가 보고하는 문제(콘솔 호출이 아님)의 기록을 만든다. 수준은 <code>error</code> 이다.
     *
     * @param kind       the kind, not <code>CALL</code> or <code>DIALOG</code> / 종류(<code>CALL</code>, <code>DIALOG</code> 가 아님)
     * @param text       the message / 메시지
     * @param sourceName the script name or null / 스크립트 이름 또는 null
     * @param line       the line or 0 / 줄 또는 0
     * @param column     the column or 0 / 열 또는 0
     * @return the record / 기록
     * @throws IllegalArgumentException if the kind is null, <code>CALL</code> or <code>DIALOG</code> / 종류가 null, <code>CALL</code>, <code>DIALOG</code> 일 때
     */
    public static ConsoleRecord report(Kind kind, String text, String sourceName, int line, int column) {
        if (kind == null || kind == Kind.CALL || kind == Kind.DIALOG) {
            throw new IllegalArgumentException("kind must be a report kind");
        }
        List<String> a = new ArrayList<String>(1);
        a.add(text == null ? "" : text);
        return new ConsoleRecord(kind, "", "error", text, a, sourceName, line, column, 0);
    }

    /**
     * Sends a record to a hook: through {@link ConsoleRecordHook#record(ConsoleRecord)} when the hook understands records, otherwise
     * through {@link ConsoleHook#message(String, String)} with {@link #getLegacyLevel()} and {@link #getText()}.
     * <p>
     * 기록을 연결에 보낸다: 연결이 기록을 이해하면 {@link ConsoleRecordHook#record(ConsoleRecord)} 로, 아니면
     * {@link #getLegacyLevel()} 와 {@link #getText()} 로 {@link ConsoleHook#message(String, String)} 를 호출한다.
     *
     * @param hook   the hook, not null / 연결(null 불가)
     * @param record the record, not null / 기록(null 불가)
     */
    public static void deliver(ConsoleHook hook, ConsoleRecord record) {
        // 구조화된 기록을 받을 수 있는 연결에는 기록 전체를, 아니면 예전 방식의 수준과 문자열만 보낸다.
        if (hook instanceof ConsoleRecordHook) {
            ((ConsoleRecordHook) hook).record(record);
        } else {
            hook.message(record.getLegacyLevel(), record.getText());
        }
    }

    /**
     * Returns the kind.
     * <p>
     * 종류를 반환한다.
     *
     * @return the kind / 종류
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the name of the console method or dialog function, or the empty string for a report of the host.
     * <p>
     * 콘솔 메서드나 대화 상자 함수의 이름을 반환한다. 호스트의 보고이면 빈 문자열.
     *
     * @return the name / 이름
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the level: <code>log</code>, <code>info</code>, <code>warn</code>, <code>error</code> or <code>debug</code>.
     * <p>
     * 수준을 반환한다: <code>log</code>, <code>info</code>, <code>warn</code>, <code>error</code>, <code>debug</code>.
     *
     * @return the level / 수준
     */
    public String getLevel() {
        return level;
    }

    /**
     * Returns the level that the plain {@link ConsoleHook} always got: the level, except for the dialog functions whose name
     * (<code>alert</code>, <code>confirm</code>, <code>prompt</code>) was passed in its place.
     * <p>
     * 일반 {@link ConsoleHook} 이 지금까지 받은 수준을 반환한다: 수준 그대로이며, 대화 상자 함수는 그 자리에 함수 이름
     * (<code>alert</code>, <code>confirm</code>, <code>prompt</code>)을 넘겼다.
     *
     * @return the level for plain hooks / 일반 연결용 수준
     */
    public String getLegacyLevel() {
        return kind == Kind.DIALOG ? method : level;
    }

    /**
     * Returns the message text (after the Formatter operation, indented by the open groups).
     * <p>
     * 메시지 문자열을 반환한다(Formatter 연산을 거치고 열린 그룹만큼 들여 쓴 것).
     *
     * @return the text / 문자열
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the texts of the arguments of the call, each as the console prints it. The list is read-only.
     * <p>
     * 호출 인수의 문자열을 반환한다. 각각 콘솔이 출력하는 형태이다. 목록은 읽기 전용이다.
     *
     * @return the argument texts / 인수 문자열
     */
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * Returns the name (URL) of the script that made the call, or the empty string when it is not known.
     * <p>
     * 호출한 스크립트의 이름(URL)을 반환한다. 알 수 없으면 빈 문자열.
     *
     * @return the script name / 스크립트 이름
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Returns the 1-based line of the call, or 0 when it is not known.
     * <p>
     * 호출의 1부터 시작하는 줄 번호를 반환한다. 알 수 없으면 0.
     *
     * @return the line / 줄
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the 1-based column of the call, or 0 when it is not known.
     * <p>
     * 호출의 1부터 시작하는 열 번호를 반환한다. 알 수 없으면 0.
     *
     * @return the column / 열
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns the number of <code>console.group</code> levels that were open when the record was made.
     * <p>
     * 기록을 만들 때 열려 있던 <code>console.group</code> 단계 수를 반환한다.
     *
     * @return the group depth / 그룹 단계 수
     */
    public int getGroupDepth() {
        return groupDepth;
    }

    /**
     * Returns a short description for logs.
     * <p>
     * 로그용 짧은 설명을 반환한다.
     *
     * @return the description / 설명
     */
    public String toString() {
        return "ConsoleRecord[" + kind + " " + level + (method.length() > 0 ? " " + method : "") + ": " + text + "]";
    }
}
