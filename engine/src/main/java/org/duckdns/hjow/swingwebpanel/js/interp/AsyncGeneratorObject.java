package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
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
 * Async generator object. Calls to next, return and throw create Promise requests that are processed in order; the body
 * resumes from saved {@link GenState} and never owns a Java thread.
 * <p>
 * async generator 객체. next, return, throw 호출은 순서대로 처리하는 Promise 요청을 만들며,
 * 본문은 저장된 {@link GenState} 로 재개되고 Java 스레드를 소유하지 않는다.
 */
public final class AsyncGeneratorObject extends JsObject {

    /** next request. / next 요청. */
    public static final int NEXT = GenState.NEXT;
    /** return request. / return 요청. */
    public static final int RETURN = GenState.RETURN;
    /** throw request. / throw 요청. */
    public static final int THROW = GenState.THROW;

    /** Awaiting the first request. / 첫 요청 대기. */
    private static final int START = 0;
    /** Suspended after a yield. / yield 뒤 중단. */
    private static final int SUSPENDED = 1;
    /** Executing the body. / 본문 실행 중. */
    private static final int RUNNING = 2;
    /** Awaiting a value Promise. / 값 Promise 대기. */
    private static final int AWAITING = 3;
    /** Completed state. / 완료된 상태. */
    private static final int DONE = 4;

    /**
     * One queued request.
     * <p>
     * 대기 요청 하나.
     */
    private static final class Request {
        /** Request mode. / 요청 종류. */
        final int mode;
        /** Request value. / 요청 값. */
        final JsValue value;
        /** Promise capability. / Promise capability 기록. */
        final PromiseCapability capability;

        /**
         * Creates a request.
         * <p>
         * 요청을 만든다.
         */
        Request(int mode, JsValue value, PromiseCapability capability) {
            this.mode = mode;
            this.value = value;
            this.capability = capability;
        }
    }

    /** Interpreter owner. / 소유 인터프리터. */
    private final Interpreter in;
    /** Async generator function. / async generator 함수. */
    private final ScriptFunction function;
    /** Execution context. / 실행 문맥. */
    private final Ctx ctx;
    /** Queued requests. / 대기 요청. */
    private final List<Request> requests = new ArrayList<Request>();
    /** Current request. / 현재 요청. */
    private Request current;
    /** Lifecycle state. / 수명 상태. */
    private int state = START;

    /**
     * Creates an object.
     * <p>
     * 객체를 만든다.
     */
    private AsyncGeneratorObject(JsObject proto, Interpreter in, ScriptFunction function, Ctx ctx) {
        super(proto);
        this.in = in;
        this.function = function;
        this.ctx = ctx;
    }

    /**
     * Creates an async generator object after the function arguments have been bound.
     * <p>
     * 인수 바인딩 뒤 async generator 객체를 만든다.
     *
     * @param in interpreter / 인터프리터
     * @param function async generator function / async generator 함수
     * @param ctx prepared context / 준비 문맥
     * @return object / 객체
     */
    static JsObject create(Interpreter in, ScriptFunction function, Ctx ctx) {
        JsValue value = function.get("prototype");
        JsObject proto = value instanceof JsObject ? (JsObject) value : in.realm.getIntrinsic("%AsyncGeneratorPrototype%");
        return new AsyncGeneratorObject(proto == null ? in.realm.newObject() : proto, in, function, ctx);
    }

    /**
     * Queues a next/return/throw request and returns its Promise.
     * <p>
     * next/return/throw 요청을 큐에 넣고 Promise 를 반환한다.
     *
     * @param mode request kind / 요청 종류
     * @param value request value / 요청 값
     * @return Promise / Promise
     */
    public JsObject request(int mode, JsValue value) {
        PromiseCapability capability = PromiseInterop.newCapability(in.realm);
        if (state == DONE) {
            if (mode == THROW) {
                Ops.call(capability.getRejectFunction(), Js.UNDEFINED, new JsValue[] { value });
            } else {
                Ops.call(capability.getResolveFunction(), Js.UNDEFINED,
                        new JsValue[] { IteratorHelper.createResult(in.realm, mode == RETURN ? value : Js.UNDEFINED, true) });
            }
            return capability.getPromise();
        }
        requests.add(new Request(mode, value, capability));
        process();
        return capability.getPromise();
    }

