package org.duckdns.hjow.swingwebpanel.tls13.crypto;

/**
 * Byte array helpers of the TLS module: comparison without data dependent branches, wiping of key material, concatenation and big endian
 * integers. Example: <code>if (!Bytes.constantTimeEquals(tag, 0, expected, 0, 16)) { reject(); }</code>.
 * <p>
 * TLS 모듈의 바이트 배열 도우미: 데이터에 따른 분기가 없는 비교, 키 자료 지우기, 이어 붙이기, 빅 엔디언 정수.
 * 사용 예: <code>if (!Bytes.constantTimeEquals(tag, 0, expected, 0, 16)) { reject(); }</code>.
 */
public final class Bytes {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Bytes() {
    }

    /**
     * Compares two ranges without stopping at the first difference, so the time does not tell where they differ.
     * <p>
     * 두 범위를 첫 차이에서 멈추지 않고 비교한다. 그래서 걸린 시간으로 어디가 다른지 알 수 없다.
     *
     * @param a    first array / 첫 배열
     * @param aOff start in the first array / 첫 배열의 시작 위치
     * @param b    second array / 둘째 배열
     * @param bOff start in the second array / 둘째 배열의 시작 위치
     * @param len  number of bytes / 바이트 수
     * @return true if all bytes are equal / 모든 바이트가 같으면 true
     */
    public static boolean constantTimeEquals(byte[] a, int aOff, byte[] b, int bOff, int len) {
        int diff = 0;
        // 모든 바이트의 차이를 OR 로 누적한다(중간에 멈추지 않는다).
        for (int i = 0; i < len; i++) {
            diff |= a[aOff + i] ^ b[bOff + i];
        }
        return diff == 0;
    }

    /**
     * Compares two whole arrays; arrays of different length are different.
     * <p>
     * 두 배열 전체를 비교한다. 길이가 다르면 다른 것이다.
     *
     * @param a first array / 첫 배열
     * @param b second array / 둘째 배열
     * @return true if equal / 같으면 true
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        return a.length == b.length && constantTimeEquals(a, 0, b, 0, a.length);
    }

    /**
     * Overwrites an array with zeros (key material that is no longer needed). A null array is ignored.
     * <p>
     * 배열을 0 으로 덮어쓴다(더 필요 없는 키 자료). null 배열은 무시한다.
     *
     * @param a the array or null / 배열 또는 null
     */
    public static void wipe(byte[] a) {
        if (a != null) {
            java.util.Arrays.fill(a, (byte) 0);
        }
    }

    /**
     * Concatenates arrays.
     * <p>
     * 배열을 이어 붙인다.
     *
     * @param parts the arrays / 배열들
     * @return the concatenation / 이어 붙인 결과
     */
    public static byte[] concat(byte[]... parts) {
        int n = 0;
        // 전체 길이를 센다.
        for (int i = 0; i < parts.length; i++) {
            n += parts[i].length;
        }
        byte[] out = new byte[n];
        int at = 0;
        // 차례로 복사한다.
        for (int i = 0; i < parts.length; i++) {
            System.arraycopy(parts[i], 0, out, at, parts[i].length);
            at += parts[i].length;
        }
        return out;
    }

    /**
     * Copies a range into a new array.
     * <p>
     * 범위를 새 배열로 복사한다.
     *
     * @param a   source / 원본
     * @param off start / 시작 위치
     * @param len length / 길이
     * @return the copy / 사본
     */
    public static byte[] copy(byte[] a, int off, int len) {
        byte[] out = new byte[len];
        System.arraycopy(a, off, out, 0, len);
        return out;
    }

    /**
     * Reads a big endian 32 bit integer.
     * <p>
     * 빅 엔디언 32비트 정수를 읽는다.
     *
     * @param a   source / 원본
     * @param off position / 위치
     * @return the value / 값
     */
    public static int be32(byte[] a, int off) {
        return ((a[off] & 0xff) << 24) | ((a[off + 1] & 0xff) << 16) | ((a[off + 2] & 0xff) << 8) | (a[off + 3] & 0xff);
    }

    /**
     * Reads a big endian 64 bit integer.
     * <p>
     * 빅 엔디언 64비트 정수를 읽는다.
     *
     * @param a   source / 원본
     * @param off position / 위치
     * @return the value / 값
     */
    public static long be64(byte[] a, int off) {
        return ((long) be32(a, off) << 32) | (be32(a, off + 4) & 0xffffffffL);
    }

    /**
     * Writes a big endian 32 bit integer.
     * <p>
     * 빅 엔디언 32비트 정수를 쓴다.
     *
     * @param v   the value / 값
     * @param a   destination / 대상
     * @param off position / 위치
     */
    public static void putBe32(int v, byte[] a, int off) {
        a[off] = (byte) (v >>> 24);
        a[off + 1] = (byte) (v >>> 16);
        a[off + 2] = (byte) (v >>> 8);
        a[off + 3] = (byte) v;
    }

    /**
     * Writes a big endian 64 bit integer.
     * <p>
     * 빅 엔디언 64비트 정수를 쓴다.
     *
     * @param v   the value / 값
     * @param a   destination / 대상
     * @param off position / 위치
     */
    public static void putBe64(long v, byte[] a, int off) {
        putBe32((int) (v >>> 32), a, off);
        putBe32((int) v, a, off + 4);
    }

    /**
     * Reads a little endian 32 bit integer.
     * <p>
     * 리틀 엔디언 32비트 정수를 읽는다.
     *
     * @param a   source / 원본
     * @param off position / 위치
     * @return the value / 값
     */
    public static int le32(byte[] a, int off) {
        return (a[off] & 0xff) | ((a[off + 1] & 0xff) << 8) | ((a[off + 2] & 0xff) << 16) | ((a[off + 3] & 0xff) << 24);
    }

    /**
     * Writes a little endian 32 bit integer.
     * <p>
     * 리틀 엔디언 32비트 정수를 쓴다.
     *
     * @param v   the value / 값
     * @param a   destination / 대상
     * @param off position / 위치
     */
    public static void putLe32(int v, byte[] a, int off) {
        a[off] = (byte) v;
        a[off + 1] = (byte) (v >>> 8);
        a[off + 2] = (byte) (v >>> 16);
        a[off + 3] = (byte) (v >>> 24);
    }

    /**
     * Writes a little endian 64 bit integer.
     * <p>
     * 리틀 엔디언 64비트 정수를 쓴다.
     *
     * @param v   the value / 값
     * @param a   destination / 대상
     * @param off position / 위치
     */
    public static void putLe64(long v, byte[] a, int off) {
        putLe32((int) v, a, off);
        putLe32((int) (v >>> 32), a, off + 4);
    }
}
