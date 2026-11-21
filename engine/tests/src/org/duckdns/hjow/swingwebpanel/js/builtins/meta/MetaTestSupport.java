package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.SimpleJobQueue;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.AssertionFailure;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers shared by the tests of the object-model built-ins: a realm with the meta library installed and a simple job queue, short
 * constructors for functions and objects, and error expectations. The tests call the built-ins from Java through
 * {@link Ops}; script-level tests live in {@link ScriptLevelMetaTest}.
 * <p>
 * 객체 모델 내장 객체 시험이 공유하는 도우미: meta 라이브러리가 설치되고 단순 작업 큐가 연결된 realm, 함수와 객체를 만드는 짧은 생성자,
 * 오류 기대 도구. 시험은 {@link Ops} 로 자바에서 내장 객체를 호출하며, 스크립트 수준 시험은 {@link ScriptLevelMetaTest} 에 있다.
 */
public final class MetaTestSupport {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private MetaTestSupport() {
    }

    /**
     * A test environment: realm, job queue and a log for callbacks.
     * <p>
     * 시험 환경: realm, 작업 큐, 콜백 기록.
     */
    public static final class Env {
        /**
         * The realm with the meta library installed.
         * <p>
         * meta 라이브러리가 설치된 realm.
         */
        public final JsRealm realm = new JsRealm();

        /**
         * The job queue that receives promise jobs.
         * <p>
         * promise 작업을 받는 작업 큐.
         */
        public final SimpleJobQueue queue = new SimpleJobQueue();

        /**
         * Messages recorded by logging callbacks.
         * <p>
         * 기록 콜백이 남긴 메시지.
         */
        public final List<String> log = new ArrayList<String>();

        /**
         * Creates the environment.
         * <p>
         * 환경을 만든다.
         */
        public Env() {
            new MetaLibInstaller().install(realm);
            PromiseJobs.setQueue(realm, queue);
        }

        /**
         * Runs all queued promise jobs.
         * <p>
         * 대기 중인 promise 작업을 모두 실행한다.
         */
        public void drain() {
            queue.runAll();
        }

        /**
         * Returns the log as one comma separated string.
         * <p>
         * 기록을 쉼표로 이은 문자열로 돌려준다.
         *
         * @return the log text / 기록 문자열
         */
        public String logText() {
            StringBuilder sb = new StringBuilder();
            // 기록된 메시지를 순서대로 쉼표로 잇는다.
            for (int i = 0; i < log.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(log.get(i));
            }
            return sb.toString();
        }

        /**
         * Returns a global property.
         * <p>
         * 전역 프로퍼티를 반환한다.
         *
         * @param name the property name / 프로퍼티 이름
         * @return the value / 값
         */
        public JsValue global(String name) {
            return realm.getGlobalObject().get(name);
        }

        /**
         * Creates a native function with a Java body.
         * <p>
         * Java 본문을 가진 네이티브 함수를 만든다.
         *
         * @param length the function length / 함수 length
         * @param call   the body / 본문
         * @return the function / 함수
         */
        public NativeFunction fn(int length, NativeFunction.Call call) {
            return new NativeFunction(realm, "f", length, call);
        }

