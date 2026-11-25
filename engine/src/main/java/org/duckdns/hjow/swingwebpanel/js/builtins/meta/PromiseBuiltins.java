package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The Promise constructor, its prototype and the promise abstract operations (ECMA-262 6th edition 25.4). Reaction and thenable jobs
 * are put on the job queue of the realm ({@link PromiseJobs}) and never run synchronously. Resolution with a thenable takes two extra
 * ticks (PromiseResolveThenableJob, then the reaction job of the inner promise). Subclassing works through
 * <code>constructor</code> and <code>Symbol.species</code>: <code>then</code> creates its result promise with
 * SpeciesConstructor, and the static methods use their <code>this</code> value as the constructor.
 * <p>
 * Promise 생성자, 프로토타입, promise 추상 연산(ECMA-262 6판 25.4). 반응 작업과 thenable 작업은 realm 의 작업 큐
 * ({@link PromiseJobs})에 넣으며 절대 동기적으로 실행되지 않는다. thenable 로 해결하면 틱이 두 번 더 걸린다
 * (PromiseResolveThenableJob, 이어서 안쪽 promise 의 반응 작업). 하위 클래스는 <code>constructor</code> 와 <code>Symbol.species</code>
 * 로 동작한다: <code>then</code> 은 SpeciesConstructor 로 결과 promise 를 만들고 정적 메서드는 <code>this</code> 값을 생성자로 쓴다.
 */
final class PromiseBuiltins {

    /**
     * Name of the intrinsic that holds the Promise constructor.
     * <p>
     * Promise 생성자를 담는 내장 객체 이름.
     */
    static final String INTRINSIC_PROMISE = "%Promise%";

    /**
     * Name of the intrinsic that holds Promise.prototype.
     * <p>
     * Promise.prototype 을 담는 내장 객체 이름.
     */
    static final String INTRINSIC_PROTOTYPE = "%PromisePrototype%";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PromiseBuiltins() {
    }

    // ------------------------------------------------------------------ 25.4.1 abstract operations

