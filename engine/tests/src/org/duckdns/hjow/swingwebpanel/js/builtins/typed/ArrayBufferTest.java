package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of ArrayBuffer (ECMA-262 6th edition, 24.1; plan item P14-06): constructor conversions and limits, byteLength, isView, slice with
 * species, the toStringTag and the species getter.
 * <p>
 * ArrayBuffer 시험(ECMA-262 6판, 24.1, 계획 P14-06): 생성자의 변환과 제한, byteLength, isView, species 를 쓰는 slice, toStringTag, species getter.
 */
public class ArrayBufferTest {

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
    public ArrayBufferTest() {
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
     * Calling ArrayBuffer without new is a TypeError.
     * <p>
     * new 없이 ArrayBuffer 를 호출하면 TypeError.
     */
    public void testRequiresNew() {
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                Ops.call(TypedTestSupport.ctor(realm, "ArrayBuffer"), Js.UNDEFINED, new JsValue[] { TypedTestSupport.n(1) });
            }
        }, "ArrayBuffer(1)");
    }

    /**
     * The length argument is converted with ToIndex: strings and fractions are accepted, negative and huge values are a RangeError.
     * <p>
     * 길이 인수는 ToIndex 로 변환한다: 문자열과 소수는 받아들이고 음수와 너무 큰 값은 RangeError.
     */
    public void testLengthConversion() {
        Check.equal(0, ((ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer")).getByteLength(), "no argument");
        Check.equal(8, ((ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(8))).getByteLength(), "8");
        Check.equal(3, ((ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.s("3"))).getByteLength(), "string 3");
        Check.equal(1, ((ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(1.9))).getByteLength(), "1.9");
        Check.equal(0, ((ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(Double.NaN))).getByteLength(), "NaN");
        Check.equal(0, ((ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", Js.UNDEFINED)).getByteLength(), "undefined");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(-1));
            }
        }, "-1");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(Double.POSITIVE_INFINITY));
            }
        }, "Infinity");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(4294967296.0 * 4));
            }
        }, "larger than any Java array");
    }

    /**
     * A new buffer is zero filled.
     * <p>
     * 새 버퍼는 0 으로 채워져 있다.
     */
    public void testBufferIsZeroFilled() {
        ArrayBufferObject b = (ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(16));
        byte[] data = b.getData();
        // 모든 바이트가 0 인지 확인한다.
        for (int i = 0; i < data.length; i++) {
            Check.equal(0, data[i], "byte " + i);
        }
    }

    /**
     * The byteLength getter checks its this value and the prototype chain is correct.
     * <p>
     * byteLength getter 는 this 값을 검사하고 프로토타입 체인이 올바르다.
     */
    public void testByteLengthGetterAndPrototype() {
        final JsObject proto = (JsObject) TypedTestSupport.ctor(realm, "ArrayBuffer").get("prototype");
        JsObject b = TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(5));
        Check.same(proto, b.getPrototypeOf(), "[[Prototype]] of an instance");
        Check.same(realm.getObjectPrototype(), proto.getPrototypeOf(), "[[Prototype]] of ArrayBuffer.prototype");
        TypedTestSupport.eq(5, TypedTestSupport.num(b.get("byteLength")), "byteLength");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                proto.get("byteLength");
            }
        }, "byteLength on the prototype itself");
        Check.same(TypedTestSupport.ctor(realm, "ArrayBuffer"), proto.get("constructor"), "constructor");
        Check.equal("ArrayBuffer", TypedTestSupport.str(b.get(WellKnownSymbols.TO_STRING_TAG)), "@@toStringTag");
    }

    /**
     * ArrayBuffer.isView is true for typed arrays and DataViews only.
     * <p>
     * ArrayBuffer.isView 는 형식화 배열과 DataView 에 대해서만 true.
     */
    public void testIsView() {
        JsObject buf = TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(8));
        // 버퍼 위의 뷰는 true, 그 밖의 값은 false 이다.
        Check.isTrue(isView(buf) == false, "buffer is not a view");
        Check.isTrue(isView(TypedTestSupport.make(realm, "DataView", buf)), "DataView is a view");
        Check.isTrue(isView(realm.newObject()) == false, "object");
        Check.isTrue(isView(Js.UNDEFINED) == false, "undefined");
        Check.isTrue(isView(TypedTestSupport.n(1)) == false, "number");
    }

    /**
     * Calls ArrayBuffer.isView.
     * <p>
     * ArrayBuffer.isView 를 호출한다.
     *
     * @param v the argument / 인수
     * @return the result / 결과
     */
    private boolean isView(JsValue v) {
        return Ops.toBoolean(TypedTestSupport.call(realm, TypedTestSupport.ctor(realm, "ArrayBuffer"), "isView", v));
    }

    /**
     * slice copies a range with relative indices and clamping.
     * <p>
     * slice 는 상대 인덱스와 제한을 적용해 구간을 복사한다.
     */
    public void testSlice() {
        ArrayBufferObject b = (ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(8));
        // 바이트에 0..7 을 채운다.
        for (int i = 0; i < 8; i++) {
            b.getData()[i] = (byte) i;
        }
        Check.equal(new byte[] { 2, 3, 4 }, slice(b, 2, 5), "slice(2,5)");
        Check.equal(new byte[] { 6, 7 }, slice(b, -2, Double.NaN), "slice(-2)");
        Check.equal(new byte[] { 0, 1, 2, 3, 4, 5, 6 }, slice(b, 0, -1), "slice(0,-1)");
        Check.equal(new byte[0], slice(b, 5, 2), "start after end");
        Check.equal(new byte[] { 5, 6, 7 }, slice(b, 5, 100), "end beyond the length");
        Check.equal(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 }, slice(b, -100, 100), "start before the beginning");
        JsObject copy = (JsObject) TypedTestSupport.call(realm, b, "slice");
        TypedTestSupport.eq(8, TypedTestSupport.num(copy.get("byteLength")), "slice() copies everything");
        Check.isTrue(copy != b, "slice creates a new buffer");
        ((ArrayBufferObject) copy).getData()[0] = 99;
        Check.equal(0, b.getData()[0], "the copy is independent");
    }

    /**
     * Slices with start and an optional end and returns the bytes.
     * <p>
     * 시작과 선택적인 끝으로 slice 하고 바이트를 반환한다.
     *
     * @param b     the buffer / 버퍼
     * @param start the start / 시작
     * @param end   the end (NaN means undefined) / 끝(NaN 이면 undefined)
     * @return the bytes of the result / 결과의 바이트
     */
    private byte[] slice(ArrayBufferObject b, double start, double end) {
        JsValue e = Double.isNaN(end) ? Js.UNDEFINED : (JsValue) TypedTestSupport.n(end);
        ArrayBufferObject r = (ArrayBufferObject) TypedTestSupport.call(realm, b, "slice", TypedTestSupport.n(start), e);
        return r.getData();
    }

    /**
     * slice rejects a non-buffer this value.
     * <p>
     * slice 는 버퍼가 아닌 this 값을 거부한다.
     */
    public void testSliceChecksThis() {
        final JsObject proto = (JsObject) TypedTestSupport.ctor(realm, "ArrayBuffer").get("prototype");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                Ops.call(proto.get("slice"), realm.newObject(), new JsValue[0]);
            }
        }, "slice with an object as this");
    }

    /**
     * slice uses the species constructor and validates what it returns.
     * <p>
     * slice 는 species 생성자를 쓰고 그것이 돌려준 것을 검증한다.
     */
    public void testSliceSpecies() {
        final JsObject abCtor = TypedTestSupport.ctor(realm, "ArrayBuffer");
        final ArrayBufferObject b = (ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(8));
        // species 가 undefined 이면 기본 생성자를 쓴다.
        b.defineData("constructor", abCtor, true, false, true);
        Check.isTrue(TypedTestSupport.call(realm, b, "slice", TypedTestSupport.n(1)) instanceof ArrayBufferObject, "default species");
        final int[] created = new int[1];
        NativeFunction ctor = new NativeFunction(realm, "Custom", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                created[0]++;
                return Ops.construct(abCtor, new JsValue[] { TypedTestSupport.n(TypedTestSupport.num(args[0]) + 4) });
            }
        });
        JsObject c = realm.newObject();
        c.defineData(WellKnownSymbols.SPECIES, ctor, true, false, true);
        b.defineData("constructor", c, true, false, true);
        JsObject r = (JsObject) TypedTestSupport.call(realm, b, "slice", TypedTestSupport.n(0), TypedTestSupport.n(2));
        Check.equal(1, created[0], "the species constructor was used");
        TypedTestSupport.eq(6, TypedTestSupport.num(r.get("byteLength")), "a bigger buffer is allowed");

        // 너무 작은 버퍼, 같은 버퍼, 버퍼가 아닌 객체를 돌려주는 생성자는 TypeError 이다.
        c.defineData(WellKnownSymbols.SPECIES, new NativeFunction(realm, "Small", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm rr, JsValue[] args, JsObject newTarget) {
                return Ops.construct(abCtor, new JsValue[] { TypedTestSupport.n(1) });
            }
        }), true, false, true);
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, b, "slice", TypedTestSupport.n(0), TypedTestSupport.n(4));
            }
        }, "too small result");
        c.defineData(WellKnownSymbols.SPECIES, new NativeFunction(realm, "Same", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm rr, JsValue[] args, JsObject newTarget) {
                return b;
            }
        }), true, false, true);
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, b, "slice", TypedTestSupport.n(0), TypedTestSupport.n(4));
            }
        }, "the same buffer");
        c.defineData(WellKnownSymbols.SPECIES, new NativeFunction(realm, "NotBuffer", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm rr, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm rr, JsValue[] args, JsObject newTarget) {
                return realm.newObject();
            }
        }), true, false, true);
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, b, "slice", TypedTestSupport.n(0), TypedTestSupport.n(4));
            }
        }, "not a buffer");
        b.defineData("constructor", TypedTestSupport.n(1), true, false, true);
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, b, "slice", TypedTestSupport.n(0), TypedTestSupport.n(4));
            }
        }, "constructor that is not an object");
    }

    /**
     * ArrayBuffer[@@species] returns this.
     * <p>
     * ArrayBuffer[@@species] 는 this 를 반환한다.
     */
    public void testSpeciesGetter() {
        JsObject ab = TypedTestSupport.ctor(realm, "ArrayBuffer");
        Check.same(ab, ab.get(WellKnownSymbols.SPECIES), "ArrayBuffer[@@species]");
    }

    /**
     * A detached buffer has length 0 and slice throws a TypeError.
     * <p>
     * 분리된 버퍼는 길이가 0 이고 slice 는 TypeError 를 던진다.
     */
    public void testDetached() {
        final ArrayBufferObject b = (ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(8));
        b.detach();
        Check.isTrue(b.isDetached(), "detached");
        TypedTestSupport.eq(0, TypedTestSupport.num(b.get("byteLength")), "byteLength of a detached buffer");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, b, "slice");
            }
        }, "slice of a detached buffer");
    }
}
