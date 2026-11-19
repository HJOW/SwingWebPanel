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
 * Tests of the static methods <code>from</code> and <code>of</code>, the species getter of <code>%TypedArray%</code>, subclass-like constructors
 * (a constructor that produces typed arrays) and of the stable Java API in {@link TypedArrays} (plan item P14-06).
 * <p>
 * <code>%TypedArray%</code> 의 정적 메서드 <code>from</code>, <code>of</code>, species getter, 하위 클래스 같은 생성자(형식화 배열을 만드는 생성자) 시험과
 * {@link TypedArrays} 의 안정적인 Java API 시험(계획 P14-06).
 */
public class TypedArrayStaticsTest {

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
    public TypedArrayStaticsTest() {
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
     * from converts arrays and array-likes, optionally through a mapping function with a this value.
     * <p>
     * from 은 배열과 유사 배열을 변환하며 this 값을 가진 변환 함수를 거칠 수 있다.
     */
    public void testFrom() {
        JsObject u8 = TypedTestSupport.ctor(realm, "Uint8Array");
        TypedArrayObject a = (TypedArrayObject) TypedTestSupport.call(realm, u8, "from", TypedTestSupport.arr(realm, 1, 2, 300));
        TypedTestSupport.checkElements(new double[] { 1, 2, 44 }, a, "from an array");
        Check.equal(ElementType.UINT8, a.getType(), "the result type is that of the constructor");
        JsObject arrayLike = realm.newObject();
        arrayLike.set("length", TypedTestSupport.n(2));
        arrayLike.set("0", TypedTestSupport.n(9));
        arrayLike.set("1", TypedTestSupport.n(8));
        TypedTestSupport.checkElements(new double[] { 9, 8 }, (JsObject) TypedTestSupport.call(realm, u8, "from", arrayLike), "from an array-like");
        final JsObject thisArg = realm.newObject();
        final boolean[] thisOk = new boolean[1];
        JsObject mapper = TypedTestSupport.fn(realm, "map", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                thisOk[0] = thisValue == thisArg;
                return TypedTestSupport.n(TypedTestSupport.num(args[0]) * 10 + TypedTestSupport.num(args[1]));
            }
        });
        TypedTestSupport.checkElements(new double[] { 10, 21, 32 },
                (JsObject) TypedTestSupport.call(realm, TypedTestSupport.ctor(realm, "Float32Array"), "from", TypedTestSupport.arr(realm, 1, 2, 3), mapper, thisArg),
                "from with a mapping function (value, index)");
        Check.isTrue(thisOk[0], "the mapping function got the this value");
        TypedTestSupport.checkElements(new double[0], (JsObject) TypedTestSupport.call(realm, u8, "from", TypedTestSupport.arr(realm)), "from an empty array");
    }

    /**
     * from uses the iterator of an iterable and reports invalid arguments.
     * <p>
     * from 은 반복 가능 객체의 이터레이터를 쓰고 잘못된 인수를 보고한다.
     */
    public void testFromIterableAndErrors() {
        final JsObject u8 = TypedTestSupport.ctor(realm, "Uint8Array");
        final int[] pos = new int[1];
        JsObject iterable = realm.newObject();
        iterable.defineBuiltin(WellKnownSymbols.ITERATOR, TypedTestSupport.fn(realm, "iter", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject it = r.newObject();
                it.defineBuiltin("next", TypedTestSupport.fn(r, "next", 0, new NativeFunction.Call() {
                    public JsValue call(JsRealm rr, JsValue t, JsValue[] a) {
                        JsObject result = rr.newObject();
                        boolean done = pos[0] >= 2;
                        result.set("done", Js.bool(done));
                        result.set("value", done ? Js.UNDEFINED : (JsValue) TypedTestSupport.n(100 + pos[0]++));
                        return result;
                    }
                }));
                return it;
            }
        }));
        TypedTestSupport.checkElements(new double[] { 100, 101 }, (JsObject) TypedTestSupport.call(realm, u8, "from", iterable), "from an iterable");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, u8, "from", TypedTestSupport.arr(realm, 1), TypedTestSupport.n(5));
            }
        }, "a mapping function that is not callable");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, u8, "from", Js.UNDEFINED);
            }
        }, "undefined source");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                Ops.call(u8.get("from"), realm.newObject(), new JsValue[] { TypedTestSupport.arr(realm, 1) });
            }
        }, "this is not a constructor");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                Ops.call(u8.get("from"), TypedTestSupport.ctor(realm, "ArrayBuffer"), new JsValue[] { TypedTestSupport.arr(realm, 1) });
            }
        }, "a constructor that does not create typed arrays");
    }

    /**
     * of collects its arguments with the constructor it is called on.
     * <p>
     * of 는 호출된 생성자로 인수를 모아 배열을 만든다.
     */
    public void testOf() {
        JsObject i16 = TypedTestSupport.ctor(realm, "Int16Array");
        TypedArrayObject a = (TypedArrayObject) TypedTestSupport.call(realm, i16, "of", TypedTestSupport.n(1), TypedTestSupport.n(-2), TypedTestSupport.n(70000));
        TypedTestSupport.checkElements(new double[] { 1, -2, 4464 }, a, "of(1, -2, 70000)");
        Check.equal(ElementType.INT16, a.getType(), "type of the result");
        Check.equal(0, ((TypedArrayObject) TypedTestSupport.call(realm, i16, "of")).getLength(), "of()");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                Ops.call(realm.getIntrinsic("%Int16Array%").get("of"), Js.UNDEFINED, new JsValue[] { TypedTestSupport.n(1) });
            }
        }, "this is not a constructor");
    }

    /**
     * %TypedArray%[@@species] returns the this value.
     * <p>
     * %TypedArray%[@@species] 는 this 값을 반환한다.
     */
    public void testSpeciesGetter() {
        JsObject abstractCtor = realm.getIntrinsic("%TypedArray%");
        Check.same(abstractCtor, abstractCtor.get(WellKnownSymbols.SPECIES), "%TypedArray%[@@species]");
        JsObject f64 = TypedTestSupport.ctor(realm, "Float64Array");
        Check.same(f64, f64.get(WellKnownSymbols.SPECIES), "Float64Array[@@species] (inherited getter, this is the receiver)");
    }

    /**
     * The Java API creates and reads buffers and views without copying.
     * <p>
     * Java API 는 버퍼와 뷰를 복사 없이 만들고 읽는다.
     */
    public void testJavaApi() {
        byte[] bytes = { 1, 2, 3, 4, 5, 6, 7, 8 };
        ArrayBufferObject buffer = TypedArrays.newArrayBuffer(realm, bytes);
        Check.same(bytes, buffer.getData(), "the array is wrapped, not copied");
        Check.isTrue(TypedArrays.isArrayBuffer(buffer), "isArrayBuffer");
        TypedArrayObject u8 = TypedArrays.newUint8Array(realm, buffer, 2, 4);
        Check.isTrue(TypedArrays.isTypedArray(u8), "isTypedArray");
        TypedTestSupport.checkElements(new double[] { 3, 4, 5, 6 }, u8, "Uint8Array view");
        u8.set("0", TypedTestSupport.n(99));
        Check.equal(99, bytes[2], "writes through the script object are visible in the Java array");
        bytes[3] = 77;
        TypedTestSupport.eq(77, TypedTestSupport.num(u8.get("1")), "writes to the Java array are visible to scripts");
        TypedArrays.ByteView view = TypedArrays.bytesOf(u8);
        Check.same(bytes, view.array, "bytesOf shares the array");
        Check.equal(2, view.offset, "bytesOf offset");
        Check.equal(4, view.length, "bytesOf length");
        TypedArrayObject clamped = TypedArrays.newUint8ClampedArray(realm, buffer, 0, 8);
        clamped.set("0", TypedTestSupport.n(1000));
        Check.equal(255, bytes[0] & 0xFF, "Uint8ClampedArray clamps");
        TypedArrayObject f32 = TypedArrays.newFloat32Array(realm, buffer, 4, 1);
        Check.equal(ElementType.FLOAT32, f32.getType(), "Float32Array");
        TypedArrays.ByteView dv = TypedArrays.bytesOf(TypedTestSupport.view(realm, "DataView", buffer, 1, 3));
        Check.equal(1, dv.offset, "DataView offset");
        Check.equal(3, dv.length, "DataView length");
        Check.equal(8, TypedArrays.bytesOf(buffer).length, "ArrayBuffer length");
        Check.isNull(TypedArrays.bytesOf(realm.newObject()), "other values have no bytes");
        TypedArrayObject whole = TypedArrays.newUint8Array(realm, new byte[] { 9, 8 });
        TypedTestSupport.checkElements(new double[] { 9, 8 }, whole, "Uint8Array over a byte[]");
        ArrayBufferObject copy = TypedArrays.newArrayBufferCopy(realm, bytes, 2, 3);
        Check.equal(3, copy.getByteLength(), "copied range length");
        copy.getData()[0] = 0;
        Check.equal(99, bytes[2], "a copy does not share");
        buffer.detach();
        Check.isNull(TypedArrays.bytesOf(buffer), "a detached buffer has no bytes");
        Check.isNull(TypedArrays.bytesOf(u8), "a view on a detached buffer has no bytes");
    }

    /**
     * The Java API rejects invalid ranges and alignments.
     * <p>
     * Java API 는 잘못된 범위와 정렬을 거부한다.
     */
    public void testJavaApiValidation() {
        final ArrayBufferObject buffer = TypedArrays.newArrayBuffer(realm, new byte[8]);
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                TypedArrays.newTypedArray(realm, ElementType.INT32, buffer, 2, 1);
            }
        }, "misaligned offset");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                TypedArrays.newTypedArray(realm, ElementType.INT32, buffer, 4, 2);
            }
        }, "range beyond the buffer");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                TypedArrays.newArrayBuffer(realm, null);
            }
        }, "null data");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                TypedArrays.newArrayBufferCopy(realm, new byte[2], 1, 5);
            }
        }, "bad copy range");
        Check.throwsException(IllegalStateException.class, new Block() {
            public void run() {
                TypedArrays.newArrayBuffer(new JsRealm(), new byte[1]);
            }
        }, "a realm without the library");
    }
}
