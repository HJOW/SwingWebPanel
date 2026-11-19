package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The element types of typed arrays and the accessors of DataView: how many bytes an element takes, how a JavaScript number is converted
 * into the stored bytes and how stored bytes are turned back into a number. Typed arrays always store little endian (ECMAScript leaves
 * the platform endianness implementation-defined; this library chooses little endian); DataView passes the endianness of every call.
 * Float values are written with canonical NaN bit patterns (0x7fc00000 for Float32, 0x7ff8000000000000 for Float64), so a NaN payload is
 * not preserved (allowed by ECMAScript 2015).
 * <p>
 * 형식화 배열의 요소 형식과 DataView 접근자: 요소가 몇 바이트인지, JavaScript 숫자를 저장 바이트로 바꾸는 방법, 저장 바이트를 다시 숫자로
 * 바꾸는 방법. 형식화 배열은 항상 리틀 엔디언으로 저장한다(ECMAScript 는 플랫폼 엔디언을 구현 정의로 남겨 두며 이 라이브러리는 리틀 엔디언을 고른다).
 * DataView 는 호출마다 엔디언을 받는다. 부동소수점 값은 표준 NaN 비트 패턴(Float32 는 0x7fc00000, Float64 는 0x7ff8000000000000)으로 쓰므로
 * NaN 페이로드는 보존하지 않는다(ECMAScript 2015 가 허용함).
 */
public enum ElementType {

    /**
     * Int8Array: signed 8 bit integers (ToInt8).
     * <p>
     * Int8Array: 부호 있는 8비트 정수(ToInt8).
     */
    INT8("Int8Array", 1),

    /**
     * Uint8Array: unsigned 8 bit integers (ToUint8).
     * <p>
     * Uint8Array: 부호 없는 8비트 정수(ToUint8).
     */
    UINT8("Uint8Array", 1),

    /**
     * Uint8ClampedArray: unsigned 8 bit integers clamped to 0..255 with round-half-to-even (ToUint8Clamp).
     * <p>
     * Uint8ClampedArray: 0~255 로 자르고 짝수로 반올림하는 부호 없는 8비트 정수(ToUint8Clamp).
     */
    UINT8_CLAMPED("Uint8ClampedArray", 1),

    /**
     * Int16Array: signed 16 bit integers.
     * <p>
     * Int16Array: 부호 있는 16비트 정수.
     */
    INT16("Int16Array", 2),

    /**
     * Uint16Array: unsigned 16 bit integers.
     * <p>
     * Uint16Array: 부호 없는 16비트 정수.
     */
    UINT16("Uint16Array", 2),

    /**
     * Int32Array: signed 32 bit integers.
     * <p>
     * Int32Array: 부호 있는 32비트 정수.
     */
    INT32("Int32Array", 4),

    /**
     * Uint32Array: unsigned 32 bit integers.
     * <p>
     * Uint32Array: 부호 없는 32비트 정수.
     */
    UINT32("Uint32Array", 4),

    /**
     * Float32Array: IEEE 754 single precision numbers.
     * <p>
     * Float32Array: IEEE 754 단정밀도 숫자.
     */
    FLOAT32("Float32Array", 4),

    /**
     * Float64Array: IEEE 754 double precision numbers.
     * <p>
     * Float64Array: IEEE 754 배정밀도 숫자.
     */
    FLOAT64("Float64Array", 8);

    /**
     * Name of the constructor, for example "Int8Array" (also the value of the [[TypedArrayName]] slot).
     * <p>
     * 생성자 이름. 예: "Int8Array" ([[TypedArrayName]] 슬롯의 값이기도 하다).
     */
    private final String constructorName;

    /**
     * Number of bytes of one element.
     * <p>
     * 요소 하나의 바이트 수.
     */
    private final int size;

    /**
     * Creates an element type.
     * <p>
     * 요소 형식을 만든다.
     *
     * @param constructorName constructor name / 생성자 이름
     * @param size            bytes per element / 요소당 바이트 수
     */
    private ElementType(String constructorName, int size) {
        this.constructorName = constructorName;
        this.size = size;
    }

    /**
     * Returns the constructor name such as "Uint8Array".
     * <p>
     * "Uint8Array" 같은 생성자 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getConstructorName() {
        return constructorName;
    }

    /**
     * Returns the number of bytes of one element (BYTES_PER_ELEMENT).
     * <p>
     * 요소 하나의 바이트 수(BYTES_PER_ELEMENT)를 반환한다.
     *
     * @return bytes per element / 요소당 바이트 수
     */
    public int getSize() {
        return size;
    }

