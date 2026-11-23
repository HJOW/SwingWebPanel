package org.duckdns.hjow.swingwebpanel.net;

import java.util.ArrayList;
import java.util.List;

/**
 * A cancellation flag shared between the thread that waits for a resource and the thread that wants to stop it. Network
 * code registers {@link Hook}s (for example "disconnect this HTTP connection" or "close this socket"); {@link #cancel()}
 * sets the flag once and runs every hook so that blocked reads end promptly. A token can be cancelled only once; later
 * hooks run immediately. Hooks must be quick and must not throw.
 * <p>
 * Use: <code>CancelToken t = new CancelToken(); request.setCancelToken(t); ... t.cancel();</code>
 * <p>
 * 리소스를 기다리는 스레드와 이를 멈추려는 스레드가 공유하는 취소 표시. 네트워크 코드는 {@link Hook}(예: "이 HTTP 연결을
 * 끊어라", "이 소켓을 닫아라")을 등록하고, {@link #cancel()} 은 표시를 한 번 세운 뒤 모든 훅을 실행해 막힌 읽기가 곧바로
 * 끝나게 한다. 토큰은 한 번만 취소할 수 있으며 그 뒤에 등록한 훅은 즉시 실행된다. 훅은 빠르고 예외를 던지지 않아야 한다.
 * <p>
 * 사용: <code>CancelToken t = new CancelToken(); request.setCancelToken(t); ... t.cancel();</code>
 */
public final class CancelToken {

    /**
     * Action run when the token is cancelled.
     * <p>
     * 토큰이 취소될 때 실행할 동작.
     */
    public interface Hook {
        /**
         * Called once from the cancelling thread.
         * <p>
         * 취소하는 스레드에서 한 번 호출된다.
         */
        void onCancel();
    }

    /**
     * Whether cancel() was called.
     * <p>
     * cancel() 이 호출되었는지 여부.
     */
    private boolean cancelled;
    /**
     * Registered hooks.
     * <p>
     * 등록된 훅.
     */
    private final List<Hook> hooks = new ArrayList<Hook>();

    /**
     * Creates a token that is not cancelled.
     * <p>
     * 취소되지 않은 토큰을 만든다.
     */
    public CancelToken() {
    }

    /**
     * Tells whether the token was cancelled.
     * <p>
     * 토큰이 취소되었는지 알려 준다.
     *
     * @return true if cancelled / 취소되었으면 true
     */
    public synchronized boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels: sets the flag and runs all hooks (once).
     * <p>
     * 취소한다. 표시를 세우고 모든 훅을 한 번 실행한다.
     */
    public void cancel() {
        List<Hook> copy;
        synchronized (this) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            copy = new ArrayList<Hook>(hooks);
            hooks.clear();
        }
        // 등록된 훅을 차례로 실행하고 훅의 오류는 다른 훅에 영향을 주지 않게 삼킨다.
        for (int i = 0; i < copy.size(); i++) {
            try {
                copy.get(i).onCancel();
            } catch (RuntimeException e) {
                // 한 훅의 실패가 취소 처리를 막지 않는다.
            }
        }
    }

    /**
     * Registers a hook; runs it immediately when already cancelled.
     * <p>
     * 훅을 등록한다. 이미 취소되었으면 즉시 실행한다.
     *
     * @param hook hook to run on cancel / 취소 시 실행할 훅
     */
    public void addHook(Hook hook) {
        boolean runNow;
        synchronized (this) {
            runNow = cancelled;
            if (!runNow) {
                hooks.add(hook);
            }
        }
        if (runNow) {
            hook.onCancel();
        }
    }

    /**
     * Unregisters a hook.
     * <p>
     * 훅 등록을 해제한다.
     *
     * @param hook hook to remove / 제거할 훅
     */
    public synchronized void removeHook(Hook hook) {
        hooks.remove(hook);
    }

    /**
     * Throws {@link CancelledException} if cancelled.
     * <p>
     * 취소되었으면 {@link CancelledException} 을 던진다.
     *
     * @throws CancelledException when cancelled / 취소되었을 때
     */
    public void throwIfCancelled() throws CancelledException {
        if (isCancelled()) {
            throw new CancelledException();
        }
    }
}
