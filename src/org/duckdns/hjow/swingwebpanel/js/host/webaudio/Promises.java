package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * Creating and settling promises of the Web Audio interfaces (<code>resume()</code>, <code>decodeAudioData()</code>, <code>startRendering()</code>
 * and the others return promises). A promise is built with the <code>%Promise%</code> intrinsic of the realm, so a page that replaces
 * <code>window.Promise</code> cannot interfere, and its reactions run as ordinary promise jobs at the next microtask checkpoint of the task that settled it.
 * A <em>deferred</em> keeps the two resolving functions so that a later task can settle the promise.
 * <p>
 * Web Audio 인터페이스의 프라미스를 만들고 확정하는 도우미(<code>resume()</code>, <code>decodeAudioData()</code>, <code>startRendering()</code> 등이 프라미스를
 * 반환한다). 프라미스는 realm 의 <code>%Promise%</code> 내장 객체로 만들므로 페이지가 <code>window.Promise</code> 를 바꿔도 영향을 받지 않으며, 반응은 확정한 작업의
 * 다음 마이크로태스크 검사점에서 일반 프라미스 작업으로 실행된다. <em>deferred</em> 는 두 확정 함수를 보관하여 나중의 작업이 프라미스를 확정하게 한다.
 */
final class Promises {

    /**
     * A promise together with its resolving functions.
     * <p>
     * 프라미스와 그 확정 함수.
     */
    static final class Deferred {

        /**
         * The promise object that the script holds.
         * <p>
         * 스크립트가 쥐는 프라미스 객체.
         */
        final JsObject promise;

        /**
         * The resolve function.
         * <p>
         * resolve 함수.
         */
        private final JsValue resolveFn;

        /**
         * The reject function.
         * <p>
         * reject 함수.
         */
        private final JsValue rejectFn;

        /**
         * Creates the record.
         * <p>
         * 기록을 만든다.
         *
         * @param promise the promise / 프라미스
         * @param resolve the resolve function / resolve 함수
         * @param reject  the reject function / reject 함수
         */
        Deferred(JsObject promise, JsValue resolve, JsValue reject) {
            this.promise = promise;
            this.resolveFn = resolve;
            this.rejectFn = reject;
        }

        /**
         * Resolves the promise (no effect once it is settled).
         * <p>
         * 프라미스를 resolve 한다(이미 확정되었으면 효과 없음).
         *
         * @param value the value / 값
         */
        void resolve(JsValue value) {
            Ops.call(resolveFn, Js.UNDEFINED, new JsValue[] {value});
        }

        /**
         * Rejects the promise (no effect once it is settled).
         * <p>
         * 프라미스를 reject 한다(이미 확정되었으면 효과 없음).
         *
         * @param reason the reason / 이유
         */
        void reject(JsValue reason) {
            Ops.call(rejectFn, Js.UNDEFINED, new JsValue[] {reason});
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Promises() {
    }

    /**
     * Creates a pending promise.
     * <p>
     * 대기 중인 프라미스를 만든다.
     *
     * @param realm the realm / realm
     * @return the promise and its functions / 프라미스와 그 함수
     */
    static Deferred create(JsRealm realm) {
        final JsValue[] fns = new JsValue[2];
        NativeFunction executor = realm.newFunction("", 2, new NativeFunction.Call() {
            /**
             * Remembers the two resolving functions.
             * <p>
             * 두 확정 함수를 기억한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args the resolving functions / 확정 함수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                fns[0] = Js.arg(args, 0);
                fns[1] = Js.arg(args, 1);
                return Js.UNDEFINED;
            }
        });
        JsObject p = Ops.construct(realm.getIntrinsic("%Promise%"), new JsValue[] {executor});
        return new Deferred(p, fns[0], fns[1]);
    }

    /**
     * Creates a promise that is already rejected.
     * <p>
     * 이미 reject 된 프라미스를 만든다.
     *
     * @param realm  the realm / realm
     * @param reason the reason / 이유
     * @return the promise / 프라미스
     */
    static JsObject rejected(JsRealm realm, JsValue reason) {
        Deferred d = create(realm);
        d.reject(reason);
        return d.promise;
    }

    /**
     * Creates a promise that is already resolved.
     * <p>
     * 이미 resolve 된 프라미스를 만든다.
     *
     * @param realm the realm / realm
     * @param value the value / 값
     * @return the promise / 프라미스
     */
    static JsObject resolved(JsRealm realm, JsValue value) {
        Deferred d = create(realm);
        d.resolve(value);
        return d.promise;
    }
}
