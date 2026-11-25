package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the methods of <code>%TypedArray%.prototype</code> (ECMA-262 6th edition, 22.2.3; plan item P14-06): searching, joining, in-place
 * changes, copying, callbacks with their arguments, species creation, sorting and iterators, plus the checks of their this values and
 * arguments.
 * <p>
 * <code>%TypedArray%.prototype</code> 메서드 시험(ECMA-262 6판, 22.2.3, 계획 P14-06): 검색, 이어 붙이기, 제자리 변경, 복사, 인수를 확인하는 콜백,
 * species 생성, 정렬, 이터레이터와 this 값/인수 검사.
 */
public class TypedArrayMethodsTest {

    /**
     * The realm used by a case.
     * <p>
     * 사례가 쓰는 realm.
     */
    private JsRealm realm;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TypedArrayMethodsTest() {
    }

    /**
     * Creates a fresh realm before each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        realm = TypedTestSupport.newRealm();
    }

    /**
     * Creates a Uint8Array.
     * <p>
     * Uint8Array 를 만든다.
     *
     * @param xs the elements / 요소들
     * @return the array / 배열
     */
    private TypedArrayObject u8(double... xs) {
        return TypedTestSupport.ta(realm, "Uint8Array", xs);
    }

    /**
     * Calls a method with numeric arguments.
     * <p>
     * 숫자 인수로 메서드를 호출한다.
     *
     * @param target the receiver / 수신자
     * @param method the method name / 메서드 이름
     * @param xs     the numeric arguments / 숫자 인수
     * @return the result / 결과
     */
    private JsValue m(JsValue target, String method, double... xs) {
        JsValue[] a = new JsValue[xs.length];
        // 숫자 인수를 JavaScript 값으로 바꾼다.
        for (int i = 0; i < xs.length; i++) {
            a[i] = TypedTestSupport.n(xs[i]);
        }
        return TypedTestSupport.call(realm, target, method, a);
    }

    /**
     * Returns the numbers of a typed array result.
     * <p>
     * 형식화 배열 결과의 숫자를 돌려준다.
     *
     * @param v the result / 결과
     * @return the elements / 요소들
     */
    private double[] els(JsValue v) {
        Check.isTrue(v instanceof TypedArrayObject, "the result must be a typed array");
        return TypedTestSupport.elements((TypedArrayObject) v);
    }

    /**
     * Records the arguments of every callback call.
     * <p>
     * 콜백 호출마다 인수를 기록한다.
     */
    private static final class Recorder implements NativeFunction.Call {
        /**
         * Recorded calls as "value,index,sameArray,thisIsArg".
         * <p>
         * 기록된 호출: "값,인덱스,같은배열,this".
         */
        final List<String> calls = new ArrayList<String>();

        /**
         * The expected array.
         * <p>
         * 기대하는 배열.
         */
        private final JsObject array;

        /**
         * The expected this value.
         * <p>
         * 기대하는 this 값.
         */
        private final JsValue self;

        /**
         * The result to return (Js.TRUE by default).
         * <p>
         * 돌려줄 결과(기본은 Js.TRUE).
         */
        private final JsValue result;

        /**
         * Creates a recorder.
         * <p>
         * 기록기를 만든다.
         *
         * @param array  the array the callback is expected to receive / 콜백이 받을 것으로 기대하는 배열
         * @param self   the expected this / 기대하는 this
         * @param result the result to return / 돌려줄 결과
         */
        Recorder(JsObject array, JsValue self, JsValue result) {
            this.array = array;
            this.self = self;
            this.result = result;
        }

