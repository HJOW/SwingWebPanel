package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * Small public bridge for the interpreter's async functions. It keeps Promise internal classes private while exposing only
 * intrinsic capability creation and await reaction attachment.
 * <p>
 * 인터프리터 async 함수를 위한 작은 공개 연결점이다. Promise 내부 클래스는 비공개로 두고,
 * intrinsic capability 생성과 await 반응 연결만 제공한다.
 */
public final class PromiseInterop {

    /**
     * Creates no instances.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private PromiseInterop() {
    }

    /**
     * Creates an intrinsic Promise capability.
     * <p>
     * intrinsic Promise capability 를 만든다.
     *
     * @param realm realm / realm
     * @return capability / capability
     */
    public static PromiseCapability newCapability(JsRealm realm) {
        return PromiseBuiltins.newCapability(realm, realm.getIntrinsic(PromiseBuiltins.INTRINSIC_PROMISE));
    }

    /**
     * Attaches fulfillment and rejection continuations with intrinsic PromiseResolve and PerformPromiseThen.
     * <p>
     * 내장 PromiseResolve와 PerformPromiseThen으로 이행·거부 이어짐을 연결한다.
     *
     * @param realm realm / realm
     * @param value awaited value / await 값
     * @param fulfilled fulfillment callback / 이행 콜백
     * @param rejected rejection callback / 거부 콜백
     */
    public static void await(JsRealm realm, JsValue value, JsValue fulfilled, JsValue rejected) {
        JsValue promiseConstructor = realm.getIntrinsic(PromiseBuiltins.INTRINSIC_PROMISE);
        PromiseObject promise = (PromiseObject) PromiseBuiltins.resolveWith(realm, promiseConstructor, value);
        PromiseBuiltins.performThen(realm, promise, fulfilled, rejected, null);
    }
}

