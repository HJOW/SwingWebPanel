package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.SimpleJobQueue;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of Promise (ECMA-262 6th edition 25.4): the shape of the constructor and prototype, resolving functions, reaction and thenable
 * jobs and their order on the job queue, chaining, error handling, Promise.all/race/resolve/reject, subclassing through
 * <code>Symbol.species</code>, and the host rejection tracker. The job queue is drained explicitly, so the tick order of every
 * case is deterministic.
 * <p>
 * Promise 시험(ECMA-262 6판 25.4): 생성자와 프로토타입의 모양, 해결 함수, 반응 작업과 thenable 작업 및 작업 큐에서의 순서, 연쇄, 오류 처리,
 * Promise.all/race/resolve/reject, <code>Symbol.species</code> 를 통한 하위 클래스, 호스트 거부 추적기. 작업 큐를 직접 비우므로 사례마다
 * 틱 순서가 결정적이다.
 */
public class PromiseTest {

    /**
     * The environment of the running case.
     * <p>
     * 실행 중인 사례의 환경.
     */
    private MetaTestSupport.Env e;

    /**
     * The Promise constructor.
     * <p>
     * Promise 생성자.
     */
    private JsObject promiseCtor;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PromiseTest() {
    }

    /**
     * Creates a fresh realm for each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        e = new MetaTestSupport.Env();
        promiseCtor = (JsObject) e.global("Promise");
    }

    /**
     * A promise together with its resolving functions, captured from an executor.
     * <p>
     * 실행자에서 가로챈 해결 함수와 promise.
     */
    private static final class Deferred {
        /**
         * The promise object.
         * promise 객체.
         * promise.
         */
        JsObject promise;

        /**
         * The resolve function.
         * <p>
         * resolve 함수.
         */
        JsValue resolve;

        /**
         * The reject function.
         * <p>
         * reject 함수.
         */
        JsValue reject;
    }

