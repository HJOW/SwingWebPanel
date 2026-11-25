package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * A PromiseCapability record (ECMA-262 6th edition 25.4.1.1): a promise (possibly of a subclass) together with its resolve and reject
 * functions, created by NewPromiseCapability for a constructor.
 * <p>
 * PromiseCapability 기록(ECMA-262 6판 25.4.1.1): NewPromiseCapability 가 생성자로 만든 promise(하위 클래스일 수 있음)와 그 resolve,
 * reject 함수.
 */
public final class PromiseCapability {

    /**
     * The promise object.
     * <p>
     * promise 객체.
     */
    private final JsObject promise;

    /**
     * The resolve function (callable).
     * <p>
     * resolve 함수(호출 가능).
     */
    private final JsValue resolveFunction;

    /**
     * The reject function (callable).
     * <p>
     * reject 함수(호출 가능).
     */
    private final JsValue rejectFunction;

    /**
     * Creates the record.
     * <p>
     * 기록을 만든다.
     *
     * @param promise the promise / promise
     * @param resolve the resolve function / resolve 함수
     * @param reject  the reject function / reject 함수
     */
    PromiseCapability(JsObject promise, JsValue resolve, JsValue reject) {
        this.promise = promise;
        this.resolveFunction = resolve;
        this.rejectFunction = reject;
    }

    /**
     * Returns the promise.
     * <p>
     * promise 를 반환한다.
     *
     * @return the promise / promise
     */
    public JsObject getPromise() {
        return promise;
    }

    /**
     * Returns the resolve function.
     * <p>
     * resolve 함수를 반환한다.
     *
     * @return the function / 함수
     */
    public JsValue getResolveFunction() {
        return resolveFunction;
    }

    /**
     * Returns the reject function.
     * <p>
     * reject 함수를 반환한다.
     *
     * @return the function / 함수
     */
    public JsValue getRejectFunction() {
        return rejectFunction;
    }

    /**
     * Calls the resolve function with a value.
     * <p>
     * resolve 함수를 값과 함께 호출한다.
     *
     * @param value the resolution / 해결 값
     */
    public void resolve(JsValue value) {
        Ops.call(resolveFunction, Js.UNDEFINED, new JsValue[] { value });
    }

    /**
     * Calls the reject function with a reason.
     * <p>
     * reject 함수를 이유와 함께 호출한다.
     *
     * @param reason the reason / 이유
     */
    public void reject(JsValue reason) {
        Ops.call(rejectFunction, Js.UNDEFINED, new JsValue[] { reason });
    }
}