    /**
     * Reads one element from a byte array.
     * <p>
     * 바이트 배열에서 요소 하나를 읽는다.
     *
     * @param data         the bytes / 바이트
     * @param offset       index of the first byte of the element / 요소의 첫 바이트 위치
     * @param littleEndian true for little endian byte order / 리틀 엔디언이면 true
     * @return the value as a JavaScript number / JavaScript 숫자로 바꾼 값
     */
    public double read(byte[] data, int offset, boolean littleEndian) {
        switch (this) {
        case INT8:
            return data[offset];
        case UINT8:
        case UINT8_CLAMPED:
            return data[offset] & 0xFF;
        case INT16:
            return (short) readBits(data, offset, 2, littleEndian);
        case UINT16:
            return (int) (readBits(data, offset, 2, littleEndian) & 0xFFFFL);
        case INT32:
            return (int) readBits(data, offset, 4, littleEndian);
        case UINT32:
            return readBits(data, offset, 4, littleEndian) & 0xFFFFFFFFL;
        case FLOAT32:
            return Float.intBitsToFloat((int) readBits(data, offset, 4, littleEndian));
        default:
            return Double.longBitsToDouble(readBits(data, offset, 8, littleEndian));
        }
    }

    /**
     * Converts a number to this element type and writes it into a byte array.
     * <p>
     * 숫자를 이 요소 형식으로 바꾸어 바이트 배열에 쓴다.
     *
     * @param data         the bytes / 바이트
     * @param offset       index of the first byte of the element / 요소의 첫 바이트 위치
     * @param value        the number to store / 저장할 숫자
     * @param littleEndian true for little endian byte order / 리틀 엔디언이면 true
     */
    public void write(byte[] data, int offset, double value, boolean littleEndian) {
        switch (this) {
        case INT8:
        case UINT8:
            data[offset] = (byte) Ops.toInt32(value);
            return;
        case UINT8_CLAMPED:
            data[offset] = (byte) clampUint8(value);
            return;
        case INT16:
        case UINT16:
            writeBits(data, offset, 2, Ops.toInt32(value), littleEndian);
            return;
        case INT32:
        case UINT32:
            writeBits(data, offset, 4, Ops.toInt32(value), littleEndian);
            return;
        case FLOAT32:
            writeBits(data, offset, 4, Float.floatToIntBits((float) value), littleEndian);
            return;
        default:
            writeBits(data, offset, 8, Double.doubleToLongBits(value), littleEndian);
            return;
        }
    }

    /**
     * ToUint8Clamp (7.1.11): NaN becomes 0, the value is clamped to 0..255 and rounded to the nearest integer, ties go to the even one.
     * <p>
     * ToUint8Clamp (7.1.11): NaN 은 0, 값은 0~255 로 자르고 가장 가까운 정수로 반올림하며 중간값은 짝수로 보낸다.
     *
     * @param d the number / 숫자
     * @return an integer from 0 to 255 / 0~255 의 정수
     */
    public static int clampUint8(double d) {
        if (d != d || d <= 0) {
            return 0;
        }
        if (d >= 255) {
            return 255;
        }
        double f = Math.floor(d);
        if (f + 0.5 < d) {
            return (int) f + 1;
        }
        if (d < f + 0.5) {
            return (int) f;
        }
        // 정확히 중간이면 짝수 쪽으로 보낸다.
        return ((int) f) % 2 == 0 ? (int) f : (int) f + 1;
    }

    /**
     * Reads a multi byte unsigned bit pattern.
     * <p>
     * 여러 바이트로 된 부호 없는 비트 패턴을 읽는다.
     *
     * @param data         the bytes / 바이트
     * @param offset       first byte / 첫 바이트
     * @param count        number of bytes (2, 4 or 8) / 바이트 수(2, 4, 8)
     * @param littleEndian byte order / 바이트 순서
     * @return the bits in the low part of a long / long 의 아래쪽에 담은 비트
     */
    private static long readBits(byte[] data, int offset, int count, boolean littleEndian) {
        long v = 0;
        // 바이트를 순서대로 모아 하나의 비트 패턴으로 만든다.
        for (int i = 0; i < count; i++) {
            int b = data[offset + (littleEndian ? count - 1 - i : i)] & 0xFF;
            v = (v << 8) | b;
        }
        return v;
    }

    /**
     * Writes a multi byte bit pattern.
     * <p>
     * 여러 바이트로 된 비트 패턴을 쓴다.
     *
     * @param data         the bytes / 바이트
     * @param offset       first byte / 첫 바이트
     * @param count        number of bytes (2, 4 or 8) / 바이트 수(2, 4, 8)
     * @param bits         the bit pattern (low bits are used) / 비트 패턴(아래쪽 비트를 사용)
     * @param littleEndian byte order / 바이트 순서
     */
    private static void writeBits(byte[] data, int offset, int count, long bits, boolean littleEndian) {
        long v = bits;
        // 낮은 바이트부터 풀어서 엔디언에 맞는 위치에 쓴다.
        for (int i = 0; i < count; i++) {
            data[offset + (littleEndian ? i : count - 1 - i)] = (byte) (v & 0xFF);
            v >>>= 8;
        }
    }
}
