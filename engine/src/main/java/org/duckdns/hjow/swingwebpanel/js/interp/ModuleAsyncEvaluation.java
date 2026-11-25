package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.RealmExecution.ScriptScope;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseCapability;
import org.duckdns.hjow.swingwebpanel.js.builtins.meta.PromiseInterop;

/**
 * Promise-driven evaluation of a module with top-level await. It evaluates requested modules first, then replays the module
 * program through its saved generator state until all awaits settle.
 * <p>
 * top-level await 가 있는 모듈의 Promise 기반 평가. 요청 모듈을 먼저 평가한 뒤 저장된 제너레이터 상태로 모듈 프로그램을
 * 다시 따라가 모든 await 가 확정될 때까지 진행한다.
 */
final class ModuleAsyncEvaluation {

    /** Module record. / 모듈 레코드. */
    private final ModuleRecord module;
    /** Reused async execution state. / 재사용하는 비동기 실행 상태. */
    private final Ctx ctx;
    /** Requested-module index. / 요청 모듈 인덱스. */
    private int dependency;

    /**
     * Creates and starts evaluation.
     * <p>
     * 평가를 만들고 시작한다.
     */
    static void start(ModuleRecord module, PromiseCapability capability) {
        new ModuleAsyncEvaluation(module).nextDependency();
    }

    /**
     * Creates evaluation state.
     * <p>
     * 평가 상태를 만든다.
     */
    private ModuleAsyncEvaluation(ModuleRecord module) {
        this.module = module;
        this.ctx = new Ctx(true, module.env, null);
        this.ctx.funcEnv = module.env;
        this.ctx.sourceName = module.key;
        this.ctx.gen = new GenState();
    }

    /**
     * Evaluates the next dependency Promise.
     * <p>
     * 다음 의존성 Promise 를 평가한다.
     */
    private void nextDependency() {
        if (dependency >= module.requested().size()) {
            resume(GenState.NEXT, Js.UNDEFINED);
            return;
        }
        final ModuleRecord dep = module.requested().get(dependency++);
        NativeFunction fulfilled = new NativeFunction(module.in.realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args) {
                nextDependency();
                return Js.UNDEFINED;
            }
        });
        NativeFunction rejected = new NativeFunction(module.in.realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args) {
                failValue(Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        try {
            PromiseInterop.await(module.in.realm, dep.evaluateAsync(true), fulfilled, rejected);
        } catch (JsException e) {
            module.failAsync(e);
        }
    }

    /**
     * Resumes or starts the module body.
     * <p>
     * 모듈 본문을 재개하거나 시작한다.
     */
    private void resume(int mode, JsValue value) {
        GenState gen = ctx.gen;
        gen.suspending = false;
        gen.resumeMode = mode;
        gen.resumeValue = value;
        ScriptScope previous = module.in.enter();
        try {
            module.evaluateBody(ctx);
            module.finishAsync();
        } catch (AwaitSignal signal) {
            await(gen.awaited);
        } catch (JsException e) {
            module.failAsync(e);
        } finally {
            module.in.leave(previous);
        }
    }

    /**
     * Attaches reactions to one top-level await value.
     * <p>
     * top-level await 값 하나에 반응을 연결한다.
     */
    private void await(JsValue value) {
        NativeFunction fulfilled = new NativeFunction(module.in.realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args) {
                resume(GenState.NEXT, Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        NativeFunction rejected = new NativeFunction(module.in.realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm realm, JsValue thisValue, JsValue[] args) {
                resume(GenState.THROW, Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        try {
            PromiseInterop.await(module.in.realm, value, fulfilled, rejected);
        } catch (JsException e) {
            module.failAsync(e);
        }
    }

    /**
     * Fails the module with a Promise rejection value.
     * <p>
     * Promise 거부 값으로 모듈을 실패시킨다.
     */
    private void failValue(JsValue value) {
        module.failAsync(new JsException(value));
    }
}

