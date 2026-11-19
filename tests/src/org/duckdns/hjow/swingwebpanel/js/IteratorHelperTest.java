package org.duckdns.hjow.swingwebpanel.js;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the iterator protocol helpers in {@link IteratorHelper} using hand-made native iterables.
 * <p>
 * 직접 만든 네이티브 반복 가능 객체로 {@link IteratorHelper} 의 이터레이터 프로토콜 도우미를 시험한다.
 */
public class IteratorHelperTest {

    /**
     * The realm used by the cases.
     * <p>
     * 사례가 쓰는 realm.
     */
    private final JsRealm realm = new JsRealm();

    /**
     * Counts calls of return() of the iterables created by {@link #iterableOf(int, StringBuilder)}.
     * <p>
     * {@link #iterableOf(int, StringBuilder)} 로 만든 반복 가능 객체의 return() 호출을 기록한다.
     */
    private final StringBuilder log = new StringBuilder();

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public IteratorHelperTest() {
    }

    /**
     * Creates an iterable producing 0, 1, ... count-1 whose iterator logs "R" when return() is called.
     * <p>
     * 0, 1, ... count-1 을 만들어 내는 반복 가능 객체를 만든다. return() 이 호출되면 이터레이터가 "R" 을 기록한다.
     *
     * @param count number of values / 값의 개수
     * @param log   the log / 로그
     * @return the iterable / 반복 가능 객체
     */
    private JsObject iterableOf(final int count, final StringBuilder log) {
        JsObject iterable = realm.newObject();
        realm.method(iterable, WellKnownSymbols.ITERATOR, 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                final int[] pos = new int[1];
                JsObject it = new JsObject(r.getIteratorPrototype());
                r.method(it, "next", 0, new NativeFunction.Call() {
                    public JsValue call(JsRealm r2, JsValue t, JsValue[] a) {
                        if (pos[0] < count) {
                            return IteratorHelper.createResult(r2, Js.num(pos[0]++), false);
                        }
                        return IteratorHelper.createResult(r2, Js.UNDEFINED, true);
                    }
                });
                r.method(it, "return", 0, new NativeFunction.Call() {
                    public JsValue call(JsRealm r2, JsValue t, JsValue[] a) {
                        log.append("R");
                        return r2.newObject();
                    }
                });
                return it;
            }
        });
        return iterable;
    }

    /**
     * An iterable can be drained into a list in order.
     * <p>
     * 반복 가능 객체를 순서대로 목록으로 모을 수 있다.
     */
    public void testToList() {
        List<JsValue> list = IteratorHelper.toList(realm, iterableOf(3, log));
        Check.equal(3, list.size(), "three values");
        Check.equal("0,1,2", Ops.toString(list.get(0)) + "," + Ops.toString(list.get(1)) + "," + Ops.toString(list.get(2)), "values in order");
        Check.equal("", log.toString(), "return() is not called when the iterator finishes by itself");
    }

    /**
     * step() returns null at the end; close() calls return(); closeQuietly() hides errors of return().
     * <p>
     * step() 은 끝에서 null 을 반환하고 close() 는 return() 을 호출하며 closeQuietly() 는 return() 의 오류를 숨긴다.
     */
    public void testStepAndClose() {
        JsObject it = IteratorHelper.getIterator(realm, iterableOf(1, log));
        JsObject first = IteratorHelper.step(it);
        Check.notNull(first, "first step");
        Check.equal(0, (long) ((JsNumber) IteratorHelper.value(first)).getValue(), "first value");
        Check.isNull(IteratorHelper.step(it), "second step is done");
        IteratorHelper.close(it);
        Check.equal("R", log.toString(), "close called return()");

        final JsObject failing = new JsObject(realm.getIteratorPrototype());
        realm.method(failing, "return", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("boom");
            }
        });
        IteratorHelper.closeQuietly(failing);
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                IteratorHelper.close(failing);
            }
        }, "close() on a normal completion reports errors of return()");
    }

    /**
     * Non-iterable values and iterators returning non-objects are TypeErrors.
     * <p>
     * 반복 가능하지 않은 값과 객체가 아닌 값을 돌려주는 이터레이터는 TypeError 이다.
     */
    public void testProtocolErrors() {
        JsException e = (JsException) Check.throwsException(JsException.class, new Block() {
            public void run() {
                IteratorHelper.getIterator(realm, Js.num(1));
            }
        }, "a number is not iterable");
        Check.equal(ErrorKind.TYPE_ERROR, e.getKind(), "TypeError");
        final JsObject bad = realm.newObject();
        realm.method(bad, WellKnownSymbols.ITERATOR, 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(1);
            }
        });
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                IteratorHelper.getIterator(realm, bad);
            }
        }, "@@iterator must return an object");
        final JsObject badNext = new JsObject(realm.getIteratorPrototype());
        realm.method(badNext, "next", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(1);
            }
        });
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                IteratorHelper.step(badNext);
            }
        }, "next() must return an object");
    }

    /**
     * %IteratorPrototype% is registered and its [Symbol.iterator] returns this.
     * <p>
     * %IteratorPrototype% 이 등록되어 있고 그 [Symbol.iterator] 는 this 를 반환한다.
     */
    public void testIteratorPrototypeIntrinsic() {
        Check.isTrue(realm.getIntrinsic("%IteratorPrototype%") == realm.getIteratorPrototype(), "registered intrinsic");
        JsObject it = new JsObject(realm.getIteratorPrototype());
        JsValue back = Ops.call(it.get(WellKnownSymbols.ITERATOR), it, Js.NO_ARGS);
        Check.isTrue(back == it, "returns this");
    }
}
