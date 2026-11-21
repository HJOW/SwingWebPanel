package org.duckdns.hjow.swingwebpanel.js.embed;

import org.duckdns.hjow.swingwebpanel.js.JsException;

/**
 * An error that script code threw (or that the engine raised while running script code) and that nobody caught inside the script. It is
 * what {@link JsEngine} throws to its Java callers for a syntax error, for <code>throw</code> and for a runtime error such as a
 * <code>TypeError</code>. It carries the name and the message of the JavaScript error, the position in the script where it happened (the
 * position of a syntax error, or of the expression that failed or of the <code>throw</code> statement), and the thrown value as a Java value.
 * An execution that the engine had to stop (step limit, time limit, cancellation) is <b>not</b> reported with this class but with
 * {@link org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException}, because a script cannot catch it.
 * <p>
 * 스크립트 코드가 던졌거나(또는 스크립트 코드를 실행하다 엔진이 일으켰고) 스크립트 안에서 아무도 잡지 않은 오류. {@link JsEngine} 이 구문 오류,
 * <code>throw</code>, <code>TypeError</code> 같은 런타임 오류를 Java 호출자에게 알릴 때 던진다. JavaScript 오류의 이름과 메시지, 오류가 난
 * 스크립트의 위치(구문 오류의 위치, 또는 실패한 표현식이나 <code>throw</code> 문의 위치), 던져진 값을 Java 값으로 담는다. 엔진이 멈춰야 했던
 * 실행(단계 수 제한, 시간 제한, 취소)은 스크립트가 잡을 수 없으므로 이 클래스가 아니라
 * {@link org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException} 으로 알린다.
 */
