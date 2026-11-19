package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of DataView (ECMA-262 6th edition, 24.2; plan item P14-06): constructor range checks, the accessors, every get/set method with both
 * byte orders, value conversions (wrap-around, float rounding) and error cases.
 * <p>
 * DataView 시험(ECMA-262 6판, 24.2, 계획 P14-06): 생성자의 범위 검사, 접근자, 모든 get/set 메서드의 두 바이트 순서, 값 변환(감김, 부동소수점
 * 반올림), 오류 사례.
 */
public class DataViewTest {

    /**
     * The realm used by a case.
     * <p>
     * 사례가 쓰는 realm.
     */
    private JsRealm realm;

    /**
     * The 16 byte buffer of a case.
     * <p>
     * 사례의 16바이트 버퍼.
     */
    private ArrayBufferObject buffer;

    /**
     * A view on the whole buffer.
     * <p>
     * 버퍼 전체를 보는 뷰.
     */
    private JsObject view;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DataViewTest() {
    }

    /**
     * Creates a fresh realm, buffer and view before each case.
     * <p>
     * 사례마다 새 realm, 버퍼, 뷰를 만든다.
     */
    public void setUp() {
        realm = TypedTestSupport.newRealm();
        buffer = (ArrayBufferObject) TypedTestSupport.make(realm, "ArrayBuffer", TypedTestSupport.n(16));
        view = TypedTestSupport.make(realm, "DataView", buffer);
    }

    /**
     * Calls a method of the whole-buffer view.
     * <p>
     * 버퍼 전체 뷰의 메서드를 호출한다.
     *
     * @param method the method name / 메서드 이름
     * @param args   the numeric arguments / 숫자 인수
     * @return the result / 결과
     */
    private JsValue v(String method, double... args) {
        JsValue[] a = new JsValue[args.length];
        // 숫자 인수를 JavaScript 값으로 바꾼다.
        for (int i = 0; i < args.length; i++) {
            a[i] = TypedTestSupport.n(args[i]);
        }
        return TypedTestSupport.call(realm, view, method, a);
    }

