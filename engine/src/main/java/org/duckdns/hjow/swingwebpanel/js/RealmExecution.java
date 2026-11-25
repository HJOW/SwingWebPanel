package org.duckdns.hjow.swingwebpanel.js;

/**
 * Tracks the running realm on the current Java thread while JavaScript or a built-in is executing.
 * <p>
 * JavaScript 또는 내장 함수가 실행되는 동안 현재 Java 스레드의 실행 realm을 추적한다.
 */
public final class RealmExecution {

    /** Innermost executing realm for each thread. / 스레드마다 가장 안쪽에서 실행하는 realm. */
    private static final ThreadLocal<JsRealm> ACTIVE = new ThreadLocal<JsRealm>();

    /** Innermost executing ECMAScript code's realm. / 가장 안쪽에서 ECMAScript 코드를 실행하는 realm. */
    private static final ThreadLocal<JsRealm> SCRIPT = new ThreadLocal<JsRealm>();

    /** Key of the active script or module, independent of stack trace labels. / 오류 위치 이름과 별개인 활성 스크립트 또는 모듈 키. */
    private static final ThreadLocal<String> SCRIPT_SOURCE = new ThreadLocal<String>();

    /** Backup incumbent realm supplied by a browser job callback. / 브라우저 작업 콜백이 제공하는 예비 incumbent realm. */
    private static final ThreadLocal<JsRealm> INCUMBENT = new ThreadLocal<JsRealm>();

    /** Static execution scope. / 정적 실행 범위. */
    private RealmExecution() {
    }

    /**
     * Returns the executing realm, or null outside an execution scope.
     * <p>
     * 실행 중인 realm을 반환한다. 실행 범위 밖이면 null이다.
     *
     * @return current realm / 현재 realm
     */
    public static JsRealm current() {
        return ACTIVE.get();
    }

    /**
     * Returns the current script realm through calls to built-in functions.
     * <p>
     * 내장 함수 실행 중에도 현재 스크립트 realm을 반환한다.
     *
     * @return current script realm, or null / 현재 스크립트 realm 또는 null
     */
    public static JsRealm currentScript() {
        return SCRIPT.get();
    }

    /**
     * Returns the source key used as the referrer for host module loading.
     * <p>
     * 호스트 모듈을 불러올 때 참조 기준으로 쓰는 원문 키를 반환한다.
     *
     * @return the active key or null if there is no active script record / 활성 키 또는 활성 스크립트 레코드가 없으면 null
     */
    public static String currentScriptSource() {
        return SCRIPT_SOURCE.get();
    }

    /**
     * Returns the active script realm, falling back to the host's saved incumbent during native callbacks.
     * <p>
     * 활성 스크립트 realm을 반환한다. 네이티브 콜백에서 활성 스크립트가 없으면 호스트가 보관한 incumbent를 반환한다.
     *
     * @return the incumbent realm, or null / incumbent realm 또는 null
     */
    public static JsRealm currentIncumbent() {
        JsRealm script = SCRIPT.get();
        return script == null ? INCUMBENT.get() : script;
    }

    /**
     * Enters the host state saved when a Promise job callback was registered.
     * <p>
     * Promise 작업 콜백 등록 시 저장한 호스트 상태에 들어간다.
     *
     * @param incumbent the saved incumbent realm / 저장한 incumbent realm
     * @param source the saved active script key, or null / 저장한 활성 스크립트 키 또는 null
     * @return prior state for restoration / 복원할 이전 상태
     */
    public static CallbackScope enterJobCallback(JsRealm incumbent, String source) {
        CallbackScope previous = new CallbackScope(INCUMBENT.get(), SCRIPT_SOURCE.get());
        INCUMBENT.set(incumbent);
        // 실제 스크립트가 없으면 호출자를 바꾸지 않고 예비 realm만 제공한다.
        if (source != null) {
            SCRIPT_SOURCE.set(source);
        }
        return previous;
    }

    /**
     * Restores the host state from before a Promise job callback.
     * <p>
     * Promise 작업 콜백을 실행하기 전 호스트 상태를 복원한다.
     *
     * @param previous the saved prior state / 저장한 이전 상태
     */
    public static void restoreJobCallback(CallbackScope previous) {
        if (previous.incumbent == null) {
            INCUMBENT.remove();
        } else {
            INCUMBENT.set(previous.incumbent);
        }
        setScriptSource(previous.source);
    }

    /**
     * Replaces or clears the active script key.
     * <p>
     * 활성 스크립트 키를 교체하거나 지운다.
     *
     * @param source the new key or null / 새 키 또는 null
     */
    private static void setScriptSource(String source) {
        if (source == null) {
            SCRIPT_SOURCE.remove();
        } else {
            SCRIPT_SOURCE.set(source);
        }
    }

    /**
     * The host state interrupted by a job callback.
     * <p>
     * 작업 콜백이 실행되기 전의 호스트 상태.
     */
    public static final class CallbackScope {