        /**
         * Creates a function that appends a message to the log when it is called and returns undefined.
         * <p>
         * 호출될 때 기록에 메시지를 덧붙이고 undefined 를 반환하는 함수를 만든다.
         *
         * @param message the message, followed by the first argument converted to a string / 메시지. 뒤에 첫 인수를 문자열로 붙인다
         * @return the function / 함수
         */
        public NativeFunction logger(final String message) {
            return fn(1, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    log.add(message + (args.length > 0 ? ":" + show(args[0]) : ""));
                    return Js.UNDEFINED;
                }
            });
        }

        /**
         * Creates a function that logs like {@link #logger(String)} and returns the given value.
         * <p>
         * {@link #logger(String)} 처럼 기록하고 주어진 값을 반환하는 함수를 만든다.
         *
         * @param message the message / 메시지
         * @param result  the value to return / 반환할 값
         * @return the function / 함수
         */
        public NativeFunction loggerReturning(final String message, final JsValue result) {
            return fn(1, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    log.add(message + (args.length > 0 ? ":" + show(args[0]) : ""));
                    return result;
                }
            });
        }

        /**
         * Creates a function that logs like {@link #logger(String)} and then throws the given value.
         * <p>
         * {@link #logger(String)} 처럼 기록한 뒤 주어진 값을 던지는 함수를 만든다.
         *
         * @param message the message / 메시지
         * @param error   the value to throw / 던질 값
         * @return the function / 함수
         */
        public NativeFunction loggerThrowing(final String message, final JsValue error) {
            return fn(1, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    log.add(message + (args.length > 0 ? ":" + show(args[0]) : ""));
                    throw new JsException(error);
                }
            });
        }

        /**
         * Formats a value for logs without calling any JavaScript method: strings and numbers as text, arrays as "[a+b]",
         * promises as "P(state)", other objects as "{}".
         * <p>
         * JavaScript 메서드를 호출하지 않고 값을 기록용 문자열로 만든다: 문자열과 숫자는 그대로, 배열은 "[a+b]", promise 는 "P(상태)",
         * 그 밖의 객체는 "{}".
         *
         * @param v the value / 값
         * @return the text / 문자열
         */
        public String show(JsValue v) {
            if (v instanceof JsArray) {
                StringBuilder sb = new StringBuilder("[");
                JsArray a = (JsArray) v;
                // 배열의 요소를 + 로 이어 붙인다.
                for (int i = 0; i < a.getLength(); i++) {
                    if (i > 0) {
                        sb.append('+');
                    }
                    sb.append(show(a.get(String.valueOf(i))));
                }
                return sb.append(']').toString();
            }
            if (v instanceof PromiseObject) {
                int st = ((PromiseObject) v).getState();
                return "P(" + (st == PromiseObject.PENDING ? "pending" : st == PromiseObject.FULFILLED ? "ok" : "err") + ")";
            }
            if (v instanceof JsObject) {
                return "{}";
            }
            return Ops.toString(v);
        }

        /**
         * Creates an iterable object over the given values. Its iterator counts calls of <code>return</code> in the given counter.
         * <p>
         * 주어진 값들에 대한 이터러블 객체를 만든다. 이터레이터의 <code>return</code> 호출 횟수는 주어진 카운터에 센다.
         *
         * @param items       the values / 값들
         * @param closeCount  one-element array that counts calls of return, or null / return 호출 횟수를 세는 원소 하나짜리 배열. 없으면 null
         * @param failAtEnd   true to throw a TypeError from next instead of finishing / 끝나는 대신 next 에서 TypeError 를 던지려면 true
         * @return the iterable / 이터러블
         */
        public JsObject iterable(final JsValue[] items, final int[] closeCount, final boolean failAtEnd) {
            JsObject iterable = realm.newObject();
            realm.method(iterable, WellKnownSymbols.ITERATOR, 0, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    final int[] pos = new int[1];
                    JsObject it = new JsObject(r.getIteratorPrototype());
                    r.method(it, "next", 0, new NativeFunction.Call() {
                        public JsValue call(JsRealm r2, JsValue t, JsValue[] a) {
                            if (pos[0] < items.length) {
                                return IteratorHelper.createResult(r2, items[pos[0]++], false);
                            }
                            if (failAtEnd) {
                                throw JsException.typeError("iteration failed");
                            }
                            return IteratorHelper.createResult(r2, Js.UNDEFINED, true);
                        }
                    });
                    if (closeCount != null) {
                        r.method(it, "return", 0, new NativeFunction.Call() {
                            public JsValue call(JsRealm r2, JsValue t, JsValue[] a) {
                                closeCount[0]++;
                                return IteratorHelper.createResult(r2, Js.UNDEFINED, true);
                            }
                        });
                    }
                    return it;
                }
            });
            return iterable;
        }

        /**
         * Creates an ordinary object from alternating keys and values.
         * <p>
         * 키와 값이 번갈아 나오는 목록으로 일반 객체를 만든다.
         *
         * @param kv alternating String keys and JsValue values / 번갈아 나오는 String 키와 JsValue 값
         * @return the object / 객체
         */
        public JsObject obj(Object... kv) {
            JsObject o = realm.newObject();
            // 키와 값을 둘씩 읽어 일반 데이터 프로퍼티로 만든다.
            for (int i = 0; i + 1 < kv.length; i += 2) {
                o.createDataProperty(kv[i], (JsValue) kv[i + 1]);
            }
            return o;
        }

        /**
         * Calls a method of a value.
         * <p>
         * 값의 메서드를 호출한다.
         *
         * @param target the value / 값
         * @param name   the method name / 메서드 이름
         * @param a      the arguments / 인수
         * @return the result / 결과
         */
        public JsValue invoke(JsValue target, String name, JsValue... a) {
            return Ops.invoke(realm, target, name, a);
        }
    }

    /**
     * Shorthand for an argument list.
     * <p>
     * 인수 목록의 약식 표기.
     *
     * @param a the values / 값들
     * @return the array / 배열
     */
    public static JsValue[] args(JsValue... a) {
        return a;
    }

    /**
     * Shorthand for a JavaScript string.
     * <p>
     * JavaScript 문자열의 약식 표기.
     *
     * @param s the text / 문자열
     * @return the value / 값
     */
    public static JsString s(String s) {
        return Js.str(s);
    }

    /**
     * Shorthand for a JavaScript number.
     * <p>
     * JavaScript 숫자의 약식 표기.
     *
     * @param d the number / 숫자
     * @return the value / 값
     */
    public static JsValue n(double d) {
        return Js.num(d);
    }

    /**
     * Runs a block that must throw a JavaScript exception of the given kind.
     * <p>
     * 주어진 종류의 JavaScript 예외를 던져야 하는 코드를 실행한다.
     *
     * @param kind    the expected error kind / 기대하는 오류 종류
     * @param block   the code / 코드
     * @param message description for failures / 실패 설명
     * @return the exception / 예외
     */
    public static JsException throwsKind(ErrorKind kind, Block block, String message) {
        JsException e = (JsException) Check.throwsException(JsException.class, block, message);
        if (e.getKind() != kind) {
            throw new AssertionFailure(message + " (expected " + kind + " but was " + e.getKind() + ": " + e.getMessage() + ")");
        }
        return e;
    }

    /**
     * Runs a block that must throw a TypeError.
     * <p>
     * TypeError 를 던져야 하는 코드를 실행한다.
     *
     * @param block   the code / 코드
     * @param message description for failures / 실패 설명
     * @return the exception / 예외
     */
    public static JsException typeError(Block block, String message) {
        return throwsKind(ErrorKind.TYPE_ERROR, block, message);
    }
}