    /**
     * CreateResolvingFunctions (25.4.1.3): the pair of resolve and reject functions of a promise. They share an
     * [[AlreadyResolved]] flag, so only the first call of either has an effect.
     * <p>
     * CreateResolvingFunctions (25.4.1.3): promise 의 resolve, reject 함수 쌍. [[AlreadyResolved]] 플래그를 공유하므로 둘 중 먼저
     * 호출한 것만 효과가 있다.
     *
     * @param realm   the realm / realm
     * @param promise the promise / promise
     * @return array {resolve, reject} / 배열 {resolve, reject}
     */
    static NativeFunction[] createResolvingFunctions(final JsRealm realm, final PromiseObject promise) {
        final boolean[] alreadyResolved = new boolean[1];
        NativeFunction resolve = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (alreadyResolved[0]) {
                    return Js.UNDEFINED;
                }
                alreadyResolved[0] = true;
                resolvePromise(r, promise, Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        NativeFunction reject = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (alreadyResolved[0]) {
                    return Js.UNDEFINED;
                }
                alreadyResolved[0] = true;
                rejectPromise(r, promise, Js.arg(args, 0));
                return Js.UNDEFINED;
            }
        });
        return new NativeFunction[] { resolve, reject };
    }

    /**
     * The body of a promise resolve function (25.4.1.3 steps 7 onward): self-resolution is a TypeError, non-objects fulfill, objects
     * with a callable <code>then</code> start a PromiseResolveThenableJob, everything else fulfills.
     * <p>
     * promise resolve 함수의 본문(25.4.1.3 의 7단계부터): 자기 자신으로 해결하면 TypeError, 객체가 아니면 이행, 호출 가능한
     * <code>then</code> 이 있는 객체는 PromiseResolveThenableJob 을 시작하고 그 밖에는 이행한다.
     *
     * @param realm      the realm / realm
     * @param promise    the promise to resolve / 해결할 promise
     * @param resolution the resolution value / 해결 값
     */
    private static void resolvePromise(final JsRealm realm, final PromiseObject promise, JsValue resolution) {
        if (Ops.sameValue(resolution, promise)) {
            rejectPromise(realm, promise, JsException.typeError("Chaining cycle detected for promise #<Promise>").getValue(realm));
            return;
        }
        if (!(resolution instanceof JsObject)) {
            fulfillPromise(realm, promise, resolution);
            return;
        }
        final JsObject thenable = (JsObject) resolution;
        final JsValue then;
        try {
            then = thenable.get("then");
        } catch (JsException e) {
            rejectPromise(realm, promise, e.getValue(realm));
            return;
        }
        if (!then.isCallable()) {
            fulfillPromise(realm, promise, resolution);
            return;
        }
        // PromiseResolveThenableJob(25.4.2.2): 작업 큐에 넣어 나중에 thenable.then 을 호출한다.
        PromiseJobs.enqueue(realm, new Runnable() {
            public void run() {
                NativeFunction[] fns = createResolvingFunctions(realm, promise);
                try {
                    Ops.call(then, thenable, new JsValue[] { fns[0], fns[1] });
                } catch (JsException e) {
                    Ops.call(fns[1], Js.UNDEFINED, new JsValue[] { e.getValue(realm) });
                }
            }
        });
    }

    /**
     * FulfillPromise (25.4.1.4).
     * <p>
     * FulfillPromise (25.4.1.4).
     *
     * @param realm   the realm / realm
     * @param promise the promise / promise
     * @param value   the fulfillment value / 이행 값
     */
    static void fulfillPromise(JsRealm realm, PromiseObject promise, JsValue value) {
        List<PromiseReaction> reactions = promise.fulfillReactions();
        promise.settle(PromiseObject.FULFILLED, value);
        triggerReactions(realm, reactions, value);
    }

    /**
     * RejectPromise (25.4.1.7), including the host notification for promises without a handler.
     * <p>
     * RejectPromise (25.4.1.7). 처리기가 없는 promise 에 대한 호스트 통지를 포함한다.
     *
     * @param realm   the realm / realm
     * @param promise the promise / promise
     * @param reason  the rejection reason / 거부 이유
     */
    static void rejectPromise(JsRealm realm, PromiseObject promise, JsValue reason) {
        List<PromiseReaction> reactions = promise.rejectReactions();
        promise.settle(PromiseObject.REJECTED, reason);
        if (!promise.isHandled()) {
            PromiseRejectionTracker tracker = PromiseJobs.getRejectionTracker(realm);
            if (tracker != null) {
                tracker.rejectedWithoutHandler(realm, promise, reason);
            }
        }
        triggerReactions(realm, reactions, reason);
    }

    /**
     * TriggerPromiseReactions (25.4.1.8): puts one PromiseReactionJob per reaction on the job queue.
     * <p>
     * TriggerPromiseReactions (25.4.1.8): 반응마다 PromiseReactionJob 하나를 작업 큐에 넣는다.
     *
     * @param realm     the realm / realm
     * @param reactions the reactions / 반응 목록
     * @param argument  the value or reason / 값 또는 이유
     */
    private static void triggerReactions(JsRealm realm, List<PromiseReaction> reactions, JsValue argument) {
        // 등록된 순서대로 반응 작업을 큐에 넣는다.
        for (int i = 0; i < reactions.size(); i++) {
            enqueueReaction(realm, reactions.get(i), argument);
        }
    }

    /**
     * Queues a PromiseReactionJob (25.4.2.1).
     * <p>
     * PromiseReactionJob 을 큐에 넣는다(25.4.2.1).
     *
     * @param realm    the realm / realm
     * @param reaction the reaction / 반응
     * @param argument the value or reason / 값 또는 이유
     */
    private static void enqueueReaction(final JsRealm realm, final PromiseReaction reaction, final JsValue argument) {
        PromiseJobs.enqueue(realm, new Runnable() {
            public void run() {
                runReaction(realm, reaction, argument);
            }
        });
    }

    /**
     * The body of PromiseReactionJob: runs the handler (or the default identity/thrower) and settles the derived promise with its result
     * or error.
     * <p>
     * PromiseReactionJob 의 본문: 처리기(또는 기본 동작)를 실행하고 그 결과나 오류로 파생 promise 를 확정한다.
     *
     * @param realm    the realm / realm
     * @param reaction the reaction / 반응
     * @param argument the value or reason / 값 또는 이유
     */
    private static void runReaction(JsRealm realm, PromiseReaction reaction, JsValue argument) {
        JsValue handlerResult;
        boolean threw = false;
        if (reaction.handler.isUndefined()) {
            handlerResult = argument;
            threw = !reaction.fulfill;
        } else {
            try {
                handlerResult = Ops.call(reaction.handler, Js.UNDEFINED, new JsValue[] { argument });
            } catch (JsException e) {
                handlerResult = e.getValue(realm);
                threw = true;
            }
        }
        if (reaction.capability == null) {
            return;
        }
        if (threw) {
            reaction.capability.reject(handlerResult);
        } else {
            reaction.capability.resolve(handlerResult);
        }
    }

    /**
     * NewPromiseCapability (25.4.1.5): constructs a promise with the given constructor and captures its resolve and reject
     * functions through an executor.
     * <p>
     * NewPromiseCapability (25.4.1.5): 주어진 생성자로 promise 를 만들고 실행자를 통해 resolve, reject 함수를 가로챈다.
     *
     * @param realm the realm / realm
     * @param c     the constructor / 생성자
     * @return the capability / capability
     */
    static PromiseCapability newCapability(JsRealm realm, JsValue c) {
        if (!Ops.isConstructor(c)) {
            throw JsException.typeError(JsException.describe(c) + " is not a constructor");
        }
        final JsValue[] slots = new JsValue[] { Js.UNDEFINED, Js.UNDEFINED };
        NativeFunction executor = new NativeFunction(realm, "", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!slots[0].isUndefined() || !slots[1].isUndefined()) {
                    throw JsException.typeError("Promise executor has already been invoked with non-undefined arguments");
                }
                slots[0] = Js.arg(args, 0);
                slots[1] = Js.arg(args, 1);
                return Js.UNDEFINED;
            }
        });
        JsObject promise = Ops.construct(c, new JsValue[] { executor });
        if (!slots[0].isCallable()) {
            throw JsException.typeError("Promise resolve or reject function is not callable");
        }
        if (!slots[1].isCallable()) {
            throw JsException.typeError("Promise resolve or reject function is not callable");
        }
        return new PromiseCapability(promise, slots[0], slots[1]);
    }

    /**
     * PerformPromiseThen (25.4.5.3.1): registers the handlers; if the promise is already settled a reaction job is queued at once.
     * <p>
     * PerformPromiseThen (25.4.5.3.1): 처리기를 등록한다. promise 가 이미 확정되었으면 반응 작업을 곧바로 큐에 넣는다.
     *
     * @param realm       the realm / realm
     * @param promise     the promise / promise
     * @param onFulfilled fulfillment handler (non-callables are ignored) / 이행 처리기(호출 불가 값은 무시)
     * @param onRejected  rejection handler (non-callables are ignored) / 거부 처리기(호출 불가 값은 무시)
     * @param capability  capability of the derived promise or null / 파생 promise 의 capability 또는 null
     */
    static void performThen(JsRealm realm, PromiseObject promise, JsValue onFulfilled, JsValue onRejected, PromiseCapability capability) {
        PromiseReaction fulfillReaction = new PromiseReaction(capability, true, onFulfilled.isCallable() ? onFulfilled : Js.UNDEFINED);
        PromiseReaction rejectReaction = new PromiseReaction(capability, false, onRejected.isCallable() ? onRejected : Js.UNDEFINED);
        if (promise.getState() == PromiseObject.PENDING) {
            promise.fulfillReactions().add(fulfillReaction);
            promise.rejectReactions().add(rejectReaction);
        } else if (promise.getState() == PromiseObject.FULFILLED) {
            enqueueReaction(realm, fulfillReaction, promise.getResult());
        } else {
            if (!promise.isHandled()) {
                PromiseRejectionTracker tracker = PromiseJobs.getRejectionTracker(realm);
                if (tracker != null) {
                    tracker.handlerAddedToRejectedPromise(realm, promise);
                }
            }
            enqueueReaction(realm, rejectReaction, promise.getResult());
        }
        promise.markHandled();
    }

    /**
     * Resolves a value using an explicit Promise constructor (PromiseResolve).
     * <p>
     * 지정한 Promise 생성자로 값을 해결한다(PromiseResolve).
     *
     * @param realm the current realm / 현재 realm
     * @param constructor the Promise constructor / Promise 생성자
     * @param value the value to resolve / 해결할 값
     * @return the result promise / 결과 Promise
     */
    static JsObject resolveWith(JsRealm realm, JsValue constructor, JsValue value) {
        if (!(constructor instanceof JsObject)) {
            throw JsException.typeError("PromiseResolve called on non-object");
        }
        if (value instanceof PromiseObject) {
            JsObject promise = (JsObject) value;
            if (Ops.sameValue(promise.get("constructor"), constructor)) {
                return promise;
            }
        }
        PromiseCapability capability = newCapability(realm, constructor);
        capability.resolve(value);
        return capability.getPromise();
    }

    /**
     * SpeciesConstructor (7.3.20): the constructor to use for derived objects.
     * <p>
     * SpeciesConstructor (7.3.20): 파생 객체를 만들 때 쓸 생성자.
     *
     * @param o              the object / 객체
     * @param defaultCtor    the constructor to use if no species is set / species 가 없을 때 쓸 생성자
     * @return the constructor / 생성자
     */
    static JsValue speciesConstructor(JsObject o, JsValue defaultCtor) {
        JsValue c = o.get("constructor");
        if (c.isUndefined()) {
            return defaultCtor;
        }
        if (!(c instanceof JsObject)) {
            throw JsException.typeError("The 'constructor' property is not an object");
        }
        JsValue s = ((JsObject) c).get(WellKnownSymbols.SPECIES);
        if (s.isNullOrUndefined()) {
            return defaultCtor;
        }
        if (Ops.isConstructor(s)) {
            return s;
        }
        throw JsException.typeError("object.constructor[Symbol.species] is not a constructor");
    }

    /**
     * Requires a promise as this value.
     * <p>
     * this 값이 promise 임을 요구한다.
     *
     * @param thisValue the this value / this 값
     * @param method    method name for the message / 메시지에 쓸 메서드 이름
     * @return the promise / promise
     */
    private static PromiseObject thisPromise(JsValue thisValue, String method) {
        if (!(thisValue instanceof PromiseObject)) {
            throw JsException.typeError("Method Promise.prototype." + method + " called on incompatible receiver " + JsException.describe(thisValue));
        }
        return (PromiseObject) thisValue;
    }

    // ------------------------------------------------------------------ installation

    /**
     * Creates the constructor, the prototype and the static methods and defines them.
     * <p>
     * 생성자, 프로토타입, 정적 메서드를 만들어 정의한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        final JsObject proto = realm.newObject();
        NativeFunction ctor = new NativeFunction(realm, "Promise", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("Promise constructor cannot be invoked without 'new'");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsValue executor = Js.arg(args, 0);
                if (!executor.isCallable()) {
                    throw JsException.typeError("Promise resolver " + JsException.describe(executor) + " is not a function");
                }
                PromiseObject promise = new PromiseObject(Ops.getPrototypeFromConstructor(r, newTarget, proto));
                NativeFunction[] fns = createResolvingFunctions(r, promise);
                try {
                    Ops.call(executor, Js.UNDEFINED, new JsValue[] { fns[0], fns[1] });
                } catch (JsException e) {
                    Ops.call(fns[1], Js.UNDEFINED, new JsValue[] { e.getValue(r) });
                }
                return promise;
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("Promise"), false, false, true);

        // Promise.all(iterable) (25.4.4.1)
        realm.method(ctor, "all", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return all(r, thisValue, Js.arg(args, 0));
            }
        });
        realm.method(ctor, "allSettled", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return allSettled(r, thisValue, Js.arg(args, 0));
            }
        });
        realm.method(ctor, "any", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return any(r, thisValue, Js.arg(args, 0));
            }
        });
        // Promise.race(iterable) (25.4.4.3)
        realm.method(ctor, "race", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return race(r, thisValue, Js.arg(args, 0));
            }
        });
        // Promise.reject(r) (25.4.4.4)
        realm.method(ctor, "reject", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (!(thisValue instanceof JsObject)) {
                    throw JsException.typeError("PromiseReject called on non-object");
                }
                PromiseCapability cap = newCapability(r, thisValue);
                cap.reject(Js.arg(args, 0));
                return cap.getPromise();
            }
        });
        // Promise.resolve(x) (25.4.4.5)
        realm.method(ctor, "resolve", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return resolveWith(r, thisValue, Js.arg(args, 0));
            }
        });
        realm.method(ctor, "withResolvers", 0, new NativeFunction.Call() {
            /** Returns the new promise and its resolving functions. / 새 promise와 해결 함수들을 반환한다. */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                PromiseCapability capability = newCapability(r, thisValue);
                JsObject result = r.newObject();
                result.createDataProperty("promise", capability.getPromise());
                result.createDataProperty("resolve", capability.getResolveFunction());
                result.createDataProperty("reject", capability.getRejectFunction());
                return result;
            }
        });
        realm.method(ctor, "try", 1, new NativeFunction.Call() {
            /** Calls the callback immediately and settles the new promise from its completion. / 콜백을 즉시 호출하고 완료 값을 새 promise에 전달한다. */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                PromiseCapability capability = newCapability(r, thisValue);
                JsValue[] callbackArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : Js.NO_ARGS;
                JsValue result;
                try {
                    result = Ops.call(Js.arg(args, 0), Js.UNDEFINED, callbackArgs);
                } catch (JsException called) {
                    // 콜백의 예외를 거부 이유로 넘긴다. 해결 함수 자체의 예외는 호출자에게 전파한다.
                    capability.reject(called.getValue(r));
                    return capability.getPromise();
                }
                capability.resolve(result);
                return capability.getPromise();
            }
        });
        // get Promise[@@species] (25.4.4.6)
        realm.getter(ctor, WellKnownSymbols.SPECIES, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisValue;
            }
        });

        // Promise.prototype.catch(onRejected) (25.4.5.1)
        realm.method(proto, "catch", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Ops.invoke(r, thisValue, "then", new JsValue[] { Js.UNDEFINED, Js.arg(args, 0) });
            }
        });
        realm.method(proto, "finally", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return promiseFinally(r, thisValue, Js.arg(args, 0));
            }
        });
        // Promise.prototype.then(onFulfilled, onRejected) (25.4.5.3)
        realm.method(proto, "then", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                PromiseObject promise = thisPromise(thisValue, "then");
                JsValue c = speciesConstructor(promise, r.getIntrinsic(INTRINSIC_PROMISE));
                PromiseCapability cap = newCapability(r, c);
                performThen(r, promise, Js.arg(args, 0), Js.arg(args, 1), cap);
                return cap.getPromise();
            }
        });

        realm.setIntrinsic(INTRINSIC_PROMISE, ctor);
        realm.setIntrinsic(INTRINSIC_PROTOTYPE, proto);
        realm.getGlobalObject().defineBuiltin("Promise", ctor);
    }

    // ------------------------------------------------------------------ Promise.all and Promise.race

    /**
     * Promise.all (25.4.4.1, PerformPromiseAll 25.4.4.1.1): resolves with an array of the values once every promise from the
     * iterable fulfilled, rejects with the first rejection. Errors while iterating close the iterator (unless it ended itself)
     * and reject the result promise.
     * <p>
     * Promise.all (25.4.4.1, PerformPromiseAll 25.4.4.1.1): 이터러블의 모든 promise 가 이행되면 값 배열로 이행하고 첫 거부로 거부한다.
     * 순회 중 오류는 이터레이터를 닫고(스스로 끝난 경우 제외) 결과 promise 를 거부한다.
     *
     * @param realm    the realm / realm
     * @param c        the constructor (this value) / 생성자(this 값)
     * @param iterable the iterable / 이터러블
     * @return the result promise / 결과 promise
     */
    private static JsValue all(final JsRealm realm, JsValue c, JsValue iterable) {
        if (!(c instanceof JsObject)) {
            throw JsException.typeError("Promise.all called on non-object");
        }
        final PromiseCapability cap = newCapability(realm, c);
        JsObject iterator;
        try {
            iterator = IteratorHelper.getIterator(realm, iterable);
        } catch (JsException e) {
            cap.reject(e.getValue(realm));
            return cap.getPromise();
        }
        final List<JsValue> values = new ArrayList<JsValue>();
        final int[] remaining = new int[] { 1 };
        boolean iteratorDone = false;
        try {
            int index = 0;
            // 이터러블이 끝날 때까지 요소마다 resolve 한 promise 에 then 으로 결과 수집 함수를 건다.
            while (true) {
                JsObject next;
                try {
                    next = IteratorHelper.step(iterator);
                } catch (JsException e) {
                    iteratorDone = true;
                    throw e;
                }
                if (next == null) {
                    iteratorDone = true;
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        cap.resolve(realm.newArray(values));
                    }
                    return cap.getPromise();
                }
                JsValue nextValue;
                try {
                    nextValue = IteratorHelper.value(next);
                } catch (JsException e) {
                    iteratorDone = true;
                    throw e;
                }
                values.add(Js.UNDEFINED);
                JsValue nextPromise = Ops.invoke(realm, c, "resolve", new JsValue[] { nextValue });
                final int slot = index;
                final boolean[] alreadyCalled = new boolean[1];
                NativeFunction resolveElement = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                        if (alreadyCalled[0]) {
                            return Js.UNDEFINED;
                        }
                        alreadyCalled[0] = true;
                        values.set(slot, Js.arg(args, 0));
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            cap.resolve(r.newArray(values));
                        }
                        return Js.UNDEFINED;
                    }
                });
                remaining[0]++;
                Ops.invoke(realm, nextPromise, "then", new JsValue[] { resolveElement, cap.getRejectFunction() });
                index++;
            }
        } catch (JsException e) {
            if (!iteratorDone) {
                IteratorHelper.closeQuietly(iterator);
            }
            cap.reject(e.getValue(realm));
            return cap.getPromise();
        }
    }

    /**
     * Promise.race (25.4.4.3, PerformPromiseRace 25.4.4.3.1): settles like the first promise of the iterable that settles.
     * <p>
     * Promise.race (25.4.4.3, PerformPromiseRace 25.4.4.3.1): 이터러블 중 가장 먼저 확정되는 promise 와 같게 확정된다.
     *
     * @param realm    the realm / realm
     * @param c        the constructor (this value) / 생성자(this 값)
     * @param iterable the iterable / 이터러블
     * @return the result promise / 결과 promise
     */
    private static JsValue race(JsRealm realm, JsValue c, JsValue iterable) {
        if (!(c instanceof JsObject)) {
            throw JsException.typeError("Promise.race called on non-object");
        }
        PromiseCapability cap = newCapability(realm, c);
        JsObject iterator;
        try {
            iterator = IteratorHelper.getIterator(realm, iterable);
        } catch (JsException e) {
            cap.reject(e.getValue(realm));
            return cap.getPromise();
        }
        boolean iteratorDone = false;
        try {
            // 이터러블의 모든 요소를 결과 promise 의 resolve/reject 에 연결한다.
            while (true) {
                JsObject next;
                try {
                    next = IteratorHelper.step(iterator);
                } catch (JsException e) {
                    iteratorDone = true;
                    throw e;
                }
                if (next == null) {
                    iteratorDone = true;
                    return cap.getPromise();
                }
                JsValue nextValue;
                try {
                    nextValue = IteratorHelper.value(next);
                } catch (JsException e) {
                    iteratorDone = true;
                    throw e;
                }
                JsValue nextPromise = Ops.invoke(realm, c, "resolve", new JsValue[] { nextValue });
                Ops.invoke(realm, nextPromise, "then", new JsValue[] { cap.getResolveFunction(), cap.getRejectFunction() });
            }
        } catch (JsException e) {
            if (!iteratorDone) {
                IteratorHelper.closeQuietly(iterator);
            }
            cap.reject(e.getValue(realm));
            return cap.getPromise();
        }
    }

    /**
     * Promise.allSettled: resolves after every input settles and preserves values and reasons in order.
     * <p>
     * Promise.allSettled: 모든 입력이 확정된 뒤 값과 거부 이유를 순서대로 보존한다.
     *
     * @param realm the realm / realm
     * @param c the Promise constructor / Promise 생성자
     * @param iterable the inputs / 입력들
     * @return the aggregate promise / 결과 promise
     */
    private static JsValue allSettled(final JsRealm realm, JsValue c, JsValue iterable) {
        if (!(c instanceof JsObject)) {
            throw JsException.typeError("Promise.allSettled called on non-object");
        }
        final PromiseCapability cap = newCapability(realm, c);
        final List<JsValue> values = new ArrayList<JsValue>();
        final int[] remaining = new int[] { 1 };
        JsObject iterator;
        try {
            iterator = IteratorHelper.getIterator(realm, iterable);
        } catch (JsException e) {
            cap.reject(e.getValue(realm));
            return cap.getPromise();
        }
        boolean iteratorDone = false;
        try {
            // 이터러블의 promise 마다 상태 객체 기록 함수를 연결한다.
            while (true) {
                JsObject next = IteratorHelper.step(iterator);
                if (next == null) {
                    iteratorDone = true;
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        cap.resolve(realm.newArray(values));
                    }
                    return cap.getPromise();
                }
                final int slot = values.size();
                values.add(Js.UNDEFINED);
                JsValue p = Ops.invoke(realm, c, "resolve", new JsValue[] { IteratorHelper.value(next) });
                final boolean[] called = new boolean[] { false };
                NativeFunction fulfilled = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                        if (!called[0]) {
                            called[0] = true;
                            values.set(slot, recordAllSettled(r, true, Js.arg(args, 0)));
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                cap.resolve(r.newArray(values));
                            }
                        }
                        return Js.UNDEFINED;
                    }
                });
                NativeFunction rejected = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                        if (!called[0]) {
                            called[0] = true;
                            values.set(slot, recordAllSettled(r, false, Js.arg(args, 0)));
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                cap.resolve(r.newArray(values));
                            }
                        }
                        return Js.UNDEFINED;
                    }
                });
                remaining[0]++;
                Ops.invoke(realm, p, "then", new JsValue[] { fulfilled, rejected });
            }
        } catch (JsException e) {
            if (!iteratorDone) {
                IteratorHelper.closeQuietly(iterator);
            }
            cap.reject(e.getValue(realm));
            return cap.getPromise();
        }
    }

    /**
     * Creates one Object record for Promise.allSettled.
     * <p>
     * Promise.allSettled 의 결과 객체 하나를 만든다.
     *
     * @param realm the realm / realm
     * @param fulfilled true for a fulfilled record / 이행 기록이면 true
     * @param value value or reason / 값 또는 이유
     * @return the record / 기록 객체
     */
    private static JsObject recordAllSettled(JsRealm realm, boolean fulfilled, JsValue value) {
        JsObject record = realm.newObject();
        record.createDataProperty("status", Js.str(fulfilled ? "fulfilled" : "rejected"));
        record.createDataProperty(fulfilled ? "value" : "reason", value);
        return record;
    }

    /**
     * Promise.any: fulfills with the first fulfillment and rejects with AggregateError after all inputs reject.
     * <p>
     * Promise.any: 첫 이행으로 이행하고 모든 입력이 거부되면 AggregateError 로 거부한다.
     *
     * @param realm the realm / realm
     * @param c the Promise constructor / Promise 생성자
     * @param iterable the inputs / 입력들
     * @return the result promise / 결과 promise
     */
    private static JsValue any(final JsRealm realm, JsValue c, JsValue iterable) {
        if (!(c instanceof JsObject)) {
            throw JsException.typeError("Promise.any called on non-object");
        }
        final PromiseCapability cap = newCapability(realm, c);
        final List<JsValue> errors = new ArrayList<JsValue>();
        final int[] remaining = new int[] { 1 };
        JsObject iterator;
        try {
            iterator = IteratorHelper.getIterator(realm, iterable);
        } catch (JsException e) {
            cap.reject(e.getValue(realm));
            return cap.getPromise();
        }
        boolean iteratorDone = false;
        try {
            // 첫 이행 또는 마지막 거부를 결정하도록 promise 마다 반응을 건다.
            while (true) {
                JsObject next = IteratorHelper.step(iterator);
                if (next == null) {
                    iteratorDone = true;
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        rejectAnyAll(realm, cap, errors);
                    }
                    return cap.getPromise();
                }
                final int slot = errors.size();
                errors.add(Js.UNDEFINED);
                JsValue p = Ops.invoke(realm, c, "resolve", new JsValue[] { IteratorHelper.value(next) });
                final boolean[] called = new boolean[1];
                NativeFunction rejected = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                        if (!called[0]) {
                            called[0] = true;
                            errors.set(slot, Js.arg(args, 0));
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                rejectAnyAll(r, cap, errors);
                            }
                        }
                        return Js.UNDEFINED;
                    }
                });
                remaining[0]++;
                Ops.invoke(realm, p, "then", new JsValue[] { cap.getResolveFunction(), rejected });
            }
        } catch (JsException e) {
            if (!iteratorDone) {
                IteratorHelper.closeQuietly(iterator);
            }
            cap.reject(e.getValue(realm));
            return cap.getPromise();
        }
    }

    /**
     * Rejects Promise.any with an AggregateError containing the ordered rejection reasons.
     * <p>
     * 순서대로 보존한 거부 이유를 AggregateError 에 담아 Promise.any 를 거부한다.
     *
     * @param realm the realm / realm
     * @param capability the promise capability / promise capability
     * @param errors rejection reasons / 거부 이유들
     */
    private static void rejectAnyAll(JsRealm realm, PromiseCapability capability, List<JsValue> errors) {
        JsValue aggregate = realm.getIntrinsic("%AggregateError%");
        if (aggregate == null) {
            throw new IllegalStateException("AggregateError intrinsic is not installed");
        }
        JsObject error = Ops.construct(aggregate, new JsValue[] { realm.newArray(errors), Js.str("All promises were rejected") });
        capability.reject(error);
    }

    /**
     * Implements Promise.prototype.finally without changing fulfillment values or rejection reasons.
     * <p>
     * 이행 값이나 거부 이유를 바꾸지 않고 Promise.prototype.finally 를 구현한다.
     *
     * @param realm the realm / realm
     * @param receiver the receiver / 수신 객체
     * @param onFinally finally callback / finally 처리기
     * @return the result of then / then 의 결과
     */
    private static JsValue promiseFinally(final JsRealm realm, JsValue receiver, final JsValue onFinally) {
        if (!(receiver instanceof JsObject)) {
            throw JsException.typeError("Method Promise.prototype.finally called on incompatible receiver");
        }
        final JsValue c = speciesConstructor((JsObject) receiver, realm.getIntrinsic(INTRINSIC_PROMISE));
        if (!onFinally.isCallable()) {
            return Ops.invoke(realm, receiver, "then", new JsValue[] { onFinally, onFinally });
        }
        NativeFunction fulfilled = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                final JsValue value = Js.arg(args, 0);
                JsValue cleanup = Ops.call(onFinally, Js.UNDEFINED, Js.NO_ARGS);
                JsValue resolved = Ops.invoke(r, c, "resolve", new JsValue[] { cleanup });
                return Ops.invoke(r, resolved, "then", new JsValue[] { new NativeFunction(r, "", 0, new NativeFunction.Call() {
                    public JsValue call(JsRealm rr, JsValue ignored, JsValue[] ignoredArgs) {
                        return value;
                    }
                }) });
            }
        });
        NativeFunction rejected = new NativeFunction(realm, "", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                final JsValue reason = Js.arg(args, 0);
                JsValue cleanup = Ops.call(onFinally, Js.UNDEFINED, Js.NO_ARGS);
                JsValue resolved = Ops.invoke(r, c, "resolve", new JsValue[] { cleanup });
                return Ops.invoke(r, resolved, "then", new JsValue[] { new NativeFunction(r, "", 0, new NativeFunction.Call() {
                    public JsValue call(JsRealm rr, JsValue ignored, JsValue[] ignoredArgs) {
                        throw new JsException(reason);
                    }
                }) });
            }
        });
        return Ops.invoke(realm, receiver, "then", new JsValue[] { fulfilled, rejected });
    }
}
