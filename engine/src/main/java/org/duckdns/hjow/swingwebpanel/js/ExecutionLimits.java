package org.duckdns.hjow.swingwebpanel.js;

/**
 * Execution limits and cancellation of one realm (plan item P13-12). The interpreter calls {@link #tick()} for every executed
 * statement, loop iteration and function call, and built-ins that run long loops should call {@link #tick()} too.
 * <ul>
 * <li><b>Steps</b>: after {@link #getMaxSteps()} ticks in one task a {@link ScriptAbortedException} (STEP_LIMIT) is thrown. Default 200 million.</li>
 * <li><b>Time</b>: after {@link #getMaxTimeMillis()} milliseconds of one task a {@link ScriptAbortedException} (TIME_LIMIT) is thrown. Default 0 = unlimited
 * (the host normally sets a value).</li>
 * <li><b>Cancellation</b>: {@link #cancel()} may be called from any thread; the running script stops at the next tick with
 * {@link ScriptAbortedException} (CANCELLED). {@link #reset()} clears the flag.</li>
 * <li><b>Call depth</b>: more than {@link #getMaxCallDepth()} nested script calls throw a script-visible RangeError
 * "Maximum call stack size exceeded". Default 400. Each script call uses several Java stack frames, so run the engine on a
 * thread with a large stack (for example 16 MB) when raising this value.</li>
 * <li><b>String length</b>: strings longer than {@link #getMaxStringLength()} code units cannot be created by concatenation or
 * repetition (RangeError "Invalid string length"). Default 33,554,432.</li>
 * <li><b>Array length</b>: arrays with a dense storage are limited by memory; the JavaScript maximum 2^32 - 1 applies to the
 * length property. A script cannot allocate more than 50 million dense elements (larger indices switch to sparse mode).</li>
 * </ul>
 * Time and steps are measured per <em>task</em>: the host (or the realm's {@link JsRealm#evaluateScript(String, String)}) calls
 * {@link #beginTask()} when the outermost script call starts.
 * <p>
 * realm 하나의 실행 제한과 취소(계획 P13-12). 인터프리터는 실행하는 문장, 반복 한 번, 함수 호출마다 {@link #tick()} 을 호출하며,
 * 긴 반복을 도는 내장 함수도 {@link #tick()} 을 호출해야 한다.
 * <ul>
 * <li><b>단계 수</b>: 한 작업에서 {@link #getMaxSteps()} 번 넘으면 {@link ScriptAbortedException}(STEP_LIMIT). 기본 2억.</li>
 * <li><b>시간</b>: 한 작업이 {@link #getMaxTimeMillis()} 밀리초를 넘으면 {@link ScriptAbortedException}(TIME_LIMIT). 기본 0 = 무제한(호스트가 보통 지정).</li>
 * <li><b>취소</b>: {@link #cancel()} 은 어느 스레드에서나 호출할 수 있고, 실행 중인 스크립트는 다음 tick 에서 {@link ScriptAbortedException}(CANCELLED)으로 멈춘다.
 * {@link #reset()} 이 표시를 지운다.</li>
 * <li><b>호출 깊이</b>: 중첩된 스크립트 호출이 {@link #getMaxCallDepth()} 를 넘으면 스크립트가 볼 수 있는 RangeError "Maximum call stack size exceeded".
 * 기본 400. 스크립트 호출마다 Java 스택 프레임이 여러 개 쓰이므로 값을 올릴 때는 큰 스택(예: 16MB)의 스레드에서 엔진을 실행한다.</li>
 * <li><b>문자열 길이</b>: {@link #getMaxStringLength()} 코드 단위를 넘는 문자열은 연결이나 반복으로 만들 수 없다(RangeError "Invalid string length"). 기본 33,554,432.</li>
 * <li><b>배열 길이</b>: 조밀 저장 배열은 메모리로 제한된다. length 프로퍼티는 JavaScript 최댓값 2^32 - 1 이다. 조밀 요소는 5천만 개를 넘지 못하며
 * 더 큰 인덱스는 희소 모드로 바뀐다.</li>
 * </ul>
 * 시간과 단계 수는 <em>작업</em> 단위로 측정한다: 호스트(또는 realm 의 {@link JsRealm#evaluateScript(String, String)})가 가장 바깥 스크립트 호출이
 * 시작될 때 {@link #beginTask()} 를 호출한다.
 */
public final class ExecutionLimits {

    /**
     * Default step limit per task.
     * <p>
     * 작업당 기본 단계 수 제한.
     */
    public static final long DEFAULT_MAX_STEPS = 200000000L;