public final class JsScriptError extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the JavaScript error, for example <code>TypeError</code>.
     * <p>
     * JavaScript 오류의 이름. 예: <code>TypeError</code>.
     */
    private final String errorName;

    /**
     * The message of the JavaScript error without the name and the position.
     * <p>
     * 이름과 위치를 뺀 JavaScript 오류의 메시지.
     */
    private final String errorMessage;

    /**
     * The name of the script where the error happened, or the empty string.
     * <p>
     * 오류가 난 스크립트의 이름. 모르면 빈 문자열.
     */
    private final String sourceName;

    /**
     * The 1-based line where the error happened, or 0.
     * <p>
     * 오류가 난 곳의 1부터 시작하는 줄. 모르면 0.
     */
    private final int line;

    /**
     * The 1-based column where the error happened, or 0.
     * <p>
     * 오류가 난 곳의 1부터 시작하는 열. 모르면 0.
     */
    private final int column;

    /**
     * The thrown value converted to a Java value (see {@link JsEngine}).
     * <p>
     * 던져진 값을 Java 값으로 바꾼 것({@link JsEngine} 참조).
     */
    private final transient Object thrownValue;

    /**
     * The engine level exception that this error reports.
     * <p>
     * 이 오류가 알리는 엔진 수준의 예외.
     */
    private final transient JsException origin;

    /**
     * Creates the error.
     * <p>
     * 오류를 만든다.
     *
     * @param errorName    the JavaScript error name / JavaScript 오류 이름
     * @param errorMessage the JavaScript error message / JavaScript 오류 메시지
     * @param sourceName   the script name or null / 스크립트 이름 또는 null
     * @param line         the 1-based line or 0 / 1부터 시작하는 줄. 모르면 0
     * @param column       the 1-based column or 0 / 1부터 시작하는 열. 모르면 0
     * @param thrownValue  the thrown value as a Java value / 던져진 값(Java 값)
     * @param origin       the engine level exception / 엔진 수준의 예외
     */
    JsScriptError(String errorName, String errorMessage, String sourceName, int line, int column, Object thrownValue, JsException origin) {
        super(describe(errorName, errorMessage, sourceName, line, column), origin != null && origin.getHostCause() != null ? origin.getHostCause() : null);
        this.errorName = errorName;
        this.errorMessage = errorMessage;
        this.sourceName = sourceName == null ? "" : sourceName;
        this.line = line;
        this.column = column;
        this.thrownValue = thrownValue;
        this.origin = origin;
    }

    /**
     * Builds the text of {@link #getMessage()}: <code>TypeError: x is not a function (a.js:3:5)</code>.
     * <p>
     * {@link #getMessage()} 의 문자열을 만든다. 예: <code>TypeError: x is not a function (a.js:3:5)</code>.
     *
     * @param name    the error name / 오류 이름
     * @param message the error message / 오류 메시지
     * @param source  the script name or null / 스크립트 이름 또는 null
     * @param line    the line or 0 / 줄. 모르면 0
     * @param column  the column / 열
     * @return the text / 문자열
     */
    private static String describe(String name, String message, String source, int line, int column) {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (message != null && message.length() > 0) {
            sb.append(": ").append(message);
        }
        // 위치를 알면 괄호 안에 스크립트 이름과 줄, 열을 덧붙인다.
        if (line > 0) {
            sb.append(" (");
            if (source != null && source.length() > 0) {
                sb.append(source).append(':');
            }
            sb.append(line).append(':').append(column).append(')');
        }
        return sb.toString();
    }

    /**
     * Returns the name of the JavaScript error, for example <code>SyntaxError</code>, <code>TypeError</code> or <code>Error</code>. A thrown
     * value that is not an object (<code>throw 42</code>) is reported with the name <code>Error</code>.
     * <p>
     * JavaScript 오류의 이름을 반환한다. 예: <code>SyntaxError</code>, <code>TypeError</code>, <code>Error</code>. 객체가 아닌 값을 던지면
     * (<code>throw 42</code>) 이름은 <code>Error</code> 이다.
     *
     * @return the error name / 오류 이름
     */
    public String getErrorName() {
        return errorName;
    }

    /**
     * Returns the message of the JavaScript error without the name and the position.
     * <p>
     * 이름과 위치를 뺀 JavaScript 오류의 메시지를 반환한다.
     *
     * @return the message, possibly empty / 메시지. 비어 있을 수 있다
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the name of the script where the error happened (the name that was given to the engine, for example a file name), or the empty
     * string if it is not known.
     * <p>
     * 오류가 난 스크립트의 이름(엔진에 준 이름. 예: 파일 이름)을 반환한다. 모르면 빈 문자열.
     *
     * @return the script name / 스크립트 이름
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Returns the 1-based line where the error happened, or 0 if it is not known. For a syntax error it is the line of the offending token;
     * for a runtime error it is the line of the innermost expression that was being evaluated; for a <code>throw</code> statement it is the line of
     * the statement.
     * <p>
     * 오류가 난 곳의 1부터 시작하는 줄을 반환한다. 모르면 0. 구문 오류는 잘못된 토큰의 줄, 런타임 오류는 평가 중이던 가장 안쪽 표현식의 줄,
     * <code>throw</code> 문은 그 문장의 줄이다.
     *
     * @return the line / 줄
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the 1-based column where the error happened, or 0 if it is not known (see {@link #getLine()}).
     * <p>
     * 오류가 난 곳의 1부터 시작하는 열을 반환한다. 모르면 0({@link #getLine()} 참조).
     *
     * @return the column / 열
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns the value that the script threw, converted to a Java value by the rules of {@link JsEngine} (an error object becomes a
     * {@link JsObjectHandle}, <code>throw 42</code> gives the <code>Double</code> 42.0). For an error that the engine raised itself the
     * error object is created when this method is called.
     * <p>
     * 스크립트가 던진 값을 {@link JsEngine} 의 규칙대로 Java 값으로 바꾸어 반환한다(오류 객체는 {@link JsObjectHandle}, <code>throw 42</code> 는
     * <code>Double</code> 42.0). 엔진이 일으킨 오류는 이 메서드를 부를 때 오류 객체를 만든다.
     *
     * @return the thrown value / 던져진 값
     */
    public Object getThrownValue() {
        return thrownValue;
    }

    /**
     * Returns the engine level exception that this error reports (an advanced hook for code that works with the classes of the engine itself).
     * <p>
     * 이 오류가 알리는 엔진 수준의 예외를 반환한다(엔진 클래스를 직접 다루는 코드를 위한 고급 연결 지점).
     *
     * @return the exception / 예외
     */
    public JsException getJsException() {
        return origin;
    }

    /**
     * Disables stack trace capture: the Java stack says nothing about the script, and the position of the error is in this object.
     * <p>
     * 스택 추적 수집을 끈다. Java 스택은 스크립트에 대해 아무것도 알려 주지 않으며 오류 위치는 이 객체가 가지고 있다.
     *
     * @return this error / 이 오류
     */
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
