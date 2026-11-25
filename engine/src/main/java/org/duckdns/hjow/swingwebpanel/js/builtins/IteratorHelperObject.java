package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * A suspended iterator helper with a shared next and return protocol.
 * <p>
 * 공통 next·return 규칙을 사용하는 중단 가능한 이터레이터 헬퍼.
 */
abstract class IteratorHelperObject extends JsObject {

    /**
     * States of the generator-like helper.
     * <p>
     * 제너레이터처럼 중단 가능한 헬퍼의 상태.
     */
    private enum State {
        /** No next call has resumed the helper. / 아직 next로 헬퍼를 재개하지 않았다. */
        START,
        /** The helper is evaluating observable code. / 헬퍼가 관찰 가능한 코드를 평가 중이다. */
        RUNNING,
        /** A value was yielded and the sources are paused. / 값을 반환한 뒤 원본의 진행을 멈춘 상태다. */
        YIELDED,
        /** The helper can no longer resume. / 헬퍼를 더 이상 재개할 수 없다. */
        COMPLETE
    }

    /** The realm in which this helper was constructed. / 헬퍼를 만든 realm. */
    private final JsRealm executionRealm;

    /** Current suspension state. / 현재 중단 상태. */
    private State state = State.START;

    /**
     * Creates a helper under the realm's IteratorHelperPrototype.
     * <p>
     * realm의 IteratorHelperPrototype을 상속하는 헬퍼를 만든다.
     *
     * @param realm realm that constructs and owns the helper / 헬퍼를 만들고 소유하는 realm
     */
    IteratorHelperObject(JsRealm realm) {
        super(realm.getIntrinsic("%IteratorHelperPrototype%"));
        executionRealm = realm;
    }

    /**
     * Installs the shared next, return and toStringTag properties.
     * <p>
     * 공통 next, return, toStringTag 프로퍼티를 설치한다.
     *
     * @param realm receiving realm / 설치할 realm
     */
    static void install(JsRealm realm) {
        JsObject proto = new JsObject(realm.getIteratorPrototype());
        realm.setIntrinsic("%IteratorHelperPrototype%", proto);
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("Iterator Helper"), false, false, true);
        realm.method(proto, "next", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return requireHelper(receiver).next(r);
            }
        });
        realm.method(proto, "return", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                return requireHelper(receiver).returnNow(r);
            }
        });
    }

    /**
     * Checks that the receiver has the helper's state and source records.
     * <p>
     * 수신 객체가 헬퍼의 상태와 원본 기록을 갖는지 확인한다.
     *
     * @param receiver the receiver / 수신 객체
     * @return the helper object / 헬퍼 객체
     */
    private static IteratorHelperObject requireHelper(JsValue receiver) {
        if (!(receiver instanceof IteratorHelperObject)) {
            throw JsException.typeError("Expected an Iterator Helper");
        }
        return (IteratorHelperObject) receiver;
    }

    /**
     * Returns the next value produced by the suspended operation.
     * <p>
     * 중단된 연산을 재개해 다음 값을 반환한다.
     *
     * @param realm realm of the result object / 결과 객체의 realm
     * @return iterator result / 이터레이터 결과
     */
    private JsObject next(JsRealm realm) {
        checkReentrancy();
        if (state == State.COMPLETE) {
            return IteratorHelper.createResult(realm, Js.UNDEFINED, true);
        }
        state = State.RUNNING;
        try {
            JsValue item = advance(executionRealm);
            if (item == null) {
                return IteratorHelper.createResult(realm, Js.UNDEFINED, true);
            }
            state = State.YIELDED;
            return IteratorHelper.createResult(realm, item, false);
        } finally {
            // 값으로 중단한 경우를 제외하면 끝난 상태로 바꾸고 원본을 놓는다.
            if (state == State.RUNNING) {
                complete();
            }
        }
    }

    /**
     * Stops this helper and closes its live source iterators.
     * <p>
     * 헬퍼를 끝내고 진행 중인 원본 이터레이터를 닫는다.
     *
     * @param realm realm of the result object / 결과 객체의 realm
     * @return done result / 완료된 이터레이터 결과
     */
    private JsObject returnNow(JsRealm realm) {
        checkReentrancy();
        if (state != State.COMPLETE) {
            // 시작 전 return은 이미 끝난 상태로 닫고, 중단 중이면 실행 상태에서 닫는다.
            state = state == State.START ? State.COMPLETE : State.RUNNING;
            try {
                closeSources();
            } finally {
                complete();
            }
        }
        return IteratorHelper.createResult(realm, Js.UNDEFINED, true);
    }

    /** Disallows reentrant next and return while the operation runs. / 연산 중 next와 return의 재진입을 금지한다. */
    private void checkReentrancy() {
        if (state == State.RUNNING) {
            throw JsException.typeError("Iterator Helper is already running");
        }
    }

    /** Marks the helper as completed and releases its captured objects. / 헬퍼를 완료 상태로 만들고 기록을 해제한다. */
    private void complete() {
        state = State.COMPLETE;
        discard();
    }

    /**
     * Advances the operation to its next yield.
     * <p>
     * 연산을 다음 반환값까지 진행한다.
     *
     * @param realm the execution realm / 실행 realm
     * @return the next value or Java null on completion / 다음 반환값. 끝났으면 Java null
     */
    abstract JsValue advance(JsRealm realm);

    /** Closes the current sources on a normal return. / 정상 return 시 활성 원본을 닫는다. */
    abstract void closeSources();

    /** Releases state that will no longer be used. / 더 이상 쓰지 않는 상태를 해제한다. */
    abstract void discard();
}
