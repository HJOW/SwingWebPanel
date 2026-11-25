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
        JsRealm previousScript = SCRIPT.get();
        JsRealm previous = enter(realm);
        if (previous == realm && previousScript == realm) {
            return null;
        }
        if (previousScript != realm) {
            SCRIPT.set(realm);
        }
        return new ScriptScope(previous, previousScript);
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

        /**
         * Stores both realms for restoration.
         * <p>
         * 나중에 복원할 두 realm을 저장한다.
         *
         * @param previous prior running realm / 이전 실행 realm
         * @param previousScript prior script realm / 이전 스크립트 realm
         */
        private ScriptScope(JsRealm previous, JsRealm previousScript) {
            this.previous = previous;
            this.previousScript = previousScript;
        }
    }
}