    /**
     * Default maximum nesting of script calls.
     * <p>
     * 스크립트 호출 중첩의 기본 최댓값.
     */
    public static final int DEFAULT_MAX_CALL_DEPTH = 400;

    /**
     * Default maximum string length in UTF-16 code units.
     * <p>
     * UTF-16 코드 단위 기준 기본 최대 문자열 길이.
     */
    public static final int DEFAULT_MAX_STRING_LENGTH = 33554432;

    /**
     * How many ticks pass between checks of the clock and the cancellation flag (must be a power of two).
     * <p>
     * 시계와 취소 표시를 확인하는 tick 간격(2의 거듭제곱이어야 한다).
     */
    private static final int CHECK_INTERVAL = 1024;

    /**
     * The step limit.
     * <p>
     * 단계 수 제한.
     */
    private volatile long maxSteps = DEFAULT_MAX_STEPS;

    /**
     * The time limit in milliseconds (0 = unlimited).
     * <p>
     * 시간 제한(밀리초, 0 이면 무제한).
     */
    private volatile long maxTimeMillis;

    /**
     * The call depth limit.
     * <p>
     * 호출 깊이 제한.
     */
    private volatile int maxCallDepth = DEFAULT_MAX_CALL_DEPTH;

    /**
     * The string length limit.
     * <p>
     * 문자열 길이 제한.
     */
    private volatile int maxStringLength = DEFAULT_MAX_STRING_LENGTH;

    /**
     * The cancellation flag.
     * <p>
     * 취소 표시.
     */
    private volatile boolean cancelled;

    /**
     * Steps executed in the current task.
     * <p>
     * 현재 작업에서 실행한 단계 수.
     */
    private long steps;

    /**
     * Start time of the current task.
     * <p>
     * 현재 작업의 시작 시각.
     */
    private long taskStart = System.currentTimeMillis();

    /**
     * Nesting of {@link #beginTask()} calls so that only the outermost call resets the counters.
     * <p>
     * {@link #beginTask()} 중첩 횟수. 가장 바깥 호출만 카운터를 초기화한다.
     */
    private int taskDepth;

    /**
     * Creates limits with the default values.
     * <p>
     * 기본값으로 제한을 만든다.
     */
    public ExecutionLimits() {
    }

    /**
     * Returns the step limit.
     * <p>
     * 단계 수 제한을 반환한다.
     *
     * @return the limit / 제한
     */
    public long getMaxSteps() {
        return maxSteps;
    }

    /**
     * Sets the step limit per task.
     * <p>
     * 작업당 단계 수 제한을 지정한다.
     *
     * @param maxSteps the limit (0 or less = unlimited) / 제한(0 이하이면 무제한)
     */
    public void setMaxSteps(long maxSteps) {
        this.maxSteps = maxSteps;
    }

    /**
     * Returns the time limit.
     * <p>
     * 시간 제한을 반환한다.
     *
     * @return milliseconds, 0 for unlimited / 밀리초. 0 이면 무제한
     */
    public long getMaxTimeMillis() {
        return maxTimeMillis;
    }