    /**
     * The constructor needs new and a real ArrayBuffer.
     * <p>
     * 생성자는 new 와 진짜 ArrayBuffer 가 필요하다.
     */
    public void testConstructorArguments() {
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                Ops.call(TypedTestSupport.ctor(realm, "DataView"), Js.UNDEFINED, new JsValue[] { buffer });
            }
        }, "DataView(buffer) without new");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "DataView");
            }
        }, "no argument");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "DataView", realm.newObject());
            }
        }, "plain object");
        TypedTestSupport.eq(3, TypedTestSupport.num(TypedTestSupport.ctor(realm, "DataView").get("length")), "DataView.length is 3");
    }

    /**
     * Offset and length arguments are range checked against the buffer.
     * <p>
     * 위치와 길이 인수는 버퍼에 대해 범위를 검사한다.
     */
    public void testOffsetAndLengthRanges() {
        DataViewObject whole = (DataViewObject) view;
        Check.equal(0, whole.getByteOffset(), "offset of the whole view");
        Check.equal(16, whole.getByteLength(), "length of the whole view");
        DataViewObject tail = (DataViewObject) TypedTestSupport.make(realm, "DataView", buffer, TypedTestSupport.n(4));
        Check.equal(4, tail.getByteOffset(), "offset 4");
        Check.equal(12, tail.getByteLength(), "length defaults to the rest");
        DataViewObject mid = (DataViewObject) TypedTestSupport.make(realm, "DataView", buffer, TypedTestSupport.n(4), TypedTestSupport.n(8));
        Check.equal(8, mid.getByteLength(), "explicit length");
        DataViewObject empty = (DataViewObject) TypedTestSupport.make(realm, "DataView", buffer, TypedTestSupport.n(16));
        Check.equal(0, empty.getByteLength(), "an empty view at the end is allowed");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "DataView", buffer, TypedTestSupport.n(17));
            }
        }, "offset beyond the buffer");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "DataView", buffer, TypedTestSupport.n(-1));
            }
        }, "negative offset");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "DataView", buffer, TypedTestSupport.n(8), TypedTestSupport.n(9));
            }
        }, "length beyond the buffer");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "DataView", buffer, TypedTestSupport.n(0), TypedTestSupport.n(-1));
            }
        }, "negative length");
    }

    /**
     * The accessors report buffer, byteOffset and byteLength and reject foreign this values.
     * <p>
     * 접근자는 buffer, byteOffset, byteLength 를 보고하고 낯선 this 값을 거부한다.
     */
    public void testAccessors() {
        JsObject d = TypedTestSupport.make(realm, "DataView", buffer, TypedTestSupport.n(2), TypedTestSupport.n(5));
        Check.same(buffer, d.get("buffer"), "buffer");
        TypedTestSupport.eq(2, TypedTestSupport.num(d.get("byteOffset")), "byteOffset");
        TypedTestSupport.eq(5, TypedTestSupport.num(d.get("byteLength")), "byteLength");
        Check.equal("DataView", TypedTestSupport.str(d.get(WellKnownSymbols.TO_STRING_TAG)), "@@toStringTag");
        final JsObject proto = (JsObject) TypedTestSupport.ctor(realm, "DataView").get("prototype");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                proto.get("buffer");
            }
        }, "buffer getter on the prototype");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                Ops.call(proto.get("getInt8"), buffer, new JsValue[] { TypedTestSupport.n(0) });
            }
        }, "getInt8 on an ArrayBuffer");
    }

    /**
     * Integer values are stored with wrap-around and read back with the right sign in both byte orders.
     * <p>
     * 정수 값은 감기며 저장되고 두 바이트 순서 모두에서 올바른 부호로 다시 읽힌다.
     */
    public void testIntegerAccessorsAndByteOrder() {
        byte[] d = buffer.getData();
        v("setUint32", 0, 0x01020304);
        Check.equal(new byte[] { 1, 2, 3, 4 }, java.util.Arrays.copyOfRange(d, 0, 4), "big endian is the default");
        v("setUint32", 4, 0x01020304, 1);
        Check.equal(new byte[] { 4, 3, 2, 1 }, java.util.Arrays.copyOfRange(d, 4, 8), "little endian with the flag");
        TypedTestSupport.eq(0x01020304, TypedTestSupport.num(v("getUint32", 0)), "getUint32 big endian");
        TypedTestSupport.eq(0x04030201, TypedTestSupport.num(v("getUint32", 0, 1)), "getUint32 little endian");
        TypedTestSupport.eq(0x0102, TypedTestSupport.num(v("getUint16", 0)), "getUint16");
        TypedTestSupport.eq(0x0201, TypedTestSupport.num(v("getUint16", 0, 1)), "getUint16 little");
        v("setInt8", 8, 300);
        TypedTestSupport.eq(44, TypedTestSupport.num(v("getInt8", 8)), "setInt8 wraps modulo 256");
        v("setUint8", 9, -1);
        TypedTestSupport.eq(255, TypedTestSupport.num(v("getUint8", 9)), "setUint8(-1) is 255");
        TypedTestSupport.eq(-1, TypedTestSupport.num(v("getInt8", 9)), "the same byte read as Int8");
        v("setInt16", 10, -2);
        TypedTestSupport.eq(-2, TypedTestSupport.num(v("getInt16", 10)), "Int16 round trip");
        TypedTestSupport.eq(65534, TypedTestSupport.num(v("getUint16", 10)), "Int16 -2 read as Uint16");
        v("setInt32", 12, -123456789, 1);
        TypedTestSupport.eq(-123456789, TypedTestSupport.num(v("getInt32", 12, 1)), "Int32 little endian");
        TypedTestSupport.eq(4294967295.0, setAndGetUint32(4294967295.0), "Uint32 max");
        TypedTestSupport.eq(0, setAndGetUint32(4294967296.0), "Uint32 wraps");
        TypedTestSupport.eq(1, setAndGetUint32(4294967297.0), "Uint32 wraps to 1");
        TypedTestSupport.eq(0, setAndGetUint32(Double.NaN), "NaN is 0");
        TypedTestSupport.eq(0, setAndGetUint32(Double.POSITIVE_INFINITY), "Infinity is 0");
        TypedTestSupport.eq(4294967295.0, setAndGetUint32(-1), "-1 wraps");
    }

    /**
     * Stores a value with setUint32 and reads it back.
     * <p>
     * setUint32 로 값을 저장하고 다시 읽는다.
     *
     * @param x the value / 값
     * @return the stored value / 저장된 값
     */
    private double setAndGetUint32(double x) {
        v("setUint32", 0, x);
        return TypedTestSupport.num(v("getUint32", 0));
    }

    /**
     * Float32 and Float64 values keep their bit patterns and are rounded like IEEE 754 requires.
     * <p>
     * Float32 와 Float64 값은 비트 패턴을 유지하고 IEEE 754 대로 반올림된다.
     */
    public void testFloatAccessors() {
        byte[] d = buffer.getData();
        v("setFloat32", 0, 1.5);
        Check.equal(new byte[] { 0x3f, (byte) 0xc0, 0, 0 }, java.util.Arrays.copyOfRange(d, 0, 4), "1.5 as Float32");
        TypedTestSupport.eq(1.5, TypedTestSupport.num(v("getFloat32", 0)), "read back");
        v("setFloat32", 0, 16777217);
        TypedTestSupport.eq(16777216.0, TypedTestSupport.num(v("getFloat32", 0)), "2^24+1 is rounded to even in Float32");
        v("setFloat32", 0, 0.1);
        TypedTestSupport.eq((double) 0.1f, TypedTestSupport.num(v("getFloat32", 0)), "0.1 is rounded to float");
        v("setFloat32", 0, 1e40);
        TypedTestSupport.eq(Double.POSITIVE_INFINITY, TypedTestSupport.num(v("getFloat32", 0)), "overflow becomes Infinity");
        v("setFloat64", 0, Math.PI, 1);
        TypedTestSupport.eq(Math.PI, TypedTestSupport.num(v("getFloat64", 0, 1)), "Float64 round trip");
        Check.equal(Double.doubleToLongBits(Math.PI), java.nio.ByteBuffer.wrap(d, 0, 8).order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong(), "little endian bits");
        v("setFloat64", 0, Double.NaN);
        Check.isTrue(Double.isNaN(TypedTestSupport.num(v("getFloat64", 0))), "NaN");
        Check.equal(new byte[] { 0x7f, (byte) 0xf8, 0, 0, 0, 0, 0, 0 }, java.util.Arrays.copyOfRange(d, 0, 8), "NaN is stored in its canonical form");
        v("setFloat64", 0, -0.0);
        Check.isTrue(1 / TypedTestSupport.num(v("getFloat64", 0)) < 0, "-0 is kept");
    }

    /**
     * Offsets must be integers inside the view; out-of-range access is a RangeError for reads and writes.
     * <p>
     * 위치는 뷰 안의 정수여야 하며 범위를 벗어난 읽기와 쓰기는 RangeError.
     */
    public void testBoundsAndIndexConversion() {
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                v("getInt8", 16);
            }
        }, "getInt8(16)");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                v("getInt16", 15);
            }
        }, "getInt16(15)");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                v("getFloat64", 9);
            }
        }, "getFloat64(9)");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                v("setUint32", 13, 1);
            }
        }, "setUint32(13)");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                v("getInt8", -1);
            }
        }, "negative offset");
        TypedTestSupport.eq(0, TypedTestSupport.num(v("getInt8", 15)), "the last byte is readable");
        TypedTestSupport.eq(0, TypedTestSupport.num(TypedTestSupport.call(realm, view, "getInt8")), "a missing offset is 0");
        DataViewObject mid = (DataViewObject) TypedTestSupport.make(realm, "DataView", buffer, TypedTestSupport.n(4), TypedTestSupport.n(4));
        TypedTestSupport.eq(0, TypedTestSupport.num(TypedTestSupport.call(realm, mid, "getInt32", TypedTestSupport.n(0))), "inside the window");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.call(realm, mid(), "getInt32", TypedTestSupport.n(1));
            }

            /**
             * Creates the window view again.
             * <p>
             * 창 뷰를 다시 만든다.
             *
             * @return the view / 뷰
             */
            private JsObject mid() {
                return TypedTestSupport.make(realm, "DataView", buffer, TypedTestSupport.n(4), TypedTestSupport.n(4));
            }
        }, "the window of a view is respected");
    }

    /**
     * The value of a setter is converted with ToNumber before the bounds check, and the setters return undefined.
     * <p>
     * setter 의 값은 범위 검사 전에 ToNumber 로 변환되며 setter 는 undefined 를 반환한다.
     */
    public void testSetterConversionAndResult() {
        final boolean[] converted = new boolean[1];
        JsObject valueOf = realm.newObject();
        valueOf.defineData("valueOf", TypedTestSupport.fn(realm, "valueOf", 0, new org.duckdns.hjow.swingwebpanel.js.NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                converted[0] = true;
                return TypedTestSupport.n(7);
            }
        }), true, false, true);
        Check.isTrue(Ops.call(view.get("setInt8"), view, new JsValue[] { TypedTestSupport.n(0), valueOf }).isUndefined(), "setInt8 returns undefined");
        Check.isTrue(converted[0], "valueOf was called");
        TypedTestSupport.eq(7, TypedTestSupport.num(v("getInt8", 0)), "the converted value was stored");
        converted[0] = false;
        final JsObject target = view;
        final JsObject vo = valueOf;
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                Ops.call(target.get("setInt8"), target, new JsValue[] { TypedTestSupport.n(99), vo });
            }
        }, "out of range index");
        Check.isTrue(converted[0], "the value is converted before the range check (side effects happen)");
    }

    /**
     * Methods of a view on a detached buffer throw a TypeError.
     * <p>
     * 분리된 버퍼 위의 뷰의 메서드는 TypeError 를 던진다.
     */
    public void testDetachedBuffer() {
        buffer.detach();
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                v("getInt8", 0);
            }
        }, "getInt8 after detach");
        TypedTestSupport.eq(0, TypedTestSupport.num(view.get("byteLength")), "byteLength after detach");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                TypedTestSupport.make(realm, "DataView", buffer);
            }
        }, "creating a view on a detached buffer");
    }
}