    /**
     * Creates a pending promise and captures its resolving functions.
     * <p>
     * 대기 중인 promise 를 만들고 해결 함수를 가로챈다.
     *
     * @return the deferred / 가로챈 값
     */
    private Deferred deferred() {
        final Deferred d = new Deferred();
        d.promise = Ops.construct(promiseCtor, MetaTestSupport.args(e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                d.resolve = args[0];
                d.reject = args[1];
                return Js.UNDEFINED;
            }
        })));
        return d;
    }

    /**
     * Calls a function value with one argument.
     * <p>
     * 함수 값을 인수 하나로 호출한다.
     *
     * @param f   the function / 함수
     * @param arg the argument / 인수
     */
    private void call1(JsValue f, JsValue arg) {
        Ops.call(f, Js.UNDEFINED, MetaTestSupport.args(arg));
    }

    /**
     * Returns the state of a promise.
     * <p>
     * promise 의 상태를 반환한다.
     *
     * @param p the promise / promise
     * @return the state / 상태
     */
    private int state(JsValue p) {
        return ((PromiseObject) p).getState();
    }

    /**
     * Calls promise.then(onFulfilled, onRejected).
     * <p>
     * promise.then(onFulfilled, onRejected) 를 호출한다.
     *
     * @param p           the promise / promise
     * @param onFulfilled the fulfillment handler / 이행 처리기
     * @param onRejected  the rejection handler / 거부 처리기
     * @return the derived promise / 파생 promise
     */
    private JsObject then(JsValue p, JsValue onFulfilled, JsValue onRejected) {
        return (JsObject) e.invoke(p, "then", onFulfilled, onRejected);
    }

    /**
     * 25.4.3 - 25.4.5: names, lengths and attributes of the constructor and its prototype.
     * <p>
     * 25.4.3 - 25.4.5: 생성자와 프로토타입의 이름, length, 속성.
     */
    public void testShape() {
        Check.isTrue(promiseCtor.isConstructor(), "Promise is a constructor");
        Check.equal("Promise", promiseCtor.getString("name"), "name");
        Check.equal(1.0, ((JsNumber) promiseCtor.get("length")).getValue(), "length");
        JsObject proto = (JsObject) promiseCtor.get("prototype");
        PropertyDescriptor d = promiseCtor.getOwnProperty("prototype");
        Check.isTrue(!d.isWritableTrue() && !d.isEnumerableTrue() && !d.isConfigurableTrue(), "prototype attributes");
        Check.same(promiseCtor, proto.get("constructor"), "prototype.constructor");
        PropertyDescriptor tag = proto.getOwnProperty(WellKnownSymbols.TO_STRING_TAG);
        Check.equal("Promise", ((org.duckdns.hjow.swingwebpanel.js.JsString) tag.value).getValue(), "@@toStringTag value");
        Check.isTrue(!tag.isWritableTrue() && !tag.isEnumerableTrue() && tag.isConfigurableTrue(), "@@toStringTag attributes");
        String[] statics = { "all", "race", "reject", "resolve" };
        // 정적 메서드는 모두 length 1 이다.
        for (int i = 0; i < statics.length; i++) {
            JsObject f = (JsObject) promiseCtor.get(statics[i]);
            Check.equal(1.0, ((JsNumber) f.get("length")).getValue(), "Promise." + statics[i] + ".length");
            Check.isFalse(f.isConstructor(), "Promise." + statics[i] + " is not a constructor");
        }
        Check.equal(2.0, ((JsNumber) ((JsObject) proto.get("then")).get("length")).getValue(), "then.length");
        Check.equal(1.0, ((JsNumber) ((JsObject) proto.get("catch")).get("length")).getValue(), "catch.length");
        PropertyDescriptor species = promiseCtor.getOwnProperty(WellKnownSymbols.SPECIES);
        Check.isTrue(species.isAccessor() && species.set.isUndefined(), "@@species is a getter-only accessor");
        Check.equal("get [Symbol.species]", ((JsObject) species.get).getString("name"), "species getter name");
        Check.same(promiseCtor, Ops.call(species.get, promiseCtor, Js.NO_ARGS), "Promise[@@species] returns this");
        Check.same(proto, e.realm.getIntrinsic("%PromisePrototype%"), "intrinsic %PromisePrototype%");
        Check.same(promiseCtor, e.realm.getIntrinsic("%Promise%"), "intrinsic %Promise%");
    }

    /**
     * 25.4.3.1: the constructor needs new and a callable executor.
     * <p>
     * 25.4.3.1: 생성자는 new 와 호출 가능한 실행자를 요구한다.
     */
    public void testConstructorArguments() {
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(promiseCtor, Js.UNDEFINED, MetaTestSupport.args(e.fn(2, new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                        return Js.UNDEFINED;
                    }
                })));
            }
        }, "Promise() without new");
        JsValue[] bad = { Js.UNDEFINED, MetaTestSupport.n(1), e.obj(), Js.NULL };
        // 호출 가능하지 않은 실행자는 모두 TypeError 이다.
        for (int i = 0; i < bad.length; i++) {
            final JsValue executor = bad[i];
            MetaTestSupport.typeError(new Block() {
                public void run() {
                    Ops.construct(promiseCtor, MetaTestSupport.args(executor));
                }
            }, "executor #" + i);
        }
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.construct(promiseCtor, Js.NO_ARGS);
            }
        }, "no executor");
    }

    /**
     * 25.4.3.1: the executor runs synchronously and receives two functions of length 1; reactions never run synchronously.
     * <p>
     * 25.4.3.1: 실행자는 동기적으로 실행되며 length 1 인 함수 두 개를 받는다. 반응은 동기적으로 실행되지 않는다.
     */
    public void testExecutorRunsSynchronouslyButReactionsDoNot() {
        final JsValue[] fns = new JsValue[2];
        JsObject p = Ops.construct(promiseCtor, MetaTestSupport.args(e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                e.log.add("executor");
                fns[0] = args[0];
                fns[1] = args[1];
                call1(args[0], MetaTestSupport.n(1));
                return Js.UNDEFINED;
            }
        })));
        e.log.add("after-construct");
        then(p, e.logger("fulfilled"), e.logger("rejected"));
        e.log.add("after-then");
        Check.equal("executor,after-construct,after-then", e.logText(), "nothing ran inside then");
        e.drain();
        Check.equal("executor,after-construct,after-then,fulfilled:1", e.logText(), "the reaction ran when the queue was drained");
        Check.equal(2, fns.length, "two resolving functions");
        Check.equal(1.0, ((JsNumber) ((JsObject) fns[0]).get("length")).getValue(), "resolve.length");
        Check.equal("", ((JsObject) fns[1]).getString("name"), "reject.name is empty");
        Check.isFalse(((JsObject) fns[0]).isConstructor(), "resolving functions are not constructors");
    }

    /**
     * 25.4.1.3 [[AlreadyResolved]]: only the first call of resolve or reject counts.
     * <p>
     * 25.4.1.3 [[AlreadyResolved]]: resolve 나 reject 의 첫 호출만 유효하다.
     */
    public void testResolveFunctionsWorkOnce() {
        Deferred d = deferred();
        then(d.promise, e.logger("ok"), e.logger("err"));
        call1(d.resolve, MetaTestSupport.n(1));
        call1(d.resolve, MetaTestSupport.n(2));
        call1(d.reject, MetaTestSupport.n(3));
        e.drain();
        Check.equal("ok:1", e.logText(), "only the first resolution");
        Check.equal(PromiseObject.FULFILLED, state(d.promise), "state");
        Check.equal(1.0, ((JsNumber) ((PromiseObject) d.promise).getResult()).getValue(), "result");
        Deferred r = deferred();
        then(r.promise, e.logger("ok2"), e.logger("err2"));
        call1(r.reject, MetaTestSupport.n(4));
        call1(r.resolve, MetaTestSupport.n(5));
        e.drain();
        Check.equal("ok:1,err2:4", e.logText(), "rejection first, later resolve ignored");
        Check.equal(PromiseObject.REJECTED, state(r.promise), "state");
    }

    /**
     * 25.4.5.3: callbacks of one promise run in registration order; a callback added after settlement still runs asynchronously.
     * <p>
     * 25.4.5.3: 한 promise 의 콜백은 등록 순서대로 실행되며 확정 뒤에 추가한 콜백도 비동기로 실행된다.
     */
    public void testReactionOrder() {
        Deferred d = deferred();
        then(d.promise, e.logger("a"), null_());
        then(d.promise, e.logger("b"), null_());
        call1(d.resolve, MetaTestSupport.n(1));
        then(d.promise, e.logger("c"), null_());
        e.log.add("sync");
        e.drain();
        Check.equal("sync,a:1,b:1,c:1", e.logText(), "registration order, all after the synchronous code");
        JsObject p1 = (JsObject) e.invoke(promiseCtor, "resolve", MetaTestSupport.n(1));
        JsObject p2 = (JsObject) e.invoke(promiseCtor, "resolve", MetaTestSupport.n(2));
        e.log.clear();
        then(p2, e.logger("second"), null_());
        then(p1, e.logger("first"), null_());
        e.drain();
        Check.equal("second:2,first:1", e.logText(), "the order of registration decides, not the order of settlement");
    }

    /**
     * Shorthand for an absent handler.
     * <p>
     * 없는 처리기의 약식 표기.
     *
     * @return undefined / undefined
     */
    private JsValue null_() {
        return Js.UNDEFINED;
    }

    /**
     * 25.4.5.3: then returns a new promise; handler results become its value; thrown values reject it; missing handlers pass the
     * value or reason through; non-callable handlers are ignored.
     * <p>
     * 25.4.5.3: then 은 새 promise 를 돌려주고, 처리기 결과가 그 값이 되며, 던진 값은 거부가 되고, 없는 처리기는 값이나 이유를 그대로
     * 전달하며, 호출할 수 없는 처리기는 무시한다.
     */
    public void testChaining() {
        JsObject p = (JsObject) e.invoke(promiseCtor, "resolve", MetaTestSupport.n(1));
        JsObject a = then(p, e.loggerReturning("h1", MetaTestSupport.n(10)), null_());
        Check.isTrue(a != p, "then returns a new promise");
        Check.isTrue(a instanceof PromiseObject, "the result is a promise");
        JsObject b = then(a, e.loggerThrowing("h2", MetaTestSupport.s("boom")), null_());
        JsObject c = then(b, e.logger("skipped"), e.loggerReturning("h3", MetaTestSupport.s("recovered")));
        JsObject d = then(c, e.logger("h4"), null_());
        JsObject passFulfill = then(p, null_(), e.logger("never"));
        JsObject ignoredHandlers = then(p, MetaTestSupport.n(1), e.obj());
        e.drain();
        Check.equal("h1:1,h2:10,h3:boom,h4:recovered", e.logText(), "handler chain");
        Check.equal(PromiseObject.FULFILLED, state(d), "the chain ends fulfilled");
        Check.equal(PromiseObject.FULFILLED, state(passFulfill), "a missing fulfillment handler passes the value on");
        Check.equal(1.0, ((JsNumber) ((PromiseObject) passFulfill).getResult()).getValue(), "passed value");
        Check.equal(PromiseObject.FULFILLED, state(ignoredHandlers), "non-callable handlers are ignored");
        JsObject rej = (JsObject) e.invoke(promiseCtor, "reject", MetaTestSupport.n(7));
        JsObject passReject = then(rej, e.logger("never2"), null_());
        JsObject viaCatch = (JsObject) e.invoke(passReject, "catch", e.loggerReturning("caught", MetaTestSupport.n(8)));
        e.drain();
        Check.equal(PromiseObject.REJECTED, state(passReject), "a missing rejection handler passes the reason on");
        Check.equal(7.0, ((JsNumber) ((PromiseObject) passReject).getResult()).getValue(), "passed reason");
        Check.equal(8.0, ((JsNumber) ((PromiseObject) viaCatch).getResult()).getValue(), "catch handler result");
        Check.isTrue(e.logText().endsWith("caught:7"), "catch got the reason");
    }

    /**
     * 25.4.5.1: catch(f) is then(undefined, f) on whatever this value it was called on (any object with a then method).
     * <p>
     * 25.4.5.1: catch(f) 는 호출한 this 값(then 메서드가 있는 모든 객체)에 대한 then(undefined, f) 이다.
     */
    public void testCatchCallsThen() {
        final JsValue[] seen = new JsValue[3];
        JsObject fake = e.obj();
        fake.createDataProperty("then", e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                seen[0] = thisValue;
                seen[1] = args.length > 0 ? args[0] : null;
                seen[2] = args.length > 1 ? args[1] : null;
                return MetaTestSupport.s("then-result");
            }
        }));
        JsValue handler = e.fn(1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        });
        JsObject catchFn = (JsObject) ((JsObject) promiseCtor.get("prototype")).get("catch");
        JsValue result = Ops.call(catchFn, fake, MetaTestSupport.args(handler));
        Check.equal("then-result", ((org.duckdns.hjow.swingwebpanel.js.JsString) result).getValue(), "the result of then is returned");
        Check.same(fake, seen[0], "this value");
        Check.isTrue(seen[1].isUndefined(), "first argument is undefined");
        Check.same(handler, seen[2], "second argument is the handler");
        final JsObject thenFn = (JsObject) ((JsObject) promiseCtor.get("prototype")).get("then");
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(thenFn, e.obj(), MetaTestSupport.args(Js.UNDEFINED));
            }
        }, "then on a non-promise");
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(thenFn, MetaTestSupport.n(1), Js.NO_ARGS);
            }
        }, "then on a primitive");
    }

    /**
     * 25.4.1.3: resolving a promise with itself rejects it with a TypeError.
     * <p>
     * 25.4.1.3: promise 를 자기 자신으로 해결하면 TypeError 로 거부된다.
     */
    public void testSelfResolution() {
        Deferred d = deferred();
        call1(d.resolve, d.promise);
        Check.equal(PromiseObject.REJECTED, state(d.promise), "rejected at once");
        JsValue reason = ((PromiseObject) d.promise).getResult();
        Check.isTrue(reason instanceof JsObject, "the reason is an error object");
        Check.same(e.realm.getErrorPrototype(ErrorKind.TYPE_ERROR), ((JsObject) reason).getPrototypeOf(), "with a TypeError");
    }

    /**
     * 25.4.2.2: resolving with a thenable takes two extra ticks: this is the classic order of the chain a1..a4 and the promise b that was
     * resolved with an already fulfilled promise.
     * <p>
     * 25.4.2.2: thenable 로 해결하면 틱이 두 번 더 걸린다. 이미 이행된 promise 로 해결한 promise b 와 연쇄 a1..a4 의 고전적인 순서이다.
     */
    public void testThenableTakesTwoExtraTicks() {
        JsObject start = (JsObject) e.invoke(promiseCtor, "resolve");
        JsObject c1 = then(start, e.logger("a1"), null_());
        JsObject c2 = then(c1, e.logger("a2"), null_());
        JsObject c3 = then(c2, e.logger("a3"), null_());
        then(c3, e.logger("a4"), null_());
        final JsObject inner = (JsObject) e.invoke(promiseCtor, "resolve");
        JsObject p = Ops.construct(promiseCtor, MetaTestSupport.args(e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                call1(args[0], inner);
                return Js.UNDEFINED;
            }
        })));
        then(p, e.logger("b"), null_());
        Check.equal(PromiseObject.PENDING, state(p), "still pending right after resolve(promise)");
        e.drain();
        Check.equal("a1:undefined,a2:undefined,a3:undefined,b:undefined,a4:undefined", e.logText(), "b runs after a3: two ticks later than a plain value would");
    }

    /**
     * 25.4.1.3, 25.4.2.2: thenable objects: then is called asynchronously with the thenable as this; the first of resolve/reject counts;
     * exceptions before settling reject; exceptions after settling are ignored; a getter that throws rejects at once; a non-callable
     * then fulfills with the object.
     * <p>
     * 25.4.1.3, 25.4.2.2: thenable 객체: then 은 thenable 을 this 로 비동기 호출되고, resolve/reject 중 먼저 호출한 것이 유효하며, 확정 전
     * 예외는 거부, 확정 뒤 예외는 무시되고, 던지는 getter 는 즉시 거부하며 호출 불가 then 은 그 객체로 이행한다.
     */
    public void testThenableObjects() {
        final JsValue[] thenThis = new JsValue[1];
        final JsObject thenable = e.obj();
        thenable.createDataProperty("then", e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                thenThis[0] = thisValue;
                e.log.add("then-called");
                call1(args[0], MetaTestSupport.n(1));
                call1(args[0], MetaTestSupport.n(2));
                call1(args[1], MetaTestSupport.n(3));
                throw new JsException(MetaTestSupport.s("ignored"));
            }
        }));
        JsObject p = (JsObject) e.invoke(promiseCtor, "resolve", thenable);
        e.log.add("sync");
        Check.equal("sync", e.logText(), "then of the thenable is not called synchronously");
        then(p, e.logger("ok"), e.logger("err"));
        e.drain();
        Check.equal("sync,then-called,ok:1", e.logText(), "first resolve wins, later calls and exceptions are ignored");
        Check.same(thenable, thenThis[0], "this of then is the thenable");

        JsObject throwing = e.obj();
        throwing.createDataProperty("then", e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw new JsException(MetaTestSupport.s("early"));
            }
        }));
        JsObject q = (JsObject) e.invoke(promiseCtor, "resolve", throwing);
        then(q, e.logger("ok2"), e.logger("err2"));
        e.drain();
        Check.isTrue(e.logText().endsWith("err2:early"), "an exception before settling rejects");

        JsObject getterThrows = e.obj();
        getterThrows.defineAccessor("then", e.fn(0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw new JsException(MetaTestSupport.s("getter"));
            }
        }), null, true, true);
        JsObject g = (JsObject) e.invoke(promiseCtor, "resolve", getterThrows);
        Check.equal(PromiseObject.REJECTED, state(g), "a throwing then getter rejects immediately");
        JsObject plain = e.obj("then", MetaTestSupport.n(5));
        JsObject f = (JsObject) e.invoke(promiseCtor, "resolve", plain);
        Check.equal(PromiseObject.FULFILLED, state(f), "a non-callable then fulfills at once");
        Check.same(plain, ((PromiseObject) f).getResult(), "with the object itself");
    }

    /**
     * 25.4.3.1: an exception in the executor rejects the promise, unless it was already resolved.
     * <p>
     * 25.4.3.1: 실행자의 예외는 promise 를 거부하며 이미 해결된 뒤라면 무시된다.
     */
    public void testExecutorExceptions() {
        JsObject p = Ops.construct(promiseCtor, MetaTestSupport.args(e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw new JsException(MetaTestSupport.s("oops"));
            }
        })));
        Check.equal(PromiseObject.REJECTED, state(p), "rejected by the exception");
        Check.equal("oops", ((org.duckdns.hjow.swingwebpanel.js.JsString) ((PromiseObject) p).getResult()).getValue(), "with its value");
        JsObject q = Ops.construct(promiseCtor, MetaTestSupport.args(e.fn(2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                call1(args[0], MetaTestSupport.n(1));
                throw new JsException(MetaTestSupport.s("late"));
            }
        })));
        Check.equal(PromiseObject.FULFILLED, state(q), "an exception after resolve is ignored");
    }

    /**
     * 25.4.4.4, 25.4.4.5: Promise.resolve returns the same promise for promises of the same constructor, adopts thenables and wraps
     * values; Promise.reject creates rejected promises; the this value must be a constructor object.
     * <p>
     * 25.4.4.4, 25.4.4.5: Promise.resolve 는 같은 생성자의 promise 는 그대로 돌려주고 thenable 은 받아들이며 값은 감싼다.
     * Promise.reject 는 거부된 promise 를 만든다. this 값은 생성자 객체여야 한다.
     */
    public void testStaticResolveAndReject() {
        JsObject p = (JsObject) e.invoke(promiseCtor, "resolve", MetaTestSupport.n(1));
        Check.same(p, e.invoke(promiseCtor, "resolve", p), "Promise.resolve(promise) returns the promise");
        Check.equal(PromiseObject.FULFILLED, state(p), "resolve(1) is fulfilled at once");
        JsObject r = (JsObject) e.invoke(promiseCtor, "reject", MetaTestSupport.n(2));
        Check.equal(PromiseObject.REJECTED, state(r), "reject(2) is rejected at once");
        Check.isTrue(r != e.invoke(promiseCtor, "reject", r), "reject always creates a new promise");
        final JsObject resolveFn = (JsObject) promiseCtor.get("resolve");
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(resolveFn, MetaTestSupport.n(1), MetaTestSupport.args(Js.UNDEFINED));
            }
        }, "resolve with a primitive this");
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(resolveFn, e.obj(), MetaTestSupport.args(Js.UNDEFINED));
            }
        }, "resolve with a non-constructor this");
        final JsObject rejectFn = (JsObject) promiseCtor.get("reject");
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(rejectFn, Js.UNDEFINED, MetaTestSupport.args(Js.UNDEFINED));
            }
        }, "reject with undefined this");
    }

    /**
     * 25.4.4.1: Promise.all: values in order of the iterable regardless of settlement order; non-promises are accepted; the empty
     * iterable fulfills at once; the first rejection rejects.
     * <p>
     * 25.4.4.1: Promise.all: 확정 순서와 관계없이 이터러블 순서의 값, promise 가 아닌 값 허용, 빈 이터러블은 즉시 이행, 첫 거부로 거부.
     */
    public void testAll() {
        Check.equal(PromiseObject.FULFILLED, state(e.invoke(promiseCtor, "all", e.iterable(new JsValue[0], null, false))), "empty iterable is fulfilled at once");
        Deferred d1 = deferred();
        Deferred d2 = deferred();
        JsObject all = (JsObject) e.invoke(promiseCtor, "all", e.iterable(new JsValue[] { d1.promise, MetaTestSupport.n(5), d2.promise }, null, false));
        then(all, e.logger("all"), e.logger("fail"));
        call1(d2.resolve, MetaTestSupport.s("two"));
        e.drain();
        Check.equal("", e.logText(), "still waiting for the first promise");
        call1(d1.resolve, MetaTestSupport.s("one"));
        e.drain();
        Check.equal("all:[one+5+two]", e.logText(), "values keep the order of the iterable");
        e.log.clear();
        Deferred d3 = deferred();
        Deferred d4 = deferred();
        JsObject failing = (JsObject) e.invoke(promiseCtor, "all", e.iterable(new JsValue[] { d3.promise, d4.promise }, null, false));
        then(failing, e.logger("all2"), e.logger("fail2"));
        call1(d4.reject, MetaTestSupport.s("bad"));
        call1(d3.resolve, MetaTestSupport.n(1));
        e.drain();
        Check.equal("fail2:bad", e.logText(), "the first rejection rejects the result");
    }

    /**
     * 25.4.4.1: Promise.all with a non-iterable or a failing iterator rejects (it does not throw) and closes iterators that did not
     * finish by themselves.
     * <p>
     * 25.4.4.1: 이터러블이 아니거나 순회가 실패하는 Promise.all 은 예외를 던지지 않고 거부하며, 스스로 끝나지 않은 이터레이터는 닫는다.
     */
    public void testAllErrors() {
        JsObject notIterable = (JsObject) e.invoke(promiseCtor, "all", MetaTestSupport.n(1));
        Check.equal(PromiseObject.REJECTED, state(notIterable), "a number is not iterable: rejected promise");
        int[] closes = new int[1];
        JsObject failing = (JsObject) e.invoke(promiseCtor, "all", e.iterable(new JsValue[] { MetaTestSupport.n(1) }, closes, true));
        Check.equal(PromiseObject.REJECTED, state(failing), "a failing next() rejects");
        Check.equal(0, closes[0], "an iterator whose next() threw is not closed");
        // 정적 resolve 가 던지면 이터레이터를 닫고 거부한다.
        final JsObject fakeCtor = new NativeFunction(e.realm, "Fake", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return ((NativeFunction) promiseCtor).construct(args, newTarget);
            }
        });
        fakeCtor.defineBuiltin("resolve", e.fn(1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw new JsException(MetaTestSupport.s("resolve failed"));
            }
        }));
        fakeCtor.defineData("prototype", promiseCtor.get("prototype"), false, false, false);
        int[] closes2 = new int[1];
        JsObject viaFake = (JsObject) Ops.call(promiseCtor.get("all"), fakeCtor, MetaTestSupport.args(e.iterable(new JsValue[] { MetaTestSupport.n(1) }, closes2, false)));
        Check.equal(PromiseObject.REJECTED, state(viaFake), "an exception in the loop rejects");
        Check.equal(1, closes2[0], "and closes the iterator once");
    }

    /**
     * 25.4.4.3: Promise.race settles like the first promise that settles; an empty iterable stays pending forever.
     * <p>
     * 25.4.4.3: Promise.race 는 가장 먼저 확정되는 promise 와 같게 확정되며 빈 이터러블은 영원히 대기한다.
     */
    public void testRace() {
        JsObject empty = (JsObject) e.invoke(promiseCtor, "race", e.iterable(new JsValue[0], null, false));
        e.drain();
        Check.equal(PromiseObject.PENDING, state(empty), "an empty race stays pending");
        Deferred slow = deferred();
        Deferred fast = deferred();
        JsObject race = (JsObject) e.invoke(promiseCtor, "race", e.iterable(new JsValue[] { slow.promise, fast.promise }, null, false));
        then(race, e.logger("won"), e.logger("lost"));
        call1(fast.resolve, MetaTestSupport.s("fast"));
        call1(slow.reject, MetaTestSupport.s("slow"));
        e.drain();
        Check.equal("won:fast", e.logText(), "the first settled promise decides");
        JsObject plain = (JsObject) e.invoke(promiseCtor, "race", e.iterable(new JsValue[] { MetaTestSupport.n(1), MetaTestSupport.n(2) }, null, false));
        e.drain();
        Check.equal(1.0, ((JsNumber) ((PromiseObject) plain).getResult()).getValue(), "of two plain values the first one wins");
        JsObject notIterable = (JsObject) e.invoke(promiseCtor, "race", Js.NULL);
        Check.equal(PromiseObject.REJECTED, state(notIterable), "not iterable: rejected promise");
    }

    /**
     * 25.4.5.3.1, host hook: unhandled rejections and late handlers are reported to the tracker; handled rejections are not.
     * <p>
     * 25.4.5.3.1, 호스트 훅: 처리되지 않은 거부와 늦게 붙은 처리기는 추적기에 보고되며 처리된 거부는 보고되지 않는다.
     */
    public void testRejectionTracker() {
        final List<String> events = new ArrayList<String>();
        PromiseJobs.setRejectionTracker(e.realm, new PromiseRejectionTracker() {
            public void rejectedWithoutHandler(JsRealm realm, JsObject promise, JsValue reason) {
                events.add("unhandled:" + e.show(reason));
            }

            public void handlerAddedToRejectedPromise(JsRealm realm, JsObject promise) {
                events.add("handled-later");
            }
        });
        e.invoke(promiseCtor, "reject", MetaTestSupport.s("a"));
        Deferred d = deferred();
        then(d.promise, null_(), e.logger("h"));
        call1(d.reject, MetaTestSupport.s("b"));
        Deferred late = deferred();
        call1(late.reject, MetaTestSupport.s("c"));
        then(late.promise, null_(), e.logger("h2"));
        Check.equal("[unhandled:a, unhandled:c, handled-later]", events.toString(), "tracker events: handled rejections are not reported");
        e.drain();
        Check.equal(3, events.size(), "draining the queue adds no events");
        PromiseJobs.setRejectionTracker(e.realm, null);
        e.invoke(promiseCtor, "reject", MetaTestSupport.s("d"));
        Check.equal(3, events.size(), "removed tracker gets nothing");
    }

    /**
     * 25.4.1.5, 25.4.4.5: NewPromiseCapability requires a constructor that calls the executor once with callable functions.
     * <p>
     * 25.4.1.5, 25.4.4.5: NewPromiseCapability 는 호출 가능한 함수로 실행자를 정확히 한 번 호출하는 생성자를 요구한다.
     */
    public void testCapabilityErrors() {
        final JsObject resolveFn = (JsObject) promiseCtor.get("resolve");
        final JsObject neverCalls = new NativeFunction(e.realm, "Never", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return r.newObject();
            }
        });
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(resolveFn, neverCalls, MetaTestSupport.args(MetaTestSupport.n(1)));
            }
        }, "a constructor that never calls the executor");
        final JsObject callsTwice = new NativeFunction(e.realm, "Twice", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Ops.call(args[0], Js.UNDEFINED, MetaTestSupport.args(MetaTestSupport.n(1), MetaTestSupport.n(2)));
                Ops.call(args[0], Js.UNDEFINED, MetaTestSupport.args(MetaTestSupport.n(1), MetaTestSupport.n(2)));
                return r.newObject();
            }
        });
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(resolveFn, callsTwice, MetaTestSupport.args(MetaTestSupport.n(1)));
            }
        }, "a constructor that calls the executor twice with defined arguments");
        final JsObject goodFns = new NativeFunction(e.realm, "Good", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                e.log.add("executor-length:" + (int) ((JsNumber) ((JsObject) args[0]).get("length")).getValue());
                Ops.call(args[0], Js.UNDEFINED, MetaTestSupport.args(e.fn(1, new NativeFunction.Call() {
                    public JsValue call(JsRealm r2, JsValue t, JsValue[] a) {
                        e.log.add("custom-resolve");
                        return Js.UNDEFINED;
                    }
                }), MetaTestSupport.n(5)));
                return r.newObject();
            }
        });
        MetaTestSupport.typeError(new Block() {
            public void run() {
                Ops.call(resolveFn, goodFns, MetaTestSupport.args(MetaTestSupport.n(1)));
            }
        }, "a non-callable reject function");
        Check.equal("executor-length:2", e.logText(), "the capability executor has length 2");
    }

    /**
     * 25.4.5.3, 25.4.4.6: subclasses: static methods use this as constructor, then uses SpeciesConstructor, and Symbol.species can
     * redirect the derived promises to another constructor.
     * <p>
     * 25.4.5.3, 25.4.4.6: 하위 클래스: 정적 메서드는 this 를 생성자로 쓰고, then 은 SpeciesConstructor 를 쓰며, Symbol.species 로 파생
     * promise 를 다른 생성자로 돌릴 수 있다.
     */
    public void testSubclassAndSpecies() {
        final JsObject subProto = new JsObject((JsObject) promiseCtor.get("prototype"));
        final NativeFunction sub = new NativeFunction(e.realm, promiseCtor, "Sub", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("needs new");
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return ((NativeFunction) promiseCtor).construct(args, newTarget);
            }
        });
        sub.defineData("prototype", subProto, false, false, false);
        subProto.defineBuiltin("constructor", sub);
        JsObject resolved = (JsObject) e.invoke(sub, "resolve", MetaTestSupport.n(1));
        Check.same(subProto, resolved.getPrototypeOf(), "Sub.resolve creates a Sub");
        Check.same(resolved, e.invoke(sub, "resolve", resolved), "Sub.resolve(sub instance) returns it");
        JsObject asPromise = (JsObject) e.invoke(promiseCtor, "resolve", resolved);
        Check.isTrue(asPromise != resolved, "Promise.resolve(sub instance) wraps it (different constructor)");
        Check.same(promiseCtor.get("prototype"), asPromise.getPrototypeOf(), "into a plain Promise");
        JsObject derived = then(resolved, e.logger("x"), null_());
        Check.same(subProto, derived.getPrototypeOf(), "then creates a Sub through the constructor property");
        Check.same(sub, Ops.call(((JsObject) promiseCtor.getOwnProperty(WellKnownSymbols.SPECIES).get), sub, Js.NO_ARGS), "species getter returns this");
        sub.defineAccessor(WellKnownSymbols.SPECIES, e.fn(0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return promiseCtor;
            }
        }), null, false, true);
        JsObject viaSpecies = then(resolved, e.logger("y"), null_());
        Check.same(promiseCtor.get("prototype"), viaSpecies.getPrototypeOf(), "@@species redirects derived promises to Promise");
        JsObject nullSpecies = (JsObject) e.invoke(sub, "resolve", MetaTestSupport.n(2));
        sub.defineAccessor(WellKnownSymbols.SPECIES, e.fn(0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.NULL;
            }
        }), null, false, true);
        Check.same(promiseCtor.get("prototype"), then(nullSpecies, null_(), null_()).getPrototypeOf(), "null species means the default constructor");
        final JsObject brokenSub = (JsObject) e.invoke(sub, "resolve", MetaTestSupport.n(3));
        sub.defineAccessor(WellKnownSymbols.SPECIES, e.fn(0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return MetaTestSupport.n(5);
            }
        }), null, false, true);
        MetaTestSupport.typeError(new Block() {
            public void run() {
                then(brokenSub, null_(), null_());
            }
        }, "a non-constructor species");
        subProto.defineBuiltin("constructor", MetaTestSupport.n(1));
        MetaTestSupport.typeError(new Block() {
            public void run() {
                then(brokenSub, null_(), null_());
            }
        }, "a primitive constructor property");
        e.drain();
    }

    /**
     * The job queue of the realm is chosen by {@link PromiseJobs}: without a configured queue the private default queue is used and
     * {@link PromiseJobs#drain(JsRealm)} runs its jobs.
     * <p>
     * realm 의 작업 큐는 {@link PromiseJobs} 가 고른다. 설정한 큐가 없으면 비공개 기본 큐를 쓰고 {@link PromiseJobs#drain(JsRealm)} 이 그 작업을
     * 실행한다.
     */
    public void testDefaultQueueIsDrainedByTheHost() {
        JsRealm fresh = new JsRealm();
        new MetaLibInstaller().install(fresh);
        if (!(PromiseJobs.queueOf(fresh) instanceof SimpleJobQueue)) {
            Check.skip("the realm provides its own job queue; the private default queue is not used");
        }
        JsObject ctor = (JsObject) fresh.getGlobalObject().get("Promise");
        final List<String> log = new ArrayList<String>();
        JsObject p = (JsObject) Ops.invoke(fresh, ctor, "resolve", MetaTestSupport.args(MetaTestSupport.n(1)));
        Ops.invoke(fresh, p, "then", MetaTestSupport.args(new NativeFunction(fresh, "cb", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.add("ran");
                return Js.UNDEFINED;
            }
        })));
        Check.equal("[]", log.toString(), "nothing ran before the host drained the queue");
        Check.isTrue(PromiseJobs.drain(fresh), "the default queue can be drained");
        Check.equal("[ran]", log.toString(), "job ran");
    }
}
