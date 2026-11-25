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
 * Tests of the typed array constructors (ECMA-262 6th edition, 22.2.4; plan item P14-06) and of the value conversion of every element type:
 * all constructor forms, alignment and range errors, copying buffers, new.target prototypes, wrap-around and rounding conversions
 * and the little endian byte layout.
 * <p>
 * 형식화 배열 생성자 시험(ECMA-262 6판, 22.2.4, 계획 P14-06)과 모든 요소 형식의 값 변환 시험: 모든 생성자 형태, 정렬/범위 오류, 버퍼 복사,
 * new.target 프로토타입, 감김과 반올림 변환, 리틀 엔디언 바이트 배치.
 */
public class TypedArrayConstructorTest {

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
    public TypedArrayConstructorTest() {
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
     * Converts numbers with a typed array of the given kind and returns what is stored.
     * <p>
     * 주어진 종류의 형식화 배열로 숫자를 변환하고 저장된 값을 돌려준다.
     *
     * @param name the constructor name / 생성자 이름
     * @param xs   the numbers to store / 저장할 숫자
     * @return the stored numbers / 저장된 숫자
     */
    private double[] convert(String name, double... xs) {
        return TypedTestSupport.elements(TypedTestSupport.ta(realm, name, xs));
    }

    /**
     * Int8, Uint8 and Uint8Clamped wrap or clamp as specified.
     * <p>
     * Int8, Uint8, Uint8Clamped 는 규격대로 감기거나 잘린다.
     */
    public void testEightBitConversions() {
        TypedTestSupport.checkElements(new double[] { 127, -128, -127, 127, -1, 0, 1, -1, 0, 0, 0 },
                TypedTestSupport.ta(realm, "Int8Array", 127, 128, 129, -129, 255, 256, 1.9, -1.9, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
                "Int8Array");
        TypedTestSupport.checkElements(new double[] { 255, 0, 255, 1, 0, 255 },
                TypedTestSupport.ta(realm, "Uint8Array", 255, 256, -1, 1.5, Double.NaN, 511), "Uint8Array");
        TypedTestSupport.checkElements(new double[] { 0, 0, 0, 2, 2, 254, 255, 255, 0, 0, 1, 0, 255 },
                TypedTestSupport.ta(realm, "Uint8ClampedArray", -1, 0, 0.5, 1.5, 2.5, 254.5, 255.5, 300, Double.NaN, 0.49999, 0.50001,
                        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                "Uint8ClampedArray rounds half to even and clamps");
    }

    /**
     * 16 and 32 bit integer types wrap modulo 2^n.
     * <p>
     * 16, 32 비트 정수 형식은 2^n 으로 나눈 나머지로 감긴다.
     */
    public void testWiderIntegerConversions() {
        TypedTestSupport.checkElements(new double[] { 32767, -32768, 32767, 5 },
                TypedTestSupport.ta(realm, "Int16Array", 32767, 32768, -32769, 65541), "Int16Array");
        TypedTestSupport.checkElements(new double[] { 65535, 0, 65535, 0 },
                TypedTestSupport.ta(realm, "Uint16Array", 65535, 65536, -1, Double.NaN), "Uint16Array");
        TypedTestSupport.checkElements(new double[] { 2147483647, -2147483648.0, 2147483647, 7 },
                TypedTestSupport.ta(realm, "Int32Array", 2147483647.0, 2147483648.0, -2147483649.0, 4294967303.0), "Int32Array");
        TypedTestSupport.checkElements(new double[] { 4294967295.0, 0, 4294967295.0, 3 },
                TypedTestSupport.ta(realm, "Uint32Array", 4294967295.0, 4294967296.0, -1, 3.99), "Uint32Array");
    }

    /**
     * Float32 rounds to single precision and Float64 keeps the number; NaN, infinities and -0 are preserved.
     * <p>
     * Float32 는 단정밀도로 반올림하고 Float64 는 숫자를 유지한다. NaN, 무한대, -0 은 보존된다.
     */
    public void testFloatConversions() {
        TypedTestSupport.checkElements(new double[] { (double) 1.1f, 16777216, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, Double.NaN, -0.0 },
                TypedTestSupport.ta(realm, "Float32Array", 1.1, 16777217, 1e40, -1e40, 1e-50, Double.NaN, -0.0), "Float32Array");
        TypedTestSupport.checkElements(new double[] { 1.1, 1e300, Double.MIN_VALUE, -0.0, Double.NaN },
                TypedTestSupport.ta(realm, "Float64Array", 1.1, 1e300, Double.MIN_VALUE, -0.0, Double.NaN), "Float64Array");
    }

    /**
     * Typed arrays store little endian: the bytes of the buffer show the layout.
     * <p>
     * 형식화 배열은 리틀 엔디언으로 저장한다: 버퍼의 바이트가 배치를 보여 준다.
     */
    public void testLittleEndianLayout() {
        TypedArrayObject u16 = TypedTestSupport.ta(realm, "Uint16Array", 0x0102, 0xA0B0);
        Check.equal(new byte[] { 2, 1, (byte) 0xB0, (byte) 0xA0 }, u16.getBuffer().getData(), "Uint16Array bytes");
        TypedArrayObject i32 = TypedTestSupport.ta(realm, "Int32Array", -2);
        Check.equal(new byte[] { (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, i32.getBuffer().getData(), "Int32Array bytes");
        TypedArrayObject f32 = TypedTestSupport.ta(realm, "Float32Array", 1.5);
        Check.equal(new byte[] { 0, 0, (byte) 0xC0, 0x3F }, f32.getBuffer().getData(), "Float32Array bytes");
        TypedArrayObject nan = TypedTestSupport.ta(realm, "Float32Array", Double.NaN);
        Check.equal(new byte[] { 0, 0, (byte) 0xC0, 0x7F }, nan.getBuffer().getData(), "canonical Float32 NaN");
        TypedArrayObject nan64 = TypedTestSupport.ta(realm, "Float64Array", Double.NaN);
        Check.equal(new byte[] { 0, 0, 0, 0, 0, 0, (byte) 0xF8, 0x7F }, nan64.getBuffer().getData(), "canonical Float64 NaN");
    }

    /**
     * Every constructor needs new, has length 3 and BYTES_PER_ELEMENT, and inherits from %TypedArray%.
     * <p>
     * 모든 생성자는 new 가 필요하고 length 3 과 BYTES_PER_ELEMENT 를 가지며 %TypedArray% 를 상속한다.
     */
    public void testConstructorShape() {
        final String[] names = { "Int8Array", "Uint8Array", "Uint8ClampedArray", "Int16Array", "Uint16Array", "Int32Array", "Uint32Array", "Float32Array",
            "Float64Array" };
        double[] sizes = { 1, 1, 1, 2, 2, 4, 4, 4, 8 };
        JsObject abstractCtor = realm.getIntrinsic("%TypedArray%");
        Check.notNull(abstractCtor, "%TypedArray% intrinsic");
        Check.isFalse(realm.getGlobalObject().hasOwnProperty("TypedArray"), "%TypedArray% is not a global");
        // 각 생성자의 모양(프로토타입 체인, 속성, new 필요 여부)을 확인한다.
        for (int i = 0; i < names.length; i++) {
            final JsObject ctor = TypedTestSupport.ctor(realm, names[i]);
            Check.same(abstractCtor, ctor.getPrototypeOf(), names[i] + ".__proto__ is %TypedArray%");
            JsObject proto = (JsObject) ctor.get("prototype");
            Check.same(realm.getIntrinsic("%TypedArrayPrototype%"), proto.getPrototypeOf(), names[i] + ".prototype.__proto__");
            TypedTestSupport.eq(sizes[i], TypedTestSupport.num(ctor.get("BYTES_PER_ELEMENT")), names[i] + ".BYTES_PER_ELEMENT");
            TypedTestSupport.eq(sizes[i], TypedTestSupport.num(proto.get("BYTES_PER_ELEMENT")), names[i] + ".prototype.BYTES_PER_ELEMENT");
            TypedTestSupport.eq(3, TypedTestSupport.num(ctor.get("length")), names[i] + ".length");
            Check.equal(names[i], TypedTestSupport.str(ctor.get("name")), names[i] + ".name");
            Check.same(ctor, proto.get("constructor"), names[i] + ".prototype.constructor");
            TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
                public void run() {
                    Ops.call(ctor, Js.UNDEFINED, new JsValue[0]);
                }
            }, names[i] + "() without new");
        }
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                Ops.construct(realm.getIntrinsic("%TypedArray%"), new JsValue[0]);
            }
        }, "new %TypedArray%()");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                Ops.call(realm.getIntrinsic("%TypedArray%"), Js.UNDEFINED, new JsValue[0]);
            }
        }, "%TypedArray%()");
        TypedTestSupport.eq(0, TypedTestSupport.num(abstractCtor.get("length")), "%TypedArray%.length is 0");
        Check.equal("TypedArray", TypedTestSupport.str(abstractCtor.get("name")), "%TypedArray%.name");
    }

    /**
     * The length forms: no argument, numbers, strings, fractions; invalid lengths are a RangeError.
     * <p>
     * 길이 형태: 인수 없음, 숫자, 문자열, 소수. 잘못된 길이는 RangeError.
     */
    public void testLengthForms() {
        Check.equal(0, ((TypedArrayObject) TypedTestSupport.make(realm, "Uint8Array")).getLength(), "no argument");
        Check.equal(5, ((TypedArrayObject) TypedTestSupport.make(realm, "Uint8Array", TypedTestSupport.n(5))).getLength(), "5");
        Check.equal(3, ((TypedArrayObject) TypedTestSupport.make(realm, "Uint8Array", TypedTestSupport.s("3"))).getLength(), "string 3");
        Check.equal(2, ((TypedArrayObject) TypedTestSupport.make(realm, "Uint8Array", TypedTestSupport.n(2.7))).getLength(), "2.7");
        Check.equal(0, ((TypedArrayObject) TypedTestSupport.make(realm, "Uint8Array", Js.UNDEFINED)).getLength(), "undefined");
        Check.equal(0, ((TypedArrayObject) TypedTestSupport.make(realm, "Uint8Array", Js.NULL)).getLength(), "null");
        TypedArrayObject f64 = (TypedArrayObject) TypedTestSupport.make(realm, "Float64Array", TypedTestSupport.n(3));
        Check.equal(24, f64.getByteLength(), "byteLength of 3 doubles");
        Check.equal(24, f64.getBuffer().getByteLength(), "buffer size");
        TypedTestSupport.checkElements(new double[] { 0, 0, 0 }, f64, "new arrays are zero filled");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "Uint8Array", TypedTestSupport.n(-1));
            }
        }, "negative length");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "Float64Array", TypedTestSupport.n(1e10));
            }
        }, "too many bytes");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "Uint8Array", TypedTestSupport.n(Double.POSITIVE_INFINITY));
            }
        }, "Infinity");
    }

    /**
     * Copying from another typed array converts element types and gets its own buffer.
     * <p>
     * 다른 형식화 배열에서 복사하면 요소 형식이 변환되고 자기만의 버퍼를 가진다.
     */
    public void testCopyFromTypedArray() {
        TypedArrayObject src = TypedTestSupport.ta(realm, "Float32Array", 1.5, -2.5, 300);
        TypedArrayObject u8 = (TypedArrayObject) TypedTestSupport.make(realm, "Uint8Array", src);
        TypedTestSupport.checkElements(new double[] { 1, 254, 44 }, u8, "Float32 to Uint8 conversion");
        Check.isTrue(u8.getBuffer() != src.getBuffer(), "the copy has its own buffer");
        TypedArrayObject same = (TypedArrayObject) TypedTestSupport.make(realm, "Float32Array", src);
        TypedTestSupport.checkElements(new double[] { 1.5, -2.5, 300 }, same, "same type copy");
        same.setElement(0, 9);
        TypedTestSupport.eq(1.5, src.getElement(0), "source is unchanged by writes to the copy");
        TypedArrayObject clamped = (TypedArrayObject) TypedTestSupport.make(realm, "Uint8ClampedArray", TypedTestSupport.ta(realm, "Int16Array", -5, 300));
        TypedTestSupport.checkElements(new double[] { 0, 255 }, clamped, "Int16 to Uint8Clamped");
        final TypedArrayObject detached = TypedTestSupport.ta(realm, "Uint8Array", 1, 2);
        detached.getBuffer().detach();
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "Uint8Array", detached);
            }
        }, "copying a detached array");
    }

    /**
     * Arrays, array-likes and iterables are converted element by element with ToNumber.
     * <p>
     * 배열, 유사 배열, 반복 가능 객체는 요소마다 ToNumber 로 변환된다.
     */
    public void testArrayLikeAndIterableSources() {
        TypedTestSupport.checkElements(new double[] { 1, 2, 3 }, TypedTestSupport.ta(realm, "Uint8Array", 1, 2, 3), "array");
        JsObject arrayLike = realm.newObject();
        arrayLike.set("length", TypedTestSupport.n(3));
        arrayLike.set("0", TypedTestSupport.s("7"));
        arrayLike.set("1", Js.TRUE);
        arrayLike.set("2", Js.NULL);
        TypedTestSupport.checkElements(new double[] { 7, 1, 0 }, TypedTestSupport.make(realm, "Uint8Array", arrayLike), "array-like with conversions");
        JsObject sparse = realm.newObject();
        sparse.set("length", TypedTestSupport.n(2));
        TypedTestSupport.checkElements(new double[] { 0, 0 }, TypedTestSupport.make(realm, "Uint8Array", sparse), "missing elements become NaN, which is 0 for integer types");
        TypedTestSupport.checkElements(new double[] { Double.NaN, Double.NaN }, TypedTestSupport.make(realm, "Float64Array", sparse), "missing elements are NaN for floats");

        final int[] pos = new int[1];
        JsObject iterable = realm.newObject();
        iterable.defineBuiltin(WellKnownSymbols.ITERATOR, TypedTestSupport.fn(realm, "iter", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject it = r.newObject();
                it.defineBuiltin("next", TypedTestSupport.fn(r, "next", 0, new NativeFunction.Call() {
                    public JsValue call(JsRealm rr, JsValue t, JsValue[] a) {
                        JsObject result = rr.newObject();
                        boolean done = pos[0] >= 3;
                        result.set("done", Js.bool(done));
                        result.set("value", done ? Js.UNDEFINED : (JsValue) TypedTestSupport.n(10 + pos[0]++));
                        return result;
                    }
                }));
                return it;
            }
        }));
        TypedTestSupport.checkElements(new double[] { 10, 11, 12 }, TypedTestSupport.make(realm, "Int16Array", iterable), "iterable source (preferred over length)");
        TypedTestSupport.checkElements(new double[] { 104, 105 }, TypedTestSupport.make(realm, "Uint8Array", arrFromString("hi")), "array of char codes");
    }

    /**
     * Builds the array [104, 105] style list of char codes of a string (strings themselves have no iterator in this bare realm).
     * <p>
     * 문자열의 글자 코드 목록을 만든다(이 최소 realm 에서는 문자열 자체에 이터레이터가 없다).
     *
     * @param s the text / 문자열
     * @return the array / 배열
     */
    private JsObject arrFromString(String s) {
        double[] codes = new double[s.length()];
        // 글자마다 코드 값을 모은다.
        for (int i = 0; i < codes.length; i++) {
            codes[i] = s.charAt(i);
        }
        return TypedTestSupport.arr(realm, codes);
    }

    /**
     * A view on an ArrayBuffer shares its memory; offset and length are validated for range and alignment.
     * <p>
     * ArrayBuffer 위의 뷰는 메모리를 공유한다. 위치와 길이는 범위와 정렬이 검증된다.
     */
    public void testViewOnBuffer() {
        final ArrayBufferObject buf = (ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(16));
        TypedArrayObject all = (TypedArrayObject) TypedTestSupport.make(realm, "Int32Array", buf);
        Check.equal(4, all.getLength(), "length defaults to the whole buffer");
        Check.same(buf, all.getBuffer(), "the buffer is shared");
        all.setElement(0, 0x01020304);
        Check.equal(4, buf.getData()[0], "the write is visible in the buffer");
        TypedArrayObject part = (TypedArrayObject) TypedTestSupport.view(realm, "Int16Array", buf, 4, 3);
        Check.equal(3, part.getLength(), "explicit length");
        Check.equal(4, part.getByteOffset(), "byteOffset");
        Check.equal(6, part.getByteLength(), "byteLength");
        TypedArrayObject tail = (TypedArrayObject) TypedTestSupport.view(realm, "Uint8Array", buf, 10);
        Check.equal(6, tail.getLength(), "length defaults to the rest");
        Check.equal(0, ((TypedArrayObject) TypedTestSupport.view(realm, "Uint8Array", buf, 16)).getLength(), "an empty view at the end");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.view(realm, "Int32Array", buf, 2);
            }
        }, "misaligned offset");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.view(realm, "Int32Array", buf, 4, 4);
            }
        }, "length beyond the buffer");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.view(realm, "Uint8Array", buf, 17);
            }
        }, "offset beyond the buffer");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.view(realm, "Int16Array", TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(5)));
            }
        }, "buffer length not a multiple of the element size");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.view(realm, "Uint8Array", buf, -1);
            }
        }, "negative offset");
        buf.detach();
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "Uint8Array", buf);
            }
        }, "detached buffer");
    }

    /**
     * new.target decides the prototype, which allows subclassing.
     * <p>
     * new.target 이 프로토타입을 정하며 이것으로 하위 클래스를 만들 수 있다.
     */
    public void testNewTargetPrototype() {
        JsObject customProto = realm.newObject();
        JsObject target = TypedTestSupport.fn(realm, "Sub", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        });
        target.defineData("prototype", customProto, false, false, false);
        JsObject o = TypedTestSupport.ctor(realm, "Uint8Array").construct(new JsValue[] { TypedTestSupport.n(2) }, target);
        Check.same(customProto, o.getPrototypeOf(), "[[Prototype]] taken from new.target");
        Check.isTrue(o instanceof TypedArrayObject, "still a typed array");
        JsObject plain = TypedTestSupport.make(realm, "Uint8Array", TypedTestSupport.n(1));
        Check.same(realm.getIntrinsic("%Uint8ArrayPrototype%"), plain.getPrototypeOf(), "default prototype");
    }

    /**
     * Copying a typed array creates an intrinsic ArrayBuffer in the current realm.
     * <p>
     * 형식화 배열을 복사하면 현재 realm의 내장 ArrayBuffer를 만든다.
     */
    public void testIntrinsicBufferOfCopy() {
        final TypedArrayObject src = TypedTestSupport.ta(realm, "Uint8Array", 1, 2, 3);
        final int[] calls = new int[1];
        final JsObject abCtor = TypedTestSupport.ctor(realm, "ArrayBuffer");
        NativeFunction species = new NativeFunction(realm, "Species", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.UNDEFINED;
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                calls[0]++;
                return Ops.construct(abCtor, args);
            }
        });
        JsObject c = realm.newObject();
        c.defineData(WellKnownSymbols.SPECIES, species, true, false, true);
        src.getBuffer().defineData("constructor", c, true, false, true);
        TypedArrayObject copy = (TypedArrayObject) TypedTestSupport.make(realm, "Uint8Array", src);
        Check.equal(0, calls[0], "copying bypasses the source buffer's species constructor");
        Check.same(realm.getIntrinsic("%ArrayBufferPrototype%"), copy.getBuffer().getPrototypeOf(), "intrinsic buffer prototype");
        TypedTestSupport.checkElements(new double[] { 1, 2, 3 }, copy, "copy");
    }

    /**
     * Converting an element can run user code (valueOf) and a failure stops the construction.
     * <p>
     * 요소 변환은 사용자 코드(valueOf)를 실행할 수 있고 실패하면 생성이 중단된다.
     */
    public void testConversionRunsUserCodeAndPropagatesErrors() {
        JsObject bad = realm.newObject();
        bad.defineData("valueOf", TypedTestSupport.fn(realm, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw org.duckdns.hjow.swingwebpanel.js.JsException.rangeError("from valueOf");
            }
        }), true, false, true);
        final JsObject list = TypedTestSupport.arr(realm, 1);
        list.set("0", bad);
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "Uint8Array", list);
            }
        }, "an exception from valueOf propagates");
        JsObject good = realm.newObject();
        good.defineData("valueOf", TypedTestSupport.fn(realm, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return TypedTestSupport.n(42);
            }
        }), true, false, true);
        list.set("0", good);
        TypedTestSupport.checkElements(new double[] { 42 }, TypedTestSupport.make(realm, "Uint8Array", list), "valueOf result");
    }
}