        /** Previous incumbent realm. / 이전 incumbent realm. */
        private final JsRealm incumbent;

        /** Previous active script key. / 이전 활성 스크립트 키. */
        private final String source;

        /**
         * Saves the state for restoration.
         * <p>
         * 나중에 복원할 상태를 저장한다.
         *
         * @param incumbent prior incumbent realm / 이전 incumbent realm
         * @param source prior active script key / 이전 활성 스크립트 키
         */
        private CallbackScope(JsRealm incumbent, String source) {
            this.incumbent = incumbent;
            this.source = source;
        }
    }

    /**
     * Enters the supplied realm. The implementation must restore the returned value in a finally block.
     * <p>
     * 지정한 realm에서 실행을 시작한다. 반환값은 finally 블록에서 복원해야 한다.
     *
     * @param realm entering realm / 시작할 realm
     * @return previous realm, or null / 이전 realm 또는 null
     */
    public static JsRealm enter(JsRealm realm) {
        JsRealm previous = ACTIVE.get();
        if (previous != realm) {
            ACTIVE.set(realm);
        }
        return previous;
    }

    /**
     * Restores the caller's execution scope without retaining ended scopes in thread-local storage.
     * <p>
     * 호출자의 실행 범위를 복원하고 종료한 범위를 스레드 로컬에 보관하지 않는다.
     *
     * @param previous previously returned realm, or null / 시작할 때 반환한 이전 realm 또는 null
     */
    public static void restore(JsRealm previous) {
        if (previous == null) {
            ACTIVE.remove();
        } else if (ACTIVE.get() != previous) {
            ACTIVE.set(previous);
        }
    }

    /**
     * Enters ECMAScript code and tracks its realm separately from any built-in it calls.
     * <p>
     * ECMAScript 코드에 들어가며 코드의 realm을 그 코드가 호출한 내장 함수와 별도로 추적한다.
     *
     * @param realm entering realm / 실행할 realm
     * @return previous execution scope, or null if unchanged / 이전 실행 범위. 바뀌지 않으면 null
     */
    public static ScriptScope enterScript(JsRealm realm) {
        return enterScript(realm, null);
    }

    /**
     * Enters ECMAScript code with its script or module reference key.
     * <p>
     * 스크립트나 모듈의 참조 키를 사용해 ECMAScript 코드로 들어간다.
     *
     * @param realm entering realm / 실행할 realm
     * @param source active script or module key, or null / 활성 스크립트나 모듈 키 또는 null
     * @return previous execution scope, or null if unchanged / 이전 실행 범위. 바뀌지 않으면 null
     */
    public static ScriptScope enterScript(JsRealm realm, String source) {
        JsRealm previousScript = SCRIPT.get();
        JsRealm previous = enter(realm);
        String previousSource = SCRIPT_SOURCE.get();
        if (previous == realm && previousScript == realm && previousSource == source) {
            return null;
        }
        if (previousScript != realm) {
            SCRIPT.set(realm);
        }
        if (source != previousSource) {
            setScriptSource(source);
        }
        return new ScriptScope(previous, previousScript, previousSource);
    }

    /**
     * Restores execution scopes after ECMAScript code stops.
     * <p>
     * ECMAScript 코드가 멈추면 이전 실행 범위를 복원한다.
     *
     * @param scope previous scope from enterScript / enterScript가 반환한 이전 범위
     */
    public static void restoreScript(ScriptScope scope) {
        if (scope != null) {
            restore(scope.previous);
            if (scope.previousScript == null) {
                SCRIPT.remove();
            } else if (SCRIPT.get() != scope.previousScript) {
                SCRIPT.set(scope.previousScript);
            }
            if (SCRIPT_SOURCE.get() != scope.previousSource) {
                setScriptSource(scope.previousSource);
            }
        }
    }

    /**
     * State saved when entering code in a different realm.
     * <p>
     * 다른 realm의 코드에 진입할 때 저장한 상태.
     */
    public static final class ScriptScope {
        /** Prior realm for operations. / 연산에 쓰던 realm. */
        private final JsRealm previous;

        /** Prior realm executing script source. / 스크립트 원문을 실행하던 realm. */
        private final JsRealm previousScript;

        /** Prior active script or module key. / 이전 활성 스크립트 또는 모듈 키. */
        private final String previousSource;

        /**
         * Stores the realms and source for restoration.
         * <p>
         * 나중에 복원할 두 realm과 원문 키를 저장한다.
         *
         * @param previous prior running realm / 이전 실행 realm
         * @param previousScript prior script realm / 이전 스크립트 realm
         * @param previousSource prior script or module key / 이전 스크립트 또는 모듈 키
         */
        private ScriptScope(JsRealm previous, JsRealm previousScript, String previousSource) {
            this.previous = previous;
            this.previousScript = previousScript;
            this.previousSource = previousSource;
        }
    }
}
