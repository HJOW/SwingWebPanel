package org.duckdns.hjow.swingwebpanel.media.image;

import java.io.ByteArrayOutputStream;

/**
 * A small byte writer for building test files: little-endian and big-endian integers on top of a growing buffer.
 * <p>
 * 시험 파일을 만들기 위한 작은 바이트 기록기: 늘어나는 버퍼 위에 리틀 엔디언과 빅 엔디언 정수를 쓴다.
 */
final class Bytes {

    /**
     * The buffer.
     * <p>
     * 버퍼.
     */
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    /**
     * Writes one byte.
     * <p>
     * 바이트 하나를 쓴다.
     *
     * @param v value, the low 8 bits are used / 값. 하위 8비트를 쓴다
     * @return this writer / 이 기록기
     */
    Bytes u8(int v) {
        out.write(v & 0xFF);
        return this;
    }

    /**
     * Writes a 16-bit little-endian value.
     * <p>
     * 16비트 리틀 엔디언 값을 쓴다.
     *
     * @param v value / 값
     * @return this writer / 이 기록기
     */
    Bytes le16(int v) {
        return u8(v).u8(v >> 8);
    }

    /**
     * Writes a 32-bit little-endian value.
     * <p>
     * 32비트 리틀 엔디언 값을 쓴다.
     *
     * @param v value / 값
     * @return this writer / 이 기록기
     */
    Bytes le32(int v) {
        return le16(v).le16(v >> 16);
    }

    /**
     * Writes a 16-bit big-endian value.
     * <p>
     * 16비트 빅 엔디언 값을 쓴다.
     *
     * @param v value / 값
     * @return this writer / 이 기록기
     */
    Bytes be16(int v) {
        return u8(v >> 8).u8(v);
    }

    /**
     * Writes a 32-bit big-endian value.
     * <p>
     * 32비트 빅 엔디언 값을 쓴다.
     *
     * @param v value / 값
     * @return this writer / 이 기록기
     */
    Bytes be32(int v) {
        return be16(v >> 16).be16(v);
    }

    /**
     * Writes bytes.
     * <p>
     * 바이트들을 쓴다.
     *
     * @param b bytes / 바이트
     * @return this writer / 이 기록기
     */
    Bytes put(byte[] b) {
        out.write(b, 0, b.length);
        return this;
    }

    /**
     * Writes an ASCII string.
     * <p>
     * ASCII 문자열을 쓴다.
     *
     * @param s text / 문자열
     * @return this writer / 이 기록기
     */
    Bytes ascii(String s) {
        // 문자마다 하위 8비트를 쓴다.
        for (int i = 0; i < s.length(); i++) {
            u8(s.charAt(i));
        }
        return this;
    }

    /**
     * Returns the number of bytes written so far.
     * <p>
     * 지금까지 쓴 바이트 수를 반환한다.
     *
     * @return size / 크기
     */
    int size() {
        return out.size();
    }

    /**
     * Returns the bytes written.
     * <p>
     * 쓴 바이트를 반환한다.
     *
     * @return bytes / 바이트
     */
    byte[] toBytes() {
        return out.toByteArray();
    }
}
