package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.RealmExecution.ScriptScope;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseCapability;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;

/**
 * State of one async function activation. The JavaScript body is resumed by replaying its syntax tree with the saved
 * {@link GenState}; no Java thread is created for the function.
 * <p>
 * async 함수 활성 하나의 상태. 저장된 {@link GenState} 로 구문 트리를 다시 따라가 본문을 재개하며 Java 스레드를 만들지 않는다.
 */
final class AsyncFunction {

    /** Created, not started. / 만들어졌고 시작 전. */
    private static final int CREATED = 0;
    /** Running on the Java stack. / Java 스택에서 실행 중. */
    private static final int RUNNING = 1;
    /** Suspended at await. / await 에서 중단. */
    private static final int SUSPENDED = 2;
    /** Settled state. / 확정된 상태. */
    private static final int DONE = 3;

    /** Interpreter owner. / 소유 인터프리터. */
    private final Interpreter in;
    /** Async function. / async 함수. */
    private final ScriptFunction function;
    /** Prepared activation. / 준비된 활성. */
    private final Ctx ctx;
    /** Returned promise capability. / 반환 Promise capability. */
    private final PromiseCapability capability;
    /** Lifecycle state. / 수명 상태. */
    private int state = CREATED;

    /**
     * Creates the state.
     * <p>
     * 상태를 만든다.
     */
    private AsyncFunction(Interpreter in, ScriptFunction function, Ctx ctx) {
        this.in = in;
        this.function = function;
        this.ctx = ctx;
        this.capability = PromiseInterop.newCapability(in.realm);
    }

    /**
     * Starts an async function synchronously through its first await and returns its Promise.
     * <p>
     * async 함수를 첫 await 까지 동기로 시작하고 Promise 를 반환한다.
     *
     * @param in interpreter / 인터프리터
     * @param function async function / async 함수
     * @param ctx prepared context / 준비 문맥
     * @return Promise / Promise
     */
    static JsObject start(Interpreter in, ScriptFunction function, Ctx ctx) {
        AsyncFunction activation = new AsyncFunction(in, function, ctx);
        activation.resume(GenState.NEXT, Js.UNDEFINED, true);
        return activation.capability.getPromise();
    }

    /**
     * Resumes the body or handles the first run.
     * <p>
     * 본문을 재개하거나 첫 실행을 처리한다.
     *
     * @param mode {@link GenState#NEXT} or {@link GenState#THROW} / next 또는 throw
     * @param value fulfillment or rejection value / 이행 또는 거부 값
     * @param initial true for the synchronous start / 동기 시작이면 true
     */
    private void resume(int mode, JsValue value, boolean initial) {
        if (state == RUNNING || state == DONE) {
            return;
        }
        state = RUNNING;
        ScriptScope previous = initial ? null : in.enter();
        GenState gen = ctx.gen;
        gen.suspending = false;
        gen.resumeMode = mode;
        gen.resumeValue = value;
        try {
            JsValue result = in.runBody(function, ctx);
            settle(true, result);
        } catch (AwaitSignal signal) {
            state = SUSPENDED;
            await(gen.awaited);
        } catch (JsException e) {
            settle(false, e.getValue(in.realm));
        } catch (RuntimeException e) {
            state = DONE;
            throw e;
        } finally {
            if (!initial) {
                in.leave(previous);
            }
        }
    }

    /**
     * Attaches Promise reactions to an awaited value.
     * <p>
     * await 값에 Promise 반응을 연결한다.
     */
    private void await(JsValue value) {
        final JsRealm realm = in.realm;
        NativeFunction fulfilled = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                resume(GenState.NEXT, Js.arg(args, 0), false);
                return Js.UNDEFINED;
            }
        });
        NativeFunction rejected = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                resume(GenState.THROW, Js.arg(args, 0), false);
                return Js.UNDEFINED;
            }
        });
        try {
            PromiseInterop.await(realm, value, fulfilled, rejected);
        } catch (JsException e) {
            settle(false, e.getValue(realm));
        }
    }

    /**
     * Settles the returned async-function Promise.
     * <p>
     * 반환한 async 함수 Promise 를 확정한다.
     */
    private void settle(boolean fulfilled, JsValue value) {
        state = DONE;
        ctx.gen.saved.clear();
        Ops.call(fulfilled ? capability.getResolveFunction() : capability.getRejectFunction(),
                Js.UNDEFINED, new JsValue[] { value });
    }
}

