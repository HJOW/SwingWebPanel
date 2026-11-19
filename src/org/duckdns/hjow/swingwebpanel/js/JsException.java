package org.duckdns.hjow.swingwebpanel.js;

/**
 * A JavaScript exception travelling through Java code. It carries either an arbitrary thrown JavaScript value
 * (<code>throw 42</code>) or a lazily created native error (kind + message) so that Java built-ins can throw
 * <code>JsException.typeError("...")</code> without needing a realm; the error object is created when the exception is
 * caught by JavaScript code via {@link #getValue(JsRealm)}.
 * Java stack traces are disabled for speed; position information of syntax errors is kept in {@link #getLine()},
 * {@link #getColumn()} and {@link #getSourceName()}.
 * <p>
 * Java 코드를 통과하는 JavaScript 예외. 임의로 던진 JavaScript 값(<code>throw 42</code>)이나, 늦게 생성하는 기본 오류(종류 + 메시지)를
 * 담는다. 덕분에 Java 내장 함수는 realm 없이 <code>JsException.typeError("...")</code> 를 던질 수 있고, JavaScript 코드가
 * 예외를 잡을 때 {@link #getValue(JsRealm)} 로 오류 객체가 만들어진다.
 * 속도를 위해 Java 스택 추적은 끄며, 구문 오류의 위치 정보는 {@link #getLine()}, {@link #getColumn()}, {@link #getSourceName()} 에 둔다.
 */