        /**
         * Records one call.
         * <p>
         * 호출 하나를 기록한다.
         *
         * @param r         the realm / realm
         * @param thisValue the this value / this 값
         * @param args      the arguments / 인수
         * @return the configured result / 설정된 결과
         */
        public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
            calls.add(Ops.toString(args[0]) + "," + Ops.toString(args[1]) + "," + (args[2] == array) + "," + (thisValue == self));
            return result;
        }
    }

    /**
     * Tests join with and without separator, and toString.
     * <p>
     * 구분자가 있을 때와 없을 때의 join 과 toString 을 시험한다.
     */
    public void testJoinAndToString() {
        TypedArrayObject a = u8(1, 2, 3);
        Check.equal("1,2,3", TypedTestSupport.str(m(a, "join")), "join()");
        Check.equal("1-2-3", TypedTestSupport.str(TypedTestSupport.call(realm, a, "join", TypedTestSupport.s("-"))), "join('-')");
        Check.equal("123", TypedTestSupport.str(TypedTestSupport.call(realm, a, "join", TypedTestSupport.s(""))), "join('')");
        Check.equal("1,2,3", TypedTestSupport.str(m(a, "toString")), "toString()");
        Check.equal("", TypedTestSupport.str(m(u8(), "join")), "empty array");
        Check.equal("1.5,NaN,-Infinity", TypedTestSupport.str(m(TypedTestSupport.ta(realm, "Float64Array", 1.5, Double.NaN, Double.NEGATIVE_INFINITY), "join")),
                "float elements");
        Check.equal("1,2,3", TypedTestSupport.str(TypedTestSupport.call(realm, a, "join", Js.UNDEFINED)), "undefined separator means ','");
    }

    /**
     * indexOf and lastIndexOf use strict equality and clamp their start argument.
     * <p>
     * indexOf 와 lastIndexOf 는 엄격한 동등 비교를 쓰고 시작 인수를 제한한다.
     */
    public void testIndexOfAndLastIndexOf() {
        TypedArrayObject a = u8(1, 2, 3, 2, 1);
        TypedTestSupport.eq(1, TypedTestSupport.num(m(a, "indexOf", 2)), "indexOf(2)");
        TypedTestSupport.eq(3, TypedTestSupport.num(m(a, "indexOf", 2, 2)), "indexOf(2, 2)");
        TypedTestSupport.eq(3, TypedTestSupport.num(m(a, "indexOf", 2, -2)), "indexOf(2, -2)");
        TypedTestSupport.eq(1, TypedTestSupport.num(m(a, "indexOf", 2, -100)), "indexOf(2, -100)");
        TypedTestSupport.eq(-1, TypedTestSupport.num(m(a, "indexOf", 9)), "indexOf(9)");
        TypedTestSupport.eq(-1, TypedTestSupport.num(m(a, "indexOf", 1, 10)), "indexOf(1, 10)");
        TypedTestSupport.eq(-1, TypedTestSupport.num(TypedTestSupport.call(realm, a, "indexOf", TypedTestSupport.s("1"))), "strict equality: '1' is not 1");
        TypedTestSupport.eq(3, TypedTestSupport.num(m(a, "lastIndexOf", 2)), "lastIndexOf(2)");
        TypedTestSupport.eq(1, TypedTestSupport.num(m(a, "lastIndexOf", 2, 2)), "lastIndexOf(2, 2)");
        TypedTestSupport.eq(0, TypedTestSupport.num(m(a, "lastIndexOf", 1, -5)), "lastIndexOf(1, -5)");
        TypedTestSupport.eq(-1, TypedTestSupport.num(m(a, "lastIndexOf", 1, -6)), "lastIndexOf(1, -6)");
        TypedTestSupport.eq(3, TypedTestSupport.num(m(a, "lastIndexOf", 2, -1)), "lastIndexOf(2, -1)");
        TypedTestSupport.eq(-1, TypedTestSupport.num(m(u8(), "indexOf", 1)), "empty array");
        TypedArrayObject f = TypedTestSupport.ta(realm, "Float32Array", Double.NaN, 0, -0.0);
        TypedTestSupport.eq(-1, TypedTestSupport.num(m(f, "indexOf", Double.NaN)), "NaN is never found");
        TypedTestSupport.eq(1, TypedTestSupport.num(m(f, "indexOf", -0.0)), "-0 equals +0");
    }

    /**
     * fill converts the value first and honors relative start and end.
     * <p>
     * fill 은 값을 먼저 변환하고 상대적인 시작과 끝을 따른다.
     */
    public void testFill() {
        TypedArrayObject a = u8(0, 0, 0, 0, 0);
        Check.same(a, m(a, "fill", 7, 1, 3), "fill returns this");
        TypedTestSupport.checkElements(new double[] { 0, 7, 7, 0, 0 }, a, "fill(7, 1, 3)");
        m(a, "fill", 9, -2);
        TypedTestSupport.checkElements(new double[] { 0, 7, 7, 9, 9 }, a, "fill(9, -2)");
        m(a, "fill", 1, 3, 1);
        TypedTestSupport.checkElements(new double[] { 0, 7, 7, 9, 9 }, a, "start after end fills nothing");
        TypedTestSupport.call(realm, a, "fill", TypedTestSupport.s("5"));
        TypedTestSupport.checkElements(new double[] { 5, 5, 5, 5, 5 }, a, "fill('5') converts the value");
        TypedArrayObject clamped = TypedTestSupport.ta(realm, "Uint8ClampedArray", 0, 0);
        m(clamped, "fill", 300);
        TypedTestSupport.checkElements(new double[] { 255, 255 }, clamped, "fill respects the element type");
    }

    /**
     * reverse changes the array in place.
     * <p>
     * reverse 는 배열을 제자리에서 바꾼다.
     */
    public void testReverse() {
        TypedArrayObject a = u8(1, 2, 3, 4, 5);
        Check.same(a, m(a, "reverse"), "returns this");
        TypedTestSupport.checkElements(new double[] { 5, 4, 3, 2, 1 }, a, "odd length");
        TypedTestSupport.checkElements(new double[] { 4, 3, 2, 1 }, (JsObject) m(u8(1, 2, 3, 4), "reverse"), "even length");
        TypedTestSupport.checkElements(new double[0], (JsObject) m(u8(), "reverse"), "empty");
    }

    /**
     * copyWithin copies inside the array, also when the ranges overlap, scaled by the element size.
     * <p>
     * copyWithin 은 배열 안에서 복사하며 구간이 겹쳐도 되고 요소 크기에 맞게 환산한다.
     */
    public void testCopyWithin() {
        TypedTestSupport.checkElements(new double[] { 4, 5, 3, 4, 5 }, (JsObject) m(u8(1, 2, 3, 4, 5), "copyWithin", 0, 3), "copyWithin(0, 3)");
        TypedTestSupport.checkElements(new double[] { 1, 1, 2, 3, 5 }, (JsObject) m(u8(1, 2, 3, 4, 5), "copyWithin", 1, 0, 3), "overlapping forward");
        TypedTestSupport.checkElements(new double[] { 2, 3, 4, 4, 5 }, (JsObject) m(u8(1, 2, 3, 4, 5), "copyWithin", 0, 1, 4), "overlapping backward");
        TypedTestSupport.checkElements(new double[] { 1, 2, 3, 1, 2 }, (JsObject) m(u8(1, 2, 3, 4, 5), "copyWithin", -2, 0), "negative target");
        TypedTestSupport.checkElements(new double[] { 1, 2, 3, 4, 5 }, (JsObject) m(u8(1, 2, 3, 4, 5), "copyWithin", 0, 5), "nothing to copy");
        TypedTestSupport.checkElements(new double[] { 300, 400, 300, 400 }, (JsObject) m(TypedTestSupport.ta(realm, "Int16Array", 100, 200, 300, 400), "copyWithin", 0, 2),
                "Int16Array copies whole elements");
        TypedTestSupport.checkElements(new double[] { 3, 4, 3, 4 }, (JsObject) m(TypedTestSupport.ta(realm, "Float64Array", 1, 2, 3, 4), "copyWithin", 0, 2),
                "Float64Array");
    }

    /**
     * slice copies into a new array of the same type; subarray shares the buffer.
     * <p>
     * slice 는 같은 형식의 새 배열로 복사하고 subarray 는 버퍼를 공유한다.
     */
    public void testSliceAndSubarray() {
        TypedArrayObject a = TypedTestSupport.ta(realm, "Int16Array", 10, 20, 30, 40, 50);
        TypedArrayObject s = (TypedArrayObject) m(a, "slice", 1, 3);
        TypedTestSupport.checkElements(new double[] { 20, 30 }, s, "slice(1, 3)");
        Check.equal(ElementType.INT16, s.getType(), "the slice keeps the type");
        Check.isTrue(s.getBuffer() != a.getBuffer(), "the slice has its own buffer");
        TypedTestSupport.checkElements(new double[] { 40, 50 }, (JsObject) m(a, "slice", -2), "slice(-2)");
        TypedTestSupport.checkElements(new double[] { 10, 20, 30, 40, 50 }, (JsObject) m(a, "slice"), "slice()");
        TypedTestSupport.checkElements(new double[0], (JsObject) m(a, "slice", 3, 1), "empty slice");
        TypedArrayObject sub = (TypedArrayObject) m(a, "subarray", 1, 4);
        TypedTestSupport.checkElements(new double[] { 20, 30, 40 }, sub, "subarray(1, 4)");
        Check.same(a.getBuffer(), sub.getBuffer(), "the subarray shares the buffer");
        Check.equal(2, sub.getByteOffset(), "byteOffset of the subarray");
        Check.equal(6, sub.getByteLength(), "byteLength of the subarray");
        sub.setElement(0, 99);
        TypedTestSupport.eq(99, a.getElement(1), "writes through the subarray are visible in the original");
        TypedArrayObject sub2 = (TypedArrayObject) m(sub, "subarray", 1);
        Check.equal(4, sub2.getByteOffset(), "a subarray of a subarray adds the offsets");
        TypedTestSupport.checkElements(new double[] { 30, 40 }, sub2, "nested subarray");
        TypedTestSupport.checkElements(new double[] { 40, 50 }, (JsObject) m(a, "subarray", -2), "subarray(-2)");
        TypedTestSupport.checkElements(new double[0], (JsObject) m(a, "subarray", 4, 2), "empty subarray");
    }

    /**
     * set copies arrays and typed arrays, also overlapping ones, and checks offsets and sizes.
     * <p>
     * set 은 배열과 형식화 배열을 복사하며 겹치는 경우도 처리하고 위치와 크기를 검사한다.
     */
    public void testSet() {
        TypedArrayObject a = u8(0, 0, 0, 0, 0, 0);
        m(a, "set", 0);
        Check.same(Js.UNDEFINED, TypedTestSupport.call(realm, a, "set", TypedTestSupport.arr(realm, 1, 2), TypedTestSupport.n(1)), "set returns undefined");
        TypedTestSupport.checkElements(new double[] { 0, 1, 2, 0, 0, 0 }, a, "set([1, 2], 1)");
        JsObject arrayLike = realm.newObject();
        arrayLike.set("length", TypedTestSupport.n(2));
        arrayLike.set("0", TypedTestSupport.s("8"));
        arrayLike.set("1", Js.TRUE);
        TypedTestSupport.call(realm, a, "set", arrayLike, TypedTestSupport.n(4));
        TypedTestSupport.checkElements(new double[] { 0, 1, 2, 0, 8, 1 }, a, "array-like source");
        TypedArrayObject bytes = u8(1, 2, 3, 4, 5, 6, 7, 8);
        TypedTestSupport.call(realm, bytes, "set", m(bytes, "subarray", 0, 4), TypedTestSupport.n(2));
        TypedTestSupport.checkElements(new double[] { 1, 2, 1, 2, 3, 4, 7, 8 }, bytes, "overlapping copy within one buffer");
        TypedArrayObject f = TypedTestSupport.ta(realm, "Float32Array", 1.5, 300.7, -1);
        TypedArrayObject target = u8(0, 0, 0, 0);
        TypedTestSupport.call(realm, target, "set", f, TypedTestSupport.n(1));
        TypedTestSupport.checkElements(new double[] { 0, 1, 44, 255 }, target, "typed array of another type is converted");
        ArrayBufferObject shared = (ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(8));
        TypedArrayObject small = (TypedArrayObject) TypedTestSupport.view(realm, "Uint8Array", shared);
        TypedArrayObject wide = (TypedArrayObject) TypedTestSupport.view(realm, "Uint16Array", shared);
        small.setElement(0, 1);
        small.setElement(1, 2);
        small.setElement(2, 3);
        small.setElement(3, 4);
        TypedTestSupport.call(realm, wide, "set", m(small, "subarray", 0, 3), TypedTestSupport.n(1));
        TypedTestSupport.checkElements(new double[] { 513, 1, 2, 3 }, wide, "overlapping copy between different types uses the original values");
        final TypedArrayObject t = u8(0, 0, 0);
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, t, "set", TypedTestSupport.arr(realm, 1, 2, 3), TypedTestSupport.n(1));
            }
        }, "source too long for the offset");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, t, "set", TypedTestSupport.arr(realm, 1), TypedTestSupport.n(-1));
            }
        }, "negative offset");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, t, "set");
            }
        }, "undefined source");
        TypedTestSupport.eq(0, TypedTestSupport.num(m(t, "indexOf", 0)), "the failed calls changed nothing");
    }

    /**
     * Iteration callbacks receive (value, index, array) with the given this value.
     * <p>
     * 반복 콜백은 (값, 인덱스, 배열)을 받으며 주어진 this 값을 쓴다.
     */
    public void testCallbackArguments() {
        TypedArrayObject a = u8(5, 6);
        JsObject self = realm.newObject();
        String[] methods = { "forEach", "every", "some", "find", "findIndex", "map", "filter" };
        // 각 메서드가 콜백에 같은 모양의 인수를 넘기고 모든 요소를 방문하는지 확인한다.
        for (int i = 0; i < methods.length; i++) {
            Recorder rec = new Recorder(a, self, "every".equals(methods[i]) ? Js.TRUE : Js.FALSE);
            TypedTestSupport.call(realm, a, methods[i], TypedTestSupport.fn(realm, "cb", 3, rec), self);
            Check.equal(2, rec.calls.size(), methods[i] + ": number of calls");
            Check.equal("5,0,true,true", rec.calls.get(0), methods[i] + ": first call (value, index, array, this)");
            Check.equal("6,1,true,true", rec.calls.get(1), methods[i] + ": second call");
        }
    }

    /**
     * Tests the predicate methods every, some, find, findIndex and the visiting method forEach.
     * <p>
     * 조건 메서드 every, some, find, findIndex 와 순회 메서드 forEach 를 시험한다.
     */
    public void testPredicates() {
        final TypedArrayObject a = u8(1, 2, 3, 4);
        JsObject even = TypedTestSupport.fn(realm, "even", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(TypedTestSupport.num(args[0]) % 2 == 0);
            }
        });
        JsObject small = TypedTestSupport.fn(realm, "small", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(TypedTestSupport.num(args[0]) < 10);
            }
        });
        Check.isFalse(Ops.toBoolean(TypedTestSupport.call(realm, a, "every", even)), "every even");
        Check.isTrue(Ops.toBoolean(TypedTestSupport.call(realm, a, "every", small)), "every small");
        Check.isTrue(Ops.toBoolean(TypedTestSupport.call(realm, a, "some", even)), "some even");
        Check.isFalse(Ops.toBoolean(TypedTestSupport.call(realm, u8(), "some", even)), "some on an empty array");
        Check.isTrue(Ops.toBoolean(TypedTestSupport.call(realm, u8(), "every", even)), "every on an empty array");
        TypedTestSupport.eq(2, TypedTestSupport.num(TypedTestSupport.call(realm, a, "find", even)), "find");
        TypedTestSupport.eq(1, TypedTestSupport.num(TypedTestSupport.call(realm, a, "findIndex", even)), "findIndex");
        TypedTestSupport.eq(4, TypedTestSupport.num(TypedTestSupport.call(realm, a, "findLast", even)), "findLast");
        TypedTestSupport.eq(3, TypedTestSupport.num(TypedTestSupport.call(realm, a, "findLastIndex", even)), "findLastIndex");
        Check.isTrue(TypedTestSupport.call(realm, u8(1, 3), "find", even).isUndefined(), "find without match");
        TypedTestSupport.eq(-1, TypedTestSupport.num(TypedTestSupport.call(realm, u8(1, 3), "findIndex", even)), "findIndex without match");
        final double[] sum = new double[1];
        TypedTestSupport.call(realm, a, "forEach", TypedTestSupport.fn(realm, "add", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                sum[0] += TypedTestSupport.num(args[0]);
                return Js.UNDEFINED;
            }
        }));
        TypedTestSupport.eq(10, sum[0], "forEach visits every element");
    }

    /**
     * Copies into the intrinsic type and shares numeric sort semantics with sort.
     * <p>
     * 내장 형식의 배열로 복사하고 sort와 같은 수치 정렬을 적용한다.
     */
    public void testCopyMethodsUseIntrinsicElementType() {
        String source = "class Derived extends Int16Array {}"
                + "let a=new Derived([10,2,1]);Object.defineProperty(a,'constructor',{get(){throw 'species';}});"
                + "let reverse=a.toReversed(),changed=a.with(-0.5,3);"
                + "let sorted=a.toSorted((x,y)=>{a[0]=99;return x-y;});"
                + "let f=new Float64Array([NaN,0,2,-0,-3]).toSorted();"
                + "let b=new BigInt64Array([10n,-1n,2n]);"
                + "[Object.getPrototypeOf(reverse)===Int16Array.prototype,"
                + " Object.getPrototypeOf(changed)===Int16Array.prototype,Object.getPrototypeOf(sorted)===Int16Array.prototype,"
                + " reverse.join('/'),changed.join('/'),sorted.join('/'),a.join('/'),"
                + " f.join('/'),Object.is(f[1],-0),b.toSorted().join('/'),b.with(-1,'8').join('/')].join('|');";
        Check.equal("true|true|true|1/2/10|3/2/1|1/2/10|99/2/1|-3/0/0/2/NaN|true|-1/2/10|10/-1/8",
                Ops.toString(JsRealm.create().evaluateScript(source, "typed-copy.js")), "species, numeric sort and BigInt");
    }

    /**
     * Observes a detach during descending search and replacement conversion.
     * <p>
     * 역방향 탐색과 교체 값 변환 도중 버퍼가 분리되는 경우를 확인한다.
     */
    public void testFindLastAndWithAfterDetach() {
        final TypedArrayObject searching = u8(1, 2, 3);
        final List<String> seen = new ArrayList<String>();
        JsValue predicate = TypedTestSupport.fn(realm, "predicate", 3, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                seen.add(Ops.toString(args[1]) + ":" + Ops.toString(args[0]));
                if (seen.size() == 1) {
                    searching.getBuffer().detach();
                }
                return Js.FALSE;
            }
        });
        TypedTestSupport.eq(-1, TypedTestSupport.num(TypedTestSupport.call(realm, searching, "findLastIndex", predicate)), "no match");
        Check.equal("[2:3, 1:undefined, 0:undefined]", seen.toString(), "reads each remaining index after detach");
        final TypedArrayObject updating = u8(1, 2);
        final JsObject value = realm.newObject();
        value.defineBuiltin("valueOf", TypedTestSupport.fn(realm, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                updating.getBuffer().detach();
                return Js.num(3);
            }
        }));
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, updating, "with", Js.num(0), value);
            }
        }, "replacement conversion can invalidate the target index");
        Check.isTrue(updating.getBuffer().isDetached(), "replacement conversion ran");
    }

    /**
     * map and filter create arrays of the same type (through the species constructor).
     * <p>
     * map 과 filter 는 (species 생성자를 거쳐) 같은 형식의 배열을 만든다.
     */
    public void testMapAndFilter() {
        JsObject twice = TypedTestSupport.fn(realm, "twice", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedTestSupport.n(TypedTestSupport.num(args[0]) * 2);
            }
        });
        TypedArrayObject m = (TypedArrayObject) TypedTestSupport.call(realm, u8(1, 100, 200), "map", twice);
        TypedTestSupport.checkElements(new double[] { 2, 200, 144 }, m, "map converts to the element type");
        Check.equal(ElementType.UINT8, m.getType(), "map keeps the type");
        JsObject odd = TypedTestSupport.fn(realm, "odd", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(TypedTestSupport.num(args[0]) % 2 == 1);
            }
        });
        TypedTestSupport.checkElements(new double[] { 1, 3, 5 }, (JsObject) TypedTestSupport.call(realm, u8(1, 2, 3, 4, 5), "filter", odd), "filter");
        Check.equal(0, ((TypedArrayObject) TypedTestSupport.call(realm, u8(2, 4), "filter", odd)).getLength(), "filter without match");
    }

    /**
     * The species constructor decides the result type; invalid results are TypeErrors.
     * <p>
     * species 생성자가 결과 형식을 정하며 잘못된 결과는 TypeError 이다.
     */
    public void testSpeciesCreate() {
        final TypedArrayObject a = u8(1, 2, 3);
        JsObject c = realm.newObject();
        final JsObject u16 = TypedTestSupport.ctor(realm, "Uint16Array");
        c.defineData(WellKnownSymbols.SPECIES, u16, true, false, true);
        a.defineData("constructor", c, true, false, true);
        JsObject identity = TypedTestSupport.fn(realm, "id", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return args[0];
            }
        });
        TypedArrayObject mapped = (TypedArrayObject) TypedTestSupport.call(realm, a, "map", identity);
        Check.equal(ElementType.UINT16, mapped.getType(), "map result made by the species constructor");
        Check.equal(ElementType.UINT16, ((TypedArrayObject) m(a, "slice", 1)).getType(), "slice result made by the species constructor");
        Check.equal(ElementType.UINT16, ((TypedArrayObject) m(a, "subarray", 0, 1)).getType(), "subarray result made by the species constructor (views the same buffer)");
        c.defineData(WellKnownSymbols.SPECIES, TypedTestSupport.fn(realm, "notConstructor", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }), true, false, true);
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                m(a, "slice", 0);
            }
        }, "species that is not a constructor");
        NativeFunction notTyped = new NativeFunction(realm, "NotTyped", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return r.newObject();
            }
        });
        c.defineData(WellKnownSymbols.SPECIES, notTyped, true, false, true);
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                m(a, "slice", 0);
            }
        }, "species result that is not a typed array");
        NativeFunction tooShort = new NativeFunction(realm, "TooShort", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return Ops.construct(u16, new JsValue[] { TypedTestSupport.n(0) });
            }
        });
        c.defineData(WellKnownSymbols.SPECIES, tooShort, true, false, true);
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                m(a, "slice", 0);
            }
        }, "species result shorter than requested");
        c.defineData(WellKnownSymbols.SPECIES, Js.NULL, true, false, true);
        Check.equal(ElementType.UINT8, ((TypedArrayObject) m(a, "slice", 0)).getType(), "a null species means the default constructor");
        a.defineData("constructor", Js.UNDEFINED, true, false, true);
        Check.equal(ElementType.UINT8, ((TypedArrayObject) m(a, "slice", 0)).getType(), "an undefined constructor means the default constructor");
    }

    /**
     * reduce and reduceRight with and without an initial value.
     * <p>
     * 초기값이 있을 때와 없을 때의 reduce 와 reduceRight.
     */
    public void testReduce() {
        final TypedArrayObject a = u8(1, 2, 3, 4);
        JsObject concat = TypedTestSupport.fn(realm, "concat", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedTestSupport.s(Ops.toString(args[0]) + Ops.toString(args[1]));
            }
        });
        Check.equal("1234", TypedTestSupport.str(TypedTestSupport.call(realm, a, "reduce", concat)), "reduce without initial value");
        Check.equal("x1234", TypedTestSupport.str(TypedTestSupport.call(realm, a, "reduce", concat, TypedTestSupport.s("x"))), "reduce with initial value");
        Check.equal("4321", TypedTestSupport.str(TypedTestSupport.call(realm, a, "reduceRight", concat)), "reduceRight without initial value");
        Check.equal("x4321", TypedTestSupport.str(TypedTestSupport.call(realm, a, "reduceRight", concat, TypedTestSupport.s("x"))), "reduceRight with initial value");
        TypedTestSupport.eq(7, TypedTestSupport.num(TypedTestSupport.call(realm, u8(7), "reduce", concat)), "a single element is returned unchanged");
        Check.equal("init", TypedTestSupport.str(TypedTestSupport.call(realm, u8(), "reduce", concat, TypedTestSupport.s("init"))), "empty array with an initial value");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, u8(), "reduce", fnIdentity());
            }
        }, "reduce of an empty array without initial value");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, u8(), "reduceRight", fnIdentity());
            }
        }, "reduceRight of an empty array without initial value");
        Check.equal("2,1,true;3,2,true", reduceTrace(u8(1, 2, 3)), "reduce passes the element, index and array (after the initial accumulator)");
    }

    /**
     * A callback that returns its first argument.
     * <p>
     * 첫 인수를 돌려주는 콜백.
     *
     * @return the function / 함수
     */
    private JsObject fnIdentity() {
        return TypedTestSupport.fn(realm, "id", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return args[0];
            }
        });
    }

    /**
     * Runs reduce without initial value and records element, index and whether the array argument is right.
     * <p>
     * 초기값 없이 reduce 를 실행하고 요소, 인덱스, 배열 인수의 올바름을 기록한다.
     *
     * @param a the array / 배열
     * @return the trace / 기록
     */
    private String reduceTrace(final TypedArrayObject a) {
        final StringBuilder sb = new StringBuilder();
        TypedTestSupport.call(realm, a, "reduce", TypedTestSupport.fn(realm, "trace", 4, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(Ops.toString(args[1])).append(',').append(Ops.toString(args[2])).append(',').append(args[3] == a);
                return args[0];
            }
        }));
        return sb.toString();
    }

    /**
     * The default sort order is numeric (not text), NaN last and -0 before +0; a comparison function is honored and the sort is stable.
     * <p>
     * 기본 정렬 순서는 (문자열이 아닌) 숫자 순서이고 NaN 은 맨 뒤, -0 은 +0 앞이다. 비교 함수를 따르며 정렬은 안정적이다.
     */
    public void testSort() {
        TypedTestSupport.checkElements(new double[] { 1, 9, 10, 100 }, (JsObject) m(u8(100, 10, 9, 1), "sort"), "numeric, not lexicographic");
        TypedTestSupport.checkElements(new double[] { -5, -1, 3 }, (JsObject) m(TypedTestSupport.ta(realm, "Int8Array", 3, -1, -5), "sort"), "negative numbers");
        TypedArrayObject f = TypedTestSupport.ta(realm, "Float64Array", 3, Double.NaN, 0.0, -0.0, -1, Double.NaN, Double.NEGATIVE_INFINITY);
        TypedTestSupport.checkElements(new double[] { Double.NEGATIVE_INFINITY, -1, -0.0, 0.0, 3, Double.NaN, Double.NaN }, (JsObject) m(f, "sort"),
                "NaN last, -0 before +0");
        JsObject descending = TypedTestSupport.fn(realm, "desc", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedTestSupport.n(TypedTestSupport.num(args[1]) - TypedTestSupport.num(args[0]));
            }
        });
        TypedTestSupport.checkElements(new double[] { 4, 3, 2, 1 }, (JsObject) TypedTestSupport.call(realm, u8(2, 4, 1, 3), "sort", descending), "comparison function");
        JsObject byLastDigit = TypedTestSupport.fn(realm, "digit", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedTestSupport.n(TypedTestSupport.num(args[0]) % 10 - TypedTestSupport.num(args[1]) % 10);
            }
        });
        TypedTestSupport.checkElements(new double[] { 11, 21, 22, 12 }, (JsObject) TypedTestSupport.call(realm, u8(22, 11, 21, 12), "sort", byLastDigit),
                "the sort is stable (equal keys keep their order)");
        JsObject nanCompare = TypedTestSupport.fn(realm, "nan", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedTestSupport.n(Double.NaN);
            }
        });
        TypedTestSupport.checkElements(new double[] { 3, 1, 2 }, (JsObject) TypedTestSupport.call(realm, u8(3, 1, 2), "sort", nanCompare), "NaN results count as equal");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, u8(1), "sort", TypedTestSupport.n(1));
            }
        }, "non-callable comparison function");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, u8(2, 1, 3), "sort", TypedTestSupport.fn(realm, "boom", 2, new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                        throw JsException.rangeError("from the comparison function");
                    }
                }));
            }
        }, "exceptions of the comparison function propagate");
        double[] big = new double[500];
        // 섞인 큰 입력을 만들어 정렬 결과가 오름차순인지 확인한다.
        for (int i = 0; i < big.length; i++) {
            big[i] = (i * 7919) % 1000;
        }
        double[] sorted = TypedTestSupport.elements((JsObject) m(TypedTestSupport.ta(realm, "Uint16Array", big), "sort"));
        // 이웃한 요소가 항상 오름차순인지 확인한다.
        for (int i = 1; i < sorted.length; i++) {
            Check.isTrue(sorted[i - 1] <= sorted[i], "ascending at " + i);
        }
    }

    /**
     * keys, values, entries and [Symbol.iterator]: iterator objects that end properly.
     * <p>
     * keys, values, entries, [Symbol.iterator]: 올바르게 끝나는 이터레이터 객체.
     */
    public void testIterators() {
        TypedArrayObject a = u8(10, 20);
        JsObject proto = realm.getIntrinsic("%TypedArrayPrototype%");
        Check.same(proto.get("values"), proto.get(WellKnownSymbols.ITERATOR), "[Symbol.iterator] is the same function as values");
        JsObject values = (JsObject) m(a, "values");
        Check.same(values, Ops.invoke(realm, values, WellKnownSymbols.ITERATOR, Js.NO_ARGS), "the iterator is its own iterable");
        JsObject it = (JsObject) m(a, "values");
        checkNext(it, 10, false);
        checkNext(it, 20, false);
        checkNext(it, Double.NaN, true);
        checkNext(it, Double.NaN, true);
        JsObject keys = (JsObject) m(a, "keys");
        checkNext(keys, 0, false);
        checkNext(keys, 1, false);
        checkNext(keys, Double.NaN, true);
        JsObject entries = (JsObject) m(a, "entries");
        JsObject first = (JsObject) ((JsObject) TypedTestSupport.call(realm, entries, "next")).get("value");
        TypedTestSupport.eq(0, TypedTestSupport.num(first.get("0")), "entries index");
        TypedTestSupport.eq(10, TypedTestSupport.num(first.get("1")), "entries value");
        Check.equal("Array Iterator", TypedTestSupport.str(it.get(WellKnownSymbols.TO_STRING_TAG)), "@@toStringTag of the iterator");
    }

    /**
     * Calls next and checks the result.
     * <p>
     * next 를 호출하고 결과를 확인한다.
     *
     * @param it    the iterator / 이터레이터
     * @param value the expected value (ignored when done) / 기대하는 값(done 이면 무시)
     * @param done  the expected done flag / 기대하는 done 값
     */
    private void checkNext(JsObject it, double value, boolean done) {
        JsObject r = (JsObject) TypedTestSupport.call(realm, it, "next");
        Check.equal(done, Ops.toBoolean(r.get("done")), "done");
        if (done) {
            Check.isTrue(r.get("value").isUndefined(), "value of the final result");
        } else {
            TypedTestSupport.eq(value, TypedTestSupport.num(r.get("value")), "value");
        }
    }

    /**
     * The accessors report the view properties and reject other receivers; @@toStringTag is undefined for them.
     * <p>
     * 접근자는 뷰 속성을 보고하고 다른 수신자를 거부한다. 그 경우 @@toStringTag 는 undefined 이다.
     */
    public void testAccessors() {
        TypedArrayObject a = (TypedArrayObject) TypedTestSupport.view(realm, "Int16Array", TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(16)), 4, 3);
        TypedTestSupport.eq(3, TypedTestSupport.num(a.get("length")), "length");
        TypedTestSupport.eq(6, TypedTestSupport.num(a.get("byteLength")), "byteLength");
        TypedTestSupport.eq(4, TypedTestSupport.num(a.get("byteOffset")), "byteOffset");
        Check.isTrue(a.get("buffer") instanceof ArrayBufferObject, "buffer");
        Check.equal("Int16Array", TypedTestSupport.str(a.get(WellKnownSymbols.TO_STRING_TAG)), "@@toStringTag");
        final JsObject proto = realm.getIntrinsic("%TypedArrayPrototype%");
        String[] names = { "length", "byteLength", "byteOffset", "buffer" };
        // 접근자 이름마다 프로토타입 자신을 수신자로 읽으면 TypeError 여야 한다.
        for (int i = 0; i < names.length; i++) {
            final String n = names[i];
            TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
                public void run() {
                    proto.get(n);
                }
            }, n + " on the prototype");
        }
        Check.isTrue(proto.get(WellKnownSymbols.TO_STRING_TAG).isUndefined(), "@@toStringTag on the prototype is undefined");
    }

    /**
     * The methods reject receivers that are not typed arrays and arguments that are not callable.
     * <p>
     * 메서드는 형식화 배열이 아닌 수신자와 호출할 수 없는 인수를 거부한다.
     */
    public void testReceiverAndArgumentChecks() {
        final JsObject proto = realm.getIntrinsic("%TypedArrayPrototype%");
        String[] methods = { "copyWithin", "entries", "every", "fill", "filter", "find", "findIndex", "forEach", "indexOf", "join", "keys", "lastIndexOf",
            "map", "reduce", "reduceRight", "reverse", "set", "slice", "some", "sort", "subarray", "toLocaleString", "values" };
        // 모든 메서드가 일반 객체와 배열 수신자를 거부하는지 확인한다.
        for (int i = 0; i < methods.length; i++) {
            final String name = methods[i];
            TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
                public void run() {
                    Ops.call(proto.get(name), realm.newObject(), new JsValue[] { TypedTestSupport.fn(realm, "f", 0, new NativeFunction.Call() {
                        public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                            return Js.UNDEFINED;
                        }
                    }) });
                }
            }, name + " on a plain object");
            TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
                public void run() {
                    Ops.call(proto.get(name), TypedTestSupport.arr(realm, 1, 2), new JsValue[0]);
                }
            }, name + " on an array");
        }
        String[] callbackMethods = { "every", "filter", "find", "findIndex", "findLast", "findLastIndex", "forEach", "map", "reduce", "reduceRight", "some" };
        // 콜백이 필요한 메서드에 함수가 아닌 값을 주면 TypeError 여야 한다.
        for (int i = 0; i < callbackMethods.length; i++) {
            final String name = callbackMethods[i];
            TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
                public void run() {
                    TypedTestSupport.call(realm, u8(1), name, TypedTestSupport.n(1));
                }
            }, name + " with a non-callable argument");
        }
    }

    /**
     * Methods on an array whose buffer is detached throw a TypeError (ValidateTypedArray), the length reads as 0.
     * <p>
     * 버퍼가 분리된 배열의 메서드는 TypeError(ValidateTypedArray)를 던지고 length 는 0 으로 읽힌다.
     */
    public void testDetachedBuffer() {
        final TypedArrayObject a = u8(1, 2, 3);
        a.getBuffer().detach();
        TypedTestSupport.eq(0, TypedTestSupport.num(a.get("length")), "length of a detached array");
        String[] methods = { "copyWithin", "entries", "every", "fill", "filter", "find", "findIndex", "findLast", "findLastIndex", "forEach",
            "indexOf", "join", "keys", "lastIndexOf", "map", "reduce", "reduceRight", "reverse", "slice", "some", "sort", "toLocaleString",
            "toReversed", "toSorted", "values", "with" };
        // 분리된 버퍼 위의 배열에는 어떤 메서드도 쓸 수 없다.
        for (int i = 0; i < methods.length; i++) {
            final String name = methods[i];
            TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
                public void run() {
                    TypedTestSupport.call(realm, a, name, fnIdentity());
                }
            }, name + " on a detached array");
        }
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, a, "set", TypedTestSupport.arr(realm, 1));
            }
        }, "set on a detached array");
    }

    /**
     * toLocaleString calls toLocaleString of every element and joins with commas.
     * <p>
     * toLocaleString 은 요소마다 toLocaleString 을 호출해 쉼표로 이어 붙인다.
     */
    public void testToLocaleString() {
        JsObject numberProto = realm.getNumberPrototype();
        numberProto.defineBuiltin("toLocaleString", TypedTestSupport.fn(realm, "toLocaleString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedTestSupport.s("<" + Ops.toString(thisValue) + ">");
            }
        }));
        Check.equal("<1>,<2>", TypedTestSupport.str(m(u8(1, 2), "toLocaleString")), "toLocaleString of two elements");
        Check.equal("", TypedTestSupport.str(m(u8(), "toLocaleString")), "empty array");
    }
}