    /**
     * Processes the first queued request when no request is running or awaiting.
     * <p>
     * 실행·대기 중인 요청이 없으면 첫 대기 요청을 처리한다.
     */
    private void process() {
        if (state == RUNNING || state == AWAITING || current != null || requests.isEmpty()) {
            return;
        }
        current = requests.remove(0);
        if (state == START && current.mode != NEXT) {
            if (current.mode == RETURN) {
                state = DONE;
                settle(true, IteratorHelper.createResult(in.realm, current.value, true));
                process();
                return;
            }
            state = DONE;
            settle(false, current.value);
            process();
            return;
        }
        state = RUNNING;
        resume(current.mode, current.value);
    }

    /**
     * Resumes the generator body from the request or await reaction.
     * <p>
     * 요청이나 await 반응에서 제너레이터 본문을 재개한다.
     */
    private void resume(int mode, JsValue value) {
        GenState gen = ctx.gen;
        gen.suspending = false;
        gen.resumeMode = mode;
        gen.resumeValue = value;
        ScriptScope previous = in.enter(function.scriptReference);
        try {
            JsValue result = in.runBody(function, ctx);
            state = DONE;
            settle(true, IteratorHelper.createResult(in.realm, result, true));
            process();
        } catch (YieldSignal signal) {
            state = SUSPENDED;
            yieldValue(gen.yieldedRaw, gen.yielded);
        } catch (AwaitSignal signal) {
            state = AWAITING;
            await(gen.awaited);
        } catch (GeneratorReturn returned) {
            state = DONE;
            settle(true, IteratorHelper.createResult(in.realm, returned.value, true));
            process();
        } catch (JsException e) {
            state = DONE;
            settle(false, e.getValue(in.realm));
            process();
        } finally {
            in.leave(previous);
        }
    }

    /**
     * Await-resolves a yielded value or passes a delegated iterator result through.
     * <p>
     * yield 값을 await 해결하거나 위임한 이터레이터 결과를 그대로 전달한다.
     */
    private void yieldValue(boolean raw, JsValue value) {
        if (raw && value instanceof JsObject) {
            settle(true, (JsObject) value);
            process();
            return;
        }
        final Request request = current;
        NativeFunction fulfilled = new NativeFunction(in.realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args) {
                settle(true, IteratorHelper.createResult(realm, Js.arg(args, 0), false));
                process();
                return Js.UNDEFINED;
            }
        });
        NativeFunction rejected = new NativeFunction(in.realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args) {
                current = request;
                state = SUSPENDED;
                resume(THROW, Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        PromiseInterop.await(in.realm, value, fulfilled, rejected);
    }

    /**
     * Attaches reactions to an await value while retaining the current request.
     * <p>
     * 현재 요청을 유지한 채 await 값에 반응을 연결한다.
     */
    private void await(JsValue value) {
        NativeFunction fulfilled = new NativeFunction(in.realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args) {
                resume(NEXT, Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        NativeFunction rejected = new NativeFunction(in.realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args) {
                resume(THROW, Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        PromiseInterop.await(in.realm, value, fulfilled, rejected);
    }

    /**
     * Settles the current request and clears it.
     * <p>
     * 현재 요청을 확정하고 지운다.
     */
    private void settle(boolean fulfilled, JsValue value) {
        Request request = current;
        current = null;
        Ops.call(fulfilled ? request.capability.getResolveFunction() : request.capability.getRejectFunction(),
                Js.UNDEFINED, new JsValue[] { value });
    }

    /**
     * Returns "AsyncGenerator".
     * <p>
     * "AsyncGenerator" 를 반환한다.
     */
    public String className() {
        return "AsyncGenerator";
    }
}

