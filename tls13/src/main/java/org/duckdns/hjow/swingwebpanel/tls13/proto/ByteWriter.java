package org.duckdns.hjow.swingwebpanel.tls13.proto;

import java.io.ByteArrayOutputStream;

/**
 * Builds TLS structures: big endian integers of one, two and three bytes and vectors with a length prefix (RFC 8446 section 3).
 * Example:
 * <pre>
 * ByteWriter w = new ByteWriter();
 * w.u16(0x0303);
 * w.vector8(sessionId);          // opaque legacy_session_id&lt;0..32&gt;
 * byte[] encoded = w.toByteArray();
 * </pre>
 * <p>
 * TLS 구조를 만든다: 1, 2, 3바이트의 빅 엔디언 정수와 길이 접두사가 있는 벡터(RFC 8446 3절).
 */
public final class ByteWriter {

    /** The bytes written so far. / 지금까지 쓴 바이트. */
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    /**
     * Creates an empty writer.
     * <p>
     * 빈 작성기를 만든다.
     */
    public ByteWriter() {
    }

    /**
     * Writes one byte.
     * <p>
     * 한 바이트를 쓴다.
     *
     * @param v the value / 값
     * @return this writer / 이 작성기
     */
    public ByteWriter u8(int v) {
        out.write(v);
        return this;
    }

    /**
     * Writes a two byte integer.
     * <p>
     * 2바이트 정수를 쓴다.
     *
     * @param v the value / 값
     * @return this writer / 이 작성기
     */
    public ByteWriter u16(int v) {
        out.write(v >> 8);
        out.write(v);
        return this;
    }

    /**
     * Writes a three byte integer.
     * <p>
     * 3바이트 정수를 쓴다.
     *
     * @param v the value / 값
     * @return this writer / 이 작성기
     */
    public ByteWriter u24(int v) {
        out.write(v >> 16);
        out.write(v >> 8);
        out.write(v);
        return this;
    }

    /**
     * Writes bytes without a length.
     * <p>
     * 길이 없이 바이트를 쓴다.
     *
     * @param data the bytes / 바이트
     * @return this writer / 이 작성기
     */
    public ByteWriter bytes(byte[] data) {
        out.write(data, 0, data.length);
        return this;
    }

    /**
     * Writes a vector with a one byte length.
     * <p>
     * 1바이트 길이의 벡터를 쓴다.
     *
     * @param data the content / 내용
     * @return this writer / 이 작성기
     */
    public ByteWriter vector8(byte[] data) {
        return u8(data.length).bytes(data);
    }

    /**
     * Writes a vector with a two byte length.
     * <p>
     * 2바이트 길이의 벡터를 쓴다.
     *
     * @param data the content / 내용
     * @return this writer / 이 작성기
     */
    public ByteWriter vector16(byte[] data) {
        return u16(data.length).bytes(data);
    }

    /**
     * Writes a vector with a three byte length.
     * <p>
     * 3바이트 길이의 벡터를 쓴다.
     *
     * @param data the content / 내용
     * @return this writer / 이 작성기
     */
    public ByteWriter vector24(byte[] data) {
        return u24(data.length).bytes(data);
    }

    /**
     * Returns the number of bytes written.
     * <p>
     * 쓴 바이트 수를 반환한다.
     *
     * @return the size / 크기
     */
    public int size() {
        return out.size();
    }

    /**
     * Returns the bytes written.
     * <p>
     * 쓴 바이트를 반환한다.
     *
     * @return the bytes / 바이트
     */
    public byte[] toByteArray() {
        return out.toByteArray();
    }
}