public class JsException extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The thrown value or the lazily created error object (null until created).
     * <p>
     * 던져진 값 또는 늦게 만든 오류 객체(만들기 전에는 null).
     */
    private transient JsValue value;

    /**
     * Error kind for lazily created errors (null when a value was thrown).
     * <p>
     * 늦게 만드는 오류의 종류(값을 던진 경우 null).
     */
    private final ErrorKind kind;

    /**
     * Message for lazily created errors.
     * <p>
     * 늦게 만드는 오류의 메시지.
     */
    private final String errorMessage;

    /**
     * 1-based line of a syntax error, or 0 if unknown.
     * <p>
     * 구문 오류의 1부터 시작하는 줄 번호. 모르면 0.
     */
    private int line;

    /**
     * 1-based column of a syntax error, or 0 if unknown.
     * <p>
     * 구문 오류의 1부터 시작하는 열 번호. 모르면 0.
     */
    private int column;

    /**
     * Name of the source (file name or URL) of a syntax error.
     * <p>
     * 구문 오류가 난 소스의 이름(파일 이름이나 URL).
     */
    private String sourceName;

    /**
     * Wraps a thrown JavaScript value.
     * <p>
     * 던져진 JavaScript 값을 감싼다.
     *
     * @param thrown the thrown value / 던져진 값
     */
    public JsException(JsValue thrown) {
        super(null, null);
        this.value = thrown;
        this.kind = null;
        this.errorMessage = null;
    }

    /**
     * Creates a lazily materialized native error.
     * <p>
     * 늦게 객체를 만드는 기본 오류를 만든다.
     *
     * @param kind    error kind / 오류 종류
     * @param message error message / 오류 메시지
     */
    public JsException(ErrorKind kind, String message) {
        super(null, null);
        this.kind = kind;
        this.errorMessage = message;
    }

    /**
     * Creates a TypeError.
     * <p>
     * TypeError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception to throw / 던질 예외
     */
    public static JsException typeError(String message) {
        return new JsException(ErrorKind.TYPE_ERROR, message);
    }

    /**
     * Creates a RangeError.
     * <p>
     * RangeError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception to throw / 던질 예외
     */
    public static JsException rangeError(String message) {
        return new JsException(ErrorKind.RANGE_ERROR, message);
    }

    /**
     * Creates a ReferenceError.
     * <p>
     * ReferenceError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception to throw / 던질 예외
     */
    public static JsException referenceError(String message) {
        return new JsException(ErrorKind.REFERENCE_ERROR, message);
    }

    /**
     * Creates a SyntaxError without position.
     * <p>
     * 위치 정보가 없는 SyntaxError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception to throw / 던질 예외
     */
    public static JsException syntaxError(String message) {
        return new JsException(ErrorKind.SYNTAX_ERROR, message);
    }

    /**
     * Creates a SyntaxError with source position.
     * <p>
     * 소스 위치가 있는 SyntaxError 를 만든다.
     *
     * @param message    message / 메시지
     * @param sourceName name of the source / 소스 이름
     * @param line       1-based line / 1부터 시작하는 줄
     * @param column     1-based column / 1부터 시작하는 열
     * @return the exception to throw / 던질 예외
     */
    public static JsException syntaxError(String message, String sourceName, int line, int column) {
        JsException e = new JsException(ErrorKind.SYNTAX_ERROR, message);
        e.sourceName = sourceName;
        e.line = line;
        e.column = column;
        return e;
    }

    /**
     * Creates a URIError.
     * <p>
     * URIError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception to throw / 던질 예외
     */
    public static JsException uriError(String message) {
        return new JsException(ErrorKind.URI_ERROR, message);
    }

    /**
     * Returns the JavaScript value of this exception, creating the error object in the given realm if needed.
     * <p>
     * 이 예외의 JavaScript 값을 반환한다. 필요하면 주어진 realm 에서 오류 객체를 만든다.
     *
     * @param realm realm used to create native errors / 기본 오류를 만들 realm
     * @return the thrown value / 던져진 값
     */
    public JsValue getValue(JsRealm realm) {
        if (value == null) {
            value = realm.createError(kind, errorMessage);
        }
        return value;
    }

    /**
     * Returns true if this exception carries a thrown value or an already created error object.
     * <p>
     * 던져진 값이나 이미 만든 오류 객체를 갖고 있으면 true.
     *
     * @return true if a value is present / 값이 있으면 true
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Returns the error kind of a native error, or null if an arbitrary value was thrown.
     * <p>
     * 기본 오류의 종류를 반환한다. 임의 값을 던졌으면 null.
     *
     * @return kind or null / 종류 또는 null
     */
    public ErrorKind getKind() {
        return kind;
    }

    /**
     * Returns the 1-based line of a syntax error, or 0.
     * <p>
     * 구문 오류의 줄 번호(1부터)를 반환한다. 모르면 0.
     *
     * @return line / 줄
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the 1-based column of a syntax error, or 0.
     * <p>
     * 구문 오류의 열 번호(1부터)를 반환한다. 모르면 0.
     *
     * @return column / 열
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns the source name of a syntax error, or null.
     * <p>
     * 구문 오류의 소스 이름을 반환한다. 없으면 null.
     *
     * @return source name or null / 소스 이름 또는 null
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Returns the plain message of a native error (without the kind name), or null for thrown values.
     * <p>
     * 기본 오류의 순수 메시지(종류 이름 제외)를 반환한다. 던진 값이면 null.
     *
     * @return message or null / 메시지 또는 null
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Describes the exception for Java logs, for example "TypeError: x is not a function (a.js:3:5)".
     * Never runs JavaScript code.
     * <p>
     * Java 로그용 설명을 반환한다. 예: "TypeError: x is not a function (a.js:3:5)". JavaScript 코드는 실행하지 않는다.
     *
     * @return description / 설명
     */
    public String getMessage() {
        if (kind != null && !(value instanceof JsObject)) {
            String pos = line > 0 ? " (" + (sourceName == null ? "" : sourceName + ":") + line + ":" + column + ")" : "";
            return kind.getErrorName() + ": " + errorMessage + pos;
        }
        return describe(value);
    }

    /**
     * Disables Java stack trace capture for speed.
     * <p>
     * 속도를 위해 Java 스택 추적 수집을 끈다.
     *
     * @return this exception / 이 예외
     */
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    /**
     * Describes a value without invoking user code (reads only data properties).
     * <p>
     * 사용자 코드를 실행하지 않고 값을 설명한다(데이터 프로퍼티만 읽는다).
     *
     * @param v value / 값
     * @return description / 설명
     */
    public static String describe(JsValue v) {
        if (v == null) {
            return "(no value)";
        }
        if (v instanceof JsObject) {
            JsObject o = (JsObject) v;
            String name = rawString(o, "name");
            String msg = rawString(o, "message");
            if (name != null || msg != null) {
                return (name == null ? "Error" : name) + (msg == null || msg.length() == 0 ? "" : ": " + msg);
            }
            return "[object " + o.className() + "]";
        }
        if (v instanceof JsSymbol) {
            return v.toString();
        }
        return v.toString();
    }

    /**
     * Reads a string data property along the prototype chain without calling getters.
     * <p>
     * getter 를 호출하지 않고 프로토타입 체인에서 문자열 데이터 프로퍼티를 읽는다.
     *
     * @param o   object / 객체
     * @param key property name / 프로퍼티 이름
     * @return the string or null / 문자열 또는 null
     */
    private static String rawString(JsObject o, String key) {
        JsObject cur = o;
        // 프로토타입 체인을 따라가며 데이터 프로퍼티를 찾는다.
        for (int depth = 0; cur != null && depth < 64; depth++) {
            Property p = cur.getOwnPropertySlot(key);
            if (p != null) {
                if (!p.accessor && p.value instanceof JsString) {
                    return ((JsString) p.value).getValue();
                }
                return null;
            }
            cur = cur.getPrototypeOf();
        }
        return null;
    }
}