    /**
     * Sets the time limit per task.
     * <p>
     * 작업당 시간 제한을 지정한다.
     *
     * @param millis milliseconds, 0 for unlimited / 밀리초. 0 이면 무제한
     * @throws IllegalArgumentException if millis is negative / millis 가 음수이면
     */
    public void setMaxTimeMillis(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("millis must not be negative: " + millis);
        }
        this.maxTimeMillis = millis;
    }

    /**
     * Returns the call depth limit.
     * <p>
     * 호출 깊이 제한을 반환한다.
     *
     * @return the limit / 제한
     */
    public int getMaxCallDepth() {
        return maxCallDepth;
    }

    /**
     * Sets the call depth limit.
     * <p>
     * 호출 깊이 제한을 지정한다.
     *
     * @param depth the limit, at least 1 / 제한. 1 이상
     * @throws IllegalArgumentException if depth is less than 1 / depth 가 1 보다 작으면
     */
    public void setMaxCallDepth(int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be at least 1: " + depth);
        }
        this.maxCallDepth = depth;
    }

    /**
     * Returns the string length limit.
     * <p>
     * 문자열 길이 제한을 반환한다.
     *
     * @return code units / 코드 단위 수
     */
    public int getMaxStringLength() {
        return maxStringLength;
    }

    /**
     * Sets the string length limit.
     * <p>
     * 문자열 길이 제한을 지정한다.
     *
     * @param length code units, at least 1 / 코드 단위 수. 1 이상
     * @throws IllegalArgumentException if length is less than 1 / length 가 1 보다 작으면
     */
    public void setMaxStringLength(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be at least 1: " + length);
        }
        this.maxStringLength = length;
    }

    /**
     * Requests the running script to stop; callable from any thread.
     * <p>
     * 실행 중인 스크립트에 중단을 요청한다. 어느 스레드에서나 호출할 수 있다.
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     * Clears the cancellation flag.
     * <p>
     * 취소 표시를 지운다.
     */
    public void reset() {
        cancelled = false;
    }

    /**
     * Non-throwing query for long running native code (regular expression matching, built-in loops): returns true when the
     * running task should stop because cancellation was requested or the time limit of the task is exceeded. Step counting is not
     * part of this query. Safe to call from the thread that runs the script at any frequency.
     * <p>
     * 오래 걸리는 네이티브 코드(정규식 일치, 내장 객체의 반복)용 비예외 조회: 취소가 요청되었거나 작업의 시간 제한을 넘어 실행 중인 작업을
     * 멈춰야 하면 true 를 반환한다. 단계 수는 이 조회에 포함되지 않는다. 스크립트를 실행하는 스레드에서 얼마든지 자주 호출해도 된다.
     *
     * @return true if the task should stop / 작업을 멈춰야 하면 true
     */
    public boolean isAbortRequested() {
        if (cancelled) {
            return true;
        }
        long t = maxTimeMillis;
        return t > 0 && System.currentTimeMillis() - taskStart > t;
    }

    /**
     * Throwing variant of {@link #isAbortRequested()} for long running native loops: throws {@link ScriptAbortedException}
     * (CANCELLED or TIME_LIMIT) when the task must stop. Built-ins that loop over large inputs should call it every few thousand
     * iterations.
     * <p>
     * 오래 걸리는 네이티브 반복을 위한 {@link #isAbortRequested()} 의 예외 버전: 작업을 멈춰야 하면 {@link ScriptAbortedException}
     * (CANCELLED 또는 TIME_LIMIT)을 던진다. 큰 입력을 반복하는 내장 객체는 수천 번 반복마다 호출해야 한다.
     */
    public void poll() {
        if (cancelled) {
            throw new ScriptAbortedException(ScriptAbortedException.Reason.CANCELLED, "Script execution was cancelled");
        }
        long t = maxTimeMillis;
        if (t > 0 && System.currentTimeMillis() - taskStart > t) {
            throw new ScriptAbortedException(ScriptAbortedException.Reason.TIME_LIMIT, "Script time limit exceeded (" + t + " ms)");
        }
    }

    /**
     * Returns true if cancellation was requested.
     * <p>
     * 취소가 요청되었으면 true.
     *
     * @return the flag / 표시
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Marks the start of a task (an outermost script evaluation or a job); nested calls are counted and ignored.
     * <p>
     * 작업(가장 바깥 스크립트 평가나 작업 큐의 작업)의 시작을 표시한다. 중첩 호출은 세기만 하고 무시한다.
     */
    public void beginTask() {
        if (taskDepth++ == 0) {
            steps = 0;
            taskStart = System.currentTimeMillis();
        }
    }

    /**
     * Marks the end of a task started with {@link #beginTask()}.
     * <p>
     * {@link #beginTask()} 로 시작한 작업의 끝을 표시한다.
     */
    public void endTask() {
        if (taskDepth > 0) {
            taskDepth--;
        }
    }

    /**
     * Counts one step and enforces the limits. Throws {@link ScriptAbortedException} when a limit is hit or cancellation was
     * requested.
     * <p>
     * 단계 하나를 세고 제한을 검사한다. 제한에 걸렸거나 취소가 요청되었으면 {@link ScriptAbortedException} 을 던진다.
     */
    public void tick() {
        long s = ++steps;
        if (cancelled) {
            throw new ScriptAbortedException(ScriptAbortedException.Reason.CANCELLED, "Script execution was cancelled");
        }
        long max = maxSteps;
        if (max > 0 && s > max) {
            throw new ScriptAbortedException(ScriptAbortedException.Reason.STEP_LIMIT, "Script step limit exceeded (" + max + ")");
        }
        if ((s & (CHECK_INTERVAL - 1)) == 0) {
            long t = maxTimeMillis;
            if (t > 0 && System.currentTimeMillis() - taskStart > t) {
                throw new ScriptAbortedException(ScriptAbortedException.Reason.TIME_LIMIT, "Script time limit exceeded (" + t + " ms)");
            }
        }
    }

    /**
     * Throws a RangeError if a string would be longer than the limit.
     * <p>
     * 문자열이 제한보다 길어질 경우 RangeError 를 던진다.
     *
     * @param length the resulting length in code units / 결과 길이(코드 단위)
     */
    public void checkStringLength(long length) {
        if (length > maxStringLength) {
            throw JsException.rangeError("Invalid string length");
        }
    }
}
