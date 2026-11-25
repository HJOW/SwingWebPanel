package org.duckdns.hjow.swingwebpanel.js;

/**
 * Tracks the running realm on the current Java thread while JavaScript or a built-in is executing.
 * <p>
 * JavaScript 또는 내장 함수가 실행되는 동안 현재 Java 스레드의 실행 realm을 추적한다.
 */
public final class RealmExecution {

    /** Innermost executing realm for each thread. / 스레드마다 가장 안쪽에서 실행하는 realm. */
    private static final ThreadLocal<JsRealm> ACTIVE = new ThreadLocal<JsRealm>();

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
}
