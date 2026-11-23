package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * Host hook that observes promises that are rejected without a rejection handler (HostPromiseRejectionTracker). A browser host uses it
 * to implement the <code>unhandledrejection</code> and <code>rejectionhandled</code> events; it is only a notification
 * interface and never changes the behavior of promises. Callbacks run synchronously inside the operation that caused them (on the thread
 * that runs the script), so implementations must return quickly and must not call back into the script engine.
 * <p>
 * 거부 처리기 없이 거부된 promise 를 관찰하는 호스트 훅(HostPromiseRejectionTracker). 브라우저 호스트가 <code>unhandledrejection</code>,
 * <code>rejectionhandled</code> 이벤트를 구현하는 데 쓴다. 통지용 인터페이스일 뿐이며 promise 의 동작을 바꾸지 않는다. 콜백은 원인이 된 연산
 * 안에서(스크립트를 실행하는 스레드에서) 동기적으로 실행되므로 구현은 빨리 반환해야 하고 스크립트 엔진을 다시 호출하면 안 된다.
 */
public interface PromiseRejectionTracker {

    /**
     * A promise was rejected while it had no rejection handler.
     * <p>
     * 거부 처리기가 없는 promise 가 거부되었다.
     *
     * @param realm   the realm / realm
     * @param promise the rejected promise / 거부된 promise
     * @param reason  the rejection reason / 거부 이유
     */
    void rejectedWithoutHandler(JsRealm realm, JsObject promise, JsValue reason);

    /**
     * A handler was added to a promise that had already been rejected without a handler.
     * <p>
     * 이미 처리기 없이 거부된 promise 에 처리기가 추가되었다.
     *
     * @param realm   the realm / realm
     * @param promise the promise / promise
     */
    void handlerAddedToRejectedPromise(JsRealm realm, JsObject promise);
}
